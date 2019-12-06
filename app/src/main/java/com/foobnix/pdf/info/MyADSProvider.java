package com.foobnix.pdf.info;

import android.app.Activity;
import android.os.Handler;

import com.foobnix.android.utils.Apps;
import com.foobnix.android.utils.LOG;
import com.foobnix.ui2.MainTabs2;
import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.InterstitialAd;
import com.google.android.gms.ads.MobileAds;

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
        handler = new Handler();
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

                        mInterstitialAd = new InterstitialAd(a);
                        mInterstitialAd.setAdUnitId(Apps.getMetaData(a, "librera.ADMOB_FULLSCREEN_ID"));
                        mInterstitialAd.setAdListener(new AdListener() {
                            @Override
                            public void onAdClosed() {
                                finish.run();
                            }
                        });
                        mInterstitialAd.loadAd(ADS.getAdRequest(a));
                    } catch (Exception e) {
                        LOG.e(e);
                    }
                }

            };
            LOG.d("ADS post delay postDelayed", intetrstialTimeout);
            if (LOG.isEnable) {
                handler.postDelayed(r, 0);
            } else {
                handler.postDelayed(r, TimeUnit.SECONDS.toMillis(intetrstialTimeout));
            }
        }

        if(!AppsConfig.ADS_ON_PAGE && !(a instanceof MainTabs2)) {
            LOG.d("Skip ads in the book");
            return;
        }
        ADS.activateAdmobSmartBanner(a, adView);


    }

    public boolean showInterstial() {
        if (mInterstitialAd != null && mInterstitialAd.isLoaded()) {
            mInterstitialAd.show();
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
