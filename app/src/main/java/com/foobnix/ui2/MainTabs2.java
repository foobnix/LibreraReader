package com.foobnix.ui2;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.SearchManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

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
import com.foobnix.android.utils.LOG;
import com.foobnix.android.utils.ResultResponse;
import com.foobnix.android.utils.Safe;
import com.foobnix.android.utils.StringDB;
import com.foobnix.android.utils.TxtUtils;
import com.foobnix.drive.GFile;
import com.foobnix.ext.CacheZipUtils.CacheDir;
import com.foobnix.model.AppProfile;
import com.foobnix.model.AppSP;
import com.foobnix.model.AppState;
import com.foobnix.pdf.SlidingTabLayout;
import com.foobnix.pdf.info.Android6;
import com.foobnix.pdf.info.AndroidWhatsNew;
import com.foobnix.pdf.info.AppsConfig;
import com.foobnix.pdf.info.Clouds;
import com.foobnix.pdf.info.ExportConverter;
import com.foobnix.pdf.info.ExportSettingsManager;
import com.foobnix.pdf.info.FontExtractor;
import com.foobnix.pdf.info.IMG;
import com.foobnix.pdf.info.PasswordDialog;
import com.foobnix.pdf.info.R;
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
import com.foobnix.pdf.search.view.AsyncProgressResultToastTask;
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
import com.google.android.gms.auth.api.signin.GoogleSignIn;

import org.ebookdroid.common.settings.books.SharedBooks;
import org.ebookdroid.ui.viewer.VerticalViewActivity;
import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import test.SvgActivity;


@SuppressLint("NewApi")
public class MainTabs2 extends AdsFragmentActivity {
    public static final int REQUEST_CODE_ADD_RESOURCE = 123;
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
    TextView toastBrightnessText;
    Handler handler;
    MyProgressBar fab;
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
            Apps.accessibilityText(MainTabs2.this, adapter.getPageTitle(pos).toString() + " " + getString(R.string.tab_selected));


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
                if (uiFragment instanceof PrefFragment2) {
                    swipeRefreshLayout.setEnabled(false);
                } else {
                    swipeRefreshLayout.setEnabled(true);
                }
            }
        }
    };
    Runnable closeActivityRunnable = new Runnable() {

        @Override
        public void run() {
//            TTSNotification.hideNotification();
//            TTSEngine.get().shutdown();
//            adsPause();
            finish();
        }
    };
    boolean once = true;
    private SlidingTabLayout indicator;
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
                    indicator.setBackgroundColor(TintUtil.color);
                    imageMenuParent.setBackgroundColor(TintUtil.color);
                }
            }
        }

    };
    private DrawerLayout drawerLayout;

    public static boolean isPullToRefreshEnable(Context a, View swipeRefreshLayout) {
        if (a == null || swipeRefreshLayout == null) {
            return false;
        }
        return AppSP.get().isEnableSync && GoogleSignIn.getLastSignedInAccount(a) != null && BookCSS.get().isSyncPullToRefresh;
    }

    public static void startActivity(Activity c, int tab) {
        final Intent intent = new Intent(c, MainTabs2.class);
        intent.putExtra(MainTabs2.EXTRA_SHOW_TABS, true);
        intent.putExtra(MainTabs2.EXTRA_PAGE_NUMBER, tab);
        intent.putExtra(PasswordDialog.EXTRA_APP_PASSWORD, c.getIntent().getStringExtra(PasswordDialog.EXTRA_APP_PASSWORD));
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
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        LOG.d("REQUEST_CODE_ADD_RESOURCE", requestCode, resultCode, data);
        if (resultCode != Activity.RESULT_OK) {
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
        } else if (requestCode == GFile.REQUEST_CODE_SIGN_IN) {
            GoogleSignIn.getSignedInAccountFromIntent(data)
                    .addOnSuccessListener(googleAccount -> {
                        AppSP.get().isEnableSync = true;
                        Toast.makeText(this, R.string.success, Toast.LENGTH_SHORT).show();
                        EventBus.getDefault().post(new GDriveSycnEvent());
                        GFile.runSyncService(MainTabs2.this);

                        swipeRefreshLayout.setEnabled(isPullToRefreshEnable());

                        AppSP.get().save();

                    })
                    .addOnFailureListener(exception ->
                            {
                                LOG.e(exception);
                                Toast.makeText(this, R.string.fail, Toast.LENGTH_SHORT).show();
                                AppSP.get().isEnableSync = false;
                                swipeRefreshLayout.setEnabled(false);
                                AppSP.get().save();

                            }
                    );


        }


    }

    public boolean isPullToRefreshEnable() {
        return isPullToRefreshEnable(MainTabs2.this, swipeRefreshLayout);
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        withInterstitial = false;
        super.onPostCreate(savedInstanceState);
        // testIntentHandler();

        if (Android6.canWrite(this)) {
            BrightnessHelper.applyBrigtness(this);
            BrightnessHelper.updateOverlay(overlay);
        }
        GFile.runSyncService(this);
    }

    @Override
    protected void attachBaseContext(Context context) {
        super.attachBaseContext(MyContextWrapper.wrap(context));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        if (AppState.get().appTheme == AppState.THEME_LIGHT || AppState.get().appTheme == AppState.THEME_INK) {
            setTheme(R.style.StyledIndicatorsWhite);
        } else {
            setTheme(R.style.StyledIndicatorsBlack);
        }
        super.onCreate(savedInstanceState);
        //FirebaseAnalytics.getInstance(this);

        if (false) {
            startActivity(new Intent(this, SvgActivity.class));
            return;
        }

        if (!Android6.canWrite(this)) {
            Android6.checkPermissions(this, true);
            return;
        }


        Clouds.get().init(this);

        //import settings


        SharedPreferences BOOKS = getSharedPreferences("BOOKS", Context.MODE_PRIVATE);
        if (BOOKS.getAll() != null && !BOOKS.getAll().isEmpty()) {
            LOG.d("Restore from backup");
            File oldConfig = new File(AppProfile.SYNC_FOLDER_ROOT, Build.MODEL.replace(" ", "_") + "-backup-v8.0.json");
            if (!oldConfig.exists()) {
                new AsyncProgressResultToastTask(this, new ResultResponse<Boolean>() {
                    @Override
                    public boolean onResultRecive(Boolean result) {
                        if (MainTabs2.this != null) {
                            MainTabs2.this.finish();
                            MainTabs2.this.startActivity(getIntent());
                        }
                        return false;
                    }
                }) {
                    @Override
                    protected Boolean doInBackground(Object... objects) {
                        try {
                            oldConfig.getParentFile().mkdirs();
                            AppDB.get().open(MainTabs2.this, "all-5");//old db data
                            AppProfile.SYNC_FOLDER_ROOT.mkdirs();

                            ExportSettingsManager.exportAll(MainTabs2.this, oldConfig);
                            try {
                                ExportConverter.covertJSONtoNew(MainTabs2.this, oldConfig);
                                ExportConverter.copyPlaylists();
                            } catch (Exception e) {
                                LOG.e(e);
                            }
                            AppProfile.clear();
                        } catch (Exception e) {
                            LOG.e(e);
                        }
                        return true;
                    }
                }.execute();

                return;
            }
        }


        if (PasswordDialog.isNeedPasswordDialog(this)) {
            return;
        }

        LOG.d(TAG, "onCreate");

        LOG.d("EXTRA_EXIT", EXTRA_EXIT);
        if (getIntent().getBooleanExtra(EXTRA_EXIT, false)) {
            finish();
            return;
        }

        handler = new Handler();
        isEink = Dips.isEInk();

        TintUtil.setStatusBarColor(this);
        DocumentController.doRotation(this);
        DocumentController.doContextMenu(this);

        setContentView(R.layout.main_tabs);


        imageMenu = (ImageView) findViewById(R.id.imageMenu1);
        imageMenuParent = findViewById(R.id.imageParent1);
        imageMenuParent.setBackgroundColor(TintUtil.color);

        fab = findViewById(R.id.fab);
        fab.setVisibility(View.GONE);
        fab.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                Dialogs.showSyncLOGDialog(MainTabs2.this);
            }
        });
        fab.setBackgroundResource(R.drawable.bg_circular);
        TintUtil.setDrawableTint(fab.getBackground().getCurrent(), TintUtil.color);

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

        toastBrightnessText = (TextView) findViewById(R.id.toastBrightnessText);
        toastBrightnessText.setVisibility(View.GONE);
        TintUtil.setDrawableTint(toastBrightnessText.getCompoundDrawables()[0], Color.WHITE);

        tabFragments = new ArrayList<UIFragment>();

        try {

            for (UITab tab : UITab.getOrdered()) {
                if (tab.isVisible()) {
                    tabFragments.add(tab.getClazz().newInstance());
                }
            }
            if (tabFragments.size() == 0) {
                synchronized (AppState.get().tabsOrder7) {
                    AppState.get().tabsOrder7 = AppState.DEFAULTS_TABS_ORDER;
                }
                for (UITab tab : UITab.getOrdered()) {
                    if (tab.isVisible()) {
                        tabFragments.add(tab.getClazz().newInstance());
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

        drawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);

        imageMenu.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                if (drawerLayout.isDrawerOpen(GravityCompat.START))
                    drawerLayout.closeDrawer(GravityCompat.START, AppState.get().appTheme != AppState.THEME_INK);
                else
                    drawerLayout.openDrawer(GravityCompat.START, AppState.get().appTheme != AppState.THEME_INK);

            }
        });

        if (UITab.isShowPreferences()) {
            imageMenu.setVisibility(View.GONE);
            drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED);
        } else {
            imageMenu.setVisibility(View.VISIBLE);
        }

        if (AppState.get().isEnableAccessibility) {
            imageMenu.setVisibility(View.VISIBLE);
        }

        // ((BrigtnessDraw)
        // findViewById(R.id.brigtnessProgressView)).setActivity(this);

        adapter = new TabsAdapter2(this, tabFragments);
        pager = (ViewPager) findViewById(R.id.pager);
        pager.setAccessibilityDelegate(new View.AccessibilityDelegate());


        if (Android6.canWrite(this)) {
            pager.setAdapter(adapter);
        }

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
                    TintUtil.setDrawableTint(fab.getBackground().getCurrent(), TintUtil.color);


                } catch (Exception e) {
                    LOG.e(e);
                }

            }
        });

        if (AppState.get().tapPositionTop) {
            indicator = (SlidingTabLayout) findViewById(R.id.slidingTabs1);
        } else {
            indicator = (SlidingTabLayout) findViewById(R.id.slidingTabs2);
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

        indicator.setDividerColors(getResources().getColor(R.color.tint_divider));
        indicator.setSelectedIndicatorColors(Color.WHITE);
        indicator.setBackgroundColor(TintUtil.color);

        if (!AppState.get().tapPositionTop || !AppState.get().tabWithNames) {
            imageMenu.setVisibility(View.GONE);
            indicator.setDividerColors(Color.TRANSPARENT);
            indicator.setSelectedIndicatorColors(Color.TRANSPARENT);
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
        if (AppState.get().isEnableAccessibility) {
            imageMenu.setVisibility(View.VISIBLE);
        }

        if (AppState.get().appTheme == AppState.THEME_INK) {
            TintUtil.setTintImageNoAlpha(imageMenu, TintUtil.color);
            indicator.setSelectedIndicatorColors(TintUtil.color);
            indicator.setDividerColors(TintUtil.color);
            indicator.setBackgroundColor(Color.TRANSPARENT);
            imageMenuParent.setBackgroundColor(Color.TRANSPARENT);

        }


        Android6.checkPermissions(this, true);
        // Analytics.onStart(this);

        List<String> actions = Arrays.asList("android.intent.action.PROCESS_TEXT", "android.intent.action.SEARCH", "android.intent.action.SEND");
        List<String> extras = Arrays.asList(Intent.EXTRA_PROCESS_TEXT_READONLY, Intent.EXTRA_PROCESS_TEXT, SearchManager.QUERY, Intent.EXTRA_TEXT);
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

        try {
            if (AppState.get().isShowWhatIsNewDialog && !AppState.get().isEnableAccessibility) {
                LOG.d("checkForNewBeta");
                AndroidWhatsNew.checkForNewBeta(this);
            }
        } catch (Exception e) {
            LOG.e(e);
        }
        if (Android6.canWrite(this)) {
            FontExtractor.extractFonts(this);
        }
        EventBus.getDefault().register(this);

        boolean showTabs = getIntent().getBooleanExtra(EXTRA_SHOW_TABS, false);
        LOG.d("EXTRA_SHOW_TABS", showTabs, AppSP.get().lastClosedActivity);
        if (showTabs == false && AppState.get().isOpenLastBook) {
            LOG.d("Open lastBookPath", AppSP.get().lastBookPath);
            if (AppSP.get().lastBookPath == null || !new File(AppSP.get().lastBookPath).isFile()) {
                LOG.d("Open Last book not found");
                return;
            }

            Safe.run(() -> {
                boolean isEasyMode = HorizontalViewActivity.class.getSimpleName().equals(AppSP.get().lastClosedActivity);
                Intent intent = new Intent(MainTabs2.this, isEasyMode ? HorizontalViewActivity.class : VerticalViewActivity.class);
                intent.putExtra(PasswordDialog.EXTRA_APP_PASSWORD, getIntent().getStringExtra(PasswordDialog.EXTRA_APP_PASSWORD));
                intent.setData(Uri.fromFile(new File(AppSP.get().lastBookPath)));
                startActivity(intent);
            });
        } else if (false && !AppState.get().isOpenLastBook) {//templorary disable this feature
            LOG.d("Open book lastA", AppSP.get().lastClosedActivity);

            if (AppSP.get().lastBookPath == null || !new File(AppSP.get().lastBookPath).isFile()) {
                LOG.d("Open Last book not found");
                return;
            }
            final String saveMode = AppSP.get().lastClosedActivity;
            Safe.run(new Runnable() {

                @Override
                public void run() {
                    LOG.d("Open AppSP.get().lastBookPath", saveMode);
                    if (HorizontalViewActivity.class.getSimpleName().equals(saveMode)) {
                        Intent intent = new Intent(MainTabs2.this, HorizontalViewActivity.class);
                        intent.setData(Uri.fromFile(new File(AppSP.get().lastBookPath)));
                        startActivity(intent);
                        LOG.d("Start lastA", saveMode);
                    } else if (VerticalViewActivity.class.getSimpleName().equals(saveMode)) {
                        Intent intent = new Intent(MainTabs2.this, VerticalViewActivity.class);
                        intent.setData(Uri.fromFile(new File(AppSP.get().lastBookPath)));
                        startActivity(intent);
                        LOG.d("Start lastA", saveMode);
                    }

                }
            });

        }

        checkGoToPage(getIntent());

        if (!AppState.get().isEnableAccessibility && once) {
            once = false;
            handler.postDelayed(() -> {
                Apps.accessibilityText(MainTabs2.this, getString(R.string.welcome_accessibility));
            }, 5000);
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onShowSycn(MessageSync msg) {

        try {
            if (msg.state == MessageSync.STATE_VISIBLE) {
                if (BookCSS.get().isSyncAnimation) {
                    fab.setVisibility(View.VISIBLE);
                }
                swipeRefreshLayout.setRefreshing(false);
            } else if (msg.state == MessageSync.STATE_FAILE) {
                fab.setVisibility(View.GONE);
                swipeRefreshLayout.setRefreshing(false);
                //Toast.makeText(this, getString(R.string.sync_error), Toast.LENGTH_LONG).show();
            } else {
                fab.setVisibility(View.GONE);
                swipeRefreshLayout.setRefreshing(false);

            }
        } catch (Exception e) {
            LOG.e(e);
        }
    }

    @Subscribe
    public void onMessegeBrightness(MessegeBrightness msg) {
        BrightnessHelper.onMessegeBrightness(handler, msg, toastBrightnessText, overlay);
    }

    public void checkGoToPage(Intent intent) {
        int pos = intent.getIntExtra(EXTRA_PAGE_NUMBER, -1);
        if (pos != -1) {
            pager.setCurrentItem(pos);
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

        AppsConfig.isCloudsEnable = UITab.isShowCloudsPreferences();

        LOG.d(TAG, "onResume");
        if (Dips.isEInk()) {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        } else {
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        }
        LOG.d("FLAG clearFlags", "FLAG_KEEP_SCREEN_ON", "clear");


        DocumentController.chooseFullScreen(this, AppState.get().fullScreenMainMode);
        TintUtil.updateAll();
        LocalBroadcastManager.getInstance(this).registerReceiver(broadcastReceiver, new IntentFilter(UIFragment.INTENT_TINT_CHANGE));
        if (swipeRefreshLayout != null) {
            swipeRefreshLayout.setEnabled(isPullToRefreshEnable());
        }

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
        IMG.clearMemoryCache();
        super.onDestroy();
    }

    @Override
    public void onConfigurationChanged(final Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

        String language = newConfig.locale.getLanguage();
        float fontScale = newConfig.fontScale;

        LOG.d("ContextWrapper ConfigChanged", language, fontScale);

        if (pager != null) {
            int currentItem = pager.getCurrentItem();
            //pager.setAdapter(adapter); //WHY???
            pager.setCurrentItem(currentItem);
            IMG.clearMemoryCache();
        }
        activateAds();

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
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
    public void onBackPressed() {

        if (drawerLayout != null && drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START, AppState.get().appTheme != AppState.THEME_INK);
            return;
        }

        if (tabFragments != null) {
            if (!tabFragments.isEmpty() && tabFragments.get(pager.getCurrentItem()).isBackPressed()) {
                return;
            }

            CloseAppDialog.show(this, closeActivityRunnable);
        } else {
            closeActivityRunnable.run();
        }
    }

    @Subscribe
    public void onCloseAppMsg(MsgCloseMainTabs event) {
        onFinishActivity();
    }
}
