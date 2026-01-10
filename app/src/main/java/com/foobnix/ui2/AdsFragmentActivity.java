package com.foobnix.ui2;



import static com.foobnix.pdf.info.ADS.FULL_SCREEN_TIMEOUT_SEC;

import android.annotation.TargetApi;
import android.content.res.Configuration;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import androidx.activity.OnBackPressedCallback;
import androidx.fragment.app.FragmentActivity;

import com.foobnix.android.utils.Dips;
import com.foobnix.android.utils.LOG;
import com.foobnix.model.AppState;
import com.foobnix.pdf.info.ADS;
import com.foobnix.pdf.info.Android6;
import com.foobnix.pdf.info.AppsConfig;
import com.foobnix.pdf.info.IMG;
import com.foobnix.pdf.info.R;
import com.foobnix.pdf.search.activity.HorizontalViewActivity;
import com.foobnix.tts.TTSEngine;
import com.foobnix.tts.TTSNotification;
import com.google.android.gms.ads.OnUserEarnedRewardListener;

import org.ebookdroid.ui.viewer.VerticalViewActivity;

import java.util.concurrent.TimeUnit;

@TargetApi(Build.VERSION_CODES.JELLY_BEAN)
public abstract class AdsFragmentActivity extends FragmentActivity {

    Handler handler;

    boolean doubleBackToExitPressedOnce = false;

    public abstract void onFinishActivity();

    long timeActivityCreated = 0;

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

        handler = new Handler(Looper.getMainLooper());
        timeActivityCreated = System.currentTimeMillis();

        getOnBackPressedDispatcher().addCallback(new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                onBackPressedAction();
            }
        });
    }


    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        if (AppsConfig.isShowAdsInApp(this) && Android6.canWrite(this)) {
            if (this instanceof MainTabs2) {
                showBannerAds();
            }
            if (this instanceof HorizontalViewActivity || this instanceof VerticalViewActivity) {
                ADS.get().loadRewardedAd(this, onRewardLoaded);
                ADS.get().loadInterstitial(this);
            }
        }
    }
    public void loadInterstitial(){
        if (AppsConfig.isShowAdsInApp(this)) {
            ADS.get().loadInterstitial(this);
        }
    }

    Runnable onRewardLoaded = new Runnable() {
        @Override
        public void run() {
            onRewardLoaded();
        }
    };

    public void onRewardLoaded() {

    }

    public void showBannerAds() {
            ADS.get().showBanner(this);
    }

    public void showRewardVideo(OnUserEarnedRewardListener listener) {
        if (AppsConfig.isShowAdsInApp(this)) {
            ADS.get().showRewardedAd(this, listener);
        }
    }

    public boolean isRewardLoaded() {
        return ADS.get().isRewardsLoaded();
    }

    public boolean isRewardActivated() {
        if (!AppsConfig.isShowAdsInApp(this)) {
            return true;
        }
        return ADS.get().isRewardActivated();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (AppsConfig.isShowAdsInApp(this) && Android6.canWrite(this)) {

            if (this instanceof MainTabs2) {
                ADS.get().onResumeBanner(this);
            }
            if (this instanceof HorizontalViewActivity || this instanceof VerticalViewActivity) {
                ADS.get().loadRewardedAd(this, onRewardLoaded);
                ADS.get().loadInterstitial(this);
            }
        }

    }

    @Override
    protected void onPause() {
        super.onPause();
        if (this instanceof MainTabs2 && Android6.canWrite(this)) {
            ADS.get().onPauseBanner();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

    }
    protected void onDestroyBanner(){
        ADS.get().onDestroyBanner();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
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

    public void showInterstitial() {
        IMG.pauseRequests(this);
        TTSNotification.hideNotification();
        TTSEngine.get().shutdown();
//        if (ADS.secondsRemain(timeActivityCreated) > FULL_SCREEN_TIMEOUT_SEC) {
//
//        } else {
//            LOG.d("ADS1 showInterstitial skip",ADS.secondsRemain(timeActivityCreated));
//        }
        ADS.get().showInterstitial(this);
        onFinishActivity();
    }
    public void showInterstitialNoFinish(){
        if (AppsConfig.isShowAdsInApp(this)) {
            ADS.get().showInterstitial(this);
        }
    }

    public void onBackPressedAction() {
        LOG.d("onBackPressed", "doubleBackToExitPressedOnce", doubleBackToExitPressedOnce);
        if (doubleBackToExitPressedOnce) {
            handler.removeCallbacksAndMessages(null);
            onBackPressedFinishImpl();
            return;
        }
        this.doubleBackToExitPressedOnce = true;

        if (this instanceof MainTabs2) {
            if (handler != null) {
                handler.postDelayed(() -> {
                    doubleBackToExitPressedOnce = false;
                    LOG.d("onBackPressed", "timer", doubleBackToExitPressedOnce);
                    onBackPressedImpl();
                }, 500);

            }
        } else {
            onBackPressedImpl();
        }
    }

    public abstract void onBackPressedImpl();

    public abstract void onBackPressedFinishImpl();
}
