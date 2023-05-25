package org.the3deer.android_3d_model_engine.services.wavefront;

import android.app.Activity;
import android.opengl.GLES20;
import android.util.Log;

import org.the3deer.android_3d_model_engine.model.Object3DData;
import org.the3deer.android_3d_model_engine.services.LoadListener;
import org.the3deer.android_3d_model_engine.services.LoaderTask;

import java.net.URI;
import java.util.Arrays;
import java.util.List;

/**
 * Wavefront loader implementation
 *
 * @author andresoviedo
 */

public class WavefrontLoaderTask extends LoaderTask {
    private float[] userSelectedObjColor;
    public WavefrontLoaderTask(final Activity parent, final URI uri, final LoadListener callback,  float[] userSelectedObjColor) {
        super(parent, uri, callback);
        this.userSelectedObjColor = userSelectedObjColor;
        Log.d("TAG", "!@# WavefrontLoaderTask: userSelectedObjColor => "+ Arrays.toString(userSelectedObjColor));
    }

    @Override
    protected List<Object3DData> build() {

        final WavefrontLoader wfl = new WavefrontLoader(GLES20.GL_TRIANGLE_FAN, this, userSelectedObjColor);

        super.publishProgress("Loading model...");

        final List<Object3DData> load = wfl.load(uri);

        return load;
    }

    @Override
    public void onProgress(String progress) {
        super.publishProgress(progress);
    }
}
