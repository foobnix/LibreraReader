package com.foobnix.ui2;

import com.foobnix.pdf.info.MyADSProvider;
import com.foobnix.tts.TTSEngine;
import com.foobnix.tts.TTSNotification;

import android.annotation.TargetApi;
import android.content.res.Configuration;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;

@TargetApi(Build.VERSION_CODES.JELLY_BEAN)
public abstract class AdsFragmentActivity extends FragmentActivity {

    private final MyADSProvider myAds = new MyADSProvider();

    public abstract void onFinishActivity();

    protected int intetrstialTimeoutSec = 0;

    Runnable onFinish = new Runnable() {

        @Override
        public void run() {
            onFinishActivity();
        }
    };

    @Override
    protected void onCreate(Bundle arg0) {
        super.onCreate(arg0);
        myAds.intetrstialTimeout = intetrstialTimeoutSec;
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        myAds.createHandler();
        myAds.activate(this, onFinish);
    }

    public void activateAds() {
        myAds.activate(this, onFinish);
    }

    @Override
    protected void onResume() {
        super.onResume();
        myAds.resume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        myAds.pause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        myAds.destroy();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        myAds.activate(this, onFinish);
    }

    public void showInterstial() {
        TTSNotification.hideNotification();
        TTSEngine.get().shutdown();
        if (myAds.showInterstial()) {
            // ok
        } else {
            onFinish.run();
        }

    }

    public boolean isInterstialShown() {
        return false;
    }

}
