package com.foobnix.sys;

import java.io.IOException;
import java.io.InputStream;

import android.graphics.Bitmap;

public class InputStreamBitmap extends InputStream {

    public Bitmap bitmap;

    public InputStreamBitmap(Bitmap bitmap) {
        super();
        this.bitmap = bitmap;
    }

    @Override
    public int read() throws IOException {
        return -1;
    }

    @Override
    public void close() throws IOException {

    }

}
