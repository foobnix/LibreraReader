package com.foobnix.sys;

import android.graphics.Bitmap;
import android.graphics.Color;
import com.bumptech.glide.load.engine.bitmap_recycle.BitmapPoolAdapter;
import org.junit.Test;
import static org.junit.Assert.*;

public class CoverTransformationTest {
    @Test public void opaquePooledBitmapCannotReplaceCoverWithBlankWhiteBox() {
        Bitmap cover = Bitmap.createBitmap(2, 2, Bitmap.Config.ARGB_8888); cover.eraseColor(Color.RED);
        Bitmap pooled = Bitmap.createBitmap(2, 2, Bitmap.Config.ARGB_8888); pooled.setHasAlpha(false);
        pooled.eraseColor(Color.WHITE);
        java.util.concurrent.atomic.AtomicBoolean returned = new java.util.concurrent.atomic.AtomicBoolean();
        BitmapPoolAdapter pool = new BitmapPoolAdapter() {
            @Override public Bitmap get(int width, int height, Bitmap.Config config) { return pooled; }
            @Override public void put(Bitmap bitmap) { assertSame(pooled, bitmap); returned.set(true); }
        };
        try {
            Bitmap result = new LibreraAppGlideModule().new WhiteBackgroundTransformation().transform(pool, cover, 2, 2);
            assertSame(cover, result); assertEquals(Color.RED, result.getPixel(0, 0)); assertTrue(returned.get());
        } finally { cover.recycle(); pooled.recycle(); }
    }
    @Test public void transparentCoverCompositesOntoWhiteWithoutLosingPixels() {
        Bitmap cover = Bitmap.createBitmap(2, 2, Bitmap.Config.ARGB_8888); cover.setPixel(0, 0, Color.RED);
        Bitmap result = new LibreraAppGlideModule().new WhiteBackgroundTransformation()
                .transform(new BitmapPoolAdapter(), cover, 2, 2);
        try { assertEquals(Color.RED, result.getPixel(0, 0)); assertEquals(Color.WHITE, result.getPixel(1, 1)); }
        finally { result.recycle(); cover.recycle(); }
    }
}
