package com.example.arthreedmodelrotation

import android.annotation.SuppressLint
import android.content.Intent
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
import android.view.View
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
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

class MainActivity : ComponentActivity(), EventListener {

    private val REQUEST_CODE_LOAD_TEXTURE = 1000
    private val FULLSCREEN_DELAY = 10000L

    /**
     * Type of model if file name has no extension (provided though content provider)
     */
    private var paramType = 0

    /**
     * The file to load. Passed as input parameter
     */
//    private var paramUri: URI? = null

    /**
     * Enter into Android Immersive mode so the renderer is full screen or not
     */
    private var immersiveMode = false

    /**
     * Background GL clear color. Default is light gray
     */
    private val backgroundColor = floatArrayOf(0.0f, 0.0f, 0.0f, 1.0f)

    private var glView: ModelSurfaceView? = null
    private var touchController: TouchController? = null
    private var scene: SceneLoader? = null

    private var gui: ModelViewerGUI? = null
    private var collisionController: CollisionController? = null


    private var handler: Handler? = null
    private var cameraController: CameraController? = null

    private var sensorManager: SensorManager? = null
    private var sensor: Sensor? = null

//    private val REQUEST_CODE_LOAD_MODEL = 1101
//    private val REQUEST_CODE_OPEN_MATERIAL = 1102
//    private val REQUEST_CODE_OPEN_TEXTURE = 1103
//    private val REQUEST_CODE_ADD_FILES = 1200
//    private val SUPPORTED_FILE_TYPES_REGEX = "(?i).*\\.(obj|stl|dae|gltf|index)"
//    private val loadModelParameters = HashMap<String, Any>()

    @SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
    override fun onCreate(savedInstanceState: Bundle?) {

        // final List<MeshData> allMeshes = new ArrayList<>();
        URL.setURLStreamHandlerFactory { protocol ->
            if ("android" == protocol) {
                org.the3deer.util.android.assets.Handler()
            } else null
        }

        super.onCreate(savedInstanceState)
        setContent {
            ArThreeDModelRotationTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
//                    Box(
//                        modifier = Modifier
//                            .fillMaxSize()
//                            .background(Color.Black)
//                    ) {
                    loadModelFromAssets()
//                    }
                }
            }
        }
    }

    //to get proper URI format like - android://org.andresoviedo.dddmodel2/assets/models/teapot.obj
    private fun loadModelFromAssets() {
        val TAG: String = MainActivity::class.java.simpleName

//        val baseUrl = "file:///android_asset/models/Avocado.gltf"
        val baseUrl = "android://${packageName}/assets/models/Avocado.gltf"
        val uri = URI(baseUrl)
        Log.i("ModelActivity", "!@# URI(baseUrl) => ${URI(baseUrl)}")
        Log.i("ModelActivity", "!@# uri => $uri")

//        paramUri = Uri.parse(URI(baseUrl).toString())
//        paramUri = Uri.parse(baseUrl)
        handler = Handler(mainLooper)

        // Create our 3D scenario
        Log.i("ModelActivity", "Loading Scene...")
        Log.i("ModelActivity", "!@# paramUri => $uri, paramType => $paramType")

        paramType = -1
        scene = SceneLoader(this, uri, paramType)
        scene?.addListener(this)

        //FIXME: This "if" condition is simply to render default UI in case of uri failure
        if (uri == null) {
            Log.d("TAG", "!@# loadModelFromAssets: $uri")
            val task: LoaderTask = DemoLoaderTask(this, null, scene)
            task.execute()
        }


        try {
            Log.i("ModelActivity", "!@# Loading GLSurfaceView...")
            glView = ModelSurfaceView(this, backgroundColor, scene)
            glView?.addListener(this)
            setContentView(glView)
//            scene.setView(glView);
        } catch (e: Exception) {
            Log.e("ModelActivity", "!@# " + e.message, e)
            Toast.makeText(this, "Error loading OpenGL view: ${e.message}", Toast.LENGTH_LONG)
                .show()
        }

        try {
            Log.i("ModelActivity", "!@# Loading TouchController...")
            touchController = TouchController(this)
            touchController?.addListener(this)
            Log.d(TAG, "!@# onCreate: touchController init")
//            touchController?.addListener(glView);
        } catch (e: java.lang.Exception) {
            Log.e("ModelActivity", "!@#" + e.message, e)
            Toast.makeText(this, "Error loading TouchController: ${e.message}", Toast.LENGTH_LONG)
                .show()
        }
        try {
            Log.i("ModelActivity", "!@# Loading CollisionController...")
            collisionController = CollisionController(glView, scene)
            collisionController?.addListener(this)
            //touchController.addListener(collisionController);
            //touchController.addListener(scene);
        } catch (e: java.lang.Exception) {
            Log.e("ModelActivity", "!@#" + e.message, e)
            Toast.makeText(
                this,
                "Error loading CollisionController: ${e.message}",
                Toast.LENGTH_LONG
            ).show()
        }

        try {
            Log.i("ModelActivity", "!@# Loading CameraController...")
            cameraController = CameraController(scene?.camera)
            Log.d(TAG, "!@# onCreate: cameraController: " + scene?.camera)
            Log.d(TAG, "!@# onCreate: cameraController OBJ: $cameraController")

            //glView.getModelRenderer().addListener(cameraController);
            //touchController.addListener(cameraController);
        } catch (e: java.lang.Exception) {
            Log.e("ModelActivity", "!@# Loading CameraController..." + e.message, e)
            Toast.makeText(this, "Error loading CameraController" + e.message, Toast.LENGTH_LONG)
                .show()
        }

        try {
            // TODO: finish UI implementation
            Log.i("ModelActivity", "!@# Loading GUI...")
            gui = glView?.let { gui ->
                scene?.let { scene ->
                    ModelViewerGUI(gui, scene)
                }
            }
            touchController?.addListener(gui)
            Log.d(TAG, "!@# onCreate: gui + touchController: $gui")
            glView?.addListener(gui)
            scene?.addGUIObject(gui)
        } catch (e: java.lang.Exception) {
            Log.e("ModelActivity", "!@#" + e.message, e)
            Toast.makeText(this, "Error loading GUI" + e.message, Toast.LENGTH_LONG).show()
        }

        setupOnSystemVisibilityChangeListener()

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
        if (state.containsKey("renderer.skybox")) {
            glView?.setSkyBox(state.getInt("renderer.skybox"))
        }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            hideSystemUIDelayed()
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

    private fun setupOnSystemVisibilityChangeListener() {
        window.decorView.setOnSystemUiVisibilityChangeListener { visibility: Int ->
            // Note that system bars will only be "visible" if none of the
            // LOW_PROFILE, HIDE_NAVIGATION, or FULLSCREEN flags are set.
            if (visibility and View.SYSTEM_UI_FLAG_FULLSCREEN == 0) {
                // The system bars are visible. Make any desired
                hideSystemUIDelayed()
            }
        }
    }

    private fun hideSystemUIDelayed() {
        if (!immersiveMode) {
            return
        }
        handler?.removeCallbacksAndMessages(null)
        handler?.postDelayed(
            { this.hideSystemUI() },
            FULLSCREEN_DELAY
        )
    }

    private fun hideSystemUI() {
        if (!immersiveMode) {
            return
        }
        hideSystemUIKitKat()
    }

    private fun hideSystemUIKitKat() {
        // Set the IMMERSIVE flag.
        // Set the content to appear under the system bars so that the content
        // doesn't resize when the system bars hide and show.
        val decorView = window.decorView
        decorView.systemUiVisibility =
            (View.SYSTEM_UI_FLAG_LAYOUT_STABLE or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION // hide nav bar
                    or View.SYSTEM_UI_FLAG_FULLSCREEN // hide status bar
                    or View.SYSTEM_UI_FLAG_IMMERSIVE)
    }

//    private fun hideSystemUIJellyBean() {
//        val decorView = window.decorView
//        decorView.systemUiVisibility =
//            (View.SYSTEM_UI_FLAG_LAYOUT_STABLE or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
//                    or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
//                    or View.SYSTEM_UI_FLAG_FULLSCREEN or View.SYSTEM_UI_FLAG_LOW_PROFILE)
//    }

    override fun onEvent(event: EventObject?): Boolean {
        if (event is FPSEvent) {
            Log.d("TAG", "!@# onEvent: FPSEvent ==> $event")
            gui?.onEvent(event)
        } else if (event is SelectedObjectEvent) {
            Log.d("TAG", "!@# onEvent: SelectedObjectEvent ==> $event")
            gui?.onEvent(event)
        } else if (event?.source is MotionEvent) {
            // event coming from glview

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
                if (gui != null) {
                    gui?.setSize(event.width, event.height)
                    Log.d("TAG", "!@# onEvent: gui.setSize) Width ==> " + event.width)
                    gui?.isVisible = true
                }
            } else if (event.code == ViewEvent.Code.PROJECTION_CHANGED) {
                cameraController?.onEvent(event)
            }
        }
        return true
    }

    private fun toggleImmersive() {
        immersiveMode = !immersiveMode
        if (immersiveMode) {
            hideSystemUI()
        } else {
            showSystemUI()
        }
        Toast.makeText(this, "Fullscreen $immersiveMode", Toast.LENGTH_SHORT).show()
    }

    private fun showSystemUI() {
        handler?.removeCallbacksAndMessages(null)
        val decorView = window.decorView
        decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_VISIBLE
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        if (immersiveMode) {
            toggleImmersive()
        } else {
            super.onBackPressed()
        }
    }
}