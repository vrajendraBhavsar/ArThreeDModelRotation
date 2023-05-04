package com.example.arthreedmodelrotation

import android.annotation.SuppressLint
import android.hardware.Sensor
import android.hardware.SensorManager
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.example.arthreedmodelrotation.ui.theme.ArThreeDModelRotationTheme
import org.the3deer.android_3d_model_engine.camera.CameraController
import org.the3deer.android_3d_model_engine.collision.CollisionController
import org.the3deer.android_3d_model_engine.controller.TouchController
import org.the3deer.android_3d_model_engine.services.SceneLoader
import org.the3deer.android_3d_model_engine.view.ModelSurfaceView
import org.the3deer.util.android.AssetUtils
import org.the3deer.util.android.ContentUtils
import java.io.IOException
import java.io.InputStream
import java.net.URI

class MainActivity : ComponentActivity() {

    private val REQUEST_CODE_LOAD_TEXTURE = 1000
    private val FULLSCREEN_DELAY = 10000

    /**
     * Type of model if file name has no extension (provided though content provider)
     */
    private val paramType = 0

    /**
     * The file to load. Passed as input parameter
     */
    private val paramUri: URI? = null

    /**
     * Enter into Android Immersive mode so the renderer is full screen or not
     */
    private val immersiveMode = false

    /**
     * Background GL clear color. Default is light gray
     */
    private val backgroundColor = floatArrayOf(0.0f, 0.0f, 0.0f, 1.0f)

    private val glView: ModelSurfaceView? = null
    private val touchController: TouchController? = null
    private val scene: SceneLoader? = null
//    private val gui: ModelViewerGUI? = null
    private val collisionController: CollisionController? = null


    private val handler: Handler? = null
    private val cameraController: CameraController? = null

    private val sensorManager: SensorManager? = null
    private val sensor: Sensor? = null

    private val REQUEST_CODE_LOAD_MODEL = 1101
    private val REQUEST_CODE_OPEN_MATERIAL = 1102
    private val REQUEST_CODE_OPEN_TEXTURE = 1103
    private val REQUEST_CODE_ADD_FILES = 1200
    private val SUPPORTED_FILE_TYPES_REGEX = "(?i).*\\.(obj|stl|dae|gltf|index)"
    private val loadModelParameters = HashMap<String, Any>()

    @SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ArThreeDModelRotationTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    Box(modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black)) {
                        loadModelFromAssets()
                    }
                }
            }
        }
    }

    //to get proper URI format like - android://org.andresoviedo.dddmodel2/assets/models/teapot.obj
    private fun loadModelFromAssets() {
//        val baseUrl = "file:///android_asset/models/Avocado.gltf"
        val baseUrl = "android://${packageName}/assets/models/Avocado.gltf"

        /*var inputStream: InputStream? = null
        try {
            inputStream = assets.open("models/Avocado.gltf")
            val size = inputStream.available()
            val buffer = ByteArray(size)
            inputStream.read(buffer)
            val modelString = String(buffer)

            val filePath = "$baseUrl/$modelString"
            Log.d("TAG", "!@# loadModelFromAssets: modelString => $modelString, filePath => $filePath")
        } catch (e: IOException) {
            e.printStackTrace()
        } finally {
            inputStream?.close()
        }*/

//.............................................................................. MenuActivity code

        /*if (file != null) {
            ContentUtils.provideAssets(this)
            launchModelRendererActivity(Uri.parse("android://$packageName/assets/$file"))
        }*/
        /*AssetUtils.createChooserDialog(
            this,
            "Select file",
            null,
            "models",
            SUPPORTED_FILE_TYPES_REGEX
        ) { file: String? ->
            var baseUrl = "file://assets/models/"

            val inputStream : InputStream = assets.open("Avocado.gltf")
            val size = inputStream.available()
            val buffer = ByteArray(size)
            inputStream.read(buffer)
            val modelString = String(buffer)

            val filePath = "$baseUrl/$modelString"
            Log.d("TAG", "!@# loadModelFromAssets: modelString => $modelString, filePath => $filePath")

            *//*if (file != null) {
                ContentUtils.provideAssets(this)
                launchModelRendererActivity(Uri.parse("android://$packageName/assets/$file"))
            }*//*
        }*/
    }

//    private fun loadModelFromAssets() {
//        AssetUtils.createChooserDialog(this, "Select file", null, "models", SUPPORTED_FILE_TYPES_REGEX
//        ) { file: String? ->
//            if (file != null) {
//                ContentUtils.provideAssets(this)
//                val uri: Uri? = Uri.parse("android://$packageName/assets/$file")
//                Log.i("Menu", "loadModelFromAssets URI $uri")
//
//                val uriIntent = uri.toString()
//                val immersiveMode = false
//                val type = loadModelParameters["type"].toString()
//
//
//                // save user selected model
//                uri?.let { loadModelParameters.put("model", it) }
//
//
//                launchModelRendererActivity(Uri.parse("android://$packageName/assets/$file"))
//            }
//        }
//    }
//
//    @Throws(IOException::class)
//    private fun onLoadModel(uri: Uri) {
//
//        // save user selected model
//        loadModelParameters.put("model", uri)
//
//        // detect model type
//        if (uri.toString().lowercase(Locale.getDefault()).endsWith(".obj")) {
//            askForRelatedFiles(0)
//        } else if (uri.toString().lowercase(Locale.getDefault()).endsWith(".stl")) {
//            askForRelatedFiles(1)
//        } else if (uri.toString().lowercase(Locale.getDefault()).endsWith(".dae")) {
//            askForRelatedFiles(2)
//        }
//        if (uri.toString().lowercase(Locale.getDefault()).endsWith(".gltf")) {
//            askForRelatedFiles(3)
//        } else {
//            // no model type from filename, ask user...
//            ContentUtils.showListDialog(
//                this, "Select type", arrayOf<String>(
//                    "Wavefront (*.obj)", "Stereolithography (*" +
//                            ".stl)", "Collada (*.dae)"
//                )
//            ) { dialog: DialogInterface?, which: Int ->
//                try {
//                    askForRelatedFiles(which)
//                } catch (e: IOException) {
//                    e.printStackTrace()
//                }
//            }
//        }
//    }
//
//    @Throws(IOException::class)
//    private fun askForRelatedFiles(modelType: Int) {
//        loadModelParameters.put("type", modelType)
//        when (modelType) {
//            0 -> {
//                // check if model references material file
//                val materialFile = WavefrontLoader.getMaterialLib(getUserSelectedModel())
//                if (materialFile == null) {
//                    getUserSelectedModel()?.let { launchModelRendererActivity(it) }
//                }
//                ContentUtils.showDialog(
//                    this, "Select material file", "This model references a " +
//                            "material file (" + materialFile + "). Please select it", "OK",
//                    "Cancel"
//                ) { dialog: DialogInterface?, which: Int ->
//                    when (which) {
//                        DialogInterface.BUTTON_NEGATIVE -> getUserSelectedModel()?.let {
//                            launchModelRendererActivity(
//                                it
//                            )
//                        }
//
//                        DialogInterface.BUTTON_POSITIVE -> {
//                            materialFile?.let { loadModelParameters.put("file", it) }
//                            askForFile(
//                                REQUEST_CODE_OPEN_MATERIAL,
//                                "*/*"
//                            )
//                        }
//                    }
//                }
//            }
//
//            1 -> getUserSelectedModel()?.let { launchModelRendererActivity(it) }
//            2 -> {
//                val images =
//                    ColladaLoader.getImages(ContentUtils.getInputStream(getUserSelectedModel()))
//                if (images == null || images.isEmpty()) {
//                    getUserSelectedModel()?.let { launchModelRendererActivity(it) }
//                } else {
//                    Log.i("MenuActivity", "Prompting user to choose files from picker...")
//                    loadModelParameters.put("files", images)
//                    val file = images[0]
//                    ContentUtils.showDialog(
//                        this, "Select texture", "This model references a " +
//                                " file (" + file + "). Please select it", "OK",
//                        "Cancel"
//                    ) { dialog: DialogInterface?, which: Int ->
//                        when (which) {
//                            DialogInterface.BUTTON_NEGATIVE -> getUserSelectedModel()?.let {
//                                launchModelRendererActivity(it)
//                            }
//
//                            DialogInterface.BUTTON_POSITIVE -> askForFile(
//                                REQUEST_CODE_ADD_FILES,
//                                "*/*"
//                            )
//                        }
//                    }
//                }
//            }
//        }
//    }
//    private fun getUserSelectedModel(): Uri? {
//        return loadModelParameters["model"] as Uri?
//    }
//
//    private fun askForFile(requestCode: Int, mimeType: String) {
//        val target = ContentUtils.createGetContentIntent(mimeType)
//        val intent = Intent.createChooser(target, "Select file")
//        try {
//            startActivityForResult(intent, requestCode)
//        } catch (e: ActivityNotFoundException) {
//            Toast.makeText(this, "Error. Please install a file content provider", Toast.LENGTH_LONG)
//                .show()
//        }
//    }
//
//    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
//        super.onActivityResult(requestCode, resultCode, data)
//
//        ContentUtils.setThreadActivity(this)
//        try {
//            when (requestCode) {
//                REQUEST_CODE_LOAD_MODEL -> {
//                    if (resultCode != RESULT_OK) {
//                        return
//                    }
//                    val uri = data?.data ?: return
//                    onLoadModel(uri)
//                }
//
//                REQUEST_CODE_OPEN_MATERIAL -> {
//                    if (resultCode != RESULT_OK || data?.data == null) {
//                        getUserSelectedModel()?.let { launchModelRendererActivity(it) }
//                    }
//                    val filename = loadModelParameters["file"] as String?
//                    ContentUtils.addUri(filename, data?.data)
//                    // check if material references texture file
//                    val textureFile = WavefrontLoader.getTextureFile(data?.data)
//                    if (textureFile == null) {
//                        getUserSelectedModel()?.let { launchModelRendererActivity(it) }
//                    }
//                    ContentUtils.showDialog(
//                        this, "Select texture file", "This model references a " +
//                                "texture file (" + textureFile + "). Please select it", "OK",
//                        "Cancel"
//                    ) { dialog: DialogInterface?, which: Int ->
//                        when (which) {
//                            DialogInterface.BUTTON_NEGATIVE -> getUserSelectedModel()?.let {
//                                launchModelRendererActivity(
//                                    it
//                                )
//                            }
//
//                            DialogInterface.BUTTON_POSITIVE -> {
//                                textureFile?.let {
//                                    loadModelParameters["file"] = it
//                                }
//
//                                askForFile(
//                                    REQUEST_CODE_OPEN_TEXTURE,
//                                    "image/*"
//                                )
//                            }
//                        }
//                    }
//                }
//
//                REQUEST_CODE_OPEN_TEXTURE -> {
//                    if (resultCode != RESULT_OK || data?.data == null) {
//                        getUserSelectedModel()?.let { launchModelRendererActivity(it) }
//                    }
//                    val textureFilename = loadModelParameters["file"] as String?
//                    ContentUtils.addUri(textureFilename, data?.data)
//                    getUserSelectedModel()?.let { launchModelRendererActivity(it) }
//                }
//
//                REQUEST_CODE_ADD_FILES -> {
//
//                    // get list of files to prompt to user
//                    val files: List<String>? = loadModelParameters["files"] as List<String>?
//                    val file = mutableListOf(loadModelParameters["files"] as List<String>?)
//                    if (files.isNullOrEmpty()) {
//                        getUserSelectedModel()?.let { launchModelRendererActivity(it) }
//                    }
//
//                    // save picked up file
//                    val current: String? = files?.toMutableList()?.removeAt(0)
//                    ContentUtils.addUri(current, data?.data)
//
//                    // no more files then load model...
//                    if (files?.isEmpty() == true) {
//                        getUserSelectedModel()?.let { launchModelRendererActivity(it) }
//                    }
//                    val next = files?.get(0)
//                    ContentUtils.showDialog(
//                        this, "Select file", "Please select file $next", "OK",
//                        "Cancel"
//                    ) { dialog: DialogInterface?, which: Int ->
//                        when (which) {
//                            DialogInterface.BUTTON_NEGATIVE -> getUserSelectedModel()?.let {
//                                launchModelRendererActivity(
//                                    it
//                                )
//                            }
//
//                            DialogInterface.BUTTON_POSITIVE -> askForFile(
//                                REQUEST_CODE_ADD_FILES,
//                                "image/*"
//                            )
//                        }
//                    }
//                }
//            }
//        } catch (ex: Exception) {
//            Log.e("MenuActivity", ex.message, ex)
//            Toast.makeText(this, "Unexpected exception: " + ex.message, Toast.LENGTH_LONG).show()
//        }
//    }
//
//    private fun launchModelRendererActivity(uri: Uri) {
//        Log.i("Menu", "Launching renderer for '$uri'")
//        val intent = Intent(applicationContext, ModelActivity::class.java)
//        try {
//            URI.create(uri.toString())
//            intent.putExtra("uri", uri.toString())
//        } catch (e: java.lang.Exception) {
//            // info: filesystem url may contain spaces, therefore we re-encode URI
//            try {
//                intent.putExtra(
//                    "uri",
//                    URI(uri.scheme, uri.authority, uri.path, uri.query, uri.fragment).toString()
//                )
//            } catch (ex: URISyntaxException) {
//                Toast.makeText(this, "Error: $uri", Toast.LENGTH_LONG).show()
//                return
//            }
//        }
//        intent.putExtra("immersiveMode", "false")
//
//        // content provider case
//        if (!loadModelParameters.isEmpty()) {
//            intent.putExtra("type", loadModelParameters["type"].toString())
//            //intent.putExtra("backgroundColor", "0.25 0.25 0.25 1");
//            loadModelParameters.clear()
//        }
//        startActivity(intent)
//    }
//
//    @Composable
//    fun ModelActivityScreen() {
//
//    }
}