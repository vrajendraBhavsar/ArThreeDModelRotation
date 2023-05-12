package org.the3deer.util.android;

import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;

public class AndroidURLConnection extends URLConnection {

    private InputStream stream;

    public AndroidURLConnection(URL url) {
        super(url);
    }

    @Override
    public void connect() throws IOException
    {
        if (stream == null) {
            try {
                Log.d("TAG", "!@# connect: "+url.toURI());
                stream = ContentUtils.getInputStream(url.toURI());
                Log.d("TAG", "!@# connect: stream:: "+stream);
            } catch (URISyntaxException e) {
                Log.e("Handler", e.getMessage(), e);
                throw new IOException("Error opening stream " + url + ". " + e.getMessage());
            }
        }
    }

    @Override
    public InputStream getInputStream() throws IOException {
        connect();
        Log.d("TAG", "!@# getInputStream: "+stream);
        return stream;
    }
}
