package com.adclient.android.sdk.listeners;

import com.adclient.android.sdk.view.AbstractAdClientView;

public interface ClientAdListener {

    void onReceivedAd(AbstractAdClientView adClientView);

    void onFailedToReceiveAd(AbstractAdClientView adClientView);

    void onClickedAd(AbstractAdClientView adClientView);

    void onLoadingAd(AbstractAdClientView adClientView, String message);

    void onClosedAd(AbstractAdClientView adClientView);

}
