package com.foobnix.tts;

import org.ebookdroid.LirbiApp;
import org.ebookdroid.core.codec.CodecDocument;
import org.ebookdroid.core.codec.CodecPage;
import org.greenrobot.eventbus.EventBus;

import com.foobnix.android.utils.LOG;
import com.foobnix.android.utils.TxtUtils;
import com.foobnix.pdf.info.R;
import com.foobnix.pdf.info.wrapper.AppState;
import com.foobnix.sys.TempHolder;

import android.annotation.TargetApi;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;
import android.os.Vibrator;
import android.speech.tts.TextToSpeech.OnUtteranceCompletedListener;

public class TTSService extends Service {

    public static final String EXTRA_TEXT = "TEXT";
    public static final String EXTRA_INT = "INT";

    private static final String TAG = "TTSService";

    public static String ACTION_PLAY_CURRENT_PAGE = "ACTION_PLAY_CURRENT_PAGE";

    public TTSService() {
        LOG.d(TAG, "Create");
    }

    @Override
    public void onCreate() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    public static void playBookPage(int page) {
        Intent intent = new Intent(LirbiApp.context, TTSService.class).setAction(TTSService.ACTION_PLAY_CURRENT_PAGE);
        intent.putExtra(EXTRA_INT, page);
        LirbiApp.context.startService(intent);

        EventBus.getDefault().post(new MessagePageNumber(page));
    }

    public static void playBookPage(String preText, int page) {
        Intent intent = new Intent(LirbiApp.context, TTSService.class).setAction(TTSService.ACTION_PLAY_CURRENT_PAGE);
        intent.putExtra(EXTRA_TEXT, preText);
        intent.putExtra(EXTRA_INT, page);
        LirbiApp.context.startService(intent);

        EventBus.getDefault().post(new MessagePageNumber(page));
    }

    private int currentPage;

    @TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH_MR1)
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent == null) {
            return START_STICKY;
        }
        LOG.d(TAG, intent.getAction(), intent.getExtras());

        if (TTSNotification.TTS_STOP.equals(intent.getAction())) {
            TTSEngine.get().stop();
        }
        if (TTSNotification.TTS_READ.equals(intent.getAction())) {
            TTSEngine.get().stop();
            playBookPage(currentPage);
        }
        if (TTSNotification.TTS_NEXT.equals(intent.getAction())) {
            TTSEngine.get().stop();
            playBookPage(currentPage + 1);
        }

        if (ACTION_PLAY_CURRENT_PAGE.equals(intent.getAction())) {
            int pageNumber = intent.getIntExtra(EXTRA_INT, -1);
            String preText = intent.getStringExtra(EXTRA_TEXT);
            LOG.d(TAG, ACTION_PLAY_CURRENT_PAGE, pageNumber);
            if (pageNumber != -1) {
                currentPage = pageNumber;
                CodecDocument dc = TempHolder.get().codecDocument;
                if (dc == null) {
                    LOG.d(TAG, "CodecDocument", "is NULL");
                    return START_STICKY;
                }
                if (pageNumber >= dc.getPageCount()) {
                    LOG.d(TAG, "CodecDocument", "is NULL");

                    Vibrator v = (Vibrator) LirbiApp.context.getSystemService(Context.VIBRATOR_SERVICE);
                    if (AppState.get().isVibration) {
                        v.vibrate(1000);
                    }

                    try {
                        Thread.sleep(2000);
                    } catch (InterruptedException e) {
                    }
                    TTSEngine.get().getTTS().setOnUtteranceCompletedListener(null);
                    TTSEngine.get().speek(LirbiApp.context.getString(R.string.the_book_is_over));
                    return START_STICKY;
                }
                CodecPage page = dc.getPage(pageNumber);
                String pageHTML = page.getPageHTML();
                pageHTML = TxtUtils.replaceHTMLforTTS(pageHTML);
                LOG.d(TAG, pageHTML);

                String[] parts = TxtUtils.getParts(pageHTML);
                String firstPart = parts[0];
                final String secondPart = parts[1];

                if (TxtUtils.isNotEmpty(preText)) {
                    firstPart = preText + firstPart;
                }

                TTSEngine.get().speek(firstPart);
                TTSNotification.show(TempHolder.get().path, pageNumber + 1);

                TTSEngine.get().getTTS().setOnUtteranceCompletedListener(new OnUtteranceCompletedListener() {

                    @Override
                    public void onUtteranceCompleted(String utteranceId) {
                        LOG.d(TAG, "onUtteranceCompleted");
                        playBookPage(secondPart, currentPage + 1);
                    }

                });

            }

        }

        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        LOG.d(TAG, "onDestroy");
    }

}
