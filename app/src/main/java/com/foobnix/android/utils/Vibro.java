package com.foobnix.android.utils;

import android.annotation.TargetApi;
import android.content.Context;
import android.os.Build;
import android.os.VibrationEffect;
import android.os.Vibrator;

import com.foobnix.model.AppState;

import com.foobnix.LibreraApp;

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
    public static void vibrateFinish(){
        try {
            Vibrator v = (Vibrator) LibreraApp.context.getSystemService(Context.VIBRATOR_SERVICE);

            long[] timings = {0, 150, 100, 600};
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                v.vibrate(VibrationEffect.createWaveform(timings, -1));
            } else {
                v.vibrate(timings, -1);
            }
        }catch (Exception e){
            LOG.e(e);
        }
    }
}
