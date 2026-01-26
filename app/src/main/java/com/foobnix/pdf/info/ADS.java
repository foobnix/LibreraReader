package com.foobnix.pdf.info;

import android.app.Activity;
import android.content.Context;
import android.os.Handler;
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
import com.foobnix.model.AppSP;
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
import java.text.SimpleDateFormat;
import java.util.Random;
import java.util.concurrent.TimeUnit;

public class ADS {
    //public static int FULL_SCREEN_TIMEOUT_SEC = 15;
    public static int ADS_LIVE_SEC = 60 * 60;//60 min
    public static int INTERSTITIAL_DELAY_SEC = 60 * 5;//4 min

    public static int REWARDS_HOURS_IN_SECONDS = 2 * 60 * 60;//2 hours

    private InterstitialAd interstitialAd;
    private RewardedAd rewardedAd;

    private AdView adView;

    private final static ADS instance = new ADS();

    public static synchronized ADS get() {
        return instance;
    }

    private ADS() {

    }

    public static long secondsRemain(long time) {
        return TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis() - time);

    }

    public static void hideAdsTemp(Activity a) {
    }

    Handler handler;

    public void showInterstitial(Activity a) {
        if (a == null || a.isDestroyed() || a.isFinishing()) {
            return;
        }
        if (isRewardActivated()) {
            return;
        }
        if (secondsRemain(AppSP.get().interstitialLoadAdTime) > ADS_LIVE_SEC * 2L) {
            interstitialAd = null;
            LOG.d("ADS1", "showInterstitial interstitialLoadAdTime > ADS_LIVE_SEC");
            return;
        }

        if (secondsRemain(AppSP.get().interstitialAdShowTime) < INTERSTITIAL_DELAY_SEC) {
            LOG.d("ADS1", "showInterstitial delay timeout");
            return;
        }

        if (interstitialAd != null) {
            LOG.d("ADS1", "showInterstitial");
            interstitialAd.show(a);
            AppSP.get().interstitialAdShowTime = System.currentTimeMillis();
            interstitialAd = null;
            //loadInterstitial(a);
        }
    }

    public boolean isRewardActivated() {
        try {
            boolean activated = secondsRemain(AppSP.get().rewardShowTime) < REWARDS_HOURS_IN_SECONDS;
            LOG.d("ADS1", "isRewardActivated", activated);
            return activated;
        } catch (Exception e) {
            LOG.e(e);
        }
        return true;
    }

    public void showRewardedAd(Activity a, OnUserEarnedRewardListener listener) {
        if (a == null || a.isDestroyed() || a.isFinishing()) {
            return;
        }

        if (rewardedAd != null) {
            LOG.d("ADS1", "showRewardedAd");
            rewardedAd.show(a, listener);
            rewardedAd = null;
            AppSP.get().rewardShowTime = System.currentTimeMillis();
        }
    }

    public boolean isRewardsLoaded() {
        return rewardedAd != null;
    }

    public void loadRewardedAd(Activity a, Runnable onRewardLoaded) {

        if (a == null || a.isDestroyed() || a.isFinishing()) {
            return;
        }

        if (isRewardActivated()) {
            return;
        }
        if (rewardedAd != null && secondsRemain(AppSP.get().rewardedAdLoadedTime) < ADS_LIVE_SEC) {
            LOG.d("ADS1", "loadRewardedAd in cache", secondsRemain(AppSP.get().rewardedAdLoadedTime));
            if (onRewardLoaded != null) {
                onRewardLoaded.run();
            }
            return;
        }
        LOG.d("ADS1", "RewardedAd load started...");

        String adUnitId = Apps.getMetaData(LibreraApp.context, "librera.ADMOB_REWARD");
        RewardedAd.load(a, adUnitId, new AdRequest.Builder().build(), new RewardedAdLoadCallback() {
            @Override public void onAdLoaded(@NonNull RewardedAd rewardedAdLoaded) {
                rewardedAd = rewardedAdLoaded;
                AppSP.get().rewardedAdLoadedTime = System.currentTimeMillis();
                if (onRewardLoaded != null) {
                    onRewardLoaded.run();
                }
                LOG.d("ADS1", "RewardedAd loaded");
            }

            @Override public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                rewardedAd = null;
                LOG.d("ADS1", "RewardedAd failed", loadAdError);
            }
        });
    }

    public void loadInterstitial(Activity a) {

        if (a == null || a.isDestroyed() || a.isFinishing()) {
            LOG.d("ADS1", "Interstitial destroyed");
            return;
        }
        if (interstitialAd != null && secondsRemain(AppSP.get().interstitialLoadAdTime) < ADS_LIVE_SEC) {
            LOG.d("ADS1", "loadInterstitial in cache", secondsRemain(AppSP.get().interstitialLoadAdTime));
            return;
        }
        if (isRewardActivated()) {
            return;
        }

        LOG.d("ADS1", "Interstitial try show");

        try {
            LOG.d("ADS1", "Interstitial loading...");
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
                @Override public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                    super.onAdFailedToLoad(loadAdError);
                    LOG.d("ADS1", "Interstitial LoadAdError", loadAdError);
                    interstitialAd = null;
                }

                @Override public void onAdLoaded(@NonNull InterstitialAd interstitialAd) {
                    super.onAdLoaded(interstitialAd);
                    LOG.d("ADS1", "Interstitial loaded");
                    ADS.this.interstitialAd = interstitialAd;
                    AppSP.get().interstitialLoadAdTime = System.currentTimeMillis();

                }
            });
        } catch (Exception e) {
            LOG.e(e);
        }

    }

    public synchronized void showBanner(final Activity a) {
        if (a == null || a.isDestroyed() || a.isFinishing()) {
            return;
        }
        if (isRewardActivated()) {
            return;
        }
        try {
            FrameLayout adFrame1 = a.findViewById(R.id.adFrame1);
            FrameLayout adFrame2 = a.findViewById(R.id.adFrame2);
            boolean isTopBanner = new Random().nextBoolean();
            final FrameLayout frame = isTopBanner ?//
                    adFrame1 ://
                    adFrame2;//

            if (frame == null) {
                return;
            }
            adFrame1.removeAllViews();
            adFrame2.removeAllViews();
            onDestroyBanner();

            LOG.d("ADS1", "Banner-show top", isTopBanner);
            adView = new AdView(a);
            AdSize size;
            if (isTopBanner) {
                size = new Random().nextBoolean() ?//
                        AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(a, Dips.screenWidthDP()) ://
                        AdSize.LARGE_BANNER;
            } else {
                size = AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(a, Dips.screenWidthDP());
            }

            adView.setAdSize(size);

            String metaData = Apps.getMetaData(a, "librera.ADMOB_BANNER_ID");
            adView.setAdUnitId(metaData);

            adView.loadAd(getAdRequest(a));

            adView.setAdListener(new AdListener() {
                @Override public void onAdFailedToLoad(LoadAdError arg0) {
                    LOG.d("ADS1", "Banner LoadAdError", arg0);
                    try {
                        frame.setVisibility(View.GONE);
                    } catch (Exception e) {
                        LOG.e(e);
                    }
                }

                @Override public void onAdLoaded() {
                    try {
                        frame.setVisibility(View.VISIBLE);
                        LOG.d("ADS1", "Banner loaded");
                    } catch (Exception e) {
                        LOG.e(e);
                    }
                }
            });

            FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT,
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
            LOG.d("ADS1", "Banner pause");
        }
    }

    public void onResumeBanner(Activity a) {
        if (a == null || a.isDestroyed() || a.isFinishing()) {
            return;
        }
        if (isRewardActivated()) {
            LOG.d("ADS1", "RewardActivated");
            onDestroyBanner();
            return;
        }

        if (adView != null) {
            adView.resume();
            LOG.d("ADS1", "Banner resume");
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
                LOG.d("ADS1", "Banner destroy");
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
