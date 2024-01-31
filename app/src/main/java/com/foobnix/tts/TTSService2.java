package com.foobnix.tts;

import static android.media.MediaMetadata.METADATA_KEY_DURATION;

import static com.foobnix.ext.CacheZipUtils.CACHE_TEMP;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
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
import android.media.MediaDataSource;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.speech.tts.TextToSpeech;
import android.speech.tts.TextToSpeech.OnUtteranceCompletedListener;
import android.speech.tts.UtteranceProgressListener;
import android.support.v4.media.MediaDescriptionCompat;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaControllerCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.text.TextUtils;
import android.util.Pair;
import android.view.KeyEvent;

import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.ServiceCompat;
import androidx.core.content.ContextCompat;
import androidx.media.session.MediaButtonReceiver;

import com.foobnix.LibreraApp;
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
import com.foobnix.ui2.MainTabs2;

import org.ebookdroid.common.settings.books.SharedBooks;
import org.ebookdroid.core.codec.CodecDocument;
import org.ebookdroid.core.codec.CodecPage;
import org.greenrobot.eventbus.EventBus;

import java.io.File;
import java.io.FileDescriptor;
import java.io.IOException;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class TTSService2 extends Service {


    public static final String EXTRA_PASSWORD = "EXTRA_PASSWORD";
    public static final String EXTRA_PATH = "EXTRA_PASSWORD";
    public static final String EXTRA_WIDTH = "EXTRA_WIDTH";
    public static final String EXTRA_HEIGHT = "EXTRA_HEIGHT";
    public static final String EXTRA_FONT_SIZE = "EXTRA_FONT_SIZE";
    public static final String EXTRA_PAGE_NUMBER = "EXTRA_PAGE_NUMBER";
    public static final String ACTION_PLAY_PAUSE = "EXTRA_PAGE_NUMBER";
    private final MediaSessionCompat.Callback mediaSessionController = new MediaSessionCompat.Callback() {


        @Override
        public void onPlay() {

        }

        @Override
        public void onPause() {
        }

        @Override
        public void onSkipToNext() {

        }

        @Override
        public void onSkipToPrevious() {
        }

        @Override
        public void onStop() {
        }

        @Override
        public void onPrepare() {

        }


        @Override
        public void onPlayFromUri(Uri uri, Bundle extras) {


        }
    };
    MediaPlayer mp = new MediaPlayer();
    int NOTIFICATION_ID = 123;
    String NOTIFICATION_CHANNEL = "NOTIFICATION_CHANNEL";
    NotificationManager notificationManager;
    Notification nothingPlayingNotification;
    MediaSessionCompat mediaSession;
    MediaPageManger mediaPageManger;

    @Override
    public void onCreate() {
        super.onCreate();

        notificationManager = (NotificationManager) this.getSystemService(Context.NOTIFICATION_SERVICE);

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(NOTIFICATION_CHANNEL,
                    getString(R.string.app_name_pro),
                    NotificationManager.IMPORTANCE_LOW);

            channel.enableLights(false);
            channel.enableVibration(false);
            channel.setShowBadge(false);
            notificationManager.createNotificationChannel(channel);
        }


        nothingPlayingNotification = new NotificationCompat.Builder(this, NOTIFICATION_CHANNEL)
                .setSmallIcon(R.drawable.glyphicons_175_play)
                .setShowWhen(false)
                .setOngoing(true)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setCategory(NotificationCompat.CATEGORY_TRANSPORT)
                .setContentIntent(PendingIntent.getActivity(this, 0, new Intent(this, MainTabs2.class), PendingIntent.FLAG_IMMUTABLE))
                .setContentTitle("title")
                .setProgress(100, 10, true)
                .setContentText("Init...")
                .build();


        MediaSessionCompat mediaSession = new MediaSessionCompat(this, "MEDIA_SESSION_TAG",null, PendingIntent.getActivity(this,123, new Intent(this, MainTabs2.class),PendingIntent.FLAG_IMMUTABLE));
        mediaSession.setCallback(mediaSessionController);


        PlaybackStateCompat stateBuilder = new PlaybackStateCompat.Builder()
                .setActions(PlaybackStateCompat.ACTION_PLAY |
                        PlaybackStateCompat.ACTION_PAUSE |
                        PlaybackStateCompat.ACTION_PLAY_PAUSE |
                        PlaybackStateCompat.ACTION_STOP |
                        PlaybackStateCompat.ACTION_SKIP_TO_NEXT |
                        PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS).build();
        mediaSession.setPlaybackState(stateBuilder);

        mediaSession.setMetadata(new MediaMetadataCompat.Builder()
                .putLong(METADATA_KEY_DURATION, 123)
                .build()
        );

        MediaControllerCompat mcc = new MediaControllerCompat(this, mediaSession);
        //mcc.getQueue().add(new MediaSessionCompat.QueueItem.Bui)
        mcc.getTransportControls().stop();
        mcc.getTransportControls().playFromUri(Uri.parse("sdf"), new Bundle());

        //mcc.addQueueItem(new MediaDescriptionCompat.Builder().setMediaUri(Uri.parse("http://uri")).build());

    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(NOTIFICATION_ID, nothingPlayingNotification, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK);
        } else {
            startForeground(NOTIFICATION_ID, nothingPlayingNotification);
        }

        LOG.d(this.getClass().getName(), "onStartCommand", intent);


        return START_STICKY;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mediaSession.release();
    }

    public void play(String bookFile, int pageNumber) {
        if (mediaPageManger == null) {
            CodecDocument dc = ImageExtractor.singleCodecContext(bookFile, "");
            dc.getPageCount(0, 0, 0);
            mediaPageManger = new MediaPageManger(dc);
        }
        File file = mediaPageManger.createFileToPlay(pageNumber);

        try {
            mp.setDataSource(file.getPath());
        } catch (Exception e) {
            LOG.e(e);
        }

        mp.prepareAsync();
        mp.setOnPreparedListener(mp -> mp.start());
        mp.setOnCompletionListener(mp -> play(bookFile, pageNumber + 1));
    }

    class MediaPageManger {

        CodecDocument dc;
        Map<Integer, String> pages = new LinkedHashMap<>();

        MediaPageManger(CodecDocument dc) {
            this.dc = dc;
        }

        public String getTextToPlay(int pageNumber) {
            String prev = getTextForPage(pageNumber - 1);
            String current = getTextForPage(pageNumber);
            return mergeText(prev, current);
        }

        private String mergeText(String prev, String current) {
            String pre = split(prev).second;
            String cur = split(current).first;
            return pre + " " + cur;
        }

        public Pair<String, String> split(String text) {
            String[] parts = TxtUtils.getParts(text);
            return new Pair<>(parts[0], parts[1]);
        }

        public String getTextForPage(int paNumber) {
            if (pages.containsKey(paNumber)) {
                return pages.get(paNumber);
            }

            CodecPage page = dc.getPage(paNumber);
            String pageText = page.getPageHTML();
            pageText = TxtUtils.replaceHTMLforTTS(pageText);
            page.recycle();
            pages.put(paNumber, pageText);
            return pageText;
        }

        public File createFileToPlay(int page) {
            TextToSpeech ttsEngine = new TextToSpeech(TTSService2.this, new TextToSpeech.OnInitListener() {
                @Override
                public void onInit(int status) {

                }
            });
            File file = new File(CACHE_TEMP, "page-" + page + ".wav");
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                Bundle bundle = new Bundle();
                bundle.putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, "UTTERANCE_ID_DONE");
                ttsEngine.synthesizeToFile(getTextToPlay(page), bundle, file, "");
            }
            return file;
        }

    }
}
