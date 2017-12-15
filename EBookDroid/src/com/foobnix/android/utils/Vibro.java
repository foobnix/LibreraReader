package com.foobnix.android.utils;

import org.ebookdroid.LibreraApp;

import com.foobnix.pdf.info.wrapper.AppState;

import android.content.Context;
import android.os.Vibrator;

public class Vibro {

    public static void vibrate() {
        if (AppState.get().isVibration) {
            Vibrator v = (Vibrator) LibreraApp.context.getSystemService(Context.VIBRATOR_SERVICE);
            v.vibrate(50);
        }
        LOG.d("Vibro", "vibrate", 50);
    }

    public static void vibrate(int time) {
        if (AppState.get().isVibration) {
            Vibrator v = (Vibrator) LibreraApp.context.getSystemService(Context.VIBRATOR_SERVICE);
            v.vibrate(time);
        }
        LOG.d("Vibro", "vibrate", time);
    }

}
