package org.ebookdroid.core.crop;

import com.foobnix.android.utils.Dips;
import com.foobnix.android.utils.LOG;
import com.foobnix.pdf.info.wrapper.MagicHelper;

import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.RectF;

public class PageCropper {
    public static final int BMP_SIZE = 800;
    private static final Rect RECT = new Rect(0, 0, BMP_SIZE, BMP_SIZE);

    public static RectF getCropBounds(Bitmap bitmap1, final Rect bitmapBounds, final RectF pageSliceBounds) {

        // Bitmap source = Bitmap.createBitmap(BMP_SIZE, BMP_SIZE,
        // Bitmap.Config.RGB_565);
        // Bitmap source = Bitmap.createBitmap(bitmapRef.getBitmap());

        // final Canvas c = new Canvas(source);
        // c.drawBitmap(bitmap1, bitmapBounds, RECT, null);

        int f = bitmap1.getPixel(0, 0);
        int fR = Color.red(f);
        int fG = Color.green(f);
        int fB = Color.blue(f);

        int width = bitmap1.getWidth();
        int height = bitmap1.getHeight();

        int topY = height;
        int topX = width;
        int bottomY = 0;
        int bottomX = 0;

        LOG.d("firstColor", MagicHelper.colorToString(f));
        int dx = Dips.dpToPx(4);

        for (int y = 0; y < height; y += dx) {
            for (int x = 0; x < width; x += dx) {
                int p = bitmap1.getPixel(x, y);
                if (p == Color.WHITE || p == Color.BLACK || p == f) {
                    continue;
                }

                int pR = Color.red(p);
                int pG = Color.green(p);
                int pB = Color.blue(p);

                if (Math.abs(fR - pR) > 10 && Math.abs(fG - pG) > 10 && Math.abs(fB - pB) > 10) {
                    if (x < topX)
                        topX = x;
                    if (y < topY)
                        topY = y;
                    if (x > bottomX)
                        bottomX = x;
                    if (y > bottomY)
                        bottomY = y;
                } else {

                }
            }
        }

        LOG.d("getCropBounds", topX, topY, bottomX, bottomY, "WxH", width, height);

        if (topY == height) {
            topY = 0;
        }
        if (topX == width) {
            topX = 0;
        }

        if (bottomY == 0) {
            bottomY = height;
        }

        if (bottomX == 0) {
            bottomX = width;
        }

        float k = 0.02f;
        float left = Math.max(0, (float) topX / width - k);
        float top = Math.max(0, (float) topY / height - k);

        float right = Math.min(1, (float) bottomX / width + k);
        float bottom = Math.min(1, (float) bottomY / height + k);

        LOG.d("getCropBounds", left, top, right, bottom);
        return new RectF(left * pageSliceBounds.width() + pageSliceBounds.left, top * pageSliceBounds.height() + pageSliceBounds.top, right * pageSliceBounds.width() + pageSliceBounds.left,
                bottom * pageSliceBounds.height() + pageSliceBounds.top);
    }

}
