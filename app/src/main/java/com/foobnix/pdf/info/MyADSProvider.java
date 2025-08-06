package com.foobnix.pdf.info;

import android.app.Activity;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;

import com.foobnix.LibreraApp;
import com.foobnix.android.utils.Apps;
import com.foobnix.android.utils.LOG;
import com.foobnix.ui2.MainTabs2;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.interstitial.InterstitialAd;
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback;

import java.util.Random;
import java.util.concurrent.TimeUnit;

public class MyADSProvider {

    public int intetrstialTimeout = 0;
    Random random = new Random();
    InterstitialAd mInterstitialAd;
    Handler handler;
    private AdView adView;
    private Activity a;

    public void createHandler() {
        handler = new Handler(Looper.getMainLooper());
        try {
            Class.forName("android.os.AsyncTask");
        } catch (Throwable ignore) {
        }
    }

    public void activate(final Activity a, boolean withInterstitial, final Runnable finish) {
        this.a = a;

        if (AppsConfig.checkIsProInstalled(a)) {
            LOG.d("PRO is installed or beta");
            return;
        }
//        ConsentInformation consentInformation = UserMessagingPlatform.getConsentInformation(a);
//        if(!consentInformation.canRequestAds()){
//            LOG.d("ADS, can not Request Ads");
//            return;
//        }
        LOG.d("ADS, can Request Ads");


        if (withInterstitial) {
            if (handler == null) {
                return;
            }

            handler.removeCallbacksAndMessages(null);

            Runnable r = new Runnable() {

                @Override
                public void run() {
                    try {


                        try {
                            if (Apps.isNight(a)) {
                                MobileAds.setAppVolume(0.1f);
                            } else {
                                MobileAds.setAppVolume(0.8f);
                            }
                            //Toast.makeText(a,"isNight: "+Apps.isNight(a),Toast.LENGTH_SHORT).show();
                        } catch (Exception e) {
                            LOG.e(e);
                        }

                        InterstitialAd.load(LibreraApp.context, Apps.getMetaData(LibreraApp.context, "librera.ADMOB_FULLSCREEN_ID"), ADS.getAdRequest(a), new InterstitialAdLoadCallback() {
                            @Override
                            public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                                super.onAdFailedToLoad(loadAdError);
                                LOG.d("LoadAdError", loadAdError);
                                mInterstitialAd = null;
                            }

                            @Override
                            public void onAdLoaded(@NonNull InterstitialAd interstitialAd) {
                                super.onAdLoaded(interstitialAd);
                                mInterstitialAd = interstitialAd;
                            }
                        });
                    } catch (Exception e) {
                        LOG.e(e);
                    }
                }

            };
            LOG.d("ADS post delay postDelayed", intetrstialTimeout);
            if (AppsConfig.IS_LOG) {
                handler.postDelayed(r, 0);
            } else {
                handler.postDelayed(r, TimeUnit.SECONDS.toMillis(intetrstialTimeout));
            }
        }

        if (!AppsConfig.ADS_ON_PAGE && !(a instanceof MainTabs2)) {
            LOG.d("Skip ads in the book");
            return;
        }
        ADS.activateAdmobSmartBanner(a, adView);


    }

    public boolean showInterstial(Activity a) {
        if (mInterstitialAd != null) {
            mInterstitialAd.show(a);
            return true;
        }
        return false;
    }

    public void pause() {
        ADS.onPauseAll(adView);
    }

    public void resume() {
        ADS.onResumeAll(adView);
    }

    public void destroy() {
        ADS.destoryAll(adView);
        a = null;
    }

}
