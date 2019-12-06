package com.foobnix.pdf.info;

import android.app.Activity;
import android.content.Context;
import android.provider.Settings;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;

import com.foobnix.android.utils.Apps;
import com.foobnix.android.utils.Dips;
import com.foobnix.android.utils.LOG;
import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.AdView;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Random;

public class ADS {
    private static final String TAG = "ADS";
    public static int FULL_SCREEN_TIMEOUT_SEC = 10;


    public static void hideAdsTemp(Activity a) {
        try {
            if (a == null) {
                return;
            }
            View adFrame = a.findViewById(R.id.adFrame);
            if (adFrame.getVisibility() == View.VISIBLE) {
                adFrame.setVisibility(View.INVISIBLE);
            }
        } catch (Exception e) {
            LOG.e(e);
        }
    }

    public static void activateAdmobSmartBanner(final Activity a, AdView adView) {
        try {
            LOG.d("activateAdmobSmartBanner");
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
            //adView.setAdSize(AdSize.SMART_BANNER);
            //AdSize adSize = new AdSize(Dips.screenWidth(), Dips.DP_80);

            //final AdSize size = AdSize.getCurrentOrientationBannerAdSizeWithWidth(a, Dips.screenWidth());

            //LOG.d("getCurrentOrientationBannerAdSizeWithWidth", size.getWidth(), size.getHeight(), size.getHeightInPixels(a));

            if (Dips.isVertical()) {
                if (new Random().nextBoolean()) {
                    if (Dips.screenHeightDP() >= 720) {
                        adView.setAdSize(new Random().nextBoolean() ? AdSize.LARGE_BANNER : AdSize.BANNER);
                    } else {
                        adView.setAdSize(AdSize.BANNER);
                    }
                } else {
                    adView.setAdSize(AdSize.SMART_BANNER);
                }
            } else {
                adView.setAdSize(AdSize.FULL_BANNER);
            }
            adView.setAdUnitId(Apps.getMetaData(a, "librera.ADMOB_BANNER_ID"));

            adView.loadAd(getAdRequest(a));

            adView.setAdListener(new AdListener() {
                @Override
                public void onAdFailedToLoad(int arg0) {
                    LOG.d("failed ads", arg0);
                    frame.removeAllViews();
                    frame.setVisibility(View.GONE);
                }

                @Override
                public void onAdLoaded() {
                    frame.setVisibility(View.VISIBLE);
                }

            });


            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT);
            params.gravity = Gravity.CENTER_HORIZONTAL;
            adView.setLayoutParams(params);

            frame.addView(adView);
        } catch (Throwable e) {
            LOG.e(e);
        }

    }

    public static void onPauseAll(AdView adView) {
        if (adView != null) {
            adView.pause();
        }
    }

    public static void onResumeAll(AdView adView) {
        if (adView != null) {
            adView.resume();
        }
    }

    public static void destoryAll(AdView adView) {
        if (adView != null) {
            adView.destroy();
            adView = null;
        }
    }

    public static String getByTestID(Context c) {
        String android_id = Settings.Secure.getString(c.getContentResolver(), Settings.Secure.ANDROID_ID);
        String upperCase = md5_2(android_id).toUpperCase();
        LOG.d("test-MY_ADS_ID", upperCase);
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
                while (h.length() < 2)
                    h = "0" + h;
                hexString.append(h);
            }
            return hexString.toString();

        } catch (NoSuchAlgorithmException e) {
        }
        return "";
    }

    public static AdRequest getAdRequest(Activity a) {
        if (BuildConfig.DEBUG) {
            String myID = ADS.getByTestID(a);
            return new AdRequest.Builder()//
                    .addTestDevice(AdRequest.DEVICE_ID_EMULATOR)//
                    .addTestDevice(myID)//
                    .build();//
        } else {
            return new AdRequest.Builder().build();//

        }
    }
}
