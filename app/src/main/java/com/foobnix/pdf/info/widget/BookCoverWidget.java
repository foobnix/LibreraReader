package com.foobnix.pdf.info.widget;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.widget.RemoteViews;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;
import com.foobnix.android.utils.LOG;
import com.foobnix.librerax.LibreraX;
import com.foobnix.pdf.info.IMG;
import com.foobnix.pdf.info.R;
import com.foobnix.ui2.MainTabs2;

import java.io.File;

/**
 * One book on the home screen: only its cover, with rounded corners, and a tap opens the book
 * in the reader. The book each widget shows is kept by widget id.
 */
public class BookCoverWidget extends AppWidgetProvider {

    private static final String ACTION_PINNED = "com.foobnix.pdf.info.widget.BOOK_COVER_PINNED";
    private static final String EXTRA_PATH = "path";
    private static final String PREFS = "BookCoverWidget";
    private static final int MAX_SIZE = 480;

    public static boolean pin(Context context, final File file) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return false;
        }
        final Context c = context.getApplicationContext();
        final AppWidgetManager manager = AppWidgetManager.getInstance(c);
        if (!manager.isRequestPinAppWidgetSupported()) {
            return false;
        }
        IMG.getCoverPageWithEffect(c, file.getPath(), null).into(new CustomTarget<Bitmap>() {

            @Override public void onResourceReady(@NonNull Bitmap cover, @Nullable Transition<? super Bitmap> transition) {
                // the launcher's "add to home screen" card shows the cover rather than the app icon
                RemoteViews preview = new RemoteViews(c.getPackageName(), R.layout.widget_book_cover);
                preview.setImageViewBitmap(R.id.cover, rounded(cover));
                Bundle extras = new Bundle();
                extras.putParcelable(AppWidgetManager.EXTRA_APPWIDGET_PREVIEW, preview);
                request(c, manager, file, extras);
            }

            @Override public void onLoadFailed(@Nullable Drawable errorDrawable) {
                request(c, manager, file, null);
            }

            @Override public void onLoadCleared(@Nullable Drawable placeholder) {
            }
        });
        return true;
    }

    private static void request(Context c, AppWidgetManager manager, File file, Bundle extras) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return;
        }
        Intent pinned = new Intent(c, BookCoverWidget.class);
        pinned.setAction(ACTION_PINNED);
        pinned.putExtra(EXTRA_PATH, file.getPath());
        // mutable: the launcher adds the id of the new widget to it
        PendingIntent callback = PendingIntent.getBroadcast(c, file.getPath().hashCode(), pinned,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_MUTABLE);
        manager.requestPinAppWidget(new ComponentName(c, BookCoverWidget.class), extras, callback);
    }

    @Override public void onReceive(Context context, Intent intent) {
        if (ACTION_PINNED.equals(intent.getAction())) {
            int id = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID);
            String path = intent.getStringExtra(EXTRA_PATH);
            if (id != AppWidgetManager.INVALID_APPWIDGET_ID && path != null) {
                prefs(context).edit().putString("" + id, path).apply();
                update(context, AppWidgetManager.getInstance(context), id);
            }
            return;
        }
        super.onReceive(context, intent);
    }

    @Override public void onUpdate(Context context, AppWidgetManager manager, int[] ids) {
        for (int id : ids) {
            update(context, manager, id);
        }
    }

    @Override public void onDeleted(Context context, int[] ids) {
        SharedPreferences.Editor edit = prefs(context).edit();
        for (int id : ids) {
            edit.remove("" + id);
        }
        edit.apply();
    }

    private static SharedPreferences prefs(Context c) {
        return c.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    private static void update(Context context, final AppWidgetManager manager, final int id) {
        final Context c = context.getApplicationContext();
        final String path = prefs(c).getString("" + id, null);
        if (path == null) {
            // added from the widget list rather than from a book: it opens the library
            RemoteViews views = new RemoteViews(c.getPackageName(), R.layout.widget_book_cover);
            Intent intent = new Intent(c, MainTabs2.class);
            views.setOnClickPendingIntent(R.id.cover, PendingIntent.getActivity(c, id, intent, PendingIntent.FLAG_IMMUTABLE));
            manager.updateAppWidget(id, views);
            return;
        }
        IMG.getCoverPageWithEffect(c, path, null).into(new CustomTarget<Bitmap>() {

            @Override public void onResourceReady(@NonNull Bitmap cover, @Nullable Transition<? super Bitmap> transition) {
                try {
                    RemoteViews views = new RemoteViews(c.getPackageName(), R.layout.widget_book_cover);
                    views.setImageViewBitmap(R.id.cover, rounded(cover));

                    Intent intent = new Intent(Intent.ACTION_VIEW, Uri.fromFile(new File(path)));
                    intent.setClassName(c, LibreraX.readerClass().getName());
                    views.setOnClickPendingIntent(R.id.cover, PendingIntent.getActivity(c, id, intent,
                            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE));

                    manager.updateAppWidget(id, views);
                } catch (Exception e) {
                    LOG.e(e);
                }
            }

            @Override public void onLoadCleared(@Nullable Drawable placeholder) {
            }
        });
    }

    private static Bitmap rounded(Bitmap cover) {
        float scale = Math.min(1f, (float) MAX_SIZE / Math.max(cover.getWidth(), cover.getHeight()));
        int w = Math.max(1, Math.round(cover.getWidth() * scale));
        int h = Math.max(1, Math.round(cover.getHeight() * scale));
        Bitmap scaled = Bitmap.createScaledBitmap(cover, w, h, true);

        Bitmap out = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setShader(new BitmapShader(scaled, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP));
        float radius = Math.min(w, h) * 0.08f;
        new Canvas(out).drawRoundRect(new RectF(0, 0, w, h), radius, radius, paint);
        return out;
    }
}
