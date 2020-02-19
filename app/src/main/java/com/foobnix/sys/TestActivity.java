package com.foobnix.sys;

import android.app.Activity;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.widget.ImageView;

import com.foobnix.android.utils.Dips;
import com.foobnix.android.utils.LOG;
import com.foobnix.pdf.info.R;

import mobi.librera.smartreflow.AndroidPlatformImage;
import mobi.librera.smartreflow.ImageUtils;
import mobi.librera.smartreflow.SmartReflow;

public class TestActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ImageUtils.platformLogger = new ImageUtils.Logger() {
            @Override
            public void log(String str) {
                LOG.d(str);
            }
        };

        try {

            final AndroidPlatformImage input = new AndroidPlatformImage(BitmapFactory.decodeResource(getResources(), R.drawable.sample6));
            final AndroidPlatformImage output = new AndroidPlatformImage(input.getWidth() / 2, Dips.screenHeight());

            SmartReflow sm = new SmartReflow(input);
            sm.smartReflow(output);

            ImageView img = new ImageView(this);
            img.setImageBitmap(output.getImage());


            setContentView(img);
        } catch (Exception e) {
            LOG.e(e);
        }
    }
}
