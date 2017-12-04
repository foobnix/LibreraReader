package com.foobnix.ui2;

import com.foobnix.pdf.info.MyADSProvider;

import android.annotation.TargetApi;
import android.content.res.Configuration;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;

@TargetApi(Build.VERSION_CODES.JELLY_BEAN)
public class AdsFragmentActivity extends FragmentActivity {

    private final MyADSProvider myAds = new MyADSProvider();

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        myAds.activate(this);
    }

    public void activateAds() {
        myAds.activate(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        myAds.resume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        myAds.pause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        myAds.destroy();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        myAds.activate(this);
    }

    public void closeActivity() {
        if (!myAds.canShowInterstial()) {
            finish();
        }
    }

    public boolean canShowInterstial() {
        return myAds.canShowInterstial();
    }

    public boolean isInterstialShown() {
        return myAds.isInterstialShown();
    }

}
