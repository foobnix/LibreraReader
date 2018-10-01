package com.foobnix.tts;

import java.io.File;

import org.ebookdroid.LibreraApp;
import org.ebookdroid.ui.viewer.VerticalViewActivity;

import com.foobnix.android.utils.LOG;
import com.foobnix.android.utils.TxtUtils;
import com.foobnix.dao2.FileMeta;
import com.foobnix.pdf.info.AppsConfig;
import com.foobnix.pdf.info.ExtUtils;
import com.foobnix.pdf.info.IMG;
import com.foobnix.pdf.info.R;
import com.foobnix.pdf.info.TintUtil;
import com.foobnix.pdf.info.wrapper.AppState;
import com.foobnix.pdf.search.activity.HorizontalViewActivity;
import com.foobnix.sys.ImageExtractor;
import com.foobnix.ui2.AppDB;
import com.nostra13.universalimageloader.core.ImageLoader;

import android.annotation.TargetApi;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.support.v4.app.NotificationCompat;
import android.view.View;
import android.widget.RemoteViews;

public class TTSNotification {

    public static final String DEFAULT = "default";

    public static final String ACTION_TTS = "TTSNotification_TTS";

    public static final String TTS_PLAY = "TTS_PLAY";
    public static final String TTS_PAUSE = "TTS_PAUSE";
    public static final String TTS_PLAY_PAUSE = "TTS_PLAY_PAUSE";
    public static final String TTS_STOP_DESTROY = "TTS_STOP_DESTROY";
    public static final String TTS_NEXT = "TTS_NEXT";
    public static final String TTS_PREV = "TTS_PREV";

    private static final String KEY_TEXT_REPLY = "key_text_reply";

    public static final int NOT_ID = 123123;

    static String bookPath1;
    static int page1;
    static int pageCount;

    private static Context context;
    private static Handler handler;

    @TargetApi(26)
    public static void initChannels(Context context) {
        TTSNotification.context = context;
        handler = new Handler();

        if (Build.VERSION.SDK_INT < 26) {
            return;
        }
        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        NotificationChannel channel = new NotificationChannel(DEFAULT, AppsConfig.TXT_APP_NAME, NotificationManager.IMPORTANCE_DEFAULT);
        channel.setImportance(NotificationManager.IMPORTANCE_LOW);
        notificationManager.createNotificationChannel(channel);

    }

    public static void show(String bookPath, int page, int maxPages) {
        bookPath1 = bookPath;
        page1 = page;
        pageCount = maxPages;
        try {
            NotificationManager nm = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

            NotificationCompat.Builder builder = new NotificationCompat.Builder(context, DEFAULT);

            FileMeta fileMeta = AppDB.get().getOrCreate(bookPath);

            Intent intent = new Intent(context, HorizontalViewActivity.class.getSimpleName().equals(AppState.get().lastMode) ? HorizontalViewActivity.class : VerticalViewActivity.class);
            intent.setAction(ACTION_TTS);
            intent.setData(Uri.fromFile(new File(bookPath)));
            if (page > 0) {
                intent.putExtra("page", page - 1);
            }

            PendingIntent contentIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);

            PendingIntent playPause = PendingIntent.getService(context, 0, new Intent(TTS_PLAY_PAUSE, null, context, TTSService.class), PendingIntent.FLAG_UPDATE_CURRENT);
            PendingIntent pause = PendingIntent.getService(context, 0, new Intent(TTS_PAUSE, null, context, TTSService.class), PendingIntent.FLAG_UPDATE_CURRENT);
            PendingIntent play = PendingIntent.getService(context, 0, new Intent(TTS_PLAY, null, context, TTSService.class), PendingIntent.FLAG_UPDATE_CURRENT);
            PendingIntent next = PendingIntent.getService(context, 0, new Intent(TTS_NEXT, null, context, TTSService.class), PendingIntent.FLAG_UPDATE_CURRENT);
            PendingIntent prev = PendingIntent.getService(context, 0, new Intent(TTS_PREV, null, context, TTSService.class), PendingIntent.FLAG_UPDATE_CURRENT);
            PendingIntent stopDestroy = PendingIntent.getService(context, 0, new Intent(TTS_STOP_DESTROY, null, context, TTSService.class), PendingIntent.FLAG_UPDATE_CURRENT);

            RemoteViews remoteViews = new RemoteViews(context.getPackageName(), R.layout.notification_tts_line);

            Bitmap bookImage = getBookImage(bookPath);
            remoteViews.setImageViewBitmap(R.id.ttsIcon, bookImage);

            remoteViews.setOnClickPendingIntent(R.id.ttsPlay, playPause);
            remoteViews.setOnClickPendingIntent(R.id.ttsNext, next);
            remoteViews.setOnClickPendingIntent(R.id.ttsPrev, prev);
            remoteViews.setOnClickPendingIntent(R.id.ttsStop, stopDestroy);

            remoteViews.setViewVisibility(R.id.ttsDialog, View.GONE);

            if (TTSEngine.get().isPlaying()) {
                remoteViews.setImageViewResource(R.id.ttsPlay, R.drawable.glyphicons_175_pause);
            } else {
                remoteViews.setImageViewResource(R.id.ttsPlay, R.drawable.glyphicons_174_play);
            }

            remoteViews.setInt(R.id.ttsPlay, "setColorFilter", TintUtil.color);
            remoteViews.setInt(R.id.ttsNext, "setColorFilter", TintUtil.color);
            remoteViews.setInt(R.id.ttsPrev, "setColorFilter", TintUtil.color);
            remoteViews.setInt(R.id.ttsStop, "setColorFilter", TintUtil.color);

            String fileMetaBookName = TxtUtils.getFileMetaBookName(fileMeta);
            String pageNumber = context.getString(R.string.page) + " " + page + "/" + maxPages;

            if (page == -1 || maxPages == -1) {
                pageNumber = "";
            }

            String textLine = pageNumber + " " + fileMetaBookName;

            if (TxtUtils.isNotEmpty(AppState.get().mp3BookPath)) {
                textLine = "[" + ExtUtils.getFileName(AppState.get().mp3BookPath) + "] " + textLine;
            }

            remoteViews.setTextViewText(R.id.bookInfo, textLine.trim());
            remoteViews.setViewVisibility(R.id.bookInfo, View.VISIBLE);

            builder.setContentIntent(contentIntent) //
                    .setSmallIcon(R.drawable.glyphicons_185_volume_up) //
                    // .setLargeIcon(bookImage) //
                    // .setTicker(context.getString(R.string.app_name)) //
                    // .setWhen(System.currentTimeMillis()) //
                    .setOngoing(true)//
                    .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)//
                    // .addAction(R.drawable.glyphicons_175_pause,
                    // context.getString(R.string.to_paly_pause), playPause)//
                    // .addAction(R.drawable.glyphicons_174_play, context.getString(R.string.next),
                    // next)//
                    // .addAction(R.drawable.glyphicons_177_forward,
                    // context.getString(R.string.stop), stopDestroy)//
                    // .setContentTitle(fileMetaBookName) //
                    // .setContentText(pageNumber) //
                    // .setStyle(new NotificationCompat.DecoratedCustomViewStyle())//
                    // .addAction(action)//
                    .setCustomBigContentView(remoteViews) ///
                    .setCustomContentView(remoteViews); ///

            Notification n = builder.build(); //
            nm.notify(NOT_ID, n);
        } catch (Exception e) {
            LOG.e(e);
            return;
        }
    }

    public static void hideNotification() {
        try {
            NotificationManager nm = (NotificationManager) LibreraApp.context.getSystemService(Context.NOTIFICATION_SERVICE);
            nm.cancel(NOT_ID);
        } catch (Exception e) {
            LOG.e(e);
        }
    }

    public static void showLast() {
        if (TTSEngine.get().isShutdown()) {
            hideNotification();
        } else if (handler != null) {
            handler.postDelayed(run, 500);
        }

    }

    static Runnable run = new Runnable() {

        @Override
        public void run() {
            show(bookPath1, page1, pageCount);
        }
    };

    public static Bitmap getBookImage(String path) {
        String url = IMG.toUrl(path, ImageExtractor.COVER_PAGE_WITH_EFFECT, IMG.getImageSize());
        return ImageLoader.getInstance().loadImageSync(url, IMG.displayCacheMemoryDisc);
    }
}
