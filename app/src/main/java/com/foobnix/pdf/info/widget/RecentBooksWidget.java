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
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.widget.RemoteViews;

import com.foobnix.dao2.FileMeta;
import com.foobnix.model.AppData;
import com.foobnix.model.AppProfile;
import com.foobnix.model.AppState;
import com.foobnix.model.AppTemp;
import com.foobnix.pdf.info.IMG;
import com.foobnix.pdf.info.R;
import com.foobnix.pdf.search.activity.HorizontalViewActivity;
import com.foobnix.sys.ImageExtractor;
import com.foobnix.ui2.AppDB;
import com.foobnix.ui2.MainTabs2;
import com.nostra13.universalimageloader.core.ImageLoader;

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

        if (intent.getAction().equals(ACTION_MY)) {

            String className = VerticalViewActivity.class.getName();
            if (AppTemp.get().readingMode == AppState.READING_MODE_BOOK) {
                className = HorizontalViewActivity.class.getName();
            }

            Intent nintent = new Intent(Intent.ACTION_VIEW, (Uri) intent.getParcelableExtra("uri"));
            nintent.setClassName(context, className);
            PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, nintent, 0);
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

    private void easyMode(Context context, FileMeta meta, String id, boolean isEditMode) {
        Intent intent2 = new Intent(context, HorizontalViewActivity.class);
        intent2.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        intent2.setData(Uri.fromFile(new File(meta.getPath())));
        intent2.putExtra(id, true);
        intent2.putExtra("isEditMode", isEditMode);
        context.startActivity(intent2);
    }

    private void advMode(Context context, FileMeta meta, String id, boolean isEditMode) {
        Intent intent2 = new Intent(context, VerticalViewActivity.class);
        intent2.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        intent2.setData(Uri.fromFile(new File(meta.getPath())));
        intent2.putExtra(id, true);
        intent2.putExtra("isEditMode", isEditMode);
        context.startActivity(intent2);
    }

    @Override
    public synchronized void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        AppProfile.init(context);
        for (int widgetId : appWidgetIds) {
            RemoteViews remoteViews = null;
            if (Build.VERSION.SDK_INT >= 16 && AppState.get().widgetType == AppState.WIDGET_GRID) {
                remoteViews = new RemoteViews(context.getPackageName(), R.layout.recent_images_widget_grid);
                // remoteViews.setInt(R.id.gridView1, "setColumnWidth", 100);
                updateGrid(remoteViews);
            } else {
                remoteViews = new RemoteViews(context.getPackageName(), R.layout.recent_images_widget_list);
                updateList(remoteViews);
            }

            try {
                appWidgetManager.updateAppWidget(widgetId, remoteViews);
            } catch (Exception e) {
                AppState.get().widgetItemsCount = 1;

            }
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
    private void updateGrid(RemoteViews remoteViews) {
        Intent intent = new Intent(context, StackGridWidgetService.class);
        intent.setData(Uri.parse(intent.toUri(Intent.URI_INTENT_SCHEME)));
        remoteViews.setRemoteAdapter(R.id.gridView1, intent);

        Intent toastIntent = new Intent(context, RecentBooksWidget.class);
        toastIntent.setAction(ACTION_MY);
        intent.setData(Uri.parse(intent.toUri(Intent.URI_INTENT_SCHEME)));
        PendingIntent toastPendingIntent = PendingIntent.getBroadcast(context, 0, toastIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        remoteViews.setPendingIntentTemplate(R.id.gridView1, toastPendingIntent);
    }

    private void updateList(final RemoteViews remoteViews) {
        List<FileMeta> recent = null;
        if (AppState.get().isStarsInWidget) {
            recent = AppData.get().getAllFavoriteFiles();
        } else {
            recent = AppData.get().getAllRecent();
        }
        AppDB.removeClouds(recent);

        String className = VerticalViewActivity.class.getName();
        if (AppTemp.get().readingMode == AppState.READING_MODE_BOOK) {
            className = HorizontalViewActivity.class.getName();
        }
        remoteViews.removeAllViews(R.id.linearLayout);

        if (recent.size() == 0) {

            RemoteViews v = new RemoteViews(context.getPackageName(), R.layout.widget_list_image);
            v.setImageViewResource(R.id.imageView1, R.drawable.books_widget);

            Intent intent = new Intent(context, MainTabs2.class);
            PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent, 0);
            v.setOnClickPendingIntent(R.id.imageView1, pendingIntent);

            remoteViews.addView(R.id.linearLayout, v);

        } else {

            for (int i = 0; i < recent.size() && i < AppState.get().widgetItemsCount; i++) {
                FileMeta fileMeta = recent.get(i);

                RemoteViews v = new RemoteViews(context.getPackageName(), R.layout.widget_list_image);
                String url = IMG.toUrl(fileMeta.getPath(), ImageExtractor.COVER_PAGE_WITH_EFFECT, IMG.getImageSize());
                Bitmap image = ImageLoader.getInstance().loadImageSync(url, IMG.displayCacheMemoryDisc);
                v.setImageViewBitmap(R.id.imageView1, image);

                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.fromFile(new File(fileMeta.getPath())));
                intent.setClassName(context, className);
                PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent, 0);
                v.setOnClickPendingIntent(R.id.imageView1, pendingIntent);

                remoteViews.addView(R.id.linearLayout, v);
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
