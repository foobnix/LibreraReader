package com.foobnix.pdf.info.widget;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ShortcutInfo;
import android.content.pm.ShortcutManager;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.Icon;
import android.net.Uri;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;
import com.foobnix.android.utils.LOG;
import com.foobnix.android.utils.TxtUtils;
import com.foobnix.dao2.FileMeta;
import com.foobnix.model.AppProfile;
import com.foobnix.model.AppSP;
import com.foobnix.model.AppState;
import com.foobnix.pdf.info.IMG;
import com.foobnix.pdf.info.R;
import com.foobnix.pdf.search.activity.HorizontalViewActivity;
import com.foobnix.sys.ImageExtractor;
import com.foobnix.tts.TTSActivity;
import com.foobnix.ui2.AppDB;

import org.ebookdroid.LibreraApp;
import org.ebookdroid.ui.viewer.VerticalViewActivity;

import java.io.File;
import java.util.Arrays;

public class RecentUpates {

    @TargetApi(25)
    public static void updateAll() {
        Context c = LibreraApp.context;
        if (c == null) {
            return;
        }

        AppProfile.save(c);

        LOG.d("RecentUpates", "MUPDF!", c.getClass());
        try {

            {
                Intent intent = new Intent(c, RecentBooksWidget.class);
                intent.setAction("android.appwidget.action.APPWIDGET_UPDATE");
                c.sendBroadcast(intent);
            }
        } catch (Exception e) {
            LOG.e(e);
        }

        if (Build.VERSION.SDK_INT >= 25) {
            try {

                FileMeta recentLast = AppDB.get().getRecentLastNoFolder();
                if (recentLast != null && TxtUtils.isNotEmpty(recentLast.getTitle())) {
                    File bookFile = new File(recentLast.getPath());
                    if (!bookFile.isFile()) {
                        LOG.d("Book not found", bookFile.getPath());
                        return;
                    }

                    ShortcutManager shortcutManager = c.getSystemService(ShortcutManager.class);
                    String url = IMG.toUrl(recentLast.getPath(), ImageExtractor.COVER_PAGE, IMG.getImageSize());
                    //Bitmap image = ImageLoader.getInstance().loadImageSync(url, IMG.displayCacheMemoryDisc);

                    Glide.with(c).asBitmap().load(url).into(new CustomTarget<Bitmap>() {
                        @Override
                        public void onResourceReady(@NonNull Bitmap image, @Nullable Transition<? super Bitmap> transition) {


                            Intent lastBookIntent = new Intent(c, VerticalViewActivity.class);
                            if (AppSP.get().readingMode == AppState.READING_MODE_BOOK) {
                                lastBookIntent = new Intent(c, HorizontalViewActivity.class);
                            }
                            lastBookIntent.setAction(Intent.ACTION_VIEW);

                            lastBookIntent.setData(Uri.fromFile(bookFile));

                            ShortcutInfo shortcut = new ShortcutInfo.Builder(c, "last")//
                                    .setShortLabel(recentLast.getTitle())//
                                    .setLongLabel(TxtUtils.getFileMetaBookName(recentLast))//
                                    .setIcon(Icon.createWithBitmap(image))//
                                    .setIntent(lastBookIntent)//
                                    .build();//

                            Intent tTSIntent = new Intent(c, TTSActivity.class);
                            tTSIntent.setData(Uri.fromFile(bookFile));
                            tTSIntent.setAction(Intent.ACTION_VIEW);

                            ShortcutInfo tts = new ShortcutInfo.Builder(c, "tts")//
                                    .setShortLabel(c.getString(R.string.reading_out_loud))//
                                    .setLongLabel(c.getString(R.string.reading_out_loud))//
                                    .setIcon(Icon.createWithBitmap(image))//
                                    .setIntent(tTSIntent)//
                                    .build();//

                            shortcutManager.setDynamicShortcuts(Arrays.asList(tts, shortcut));
                        }

                        @Override
                        public void onLoadCleared(@Nullable Drawable placeholder) {

                        }


                    });


                    // shortcutManager.setDynamicShortcuts(Arrays.asList(shortcut));
                }
            } catch (Exception e) {
                LOG.e(e);
            }

        }
    }

}
