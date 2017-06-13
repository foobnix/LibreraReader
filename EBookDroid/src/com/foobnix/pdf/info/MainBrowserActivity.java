package com.foobnix.pdf.info;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import org.ebookdroid.ui.viewer.ViewerActivity;

import com.foobnix.android.utils.LOG;
import com.foobnix.android.utils.TxtUtils;
import com.foobnix.pdf.SlidingTabLayout;
import com.foobnix.pdf.info.fragment.BookmarksFragment;
import com.foobnix.pdf.info.fragment.BrowseFragmet;
import com.foobnix.pdf.info.fragment.RecentFragmet;
import com.foobnix.pdf.info.fragment.SearchFragment;
import com.foobnix.pdf.info.wrapper.AppState;
import com.foobnix.pdf.info.wrapper.DocumentController;
import com.foobnix.pdf.search.activity.HorizontalViewActivity;
import com.foobnix.sys.TempHolder;
import com.foobnix.ui2.fragment.PrefFragment2;
import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.InterstitialAd;
import com.google.android.gms.ads.NativeExpressAdView;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.SearchManager;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Parcelable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v4.view.ViewPager.OnPageChangeListener;
import android.view.KeyEvent;
import android.view.View;
import android.widget.FrameLayout;

@SuppressLint("NewApi")
class MainBrowserActivity extends FragmentActivity {
    private RecentFragmet RECENT_FRAGMET = new RecentFragmet();
    private SearchFragment SEARCH_FRAGMET = new SearchFragment();
    private PrefFragment2 PREF_FRAGMET = new PrefFragment2();
    private BookmarksFragment bookmarks = new BookmarksFragment();

    private AdView adView;
    private NativeExpressAdView adViewNative;

    private final List<Fragment> FRAGMENTS = new ArrayList<Fragment>();
    private final List<String> CONTENT = new ArrayList<String>();
    private final List<Integer> ICONS = new ArrayList<Integer>();

    private static int lastPage = 0;

    InterstitialAd mInterstitialAd;
    FrameLayout adFrame;

    Handler hadler;

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        LOG.d("MainBrowserActivity onCreate", getIntent());

        hadler = new Handler();
        if (getIntent() != null && "android.appwidget.action.APPWIDGET_CONFIGURE".equals(getIntent().getAction())) {
            int prefPage = 5;
            lastPage = prefPage;
        }

        if (AppState.getInstance().isWhiteTheme) {
            setTheme(R.style.StyledIndicatorsWhite);
        } else {
            setTheme(R.style.StyledIndicatorsBlack);
        }
        super.onCreate(savedInstanceState);

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

        // MetaCache.get().load(this);

        TintUtil.setStatusBarColor(this);
        // DocumentController.chooseFullScreen(this,
        // AppState.getInstance().isFullScrean());
        DocumentController.chooseFullScreen(this, false);
        DocumentController.applyBrigtness(this);
        DocumentController.doRotation(this);

        setContentView(R.layout.main_tabs);

        PREF_FRAGMET.setSearchFragmet(SEARCH_FRAGMET);

        browseFragmet = new BrowseFragmet();

        FRAGMENTS.add(SEARCH_FRAGMET);
        CONTENT.add("Lirbi - " + getString(R.string.library));
        ICONS.add(R.drawable.glyphicons_2_book_open);

        FRAGMENTS.add(browseFragmet);
        CONTENT.add(getString(R.string.folders));
        ICONS.add(R.drawable.glyphicons_145_folder_open);

        FRAGMENTS.add(RECENT_FRAGMET);
        CONTENT.add(getString(R.string.recent));
        ICONS.add(R.drawable.glyphicons_72_book);

        FRAGMENTS.add(bookmarks);
        CONTENT.add(getString(R.string.bookmarks));
        ICONS.add(R.drawable.glyphicons_73_bookmark);

        FRAGMENTS.add(PREF_FRAGMET);
        CONTENT.add(getString(R.string.preferences));
        ICONS.add(R.drawable.glyphicons_281_settings);

        final TabPagerAdapter adapter = new TabPagerAdapter(getSupportFragmentManager());

        pager = (ViewPager) findViewById(R.id.pager);
        pager.setAdapter(adapter);
        pager.setOffscreenPageLimit(5);

        final SlidingTabLayout indicator = (SlidingTabLayout) findViewById(R.id.slidingTabs);
        indicator.setViewPager(pager);

        indicator.setDividerColors(getResources().getColor(R.color.tint_divider));
        // indicator.setDividerColors(Color.TRANSPARENT);
        indicator.setSelectedIndicatorColors(Color.WHITE);

        indicator.setBackgroundColor(TintUtil.color);

        // indicator.notifyDataSetChanged();
        indicator.setOnPageChangeListener(onChange);

        Analytics.onStart(this);

        // ADS.activate(this, adView);
        ADS.activateNative(this, adViewNative);

        adFrame = (FrameLayout) findViewById(R.id.adFrame);

        Android6.checkPermissions(this);

        List<String> actions = Arrays.asList("android.intent.action.PROCESS_TEXT", "android.intent.action.SEARCH", "android.intent.action.SEND");
        List<String> extras = Arrays.asList(Intent.EXTRA_PROCESS_TEXT_READONLY, Intent.EXTRA_PROCESS_TEXT, SearchManager.QUERY, Intent.EXTRA_TEXT);
        if (getIntent() != null && getIntent().getAction() != null) {
            if (actions.contains(getIntent().getAction())) {
                for (String extra : extras) {
                    final String text = getIntent().getStringExtra(extra);
                    if (TxtUtils.isNotEmpty(text)) {
                        AppState.get().lastA = null;
                        hadler.postDelayed(new Runnable() {

                            @Override
                            public void run() {
                                SEARCH_FRAGMET.sortAndFilterIntent(text);
                            }
                        }, 250);

                        break;
                    }
                }

            }

        }

        LOG.d("lasta", AppState.get().lastA);
        if (HorizontalViewActivity.class.getSimpleName().equals(AppState.get().lastA)) {
            Uri doc = getLastOpenDoc();
            if (doc != null) {
                Intent intent = new Intent(this, HorizontalViewActivity.class);
                intent.setData(doc);
                startActivity(intent);
                LOG.d("Start lasta", AppState.get().lastA);
            }
        } else if (ViewerActivity.class.getSimpleName().equals(AppState.get().lastA)) {
            Uri doc = getLastOpenDoc();
            if (doc != null) {
                Intent intent = new Intent(this, ViewerActivity.class);
                intent.setData(doc);
                startActivity(intent);
                LOG.d("Start lasta", AppState.get().lastA);
            }

        }

        // new DroidActivate(this);
        FontExtractor.extractFonts(this);

    }

    public void closeActivity() {
        if (mInterstitialAd != null && mInterstitialAd.isLoaded()) {
            mInterstitialAd.show();
        } else {
            TempHolder.listHash = 0;
            finish();
            // android.os.Process.killProcess(android.os.Process.myPid());
        }
    }

    public Uri getLastOpenDoc() {

        List<Uri> recent = AppSharedPreferences.get().getRecent();
        if (recent != null && recent.size() >= 1) {
            return recent.get(0);
        }
        return null;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        Android6.onRequestPermissionsResult(this, requestCode, permissions, grantResults);
    }

    @Override
    public boolean onKeyLongPress(final int keyCode, final KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            return true;
        }
        return super.onKeyLongPress(keyCode, event);
    }

    @Override
    protected void onResume() {
        super.onResume();
        // ADS.onResume(adView);
        ADS.onResumeNative(adViewNative);
        // DocumentController.chooseFullScreen(this,
        // AppState.getInstance().isFullScrean());
        DocumentController.chooseFullScreen(this, false);
        TintUtil.updateAll();
        AppState.get().lastA = MainBrowserActivity.class.getSimpleName();
    };

    @Override
    public void onConfigurationChanged(final Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        // ADS.activate(this, adView);
        ADS.activateNative(this, adViewNative);
        LOG.d("onConfigurationChanged");
    }

    @Override
    protected void onPause() {
        super.onPause();
        LOG.d("onPause", this.getClass());
        AppState.getInstance().save(this);
        ADS.onPause(adView);
        ADS.onPauseNative(adViewNative);
    };

    OnPageChangeListener onChange = new OnPageChangeListener() {

        @Override
        public void onPageScrollStateChanged(final int arg0) {
            // TODO Auto-generated method stub

        }

        @Override
        public void onPageScrolled(final int arg0, final float arg1, final int arg2) {
            // TODO Auto-generated method stub

        }

        @Override
        public void onPageSelected(final int page) {
            lastPage = page;
            adFrame.setVisibility(View.VISIBLE);

            RECENT_FRAGMET.adsPause();
            bookmarks.adsPause();

            if (page == 0) {
                SEARCH_FRAGMET.onSelected();
            }

            if (page == 1) {
                browseFragmet.onSelected();

            }

            if (page == 2) {
                if (RECENT_FRAGMET.hideBottomBanner()) {
                    adFrame.setVisibility(View.INVISIBLE);
                }
                RECENT_FRAGMET.onSelected();
                RECENT_FRAGMET.adsResume();

            } else if (page == 3) {
                if (bookmarks.hideBottomBanner()) {
                    adFrame.setVisibility(View.INVISIBLE);
                }

                bookmarks.filterByText();
                bookmarks.adsResume();

            }
        }

    };
    private BrowseFragmet browseFragmet;
    private ViewPager pager;

    public class TabPagerAdapter extends FragmentStatePagerAdapter {
        public TabPagerAdapter(final FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(final int position) {
            return FRAGMENTS.get(position);

        }

        @Override
        public CharSequence getPageTitle(final int position) {
            return CONTENT.get(position % CONTENT.size()).toUpperCase(Locale.getDefault());
        }

        public int getIconResId(final int index) {
            return ICONS.get(index);
        }

        @Override
        public int getCount() {
            return CONTENT.size();
        }

        @Override
        public int getItemPosition(Object object) {
            return POSITION_NONE;
        }

        @Override
        public void restoreState(Parcelable state, ClassLoader loader) {
        }

        @Override
        public Parcelable saveState() {
            return null;
        }

    }

    @Override
    public void onBackPressed() {
        if (pager.getCurrentItem() == 0 && SEARCH_FRAGMET.isBackProccesed()) {
            return;
        }
        if (pager.getCurrentItem() == 1 && browseFragmet.isBackProccesed()) {
            return;
        }

        AlertDialog.Builder dialog = new AlertDialog.Builder(this);
        dialog.setTitle(R.string.close_application_);
        dialog.setPositiveButton(R.string.no, new OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {
            }
        });
        dialog.setNegativeButton(R.string.yes, new OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {
                closeActivity();
            }
        });
        dialog.show();

    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Analytics.onStop(this);
        ADS.destory(adView);
        ADS.destoryNative(adViewNative);
    }
}
