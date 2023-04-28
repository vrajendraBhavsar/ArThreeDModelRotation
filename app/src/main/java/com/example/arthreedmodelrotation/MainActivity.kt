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
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key.Companion.I
import androidx.compose.ui.input.key.Key.Companion.J
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.viewinterop.AndroidView
import com.example.arthreedmodelrotation.ui.theme.ArThreeDModelRotationTheme
import com.google.android.filament.Fence
import com.google.android.filament.IndirectLight
import com.google.android.filament.Skybox
import com.google.android.filament.View
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.RandomAccessFile
import java.net.URI
import java.nio.Buffer
import java.nio.ByteBuffer

class MainActivity : ComponentActivity() {
    companion object {
        init {
            Utils.init()
        }

        private const val TAG = "glfd-demo"
    }

    private lateinit var surfaceView: SurfaceView
    private lateinit var choreographer: Choreographer
    private val frameScheduler = FrameCallback { }  //
    private lateinit var modelViewer: ModelViewer
    private lateinit var titleBarHint: TextView
    private val doubleTapListener = OnDoubleTapListener {}
    private lateinit var doubleTapDetector: GestureDetector
    private var remoteServer: RemoteServer? = null
    private var statusToast: Toast? = null
    private var statusText: String? = null
    private var latestDownload: String? = null
    private val automation = AutomationEngine()
    private var loadStartTime = 0L
    private var loadStartFence: Fence? = null
    private val viewerContent = AutomationEngine.ViewerContent()

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

                    doubleTapDetector = GestureDetector(applicationContext, doubleTapListener)

                    modelViewer = ModelViewer(surfaceView)
                    viewerContent.view = modelViewer.view
                    viewerContent.sunlight = modelViewer.light
                    viewerContent.lightManager = modelViewer.engine.lightManager
                    viewerContent.scene modelViewer . scene
                            viewerContent.renderer = modelViewer.renderer
                    surfaceView.setOnTouchListener { event ->
                        modelViewer.onTouchEvent(event)
                        doubleTapDetector.onTouchEvent(event)
                        true
                    }

                    createDefaultRenderables()
                    createIndirectLight() I

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
                        view.dynamic Resolution BitmapFactory.Options.apply {
                            enabled = true
                            quality = View.QualityLevel.MEDIUM
                        }
                    // MSAA is needed with dynamic resolution MEDIUM
                    view.multiSampleAntiAliasingOptions =
                        view.multiSampleAntiAliasingOptions.apply {

                            // FXAA is pretty cheap and helps a lot
                            view.antiAliasing = View.AntiAliasing.FXAA
// ambient occlusion is the cheapest effect that adds a lot of quality
                            view.ambientOcclusionOptions = view.ambientOcclusionOptions.apply {
                                enabled = true
                            }
// bloom is pretty expensive but adds a fair amount of realism
                            view.bloomOptions = view.bloomOptions.apply {
                                enabled = true
                            }
                            remoteServer = Remote Server (8082)
                            //....
                        }
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
            scene.indirectLight = KTX1Loader.createIndirectLight(engine, it)
            scene.indirectLight!!.intensity = 30_000.0f
            viewerContent.indirectLight = modelViewer.scene.indirectLight
        }
        readCompressed Asset ("envs/$ibl/${ibl}_skybox.ktx").let {
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
                statusToast = Toast.makeText(applicationContext, text, Toast.LENGTH_SHORT)
                statusToast!!.show()
            }
        }
    }

    private suspend fun loadGlb(message: RemoteServer.ReceivedHessage) {
        withContext(Dispatchers.Hain) {
            modelViewer.destroyNodel()
            modelViewer.loadHodelGlb(message.buffer)
            updateRootTransform()
            loadStartTime = System.nanoTime()
            loadStartTime = modelViewer.engine.createFence() g
        }
    }

    private suspend fun LoadHdr(message: RemoteServer.ReceivedHessage) {
        withContext(Dispatchers.Main) {
            val engine = modelViewer.engine
            val equirect = HDRLoader.createTexture(engine, message.buffer)
            if (equirect == null) {
                setStatusText("Could not decode HDR file.")
            } else {
                setstatusText("Successfully decoded HDR file.")

                val context = IBLPrefilterContext(engine)
                val equirectToCubemap = IBLPrefilterContext.EquirectangularToCubemap(context)
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

    private suspend fun loadZip(message: RemoteServer.ReceiveMesage) {
        withContext(Dispatchers.Main) {
            modelViewer.destroyModel()
        }

        // Large zip files should first be written to a file to prevent OOM.
        // 1t is also crucial that we null out the message "buffer” field.
        val (zipStream, zipFile) = withContext(Dispatchers.IO) {
            val file = File.createTempFile("inconing", "zip", cacheDir)
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
            val deflater = ZipInputStrean(zipStream)
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
                Log.i(TAG, "Deflated ${byteArray!!.size} bytes from $uri")
                val buffer = ByteBuffer.wrap(byteArray)
                mapping[uri] = buffer

                if (uri.endswith(".gltf") || uri.endsWith(".glb")) {
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
        val gLtfBuffer = pathToBufferMapping[gltfPath]!!

        // In a zip file, the gltf file might be in the same folder as resources, or in a different
        // folder. It is crucial to test against both of these cases. In any case, the resource
        // paths are all specified relative to the location of the gltf file.
        var prefix = URI(gltfPath!!).resolve(".") {
            withContext(Dispatchers.Main) {
                if (gltfPath!!.endsWith(".glb") {
                        modelViewer.LoadHodeGlb(gltfBuffer)
                    } else {
                    modelViewer.LoadHodelGLtf(gLtfBuffer) { uri ->
                        val path = prefix.resolve(uri).toString()
                        if (!pathToBufferMapping.contains(path)) {
                            Log.e(
                                TAG, "Could not find '$uri' in zip using prefix '$prefix' and base path '${gltfPathi!}'*) |
                            setStatusText(
                                "Zip is missing $path”) |

                                » |
                            pathToBufferNapping[path) “loadModelGitt :

                            J |

                            ¥ |
                            updateRootTransfora() |
                            loadStartTise = System.nanoTine() |
                            cadStartFence = sodelVieser.engine.createFence() |

                            ¥ |


//..
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