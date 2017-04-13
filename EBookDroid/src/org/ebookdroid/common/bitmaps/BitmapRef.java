package org.ebookdroid.common.bitmaps;

import android.graphics.Bitmap;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class BitmapRef {

    private static final AtomicInteger SEQ = new AtomicInteger();

    public final int id = SEQ.incrementAndGet();
    public final int size;
    public final int width;
    public final int height;

    final AtomicBoolean used = new AtomicBoolean(true);

    long gen;
    String name;
    Bitmap bitmap;

    BitmapRef(final Bitmap bitmap, final long generation) {
        this.bitmap = bitmap;
        this.width = bitmap.getWidth();
        this.height = bitmap.getHeight();
        this.size = BitmapManager.getBitmapBufferSize(width, height, bitmap.getConfig());
        this.gen = generation;
    }

    @Override
    protected void finalize() throws Throwable {
        recycle();
    }

    public Bitmap getBitmap() {
        return bitmap;
    }

    public void setBitmap(Bitmap b) {
        bitmap = b;
    }

    public boolean isRecycled() {
        if (bitmap != null) {
            if (!bitmap.isRecycled()) {
                return false;
            }
            bitmap = null;
        }
        return true;
    }

    void recycle() {
        if (bitmap != null) {
            // erly resicle
            if (false) {
                bitmap.recycle();
            }
            bitmap = null;
        }
    }

    @Override
    public String toString() {
        return "BitmapRef [id=" + id + ", name=" + name + ", width=" + width + ", height=" + height + ", size=" + size
                + "]";
    }
}
