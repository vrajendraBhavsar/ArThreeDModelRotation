package com.example.arthreedmodelrotation

import android.graphics.BitmapFactory
import android.graphics.Typeface
import android.os.Bundle
import android.util.Log
import android.view.Choreographer
import android.view.Choreographer.FrameCallback
import android.view.GestureDetector
import android.view.GestureDetector.OnDoubleTapListener
import android.view.Gravity
import android.view.MotionEvent
import android.view.SurfaceView
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DefaultChoreographerFrameClock.choreographer
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key.Companion.D
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.lifecycleScope
import com.example.arthreedmodelrotation.ui.theme.ArThreeDModelRotationTheme
import com.google.android.filament.Fence
import com.google.android.filament.IndirectLight
import com.google.android.filament.Skybox
import com.google.android.filament.View
import com.google.android.filament.utils.AutomationEngine
import com.google.android.filament.utils.HDRLoader
import com.google.android.filament.utils.IBLPrefilterContext
import com.google.android.filament.utils.KTX1Loader
import com.google.android.filament.utils.KTX1Loader.createIndirectLight
import com.google.android.filament.utils.ModelViewer
import com.google.android.filament.utils.RemoteServer
import com.google.android.filament.utils.Utils
import com.google.gson.GsonBuilder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.ResponseBody
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream
import java.io.RandomAccessFile
import java.net.URI
import java.nio.Buffer
import java.nio.ByteBuffer
import java.nio.charset.StandardCharsets
import java.util.concurrent.TimeUnit
import java.util.zip.ZipInputStream

class MainActivity : ComponentActivity() {
    companion object {
        init {
            Utils.init()
        }

        private const val TAG = "mi-gltf-demo"
    }

    private lateinit var surfaceView: SurfaceView
    private lateinit var choreographer: Choreographer
    private val frameScheduler = FrameCallback()
    private lateinit var modelViewer: ModelViewer
    private lateinit var titleBarHint: TextView
    private val doubleTapListener = DoubleTapListener()
    private lateinit var doubleTapDetector: GestureDetector
    private var remoteServer: RemoteServer? = null
    private var statusToast: Toast? = null
    private var statusText: String? = null
    private var latestDownload: String? = null
    private val automation = AutomationEngine()
    private var loadStartTime = 0L
    private var loadStartFence: Fence? = null
    private val viewerContent = AutomationEngine.ViewerContent()

    private var path: File? = null
    var file: File? = null
    private fun createPath(): File? {
        path = File(filesDir, "gltf")
        if (path?.exists() == false) {
            path?.mkdirs()
        }
        return path
    }

    private suspend fun downloadModel(): String? {
        file = File(createPath(), "Duck.glb")
        if (file?.exists() == true) {
            Log.i(TAG, "not downloaded")
            return "gltf/Duck.glb"
        } else {
            Log.i(TAG, "downloading")
            val okHttpClient = OkHttpClient.Builder()
                .connectTimeout(3, TimeUnit.MINUTES)
                .readTimeout(3, TimeUnit.MINUTES)
                .build()
            val retrofit = Retrofit.Builder()
                .client(okHttpClient)
                .baseUrl("https://raw.githubusercontent.com/")
                .addConverterFactory(GsonConverterFactory.create(GsonBuilder().create()))
                .build().create(GltfApi::class.java)
//            https://raw.githubusercontent.com/Sachinbhola/App-Templates/master/Resources/chair/arm_chair__furniture/scene.gltf
            val response =
                retrofit.downloadFile("Sachinbhola/App-Templates/master/Resources/chair/arm_chair__furniture/scene.gltf")
            return if (response.isSuccessful) {
                file = File(createPath(), "Duck.glb")
                file?.absolutePath?.let { saveFile(response.body(), it) }
                return "gltf/Duck.glb"
            } else {
                Log.i(TAG, "Retrofit Error")
                null
            }
        }
    }

    private fun saveFile(body: ResponseBody?, pathWhereYouWantToSaveFile: String): String {
        if (body == null)
            return ""

        var input: InputStream? = null
        try {
            input = body.byteStream()
            //val file = File(getCachedir(), "cacheFileAppeal.srl")
            val fos = FileOutputStream(pathWhereYouWantToSaveFile)
            fos.use { output ->
                val buffer = ByteArray(4 * 1024) // or other buffer size
                var read: Int
                while (input.read(buffer).also { read = it } != -1) {
                    output.write(buffer,0, read)
                }
                output.flush()
            }
            return pathWhereYouWantToSaveFile
        } catch (e: Exception) {
            Log.e( "saveFile", e.toString())
        } finally {
            input?.close()
        }
        return ""
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ArThreeDModelRotationTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

                    titleBarHint = findViewById(R.id.user_hint)
                    surfaceView = findViewById(R.id.main_sv)
                    choreographer = Choreographer.getInstance()

                    doubleTapDetector =
                        GestureDetector(applicationContext, doubleTapListener)

                    modelViewer = ModelViewer(surfaceView)
                    viewerContent.view = modelViewer.view
                    viewerContent.sunlight = modelViewer.light
                    viewerContent.lightManager = modelViewer.engine.lightManager
                    viewerContent.scene = modelViewer.scene
                    viewerContent.renderer = modelViewer.renderer
                    surfaceView.setOnTouchListener { _, motionEvent ->
                        modelViewer.onTouchEvent(motionEvent)
                        doubleTapDetector.onTouchEvent(motionEvent)
                        true
                    }

//                    lifecycleScope.launch {
                    LaunchedEffect(key1 = true) {}
                        downloadGltf()
                    }

                    createDefaultRenderables()
                    createIndirectLight()

                    setStatusText("To load a new model, go to the above URL on your host machine.")

                    val view = modelViewer.view
                    /*
                    * Note: The settings below are overriden when connecting to the remote UI.
                    */

                    // on mobile, better use lower quality color buffer
                    view.renderQuality = view.renderQuality.apply {
                        hdrColorBuffer = View.QualityLevel.MEDIUM
                    }
                    // dynamic resolution often helps a lot
                    view.dynamicResolutionOptions =
//                        view.dynamicResolutionBitmapFactory.Options.apply {
                        view.dynamicResolutionOptions.apply {
                            enabled = true
                            quality = View.QualityLevel.MEDIUM
                        }
                    // MSAA is needed with dynamic resolution MEDIUM
                    view.multiSampleAntiAliasingOptions =
                        view.multiSampleAntiAliasingOptions.apply {

                            // FXAA is pretty cheap and helps a lot
                            view.antiAliasing = View.AntiAliasing.FXAA
// ambient occlusion is the cheapest effect that adds a lot of quality
                            view.ambientOcclusionOptions =
                                view.ambientOcclusionOptions.apply {
                                    enabled = true
                                }
// bloom is pretty expensive but adds a fair amount of realism
                            view.bloomOptions = view.bloomOptions.apply {
                                enabled = true
                            }
                            remoteServer = RemoteServer(8082)
                            //....
                        }
                }
            }
        }

    //Here we'll load GLB/ GLTF model
    private fun createDefaultRenderables() {
        val buffer = assets.open("models/Duck.glb").use { input ->
            val bytes = ByteArray(input.available())
            input.read(bytes)
            ByteBuffer.wrap(bytes)
        }
        modelViewer.loadModelGltfAsync(buffer) { uri -> readCompressedAsset("models/$uri") }
        updateRootTransform()
    }

    private fun createIndirectLight() {
        val engine = modelViewer.engine
        val scene = modelViewer.scene
        val ibl = "venetian_crossroads_2k"
        readCompressedAsset("envs/$ibl/${ibl}_ibl.ktx").let {
            scene.indirectLight = createIndirectLight(engine, it)
            scene.indirectLight!!.intensity = 30_000.0f
            viewerContent.indirectLight = modelViewer.scene.indirectLight
        }
        readCompressedAsset("envs/$ibl/${ibl}_skybox.ktx").let {
            scene.skybox = KTX1Loader.createSkybox(engine, it)
        }
    }

    private fun readCompressedAsset(assetName: String): ByteBuffer {
        val input = assets.open(assetName)
        val bytes = ByteArray(input.available())
        input.read(bytes)
        return ByteBuffer.wrap(bytes)
    }

    private fun clearStatusText() {
        statusToast?.let {
            it.cancel()
            statusText = null
        }
    }

    private fun setStatusText(text: String) {
        runOnUiThread {
            if (statusToast == null || statusText != text) {
                statusText = text
                statusToast =
                    Toast.makeText(applicationContext, text, Toast.LENGTH_SHORT)
                statusToast!!.show()
            }
        }
    }

    private suspend fun loadGlb(message: RemoteServer.ReceivedMessage) {
        withContext(Dispatchers.Main) {
            modelViewer.destroyModel()
            modelViewer.loadModelGlb(message.buffer)
            updateRootTransform()
            loadStartTime = System.nanoTime()
            loadStartTime = modelViewer.engine.createFence()
        }
    }

    private suspend fun loadHdr(message: RemoteServer.ReceivedMessage) {
        withContext(Dispatchers.Main) {
            val engine = modelViewer.engine
            val equirect = HDRLoader.createTexture(engine, message.buffer)
            if (equirect == null) {
                setStatusText("Could not decode HDR file.")
            } else {
                setStatusText("Successfully decoded HDR file.")

                val context = IBLPrefilterContext(engine)
                val equirectToCubemap =
                    IBLPrefilterContext.EquirectangularToCubemap(context)
                val skyboxTexture = equirectToCubemap.run(equirect)!!
                engine.destroyTexture(equirect)

                val specularFilter = IBLPrefilterContext.SpecularFilter(context)
                val reflections = specularFilter.run(skyboxTexture)

                val ibl = IndirectLight.Builder()
                    .reflections(reflections)
                    .intensity(36000.0f)
                    .build(engine)

                val sky = Skybox.Builder().environment(skyboxTexture).build(engine)

                specularFilter.destroy()
                equirectToCubemap.destroy()
                context.destroy()

                //destroy the previous IBL
                engine.destroyIndirectLight(modelViewer.scene.indirectLight!!)
                engine.destroySkybox(modelViewer.scene.skybox!!)

                modelViewer.scene.skybox = sky
                modelViewer.scene.indirectLight = ibl
                viewerContent.indirectLight = ibl
            }
        }
    }

    private suspend fun loadZip(message: RemoteServer.ReceivedMessage) {
        withContext(Dispatchers.Main) {
            modelViewer.destroyModel()
        }

        // Large zip files should first be written to a file to prevent OOM.
        // 1t is also crucial that we null out the message "buffer” field.
        val (zipStream, zipFile) = withContext(Dispatchers.IO) {
            val file = File.createTempFile("incoming", "zip", cacheDir)
            val raf = RandomAccessFile(file, "rw")
            raf.channel.write(message.buffer)
            message.buffer = null
            raf.seek(0)
            Pair(FileInputStream(file), file)
        }

        // Deflate each resource using the 10 dispatcher, one by one. §
        var gltfPath: String? = null
        var outOfMemory: String? = null
        val pathToBufferMapping = withContext(Dispatchers.IO) {
            val deflater = ZipInputStream(zipStream)
            val mapping = HashMap<String, Buffer>()
            while (true) {
                val entry = deflater.nextEntry ?: break
                if (entry.isDirectory) continue

                // This isn't strictly required, but as an optimization
                // we ignore common junk that often pollutes ZIP files.
                if (entry.name.startsWith("__MACOSX")) continue
                if (entry.name.startsWith(".DS_Store")) continue

                val uri: String = entry.name
                val byteArray: ByteArray? = try {
                    deflater.readBytes()
                } catch (e: OutOfMemoryError) {
                    outOfMemory = uri
                    break
                }
                Log.i("TAG", "Deflated ${byteArray!!.size} bytes from $uri")
                val buffer = ByteBuffer.wrap(byteArray)
                mapping[uri] = buffer

                if (uri.endsWith(".gltf") || uri.endsWith(".glb")) {
                    gltfPath = uri
                }
            }
            mapping
        }

        zipFile.delete()

        if (gltfPath == null) {
            setStatusText("Could not find .gltf or .glb in the zip.")
            return
        }

        if (outOfMemory != null) {
            setStatusText("0ut of memory while deflating $outOfMemory")
            return
        }
        //..
        val gltfBuffer = pathToBufferMapping[gltfPath]!!

        // In a zip file, the gltf file might be in the same folder as resources, or in a different
        // folder. It is crucial to test against both of these cases. In any case, the resource
        // paths are all specified relative to the location of the gltf file.
        var prefix = URI(gltfPath!!).resolve(".")

        withContext(Dispatchers.Main) {
            if (gltfPath!!.endsWith(".glb")) {
                    modelViewer.loadModelGlb(gltfBuffer)
                } else {
                modelViewer.loadModelGltf(gltfBuffer) { uri ->
                    val path = prefix.resolve(uri).toString()
                    if (!pathToBufferMapping.contains(path)) {
                        Log.e(
                            TAG,
                            "Could not find '$uri' in zip using prefix '$prefix' and base path '${gltfPath!!}'"
                        )
                        setStatusText("Zip is missing $path")
                    }
                    pathToBufferMapping[path]
                }
            }
            updateRootTransform()
            loadStartTime = System.nanoTime()
            loadStartFence = modelViewer.engine.createFence()
        }
    }

    override fun onResume() {
        super.onResume()
        choreographer.postFrameCallback(frameScheduler)
    }

    override fun onPause() {
        super.onPause()
        choreographer.removeFrameCallback(frameScheduler)
    }

    override fun onDestroy() {
        super.onDestroy()
        choreographer.removeFrameCallback(frameScheduler)
        remoteServer?.close()
    }

    fun loadModelData(message: RemoteServer.ReceivedMessage) {
        Log.i(
            TAG,
            "Download model ${message.label} (${message.buffer.capacity()} bytes)"
        )
        clearStatusText()
        titleBarHint.text = message.label
        CoroutineScope(Dispatchers.IO).launch {
            when {
                message.label.endsWith(".zip") -> loadZip(message)
                message.label.endsWith(".hdr") -> loadHdr(message)
                else -> loadGlb(message)
            }
        }
    }

    fun loadSettings(message: RemoteServer.ReceivedMessage) {
        val json = StandardCharsets.UTF_8.decode(message.buffer).toString()
        viewerContent.applyLights = modelViewer.asset?.lightEntities
        automation.applySettings(modelViewer.engine, json, viewerContent)
        modelViewer.view.colorGrading =
            automation.getColorGrading(modelViewer.engine)
        modelViewer.cameraFocalLength = automation.viewerOptions.cameraFocalLength
        modelViewer.cameraNear = automation.viewerOptions.cameraNear
        modelViewer.cameraFar = automation.viewerOptions.cameraFar
        updateRootTransform()
    }

    private fun updateRootTransform() {
        if (automation.viewerOptions.autoScaleEnabled) {
            modelViewer.transformToUnitCube()
        } else {
            modelViewer.clearRootTransform()
        }
    }

    inner class FrameCallback : Choreographer.FrameCallback {
        private val startTime = System.nanoTime()

        override fun doFrame(frameTimeNanos: Long) {
            choreographer.postFrameCallback(this)

            loadStartFence?.let {
                if (it.wait(
                        Fence.Mode.FLUSH,
                        0
                    ) == Fence.FenceStatus.CONDITION_SATISFIED
                ) {
                    val end = System.nanoTime()
                    val total = (end - loadStartTime) / 1.660_660

                    Log.i(
                        TAG,
                        "The Filament backend took $total ms to load the model geometry."
                    )
                    modelViewer.engine.destroyFence(it)
                    loadStartFence = null
                }
            }

            modelViewer.animator?.apply {
                if (animationCount > 8) {
                    val elapsedTineSeconds =
                        (frameTimeNanos - loadStartTime).toDouble() / 1_000_000_000
                    applyAnimation(0, elapsedTineSeconds.toFloat())
                }
                updateBoneMatrices()
            }
            modelViewer.render(frameTimeNanos)
            //Check if a new download is in progress. If so, let the user know with toast 3
            val currentDownload = remoteServer?.peekIncomingLabel()
            if (RemoteServer.isBinary(currentDownload) && currentDownload != latestDownload) {
                latestDownload = currentDownload
                Log.i(TAG, "Downloading $currentDownload")
                setStatusText("Downloading $currentDownload")
            }

            //Check if a new message has been fuly received from the client
            val message = remoteServer?.acquireReceivedMessage()
            if (message != null) {
                if (message.label == latestDownload) {
                    latestDownload = null
                }

                if (RemoteServer.isJson(message.label)) {
                    loadSettings(message)
                } else {
                    loadModelData(message)
                }
            }
        }
    }


    // Just for testing purposes, this releases the current model and reloads the default model.
    inner class DoubleTapListener : GestureDetector.SimpleOnGestureListener() {
        override fun onDoubleTap(e: MotionEvent): Boolean {
            modelViewer.destroyModel()
            createDefaultRenderables()
            return super.onDoubleTap(e)
        }
    }

}
//<?xml version="1.0" encoding="utf-8"?>
//<LindarLayout xmlns:android="http://schemas.android.com/apk/res/android"
//android:id="@+id/simple_layout"
//android:layout_width="match_parent"
//android:layout_height="match_parent"
//android:orientation="vertical">
//<TextView
//android:id="@+id/user_hint"
//android:layout_width="match_parent"
//android:layout_height="50dp"
//android:gravity="center center_horizontal center_vertical"
//android:text="https://google.github.io/filament/remote"
//android:textIsSelectable="true"
//android:textSize="18sp"
//android:textStyle="bold" />
//<SurfaceView
//android:id="@+id/main_sv"
//android:layout_width="match_parent"
//android:layout_height="0dp"
//android:layout_weight="1" />


@Composable
fun SurfaceViewWrapper() {
    AndroidView(
        factory = { context ->
            val linearLayout = LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
            }

            val textView = TextView(context).apply {
                id = R.id.user_hint
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    50
                ).apply {
                    gravity =
                        Gravity.CENTER_HORIZONTAL or Gravity.CENTER_VERTICAL
                }
                text = "https://google.github.io/filament/remote"
                textSize = 18f
                setTypeface(null, Typeface.BOLD)
                isClickable = true
                isFocusable = true
            }

            val surfaceView =
                SurfaceView(context).apply {  //To render a 3D model w have a Surface View
                    id = R.id.main_sv
                    layoutParams = LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        0
                    ).apply {
                        weight = 1f
                    }
                    // setup your SurfaceView here
                }

            linearLayout.addView(textView)
            linearLayout.addView(surfaceView)
            linearLayout
        },
        update = { surfaceView ->
            // update your SurfaceView here if necessary
        }
    )
}

/*@Composable
fun SurfaceViewWrapper() {
    AndroidView(
        factory = { context ->
            SurfaceView(context).apply {
                id = R.id.main_sv,
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    0
                ).apply {
                    this.height = 1f
                }
            }
        },
        update = { surfaceView ->
            // update your SurfaceView here if necessary
        }
    )
}*/

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "Hello $name!",
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    ArThreeDModelRotationTheme {
        Greeting("Android")
    }
}