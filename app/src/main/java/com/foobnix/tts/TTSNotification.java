package com.foobnix.tts;

import android.annotation.TargetApi;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.RemoteViews;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;
import com.foobnix.LibreraApp;
import com.foobnix.android.utils.Apps;
import com.foobnix.android.utils.LOG;
import com.foobnix.android.utils.TxtUtils;
import com.foobnix.dao2.FileMeta;
import com.foobnix.model.AppSP;
import com.foobnix.model.AppState;
import com.foobnix.pdf.info.ExtUtils;
import com.foobnix.pdf.info.IMG;
import com.foobnix.pdf.info.R;
import com.foobnix.pdf.info.model.BookCSS;
import com.foobnix.pdf.info.widget.TTSWidget;
import com.foobnix.pdf.search.activity.HorizontalViewActivity;
import com.foobnix.ui2.AppDB;

import org.ebookdroid.ui.viewer.VerticalViewActivity;

import java.io.File;

public class TTSNotification {

    public static final String DEFAULT = "default";
    /**
     * Playback channel. A separate id from DEFAULT because an existing channel's importance can
     * never be raised programmatically - the user owns it once created. DEFAULT was
     * IMPORTANCE_LOW, which marks the notification silent, and Android 12+ drops status bar
     * icons for silent notifications. IMPORTANCE_DEFAULT with no sound keeps it quiet while
     * keeping the icon pinned in the status bar, the way media players do it.
     */
    public static final String CHANNEL_PLAYBACK = "playback";

    public static final String ACTION_TTS = "TTSNotification_TTS";

    public static final String TTS_PLAY = "TTS_PLAY";
    public static final String TTS_PAUSE = "TTS_PAUSE";
    public static final String TTS_PLAY_PAUSE = "TTS_PLAY_PAUSE";
    public static final String TTS_STOP_DESTROY = "TTS_STOP_DESTROY";
    public static final String TTS_NEXT = "TTS_NEXT";
    public static final String TTS_PREV = "TTS_PREV";
    public static final int NOT_ID = 10;
    public static final int NOT_ID_2 = 11;
    private static final String KEY_TEXT_REPLY = "key_text_reply";
    static String bookPath1;
    static int page1;
    static int pageCount;

    private static Context context;
    static Runnable run = new Runnable() {

        @Override
        public void run() {
            show(bookPath1, page1, pageCount);
        }
    };
    private static Handler handler;

    @TargetApi(26)
    public static void initChannels(Context context) {
        TTSNotification.context = context;
        handler = new Handler(Looper.getMainLooper());

        if (Build.VERSION.SDK_INT < 26) {
            return;
        }
        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        NotificationChannel channel = new NotificationChannel(CHANNEL_PLAYBACK,
                Apps.getApplicationName(context), NotificationManager.IMPORTANCE_DEFAULT);
        channel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
        // Quiet, but not "silent" in the system's sense - that distinction is what decides
        // whether the icon stays in the status bar.
        channel.setSound(null, null);
        channel.enableVibration(false);
        channel.setShowBadge(false);

        notificationManager.createNotificationChannel(channel);

        // The old low-importance channel is no longer used; drop it so it does not linger in
        // the app's notification settings.
        try {
            notificationManager.deleteNotificationChannel(DEFAULT);
        } catch (Exception e) {
            LOG.e(e);
        }

    }

    public static void show(String bookPath, int page, int maxPages) {
        bookPath1 = bookPath;
        page1 = page;
        pageCount = maxPages;
        try {
            NotificationManager nm = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

            NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_PLAYBACK);

            FileMeta fileMeta = AppDB.get().getOrCreate(bookPath);

            boolean isEasyMode = AppSP.get().readingMode == AppState.READING_MODE_BOOK;

            Intent intent = new Intent(context, isEasyMode ? HorizontalViewActivity.class : VerticalViewActivity.class);//TO-CHECK
            intent.setAction(ACTION_TTS);
            intent.setData(Uri.fromFile(new File(bookPath)));
            if (page > 0) {
                intent.putExtra("page", page - 1);
            }

            PendingIntent contentIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_IMMUTABLE);

            PendingIntent playPause = PendingIntent.getService(context, 0, new Intent(TTS_PLAY_PAUSE, null, context, TTSService.class), PendingIntent.FLAG_IMMUTABLE);
            PendingIntent pause = PendingIntent.getService(context, 0, new Intent(TTS_PAUSE, null, context, TTSService.class), PendingIntent.FLAG_IMMUTABLE);
            PendingIntent play = PendingIntent.getService(context, 0, new Intent(TTS_PLAY, null, context, TTSService.class), PendingIntent.FLAG_IMMUTABLE);
            PendingIntent next = PendingIntent.getService(context, 0, new Intent(TTS_NEXT, null, context, TTSService.class), PendingIntent.FLAG_IMMUTABLE);
            PendingIntent prev = PendingIntent.getService(context, 0, new Intent(TTS_PREV, null, context, TTSService.class), PendingIntent.FLAG_IMMUTABLE);
            PendingIntent stopDestroy = PendingIntent.getService(context, 0, new Intent(TTS_STOP_DESTROY, null, context, TTSService.class), PendingIntent.FLAG_IMMUTABLE);

            final int color = AppState.get().isUiTextColor ? AppState.get().uiTextColor : AppState.get().tintColor;

            String fileMetaBookName = TxtUtils.getFileMetaBookName(fileMeta);

            String pageNumber = "(" + TxtUtils.getProgressPercent(page, maxPages) + " " + page + "/" + maxPages + ")";

            if (page == -1 || maxPages == -1) {
                pageNumber = "";
            }
            final String pageNumberText = pageNumber;

            String textLine = pageNumber + " " + fileMetaBookName;

            if (TxtUtils.isNotEmpty(BookCSS.get().mp3BookPathGet())) {
                textLine = "[" + ExtUtils.getFileName(BookCSS.get().mp3BookPathGet()) + "] " + textLine;
            }

            final String extraText = textLine;

            //final String url = IMG.getCoverUrl(bookPath);
            //String url = IMG.toUrl(bookPath, ImageExtractor.COVER_PAGE_NO_EFFECT, IMG.getImageSize());


            IMG.getCoverPageWithEffect(LibreraApp.context,bookPath,null).into(new CustomTarget<Bitmap>() {
                @Override
                public void onResourceReady(@NonNull Bitmap resource, @Nullable Transition<? super Bitmap> transition) {
                    // The notification uses Android's own media UI. Custom RemoteViews are not
                    // set: once a MediaSession token is attached, Android 13+ renders media
                    // notifications with the system template and ignores custom content views.
                    final androidx.media.app.NotificationCompat.MediaStyle mediaStyle =
                            new androidx.media.app.NotificationCompat.MediaStyle();
                    final android.support.v4.media.session.MediaSessionCompat.Token token =
                            TTSService.getMediaSessionToken();
                    if (token != null) {
                        mediaStyle.setMediaSession(token);
                        mediaStyle.setShowActionsInCompactView(0, 1, 2);
                    }

                    final boolean isPlaying = TTSEngine.get().isPlaying();

                    // Refresh the session before posting: a MediaStyle notification is only
                    // rendered while its session is active, so this guarantees the notification
                    // is actually visible whenever it is shown.
                    TTSService.updatePlaybackState();

                    // Cover + title + DURATION for the system player. The duration is what makes
                    // Android draw the seek bar; one "second" per page keeps the bar position
                    // equal to the reading percentage.
                    TTSService.updateMediaMetadata(fileMetaBookName, pageNumberText, resource);

                    builder.setContentIntent(contentIntent) //
                            .setSmallIcon(R.drawable.ic_notification_librera) //
                            .setColor(color)
                            .setOngoing(true)//
                            .setPriority(NotificationCompat.PRIORITY_HIGH) //
                            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)//
                            .setStyle(mediaStyle)
                            .setContentTitle(fileMetaBookName)
                            .setContentText(pageNumberText)
                            .setLargeIcon(resource)
                            .addAction(R.drawable.glyphicons_173_rewind, "prev", prev)
                            .addAction(isPlaying ? R.drawable.glyphicons_174_pause
                                    : R.drawable.glyphicons_175_play, "play", playPause)
                            .addAction(R.drawable.glyphicons_177_forward, "next", next)
                            .addAction(R.drawable.glyphicons_599_menu_close, "stop", stopDestroy);
                    Notification n = builder.build(); //

                    nm.notify(NOT_ID, n);


                }

                @Override
                public void onLoadCleared(@Nullable Drawable placeholder) {

                }
            });


            Intent update = new Intent(LibreraApp.context, TTSWidget.class);
            update.setAction("android.appwidget.action.APPWIDGET_UPDATE");
            update.putExtra(Intent.EXTRA_TEXT, extraText);
            update.putExtra("bookPath", bookPath);
            LibreraApp.context.sendBroadcast(update);
        } catch (Exception e) {
            LOG.e(e);
        }

    }

    public static void hideNotification() {
        try {
            LOG.d("Notification hideNotification");
            NotificationManager nm = (NotificationManager) LibreraApp.context.getSystemService(Context.NOTIFICATION_SERVICE);
            nm.cancel(NOT_ID);
        } catch (Exception e) {
            LOG.e(e);
        }
    }

    public static void showLast() {
        LOG.d("Notification showLast");
        if (TTSEngine.get().isShutdown()) {
            hideNotification();
        } else if (handler != null) {
            handler.removeCallbacks(run);
            handler.postDelayed(run, 500);
        }

    }


}
