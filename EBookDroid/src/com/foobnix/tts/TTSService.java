package com.foobnix.tts;

import org.ebookdroid.LirbiApp;
import org.ebookdroid.core.codec.CodecDocument;
import org.ebookdroid.core.codec.CodecPage;
import org.greenrobot.eventbus.EventBus;

import com.foobnix.android.utils.Dips;
import com.foobnix.android.utils.LOG;
import com.foobnix.android.utils.TxtUtils;
import com.foobnix.pdf.info.R;
import com.foobnix.pdf.info.wrapper.AppState;
import com.foobnix.sys.ImageExtractor;
import com.foobnix.sys.TempHolder;

import android.annotation.TargetApi;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;
import android.os.Vibrator;
import android.speech.tts.TextToSpeech.OnUtteranceCompletedListener;
import android.speech.tts.UtteranceProgressListener;

public class TTSService extends Service {

    public static final String EXTRA_PATH = "EXTRA_PATH";
    public static final String EXTRA_INT = "INT";

    private static final String TAG = "TTSService";

    public static String ACTION_PLAY_CURRENT_PAGE = "ACTION_PLAY_CURRENT_PAGE";

    public TTSService() {
        LOG.d(TAG, "Create constructor");

    }

    @Override
    public void onCreate() {
        LOG.d(TAG, "Create");
        AppState.get().load(getApplicationContext());
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    public static void playBookPage(int page, String path) {
        TTSEngine.get().stop();

        Intent intent = playBookIntent(page, path);

        LirbiApp.context.startService(intent);
    }

    public static Intent playBookIntent(int page, String path) {
        Intent intent = new Intent(LirbiApp.context, TTSService.class);
        intent.setAction(TTSService.ACTION_PLAY_CURRENT_PAGE);
        intent.putExtra(EXTRA_INT, page);
        intent.putExtra(EXTRA_PATH, path);
        return intent;
    }

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
            playPage("", AppState.get().lastBookPage);
        }
        if (TTSNotification.TTS_NEXT.equals(intent.getAction())) {
            TTSEngine.get().stop();
            playPage("", AppState.get().lastBookPage + 1);
        }

        if (ACTION_PLAY_CURRENT_PAGE.equals(intent.getAction())) {
            int pageNumber = intent.getIntExtra(EXTRA_INT, -1);
            AppState.get().lastBookPath = intent.getStringExtra(EXTRA_PATH);

            if (pageNumber != -1) {
                playPage("", pageNumber);
            }

        }

        return START_STICKY;
    }

    public CodecDocument getDC() {
        return ImageExtractor.getCodecContext(AppState.get().lastBookPath, "", Dips.screenWidth(), Dips.screenHeight());
    }

    @TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH_MR1)
    private void playPage(String preText, int pageNumber) {
        if (pageNumber != -1) {
            EventBus.getDefault().post(new MessagePageNumber(pageNumber));
            AppState.get().lastBookPage = pageNumber;
            CodecDocument dc = getDC();
            if (dc == null) {
                LOG.d(TAG, "CodecDocument", "is NULL");
                return;
            }
            AppState.get().save(getApplicationContext());

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
                return;
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

            if (Build.VERSION.SDK_INT >= 15) {
                TTSEngine.get().getTTS().setOnUtteranceProgressListener(new UtteranceProgressListener() {

                    @Override
                    public void onStart(String utteranceId) {

                    }

                    @Override
                    public void onError(String utteranceId) {
                        TTSEngine.get().stop();
                    }

                    @Override
                    public void onDone(String utteranceId) {
                        LOG.d(TAG, "onUtteranceCompleted");
                        playPage(secondPart, AppState.get().lastBookPage + 1);

                    }
                });
            } else {
                TTSEngine.get().getTTS().setOnUtteranceCompletedListener(new OnUtteranceCompletedListener() {

                    @Override
                    public void onUtteranceCompleted(String utteranceId) {
                        LOG.d(TAG, "onUtteranceCompleted");
                        playPage(secondPart, AppState.get().lastBookPage + 1);

                    }
                });
            }

            TTSNotification.show(TempHolder.get().path, pageNumber + 1);
            TTSEngine.get().speek(firstPart);

        }

    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        LOG.d(TAG, "onDestroy");
    }

}
