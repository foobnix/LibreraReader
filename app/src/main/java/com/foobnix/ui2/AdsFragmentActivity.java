package com.foobnix.ui2;

import android.annotation.TargetApi;
import android.content.res.Configuration;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;

import androidx.fragment.app.FragmentActivity;

import com.foobnix.android.utils.Dips;
import com.foobnix.android.utils.LOG;
import com.foobnix.model.AppState;
import com.foobnix.pdf.info.IMG;
import com.foobnix.pdf.info.MyADSProvider;
import com.foobnix.pdf.info.R;
import com.foobnix.pdf.search.view.CloseAppDialog;
import com.foobnix.tts.TTSEngine;
import com.foobnix.tts.TTSNotification;

@TargetApi(Build.VERSION_CODES.JELLY_BEAN)
public abstract class AdsFragmentActivity extends FragmentActivity {
    private final MyADSProvider myAds = new MyADSProvider();
    protected int intetrstialTimeoutSec = 0;
    protected boolean withInterstitial = true;
    Handler handler;
    Runnable onFinish = new Runnable() {

        @Override
        public void run() {
            onFinishActivity();
        }
    };
    boolean doubleBackToExitPressedOnce = false;

    public abstract void onFinishActivity();

    @Override
    protected void onCreate(Bundle arg0) {
        if (AppState.get().isSystemThemeColor) {
            AppState.get().appTheme = Dips.isDarkThemeOn() ? AppState.THEME_DARK : AppState.THEME_LIGHT;
        }

        if (AppState.get().appTheme == AppState.THEME_LIGHT || AppState.get().appTheme == AppState.THEME_INK) {
            setTheme(R.style.StyledIndicatorsWhite);
        } else {
            setTheme(R.style.StyledIndicatorsBlack);
        }
        super.onCreate(arg0);
        myAds.intetrstialTimeout = intetrstialTimeoutSec;
        myAds.createHandler();
        handler = new Handler();
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        activateAds();
    }

    public void activateAds() {
        myAds.activate(this, withInterstitial, onFinish);
    }

    @Override
    protected void onResume() {
        try {
            myAds.resume();
        } catch (Exception e) {
            LOG.e(e);
        }
        super.onResume();
        //WebServer.init(this);
    }

    @Override
    protected void onPause() {

        try {
            myAds.pause();
        } catch (Exception e) {
            LOG.e(e);
        }
        super.onPause();
    }

    public void adsPause() {
        if (myAds != null) myAds.pause();
    }

    @Override
    protected void onDestroy() {
        myAds.destroy();
        super.onDestroy();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        // myAds.activate(this, onFinish);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        LOG.d("AdsFragmentActivity onSaveInstanceState before", outState);

        if (outState != null) {
            outState.clear();
        }
        LOG.d("AdsFragmentActivity onSaveInstanceState after", outState);
        super.onSaveInstanceState(outState);

    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        LOG.d("AdsFragmentActivity onRestoreInstanceState before", savedInstanceState);

        if (savedInstanceState != null) {
            savedInstanceState.clear();
        }
        LOG.d("AdsFragmentActivity onRestoreInstanceState after", savedInstanceState);

        super.onRestoreInstanceState(savedInstanceState);
    }

    public void showInterstial() {
        IMG.pauseRequests(this);
        TTSNotification.hideNotification();
        TTSEngine.get().shutdown();
        adsPause();
        if (myAds.showInterstial(this)) {
            onFinish.run();
        } else {
            onFinish.run();
        }

    }

    public boolean isInterstialShown() {
        return false;
    }

    @Override
    public void onBackPressed() {
        LOG.d("onBackPressed", doubleBackToExitPressedOnce);
        if (doubleBackToExitPressedOnce) {
            handler.removeCallbacksAndMessages(null);
            onBackPressedFinishImpl();
            super.onBackPressed();
            return;
        }
        this.doubleBackToExitPressedOnce = true;
        if (handler != null) {
            handler.postDelayed(() -> {
                doubleBackToExitPressedOnce = false;
                LOG.d("onBackPressed", "timer", doubleBackToExitPressedOnce);
                onBackPressedImpl();
            }, 500);

        }
    }

    public abstract void onBackPressedImpl();

    public abstract void onBackPressedFinishImpl();
}
