package com.test;

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
import com.foobnix.pdf.info.R;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.res.Configuration;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.ViewGroup.LayoutParams;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

@TargetApi(Build.VERSION_CODES.JELLY_BEAN)
public class TestAdsActivity extends Activity {

    static HashMap<ParamsType, Object> banner = new HashMap<ParamsType, Object>();
    static {
        banner.put(ParamsType.AD_PLACEMENT_KEY, "9cf064256b16a112cc1fd3fb42487dbd");
        banner.put(ParamsType.ADTYPE, AdType.NATIVE_AD.toString());
        banner.put(ParamsType.AD_SERVER_URL, "http://appservestar.com/");
        banner.put(ParamsType.REFRESH_INTERVAL, 30);
    }

    static AdClientNativeAdBinder binder = new AdClientNativeAdBinder(R.layout.native_ads_ep);
    static {
        binder.bindTextAsset(AdClientNativeAd.TITLE_TEXT_ASSET, R.id.headlineView);
        binder.bindTextAsset(AdClientNativeAd.DESCRIPTION_TEXT_ASSET, R.id.descriptionView);
        binder.bindTextAsset(AdClientNativeAd.CALL_TO_ACTION_TEXT_ASSET, R.id.callToActionButton);
        binder.bindTextAsset(AdClientNativeAd.SPONSORED_ASSET, R.id.sponsoredText);

        binder.bindImageAsset(AdClientNativeAd.ICON_IMAGE_ASSET, R.id.iconView);
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
                log("onShowImageFailed", uri);
            }

            @Override
            public void onNeedToShowImage(ImageView imageView, String uri) {
                log("onNeedToShowImage", uri);
            }

            @Override
            public void onShowImageSuccess(ImageView imageView, String uri) {
                log("onShowImageSuccess", uri.hashCode());
            }
        });
    }
    AdClientNativeAd adClientNativeAd;

    private static TextView edit;

    @Override
    protected void onCreate(Bundle arg0) {
        super.onCreate(arg0);

        LinearLayout l = new LinearLayout(this);
        l.setOrientation(LinearLayout.VERTICAL);

        edit = new TextView(this);

        FrameLayout frame = new FrameLayout(this);
        frame.setId(123);

        l.addView(frame);
        l.addView(edit, new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));

        setContentView(l);
        activateEP(this, adClientNativeAd);
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        log("onConfigurationChanged");
        edit.setText("");
        activateEP(this, adClientNativeAd);
    }

    @Override
    protected void onResume() {
        super.onResume();
        log("onResume");
        if (adClientNativeAd != null)
            adClientNativeAd.resume(this);
    }

    @Override
    protected void onPause() {
        super.onPause();
        log("onPause");
        if (adClientNativeAd != null)
            adClientNativeAd.pause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        log("onDestroy");
        if (adClientNativeAd != null)
            adClientNativeAd.destroy();
    }

    public static void log(Object... msg) {
        StringBuilder build = new StringBuilder();
        for (Object s : msg) {
            build.append(s + "|");
        }
        build.append("\n");
        String out = build.toString();
        edit.setText(edit.getText() + out);
        Log.d("ADS", out);
    }

    public static void activateEP(final Activity a, AdClientNativeAd adClientNativeAd) {
        final FrameLayout frame = a.findViewById(123);
        frame.removeAllViews();

        if (adClientNativeAd != null) {
            adClientNativeAd.destroy();
            adClientNativeAd = null;
            log("AdClientNativeAd destroy");
        }

        adClientNativeAd = new AdClientNativeAd(a);
        adClientNativeAd.setConfiguration(a, banner);
        adClientNativeAd.setRenderer(renderer);
        adClientNativeAd.load(a);

        // one time loading ad
        frame.addView(adClientNativeAd.getView(a));


        adClientNativeAd.setClientNativeAdListener(new ClientNativeAdListener() {

            @Override
            public void onReceivedAd(AdClientNativeAd adClientNativeAd, boolean arg1) {
                log("onReceivedAd", arg1);
            }

            @Override
            public void onLoadingAd(AdClientNativeAd adClientNativeAd, String arg1, boolean arg2) {
                log("onLoadingAd", arg1, arg2);
                // maybe two time loading ad (bug hear)
                // frame.addView(adClientNativeAd.getView(a));
            }

            @Override
            public void onFailedToReceiveAd(AdClientNativeAd arg0, boolean arg1) {
                log("onFailedToReceiveAd", arg1);
                frame.removeAllViews();
            }

            @Override
            public void onClickedAd(AdClientNativeAd arg0, boolean arg1) {
                log("onClickedAd");
            }
        });

    }

}
