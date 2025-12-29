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

import androidx.annotation.NonNull;

import com.foobnix.LibreraApp;
import com.foobnix.android.utils.Apps;
import com.foobnix.android.utils.Dips;
import com.foobnix.android.utils.LOG;
import com.foobnix.model.AppState;
import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.OnUserEarnedRewardListener;
import com.google.android.gms.ads.interstitial.InterstitialAd;
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback;
import com.google.android.gms.ads.rewarded.RewardedAd;
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.TimeUnit;

public class ADS {
    InterstitialAd mInterstitialAd;
    RewardedAd rewardedAd;

    AdView adView;
    public static int FULL_SCREEN_TIMEOUT_SEC = 30;

    public static void hideAdsTemp(Activity a) {
    }

    Handler handler;

    public void showInterstitial(Activity a) {
        if (isRewardActivated()) {
            return;
        }
        if (mInterstitialAd != null) {
            LOG.d("ADS1 showInterstitial");
            mInterstitialAd.show(a);
            mInterstitialAd = null;
        }
    }

    public boolean isRewardActivated() {
        try {
            long oneHourMillis = TimeUnit.HOURS.toMillis(2);
            long delta = System.currentTimeMillis() - AppState.get().rewardShowedDate;
            boolean activated = delta < oneHourMillis;
            LOG.d("ADS1", "isRewardActivated", activated, new SimpleDateFormat("HH:mm:ss").format(AppState.get().rewardShowedDate + oneHourMillis));
            return activated;
        } catch (Exception e) {
            LOG.e(e);
        }
        return true;
    }

    public void showRewardedAd(Activity a, OnUserEarnedRewardListener listener) {
        if (rewardedAd != null) {
            LOG.d("ADS1 showRewardedAd");
            rewardedAd.show(a, listener);
            rewardedAd = null;
        }
    }

    public boolean isRewardsLoaded() {
        return rewardedAd != null;
    }

    public void loadRewardedAd(Activity a) {
        if (isRewardActivated()) {
            return;
        }
        LOG.d("ADS1 RewardedAd load");
        String adUnitId = Apps.getMetaData(LibreraApp.context, "librera.ADMOB_REWARD");
        rewardedAd = null;
        RewardedAd.load(a, adUnitId, new AdRequest.Builder().build(), new RewardedAdLoadCallback() {
            @Override
            public void onAdLoaded(
                    @NonNull
                    RewardedAd rewardedAdLoaded) {
                rewardedAd = rewardedAdLoaded;
                LOG.d("ADS1 RewardedAd loaded");
            }

            @Override
            public void onAdFailedToLoad(
                    @NonNull
                    LoadAdError loadAdError) {
                rewardedAd = null;
                LOG.d("ADS1 RewardedAd failed");
            }
        });
    }

    public void activateInterstitial(Activity a) {
        if (isRewardActivated()) {
            return;
        }

        LOG.d("ADS1 Interstitial try show");
        loadRewardedAd(a);
        handler = new Handler(Looper.getMainLooper());
        Runnable r = new Runnable() {
            @Override
            public void run() {
                try {
                    if (a == null || a.isDestroyed() || a.isFinishing()) {
                        LOG.d("ADS1 Interstitial destroyed");
                        return;
                    }
                    LOG.d("ADS1 Interstitial loading...");
                    try {
                        if (Apps.isNight(a)) {
                            MobileAds.setAppVolume(0.1f);
                        } else {
                            MobileAds.setAppVolume(0.6f);
                        }
                    } catch (Exception e) {
                        LOG.e(e);
                    }

                    String adUnitId = Apps.getMetaData(LibreraApp.context, "librera.ADMOB_FULLSCREEN_ID");
                    InterstitialAd.load(LibreraApp.context, adUnitId, ADS.getAdRequest(a), new InterstitialAdLoadCallback() {
                        @Override
                        public void onAdFailedToLoad(
                                @NonNull
                                LoadAdError loadAdError) {
                            super.onAdFailedToLoad(loadAdError);
                            LOG.d("ADS1", "Interstitial LoadAdError", loadAdError);
                            mInterstitialAd = null;
                        }

                        @Override
                        public void onAdLoaded(
                                @NonNull
                                InterstitialAd interstitialAd) {
                            super.onAdLoaded(interstitialAd);
                            LOG.d("ADS1 Interstitial loaded");
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
        if (isRewardActivated()) {
            return;
        }
        try {
            LOG.d("ADS1 Banner try show");
            final FrameLayout frame = a.findViewById(R.id.adFrame);
            if (frame == null) {
                return;
            }
            frame.removeAllViews();
            onDestroyBanner();
            LOG.d("ADS1 Banner show");
            adView = new AdView(a);
            AdSize size = AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(a, Dips.screenWidthDP());
            //AdSize size = AdSize.BANNER;
            adView.setAdSize(size);

            String metaData = Apps.getMetaData(a, "librera.ADMOB_BANNER_ID");
            adView.setAdUnitId(metaData);

            adView.loadAd(getAdRequest(a));

            adView.setAdListener(new AdListener() {
                @Override
                public void onAdFailedToLoad(LoadAdError arg0) {
                    LOG.d("ADS1 Banner LoadAdError", arg0);
                    try {
                        frame.setVisibility(View.GONE);
                    } catch (Exception e) {
                        LOG.e(e);
                    }
                }

                @Override
                public void onAdLoaded() {
                    try {
                        frame.setVisibility(View.VISIBLE);
                        LOG.d("ADS1 Banner loaded");
                    } catch (Exception e) {
                        LOG.e(e);
                    }
                }
            });

            FrameLayout.LayoutParams
                    params =
                    new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT);
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
            LOG.d("ADS1 Banner pause");
        }
    }

    public void onResumeBanner(Activity a) {
        if (isRewardActivated()) {
            onDestroyBanner();
            return;
        }

        if (adView != null) {
            adView.resume();
            LOG.d("ADS1 Banner resume");
        } else {
            if (AppsConfig.isShowAdsInApp(a)) {
                showBanner(a);
            }
        }
    }

    public void onDestroyBanner() {
        try {
            if (adView != null) {
                adView.setVisibility(View.GONE);
                LOG.d("ADS1 Banner destroy");
                adView.destroy();
                adView = null;
            }
        } catch (Exception e) {
            LOG.e(e);
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
