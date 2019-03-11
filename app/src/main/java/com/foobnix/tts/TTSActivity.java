package com.foobnix.tts;

import android.app.Activity;
import android.os.Bundle;

import com.foobnix.model.AppState;

public class TTSActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        AppState.get().load(this);
        TTSService.playLastBook();
        finish();

    }

}
