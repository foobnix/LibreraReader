package com.adclient.android.sdk.nativeads;

public interface ClientNativeAdListener {

    void onReceivedAd(AdClientNativeAd arg0, boolean arg1);

    void onLoadingAd(AdClientNativeAd arg0, String arg1, boolean arg2);

    void onFailedToReceiveAd(AdClientNativeAd arg0, boolean arg1);

    void onClickedAd(AdClientNativeAd arg0, boolean arg1);

}
