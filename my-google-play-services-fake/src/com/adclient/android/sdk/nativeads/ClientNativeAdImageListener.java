package com.adclient.android.sdk.nativeads;

import android.widget.ImageView;

public interface ClientNativeAdImageListener {

    public void onShowImageFailed(ImageView imageView, String uri, ImageDisplayError error);

    public void onNeedToShowImage(ImageView imageView, String uri);

    public void onShowImageSuccess(ImageView imageView, String uri);

}
