package com.foobnix.pdf.info.widget;

import java.io.File;
import java.util.Arrays;

import org.ebookdroid.ui.viewer.VerticalViewActivity;

import com.foobnix.android.utils.LOG;
import com.foobnix.android.utils.TxtUtils;
import com.foobnix.dao2.FileMeta;
import com.foobnix.pdf.info.IMG;
import com.foobnix.pdf.info.R;
import com.foobnix.pdf.info.wrapper.AppState;
import com.foobnix.pdf.search.activity.HorizontalViewActivity;
import com.foobnix.sys.ImageExtractor;
import com.foobnix.tts.TTSActivity;
import com.foobnix.ui2.AppDB;
import com.nostra13.universalimageloader.core.ImageLoader;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ShortcutInfo;
import android.content.pm.ShortcutManager;
import android.graphics.Bitmap;
import android.graphics.drawable.Icon;
import android.net.Uri;
import android.os.Build;

public class RecentUpates {

    @TargetApi(25)
    public static void updateAll(final Context c) {
        if (c == null) {
            return;
        }

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
                    ShortcutManager shortcutManager = c.getSystemService(ShortcutManager.class);
                    String url = IMG.toUrl(recentLast.getPath(), ImageExtractor.COVER_PAGE, IMG.getImageSize());
                    Bitmap image = ImageLoader.getInstance().loadImageSync(url, IMG.displayCacheMemoryDisc);

                    Intent lastBookIntent = new Intent(c, VerticalViewActivity.class);
                    if (AppState.get().isAlwaysOpenAsMagazine) {
                        lastBookIntent = new Intent(c, HorizontalViewActivity.class);
                    }
                    lastBookIntent.setAction(Intent.ACTION_VIEW);
                    lastBookIntent.setData(Uri.fromFile(new File(recentLast.getPath())));

                    ShortcutInfo shortcut = new ShortcutInfo.Builder(c, "last")//
                            .setShortLabel(recentLast.getTitle())//
                            .setLongLabel(TxtUtils.getFileMetaBookName(recentLast))//
                            .setIcon(Icon.createWithBitmap(image))//
                            .setIntent(lastBookIntent)//
                            .build();//

                    Intent tTSIntent = new Intent(c, TTSActivity.class);
                    tTSIntent.setData(Uri.fromFile(new File(recentLast.getPath())));
                    tTSIntent.setAction(Intent.ACTION_VIEW);

                    ShortcutInfo tts = new ShortcutInfo.Builder(c, "tts")//
                            .setShortLabel(c.getString(R.string.reading_out_loud))//
                            .setLongLabel(c.getString(R.string.reading_out_loud))//
                            .setIcon(Icon.createWithBitmap(image))//
                            .setIntent(tTSIntent)//
                            .build();//

                    shortcutManager.setDynamicShortcuts(Arrays.asList(tts, shortcut));
                    // shortcutManager.setDynamicShortcuts(Arrays.asList(shortcut));
                }
            } catch (Exception e) {
                LOG.e(e);
            }

        }
    }

}
