package com.foobnix.pdf.info;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.TimeUnit;

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
import com.foobnix.android.utils.TxtUtils;
import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.NativeExpressAdView;

import android.app.Activity;
import android.content.Context;
import android.os.Build;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;

public class ADS {
    private static final String TAG = "ADS";
    public static long FULL_SCREEN_TIMEOUT = TimeUnit.SECONDS.toMillis(20);

    public static AdRequest adRequest = new AdRequest.Builder()//
            .addTestDevice(AdRequest.DEVICE_ID_EMULATOR)//
            .addTestDevice("E0A9E8CB1E71AE8C3F6F64D692E914DB")//
            .addTestDevice("465253044271C009F461C81CFAC406BA")//
            .addTestDevice("ECC8DAFFDFD6BE5A3C799695FC4853E8")//
            .build();//

    public static HashMap<ParamsType, Object> interstitial = new HashMap<ParamsType, Object>();
    static {
        interstitial.put(ParamsType.AD_PLACEMENT_KEY, "0928de1630a1452b64eaab1813d3af64");
        interstitial.put(ParamsType.ADTYPE, AdType.INTERSTITIAL.toString());
        interstitial.put(ParamsType.AD_SERVER_URL, "http://appservestar.com/");
    }

    static HashMap<ParamsType, Object> banner = new HashMap<ParamsType, Object>();
    static {
        banner.put(ParamsType.AD_PLACEMENT_KEY, "ec5086312cf4959dcc54fe8a8ad15401");
        banner.put(ParamsType.ADTYPE, AdType.NATIVE_AD.toString());
        banner.put(ParamsType.AD_SERVER_URL, "http://appservestar.com/");
        banner.put(ParamsType.REFRESH_INTERVAL, 45);
    }

    static AdClientNativeAdBinder binder = new AdClientNativeAdBinder(R.layout.native_ads_ep);
    static {
        binder.bindTextAsset(AdClientNativeAd.TITLE_TEXT_ASSET, R.id.headlineView);
        binder.bindTextAsset(AdClientNativeAd.DESCRIPTION_TEXT_ASSET, R.id.descriptionView);
        binder.bindImageAsset(AdClientNativeAd.ICON_IMAGE_ASSET, R.id.iconView);
        binder.bindTextAsset(AdClientNativeAd.CALL_TO_ACTION_TEXT_ASSET, R.id.callToActionButton);
        binder.bindImageAsset(AdClientNativeAd.PRIVACY_ICON_IMAGE_ASSET, R.id.sponsoredIcon);
    }

    static AdClientNativeAdRenderer renderer = new AdClientNativeAdRenderer(binder);
    static {

        final List<Integer> clickItems = new ArrayList<Integer>();
        clickItems.add(R.id.callToActionButton);
        binder.setClickableItems(clickItems);

        renderer.setClientNativeAdImageListener(new ClientNativeAdImageListener() {
            @Override
            public void onShowImageFailed(ImageView imageView, String uri, ImageDisplayError error) {
                LOG.d(TAG, "onShowImageFailed");
                if (imageView != null) {
                    AdClientNativeAd.displayImage(imageView, uri, this);
                }
            }

            @Override
            public void onNeedToShowImage(ImageView imageView, String uri) {
                LOG.d(TAG, "onNeedToShowImage");
                if (imageView != null) {
                    AdClientNativeAd.displayImage(imageView, uri, this);
                }
            }

            @Override
            public void onShowImageSuccess(ImageView imageView, String uri) {

            }
        });
    }

    public static void activateEP(final Activity a, AdClientNativeAd adClientNativeAd) {
        final FrameLayout adClientView = a.findViewById(R.id.adFrame);
        adClientView.removeAllViews();
        if (!AppsConfig.IS_EP) {
            adClientView.setVisibility(View.GONE);
            return;
        }
        adClientNativeAd = new AdClientNativeAd(a);
        adClientNativeAd.setConfiguration(a, banner);
        adClientNativeAd.setRenderer(renderer);
        adClientNativeAd.load(a);

        adClientNativeAd.setClientNativeAdListener(new ClientNativeAdListener() {

            @Override
            public void onReceivedAd(AdClientNativeAd arg0, boolean arg1) {
            }

            @Override
            public void onLoadingAd(AdClientNativeAd arg0, String arg1, boolean arg2) {
                View view = arg0.getView(a);
                adClientView.addView(view);
            }

            @Override
            public void onFailedToReceiveAd(AdClientNativeAd arg0, boolean arg1) {
            }

            @Override
            public void onClickedAd(AdClientNativeAd arg0, boolean arg1) {
            }
        });

    }

    public static void activateNative(final Activity a, NativeExpressAdView adViewNative) {
        try {

            final FrameLayout frame = (FrameLayout) a.findViewById(R.id.adFrame);
            if (!AppsConfig.checkIsProInstalled(a)) {

                final String unitID = AppsConfig.ADMOB_NATIVE_SMALL;

                if (TxtUtils.isEmpty(unitID) || Build.VERSION.SDK_INT <= 9) {
                    frame.setVisibility(View.GONE);
                    return;
                }

                try {
                    Class.forName("android.os.AsyncTask");
                } catch (Throwable ignore) {
                }

                destoryNative(adViewNative);
                adViewNative = new NativeExpressAdView(a);
                adViewNative.setAdUnitId(unitID);
                int adSizeHeight = Dips.screenHeightDP() / 8;
                LOG.d("adSizeHeight", adSizeHeight);
                adViewNative.setAdSize(new AdSize(AdSize.FULL_WIDTH, Math.max(82, adSizeHeight)));

                adViewNative.loadAd(ADS.adRequest);

                if (frame != null) {
                    frame.setVisibility(View.VISIBLE);
                    frame.removeAllViews();
                    frame.addView(adViewNative);
                }

                adViewNative.setAdListener(new AdListener() {
                    @Override
                    public void onAdFailedToLoad(int arg0) {
                        frame.removeAllViews();
                        frame.setVisibility(View.GONE);
                        // frame.addView(proAdsLayout(a));
                    }
                });
            } else {
                frame.setVisibility(View.GONE);
            }
        } catch (Exception e) {
            LOG.e(e);
        }

    }

    public static void onPause(final AdView adView) {
        if (adView != null) {
            adView.pause();
        }
    }

    public static void onPauseNative(final NativeExpressAdView adView) {
        if (adView != null) {
            adView.pause();
        }
    }

    public static void onResumeNative(final NativeExpressAdView adView) {
        if (adView != null) {
            AppsConfig.checkIsProInstalled(adView.getContext());
            adView.resume();
        }
    }

    public static void onResume(final AdView adView) {

        if (adView != null) {
            AppsConfig.checkIsProInstalled(adView.getContext());
            adView.resume();
        }
    }

    public static void onResumeEP(final AdClientNativeAd adClientView, Context c) {
        if (adClientView != null) {
            adClientView.resume(c);
        }
    }

    public static void onPauseEP(final AdClientNativeAd adClientView) {
        if (adClientView != null) {
            adClientView.pause();
        }
    }

    public static void destoryNative(NativeExpressAdView adView) {
        if (adView != null) {
            adView.destroy();
            adView = null;
        }
    }

    public static void destory(AdView adView) {
        if (adView != null) {
            adView.destroy();
            adView = null;
        }
    }

    public static void destoryEP(AdClientNativeAd adClientView) {
        if (adClientView != null) {
            adClientView.destroy();
            adClientView = null;
        }
    }

}
