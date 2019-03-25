package com.foobnix.tts;

import android.app.Activity;
import android.os.Bundle;

import com.foobnix.model.AppProfile;

public class TTSActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        AppProfile.init(this);
        TTSService.playLastBook();
        finish();

    }

}
