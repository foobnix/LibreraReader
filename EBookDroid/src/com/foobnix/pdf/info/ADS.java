package com.foobnix.pdf.info;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import com.foobnix.android.utils.Dips;
import com.foobnix.android.utils.LOG;
import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.NativeExpressAdView;

import android.app.Activity;
import android.content.Context;
import android.provider.Settings;
import android.view.View;
import android.widget.FrameLayout;

public class ADS {
    private static final String TAG = "ADS";
    public static int FULL_SCREEN_TIMEOUT_SEC = 10;

    public static AdRequest adRequest = new AdRequest.Builder()//
            .addTestDevice(AdRequest.DEVICE_ID_EMULATOR)//
            .build();//

    public static void hideAdsTemp(Activity a) {
        try {
            if (a == null) {
                return;
            }
            View adFrame = a.findViewById(R.id.adFrame);
            adFrame.setVisibility(View.INVISIBLE);
        } catch (Exception e) {
            LOG.e(e);
        }
    }

    public static void activateAdmobSmartBanner(final Activity a, AdView adView) {
        try {
            final FrameLayout frame = (FrameLayout) a.findViewById(R.id.adFrame);
            frame.removeAllViews();

            if (adView != null) {
                adView.destroy();
                adView = null;
            }
            adView = new AdView(a);
            adView.setAdSize(AdSize.SMART_BANNER);
            adView.setAdUnitId(AppsConfig.ADMOB_BANNER);

            adView.loadAd(adRequest);

            adView.setAdListener(new AdListener() {
                @Override
                public void onAdFailedToLoad(int arg0) {
                    frame.removeAllViews();
                    // frame.setVisibility(View.GONE);
                }
            });

            frame.addView(adView);
        } catch (Exception e) {
            LOG.e(e);
        }

    }

    public static void activateAdmobNativeBanner(final Activity a, NativeExpressAdView adViewNative) {
        try {

            final FrameLayout frame = (FrameLayout) a.findViewById(R.id.adFrame);
            frame.removeAllViews();
            frame.setVisibility(View.VISIBLE);

            if (adViewNative != null) {
                adViewNative.destroy();
                adViewNative = null;
            }

            adViewNative = new NativeExpressAdView(a);
            adViewNative.setAdUnitId(AppsConfig.ADMOB_NATIVE_BANNER);
            int adSizeHeight = Dips.screenHeightDP() / 9;
            LOG.d("adSizeHeight", adSizeHeight);
            adViewNative.setAdSize(new AdSize(AdSize.FULL_WIDTH, Math.max(82, adSizeHeight)));

            adViewNative.loadAd(ADS.adRequest);

            adViewNative.setAdListener(new AdListener() {
                @Override
                public void onAdFailedToLoad(int arg0) {
                    frame.removeAllViews();
                    // frame.setVisibility(View.GONE);
                }
            });

            frame.addView(adViewNative);

        } catch (Exception e) {
            LOG.e(e);
        }

    }

    public static void onPauseAll(NativeExpressAdView adViewNative, AdView adView) {
        if (adViewNative != null) {
            adViewNative.pause();
        }
        if (adView != null) {
            adView.pause();
        }
    }

    public static void onResumeAll(Context c, NativeExpressAdView adViewNative, AdView adView) {
        if (adViewNative != null) {
            adViewNative.resume();
        }
        if (adView != null) {
            adView.resume();
        }
    }

    public static void destoryAll(NativeExpressAdView adViewNative, AdView adView) {
        if (adViewNative != null) {
            adViewNative.destroy();
            adViewNative = null;
        }
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
}
