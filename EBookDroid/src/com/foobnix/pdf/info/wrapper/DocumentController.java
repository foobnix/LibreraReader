package com.foobnix.pdf.info.wrapper;

import java.io.File;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import org.ebookdroid.core.codec.Annotation;
import org.ebookdroid.core.codec.PageLink;

import com.foobnix.android.utils.ResultResponse;
import com.foobnix.dao2.FileMeta;
import com.foobnix.pdf.info.ExtUtils;
import com.foobnix.pdf.info.IMG;
import com.foobnix.pdf.info.OutlineHelper;
import com.foobnix.pdf.info.R;
import com.foobnix.pdf.info.model.AnnotationType;
import com.foobnix.pdf.info.model.OutlineLinkWrapper;
import com.foobnix.sys.ImageExtractor;
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

    public static List<Integer> ROTATIONS = Arrays.asList(//
            ActivityInfo.SCREEN_ORIENTATION_SENSOR, //
            ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE, //
            ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

    protected final Activity activity;
    private DocumentWrapperUI ui;
    public Handler handler;

    public DocumentController(final Activity activity) {
        this.activity = activity;
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

    public abstract void getOutline(ResultResponse<List<OutlineLinkWrapper>> outline);

    public abstract String getFootNote(String text);

    public abstract List<String> getMediaAttachments();

    public abstract String getPagePath(int page);

    public abstract void saveAnnotationsToFile();

    public void saveSettings() {

    }

    public void updateRendering() {

    }

    public abstract void cleanImageMatrix();

    public Bitmap getBookImage() {
        String url = IMG.toUrl(getCurrentBook().getPath(), ImageExtractor.COVER_PAGE_WITH_EFFECT, IMG.getImageSize());
        return ImageLoader.getInstance().loadImageSync(url, IMG.displayImageOptions);
    }

    public FileMeta getBookFileMeta() {
        return AppDB.get().getOrCreate(getCurrentBook().getPath());
    }

    public void loadOutline() {
        getOutline(new ResultResponse<List<OutlineLinkWrapper>>() {

            @Override
            public boolean onResultRecive(List<OutlineLinkWrapper> result) {
                outline = result;
                return false;
            }
        });
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
            return item.title;
        } else {
            return null;
        }
    }

    public boolean isTextFormat() {
        return ExtUtils.isTextFomat(getCurrentBook().getPath());
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

        }
    }

    public static void runFullScreen(final Activity a) {
        a.getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        a.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN);
        hideHavigationBar(a);

    }

    public static void hideHavigationBar(final Activity a) {
        final View decorView = a.getWindow().getDecorView();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            decorView.postDelayed(new Runnable() {
                @Override
                public void run() {
                    decorView.setSystemUiVisibility(//
                            View.SYSTEM_UI_FLAG_LAYOUT_STABLE//
                                    | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION //
                                    | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION //
                                    | View.SYSTEM_UI_FLAG_IMMERSIVE);
                }
            }, 100);
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
            decorView.postDelayed(new Runnable() {
                @Override
                public void run() {
                    decorView.setSystemUiVisibility( //
                            View.SYSTEM_UI_FLAG_LOW_PROFILE //
                                    | View.SYSTEM_UI_FLAG_FULLSCREEN //
                    );
                }
            }, 100);
        }

    }

    public static void runNormalScreen(final Activity a) {
        a.getWindow().addFlags(WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN);
        a.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);

        final View decorView = a.getWindow().getDecorView();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
            decorView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
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
        final float brightness = AppState.getInstance().brightness;
        final WindowManager.LayoutParams lp = a.getWindow().getAttributes();
        if (brightness >= 0.01) {
            lp.screenBrightness = brightness;
            a.getWindow().setAttributes(lp);
        } else {
            lp.screenBrightness = -1;
            a.getWindow().setAttributes(lp);
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

    public static void nextRotation() {
        final int type = AppState.getInstance().orientation;
        if (type == ROTATIONS.get(0)) {
            AppState.getInstance().orientation = ROTATIONS.get(1);
        }

        else if (type == ROTATIONS.get(1)) {
            AppState.getInstance().orientation = ROTATIONS.get(2);
        }

        else if (type == ROTATIONS.get(2)) {
            AppState.getInstance().orientation = ROTATIONS.get(0);
        }
    }

    public static void restartActivity(Activity a) {
        a.finish();
        a.startActivity(a.getIntent());
    }

    public void restartActivity() {
        saveAppState();
        if (Build.VERSION.SDK_INT >= 11) {
            // activity.recreate();
        } else {

        }
        activity.finish();
        activity.startActivity(activity.getIntent());
    }

    public void saveAppState() {
        AppState.get().save(activity);
    }

    public static void doRotation(final Activity a) {
        a.setRequestedOrientation(AppState.getInstance().orientation);
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

    public abstract void recenterDocument();

}
