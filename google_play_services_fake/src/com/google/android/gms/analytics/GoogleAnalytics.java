package com.google.android.gms.analytics;

import android.content.Context;

public class GoogleAnalytics {
    static GoogleAnalytics instance = new GoogleAnalytics();

    public static GoogleAnalytics getInstance(Context c) {
        return instance;
    }

    public Tracker newTracker(String id) {
        return new Tracker();
    }

}
