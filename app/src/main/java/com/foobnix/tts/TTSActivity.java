package com.foobnix.tts;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;

import com.foobnix.model.AppProfile;
import com.foobnix.ui2.MyContextWrapper;

public class TTSActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        AppProfile.init(this);
        TTSService.playLastBook();
        finish();

    }

    @Override
    protected void attachBaseContext(Context context) {
        AppProfile.init(context);
        super.attachBaseContext(MyContextWrapper.wrap(context));
    }

}
