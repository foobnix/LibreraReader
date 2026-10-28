package com.foobnix.pdf.info.widget;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.pm.ShortcutInfoCompat;
import androidx.core.content.pm.ShortcutManagerCompat;
import androidx.core.graphics.drawable.IconCompat;

import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;
import com.foobnix.android.utils.LOG;
import com.foobnix.android.utils.TxtUtils;
import com.foobnix.librerax.LibreraX;
import com.foobnix.pdf.info.IMG;
import com.foobnix.pdf.info.R;

import java.io.File;

/**
 * A book pinned to the home screen: its cover is the icon, and a tap opens it in the reader,
 * the way the recent books widget opens one.
 */
public class BookShortcut {

    private static final int ICON_SIZE = 192;

    public static void pin(final Activity a, final File file, final String title) {
        if (!ShortcutManagerCompat.isRequestPinShortcutSupported(a)) {
            Toast.makeText(a, R.string.msg_unexpected_error, Toast.LENGTH_SHORT).show();
            return;
        }
        IMG.getCoverPageWithEffect(a, file.getPath(), null).into(new CustomTarget<Bitmap>() {

            @Override public void onResourceReady(@NonNull Bitmap cover, @Nullable Transition<? super Bitmap> transition) {
                request(a, file, title, IconCompat.createWithBitmap(square(cover)));
            }

            @Override public void onLoadFailed(@Nullable Drawable errorDrawable) {
                request(a, file, title, IconCompat.createWithResource(a, R.mipmap.icon_pdf_pro));
            }

            @Override public void onLoadCleared(@Nullable Drawable placeholder) {
            }
        });
    }

    private static void request(Activity a, File file, String title, IconCompat icon) {
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.fromFile(file));
            intent.setClassName(a, LibreraX.readerClass().getName());

            String label = TxtUtils.isEmpty(title) ? file.getName() : title;
            ShortcutInfoCompat shortcut = new ShortcutInfoCompat.Builder(a, "book:" + file.getPath())
                    .setShortLabel(label)
                    .setLongLabel(label)
                    .setIcon(icon)
                    .setIntent(intent)
                    .build();
            ShortcutManagerCompat.requestPinShortcut(a, shortcut, null);
        } catch (Exception e) {
            LOG.e(e);
            Toast.makeText(a, R.string.msg_unexpected_error, Toast.LENGTH_SHORT).show();
        }
    }

    // the cover is fitted whole into a square, as launchers expect a square icon
    private static Bitmap square(Bitmap cover) {
        Bitmap icon = Bitmap.createBitmap(ICON_SIZE, ICON_SIZE, Bitmap.Config.ARGB_8888);
        float scale = Math.min((float) ICON_SIZE / cover.getWidth(), (float) ICON_SIZE / cover.getHeight());
        int w = Math.round(cover.getWidth() * scale);
        int h = Math.round(cover.getHeight() * scale);
        int left = (ICON_SIZE - w) / 2;
        int top = (ICON_SIZE - h) / 2;
        new Canvas(icon).drawBitmap(cover, null, new Rect(left, top, left + w, top + h), new Paint(Paint.FILTER_BITMAP_FLAG));
        return icon;
    }
}
