package com.foobnix.pdf.search.activity;

import android.annotation.TargetApi;
import android.app.ActionBar.LayoutParams;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Parcelable;
import android.text.InputType;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.MenuItem;
import android.view.MenuItem.OnMenuItemClickListener;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.view.ViewGroup;
import android.view.ViewTreeObserver.OnGlobalLayoutListener;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.view.animation.TranslateAnimation;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.fragment.app.Fragment;
import androidx.viewpager.widget.ViewPager;
import androidx.viewpager.widget.ViewPager.OnPageChangeListener;

import com.foobnix.android.utils.Apps;
import com.foobnix.android.utils.Dips;
import com.foobnix.android.utils.Keyboards;
import com.foobnix.android.utils.LOG;
import com.foobnix.android.utils.ResultResponse;
import com.foobnix.android.utils.TxtUtils;
import com.foobnix.android.utils.Vibro;
import com.foobnix.android.utils.Views;
import com.foobnix.drive.GFile;
import com.foobnix.ext.CacheZipUtils;
import com.foobnix.model.AppProfile;
import com.foobnix.model.AppSP;
import com.foobnix.model.AppState;
import com.foobnix.pdf.CopyAsyncTask;
import com.foobnix.pdf.info.ADS;
import com.foobnix.pdf.info.Android6;
import com.foobnix.pdf.info.AppsConfig;
import com.foobnix.pdf.info.BookmarksData;
import com.foobnix.pdf.info.DictsHelper;
import com.foobnix.pdf.info.ExtUtils;
import com.foobnix.pdf.info.IMG;
import com.foobnix.pdf.info.OutlineHelper;
import com.foobnix.pdf.info.OutlineHelper.Info;
import com.foobnix.pdf.info.PasswordDialog;
import com.foobnix.pdf.info.R;
import com.foobnix.pdf.info.TintUtil;
import com.foobnix.pdf.info.UiSystemUtils;
import com.foobnix.pdf.info.model.OutlineLinkWrapper;
import com.foobnix.pdf.info.view.AlertDialogs;
import com.foobnix.pdf.info.view.AnchorHelper;
import com.foobnix.pdf.info.view.BookmarkPanel;
import com.foobnix.pdf.info.view.BrightnessHelper;
import com.foobnix.pdf.info.view.Dialogs;
import com.foobnix.pdf.info.view.DialogsPlaylist;
import com.foobnix.pdf.info.view.DragingDialogs;
import com.foobnix.pdf.info.view.HorizontallSeekTouchEventListener;
import com.foobnix.pdf.info.view.HypenPanelHelper;
import com.foobnix.pdf.info.view.MyPopupMenu;
import com.foobnix.pdf.info.view.ProgressDraw;
import com.foobnix.pdf.info.view.UnderlineImageView;
import com.foobnix.pdf.info.widget.DraggbleTouchListener;
import com.foobnix.pdf.info.widget.ShareDialog;
import com.foobnix.pdf.info.wrapper.DocumentController;
import com.foobnix.pdf.info.wrapper.MagicHelper;
import com.foobnix.pdf.search.activity.msg.FlippingStart;
import com.foobnix.pdf.search.activity.msg.FlippingStop;
import com.foobnix.pdf.search.activity.msg.InvalidateMessage;
import com.foobnix.pdf.search.activity.msg.MessageAutoFit;
import com.foobnix.pdf.search.activity.msg.MessageEvent;
import com.foobnix.pdf.search.activity.msg.MessagePageXY;
import com.foobnix.pdf.search.activity.msg.MessegeBrightness;
import com.foobnix.pdf.search.view.CloseAppDialog;
import com.foobnix.pdf.search.view.VerticalViewPager;
import com.foobnix.sys.ClickUtils;
import com.foobnix.sys.TempHolder;
import com.foobnix.tts.MessagePageNumber;
import com.foobnix.tts.TTSControlsView;
import com.foobnix.tts.TTSEngine;
import com.foobnix.tts.TTSNotification;
import com.foobnix.tts.TTSService;
import com.foobnix.tts.TtsStatus;
import com.foobnix.ui2.AdsFragmentActivity;
import com.foobnix.ui2.AppDB;
import com.foobnix.ui2.MainTabs2;
import com.foobnix.ui2.MyContextWrapper;

import org.ebookdroid.common.settings.SettingsManager;
import org.ebookdroid.common.settings.books.SharedBooks;
import org.ebookdroid.droids.mupdf.codec.exceptions.MuPdfPasswordException;
import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class HorizontalViewActivity extends AdsFragmentActivity {

    public boolean prev = true;
    VerticalViewPager viewPager;
    SeekBar seekBar;
    TextView toastBrightnessText, floatingBookmarkTextView, maxSeek, currentSeek, pagesCountIndicator, flippingIntervalView, pagesTime, pagesTime1, pagesPower, titleTxt, chapterView, modeName, pannelBookTitle;
    View adFrame, bottomBar, bottomIndicators, onClose, overlay, pagesBookmark, musicButtonPanel, parentParent;
    LinearLayout actionBar, bottomPanel;
    TTSControlsView ttsActive;
    FrameLayout anchor;
    UnderlineImageView onCrop, onBC;
    ImageView moveCenter, lockModelImage, linkHistory, onModeChange, outline, onMove, textToSpeach, onPageFlip1, anchorX, anchorY;
    HorizontalModeController dc;
    Handler handler = new Handler(Looper.getMainLooper());
    Handler flippingHandler = new Handler(Looper.getMainLooper());
    Handler handlerTimer = new Handler(Looper.getMainLooper());
    CopyAsyncTask loadinAsyncTask;
    Dialog rotatoinDialog;
    volatile Boolean isInitPosistion = null;
    volatile int isInitOrientation;
    ProgressDraw progressDraw;
    LinearLayout pageshelper;
    String quickBookmark;
    ClickUtils clickUtils;

    int flippingTimer = 0;
    boolean isFlipping = false;
    Runnable reloadDocBrigntness = new Runnable() {

        @Override
        public void run() {
            onBC.underline(AppState.get().isEnableBC);
            IMG.clearMemoryCache();
            int position = viewPager.getCurrentItem();
            ImagePageFragment f2 = (ImagePageFragment) getSupportFragmentManager().findFragmentByTag("f" + (viewPager.getCurrentItem()));
            LOG.d("reloadDocBrigntness", f2);
            if (f2 != null) {

                final Bundle b = new Bundle();
                b.putInt(ImagePageFragment.POS, position);
                b.putBoolean(ImagePageFragment.IS_TEXTFORMAT, dc.isTextFormat());
                b.putString(ImagePageFragment.PAGE_PATH, dc.getPageUrl(position).toString());

                f2.setArguments(b);

                f2.loadImageGlide();
            }
            return;
        }
    };
    Runnable closeRunnable = new Runnable() {

        @Override
        public void run() {
            LOG.d("Close App");
            if (dc != null) {
                dc.saveCurrentPageAsync();
                dc.onCloseActivityAdnShowInterstial();
                dc.closeActivity();
            } else {
                finish();
            }
            MainTabs2.closeApp(HorizontalViewActivity.this);
        }
    };
    long lastClick = 0;
    long lastClickMaxTime = 300;
    Runnable flippingRunnable = new Runnable() {

        @Override
        public void run() {
            LOG.d("flippingRunnable");
            if (flippingTimer >= AppState.get().flippingInterval) {
                flippingTimer = 0;
                if (dc.getCurentPage() == dc.getPageCount() - 1) {
                    if (AppState.get().isLoopAutoplay) {
                        dc.onGoToPage(1);
                    } else {
                        onFlippingStop(null);
                        return;
                    }
                } else {
                    nextPage();
                }
            }

            flippingTimer += 1;
            flippingIntervalView.setText("{" + (AppState.get().flippingInterval - flippingTimer + 1) + "}");
            flippingIntervalView.setVisibility(AppState.get().isShowToolBar ? View.VISIBLE : View.GONE);
            flippingHandler.postDelayed(flippingRunnable, 1000);

        }
    };
    Runnable onCloseDialog = new Runnable() {

        @Override
        public void run() {
            if (AppState.get().selectedText != null) {
                AppState.get().selectedText = null;
                EventBus.getDefault().post(new InvalidateMessage());
            }
        }
    };
    Runnable updateTimePower = new Runnable() {

        @Override
        public void run() {
            LOG.d("Update time and updateTimePower");
            try {
                if (pagesTime != null) {
                    pagesTime.setText(UiSystemUtils.getSystemTime(HorizontalViewActivity.this));
                    pagesTime1.setText(UiSystemUtils.getSystemTime(HorizontalViewActivity.this));

                    int myLevel = UiSystemUtils.getPowerLevel(HorizontalViewActivity.this);
                    pagesPower.setText(myLevel + "%");
                }
            } catch (Exception e) {
                LOG.e(e);
            }
            LOG.d("Update time and power");
            handlerTimer.postDelayed(updateTimePower, AppState.APP_UPDATE_TIME_IN_UI);

        }
    };
    Runnable clearFlags = new Runnable() {

        @Override
        public void run() {
            try {
                getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                LOG.d("FLAG clearFlags", "FLAG_KEEP_SCREEN_ON", "clear");
            } catch (Exception e) {
                LOG.e(e);
            }
        }
    };
    UpdatableFragmentPagerAdapter pagerAdapter;
    Runnable onRefresh = new Runnable() {

        @Override
        public void run() {
            dc.saveCurrentPageAsync();
            updateUI(viewPager.getCurrentItem());
            showHideInfoToolBar();
            updateSeekBarColorAndSize();
            BrightnessHelper.updateOverlay(overlay);
            hideShow();
            TTSEngine.get().stop();
            showPagesHelper();

        }
    };
    public View.OnClickListener onBookmarks = new View.OnClickListener() {

        @Override
        public void onClick(final View v) {
            DragingDialogs.showBookmarksDialog(anchor, dc, new Runnable() {

                @Override
                public void run() {
                    showHideHistory();
                    showPagesHelper();
                    updateUI(dc.getCurrentPage());
                }
            });
        }
    };
    View.OnLongClickListener onBookmarksLong = new View.OnLongClickListener() {

        @Override
        public boolean onLongClick(final View arg0) {
            DragingDialogs.addBookmarksLong(anchor, dc);
            showPagesHelper();
            return true;
        }
    };
    Runnable doShowHideWrapperControllsRunnable = new Runnable() {

        @Override
        public void run() {
            doShowHideWrapperControlls();
        }
    };
    SeekBar.OnSeekBarChangeListener onSeek = new SeekBar.OnSeekBarChangeListener() {

        @Override
        public void onStopTrackingTouch(final SeekBar seekBar) {
        }

        @Override
        public void onStartTrackingTouch(final SeekBar seekBar) {
        }

        @Override
        public void onProgressChanged(final SeekBar seekBar, final int progress, final boolean fromUser) {
            // updateUI(progress);
            viewPager.setCurrentItem(progress, false);
            flippingTimer = 0;

            if (AppState.get().isEditMode && !fromUser) {
                AppState.get().isEditMode = false;
                hideShow();
            }
            if (fromUser) {
                //Apps.accessibilityText(HorizontalViewActivity.this, getString(R.string.m_current_page) + " " + dc.getCurentPageFirst1());

            }
        }
    };
    long keyTimeout = 0;
    private int currentScrollState;
    OnPageChangeListener onViewPagerChangeListener = new OnPageChangeListener() {

        @Override
        public void onPageSelected(final int pos) {
            PageImageState.currentPage = pos;
            dc.setCurrentPage(viewPager.getCurrentItem());
            updateUI(pos);

            if (PageImageState.get().isAutoFit) {
                EventBus.getDefault().post(new MessageAutoFit(pos));
            }


            if (AppState.get().inactivityTime > 0) {
                getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                LOG.d("FLAG addFlags", "FLAG_KEEP_SCREEN_ON", "add", AppState.get().inactivityTime);
                handler.removeCallbacks(clearFlags);
                handler.postDelayed(clearFlags, TimeUnit.MINUTES.toMillis(AppState.get().inactivityTime));
            }

            LOG.d("onPageSelected", pos);

            progressDraw.updateProgress(pos);

            EventBus.getDefault().post(new MessagePageXY(MessagePageXY.TYPE_HIDE));

            if (!TTSEngine.get().isPlaying()) {
                Apps.accessibilityText(HorizontalViewActivity.this, getString(R.string.m_current_page) + " " + dc.getCurentPageFirst1());
            }


        }

        @Override
        public void onPageScrolled(int arg0, float arg1, int arg2) {
            // TODO Auto-generated method stub

        }

        @Override
        public void onPageScrollStateChanged(int arg0) {
            currentScrollState = arg0;
        }
    };
    Runnable reloadDoc = new Runnable() {

        @Override
        public void run() {
            onBC.underline(AppState.get().isEnableBC);
            // dc.getOutline(null, false);
            //dc.saveCurrentPageAsync();
            createAdapter();

            loadUI();
            dc.onGoToPage(dc.getCurentPage() + 1);
        }
    };
    private volatile boolean isMyKey = false;

    @Override
    protected void onNewIntent(final Intent intent) {

        if (TTSNotification.ACTION_TTS.equals(intent.getAction())) {
            return;
        }

        if (!intent.filterEquals(getIntent())) {
            finish();
            startActivity(intent);
        }
    }

    protected void onCreateTest(final Bundle savedInstanceState) {
        DocumentController.doRotation(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_horiziontal_view);
    }

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        quickBookmark = getString(R.string.fast_bookmark);
        intetrstialTimeoutSec = ADS.FULL_SCREEN_TIMEOUT_SEC;
        LOG.d("getRequestedOrientation", AppState.get().orientation, getRequestedOrientation());


        flippingTimer = 0;

        long crateBegin = System.currentTimeMillis();

        requestWindowFeature(Window.FEATURE_NO_TITLE);
        BrightnessHelper.applyBrigtness(this);

        if (AppState.get().isDayNotInvert) {
            setTheme(R.style.StyledIndicatorsWhite);
        } else {
            setTheme(R.style.StyledIndicatorsBlack);
        }

        DocumentController.doRotation(this);
        DocumentController.doContextMenu(this);

        clickUtils = new ClickUtils();

        super.onCreate(savedInstanceState);

        //FirebaseAnalytics.getInstance(this);

        if (PasswordDialog.isNeedPasswordDialog(this)) {
            return;
        }
        boolean isTextFomat = ExtUtils.isTextFomat(getIntent());


        // AppSP.get().isCut = false;
        PageImageState.get().isShowCuttingLine = false;

        PageImageState.get().cleanSelectedWords();

        setContentView(R.layout.activity_horiziontal_view);

        if (!Android6.canWrite(this)) {
            Android6.checkPermissions(this, true);
            return;
        }


        findViewById(R.id.showHypenLangPanel).setVisibility(View.GONE);

        viewPager = (VerticalViewPager) findViewById(R.id.pager2);
        viewPager.setAccessibilityDelegate(new View.AccessibilityDelegate());

        parentParent = findViewById(R.id.parentParent);
        pannelBookTitle = findViewById(R.id.pannelBookTitle);

        overlay = findViewById(R.id.overlay);
        overlay.setVisibility(View.VISIBLE);

        progressDraw = (ProgressDraw) findViewById(R.id.progressDraw);

        actionBar = (LinearLayout) findViewById(R.id.actionBar);
        bottomPanel = (LinearLayout) findViewById(R.id.bottomPanel);

        bottomBar = findViewById(R.id.bottomBar);
        bottomIndicators = findViewById(R.id.bottomIndicators);
        adFrame = findViewById(R.id.adFrame);
        anchor = (FrameLayout) findViewById(R.id.anchor);

        anchorX = (ImageView) findViewById(R.id.anchorX);
        anchorY = (ImageView) findViewById(R.id.anchorY);
        floatingBookmarkTextView = findViewById(R.id.floatingBookmark);
        floatingBookmarkTextView.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (dc == null) {
                    return;
                }
                dc.floatingBookmark = null;
                onRefresh.run();
                onBookmarks.onClick(v);
            }
        });
        floatingBookmarkTextView.setOnLongClickListener(new OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                if (dc == null) {
                    return true;
                }
                dc.floatingBookmark = null;
                onRefresh.run();
                return true;
            }
        });

        TintUtil.setTintImageWithAlpha(anchorX, AppState.get().isDayNotInvert ? Color.BLUE : Color.YELLOW, 150);
        TintUtil.setTintImageWithAlpha(anchorY, AppState.get().isDayNotInvert ? Color.BLUE : Color.YELLOW, 150);

        anchorX.setVisibility(View.GONE);
        anchorY.setVisibility(View.GONE);

        DraggbleTouchListener touch1 = new DraggbleTouchListener(anchorX, (View) anchorX.getParent());
        DraggbleTouchListener touch2 = new DraggbleTouchListener(anchorY, (View) anchorY.getParent());

        final Runnable onMoveActionOnce = new Runnable() {

            @Override
            public void run() {
                float x = anchorX.getX() + anchorX.getWidth();
                float y = anchorX.getY() + anchorX.getHeight() / 2;

                float x1 = anchorY.getX();
                float y1 = anchorY.getY();
                EventBus.getDefault().post(new MessagePageXY(MessagePageXY.TYPE_SELECT_TEXT, viewPager.getCurrentItem(), x, y, x1, y1));
            }
        };

        final Runnable onMoveAction = new Runnable() {

            @Override
            public void run() {
                handler.removeCallbacks(onMoveActionOnce);
                handler.postDelayed(onMoveActionOnce, 150);

            }
        };

        Runnable onMoveFinish = new Runnable() {

            @Override
            public void run() {
                onMoveAction.run();
                if (AppState.get().isRememberDictionary) {
                    DictsHelper.runIntent(dc.getActivity(), AppState.get().selectedText);
                } else {
                    DragingDialogs.selectTextMenu(anchor, dc, true, onRefresh);
                }

            }
        };

        touch1.setOnMoveFinish(onMoveFinish);
        touch2.setOnMoveFinish(onMoveFinish);

        touch1.setOnMove(onMoveAction);
        touch2.setOnMove(onMoveAction);

        moveCenter = findViewById(R.id.moveCenter);

        currentSeek = (TextView) findViewById(R.id.currentSeek);
        maxSeek = (TextView) findViewById(R.id.maxSeek);

        toastBrightnessText = (TextView) findViewById(R.id.toastBrightnessText);
        toastBrightnessText.setVisibility(View.GONE);
        TintUtil.setDrawableTint(toastBrightnessText.getCompoundDrawables()[0], Color.WHITE);

        modeName = (TextView) findViewById(R.id.modeName);
        modeName.setText(AppState.get().nameHorizontalMode);

        pagesCountIndicator = (TextView) findViewById(R.id.pagesCountIndicator);
        flippingIntervalView = (TextView) findViewById(R.id.flippingIntervalView);
        pagesTime = (TextView) findViewById(R.id.pagesTime);
        pagesTime1 = (TextView) findViewById(R.id.pagesTime1);
        pagesTime1.setVisibility(AppState.get().fullScreenMode == AppState.FULL_SCREEN_NORMAL ? View.GONE : View.VISIBLE);


        pagesPower = (TextView) findViewById(R.id.pagesPower);
        linkHistory = (ImageView) findViewById(R.id.linkHistory);
        onMove = (ImageView) findViewById(R.id.onMove);
        onMove.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                DragingDialogs.onMoveDialog(anchor, dc, onRefresh, reloadDoc);
            }
        });

        currentSeek.setOnLongClickListener(new OnLongClickListener() {

            @Override
            public boolean onLongClick(View v) {
                Dialogs.showDeltaPage(anchor, dc, dc.getCurentPageFirst1(), onRefresh);
                return true;
            }
        });
        maxSeek.setOnLongClickListener(new OnLongClickListener() {

            @Override
            public boolean onLongClick(View v) {
                Dialogs.showDeltaPage(anchor, dc, dc.getCurentPageFirst1(), onRefresh);
                return true;
            }
        });

        updateSeekBarColorAndSize();

        titleTxt = (TextView) findViewById(R.id.title);
        chapterView = (TextView) findViewById(R.id.chapter);

        linkHistory.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                dc.onLinkHistory();
                showHideHistory();
            }
        });
        linkHistory.setVisibility(View.GONE);

        seekBar = (SeekBar) findViewById(R.id.seekBar);

        if (AppState.get().isRTL) {
            if (Build.VERSION.SDK_INT >= 11) {
                seekBar.setRotation(180);
            }
        }

        onPageFlip1 = findViewById(R.id.autoScroll);
        onPageFlip1.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                DragingDialogs.pageFlippingDialog(anchor, dc, onRefresh);
            }
        });

        pageshelper = (LinearLayout) findViewById(R.id.pageshelper);
        musicButtonPanel = findViewById(R.id.musicButtonPanel);
        musicButtonPanel.setVisibility(View.GONE);

        pagesBookmark = findViewById(R.id.pagesBookmark);
        pagesBookmark.setOnClickListener(onBookmarks);
        pagesBookmark.setOnLongClickListener(onBookmarksLong);

        final ImageView onFullScreen = (ImageView) findViewById(R.id.onFullScreen);
        onFullScreen.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                if (dc == null) {
                    return;
                }

                DocumentController.showFullScreenPopup(dc.getActivity(), v, id -> {
                    AppState.get().fullScreenMode = id;
                    DocumentController.chooseFullScreen(HorizontalViewActivity.this, AppState.get().fullScreenMode);
                    onFullScreen.setImageResource(DocumentController.getFullScreenIcon(HorizontalViewActivity.this, AppState.get().fullScreenMode));
                    if (dc.isTextFormat()) {
                        if (onRefresh != null) {
                            onRefresh.run();
                        }
                        nullAdapter();
                        dc.restartActivity();
                    }
                    return true;
                }, AppState.get().fullScreenMode);


            }
        });
        onFullScreen.setImageResource(DocumentController.getFullScreenIcon(HorizontalViewActivity.this, AppState.get().fullScreenMode));

        ImageView dayNightButton = (ImageView) findViewById(R.id.bookNight);
        dayNightButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(final View v) {
                if (dc == null) {
                    return;
                }
                v.setEnabled(false);
                AppState.get().isDayNotInvert = !AppState.get().isDayNotInvert;
                nullAdapter();
                dc.restartActivity();
            }
        });
        // if (Dips.isEInk(this)) {
        // dayNightButton.setVisibility(View.GONE);
        // AppState.get().isDayNotInvert = true;
        // }

        onBC = (UnderlineImageView) findViewById(R.id.onBC);
        onBC.underline(AppState.get().isEnableBC);
        onBC.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(final View v) {
                DragingDialogs.contrastAndBrigtness(anchor, dc, reloadDocBrigntness, reloadDoc);
            }
        });

        dayNightButton.setImageResource(!AppState.get().isDayNotInvert ? R.drawable.glyphicons_232_sun : R.drawable.glyphicons_2_moon);

        moveCenter.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                authoFit();
            }
        });

        onBC.setVisibility(isTextFomat ? View.GONE : View.VISIBLE);

        if (DocumentController.isEinkOrMode(this) || AppState.get().isEnableBC) {
            onBC.setVisibility(View.VISIBLE);
        }
        onMove.setVisibility(DocumentController.isEinkOrMode(this) && !isTextFomat ? View.VISIBLE : View.GONE);

        findViewById(R.id.thumbnail).setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(final View v) {
                DragingDialogs.gotoPageDialog(anchor, dc);

            }
        });
        findViewById(R.id.onShowSearch).setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(final View v) {
                showSearchDialog();
            }

        });

        outline = (ImageView) findViewById(R.id.onDocDontext);
        outline.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(final View v) {
                if (dc != null) {
                    DragingDialogs.showContent(anchor, dc);
                }
            }
        });
        findViewById(R.id.onBookmarks).setOnClickListener(onBookmarks);

        findViewById(R.id.onBookmarks).setOnLongClickListener(onBookmarksLong);

        findViewById(R.id.onRecent).setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(final View v) {
                DragingDialogs.recentBooks(anchor, dc);
            }
        });

        textToSpeach = (ImageView) findViewById(R.id.textToSpeach);
        textToSpeach.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(final View v) {
                LOG.d("bookTTS", AppSP.get().isDoubleCoverAlone, AppSP.get().isDouble, AppSP.get().isCut);
                if (AppSP.get().isDouble || AppSP.get().isCut) {
                    modeOnePage();
                    return;
                }
                DragingDialogs.textToSpeachDialog(anchor, dc);
            }
        });
        textToSpeach.setOnLongClickListener(v -> {
            AlertDialogs.showTTSDebug(dc);
            hideShow();
            return true;
        });
        ttsActive = findViewById(R.id.ttsActive);

        // ttsActive.setOnClickListener(new View.OnClickListener() {
        //
        // @Override
        // public void onClick(final View v) {
        // DragingDialogs.textToSpeachDialog(anchor, dc);
        // }
        // });

        onModeChange = (ImageView) findViewById(R.id.onModeChange);
        onModeChange.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                if (dc == null) {
                    return;
                }

                MyPopupMenu p = new MyPopupMenu(v.getContext(), v);
                p.getMenu().add(R.string.one_page).setIcon(R.drawable.glyphicons_two_page_one).setOnMenuItemClickListener(new OnMenuItemClickListener() {

                    @Override
                    public boolean onMenuItemClick(MenuItem item) {
                        closeDialogs();
                        onModeChange.setImageResource(R.drawable.glyphicons_two_page_one);
                        AppSP.get().isDouble = false;
                        AppSP.get().isDoubleCoverAlone = false;
                        AppSP.get().isCut = false;
                        AppSP.get().isSmartReflow = false;

                        SettingsManager.getBookSettings().updateFromAppState();
                        SharedBooks.save(SettingsManager.getBookSettings());


                        if (dc.isTextFormat()) {
                            nullAdapter();
                            dc.restartActivity();
                            dc.cleanImageMatrix();
                        } else {
                            TTSEngine.get().stop();
                            dc.cleanImageMatrix();
                            reloadDoc.run();
                            authoFit();
                        }
                        return false;
                    }
                });
                p.getMenu().add(R.string.two_pages).setIcon(R.drawable.glyphicons_two_pages_12).setOnMenuItemClickListener(new OnMenuItemClickListener() {

                    @Override
                    public boolean onMenuItemClick(MenuItem item) {

                        closeDialogs();
                        onModeChange.setImageResource(R.drawable.glyphicons_two_pages_12);
                        AppSP.get().isDouble = true;
                        AppSP.get().isCut = false;
                        AppSP.get().isDoubleCoverAlone = false;
                        AppSP.get().isSmartReflow = false;

                        SettingsManager.getBookSettings().updateFromAppState();
                        SharedBooks.save(SettingsManager.getBookSettings());


                        if (dc.isTextFormat()) {
                            nullAdapter();
                            dc.restartActivity();
                            dc.cleanImageMatrix();
                        } else {
                            TTSEngine.get().stop();
                            dc.cleanImageMatrix();
                            reloadDoc.run();
                            authoFit();
                        }
                        return false;
                    }
                });
                if (!dc.isTextFormat()) {
                    p.getMenu().add(R.string.two_pages_cover).setIcon(R.drawable.glyphicons_two_pages_23).setOnMenuItemClickListener(new OnMenuItemClickListener() {

                        @Override
                        public boolean onMenuItemClick(MenuItem item) {

                            closeDialogs();
                            onModeChange.setImageResource(R.drawable.glyphicons_two_pages_23);
                            AppSP.get().isDouble = true;
                            AppSP.get().isCut = false;
                            AppSP.get().isDoubleCoverAlone = true;
                            AppSP.get().isSmartReflow = false;
                            SettingsManager.getBookSettings().updateFromAppState();
                            SharedBooks.save(SettingsManager.getBookSettings());


                            if (dc.isTextFormat()) {
                                nullAdapter();
                                dc.restartActivity();
                                dc.cleanImageMatrix();
                            } else {
                                TTSEngine.get().stop();
                                dc.cleanImageMatrix();
                                reloadDoc.run();
                                authoFit();
                            }
                            return false;
                        }
                    });
                }
                if (!dc.isTextFormat()) {
                    p.getMenu().add(R.string.half_page).setIcon(R.drawable.glyphicons_page_split).setOnMenuItemClickListener(new OnMenuItemClickListener() {

                        @Override
                        public boolean onMenuItemClick(MenuItem item) {

                            closeDialogs();
                            onModeChange.setImageResource(R.drawable.glyphicons_page_split);
                            AppSP.get().isDouble = false;
                            AppSP.get().isCut = true;
                            AppSP.get().isSmartReflow = false;
                            // AppSP.get().isCrop = false;
                            SettingsManager.getBookSettings().updateFromAppState();
                            SharedBooks.save(SettingsManager.getBookSettings());

                            TTSEngine.get().stop();

                            // onCrop.underline(AppSP.get().isCrop);

                            dc.cleanImageMatrix();
                            reloadDoc.run();
                            authoFit();
                            return false;
                        }
                    });
                }
                if ((AppsConfig.IS_BETA || AppState.get().isExperimental) && !dc.isTextFormat()) {
                    p.getMenu().add("Smart Reflow").setIcon(R.drawable.glyphicons_108_text_resize).setOnMenuItemClickListener(new OnMenuItemClickListener() {

                        @Override
                        public boolean onMenuItemClick(MenuItem item) {
                            closeDialogs();
                            onModeChange.setImageResource(R.drawable.glyphicons_108_text_resize);
                            AppSP.get().isDouble = false;
                            AppSP.get().isCut = false;
                            AppSP.get().isCrop = false;
                            AppSP.get().isSmartReflow = true;

                            SettingsManager.getBookSettings().updateFromAppState();
                            SharedBooks.save(SettingsManager.getBookSettings());

                            TTSEngine.get().stop();

                            // onCrop.underline(AppSP.get().isCrop);

                            dc.cleanImageMatrix();
                            reloadDoc.run();
                            authoFit();
                            return false;
                        }
                    });
                }

                p.show();
                Keyboards.hideNavigation(HorizontalViewActivity.this);

            }
        });

        onCrop = (UnderlineImageView) findViewById(R.id.onCrop);
        onCrop.setVisibility(isTextFomat && !AppSP.get().isCrop ? View.GONE : View.VISIBLE);

        final Runnable onCropChange = () -> {
            SettingsManager.getBookSettings().cp = AppSP.get().isCrop;
            reloadDocBrigntness.run();
            onCrop.underline(AppSP.get().isCrop);

            PageImageState.get().isAutoFit = true;
            EventBus.getDefault().post(new MessageAutoFit(viewPager.getCurrentItem()));

            AppState.get().isEditMode = false;
            hideShow();
        };

        onCrop.setOnClickListener(v -> DragingDialogs.customCropDialog(anchor, dc, onCropChange));
        onCrop.setOnLongClickListener(v -> {
            AppSP.get().isCrop = !AppSP.get().isCrop;
            onCropChange.run();
            return true;
        });

        final View bookMenu = findViewById(R.id.bookMenu);
        bookMenu.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(final View v) {
                if (dc == null || dc.getCurrentBook() == null) {
                    return;
                }
                ShareDialog.show(HorizontalViewActivity.this, dc.getCurrentBook(), new Runnable() {

                    @Override
                    public void run() {
                        if (dc.getCurrentBook().delete()) {
                            TempHolder.listHash++;
                            AppDB.get().deleteBy(dc.getCurrentBook().getPath());
                            dc.getActivity().finish();
                        }
                    }
                }, dc.getCurentPage(), dc, new Runnable() {

                    @Override
                    public void run() {
                        hideShow();

                    }
                });
                Keyboards.hideNavigation(HorizontalViewActivity.this);
                hideAds();

            }
        });
        modeName.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                bookMenu.performClick();
            }
        });
//        modeName.setOnLongClickListener(new OnLongClickListener() {
//
//            @Override
//            public boolean onLongClick(View v) {
//                dc.onChangeTextSelection();
//                AppState.get().isEditMode = false;
//                hideShow();
//
//                return true;
//            }
//        });
        modeName.setOnLongClickListener(onCloseLongClick);


        findViewById(R.id.bookPref).setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(final View v) {
                if (dc != null) {
                    DragingDialogs.preferences(anchor, dc, onRefresh, reloadDoc);
                }
            }
        });

        onClose = findViewById(R.id.bookClose);
        Apps.accessibilityButtonSize(onClose);
        onClose.setVisibility(View.INVISIBLE);

        onClose.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(final View v) {
                nullAdapter();
                closeDialogs();
                showInterstial();
            }

        });

        onClose.setOnLongClickListener(onCloseLongClick);

        findViewById(R.id.editTop2).setVisibility(View.GONE);
        findViewById(R.id.nextTypeBootom).setVisibility(View.GONE);

        lockModelImage = (ImageView) findViewById(R.id.lockUnlock);
        lockModelImage.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                AppSP.get().isLocked = !AppSP.get().isLocked;
                updateLockMode();
            }
        });
        updateLockMode();

        Keyboards.hideNavigationOnCreate(HorizontalViewActivity.this);


        currentSeek.setVisibility(View.GONE);
        maxSeek.setVisibility(View.GONE);
        seekBar.setVisibility(View.INVISIBLE);
        bottomIndicators.setVisibility(View.GONE);

        titleTxt.setText(HorizontalModeController.getTempTitle(this));


        loadinAsyncTask = new CopyAsyncTask() {
            AlertDialog dialog;
            long start = 0;
            private boolean isCancelled = false;

            @Override
            protected void onPreExecute() {
                start = System.currentTimeMillis();
                TempHolder.get().loadingCancelled = false;
                dialog = Dialogs.loadingBook(HorizontalViewActivity.this, new Runnable() {

                    @Override
                    public void run() {
                        isCancelled = true;
                        TempHolder.get().loadingCancelled = true;
                        CacheZipUtils.removeFiles(CacheZipUtils.CACHE_BOOK_DIR.listFiles());
                        finish();
                    }
                });
            }

            ;

            @Override
            protected Object doInBackground(Object... params) {
                try {
                    LOG.d("doRotation(this)", AppState.get().orientation, HorizontalViewActivity.this.getRequestedOrientation());
                    try {
                        while (viewPager.getHeight() == 0) {
                            Thread.sleep(250);
                        }
                        int count = 0;
                        if (AppState.get().orientation == ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE || AppState.get().orientation == ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE) {
                            while (viewPager.getHeight() > viewPager.getWidth() && count < 20) {
                                Thread.sleep(250);
                                count++;
                            }
                        } else if (AppState.get().orientation == ActivityInfo.SCREEN_ORIENTATION_PORTRAIT || AppState.get().orientation == ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT) {
                            while (viewPager.getWidth() > viewPager.getHeight() && count < 20) {
                                Thread.sleep(250);
                                count++;
                            }
                        }

                        HorizontalViewActivity.this.runOnUiThread(new Runnable() {

                            @Override
                            public void run() {
                                updateBannnerTop();
                            }
                        });

                    } catch (InterruptedException e) {
                    }
                    LOG.d("viewPager", viewPager.getHeight() + "x" + viewPager.getWidth());
                    initAsync(viewPager.getWidth(), viewPager.getHeight());
                } catch (MuPdfPasswordException e) {
                    return -1;
                } catch (RuntimeException e) {
                    LOG.e(e);
                    return -2;
                }
                return 0;
            }

            @Override
            protected void onCancelled() {
                try {
                    if (dialog != null) {
                        dialog.dismiss();
                    }
                } catch (Exception e) {
                }
                isCancelled = true;
            }

            ;

            @Override
            protected void onPostExecute(Object result) {
                if (AppsConfig.IS_BETA) {
                    long time = System.currentTimeMillis() - start;
                    float sec = (float) time / 1000;
                    modeName.setText(modeName.getText() + " (" + String.format("%.1f", sec) + " sec" + ")");
                }
                try {
                    // onClose.setVisibility(View.VISIBLE);
                    LOG.d("RESULT", result);
                    if (dialog != null) {
                        dialog.dismiss();
                    }
                } catch (Exception e) {
                }
                if (isCancelled) {
                    LOG.d("Cancelled");
                    finish();
                    return;
                }
                if ((Integer) result == -2) {
                    Toast.makeText(HorizontalViewActivity.this, R.string.msg_unexpected_error, Toast.LENGTH_SHORT).show();
                    AppState.get().isEditMode = true;
                    hideShow();
                    onClose.setVisibility(View.VISIBLE);
                    return;
                }

                if ((Integer) result == -1) {
                    final EditText input = new EditText(HorizontalViewActivity.this);
                    input.setSingleLine(true);
                    input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);

                    AlertDialog.Builder dialog = new AlertDialog.Builder(HorizontalViewActivity.this);
                    dialog.setTitle(R.string.enter_password);
                    dialog.setView(input);
                    dialog.setCancelable(false);
                    dialog.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {

                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                            if (dc != null) {
                                dc.onCloseActivityFinal(null);
                            } else {
                                HorizontalViewActivity.this.finish();
                            }
                        }

                    });
                    dialog.setPositiveButton(R.string.open_file, new DialogInterface.OnClickListener() {

                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            final String txt = input.getText().toString();
                            if (TxtUtils.isNotEmpty(txt)) {
                                dialog.dismiss();

                                final Runnable runnable = () -> {
                                    HorizontalViewActivity.this.finish();
                                    getIntent().putExtra(HorizontalModeController.EXTRA_PASSWORD, txt);
                                    startActivity(getIntent());
                                };
                                if (dc != null) {
                                    dc.onCloseActivityFinal(runnable);
                                } else {
                                    runnable.run();
                                }

                            } else {
                                if (dc == null) {
                                    HorizontalViewActivity.this.finish();
                                } else {
                                    dc.onCloseActivityFinal(null);
                                }
                            }
                        }
                    });
                    AlertDialog show = dialog.show();

                } else {

                    currentSeek.setVisibility(View.VISIBLE);
                    maxSeek.setVisibility(View.VISIBLE);
                    seekBar.setVisibility(View.VISIBLE);
                    bottomIndicators.setVisibility(View.VISIBLE);
                    onModeChange.setVisibility(View.VISIBLE);


                    PageImageState.get().isAutoFit = PageImageState.get().needAutoFit;

                    if (ExtUtils.isTextFomat(getIntent())) {
                        PageImageState.get().isAutoFit = true;
                        // moveCenter.setVisibility(View.GONE);
                    } else if (AppState.get().isLockPDF) {
                        // moveCenter.setVisibility(View.VISIBLE);
                        AppSP.get().isLocked = true;
                    }

                    if (ExtUtils.isNoTextLayerForamt(dc.getCurrentBook().getPath())) {
                        TintUtil.setTintImageWithAlpha(textToSpeach, Color.LTGRAY);
                    }
                    if (dc.isTextFormat()) {
                        // TintUtil.setTintImage(lockModelImage, Color.LTGRAY);
                    }

                    loadUI();

                    // AppState.get().isEditMode = false; //remember last
                    if (AppState.get().isEnableAccessibility) {
                        AppState.get().isEditMode = true;
                    }
                    int pageFromUri = dc.getCurentPage();
                    updateUI(pageFromUri);
                    hideShow();

                    Apps.accessibilityText(HorizontalViewActivity.this, getString(R.string.book_is_open), getString(R.string.m_current_page), " " + dc.getCurentPageFirst1());


                    EventBus.getDefault().post(new MessageAutoFit(pageFromUri));
                    seekBar.setOnSeekBarChangeListener(onSeek);
                    showHideInfoToolBar();


                    isInitPosistion = Dips.screenHeight() > Dips.screenWidth();
                    isInitOrientation = AppState.get().orientation;

                    updateIconMode();

                    onCrop.setVisibility(dc.isTextFormat() && !AppSP.get().isCrop ? View.GONE : View.VISIBLE);
                    onMove.setVisibility(DocumentController.isEinkOrMode(HorizontalViewActivity.this) && !dc.isTextFormat() ? View.VISIBLE : View.GONE);
                    onBC.setVisibility(dc.isTextFormat() ? View.GONE : View.VISIBLE);
                    if (Dips.isEInk() || AppState.get().appTheme == AppState.THEME_INK || AppState.get().isEnableBC) {
                        onBC.setVisibility(View.VISIBLE);
                    }

                    onCrop.underline(AppSP.get().isCrop);
                    onCrop.invalidate();

                    ttsActive.setDC(dc);
                    ttsActive.addOnDialogRunnable(new Runnable() {

                        @Override
                        public void run() {
                            AppState.get().isEditMode = true;
                            hideShow();
                            DragingDialogs.textToSpeachDialog(anchor, dc);
                        }
                    });

                    DialogsPlaylist.dispalyPlaylist(HorizontalViewActivity.this, dc);

                    // RecentUpates.updateAll(HorizontalViewActivity.this);
                    if (dc.getPageCount() == 0) {
                        onClose.setVisibility(View.VISIBLE);
                    }

                    HypenPanelHelper.init(parentParent, dc);

                }

            }

            ;
        };
        loadinAsyncTask.executeOnExecutor(Executors.newSingleThreadExecutor());
        updateIconMode();
        BrightnessHelper.updateOverlay(overlay);

        //
        tinUI();
        LOG.d("INIT end", (float) (System.currentTimeMillis() - crateBegin) / 1000);

        anchor.getViewTreeObserver().addOnGlobalLayoutListener(new OnGlobalLayoutListener() {

            @TargetApi(Build.VERSION_CODES.HONEYCOMB)
            @Override
            public void onGlobalLayout() {

                if (anchor.getVisibility() == View.VISIBLE) {
                    adFrame.setVisibility(View.GONE);
                    adFrame.setClickable(false);
                } else {
                    if (AppState.get().isEditMode && adFrame.getTag() == null) {
                        adFrame.setVisibility(View.VISIBLE);
                        adFrame.setClickable(true);
                    } else {
                        adFrame.setVisibility(View.GONE);
                        adFrame.setClickable(false);
                    }
                }

                if (Build.VERSION.SDK_INT >= 11) {
                    if (anchor.getX() < 0) {
                        anchor.setX(0);
                    }
                    if (anchor.getY() < 0) {
                        anchor.setY(0);
                    }
                }
            }

        });

    }

    OnLongClickListener onCloseLongClick = new OnLongClickListener() {

        @Override
        public boolean onLongClick(final View v) {
            Vibro.vibrate();
            CloseAppDialog.showOnLongClickDialog(HorizontalViewActivity.this, v, dc);
            hideAds();
            return true;
        }
    };

    public void showPagesHelper() {
        try {
            BookmarkPanel.showPagesHelper(pageshelper, musicButtonPanel, dc, pagesBookmark, quickBookmark, onRefresh);
        } catch (Exception e) {
            LOG.e(e);
        }

    }

    @Subscribe
    public void showHideTextSelectors(MessagePageXY event) {
        if (event.getType() == MessagePageXY.TYPE_HIDE) {
            anchorX.setVisibility(View.GONE);
            anchorY.setVisibility(View.GONE);

        }
        if (event.getType() == MessagePageXY.TYPE_SHOW) {
            anchorX.setVisibility(View.VISIBLE);
            anchorY.setVisibility(View.VISIBLE);

            float x = event.getX() - anchorX.getWidth();
            float y = event.getY() - anchorX.getHeight() / 2;

            AnchorHelper.setXY(anchorX, x, y);
            AnchorHelper.setXY(anchorY, event.getX1(), event.getY1());

        }

    }

    @Subscribe
    public void onMessegeBrightness(MessegeBrightness msg) {
        BrightnessHelper.onMessegeBrightness(handler, msg, toastBrightnessText, overlay);
    }

    private boolean closeDialogs() {
        if (dc == null) {
            return false;
        }
        return dc.closeDialogs();
    }

    public void hideAds() {
        adFrame.setTag("");
        adFrame.setVisibility(View.GONE);
    }

    public void updateIconMode() {
        if (AppSP.get().isDouble) {
            if (AppSP.get().isDoubleCoverAlone) {
                onModeChange.setImageResource(R.drawable.glyphicons_two_pages_23);
            } else {
                onModeChange.setImageResource(R.drawable.glyphicons_two_pages_12);
            }
        } else if (AppSP.get().isCut) {
            onModeChange.setImageResource(R.drawable.glyphicons_page_split);
        } else if (AppSP.get().isSmartReflow) {
            onModeChange.setImageResource(R.drawable.glyphicons_108_text_resize);
        } else {
            onModeChange.setImageResource(R.drawable.glyphicons_two_page_one);
        }
    }

    @Override
    protected void attachBaseContext(Context context) {
        super.attachBaseContext(MyContextWrapper.wrap(context));
    }

    public void modeOnePage() {
        onModeChange.setImageResource(R.drawable.glyphicons_two_page_one);
        AppSP.get().isDouble = false;
        AppSP.get().isDoubleCoverAlone = false;
        AppSP.get().isCut = false;
        SettingsManager.getBookSettings().updateFromAppState();
        SharedBooks.save(SettingsManager.getBookSettings());

        nullAdapter();
        dc.restartActivity();
    }

    public void updateSeekBarColorAndSize() {

        TintUtil.setTintText(pagesPower, TintUtil.getStatusBarColor());
        TintUtil.setTintText(pagesTime, TintUtil.getStatusBarColor());
        TintUtil.setTintText(pagesCountIndicator, TintUtil.getStatusBarColor());
        TintUtil.setTintText(pannelBookTitle, TintUtil.getStatusBarColor());
        TintUtil.setTintText(flippingIntervalView, TintUtil.getStatusBarColor());

        if (false) {
            GradientDrawable bg = (GradientDrawable) pagesPower.getBackground();
            bg.setStroke(1, TintUtil.getStatusBarColor());
        } else {
            pagesPower.setBackgroundColor(Color.TRANSPARENT);
        }

        pagesPower.setTextSize(AppState.get().statusBarTextSizeEasy);
        pagesTime.setTextSize(AppState.get().statusBarTextSizeEasy);
        pagesTime1.setTextSize(AppState.get().statusBarTextSizeEasy);
        pagesCountIndicator.setTextSize(AppState.get().statusBarTextSizeEasy);
        pannelBookTitle.setTextSize((AppState.get().statusBarTextSizeEasy + 2));
        flippingIntervalView.setTextSize(AppState.get().statusBarTextSizeEasy);

        int progressColor = AppState.get().isDayNotInvert ? AppState.get().statusBarColorDay : MagicHelper.otherColor(AppState.get().statusBarColorNight, +0.2f);
        progressDraw.updateColor(progressColor);

        progressDraw.getLayoutParams().height = Dips.dpToPx(AppState.get().progressLineHeight);
        progressDraw.requestLayout();
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onTTSStatus(TtsStatus status) {
        try {
            ttsActive.setVisibility(TxtUtils.visibleIf(!TTSEngine.get().isShutdown()));
        } catch (Exception e) {
            LOG.e(e);
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onPageNumber(final MessagePageNumber event) {
        try {
            ttsActive.setVisibility(View.VISIBLE);
            dc.onGoToPage(event.getPage() + 1);
        } catch (Exception e) {
            LOG.e(e);
        }
    }

    @Subscribe
    public void onFlippingStart(FlippingStart event) {
        isFlipping = true;
        flippingTimer = 0;
        flippingHandler.removeCallbacks(flippingRunnable);
        flippingHandler.post(flippingRunnable);
        flippingIntervalView.setText("");
        flippingIntervalView.setVisibility(AppState.get().isShowToolBar ? View.VISIBLE : View.GONE);

        if (AppState.get().isEditMode) {
            AppState.get().isEditMode = false;
            hideShow();
        }

        onPageFlip1.setVisibility(View.VISIBLE);
        onPageFlip1.setImageResource(R.drawable.glyphicons_37_file_pause);
    }

    @Subscribe
    public void onFlippingStop(FlippingStop event) {
        isFlipping = false;
        flippingHandler.removeCallbacks(flippingRunnable);
        flippingHandler.removeCallbacksAndMessages(null);
        flippingIntervalView.setVisibility(View.GONE);
        onPageFlip1.setImageResource(R.drawable.glyphicons_37_file_play);

    }


    public void showHideHistory() {
        linkHistory.setVisibility(!dc.getLinkHistory().isEmpty() ? View.VISIBLE : View.GONE);

    }

    public void showHideInfoToolBar() {
        int isVisible = AppState.get().isShowToolBar ? View.VISIBLE : View.GONE;
        pagesTime.setVisibility(isVisible);
        pagesTime1.setVisibility(AppState.get().fullScreenMode == AppState.FULL_SCREEN_NORMAL ? View.GONE : View.VISIBLE);

        pagesCountIndicator.setVisibility(isVisible);
        pagesPower.setVisibility(isVisible);
        bottomIndicators.setVisibility(isVisible);

        pannelBookTitle.setVisibility(AppState.get().isShowPanelBookNameBookMode ? View.VISIBLE : View.GONE);

        progressDraw.setVisibility(AppState.get().isShowReadingProgress ? View.VISIBLE : View.GONE);

        if (AppState.get().isShowToolBar) {
            pagesPower.setVisibility(AppState.get().isShowBattery ? View.VISIBLE : View.INVISIBLE);
            pagesTime.setVisibility(AppState.get().isShowTime ? View.VISIBLE : View.INVISIBLE);
        }

        RelativeLayout.LayoutParams layoutParams = (RelativeLayout.LayoutParams) bottomPanel.getLayoutParams();

        if (AppState.get().statusBarPosition == AppState.STATUSBAR_POSITION_TOP) {

            layoutParams.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM, 0);
            layoutParams.addRule(RelativeLayout.ALIGN_PARENT_TOP);
        } else {
            layoutParams.addRule(RelativeLayout.ALIGN_PARENT_TOP, 0);
            layoutParams.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
        }

    }

    private void showSearchDialog() {

        if (AppSP.get().isCrop || AppSP.get().isCut) {
            onModeChange.setImageResource(R.drawable.glyphicons_two_page_one);
            AppSP.get().isCrop = false;
            AppSP.get().isCut = false;
            AppSP.get().isDouble = false;

            onCrop.underline(AppSP.get().isCrop);
            onCrop.invalidate();
            reloadDoc.run();
        }
        DragingDialogs.searchMenu(anchor, dc, "");
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        Android6.onRequestPermissionsResult(this, requestCode, permissions, grantResults);
    }

    public void updateLockMode() {
        if (AppSP.get().isLocked) {
            lockModelImage.setImageResource(R.drawable.glyphicons_204_lock);
        } else {
            lockModelImage.setImageResource(R.drawable.glyphicons_205_unlock);
        }
//        if (AppState.get().l) {
//            TintUtil.setTintImageWithAlpha(moveCenter, Color.LTGRAY);
//        } else {
//            TintUtil.setTintImageWithAlpha(moveCenter, Color.WHITE);
//        }

    }

    @Override
    public void onStart() {
        super.onStart();
        // Analytics.onStart(this);
        EventBus.getDefault().register(this);

    }

    @Override
    public void onStop() {
        EventBus.getDefault().unregister(this);
        super.onStop();
        // Analytics.onStop(this);
        if (flippingHandler != null) {
            flippingHandler.removeCallbacksAndMessages(null);
        }

    }

    @Override
    protected void onDestroy() {

        if (loadinAsyncTask != null) {
            try {
                loadinAsyncTask.cancel(true);
                loadinAsyncTask = null;
            } catch (Exception e) {
                LOG.e(e);
            }
        }
        if (handler != null) {
            handler.removeCallbacksAndMessages(null);
        }
        if (flippingHandler != null) {
            flippingHandler.removeCallbacksAndMessages(null);
        }
        nullAdapter();

        // AppSP.get().isCut = false;
        PageImageState.get().clearResouces();


        super.onDestroy();


    }

    public void nullAdapter() {
        if (viewPager != null) {
            try {

                //ImageLoader.getInstance().clearAllTasks();
                IMG.clearMemoryCache();
                closeDialogs();
                viewPager.setAdapter(null);
            } catch (Exception e) {
                LOG.e(e);
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        DocumentController.chooseFullScreen(this, AppState.get().fullScreenMode);
        DocumentController.doRotation(this);

        if (clickUtils != null) {
            clickUtils.init();
        }

        if (dc != null) {
            dc.onResume();
        }

        if (dc != null) {
            dc.goToPageByTTS();
        }


        handler.removeCallbacks(closeRunnable);
        handlerTimer.post(updateTimePower);

        if (AppState.get().inactivityTime != -1) {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
            LOG.d("FLAG addFlags", "FLAG_KEEP_SCREEN_ON", "add", AppState.get().inactivityTime);

        }

        if (ttsActive != null) {
            ttsActive.setVisibility(TxtUtils.visibleIf(TTSEngine.get().isTempPausing()));
        }
        AppSP.get().lastClosedActivity = HorizontalViewActivity.class.getSimpleName();
        LOG.d("lasta save", AppSP.get().lastClosedActivity);

    }

    @Override
    protected void onPause() {
        super.onPause();
        AppProfile.save(this);
        TempHolder.isSeaching = false;
        TempHolder.isActiveSpeedRead.set(false);
        //dc.saveCurrentPageAsync();
        handler.postDelayed(closeRunnable, AppState.APP_CLOSE_AUTOMATIC);
        handlerTimer.removeCallbacks(updateTimePower);
        GFile.runSyncService(this);

    }

    public synchronized void nextPage() {
        flippingTimer = 0;

        boolean isAnimate = AppState.get().isScrollAnimation;
        long lx = System.currentTimeMillis() - lastClick;
        LOG.d("lastClick", lx);
        if (lx < lastClickMaxTime) {
            isAnimate = false;
        }
        lastClick = System.currentTimeMillis();
        viewPager.setCurrentItem(dc.getCurentPage() + 1, isAnimate);
        dc.checkReadingTimer();

    }

    public synchronized void prevPage() {
        flippingTimer = 0;

        boolean isAnimate = AppState.get().isScrollAnimation;
        if (System.currentTimeMillis() - lastClick < lastClickMaxTime) {
            isAnimate = false;
        }
        lastClick = System.currentTimeMillis();
        viewPager.setCurrentItem(dc.getCurentPage() - 1, isAnimate);
        dc.checkReadingTimer();

    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    @Subscribe
    public void onEvent(MessageEvent ev) {

        if (currentScrollState != ViewPager.SCROLL_STATE_IDLE) {
            LOG.d("Skip event");
            return;
        }

        clickUtils.init();
        LOG.d("MessageEvent", ev.getMessage(), ev.getX(), ev.getY());
        if (ev.getMessage().equals(MessageEvent.MESSAGE_CLOSE_BOOK)) {
            showInterstial();
        } else if (ev.getMessage().equals(MessageEvent.MESSAGE_CLOSE_BOOK_APP)) {
            dc.onCloseActivityFinal(new Runnable() {

                @Override
                public void run() {
                    MainTabs2.closeApp(dc.getActivity());
                }
            });
        } else if (ev.getMessage().equals(MessageEvent.MESSAGE_PERFORM_CLICK)) {
            boolean isOpen = closeDialogs();
            if (isOpen) {
                return;
            }

            int x = (int) ev.getX();
            int y = (int) ev.getY();
            if (clickUtils.isClickRight(x, y) && AppState.get().tapZoneRight != AppState.TAP_DO_NOTHING) {
                if (AppState.get().tapZoneRight == AppState.TAP_NEXT_PAGE) {
                    nextPage();
                } else {
                    prevPage();
                }
            } else if (clickUtils.isClickLeft(x, y) && AppState.get().tapZoneLeft != AppState.TAP_DO_NOTHING) {
                if (AppState.get().tapZoneLeft == AppState.TAP_PREV_PAGE) {
                    prevPage();
                } else {
                    nextPage();
                }
            } else if (clickUtils.isClickTop(x, y) && AppState.get().tapZoneTop != AppState.TAP_DO_NOTHING) {
                if (AppState.get().tapZoneTop == AppState.TAP_PREV_PAGE) {
                    prevPage();
                } else {
                    nextPage();
                }

            } else if (clickUtils.isClickBottom(x, y) && AppState.get().tapZoneBottom != AppState.TAP_DO_NOTHING) {
                if (AppState.get().tapZoneBottom == AppState.TAP_NEXT_PAGE) {
                    nextPage();
                } else {
                    prevPage();
                }

            } else {
                LOG.d("Click-center!", x, y);
                handler.removeCallbacks(doShowHideWrapperControllsRunnable);
                handler.postDelayed(doShowHideWrapperControllsRunnable, 250);
                // Toast.makeText(this, "Click", Toast.LENGTH_SHORT).show();
            }
        } else if (ev.getMessage().equals(MessageEvent.MESSAGE_DOUBLE_TAP)) {
            handler.removeCallbacks(doShowHideWrapperControllsRunnable);
            updateLockMode();
            // Toast.makeText(this, "DB", Toast.LENGTH_SHORT).show();
        } else if (ev.getMessage().equals(MessageEvent.MESSAGE_PLAY_PAUSE)) {
            TTSService.playPause(HorizontalViewActivity.this, dc);
        } else if (ev.getMessage().equals(MessageEvent.MESSAGE_SELECTED_TEXT)) {
            if (dc.isTextFormat() && TxtUtils.isFooterNote(AppState.get().selectedText)) {
                DragingDialogs.showFootNotes(anchor, dc, new Runnable() {

                    @Override
                    public void run() {
                        showHideHistory();
                    }
                });
            } else {

                if (AppState.get().isRememberDictionary) {
                    final String text = AppState.get().selectedText;
                    DictsHelper.runIntent(dc.getActivity(), text);
                    dc.clearSelectedText();
                } else {
                    DragingDialogs.selectTextMenu(anchor, dc, true, onRefresh);
                }
            }
        } else if (ev.getMessage().equals(MessageEvent.MESSAGE_GOTO_PAGE_BY_LINK)) {
            if (ev.getPage() == -1 && TxtUtils.isNotEmpty(ev.getBody())) {
                AlertDialogs.openUrl(this, ev.getBody());
            } else {
                dc.getLinkHistory().add(dc.getCurentPage() + 1);
                dc.onGoToPage(ev.getPage() + 1);
                showHideHistory();
            }
        } else if (ev.getMessage().equals(MessageEvent.MESSAGE_GOTO_PAGE_SWIPE)) {
            if (ev.getPage() > 0) {
                nextPage();
            } else {
                prevPage();
            }
        } else if (ev.getMessage().equals(MessageEvent.MESSAGE_AUTO_SCROLL)) {
            if (isFlipping) {
                onFlippingStop(null);
            } else {
                onFlippingStart(null);
            }

        }
    }

    private void doShowHideWrapperControlls() {
        AppState.get().isEditMode = !AppState.get().isEditMode;
        hideShow();
    }

    public void initAsync(int w, int h) {
        dc = new HorizontalModeController(this, w, h) {
            @Override
            public void onGoToPageImpl(int page) {
                updateUI(page);
                EventBus.getDefault().post(new InvalidateMessage());
            }

            @Override
            public void notifyAdapterDataChanged() {
            }

            @Override
            public void showInterstialAndClose() {
                showInterstial();
            }

        };
        // dc.init(this);
        dc.initAnchor(anchor);
    }

    public void updateUI(int page) {
        if (dc == null || viewPager == null || viewPager.getAdapter() == null) {
            return;
        }

        if (page <= viewPager.getAdapter().getCount() - 1) {
            viewPager.setCurrentItem(page, false);
        }

        Info info = OutlineHelper.getForamtingInfo(dc, false);
        maxSeek.setText(info.textPage);
        currentSeek.setText(info.textMax);
        pagesCountIndicator.setText(info.chText);

        currentSeek.setContentDescription(dc.getString(R.string.m_current_page) + " " + info.textMax);
        maxSeek.setContentDescription(dc.getString(R.string.m_total_pages) + " " + info.textPage);

        seekBar.setProgress(page);
        if (dc != null) {
            dc.currentPage = page;
        }

        pagesTime.setText(UiSystemUtils.getSystemTime(this));
        pagesTime1.setText(UiSystemUtils.getSystemTime(this));

        int myLevel = UiSystemUtils.getPowerLevel(this);
        pagesPower.setText(myLevel + "%");
        if (myLevel == -1) {
            pagesPower.setVisibility(View.GONE);
        }
        if (TxtUtils.isNotEmpty(dc.getCurrentChapter())) {
            chapterView.setText(dc.getCurrentChapter());
            chapterView.setVisibility(View.VISIBLE);
        } else {
            chapterView.setVisibility(View.GONE);
        }

        LOG.d("_PAGE", "Update UI", page);
        dc.saveCurrentPage();

        if (dc.floatingBookmark != null) {
            dc.floatingBookmark.p = dc.getPercentage();
            floatingBookmarkTextView.setText("{" + dc.getCurentPageFirst1() + "}");
            floatingBookmarkTextView.setVisibility(View.VISIBLE);

            BookmarksData.get().add(dc.floatingBookmark);
            showPagesHelper();
        } else {
            floatingBookmarkTextView.setVisibility(View.GONE);
        }

    }

    public void loadUI() {
        titleTxt.setText(dc.getTitle());
        pannelBookTitle.setText(dc.getTitle());
        createAdapter();

        viewPager.addOnPageChangeListener(onViewPagerChangeListener);
        viewPager.setCurrentItem(dc.getCurentPage(), false);

        seekBar.setMax(dc.getPageCount() - 1);
        seekBar.setProgress(dc.getCurentPage());

        bottomIndicators.setOnTouchListener(new HorizontallSeekTouchEventListener(onSeek, dc.getPageCount(), false));
        progressDraw.setOnTouchListener(new HorizontallSeekTouchEventListener(onSeek, dc.getPageCount(), false));
        bottomIndicators.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                if (AppState.get().tapZoneBottom == AppState.TAP_DO_NOTHING) {
                    // do nothing
                } else if (AppState.get().tapZoneBottom == AppState.TAP_NEXT_PAGE) {
                    nextPage();
                } else if (AppState.get().tapZoneBottom == AppState.TAP_PREV_PAGE) {
                    prevPage();
                }

            }
        });

        updateLockMode();

        tinUI();

        onViewPagerChangeListener.onPageSelected(dc.getCurentPage());

        progressDraw.updatePageCount(dc.getPageCount());

        dc.getOutline(new ResultResponse<List<OutlineLinkWrapper>>() {

            @Override
            public boolean onResultRecive(List<OutlineLinkWrapper> result) {
                onClose.setVisibility(View.VISIBLE);
                progressDraw.updateDivs(result);
                updateUI(dc.getCurrentPage());
                if (TxtUtils.isListEmpty(result)) {
                    TintUtil.setTintImageWithAlpha(outline, Color.LTGRAY);
                }
                showPagesHelper();
                return false;
            }
        }, false);

        showHelp();

    }

    public void showHelp() {
        if (AppSP.get().isFirstTimeHorizontal) {
            handler.postDelayed(new Runnable() {

                @Override
                public void run() {
                    AppSP.get().isFirstTimeHorizontal = false;
                    AppState.get().isEditMode = true;
                    hideShow();
                    Views.showHelpToast(lockModelImage);

                }
            }, 1000);
        }

    }

    private void tinUI() {
        TintUtil.setTintBgSimple(actionBar, AppState.get().transparencyUI);
        TintUtil.setTintBgSimple(bottomBar, AppState.get().transparencyUI);
        TintUtil.setStatusBarColor(this);
        // TintUtil.setBackgroundFillColorBottomRight(ttsActive,
        // ColorUtils.setAlphaComponent(TintUtil.color, 230));
    }

    // @Override
    @Override
    public void onConfigurationChanged(final Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        TempHolder.isActiveSpeedRead.set(false);
        clickUtils.init();
        if (isInitPosistion == null) {
            return;
        }
        handler.removeCallbacksAndMessages(null);

        final boolean currentPosistion = Dips.screenHeight() > Dips.screenWidth();
        if (ExtUtils.isTextFomat(getIntent()) && isInitOrientation == AppState.get().orientation) {

            if (rotatoinDialog != null) {
                try {
                    rotatoinDialog.dismiss();
                } catch (Exception e) {
                    LOG.e(e);
                }
            }

            if (isInitPosistion != currentPosistion) {
                AlertDialog.Builder dialog = new AlertDialog.Builder(this);
                dialog.setCancelable(false);
                dialog.setMessage(R.string.apply_a_new_screen_orientation_);
                dialog.setPositiveButton(R.string.yes, new AlertDialog.OnClickListener() {

                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        onRotateScreen();
                        isInitPosistion = currentPosistion;
                    }
                });
                rotatoinDialog = dialog.show();
                rotatoinDialog.getWindow().setLayout((int) (Dips.screenMinWH() * 0.8f), LayoutParams.WRAP_CONTENT);
            }
        } else {
            Keyboards.hideNavigationOnCreate(this);
            dc.udpateImageSize(dc.isTextFormat(), viewPager.getWidth(), viewPager.getHeight());
            onRotateScreen();
        }

        isInitOrientation = AppState.get().orientation;

    }

    public void authoFit() {
        if (handler == null) {
            return;
        }
        handler.postDelayed(new Runnable() {

            @Override
            public void run() {
                PageImageState.get().isAutoFit = true;
                if (dc != null) {
                    dc.cleanImageMatrix();
                }
                EventBus.getDefault().post(new MessageAutoFit(viewPager.getCurrentItem()));
            }
        }, 50);
    }

    @Override
    public View onCreateView(View parent, String name, Context context, AttributeSet attrs) {
        return super.onCreateView(parent, name, context, attrs);
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public void onRotateScreen() {
        // ADS.activate(this, adView);
        activateAds();

        AppProfile.save(this);
        if (ExtUtils.isTextFomat(getIntent())) {
            nullAdapter();
            dc.restartActivity();
        } else {
            if (viewPager != null) {
                authoFit();
            }
        }
    }

    public void createAdapter() {
        LOG.d("createAdapter");
        nullAdapter();
        pagerAdapter = null;
        final int count = dc.getPageCount();
        pagerAdapter = new UpdatableFragmentPagerAdapter(getSupportFragmentManager()) {

            @Override
            public int getCount() {
                return count;
            }

            @Override
            public Fragment getItem(final int position) {
                final ImagePageFragment imageFragment = new ImagePageFragment();

                final Bundle b = new Bundle();
                b.putInt(ImagePageFragment.POS, position);
                b.putBoolean(ImagePageFragment.IS_TEXTFORMAT, dc.isTextFormat());
                b.putString(ImagePageFragment.PAGE_PATH, dc.getPageUrl(position).toString());

                imageFragment.setArguments(b);
                return imageFragment;
            }

            @Override
            public Parcelable saveState() {
                try {
                    return super.saveState();
                } catch (Exception e) {
                    Toast.makeText(HorizontalViewActivity.this, R.string.msg_unexpected_error, Toast.LENGTH_LONG).show();
                    LOG.e(e);
                    return null;
                }
            }

            @Override
            public void restoreState(Parcelable arg0, ClassLoader arg1) {
                try {
                    super.restoreState(arg0, arg1);
                } catch (Exception e) {
                    Toast.makeText(HorizontalViewActivity.this, R.string.msg_unexpected_error, Toast.LENGTH_LONG).show();
                    LOG.e(e);
                }
            }

        };
        int pagesInMemory = AppState.get().pagesInMemory;
        if (pagesInMemory == 1 || pagesInMemory == 0) {
            pagesInMemory = 0;
        } else if (pagesInMemory == 3) {
            pagesInMemory = 1;
        } else if (pagesInMemory == 5) {
            pagesInMemory = 2;
        } else {
            pagesInMemory = 1;
        }
        viewPager.setOffscreenPageLimit(pagesInMemory);
        LOG.d("setOffscreenPageLimit", pagesInMemory);
        viewPager.setAdapter(pagerAdapter);
        viewPager.setSaveEnabled(false);
        viewPager.setSaveFromParentEnabled(false);

    }

    public void hideShow() {
        hideShow(true);
    }

    public void updateBannnerTop() {
        try {
            ((RelativeLayout.LayoutParams) adFrame.getLayoutParams()).topMargin = actionBar.getHeight() + Dips.dpToPx(24);
        } catch (Exception e) {
            LOG.e(e);
        }
    }

    public void hideShow(boolean animated) {
        if (AppState.get().isEnableAccessibility) {
            animated = false;
            AppState.get().isEditMode = true;
            ttsFixPosition();
        }



        updateBannnerTop();
        showPagesHelper();

        if (prev == AppState.get().isEditMode) {
            return;
        }
        prev = AppState.get().isEditMode;

        if (AppsConfig.IS_BETA && animated) {
            modeName.setText(R.string.mode_horizontally);
        }

        if (!animated || AppState.get().appTheme == AppState.THEME_INK) {
            actionBar.setVisibility(AppState.get().isEditMode ? View.VISIBLE : View.GONE);
            bottomBar.setVisibility(AppState.get().isEditMode ? View.VISIBLE : View.GONE);
            adFrame.setVisibility(AppState.get().isEditMode ? View.VISIBLE : View.GONE);

            DocumentController.chooseFullScreen(this, AppState.get().fullScreenMode);
            ttsFixPosition();
            return;
        }

        final TranslateAnimation hideActionBar = new TranslateAnimation(0, 0, 0, -actionBar.getHeight());
        final TranslateAnimation hideBottomBar = new TranslateAnimation(0, 0, 0, bottomBar.getHeight());

        final TranslateAnimation showActoinBar = new TranslateAnimation(0, 0, -actionBar.getHeight(), 0);
        final TranslateAnimation showBottomBar = new TranslateAnimation(0, 0, bottomBar.getHeight(), 0);

        final TranslateAnimation adsShow = new TranslateAnimation(-adFrame.getWidth(), 0, 0, 0);
        final TranslateAnimation adsHide = new TranslateAnimation(0, -adFrame.getWidth(), 0, 0);

        updateAnimation(hideActionBar);
        updateAnimation(hideBottomBar);

        updateAnimation(showActoinBar);
        updateAnimation(showBottomBar);

        updateAnimation(adsShow);
        updateAnimation(adsHide);

        if (AppState.get().isEditMode) {
            DocumentController.turnOnButtons(this);

            if (anchor.getVisibility() == View.GONE) {
                adFrame.startAnimation(adsShow);
            }

            actionBar.startAnimation(showActoinBar);
            bottomBar.startAnimation(showBottomBar);

            showBottomBar.setAnimationListener(new AnimationListener() {

                @Override
                public void onAnimationStart(final Animation animation) {
                }

                @Override
                public void onAnimationRepeat(final Animation animation) {
                }

                @Override
                public void onAnimationEnd(final Animation animation) {
                    actionBar.setVisibility(View.VISIBLE);
                    bottomBar.setVisibility(View.VISIBLE);
                    adFrame.setVisibility(View.VISIBLE);
                    adFrame.setTag(null);

                    Keyboards.invalidateEink(parentParent);

                    ttsFixPosition();

                }
            });

        } else {
            DocumentController.turnOffButtons(this);

            if (anchor.getVisibility() == View.GONE && adFrame.getVisibility() == View.VISIBLE) {
                adFrame.startAnimation(adsHide);
            }
            actionBar.startAnimation(hideActionBar);
            bottomBar.startAnimation(hideBottomBar);

            hideBottomBar.setAnimationListener(new AnimationListener() {

                @Override
                public void onAnimationStart(final Animation animation) {
                }

                @Override
                public void onAnimationRepeat(final Animation animation) {
                }

                @Override
                public void onAnimationEnd(final Animation animation) {
                    actionBar.setVisibility(View.GONE);
                    bottomBar.setVisibility(View.GONE);
                    adFrame.setVisibility(View.GONE);

                    Keyboards.invalidateEink(parentParent);

                    ttsFixPosition();
                }

            });

        }

        if (pagerAdapter != null) {
            DocumentController.chooseFullScreen(this, AppState.get().fullScreenMode);
            pagerAdapter.notifyDataSetChanged();
        }
    }

    private void ttsFixPosition() {
        RelativeLayout.LayoutParams layoutParams = (RelativeLayout.LayoutParams) ttsActive.getLayoutParams();
        if (AppState.get().isEditMode) {
            layoutParams.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM, 0);
        } else {
            layoutParams.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
        }
        ttsActive.setLayoutParams(layoutParams);
    }

    @Override
    public boolean onKeyUp(final int keyCode, final KeyEvent event) {
        if (isMyKey) {
            return true;
        }
        if (anchor != null && anchor.getVisibility() == View.GONE) {
            if (keyCode >= KeyEvent.KEYCODE_1 && keyCode <= KeyEvent.KEYCODE_9) {
                dc.onGoToPage(keyCode - KeyEvent.KEYCODE_1 + 1);
                return true;
            }
            if (keyCode == KeyEvent.KEYCODE_0) {
                DragingDialogs.gotoPageDialog(anchor, dc);
                return true;
            }

            if (KeyEvent.KEYCODE_MENU == keyCode || KeyEvent.KEYCODE_M == keyCode) {
                doShowHideWrapperControlls();
                return true;
            }
            if (KeyEvent.KEYCODE_F == keyCode) {
                dc.alignDocument();
                return true;
            }

            if (KeyEvent.KEYCODE_S == keyCode || KeyEvent.KEYCODE_SEARCH == keyCode) {
                showSearchDialog();
                return true;
            }
        }
        return super.onKeyUp(keyCode, event);
    }

    @Override
    public boolean onKeyDown(final int keyCode1, final KeyEvent event) {

        int keyCode = event.getKeyCode();
        if (keyCode == 0) {
            keyCode = event.getScanCode();
        }

        LOG.d("onKeyDown", keyCode, event);

        isMyKey = false;

        if (AppState.get().isUseVolumeKeys) {

            int repeatCount = event.getRepeatCount();
            if (repeatCount >= 1 && repeatCount < DocumentController.REPEAT_SKIP_AMOUNT) {
                isMyKey = true;
                return true;
            }
            if (repeatCount == 0 && System.currentTimeMillis() - keyTimeout < 250) {
                LOG.d("onKeyDown timeout", System.currentTimeMillis() - keyTimeout);
                isMyKey = true;
                return true;
            }

            keyTimeout = System.currentTimeMillis();

            if (keyCode == KeyEvent.KEYCODE_ENTER) {

                closeDialogs();
                AppState.get().isEditMode = false;
                hideShow();

                if (TTSEngine.get().isTempPausing()) {
                    TTSService.playPause(dc.getActivity(), dc);
                } else {
                    EventBus.getDefault().post(new MessageEvent(MessageEvent.MESSAGE_AUTO_SCROLL));
                }

                isMyKey = true;
                return true;
            }


            if (AppState.get().isZoomInOutWithVolueKeys) {
                if (keyCode == KeyEvent.KEYCODE_VOLUME_UP) {
                    dc.onZoomInc();
                    isMyKey = true;
                    return true;
                }

                if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN) {
                    dc.onZoomDec();
                    isMyKey = true;
                    return true;
                }

            }

            LOG.d("onKeyDown", keyCode, repeatCount, System.currentTimeMillis());

            if (AppState.get().isUseVolumeKeys && KeyEvent.KEYCODE_HEADSETHOOK == keyCode) {
                if (TTSEngine.get().isPlaying()) {
                    if (AppState.get().isFastBookmarkByTTS) {
                        TTSEngine.get().fastTTSBookmakr(dc);
                    } else {
                        TTSEngine.get().stop();
                    }
                } else {
                    //FTTSEngine.get().playCurrent();
                    TTSService.playPause(dc.getActivity(), dc);

                    anchor.setTag("");
                }
                //TTSNotification.showLast();
                //DragingDialogs.textToSpeachDialog(anchor, dc);
                return true;
            }

            if (!TTSEngine.get().isPlaying()) {
                if (AppState.get().getNextKeys().contains(keyCode)) {
                    if (closeDialogs()) {
                        isMyKey = true;
                        return true;
                    }
                    if (PageImageState.get().hasSelectedWords()) {
                        dc.clearSelectedText();
                        isMyKey = true;
                        return true;
                    }
                    nextPage();
                    flippingTimer = 0;
                    isMyKey = true;
                    return true;
                } else if (AppState.get().getPrevKeys().contains(keyCode)) {
                    if (closeDialogs()) {
                        isMyKey = true;
                        return true;
                    }
                    if (PageImageState.get().hasSelectedWords()) {
                        dc.clearSelectedText();
                        isMyKey = true;
                        return true;
                    }
                    prevPage();
                    flippingTimer = 0;
                    isMyKey = true;
                    return true;
                }
            }


        }

        return super.onKeyDown(keyCode, event);

    }

    @Override
    public boolean onKeyLongPress(final int keyCode, final KeyEvent event) {
        // Toast.makeText(this, "onKeyLongPress", Toast.LENGTH_SHORT).show();
        if (CloseAppDialog.checkLongPress(this, event)) {
            CloseAppDialog.showOnLongClickDialog(HorizontalViewActivity.this, null, dc);
            return true;
        }
        return super.onKeyLongPress(keyCode, event);
    }

    @Override
    public void onBackPressed() {
        // Toast.makeText(this, "onBackPressed", Toast.LENGTH_SHORT).show();
        if (isInterstialShown()) {
            onFinishActivity();
            return;
        }
        if (dc != null && dc.floatingBookmark != null) {
            dc.floatingBookmark = null;
            onRefresh.run();
            return;
        }

        if (anchor != null && anchor.getChildCount() > 0 && anchor.getVisibility() == View.VISIBLE) {
            dc.clearSelectedText();
            try {
                findViewById(R.id.closePopup).performClick();
            } catch (Exception e) {
                LOG.e(e);
            }

            return;
        }

        if (dc != null && !dc.getLinkHistory().isEmpty()) {
            dc.onLinkHistory();
            showHideHistory();
            return;
        }

        if (AppState.get().isShowLongBackDialog) {
            CloseAppDialog.showOnLongClickDialog(HorizontalViewActivity.this, null, dc);
        } else {
            showInterstial();
        }
    }

    @Override
    public void onFinishActivity() {
        if (handler != null) {
            handler.removeCallbacksAndMessages(null);
        }
        nullAdapter();

        if (dc != null) {
            dc.saveCurrentPageAsync();
            dc.onCloseActivityFinal(null);
            dc.closeActivity();
        } else {
            finish();
        }
    }

    private void updateAnimation(final TranslateAnimation a) {
        a.setDuration(250);
    }
}
