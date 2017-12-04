package com.foobnix.pdf.info;

import com.adclient.android.sdk.listeners.ClientAdListener;
import com.adclient.android.sdk.nativeads.AdClientNativeAd;
import com.adclient.android.sdk.view.AbstractAdClientView;
import com.adclient.android.sdk.view.AdClientInterstitial;
import com.foobnix.android.utils.LOG;
import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.InterstitialAd;
import com.google.android.gms.ads.NativeExpressAdView;

import android.app.Activity;

public class MyADSProvider {

    private NativeExpressAdView adViewNative;
    private AdClientNativeAd adClientView;
    private Activity a;

    InterstitialAd mInterstitialAd;
    AdClientInterstitial interstitialEP;

    Runnable finish;

    public void activate(final Activity a, Runnable finish) {
        this.a = a;
        this.finish = finish;

        if (AppsConfig.IS_EP) {
            ADS.activateEP(a, adClientView);
        } else {
            ADS.activateNative(a, adViewNative);
        }

        if (!AppsConfig.checkIsProInstalled(a) && AppsConfig.ADMOB_FULLSCREEN != null) {
            mInterstitialAd = new InterstitialAd(a);
            mInterstitialAd.setAdUnitId(AppsConfig.ADMOB_FULLSCREEN);
            mInterstitialAd.setAdListener(new AdListener() {
                @Override
                public void onAdClosed() {
                    a.finish();
                }
            });

            try {
                mInterstitialAd.loadAd(ADS.adRequest);
            } catch (Exception e) {
                LOG.e(e);
            }
        }

        if (AppsConfig.IS_EP) {
            interstitialEP = new AdClientInterstitial(a);
            interstitialEP.setConfiguration(ADS.interstial);

            interstitialEP.addClientAdListener(new ClientAdListener() {
                @Override
                public void onReceivedAd(AbstractAdClientView adClientView) {
                    LOG.d("interstitialEP", "onReceivedAd");
                }

                @Override
                public void onFailedToReceiveAd(AbstractAdClientView adClientView) {
                    LOG.d("interstitialEP", "onFailedToReceiveAd");
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
                    LOG.d("interstitialEP", "onClosedAd");
                    a.finish();
                }
            });

            interstitialEP.load();

        }
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
