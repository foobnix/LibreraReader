package com.foobnix.zipmanager;

import android.app.Activity;
import android.os.Bundle;

import com.foobnix.model.AppState;
import com.foobnix.pdf.info.R;

public class ZipActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        if (AppState.get().isDayNotInvert) {
            setTheme(R.style.StyledIndicatorsWhite);
        } else {
            setTheme(R.style.StyledIndicatorsBlack);
        }
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

    @Override
    protected void onStop() {
        super.onStop();
        finish();
    }
}
