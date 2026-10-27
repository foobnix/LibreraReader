package com.foobnix.pdf.info.wrapper;

import android.content.pm.ApplicationInfo;
import android.widget.TextView;
import org.ebookdroid.droids.foliate.codec.FoliateDocument;
import org.ebookdroid.droids.pdfium.codec.PdfiumDocument;
import org.ebookdroid.droids.djvu.codec.DjvuDocument;
import org.ebookdroid.droids.comic.codec.ComicDocument;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.graphics.PointF;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.graphics.Color;
import android.graphics.Rect;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ListView;
import android.widget.Toast;

import androidx.activity.ComponentActivity;
import androidx.activity.EdgeToEdge;
import androidx.activity.SystemBarStyle;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsControllerCompat;

import com.foobnix.android.utils.Apps;
import com.foobnix.android.utils.Dips;
import com.foobnix.android.utils.IntegerResponse;
import com.foobnix.android.utils.Keyboards;
import com.foobnix.android.utils.LOG;
import com.foobnix.android.utils.MyMath;
import com.foobnix.android.utils.ResultResponse;
import com.foobnix.android.utils.Safe;
import android.graphics.RectF;
import com.foobnix.android.utils.TxtUtils;
import com.foobnix.dao2.FileMeta;
import com.foobnix.model.AppBook;
import com.foobnix.model.AppBookmark;
import com.foobnix.model.AppProfile;
import com.foobnix.model.AppSP;
import com.foobnix.model.AppState;
import com.foobnix.pdf.info.ExtUtils;
import com.foobnix.pdf.info.IMG;
import com.foobnix.pdf.info.OutlineHelper;
import com.foobnix.pdf.info.PageUrl;
import com.foobnix.pdf.info.R;
import com.foobnix.pdf.info.TintUtil;
import com.foobnix.model.AppAnnotation;
import com.foobnix.pdf.info.BookmarksData;
import com.foobnix.pdf.info.AnnotationsData;
import com.foobnix.pdf.info.AnnotationsFinder;
import com.foobnix.pdf.info.Engines;
import org.ebookdroid.BookType;
import com.foobnix.pdf.info.model.AnnotationType;
import com.foobnix.pdf.info.model.OutlineLinkWrapper;
import com.foobnix.pdf.info.view.AlertDialogs;
import com.foobnix.pdf.info.view.Dialogs;
import com.foobnix.pdf.info.view.MyPopupMenu;
import com.foobnix.sys.ImageExtractor;
import com.foobnix.sys.TempHolder;
import com.foobnix.tts.TTSEngine;
import com.foobnix.ui2.AppDB;
import com.foobnix.pdf.search.activity.HorizontalViewActivity;
import com.foobnix.ui2.MainTabs2;

import org.ebookdroid.common.settings.SettingsManager;
import org.ebookdroid.common.settings.books.SharedBooks;
import org.ebookdroid.core.codec.Annotation;
import org.ebookdroid.ui.viewer.VerticalViewActivity;
import org.ebookdroid.core.codec.CodecDocument;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import org.ebookdroid.core.codec.PageLink;

import java.io.File;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public abstract class DocumentController {

    public static final String EXTRA_PASSWORD = "password";
    public static final String EXTRA_PERCENT = "p";
    public static final String EXTRA_BOOKMARK_TEXT = "bt"; // with EXTRA_PERCENT, finds the bookmark page by the text
    public static final String EXTRA_BOOKMARK_PAGE_TEXT = "bpt";
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
            R.string.portrait_180);

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
                if (Apps.isDestroyedActivity(activity)) {
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
        TTSEngine.get().mp3Destroy();
        resetReadTimer();
        LOG.d("readTimeStart");
    }

    public Drawable getDrawable(int res) {
        return ContextCompat.getDrawable(activity, res);
    }

    public int getColor(int res) {
        return ContextCompat.getColor(activity, res);
    }

    public void resetReadTimer() {
        readTimeStart = System.currentTimeMillis();
        LOG.d("resetReadTimer");
    }

    public static int getRotationText() {
        return orientationTexts.get(orientationIds.indexOf(AppState.get().orientation));
    }

    public static boolean isEinkOrMode(Context c) {
        if(c==null){
            return false;
        }
        return Dips.isEInk() || AppState.get().appTheme == AppState.THEME_INK;

    }

    public static void turnOffButtons(final Activity a) {
        a.getWindow().getAttributes().buttonBrightness = WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_OFF;
    }

    public static void turnOnButtons(final Activity a) {
        a.getWindow().getAttributes().buttonBrightness = WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE;
    }

    /**
     * Whether the status bar and the navigation bar were last asked to go away.
     *
     * Up to Android 10 the room a hidden bar took goes on being reported, so the insets have to
     * be corrected by hand (see {@link #barsShown}). That used to be read back off the decor
     * view's system-ui flags; those are gone, and the controller has no getter before API 30, so
     * what was asked for is remembered here instead. It is only ever read on that older path.
     */
    private static boolean statusBarHidden = false;

    private static boolean navigationBarHidden = false;

    /** The controller that puts the system's bars up and down on this window. */
    private static WindowInsetsControllerCompat barsOf(final Activity a) {
        final Window window = a.getWindow();
        return WindowCompat.getInsetsController(window, window.getDecorView());
    }

    /**
     * Lays the window out behind the system's bars and puts them up or down.
     *
     * This is what the old SYSTEM_UI_FLAG_LAYOUT_* trio and FLAG_FULLSCREEN did together: the
     * page keeps the size it has whether or not the bars are showing, and the bars are drawn
     * over it. Hidden bars come back on a swipe and go again by themselves, which is what
     * SYSTEM_UI_FLAG_IMMERSIVE_STICKY meant.
     */
    private static void applyBars(final Activity a, final boolean hideStatus, final boolean hideNavigation) {
        final Window window = a.getWindow();
        WindowCompat.setDecorFitsSystemWindows(window, false);
        final WindowInsetsControllerCompat bars = barsOf(a);
        bars.setSystemBarsBehavior(
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
        if (hideStatus) {
            bars.hide(WindowInsetsCompat.Type.statusBars());
        } else {
            bars.show(WindowInsetsCompat.Type.statusBars());
        }
        if (hideNavigation) {
            bars.hide(WindowInsetsCompat.Type.navigationBars());
        } else {
            bars.show(WindowInsetsCompat.Type.navigationBars());
        }
        statusBarHidden = hideStatus;
        navigationBarHidden = hideNavigation;
    }

    /**
     * Puts both of the system's bars away, leaving the page the size it already had. They come
     * back on a swipe and go again by themselves.
     */
    public static void hideSystemBars(final Activity a) {
        applyBars(a, true, true);
    }

    public static void runFullScreen(final Activity a) {
        try {
            // The status bar goes; the navigation bar is Keyboards.hideNavigation's to decide,
            // which it does on a delay so a panel closing does not flash it back up.
            applyBars(a, true, navigationBarHidden);

            Keyboards.hideNavigation(a);

        } catch (Exception e) {
            LOG.e(e);
        }
    }

    /**
     * The window always reaches into the camera's cut-out (EdgeToEdge sets that up); whether
     * the page follows it there is the page's own affair - see keptClearOf.
     */
    public static void runFullScreenCutOut(final Activity a) {
        try {
            // The cut-out itself is EdgeToEdge's doing and holds whatever the bars are up to;
            // what this mode adds over the plain one is only how far the page is let out, which
            // keptClearOf works out from the insets.
            applyBars(a, true, navigationBarHidden);

            Keyboards.hideNavigation(a);

        } catch (Exception e) {
            LOG.e(e);
        }
    }

    /** The two screens a book is read on, whose panels come and go over the page. */
    public static boolean isReader(final Activity a) {
        return a instanceof HorizontalViewActivity || a instanceof VerticalViewActivity;
    }

    /**
     * In a full screen mode the system's bars come up with the reader's panels and go with
     * them: they lie over the page, which keeps the size it has without them, and the panels
     * reach in under them in their own colour.
     */
    public static boolean isBarsWithPanels(final Activity a) {
        return isReader(a) && AppState.get().fullScreenMode != AppState.FULL_SCREEN_NORMAL && AppState.get().isEditMode;
    }

    private static void runBarsOverPanels(final Activity a) {
        try {
            // The bars come up with the panels and lie over the page, which keeps its size.
            applyBars(a, false, false);
        } catch (Exception e) {
            LOG.e(e);
        }
    }

    /**
     * Gives the panel the room the bars take over it, on top of the padding it was laid out
     * with, so its own buttons stay clear of them while its colour runs on under them.
     */
    private static void padUnderBars(final View panel, int left, int top, int right, int bottom) {
        if (panel == null) {
            return;
        }
        Object tag = panel.getTag(R.id.basePadding);
        if (!(tag instanceof Rect)) {
            tag = new Rect(panel.getPaddingLeft(), panel.getPaddingTop(), panel.getPaddingRight(), panel.getPaddingBottom());
            panel.setTag(R.id.basePadding, tag);
        }
        Rect base = (Rect) tag;
        int l = base.left + left, t = base.top + top, r = base.right + right, b = base.bottom + bottom;
        if (panel.getPaddingLeft() != l || panel.getPaddingTop() != t || panel.getPaddingRight() != r || panel.getPaddingBottom() != b) {
            panel.setPadding(l, t, r, b);
        }
    }

    public static void runNormalScreen(final Activity a) {
        try {
            // The bars come back and the page goes on running under them.
            applyBars(a, false, false);
        } catch (Exception e) {
            LOG.e(e);
        }
    }

    public static void chooseFullScreen(final Activity a, final int mode) {

        if (mode != AppState.FULL_SCREEN_NORMAL && isBarsWithPanels(a)) {
            runBarsOverPanels(a);
        } else if (mode == AppState.FULL_SCREEN_FULLSCREEN) {
            runFullScreen(a);
        } else if (mode == AppState.FULL_SCREEN_NORMAL) {
            runNormalScreen(a);
        } else if (mode == AppState.FULL_SCREEN_FULLSCREEN_CUTOUT) {
            runFullScreenCutOut(a);
        }
        // Between the two modes with the bars away nothing the system reports changes, only
        // whether the page keeps clear of the camera, so the page is asked to look again.
        ViewCompat.requestApplyInsets(a.getWindow().getDecorView());

        try {
            View bookName = a.findViewById(R.id.bookName);
            if (bookName == null) {
                return;
            }
            View imageToolBar = a.findViewById(R.id.imageToolbar);

            ViewGroup.MarginLayoutParams lp1 = (ViewGroup.MarginLayoutParams) bookName.getLayoutParams();
            if (lp1 == null) {
                return;
            }
            ViewGroup.MarginLayoutParams lp2 = (ViewGroup.MarginLayoutParams) imageToolBar.getLayoutParams();

            if (mode == AppState.FULL_SCREEN_FULLSCREEN) {
                lp2.rightMargin = lp1.leftMargin = Dips.dpToPx(10);
            } else if (mode == AppState.FULL_SCREEN_NORMAL) {
                lp2.rightMargin = lp1.leftMargin = Dips.dpToPx(4);
            } else if (mode == AppState.FULL_SCREEN_FULLSCREEN_CUTOUT) {
                lp2.rightMargin = lp1.leftMargin = Dips.dpToPx(15);
            }
            bookName.setLayoutParams(lp1);
            imageToolBar.setLayoutParams(lp2);
        } catch (Exception e) {
            LOG.e(e);
        }

    }

    public static String getFullScreenName(final Activity a, final int mode) {
        switch (mode) {
            case AppState.FULL_SCREEN_FULLSCREEN_CUTOUT:
                return a.getString(R.string.with_cutout);
            case AppState.FULL_SCREEN_NORMAL:
                return a.getString(R.string.normal_screen);
            case AppState.FULL_SCREEN_FULLSCREEN:
                return a.getString(R.string.full_screen);
        }
        return "-";
    }

    public static int getFullScreenIcon(final Activity a, final int mode) {
        switch (mode) {
            case AppState.FULL_SCREEN_FULLSCREEN_CUTOUT:
                return R.drawable.glyphicons_215_fullscreen_off;
            case AppState.FULL_SCREEN_NORMAL:
                return R.drawable.glyphicons_216_fullscreen;
            case AppState.FULL_SCREEN_FULLSCREEN:
                return R.drawable.glyphicons_215_fullscreen_off;
        }
        return R.drawable.glyphicons_216_fullscreen;
    }

    /**
     * The mode a tap on the full screen button moves to: out of normal into full screen, and
     * out of anything else back to normal. The button asked which of three to use on every
     * press, two of which differ only on a device with a notch; the third is still there on a
     * long press.
     */
    public static int nextFullScreenMode(int currentMode) {
        return currentMode == AppState.FULL_SCREEN_NORMAL ? AppState.FULL_SCREEN_FULLSCREEN :
                AppState.FULL_SCREEN_NORMAL;
    }

    public static void showFullScreenPopup(Activity a, View v, IntegerResponse response, int currentMode) {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P && a.getWindow()
                                                               .getDecorView()
                                                               .getRootWindowInsets()
                                                               .getDisplayCutout() != null) {
            List<Integer>
                    ids =
                    Arrays.asList(AppState.FULL_SCREEN_FULLSCREEN_CUTOUT, AppState.FULL_SCREEN_FULLSCREEN, AppState.FULL_SCREEN_NORMAL);

            MyPopupMenu popup = new MyPopupMenu(a, v);
            for (int id : ids) {

                if (Build.VERSION.SDK_INT >= 35 && id == AppState.FULL_SCREEN_FULLSCREEN_CUTOUT) {
                    continue;
                }

                if (id == AppState.FULL_SCREEN_FULLSCREEN_CUTOUT && Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    LOG.d("getDisplayCutout", a.getWindow().getDecorView().getRootWindowInsets().getDisplayCutout());
                    // if (getActivity().getWindow().getDecorView().getRootWindowInsets().getDisplayCutout() != null) {
                    popup.getMenu()
                         .add(DocumentController.getFullScreenName(a, id))
                         .setOnMenuItemClickListener(item -> {
                             response.onResultRecive(id);
                             return false;
                         });
                    //}
                } else {
                    popup.getMenu()
                         .add(DocumentController.getFullScreenName(a, id))
                         .setOnMenuItemClickListener(item -> {
                             response.onResultRecive(id);
                             return false;
                         });

                }
                popup.show();
            }
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

    public static void applyEdgeToEdge(final Activity a) {
        applyEdgeToEdge(a, false, TintUtil.color);
    }

    public static void applyEdgeToEdge(final Activity a, final boolean underStatusBar) {
        applyEdgeToEdge(a, underStatusBar, TintUtil.color);
    }

    /**
     * The page is laid out under the system's bars on every version, not only from Android 15
     * on where the system insists on it, and keeps itself clear of them - see keptClearOf.
     *
     * @param underStatusBar the page runs up behind the status bar instead of stopping
     * below a strip of its own, so that a header floating at the top of the page and the
     * status bar over it read as one surface.
     * @param barColor what the strip kept for the status bar is painted with, where one is
     * kept at all - the chrome below it is painted in the same colour.
     */
    public static void applyEdgeToEdge(final Activity a, final boolean underStatusBar, final int barColor) {
        if (a instanceof ComponentActivity) {
            // Both bars take the app's own colour, with the light icons they always had: the
            // strip kept for the status bar is painted by the app, and the strip the buttons
            // at the foot stand on is painted here. The system's scrim would leave that strip
            // black under chrome that is not, which reads as a band cut off the screen.
            EdgeToEdge.enable((ComponentActivity) a,
                              SystemBarStyle.dark(Color.TRANSPARENT),
                              SystemBarStyle.dark(barColor));
        }
        // From Android 10 on, the style above leaves the bar clear and lets the system put its
        // own scrim over whatever shows through. The colour is set here instead, and the scrim
        // turned off, so the strip is the app's colour under buttons and under a gesture bar
        // alike rather than a dark band on one and the page on the other.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            a.getWindow().setNavigationBarColor(barColor);
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            a.getWindow().setNavigationBarContrastEnforced(false);
        }
        View parentParent = a.findViewById(R.id.parentParent);
        View statusBarHack = a.findViewById(R.id.systemBarHack);

        if (parentParent != null) {
            ViewCompat.setOnApplyWindowInsetsListener(parentParent, (v, windowInsets) -> {
                Insets insets = keptClearOf(a, windowInsets);

                ViewGroup.MarginLayoutParams mlp = (ViewGroup.MarginLayoutParams) v.getLayoutParams();
                // Where the chrome floats it carries the bars itself, and the page runs
                // the whole height of the screen - under the buttons at the foot as well
                // as under the clock at the head.
                int bottom = underStatusBar ? 0 : insets.bottom;
                if (mlp.leftMargin != insets.left || mlp.rightMargin != insets.right || mlp.bottomMargin != bottom) {
                    mlp.leftMargin = insets.left;
                    mlp.bottomMargin = bottom;
                    mlp.rightMargin = insets.right;
                    v.setLayoutParams(mlp);
                }

                if (statusBarHack != null) {
                    ViewGroup.LayoutParams statusBarHackLayoutParams = statusBarHack.getLayoutParams();
                    int top = underStatusBar ? 0 : insets.top;
                    if (statusBarHackLayoutParams.height != top) {
                        statusBarHackLayoutParams.height = top;
                        statusBarHack.setLayoutParams(statusBarHackLayoutParams);
                    }
                    // With the status bar put away, all the strip still keeps clear of is the
                    // camera, and that is boxed off in black the way the system used to do it.
                    statusBarHack.setBackgroundColor(barsShown(a, windowInsets).top > 0 ? barColor : Color.BLACK);
                }

                if (isReader(a)) {
                    // What of the bars lies over the page rather than beside it: the panels at
                    // the head and the foot carry it, in their own colour.
                    Insets over = Insets.max(Insets.subtract(barsShown(a, windowInsets), insets), Insets.NONE);
                    View top = a.findViewById(R.id.actionBar);
                    if (top == null) {
                        top = a.findViewById(R.id.titleBar);
                    }
                    View bottomPanel = a.findViewById(R.id.bottomBar1);
                    if (bottomPanel == null) {
                        bottomPanel = a.findViewById(R.id.bottomBar);
                    }
                    padUnderBars(top, over.left, over.top, over.right, 0);
                    padUnderBars(bottomPanel, over.left, 0, over.right, over.bottom);

                    // Nothing over the buttons at the foot while the panel is under them, so
                    // they stand on the panel's own colour; once the panel goes, the strip is
                    // painted in the app's colour rather than left to the system's scrim.
                    boolean overPanel = over.bottom > 0 || over.left > 0 || over.right > 0;
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        a.getWindow().setNavigationBarContrastEnforced(false);
                    }
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        a.getWindow().setNavigationBarColor(overPanel ? Color.TRANSPARENT : barColor);
                    }
                }

                // Only the page runs up behind the status bar. The drawer slides over
                // the page, so it starts below the bar or its own title would be read
                // through the clock.
                View drawer = a.findViewById(R.id.left_drawer);
                if (drawer != null) {
                    drawer.setPadding(drawer.getPaddingLeft(),
                                      underStatusBar ? insets.top : 0,
                                      drawer.getPaddingRight(),
                                      underStatusBar ? insets.bottom : 0);
                    // The strip it keeps is left clear: the page shows through it, and
                    // the bar's own light icons stay readable over that rather than
                    // being lost on the drawer's white ground.
                    drawer.setBackgroundColor(underStatusBar ? Color.TRANSPARENT : Color.WHITE);
                }

                return WindowInsetsCompat.CONSUMED;
            });
        }
    }

    /**
     * The room at the edges of the screen the page keeps clear of: the bars while they are
     * up, and - up to Android 14 - the camera's cut-out, the way each mode's cut-out setting
     * boxed it off there: full screen and normal screen stay clear of it, the notched mode runs
     * round it. From Android 15 on the page runs the full height of the screen in every mode,
     * camera and all, as the system itself has it there.
     */
    public static Insets keptClearOf(Activity a, WindowInsetsCompat windowInsets) {
        if (windowInsets == null) {
            return Insets.NONE;
        }
        Insets bars = barsShown(a, windowInsets);
        int mode = a instanceof MainTabs2 ? AppState.get().fullScreenMainMode : AppState.get().fullScreenMode;
        // A reader in full screen has the bars only while its panels are up, and lays them
        // over the page rather than making room - see isBarsWithPanels.
        if (mode != AppState.FULL_SCREEN_NORMAL && isReader(a) && !a.isInMultiWindowMode()) {
            bars = Insets.NONE;
        }
        if (Build.VERSION.SDK_INT >= 35 || mode == AppState.FULL_SCREEN_FULLSCREEN_CUTOUT) {
            return bars;
        }
        return Insets.max(bars, windowInsets.getInsets(WindowInsetsCompat.Type.displayCutout()));
    }

    /**
     * What the system's bars take while they are up. Up to Android 10 the room of a bar that
     * has been put away goes on being reported - the page is laid out stable - so there it is
     * the flags it was put away with that are asked. Beside another app the bars stay up.
     */
    private static Insets barsShown(Activity a, WindowInsetsCompat windowInsets) {
        Insets bars = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars());
        if (a == null || Build.VERSION.SDK_INT >= Build.VERSION_CODES.R || a.isInMultiWindowMode()) {
            return bars;
        }
        boolean statusAway = statusBarHidden;
        boolean navigationAway = navigationBarHidden;
        return Insets.of(navigationAway ? 0 : bars.left,
                         statusAway ? 0 : bars.top,
                         navigationAway ? 0 : bars.right,
                         navigationAway ? 0 : bars.bottom);
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
        ComponentName
                compName =
                new ComponentName(a.getPackageName(), "com.foobnix.zipmanager.SendReceiveActivityAlias");
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

    public abstract boolean hasPDFAnnotations();

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

    public abstract String getFootNote(String text, String chapter);

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

    /**
     * In a debuggable build, the reading mode's name in the bottom bar is followed by the engine
     * that lays the book out - [MuPDF], [foliate-js], [PDFium] - to tell at a glance which one is
     * at work, as the engine asked for can fall back to MuPDF. Once the book is open, and again
     * on every update, so the name is kept whatever rewrote it meanwhile.
     */
    public static void showEngineInDebug(TextView modeName, DocumentController dc) {
        if (modeName == null || dc == null || dc.getActivity() == null
                || (dc.getActivity().getApplicationInfo().flags & ApplicationInfo.FLAG_DEBUGGABLE) == 0) {
            return;
        }
        CodecDocument doc = dc.getCodecDocument();
        if (doc == null) {
            return;
        }
        String engine;
        if (doc instanceof FoliateDocument) {
            engine = "foliate-js";
        } else if (doc instanceof PdfiumDocument) {
            engine = "PDFium";
        } else if (doc instanceof DjvuDocument) {
            engine = "DjVu";
        } else if (doc instanceof ComicDocument) {
            engine = "Comic";
        } else {
            engine = "MuPDF";
        }
        String text = modeName.getText().toString().replaceAll(" \\[[^\\]]*\\]$", "");
        modeName.setText(text + " [" + engine + "]");
    }

    public CodecDocument getCodecDocument() {
        return null;
    }

    public String getBookmarkText() {
        try {
            CodecDocument doc = getCodecDocument();
            return doc != null ? doc.getBookmarkText(getCurentPageFirst1()) : null;
        } catch (Exception e) {
            LOG.e(e);
            return null;
        }
    }

    /** The exact place the reader is, when the engine can name one; null otherwise. */
    public String getLocation() {
        try {
            CodecDocument doc = getCodecDocument();
            return doc != null ? doc.getLocation(getCurentPageFirst1()) : null;
        } catch (Exception e) {
            LOG.e(e);
            return null;
        }
    }

    /**
     * The page a saved location names, or -1.
     *
     * A location written by one engine means nothing to another - a CFI from foliate-js is not
     * something MuPDF can resolve - and the engine says so rather than guessing, which is why the
     * percentage is always saved beside it.
     */
    public int findLocationPage(String location) {
        try {
            CodecDocument doc = getCodecDocument();
            if (doc != null && TxtUtils.isNotEmpty(location)) {
                return doc.findLocationPage(location);
            }
        } catch (Exception e) {
            LOG.e(e);
        }
        return -1;
    }

    /**
     * Whether a mark on this book belongs inside the document itself.
     *
     * A PDF has carried its own annotations since long before this, and keeps doing so - another
     * reader opening the file sees them. Everything else has nowhere inside the file to put one,
     * so Librera keeps it: see {@link com.foobnix.model.AppAnnotation}. Two PDFs fall back to
     * Librera's store as well - one in a build with no MuPDF to write the annotation with, and
     * one that is encrypted, which is not a file to be rewriting.
     */
    public boolean isAnnotationsInsideDocument() {
        try {
            return Engines.IS_MUPDF
                    && BookType.PDF.is(getCurrentBook().getPath())
                    && !isPasswordProtected();
        } catch (Exception e) {
            LOG.e(e);
            return false;
        }
    }

    /**
     * Stores a mark on the words the reader selected, in Librera's own store.
     *
     * The anchor depends on what the engine laying this book out can promise - see
     * {@link com.foobnix.model.AppAnnotation}. Asking for one means asking the engine, so this
     * belongs off the UI thread; the caller already has a thread for it.
     *
     * @param page0 the page the selection is on, 0-based
     * @param rects the selected rectangles, each normalized to the page
     */
    public AppAnnotation createAnnotation(final int page0, final List<RectF> rects, final String text,
                                          final AnnotationType type, final String color) {
        if (rects == null || rects.isEmpty()) {
            return null;
        }
        final AppAnnotation annotation = new AppAnnotation(getCurrentBook().getPath());
        annotation.setType(type);
        annotation.color = color;
        annotation.text = TxtUtils.isNotEmpty(text) ? text.trim() : "";
        annotation.page = page0;
        annotation.p = getPercentage();
        if (isTextFormat()) {
            // Reflowable: the rectangles mean nothing at another font size, so the characters
            // have to be named instead - exactly if the engine can, by the page's text if not.
            final CodecDocument doc = getCodecDocument();
            final String cfi = doc != null ? doc.getLocation(page0 + 1, rects) : null;
            if (TxtUtils.isNotEmpty(cfi)) {
                annotation.cfi = cfi;
            } else if (doc != null) {
                annotation.pt = TxtUtils.nullToEmpty(doc.getBookmarkText(page0 + 1));
            }
        } else {
            annotation.rects = AnnotationsFinder.write(rects);
        }
        AnnotationsData.get().add(annotation);
        AnnotationsFinder.invalidate();
        LOG.d("createAnnotation", annotation);
        return annotation;
    }

    // page is estimated by percent, returns the nearest page (+-10) with the text, else with the page text
    // saved at the position (finds the page after re-layout, font size change), else the page
    public int findBookmarkPage(int page, String text, String pageText) {
        return findBookmarkPage(page, text, pageText, null);
    }

    /**
     * The same, given an exact place to try first.
     *
     * The exact place wins when the engine understands it; otherwise this is what it always was -
     * the page the percentage points at, corrected by searching the saved page text around it.
     */
    public int findBookmarkPage(int page, String text, String pageText, String location) {
        final int exact = findLocationPage(location);
        if (exact > 0) {
            LOG.d("findBookmarkPage by location", exact, "instead of", page);
            return exact;
        }
        try {
            CodecDocument doc = getCodecDocument();
            if (doc != null && (TxtUtils.isNotEmpty(text) || TxtUtils.isNotEmpty(pageText))) {
                int found = doc.findBookmarkPage(page, text, pageText);
                if (found > 0) {
                    return found;
                }
            }
        } catch (Exception e) {
            LOG.e(e);
        }
        return page;
    }

    public int getBookmarkPage(AppBookmark bookmark) {
        String text = bookmark.text;
        if (getString(R.string.fast_bookmark).equals(text)) {
            text = null; // a label, not text of the page
        }
        return findBookmarkPage(bookmark.getPage(getPageCount()), text, bookmark.pt);
    }

    // the book is opened on a bookmark from the library bookmarks, see ExtUtils.showDocumentWithoutDialog2
    public int getBookmarkPage(float percent, String text, String pageText) {
        AppBookmark bookmark = new AppBookmark();
        bookmark.p = percent;
        bookmark.text = text;
        bookmark.pt = pageText;
        return getBookmarkPage(bookmark);
    }

    public abstract void updateRendering();

    public void goToPageByTTS() {
        try {
            if (TTSEngine.get().isPlaying()) {

                AppBook bs = SettingsManager.refreshBookSettings(getCurrentBook().getPath());

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

    AlertDialog alertDialog;

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
        // int page = PageUrl.fakeToReal(currentPage);
        LOG.d("_PAGE", "saveCurrentPage", getCurentPageFirst1(), getPageCount());
        try {
            if (getPageCount() <= 0) {
                LOG.d("_PAGE", "saveCurrentPage skip");
                return;
            }
            AppBook bs = SettingsManager.getBookSettings();
            bs.updateFromAppState();
            if (bs.pt == null && bs.getCurrentPage(getPageCount()).viewIndex + 1 == getCurentPageFirst1()) {
                // foliate answers only off the main thread, which is where this save runs: there
                // the page text and the place are read on a thread of their own and written after
                if (getCodecDocument() instanceof FoliateDocument && Looper.myLooper() == Looper.getMainLooper()) {
                    savePlaceInBackground(bs, getCurentPageFirst1());
                } else {
                    bs.pt = getBookmarkText();
                    // The exact place, when the engine can name one. It is written beside the
                    // percentage and the page text, never instead of them: another engine, an older
                    // version or another device has to be able to restore the position without it.
                    bs.cfi = getLocation();
                }
            }
            SharedBooks.save(bs);
            // The row the shelves are drawn from is brought up to the file straight away, so a
            // book closed shows where it was left without waiting for the shelf to be scanned.
            SharedBooks.syncToDb(bs.path);

            //AppBook bs = SettingsManager.getBookSettings(getCurrentBook().getPath());
            //bs.updateFromAppState();
            //bs.currentPageChanged(getCurentPageFirst1(), getPageCount());
            //SharedBooks.save(bs);
        } catch (Exception e) {
            LOG.e(e);
        }

    }

    private static final long PLACE_READ_DELAY_MS = 2000;
    private static final ScheduledExecutorService placeReader =
            Executors.newSingleThreadScheduledExecutor(r -> new Thread(r, "@T PlaceRead"));
    private ScheduledFuture<?> placeRead;

    // Read once the page has stood still: a turn, and the next page drawn ahead of it, need the
    // same renderer and are not kept waiting on this. A newer page cancels the read still to come.
    private void savePlaceInBackground(final AppBook bs, final int page) {
        final float p = bs.p;
        if (placeRead != null) {
            placeRead.cancel(false);
        }
        placeRead = placeReader.schedule(() -> {
            try {
                final CodecDocument doc = getCodecDocument();
                if (doc == null || bs.p != p || bs.pt != null) {
                    return;
                }
                final String pt = doc.getBookmarkText(page);
                final String cfi = doc.getLocation(page);
                // the page turned meanwhile: currentPageChanged has cleared the place for the new one
                if (pt == null || bs.p != p || bs.pt != null) {
                    return;
                }
                bs.pt = pt;
                bs.cfi = cfi;
                SharedBooks.save(bs);
            } catch (Exception e) {
                LOG.e(e);
            }
        }, PLACE_READ_DELAY_MS, TimeUnit.MILLISECONDS);
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

    /**
     * Writes the position the moment the reader leaves the screen - locked, sent to the
     * background, or closed.
     *
     * Nothing was written on pause: a page turned only queued a write a second later, so a
     * phone locked straight after a page turn kept the new page only in memory, for a process
     * the system may kill in the background before it ever comes back to the front.
     */
    public void saveCurrentPageOnPause() {
        handler2.removeCallbacks(saveCurrentPageRunnable);
        if (activity.isFinishing()) {
            // The document may already be closed, so its text cannot be asked for: what is in
            // memory - the page, kept up to date on every page turn - is what is written.
            SharedBooks.save(SettingsManager.getBookSettings());
        } else {
            saveCurrentPageAsync();
        }
    }

    public void onResume() {
        resetReadTimer();

        try {
            if (getPageCount() != 0) {
                AppBook bs = SettingsManager.refreshBookSettings(getCurrentBook().getPath());
                int page = bs.getCurrentPage(getPageCount()).viewIndex + 1;
                // Both pages counted from 1. The book mode's getCurentPage() counts from 0, so it
                // never equalled the saved page, and every return to the front searched the
                // page text and turned the page, on the main thread, even with nothing to do.
                if (getCurentPageFirst1() != page) {
                    page = findBookmarkPage(page, null, bs.pt, bs.cfi);
                    if (getCurentPageFirst1() != page) {
                        onGoToPage(page);
                    }
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

    public Boolean isEpub3;

    public String getCurrentChapter() {
        return OutlineHelper.getCurrentChapterAsString(this);
    }

    public String getCurrentChapterFile() {
        return OutlineHelper.getCurrentChapterFile(this);
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
                if (activity.findViewById(R.id.closePopup) != null) {
                    activity.findViewById(R.id.closePopup).performClick();
                }
                anchor.setVisibility(View.GONE);
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

    public void addRecent(final String path) {
        AppDB.get().addRecent(path);
        // BookmarksData.get().addRecent(uri);
    }

    public void onClickTop() {

    }

    public String getString(int resId) {
        try {
            return activity.getString(resId);
        }catch (Exception e){
            return  "";
        }
    }

    public void onLinkHistory() {
        if (!getLinkHistory().isEmpty()) {
            final int last = getLinkHistory().removeLast();
            onScrollY(last);
            LOG.d("onLinkHistory", last);

        }
    }

    public void restartActivity() {
        //IMG.clearMemoryCache();
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
        AppBook bs = SettingsManager.getBookSettings();
        bs.updateFromAppState();
        SharedBooks.save(bs);

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
        if (TxtUtils.isNotEmpty(annotation.text)) {
            Dialogs.showTextDialog(activity, annotation.text);
        }
        //Toast.makeText(activity, "" + annotation.text, Toast.LENGTH_SHORT).show();
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

    /** Removes one of Librera's own marks, the way tapping a PDF annotation removes that. */
    public void removeAnnotation(final AppAnnotation annotation) {
        if (annotation == null) {
            return;
        }
        AnnotationsData.get().remove(annotation);
        AnnotationsFinder.invalidate();
        LOG.d("removeAnnotation", annotation);
    }

    public void showEditDialogIfNeed() {
        ui.showEditDialogIfNeed();
    }

    public void onLongPress(MotionEvent ev) {
        ui.onLongPress(ev);
    }

    public abstract void doSearch(String text, ResultResponse<Integer> result, int firstPage, int lastPage);

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
        AppSP.get().lastBookPath = currentBook.getPath();
        TempHolder.get().loadingCancelled.set(false);
        LOG.d("setCurrentBook",currentBook);
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

    public boolean checkReadingTimer() {
        long timeout = System.currentTimeMillis() - readTimeStart;
        LOG.d("readTimeStart  checkReadingTimer", timeout, TimeUnit.MINUTES.toMillis(AppState.get().remindRestTime));
        boolean
                needToRest =
                AppState.get().remindRestTime != -1 && timeout >= TimeUnit.MINUTES.toMillis(AppState.get().remindRestTime);
        if (alertDialog != null && alertDialog.isShowing()) {
            return needToRest;
        }
        if (needToRest) {
            alertDialog =
                    AlertDialogs.showOkDialog(activity, getString(R.string.remind_msg), R.string.read_a_book, () -> {
                        LOG.d("checkReadingTimer action");
                        resetReadTimer();
                    }, () -> {
                        LOG.d("checkReadingTimer dismiss");
                    });
        }
        return needToRest;

    }

}
