package com.foobnix.tts;

import android.annotation.TargetApi;
import android.app.Notification;
import android.app.PendingIntent;
import android.app.PendingIntent.CanceledException;
import android.app.Service;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioManager;
import android.media.AudioManager.OnAudioFocusChangeListener;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.IBinder;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.speech.tts.TextToSpeech.OnUtteranceCompletedListener;
import android.speech.tts.UtteranceProgressListener;
import android.support.v4.app.NotificationCompat;
import android.support.v4.media.session.MediaButtonReceiver;
import android.support.v4.media.session.MediaSessionCompat;
import android.view.KeyEvent;

import com.foobnix.android.utils.LOG;
import com.foobnix.android.utils.TxtUtils;
import com.foobnix.android.utils.Vibro;
import com.foobnix.model.AppBook;
import com.foobnix.model.AppState;
import com.foobnix.pdf.info.R;
import com.foobnix.pdf.info.wrapper.DocumentController;
import com.foobnix.sys.ImageExtractor;
import com.foobnix.sys.TempHolder;

import org.ebookdroid.LibreraApp;
import org.ebookdroid.common.settings.books.SharedBooks;
import org.ebookdroid.core.codec.CodecDocument;
import org.ebookdroid.core.codec.CodecPage;
import org.greenrobot.eventbus.EventBus;

import java.io.IOException;

public class TTSService extends Service {

    public static final String EXTRA_PATH = "EXTRA_PATH";
    public static final String EXTRA_ANCHOR = "EXTRA_ANCHOR";
    public static final String EXTRA_INT = "INT";

    private static final String TAG = "TTSService";

    public static String ACTION_PLAY_CURRENT_PAGE = "ACTION_PLAY_CURRENT_PAGE";
    private WakeLock wakeLock;

    public TTSService() {
        LOG.d(TAG, "Create constructor");
    }

    int width;
    int height;

    AudioManager mAudioManager;
    MediaSessionCompat mMediaSessionCompat;
    boolean isActivated;

    @TargetApi(24)
    @Override
    public void onCreate() {
        LOG.d(TAG, "Create");

        PowerManager myPowerManager = (PowerManager) getSystemService(POWER_SERVICE);
        wakeLock = myPowerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "Librera:TTSServiceLock");

        AppState.get().load(getApplicationContext());
        mAudioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);

        if (Build.VERSION.SDK_INT >= 26) {
            // mAudioManager.requestAudioFocus(new
            // AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN).setOnAudioFocusChangeListener(listener).build());
        } else {
        }
        mAudioManager.requestAudioFocus(listener, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN);

        mMediaSessionCompat = new MediaSessionCompat(getApplicationContext(), "Tag");
        mMediaSessionCompat.setFlags(MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS | MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS);
        mMediaSessionCompat.setCallback(new MediaSessionCompat.Callback() {
            @Override
            public boolean onMediaButtonEvent(Intent intent) {
                LOG.d(TAG, "onMediaButtonEvent", isActivated, intent);
                if (isActivated) {
                    KeyEvent event = (KeyEvent) intent.getExtras().get(Intent.EXTRA_KEY_EVENT);
                    LOG.d(TAG, "onMediaButtonEvent", "event", event);

                    if (KeyEvent.ACTION_UP != event.getAction()) {
                        return isActivated;
                    }

                    boolean isPlaying = TTSEngine.get().isPlaying();

                    if (KeyEvent.KEYCODE_HEADSETHOOK == event.getKeyCode() || KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE == event.getKeyCode()) {

                        if (isPlaying) {
                            if (AppState.get().isFastBookmarkByTTS) {
                                TTSEngine.get().fastTTSBookmakr(getBaseContext(), 0, 0);
                            } else {
                                TTSEngine.get().stop();
                            }
                        } else {
                            playPage("", AppState.get().lastBookPage, null);
                        }
                    } else if (KeyEvent.KEYCODE_MEDIA_STOP == event.getKeyCode() || KeyEvent.KEYCODE_MEDIA_PAUSE == event.getKeyCode()) {
                        TTSEngine.get().stop();
                    } else if (KeyEvent.KEYCODE_MEDIA_PLAY == event.getKeyCode()) {
                        if (!isPlaying) {
                            playPage("", AppState.get().lastBookPage, null);
                        }

                    }
                    TTSNotification.showLast();
                }
                return isActivated;
            }

        });

        Intent mediaButtonIntent = new Intent(Intent.ACTION_MEDIA_BUTTON);
        mediaButtonIntent.setClass(this, MediaButtonReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(this, 0, mediaButtonIntent, 0);
        mMediaSessionCompat.setMediaButtonReceiver(pendingIntent);

        // mMediaSessionCompat.setPlaybackState(new
        // PlaybackStateCompat.Builder().setActions(PlaybackStateCompat.ACTION_PLAY_PAUSE).setState(PlaybackStateCompat.STATE_CONNECTING,
        // 0, 0f).build());

        TTSEngine.get().getTTS();

        if (Build.VERSION.SDK_INT >= 24) {
            MediaPlayer mp = new MediaPlayer();
            try {
                mp.setDataSource(getAssets().openFd("silence.mp3"));
                mp.prepareAsync();
                mp.start();

                LOG.d("silence");
            } catch (IOException e) {
                LOG.d("silence error");
                LOG.e(e);

            }
        }

        Notification notification = new NotificationCompat.Builder(this, TTSNotification.DEFAULT) //
                .setSmallIcon(R.drawable.glyphicons_185_volume_up1) //
                .setContentTitle("Librera") //
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)//
                .setContentText("TTS").build();

        startForeground(TTSNotification.NOT_ID, notification);

        IntentFilter filter = new IntentFilter();
        filter.addAction(BluetoothDevice.ACTION_ACL_CONNECTED);
        filter.addAction(BluetoothDevice.ACTION_ACL_DISCONNECT_REQUESTED);
        filter.addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED);
        registerReceiver(blueToothReceiver, filter);

    }

    private final BroadcastReceiver blueToothReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            TTSEngine.get().stop();
            TTSNotification.showLast();
        }
    };

    boolean isPlaying;
    OnAudioFocusChangeListener listener = new OnAudioFocusChangeListener() {

        @Override
        public void onAudioFocusChange(int focusChange) {
            LOG.d("onAudioFocusChange", focusChange);
            if (!AppState.get().stopReadingOnCall) {
                return;
            }

            if (focusChange < 0) {
                isPlaying = TTSEngine.get().isPlaying();
                LOG.d("onAudioFocusChange", "Is playing", isPlaying);
                TTSEngine.get().stop();
                TTSNotification.showLast();
            } else {
                if (isPlaying) {
                    playPage("", AppState.get().lastBookPage, null);
                }
            }
        }
    };

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    public static void playLastBook() {
        playBookPage(AppState.get().lastBookPage, AppState.get().lastBookPath, "", AppState.get().lastBookWidth, AppState.get().lastBookHeight, AppState.get().lastFontSize, AppState.get().lastBookTitle);
    }

    public static void playPause(Context context, DocumentController controller) {
        if (TTSEngine.get().isPlaying()) {
            PendingIntent next = PendingIntent.getService(context, 0, new Intent(TTSNotification.TTS_PAUSE, null, context, TTSService.class), PendingIntent.FLAG_UPDATE_CURRENT);
            try {
                next.send();
            } catch (CanceledException e) {
                LOG.d(e);
            }
        } else {
            if (controller != null) {
                TTSService.playBookPage(controller.getCurentPageFirst1() - 1, controller.getCurrentBook().getPath(), "", controller.getBookWidth(), controller.getBookHeight(), AppState.get().fontSizeSp, controller.getTitle());
            }
        }
    }

    @TargetApi(26)
    public static void playBookPage(int page, String path, String anchor, int width, int height, int fontSize, String title) {
        LOG.d(TAG, "playBookPage", page, path, width, height);
        TTSEngine.get().stop();

        AppState.get().lastBookWidth = width;
        AppState.get().lastBookHeight = height;
        AppState.get().lastFontSize = fontSize;
        AppState.get().lastBookTitle = title;
        AppState.get().lastBookPage = page;

        Intent intent = playBookIntent(page, path, anchor);

        if (Build.VERSION.SDK_INT >= 26) {
            LibreraApp.context.startForegroundService(intent);
        } else {
            LibreraApp.context.startService(intent);
        }

    }

    private static Intent playBookIntent(int page, String path, String anchor) {
        Intent intent = new Intent(LibreraApp.context, TTSService.class);
        intent.setAction(TTSService.ACTION_PLAY_CURRENT_PAGE);
        intent.putExtra(EXTRA_INT, page);
        intent.putExtra(EXTRA_PATH, path);
        intent.putExtra(EXTRA_ANCHOR, anchor);
        return intent;
    }

    @TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH_MR1)
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        MediaButtonReceiver.handleIntent(mMediaSessionCompat, intent);
        LOG.d(TAG, "onStartCommand", intent);
        if (intent == null) {
            return START_STICKY;
        }
        LOG.d(TAG, "onStartCommand", intent.getAction());
        if (intent.getExtras() != null) {
            LOG.d(TAG, "onStartCommand", intent.getAction(), intent.getExtras());
            for (String key : intent.getExtras().keySet())
                LOG.d(TAG, key, "=>", intent.getExtras().get(key));
        }

        if (TTSNotification.TTS_STOP_DESTROY.equals(intent.getAction())) {
            TTSEngine.get().mp3Destroy();
            AppState.get().mp3BookPath = null;
            AppState.get().mp3seek = 0;
            TTSEngine.get().stop();

            TTSEngine.get().stopDestroy();

            if (wakeLock.isHeld()) {
                wakeLock.release();
            }
            EventBus.getDefault().post(new TtsStatus());

            stopSelf();

            return START_NOT_STICKY;

        }

        if (TTSNotification.TTS_PLAY_PAUSE.equals(intent.getAction())) {

            if (TTSEngine.get().isMp3PlayPause()) {
                return START_STICKY;
            }

            if (TTSEngine.get().isPlaying()) {
                TTSEngine.get().stop();
            } else {
                playPage("", AppState.get().lastBookPage, null);
            }
            if (wakeLock.isHeld()) {
                wakeLock.release();
            }
            TTSNotification.showLast();

        }
        if (TTSNotification.TTS_PAUSE.equals(intent.getAction())) {

            if (TTSEngine.get().isMp3PlayPause()) {
                return START_STICKY;
            }

            TTSEngine.get().stop();
            if (wakeLock.isHeld()) {
                wakeLock.release();
            }
            TTSNotification.showLast();
        }

        if (TTSNotification.TTS_PLAY.equals(intent.getAction())) {

            if (TTSEngine.get().isMp3PlayPause()) {

                return START_STICKY;
            }

            TTSEngine.get().stop();
            playPage("", AppState.get().lastBookPage, null);
            if (!wakeLock.isHeld()) {
                wakeLock.acquire();
            }
            TTSNotification.showLast();
        }
        if (TTSNotification.TTS_NEXT.equals(intent.getAction())) {

            if (TTSEngine.get().isMp3()) {
                TTSEngine.get().mp3Next();
                return START_STICKY;
            }

            TTSEngine.get().stop();
            playPage("", AppState.get().lastBookPage + 1, null);
            if (!wakeLock.isHeld()) {
                wakeLock.acquire();
            }
        }
        if (TTSNotification.TTS_PREV.equals(intent.getAction())) {

            if (TTSEngine.get().isMp3()) {
                TTSEngine.get().mp3Prev();
                return START_STICKY;
            }

            TTSEngine.get().stop();
            playPage("", AppState.get().lastBookPage - 1, null);
            if (!wakeLock.isHeld()) {
                wakeLock.acquire();
            }
        }

        if (ACTION_PLAY_CURRENT_PAGE.equals(intent.getAction())) {
            if (TTSEngine.get().isMp3PlayPause()) {
                TTSNotification.show(AppState.get().lastBookPath, -1, -1);
                return START_STICKY;
            }

            mMediaSessionCompat.setActive(true);
            int pageNumber = intent.getIntExtra(EXTRA_INT, -1);
            AppState.get().lastBookPath = intent.getStringExtra(EXTRA_PATH);
            String anchor = intent.getStringExtra(EXTRA_ANCHOR);

            if (pageNumber != -1) {
                playPage("", pageNumber, anchor);
            }
            if (!wakeLock.isHeld()) {
                wakeLock.acquire();
            }

        }

        EventBus.getDefault().post(new TtsStatus());

        return START_STICKY;
    }

    CodecDocument cache;
    String path;
    int wh;

    public CodecDocument getDC() {
        try {
            if (AppState.get().lastBookPath != null && AppState.get().lastBookPath.equals(path) && cache != null && wh == AppState.get().lastBookWidth + AppState.get().lastBookHeight) {
                LOG.d(TAG, "CodecDocument from cache", AppState.get().lastBookPath);
                return cache;
            }
            if (cache != null) {
                cache.recycle();
                cache = null;
            }
            path = AppState.get().lastBookPath;
            cache = ImageExtractor.singleCodecContext(AppState.get().lastBookPath, "", AppState.get().lastBookWidth, AppState.get().lastBookHeight);
            cache.getPageCount(AppState.get().lastBookWidth, AppState.get().lastBookHeight, AppState.get().fontSizeSp);
            wh = AppState.get().lastBookWidth + AppState.get().lastBookHeight;
            LOG.d(TAG, "CodecDocument new", AppState.get().lastBookPath, AppState.get().lastBookWidth, AppState.get().lastBookHeight);
            return cache;
        } catch (Exception e) {
            LOG.e(e);
            return null;
        }
    }

    int emptyPageCount = 0;

    @TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH_MR1)
    private void playPage(String preText, int pageNumber, String anchor) {
        if (pageNumber != -1) {
            isActivated = true;
            EventBus.getDefault().post(new MessagePageNumber(pageNumber));
            AppState.get().lastBookPage = pageNumber;
            CodecDocument dc = getDC();
            if (dc == null) {
                LOG.d(TAG, "CodecDocument", "is NULL");
                return;
            }

            int pageCount = dc.getPageCount();
            LOG.d(TAG, "CodecDocument PageCount", pageNumber, pageCount);
            if (pageNumber >= pageCount) {

                TempHolder.get().timerFinishTime = 0;

                Vibro.vibrate(1000);

                try {
                    Thread.sleep(2000);
                } catch (InterruptedException e) {
                }
                TTSEngine.get().getTTS().setOnUtteranceCompletedListener(null);
                TTSEngine.get().speek(LibreraApp.context.getString(R.string.the_book_is_over));

                EventBus.getDefault().post(new TtsStatus());

                stopSelf();
                return;
            }

            CodecPage page = dc.getPage(pageNumber);
            String pageHTML = page.getPageHTML();
            page.recycle();
            pageHTML = TxtUtils.replaceHTMLforTTS(pageHTML);

            if (TxtUtils.isNotEmpty(anchor)) {
                int indexOf = pageHTML.indexOf(anchor);
                if (indexOf > 0) {
                    pageHTML = pageHTML.substring(indexOf);
                    LOG.d("find anchor new text", pageHTML);
                }
            }

            LOG.d(TAG, pageHTML);

            if (TxtUtils.isEmpty(pageHTML)) {
                LOG.d("empty page play next one", emptyPageCount);
                emptyPageCount++;
                if (emptyPageCount < 3) {
                    playPage("", AppState.get().lastBookPage + 1, null);
                }
                return;
            }
            emptyPageCount = 0;

            String[] parts = TxtUtils.getParts(pageHTML);
            String firstPart = AppState.get().ttsTunnOnLastWord ? pageHTML : parts[0];
            final String secondPart = AppState.get().ttsTunnOnLastWord ? "" : parts[1];

            if (TxtUtils.isNotEmpty(preText)) {
                preText = TxtUtils.replaceLast(preText, "-", "");
                firstPart = preText + firstPart;
            }
            final String preText1 = preText;

            if (Build.VERSION.SDK_INT >= 15) {
                TTSEngine.get().getTTS().setOnUtteranceProgressListener(new UtteranceProgressListener() {

                    @Override
                    public void onStart(String utteranceId) {
                        LOG.d(TAG, "onUtteranceCompleted onStart", utteranceId);
                    }

                    @Override
                    public void onError(String utteranceId) {
                        LOG.d(TAG, "onUtteranceCompleted onError", utteranceId);
                        if (!utteranceId.equals(TTSEngine.UTTERANCE_ID_DONE)) {
                            return;
                        }
                        TTSEngine.get().stop();
                        EventBus.getDefault().post(new TtsStatus());

                    }

                    @Override
                    public void onDone(String utteranceId) {
                        LOG.d(TAG, "onUtteranceCompleted", utteranceId);
                        if (utteranceId.startsWith(TTSEngine.FINISHED)) {
                            if (TxtUtils.isNotEmpty(preText1)) {
                                AppState.get().lastBookParagraph = Integer.parseInt(utteranceId.replace(TTSEngine.FINISHED, ""));
                            } else {
                                AppState.get().lastBookParagraph = Integer.parseInt(utteranceId.replace(TTSEngine.FINISHED, "")) + 1;
                            }
                            return;
                        }

                        if (!utteranceId.equals(TTSEngine.UTTERANCE_ID_DONE)) {
                            LOG.d(TAG, "onUtteranceCompleted skip", "");
                            return;
                        }

                        if (TempHolder.get().timerFinishTime != 0 && System.currentTimeMillis() > TempHolder.get().timerFinishTime) {
                            LOG.d(TAG, "Timer");
                            TempHolder.get().timerFinishTime = 0;
                            stopSelf();
                            return;
                        }

                        AppState.get().lastBookParagraph = 0;
                        playPage(secondPart, AppState.get().lastBookPage + 1, null);

                        AppBook load = SharedBooks.load(AppState.get().lastBookPath);
                        load.currentPageChanged( AppState.get().lastBookPage + 1, dc.getPageCount());
                        SharedBooks.save(load);

                    }
                });
            } else {
                TTSEngine.get().getTTS().setOnUtteranceCompletedListener(new OnUtteranceCompletedListener() {

                    @Override
                    public void onUtteranceCompleted(String utteranceId) {
                        if (utteranceId.startsWith(TTSEngine.FINISHED)) {
                            if (TxtUtils.isNotEmpty(preText1)) {
                                AppState.get().lastBookParagraph = Integer.parseInt(utteranceId.replace(TTSEngine.FINISHED, ""));
                            } else {
                                AppState.get().lastBookParagraph = Integer.parseInt(utteranceId.replace(TTSEngine.FINISHED, "")) + 1;
                            }
                            return;
                        }

                        if (!utteranceId.equals(TTSEngine.UTTERANCE_ID_DONE)) {
                            LOG.d(TAG, "onUtteranceCompleted skip", "");
                            return;
                        }

                        LOG.d(TAG, "onUtteranceCompleted", utteranceId);
                        if (TempHolder.get().timerFinishTime != 0 && System.currentTimeMillis() > TempHolder.get().timerFinishTime) {
                            LOG.d(TAG, "Timer");
                            TempHolder.get().timerFinishTime = 0;
                            stopSelf();
                            return;
                        }

                        AppState.get().lastBookParagraph = 0;
                        playPage(secondPart, AppState.get().lastBookPage + 1, null);

                        AppBook load = SharedBooks.load(AppState.get().lastBookPath);
                        load.currentPageChanged( AppState.get().lastBookPage + 1,dc.getPageCount());
                        SharedBooks.save(load);

                    }

                });
            }

            TTSNotification.show(AppState.get().lastBookPath, pageNumber + 1, dc.getPageCount());

            TTSEngine.get().speek(firstPart);

            EventBus.getDefault().post(new TtsStatus());

            TTSNotification.showLast();

        }
    }



    @Override
    public void onDestroy() {
        super.onDestroy();
        unregisterReceiver(blueToothReceiver);
        if (wakeLock.isHeld()) {
            wakeLock.release();
        }
        TTSNotification.hideNotification();
        TTSEngine.get().shutdown();

        isActivated = false;
        TempHolder.get().timerFinishTime = 0;
        mMediaSessionCompat.setActive(false);
        if (cache != null) {
            cache.recycle();
        }
        path = null;
        LOG.d(TAG, "onDestroy");
    }

}
