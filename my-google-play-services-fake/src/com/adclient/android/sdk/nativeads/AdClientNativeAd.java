package com.adclient.android.sdk.nativeads;

import java.util.HashMap;

import com.adclient.android.sdk.type.ParamsType;

import android.app.Activity;
import android.content.Context;
import android.view.View;
import android.widget.ImageView;

public class AdClientNativeAd {
    public static String TITLE_TEXT_ASSET = "";
    public static String DESCRIPTION_TEXT_ASSET = "";
    public static String ICON_IMAGE_ASSET = "";
    public static String CALL_TO_ACTION_TEXT_ASSET = "";
    public static String PRIVACY_ICON_IMAGE_ASSET = "";

    public AdClientNativeAd(Activity a) {
    }

    public static void displayImage(ImageView imageView, String uri, ClientNativeAdImageListener clientNativeAdImageListener) {
    }

    public void setConfiguration(Activity a, HashMap<ParamsType, Object> banner) {
    }

    public void setRenderer(AdClientNativeAdRenderer renderer) {
    }

    public void load(Activity a) {
    }

    public void setClientNativeAdListener(ClientNativeAdListener clientNativeAdListener) {

    }

    public View getView(Activity a) {
        return null;
    }

    public void resume(Context c) {
        // TODO Auto-generated method stub

    }

    public void pause() {
        // TODO Auto-generated method stub

    }

    public void destroy() {
        // TODO Auto-generated method stub

    }

}
