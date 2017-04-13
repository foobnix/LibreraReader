package com.foobnix.zipmanager;

import android.app.Activity;
import android.os.Bundle;

public class ZipActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ZipDialog.show(this, getIntent().getData(), new Runnable() {

            @Override
            public void run() {
                // finish();
            }
        });
    }

    @Override
    protected void onPause() {
        super.onPause();
        finish();
    }

}
