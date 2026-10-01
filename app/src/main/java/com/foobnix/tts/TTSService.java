package com.foobnix.tts;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.ActivityManager;
import android.app.Notification;
import android.app.PendingIntent;
import android.app.PendingIntent.CanceledException;
import android.app.Service;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.pm.ServiceInfo;
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
import android.provider.Settings;
import android.speech.tts.TextToSpeech;
import android.speech.tts.TextToSpeech.OnUtteranceCompletedListener;
import android.speech.tts.UtteranceProgressListener;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.v4.media.MediaBrowserCompat;
import android.support.v4.media.MediaDescriptionCompat;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.view.KeyEvent;

import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.ServiceCompat;
import androidx.core.content.ContextCompat;
import androidx.media.session.MediaButtonReceiver;

import com.foobnix.LibreraApp;
import com.foobnix.android.utils.Apps;
import com.foobnix.android.utils.LOG;
import com.foobnix.android.utils.TxtUtils;
import com.foobnix.pdf.info.ExtUtils;
import com.foobnix.ui2.AppDB;
import com.foobnix.pdf.info.IMG;
import com.foobnix.dao2.FileMeta;
import com.foobnix.android.utils.Vibro;
import com.foobnix.model.AppBook;
import com.foobnix.model.AppData;
import org.ebookdroid.common.settings.SettingsManager;
import com.foobnix.model.AppProfile;
import com.foobnix.model.AppSP;
import com.foobnix.model.AppState;
import com.foobnix.pdf.info.R;
import com.foobnix.pdf.info.model.BookCSS;
import com.foobnix.pdf.info.wrapper.DocumentController;
import com.foobnix.sys.ImageExtractor;
import com.foobnix.sys.TempHolder;

import org.ebookdroid.common.settings.books.SharedBooks;
import org.ebookdroid.core.codec.CodecDocument;
import org.ebookdroid.core.codec.CodecPage;
import org.greenrobot.eventbus.EventBus;

import java.io.IOException;
import java.util.Arrays;
import androidx.media.MediaBrowserServiceCompat;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

@TargetApi(Build.VERSION_CODES.O) public class TTSService extends MediaBrowserServiceCompat {
    public static final String EXTRA_PATH = "EXTRA_PATH";
    public static final String EXTRA_ANCHOR = "EXTRA_ANCHOR";
    public static final String EXTRA_INT = "INT";
    private static final String TAG = "TTSService";
    public static String ACTION_PLAY_CURRENT_PAGE = "ACTION_PLAY_CURRENT_PAGE";
    private final BroadcastReceiver blueToothReceiver = new BroadcastReceiver() {
        @Override public void onReceive(Context context, Intent intent) {
            LOG.d("blueToothReceiver", intent);
            stopMediaSesstionAndReleaweWakeLock();
            TTSNotification.showLast();
        }
    };
    int width;
    int height;
    AudioManager mAudioManager;
    MediaSessionCompat mMediaSessionCompat;

    private static volatile MediaSessionCompat sessionRef;
    private static volatile TTSService serviceRef;

    enum ReaderTtsAction { START_BOOK, PAUSE, RESUME }

    static ReaderTtsAction readerAction(String readerPath, String activePath,
                                        boolean playing, boolean shutdown) {
        if (readerPath == null || activePath == null || !readerPath.equals(activePath) || shutdown)
            return ReaderTtsAction.START_BOOK;
        return playing ? ReaderTtsAction.PAUSE : ReaderTtsAction.RESUME;
    }

    volatile String activeBookPath;
    volatile int activePage;
    volatile int activeParagraph;

    void restoreActivePosition() {
        if (activeBookPath == null) {
            activeBookPath = AppSP.get().lastBookPath;
            activePage = AppSP.get().lastBookPage;
            activeParagraph = AppSP.get().lastBookParagraph;
        }
        AppSP.get().lastBookPath = activeBookPath;
        AppSP.get().lastBookPage = activePage;
        AppSP.get().lastBookParagraph = activeParagraph;
    }

    private void setActiveParagraph(int paragraph) {
        activeParagraph = paragraph;
        AppSP.get().lastBookParagraph = paragraph;
    }

    public static void abandonAudioFocusCompat() {
        final TTSService service = serviceRef;
        if (service == null || service.mAudioManager == null) {
            return;
        }
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                if (service.audioFocusRequest != null) {
                    service.mAudioManager.abandonAudioFocusRequest(
                            (AudioFocusRequest) service.audioFocusRequest);
                }
            } else {
                service.mAudioManager.abandonAudioFocus(service.listener);
            }
            LOG.d(TAG, "audio focus abandoned");
        } catch (Exception e) {
            LOG.e(e);
        }
    }


    private static final double CHARS_PER_SECOND = 15.0;

    private static final long MIN_PAGE_MS = 1_500L;
    private static final long MAX_PAGE_MS = 30 * 60 * 1_000L;

    private static volatile long pageStartMs = 0;
    private static volatile long avgPageMs = 0;
    private static volatile int lastPageChars = 0;

    static void onPageSpeechStarted(String text) {
        pageStartMs = System.currentTimeMillis();
        lastPageChars = text != null ? text.length() : 0;
    }

    static void onPageSpeechFinished() {
        final long start = pageStartMs;
        if (start <= 0) {
            return;
        }
        final long elapsed = System.currentTimeMillis() - start;
        if (elapsed < MIN_PAGE_MS || elapsed > MAX_PAGE_MS) {
            return;
        }
        // Exponential moving average so the estimate settles quickly but still adapts when the
        // speech rate changes mid-book.
        avgPageMs = avgPageMs <= 0 ? elapsed : (avgPageMs * 3 + elapsed) / 4;
        LOG.d(TAG, "page spoken in", elapsed, "avg", avgPageMs);
    }

    private static long estimatedPageMs() {
        if (avgPageMs > 0) {
            return avgPageMs;
        }
        if (lastPageChars > 0) {
            float speed = AppState.get().ttsSpeed;
            if (speed <= 0) {
                speed = 1f;
            }
            return (long) (lastPageChars / (CHARS_PER_SECOND * speed) * 1000);
        }
        return 0;
    }

    static long bookDurationMs() {
        final long perPage = estimatedPageMs();
        final int pages = AppSP.get().lastBookPageCount;
        if (perPage <= 0 || pages <= 0) {
            return 0;
        }
        return perPage * pages;
    }
    boolean isActivated;
    boolean isPlaying;
    Object audioFocusRequest;
    volatile boolean isStartForeground = false;
    CodecDocument cache;
    String path;
    int wh;
    int emptyPageCount = 0;
    final OnAudioFocusChangeListener listener = new OnAudioFocusChangeListener() {
        @Override public void onAudioFocusChange(int focusChange) {
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
                isPlaying = TTSEngine.get()
                                     .isPlaybackRequested();
                LOG.d("onAudioFocusChange", "Is playing", isPlaying);
                stopMediaSesstionAndReleaweWakeLock();
                TTSNotification.showLast();
            } else {
                if (isPlaying) {
                    playPage("", activePage, null);
                }
            }
        }
    };
    private WakeLock wakeLock;
    private static final long WAKE_LOCK_TIMEOUT = 2 * 60 * 1000L; // 2 min timeout per page

    {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            audioFocusRequest = new AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN).setAudioAttributes(
                                                                                                   new AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA)
                                                                                                                                .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                                                                                                                                .setLegacyStreamType(AudioManager.STREAM_MUSIC)
                                                                                                                                .build())
                                                                                           .setAcceptsDelayedFocusGain(
                                                                                                   true)
                                                                                           .setWillPauseWhenDucked(
                                                                                                   false)
                                                                                           .setOnAudioFocusChangeListener(
                                                                                                   listener)
                                                                                           .build();
        }
    }

    public TTSService() {
        LOG.d(TAG, "Create constructor");
    }

    public static void playLastBook() {
        playBookPage(AppSP.get().lastBookPage, AppSP.get().lastBookPath, "", AppSP.get().lastBookWidth,
                AppSP.get().lastBookHeight, AppSP.get().lastFontSize, AppSP.get().lastBookTitle);
    }

    public static void updateTimer() {
        TempHolder.get().timerFinishTime = System.currentTimeMillis() + AppState.get().ttsTimer * 60 * 1000;
        LOG.d("Update-timer", TempHolder.get().timerFinishTime, AppState.get().ttsTimer);
    }

    public static void openSettingsIntent(Context a) {
        TTSEngine.get()
                 .stop();
        TTSEngine.get()
                 .stopDestroy();

        Intent intent = new Intent();
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.setAction("com.android.settings.TTS_SETTINGS");
        a.startActivity(intent);
    }

    private static void openNotificationSettings(Context context) {
        try {
            Intent intent = new Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS);
            intent.putExtra(Settings.EXTRA_APP_PACKAGE, context.getPackageName());
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
        } catch (Exception e) {
            LOG.e(e);
        }
    }

    public static boolean isTTSGranted(Context context) {
        if (Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(context,
                Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {

            final Activity activity = context instanceof Activity ? (Activity) context : null;

            if (activity == null) {
                openNotificationSettings(context);
            } else if (ActivityCompat.shouldShowRequestPermissionRationale(activity,
                    Manifest.permission.POST_NOTIFICATIONS)) {
                openNotificationSettings(context);
            } else {
                ActivityCompat.requestPermissions(activity,
                        new String[]{Manifest.permission.POST_NOTIFICATIONS}, 11);
            }
            return false;
        }
        if (TTSEngine.get()
                     .isInit() && TTSEngine.get()
                                           .getCurrentLang()
                                           .equals("---")) {
            openSettingsIntent(context);
            return false;
        }
        return true;
    }

    public static void playPause(Context context, DocumentController controller) {

        if (!isTTSGranted(context)) {
            return;
        }

        String bookPath = controller == null || controller.getCurrentBook() == null
                ? null : controller.getCurrentBook().getPath();
        TTSService live = serviceRef;
        ReaderTtsAction action = bookPath == null
                ? (TTSEngine.get().isPlaybackRequested() ? ReaderTtsAction.PAUSE : ReaderTtsAction.START_BOOK)
                : readerAction(bookPath, live == null ? null : live.activeBookPath,
                        TTSEngine.get().isPlaybackRequested(), TTSEngine.get().isShutdown());
        if (action == ReaderTtsAction.PAUSE) {
            PendingIntent next = PendingIntent.getService(context, 0,
                    new Intent(TTSNotification.TTS_PAUSE, null, context, TTSService.class),
                    PendingIntent.FLAG_IMMUTABLE);
            try {
                next.send();
            } catch (CanceledException e) {
                LOG.d(e);
            }
        } else if (action == ReaderTtsAction.RESUME && live != null) {
            live.restoreActivePosition();
            context.startService(new Intent(TTSNotification.TTS_PLAY, null, context, TTSService.class));
        } else if (controller != null && bookPath != null) {
            TTSService.playBookPage(controller.getCurentPageFirst1() - 1, bookPath, "",
                    controller.getBookWidth(), controller.getBookHeight(), BookCSS.get().fontSizeSp,
                    controller.getTitle());
        }
    }

    @TargetApi(26)
    public static void playBookPage(int page, String path, String anchor, int width, int height, int fontSize,
                                    String title) {
        LOG.d(TAG, "playBookPage1", page, path, width, height);

        TTSEngine.get()
                 .stop(null);

        AppSP.get().lastBookWidth = width;
        AppSP.get().lastBookHeight = height;
        AppSP.get().lastFontSize = fontSize;
        AppSP.get().lastBookTitle = title;
        AppSP.get().lastBookPage = page;

        Intent intent = playBookIntent(page, path, anchor);

        try {
            if (Build.VERSION.SDK_INT >= 26) {
                LibreraApp.context.startForegroundService(intent);
            } else {
                LibreraApp.context.startService(intent);
            }
        } catch (Exception e) {
            LOG.e(e);
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

    @Override public void onCreate() {
        super.onCreate();
        serviceRef = this;
        LOG.d(TAG, "onCreate:TTS playBookPage1");
        //startMyForeground();
        //

        //try without wakeLock
        PowerManager myPowerManager = (PowerManager) getSystemService(POWER_SERVICE);
        wakeLock = myPowerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "Librera:TTSServiceLock");
        wakeLock.setReferenceCounted(false);

        AppProfile.init(getApplicationContext());

        mAudioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);

        //mAudioManager.setRingerMode(AudioManager.RINGER_MODE_VIBRATE);

        Intent mediaButtonIntent = new Intent(Intent.ACTION_MEDIA_BUTTON);
        PendingIntent pendingIntent1 =
                PendingIntent.getBroadcast(getApplicationContext(), 0, mediaButtonIntent, PendingIntent.FLAG_IMMUTABLE);

        mMediaSessionCompat = new MediaSessionCompat(getApplicationContext(), "Tag", null, pendingIntent1);
        mMediaSessionCompat.setFlags(
                MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS | MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS);
        mMediaSessionCompat.setCallback(new MediaSessionCompat.Callback() {
            @Override public boolean onMediaButtonEvent(Intent intent) {
                KeyEvent event = (KeyEvent) intent.getExtras()
                                                  .get(Intent.EXTRA_KEY_EVENT);

                boolean isPlaying = TTSEngine.get()
                                             .isPlaybackRequested();

                LOG.d(TAG, "onMediaButtonEvent", "isActivated", isActivated, "isPlaying", isPlaying, "event", event);

                final List<Integer> list =
                        Arrays.asList(KeyEvent.KEYCODE_HEADSETHOOK, KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE,
                                KeyEvent.KEYCODE_MEDIA_STOP, KeyEvent.KEYCODE_MEDIA_PLAY, KeyEvent.KEYCODE_MEDIA_PAUSE);

                if (KeyEvent.ACTION_DOWN == event.getAction()) {
                    if (list.contains(event.getKeyCode())) {
                        LOG.d(TAG, "onMediaButtonEvent", "isPlaying", isPlaying, "isFastBookmarkByTTS",
                                AppState.get().isFastBookmarkByTTS);

                        if (AppState.get().isFastBookmarkByTTS) {
                            if (isPlaying) {
                                TTSEngine.get()
                                                 .fastTTSBookmakr(getBaseContext(), activeBookPath,
                                                 activePage + 1, AppSP.get().lastBookPageCount);
                            } else {
                                playPage("", activePage, null);
                            }
                        } else {
                            if (isPlaying) {
                                stopMediaSesstionAndReleaweWakeLock();
                            } else {
                                playPage("", activePage, null);
                            }
                        }
                    } else if (KeyEvent.KEYCODE_MEDIA_NEXT == event.getKeyCode()) {
                        playPage("", activePage + 1, null);
                    } else if (KeyEvent.KEYCODE_MEDIA_PREVIOUS == event.getKeyCode()) {
                        playPage("", activePage - 1, null);
                    }
                }

                EventBus.getDefault()
                        .post(new TtsStatus());
                TTSNotification.showLast();
                //  }
                return true;
            }

            @Override public void onPlayFromMediaId(String mediaId, Bundle extras) {
                LOG.d(TAG, "onPlayFromMediaId", mediaId);
                if (TxtUtils.isNotEmpty(mediaId) && !mediaId.equals(activeBookPath)) {
                    // Switching books: start the newly chosen one from its saved position.
                    AppSP.get().lastBookPath = mediaId;
                    activeBookPath = mediaId;
                    // Resume where the book was left off. The position is stored in AppBook as a
                    // fraction, so it needs the page count from the library metadata to resolve.
                    int page = 0;
                    try {
                        final FileMeta meta = AppDB.get().getOrCreate(mediaId);
                        final AppBook bs = SettingsManager.getBookSettings(mediaId);
                        final Integer pages = meta != null ? meta.getPages() : null;
                        if (bs != null && pages != null && pages > 0) {
                            page = bs.getCurrentPage(pages).viewIndex;
                        }
                    } catch (Exception e) {
                        LOG.e(e);
                    }
                    AppSP.get().lastBookPage = page;
                    activePage = page;
                    setActiveParagraph(0);
                    cache = null;
                }
                playPage("", activePage, null);
                EventBus.getDefault()
                        .post(new TtsStatus());
                TTSNotification.showLast();
            }

            @Override public void onPlay() {
                dispatchAction(TTSNotification.TTS_PLAY);
            }

            @Override public void onPause() {
                dispatchAction(TTSNotification.TTS_PAUSE);
            }

            @Override public void onStop() {
                dispatchAction(TTSNotification.TTS_STOP_DESTROY);
            }

            @Override public void onSkipToNext() {
                dispatchAction(TTSNotification.TTS_NEXT);
            }

            @Override public void onSkipToPrevious() {
                dispatchAction(TTSNotification.TTS_PREV);
            }

            @Override public void onSeekTo(long pos) {
                // The bar is in estimated listening time; convert it back into a page.
                final int maxPages = AppSP.get().lastBookPageCount;
                final long perPage = estimatedPageMs();
                int page = perPage > 0 ? (int) (pos / perPage) : 0;
                if (page < 0) {
                    page = 0;
                }
                if (maxPages > 0 && page > maxPages - 1) {
                    page = maxPages - 1;
                }
                LOG.d(TAG, "onSeekTo", pos, "page", page);
                playPage("", page, null);
                EventBus.getDefault()
                        .post(new TtsStatus());
                TTSNotification.showLast();
            }
        });

        Intent intent = new Intent(Intent.ACTION_MEDIA_BUTTON);
        mediaButtonIntent.setClass(this, MediaButtonReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(this, 0, intent, PendingIntent.FLAG_IMMUTABLE);
        try {
            mMediaSessionCompat.setMediaButtonReceiver(pendingIntent);
        } catch (Exception e) {
            LOG.e(e);
        }

        // Publishing the token is what lets Android Auto (and the Assistant) attach to this
        // session once they have connected to the browser service.
        setSessionToken(mMediaSessionCompat.getSessionToken());
        sessionRef = mMediaSessionCompat;
        updatePlaybackState();

        // mMediaSessionCompat.setPlaybackState(new
        // PlaybackStateCompat.Builder().setActions(PlaybackStateCompat.ACTION_PLAY_PAUSE).setState(PlaybackStateCompat.STATE_CONNECTING,
        // 0, 0f).build());

        TTSEngine.get()
                 .getTTS();

        if (Build.VERSION.SDK_INT >= 24) {
            MediaPlayer mp = new MediaPlayer();
            try {
                final AssetFileDescriptor afd = getAssets().openFd("silence.mp3");
                mp.setDataSource(afd);
                mp.prepareAsync();
                mp.start();
                mp.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                    @Override public void onCompletion(MediaPlayer mp) {
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

    @Override public IBinder onBind(Intent intent) {
        // MediaBrowserServiceCompat handles the browser-service binding itself; returning null
        // here would stop Android Auto from ever connecting.
        return super.onBind(intent);
    }

    private static final String MEDIA_ROOT_ID = "librera_tts_root";
    /**
     * Browse items travel to Android Auto over a Binder transaction with a ~1MB ceiling, and each
     * item carries a cover bitmap, so the recent list is capped and the covers kept small.
     */
    private static final int MAX_BROWSE_ITEMS = 20;
    private static final int BROWSE_COVER_PX = 128;

    @Override public BrowserRoot onGetRoot(String clientPackageName, int clientUid, Bundle rootHints) {
        LOG.d(TAG, "onGetRoot", clientPackageName, clientUid);
        return new BrowserRoot(MEDIA_ROOT_ID, null);
    }

    @Override public void onLoadChildren(String parentId,
            Result<List<MediaBrowserCompat.MediaItem>> result) {
        if (!MEDIA_ROOT_ID.equals(parentId)) {
            result.sendResult(new ArrayList<MediaBrowserCompat.MediaItem>());
            return;
        }
        // Reading the database and decoding covers is far too slow for the main thread, so the
        // result is detached and delivered once the list is built.
        result.detach();
        new Thread(new Runnable() {
            @Override public void run() {
                result.sendResult(loadRecentBooks());
            }
        }, "tts-browse").start();
    }

    private List<MediaBrowserCompat.MediaItem> loadRecentBooks() {
        final List<MediaBrowserCompat.MediaItem> items = new ArrayList<>();
        try {
            final List<FileMeta> recent = AppData.get().getAllRecent(true);
            if (recent == null) {
                return items;
            }
            for (FileMeta meta : recent) {
                if (items.size() >= MAX_BROWSE_ITEMS) {
                    break;
                }
                if (meta == null || TxtUtils.isEmpty(meta.getPath())) {
                    continue;
                }
                final String path = meta.getPath();

                String title = meta.getTitle();
                if (TxtUtils.isEmpty(title)) {
                    title = ExtUtils.getFileName(path);
                }

                final MediaDescriptionCompat.Builder description = new MediaDescriptionCompat.Builder()
                        .setMediaId(path)
                        .setTitle(title)
                        .setSubtitle(meta.getAuthor());

                final Bitmap cover = loadCover(path);
                if (cover != null) {
                    description.setIconBitmap(cover);
                }

                items.add(new MediaBrowserCompat.MediaItem(description.build(),
                        MediaBrowserCompat.MediaItem.FLAG_PLAYABLE));
            }
        } catch (Exception e) {
            LOG.e(e);
        }
        LOG.d(TAG, "loadRecentBooks", items.size());
        return items;
    }

    private Bitmap loadCover(String path) {
        try {
            return IMG.getCoverPageWithEffect(getApplicationContext(), path, null)
                      .submit(BROWSE_COVER_PX, BROWSE_COVER_PX)
                      .get();
        } catch (Throwable e) {
            LOG.d(TAG, "loadCover failed", path);
            return null;
        }
    }

    public boolean startMyForeground() {
        if (!isStartForeground) {
            startServiceWithNotification();
            isStartForeground = true;
        }
        return isStartForeground;
    }

    private String placeholderText() {
        try {
            final String path = AppSP.get().lastBookPath;
            if (TxtUtils.isNotEmpty(path)) {
                return ExtUtils.getFileName(path);
            }
        } catch (Exception e) {
            LOG.e(e);
        }
        return getString(R.string.please_wait);
    }

    private void startServiceWithNotification() {
        PendingIntent stopDestroy = PendingIntent.getService(this, 0,
                new Intent(TTSNotification.TTS_STOP_DESTROY, null, this, TTSService.class),
                PendingIntent.FLAG_IMMUTABLE);
        Notification notification = new NotificationCompat.Builder(this, TTSNotification.CHANNEL_PLAYBACK) //
                                                                                                  .setSmallIcon(
                                                                                                          R.drawable.ic_notification_librera) //
                                                                                                  .setContentTitle(
                                                                                                          Apps.getApplicationName(
                                                                                                                  this)) //
                                                                                                  .setContentText(
                                                                                                          placeholderText())
                                                                                                  .addAction(
                                                                                                          R.drawable.glyphicons_599_menu_close,
                                                                                                          getString(
                                                                                                                  R.string.stop),
                                                                                                          stopDestroy)//
                                                                                                  .setPriority(
                                                                                                          NotificationCompat.PRIORITY_DEFAULT)//
                                                                                                  .build();

//        if (Build.VERSION.SDK_INT >= 29) {
//            startForeground(TTSNotification.NOT_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK);
//        } else {
//            startForeground(TTSNotification.NOT_ID, notification);
//
//        }
        ServiceCompat.startForeground(this, TTSNotification.NOT_ID, notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK);
    }

    public static boolean isServiceRunning(Class<?> serviceClass, Context context) {
        ActivityManager manager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.getName()
                            .equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }

    @TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH_MR1) @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        startMyForeground();

        LOG.d(TAG, "onStartCommand", intent);
        if (intent == null) {
            return START_STICKY;
        }

        updateTimer();
        MediaButtonReceiver.handleIntent(mMediaSessionCompat, intent);

        LOG.d(TAG, "onStartCommand", intent.getAction());
        if (intent.getExtras() != null) {
            LOG.d(TAG, "onStartCommand", intent.getAction(), intent.getExtras());
            for (String key : intent.getExtras()
                                    .keySet()) {
                LOG.d(TAG, key, "=>", intent.getExtras()
                                            .get(key));
            }
        }

        if (TTSNotification.TTS_STOP_DESTROY.equals(intent.getAction())) {
            TTSEngine.get()
                     .mp3Destroy();
            BookCSS.get()
                   .mp3BookPath(null);
            AppState.get().mp3seek = 0;
            stopMediaSesstionAndReleaweWakeLock();

            TTSEngine.get()
                     .stopDestroy();

            EventBus.getDefault()
                    .post(new TtsStatus());

            TTSNotification.hideNotification();
            stopForeground(true);
            stopSelf();

            return START_STICKY;
        }

        if (TTSNotification.TTS_PLAY_PAUSE.equals(intent.getAction())) {

            if (TTSEngine.get()
                         .isMp3PlayPause()) {
                return START_STICKY;
            }

            if (TTSEngine.get()
                         .isPlaybackRequested()) {
                restoreActivePosition();
                stopMediaSesstionAndReleaweWakeLock();
            } else {
                restoreActivePosition();
                playPage("", activePage, null);
            }
            TTSNotification.showLast();
        }
        if (TTSNotification.TTS_PAUSE.equals(intent.getAction())) {

            if (TTSEngine.get()
                         .isMp3PlayPause()) {
                return START_STICKY;
            }

            restoreActivePosition();
            stopMediaSesstionAndReleaweWakeLock();
            TTSNotification.showLast();
        }

        if (TTSNotification.TTS_PLAY.equals(intent.getAction())) {

            if (TTSEngine.get()
                         .isMp3PlayPause()) {

                return START_STICKY;
            }

            restoreActivePosition();
            playPage("", activePage, null);
            TTSNotification.showLast();
        }
        if (TTSNotification.TTS_NEXT.equals(intent.getAction())) {

            if (TTSEngine.get()
                         .isMp3()) {
                TTSEngine.get()
                         .mp3Next();
                return START_STICKY;
            }

            setActiveParagraph(0);
            playPage("", activePage + 1, null);
        }
        if (TTSNotification.TTS_PREV.equals(intent.getAction())) {

            if (TTSEngine.get()
                         .isMp3()) {
                TTSEngine.get()
                         .mp3Prev();
                return START_STICKY;
            }

            setActiveParagraph(0);
            //stopMediaSesstionAndReleaweWakeLock();
            playPage("", activePage - 1, null);
        }

        if (ACTION_PLAY_CURRENT_PAGE.equals(intent.getAction())) {
            if (TTSEngine.get()
                         .isMp3PlayPause()) {
                TTSNotification.show(AppSP.get().lastBookPath, -1, -1);
                return START_STICKY;
            }

            int pageNumber = intent.getIntExtra(EXTRA_INT, -1);
            String previousActiveBook = activeBookPath;
            activeBookPath = intent.getStringExtra(EXTRA_PATH);
            activePage = pageNumber;
            activeParagraph = previousActiveBook != null
                    && !previousActiveBook.equals(activeBookPath)
                    ? 0 : AppSP.get().lastBookParagraph;
            AppSP.get().lastBookPath = activeBookPath;
            AppSP.get().lastBookParagraph = activeParagraph;
            String anchor = intent.getStringExtra(EXTRA_ANCHOR);

            if (pageNumber != -1) {
                playPage("", pageNumber, anchor);
            }
        }

        EventBus.getDefault()
                .post(new TtsStatus());

        return START_STICKY;
    }

    private final ExecutorService speechExecutor = Executors.newSingleThreadExecutor();
    private void postSpeechTask(Runnable task) {
        try {
            speechExecutor.execute(task);
        } catch (RejectedExecutionException e) {
            if (!speechExecutor.isShutdown()) throw e;
        }
    }

    private final AtomicLong speechPageGeneration = new AtomicLong();

    private void stopMediaSesstionAndReleaweWakeLock() {
        speechPageGeneration.incrementAndGet();
        TTSEngine.get()
                 .stop(mMediaSessionCompat);
        releaseWakeLock();
        updatePlaybackState();
        EventBus.getDefault()
                .post(new TtsStatus());
    }

    private CodecDocument preparedDocument;
    private int preparedPage = -1;
    private String preparedText;

    // Keep just one page ahead, and never reuse it for a seek or a different document.
    private synchronized String readPageText(CodecDocument document, int pageNumber, boolean continuing) {
        if (continuing && preparedDocument == document && preparedPage == pageNumber
                && preparedText != null) {
            String result = preparedText;
            preparedText = null;
            return result;
        }
        CodecPage page = document.getPage(pageNumber);
        if (page == null) return null;
        try {
            return TxtUtils.replaceHTMLforTTS(page.getPageHTML());
        } finally {
            page.recycle();
        }
    }

    private synchronized void prepareNextPage(CodecDocument document, int pageNumber) {
        if (cache != document || activePage != pageNumber - 1
                || !TTSEngine.get().isPlaybackRequested()) return;
        preparedText = null;
        if (pageNumber >= document.getPageCount()) return;
        try {
            preparedText = readPageText(document, pageNumber, false);
            preparedDocument = document;
            preparedPage = pageNumber;
        } catch (Exception e) {
            // A failed speculative read must not interrupt the current page.
            LOG.e(e);
        }
    }

    public synchronized CodecDocument getDC() {
        try {

            if (AppSP.get().lastBookPath != null && AppSP.get().lastBookPath.equals(
                    path) && cache != null && wh == AppSP.get().lastBookWidth + AppSP.get().lastBookHeight) {
                LOG.d(TAG, "CodecDocument from cache", AppSP.get().lastBookPath);
                return cache;
            }
            if (cache != null) {
                preparedText = null;
                preparedDocument = null;
                cache.recycle();
                cache = null;
            }
            path = AppSP.get().lastBookPath;
            LOG.d(TAG, "CodecDocument", "loadingCancelled", TempHolder.get().loadingCancelled);
            cache = ImageExtractor.singleCodecContext(AppSP.get().lastBookPath, "");
            if (cache == null) {
                TTSNotification.hideNotification();
                return null;
            }
            cache.getPageCount(AppSP.get().lastBookWidth, AppSP.get().lastBookHeight, BookCSS.get().fontSizeSp);
            wh = AppSP.get().lastBookWidth + AppSP.get().lastBookHeight;
            LOG.d(TAG, "CodecDocument new", AppSP.get().lastBookPath, AppSP.get().lastBookWidth,
                    AppSP.get().lastBookHeight);
            return cache;
        } catch (Exception e) {
            LOG.e(e);
            return null;
        }
    }

    /**
     * Token of the running session, or null when the service is not up. Used by TTSNotification.
     */
    public static MediaSessionCompat.Token getMediaSessionToken() {
        final MediaSessionCompat session = sessionRef;
        return session != null ? session.getSessionToken() : null;
    }

    /**
     * Publishes the play/pause state to the media session. Without a PlaybackState the system
     * media controls (lock screen, quick settings, Bluetooth, Android Auto) have no actions to
     * show, so the session token on its own would not be enough.
     */
    /** Elapsed listening time: whole pages already read plus progress through the current one. */
    private static long currentPositionMs() {
        final long perPage = estimatedPageMs();
        if (perPage <= 0) {
            return 0L;
        }
        long position = Math.max(0, AppSP.get().lastBookPage) * perPage;
        final long start = pageStartMs;
        if (start > 0 && TTSEngine.get().isPlaybackRequested()) {
            position += Math.min(System.currentTimeMillis() - start, perPage);
        }
        final long duration = bookDurationMs();
        return duration > 0 ? Math.min(position, duration) : position;
    }

    static void updatePlaybackState() {
        try {
            final MediaSessionCompat session = sessionRef;
            if (session == null) {
                return;
            }
            final boolean playing = TTSEngine.get().isPlaybackRequested();
            // Keep the session active even while paused. Android 13+ renders a MediaStyle
            // notification only through the media controls, and hides it completely when the
            // session is inactive - TTSEngine.stop() deactivates it on pause, which would
            // otherwise make the notification vanish instead of showing a paused player.
            session.setActive(true);
            session.setPlaybackState(new PlaybackStateCompat.Builder()
                    .setActions(PlaybackStateCompat.ACTION_PLAY
                            | PlaybackStateCompat.ACTION_PAUSE
                            | PlaybackStateCompat.ACTION_PLAY_PAUSE
                            | PlaybackStateCompat.ACTION_STOP
                            | PlaybackStateCompat.ACTION_SKIP_TO_NEXT
                            | PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS
                            | PlaybackStateCompat.ACTION_SEEK_TO)
                    // The timeline is now real time, so let the system extrapolate while playing -
                    // that keeps the bar moving smoothly between page updates.
                    .setState(playing ? PlaybackStateCompat.STATE_PLAYING : PlaybackStateCompat.STATE_PAUSED,
                            currentPositionMs(), playing ? 1f : 0f)
                    .build());
        } catch (Exception e) {
            LOG.e(e);
        }
    }

    /**
     * Routes a media-session transport callback through the same intent handling that the
     * notification buttons use, so both paths behave identically (mp3 mode included).
     */
    void dispatchAction(String action) {
        try {
            startService(new Intent(action, null, this, TTSService.class));
        } catch (Exception e) {
            LOG.e(e);
        }
    }

    /**
     * Publishes what the system media view shows: cover, title and - through DURATION - the length
     * of the seek bar. Without a duration Android draws no seek bar at all.
     */
    public static void updateMediaMetadata(String title, String subTitle, Bitmap cover) {
        try {
            final MediaSessionCompat session = sessionRef;
            if (session == null) {
                return;
            }
            final MediaMetadataCompat.Builder b = new MediaMetadataCompat.Builder()
                    .putString(MediaMetadataCompat.METADATA_KEY_TITLE, title)
                    .putString(MediaMetadataCompat.METADATA_KEY_ARTIST, subTitle)
                    .putString(MediaMetadataCompat.METADATA_KEY_ALBUM, subTitle)
                    .putLong(MediaMetadataCompat.METADATA_KEY_DURATION, bookDurationMs());
            if (cover != null) {
                b.putBitmap(MediaMetadataCompat.METADATA_KEY_ALBUM_ART, cover);
            }
            session.setMetadata(b.build());
        } catch (Exception e) {
            LOG.e(e);
        }
    }

    @TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH_MR1)
    private void playPage(String preText, int pageNumber, String anchor) {
        playPage(preText, pageNumber, anchor, false);
    }

    private void playPage(String preText, int pageNumber, String anchor, boolean continuing) {
        final long generation = speechPageGeneration.incrementAndGet();
        if (!speechExecutor.isShutdown()) {
            postSpeechTask(() -> {
                if (speechPageGeneration.get() == generation) {
                    try {
                        playPageOnWorker(preText, pageNumber, anchor, continuing, generation);
                    } catch (RuntimeException e) {
                        LOG.e(e);
                        if (speechPageGeneration.get() == generation) stopMediaSesstionAndReleaweWakeLock();
                    }
                }
            });
        }
    }

    private void playPageOnWorker(String preText, int pageNumber, String anchor,
                                  boolean continuing, long speechGeneration) {
        if (activeBookPath != null && !activeBookPath.equals(AppSP.get().lastBookPath))
            restoreActivePosition();
        //releaseWakeLock();
        acquireWakeLock();
        mMediaSessionCompat.setActive(true);
        updatePlaybackState();

        if (!continuing && !AppState.get().allowOtherMusic) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                mAudioManager.requestAudioFocus((AudioFocusRequest) audioFocusRequest);
            } else {
                mAudioManager.requestAudioFocus(listener, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN);
            }
        }

        LOG.d("playPage", preText, pageNumber, anchor);
        if (pageNumber != -1) {
            isActivated = true;
            AppSP.get().lastBookPage = pageNumber;
            activePage = pageNumber;
            CodecDocument dc = getDC();
            if (dc == null) {
                LOG.d(TAG, "CodecDocument", "is NULL");
                TTSNotification.hideNotification();
                stopMediaSesstionAndReleaweWakeLock();
                return;
            }

            AppSP.get().lastBookPageCount = dc.getPageCount();
            LOG.d(TAG, "CodecDocument PageCount", pageNumber, AppSP.get().lastBookPageCount);
            if (pageNumber >= AppSP.get().lastBookPageCount) {
                Vibro.vibrateFinish();
                LOG.d(TAG, "CodecDocument Book is Finished");
                EventBus.getDefault()
                        .post(new TtsStatus());

                stopMediaSesstionAndReleaweWakeLock();
                stopSelf();
                return;
            }

            String pageHTML = readPageText(dc, pageNumber, continuing);
            if (pageHTML == null) {
                EventBus.getDefault()
                        .post(new TtsStatus());

                stopMediaSesstionAndReleaweWakeLock();
                stopSelf();
                return;
            }

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
                    playPage("", AppSP.get().lastBookPage + 1, null, true);
                } else {
                    stopMediaSesstionAndReleaweWakeLock();
                }
                return;
            }
            emptyPageCount = 0;

            String[] parts = TxtUtils.getParts(pageHTML);
            String firstPart =
                    pageNumber + 1 >= AppSP.get().lastBookPageCount || AppState.get().ttsTunnOnLastWord ? pageHTML :
                            parts[0];
            final String secondPart =
                    pageNumber + 1 >= AppSP.get().lastBookPageCount || AppState.get().ttsTunnOnLastWord ? "" : parts[1];

            int pageBoundary = 0;
            if (TxtUtils.isNotEmpty(preText)) {
                char last = preText.charAt(preText.length() - 1);
                if (last == '-') {
                    preText = TxtUtils.replaceLast(preText, "-", "");
                    pageBoundary = preText.length();
                    firstPart = preText + firstPart;
                } else {
                    pageBoundary = preText.length() + 1;
                    firstPart = preText + " " + firstPart;
                }
            }
            final String preText1 = preText;
            final boolean deferPageTurn = continuing && pageBoundary > 0
                    && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                    && TTSEngine.get().hasRangeTiming();
            final SpeechRequest request = new SpeechRequest(deferPageTurn ? pageBoundary : 0);
            final AtomicBoolean pageTurned = new AtomicBoolean(false);
            final Runnable turnPage = () -> {
                if (speechPageGeneration.get() == speechGeneration && activePage == pageNumber
                        && TTSEngine.get().isPlaybackRequested()
                        && pageTurned.compareAndSet(false, true)) {
                    EventBus.getDefault().post(new MessagePageNumber(pageNumber));
                }
            };

            if (Build.VERSION.SDK_INT >= 15) {
                TTSEngine.get()
                         .getTTS()
                         .setOnUtteranceProgressListener(new UtteranceProgressListener() {
                             @Override public void onStart(String rawId) {
                                 reportRange(rawId, 1, false);
                             }

                             @Override public void onRangeStart(String rawId, int start, int end, int frame) {
                                 reportRange(rawId, end, true);
                             }

                             private void reportRange(String rawId, int end, boolean timed) {
                                 String signal = request.signal(rawId);
                                 if (signal == null || speechPageGeneration.get() != speechGeneration) return;
                                 if (timed) TTSEngine.get().noteRangeTiming(request);
                                 if (deferPageTurn && request.reachesPage(signal, end)) turnPage.run();
                             }

                             @Override public void onError(String rawId) {
                                 dispatch(rawId, signal -> stopMediaSesstionAndReleaweWakeLock());
                             }

                             private void dispatch(String rawId, java.util.function.Consumer<String> action) {
                                 String signal = request.signal(rawId);
                                 if (signal == null || speechExecutor.isShutdown()) return;
                                 postSpeechTask(() -> {
                                     if (speechPageGeneration.get() == speechGeneration) action.accept(signal);
                                 });
                             }

                             @Override public void onDone(String rawId) {
                                 dispatch(rawId, this::onDoneOwned);
                             }

                             private void onDoneOwned(String utteranceId) {

                                 LOG.d(TAG, "onUtteranceCompleted", utteranceId);
                                 // Engines without range timing fall back to the containing utterance end.
                                 if (deferPageTurn && request.reachesPage(utteranceId, Integer.MAX_VALUE)) {
                                     turnPage.run();
                                 }
                                 if (utteranceId.startsWith(TTSEngine.STOP_SIGNAL)) {
                                     stopMediaSesstionAndReleaweWakeLock();

                                     return;
                                 }
                                 if (utteranceId.startsWith(TTSEngine.FINISHED_SIGNAL)) {
                                     if (TxtUtils.isNotEmpty(preText1)) {
                                         setActiveParagraph(Integer.parseInt(
                                                 utteranceId.replace(TTSEngine.FINISHED_SIGNAL, "")));
                                     } else {
                                         setActiveParagraph(Integer.parseInt(
                                                 utteranceId.replace(TTSEngine.FINISHED_SIGNAL, "")) + 1);
                                     }
                                     return;
                                 }

                                 if (!utteranceId.equals(TTSEngine.UTTERANCE_ID_DONE)) {
                                     LOG.d(TAG, "onUtteranceCompleted skip", utteranceId);
                                     return;
                                 }

                                 if (System.currentTimeMillis() > TempHolder.get().timerFinishTime) {
                                     LOG.d(TAG, "Update-timer-Stop1");
                                     stopMediaSesstionAndReleaweWakeLock();
                                     stopSelf();
                                     return;
                                 }

                                 onPageSpeechFinished();
                                 setActiveParagraph(0);
                                 if (speechPageGeneration.get() == speechGeneration) {
                                     playPage(secondPart, pageNumber + 1, null, true);
                                 }
                             }
                         });
            } else {
                TTSEngine.get()
                         .getTTS()
                         .setOnUtteranceCompletedListener(new OnUtteranceCompletedListener() {
                             @Override public void onUtteranceCompleted(String rawId) {
                                 String signal = request.signal(rawId);
                                 if (signal == null || speechExecutor.isShutdown()) return;
                                 postSpeechTask(() -> {
                                     if (speechPageGeneration.get() == speechGeneration) onDoneOwned(signal);
                                 });
                             }

                             private void onDoneOwned(String utteranceId) {
                                 if (utteranceId.startsWith(TTSEngine.STOP_SIGNAL)) {
                                     stopMediaSesstionAndReleaweWakeLock();

                                     return;
                                 }
                                 if (utteranceId.startsWith(TTSEngine.FINISHED_SIGNAL)) {
                                     if (TxtUtils.isNotEmpty(preText1)) {
                                         setActiveParagraph(Integer.parseInt(
                                                 utteranceId.replace(TTSEngine.FINISHED_SIGNAL, "")));
                                     } else {
                                         setActiveParagraph(Integer.parseInt(
                                                 utteranceId.replace(TTSEngine.FINISHED_SIGNAL, "")) + 1);
                                     }
                                     return;
                                 }

                                 if (!utteranceId.equals(TTSEngine.UTTERANCE_ID_DONE)) {
                                     LOG.d(TAG, "onUtteranceCompleted skip", "");
                                     return;
                                 }

                                 LOG.d(TAG, "onUtteranceCompleted", utteranceId);
                                 if (System.currentTimeMillis() > TempHolder.get().timerFinishTime) {
                                     LOG.d(TAG, "Update-timer-Stop2");
                                     stopMediaSesstionAndReleaweWakeLock();
                                     stopSelf();
                                     return;
                                 }

                                 onPageSpeechFinished();
                                 setActiveParagraph(0);
                                 if (speechPageGeneration.get() == speechGeneration) {
                                     playPage(secondPart, pageNumber + 1, null, true);
                                 }
                             }
                         });
            }

            onPageSpeechStarted(firstPart);

            if (speechPageGeneration.get() != speechGeneration) return;
            boolean submitted = TTSEngine.get().speek(firstPart, continuing, request);
            if (speechPageGeneration.get() != speechGeneration) return;
            if (!submitted) {
                stopMediaSesstionAndReleaweWakeLock();
                return;
            }

            // Submit audio before synchronous page-turn subscribers can render the page.
            if (!deferPageTurn) turnPage.run();

            TTSNotification.show(activeBookPath, pageNumber + 1, dc.getPageCount());
            LOG.d("TtsStatus send");
            EventBus.getDefault()
                    .post(new TtsStatus());

            TTSNotification.showLast();

            final String progressBook = activeBookPath;
            final int progressPageCount = AppSP.get().lastBookPageCount;
            new Thread(() -> {
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                }
                AppBook load = SharedBooks.load(progressBook);
                load.currentPageChanged(pageNumber + 1, progressPageCount);

                SharedBooks.saveAsync(load);
                AppProfile.save(this);
            }, "@T TTS Save").start();

            prepareNextPage(dc, pageNumber + 1);
        }
    }

    @Override public void onDestroy() {
        super.onDestroy();

        isStartForeground = false;
        try {
            unregisterReceiver(blueToothReceiver);
        } catch (Exception e) {
            LOG.e(e);
        }

        stopMediaSesstionAndReleaweWakeLock();
        TTSEngine.get()
                 .shutdown();

        TTSNotification.hideNotification();

        isActivated = false;

        //mAudioManager.abandonAudioFocus(listener);

        abandonAudioFocusCompat();

        //mMediaSessionCompat.setCallback(null);
        sessionRef = null;
        serviceRef = null;
        mMediaSessionCompat.release();

        postSpeechTask(() -> {
            if (cache != null) {
                cache.recycle();
                cache = null;
            }
            preparedDocument = null;
            preparedText = null;
            path = null;
        });
        speechExecutor.shutdown();
        LOG.d(TAG, "onDestroy:TTS playBookPage1");
    }

    Object lock = new Object();

    private void acquireWakeLock() {
        try {

            if (wakeLock != null) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    wakeLock.acquire(WAKE_LOCK_TIMEOUT);
                } else {
                    wakeLock.acquire();
                }
                LOG.d(TAG, "WakeLock acquired");
            }

        } catch (Exception e) {
            LOG.e(e);
        }
    }

    private void releaseWakeLock() {
        try {
            if (wakeLock != null) {
                wakeLock.release();
                LOG.d(TAG, "WakeLock released");
            }
        } catch (Exception e) {
            LOG.e(e);
        }
    }
}
