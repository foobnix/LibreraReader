package com.foobnix.tts;

import android.annotation.TargetApi;
import android.content.Context;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.os.Build;
import android.speech.tts.TextToSpeech;
import android.speech.tts.TextToSpeech.EngineInfo;
import android.speech.tts.TextToSpeech.OnInitListener;
import android.speech.tts.TextToSpeech.OnUtteranceCompletedListener;
import android.widget.Toast;

import com.foobnix.android.utils.LOG;
import com.foobnix.android.utils.MyMath;
import com.foobnix.android.utils.ResultResponse;
import com.foobnix.android.utils.TxtUtils;
import com.foobnix.android.utils.Vibro;
import com.foobnix.ext.CacheZipUtils;
import com.foobnix.model.AppBookmark;
import com.foobnix.model.AppState;
import com.foobnix.model.AppTemp;
import com.foobnix.pdf.info.BookmarksData;
import com.foobnix.pdf.info.R;
import com.foobnix.pdf.info.model.BookCSS;
import com.foobnix.pdf.info.wrapper.DocumentController;
import com.foobnix.sys.TempHolder;

import org.ebookdroid.LibreraApp;
import org.greenrobot.eventbus.EventBus;

import java.io.File;
import java.io.FileFilter;
import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class TTSEngine {

    public static final String FINISHED = "Finished";
    private static final String WAV = ".wav";
    public static final String UTTERANCE_ID_DONE = "LirbiReader";
    private static final String TAG = "TTSEngine";
    volatile TextToSpeech ttsEngine;
    volatile MediaPlayer mp;
    Timer mTimer;
    Object helpObject = new Object();

    private static TTSEngine INSTANCE = new TTSEngine();

    public static TTSEngine get() {
        return INSTANCE;
    }

    HashMap<String, String> map = new HashMap<String, String>();

    {
        map.put(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, UTTERANCE_ID_DONE);
    }

    HashMap<String, String> mapTemp = new HashMap<String, String>();

    {
        mapTemp.put(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, "Temp");
    }

    public void shutdown() {
        LOG.d(TAG, "shutdown");

        synchronized (helpObject) {
            if (ttsEngine != null) {
                ttsEngine.shutdown();
            }
            ttsEngine = null;
        }

    }

    OnInitListener listener = new OnInitListener() {

        @Override
        public void onInit(int status) {
            LOG.d(TAG, "onInit", "SUCCESS", status == TextToSpeech.SUCCESS);
            if (status == TextToSpeech.ERROR) {
                Toast.makeText(LibreraApp.context, R.string.msg_unexpected_error, Toast.LENGTH_LONG).show();
            }

        }
    };

    public TextToSpeech getTTS() {
        return getTTS(null);
    }

    public synchronized TextToSpeech getTTS(OnInitListener onLisnter) {
        if (LibreraApp.context == null) {
            return null;
        }

        synchronized (helpObject) {

            if (TTSEngine.get().isMp3() && mp == null) {
                TTSEngine.get().loadMP3(BookCSS.get().mp3BookPath);
            }

            if (ttsEngine != null) {
                return ttsEngine;
            }
            if (onLisnter == null) {
                onLisnter = listener;
            }
            ttsEngine = new TextToSpeech(LibreraApp.context, onLisnter);
        }

        return ttsEngine;

    }

    public synchronized boolean isShutdown() {
        return ttsEngine == null;
    }

    @TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH_MR1)
    public void stop() {
        LOG.d(TAG, "stop");
        synchronized (helpObject) {

            if (ttsEngine != null) {
                if (Build.VERSION.SDK_INT >= 15) {
                    ttsEngine.setOnUtteranceProgressListener(null);
                } else {
                    ttsEngine.setOnUtteranceCompletedListener(null);
                }
                ttsEngine.stop();
                EventBus.getDefault().post(new TtsStatus());
            }
        }
    }

    public void stopDestroy() {
        LOG.d(TAG, "stop");
        synchronized (helpObject) {
            if (ttsEngine != null) {
                ttsEngine.shutdown();
            }
            ttsEngine = null;
        }
        AppTemp.get().lastBookParagraph = 0;
    }

    public synchronized TextToSpeech setTTSWithEngine(String engine) {
        shutdown();
        synchronized (helpObject) {
            ttsEngine = new TextToSpeech(LibreraApp.context, listener, engine);
        }
        return ttsEngine;
    }

    private String text = "";

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public synchronized void speek(final String text) {
        this.text = text;

        if (AppTemp.get().tempBookPage != AppTemp.get().lastBookPage) {
            AppTemp.get().tempBookPage = AppTemp.get().lastBookPage;
            AppTemp.get().lastBookParagraph = 0;
        }

        LOG.d(TAG, "speek", AppTemp.get().lastBookPage, "par", AppTemp.get().lastBookParagraph);

        if (TxtUtils.isEmpty(text)) {
            return;
        }
        if (ttsEngine == null) {
            LOG.d("getTTS-status was null");
        } else {
            LOG.d("getTTS-status not null");
        }

        ttsEngine = getTTS(new OnInitListener() {

            @Override
            public void onInit(int status) {
                LOG.d("getTTS-status", status);
                if (status == TextToSpeech.SUCCESS) {
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                    }
                    speek(text);
                }
            }
        });

        ttsEngine.setPitch(AppState.get().ttsPitch);
        if (AppState.get().ttsSpeed == 0.0f) {
            AppState.get().ttsSpeed = 0.01f;
        }
        ttsEngine.setSpeechRate(AppState.get().ttsSpeed);
        LOG.d(TAG, "Speek s", AppState.get().ttsSpeed);
        LOG.d(TAG, "Speek AppTemp.get().lastBookParagraph", AppTemp.get().lastBookParagraph);

        if (AppState.get().ttsPauseDuration > 0 && text.contains(TxtUtils.TTS_PAUSE)) {
            String[] parts = text.split(TxtUtils.TTS_PAUSE);
            ttsEngine.speak(" ", TextToSpeech.QUEUE_FLUSH, mapTemp);
            for (int i = AppTemp.get().lastBookParagraph; i < parts.length; i++) {

                String big = parts[i];
                big = big.trim();
                if (TxtUtils.isNotEmpty(big)) {

                    HashMap<String, String> mapTemp1 = new HashMap<String, String>();
                    mapTemp1.put(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, FINISHED + i);

                    ttsEngine.speak(big, TextToSpeech.QUEUE_ADD, mapTemp1);
                    ttsEngine.playSilence(AppState.get().ttsPauseDuration, TextToSpeech.QUEUE_ADD, mapTemp);
                    LOG.d("pageHTML-parts", i, big);
                }
            }
            ttsEngine.playSilence(0L, TextToSpeech.QUEUE_ADD, map);
        } else {
            String textToPlay = text.replace(TxtUtils.TTS_PAUSE, "");
            LOG.d("pageHTML-parts-single", text);
            ttsEngine.speak(textToPlay, TextToSpeech.QUEUE_FLUSH, map);
        }

    }

    public void speakToFile(final DocumentController controller, final ResultResponse<String> info) {
        File dirFolder = new File(BookCSS.get().ttsSpeakPath, "TTS_" + controller.getCurrentBook().getName());
        if (!dirFolder.exists()) {
            dirFolder.mkdirs();
        }
        if (!dirFolder.exists()) {
            info.onResultRecive(controller.getActivity().getString(R.string.file_not_found) + " " + dirFolder.getPath());
            return;
        }
        CacheZipUtils.removeFiles(dirFolder.listFiles(new FileFilter() {

            @Override
            public boolean accept(File pathname) {
                return pathname.getName().endsWith(WAV);
            }
        }));

        String path = dirFolder.getPath();
        speakToFile(controller, 0, path, info);
    }

    public void speakToFile(final DocumentController controller, final int page, final String folder, final ResultResponse<String> info) {
        LOG.d("speakToFile", page, controller.getPageCount());
        if (ttsEngine == null) {
            LOG.d("TTS is null");
            if (controller != null && controller.getActivity() != null) {
                Toast.makeText(controller.getActivity(), R.string.msg_unexpected_error, Toast.LENGTH_SHORT).show();
            }
            return;
        }

        if (page >= controller.getPageCount() || !TempHolder.isRecordTTS) {
            LOG.d("speakToFile finish", page, controller.getPageCount());
            info.onResultRecive((controller.getActivity().getString(R.string.success)));
            return;
        }

        info.onResultRecive((page + 1) + " / " + controller.getPageCount());

        DecimalFormat df = new DecimalFormat("0000");
        String pageName = "page-" + df.format(page + 1);
        final String wav = new File(folder, pageName + WAV).getPath();
        String fileText = controller.getTextForPage(page);

        ttsEngine.synthesizeToFile(fileText, map, wav);

        TTSEngine.get().getTTS().setOnUtteranceCompletedListener(new OnUtteranceCompletedListener() {

            @Override
            public void onUtteranceCompleted(String utteranceId) {
                LOG.d("speakToFile onUtteranceCompleted", page, controller.getPageCount());
                speakToFile(controller, page + 1, folder, info);
            }

        });

    }


    public static AppBookmark fastTTSBookmakr(DocumentController dc) {
        return fastTTSBookmakr(dc.getActivity(),dc.getCurrentBook().getPath(), dc.getCurentPageFirst1(), dc.getPageCount());

    }

    public static AppBookmark fastTTSBookmakr(Context c, String bookPath, int page, int pages) {
        LOG.d("fastTTSBookmakr", page, pages);

        if (pages == 0) {
            LOG.d("fastTTSBookmakr skip");
            return null;
        }
        boolean hasBookmark = BookmarksData.get().hasBookmark(bookPath, page, pages);

        if (!hasBookmark) {
            final AppBookmark bookmark = new AppBookmark(bookPath, c.getString(R.string.fast_bookmark), MyMath.percent(page, pages));
            BookmarksData.get().add(bookmark);

            String TEXT = c.getString(R.string.fast_bookmark) + " " + TxtUtils.LONG_DASH1 + " " + c.getString(R.string.page) + " " + page + "";
            Toast.makeText(c, TEXT, Toast.LENGTH_SHORT).show();
            return bookmark;
        }
        Vibro.vibrate();
        return null;


    }

    public synchronized boolean isPlaying() {
        if (TempHolder.isRecordTTS) {
            return false;
        }
        if (isMp3()) {
            return mp != null && mp.isPlaying();
        }

        synchronized (helpObject) {
            if (ttsEngine == null) {
                return false;
            }
            return ttsEngine != null && ttsEngine.isSpeaking();
        }
    }


    public boolean hasNoEngines() {
        try {
            return ttsEngine != null && (ttsEngine.getEngines() == null || ttsEngine.getEngines().size() == 0);
        } catch (Exception e) {
            return true;
        }
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public String getCurrentLang() {
        try {
            if (ttsEngine != null && Build.VERSION.SDK_INT >= 21) {
                return ttsEngine.getDefaultVoice().getLocale().getDisplayLanguage();
            }
        } catch (Exception e) {
            LOG.e(e);
        }
        return "---";
    }

    public int getEngineCount() {
        try {
            if (ttsEngine == null || ttsEngine.getEngines() == null) {
                return -1;
            }

            return ttsEngine.getEngines().size();
        } catch (Exception e) {
            LOG.e(e);
        }
        return 0;
    }

    public String getCurrentEngineName() {
        try {
            if (ttsEngine != null) {
                String enginePackage = ttsEngine.getDefaultEngine();
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
        return "---";
    }

    public static String engineToString(EngineInfo info) {
        return info.label;
    }

    public void loadMP3(String ttsPlayMp3Path) {
        loadMP3(ttsPlayMp3Path, false);
    }

    public void loadMP3(String ttsPlayMp3Path, final boolean play) {
        try {
            mp3Destroy();
            mp = new MediaPlayer();
            mp.setDataSource(ttsPlayMp3Path);
            mp.prepare();
            mp.setOnCompletionListener(new OnCompletionListener() {

                @Override
                public void onCompletion(MediaPlayer mp) {
                    mp.pause();
                }
            });
            if (play) {
                mp.start();
            }

            mTimer = new Timer();

            mTimer.schedule(new TimerTask() {

                @Override
                public void run() {
                    AppState.get().mp3seek = mp.getCurrentPosition();
                    //LOG.d("Run timer-task");
                    EventBus.getDefault().post(new TtsStatus());
                }

                ;
            }, 1000, 1000);

        } catch (Exception e) {
            LOG.e(e);
        }
    }

    public MediaPlayer getMP() {
        return mp;
    }

    public void mp3Destroy() {
        if (mp != null) {
            mp.stop();
            mp.reset();
            mp = null;
            if (mTimer != null) {
                mTimer.purge();
                mTimer.cancel();
                mTimer = null;
            }
        }
        LOG.d("mp3Desproy");
    }

    public void mp3Next() {
        int seek = mp.getCurrentPosition();
        mp.seekTo(seek + 5 * 1000);
    }

    public void mp3Prev() {
        int seek = mp.getCurrentPosition();
        mp.seekTo(seek - 5 * 1000);
    }

    public boolean isMp3PlayPause() {
        if (isMp3()) {
            if (mp == null) {
                loadMP3(BookCSS.get().mp3BookPath);
            }
            if (mp.isPlaying()) {
                mp.pause();
            } else {
                mp.start();
            }
            TTSNotification.showLast();
            return true;
        }
        return false;
    }

    public void playMp3() {
        if (mp != null) {
            mp.start();
        }
    }

    public void pauseMp3() {
        if (mp != null) {
            mp.pause();
        }
    }

    public boolean isMp3() {
        return TxtUtils.isNotEmpty(BookCSS.get().mp3BookPath);
    }

    public void seekTo(int i) {
        if (mp != null) {
            mp.seekTo(i);
        }

    }

}
