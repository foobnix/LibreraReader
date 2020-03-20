package org.ebookdroid.common.bitmaps;

import android.graphics.Bitmap;
import android.graphics.Rect;

import com.foobnix.android.utils.LOG;

import org.ebookdroid.common.settings.CoreSettings;
import org.emdev.utils.LengthUtils;

import java.util.List;

public class BitmapManager {
    static int partSize = 1 << CoreSettings.getInstance().bitmapSize;

    public static BitmapRef getBitmap(final String name, final int width, final int height, final Bitmap.Config config) {
        return new BitmapRef(Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565), 0l);
    }

    public static void release(final BitmapRef ref) {
        if (ref != null && ref.getBitmap()!=null) {
            ref.getBitmap().recycle();
        }
    }

    public static void release(final List<Bitmaps> bitmapsToRecycle) {
        try {
            if (LengthUtils.isNotEmpty(bitmapsToRecycle)) {
                for (Bitmaps b : bitmapsToRecycle) {
                    if(b!=null) {
                        b.finalize();
                    }

                }
            }
        } catch (Throwable t) {
            LOG.e(t);
        }
    }


    public static int getBitmapBufferSize(final int width, final int height, final Bitmap.Config config) {
        return getPixelSizeInBytes(config) * width * height;
    }

    public static int getBitmapBufferSize(final Bitmap parentBitmap, final Rect childSize) {
        int bytes = 4;
        if (parentBitmap != null) {
            bytes = BitmapManager.getPixelSizeInBytes(parentBitmap.getConfig());
        }
        return bytes * childSize.width() * childSize.height();
    }


    public static int getPixelSizeInBytes(final Bitmap.Config config) {
        switch (config) {
            case ALPHA_8:
                return 1;
            case ARGB_4444:
            case RGB_565:
                return 2;
            case ARGB_8888:
            default:
                return 4;
        }
    }
}
