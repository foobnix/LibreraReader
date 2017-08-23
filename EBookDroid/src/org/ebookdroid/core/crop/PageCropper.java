package org.ebookdroid.core.crop;

import com.foobnix.android.utils.LOG;
import com.foobnix.pdf.info.wrapper.MagicHelper;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.RectF;

public class PageCropper {
    public static final int BMP_SIZE = 800;
    private static final Rect RECT = new Rect(0, 0, BMP_SIZE, BMP_SIZE);

    public static RectF getCropBounds(final Bitmap bitmap, final Rect bitmapBounds, final RectF pageSliceBounds) {

        Bitmap source = Bitmap.createBitmap(BMP_SIZE, BMP_SIZE, Bitmap.Config.RGB_565);
        // Bitmap source = Bitmap.createBitmap(bitmapRef.getBitmap());

        final Canvas c = new Canvas(source);
        c.drawBitmap(bitmap, bitmapBounds, RECT, null);

        int firstColor = source.getPixel(0, 0);
        int baseColor = !MagicHelper.isColorDarkSimple(firstColor) ? firstColor : Color.WHITE;
        LOG.d("First color is ligth", !MagicHelper.isColorDarkSimple(firstColor), firstColor);

        int width = source.getWidth();
        int height = source.getHeight();

        int topY = height;
        int topX = width;
        int bottomY = 0;
        int bottomX = 0;

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int pixel = source.getPixel(x, y);
                if (baseColor == pixel) {
                    continue;
                }

                if (pixel == Color.BLACK || MagicHelper.isColorDarkSimple(pixel)) {
                    if (x < topX)
                        topX = x;
                    if (y < topY)
                        topY = y;
                    if (x > bottomX)
                        bottomX = x;
                    if (y > bottomY)
                        bottomY = y;
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
