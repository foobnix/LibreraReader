package com.foobnix.pdf.info.widget;

import java.io.File;
import java.util.List;
import java.util.Locale;

import org.ebookdroid.ui.viewer.VerticalViewActivity;

import com.foobnix.android.utils.LOG;
import com.foobnix.dao2.FileMeta;
import com.foobnix.pdf.info.IMG;
import com.foobnix.pdf.info.R;
import com.foobnix.pdf.info.TintUtil;
import com.foobnix.pdf.info.wrapper.AppState;
import com.foobnix.pdf.info.wrapper.MagicHelper;
import com.foobnix.pdf.search.activity.HorizontalViewActivity;
import com.foobnix.sys.ImageExtractor;
import com.foobnix.ui2.AppDB;
import com.foobnix.ui2.MainTabs2;
import com.nostra13.universalimageloader.core.ImageLoader;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.PendingIntent;
import android.app.PendingIntent.CanceledException;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.RectF;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.widget.RemoteViews;

public class RecentBooksWidget extends AppWidgetProvider {

    public static final String TEST_LOCALE = "test_locale";
    public static final String TEST_LOCALE_POS = "TEST_LOCALE_POS";
    private static final String ACTION_MY = "my";
    private Context context;

    @SuppressLint("NewApi")
    @Override
    public void onReceive(Context context, Intent intent) {
        this.context = context;
        String testLocale = intent.getStringExtra(TEST_LOCALE);
        if (LOG.isEnable && testLocale != null) {
            // Toast.makeText(context, testLocale, Toast.LENGTH_LONG).show();
            String DIR = "/storage/emulated/0/Download/BookTestingDB/My_Books/";

            FileMeta MUSIC = new FileMeta(DIR + "Ludwig van Beethoven - Sonata No. 14, 'Moonlight'.pdf");
            FileMeta PDF = new FileMeta(DIR + "Android-5.8-CC.pdf");
            FileMeta Alice = new FileMeta(DIR + "Carroll_-_Alice's_adventures_in_Wonderland.epub");

            String[] split = testLocale.split(",");

            String languageToLoad = split[0];
            int id = Integer.parseInt(split[1]);

            Locale locale = new Locale(languageToLoad);
            Locale.setDefault(locale);
            Configuration config = new Configuration();
            config.locale = locale;
            context.getResources().updateConfiguration(config, context.getResources().getDisplayMetrics());

            // AppState.get().tintColor =
            // Color.parseColor(AppState.STYLE_COLORS.get(new
            // Random().nextInt(AppState.STYLE_COLORS.size() - 1)));
            // TintUtil.tintRandomColor();

            AppState.get().isMusicianMode = false;
            AppState.get().isShowToolBar = true;
            AppState.get().lastClosedActivity = null;
            int i = 1;

            // MUSIC.setIsRecent(false);
            // AppDB.get().update(MUSIC);
            // PDF.setIsRecent(false);
            // AppDB.get().update(PDF);

            AppState.get().tintColor = Color.parseColor(AppState.STYLE_COLORS.get(0));
            TintUtil.init();
            if (id == i++) { // 1
                AppState.get().isWhiteTheme = true;
                AppState.get().libraryMode = AppState.MODE_GRID;
                mainTabsTest(context, 0, "id0");
            }
            if (id == i++) { // 2
                mainTabsTest(context, 1, "");
            }

            if (id == i++) {// 3
                mainTabsTest(context, 2, "");
            }

            if (id == i++) {// 4
                AppState.get().isUseBGImageNight = false;
                AppState.get().colorNigthBg = Color.parseColor("#3a3a3a");
                AppState.get().colorNigthText = Color.parseColor("#c8c8c8");

                AppState.get().isDayNotInvert = false; // nighh
                easyMode(context, Alice, "", false);
            }

            if (id == i++) {// 5
                AppState.get().isUseBGImageDay = true;
                AppState.get().bgImageDayTransparency = AppState.DAY_TRANSPARENCY;
                AppState.get().bgImageDayPath = MagicHelper.IMAGE_BG_1;
                AppState.get().colorDayText = AppState.COLOR_BLACK;
                AppState.get().colorDayBg = AppState.COLOR_WHITE;

                AppState.get().isDayNotInvert = true;
                AppState.get().isEditMode = false;
                easyMode(context, Alice, "", true);
            }

            if (id == i++) {// 6
                AppState.get().isDayNotInvert = true;
                AppState.get().isEditMode = true;
                easyMode(context, Alice, "id1", false);
            }

            if (id == i++) {// 7
                AppState.get().isDayNotInvert = true;
                AppState.get().isEditMode = true;
                easyMode(context, Alice, "id2", true);
            }
            if (id == i++) {// 8
                AppState.get().isDayNotInvert = true;
                AppState.get().isEditMode = false;
                easyMode(context, Alice, "id3", false);
            }

            if (id == i++) {// 9 PDF
                AppState.get().isCustomizeBgAndColors = false;
                AppState.get().colorDayText = AppState.COLOR_BLACK;
                AppState.get().colorDayBg = AppState.COLOR_WHITE;
                AppState.get().isUseBGImageDay = false;
                AppState.get().selectedText = "Install";

                AppState.get().isDayNotInvert = true;
                AppState.get().isEditMode = true;
                advMode(context, PDF, "id2", true);
            }

            if (id == i++) {// 10 MUSIC
                AppState.get().isCustomizeBgAndColors = false;
                AppState.get().colorDayText = AppState.COLOR_BLACK;
                AppState.get().colorDayBg = AppState.COLOR_WHITE;
                AppState.get().isUseBGImageDay = false;

                AppState.get().isDayNotInvert = true;
                AppState.get().isEditMode = false;
                AppState.get().isShowToolBar = false;
                AppState.get().isMusicianMode = true;
                advMode(context, MUSIC, "id1", false);
            }

            if (id == i++) {// 11
                AppState.get().tintColor = Color.parseColor(AppState.STYLE_COLORS.get(4));
                AppState.get().isWhiteTheme = false;
                TintUtil.init();
                mainTabsTest(context, 4, "");
            }

            if (id == i++) {// 12
                AppState.get().tintColor = Color.parseColor(AppState.STYLE_COLORS.get(3));
                AppState.get().isWhiteTheme = false;
                AppState.get().libraryMode = AppState.MODE_LIST;
                TintUtil.init();
                mainTabsTest(context, 0, "id0");
            }

            if (id == i++)// 13 //dict
            {
                AppState.get().tintColor = Color.parseColor(AppState.STYLE_COLORS.get(0));
                TintUtil.init();
                AppState.get().isDayNotInvert = true;
                AppState.get().isEditMode = true;
                AppState.get().selectedText = "There was";
                easyMode(context, Alice, "id4", true);
            }
            if (id == i++)// 14 //TTS
            {
                AppState.get().tintColor = Color.parseColor(AppState.STYLE_COLORS.get(0));
                TintUtil.init();
                AppState.get().isDayNotInvert = true;
                AppState.get().isEditMode = true;
                easyMode(context, Alice, "id5", true);
            }
            if (id == i++)// 15 //more book settings
            {
                AppState.get().tintColor = Color.parseColor(AppState.STYLE_COLORS.get(0));
                TintUtil.init();
                AppState.get().isDayNotInvert = true;
                AppState.get().isEditMode = true;
                easyMode(context, Alice, "id6", true);
            }
            if (id == i++)// 16 //file info
            {
                AppState.get().tintColor = Color.parseColor(AppState.STYLE_COLORS.get(0));
                TintUtil.init();
                AppState.get().isDayNotInvert = true;
                AppState.get().isEditMode = true;
                easyMode(context, Alice, "id7", true);

            }

            if (id == i++) {// 17
                AppState.get().tintColor = Color.parseColor(AppState.STYLE_COLORS.get(3));
                AppState.get().isWhiteTheme = true;
                AppState.get().libraryMode = AppState.MODE_AUTHORS;
                TintUtil.init();
                mainTabsTest(context, 0, "");
            }

            if (id == i++) {// 18
                AppState.get().tintColor = Color.parseColor(AppState.STYLE_COLORS.get(2));
                AppState.get().isWhiteTheme = true;
                AppState.get().libraryMode = AppState.MODE_GENRE;
                TintUtil.init();
                mainTabsTest(context, 0, "");
            }

            if (id == i++) {// 19
                AppState.get().tintColor = Color.parseColor(AppState.STYLE_COLORS.get(2));
                AppState.get().isWhiteTheme = true;
                AppState.get().libraryMode = AppState.MODE_LIST;
                TintUtil.init();
                mainTabsTest(context, 0, "id1");
            }

            return;
        }

        if (intent.getAction().equals(ACTION_MY)) {

            String className = VerticalViewActivity.class.getName();
            if (AppState.get().isAlwaysOpenAsMagazine) {
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

    private void mainTabsTest(Context context, int pos, String id) {
        Intent intent2 = new Intent(context, MainTabs2.class);
        // intent2.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        intent2.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        intent2.putExtra(TEST_LOCALE, true);
        intent2.putExtra(TEST_LOCALE_POS, pos);
        intent2.putExtra(id, true);
        context.startActivity(intent2);
    }

    @Override
    public synchronized void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        AppState.get().load(context);
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
            recent = AppDB.get().getStarsFiles();
        } else {
            recent = AppDB.get().getRecent();
        }
        AppDB.removeClouds(recent);

        String className = VerticalViewActivity.class.getName();
        if (AppState.get().isAlwaysOpenAsMagazine) {
            className = HorizontalViewActivity.class.getName();
        }
        remoteViews.removeAllViews(R.id.linearLayout);
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
