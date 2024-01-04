package com.foobnix.pdf.info.widget;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.PendingIntent;
import android.app.PendingIntent.CanceledException;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.RemoteViews;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;
import com.foobnix.LibreraApp;
import com.foobnix.android.utils.LOG;
import com.foobnix.android.utils.TxtUtils;
import com.foobnix.dao2.FileMeta;
import com.foobnix.model.AppData;
import com.foobnix.model.AppProfile;
import com.foobnix.model.AppSP;
import com.foobnix.model.AppState;
import com.foobnix.pdf.info.IMG;
import com.foobnix.pdf.info.R;
import com.foobnix.pdf.search.activity.HorizontalViewActivity;
import com.foobnix.sys.ImageExtractor;
import com.foobnix.tts.TTSEngine;
import com.foobnix.tts.TTSNotification;
import com.foobnix.tts.TTSService;
import com.foobnix.ui2.AppDB;
import com.foobnix.ui2.MainTabs2;

import org.ebookdroid.ui.viewer.VerticalViewActivity;

import java.io.File;
import java.util.List;
import java.util.concurrent.ExecutionException;

public class TTSWidget extends AppWidgetProvider {


    String text = "";

    boolean isPlaying = false;

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        super.onUpdate(context, appWidgetManager, appWidgetIds);

        for (int i = 0; i < appWidgetIds.length; i++) {
            int appWidgetId = appWidgetIds[i];

            RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.notification_tts_line);

            views.setInt(R.id.rootView, "setBackgroundColor", Color.argb(100,255,255,255));
            views.setViewPadding(R.id.rootView,0,0,0,0);
            views.setViewVisibility(R.id.ttsDialog, View.GONE);
            views.setViewVisibility(R.id.ttsPrevTrack, View.GONE);
            views.setViewVisibility(R.id.ttsNextTrack, View.GONE);
            views.setViewVisibility(R.id.ttsStop, View.GONE);
            //views.setViewVisibility(R.id.ttsPrev, View.GONE);

            PendingIntent next = PendingIntent.getService(context, 0, new Intent(TTSNotification.TTS_NEXT, null, context, TTSService.class), PendingIntent.FLAG_IMMUTABLE);
            PendingIntent prev = PendingIntent.getService(context, 0, new Intent(TTSNotification.TTS_PREV, null, context, TTSService.class), PendingIntent.FLAG_IMMUTABLE);
            PendingIntent playPause = PendingIntent.getService(context, 0, new Intent(TTSNotification.TTS_PLAY_PAUSE, null, context, TTSService.class), PendingIntent.FLAG_IMMUTABLE);


            views.setOnClickPendingIntent(R.id.ttsPlay, playPause);
            views.setOnClickPendingIntent(R.id.ttsPrev, prev);
            views.setOnClickPendingIntent(R.id.ttsNext, next);
            views.setTextViewText(R.id.bookInfo, text);
            //views.setViewLayoutMargin(R.id.ttsPrev,RemoteViews.MARGIN_LEFT,0.0f,0);


            if (isPlaying) {
                views.setImageViewResource(R.id.ttsPlay, R.drawable.glyphicons_174_pause);
            } else {
                views.setImageViewResource(R.id.ttsPlay, R.drawable.glyphicons_175_play);
            }
            if (TTSNotification.resourceBitmap != null) {
                views.setImageViewBitmap(R.id.ttsIcon, TTSNotification.resourceBitmap);
            }

            Intent mainTabs = new Intent(context, MainTabs2.class);
            PendingIntent mainTabsIntent = PendingIntent.getActivity(
                    context,
                    0,
                    mainTabs,
                    PendingIntent.FLAG_IMMUTABLE
            );
            views.setOnClickPendingIntent(R.id.ttsIcon, mainTabsIntent);


            final int color = AppState.get().isUiTextColor ? AppState.get().uiTextColor : AppState.get().tintColor;


            views.setInt(R.id.ttsPlay, "setColorFilter", color);
            views.setInt(R.id.ttsNext, "setColorFilter", color);
            views.setInt(R.id.ttsPrev, "setColorFilter", color);
            views.setInt(R.id.ttsStop, "setColorFilter", color);


            // Tell the AppWidgetManager to perform an update on the current app
            // widget.
            appWidgetManager.updateAppWidget(appWidgetId, views);
        }
    }

    @Override
    public void onReceive(Context context, Intent intent) {

        int[] appWidgetIds = AppWidgetManager.getInstance(context).getAppWidgetIds(new ComponentName(context, TTSWidget.class));
        if (intent != null && intent.getExtras() != null) {

            String textExtra = intent.getExtras().getString(Intent.EXTRA_TEXT);
            if (textExtra != null) {
                text = textExtra;
                text = text.replace(")", ")\n");
                text = text.replace("(", "(");
            } else {
                FileMeta recentLast = AppDB.get().getRecentLastNoFolder();
                if (recentLast != null && TxtUtils.isNotEmpty(recentLast.getTitle())) {
                    File bookFile = new File(recentLast.getPath());
                    if (!bookFile.isFile()) {
                        LOG.d("Book not found", bookFile.getPath());

                    }
                    text = recentLast.getTitle();

                    String url = IMG.toUrl(recentLast.getPath(), ImageExtractor.COVER_PAGE, IMG.getImageSize());
                    Glide.with(context).asBitmap().load(url).into(new CustomTarget<Bitmap>() {
                        @Override
                        public void onResourceReady(@NonNull Bitmap image, @Nullable Transition<? super Bitmap> transition) {
                            TTSNotification.resourceBitmap = image;
                            onUpdate(context, AppWidgetManager.getInstance(context), appWidgetIds);
                        }

                        @Override
                        public void onLoadCleared(@Nullable Drawable placeholder) {

                        }
                    });

                }
            }

            isPlaying = intent.getExtras().getBoolean("isPlaying");
            LOG.d("EXTRA_TEXT", text);
        }
        onUpdate(context, AppWidgetManager.getInstance(context), appWidgetIds);
        super.onReceive(context, intent);
    }
}
