package com.foobnix.ui2;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.ebookdroid.ui.viewer.ViewerActivity;

import com.foobnix.android.utils.LOG;
import com.foobnix.android.utils.TxtUtils;
import com.foobnix.dao2.FileMeta;
import com.foobnix.pdf.SlidingTabLayout;
import com.foobnix.pdf.info.ADS;
import com.foobnix.pdf.info.Analytics;
import com.foobnix.pdf.info.Android6;
import com.foobnix.pdf.info.AppsConfig;
import com.foobnix.pdf.info.FontExtractor;
import com.foobnix.pdf.info.R;
import com.foobnix.pdf.info.TintUtil;
import com.foobnix.pdf.info.widget.RecentBooksWidget;
import com.foobnix.pdf.info.wrapper.AppState;
import com.foobnix.pdf.info.wrapper.DocumentController;
import com.foobnix.pdf.info.wrapper.UITab;
import com.foobnix.pdf.search.activity.HorizontalViewActivity;
import com.foobnix.pdf.search.view.CloseAppDialog;
import com.foobnix.sys.TempHolder;
import com.foobnix.ui2.adapter.TabsAdapter2;
import com.foobnix.ui2.fragment.BookmarksFragment2;
import com.foobnix.ui2.fragment.BrowseFragment2;
import com.foobnix.ui2.fragment.PrefFragment2;
import com.foobnix.ui2.fragment.RecentFragment2;
import com.foobnix.ui2.fragment.SearchFragment2;
import com.foobnix.ui2.fragment.UIFragment;
import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.InterstitialAd;
import com.google.android.gms.ads.NativeExpressAdView;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.SearchManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.view.ViewPager;
import android.support.v4.view.ViewPager.OnPageChangeListener;
import android.support.v4.widget.DrawerLayout;
import android.support.v4.widget.DrawerLayout.DrawerListener;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.Toast;

@SuppressLint("NewApi")
public class MainTabs2 extends FragmentActivity {
    public static final String EXTRA_EXIT = "EXTRA_EXIT";
    public static final String EXTRA_SHOW_TABS = "EXTRA_SHOW_TABS";
    public static String EXTRA_PAGE_NUMBER = "EXTRA_PAGE_NUMBER";
    public static String EXTRA_SEACH_TEXT = "EXTRA_SEACH_TEXT";
    ViewPager pager;
    List<UIFragment> tabFragments;
    private NativeExpressAdView adViewNative;
    InterstitialAd mInterstitialAd;
    public static volatile boolean isInStack;

    ImageView imageMenu;

    @Override
    protected void onNewIntent(final Intent intent) {
        isInStack = true;
        testIntentHandler();
        if (intent.getBooleanExtra(EXTRA_EXIT, false)) {
            finish();
        }
        checkGoToPage(intent);
    }

    public void testIntentHandler() {
        if (getIntent().hasExtra(RecentBooksWidget.TEST_LOCALE)) {

            int pos = getIntent().getIntExtra(RecentBooksWidget.TEST_LOCALE_POS, -1);
            if (pos != -1) {
                pager.setCurrentItem(pos);

                pager.postDelayed(new Runnable() {

                    @Override
                    public void run() {
                        DocumentController.chooseFullScreen(MainTabs2.this, true);

                        if (getIntent().getBooleanExtra("id0", false)) {
                            ((SearchFragment2) tabFragments.get(0)).popupMenuTest();

                        }

                        if (getIntent().getBooleanExtra("id1", false)) {
                            ((SearchFragment2) tabFragments.get(0)).onTextRecive("Lewis");

                        }
                    }
                }, 100);

            } else {
                finish();
                startActivity(new Intent(this, MainTabs2.class));
            }

        }
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        testIntentHandler();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        if (AppState.getInstance().isWhiteTheme) {
            setTheme(R.style.StyledIndicatorsWhite);
        } else {
            setTheme(R.style.StyledIndicatorsBlack);
        }
        super.onCreate(savedInstanceState);
        // test

        LOG.d("EXTRA_EXIT", EXTRA_EXIT);
        if (getIntent().getBooleanExtra(EXTRA_EXIT, false)) {
            finish();
            return;
        }

        isInStack = true;

        if (!AppsConfig.checkIsProInstalled(this) && AppsConfig.ADMOB_FULLSCREEN != null) {
            mInterstitialAd = new InterstitialAd(this);
            mInterstitialAd.setAdUnitId(AppsConfig.ADMOB_FULLSCREEN);
            mInterstitialAd.setAdListener(new AdListener() {
                @Override
                public void onAdClosed() {
                    finish();
                }
            });

            try {
                mInterstitialAd.loadAd(ADS.adRequest);
            } catch (Exception e) {
                LOG.e(e);
            }
        }

        TintUtil.setStatusBarColor(this);
        DocumentController.chooseFullScreen(this, false);
        DocumentController.applyBrigtness(this);
        DocumentController.doRotation(this);

        setContentView(R.layout.main_tabs);

        tabFragments = new ArrayList<UIFragment>();

        try {
            for (UITab tab : UITab.getOrdered(AppState.get().tabsOrder)) {
                if (tab.isVisible()) {
                    tabFragments.add(tab.getClazz().newInstance());
                }
            }
        } catch (Exception e) {
            LOG.e(e);
            Toast.makeText(MainTabs2.this, R.string.msg_unexpected_error, Toast.LENGTH_LONG).show();
            tabFragments.add(new SearchFragment2());
            tabFragments.add(new BrowseFragment2());
            tabFragments.add(new RecentFragment2());
            tabFragments.add(new BookmarksFragment2());
            tabFragments.add(new PrefFragment2());
        }
        getSupportFragmentManager().beginTransaction().replace(R.id.left_drawer, new PrefFragment2()).commit();

        imageMenu = (ImageView) findViewById(R.id.imageMenu);
        ((ViewGroup) imageMenu.getParent()).setBackgroundColor(TintUtil.color);

        final DrawerLayout drawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);

        imageMenu.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                if (drawerLayout.isDrawerOpen(Gravity.START))
                    drawerLayout.closeDrawer(Gravity.START);
                else
                    drawerLayout.openDrawer(Gravity.START);

            }
        });

        if (UITab.isShowPreferences()) {
            imageMenu.setVisibility(View.GONE);
            drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED);
        } else {
            imageMenu.setVisibility(View.VISIBLE);
        }

        final TabsAdapter2 adapter = new TabsAdapter2(this, tabFragments);

        pager = (ViewPager) findViewById(R.id.pager);
        pager.setAdapter(adapter);
        pager.setOffscreenPageLimit(5);
        pager.addOnPageChangeListener(onPageChangeListener);

        drawerLayout.addDrawerListener(new DrawerListener() {

            @Override
            public void onDrawerStateChanged(int arg0) {
            }

            @Override
            public void onDrawerSlide(View arg0, float arg1) {

            }

            @Override
            public void onDrawerOpened(View arg0) {
                // TODO Auto-generated method stub

            }

            @Override
            public void onDrawerClosed(View arg0) {
                tabFragments.get(pager.getCurrentItem()).onSelectFragment();

            }
        });

        indicator = (SlidingTabLayout) findViewById(R.id.slidingTabs);
        indicator.setViewPager(pager);

        indicator.setDividerColors(getResources().getColor(R.color.tint_divider));
        indicator.setSelectedIndicatorColors(Color.WHITE);
        indicator.setBackgroundColor(TintUtil.color);

        Android6.checkPermissions(this);
        Analytics.onStart(this);
        ADS.activateNative(this, adViewNative);
        FontExtractor.extractFonts(this);

        List<String> actions = Arrays.asList("android.intent.action.PROCESS_TEXT", "android.intent.action.SEARCH", "android.intent.action.SEND");
        List<String> extras = Arrays.asList(Intent.EXTRA_PROCESS_TEXT_READONLY, Intent.EXTRA_PROCESS_TEXT, SearchManager.QUERY, Intent.EXTRA_TEXT);
        if (getIntent() != null && getIntent().getAction() != null) {
            if (actions.contains(getIntent().getAction())) {
                for (String extra : extras) {
                    final String text = getIntent().getStringExtra(extra);
                    if (TxtUtils.isNotEmpty(text)) {
                        AppState.get().lastA = null;
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

        boolean showTabs = getIntent().getBooleanExtra(EXTRA_SHOW_TABS, false);
        LOG.d("EXTRA_SHOW_TABS", showTabs, AppState.get().lastMode);
        if (showTabs == false && AppState.getInstance().isOpenLastBook) {
            FileMeta meta = AppDB.get().getRecentLast();
            AppState.get().lastA = null;

            if (meta != null) {
                boolean isEasyMode = HorizontalViewActivity.class.getSimpleName().equals(AppState.get().lastMode);
                Intent intent = new Intent(this, isEasyMode ? HorizontalViewActivity.class : ViewerActivity.class);
                intent.setData(Uri.fromFile(new File(meta.getPath())));
                startActivity(intent);
            }
        }

        LOG.d("lasta", AppState.get().lastA);
        if (HorizontalViewActivity.class.getSimpleName().equals(AppState.get().lastA)) {

            FileMeta meta = AppDB.get().getRecentLast();
            if (meta != null) {
                Intent intent = new Intent(this, HorizontalViewActivity.class);
                intent.setData(Uri.fromFile(new File(meta.getPath())));
                startActivity(intent);
                LOG.d("Start lasta", AppState.get().lastA);
            }
        } else if (ViewerActivity.class.getSimpleName().equals(AppState.get().lastA)) {
            FileMeta meta = AppDB.get().getRecentLast();
            if (meta != null) {
                Intent intent = new Intent(this, ViewerActivity.class);
                intent.setData(Uri.fromFile(new File(meta.getPath())));
                startActivity(intent);
                LOG.d("Start lasta", AppState.get().lastA);
            }

        }

        checkGoToPage(getIntent());


    }

    BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            int pos = intent.getIntExtra(EXTRA_PAGE_NUMBER, -1);
            if (pos != -1) {
                pager.setCurrentItem(pos);
            } else {
                indicator.setBackgroundColor(TintUtil.color);
                ((ViewGroup) imageMenu.getParent()).setBackgroundColor(TintUtil.color);
            }
        }

    };

    public void checkGoToPage(Intent intent) {
        int pos = intent.getIntExtra(EXTRA_PAGE_NUMBER, -1);
        if (pos != -1) {
            pager.setCurrentItem(pos);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        ADS.onResumeNative(adViewNative);
        DocumentController.chooseFullScreen(this, false);
        TintUtil.updateAll();
        AppState.get().lastA = MainTabs2.class.getSimpleName();
        LocalBroadcastManager.getInstance(this).registerReceiver(broadcastReceiver, new IntentFilter(UIFragment.INTENT_TINT_CHANGE));
    };

    @Override
    protected void onPause() {
        super.onPause();
        ADS.onPauseNative(adViewNative);
        AppState.getInstance().save(this);
        LocalBroadcastManager.getInstance(this).unregisterReceiver(broadcastReceiver);
    };

    @Override
    public void onDestroy() {
        super.onDestroy();
        Analytics.onStop(this);
        ADS.destoryNative(adViewNative);
        isInStack = false;
    }

    @Override
    public void onConfigurationChanged(final Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        ADS.activateNative(this, adViewNative);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        Android6.onRequestPermissionsResult(this, requestCode, permissions, grantResults);
    }

    OnPageChangeListener onPageChangeListener = new OnPageChangeListener() {

        @Override
        public void onPageSelected(int pos) {
            tabFragments.get(pos).onSelectFragment();
        }

        @Override
        public void onPageScrolled(int arg0, float arg1, int arg2) {

        }

        @Override
        public void onPageScrollStateChanged(int arg0) {

        }
    };
    private SlidingTabLayout indicator;

    public void closeActivity() {

        if (mInterstitialAd != null && mInterstitialAd.isLoaded()) {
            mInterstitialAd.show();
        } else {
            TempHolder.listHash = 0;
            finish();
        }
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
    public void onBackPressed() {
        if (!tabFragments.isEmpty() && tabFragments.get(pager.getCurrentItem()).isBackPressed()) {
            return;
        }

        CloseAppDialog.show(this, closeActivityRunnable);
    }

    Runnable closeActivityRunnable = new Runnable() {

        @Override
        public void run() {
            closeActivity();

        }
    };

    public static void startActivity(Activity c, int tab) {
        AppState.get().lastA = null;
        final Intent intent = new Intent(c, MainTabs2.class);
        intent.putExtra(MainTabs2.EXTRA_SHOW_TABS, true);
        intent.putExtra(MainTabs2.EXTRA_PAGE_NUMBER, tab);
        c.startActivity(intent);
        c.overridePendingTransition(0, 0);

    }

    public static void closeApp(Context c) {
        if (MainTabs2.isInStack) {
            Intent startMain = new Intent(c, MainTabs2.class);
            startMain.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startMain.putExtra(MainTabs2.EXTRA_EXIT, true);
            c.startActivity(startMain);
        }
    }

}
