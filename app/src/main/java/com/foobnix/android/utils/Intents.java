package com.foobnix.android.utils;

import android.content.Intent;

public class Intents {

    public static void putFloat(Intent i, String key, float f) {
        i.putExtra(key, f);
    }

    public static float getFloatAndClear(Intent i, String key) {
        float res = i.getFloatExtra(key, 0.0f);
        i.putExtra(key, 0.0f);
        return res;
    }
}
