package com.foobnix.tts;

import java.io.File;

import org.ebookdroid.LibreraApp;
import org.ebookdroid.ui.viewer.VerticalViewActivity;

import com.foobnix.android.utils.LOG;
import com.foobnix.android.utils.TxtUtils;
import com.foobnix.dao2.FileMeta;
import com.foobnix.pdf.info.AppsConfig;
import com.foobnix.pdf.info.IMG;
import com.foobnix.pdf.info.R;
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
import android.support.v4.app.NotificationCompat;

public class TTSNotification {

    public static final String DEFAULT = "default";

    public static final String ACTION_TTS = "TTSNotification_TTS";

    public static final String TTS_PLAY = "TTS_PLAY";
    public static final String TTS_PAUSE = "TTS_PAUSE";
    public static final String TTS_PLAY_PAUSE = "TTS_PLAY_PAUSE";
    public static final String TTS_STOP_DESTROY = "TTS_STOP_DESTROY";
    public static final String TTS_NEXT = "TTS_NEXT";
    public static final String TTS_PREV = "TTS_PREV";

    public static final int NOT_ID = 123123;

    static String bookPath1;
    static int page1;

    private static Context context;

    @TargetApi(26)
    public static void initChannels(Context context) {
        TTSNotification.context = context;
        if (Build.VERSION.SDK_INT < 26) {
            return;
        }
        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        NotificationChannel channel = new NotificationChannel(DEFAULT, AppsConfig.TXT_APP_NAME, NotificationManager.IMPORTANCE_DEFAULT);
        channel.setImportance(NotificationManager.IMPORTANCE_LOW);
        notificationManager.createNotificationChannel(channel);
    }

    public static void show(String bookPath, int page) {
        bookPath1 = bookPath;
        page1 = page;
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

            PendingIntent palyPause = PendingIntent.getService(context, 0, new Intent(TTS_PLAY_PAUSE, null, context, TTSService.class), PendingIntent.FLAG_UPDATE_CURRENT);
            PendingIntent next = PendingIntent.getService(context, 0, new Intent(TTS_NEXT, null, context, TTSService.class), PendingIntent.FLAG_UPDATE_CURRENT);
            PendingIntent stopDestroy = PendingIntent.getService(context, 0, new Intent(TTS_STOP_DESTROY, null, context, TTSService.class), PendingIntent.FLAG_UPDATE_CURRENT);

            builder.setContentIntent(contentIntent) //
                    .setSmallIcon(R.drawable.glyphicons_185_volume_up) //
                    .setLargeIcon(getBookImage(bookPath)) //
                    .setTicker(context.getString(R.string.app_name)) //
                    .setWhen(System.currentTimeMillis()) //
                    .setOngoing(true)//
                    .addAction(R.drawable.glyphicons_175_pause, context.getString(R.string.to_paly_pause), palyPause)//
                    .addAction(R.drawable.glyphicons_174_play, context.getString(R.string.next), next)//
                    .addAction(R.drawable.glyphicons_177_forward, context.getString(R.string.stop), stopDestroy)//
                    .setContentTitle(TxtUtils.getFileMetaBookName(fileMeta)) //
                    .setContentText(context.getString(R.string.page) + " " + page); ///

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
        show(bookPath1, page1);
    }

    public static Bitmap getBookImage(String path) {
        String url = IMG.toUrl(path, ImageExtractor.COVER_PAGE_WITH_EFFECT, IMG.getImageSize());
        return ImageLoader.getInstance().loadImageSync(url, IMG.displayCacheMemoryDisc);
    }
}
