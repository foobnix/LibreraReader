package com.foobnix.tts;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;

import com.foobnix.ui2.MyContextWrapper;

public class TTSActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        TTSService.playLastBook();
        finish();

    }

    @Override
    protected void attachBaseContext(Context context) {
        super.attachBaseContext(MyContextWrapper.wrap(context));
    }

}
