package com.foobnix.tts;

import com.foobnix.pdf.info.wrapper.AppState;

import android.app.Activity;
import android.os.Bundle;

public class TTSActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        AppState.get().load(this);
        TTSService.playLastBook();
        finish();

    }

}
