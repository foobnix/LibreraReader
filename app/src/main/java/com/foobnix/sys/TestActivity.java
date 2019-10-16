package com.foobnix.sys;

import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.widget.LinearLayout;

import com.foobnix.android.utils.Apps;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.AdView;

public class TestActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        AdView adView = new AdView(this);
        adView.setAdSize(AdSize.SMART_BANNER);
        adView.setAdUnitId(Apps.getMetaData(this, "librera.ADMOB_BANNER_ID"));



        AdRequest adRequest = new AdRequest.Builder()//
                .addTestDevice(AdRequest.DEVICE_ID_EMULATOR)//
                //.addTestDevice("FB4F2F5FAA65DBF3F1D51900356E85D9")//
                .build();//
        adView.loadAd(adRequest);

        LinearLayout l = new LinearLayout(this);
        l.setBackgroundColor(Color.RED);
        l.setOrientation(LinearLayout.VERTICAL);
        l.addView(adView);

        setContentView(l);
    }
}
