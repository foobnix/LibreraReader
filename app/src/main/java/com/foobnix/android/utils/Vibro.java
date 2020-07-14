package com.foobnix.android.utils;

import android.annotation.TargetApi;
import android.os.Build;
import android.os.VibrationEffect;
import android.os.Vibrator;

import androidx.core.content.ContextCompat;

import com.foobnix.model.AppState;

import org.ebookdroid.LibreraApp;

public class Vibro {

    public static void vibrate() {
        vibrate(100);
    }

    @TargetApi(26)
    public static void vibrate(long time) {
        if (AppState.get().isVibration) {
            final Vibrator vibrator = ContextCompat.getSystemService(LibreraApp.context, Vibrator.class);
            if (Build.VERSION.SDK_INT >= 26) {
                vibrator.vibrate(VibrationEffect.createOneShot(time, VibrationEffect.DEFAULT_AMPLITUDE));
            } else {
                vibrator.vibrate(time);
            }
        }
        LOG.d("Vibro", "vibrate", time);
    }

}
