package com.google.android.gms.ads;

import android.app.Activity;

public class AdSize {
    public static final int SMART_BANNER = 1;
    public static final int FULL_WIDTH = 1;
    public static final int LARGE_BANNER = 2;
    public static final int BANNER = 3;
    public static final int FULL_BANNER = 3;

    public AdSize(int a, int b) {

    }

    public static AdSize getCurrentOrientationAnchoredAdaptiveBannerAdSize(Activity a, int i) {
        return new AdSize(0, 0);
    }
}
