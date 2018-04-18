/**
 * 
 */
package com.foobnix.android.utils;

import java.util.Locale;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Point;
import android.os.Build;
import android.provider.Settings;
import android.view.Display;
import android.view.Surface;
import android.view.WindowManager;

public class Dips {

    public final static int DP_3 = Dips.dpToPx(3);
    public final static int DP_5 = Dips.dpToPx(5);
    public final static int DP_10 = Dips.dpToPx(10);
    public final static int DP_15 = Dips.dpToPx(15);
    public final static int DP_25 = Dips.dpToPx(25);
    public final static int DP_40 = Dips.dpToPx(40);
    public final static int DP_50 = Dips.dpToPx(50);
    public final static int DP_800 = Dips.dpToPx(800);
    public final static int DP_600 = Dips.dpToPx(600);
    public final static int DP_400 = Dips.dpToPx(400);
    public final static int DP_300 = Dips.dpToPx(300);
    public final static int DP_200 = Dips.dpToPx(200);
    public final static int DP_100 = Dips.dpToPx(100);

    private static WindowManager wm;
    static Context context;

    public static void init(Context context) {
        Dips.context = context;
        wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
    }

    public static int spToPx(final int dp) {
        return (int) (dp * Resources.getSystem().getDisplayMetrics().scaledDensity);
    }

    public static int dpToPx(final int dp) {
        return (int) (dp * Resources.getSystem().getDisplayMetrics().density);
    }

    public static int pxToDp(final int px) {
        return (int) (px / Resources.getSystem().getDisplayMetrics().density);
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
    public static int screenWidth() {
        if (Build.VERSION.SDK_INT >= 17) {
            try {
                Point size = new Point();
                wm.getDefaultDisplay().getRealSize(size);
                return size.x;
            } catch (Exception e) {
                return Resources.getSystem().getDisplayMetrics().widthPixels;
            }
        } else {
            return Resources.getSystem().getDisplayMetrics().widthPixels;
        }
    }

    public static float getRefreshRate(Context context) {
        final WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        final Display display = wm.getDefaultDisplay();
        float refreshRate = display.getRefreshRate();
        LOG.d("RefreshRate", refreshRate);
        return refreshRate;
    }

    public static boolean isEInk(Context context) {
        boolean isEink = getRefreshRate(context) < 30.0;
        if (isEink) {
            return true;
        }
        String brand = Build.BRAND.toLowerCase(Locale.US);
        if (brand.contains("onyx") || brand.contains("icarus") || brand.contains("likebook")) {
            return true;
        }
        return false;
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
    public static int screenHeight() {
        if (Build.VERSION.SDK_INT >= 17) {
            try {
                Point size = new Point();
                wm.getDefaultDisplay().getRealSize(size);
                return size.y;
            } catch (Exception e) {
                return Resources.getSystem().getDisplayMetrics().heightPixels;
            }
        } else {
            return Resources.getSystem().getDisplayMetrics().heightPixels;
        }
    }

    public static int screenWidthDP() {
        return pxToDp(screenWidth());
    }

    public static int screenHeightDP() {
        return pxToDp(screenHeight());
    }

    public static int screenMinWH() {
        return Math.min(screenHeight(), screenWidth());
    }

    public static boolean isSmallScreen() {
        // large screens are at least 640dp x 480dp
        return Dips.screenMinWH() < Dips.dpToPx(450);
    }

    public static boolean isSmallWidth() {
        return screenWidth() < Dips.dpToPx(450);
    }

    public static boolean isHorizontal() {
        return screenWidth() > screenHeight();
    }

    public static boolean isXLargeScreen() {
        int size = Resources.getSystem().getConfiguration().screenLayout & Configuration.SCREENLAYOUT_SIZE_MASK;
        // return size == Configuration.SCREENLAYOUT_SIZE_LARGE || size ==
        // Configuration.SCREENLAYOUT_SIZE_XLARGE;
        return size == Configuration.SCREENLAYOUT_SIZE_XLARGE;

    }

    public static boolean isLargeOrXLargeScreen() {
        int size = Resources.getSystem().getConfiguration().screenLayout & Configuration.SCREENLAYOUT_SIZE_MASK;
        return size == Configuration.SCREENLAYOUT_SIZE_LARGE || size == Configuration.SCREENLAYOUT_SIZE_XLARGE;
    }

    public static boolean isSystemAutoRotation(Context c) {
        try {
            return android.provider.Settings.System.getInt(c.getContentResolver(), Settings.System.ACCELEROMETER_ROTATION, 0) == 1;
        } catch (Exception e) {
            return true;
        }
    }

    public static int geUserRotation(Context c) {
        try {
            return android.provider.Settings.System.getInt(c.getContentResolver(), Settings.System.USER_ROTATION, 0);
        } catch (Exception e) {
            return Surface.ROTATION_90;
        }
    }

}
