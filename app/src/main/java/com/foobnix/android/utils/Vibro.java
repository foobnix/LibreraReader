package com.foobnix.android.utils;

import android.annotation.TargetApi;
import android.content.Context;
import android.os.Build;
import android.os.VibrationEffect;
import android.os.Vibrator;

import com.foobnix.model.AppState;

import org.ebookdroid.LibreraApp;

public class Vibro {

    public static void vibrate() {
        vibrate(100);
    }

    @TargetApi(26)
    public static void vibrate(long time) {
        if (AppState.get().isVibration) {
            if (Build.VERSION.SDK_INT >= 26) {
                ((Vibrator) LibreraApp.context.getSystemService(Context.VIBRATOR_SERVICE)).vibrate(VibrationEffect.createOneShot(time, VibrationEffect.DEFAULT_AMPLITUDE));
            } else {
                ((Vibrator) LibreraApp.context.getSystemService(Context.VIBRATOR_SERVICE)).vibrate(time);
            }
        }
        LOG.d("Vibro", "vibrate", time);
    }

}
