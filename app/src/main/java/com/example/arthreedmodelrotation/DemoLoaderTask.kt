package com.example.arthreedmodelrotation

import android.app.Activity
import android.opengl.GLES20
import android.util.Log
import kotlinx.coroutines.NonDisposableHandle.parent
import org.the3deer.android_3d_model_engine.model.Object3DData
import org.the3deer.android_3d_model_engine.objects.Cube
import org.the3deer.android_3d_model_engine.services.LoadListener
import org.the3deer.android_3d_model_engine.services.LoadListenerAdapter
import org.the3deer.android_3d_model_engine.services.LoaderTask
import org.the3deer.android_3d_model_engine.services.collada.ColladaLoader
import org.the3deer.android_3d_model_engine.services.wavefront.WavefrontLoader
import org.the3deer.android_3d_model_engine.util.Exploder
import org.the3deer.android_3d_model_engine.util.Rescaler
import org.the3deer.util.android.ContentUtils
import org.the3deer.util.io.IOUtils
import java.net.URI


/**
 * This class loads a 3D scene as an example of what can be done with the app
 *
 * @author andresoviedo
 */
class DemoLoaderTask(parent: Activity?, uri: URI?, callback: LoadListener?) :
    LoaderTask(parent, uri, callback) {
    /**
     * Build a new progress dialog for loading the data model asynchronously
     *
     * @param parent parent activity
     * @param uri      the URL pointing to the 3d model
     * @param callback listener
     */
    init {
        ContentUtils.provideAssets(parent)
    }

    @Throws(Exception::class)
    override fun build(): List<Object3DData>? {
        Log.d("TAG", "!@# build: called")
        // notify user
        super.publishProgress("Loading demo...")

        // list of errors found
        val errors: MutableList<Exception> = ArrayList()
        try {

            // test cube made of arrays
            val obj10 = Cube.buildCubeV1()
            obj10.color = floatArrayOf(1f, 0f, 0f, 0.5f)
            obj10.location = floatArrayOf(-2f, 2f, 0f)
            obj10.setScale(0.5f, 0.5f, 0.5f)
            super.onLoad(obj10)

            // test cube made of wires (I explode it to see the faces better)
            val obj11 = Cube.buildCubeV1()
            obj11.color = floatArrayOf(1f, 1f, 0f, 0.5f)
            obj11.location = floatArrayOf(0f, 2f, 0f)
            Exploder.centerAndScaleAndExplode(obj11, 2.0f, 1.5f)
            obj11.id = obj11.id + "_exploded"
            obj11.setScale(0.5f, 0.5f, 0.5f)
            super.onLoad(obj11)

            // test cube made of wires (I explode it to see the faces better)
            val obj12 = Cube.buildCubeV1_with_normals()
            obj12.color = floatArrayOf(1f, 0f, 1f, 1f)
            obj12.location = floatArrayOf(0f, 0f, -2f)
            obj12.setScale(0.5f, 0.5f, 0.5f)
            super.onLoad(obj12)

            // test cube made of indices
            val obj20 = Cube.buildCubeV2()
            obj20.color = floatArrayOf(0f, 1f, 0f, 0.25f)
            obj20.location = floatArrayOf(2f, 2f, 0f)
            obj20.setScale(0.5f, 0.5f, 0.5f)
            super.onLoad(obj20)

            // test cube with texture
            try {
                val open = ContentUtils.getInputStream("penguin.bmp")
                val obj3 = Cube.buildCubeV3(IOUtils.read(open))
                open.close()
                obj3.color = floatArrayOf(1f, 1f, 1f, 1f)
                obj3.location = floatArrayOf(-2f, -2f, 0f)
                obj3.setScale(0.5f, 0.5f, 0.5f)
                super.onLoad(obj3)
            } catch (ex: Exception) {
                errors.add(ex)
            }

            // test cube with texture & colors
            try {
                val open = ContentUtils.getInputStream("cube.bmp")
                val obj4 = Cube.buildCubeV4(IOUtils.read(open))
                open.close()
                obj4.color = floatArrayOf(1f, 1f, 1f, 1f)
                obj4.location = floatArrayOf(0f, -2f, 0f)
                obj4.setScale(0.5f, 0.5f, 0.5f)
                super.onLoad(obj4)
            } catch (ex: Exception) {
                errors.add(ex)
            }

            // test loading object
            try {
                // this has no color array
                val obj51 = WavefrontLoader(GLES20.GL_TRIANGLE_FAN, object : LoadListenerAdapter() {
                    override fun onLoad(obj53: Object3DData) {
                        obj53.location = floatArrayOf(-2f, 0f, 0f)
                        obj53.color = floatArrayOf(1.0f, 1.0f, 0f, 1.0f)
                        Rescaler.rescale(obj53, 2f)
                        this@DemoLoaderTask.onLoad(obj53)
                    }
                }, ).load(URI("android://com.example.arthreedmodelrotation/assets/models/teapot.obj"))[0]

                //obj51.setScale(2f,2f,2f);
                //obj51.setSize(0.5f);
                //super.onLoad(obj51);
            } catch (ex: Exception) {
                errors.add(ex)
            }

            // test loading object with materials
            try {
                // this has color array
                val obj52 = WavefrontLoader(GLES20.GL_TRIANGLE_FAN, object : LoadListenerAdapter() {
                    override fun onLoad(obj53: Object3DData) {
                        obj53.location = floatArrayOf(1.5f, -2.5f, -0.5f)
                        obj53.color = floatArrayOf(0.0f, 1.0f, 1f, 1.0f)
                        this@DemoLoaderTask.onLoad(obj53)
                    }
                }).load(URI("android://com.example.arthreedmodelrotation/assets/models/cube.obj"))[0]

                //obj52.setScale(0.5f, 0.5f, 0.5f);
                //super.onLoad(obj52);
            } catch (ex: Exception) {
                errors.add(ex)
            }

            // test loading object made of polygonal faces
            try {
                // this has heterogeneous faces
                val obj53 = WavefrontLoader(GLES20.GL_TRIANGLE_FAN, object : LoadListenerAdapter() {
                    override fun onLoad(obj53: Object3DData) {
                        obj53.color = floatArrayOf(1.0f, 1.0f, 1f, 1.0f)
                        Rescaler.rescale(obj53, 2f)
                        obj53.location = floatArrayOf(2f, 0f, 0f)
                        this@DemoLoaderTask.onLoad(obj53)
                    }
                }).load(URI("android://com.example.arthreedmodelrotation/assets/models/ToyPlane.obj"))[0]

                //super.onLoad(obj53);
            } catch (ex: Exception) {
                errors.add(ex)
            }

            // test loading object made of polygonal faces
            try {
                // this has heterogeneous faces
                val obj53 = ColladaLoader().load(
                    URI("android://com.example.arthreedmodelrotation/assets/models/cowboy.dae"),
                    object : LoadListenerAdapter() {
                        override fun onLoad(obj53: Object3DData) {
                            obj53.color = floatArrayOf(1.0f, 1.0f, 1f, 1.0f)
                            Rescaler.rescale(obj53, 2f)
                            obj53.location = floatArrayOf(0f, 0f, 2f)
                            obj53.isCentered = true
                            this@DemoLoaderTask.onLoad(obj53)
                        }
                    })[0]

                //super.onLoad(obj53);
            } catch (ex: Exception) {
                errors.add(ex)
            }


            // test loading object without normals
            /*try {
                        Object3DData obj = Object3DBuilder.loadV5(parent, Uri.parse("android://assets/models/cube4.obj"));
                        obj.setPosition(new float[] { 0f, 2f, -2f });
                        obj.setColor(new float[] { 0.3f, 0.52f, 1f, 1.0f });
                        addObject(obj);
                    } catch (Exception ex) {
                        errors.add(ex);
                    }*/

            // more test to check right position
            run {
                val obj111 = Cube.buildCubeV1()
                obj111.color = floatArrayOf(1f, 0f, 0f, 0.25f)
                obj111.location = floatArrayOf(-1f, -2f, -1f)
                obj111.setScale(0.5f, 0.5f, 0.5f)
                super.onLoad(obj111)

                // more test to check right position
                val obj112 = Cube.buildCubeV1()
                obj112.color = floatArrayOf(1f, 0f, 1f, 0.25f)
                obj112.location = floatArrayOf(1f, -2f, -1f)
                obj112.setScale(0.5f, 0.5f, 0.5f)
                super.onLoad(obj112)
            }
            run {

                // more test to check right position
                val obj111 = Cube.buildCubeV1()
                obj111.color = floatArrayOf(1f, 1f, 0f, 0.25f)
                obj111.location = floatArrayOf(-1f, -2f, 1f)
                obj111.setScale(0.5f, 0.5f, 0.5f)
                super.onLoad(obj111)

                // more test to check right position
                val obj112 = Cube.buildCubeV1()
                obj112.color = floatArrayOf(0f, 1f, 1f, 0.25f)
                obj112.location = floatArrayOf(1f, -2f, 1f)
                obj112.setScale(0.5f, 0.5f, 0.5f)
                super.onLoad(obj112)
            }
        } catch (ex: Exception) {
            errors.add(ex)
            if (errors.isNotEmpty()) {
                val msg = StringBuilder("There was a problem loading the data")
                for (error in errors) {
                    Log.e("Example", error.message, error)
                    msg.append("\n").append(error.message)
                }
                throw Exception(msg.toString())
            }
        }
        return null
    }

    override fun onProgress(progress: String) {
        super.publishProgress(progress)
    }
}