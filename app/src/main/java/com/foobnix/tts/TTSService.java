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
import android.content.res.AssetFileDescriptor;
import android.media.AudioAttributes;
import android.media.AudioFocusRequest;
import android.media.AudioManager;
import android.media.AudioManager.OnAudioFocusChangeListener;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.IBinder;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.speech.tts.TextToSpeech.OnUtteranceCompletedListener;
import android.speech.tts.UtteranceProgressListener;
import android.support.v4.media.session.MediaSessionCompat;
import android.view.KeyEvent;

import androidx.core.app.NotificationCompat;
import androidx.media.session.MediaButtonReceiver;

import com.foobnix.android.utils.Apps;
import com.foobnix.android.utils.LOG;
import com.foobnix.android.utils.TxtUtils;
import com.foobnix.android.utils.Vibro;
import com.foobnix.model.AppBook;
import com.foobnix.model.AppProfile;
import com.foobnix.model.AppSP;
import com.foobnix.model.AppState;
import com.foobnix.pdf.info.R;
import com.foobnix.pdf.info.model.BookCSS;
import com.foobnix.pdf.info.wrapper.DocumentController;
import com.foobnix.sys.ImageExtractor;
import com.foobnix.sys.TempHolder;

import org.ebookdroid.LibreraApp;
import org.ebookdroid.common.settings.books.SharedBooks;
import org.ebookdroid.core.codec.CodecDocument;
import org.ebookdroid.core.codec.CodecPage;
import org.greenrobot.eventbus.EventBus;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

@TargetApi(Build.VERSION_CODES.O)
public class TTSService extends Service {

    public static final String EXTRA_PATH = "EXTRA_PATH";
    public static final String EXTRA_ANCHOR = "EXTRA_ANCHOR";
    public static final String EXTRA_INT = "INT";

    private static final String TAG = "TTSService";

    public static String ACTION_PLAY_CURRENT_PAGE = "ACTION_PLAY_CURRENT_PAGE";
    private final BroadcastReceiver blueToothReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            LOG.d("blueToothReceiver", intent);
            TTSEngine.get().stop();
            TTSNotification.showLast();
        }
    };
    int width;
    int height;
    AudioManager mAudioManager;
    MediaSessionCompat mMediaSessionCompat;
    boolean isActivated;
    boolean isPlaying;
    Object audioFocusRequest;
    volatile boolean isStartForeground = false;
    CodecDocument cache;
    String path;
    int wh;
    int emptyPageCount = 0;
    final OnAudioFocusChangeListener listener = new OnAudioFocusChangeListener() {

        @Override
        public void onAudioFocusChange(int focusChange) {
            LOG.d("onAudioFocusChange", focusChange);
            if (AppState.get().isEnableAccessibility) {
                return;
            }

            if (!AppState.get().stopReadingOnCall) {
                return;
            }
            if (focusChange == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK) {
                LOG.d("Ingore Duck");
                return;
            }

            if (focusChange < 0) {
                isPlaying = TTSEngine.get().isPlaying();
                LOG.d("onAudioFocusChange", "Is playing", isPlaying);
                TTSEngine.get().stop();
                TTSNotification.showLast();
            } else {
                if (isPlaying) {
                    playPage("", AppSP.get().lastBookPage, null);
                }
            }
            EventBus.getDefault().post(new TtsStatus());
        }
    };
    private WakeLock wakeLock;

    {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            audioFocusRequest = new AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
                    .setAudioAttributes(
                            new AudioAttributes.Builder()
                                    .setUsage(AudioAttributes.USAGE_MEDIA)
                                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                                    .setLegacyStreamType(AudioManager.STREAM_MUSIC)
                                    .build())
                    .setAcceptsDelayedFocusGain(true)
                    .setWillPauseWhenDucked(false)
                    .setOnAudioFocusChangeListener(listener)
                    .build();
        }
    }

    public TTSService() {
        LOG.d(TAG, "Create constructor");
    }

    public static void playLastBook() {
        playBookPage(AppSP.get().lastBookPage, AppSP.get().lastBookPath, "", AppSP.get().lastBookWidth, AppSP.get().lastBookHeight, AppSP.get().lastFontSize, AppSP.get().lastBookTitle);
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
                TTSService.playBookPage(controller.getCurentPageFirst1() - 1, controller.getCurrentBook().getPath(), "", controller.getBookWidth(), controller.getBookHeight(), BookCSS.get().fontSizeSp, controller.getTitle());
            }
        }
    }

    @TargetApi(26)
    public static void playBookPage(int page, String path, String anchor, int width, int height, int fontSize, String title) {
        LOG.d(TAG, "playBookPage", page, path, width, height);
        TTSEngine.get().stop();

        AppSP.get().lastBookWidth = width;
        AppSP.get().lastBookHeight = height;
        AppSP.get().lastFontSize = fontSize;
        AppSP.get().lastBookTitle = title;
        AppSP.get().lastBookPage = page;


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

    @Override
    public void onCreate() {
        LOG.d(TAG, "Create");
        startMyForeground();
        //

        PowerManager myPowerManager = (PowerManager) getSystemService(POWER_SERVICE);
        wakeLock = myPowerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "Librera:TTSServiceLock");

        AppProfile.init(getApplicationContext());

        mAudioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);


        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            mAudioManager.requestAudioFocus((AudioFocusRequest) audioFocusRequest);
        } else {
            mAudioManager.requestAudioFocus(listener, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN);
        }

        //mAudioManager.setRingerMode(AudioManager.RINGER_MODE_VIBRATE);


        mMediaSessionCompat = new MediaSessionCompat(getApplicationContext(), "Tag");
        mMediaSessionCompat.setFlags(MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS | MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS);
        mMediaSessionCompat.setCallback(new MediaSessionCompat.Callback() {
            @Override
            public boolean onMediaButtonEvent(Intent intent) {
                KeyEvent event = (KeyEvent) intent.getExtras().get(Intent.EXTRA_KEY_EVENT);

                boolean isPlaying = TTSEngine.get().isPlaying();

                LOG.d(TAG, "onMediaButtonEvent", "isActivated", isActivated, "isPlaying", isPlaying, "event", event);


                final List<Integer> list = Arrays.asList(KeyEvent.KEYCODE_HEADSETHOOK, KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE, KeyEvent.KEYCODE_MEDIA_STOP, KeyEvent.KEYCODE_MEDIA_PLAY, KeyEvent.KEYCODE_MEDIA_PAUSE);

                if (KeyEvent.ACTION_DOWN == event.getAction()) {
                    if (list.contains(event.getKeyCode())) {
                        LOG.d(TAG, "onMediaButtonEvent", "isPlaying", isPlaying, "isFastBookmarkByTTS", AppState.get().isFastBookmarkByTTS);

                        if (AppState.get().isFastBookmarkByTTS) {
                            if (isPlaying) {
                                TTSEngine.get().fastTTSBookmakr(getBaseContext(), AppSP.get().lastBookPath, AppSP.get().lastBookPage + 1, AppSP.get().lastBookPageCount);
                            } else {
                                playPage("", AppSP.get().lastBookPage, null);
                            }
                        } else {
                            if (isPlaying) {
                                TTSEngine.get().stop();
                            } else {
                                playPage("", AppSP.get().lastBookPage, null);
                            }
                        }
                    } else if (KeyEvent.KEYCODE_MEDIA_NEXT == event.getKeyCode()) {
                        TTSEngine.get().stop();
                        playPage("", AppSP.get().lastBookPage + 1, null);

                    } else if (KeyEvent.KEYCODE_MEDIA_PREVIOUS == event.getKeyCode()) {
                        TTSEngine.get().stop();
                        playPage("", AppSP.get().lastBookPage - 1, null);


                    }
                }


                EventBus.getDefault().post(new TtsStatus());
                TTSNotification.showLast();
                //  }
                return true;
            }

        });

        Intent mediaButtonIntent = new Intent(Intent.ACTION_MEDIA_BUTTON);
        mediaButtonIntent.setClass(this, MediaButtonReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(this, 0, mediaButtonIntent, 0);
        mMediaSessionCompat.setMediaButtonReceiver(pendingIntent);

        //setSessionToken(mMediaSessionCompat.getSessionToken());


        // mMediaSessionCompat.setPlaybackState(new
        // PlaybackStateCompat.Builder().setActions(PlaybackStateCompat.ACTION_PLAY_PAUSE).setState(PlaybackStateCompat.STATE_CONNECTING,
        // 0, 0f).build());

        TTSEngine.get().getTTS();

        if (Build.VERSION.SDK_INT >= 24) {
            MediaPlayer mp = new MediaPlayer();
            try {
                final AssetFileDescriptor afd = getAssets().openFd("silence.mp3");
                mp.setDataSource(afd);
                mp.prepareAsync();
                mp.start();
                mp.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                    @Override
                    public void onCompletion(MediaPlayer mp) {
                        try {
                            afd.close();
                        } catch (IOException e) {
                            LOG.e(e);
                        }
                    }
                });

                LOG.d("silence");
            } catch (IOException e) {
                LOG.d("silence error");
                LOG.e(e);

            }
        }


        IntentFilter filter = new IntentFilter();
        filter.addAction(BluetoothDevice.ACTION_ACL_CONNECTED);
        filter.addAction(BluetoothDevice.ACTION_ACL_DISCONNECT_REQUESTED);
        filter.addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED);
        registerReceiver(blueToothReceiver, filter);

    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    public void startMyForeground() {
        if (!isStartForeground) {
            if (TxtUtils.isNotEmpty(AppSP.get().lastBookPath)) {
                try {
                    Notification show = TTSNotification.show(AppSP.get().lastBookPath, AppSP.get().lastBookPage, AppSP.get().lastBookPageCount);
                    if (show != null) {
                        startForeground(TTSNotification.NOT_ID, show);
                    } else {
                        startServiceWithNotification();
                    }
                } catch (Exception e) {
                    LOG.e(e);
                    startServiceWithNotification();
                }
            } else {
                startServiceWithNotification();
            }
            isStartForeground = true;
        }
    }

    private void startServiceWithNotification() {
        PendingIntent stopDestroy = PendingIntent.getService(this, 0, new Intent(TTSNotification.TTS_STOP_DESTROY, null, this, TTSService.class), PendingIntent.FLAG_UPDATE_CURRENT);
        Notification notification = new NotificationCompat.Builder(this, TTSNotification.DEFAULT) //
                .setSmallIcon(R.drawable.glyphicons_185_volume_up1) //
                .setContentTitle(Apps.getApplicationName(this)) //
                .setContentText(getString(R.string.please_wait))
                .addAction(R.drawable.glyphicons_208_remove_2, getString(R.string.stop), stopDestroy)//
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)//
                .build();

        startForeground(TTSNotification.NOT_ID, notification);
    }

    @TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH_MR1)
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        startMyForeground();


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
            BookCSS.get().mp3BookPath(null);
            AppState.get().mp3seek = 0;
            TTSEngine.get().stop();

            TTSEngine.get().stopDestroy();

            if (wakeLock.isHeld()) {
                wakeLock.release();
            }
            EventBus.getDefault().post(new TtsStatus());

            TTSNotification.hideNotification();
            stopForeground(true);
            stopSelf();

            return START_STICKY;

        }

        if (TTSNotification.TTS_PLAY_PAUSE.equals(intent.getAction())) {

            if (TTSEngine.get().isMp3PlayPause()) {
                return START_STICKY;
            }

            if (TTSEngine.get().isPlaying()) {
                TTSEngine.get().stop();
            } else {
                playPage("", AppSP.get().lastBookPage, null);
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
            playPage("", AppSP.get().lastBookPage, null);
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

            AppSP.get().lastBookParagraph = 0;
            TTSEngine.get().stop();
            playPage("", AppSP.get().lastBookPage + 1, null);
            if (!wakeLock.isHeld()) {
                wakeLock.acquire();
            }
        }
        if (TTSNotification.TTS_PREV.equals(intent.getAction())) {

            if (TTSEngine.get().isMp3()) {
                TTSEngine.get().mp3Prev();
                return START_STICKY;
            }

            AppSP.get().lastBookParagraph = 0;
            TTSEngine.get().stop();
            playPage("", AppSP.get().lastBookPage - 1, null);
            if (!wakeLock.isHeld()) {
                wakeLock.acquire();
            }
        }

        if (ACTION_PLAY_CURRENT_PAGE.equals(intent.getAction())) {
            if (TTSEngine.get().isMp3PlayPause()) {
                TTSNotification.show(AppSP.get().lastBookPath, -1, -1);
                return START_STICKY;
            }


            int pageNumber = intent.getIntExtra(EXTRA_INT, -1);
            AppSP.get().lastBookPath = intent.getStringExtra(EXTRA_PATH);
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

    public CodecDocument getDC() {
        try {
            if (AppSP.get().lastBookPath != null && AppSP.get().lastBookPath.equals(path) && cache != null && wh == AppSP.get().lastBookWidth + AppSP.get().lastBookHeight) {
                LOG.d(TAG, "CodecDocument from cache", AppSP.get().lastBookPath);
                return cache;
            }
            if (cache != null) {
                cache.recycle();
                cache = null;
            }
            path = AppSP.get().lastBookPath;
            cache = ImageExtractor.singleCodecContext(AppSP.get().lastBookPath, "", AppSP.get().lastBookWidth, AppSP.get().lastBookHeight);
            if (cache == null) {
                TTSNotification.hideNotification();
                return null;
            }
            cache.getPageCount(AppSP.get().lastBookWidth, AppSP.get().lastBookHeight, BookCSS.get().fontSizeSp);
            wh = AppSP.get().lastBookWidth + AppSP.get().lastBookHeight;
            LOG.d(TAG, "CodecDocument new", AppSP.get().lastBookPath, AppSP.get().lastBookWidth, AppSP.get().lastBookHeight);
            return cache;
        } catch (Exception e) {
            LOG.e(e);
            return null;
        }
    }

    @TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH_MR1)
    private void playPage(String preText, int pageNumber, String anchor) {
        mMediaSessionCompat.setActive(true);
        LOG.d("playPage", preText, pageNumber, anchor);
        if (pageNumber != -1) {
            isActivated = true;
            EventBus.getDefault().post(new MessagePageNumber(pageNumber));
            AppSP.get().lastBookPage = pageNumber;
            CodecDocument dc = getDC();
            if (dc == null) {
                LOG.d(TAG, "CodecDocument", "is NULL");
                TTSNotification.hideNotification();
                return;
            }

            AppSP.get().lastBookPageCount = dc.getPageCount();
            LOG.d(TAG, "CodecDocument PageCount", pageNumber, AppSP.get().lastBookPageCount);
            if (pageNumber >= AppSP.get().lastBookPageCount) {

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
                    playPage("", AppSP.get().lastBookPage + 1, null);
                }
                return;
            }
            emptyPageCount = 0;

            String[] parts = TxtUtils.getParts(pageHTML);
            String firstPart = pageNumber + 1 >= AppSP.get().lastBookPageCount || AppState.get().ttsTunnOnLastWord ? pageHTML : parts[0];
            final String secondPart = pageNumber + 1 >= AppSP.get().lastBookPageCount || AppState.get().ttsTunnOnLastWord ? "" : parts[1];

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
                        if (utteranceId.startsWith(TTSEngine.STOP_SIGNAL)) {
                            TTSEngine.get().stop();
                            return;
                        }
                        if (utteranceId.startsWith(TTSEngine.FINISHED_SIGNAL)) {
                            if (TxtUtils.isNotEmpty(preText1)) {
                                AppSP.get().lastBookParagraph = Integer.parseInt(utteranceId.replace(TTSEngine.FINISHED_SIGNAL, ""));
                            } else {
                                AppSP.get().lastBookParagraph = Integer.parseInt(utteranceId.replace(TTSEngine.FINISHED_SIGNAL, "")) + 1;
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

                        AppSP.get().lastBookParagraph = 0;
                        playPage(secondPart, AppSP.get().lastBookPage + 1, null);


                    }
                });
            } else {
                TTSEngine.get().getTTS().setOnUtteranceCompletedListener(new OnUtteranceCompletedListener() {

                    @Override
                    public void onUtteranceCompleted(String utteranceId) {
                        if (utteranceId.startsWith(TTSEngine.STOP_SIGNAL)) {
                            TTSEngine.get().stop();
                            return;
                        }
                        if (utteranceId.startsWith(TTSEngine.FINISHED_SIGNAL)) {
                            if (TxtUtils.isNotEmpty(preText1)) {
                                AppSP.get().lastBookParagraph = Integer.parseInt(utteranceId.replace(TTSEngine.FINISHED_SIGNAL, ""));
                            } else {
                                AppSP.get().lastBookParagraph = Integer.parseInt(utteranceId.replace(TTSEngine.FINISHED_SIGNAL, "")) + 1;
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

                        AppSP.get().lastBookParagraph = 0;
                        playPage(secondPart, AppSP.get().lastBookPage + 1, null);


                    }

                });
            }


            TTSEngine.get().speek(firstPart);

            TTSNotification.show(AppSP.get().lastBookPath, pageNumber + 1, dc.getPageCount());
            LOG.d("TtsStatus send");
            EventBus.getDefault().post(new TtsStatus());

            TTSNotification.showLast();

            new Thread(() -> {
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                }
                AppBook load = SharedBooks.load(AppSP.get().lastBookPath);
                load.currentPageChanged(pageNumber + 1, AppSP.get().lastBookPageCount);

                SharedBooks.save(load, false);
                AppProfile.save(this);
            }).start();

        }
    }


    @Override
    public void onDestroy() {
        super.onDestroy();

        isStartForeground = false;
        unregisterReceiver(blueToothReceiver);
        if (wakeLock.isHeld()) {
            wakeLock.release();
        }

        TTSEngine.get().stop();
        TTSEngine.get().shutdown();

        TTSNotification.hideNotification();


        isActivated = false;
        TempHolder.get().timerFinishTime = 0;


        //mAudioManager.abandonAudioFocus(listener);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            mAudioManager.abandonAudioFocusRequest((AudioFocusRequest) audioFocusRequest);
        } else {
            mAudioManager.abandonAudioFocus(listener);
        }

        //mMediaSessionCompat.setCallback(null);
        mMediaSessionCompat.setActive(false);
        mMediaSessionCompat.release();

        if (cache != null) {
            cache.recycle();
        }
        path = null;
        LOG.d(TAG, "onDestroy");
    }

}
