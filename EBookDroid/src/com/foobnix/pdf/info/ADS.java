package com.foobnix.pdf.info;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import com.adclient.android.sdk.nativeads.AdClientNativeAd;
import com.adclient.android.sdk.nativeads.AdClientNativeAdBinder;
import com.adclient.android.sdk.nativeads.AdClientNativeAdRenderer;
import com.adclient.android.sdk.nativeads.ClientNativeAdImageListener;
import com.adclient.android.sdk.nativeads.ClientNativeAdListener;
import com.adclient.android.sdk.nativeads.ImageDisplayError;
import com.adclient.android.sdk.type.AdType;
import com.adclient.android.sdk.type.ParamsType;
import com.foobnix.android.utils.Dips;
import com.foobnix.android.utils.LOG;
import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.NativeExpressAdView;

import android.app.Activity;
import android.content.Context;
import android.graphics.drawable.GradientDrawable;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

public class ADS {
    private static final String TAG = "ADS";
    public static int FULL_SCREEN_TIMEOUT_SEC = 10;

    public static AdRequest adRequest = new AdRequest.Builder()//
            .addTestDevice(AdRequest.DEVICE_ID_EMULATOR)//
            .addTestDevice("E0A9E8CB1E71AE8C3F6F64D692E914DB")//
            .addTestDevice("465253044271C009F461C81CFAC406BA")//
            .addTestDevice("ECC8DAFFDFD6BE5A3C799695FC4853E8")//
            .addTestDevice("ECC8DAFFDFD6BE5A3C799695FC4853E8")//
            .build();//

    public static HashMap<ParamsType, Object> interstitial = new HashMap<ParamsType, Object>();
    static {
        interstitial.put(ParamsType.AD_PLACEMENT_KEY, AppsConfig.EP_INTERSTITIAL);
        interstitial.put(ParamsType.ADTYPE, AdType.INTERSTITIAL.toString());
        interstitial.put(ParamsType.AD_SERVER_URL, "http://appservestar.com/");
    }

    static HashMap<ParamsType, Object> banner = new HashMap<ParamsType, Object>();
    static {
        banner.put(ParamsType.AD_PLACEMENT_KEY, AppsConfig.EP_BANNER_NATIVE);
        banner.put(ParamsType.ADTYPE, AdType.NATIVE_AD.toString());
        banner.put(ParamsType.AD_SERVER_URL, "http://appservestar.com/");
        banner.put(ParamsType.REFRESH_INTERVAL, 45);
    }

    static AdClientNativeAdBinder binder = new AdClientNativeAdBinder(R.layout.native_ads_ep);
    static {
        binder.bindTextAsset(AdClientNativeAd.TITLE_TEXT_ASSET, R.id.headlineView);
        binder.bindTextAsset(AdClientNativeAd.DESCRIPTION_TEXT_ASSET, R.id.descriptionView);
        binder.bindTextAsset(AdClientNativeAd.CALL_TO_ACTION_TEXT_ASSET, R.id.callToActionButton);
        binder.bindTextAsset(AdClientNativeAd.SPONSORED_ASSET, R.id.sponsoredText);

        binder.bindImageAsset(AdClientNativeAd.PRIVACY_ICON_IMAGE_ASSET, R.id.sponsoredIcon);
        binder.bindImageAsset(AdClientNativeAd.ICON_IMAGE_ASSET, R.id.iconView);

    }

    static AdClientNativeAdRenderer renderer = new AdClientNativeAdRenderer(binder);
    static {

        final List<Integer> clickItems = new ArrayList<Integer>();
        clickItems.add(R.id.callToActionButton);
        binder.setClickableItems(clickItems);

        renderer.setClientNativeAdImageListener(new ClientNativeAdImageListener() {
            @Override
            public void onShowImageFailed(ImageView imageView, String uri, ImageDisplayError error) {
                LOG.d(TAG, "onShowImageFailed", uri, error.getMessage());
                if (imageView != null) {
                    // imageView.setVisibility(View.GONE);
                }
            }

            @Override
            public void onNeedToShowImage(ImageView imageView, String uri) {
                LOG.d(TAG, "onNeedToShowImage", uri);
                if (imageView != null) {
                    // imageView.setVisibility(View.GONE);
                    // AdClientNativeAd.displayImage(imageView, uri, this);
                }
            }

            @Override
            public void onShowImageSuccess(ImageView imageView, String uri) {
                LOG.d(TAG, "onShowImageSuccess");
                if (imageView != null) {
                    // imageView.setVisibility(View.VISIBLE);
                }

            }
        });
    }

    public static void activateEP(final Activity a, AdClientNativeAd adClientNativeAd) {
        final FrameLayout frame = a.findViewById(R.id.adFrame);
        frame.removeAllViews();

        if (adClientNativeAd != null) {
            adClientNativeAd.destroy();
            adClientNativeAd = null;
        }

        adClientNativeAd = new AdClientNativeAd(a);
        adClientNativeAd.setConfiguration(a, banner);
        adClientNativeAd.setRenderer(renderer);
        adClientNativeAd.load(a);

        View view = adClientNativeAd.getView(a);
        // view.setVisibility(View.GONE);
        TextView txt = (TextView) view.findViewById(R.id.callToActionButton);
        GradientDrawable drawable = (GradientDrawable) txt.getBackground().getCurrent();
        drawable.setColor(TintUtil.color);

        frame.addView(view);

        adClientNativeAd.setClientNativeAdListener(new ClientNativeAdListener() {

            @Override
            public void onReceivedAd(AdClientNativeAd arg0, boolean arg1) {
                LOG.d(TAG, "onReceivedAd", arg1);
            }

            @Override
            public void onLoadingAd(AdClientNativeAd adClient, String arg1, boolean arg2) {
                LOG.d(TAG, "onLoadingAd", adClient.isAdLoaded(), arg1, arg2);
            }

            @Override
            public void onFailedToReceiveAd(AdClientNativeAd arg0, boolean arg1) {
                LOG.d(TAG, "onFailedToReceiveAd", arg1);
                frame.removeAllViews();
            }

            @Override
            public void onClickedAd(AdClientNativeAd arg0, boolean arg1) {
                LOG.d(TAG, "onClickedAd");
            }
        });

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

    public static void onPauseAll(NativeExpressAdView adViewNative, AdClientNativeAd adClientView, AdView adView) {
        if (adViewNative != null) {
            adViewNative.pause();
        }
        if (adClientView != null) {
            adClientView.pause();
        }
        if (adView != null) {
            adView.pause();
        }
    }

    public static void onResumeAll(Context c, NativeExpressAdView adViewNative, AdClientNativeAd adClientView, AdView adView) {
        if (adViewNative != null) {
            adViewNative.resume();
        }
        if (adClientView != null) {
            adClientView.resume(c);
        }
        if (adView != null) {
            adView.resume();
        }
    }

    public static void destoryAll(NativeExpressAdView adViewNative, AdClientNativeAd adClientView, AdView adView) {
        if (adViewNative != null) {
            adViewNative.destroy();
            adViewNative = null;
        }
        if (adClientView != null) {
            adClientView.destroy();
            adClientView = null;
        }
        if (adView != null) {
            adView.destroy();
            adView = null;
        }
    }
}
