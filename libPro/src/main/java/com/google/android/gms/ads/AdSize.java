package com.google.android.gms.ads;

import android.app.Activity;

public class AdSize {
    public static final AdSize SMART_BANNER =  new AdSize(0,0);
    public static final AdSize FULL_WIDTH = new AdSize(0,0);
    public static final AdSize LARGE_BANNER = new AdSize(0,0);
    public static final AdSize BANNER = new AdSize(0,0);
    public static final AdSize FULL_BANNER = new AdSize(0,0);

    public AdSize(int a, int b) {

    }

    public static AdSize getCurrentOrientationAnchoredAdaptiveBannerAdSize(Activity a, int i) {
        return new AdSize(0, 0);
    }
}
