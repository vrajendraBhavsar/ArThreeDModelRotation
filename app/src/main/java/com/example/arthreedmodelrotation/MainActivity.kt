package com.example.arthreedmodelrotation

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.PixelFormat
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import android.os.Handler
import android.os.PersistableBundle
import android.util.Log
import android.view.MotionEvent
import android.view.OrientationEventListener
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInteropFilter
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.arthreedmodelrotation.UrlUtil.BASE_URL_SOFA_WITH_PROGRESS_BAR
import com.example.arthreedmodelrotation.ui.theme.ArThreeDModelRotationTheme
import org.the3deer.android_3d_model_engine.camera.CameraController
import org.the3deer.android_3d_model_engine.collision.CollisionController
import org.the3deer.android_3d_model_engine.collision.CollisionEvent
import org.the3deer.android_3d_model_engine.controller.TouchController
import org.the3deer.android_3d_model_engine.controller.TouchEvent
import org.the3deer.android_3d_model_engine.event.SelectedObjectEvent
import org.the3deer.android_3d_model_engine.model.Projection
import org.the3deer.android_3d_model_engine.services.LoaderTask
import org.the3deer.android_3d_model_engine.services.SceneLoader
import org.the3deer.android_3d_model_engine.view.FPSEvent
import org.the3deer.android_3d_model_engine.view.ModelSurfaceView
import org.the3deer.android_3d_model_engine.view.ViewEvent
import org.the3deer.util.android.ContentUtils
import org.the3deer.util.event.EventListener
import java.io.IOException
import java.net.URI
import java.net.URL
import java.util.EventObject
import kotlin.math.roundToInt


class MainActivity : ComponentActivity(), EventListener {
    private val REQUEST_CODE_LOAD_TEXTURE = 1000

    /**
     * Type of model if file name has no extension (provided though content provider)
     */
    private var paramType = 0

    /**
     * Background GL clear color. Default is light gray
     */
    private val backgroundColor = floatArrayOf(1.0f, 1.0f, 1.0f, 1.0f)
    private var glView: ModelSurfaceView? = null
    private var touchController: TouchController? = null
    private var scene: SceneLoader? = null

    //    private var gui: ModelViewerGUI? = null
    private var collisionController: CollisionController? = null
    private var handler: Handler? = null
    private var cameraController: CameraController? = null
    private var sensorManager: SensorManager? = null
    private var sensor: Sensor? = null

    @SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
    override fun onCreate(savedInstanceState: Bundle?) {

        URL.setURLStreamHandlerFactory { protocol ->
            if ("android" == protocol) {
                org.the3deer.util.android.assets.Handler()
            } else null
        }

        super.onCreate(savedInstanceState)
        setContent {
            ArThreeDModelRotationTheme {
                val mainViewModel: MainViewModel = viewModel()

                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    Column(modifier = Modifier.fillMaxSize()) {
                        val buttonColor = remember {
                            mutableStateOf("#FDC984")
                        }

                        Box(modifier = Modifier.fillMaxWidth()) {
                            Log.d("TAG", "!@# buttonColor: $buttonColor")
                            LoadModelFromAssets(buttonColor)
                            CameraControllerUpdater(mainViewModel.changePositionX.value)
                        }
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
//                                .background(Color.Red)
                        ) {
                            Row(Modifier.fillMaxWidth()) {
                                Button(
                                    onClick = { buttonColor.value = "#FDC984" },
                                    colors = ButtonDefaults.buttonColors(Color.Yellow)
                                ) {}
                                Button(
                                    onClick = { buttonColor.value = "#CBE6D2" },
                                    colors = ButtonDefaults.buttonColors(Color.Cyan)
                                ) {}
                                Button(
                                    onClick = { buttonColor.value = "#77C9DE" },
                                    colors = ButtonDefaults.buttonColors(Color.Blue)
                                ) {}
                            }
                        }
//                        SeekBar(mainViewModel)
                    }
                }
            }
        }
    }

    @OptIn(ExperimentalComposeUiApi::class)
    @Composable
    fun SeekBar(mainViewModel: MainViewModel) {
        val boxPosition = remember { mutableStateOf(0.5f) }
        val density = LocalDensity.current
        val screenWidthPx = with(density) { LocalConfiguration.current.screenWidthDp.dp.toPx() }
        val offsetX = (boxPosition.value * screenWidthPx).roundToInt()

        Box(modifier = Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier
                    .background(Color.LightGray)
                    .fillMaxWidth()
                    .height(4.dp)
            )
            Box(
                modifier = Modifier
                    .offset { IntOffset(offsetX, 0) }
                    .size(40.dp)
                    .background(Color.Blue)
                    .pointerInteropFilter {
                        when (it.action) {
                            MotionEvent.ACTION_DOWN -> {}
                            MotionEvent.ACTION_MOVE -> {
                                Log.d("TAG", "!@# SeekBar: it.x:: ${it.x}")
                                mainViewModel.changePositionX.value = it.x
                                Log.d(
                                    "TAG",
                                    "!@# SeekBar: VM it.x:: ${mainViewModel.changePositionX.value}"
                                )
                                boxPosition.value = it.x.coerceIn(0f, 1f)
                            }

                            MotionEvent.ACTION_UP -> {}
                            else -> false
                        }
                        true
                    }
            )
        }
    }

    @Composable
    fun CameraControllerUpdater(changePositionX: Float) {
        Log.i("ModelActivity", "!@# changePositionX:: $changePositionX")
        cameraController = CameraController(scene?.camera)
        cameraController?.updateTranslateCamera(changePositionX, this)
    }

    @Composable
    private fun LoadModelFromAssets(buttonColor: MutableState<String>?) {
        val TAG: String = MainActivity::class.java.simpleName
        val uri = URI(BASE_URL_SOFA_WITH_PROGRESS_BAR)
        handler = Handler(mainLooper)

//        val viewState = remember { mutableStateOf(0) }
//        var viewKey = remember { mutableStateOf(0) }
        var updatableString = remember { mutableStateOf("") }

        // Create our 3D scenario
        paramType = -1
        Log.i(
            "ModelActivity",
            "!@# paramUri => $uri, paramType => $paramType, buttonColor.value => ${buttonColor?.value}"
        )

//        val hexColorSofa = "#F78160"
//        val rgbArraySofa = hexToRgb(hexColorSofa)
        val rgbArraySofa = buttonColor?.value?.let {
            Log.d(TAG, "!@# LoadModelFromAssets: it => $it")
            hexToRgb(it)
        }
        Log.d(TAG, "!@# rgbArray: ${rgbArraySofa.contentToString()}")

        Log.i("ModelActivity", "Loading Scene...")
        scene = SceneLoader(this@MainActivity, uri, paramType, rgbArraySofa)
        scene?.addListener(this@MainActivity)

        //FIXME: This "if" condition is simply to render default UI in case of uri failure
        if (uri == null) {
            Log.d("TAG", "!@# loadModelFromAssets: $uri")
            val task: LoaderTask = DemoLoaderTask(this@MainActivity, null, scene)
            Log.d("TAG", "!@# loadModelFromAssets LoaderTask: $task")
            task.execute()
        }

        try {
            Log.i("ModelActivity", "!@# Loading GLSurfaceView...scene:: $scene")
            glView = ModelSurfaceView(this@MainActivity, backgroundColor, scene)
            glView?.addListener(this@MainActivity)
//            isLoading.value = false
        } catch (e: Exception) {
            Log.e("ModelActivity", "!@# " + e.message, e)
//            errorMessage.value = "Error loading OpenGL view: ${e.message}"
//            isLoading.value = false
        }

        val isLoading = remember { mutableStateOf(true) }
        val errorMessage = remember { mutableStateOf("") }

//        LaunchedEffect(key1 = Unit) {
        try {
            Log.i("ModelActivity", "!@# Loading GLSurfaceView...")
            glView = ModelSurfaceView(this@MainActivity, backgroundColor, scene)
            glView?.addListener(this@MainActivity)
            isLoading.value = false
        } catch (e: Exception) {
            Log.e("ModelActivity", "!@# " + e.message, e)
            errorMessage.value = "Error loading OpenGL view: ${e.message}"
            isLoading.value = false
        }
//        }

//        if (isLoading.value) {
//            // Show loading indicator or placeholder content
//            CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
//        } else {
//            if (errorMessage.value.isNotEmpty()) {
//                // Show error message
//                Toast.makeText(this@MainActivity, errorMessage.value, Toast.LENGTH_LONG).show()
//            } else {
        // Show the GLSurfaceView
        var property = remember { mutableStateOf("") }
        AndroidView(
            factory = { context ->
                try {
                    Log.i("ModelActivity", "!@# Loading CameraController...")
//                            Log.i("ModelActivity", "!@# changePositionX:: $changePositionX")
//                            cameraController = CameraController(scene?.camera, changePositionX)
                    Log.d(TAG, "!@# onCreate: cameraController: " + scene?.camera)
                    Log.d(TAG, "!@# onCreate: cameraController OBJ: $cameraController")

                    Log.d(TAG, "!@# LoadModelFromAssets: ${rgbArraySofa.contentToString()}")

                    //glView.getModelRenderer().addListener(cameraController);
                    //touchController.addListener(cameraController);
                } catch (e: java.lang.Exception) {
                    Log.e("ModelActivity", "!@# Loading CameraController..." + e.message, e)
                    Toast.makeText(
                        this@MainActivity,
                        "Error loading CameraController" + e.message,
                        Toast.LENGTH_LONG
                    ).show()
                }
                buttonColor?.let { property = it }

                glView?.setZOrderOnTop(true)
                glView?.holder?.setFormat(PixelFormat.TRANSLUCENT)
                glView!!
            },
            modifier = Modifier
                .size(width = 400.dp, height = 500.dp)
                .focusable(true),
            update = {
                property = updatableString

                Log.i("ModelActivity", "!@# Loading CameraController UPDATE...")
                glView?.setZOrderOnTop(true)
                glView?.holder?.setFormat(PixelFormat.TRANSLUCENT)
                glView!!

                glView?.addListener(this@MainActivity)
            }
        )
//            }
//        }

        /*try {
            Log.i("ModelActivity", "!@# Loading GLSurfaceView...")
            glView = ModelSurfaceView(this, backgroundColor, scene)
//            glView?.toggleLights()  //To turn OFF the light
            glView?.addListener(this)
//            setContentView(glView)

            AndroidView(factory = { context ->
                glView?.setZOrderOnTop(true)
                glView?.holder?.setFormat(PixelFormat.TRANSLUCENT)
                glView!!
            })

//            scene.setView(glView);
        } catch (e: Exception) {
            Log.e("ModelActivity", "!@# " + e.message, e)
            Toast.makeText(this, "Error loading OpenGL view: ${e.message}", Toast.LENGTH_LONG)
                .show()
        }*/

        try {
            Log.i("ModelActivity", "!@# Loading TouchController...")
            touchController = TouchController(this@MainActivity)
            touchController?.addListener(this@MainActivity)
            Log.d(TAG, "!@# onCreate: touchController init")
//            touchController?.addListener(glView);
        } catch (e: java.lang.Exception) {
            Log.e("ModelActivity", "!@#" + e.message, e)
            Toast.makeText(
                this@MainActivity,
                "Error loading TouchController: ${e.message}",
                Toast.LENGTH_LONG
            )
                .show()
        }
        try {
            Log.i("ModelActivity", "!@# Loading CollisionController...")
            collisionController = CollisionController(glView, scene)
            collisionController?.addListener(this@MainActivity)
            //touchController.addListener(collisionController);
            //touchController.addListener(scene);
        } catch (e: java.lang.Exception) {
            Log.e("ModelActivity", "!@#" + e.message, e)
            Toast.makeText(
                this@MainActivity,
                "Error loading CollisionController: ${e.message}",
                Toast.LENGTH_LONG
            ).show()
        }

        /*try {
            Log.i("ModelActivity", "!@# Loading CameraController...")
            cameraController = CameraController(scene?.camera, changePositionX)
            Log.d(TAG, "!@# onCreate: cameraController: " + scene?.camera)
            Log.d(TAG, "!@# onCreate: cameraController OBJ: $cameraController")

            //glView.getModelRenderer().addListener(cameraController);
            //touchController.addListener(cameraController);
        } catch (e: java.lang.Exception) {
            Log.e("ModelActivity", "!@# Loading CameraController..." + e.message, e)
            Toast.makeText(
                this@MainActivity,
                "Error loading CameraController" + e.message,
                Toast.LENGTH_LONG
            ).show()
        }*/

        try {
            // TODO: finish UI implementation
            Log.i("ModelActivity", "!@# Loading GUI...")
//            gui = glView?.let { gui ->
//                scene?.let { scene ->
//                    ModelViewerGUI(gui, scene)
//                }
//            }
//            touchController?.addListener(gui)
//            Log.d(TAG, "!@# onCreate: gui + touchController: $gui")
//            glView?.addListener(gui)
//            scene?.addGUIObject(gui)
        } catch (e: java.lang.Exception) {
            Log.e("ModelActivity", "!@#" + e.message, e)
            Toast.makeText(this@MainActivity, "Error loading GUI" + e.message, Toast.LENGTH_LONG)
                .show()
        }

        setupOrientationListener()

        // load model
        scene?.init()

        Log.i("ModelActivity", "!@# Finished loading")
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (resultCode != RESULT_OK) {
            return
        }
        when (requestCode) {
            REQUEST_CODE_LOAD_TEXTURE -> {
                // The URI of the selected file
                val uri = data?.data
                if (uri != null) {
                    Log.i("ModelActivity", "!@# Loading texture '$uri'")
                    try {
                        ContentUtils.setThreadActivity(this)
                        scene?.loadTexture(null, uri)
                    } catch (ex: IOException) {
                        Log.e("ModelActivity", "!@# Error loading texture: " + ex.message, ex)
                        Toast.makeText(
                            this, "Error loading texture '$uri'. " + ex
                                .message, Toast.LENGTH_LONG
                        ).show()
                    } finally {
                        ContentUtils.setThreadActivity(null)
                    }
                }
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle, outPersistentState: PersistableBundle) {
        outState.putFloatArray("camera.pos", scene?.camera?.pos)
        outState.putFloatArray("camera.view", scene?.camera?.view)
        outState.putFloatArray("camera.up", scene?.camera?.up)
        outState.putString("renderer.projection", glView?.projection?.name)
        glView?.skyBoxId?.let { outState.putInt("renderer.skybox", it) }
    }

    override fun onRestoreInstanceState(state: Bundle) {
        if (state.containsKey("renderer.projection")) {
            glView?.projection = state.getString("renderer.projection")
                ?.let { Projection.valueOf(it) }
        }
        if (state.containsKey("camera.pos") && state.containsKey("camera.view") && state.containsKey(
                "camera.up"
            )
        ) {
            Log.d("ModelActivity", "!@# onRestoreInstanceState: Restoring camera settings...")
            scene?.camera?.set(
                state.getFloatArray("camera.pos"),
                state.getFloatArray("camera.view"),
                state.getFloatArray("camera.up")
            )
        }
    }

    private fun setupOrientationListener() {
        try {
            //setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
            sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
            //sensor = sensorManager.getDefaultSensor(Sensor.TYPE_GEOMAGNETIC_ROTATION_VECTOR);
            sensor = sensorManager?.getDefaultSensor(Sensor.TYPE_GAME_ROTATION_VECTOR)
            if (sensor != null) {
                sensorManager?.registerListener(
                    object : SensorEventListener {
                        override fun onSensorChanged(event: SensorEvent) {
                            /*Log.v("ModelActivity","sensor: "+ Arrays.toString(event.values));
                                                           Quaternion orientation = new Quaternion(event.values);
                                                           orientation.normalize();
                                                           //scene.getSelectedObject().setOrientation(orientation);
                                                           glView.setOrientation(orientation);*/
                        }

                        override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {}
                    }, sensor,
                    SensorManager.SENSOR_DELAY_NORMAL, SensorManager.SENSOR_DELAY_UI
                )
            }
            val mOrientationListener: OrientationEventListener = object : OrientationEventListener(
                applicationContext
            ) {
                override fun onOrientationChanged(orientation: Int) {
                    //scene.onOrientationChanged(orientation);
                }
            }
            if (mOrientationListener.canDetectOrientation()) {
                mOrientationListener.enable()
            }
        } catch (e: java.lang.Exception) {
            Log.e("ModelActivity", "There is an issue setting up sensors", e)
        }
    }

    override fun onEvent(event: EventObject?): Boolean {
        if (event is FPSEvent) {
            Log.d("TAG", "!@# onEvent: FPSEvent ==> $event")
//            gui?.onEvent(event)
        } else if (event is SelectedObjectEvent) {
            Log.d("TAG", "!@# onEvent: SelectedObjectEvent ==> $event")
//            gui?.onEvent(event)
        } else if (event?.source is MotionEvent) {
            // event coming from glview
            Log.d("TAG", "!@# onEvent: MotionEvent ==> $event")
            touchController?.onMotionEvent(event.source as MotionEvent)
        } else if (event is CollisionEvent) {
            Log.d("TAG", "!@# onEvent: CollisionEvent ==> $event")
            scene?.onEvent(event)
        } else if (event is TouchEvent) {
            if (event.action == TouchEvent.Action.CLICK) {
                if (collisionController?.onEvent(event)?.not() == true) {
                    Log.d(
                        "TAG",
                        "!@# onEvent: Action.CLICK ==> $event"
                    )
                    scene?.onEvent(event)
                }
                collisionController?.let { controller ->
                    if (!controller.onEvent(event)) {
                        scene?.onEvent(event)
                    }
                }
            } else {
                if (scene?.selectedObject != null) {
                    Log.d(
                        "TAG",
                        "!@# onEvent: getSelectedObject ==> $event"
                    )
                    scene?.onEvent(event)
                } else {
                    Log.d(
                        "TAG",
                        "!@# onEvent:getSelectedObject == null ==> $event"
                    )
                    cameraController?.onEvent(event)
                    scene?.onEvent(event)
                    if (event.action == TouchEvent.Action.PINCH) {
                        Log.d(
                            "TAG",
                            "!@# onEvent: TouchEvent.Action.PINCH) ==> $event"
                        )
                        glView?.onEvent(event)
                    }
                }
            }
        } else if (event is ViewEvent) {
            if (event.code == ViewEvent.Code.SURFACE_CHANGED) {
                Log.d(
                    "TAG",
                    "!@# onEvent: SURFACE_CHANGED) ==> $event"
                )
                cameraController?.onEvent(event)
                touchController?.onEvent(event)

                // process event in GUI
//                if (gui != null) {
//                    gui?.setSize(event.width, event.height)
//                    Log.d("TAG", "!@# onEvent: gui.setSize) Width ==> " + event.width)
//                    gui?.isVisible = true
//                }
            } else if (event.code == ViewEvent.Code.PROJECTION_CHANGED) {
                cameraController?.onEvent(event)
            }
        }
        return true
    }

    private fun hexToRgb(hex: String): FloatArray {
        val hexValue = hex.replace("#", "") // Remove the '#' character if present
        val r = hexValue.substring(0, 2).toInt(16) / 255f
        val g = hexValue.substring(2, 4).toInt(16) / 255f
        val b = hexValue.substring(4, 6).toInt(16) / 255f
        return floatArrayOf(r, g, b)
    }
}