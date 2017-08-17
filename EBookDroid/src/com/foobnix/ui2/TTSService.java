package com.foobnix.ui2;

import java.util.HashMap;

import com.foobnix.android.utils.Apps;
import com.foobnix.android.utils.LOG;

import android.annotation.TargetApi;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;
import android.speech.tts.TextToSpeech;
import android.speech.tts.TextToSpeech.OnInitListener;

public class TTSService extends Service {

    private static final String TAG = "TTSService";

    public static String ACTION_GET_PAGE_COUNT = "ACTION_GET_PAGE_COUNT";

    public static String ACTION_READ_TEXT = "ACTION_READ_TEXT";
    TextToSpeech ttsEngine;

    public TTSService() {
        LOG.d(TAG, "Create");
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    public static void readText(Context c, String text) {
        Intent intent = new Intent(c, TTSService.class).setAction(TTSService.ACTION_READ_TEXT);
        intent.putExtra(Intent.EXTRA_TEXT, text);
        c.startService(intent);

    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent == null) {
            return Service.START_STICKY;
        }
        LOG.d(TAG, intent.getAction());
        if (ACTION_READ_TEXT.equals(intent.getAction())) {
            ttsEngine = getOrCreateTTS();
            String text = intent.getStringExtra(Intent.EXTRA_TEXT);
            speak(text);

        }

        return Service.START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        LOG.d(TAG, "onDestroy");
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private void speak(String text) {
        LOG.d(TAG, "speak", "TEXT", text);
        if (Build.VERSION.SDK_INT >= 21) {
            ttsEngine.speak(text, TextToSpeech.QUEUE_FLUSH, null, null);
        } else {
            ttsEngine.speak(text, TextToSpeech.QUEUE_FLUSH, null);
        }
    }

    static HashMap<String, String> map = new HashMap<String, String>();

    public TextToSpeech getOrCreateTTS() {
        if (ttsEngine != null) {
            return ttsEngine;
        }
        try {
            map.put(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, Apps.getPackageName(getApplicationContext()));
            ttsEngine = new TextToSpeech(getApplicationContext(), new OnInitListener() {

                @Override
                public void onInit(int status) {
                    LOG.d(TAG, "onInit", "SUCCESS", status == TextToSpeech.SUCCESS);
                }
            });

        } catch (Exception e) {
            LOG.e(e);
        }

        return ttsEngine;

    }

}
