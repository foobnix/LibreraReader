package com.foobnix.pdf.info.wrapper;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.graphics.PointF;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ListView;
import android.widget.Toast;

import com.foobnix.android.utils.Dips;
import com.foobnix.android.utils.IntegerResponse;
import com.foobnix.android.utils.Keyboards;
import com.foobnix.android.utils.LOG;
import com.foobnix.android.utils.MyMath;
import com.foobnix.android.utils.ResultResponse;
import com.foobnix.android.utils.Safe;
import com.foobnix.android.utils.TxtUtils;
import com.foobnix.dao2.FileMeta;
import com.foobnix.model.AppBook;
import com.foobnix.model.AppBookmark;
import com.foobnix.model.AppProfile;
import com.foobnix.model.AppSP;
import com.foobnix.model.AppState;
import com.foobnix.pdf.info.ExtUtils;
import com.foobnix.pdf.info.IMG;
import com.foobnix.pdf.info.MyADSProvider;
import com.foobnix.pdf.info.OutlineHelper;
import com.foobnix.pdf.info.PageUrl;
import com.foobnix.pdf.info.R;
import com.foobnix.pdf.info.TintUtil;
import com.foobnix.pdf.info.model.AnnotationType;
import com.foobnix.pdf.info.model.OutlineLinkWrapper;
import com.foobnix.pdf.info.view.AlertDialogs;
import com.foobnix.pdf.info.view.MyPopupMenu;
import com.foobnix.sys.ImageExtractor;
import com.foobnix.sys.TempHolder;
import com.foobnix.tts.TTSEngine;
import com.foobnix.ui2.AppDB;

import org.ebookdroid.common.settings.SettingsManager;
import org.ebookdroid.common.settings.books.SharedBooks;
import org.ebookdroid.core.codec.Annotation;
import org.ebookdroid.core.codec.PageLink;

import java.io.File;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.TimeUnit;

@SuppressLint("NewApi")
public abstract class DocumentController {

    public static final String EXTRA_PASSWORD = "password";
    public static final String EXTRA_PERCENT = "p";
    public static final String EXTRA_PLAYLIST = "playlist";

    public static final int REPEAT_SKIP_AMOUNT = 15;

    public final static List<Integer> orientationIds = Arrays.asList(//
            ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED, //
            ActivityInfo.SCREEN_ORIENTATION_SENSOR, //
            ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE, //onCrop
            ActivityInfo.SCREEN_ORIENTATION_PORTRAIT, //
            ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE, //
            ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT//
    );

    public final static List<Integer> orientationTexts = Arrays.asList(//
            R.string.system, //
            R.string.automatic, //
            R.string.landscape, //
            R.string.portrait, //
            R.string.landscape_180, //
            R.string.portrait_180////
    );
    protected final Activity activity;
    private final LinkedList<Integer> linkHistory = new LinkedList<Integer>();
    public Handler handler = new Handler(Looper.getMainLooper());
    public Handler handler2 = new Handler(Looper.getMainLooper());
    public long readTimeStart;
    public volatile AppBookmark floatingBookmark;
    protected volatile List<OutlineLinkWrapper> outline;
    Runnable saveCurrentPageRunnable = new Runnable() {
        @Override
        public void run() {
            saveCurrentPageAsync();
        }
    };
    private DocumentWrapperUI ui;
    private File currentBook;
    private String title;
    private int timeout;
    private Runnable timerTask;
    Runnable timer = new Runnable() {

        @Override
        public void run() {
            try {
                if (activity == null || activity.isDestroyed()) {
                    LOG.d("Timer-Task Destroyed");
                    return;
                }
                timerTask.run();
                handler.postDelayed(timer, timeout);
            } catch (Exception e) {
                LOG.e(e);
            }
        }
    };
    private FrameLayout anchor;

    public DocumentController(final Activity activity) {
        this.activity = activity;
        readTimeStart = System.currentTimeMillis();
        TTSEngine.get().mp3Destroy();
    }

    public static int getRotationText() {
        return orientationTexts.get(orientationIds.indexOf(AppState.get().orientation));
    }

    public static boolean isEinkOrMode(Context c) {
        return Dips.isEInk() || AppState.get().appTheme == AppState.THEME_INK;

    }

    public static void turnOffButtons(final Activity a) {
        a.getWindow().getAttributes().buttonBrightness = WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_OFF;
    }

    public static void turnOnButtons(final Activity a) {
        a.getWindow().getAttributes().buttonBrightness = WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE;
    }

    public static void runFullScreen(final Activity a) {
        try {
            a.getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
            a.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                a.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
                a.getWindow().getAttributes().layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_NEVER;
                a.getWindow().setAttributes(a.getWindow().getAttributes());
            }

            Keyboards.hideNavigation(a);
        } catch (Exception e) {
            LOG.e(e);
        }
    }

    public static void runFullScreenCutOut(final Activity a) {
        try {

            setNavBarTintColor(a);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {


                a.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN);
                a.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);

                a.getWindow().addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
                a.getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);

                a.getWindow().getAttributes().layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES;
                a.getWindow().setAttributes(a.getWindow().getAttributes());


            }


            Keyboards.hideNavigation(a);
        } catch (Exception e) {
            LOG.e(e);
        }
    }

    public static void setNavBarTintColor(Activity a) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            a.getWindow().setNavigationBarColor(TintUtil.color);
        }
    }

    public static void runNormalScreen(final Activity a) {
        try {
            a.getWindow().addFlags(WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN);
            a.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);

            final View decorView = a.getWindow().getDecorView();
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
                decorView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
            }

            setNavBarTintColor(a);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                a.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
                a.getWindow().getAttributes().layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_DEFAULT;
                a.getWindow().setAttributes(a.getWindow().getAttributes());
            }


        } catch (Exception e) {
            LOG.e(e);
        }
    }

    public static void chooseFullScreen(final Activity a, final int mode) {
        if (mode == AppState.FULL_SCREEN_FULLSCREEN) {
            runFullScreen(a);
        } else if (mode == AppState.FULL_SCREEN_NORMAL) {
            runNormalScreen(a);
        } else if (mode == AppState.FULL_SCREEN_FULLSCREEN_CUTOUT) {
            runFullScreenCutOut(a);
        }

    }

    public static String getFullScreenName(final Activity a, final int mode) {
        switch (mode) {
            case AppState.FULL_SCREEN_FULLSCREEN_CUTOUT:
                return a.getString(R.string.with_cutout);
            case AppState.FULL_SCREEN_NORMAL:
                return a.getString(R.string.normal);
            case AppState.FULL_SCREEN_FULLSCREEN:
                return a.getString(R.string.full_screen);
        }
        return "-";
    }

    public static int getFullScreenIcon(final Activity a, final int mode) {
        switch (mode) {
            case AppState.FULL_SCREEN_FULLSCREEN_CUTOUT:
                return R.drawable.glyphicons_487_fit_frame_to_image;
            case AppState.FULL_SCREEN_NORMAL:
                return R.drawable.glyphicons_488_fit_image_to_frame;
            case AppState.FULL_SCREEN_FULLSCREEN:
                return R.drawable.glyphicons_487_fit_frame_to_image;
        }
        return R.drawable.glyphicons_488_fit_image_to_frame;
    }

    public static void showFullScreenPopup(Activity a, View v, IntegerResponse response, int currentMode) {


        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P && a.getWindow().getDecorView().getRootWindowInsets().getDisplayCutout() != null) {
            List<Integer> ids = Arrays.asList(AppState.FULL_SCREEN_FULLSCREEN_CUTOUT, AppState.FULL_SCREEN_FULLSCREEN, AppState.FULL_SCREEN_NORMAL);

            MyPopupMenu popup = new MyPopupMenu(a, v);
            for (int id : ids)

                if (id == AppState.FULL_SCREEN_FULLSCREEN_CUTOUT && Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    LOG.d("getDisplayCutout", a.getWindow().getDecorView().getRootWindowInsets().getDisplayCutout());
                    // if (getActivity().getWindow().getDecorView().getRootWindowInsets().getDisplayCutout() != null) {
                    popup.getMenu().add(DocumentController.getFullScreenName(a, id)).setOnMenuItemClickListener(item -> {
                        response.onResultRecive(id);
                        return false;
                    });
                    //}
                } else {
                    popup.getMenu().add(DocumentController.getFullScreenName(a, id)).setOnMenuItemClickListener(item -> {
                        response.onResultRecive(id);
                        return false;
                    });

                }
            popup.show();
        } else {
            if (currentMode == AppState.FULL_SCREEN_NORMAL) {
                response.onResultRecive(AppState.FULL_SCREEN_FULLSCREEN);
            } else {
                response.onResultRecive(AppState.FULL_SCREEN_NORMAL);
            }
        }
    }

    private static void applyTheme(final Activity a) {
        if (AppState.get().appTheme == AppState.THEME_LIGHT) {
            a.setTheme(R.style.StyledIndicatorsWhite);
        } else {
            a.setTheme(R.style.StyledIndicatorsBlack);
        }
    }

    public static void doRotation(final Activity a) {
        try {
            // LOG.d("isSystemAutoRotation isSystemAutoRotation",
            // Dips.isSystemAutoRotation(a));
            // LOG.d("isSystemAutoRotation geUserRotation", Dips.geUserRotation(a));
            a.setRequestedOrientation(AppState.get().orientation);
        } catch (Exception e) {
            LOG.e(e);
        }
    }

    public static void doContextMenu(Activity a) {
        PackageManager pm = a.getApplicationContext().getPackageManager();
        ComponentName compName = new ComponentName(a.getPackageName(), "com.foobnix.zipmanager.SendReceiveActivityAlias");
        if (AppState.get().isMenuIntegration) {
            pm.setComponentEnabledSetting(compName, PackageManager.COMPONENT_ENABLED_STATE_ENABLED, PackageManager.DONT_KILL_APP);
            LOG.d("COMPONENT_ENABLED_STATE_ENABLED");
        } else {
            pm.setComponentEnabledSetting(compName, PackageManager.COMPONENT_ENABLED_STATE_DISABLED, PackageManager.DONT_KILL_APP);
            LOG.d("COMPONENT_ENABLED_STATE_DISABLED");
        }

    }

    public boolean isPasswordProtected() {
        try {
            return TxtUtils.isNotEmpty(activity.getIntent().getStringExtra(EXTRA_PASSWORD));
        } catch (Exception e) {
            return false;
        }
    }

    public synchronized void runTimer(int timeout, Runnable run) {
        this.timeout = timeout;
        this.timerTask = run;
        stopTimer();
        if (handler != null) {
            handler.post(timer);
        }
    }

    public void stopTimer() {
        if (handler != null) {
            handler.removeCallbacks(timer);
        }
    }

    public abstract void onGoToPage(int page);

    public abstract void onSrollLeft();

    public abstract void onSrollRight();

    public abstract void onScrollUp();

    public abstract void onScrollDown();

    public abstract void onNextPage(boolean animate);

    public abstract void onPrevPage(boolean animate);

    public abstract void onNextScreen(boolean animate);

    public abstract void onPrevScreen(boolean animate);

    public abstract void onZoomInc();

    public abstract void onZoomDec();

    public abstract void onZoomInOut(int x, int y);

    public abstract void onCloseActivityAdnShowInterstial();

    public abstract void onCloseActivityFinal(Runnable run);

    public abstract void onNightMode();

    public abstract void onCrop();

    public abstract void onFullScreen();

    public abstract int getCurentPage();

    public abstract int getCurentPageFirst1();

    public abstract int getPageCount();

    public abstract void onScrollY(int value);

    public abstract void onScrollYPercent(float value);

    public abstract void onAutoScroll();

    public abstract void clearSelectedText();

    public abstract void saveChanges(List<PointF> points, int color);

    public abstract void deleteAnnotation(long pageHander, int page, int index);

    public abstract void underlineText(int color, float width, AnnotationType type);

    public abstract void getOutline(ResultResponse<List<OutlineLinkWrapper>> outline, boolean forse);

    public abstract String getFootNote(String text);

    public abstract List<String> getMediaAttachments();

    public abstract PageUrl getPageUrl(int page);

    public abstract void saveAnnotationsToFile();

    public abstract int getBookWidth();

    public abstract int getBookHeight();

    public void saveSettings() {

    }

    public float getPercentage() {
        return MyMath.percent(getCurentPageFirst1(), getPageCount());
    }

    public MyADSProvider getAdsProvider() {
        return null;
    }

    public abstract void updateRendering();

    public void goToPageByTTS() {
        try {
            if (TTSEngine.get().isPlaying()) {

                AppBook bs = SettingsManager.getBookSettings(getCurrentBook().getPath());

                if (getCurrentBook().getPath().equals(AppSP.get().lastBookPath)) {
                    onGoToPage(bs.getCurrentPage(getPageCount()).viewIndex + 1);
                    LOG.d("goToPageByTTS", AppSP.get().lastBookPage + 1);
                }
            }
        } catch (Exception e) {
            LOG.e(e);
        }
    }

    public abstract void cleanImageMatrix();

    public void checkReadingTimer() {
        long timeout = System.currentTimeMillis() - readTimeStart;
        if (AppState.get().remindRestTime != -1 && timeout >= TimeUnit.MINUTES.toMillis(AppState.get().remindRestTime)) {
            AlertDialogs.showOkDialog(activity, getString(R.string.remind_msg), new Runnable() {

                @Override
                public void run() {
                    readTimeStart = System.currentTimeMillis();
                }
            }, new Runnable() {
                @Override
                public void run() {
                    readTimeStart = System.currentTimeMillis();
                }
            });
        }

    }

    public void closeActivity() {
        handler2.removeCallbacksAndMessages(null);
        handler.removeCallbacksAndMessages(null);
    }

    public void saveCurrentPage() {
        AppBook bs = SettingsManager.getBookSettings();
        if (bs != null) {
            bs.updateFromAppState();
            bs.currentPageChanged(getCurentPageFirst1(), getPageCount());
            handler2.removeCallbacks(saveCurrentPageRunnable);
            handler2.postDelayed(saveCurrentPageRunnable, 1000);
        }
    }

    public void saveCurrentPageAsync() {
        if (TempHolder.get().loadingCancelled) {
            LOG.d("Loading cancelled");
            return;
        }
        // int page = PageUrl.fakeToReal(currentPage);
        LOG.d("_PAGE", "saveCurrentPage", getCurentPageFirst1(), getPageCount());
        try {
            if (getPageCount() <= 0) {
                LOG.d("_PAGE", "saveCurrentPage skip");
                return;
            }
            AppBook bs = SettingsManager.getBookSettings();
            bs.updateFromAppState();
            SharedBooks.save(bs);

            //AppBook bs = SettingsManager.getBookSettings(getCurrentBook().getPath());
            //bs.updateFromAppState();
            //bs.currentPageChanged(getCurentPageFirst1(), getPageCount());
            //SharedBooks.save(bs);
        } catch (Exception e) {
            LOG.e(e);
        }

    }


    public boolean isBookMode() {
        return AppSP.get().readingMode == AppState.READING_MODE_BOOK;
    }

    public boolean isScrollMode() {
        return AppSP.get().readingMode == AppState.READING_MODE_SCROLL;
    }

    public boolean isMusicianMode() {
        return AppSP.get().readingMode == AppState.READING_MODE_MUSICIAN;
    }

    public void onResume() {
        readTimeStart = System.currentTimeMillis();
        try {
            if (getPageCount() != 0) {
                AppBook bs = SettingsManager.getBookSettings(getCurrentBook().getPath());
                if (getCurentPage() != bs.getCurrentPage(getPageCount()).viewIndex + 1) {
                    onGoToPage(bs.getCurrentPage(getPageCount()).viewIndex + 1);
                }
            }
        } catch (Exception e) {
            LOG.e(e);
        }
    }

    public FileMeta getBookFileMeta() {
        return AppDB.get().getOrCreate(getCurrentBook().getPath());
    }

    public String getBookFileMetaName() {
        return TxtUtils.getFileMetaBookName(getBookFileMeta());
    }

    public void loadOutline(final ResultResponse<List<OutlineLinkWrapper>> resultTop) {
        getOutline(new ResultResponse<List<OutlineLinkWrapper>>() {

            @Override
            public boolean onResultRecive(List<OutlineLinkWrapper> result) {
                outline = result;
                if (resultTop != null) {
                    resultTop.onResultRecive(result);
                }
                return false;
            }
        }, false);
    }

    public void initAnchor(FrameLayout anchor) {
        this.anchor = anchor;
    }

    public List<OutlineLinkWrapper> getCurrentOutline() {
        return outline;
    }

    public String getCurrentChapter() {
        return OutlineHelper.getCurrentChapterAsString(this);
    }

    public boolean isTextFormat() {
        try {
            boolean textFomat = ExtUtils.isTextFomat(getCurrentBook().getPath());
            LOG.d("isTextFormat", getCurrentBook().getPath(), textFomat);
            return textFomat;
        } catch (Exception e) {
            LOG.e(e);
            return false;
        }
    }

    public boolean isVisibleDialog() {
        return anchor != null && anchor.getVisibility() == View.VISIBLE;
    }

    public boolean closeDialogs() {

        if (anchor == null) {
            LOG.d("closeDialogs", "anchor false");
            return false;
        }


        boolean isVisible = anchor.getVisibility() == View.VISIBLE;
        LOG.d("closeDialogs", "isVisible", isVisible);
        if (isVisible) {
            try {
                activity.findViewById(R.id.closePopup).performClick();
                LOG.d("closeDialogs", "performClick");
            } catch (Exception e) {
                LOG.e(e);
            }
            clearSelectedText();
        }
        return isVisible;
    }

    public abstract boolean isCropCurrentBook();

    public float getOffsetX() {
        return -1;
    }

    public float getOffsetY() {
        return -1;
    }

    public void onGoToPage(final int page, final float offsetX, final float offsetY) {

    }

    public void addRecent(final Uri uri) {
        AppDB.get().addRecent(uri.getPath());
        // BookmarksData.get().addRecent(uri);
    }

    public void onClickTop() {

    }

    public String getString(int resId) {
        return activity.getString(resId);
    }

    public void onLinkHistory() {
        if (!getLinkHistory().isEmpty()) {
            final int last = getLinkHistory().removeLast();
            onScrollY(last);
            LOG.d("onLinkHistory", last);

        }
    }

    public void restartActivity() {
        IMG.clearMemoryCache();
        saveAppState();
        TTSEngine.get().stop();

        Safe.run(new Runnable() {

            @Override
            public void run() {
                ImageExtractor.clearCodeDocument();
                activity.finish();
                activity.startActivity(activity.getIntent());
            }
        });
    }

    public void saveAppState() {
        AppProfile.save(activity);
    }

    public void onLeftPress() {
        if (AppState.get().tapZoneLeft == AppState.TAP_DO_NOTHING) {
            return;
        }
        if (AppState.get().tapZoneLeft == AppState.TAP_PREV_PAGE) {
            ui.prevChose(false);
        } else {
            ui.nextChose(false);
        }
    }

    public void onTopPress() {
        if (AppState.get().tapZoneTop == AppState.TAP_DO_NOTHING) {
            return;
        }
        if (AppState.get().tapZoneTop == AppState.TAP_PREV_PAGE) {
            ui.prevChose(false);
        } else {
            ui.nextChose(false);
        }
    }

    public void onBottomPress() {
        if (AppState.get().tapZoneBottom == AppState.TAP_DO_NOTHING) {
            return;
        }
        if (AppState.get().tapZoneBottom == AppState.TAP_NEXT_PAGE) {
            ui.nextChose(false);
        } else {
            ui.prevChose(false);
        }
    }

    public void onRightPress() {
        if (AppState.get().tapZoneRight == AppState.TAP_DO_NOTHING) {
            return;
        }
        if (AppState.get().tapZoneRight == AppState.TAP_NEXT_PAGE) {
            ui.nextChose(false);
        } else {
            ui.prevChose(false);
        }
    }

    public void onLeftPressAnimate() {
        ui.prevChose(true);
    }

    public void onRightPressAnimate() {
        ui.nextChose(true);
    }

    public void showAnnotation(Annotation annotation) {
        Toast.makeText(activity, "" + annotation.text, Toast.LENGTH_SHORT).show();
    }

    public void onSingleTap() {
        ui.onSingleTap();
    }

    public void onDoubleTap(int x, int y) {
        ui.doDoubleTap(x, y);
    }

    public abstract String getTextForPage(int page);

    public abstract String getPageHtml();


    public abstract List<PageLink> getLinksForPage(int page);

    public void onAnnotationTap(long pageHander, int page, int index) {
        deleteAnnotation(pageHander, page, index);
        showEditDialogIfNeed();
        saveAnnotationsToFile();
    }

    public void showEditDialogIfNeed() {
        ui.showEditDialogIfNeed();
    }

    public void onLongPress(MotionEvent ev) {
        ui.onLongPress(ev);
    }

    public abstract void doSearch(String text, ResultResponse<Integer> result);

    public Activity getActivity() {
        return activity;
    }

    public void toast(final String text) {
        Toast.makeText(activity, text, Toast.LENGTH_SHORT).show();
    }

    public DocumentWrapperUI getUi() {
        return ui;
    }

    public void setUi(final DocumentWrapperUI ui) {
        this.ui = ui;
    }

    public boolean showContent(final ListView contentList) {
        return false;
    }

    public File getCurrentBook() {
        return currentBook;
    }

    public void setCurrentBook(final File currentBook) {
        this.currentBook = currentBook;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(final String title) {
        this.title = title;
    }

    public LinkedList<Integer> getLinkHistory() {
        return linkHistory;
    }

    public void toPageDialog() {

    }

    public abstract void alignDocument();

    public abstract void centerHorizontal();

    public abstract void recyclePage(int page);
}
