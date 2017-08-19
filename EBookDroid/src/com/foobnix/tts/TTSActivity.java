package com.foobnix.tts;

import com.foobnix.pdf.info.wrapper.AppState;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;

public class TTSActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        AppState.get().lastBookPath = getIntent().getData().getPath();
        
        TTSService.playBookPage(AppState.get().lastBookPage, AppState.get().lastBookPath);
        new Handler().postDelayed(new Runnable() {

            @Override
            public void run() {
                TTSService.playBookPage(AppState.get().lastBookPage, AppState.get().lastBookPath);
            }
        }, 1000);
        finish();
    }

}
