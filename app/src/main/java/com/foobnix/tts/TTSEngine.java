package com.foobnix.tts;

import android.annotation.TargetApi;
import android.content.Context;
import android.icu.util.ULocale;
import android.media.AudioAttributes;
import android.media.AudioFocusRequest;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.content.Intent;
import android.content.pm.ResolveInfo;
import android.media.MediaPlayer.OnCompletionListener;
import android.provider.Settings;
import android.os.Build;
import android.os.PowerManager;
import android.speech.tts.TextToSpeech;
import android.speech.tts.TextToSpeech.EngineInfo;
import android.speech.tts.TextToSpeech.OnInitListener;
import android.speech.tts.TextToSpeech.OnUtteranceCompletedListener;
import android.support.v4.media.session.MediaSessionCompat;
import android.widget.Toast;

import com.foobnix.android.utils.IO;
import com.foobnix.android.utils.LOG;
import com.foobnix.android.utils.MyMath;
import com.foobnix.android.utils.ResultResponse;
import com.foobnix.android.utils.TxtUtils;
import com.foobnix.android.utils.Vibro;
import com.foobnix.mobi.parser.IOUtils;
import com.foobnix.mobi.parser.MobiParserIS;
import com.foobnix.model.AppBookmark;
import com.foobnix.model.AppSP;
import com.foobnix.model.AppState;
import com.foobnix.pdf.info.BookmarksData;
import com.foobnix.pdf.info.R;
import com.foobnix.pdf.info.model.BookCSS;
import com.foobnix.pdf.info.wrapper.DocumentController;
import com.foobnix.sys.TempHolder;
import com.github.axet.lamejni.Lame;

import com.foobnix.LibreraApp;

import org.greenrobot.eventbus.EventBus;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;

public class TTSEngine {

    public static final String FINISHED_SIGNAL = "Finished";
    public static final String STOP_SIGNAL = "Stoped";
    public static final String UTTERANCE_ID_DONE = "LirbiReader";
    public static final String UNKNOWN = "---";
    public static final String WAV = ".wav";
    public static final String MP3 = ".mp3";
    private static final String TAG = "TTSEngine";
    private static TTSEngine INSTANCE = new TTSEngine();
    volatile TextToSpeech ttsEngine;
    // The engine Librera asked for by name, or null when it left the choice to the system.
    private volatile String enginePackage;
    private boolean initialized;
    private boolean initializationFailed;
    private String pendingText;
    private int generation;
    private final List<OnInitListener> lateInitListeners = new ArrayList<OnInitListener>();
    volatile MediaPlayer mp;
    /**
     * Whether the player is ready to play. A file is prepared at once; a stream from the network
     * prepares in the background, and until then the player may not be started, paused, asked
     * its position or moved - each of those would put it into its error state.
     */
    private volatile boolean mpPrepared;
    /** A play asked for while the stream still prepares: it starts when it is ready. */
    private volatile boolean mpStartWhenPrepared;
    /**
     * Whether the player was started and not paused since. MediaPlayer.isPlaying() is false for a
     * while each time a stream fills its buffer again, after a pause too: asked then, a second tap
     * on play/pause took the radio for stopped and started it once more instead of pausing it.
     */
    private volatile boolean mp3Started;
    Timer mTimer;
    Object helpObject = new Object();
    HashMap<String, String> map = new HashMap<String, String>();
    HashMap<String, String> mapTemp = new HashMap<String, String>();

    public boolean isInit() {
        return ttsEngine != null;
    }

    /**
     * @return true once the engine reported a successful onInit - before that nothing can be
     *         asked about the voice or the language.
     */
    public boolean isInitialized() {
        synchronized (helpObject) {
            return ttsEngine != null && initialized;
        }
    }

    /**
     * @return true when an initialized engine has no language it can actually speak, which is
     *         what the system TTS settings are offered for. Only an initialized engine can
     *         answer this: while it is still connecting the language is not known yet.
     */
    public boolean hasUnknownLanguage() {
        return isInitialized() && UNKNOWN.equals(getCurrentLang());
    }

    OnInitListener listener = new OnInitListener() {

        @Override public void onInit(int status) {
            LOG.d(TAG, "onInit", "SUCCESS", status == TextToSpeech.SUCCESS);
            if (status == TextToSpeech.ERROR) {
                Toast.makeText(LibreraApp.context, R.string.msg_unexpected_error, Toast.LENGTH_LONG)
                     .show();
            }

        }
    };
    private String text = "";

    {
        map.put(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, UTTERANCE_ID_DONE);
    }

    {
        mapTemp.put(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, "Temp");
    }

    public static TTSEngine get() {
        return INSTANCE;
    }

    public static AppBookmark fastTTSBookmakr(DocumentController dc) {
        AppBookmark bookmark = fastTTSBookmakr(dc.getActivity(), dc.getCurrentBook()
                                                                   .getPath(), dc.getCurentPageFirst1(), dc.getPageCount());
        if (bookmark != null) {
            bookmark.pt = dc.getBookmarkText();
            BookmarksData.get().add(bookmark);
        }
        return bookmark;
    }

    public static AppBookmark fastTTSBookmakr(Context c, String bookPath, int page, int pages) {
        LOG.d("fastTTSBookmakr", page, pages);

        if (pages == 0) {
            LOG.d("fastTTSBookmakr skip");
            return null;
        }
        boolean hasBookmark = BookmarksData.get()
                                           .hasBookmark(bookPath, page, pages);

        if (!hasBookmark) {
            final AppBookmark bookmark =
                    new AppBookmark(bookPath, c.getString(R.string.fast_bookmark), MyMath.percent(page, pages));
            BookmarksData.get()
                         .add(bookmark);

            String TEXT = c.getString(R.string.fast_bookmark) + " " + TxtUtils.LONG_DASH1 + " " + c.getString(
                    R.string.page) + " " + page + "";
            Toast.makeText(c, TEXT, Toast.LENGTH_SHORT)
                 .show();
            return bookmark;
        }
        Vibro.vibrate();
        return null;

    }

    public static String engineToString(EngineInfo info) {
        return info.label;
    }

    public void shutdown() {
        LOG.d(TAG, "shutdown");

        synchronized (helpObject) {
            generation++;
            initialized = false;
            initializationFailed = false;
            pendingText = null;
            lateInitListeners.clear();
            if (ttsEngine != null) {
                ttsEngine.shutdown();
            }
            ttsEngine = null;
            enginePackage = null;
        }

    }

    public TextToSpeech getTTS() {
        return getTTS(null);
    }

    public TextToSpeech getTTS(OnInitListener onLisnter) {
        if (LibreraApp.context == null) {
            return null;
        }

        synchronized (helpObject) {

            if (TTSEngine.get()
                         .isMp3() && mp == null) {
                TTSEngine.get()
                         .loadMP3(BookCSS.get()
                                         .mp3BookPathGet());
            }

            if (initializationFailed) {
                shutdown();
            }
            if (ttsEngine != null) {
                // The engine is shared, so a caller that arrives while it is still connecting
                // would never hear about initialization: it owns no OnInitListener of its own.
                if (!initialized && onLisnter != null) {
                    lateInitListeners.add(onLisnter);
                }
                return ttsEngine;
            }
            final OnInitListener callback = onLisnter == null ? listener : onLisnter;
            // The system default engine can point at a package that is no longer installed
            // (Android then logs "is not allowed to bind to private engine" and stays silent).
            // In that case pick an engine that is actually present instead of inheriting the
            // broken default.
            final String fallback = resolveUsableEngine(LibreraApp.context);
            enginePackage = fallback;
            final int currentGeneration = ++generation;
            initialized = false;
            OnInitListener init = status -> {
                final List<OnInitListener> waiting = new ArrayList<OnInitListener>();
                synchronized (helpObject) {
                    if (generation != currentGeneration) {
                        return;
                    }
                    initialized = status == TextToSpeech.SUCCESS;
                    initializationFailed = !initialized;
                    if (!initialized) {
                        pendingText = null;
                    }
                    waiting.add(callback);
                    waiting.addAll(lateInitListeners);
                    lateInitListeners.clear();
                }
                try {
                    for (OnInitListener waiter : waiting) {
                        notifyInit(waiter, status);
                    }
                } finally {
                    synchronized (helpObject) {
                        if (generation == currentGeneration && initialized && pendingText != null) {
                            String queued = pendingText;
                            pendingText = null;
                            speekLocked(queued);
                        }
                    }
                }
            };
            if (fallback != null) {
                LOG.d(TAG, "default TTS engine unusable, falling back to", fallback);
                ttsEngine = new TextToSpeech(LibreraApp.context, init, fallback);
            } else {
                ttsEngine = new TextToSpeech(LibreraApp.context, init);
            }
        }

        return ttsEngine;

    }

    private static void notifyInit(OnInitListener waiter, int status) {
        try {
            waiter.onInit(status);
        } catch (Exception e) {
            // onInit arrives on the main thread: one broken listener must not take down the
            // others, nor the queued speech that is drained afterwards.
            LOG.e(e);
        }
    }

    /**
     * @return package of an installed engine to use, or null when the system default is fine.
     */
    static String resolveUsableEngine(Context c) {
        try {
            final List<ResolveInfo> installed = installedEngines(c);
            if (installed.isEmpty()) {
                return null;
            }

            String defaultEngine = null;
            try {
                defaultEngine = Settings.Secure.getString(c.getContentResolver(),
                        Settings.Secure.TTS_DEFAULT_SYNTH);
            } catch (Exception e) {
                LOG.e(e);
            }

            String google = null;
            String first = null;
            for (ResolveInfo info : installed) {
                if (info == null || info.serviceInfo == null) {
                    continue;
                }
                final String pkg = info.serviceInfo.packageName;
                if (first == null) {
                    first = pkg;
                }
                if ("com.google.android.tts".equals(pkg)) {
                    google = pkg;
                }
                if (pkg.equals(defaultEngine)) {
                    // The configured default is installed - leave the system to use it.
                    return null;
                }
            }
            return google != null ? google : first;
        } catch (Exception e) {
            LOG.e(e);
            return null;
        }
    }

    private static List<ResolveInfo> installedEngines(Context c) {
        final List<ResolveInfo> installed = c.getPackageManager()
                .queryIntentServices(new Intent(TextToSpeech.Engine.INTENT_ACTION_TTS_SERVICE), 0);
        return installed == null ? new ArrayList<ResolveInfo>() : installed;
    }

    public synchronized boolean isShutdown() {
        return ttsEngine == null;
    }

    public void stop() {
        stop(null);
    }

    @TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH_MR1) public void stop(MediaSessionCompat mediaSessionCompat) {
        // Every way of stopping comes here - the reader stops it when it restarts for a setting -
        // and a sentence left marked would look as if it were still being read
        ReadingHighlight.clear();
        // The media session is left active: a paused player keeps it, and switching it off
        // even for a moment may hand the headphone buttons to another app's session
        if (!AppState.get().mixWithOtherApps) {
            try {

                TTSService.abandonAudioFocusCompat();
            } catch (Exception e) {
                LOG.e(e);
            }
        }

        LOG.d(TAG, "stop");
        synchronized (helpObject) {
            pendingText = null;

            if (ttsEngine != null) {
                if (Build.VERSION.SDK_INT >= 15) {
                    ttsEngine.setOnUtteranceProgressListener(null);
                } else {
                    ttsEngine.setOnUtteranceCompletedListener(null);
                }
                ttsEngine.stop();
                EventBus.getDefault()
                        .post(new TtsStatus());
            }
        }
    }

    public void stopDestroy() {
        LOG.d(TAG, "stop");
        TxtUtils.dictHash = "";
        shutdown();
        AppSP.get().lastBookParagraph = 0;
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP) public void speek(final String text) {
        synchronized (helpObject) {
            speekLocked(text);
        }
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP) private void speekLocked(final String text) {
        this.text = text;

        if (AppSP.get().tempBookPage != AppSP.get().lastBookPage) {
            AppSP.get().tempBookPage = AppSP.get().lastBookPage;
            AppSP.get().lastBookParagraph = 0;
        }

        LOG.d(TAG, "speek", AppSP.get().lastBookPage, "par", AppSP.get().lastBookParagraph);

        if (TxtUtils.isEmpty(text)) {
            return;
        }
        if (ttsEngine == null) {
            LOG.d("getTTS-status was null");
        } else {
            LOG.d("getTTS-status not null");
        }

        ttsEngine = getTTS();

        if (ttsEngine == null) {
            LOG.d(TAG, "speek: no TTS engine available");
            return;
        }
        if (!initialized) {
            LOG.d(TAG, "speek: waiting for TTS initialization");
            pendingText = text;
            return;
        }
        pendingText = null;

        ttsEngine.setPitch(AppState.get().ttsPitch);
        if (AppState.get().ttsSpeed == 0.0f) {
            AppState.get().ttsSpeed = 0.01f;
        }
        ttsEngine.setSpeechRate(AppState.get().ttsSpeed);
        LOG.d(TAG, "Speek s", AppState.get().ttsSpeed);
        LOG.d(TAG, "Speek AppSP.get().lastBookParagraph", AppSP.get().lastBookParagraph);

        if (AppState.get().ttsPauseDuration > 0 && text.contains(TxtUtils.TTS_PAUSE)) {
            String[] parts = text.split(TxtUtils.TTS_PAUSE);
            ttsEngine.playSilence(0l, TextToSpeech.QUEUE_FLUSH, mapTemp);
            for (int i = AppSP.get().lastBookParagraph; i < parts.length; i++) {

                String big = parts[i];
                big = big.trim();

                if (TxtUtils.isNotEmpty(big)) {
                    if (big.length() == 1 && !Character.isLetterOrDigit(big.charAt(0))) {
                        LOG.d("Skip: " + big);
                        continue;

                    }
                    if (big.contains(TxtUtils.TTS_SKIP)) {
                        continue;
                    }

                    if (big.contains(TxtUtils.TTS_STOP)) {
                        HashMap<String, String> mapStop = new HashMap<String, String>();
                        mapStop.put(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, STOP_SIGNAL);
                        ttsEngine.playSilence(AppState.get().ttsPauseDuration, TextToSpeech.QUEUE_ADD, mapStop);
                        LOG.d("Add stop signal");
                    }
                    if (big.contains(TxtUtils.TTS_NEXT)) {
                        ttsEngine.playSilence(0L, TextToSpeech.QUEUE_ADD, map);
                        LOG.d("next-page signal");
                        break;
                    }

                    HashMap<String, String> mapTemp1 = new HashMap<String, String>();
                    mapTemp1.put(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, FINISHED_SIGNAL + i);

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

    public boolean isTempPausing() {
        if (AppState.get().isEnableAccessibility) {
            return true;
        }
        return mp != null || ttsEngine != null;
    }

    public boolean isPlaying() {
        if (TempHolder.isRecordTTS) {
            return false;
        }
        if (isMp3()) {
            final MediaPlayer player = mp;
            return player != null && (mpPrepared ? mp3Started && !mp3PausedForFocus : mpStartWhenPrepared);
        }

        synchronized (helpObject) {
            if (ttsEngine == null) {
                return false;
            }
            return ttsEngine != null && ttsEngine.isSpeaking();
        }
    }

    public boolean hasNoEngines() {
        return getEngineCount() == 0;
    }

    /**
     * @return the display name of the language the engine speaks, or {@link #UNKNOWN} when it
     *         has none it can speak: an engine without voice data accepts a speech request and
     *         then never answers it, so an unusable language has to read as no language.
     */
    @SuppressWarnings("deprecation") public String getCurrentLang() {
        try {
            TextToSpeech engine = ttsEngine;
            // Both answers come from the engine's own settings - the client caches the language
            // it was handed before onInit, and availability is a single query. getDefaultVoice()
            // would instead fetch the whole voice list over Binder on every call.
            Locale language = engine != null ? engine.getLanguage() : null;
            if (language != null && !language.getLanguage()
                                             .isEmpty() &&
                engine.isLanguageAvailable(language) >= TextToSpeech.LANG_AVAILABLE) {
                // The language arrives as a legacy three-letter code ("deu"), which ICU - unlike
                // java.util.Locale - resolves to a display name.
                return new ULocale(language.getLanguage()).getDisplayLanguage();
            }
        } catch (Exception e) {
            LOG.e(e);
        }
        return UNKNOWN;
    }

    /**
     * @return how many engines are installed, or -1 when that cannot be told. Asked of the
     *         package manager rather than of a TextToSpeech, which may not have been created yet.
     */
    public int getEngineCount() {
        try {
            return LibreraApp.context == null ? -1 : installedEngines(LibreraApp.context).size();
        } catch (Exception e) {
            LOG.e(e);
        }
        return -1;
    }

    public String getCurrentEngineName() {
        try {
            if (ttsEngine != null) {
                // The engine Librera named is the one it is bound to. Otherwise it is the system
                // default - which Android only reports when one is set or is part of the system
                // image, so without Google's engine it is often null.
                String enginePackage = this.enginePackage != null ? this.enginePackage : ttsEngine.getDefaultEngine();
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
        return UNKNOWN;
    }

    public void loadMP3(String ttsPlayMp3Path) {
        loadMP3(ttsPlayMp3Path, false);
    }

    public void loadMP3(String ttsPlayMp3Path, final boolean play) {
        LOG.d("loadMP3-", ttsPlayMp3Path);
        final boolean stream = StreamPlaylist.isStream(ttsPlayMp3Path);
        if (TxtUtils.isEmpty(ttsPlayMp3Path) || (!stream && !new File(ttsPlayMp3Path).isFile())) {
            LOG.d("loadMP3-skip mp3");
            return;
        }
        try {
            mp3Destroy();
            final MediaPlayer player = new MediaPlayer();
            mp = player;
            mpPrepared = false;
            mpStartWhenPrepared = play;
            player.setAudioAttributes(new AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA)
                                                               .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                                                               .build());
            player.setDataSource(ttsPlayMp3Path);
            player.setOnCompletionListener(new OnCompletionListener() {

                @Override public void onCompletion(MediaPlayer mp) {
                    mp3Started = false;
                    mp.pause();
                    abandonMp3Focus();
                }
            });
            if (stream) {
                // the screen may go off while it plays: the player keeps the device awake for it
                player.setWakeMode(LibreraApp.context, PowerManager.PARTIAL_WAKE_LOCK);
                player.setOnPreparedListener(prepared -> {
                    if (prepared != mp) {
                        return; // another stream was chosen meanwhile
                    }
                    mpPrepared = true;
                    LOG.d("stream prepared", ttsPlayMp3Path, mpStartWhenPrepared);
                    if (mpStartWhenPrepared) {
                        mpStartWhenPrepared = false;
                        startMp3();
                    }
                    EventBus.getDefault().post(new TtsStatus());
                    TTSNotification.showLast();
                });
                player.setOnErrorListener((failed, what, extra) -> {
                    LOG.d("stream error", what, extra, ttsPlayMp3Path);
                    if (failed == mp) {
                        mpStartWhenPrepared = false;
                        mp3Started = false;
                        abandonMp3Focus();
                        EventBus.getDefault().post(new TtsStatus());
                    }
                    return true;
                });
                player.prepareAsync();
            } else {
                player.prepare();
                mpPrepared = true;
                mpStartWhenPrepared = false;
                if (play) {
                    startMp3();
                }
            }

            mTimer = new Timer();

            mTimer.schedule(new TimerTask() {

                @Override public void run() {
                    final MediaPlayer current = mp;
                    if (current != null && mpPrepared && !isStream()) {
                        AppState.get().mp3seek = current.getCurrentPosition();
                    }
                    //LOG.d("Run timer-task");
                    EventBus.getDefault()
                            .post(new TtsStatus());
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

    /** Whether the player can be asked its position and length: loaded, and not a stream. */
    public boolean isMp3Seekable() {
        return mp != null && mpPrepared && !isStream();
    }

    public void mp3Destroy() {
        abandonMp3Focus();
        final MediaPlayer player = mp;
        mpStartWhenPrepared = false;
        mp3Started = false;
        if (player != null) {
            mp = null;
            try {
                if (mpPrepared) {
                    player.stop();
                }
            } catch (IllegalStateException e) {
                LOG.e(e);
            }
            mpPrepared = false;
            player.reset();
            player.release();
            if (mTimer != null) {
                mTimer.purge();
                mTimer.cancel();
                mTimer = null;
            }
        }
        LOG.d("mp3Desproy");
    }

    public void mp3Next() {
        mp3Step(1);
    }

    public void mp3Prev() {
        mp3Step(-1);
    }

    /** Five seconds on or back in a file; the next or the previous station of a stream. */
    private void mp3Step(int step) {
        if (isStream()) {
            final StreamPlaylist.Entry entry = StreamPlaylist.neighbour(BookCSS.get().mp3BookPathGet(), step);
            if (entry != null) {
                playStream(entry);
            }
            return;
        }
        final MediaPlayer player = mp;
        if (player != null && mpPrepared) {
            player.seekTo(player.getCurrentPosition() + step * 5 * 1000);
        }
    }

    /** Plays a station of the internet radio in the audio player. */
    public void playStream(StreamPlaylist.Entry entry) {
        StreamPlaylist.playing(entry);
        BookCSS.get().mp3BookPath(entry.url);
        AppState.get().mp3seek = 0;
        loadMP3(entry.url, true);
        EventBus.getDefault().post(new TtsStatus());
        TTSNotification.showLast();
    }

    /** Whether the audio player plays a stream from the network rather than a file. */
    public boolean isStream() {
        return StreamPlaylist.isStream(BookCSS.get().mp3BookPathGet());
    }

    /** Plays the audio file or the radio, never pauses it; false when TTS speaks instead. */
    public boolean isMp3Play() {
        if (!isMp3()) {
            return false;
        }
        if (mp == null) {
            loadMP3(BookCSS.get().mp3BookPathGet());
        }
        if (mp == null) {
            return false;
        }
        if (!isPlaying()) {
            startMp3();
        }
        TTSNotification.showLast();
        return true;
    }

    /** Pauses the audio file or the radio, never plays it; false when TTS speaks instead. */
    public boolean isMp3Pause() {
        if (!isMp3()) {
            return false;
        }
        pauseMp3();
        TTSNotification.showLast();
        return true;
    }

    public boolean isMp3PlayPause() {
        if (isMp3()) {
            if (mp == null) {
                loadMP3(BookCSS.get()
                               .mp3BookPathGet());
            }
            if (mp == null) {
                return false;
            }
            if (isPlaying()) {
                pauseMp3();
            } else {
                startMp3();
            }
            TTSNotification.showLast();
            return true;
        }
        return false;
    }

    public void playMp3() {
        if (mp != null) {
            startMp3();
        }
    }

    public void pauseMp3() {
        mpStartWhenPrepared = false;
        mp3Started = false;
        if (mp != null && mpPrepared) {
            mp.pause();
        }
        abandonMp3Focus();
    }

    // Unless it is to mix with other apps, the audio file player takes the sound the way a music
    // player does: other apps pause while it plays, and it pauses for a call or for music another
    // app starts - and plays on after a call, or quietly under a notification.
    private Object mp3FocusRequest;
    private boolean mp3HasFocus;
    private boolean mp3PausedForFocus;

    private final AudioManager.OnAudioFocusChangeListener mp3FocusListener = focusChange -> {
        LOG.d("mp3 onAudioFocusChange", focusChange);
        if (mp == null) {
            return;
        }
        try {
            if (focusChange == AudioManager.AUDIOFOCUS_GAIN) {
                mp.setVolume(1f, 1f);
                if (mp3PausedForFocus) {
                    mp3PausedForFocus = false;
                    mp.start();
                }
            } else if (focusChange == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK) {
                mp.setVolume(0.2f, 0.2f);
            } else if (focusChange == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT) {
                if (mp3Started) {
                    mp3PausedForFocus = true;
                    mp.pause();
                }
            } else if (focusChange == AudioManager.AUDIOFOCUS_LOSS) {
                // Another player has it for good: stop, and leave it to the reader to start again
                mp3PausedForFocus = false;
                pauseMp3();
            }
            EventBus.getDefault().post(new TtsStatus());
            TTSNotification.showLast();
        } catch (Exception e) {
            LOG.e(e);
        }
    };

    private void startMp3() {
        if (mp == null) {
            return;
        }
        if (!mpPrepared) {
            mpStartWhenPrepared = true; // starts once the stream is ready
            return;
        }
        mp3PausedForFocus = false;
        if (!AppState.get().mixWithOtherApps && !requestMp3Focus()) {
            LOG.d("mp3 audio focus refused");
            return;
        }
        mp.setVolume(1f, 1f);
        mp.start();
        mp3Started = true;
    }

    private boolean requestMp3Focus() {
        if (mp3HasFocus) {
            return true;
        }
        try {
            final AudioManager am = (AudioManager) LibreraApp.context.getSystemService(Context.AUDIO_SERVICE);
            final int result;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                if (mp3FocusRequest == null) {
                    mp3FocusRequest = new AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
                            .setAudioAttributes(new AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA)
                                                                             .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                                                                             .build())
                            .setOnAudioFocusChangeListener(mp3FocusListener)
                            .build();
                }
                result = am.requestAudioFocus((AudioFocusRequest) mp3FocusRequest);
            } else {
                result = am.requestAudioFocus(mp3FocusListener, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN);
            }
            mp3HasFocus = result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED;
        } catch (Exception e) {
            LOG.e(e);
            // Better to play along with the others than not to play at all
            return true;
        }
        return mp3HasFocus;
    }

    private void abandonMp3Focus() {
        mp3PausedForFocus = false;
        if (!mp3HasFocus) {
            return;
        }
        mp3HasFocus = false;
        try {
            final AudioManager am = (AudioManager) LibreraApp.context.getSystemService(Context.AUDIO_SERVICE);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                am.abandonAudioFocusRequest((AudioFocusRequest) mp3FocusRequest);
            } else {
                am.abandonAudioFocus(mp3FocusListener);
            }
        } catch (Exception e) {
            LOG.e(e);
        }
    }

    /** The mix setting changed: give the sound back now, or take it for a file playing. */
    public void mixChanged() {
        if (AppState.get().mixWithOtherApps) {
            // the voice too: it takes the sound again only when reading starts with this off
            try {
                TTSService.abandonAudioFocusCompat();
            } catch (Exception e) {
                LOG.e(e);
            }
            abandonMp3Focus();
            if (mp != null) {
                mp.setVolume(1f, 1f);
            }
        } else if (mp != null && mpPrepared && mp3Started) {
            requestMp3Focus();
        }
    }

    public boolean isMp3() {
        return TxtUtils.isNotEmpty(BookCSS.get()
                                          .mp3BookPathGet());
    }

    public void seekTo(int i) {
        if (mp != null && mpPrepared && !isStream()) {
            mp.seekTo(i);
        }

    }

}
