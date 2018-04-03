package org.ebookdroid.core.crop;

import com.foobnix.android.utils.Dips;
import com.foobnix.android.utils.LOG;
import com.foobnix.pdf.info.wrapper.MagicHelper;

import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.RectF;

public class PageCropper {
    public static RectF getCropBounds(Bitmap bitmap1, final Rect bitmapBounds, final RectF pageSliceBounds) {

        int f = bitmap1.getPixel(0, 0);
        int f2 = bitmap1.getPixel(bitmap1.getWidth() - 1, bitmap1.getHeight() - 1);
        int fR = Color.red(f);
        int fG = Color.green(f);
        int fB = Color.blue(f);

        int width = bitmap1.getWidth();
        int height = bitmap1.getHeight();

        int topY = height;
        int topX = width;
        int bottomY = 0;
        int bottomX = 0;

        // int dy = Math.max(Dips.dpToPx(3), height / 300);
        // int dx = Math.max(Dips.dpToPx(3), width / 300);
        int dy = Dips.dpToPx(3);
        int dx = Dips.dpToPx(3);
        LOG.d("firstColor", MagicHelper.colorToString(f), dx, dy, Dips.dpToPx(3));

        for (int y = 0; y < height; y += 1) {
            for (int x = 0; x < width; x += 1) {
                int p = bitmap1.getPixel(x, y);
                if (p == Color.WHITE || p == f || p == f2) {
                    continue;
                }

                // int pR = Color.red(p);
                // int pG = Color.green(p);
                // int pB = Color.blue(p);

                if (MagicHelper.isColorDarkSimple(p)) {
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

        LOG.d("getCropBounds-crop", left, top, right, bottom);
        return new RectF(left * pageSliceBounds.width() + pageSliceBounds.left, top * pageSliceBounds.height() + pageSliceBounds.top, right * pageSliceBounds.width() + pageSliceBounds.left,
                bottom * pageSliceBounds.height() + pageSliceBounds.top);
    }

}
