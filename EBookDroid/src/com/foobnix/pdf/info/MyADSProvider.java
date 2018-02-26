package com.foobnix.pdf.info;

import java.util.Random;
import java.util.concurrent.TimeUnit;

import com.foobnix.android.utils.LOG;
import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.InterstitialAd;
import com.google.android.gms.ads.NativeExpressAdView;

import android.app.Activity;
import android.os.Handler;

public class MyADSProvider {

    private NativeExpressAdView adViewNative;
    private AdView adView;
    private Activity a;

    Random random = new Random();

    InterstitialAd mInterstitialAd;
    public int intetrstialTimeout = 0;

    Handler handler;

    public void createHandler() {
        handler = new Handler();
        try {
            Class.forName("android.os.AsyncTask");
        } catch (Throwable ignore) {
        }
    }

    public void activate(final Activity a, final Runnable finish) {
        this.a = a;

        if (AppsConfig.checkIsProInstalled(a)) {
            LOG.d("PRO is installed or beta");
            return;
        }

        // if (AppsConfig.ADMOB_NATIVE_BANNER != null && random.nextBoolean()) {
        // ADS.activateAdmobNativeBanner(a, adViewNative);
        // } else {
        // }
        ADS.activateAdmobSmartBanner(a, adView);

        if (handler == null) {
            return;
        }

        handler.removeCallbacksAndMessages(null);

        Runnable r = new Runnable() {

            @Override
            public void run() {
                try {
                    if (AppsConfig.ADMOB_FULLSCREEN != null) {
                        mInterstitialAd = new InterstitialAd(a);
                        mInterstitialAd.setAdUnitId(AppsConfig.ADMOB_FULLSCREEN);
                        mInterstitialAd.setAdListener(new AdListener() {
                            @Override
                            public void onAdClosed() {
                                finish.run();
                            }
                        });
                        mInterstitialAd.loadAd(ADS.adRequest);
                    }
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

    public boolean showInterstial() {
        if (mInterstitialAd != null && mInterstitialAd.isLoaded()) {
            mInterstitialAd.show();
            return true;
        }
        return false;
    }

    public void pause() {
        ADS.onPauseAll(adViewNative, adView);
    }

    public void resume() {
        ADS.onResumeAll(a, adViewNative, adView);
    }

    public void destroy() {
        ADS.destoryAll(adViewNative, adView);
        a = null;
    }

}
