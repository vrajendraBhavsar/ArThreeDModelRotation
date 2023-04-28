package com.example.arthreedmodelrotation

import android.content.Context
import android.net.Uri
import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.os.Bundle
import android.view.SurfaceView
import android.view.ViewGroup
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
import com.google.ar.sceneform.Node
import com.google.ar.sceneform.Scene
import com.google.ar.sceneform.SceneView
import com.google.ar.sceneform.math.Vector3
import com.google.ar.sceneform.rendering.ModelRenderable
import com.google.ar.sceneform.ux.TransformableNode
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ArThreeDModelRotationTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {

                }

//                Surface(
//                    modifier = Modifier.fillMaxSize(),
//                    color = MaterialTheme.colorScheme.background
//                ) {
//                    val intentUri =
//                        Uri.parse("https://arvr.google.com/scene-viewer/1.0").buildUpon()
//                            .appendQueryParameter("file", "" + "https://raw.githubusercontent.com/Sachinbhola/App-Templates/master/Resources/the_matrix_red_chesterfield_chair/scene.gltf")
//                            .appendQueryParameter("mode", "ar_only")
//                            .appendQueryParameter("title", "Manchester Chair")
//                            .build()
//                    ModelRenderable.builder()
//                        .setSource(
//                            this,
//                            intentUri
//                        )
//                        .build()
//                        .thenAccept { model ->
//                            // Pass the model to your Composable function
//                            setContent {
//                                SceneFormView(model)
//                            }
//                        }
//                        .exceptionally {
//                            // Handle any errors loading the model here
//                            Log.e("MainActivity", "Error loading model: ", it)
//                            null
//                        }
//                }
            }
        }
    }
}

@Composable
fun SurfaceViewWrapper() {
    AndroidView(
        factory = { context ->
            SurfaceView(context).apply {
                // setup your SurfaceView here
            }
        },
        update = { surfaceView ->
            // update your SurfaceView here if necessary
        }
    )
}

/*class MyGLSurfaceView(context: Context) : GLSurfaceView(context) {
    private val renderer = MyGLRenderer()

    init {
        setEGLContextClientVersion(2)
        setRenderer(renderer)
    }
}

class MyGLRenderer : RajawaliRenderer {
    private lateinit var model: LoaderGltf2
    private lateinit var camera: Camera

    override fun initScene() {
        // Set up the camera
        camera = Camera()
        camera.position = Vector3(0.0, 0.0, 10.0)
        camera.lookAt(Vector3.ZERO)

        // Load the .gltf file
        model = LoaderGltf2(this, R.raw.my_model)
        model.parse()

        // Add the model to the scene
        addChild(model)
    }

    override fun onRenderFrame(gl: GL10?, unused: Int) {
        // Update the camera and render the scene
        camera.yaw += 1.0
        camera.pitch += 0.5
        super.onRenderFrame(gl, unused)
    }
}

@Composable
fun MyScreen() {
    AndroidView(
        factory = { context ->
            MyGLSurfaceView(context).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
            }
        },
        modifier = Modifier.fillMaxSize()
    )
}

*//*class MyGLSurfaceView(context: Context) : GLSurfaceView(context) {
    private val renderer = MyGLRenderer()

    init {
        setEGLContextClientVersion(2)
        setRenderer(renderer)
    }
}

class MyGLRenderer : GLSurfaceView.Renderer {
    private lateinit var model: MyModel

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        // Load the 3D model here
        model = MyModel()
        model.load()

        // Set up OpenGL ES settings
        GLES20.glClearColor(0.0f, 0.0f, 0.0f, 1.0f)
        GLES20.glEnable(GLES20.GL_DEPTH_TEST)
        GLES20.glDepthFunc(GLES20.GL_LESS)
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        GLES20.glViewport(0, 0, width, height)
    }

    override fun onDrawFrame(gl: GL10?) {
        // Clear the screen
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT or GLES20.GL_DEPTH_BUFFER_BIT)

        // Draw the 3D model
        model.draw()
    }
}

@Composable
fun MyScreen() {
    AndroidView(
        factory = { context ->
            MyGLSurfaceView(context).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
            }
        },
        modifier = Modifier.fillMaxSize()
    )
}*//*

*//*@Composable
fun GltfModelView() {
    AndroidView(factory = { context ->
        // Create a SceneView and load the GLTF model
        val sceneView = SceneView(context)
        val gltfUri = Uri.parse("file:///android_asset/your_gltf_model.gltf")
        val renderableFuture = ModelRenderable.builder()
            .setSource(context, gltfUri)
            .build()
        *//**//*renderableFuture.thenAccept { renderable ->
            // Create a Scene and add the model to it
            val scene = Scene(sceneView)
            scene.addChild(renderable.createInstance())
        }*//**//*
        renderableFuture.thenAccept { renderable ->
            // Create a Scene and add the model to it
            val scene = Scene(sceneView)
//            val renderableInstance = renderable.createInstance(TransformableNode())

//            val transformationSystem = sceneView.makeTransformationSystem()
//            val renderableInstance = renderable.createInstance(TransformableNode(transformationSystem))

            val transformationSystem = GestureTransformationSystem()
            val renderableInstance = renderable.createInstance(TransformableNode(transformationSystem))


            val renderableRootNode = renderableInstance.renderable.get() as Node
            scene.addChild(renderableRootNode)
        }
        sceneView
    })
}*//*


*//*@Composable
fun ModelViewer() {
    var rotationAngle by remember { mutableStateOf(0f) }

    Box(modifier = Modifier.fillMaxSize()
        .rotate(degrees = rotationAngle)) {
        AndroidView(
            factory = { context ->
                ArFragment.newInstance().apply {
                    setOnViewCreatedListener { arFragment ->
                        val renderableFuture = ModelRenderable.builder()
                            .setSource(context, Uri.parse("model.usdz"))
                            .build()

                        renderableFuture.thenAccept { modelRenderable ->
                            val transformableNode = TransformableNode(arFragment.transformationSystem)
                            transformableNode.renderable = modelRenderable
                            transformableNode.setParent(arFragment.arSceneView.scene)
                            arFragment.arSceneView.scene.addChild(transformableNode)

                            // Rotate model by 10 degrees on each rotation gesture
                            arFragment.arSceneView.setOnTouchListener { _, motionEvent ->
                                val rotation = motionEvent.pointerInput().detectRotationGestures { _, degrees ->
                                    rotationAngle += degrees
                                }
                                rotationGestureFilter(rotation) {
                                    true
                                }
                            }
                        }
                    }
                }
            },
            modifier = Modifier.fillMaxSize()
        )
    }
}*//*

*//*@Composable
fun SceneFormView(model: ModelRenderable) {
    val context = LocalContext.current
    AndroidView(factory = {
        SceneView(context).apply {
            scene.addOnUpdateListener { frameTime ->
                // Update any animations or other scene changes here
            }
            val node = Node().apply {
                renderable = model
            }
            scene.addChild(node)
        }
    })
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