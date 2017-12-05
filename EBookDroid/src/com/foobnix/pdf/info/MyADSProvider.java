package com.foobnix.pdf.info;

import java.util.concurrent.TimeUnit;

import com.adclient.android.sdk.listeners.ClientAdListener;
import com.adclient.android.sdk.nativeads.AdClientNativeAd;
import com.adclient.android.sdk.view.AbstractAdClientView;
import com.adclient.android.sdk.view.AdClientInterstitial;
import com.foobnix.android.utils.LOG;
import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.InterstitialAd;
import com.google.android.gms.ads.NativeExpressAdView;

import android.app.Activity;
import android.os.Handler;

public class MyADSProvider {

    private NativeExpressAdView adViewNative;
    private AdClientNativeAd adClientView;
    private Activity a;

    InterstitialAd mInterstitialAd;
    AdClientInterstitial interstitialEP;
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

        if (AppsConfig.IS_EP) {
            ADS.activateEP(a, adClientView);
        } else {
            ADS.activateNative(a, adViewNative);
        }

        handler.removeCallbacksAndMessages(null);

        Runnable r = new Runnable() {

            @Override
            public void run() {
                try {
                    if (AppsConfig.IS_EP) {
                        interstitialEP = new AdClientInterstitial(a);
                        interstitialEP.setConfiguration(ADS.interstitial);

                        interstitialEP.addClientAdListener(new ClientAdListener() {
                            @Override
                            public void onReceivedAd(AbstractAdClientView adClientView) {
                            }

                            @Override
                            public void onFailedToReceiveAd(AbstractAdClientView adClientView) {
                            }

                            @Override
                            public void onClickedAd(AbstractAdClientView adClientView) {
                            }

                            @Override
                            public void onLoadingAd(AbstractAdClientView adClientView, String message) {
                                interstitialEP.isAdLoaded();
                            }

                            @Override
                            public void onClosedAd(AbstractAdClientView adClientView) {
                                finish.run();
                            }
                        });
                        interstitialEP.load();
                    } else {
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
        handler.postDelayed(r, TimeUnit.SECONDS.toMillis(intetrstialTimeout));

    }

    public boolean showInterstial() {
        if (AppsConfig.IS_EP) {
            if (interstitialEP != null && interstitialEP.isAdLoaded()) {
                interstitialEP.show();
                return true;
            }
        } else {
            if (mInterstitialAd != null && mInterstitialAd.isLoaded()) {
                mInterstitialAd.show();
                return true;
            }
        }
        return false;
    }

    public boolean isInterstialShown() {
        if (interstitialEP != null && interstitialEP.isShown()) {
            return true;
        }
        return false;
    }

    public void pause() {
        ADS.onPauseNative(adViewNative);
        ADS.onPauseEP(adClientView);
    }

    public void resume() {
        ADS.onResumeNative(adViewNative);
        ADS.onResumeEP(adClientView, a);
    }

    public void destroy() {
        ADS.destoryNative(adViewNative);
        ADS.destoryEP(adClientView);
        a = null;
    }

}
