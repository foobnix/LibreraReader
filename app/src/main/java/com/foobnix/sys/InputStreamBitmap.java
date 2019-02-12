package com.foobnix.sys;

import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.WeakReference;

import android.graphics.Bitmap;

public class InputStreamBitmap extends InputStream {

    private WeakReference<Bitmap> bitmap;

    public InputStreamBitmap(Bitmap bitmap) {
        super();
        this.bitmap = new WeakReference<Bitmap>(bitmap);
    }

    public Bitmap getBitmap() {
        return bitmap.get();
    }

    @Override
    public int read() throws IOException {
        return -1;
    }

    @Override
    public void close() throws IOException {

    }

}
