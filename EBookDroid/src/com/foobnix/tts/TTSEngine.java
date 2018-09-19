package com.foobnix.tts;

import java.io.File;
import java.io.FileFilter;
import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.List;

import org.ebookdroid.LibreraApp;
import org.greenrobot.eventbus.EventBus;

import com.foobnix.android.utils.LOG;
import com.foobnix.android.utils.ResultResponse;
import com.foobnix.android.utils.TxtUtils;
import com.foobnix.android.utils.Vibro;
import com.foobnix.ext.CacheZipUtils;
import com.foobnix.pdf.info.AppSharedPreferences;
import com.foobnix.pdf.info.R;
import com.foobnix.pdf.info.wrapper.AppBookmark;
import com.foobnix.pdf.info.wrapper.AppState;
import com.foobnix.pdf.info.wrapper.DocumentController;
import com.foobnix.sys.TempHolder;

import android.annotation.TargetApi;
import android.content.Context;
import android.os.Build;
import android.speech.tts.TextToSpeech;
import android.speech.tts.TextToSpeech.EngineInfo;
import android.speech.tts.TextToSpeech.OnInitListener;
import android.speech.tts.TextToSpeech.OnUtteranceCompletedListener;
import android.widget.Toast;

public class TTSEngine {

    private static final String WAV = ".wav";
    public static final String UTTERANCE_ID_DONE = "LirbiReader";
    private static final String TAG = "TTSEngine";
    volatile TextToSpeech ttsEngine;
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
            ttsEngine = null;
        }
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
        LOG.d(TAG, "speek", text);
        if (TxtUtils.isEmpty(text)) {
            return;
        }
        ttsEngine = getTTS(new OnInitListener() {

            @Override
            public void onInit(int status) {
                if (status == TextToSpeech.SUCCESS) {
                    speek(text);
                }
            }
        });

        ttsEngine.setPitch(AppState.get().ttsPitch);
        if (AppState.get().ttsSpeed == 0.0f) {
            AppState.get().ttsSpeed = 0.01f;
        }
        ttsEngine.setSpeechRate(AppState.get().ttsSpeed);
        LOG.d(TAG, "Speek speed", AppState.get().ttsSpeed);

        if (AppState.get().ttsPauseDuration > 0 && text.contains(TxtUtils.TTS_PAUSE)) {
            String[] parts = text.split(TxtUtils.TTS_PAUSE);
            ttsEngine.speak(" ", TextToSpeech.QUEUE_FLUSH, mapTemp);
            for (String big : parts) {
                big = big.trim();
                if (TxtUtils.isNotEmpty(big)) {
                    ttsEngine.speak(big, TextToSpeech.QUEUE_ADD, mapTemp);
                    ttsEngine.playSilence(AppState.get().ttsPauseDuration, TextToSpeech.QUEUE_ADD, mapTemp);
                    LOG.d("pageHTML-parts", big);
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
        File dirFolder = new File(AppState.get().ttsSpeakPath, "TTS_" + controller.getCurrentBook().getName());
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

        speakToFile(controller, 0, dirFolder.getPath(), info);
    }

    public void speakToFile(final DocumentController controller, final int page, final String folder, final ResultResponse<String> info) {
        LOG.d("speakToFile", page, controller.getPageCount());

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

    public static void fastTTSBookmakr(Context c) {
        int page = AppState.get().lastBookPage + 1;
        boolean hasBookmark = AppSharedPreferences.get().hasBookmark(AppState.get().lastBookPath, page);

        if (!hasBookmark) {
            final AppBookmark bookmark = new AppBookmark(AppState.get().lastBookPath, c.getString(R.string.fast_bookmark), page, AppState.get().lastBookTitle);
            AppSharedPreferences.get().addBookMark(bookmark);

        }
        Vibro.vibrate();
        LOG.d("Fast-bookmark", AppState.get().lastBookPage);
    }

    public synchronized boolean isPlaying() {
        if (TempHolder.isRecordTTS) {
            return false;
        }
        synchronized (helpObject) {
            if (ttsEngine == null) {
                return false;
            }
            return ttsEngine != null && ttsEngine.isSpeaking();
        }
    }

    public void playCurrent() {
        speek(text);
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

}
