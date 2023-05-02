package com.example.arthreedmodelrotation

import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import android.view.Choreographer
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.SurfaceView
import android.view.WindowManager
import android.widget.TextView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.viewinterop.AndroidView
import com.example.arthreedmodelrotation.ui.theme.ArThreeDModelRotationTheme
import com.google.android.filament.Fence
import com.google.android.filament.IndirectLight
import com.google.android.filament.Skybox
import com.google.android.filament.View
import com.google.android.filament.utils.AutomationEngine
import com.google.android.filament.utils.HDRLoader
import com.google.android.filament.utils.IBLPrefilterContext
import com.google.android.filament.utils.KTX1Loader
import com.google.android.filament.utils.ModelViewer
import com.google.android.filament.utils.RemoteServer
import com.google.android.filament.utils.Utils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.RandomAccessFile
import java.net.URI
import java.nio.Buffer
import java.nio.ByteBuffer
import java.nio.charset.StandardCharsets
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
    private var modelViewer: ModelViewer? = null
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

    @SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        choreographer = Choreographer.getInstance()

        setContent {
            ArThreeDModelRotationTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    SurfaceViewWrapper()
//
//                    Log.d(TAG, "!@# SURFACE VIEW onCreate: $surfaceView")
//                    modelViewer = ModelViewer(surfaceView)
//
////                    titleBarHint = window.findViewById(R.id.tvUserHint)
////                    surfaceView = window.findViewById(R.id.svSurfaceView)
//                    doubleTapDetector =
//                        GestureDetector(applicationContext, doubleTapListener)
//
//                    modelViewer?.let { modelViewer ->
//                        viewerContent.view = modelViewer.view
//                        viewerContent.sunlight = modelViewer.light
//                        viewerContent.lightManager = modelViewer.engine.lightManager
//                        viewerContent.scene = modelViewer.scene
//                        viewerContent.renderer = modelViewer.renderer
//                        surfaceView.setOnTouchListener { _, motionEvent ->
//                            modelViewer.onTouchEvent(motionEvent)
//                            doubleTapDetector.onTouchEvent(motionEvent)
//                            true
//                        }
//                    }

                    createDefaultRenderables()
                    createIndirectLight()

                    setStatusText("To load a new model, go to the above URL on your host machine.")

                    val view = modelViewer?.view
                    /*
                    * Note: The settings below are override when connecting to the remote UI.
                    */

                    view?.let { view ->
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
                            }
                    }
                }
            }
        }
    }

    //Here we'll load GLB/ GLTF model
    private fun createDefaultRenderables() {
        val buffer = assets.open("models/armchair_leather.glb").use { input ->
            val bytes = ByteArray(input.available())
            input.read(bytes)
            ByteBuffer.wrap(bytes)
        }

        modelViewer?.loadModelGltfAsync(buffer) { uri -> readCompressedAsset("models/$uri") }
        updateRootTransform()
    }

    private fun createIndirectLight() {
        val engine = modelViewer?.engine
        val scene = modelViewer?.scene
        val ibl = "default_env"
        readCompressedAsset("envs/$ibl/${ibl}_ibl.ktx").let {
            scene?.indirectLight = engine?.let { it1 -> KTX1Loader.createIndirectLight(it1, it) }
            scene?.indirectLight?.intensity = 30_000.0f
            viewerContent.indirectLight = modelViewer?.scene?.indirectLight
        }
        readCompressedAsset("envs/$ibl/${ibl}_skybox.ktx").let {
            scene?.skybox = engine?.let { it1 -> KTX1Loader.createSkybox(it1, it) }
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
                statusToast = Toast.makeText(applicationContext, text, Toast.LENGTH_SHORT)
                statusToast?.show()
                Log.d(TAG, "!@# setStatusText: $text")
            }
        }
    }

    private suspend fun loadGlb(message: RemoteServer.ReceivedMessage) {
        withContext(Dispatchers.Main) {
            modelViewer?.destroyModel()
            modelViewer?.loadModelGlb(message.buffer)
            updateRootTransform()
            loadStartTime = System.nanoTime()
            loadStartFence = modelViewer?.engine?.createFence()
        }
    }

    private suspend fun loadHdr(message: RemoteServer.ReceivedMessage) {
        withContext(Dispatchers.Main) {
            val engine = modelViewer?.engine
            val equirect = engine?.let { HDRLoader.createTexture(it, message.buffer) }
            if (equirect == null) {
                setStatusText("Could not decode HDR file.")
            } else {
                setStatusText("Successfully decoded HDR file.")

                val context = IBLPrefilterContext(engine)
                val equirectToCubemap = IBLPrefilterContext.EquirectangularToCubemap(context)
                val skyboxTexture = equirectToCubemap.run(equirect)
                engine.destroyTexture(equirect)

                val specularFilter = IBLPrefilterContext.SpecularFilter(context)
                val reflections = specularFilter.run(skyboxTexture)

                val ibl = IndirectLight.Builder()
                    .reflections(reflections)
                    .intensity(30000.0f)
                    .build(engine)

                val sky = Skybox.Builder().environment(skyboxTexture).build(engine)

                specularFilter.destroy()
                equirectToCubemap.destroy()
                context.destroy()

                // destroy the previous IBl
                modelViewer?.scene?.indirectLight?.let { engine.destroyIndirectLight(it) }
                modelViewer?.scene?.skybox?.let { engine.destroySkybox(it) }

                modelViewer?.scene?.skybox = sky
                modelViewer?.scene?.indirectLight = ibl
                viewerContent.indirectLight = ibl
            }
        }
    }

    private suspend fun loadZip(message: RemoteServer.ReceivedMessage) {
        withContext(Dispatchers.Main) {
            modelViewer?.destroyModel()
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

                val uri = entry.name
                val byteArray: ByteArray? = try {
                    deflater.readBytes()
                } catch (e: OutOfMemoryError) {
                    outOfMemory = uri
                    break
                }
                Log.i(TAG, "Deflated ${byteArray?.size} bytes from $uri")
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
        val gltfBuffer = pathToBufferMapping[gltfPath]

        // In a zip file, the gltf file might be in the same folder as resources, or in a different
        // folder. It is crucial to test against both of these cases. In any case, the resource
        // paths are all specified relative to the location of the gltf file.
        var prefix = URI(gltfPath).resolve(".")

        withContext(Dispatchers.Main) {
            if (gltfPath?.endsWith(".glb") == true) {
                gltfBuffer?.let { modelViewer?.loadModelGlb(it) }
            } else {
                gltfBuffer?.let {
                    modelViewer?.loadModelGltf(it) { uri ->
                        val path = prefix.resolve(uri).toString()
                        if (!pathToBufferMapping.contains(path)) {
                            Log.e(
                                TAG,
                                "Could not find '$uri' in zip using prefix '$prefix' and base path '${gltfPath}'"
                            )
                            setStatusText("Zip is missing $path")
                        }
                        pathToBufferMapping[path]
                    }
                }
            }
            updateRootTransform()
            loadStartTime = System.nanoTime()
            loadStartFence = modelViewer?.engine?.createFence()
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
        Log.i(TAG, "Downloaded model ${message.label} (${message.buffer.capacity()} bytes)")
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
        viewerContent.assetLights = modelViewer?.asset?.lightEntities
        modelViewer?.engine?.let { automation.applySettings(it, json, viewerContent) }
        modelViewer?.view?.colorGrading =
            modelViewer?.engine?.let { automation.getColorGrading(it) }
        modelViewer?.cameraFocalLength = automation.viewerOptions.cameraFocalLength
        modelViewer?.cameraNear = automation.viewerOptions.cameraNear
        modelViewer?.cameraFar = automation.viewerOptions.cameraFar
        updateRootTransform()
    }

    private fun updateRootTransform() {
        if (automation.viewerOptions.autoScaleEnabled) {
            modelViewer?.transformToUnitCube()
        } else {
            modelViewer?.clearRootTransform()
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
                    modelViewer?.engine?.destroyFence(it)
                    loadStartFence = null
                }
            }

            modelViewer?.animator?.apply {
                if (animationCount > 0) {
                    val elapsedTimeSeconds = (frameTimeNanos - startTime).toDouble() / 1_000_000_000
                    applyAnimation(0, elapsedTimeSeconds.toFloat())
                }
                updateBoneMatrices()
            }

            modelViewer?.render(frameTimeNanos)
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
            modelViewer?.destroyModel()
            createDefaultRenderables()
            return super.onDoubleTap(e)
        }
    }

    @Composable
    fun SurfaceViewWrapper() {
        AndroidView(
            factory = { context ->

                android.view.View.inflate(
                    context,
                    R.layout.surface_view_layout,
                    null
                )  //XML view is inflated to use inside Compose

//            val linearLayout = LinearLayout(context).apply {
//                orientation = LinearLayout.VERTICAL
//                layoutParams = ViewGroup.LayoutParams(
//                    ViewGroup.LayoutParams.MATCH_PARENT,
//                    ViewGroup.LayoutParams.MATCH_PARENT
//                )
//            }
//
//            val textView = TextView(context).apply {
//                id = R.id.user_hint
//                layoutParams = ViewGroup.LayoutParams(
//                    ViewGroup.LayoutParams.MATCH_PARENT,
//                    50
//                ).apply {
//                    gravity =
//                        Gravity.CENTER_HORIZONTAL or Gravity.CENTER_VERTICAL
//                }
//                text = "https://google.github.io/filament/remote"
//                textSize = 18f
//                setTypeface(null, Typeface.BOLD)
//                isClickable = true
//                isFocusable = true
//            }
//
//            val surfaceView =
//                SurfaceView(context).apply {  //To render a 3D model w have a Surface View
//                    id = R.id.main_sv
//                    layoutParams = LinearLayout.LayoutParams(
//                        ViewGroup.LayoutParams.MATCH_PARENT,
//                        0
//                    ).apply {
//                        weight = 1f
//                    }
//                    // setup your SurfaceView here
//                }
//
//            linearLayout.addView(textView)
//            linearLayout.addView(surfaceView)
//            linearLayout
            },
            modifier = Modifier.fillMaxSize(),
            update = {
                // update your SurfaceView here if necessary
//                Log.d(TAG, "!@# SURFACE VIEW SurfaceViewWrapper: viewParam before set => ${this.surfaceView}")
                val titleBarHint: TextView = it.findViewById(R.id.tvUserHint)
                val surfaceView: SurfaceView = it.findViewById(R.id.svSurfaceView)
                Log.d(TAG, "!@# SURFACE VIEW SurfaceViewWrapper: viewParam set => $surfaceView")

                modelViewer = ModelViewer(surfaceView)
                doubleTapDetector =
                    GestureDetector(applicationContext, doubleTapListener)

                modelViewer?.let { modelViewer ->
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
                }
            }
        )
    }


//    @Composable
//    fun SurfaceViewWrapper() {
//        val context = LocalContext.current
//        val titleBarHint = remember { mutableStateOf("") }
//        var surfaceView by remember { mutableStateOf<SurfaceView?>(null) }
//
//        AndroidView(factory = { ctx ->
//            val inflater = LayoutInflater.from(context)
//            val view = inflater.inflate(R.layout.surface_view_layout, null) as LinearLayout
//
//            // get references to the views inside the layout
//            titleBarHint.value = view.findViewById<TextView>(R.id.tvUserHint).text.toString()
//            surfaceView = view.findViewById<SurfaceView>(R.id.svSurfaceView)
//
//            view
//        }, update = {
//            // update your SurfaceView here if necessary
//        })
//
//        // use the titleBarHint and surfaceView variables as needed
//    }
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