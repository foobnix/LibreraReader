package com.foobnix.ui2;

import static com.foobnix.pdf.info.Android6.MY_PERMISSIONS_REQUEST_WES;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.SearchManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Color;
import android.net.Uri;
import android.content.UriPermission;
import android.os.Build;
import android.provider.DocumentsContract;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.drawerlayout.widget.DrawerLayout.DrawerListener;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import androidx.viewpager.widget.ViewPager;
import androidx.viewpager.widget.ViewPager.OnPageChangeListener;

import com.cloudrail.si.CloudRail;
import com.foobnix.android.utils.Apps;
import com.foobnix.android.utils.Dips;
import com.foobnix.android.utils.JsonDB;
import com.foobnix.android.utils.LOG;
import com.foobnix.android.utils.Safe;
import com.foobnix.android.utils.StringDB;
import com.foobnix.android.utils.TxtUtils;
import com.foobnix.drive.GFile;
import com.foobnix.drive.GoogleAuth;
import com.foobnix.ext.CacheZipUtils.CacheDir;
import com.foobnix.model.AppBook;
import androidx.fragment.app.Fragment;

import com.foobnix.model.AppProfile;
import com.foobnix.pdf.info.widget.PrefDialogs;
import com.foobnix.model.AppSP;
import com.foobnix.model.AppState;
import com.foobnix.model.LibreraSafSync;
import com.foobnix.pdf.SlidingTabLayout;
import com.foobnix.pdf.info.ADS;
import com.foobnix.pdf.info.Android6;
import com.foobnix.pdf.info.ExtUtils;
import com.foobnix.pdf.info.AppsConfig;
import com.foobnix.pdf.info.Clouds;
import com.foobnix.pdf.info.IMG;
import com.foobnix.pdf.info.PasswordDialog;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import com.foobnix.pdf.info.R;
import com.foobnix.pdf.info.view.AlertDialogs;
import com.foobnix.pdf.info.TintUtil;
import com.foobnix.pdf.info.model.BookCSS;
import com.foobnix.pdf.info.view.BrightnessHelper;
import com.foobnix.pdf.info.view.Dialogs;
import com.foobnix.pdf.info.view.MyProgressBar;
import com.foobnix.pdf.info.wrapper.DocumentController;
import com.foobnix.pdf.info.wrapper.UITab;
import com.foobnix.pdf.search.activity.HorizontalViewActivity;
import com.foobnix.pdf.search.activity.msg.GDriveSycnEvent;
import com.foobnix.pdf.search.activity.msg.MessageSync;
import com.foobnix.pdf.search.activity.msg.MessegeBrightness;
import com.foobnix.pdf.search.activity.msg.MsgCloseMainTabs;
import com.foobnix.pdf.search.activity.msg.SearchMetaMsg;
import com.foobnix.pdf.search.view.CloseAppDialog;
import com.foobnix.sys.TempHolder;
import com.foobnix.ui2.adapter.TabsAdapter2;
import com.foobnix.ui2.fragment.BookmarksFragment2;
import com.foobnix.ui2.fragment.BrowseFragment2;
import com.foobnix.ui2.fragment.OpdsFragment2;
import com.foobnix.ui2.fragment.PrefFragment2;
import com.foobnix.ui2.fragment.RecentFragment2;
import com.foobnix.ui2.fragment.SearchFragment2;
import com.foobnix.ui2.fragment.UIFragment;
import com.foobnix.work.SearchAllBooksWorker;
import com.google.android.ump.ConsentDebugSettings;
import com.google.android.ump.ConsentForm;
import com.google.android.ump.ConsentInformation;
import com.google.android.ump.ConsentRequestParameters;
import com.google.android.ump.FormError;
import com.google.android.ump.UserMessagingPlatform;

import org.ebookdroid.common.settings.books.SharedBooks;
import org.ebookdroid.ui.viewer.VerticalViewActivity;
import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.HashSet;

import mobi.librera.libgooglepro.RefiewForm;

@SuppressLint("NewApi")
public class MainTabs2 extends AdsFragmentActivity {
    public static final int REQUEST_CODE_ADD_RESOURCE = 123;
    public static final int REQUEST_CODE_ADD_LIBRARY_STORAGE = 124;
    public static final String EXTRA_EXIT = "EXTRA_EXIT";
    public static final String EXTRA_SHOW_TABS = "EXTRA_SHOW_TABS";
    private static final String TAG = "MainTabs";
    public static String EXTRA_PAGE_NUMBER = "EXTRA_PAGE_NUMBER";
    public static String EXTRA_SEACH_TEXT = "EXTRA_SEACH_TEXT";
    public static String EXTRA_NOTIFY_REFRESH = "EXTRA_NOTIFY_REFRESH";
    public boolean isEink = false;
    ViewPager pager;
    List<UIFragment> tabFragments;
    TabsAdapter2 adapter;
    ImageView imageMenu;
    View imageMenuParent, overlay;
    TextView toastBrightnessText, onSubscribe;
    Handler handler;
    SwipeRefreshLayout swipeRefreshLayout;
    boolean isMyKey = false;
    OnPageChangeListener onPageChangeListener = new OnPageChangeListener() {
        UIFragment uiFragment = null;

        @Override
        public void onPageSelected(int pos) {
            uiFragment = tabFragments.get(pos);
            uiFragment.onSelectFragment();
            TempHolder.get().currentTab = pos;

            LOG.d("onPageSelected", uiFragment);
            Apps.accessibilityText(MainTabs2.this, adapter.getPageTitle(pos)
                                                          .toString() + " " + getString(R.string.tab_selected));
        }

        @Override
        public void onPageScrolled(int arg0, float arg1, int arg2) {

        }

        @Override
        public void onPageScrollStateChanged(int state) {
            if (isPullToRefreshEnable()) {
                swipeRefreshLayout.setEnabled(state == ViewPager.SCROLL_STATE_IDLE);
            }
            LOG.d("onPageSelected onPageScrollStateChanged", state);
            if (state == ViewPager.SCROLL_STATE_IDLE) {
                check();
            }
        }

        public void check() {
            if (isPullToRefreshEnable()) {
                swipeRefreshLayout.setEnabled(!(uiFragment instanceof PrefFragment2));
            }
        }
    };
    Runnable closeActivityRunnable = new Runnable() {
        @Override
        public void run() {
            if (drawerLayout != null) {
                if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
                    onDestroyBanner();
                    finish();
                } else {
                    drawerLayout.openDrawer(GravityCompat.START, AppState.get().appTheme != AppState.THEME_INK);
                }

            }
        }
    };
    boolean once = true;
    private SlidingTabLayout indicator;
    private DrawerLayout drawerLayout;

    public static boolean isPullToRefreshEnable(Context a, View swipeRefreshLayout) {
        if (a == null || swipeRefreshLayout == null) {
            return false;
        }
        return AppSP.get().isEnableSync && GoogleAuth.isAuthorized(a) && BookCSS.get().isSyncPullToRefresh;
    }

    public static void startActivity(Activity c, int tab) {
        final Intent intent = new Intent(c, MainTabs2.class);
        intent.putExtra(MainTabs2.EXTRA_SHOW_TABS, true);
        intent.putExtra(MainTabs2.EXTRA_PAGE_NUMBER, tab);
        intent.putExtra(PasswordDialog.EXTRA_APP_PASSWORD, c.getIntent()
                                                            .getStringExtra(PasswordDialog.EXTRA_APP_PASSWORD));
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        c.startActivity(intent);
        c.overridePendingTransition(0, 0);
    }

    public static void closeApp(Context c) {
        if (c == null) {
            return;
        }
        EventBus.getDefault().post(new MsgCloseMainTabs());
    }

    @Override
    protected void onNewIntent(final Intent intent) {
        LOG.d(TAG, "onNewIntent");
        // testIntentHandler();
        if (intent.getBooleanExtra(EXTRA_EXIT, false)) {
            finish();
            return;
        }
        if (intent.getCategories() != null && intent.getCategories().contains("android.intent.category.BROWSABLE")) {
            CloudRail.setAuthenticationResponse(intent);
            LOG.d("CloudRail response", intent);

            Intent intent1 = new Intent(UIFragment.INTENT_TINT_CHANGE)//
                                                                      .putExtra(MainTabs2.EXTRA_PAGE_NUMBER, UITab.getCurrentTabIndex(UITab.BrowseFragment));//

            LocalBroadcastManager.getInstance(this).sendBroadcast(intent1);
        }

        checkGoToPage(intent);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults,
                                           int deviceId) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults, deviceId);
        if (requestCode == MY_PERMISSIONS_REQUEST_WES) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                LOG.d("PermissionGranted","Granted");
                onStoragePermissionGranted();
                SearchAllBooksWorker.run(this);
            }else{
                LOG.d("PermissionGranted","NOT Granted");
            }
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        LOG.d("onActivityResult", "requestCode:",requestCode,"resultCode:",resultCode,data);
        if(requestCode==MY_PERMISSIONS_REQUEST_WES){
            AppSP.get().currentProfile="";
            AppSP.get().save();
            AppProfile.init(this);
            onStoragePermissionGranted();
            SearchAllBooksWorker.run(this);
        }
        if (Android6.isNeedToGrantAccess(this, requestCode)) {
            Toast.makeText(this, R.string.you_need_grant_permission5, Toast.LENGTH_LONG).show();
            //Android6.checkPermissions(this, false);
            //return;

        }


        // A sign-in answers for itself, whatever the answer is: the Google consent screen can
        // come back cancelled, which the check below would otherwise swallow on the older
        // devices.
        if (GFile.onSignInActivityResult(this, requestCode, resultCode, data)) {
            swipeRefreshLayout.setEnabled(isPullToRefreshEnable());
            return;
        }
        if (LibreraSafSync.onActivityResult(this, requestCode, resultCode, data)) {
            return;
        }

        if (Build.VERSION.SDK_INT < Android6.ANDROID_12_INT && resultCode != Activity.RESULT_OK) {
            Toast.makeText(this, R.string.fail, Toast.LENGTH_SHORT).show();
            return;
        }

        if (requestCode == REQUEST_CODE_ADD_RESOURCE && resultCode == Activity.RESULT_OK) {
            getContentResolver().takePersistableUriPermission(data.getData(), Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
            Uri uri = data.getData();

            String pathSAF = uri.toString();

            StringDB.add(BookCSS.get().pathSAF, pathSAF, (db) -> BookCSS.get().pathSAF = db);

            LOG.d("REQUEST_CODE_ADD_RESOURCE", pathSAF, BookCSS.get().pathSAF);

            UIFragment uiFragment = tabFragments.get(pager.getCurrentItem());
            if (uiFragment instanceof BrowseFragment2) {
                BrowseFragment2 fr = (BrowseFragment2) uiFragment;
                fr.displayAnyPath(pathSAF);
            }
        } else if (requestCode == REQUEST_CODE_ADD_LIBRARY_STORAGE && resultCode == Activity.RESULT_OK) {
            // The folders panel, when there is one, says it has a new folder itself: it has the
            // list on screen and would otherwise go on showing the old one.
            PrefFragment2 prefs = findPrefFragment();
            if (onAddLibraryStorageResult(this, requestCode, resultCode, data, prefs == null)) {
                // A folder handed over again: its books' covers were drawn from their names
                IMG.onAccessRegained();
                if (prefs != null) {
                    prefs.onStorageFolderAdded();
                }
                // the library's notice now asks for the Librera folder instead
                updateOnGrantPanels();
                // the folders tab, already showing, is moved onto it as well
                if (!Android6.canWrite(this)) {
                    for (UIFragment fragment : tabFragments) {
                        if (fragment instanceof BrowseFragment2 && fragment.isAdded()) {
                            ((BrowseFragment2) fragment).displayAnyPath(BookCSS.get().dirLastPath);
                        }
                    }
                }
            }
        }
    }

    /**
     * Takes in a folder the user handed over through the system's picker, wherever it was asked
     * for. It is kept as a storage too, so the folders screen can reach it, and put into the
     * library paths; {@code rebuild} then says whether the library is scanned again here, which
     * the caller may want to do itself once it has caught up with the new folder.
     *
     * @return whether this was the folder picker's answer and the folder was taken in.
     */
    public static boolean onAddLibraryStorageResult(Activity activity, int requestCode, int resultCode, Intent data,
            boolean rebuild) {
        if (requestCode != REQUEST_CODE_ADD_LIBRARY_STORAGE || resultCode != Activity.RESULT_OK || data == null
                || data.getData() == null) {
            return false;
        }
        Uri uri = data.getData();
        activity.getContentResolver().takePersistableUriPermission(uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);

        String pathSAF = uri.toString();
        // the same folder handed over again - after a reinstall - is not listed twice
        if (!StringDB.asList(BookCSS.get().pathSAF).contains(pathSAF)) {
            StringDB.add(BookCSS.get().pathSAF, pathSAF, (db) -> BookCSS.get().pathSAF = db);
        }

        LOG.d("REQUEST_CODE_ADD_LIBRARY_STORAGE", pathSAF);

        // The folder is put into the library here rather than in the panel, so that it lands
        // whether or not a panel is there to hear about it. With the permission to read the
        // storage it goes in as the path it stands for, as a folder added from the file list does.
        String libraryPath = pathSAF;
        if (Android6.canWrite(activity)) {
            String file = ExtUtils.getSafTreePath(pathSAF);
            if (file != null) {
                libraryPath = file;
            }
        }
        // A folder already in the list is one whose access was given back, after a reinstall
        boolean regranted = JsonDB.get(BookCSS.get().searchPathsJson).contains(libraryPath);
        if (!regranted && !PrefDialogs.addSearchPath(activity, libraryPath)) {
            return false;
        }
        // The user has said where their books are. The storage roots the library started out
        // guessing at are not readable without the permission they chose not to give, so they
        // are taken out rather than left in the folders list showing nothing.
        if (!Android6.canWrite(activity)) {
            BookCSS.removeUnreadableStorageRoots(activity);
            // The folders screen opens on it: without the permission it is the one place with
            // books the user can browse.
            BookCSS.get().dirLastPath = pathSAF;
        }
        if (rebuild) {
            AppProfile.save(activity);
            SearchAllBooksWorker.run(activity);
        }
        return true;
    }

    /**
     * Asks the system for a folder and puts it into the library. This is the way in for books the
     * app is not allowed to go looking for itself: the user hands one folder over, and what comes
     * back is a document uri rather than a path. The answer lands in
     * {@link #onActivityResult(int, int, Intent)} under
     * {@link #REQUEST_CODE_ADD_LIBRARY_STORAGE}, whichever screen asked for it.
     */
    public static void startAddLibraryStorage(Activity activity) {
        if (activity == null) {
            return;
        }
        openLibraryStoragePicker(activity);
    }

    /**
     * The library folders, to add to and take from, and Update to rebuild the library from them -
     * the preferences panel's own dialog, for the library's top bar. Through the panel when there
     * is one, so its list of folders follows what is changed here.
     */
    public void showLibraryFolders() {
        PrefFragment2 prefs = findPrefFragment();
        if (prefs != null) {
            prefs.onFolderConfigDialog();
            return;
        }
        PrefDialogs.chooseFolderDialog(this, () -> {
            AppProfile.save(this);
            updateOnGrantPanels();
        }, () -> PrefFragment2.updateLibrary(this));
    }

    /**
     * The storage can be read now: a library left with no folder is given Download, and the
     * folders panel and the notices offering a way in are brought up to date with it. Nothing
     * happens when the permission was not given after all.
     */
    private void onStoragePermissionGranted() {
        if (Android6.canWrite(this)) {
            IMG.onAccessRegained();
        }
        if (!BookCSS.addDownloadsIfNoFolders(this)) {
            return;
        }
        PrefFragment2 prefs = findPrefFragment();
        if (prefs != null) {
            prefs.onLibraryFoldersChanged();
        }
        updateOnGrantPanels();
    }

    /** Every page's notice offering a way in for books, worked out again for the current folders. */
    public void updateOnGrantPanels() {
        if (tabFragments == null) {
            return;
        }
        for (UIFragment fragment : tabFragments) {
            if (fragment != null && fragment.isAdded()) {
                fragment.updateOnGrantPanel();
            }
        }
    }

    /** The folders already asked about while the app has been running; each is asked about once. */
    private static final Set<String> askedForLostStorage = new HashSet<>();

    /**
     * Asks again for a library folder handed over through the picker whose access is gone. Android
     * forgets every such grant when the app is reinstalled, while the library folders come back
     * with the profile from the Librera folder - so they would be listed and never read. The
     * picker opens on the folder itself; the answer comes back as any other library folder does.
     */
    public static void askForLostLibraryStorage(Activity activity) {
        if (activity == null || Android6.canWrite(activity)) {
            return;
        }
        for (String path : JsonDB.get(BookCSS.get().searchPathsJson)) {
            if (!isLostLibraryStorage(activity, path) || !askedForLostStorage.add(path)) {
                continue;
            }
            LOG.d("askForLostLibraryStorage", path);
            askToRegrantLibraryStorage(activity, path);
            return;
        }
    }

    /** Whether this is a folder handed over through the picker that the app holds no grant for. */
    public static boolean isLostLibraryStorage(Context c, String path) {
        if (c == null || !ExtUtils.isExteralSD(path)) {
            return false;
        }
        for (UriPermission p : c.getContentResolver().getPersistedUriPermissions()) {
            if (p.getUri().toString().equals(path)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Says the folder can no longer be read and offers to hand it over again: the picker opens on
     * the folder itself, and the answer comes back as any other library folder does.
     */
    public static void askToRegrantLibraryStorage(Activity activity, String path) {
        Uri tree = Uri.parse(path);
        String name;
        try {
            name = DocumentsContract.getTreeDocumentId(tree);
        } catch (Exception e) {
            name = path;
        }
        name = name.replaceFirst("^primary:", "");
        AlertDialogs.showDialog(activity, activity.getString(R.string.regrant_folder_title),
                activity.getString(R.string.regrant_folder_msg, name), activity.getString(R.string.choose_),
                () -> openLibraryStoragePicker(activity, tree), null);
    }

    /** The system's folder picker, opened in Download, where books usually land. */
    private static void openLibraryStoragePicker(Activity activity) {
        openLibraryStoragePicker(activity, null);
    }

    /** @param at the tree uri of the folder to open on, or null for Download */
    private static void openLibraryStoragePicker(Activity activity, Uri at) {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
        intent.addFlags(Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION //
                        | Intent.FLAG_GRANT_PREFIX_URI_PERMISSION //
                        | Intent.FLAG_GRANT_READ_URI_PERMISSION //
                        | Intent.FLAG_GRANT_WRITE_URI_PERMISSION //
                       );
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Uri initial = DocumentsContract.buildDocumentUri("com.android.externalstorage.documents", "primary:Download");
            if (at != null) {
                try {
                    initial = DocumentsContract.buildDocumentUri(at.getAuthority(), DocumentsContract.getTreeDocumentId(at));
                } catch (Exception e) {
                    LOG.e(e);
                }
            }
            intent.putExtra(DocumentsContract.EXTRA_INITIAL_URI, initial);
        }
        try {
            activity.startActivityForResult(intent, REQUEST_CODE_ADD_LIBRARY_STORAGE);
        } catch (Exception e) {
            LOG.e(e);
            Toast.makeText(activity, R.string.fail, Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * The preferences panel, wherever it is. It is the left drawer most of the time, and a tab of
     * its own when the user has asked for one.
     */
    private PrefFragment2 findPrefFragment() {
        try {
            Fragment drawer = getSupportFragmentManager().findFragmentById(R.id.left_drawer);
            if (drawer instanceof PrefFragment2 && drawer.isAdded()) {
                return (PrefFragment2) drawer;
            }
            for (UIFragment fragment : tabFragments) {
                if (fragment instanceof PrefFragment2 && fragment.isAdded()) {
                    return (PrefFragment2) fragment;
                }
            }
        } catch (Exception e) {
            LOG.e(e);
        }
        return null;
    }

    public boolean isPullToRefreshEnable() {
        return isPullToRefreshEnable(MainTabs2.this, swipeRefreshLayout);
    }


    public void updatePullToRefresh() {
        if (swipeRefreshLayout == null) {
            return;
        }
        boolean onPrefs = false;
        try {
            onPrefs = pager != null && tabFragments.get(pager.getCurrentItem()) instanceof PrefFragment2;
        } catch (Exception e) {
            LOG.e(e);
        }
        swipeRefreshLayout.setEnabled(isPullToRefreshEnable() && !onPrefs);
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        // testIntentHandler();

       // if (Android6.canWrite(this)) {
            BrightnessHelper.applyBrigtness(this);
            BrightnessHelper.updateOverlay(overlay);
    //    }
        GFile.runSyncService(this);
    }

    @Override
    protected void attachBaseContext(Context context) {
        super.attachBaseContext(MyContextWrapper.wrap(context));
    }

    /**
     * Keeps the floating bar clear of the system's buttons - only by as much of them as lies
     * under it: with an ad beneath, the ad stands between the two and nothing is kept.
     */
    private void placeFloatingBar() {
        if (indicator == null || !SlidingTabLayout.isFloating()) {
            return;
        }
        View holder = (View) indicator.getParent();
        WindowInsetsCompat insets = ViewCompat.getRootWindowInsets(indicator);
        int below = DocumentController.keptClearOf(this, insets).bottom;
        int[] at = new int[2];
        holder.getLocationInWindow(at);
        int holderBottom = at[1] + holder.getHeight();
        int windowBottom = indicator.getRootView().getHeight();
        int covered = Math.max(0, Math.min(below, holderBottom - (windowBottom - below)));

        ViewGroup.MarginLayoutParams lp = (ViewGroup.MarginLayoutParams) indicator.getLayoutParams();
        int margin, bottom, padding = indicator.getPaddingTop();
        if (SlidingTabLayout.isFlush()) {
            // Square, the bar sits on the screen's edges, and carries the system's buttons
            // under its own instead of leaving the page showing there.
            margin = 0;
            bottom = 0;
            padding += covered;
        } else {
            margin = Dips.DP_10;
            bottom = Dips.DP_8 + covered;
        }
        if (lp.leftMargin != margin || lp.rightMargin != margin || lp.bottomMargin != bottom) {
            lp.leftMargin = lp.rightMargin = margin;
            lp.bottomMargin = bottom;
            indicator.setLayoutParams(lp);
        }
        if (indicator.getPaddingBottom() != padding) {
            indicator.setPadding(indicator.getPaddingLeft(), indicator.getPaddingTop(),
                                 indicator.getPaddingRight(), padding);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (!isTaskRoot() && getIntent().hasCategory(Intent.CATEGORY_LAUNCHER) && Intent.ACTION_MAIN.equals(getIntent().getAction())) {
            finish();
            return;
        }

//        if (!Android6.canWrite(this) ) {
//            Android6.checkPermissions(this, true);
//            //return;
//        }

        Clouds.get().init(this);

        //import settings

        if (PasswordDialog.isNeedPasswordDialog(this)) {
            return;
        }

        LOG.d(TAG, "onCreate");

        LOG.d("EXTRA_EXIT", EXTRA_EXIT);
        if (getIntent().getBooleanExtra(EXTRA_EXIT, false)) {
            finish();
            return;
        }

        handler = new Handler(Looper.getMainLooper());
        isEink = Dips.isEInk();

        DocumentController.doRotation(this);
        DocumentController.doContextMenu(this);

        setContentView(R.layout.main_tabs);
        // With the tabs floating at the foot, the headers float at the top too: the page
        // runs the whole height of the screen and they are drawn over it.
        // The page runs the whole height of the screen either way: the chrome floats over
        // it, at the head or at the foot, and carries the status bar itself.
        DocumentController.applyEdgeToEdge(this, true,
                                           SlidingTabLayout.floatingTint(TintUtil.color));

        imageMenu = findViewById(R.id.imageMenu1);
        imageMenuParent = findViewById(R.id.imageParent1);
        imageMenuParent.setBackgroundColor(SlidingTabLayout.floatingTint(TintUtil.color));

        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout);
        swipeRefreshLayout.setColorSchemeColors(TintUtil.color);

        swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                swipeRefreshLayout.setRefreshing(false);
                GFile.runSyncService(MainTabs2.this, true);
            }
        });

        overlay = findViewById(R.id.overlay);

        toastBrightnessText = findViewById(R.id.toastBrightnessText);
        toastBrightnessText.setVisibility(View.GONE);
        TintUtil.setDrawableTint(toastBrightnessText.getCompoundDrawables()[0], Color.WHITE);

        tabFragments = new ArrayList<UIFragment>();

        try {

            for (UITab tab : UITab.getOrdered()) {
                LOG.d("getOrdered", tab, tab.isVisible());
                if (tab.isVisible()) {
                    tabFragments.add(tab.getClazz().getDeclaredConstructor().newInstance());
                }
            }
            if (tabFragments.size() == 0) {
                synchronized (AppState.get().tabsOrder9) {
                    AppState.get().tabsOrder9 = AppState.DEFAULTS_TABS_ORDER;
                }
                for (UITab tab : UITab.getOrdered()) {
                    if (tab.isVisible()) {
                        tabFragments.add(tab.getClazz().getDeclaredConstructor().newInstance());
                    }
                }
            }
        } catch (Exception e) {
            LOG.e(e);
            Toast.makeText(MainTabs2.this, R.string.msg_unexpected_error, Toast.LENGTH_LONG).show();
            tabFragments.add(new SearchFragment2());
            tabFragments.add(new BrowseFragment2());
            tabFragments.add(new RecentFragment2());
            tabFragments.add(new BookmarksFragment2());
            tabFragments.add(new OpdsFragment2());
            tabFragments.add(new PrefFragment2());
            //tabFragments.add(new CloudsFragment2());
        }
        getSupportFragmentManager().beginTransaction().replace(R.id.left_drawer, new PrefFragment2()).commit();

        drawerLayout = findViewById(R.id.drawer_layout);

        imageMenu.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
                    drawerLayout.closeDrawer(GravityCompat.START, AppState.get().appTheme != AppState.THEME_INK);
                } else {
                    drawerLayout.openDrawer(GravityCompat.START, AppState.get().appTheme != AppState.THEME_INK);
                }
            }
        });

        if (UITab.isShowLibrary() || !AppState.get().tapPositionTop) {
            imageMenu.setVisibility(View.GONE);
        } else {

            imageMenu.setVisibility(View.VISIBLE);
        }

        // ((BrigtnessDraw)
        // findViewById(R.id.brigtnessProgressView)).setActivity(this);

        adapter = new TabsAdapter2(this, tabFragments);
        pager = findViewById(R.id.pager);
        pager.setAccessibilityDelegate(new View.AccessibilityDelegate());

        //if (Android6.canWrite(this)) {
            pager.setAdapter(adapter);
       // }

        if (AppState.get().appTheme == AppState.THEME_DARK_OLED) {
            pager.setBackgroundColor(Color.BLACK);
        }

        pager.setOffscreenPageLimit(10);
        pager.addOnPageChangeListener(onPageChangeListener);


        drawerLayout.addDrawerListener(new DrawerListener() {
            @Override
            public void onDrawerStateChanged(int arg0) {
                LOG.d("drawerLayout-onDrawerStateChanged", arg0);
            }

            @Override
            public void onDrawerSlide(View arg0, float arg1) {
                LOG.d("drawerLayout-onDrawerSlide");
                if (AppSP.get().isEnableSync) {
                    swipeRefreshLayout.setEnabled(false);
                }
            }

            @Override
            public void onDrawerOpened(View arg0) {
                LOG.d("drawerLayout-onDrawerOpened");
                if (AppSP.get().isEnableSync) {
                    swipeRefreshLayout.setEnabled(false);
                }
            }

            @Override
            public void onDrawerClosed(View arg0) {
                LOG.d("drawerLayout-onDrawerClosed");
                try {
                    tabFragments.get(pager.getCurrentItem()).onSelectFragment();

                    if (isPullToRefreshEnable(MainTabs2.this, swipeRefreshLayout)) {
                        swipeRefreshLayout.setEnabled(true);
                        swipeRefreshLayout.setColorSchemeColors(TintUtil.color);
                    }
                } catch (Exception e) {
                    LOG.e(e);
                }
            }
        });

        if (AppState.get().tapPositionTop) {
            indicator = findViewById(R.id.slidingTabs1);
        } else {
            indicator = findViewById(R.id.slidingTabs2);
        }
        indicator.addSwipeRefreshLayout(swipeRefreshLayout);
        indicator.setVisibility(View.VISIBLE);
        indicator.init();

        indicator.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                LOG.d("OnFocusChangeListener", hasFocus);
            }
        });

        indicator.setViewPager(pager);

        indicator.setDividerColors(ContextCompat.getColor(this, R.color.tint_divider));
        indicator.setSelectedIndicatorColors(Color.WHITE);
        indicator.setTabsBackground(TintUtil.color);

        // The chosen tab is marked by the patch behind it, wherever the tabs sit, and the
        // rules that used to stand between them are gone with the rest of them.
        indicator.setDividerColors(Color.TRANSPARENT);
        indicator.setSelectedIndicatorColors(Color.TRANSPARENT);

        if (!AppState.get().tapPositionTop || !AppState.get().tabWithNames) {
            for (int i = 0; i < indicator.getmTabStrip().getChildCount(); i++) {
                View child = indicator.getmTabStrip().getChildAt(i);
                child.setOnLongClickListener(new View.OnLongClickListener() {
                    @Override
                    public boolean onLongClick(View v) {
                        imageMenu.performClick();
                        return true;
                    }
                });
            }
        }
        if (SlidingTabLayout.isFloating()) {
            // The bar floats clear of the system's own buttons, which the page now runs
            // under rather than stopping above.
            // Placed again whenever what holds it is laid out anew - an ad coming in under the
            // bar lifts it off the system's buttons.
            final View barHolder = (View) indicator.getParent();
            barHolder.addOnLayoutChangeListener((v, l, t, r, b, ol, ot, or, ob) -> indicator.post(this::placeFloatingBar));
            indicator.post(this::placeFloatingBar);
        }

        if (AppState.get().tapPositionTop) {
            // Standing over the page now, the strip carries the status bar itself, and is
            // lifted clear of the header the page's own tab draws beneath it.
            imageMenuParent.setElevation(Dips.DP_8);
            imageMenuParent.setOutlineProvider(null);
            imageMenuParent.post(() -> {
                WindowInsetsCompat insets = ViewCompat.getRootWindowInsets(imageMenuParent);
                int top = DocumentController.keptClearOf(MainTabs2.this, insets).top;
                imageMenuParent.setPadding(imageMenuParent.getPaddingLeft(), top,
                                           imageMenuParent.getPaddingRight(),
                                           imageMenuParent.getPaddingBottom());
            });
        }

        indicator.setOnDoubleClickAction(index -> {
            try {
                tabFragments.get(index).onDoubleClick();
            } catch (Exception e) {
                LOG.e(e);
            }

            return false;
        });

        if (AppState.get().appTheme == AppState.THEME_INK) {
            TintUtil.setTintImageNoAlpha(imageMenu, TintUtil.color);
            indicator.setSelectedIndicatorColors(TintUtil.color);
            indicator.setDividerColors(TintUtil.color);
            indicator.setTabsBackground(Color.TRANSPARENT);
            // At the foot the strip lies on the sheet and lets it show through. Standing over
            // the page at the top, the books scroll beneath it, so there it is solid white,
            // as ink paper is.
            imageMenuParent.setBackgroundColor(AppState.get().tapPositionTop ? Color.WHITE : Color.TRANSPARENT);
        }

        //Android6.checkPermissions(this, true);
        // Analytics.onStart(this);

        List<String>
                actions =
                Arrays.asList("android.intent.action.PROCESS_TEXT", "android.intent.action.SEARCH", "android.intent.action.SEND");
        List<String>
                extras =
                Arrays.asList(Intent.EXTRA_PROCESS_TEXT_READONLY, Intent.EXTRA_PROCESS_TEXT, SearchManager.QUERY, Intent.EXTRA_TEXT);
        if (getIntent() != null && getIntent().getAction() != null) {
            if (actions.contains(getIntent().getAction())) {
                for (String extra : extras) {
                    final String text = getIntent().getStringExtra(extra);
                    if (TxtUtils.isNotEmpty(text)) {
                        pager.postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                ((SearchFragment2) tabFragments.get(0)).searchAndOrderExteral(text);
                            }
                        }, 250);
                        break;
                    }
                }
            }
        }



        EventBus.getDefault().register(this);

        boolean showTabs = getIntent().getBooleanExtra(EXTRA_SHOW_TABS, false);

        if (!showTabs && AppState.get().isOpenLastBook) {
            LOG.d("Open lastBookPath", AppSP.get().lastBookPath);

            if (AppSP.get().lastBookPath == null || !new File(AppSP.get().lastBookPath).isFile()) {
                LOG.d("Open Last book not found");
                return;
            }

            try {
                AppBook book = SharedBooks.load(AppSP.get().lastBookPath);
                if (book.p > 0.9999) {
                    LOG.d("Open Last book skipped", book.p);
                    Toast.makeText(MainTabs2.this, R.string.the_book_is_complete, Toast.LENGTH_LONG).show();
                    return;
                }
            }catch (Exception e){
                LOG.e(e);
            }

            boolean isEasyMode = AppSP.get().readingMode == AppState.READING_MODE_BOOK;
            Safe.run(() -> {

                Intent
                        intent =
                        new Intent(MainTabs2.this, isEasyMode ? HorizontalViewActivity.class : VerticalViewActivity.class);
                intent.putExtra(PasswordDialog.EXTRA_APP_PASSWORD, getIntent().getStringExtra(PasswordDialog.EXTRA_APP_PASSWORD));
                intent.setData(Uri.fromFile(new File(AppSP.get().lastBookPath)));
                startActivity(intent);
            });
        }

        checkGoToPage(getIntent());

        if (!AppState.get().isEnableAccessibility && once) {
            once = false;
            handler.postDelayed(() -> {
                Apps.accessibilityText(MainTabs2.this, getString(R.string.welcome_accessibility));
            }, 5000);
        }

        try {
            //ads
            LOG.d(this, "TEST-ads-device ...");
            if (AppsConfig.isShowAdsInApp(this)) {
                ConsentRequestParameters params;

                if (AppsConfig.IS_TEST_DEVICE) {
                    ConsentDebugSettings
                            debugSettings =
                            new ConsentDebugSettings.Builder(this).setDebugGeography(ConsentDebugSettings.DebugGeography.DEBUG_GEOGRAPHY_EEA)
                                                                  .addTestDeviceHashedId(ADS.getByTestID(this))
                                                                  .build();

                    params =
                            new ConsentRequestParameters.Builder().setConsentDebugSettings(debugSettings)
                                                                  .setTagForUnderAgeOfConsent(false)
                                                                  .build();
                    LOG.d(this, "TEST-ads-device true", ADS.getByTestID(this));
                } else {
                    params = new ConsentRequestParameters.Builder().setTagForUnderAgeOfConsent(false).build();
                    LOG.d(this, "TEST-ads-device false", ADS.getByTestID(this));
                }
                ConsentInformation consentInformation = UserMessagingPlatform.getConsentInformation(this);

                if (AppsConfig.IS_TEST_DEVICE) {
                    //consentInformation.reset();
                }

                consentInformation.requestConsentInfoUpdate(this, params, () -> {
                    if (consentInformation.isConsentFormAvailable()) {
                        loadForm(consentInformation);
                    }
                }, formError -> {
                    LOG.d("formError", formError.getErrorCode(), formError.getMessage());
                });
            }
        } catch (Exception e) {
            LOG.e(e);
        }
    }

    public void loadForm(ConsentInformation consentInformation) {
        // Loads a consent form. Must be called on the main thread.
        UserMessagingPlatform.loadConsentForm(this, new UserMessagingPlatform.OnConsentFormLoadSuccessListener() {
            @Override
            public void onConsentFormLoadSuccess(ConsentForm consentForm) {
                if (consentInformation.getConsentStatus() == ConsentInformation.ConsentStatus.REQUIRED) {
                    consentForm.show(MainTabs2.this, formError -> {
                        if (consentInformation.getConsentStatus() == ConsentInformation.ConsentStatus.OBTAINED) {
                        }
                        if (formError != null) {
                            LOG.d("formError", formError.getErrorCode(), formError.getMessage());
                        }
                        //activateAds();
                    });
                }
            }
        }, new UserMessagingPlatform.OnConsentFormLoadFailureListener() {
            @Override
            public void onConsentFormLoadFailure(FormError formError) {
                LOG.d("formError", formError.getErrorCode(), formError.getMessage());
            }
        });
    }

    // A sync is drawn in the preferences now, and nowhere else in the app; here the message
    // only lets go of the pull-to-refresh wheel that may have started it.
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onShowSycn(MessageSync msg) {
        try {
            swipeRefreshLayout.setRefreshing(false);
        } catch (Exception e) {
            LOG.e(e);
        }
    }

    @Subscribe
    public void onMessegeBrightness(MessegeBrightness msg) {
        BrightnessHelper.onMessegeBrightness(handler, msg, toastBrightnessText, overlay);
    }

    @Subscribe
    public void goToPageMsg(SearchMetaMsg msg){
            int index = UITab.getCurrentTabIndex(UITab.SearchFragment);
            if(index>=0) {
                pager.setCurrentItem(index);
            }
    }

    public void checkGoToPage(Intent intent) {
        try {
            int pos = intent.getIntExtra(EXTRA_PAGE_NUMBER, -1);
            if (pos != -1) {
                pager.setCurrentItem(pos);
            }
        } catch (Exception e) {
            LOG.e(e);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();

        LocalBroadcastManager.getInstance(this).unregisterReceiver(broadcastReceiver);
        AppProfile.save(this);
        IMG.pauseRequests(this);

        if (Dips.isEInk()) {
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        //AppsConfig.isCloudsEnable = UITab.isShowCloudsPreferences();
        AppsConfig.isCloudsEnable = false;

        LOG.d(TAG, "onResume");
        if (Dips.isEInk()) {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        } else {
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        }
        LOG.d("FLAG clearFlags", "FLAG_KEEP_SCREEN_ON", "clear");

        DocumentController.chooseFullScreen(this, AppState.get().fullScreenMainMode);
        TintUtil.updateAll();

        LocalBroadcastManager.getInstance(this)
                             .registerReceiver(broadcastReceiver, new IntentFilter(UIFragment.INTENT_TINT_CHANGE));
        if (swipeRefreshLayout != null) {
            swipeRefreshLayout.setEnabled(isPullToRefreshEnable());
        }
        askForLostLibraryStorage(this);

        try {
            if (pager != null) {
                final UIFragment uiFragment = tabFragments.get(pager.getCurrentItem());
                uiFragment.onSelectFragment();
            }
        } catch (Exception e) {
            LOG.e(e);
        }
        if (adapter != null) {
            adapter.notifyDataSetChanged();
        }

        IMG.resumeRequests(this);
        //AppSP.get().lastClosedActivity = MainTabs2.class.getSimpleName();
        //LOG.d("lasta save", AppSP.get().lastClosedActivity);

    }

    public void updateCurrentFragment() {
        tabFragments.get(pager.getCurrentItem()).onSelectFragment();
    }

    @Override
    public boolean onKeyDown(int keyCode1, KeyEvent event) {
        if (!isEink) {
            return super.onKeyDown(keyCode1, event);
        }

        int keyCode = event.getKeyCode();
        if (keyCode == 0) {
            keyCode = event.getScanCode();
        }
        isMyKey = false;
        if (tabFragments.get(pager.getCurrentItem()).onKeyDown(keyCode)) {
            isMyKey = true;
            return true;
        }

        return super.onKeyDown(keyCode1, event);
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        if (!isEink) {
            return super.onKeyUp(keyCode, event);
        }

        if (isMyKey) {
            return true;
        }
        // TODO Auto-generated method stub
        return super.onKeyUp(keyCode, event);
    }

    @Override
    protected void onStop() {
        super.onStop();
        SharedBooks.cache.clear();
    }

    @Override
    public void onDestroy() {

        GFile.timeout = 0;
        GFile.runSyncService(this);

        if (handler != null) {
            handler.removeCallbacksAndMessages(null);
        }

        LOG.d(TAG, "onDestroy");
        if (pager != null) {
            try {
                pager.setAdapter(null);
            } catch (Exception e) {
                LOG.e(e);
            }
        }
        // Analytics.onStop(this);
        CacheDir.ZipApp.removeCacheContent();
        // ImageExtractor.clearErrors();
        // ImageExtractor.clearCodeDocument();

        EventBus.getDefault().unregister(this);
        //IMG.clearMemoryCache();
        super.onDestroy();
    }

    @Override
    public void onConfigurationChanged(final Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

        String language = newConfig.getLocales().get(0).getLanguage();
        float fontScale = newConfig.fontScale;

        LOG.d("ContextWrapper ConfigChanged", language, fontScale);

        if (pager != null) {
            int currentItem = pager.getCurrentItem();
            //pager.setAdapter(adapter); //WHY???
            pager.setCurrentItem(currentItem);
            IMG.clearMemoryCache();
        }
        //showBannerAds();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        Android6.onRequestPermissionsResult(this, requestCode, permissions, grantResults);
    }

    @Override
    public boolean onKeyLongPress(final int keyCode, final KeyEvent event) {
        if (CloseAppDialog.checkLongPress(this, event)) {
            CloseAppDialog.show(this, closeActivityRunnable);
            return true;
        }
        return super.onKeyLongPress(keyCode, event);
    }

    @Override
    public void onFinishActivity() {
        finish();
    }

    @Override
    public void onBackPressedImpl() {
//        if (drawerLayout != null && drawerLayout.isDrawerOpen(GravityCompat.START)) {
//            drawerLayout.closeDrawer(GravityCompat.START, AppState.get().appTheme != AppState.THEME_INK);
//            return;
//        }

        if (tabFragments != null) {
            if (!tabFragments.isEmpty() && tabFragments.get(pager.getCurrentItem()).isBackPressed()) {
                return;
            }
            RefiewForm.show(this, closeActivityRunnable);
        } else {
            closeActivityRunnable.run();

        }
    }

    @Override
    public void onBackPressedFinishImpl() {
        closeActivityRunnable.run();
    }

    @Subscribe
    public void onCloseAppMsg(MsgCloseMainTabs event) {
        onFinishActivity();
    }

    BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            int pos = intent.getIntExtra(EXTRA_PAGE_NUMBER, -1);
            if (pos != -1) {
                if (pos >= 0) {
                    pager.setCurrentItem(pos);
                }

                if (intent.getBooleanExtra(EXTRA_NOTIFY_REFRESH, false)) {
                    onResume();
                }
            } else {
                if (AppState.get().appTheme == AppState.THEME_INK) {
                    TintUtil.setTintImageNoAlpha(imageMenu, TintUtil.color);
                    indicator.setSelectedIndicatorColors(TintUtil.color);
                    indicator.setDividerColors(TintUtil.color);
                    indicator.updateIcons(pager.getCurrentItem());
                } else {
                    // Through setTabsBackground, or the pill at the foot would be flattened
                    // into a plain rectangle of the new colour.
                    indicator.setTabsBackground(TintUtil.color);
                    imageMenuParent.setBackgroundColor(SlidingTabLayout.floatingTint(TintUtil.color));
                    View statusBarHack = findViewById(R.id.systemBarHack);
                    if (statusBarHack != null) {
                        statusBarHack.setBackgroundColor(SlidingTabLayout.floatingTint(TintUtil.color));
                    }
                }
            }
        }
    };
}
