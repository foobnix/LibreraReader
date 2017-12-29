package com.foobnix.pdf.info.wrapper;

import java.io.File;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.ebookdroid.core.codec.Annotation;
import org.ebookdroid.core.codec.PageLink;

import com.foobnix.android.utils.Keyboards;
import com.foobnix.android.utils.LOG;
import com.foobnix.android.utils.ResultResponse;
import com.foobnix.android.utils.Safe;
import com.foobnix.android.utils.TxtUtils;
import com.foobnix.dao2.FileMeta;
import com.foobnix.pdf.info.ExtUtils;
import com.foobnix.pdf.info.IMG;
import com.foobnix.pdf.info.MyADSProvider;
import com.foobnix.pdf.info.OutlineHelper;
import com.foobnix.pdf.info.PageUrl;
import com.foobnix.pdf.info.R;
import com.foobnix.pdf.info.model.AnnotationType;
import com.foobnix.pdf.info.model.OutlineLinkWrapper;
import com.foobnix.pdf.info.view.AlertDialogs;
import com.foobnix.sys.ImageExtractor;
import com.foobnix.tts.TTSEngine;
import com.foobnix.ui2.AppDB;
import com.nostra13.universalimageloader.core.ImageLoader;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.pm.ActivityInfo;
import android.graphics.Bitmap;
import android.graphics.PointF;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.provider.Settings.SettingNotFoundException;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.ListView;
import android.widget.Toast;

@SuppressLint("NewApi")
public abstract class DocumentController {

    public final static List<Integer> orientationIds = Arrays.asList(//
            ActivityInfo.SCREEN_ORIENTATION_SENSOR, //
            ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE, //
            ActivityInfo.SCREEN_ORIENTATION_PORTRAIT, //
            ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE, //
            ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT//
    );

    public final static List<Integer> orientationTexts = Arrays.asList(//
            R.string.automatic, //
            R.string.landscape, //
            R.string.portrait, //
            R.string.landscape_180, //
            R.string.portrait_180////
    );

    public static int getRotationText() {
        return orientationTexts.get(orientationIds.indexOf(AppState.getInstance().orientation));
    }


    protected final Activity activity;
    private DocumentWrapperUI ui;
    public Handler handler;

    public long readTimeStart;

    public DocumentController(final Activity activity) {
        this.activity = activity;
        readTimeStart = System.currentTimeMillis();
    }

    public void initHandler() {
        handler = new Handler();
    }

    private File currentBook;
    private String title;

    private final LinkedList<Integer> linkHistory = new LinkedList<Integer>();

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

    public abstract void onCloseActivity();

    public abstract void onCloseActivityFinal(Runnable run);

    public abstract void onNightMode();

    public abstract void onCrop();

    public abstract void onFullScreen();

    public abstract int getCurentPage();

    public abstract int getCurentPageFirst1();

    public abstract int getPageCount();

    public abstract void onScrollY(int value);

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

    public void saveSettings() {

    }

    public MyADSProvider getAdsProvider() {
        return null;
    }

    public void updateRendering() {

    }

    public abstract void cleanImageMatrix();

    public void checkReadingTimer() {
        long timeout = System.currentTimeMillis() - readTimeStart;
        if (timeout >= TimeUnit.MINUTES.toMillis(AppState.get().remindRestTime)) {
            AlertDialogs.showOkDialog(activity, getString(R.string.remind_msg), new Runnable() {

                @Override
                public void run() {
                    readTimeStart = System.currentTimeMillis();
                }
            });
        }
    }

    public boolean isEasyMode() {
        return AppState.getInstance().isAlwaysOpenAsMagazine;
    }

    public void onResume() {
        readTimeStart = System.currentTimeMillis();
    }

    public Bitmap getBookImage() {
        String url = IMG.toUrl(getCurrentBook().getPath(), ImageExtractor.COVER_PAGE_WITH_EFFECT, IMG.getImageSize());
        return ImageLoader.getInstance().loadImageSync(url, IMG.displayImageOptions);
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

    List<OutlineLinkWrapper> outline;

    public void setOutline(List<OutlineLinkWrapper> outline) {
        this.outline = outline;
    }

    public String getCurrentChapter() {
        if (outline == null || outline.isEmpty()) {
            return null;
        }
        int root = OutlineHelper.getRootItemByPageNumber(outline, getCurentPageFirst1());
        if (outline.size() > root) {
            OutlineLinkWrapper item = outline.get(root);
            return item.getTitleAsString();
        } else {
            return null;
        }
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
        // AppSharedPreferences.get().addRecent(uri);
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

    public static void runFullScreen(final Activity a) {
        try {
            a.getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
            a.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN);
            Keyboards.hideNavigation(a);
        } catch (Exception e) {
            LOG.e(e);
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
        } catch (Exception e) {
            LOG.e(e);
        }
    }

    public static void chooseFullScreen(final Activity a, final boolean isFullscren) {
        if (isFullscren) {
            runFullScreen(a);
        } else {
            runNormalScreen(a);
        }
    }

    private static void applyTheme(final Activity a) {
        if (AppState.getInstance().isWhiteTheme) {
            a.setTheme(R.style.StyledIndicatorsWhite);
        } else {
            a.setTheme(R.style.StyledIndicatorsBlack);
        }
    }

    public static void applyBrigtness(final Activity a) {
        try {
            float brightness = AppState.getInstance().brightness;
            final WindowManager.LayoutParams lp = a.getWindow().getAttributes();
            if (brightness < 0) {
                brightness = -1;
            }

            LOG.d("applyBrigtness", brightness);
            lp.screenBrightness = brightness;
            a.getWindow().setAttributes(lp);
        } catch (Exception e) {
            LOG.e(e);
        }
    }

    public static float getSystemBrigtness(final Activity a) {
        try {
            final int brightInt = android.provider.Settings.System.getInt(a.getContentResolver(), android.provider.Settings.System.SCREEN_BRIGHTNESS);
            return (float) brightInt / 255;
        } catch (final SettingNotFoundException e) {
            e.printStackTrace();
        }
        return 0.5f;
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
        AppState.get().save(activity);
    }

    public static void doRotation(final Activity a) {
        try {
            a.setRequestedOrientation(AppState.getInstance().orientation);
        } catch (Exception e) {
            LOG.e(e);
        }
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

    public boolean closeFooterNotesDialog() {
        if (ui != null && ui.showFootNotes != null && ui.showFootNotes.isVisible()) {
            ui.showFootNotes.closeDialog();
            return true;
        }
        return false;
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

    public void recyclePage(int page) {

    }

}
