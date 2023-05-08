package org.the3deer.android_3d_model_engine.services.gltf;

import android.app.Activity;
import android.util.Log;

import org.the3deer.android_3d_model_engine.model.Object3DData;
import org.the3deer.android_3d_model_engine.services.LoadListener;
import org.the3deer.android_3d_model_engine.services.LoaderTask;

import java.io.IOException;
import java.net.URI;
import java.util.List;

public class GltfLoaderTask extends LoaderTask {

    public GltfLoaderTask(Activity parent, URI uri, LoadListener callback) {
        super(parent, uri, callback);
    }

    @Override
    protected List<Object3DData> build() throws IOException {
        Log.d("TAG", "!@# build: uri ==>"+uri);
        return new GltfLoader().load(uri, this);
    }
}