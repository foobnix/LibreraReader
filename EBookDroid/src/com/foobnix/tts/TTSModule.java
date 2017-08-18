package com.foobnix.tts;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import com.foobnix.android.utils.Apps;
import com.foobnix.android.utils.LOG;
import com.foobnix.android.utils.TxtUtils;
import com.foobnix.dao2.FileMeta;
import com.foobnix.pdf.info.FontExtractor;
import com.foobnix.pdf.info.R;
import com.foobnix.pdf.info.wrapper.AppState;
import com.foobnix.pdf.info.wrapper.DocumentController;
import com.foobnix.ui2.AppDB;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.os.Vibrator;
import android.speech.tts.TextToSpeech;
import android.speech.tts.TextToSpeech.EngineInfo;
import android.speech.tts.TextToSpeech.OnInitListener;
import android.speech.tts.TextToSpeech.OnUtteranceCompletedListener;
import android.support.v4.app.NotificationCompat;
import android.widget.Toast;

@TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
final class TTSModule {

    private static final File DW = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);

    public static String speakToPath = DW != null ? DW.getPath() : Environment.getExternalStorageDirectory().getPath();

    public static final String DICT_FOLDER = "Dict";
    private static final int NOT_ID = 123123;
    private static final String TAG = "TTSPlayerController";

    public static final String TTS_EXTRA = "TTS_EXTRA";
    public static final String TTS_READ = "TTS_READ";
    public static final String TTS_STOP = "TTS_STOP";
    public static final String TTS_NEXT = "TTS_NEXT";

    TextToSpeech ttsEngine;
    Activity activity;


    public String engine;

    private Runnable onCompleteListener;

    static HashMap<String, String> map = new HashMap<String, String>();


    private static DocumentController controller;


    public void hideNotification() {
        if (activity == null) {
            return;
        }
        NotificationManager nm = (NotificationManager) activity.getSystemService(Context.NOTIFICATION_SERVICE);
        nm.cancel(NOT_ID);
    }

    public void showNotification() {
        if (activity == null) {
            return;
        }
        try {
            NotificationManager nm = (NotificationManager) activity.getSystemService(Context.NOTIFICATION_SERVICE);

            NotificationCompat.Builder builder = new NotificationCompat.Builder(activity);

            FileMeta fileMeta = AppDB.get().getOrCreate(controller.getCurrentBook().getPath());

            PendingIntent contentIntent = PendingIntent.getActivity(activity, 0, activity.getIntent(), PendingIntent.FLAG_UPDATE_CURRENT);

            PendingIntent stop = PendingIntent.getBroadcast(activity, 0, new Intent(TTS_STOP), PendingIntent.FLAG_UPDATE_CURRENT);
            PendingIntent read = PendingIntent.getBroadcast(activity, 0, new Intent(TTS_READ), PendingIntent.FLAG_UPDATE_CURRENT);
            PendingIntent next = PendingIntent.getBroadcast(activity, 0, new Intent(TTS_NEXT), PendingIntent.FLAG_UPDATE_CURRENT);

            builder.setContentIntent(contentIntent) //
                    .setSmallIcon(R.drawable.glyphicons_185_volume_up) //
                    .setLargeIcon(controller.getBookImage()) //
                    .setTicker(controller.getString(R.string.app_name)) //
                    .setWhen(System.currentTimeMillis()) //
                    .setOngoing(AppState.getInstance().notificationOngoing)//
                    .addAction(R.drawable.glyphicons_175_pause, activity.getString(R.string.to_stop), stop)//
                    .addAction(R.drawable.glyphicons_174_play, activity.getString(R.string.to_read), read)//
                    .addAction(R.drawable.glyphicons_177_forward, activity.getString(R.string.next), next)//
                    .setContentTitle(fileMeta.getTitle() + " " + fileMeta.getAuthor()) //
                    .setContentText(activity.getString(R.string.page) + " " + (controller.getCurentPageFirst1())); ///

            Notification n = builder.build(); //
            nm.notify(NOT_ID, n);
        } catch (Exception e) {
            return;
        }
    }

    int attempt;

    public void play() {
        if (controller == null) {
            return;
        }
        showNotification();
        String readingText = controller.getTextForPage(controller.getCurentPageFirst1() - 1);
        play(readingText);
    }


    public void bookIsOver() {
        try {
            if (controller == null || controller.getActivity() == null) {
                return;
            }
            Vibrator v = (Vibrator) controller.getActivity().getSystemService(Context.VIBRATOR_SERVICE);
            if (AppState.get().isVibration) {
                v.vibrate(1000);
            }

            controller.handler.postDelayed(new Runnable() {

                @Override
                public void run() {

                    String overString = controller.getString(R.string.the_book_is_over);
                    LOG.d("overString", ". . . . " + overString);
                    play(overString, true);

                }
            }, 2000);
        } catch (Exception e) {
            LOG.e(e);
        }

        // stop();
    }

    public void next() {
        if (controller == null) {
            return;
        }

        if (attempt >= 3) {
            stop();
            return;
        }

        if (controller.getCurentPageFirst1() + 1 > controller.getPageCount()) {
            LOG.d("next stop", controller.getCurentPageFirst1() + 1, controller.getPageCount());
            bookIsOver();
            return;
        }

        controller.onGoToPage(controller.getCurentPageFirst1() + 1);
        String readingText = controller.getTextForPage(controller.getCurentPageFirst1() - 1);
        showNotification();
        if (TxtUtils.isEmpty(readingText)) {
            attempt++;
            next();
        } else {
            attempt = 0;
            play(readingText);
        }

    }

    public static boolean isAvailableTTS() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH;
    }

    @SuppressLint("NewApi")
    private static TTSModule getInstanceInit(final Activity activity, DocumentController dc) {
        controller = dc;

        IntentFilter filter = new IntentFilter();
        filter.addAction(TTS_STOP);
        filter.addAction(TTS_NEXT);
        filter.addAction(TTS_READ);

        // activity.registerReceiver(INSTANCE.broadcastReceiver, filter);
        return new TTSModule(null);

    }

    static Map<String, String> hashMap = new HashMap<String, String>();

    public String applyReplacement(String text) {
        if (text == null) {
            return "";
        }
        text = text.replace("?", "?.");
        text = text.replace("!", "!.");

        if (false) {
            return text;
        }
        if (hashMap.isEmpty()) {
            loadReaplacment(activity);
        }

        text = text.toLowerCase(Locale.US);
        for (String key : hashMap.keySet()) {
            text = text.replace(key, hashMap.get(key));
        }
        return text;
    }

    public static void loadReaplacment(Activity a) {
        try {
            hashMap.clear();
            InputStream open = new FileInputStream(new File(FontExtractor.getFontsDir(a, DICT_FOLDER), "replace-dict.txt"));

            BufferedReader input = new BufferedReader(new InputStreamReader(open));
            String line;
            while ((line = input.readLine()) != null) {
                if (line.startsWith("#") || TxtUtils.isEmpty(line)) {
                    continue;
                }
                String[] kv = line.split("\" \"");
                String key = kv[0].replace("\"", "");
                String value = kv[1].replace("\"", "");
                hashMap.put(key, value);

                LOG.d("KeyValue", key, value, "|");

            }
            input.close();

        } catch (IOException e) {
            LOG.e(e);
        }

    }

    private TTSModule(Activity activity) {
        initTTS(activity);

    }

    public void initTTS(Activity activity) {
        if (!isAvailableTTS()) {
            return;
        }
        try {
            map.put(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, Apps.getPackageName(activity));
            this.activity = activity;
            ttsEngine = new TextToSpeech(activity, new OnInitListener() {

                @Override
                public void onInit(int status) {
                }
            });

        } catch (Exception e) {
            LOG.e(e);
        }

    }

    public TextToSpeech getDefaultTTS() {
        return ttsEngine;
    }

    public String getDefaultEngineName() {
        try {
            if (ttsEngine != null) {
                String enginePackage = ttsEngine.getDefaultEngine();
                engine = enginePackage;
                List<EngineInfo> engines = ttsEngine.getEngines();
                for (final EngineInfo eInfo : engines) {
                    if (eInfo.name.equals(enginePackage)) {

                        return engineToString(eInfo);
                    }
                }
            }
        } catch (Exception e) {
            LOG.e(e);
        }
        return "no engine";
    }

    public static String engineToString(EngineInfo info) {
        return info.label;
    }

    public void init(final Activity activity, String engine) {
        try {
            OnInitListener listener = new OnInitListener() {

                @Override
                public void onInit(int status) {
                    if (status == TextToSpeech.ERROR) {
                        Toast.makeText(activity, R.string.msg_unexpected_error, Toast.LENGTH_LONG).show();
                    }

                }
            };
            if (ttsEngine != null) {
                ttsEngine.stop();
                ttsEngine.shutdown();
                // ttsEngine = null;
            }
            ttsEngine = new TextToSpeech(activity, listener, engine);
        } catch (Exception e) {
            LOG.e(e);
        }
    }

    public void bindCompleteHandler() {

        final Handler handler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                if (msg.what == TextToSpeech.SUCCESS) {
                    if (onCompleteListener != null) {
                        onCompleteListener.run();
                    }
                }
            }
        };

        ttsEngine.setOnUtteranceCompletedListener(new OnUtteranceCompletedListener() {

            @Override
            public void onUtteranceCompleted(String utteranceId) {
                handler.sendEmptyMessage(TextToSpeech.SUCCESS);
            }
        });
    }

    public void complete() {
        if (onCompleteListener != null) {
            onCompleteListener.run();
        }
    }

    long timeout = 0;

    public void play(String text) {
        play(text, false);
    }

    public void playOnce(String text) {
        play(text, true);
    }

    private void play(String text, boolean once) {
        try {
            if (TxtUtils.isEmpty(text)) {
                LOG.d("Text is Empty");
                return;
            }
            timeout = System.currentTimeMillis();
            stop();
            if (ttsEngine == null) {
                initTTS(activity);
            }
            ttsEngine.setPitch(AppState.get().ttsPitch);
            if (AppState.get().ttsSpeed == 0.0f) {
                AppState.get().ttsSpeed = 0.01f;
            }
            ttsEngine.setSpeechRate(AppState.get().ttsSpeed);

            if (once) {
                ttsEngine.setPitch(1.0f);
                ttsEngine.setSpeechRate(1.0f);
            }

            text = applyReplacement(text);

            if (false) {
                File dirFolder = new File(AppState.get().ttsSpeakPath, "TTS_" + controller.getCurrentBook().getName());
                if (!dirFolder.exists()) {
                    dirFolder.mkdirs();
                }
                if (!dirFolder.exists()) {
                    Toast.makeText(controller.getActivity(), R.string.file_not_found, Toast.LENGTH_LONG).show();
                    return;
                }
                DecimalFormat df = new DecimalFormat("000");
                int curentPage = controller.getCurentPage();
                if (once) {
                    curentPage++;
                }
                String wav = new File(dirFolder, "page-" + df.format(curentPage) + ".wav").getPath();
                ttsEngine.synthesizeToFile(text, map, wav);
            } else {
                ttsEngine.speak(text, TextToSpeech.QUEUE_FLUSH, map);
            }

            if (once) {
                try {
                    ttsEngine.setOnUtteranceCompletedListener(null);
                } catch (Exception e) {
                    LOG.e(e);
                }
            } else {
                bindCompleteHandler();
            }
        } catch (Exception e) {
            LOG.e(e);
        }
    }

    public void stop() {
        try {
            ttsEngine.setOnUtteranceCompletedListener(null);
            ttsEngine.stop();
        } catch (Exception e) {
            LOG.e(e);
        }
    }


    public boolean isPlaying() {
        if (ttsEngine == null) {
            return false;
        }
        return ttsEngine.isSpeaking();
    }

    public void setOnCompleteListener(Runnable onCompleteListener) {
        this.onCompleteListener = onCompleteListener;
    }

    public boolean hasNoEngines() {
        try {
            return ttsEngine != null && (ttsEngine.getEngines() == null || ttsEngine.getEngines().size() == 0);
        } catch (Exception e) {
            return true;
        }
    }

}
