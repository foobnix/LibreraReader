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
import android.view.View;
import android.widget.RemoteViews;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;
import com.foobnix.LibreraApp;
import com.foobnix.android.utils.Apps;
import com.foobnix.android.utils.LOG;
import com.foobnix.android.utils.ResultResponse;
import com.foobnix.android.utils.TxtUtils;
import com.foobnix.dao2.FileMeta;
import com.foobnix.model.AppSP;
import com.foobnix.model.AppState;
import com.foobnix.pdf.info.ExtUtils;
import com.foobnix.pdf.info.IMG;
import com.foobnix.pdf.info.R;
import com.foobnix.pdf.info.model.BookCSS;
import com.foobnix.pdf.info.widget.RecentBooksWidget;
import com.foobnix.pdf.info.widget.TTSWidget;
import com.foobnix.pdf.search.activity.HorizontalViewActivity;
import com.foobnix.sys.ImageExtractor;
import com.foobnix.ui2.AppDB;

import org.ebookdroid.ui.viewer.VerticalViewActivity;

import java.io.File;

public class TTSNotification {

    public static final String DEFAULT = "default";

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
        handler = new Handler();

        if (Build.VERSION.SDK_INT < 26) {
            return;
        }
        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        NotificationChannel channel = new NotificationChannel(DEFAULT, Apps.getApplicationName(context), NotificationManager.IMPORTANCE_LOW);
        //channel.setImportance(NotificationManager.IMPORTANCE_HIGH);
        channel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);

        //channel.setShowBadge(false);
        //channel.setSound(null,null);


        notificationManager.createNotificationChannel(channel);

    }

    public static void show(String bookPath, int page, int maxPages) {
        bookPath1 = bookPath;
        page1 = page;
        pageCount = maxPages;
        try {
            NotificationManager nm = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

            NotificationCompat.Builder builder = createNotificationBuilder(context, bookPath, page, maxPages);

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

            RemoteViews remoteViews = new RemoteViews(context.getPackageName(), R.layout.notification_tts_line);
            RemoteViews remoteViewsSmall = new RemoteViews(context.getPackageName(), R.layout.notification_tts_line_small);


            //remoteViews.setImageViewBitmap(R.id.ttsIcon, bookImage);
            remoteViews.setOnClickPendingIntent(R.id.ttsPlay, playPause);
            remoteViews.setOnClickPendingIntent(R.id.ttsNext, next);
            remoteViews.setOnClickPendingIntent(R.id.ttsPrev, prev);
            remoteViews.setOnClickPendingIntent(R.id.ttsStop, stopDestroy);


            //remoteViewsSmall.setImageViewBitmap(R.id.ttsIcon, bookImage);
            remoteViewsSmall.setOnClickPendingIntent(R.id.ttsPlay, playPause);
            remoteViewsSmall.setOnClickPendingIntent(R.id.ttsNext, next);
            remoteViewsSmall.setOnClickPendingIntent(R.id.ttsPrev, prev);
            remoteViewsSmall.setOnClickPendingIntent(R.id.ttsStop, stopDestroy);


            remoteViews.setViewVisibility(R.id.ttsNextTrack, View.GONE);
            remoteViews.setViewVisibility(R.id.ttsPrevTrack, View.GONE);

            remoteViewsSmall.setViewVisibility(R.id.ttsNextTrack, View.GONE);
            remoteViewsSmall.setViewVisibility(R.id.ttsPrevTrack, View.GONE);


            remoteViews.setViewVisibility(R.id.ttsDialog, View.GONE);
            remoteViewsSmall.setViewVisibility(R.id.ttsDialog, View.GONE);

            if (TTSEngine.get().isPlaying()) {
                remoteViews.setImageViewResource(R.id.ttsPlay, R.drawable.glyphicons_174_pause);
                remoteViewsSmall.setImageViewResource(R.id.ttsPlay, R.drawable.glyphicons_174_pause);
            } else {
                remoteViews.setImageViewResource(R.id.ttsPlay, R.drawable.glyphicons_175_play);
                remoteViewsSmall.setImageViewResource(R.id.ttsPlay, R.drawable.glyphicons_175_play);
            }

            final int color = AppState.get().isUiTextColor ? AppState.get().uiTextColor : AppState.get().tintColor;


            remoteViews.setInt(R.id.ttsPlay, "setColorFilter", color);
            remoteViews.setInt(R.id.ttsNext, "setColorFilter", color);
            remoteViews.setInt(R.id.ttsPrev, "setColorFilter", color);
            remoteViews.setInt(R.id.ttsStop, "setColorFilter", color);

            remoteViewsSmall.setInt(R.id.ttsPlay, "setColorFilter", color);
            remoteViewsSmall.setInt(R.id.ttsNext, "setColorFilter", color);
            remoteViewsSmall.setInt(R.id.ttsPrev, "setColorFilter", color);
            remoteViewsSmall.setInt(R.id.ttsStop, "setColorFilter", color);

            String fileMetaBookName = TxtUtils.getFileMetaBookName(fileMeta);

            String pageNumber = "(" + TxtUtils.getProgressPercent(page, maxPages) + " " + page + "/" + maxPages + ")";

            if (page == -1 || maxPages == -1) {
                pageNumber = "";
            }

            String textLine = pageNumber + " " + fileMetaBookName;

            if (TxtUtils.isNotEmpty(BookCSS.get().mp3BookPathGet())) {
                textLine = "[" + ExtUtils.getFileName(BookCSS.get().mp3BookPathGet()) + "] " + textLine;
            }

            remoteViews.setTextViewText(R.id.bookInfo, textLine.replace(TxtUtils.LONG_DASH1 + " ", "\n").trim());
            //remoteViews.setViewVisibility(R.id.bookInfo, View.VISIBLE);

            remoteViewsSmall.setTextViewText(R.id.bookInfo, textLine.trim());
            //remoteViewsSmall.setViewVisibility(R.id.bookInfo, View.VISIBLE);
            final String extraText = textLine;

            String url = IMG.toUrl(bookPath, ImageExtractor.COVER_PAGE_NO_EFFECT, IMG.getImageSize());

            Glide.with(LibreraApp.context).asBitmap().load(url).into(new CustomTarget<Bitmap>() {
                @Override
                public void onResourceReady(@NonNull Bitmap resource, @Nullable Transition<? super Bitmap> transition) {
                    remoteViews.setImageViewBitmap(R.id.ttsIcon, resource);
                    remoteViewsSmall.setImageViewBitmap(R.id.ttsIcon, resource);

                    builder.setContentIntent(contentIntent) //
                            .setSmallIcon(R.drawable.glyphicons_smileys_100_headphones) //
                            .setColor(color)
                            .setOngoing(true)//
                            .setPriority(NotificationCompat.PRIORITY_HIGH) //
                            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)//
                            .setStyle(new androidx.media.app.NotificationCompat.MediaStyle())
                            .setSilent(true)
                            .setCustomBigContentView(remoteViews) ///
                            .setCustomContentView(remoteViewsSmall); ///
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
            handler.postDelayed(run, 500);
        }

    }

    public static void updateNotification(String bookPath, int page, int maxPages) {
        bookPath1 = bookPath;
        page1 = page;
        pageCount = maxPages;
        try {
            NotificationManager nm = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

            NotificationCompat.Builder builder = createNotificationBuilder(context, bookPath, page, maxPages);

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

            RemoteViews remoteViews = new RemoteViews(context.getPackageName(), R.layout.notification_tts_line);
            RemoteViews remoteViewsSmall = new RemoteViews(context.getPackageName(), R.layout.notification_tts_line_small);


            //remoteViews.setImageViewBitmap(R.id.ttsIcon, bookImage);
            remoteViews.setOnClickPendingIntent(R.id.ttsPlay, playPause);
            remoteViews.setOnClickPendingIntent(R.id.ttsNext, next);
            remoteViews.setOnClickPendingIntent(R.id.ttsPrev, prev);
            remoteViews.setOnClickPendingIntent(R.id.ttsStop, stopDestroy);


            //remoteViewsSmall.setImageViewBitmap(R.id.ttsIcon, bookImage);
            remoteViewsSmall.setOnClickPendingIntent(R.id.ttsPlay, playPause);
            remoteViewsSmall.setOnClickPendingIntent(R.id.ttsNext, next);
            remoteViewsSmall.setOnClickPendingIntent(R.id.ttsPrev, prev);
            remoteViewsSmall.setOnClickPendingIntent(R.id.ttsStop, stopDestroy);


            remoteViews.setViewVisibility(R.id.ttsNextTrack, View.GONE);
            remoteViews.setViewVisibility(R.id.ttsPrevTrack, View.GONE);

            remoteViewsSmall.setViewVisibility(R.id.ttsNextTrack, View.GONE);
            remoteViewsSmall.setViewVisibility(R.id.ttsPrevTrack, View.GONE);


            remoteViews.setViewVisibility(R.id.ttsDialog, View.GONE);
            remoteViewsSmall.setViewVisibility(R.id.ttsDialog, View.GONE);

            if (TTSEngine.get().isPlaying()) {
                remoteViews.setImageViewResource(R.id.ttsPlay, R.drawable.glyphicons_174_pause);
                remoteViewsSmall.setImageViewResource(R.id.ttsPlay, R.drawable.glyphicons_174_pause);
            } else {
                remoteViews.setImageViewResource(R.id.ttsPlay, R.drawable.glyphicons_175_play);
                remoteViewsSmall.setImageViewResource(R.id.ttsPlay, R.drawable.glyphicons_175_play);
            }

            final int color = AppState.get().isUiTextColor ? AppState.get().uiTextColor : AppState.get().tintColor;


            remoteViews.setInt(R.id.ttsPlay, "setColorFilter", color);
            remoteViews.setInt(R.id.ttsNext, "setColorFilter", color);
            remoteViews.setInt(R.id.ttsPrev, "setColorFilter", color);
            remoteViews.setInt(R.id.ttsStop, "setColorFilter", color);

            remoteViewsSmall.setInt(R.id.ttsPlay, "setColorFilter", color);
            remoteViewsSmall.setInt(R.id.ttsNext, "setColorFilter", color);
            remoteViewsSmall.setInt(R.id.ttsPrev, "setColorFilter", color);
            remoteViewsSmall.setInt(R.id.ttsStop, "setColorFilter", color);

            String fileMetaBookName = TxtUtils.getFileMetaBookName(fileMeta);

            String pageNumber = "(" + TxtUtils.getProgressPercent(page, maxPages) + " " + page + "/" + maxPages + ")";

            if (page == -1 || maxPages == -1) {
                pageNumber = "";
            }

            String textLine = pageNumber + " " + fileMetaBookName;

            if (TxtUtils.isNotEmpty(BookCSS.get().mp3BookPathGet())) {
                textLine = "[" + ExtUtils.getFileName(BookCSS.get().mp3BookPathGet()) + "] " + textLine;
            }

            remoteViews.setTextViewText(R.id.bookInfo, textLine.replace(TxtUtils.LONG_DASH1 + " ", "\n").trim());
            //remoteViews.setViewVisibility(R.id.bookInfo, View.VISIBLE);

            remoteViewsSmall.setTextViewText(R.id.bookInfo, textLine.trim());
            //remoteViewsSmall.setViewVisibility(R.id.bookInfo, View.VISIBLE);
            final String extraText = textLine;

            String url = IMG.toUrl(bookPath, ImageExtractor.COVER_PAGE_NO_EFFECT, IMG.getImageSize());

            Glide.with(LibreraApp.context).asBitmap().load(url).into(new CustomTarget<Bitmap>() {
                @Override
                public void onResourceReady(@NonNull Bitmap resource, @Nullable Transition<? super Bitmap> transition) {
                    remoteViews.setImageViewBitmap(R.id.ttsIcon, resource);
                    remoteViewsSmall.setImageViewBitmap(R.id.ttsIcon, resource);

                    builder.setContentIntent(contentIntent) //
                            .setSmallIcon(R.drawable.glyphicons_smileys_100_headphones) //
                            .setColor(color)
                            .setOngoing(true)//
                            .setPriority(NotificationCompat.PRIORITY_HIGH) //
                            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)//
                            .setStyle(new androidx.media.app.NotificationCompat.MediaStyle())
                            .setSilent(true)
                            .setCustomBigContentView(remoteViews) ///
                            .setCustomContentView(remoteViewsSmall); ///
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

    private static NotificationCompat.Builder createNotificationBuilder(Context context, String bookPath, int page, int maxPages) {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, DEFAULT);

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

        RemoteViews remoteViews = new RemoteViews(context.getPackageName(), R.layout.notification_tts_line);
        RemoteViews remoteViewsSmall = new RemoteViews(context.getPackageName(), R.layout.notification_tts_line_small);


        //remoteViews.setImageViewBitmap(R.id.ttsIcon, bookImage);
        remoteViews.setOnClickPendingIntent(R.id.ttsPlay, playPause);
        remoteViews.setOnClickPendingIntent(R.id.ttsNext, next);
        remoteViews.setOnClickPendingIntent(R.id.ttsPrev, prev);
        remoteViews.setOnClickPendingIntent(R.id.ttsStop, stopDestroy);


        //remoteViewsSmall.setImageViewBitmap(R.id.ttsIcon, bookImage);
        remoteViewsSmall.setOnClickPendingIntent(R.id.ttsPlay, playPause);
        remoteViewsSmall.setOnClickPendingIntent(R.id.ttsNext, next);
        remoteViewsSmall.setOnClickPendingIntent(R.id.ttsPrev, prev);
        remoteViewsSmall.setOnClickPendingIntent(R.id.ttsStop, stopDestroy);


        remoteViews.setViewVisibility(R.id.ttsNextTrack, View.GONE);
        remoteViews.setViewVisibility(R.id.ttsPrevTrack, View.GONE);

        remoteViewsSmall.setViewVisibility(R.id.ttsNextTrack, View.GONE);
        remoteViewsSmall.setViewVisibility(R.id.ttsPrevTrack, View.GONE);


        remoteViews.setViewVisibility(R.id.ttsDialog, View.GONE);
        remoteViewsSmall.setViewVisibility(R.id.ttsDialog, View.GONE);

        if (TTSEngine.get().isPlaying()) {
            remoteViews.setImageViewResource(R.id.ttsPlay, R.drawable.glyphicons_174_pause);
            remoteViewsSmall.setImageViewResource(R.id.ttsPlay, R.drawable.glyphicons_174_pause);
        } else {
            remoteViews.setImageViewResource(R.id.ttsPlay, R.drawable.glyphicons_175_play);
            remoteViewsSmall.setImageViewResource(R.id.ttsPlay, R.drawable.glyphicons_175_play);
        }

        final int color = AppState.get().isUiTextColor ? AppState.get().uiTextColor : AppState.get().tintColor;


        remoteViews.setInt(R.id.ttsPlay, "setColorFilter", color);
        remoteViews.setInt(R.id.ttsNext, "setColorFilter", color);
        remoteViews.setInt(R.id.ttsPrev, "setColorFilter", color);
        remoteViews.setInt(R.id.ttsStop, "setColorFilter", color);

        remoteViewsSmall.setInt(R.id.ttsPlay, "setColorFilter", color);
        remoteViewsSmall.setInt(R.id.ttsNext, "setColorFilter", color);
        remoteViewsSmall.setInt(R.id.ttsPrev, "setColorFilter", color);
        remoteViewsSmall.setInt(R.id.ttsStop, "setColorFilter", color);

        String fileMetaBookName = TxtUtils.getFileMetaBookName(fileMeta);

        String pageNumber = "(" + TxtUtils.getProgressPercent(page, maxPages) + " " + page + "/" + maxPages + ")";

        if (page == -1 || maxPages == -1) {
            pageNumber = "";
        }

        String textLine = pageNumber + " " + fileMetaBookName;

        if (TxtUtils.isNotEmpty(BookCSS.get().mp3BookPathGet())) {
            textLine = "[" + ExtUtils.getFileName(BookCSS.get().mp3BookPathGet()) + "] " + textLine;
        }

        remoteViews.setTextViewText(R.id.bookInfo, textLine.replace(TxtUtils.LONG_DASH1 + " ", "\n").trim());
        //remoteViews.setViewVisibility(R.id.bookInfo, View.VISIBLE);

        remoteViewsSmall.setTextViewText(R.id.bookInfo, textLine.trim());
        //remoteViewsSmall.setViewVisibility(R.id.bookInfo, View.VISIBLE);
        final String extraText = textLine;

        String url = IMG.toUrl(bookPath, ImageExtractor.COVER_PAGE_NO_EFFECT, IMG.getImageSize());

        Glide.with(LibreraApp.context).asBitmap().load(url).into(new CustomTarget<Bitmap>() {
            @Override
            public void onResourceReady(@NonNull Bitmap resource, @Nullable Transition<? super Bitmap> transition) {
                remoteViews.setImageViewBitmap(R.id.ttsIcon, resource);
                remoteViewsSmall.setImageViewBitmap(R.id.ttsIcon, resource);

                builder.setContentIntent(contentIntent) //
                        .setSmallIcon(R.drawable.glyphicons_smileys_100_headphones) //
                        .setColor(color)
                        .setOngoing(true)//
                        .setPriority(NotificationCompat.PRIORITY_HIGH) //
                        .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)//
                        .setStyle(new androidx.media.app.NotificationCompat.MediaStyle())
                        .setSilent(true)
                        .setCustomBigContentView(remoteViews) ///
                        .setCustomContentView(remoteViewsSmall); ///
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

        return builder;
    }

}
