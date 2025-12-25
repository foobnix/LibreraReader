package com.foobnix.pdf.info;

import android.app.Activity;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;

import com.foobnix.LibreraApp;
import com.foobnix.android.utils.Apps;
import com.foobnix.android.utils.Dips;
import com.foobnix.android.utils.LOG;
import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.interstitial.InterstitialAd;
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.TimeUnit;

public class ADS {
    InterstitialAd mInterstitialAd;

    AdView adView;
    public static int FULL_SCREEN_TIMEOUT_SEC = 30;

    public static void hideAdsTemp(Activity a) {
    }

    Handler handler;

    public void showInterstitial(Activity a) {
        if (mInterstitialAd != null) {
            LOG.d("ADS1 showInterstitial");
            mInterstitialAd.show(a);
        }
    }

    public void activateInterstitial(Activity a) {
        LOG.d("ADS1 activateInterstitial");
        handler = new Handler(Looper.getMainLooper());
        Runnable r = new Runnable() {
            @Override
            public void run() {
                try {
                    if (a == null || a.isDestroyed() || a.isFinishing()) {
                        LOG.d("ADS1 run destroyed");
                        return;
                    }
                    LOG.d("ADS1 run");
                    try {
                        if (Apps.isNight(a)) {
                            MobileAds.setAppVolume(0.1f);
                        } else {
                            MobileAds.setAppVolume(0.6f);
                        }
                    } catch (Exception e) {
                        LOG.e(e);
                    }

                    InterstitialAd.load(LibreraApp.context,
                            Apps.getMetaData(LibreraApp.context, "librera.ADMOB_FULLSCREEN_ID"),
                            ADS.getAdRequest(a),
                            new InterstitialAdLoadCallback() {
                                @Override
                                public void onAdFailedToLoad(
                                        @NonNull
                                        LoadAdError loadAdError) {
                                    super.onAdFailedToLoad(loadAdError);
                                    LOG.d("LoadAdError", loadAdError);
                                    mInterstitialAd = null;
                                }

                                @Override
                                public void onAdLoaded(
                                        @NonNull
                                        InterstitialAd interstitialAd) {
                                    super.onAdLoaded(interstitialAd);
                                    mInterstitialAd = interstitialAd;
                                }
                            });
                } catch (Exception e) {
                    LOG.e(e);
                }
            }
        };

        handler.removeCallbacksAndMessages(null);
        handler.postDelayed(r, TimeUnit.SECONDS.toMillis(FULL_SCREEN_TIMEOUT_SEC));

    }

    public void showBanner(final Activity a) {
        try {
            LOG.d("postDelayed activateAdmobSmartBanner");
            final FrameLayout frame = (FrameLayout) a.findViewById(R.id.adFrame);
            if (frame == null) {
                return;
            }
            frame.removeAllViews();
            frame.setVisibility(View.GONE);

            if (adView != null) {
                adView.destroy();
                adView = null;
            }
            adView = new AdView(a);
            AdSize size = AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(a, Dips.screenWidthDP());
            adView.setAdSize(size);

            String metaData = Apps.getMetaData(a, "librera.ADMOB_BANNER_ID");
            LOG.d("ads-metaData", metaData);
            adView.setAdUnitId(metaData);

            adView.loadAd(getAdRequest(a));

            adView.setAdListener(new AdListener() {
                @Override
                public void onAdFailedToLoad(LoadAdError arg0) {
                    LOG.d("ads-LoadAdError ads", arg0);
                    frame.removeAllViews();
                    frame.setVisibility(View.GONE);
                }

                @Override
                public void onAdLoaded() {
                    frame.setVisibility(View.VISIBLE);
                }
            });

            LinearLayout.LayoutParams
                    params =
                    new LinearLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT,
                            FrameLayout.LayoutParams.WRAP_CONTENT);
            params.gravity = Gravity.CENTER_HORIZONTAL;
            adView.setLayoutParams(params);

            frame.addView(adView);
        } catch (Throwable e) {
            LOG.e(e);
        }
    }

    public void onPauseBanner() {
        if (adView != null) {
            adView.pause();
        }
    }

    public void onResumeBanner() {
        if (adView != null) {
            adView.resume();
        }
    }

    public void onDestroyBanner() {
        if (adView != null) {
            adView.destroy();
            adView = null;
        }
    }

    public static String getByTestID(Context c) {
        String android_id = Settings.Secure.getString(c.getContentResolver(), Settings.Secure.ANDROID_ID);
        String upperCase = md5_2(android_id).toUpperCase();
        Log.d("device_id", upperCase);
        return upperCase;
    }

    public static final String md5_2(final String s) {
        try {
            // Create MD5 Hash
            MessageDigest digest = java.security.MessageDigest.getInstance("MD5");
            digest.update(s.getBytes());
            byte messageDigest[] = digest.digest();

            // Create Hex String
            StringBuffer hexString = new StringBuffer();
            for (int i = 0; i < messageDigest.length; i++) {
                String h = Integer.toHexString(0xFF & messageDigest[i]);
                while (h.length() < 2) {
                    h = "0" + h;
                }
                hexString.append(h);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
        }
        return "";
    }

    public static AdRequest getAdRequest(Context a) {
        return new AdRequest.Builder().build();//
    }
}
