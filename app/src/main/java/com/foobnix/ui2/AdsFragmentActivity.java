package com.foobnix.ui2;

import android.annotation.TargetApi;
import android.content.res.Configuration;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentActivity;

import com.foobnix.android.utils.Dips;
import com.foobnix.android.utils.LOG;
import com.foobnix.model.AppState;
import com.foobnix.pdf.info.ADS;
import com.foobnix.pdf.info.AppsConfig;
import com.foobnix.pdf.info.IMG;
import com.foobnix.pdf.info.R;
import com.foobnix.pdf.search.activity.HorizontalViewActivity;
import com.foobnix.tts.TTSEngine;
import com.foobnix.tts.TTSNotification;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.OnUserEarnedRewardListener;
import com.google.android.gms.ads.rewarded.RewardedAd;
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback;

import org.ebookdroid.ui.viewer.VerticalViewActivity;

@TargetApi(Build.VERSION_CODES.JELLY_BEAN)
public abstract class AdsFragmentActivity extends FragmentActivity {

    Handler handler;

    boolean doubleBackToExitPressedOnce = false;
    private ADS ads = new ADS();

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

        handler = new Handler(Looper.getMainLooper());

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
        showBannerAds();

    }
    public void showBannerAds(){
        if (AppsConfig.isShowAdsInApp(this)) {
            ads.showBanner(this);

            if (this instanceof HorizontalViewActivity || this instanceof VerticalViewActivity) {
                ads.activateInterstitial(this);
            }
        }
    }

    public void showRewardVideo(OnUserEarnedRewardListener listener){
        if (AppsConfig.isShowAdsInApp(this)) {
                ads.showRewardedAd(this,listener);
        }
    }
    public boolean isRewardActivated(){
        if(!AppsConfig.isShowAdsInApp(this)){
            return true;
        }
        return ads.isRewardActivated();
    }


    @Override
    protected void onResume() {
        super.onResume();
        ads.onResumeBanner(this);
    }

    @Override
    protected void onPause() {
        super.onPause();
        ads.onPauseBanner();
    }

    @Override
    protected void onDestroy() {
        ads.onDestroyBanner();
        super.onDestroy();

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
        ads.showInterstitial(this);
        onFinishActivity();
    }

    public void onBackPressedAction() {
        LOG.d("onBackPressed", "doubleBackToExitPressedOnce", doubleBackToExitPressedOnce);
        if (doubleBackToExitPressedOnce) {
            handler.removeCallbacksAndMessages(null);
            onBackPressedFinishImpl();
            return;
        }
        this.doubleBackToExitPressedOnce = true;

        if(this instanceof MainTabs2) {
            if (handler != null) {
                handler.postDelayed(() -> {
                    doubleBackToExitPressedOnce = false;
                    LOG.d("onBackPressed", "timer", doubleBackToExitPressedOnce);
                    onBackPressedImpl();
                }, 500);

            }
        }else{
            onBackPressedImpl();
        }
    }

    public abstract void onBackPressedImpl();

    public abstract void onBackPressedFinishImpl();
}
