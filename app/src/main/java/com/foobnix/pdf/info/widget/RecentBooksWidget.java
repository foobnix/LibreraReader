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
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.widget.RemoteViews;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;
import com.foobnix.android.utils.LOG;
import com.foobnix.dao2.FileMeta;
import com.foobnix.model.AppData;
import com.foobnix.model.AppProfile;
import com.foobnix.model.AppSP;
import com.foobnix.model.AppState;
import com.foobnix.pdf.info.IMG;
import com.foobnix.pdf.info.R;
import com.foobnix.pdf.search.activity.HorizontalViewActivity;
import com.foobnix.sys.ImageExtractor;
import com.foobnix.ui2.AppDB;
import com.foobnix.ui2.MainTabs2;

import org.ebookdroid.LibreraApp;
import org.ebookdroid.ui.viewer.VerticalViewActivity;

import java.io.File;
import java.util.List;

public class RecentBooksWidget extends AppWidgetProvider {

    public static final String TEST_LOCALE_POS = "TEST_LOCALE_POS";
    private static final String ACTION_MY = "my";
    private Context context;


    @SuppressLint("NewApi")
    @Override
    public void onReceive(Context context, Intent intent) {
        this.context = context;
        AppProfile.init(context);

        LOG.d("RecentBooksWidget", intent, intent.getData(), intent.getExtras());
        if (intent.getAction().equals(ACTION_MY)) {

            Class clazz = AppSP.get().readingMode == AppState.READING_MODE_BOOK ? HorizontalViewActivity.class : VerticalViewActivity.class;

            Intent nintent = new Intent(Intent.ACTION_VIEW, (Uri) intent.getParcelableExtra("uri"));
            nintent.setClassName(context, clazz.getName());
            PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, nintent, PendingIntent.FLAG_IMMUTABLE);
            try {
                pendingIntent.send();
            } catch (CanceledException e) {
            }

        }

        if (intent.getAction().equals("android.appwidget.action.APPWIDGET_UPDATE")) {
            int[] appWidgetIds = AppWidgetManager.getInstance(context).getAppWidgetIds(new ComponentName(context, RecentBooksWidget.class));
            if (Build.VERSION.SDK_INT >= 16 && AppState.get().widgetType == AppState.WIDGET_GRID) {
                AppWidgetManager.getInstance(context).notifyAppWidgetViewDataChanged(appWidgetIds, R.id.gridView1);
            }
            onUpdate(context, AppWidgetManager.getInstance(context), appWidgetIds);
        }

        super.onReceive(context, intent);
    }


    @Override
    public synchronized void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {

        try {
            for (int widgetId : appWidgetIds) {
                RemoteViews remoteViews = null;
                if (Build.VERSION.SDK_INT >= 16 && AppState.get().widgetType == AppState.WIDGET_GRID) {
                    remoteViews = new RemoteViews(context.getPackageName(), R.layout.recent_images_widget_grid);
                    // remoteViews.setInt(R.id.gridView1, "setColumnWidth", 100);
                    updateGrid(remoteViews, appWidgetManager, widgetId);


                    appWidgetManager.updateAppWidget(widgetId, remoteViews);
                } else {
                    remoteViews = new RemoteViews(context.getPackageName(), R.layout.recent_images_widget_list);
                    updateList(remoteViews, appWidgetManager, widgetId);
                }

            }
        } catch (Exception e) {
            LOG.e(e);
        }
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    @Override
    public void onAppWidgetOptionsChanged(Context context, AppWidgetManager appWidgetManager, int appWidgetId, Bundle newOptions) {
        Intent intent = new Intent(context, RecentBooksWidget.class);
        intent.setAction("android.appwidget.action.APPWIDGET_UPDATE");
        context.sendBroadcast(intent);
    }

    @SuppressLint("NewApi")
    private void updateGrid(RemoteViews remoteViews, AppWidgetManager appWidgetManager, int appWidgetId) {
        Intent intent = new Intent(context, StackGridWidgetService.class);
        remoteViews.setRemoteAdapter(R.id.gridView1, intent);

        Intent toastIntent = new Intent(context, RecentBooksWidget.class);
        toastIntent.setAction(ACTION_MY);
        PendingIntent toastPendingIntent = PendingIntent.getBroadcast(context, 0, toastIntent, PendingIntent.FLAG_MUTABLE);
        remoteViews.setPendingIntentTemplate(R.id.gridView1, toastPendingIntent);
    }

    private void updateList(final RemoteViews remoteViews, AppWidgetManager appWidgetManager, int appWidgetId) {
        List<FileMeta> recent = null;
        if (AppState.get().isStarsInWidget) {
            recent = AppData.get().getAllFavoriteFiles(false);
        } else {
            recent = AppData.get().getAllRecent(false);
        }
        AppDB.removeClouds(recent);

        String className = VerticalViewActivity.class.getName();
        if (AppSP.get().readingMode == AppState.READING_MODE_BOOK) {
            className = HorizontalViewActivity.class.getName();
        }
        remoteViews.removeAllViews(R.id.linearLayout);

        if (recent.size() == 0) {

            RemoteViews v = new RemoteViews(context.getPackageName(), R.layout.widget_list_image);
            v.setImageViewResource(R.id.imageView1, R.drawable.books_widget);

            Intent intent = new Intent(context, MainTabs2.class);
            PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_IMMUTABLE);
            v.setOnClickPendingIntent(R.id.imageView1, pendingIntent);

            remoteViews.addView(R.id.linearLayout, v);

        } else {

            for (int i = 0; i < recent.size() && i < AppState.get().widgetItemsCount; i++) {
                FileMeta fileMeta = recent.get(i);

                RemoteViews v = new RemoteViews(context.getPackageName(), R.layout.widget_list_image);
                String url = IMG.toUrl(fileMeta.getPath(), ImageExtractor.COVER_PAGE_WITH_EFFECT, IMG.getImageSize());
                Glide.with(LibreraApp.context).asBitmap().load(url).into(new CustomTarget<Bitmap>() {


                    @Override
                    public void onResourceReady(@NonNull Bitmap resource, @Nullable Transition<? super Bitmap> transition) {
                        try {
                            RemoteViews v = new RemoteViews(context.getPackageName(), R.layout.widget_list_image);
                            v.setImageViewBitmap(R.id.imageView1, resource);

                            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.fromFile(new File(fileMeta.getPath())));

                            Class clazz = AppSP.get().readingMode == AppState.READING_MODE_BOOK ? HorizontalViewActivity.class: VerticalViewActivity.class;

                            intent.setClassName(context, clazz.getName());

                            PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_IMMUTABLE);
                            v.setOnClickPendingIntent(R.id.imageView1, pendingIntent);

                            remoteViews.addView(R.id.linearLayout, v);

                            //for (int widgetId : appWidgetIds) {
                            if (appWidgetManager != null) {
                                appWidgetManager.updateAppWidget(appWidgetId, remoteViews);
                            }
                            //}

                        } catch (Exception e) {
                            LOG.e(e);
                        }
                    }

                    @Override
                    public void onLoadCleared(Drawable placeholder) {

                    }

                });
            }
        }

    }

    public Bitmap scaleCenterCrop(Bitmap source, int newHeight, int newWidth) {
        int sourceWidth = source.getWidth();
        int sourceHeight = source.getHeight();

        // Compute the scaling factors to fit the new height and width,
        // respectively.
        // To cover the final image, the final scaling will be the bigger
        // of these two.
        float xScale = (float) newWidth / sourceWidth;
        float yScale = (float) newHeight / sourceHeight;
        float scale = Math.max(xScale, yScale);

        // Now get the size of the source bitmap when scaled
        float scaledWidth = scale * sourceWidth;
        float scaledHeight = scale * sourceHeight;

        // Let's find out the upper left coordinates if the scaled bitmap
        // should be centered in the new size give by the parameters
        float left = (newWidth - scaledWidth) / 2;
        float top = (newHeight - scaledHeight) / 2;

        // The target rectangle for the new, scaled version of the source bitmap
        // will now
        // be
        RectF targetRect = new RectF(left, top, left + scaledWidth, top + scaledHeight);

        // Finally, we create a new bitmap of the specified size and draw our
        // new,
        // scaled bitmap onto it.
        Bitmap dest = Bitmap.createBitmap(newWidth, newHeight, source.getConfig());
        Canvas canvas = new Canvas(dest);
        canvas.drawBitmap(source, null, targetRect, null);

        return dest;
    }
}
