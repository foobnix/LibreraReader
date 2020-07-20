package com.foobnix.pdf.search.activity;

import android.annotation.TargetApi;
import android.app.ActionBar.LayoutParams;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
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
import android.view.View;
import android.view.View.OnLongClickListener;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.view.animation.TranslateAnimation;
import android.widget.EditText;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.Toast;

import androidx.fragment.app.Fragment;
import androidx.viewpager.widget.ViewPager;
import androidx.viewpager.widget.ViewPager.OnPageChangeListener;

import com.foobnix.android.utils.Apps;
import com.foobnix.android.utils.Dips;
import com.foobnix.android.utils.Keyboards;
import com.foobnix.android.utils.LOG;
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
import com.foobnix.pdf.info.databinding.ActivityHorizontalViewBinding;
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
import com.foobnix.sys.ClickUtils;
import com.foobnix.sys.TempHolder;
import com.foobnix.tts.MessagePageNumber;
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

import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class HorizontalViewActivity extends AdsFragmentActivity {
    public boolean prev = true;
    private ActivityHorizontalViewBinding binding;
    HorizontalModeController dc;
    Handler handler = new Handler(Looper.getMainLooper());
    Handler flippingHandler = new Handler(Looper.getMainLooper());
    Handler handlerTimer = new Handler(Looper.getMainLooper());
    CopyAsyncTask loadinAsyncTask;
    Dialog rotatoinDialog;
    volatile Boolean isInitPosistion = null;
    volatile int isInitOrientation;
    String quickBookmark;
    ClickUtils clickUtils;

    int flippingTimer = 0;
    boolean isFlipping = false;
    Runnable reloadDocBrigntness = new Runnable() {
        @Override
        public void run() {
            binding.onBC.underline(AppState.get().isEnableBC);
            IMG.clearMemoryCache();
            int position = binding.pager2.getCurrentItem();
            ImagePageFragment f2 = (ImagePageFragment) getSupportFragmentManager().findFragmentByTag("f" + (binding.pager2.getCurrentItem()));
            LOG.d("reloadDocBrigntness", f2);
            if (f2 != null) {
                final Bundle b = new Bundle();
                b.putInt(ImagePageFragment.POS, position);
                b.putBoolean(ImagePageFragment.IS_TEXTFORMAT, dc.isTextFormat());
                b.putString(ImagePageFragment.PAGE_PATH, dc.getPageUrl(position).toString());

                f2.setArguments(b);

                f2.loadImageGlide();
            }
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

            flippingTimer++;
            binding.flippingIntervalView.setText("{" + (AppState.get().flippingInterval - flippingTimer + 1) + "}");
            binding.flippingIntervalView.setVisibility(AppState.get().isShowToolBar ? View.VISIBLE : View.GONE);
            flippingHandler.postDelayed(flippingRunnable, 1000);
        }
    };
    Runnable updateTimePower = new Runnable() {
        @Override
        public void run() {
            LOG.d("Update time and updateTimePower");
            try {
                binding.pagesTime.setText(UiSystemUtils.getSystemTime(HorizontalViewActivity.this));
                binding.pagesTime1.setText(UiSystemUtils.getSystemTime(HorizontalViewActivity.this));

                int myLevel = UiSystemUtils.getPowerLevel(HorizontalViewActivity.this);
                binding.pagesPower.setText(myLevel + "%");
            } catch (Exception e) {
                LOG.e(e);
            }
            LOG.d("Update time and power");
            handlerTimer.postDelayed(updateTimePower, AppState.APP_UPDATE_TIME_IN_UI);
        }
    };
    Runnable clearFlags = () -> {
        try {
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
            LOG.d("FLAG clearFlags", "FLAG_KEEP_SCREEN_ON", "clear");
        } catch (Exception e) {
            LOG.e(e);
        }
    };
    UpdatableFragmentPagerAdapter pagerAdapter;
    Runnable onRefresh = new Runnable() {
        @Override
        public void run() {
            dc.saveCurrentPageAsync();
            updateUI(binding.pager2.getCurrentItem());
            showHideInfoToolBar();
            updateSeekBarColorAndSize();
            BrightnessHelper.updateOverlay(binding.overlay);
            hideShow();
            TTSEngine.get().stop();
            showPagesHelper();
        }
    };
    public View.OnClickListener onBookmarks = new View.OnClickListener() {
        @Override
        public void onClick(final View v) {
            DragingDialogs.showBookmarksDialog(binding.anchor, dc, () -> {
                showHideHistory();
                showPagesHelper();
                updateUI(dc.getCurrentPage());
            });
        }
    };
    View.OnLongClickListener onBookmarksLong = new View.OnLongClickListener() {
        @Override
        public boolean onLongClick(final View arg0) {
            DragingDialogs.addBookmarksLong(binding.anchor, dc);
            showPagesHelper();
            return true;
        }
    };
    Runnable doShowHideWrapperControllsRunnable = this::doShowHideWrapperControlls;
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
            binding.pager2.setCurrentItem(progress, false);
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
            dc.setCurrentPage(binding.pager2.getCurrentItem());
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

            binding.progressDraw.updateProgress(pos);

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
            binding.onBC.underline(AppState.get().isEnableBC);
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

        binding = ActivityHorizontalViewBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        if (!Android6.canWrite(this)) {
            Android6.checkPermissions(this, true);
            return;
        }

        binding.showHypenLangPanel.getRoot().setVisibility(View.GONE);
        binding.pager2.setAccessibilityDelegate(new View.AccessibilityDelegate());
        binding.overlay.setVisibility(View.VISIBLE);

        binding.floatingBookmark.setOnClickListener(v -> {
            if (dc == null) {
                return;
            }
            dc.floatingBookmark = null;
            onRefresh.run();
            onBookmarks.onClick(v);
        });
        binding.floatingBookmark.setOnLongClickListener(v -> {
            if (dc == null) {
                return true;
            }
            dc.floatingBookmark = null;
            onRefresh.run();
            return true;
        });

        TintUtil.setTintImageWithAlpha(binding.anchorX, AppState.get().isDayNotInvert ? Color.BLUE : Color.YELLOW, 150);
        TintUtil.setTintImageWithAlpha(binding.anchorY, AppState.get().isDayNotInvert ? Color.BLUE : Color.YELLOW, 150);

        binding.anchorX.setVisibility(View.GONE);
        binding.anchorY.setVisibility(View.GONE);

        DraggbleTouchListener touch1 = new DraggbleTouchListener(binding.anchorX, (View) binding.anchorX.getParent());
        DraggbleTouchListener touch2 = new DraggbleTouchListener(binding.anchorY, (View) binding.anchorY.getParent());

        final Runnable onMoveActionOnce = () -> {
            float x = binding.anchorX.getX() + binding.anchorX.getWidth();
            float y = binding.anchorX.getY() + binding.anchorX.getHeight() / 2;

            float x1 = binding.anchorY.getX();
            float y1 = binding.anchorY.getY();
            EventBus.getDefault().post(new MessagePageXY(MessagePageXY.TYPE_SELECT_TEXT, binding.pager2.getCurrentItem(), x, y, x1, y1));
        };

        final Runnable onMoveAction = () -> {
            handler.removeCallbacks(onMoveActionOnce);
            handler.postDelayed(onMoveActionOnce, 150);
        };

        Runnable onMoveFinish = () -> {
            onMoveAction.run();
            if (AppState.get().isRememberDictionary) {
                DictsHelper.runIntent(dc.getActivity(), AppState.get().selectedText);
            } else {
                DragingDialogs.selectTextMenu(binding.anchor, dc, true, onRefresh);
            }
        };

        touch1.setOnMoveFinish(onMoveFinish);
        touch2.setOnMoveFinish(onMoveFinish);

        touch1.setOnMove(onMoveAction);
        touch2.setOnMove(onMoveAction);

        binding.toastBrightnessText.setVisibility(View.GONE);
        TintUtil.setDrawableTint(binding.toastBrightnessText.getCompoundDrawables()[0], Color.WHITE);

        binding.footer.modeName.setText(AppState.get().nameHorizontalMode);

        binding.pagesTime1.setVisibility(AppState.get().fullScreenMode == AppState.FULL_SCREEN_NORMAL ? View.GONE : View.VISIBLE);

        binding.onMove.setOnClickListener(v -> DragingDialogs.onMoveDialog(binding.anchor, dc, onRefresh, reloadDoc));

        binding.footer.currentSeek.setOnLongClickListener(v -> {
            Dialogs.showDeltaPage(binding.anchor, dc, dc.getCurentPageFirst1(), onRefresh);
            return true;
        });
        binding.footer.maxSeek.setOnLongClickListener(v -> {
            Dialogs.showDeltaPage(binding.anchor, dc, dc.getCurentPageFirst1(), onRefresh);
            return true;
        });

        updateSeekBarColorAndSize();

        binding.footer.linkHistory.setOnClickListener(v -> {
            dc.onLinkHistory();
            showHideHistory();
        });
        binding.footer.linkHistory.setVisibility(View.GONE);

        if (AppState.get().isRTL) {
            binding.footer.seekBar.setRotation(180);
        }

        binding.footer.autoScroll.setOnClickListener(v -> DragingDialogs.pageFlippingDialog(binding.anchor, dc, onRefresh));

        binding.musicButtonPanel.setVisibility(View.GONE);
        binding.pagesBookmark.setOnClickListener(onBookmarks);
        binding.pagesBookmark.setOnLongClickListener(onBookmarksLong);

        binding.onFullScreen.setOnClickListener(v -> {
            if (dc == null) {
                return;
            }

            DocumentController.showFullScreenPopup(dc.getActivity(), v, id -> {
                AppState.get().fullScreenMode = id;
                DocumentController.chooseFullScreen(HorizontalViewActivity.this, AppState.get().fullScreenMode);
                binding.onFullScreen.setImageResource(DocumentController.getFullScreenIcon(HorizontalViewActivity.this, AppState.get().fullScreenMode));
                if (dc.isTextFormat()) {
                    if (onRefresh != null) {
                        onRefresh.run();
                    }
                    nullAdapter();
                    dc.restartActivity();
                }
                return true;
            }, AppState.get().fullScreenMode);
        });
        binding.onFullScreen.setImageResource(DocumentController.getFullScreenIcon(HorizontalViewActivity.this, AppState.get().fullScreenMode));

        binding.bookNight.setOnClickListener(v -> {
            if (dc == null) {
                return;
            }
            v.setEnabled(false);
            AppState.get().isDayNotInvert = !AppState.get().isDayNotInvert;
            nullAdapter();
            dc.restartActivity();
        });
        // if (Dips.isEInk(this)) {
        // dayNightButton.setVisibility(View.GONE);
        // AppState.get().isDayNotInvert = true;
        // }

        binding.onBC.underline(AppState.get().isEnableBC);
        binding.onBC.setOnClickListener(v -> DragingDialogs.contrastAndBrigtness(binding.anchor, dc,
                reloadDocBrigntness, reloadDoc));

        binding.bookNight.setImageResource(!AppState.get().isDayNotInvert ? R.drawable.glyphicons_232_sun : R.drawable.glyphicons_2_moon);

        binding.moveCenter.setOnClickListener(v -> authoFit());

        binding.onBC.setVisibility(isTextFomat ? View.GONE : View.VISIBLE);

        if (DocumentController.isEinkOrMode(this) || AppState.get().isEnableBC) {
            binding.onBC.setVisibility(View.VISIBLE);
        }
        binding.onMove.setVisibility(DocumentController.isEinkOrMode(this) && !isTextFomat ? View.VISIBLE : View.GONE);

        binding.footer.thumbnail.setOnClickListener(v -> DragingDialogs.gotoPageDialog(binding.anchor, dc));
        binding.footer.onShowSearch.setOnClickListener(v -> showSearchDialog());

        binding.footer.onDocDontext.setOnClickListener(v -> {
            if (dc != null) {
                DragingDialogs.showContent(binding.anchor, dc);
            }
        });
        binding.footer.onBookmarks.setOnClickListener(onBookmarks);
        binding.footer.onBookmarks.setOnLongClickListener(onBookmarksLong);

        binding.footer.onRecent.setOnClickListener(v -> DragingDialogs.recentBooks(binding.anchor, dc));

        binding.footer.textToSpeech.setOnClickListener(v -> {
            LOG.d("bookTTS", AppSP.get().isDoubleCoverAlone, AppSP.get().isDouble, AppSP.get().isCut);
            if (AppSP.get().isDouble || AppSP.get().isCut) {
                modeOnePage();
                return;
            }
            DragingDialogs.textToSpeachDialog(binding.anchor, dc);
        });
        binding.footer.textToSpeech.setOnLongClickListener(v -> {
            AlertDialogs.showTTSDebug(dc);
            hideShow();
            return true;
        });

        // ttsActive.setOnClickListener(new View.OnClickListener() {
        //
        // @Override
        // public void onClick(final View v) {
        // DragingDialogs.textToSpeachDialog(anchor, dc);
        // }
        // });
        binding.footer.onModeChange.setOnClickListener(v -> {
            if (dc == null) {
                return;
            }

            MyPopupMenu p = new MyPopupMenu(v.getContext(), v);
            p.getMenu().add(R.string.one_page).setIcon(R.drawable.glyphicons_two_page_one).setOnMenuItemClickListener(item -> {
                closeDialogs();
                binding.footer.onModeChange.setImageResource(R.drawable.glyphicons_two_page_one);
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
            });
            p.getMenu().add(R.string.two_pages).setIcon(R.drawable.glyphicons_two_pages_12).setOnMenuItemClickListener(item -> {
                closeDialogs();
                binding.footer.onModeChange.setImageResource(R.drawable.glyphicons_two_pages_12);
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
            });
            if (!dc.isTextFormat()) {
                p.getMenu().add(R.string.two_pages_cover).setIcon(R.drawable.glyphicons_two_pages_23).setOnMenuItemClickListener(item -> {
                    closeDialogs();
                    binding.footer.onModeChange.setImageResource(R.drawable.glyphicons_two_pages_23);
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
                });
            }
            if (!dc.isTextFormat()) {
                p.getMenu().add(R.string.half_page).setIcon(R.drawable.glyphicons_page_split).setOnMenuItemClickListener(item -> {
                    closeDialogs();
                    binding.footer.onModeChange.setImageResource(R.drawable.glyphicons_page_split);
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
                });
            }
            if ((AppsConfig.IS_BETA || AppState.get().isExperimental) && !dc.isTextFormat()) {
                p.getMenu().add("Smart Reflow").setIcon(R.drawable.glyphicons_108_text_resize).setOnMenuItemClickListener(item -> {
                    closeDialogs();
                    binding.footer.onModeChange.setImageResource(R.drawable.glyphicons_108_text_resize);
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
                });
            }

            p.show();
            Keyboards.hideNavigation(HorizontalViewActivity.this);
        });

        binding.onCrop.setVisibility(isTextFomat && !AppSP.get().isCrop ? View.GONE : View.VISIBLE);

        final Runnable onCropChange = () -> {
            SettingsManager.getBookSettings().cp = AppSP.get().isCrop;
            reloadDocBrigntness.run();
            binding.onCrop.underline(AppSP.get().isCrop);

            PageImageState.get().isAutoFit = true;
            EventBus.getDefault().post(new MessageAutoFit(binding.pager2.getCurrentItem()));

            AppState.get().isEditMode = false;
            hideShow();
        };

        binding.onCrop.setOnClickListener(v -> DragingDialogs.customCropDialog(binding.anchor, dc, onCropChange));
        binding.onCrop.setOnLongClickListener(v -> {
            AppSP.get().isCrop = !AppSP.get().isCrop;
            onCropChange.run();
            return true;
        });

        binding.footer.bookMenu.setOnClickListener(v -> {
            if (dc == null || dc.getCurrentBook() == null) {
                return;
            }
            ShareDialog.show(HorizontalViewActivity.this, dc.getCurrentBook(), () -> {
                if (dc.getCurrentBook().delete()) {
                    TempHolder.listHash++;
                    AppDB.get().deleteBy(dc.getCurrentBook().getPath());
                    dc.getActivity().finish();
                }
            }, dc.getCurentPage(), dc, this::hideShow);
            Keyboards.hideNavigation(HorizontalViewActivity.this);
            hideAds();
        });
        binding.footer.modeName.setOnClickListener(v -> binding.footer.bookMenu.performClick());
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
        binding.footer.modeName.setOnLongClickListener(onCloseLongClick);

        binding.bookPref.setOnClickListener(v -> {
            if (dc != null) {
                DragingDialogs.preferences(binding.anchor, dc, onRefresh, reloadDoc);
            }
        });

        Apps.accessibilityButtonSize(binding.bookClose);
        binding.bookClose.setVisibility(View.INVISIBLE);
        binding.bookClose.setOnClickListener(v -> {
            nullAdapter();
            closeDialogs();
            showInterstial();
        });

        binding.bookClose.setOnLongClickListener(onCloseLongClick);

        binding.footer.editTop2.setVisibility(View.GONE);
        binding.footer.nextTypeBottom.setVisibility(View.GONE);

        binding.footer.lockUnlock.setOnClickListener(v -> {
            AppSP.get().isLocked = !AppSP.get().isLocked;
            updateLockMode();
        });
        updateLockMode();

        Keyboards.hideNavigationOnCreate(HorizontalViewActivity.this);

        binding.footer.currentSeek.setVisibility(View.GONE);
        binding.footer.maxSeek.setVisibility(View.GONE);
        binding.footer.seekBar.setVisibility(View.INVISIBLE);
        binding.bottomIndicators.setVisibility(View.GONE);

        binding.title.setText(HorizontalModeController.getTempTitle(this));

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

            @Override
            protected Object doInBackground(Object... params) {
                try {
                    LOG.d("doRotation(this)", AppState.get().orientation, HorizontalViewActivity.this.getRequestedOrientation());
                    try {
                        while (binding.pager2.getHeight() == 0) {
                            Thread.sleep(250);
                        }
                        int count = 0;
                        if (AppState.get().orientation == ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE || AppState.get().orientation == ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE) {
                            while (binding.pager2.getHeight() > binding.pager2.getWidth() && count < 20) {
                                Thread.sleep(250);
                                count++;
                            }
                        } else if (AppState.get().orientation == ActivityInfo.SCREEN_ORIENTATION_PORTRAIT || AppState.get().orientation == ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT) {
                            while (binding.pager2.getWidth() > binding.pager2.getHeight() && count < 20) {
                                Thread.sleep(250);
                                count++;
                            }
                        }

                        HorizontalViewActivity.this.runOnUiThread(() -> updateBannnerTop());
                    } catch (InterruptedException e) {
                    }
                    LOG.d("viewPager", binding.pager2.getHeight() + "x" + binding.pager2.getWidth());
                    initAsync(binding.pager2.getWidth(), binding.pager2.getHeight());
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

            @Override
            protected void onPostExecute(Object result) {
                if (AppsConfig.IS_BETA) {
                    long time = System.currentTimeMillis() - start;
                    float sec = (float) time / 1000;
                    binding.footer.modeName.setText(binding.footer.modeName.getText() + " (" + String.format("%.1f", sec) + " sec" + ")");
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
                    binding.bookClose.setVisibility(View.VISIBLE);
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
                    dialog.setNegativeButton(R.string.cancel, (dialog1, which) -> {
                        dialog1.dismiss();
                        if (dc != null) {
                            dc.onCloseActivityFinal(null);
                        } else {
                            HorizontalViewActivity.this.finish();
                        }
                    });
                    dialog.setPositiveButton(R.string.open_file, (dialog12, which) -> {
                        final String txt = input.getText().toString();
                        if (TxtUtils.isNotEmpty(txt)) {
                            dialog12.dismiss();

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
                    });
                    dialog.show();
                } else {
                    binding.footer.currentSeek.setVisibility(View.VISIBLE);
                    binding.footer.maxSeek.setVisibility(View.VISIBLE);
                    binding.footer.seekBar.setVisibility(View.VISIBLE);
                    binding.bottomIndicators.setVisibility(View.VISIBLE);
                    binding.footer.onModeChange.setVisibility(View.VISIBLE);

                    PageImageState.get().isAutoFit = PageImageState.get().needAutoFit;

                    if (ExtUtils.isTextFomat(getIntent())) {
                        PageImageState.get().isAutoFit = true;
                        // moveCenter.setVisibility(View.GONE);
                    } else if (AppState.get().isLockPDF) {
                        // moveCenter.setVisibility(View.VISIBLE);
                        AppSP.get().isLocked = true;
                    }

                    if (ExtUtils.isNoTextLayerForamt(dc.getCurrentBook().getPath())) {
                        TintUtil.setTintImageWithAlpha(binding.footer.textToSpeech, Color.LTGRAY);
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
                    binding.footer.seekBar.setOnSeekBarChangeListener(onSeek);
                    showHideInfoToolBar();

                    isInitPosistion = Dips.screenHeight() > Dips.screenWidth();
                    isInitOrientation = AppState.get().orientation;

                    updateIconMode();

                    binding.onCrop.setVisibility(dc.isTextFormat() && !AppSP.get().isCrop ? View.GONE : View.VISIBLE);
                    binding.onMove.setVisibility(DocumentController.isEinkOrMode(HorizontalViewActivity.this) && !dc.isTextFormat() ? View.VISIBLE : View.GONE);
                    binding.onBC.setVisibility(dc.isTextFormat() ? View.GONE : View.VISIBLE);
                    if (Dips.isEInk() || AppState.get().appTheme == AppState.THEME_INK || AppState.get().isEnableBC) {
                        binding.onBC.setVisibility(View.VISIBLE);
                    }

                    binding.onCrop.underline(AppSP.get().isCrop);
                    binding.onCrop.invalidate();

                    binding.ttsActive.setDC(dc);
                    binding.ttsActive.addOnDialogRunnable(() -> {
                        AppState.get().isEditMode = true;
                        hideShow();
                        DragingDialogs.textToSpeachDialog(binding.anchor, dc);
                    });

                    DialogsPlaylist.dispalyPlaylist(HorizontalViewActivity.this, dc);

                    // RecentUpates.updateAll(HorizontalViewActivity.this);
                    if (dc.getPageCount() == 0) {
                        binding.bookClose.setVisibility(View.VISIBLE);
                    }

                    HypenPanelHelper.init(binding.parentParent, dc);
                }
            }
        };
        loadinAsyncTask.executeOnExecutor(Executors.newSingleThreadExecutor());
        updateIconMode();
        BrightnessHelper.updateOverlay(binding.overlay);

        //
        tinUI();
        LOG.d("INIT end", (float) (System.currentTimeMillis() - crateBegin) / 1000);

        binding.anchor.getViewTreeObserver().addOnGlobalLayoutListener(() -> {
            if (binding.anchor.getVisibility() == View.VISIBLE) {
                binding.adFrame.setVisibility(View.GONE);
                binding.adFrame.setClickable(false);
            } else {
                if (AppState.get().isEditMode && binding.adFrame.getTag() == null) {
                    binding.adFrame.setVisibility(View.VISIBLE);
                    binding.adFrame.setClickable(true);
                } else {
                    binding.adFrame.setVisibility(View.GONE);
                    binding.adFrame.setClickable(false);
                }
            }

            if (binding.anchor.getX() < 0) {
                binding.anchor.setX(0);
            }
            if (binding.anchor.getY() < 0) {
                binding.anchor.setY(0);
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
            BookmarkPanel.showPagesHelper(binding.pagesHelper, binding.musicButtonPanel, dc,
                    binding.pagesBookmark, quickBookmark, onRefresh);
        } catch (Exception e) {
            LOG.e(e);
        }
    }

    @Subscribe
    public void showHideTextSelectors(MessagePageXY event) {
        if (event.getType() == MessagePageXY.TYPE_HIDE) {
            binding.anchorX.setVisibility(View.GONE);
            binding.anchorY.setVisibility(View.GONE);
        }
        if (event.getType() == MessagePageXY.TYPE_SHOW) {
            binding.anchorX.setVisibility(View.VISIBLE);
            binding.anchorY.setVisibility(View.VISIBLE);

            float x = event.getX() - binding.anchorX.getWidth();
            float y = event.getY() - binding.anchorX.getHeight() / 2;

            AnchorHelper.setXY(binding.anchorX, x, y);
            AnchorHelper.setXY(binding.anchorY, event.getX1(), event.getY1());
        }
    }

    @Subscribe
    public void onMessegeBrightness(MessegeBrightness msg) {
        BrightnessHelper.onMessegeBrightness(handler, msg, binding.toastBrightnessText, binding.overlay);
    }

    private boolean closeDialogs() {
        if (dc == null) {
            return false;
        }
        return dc.closeDialogs();
    }

    public void hideAds() {
        binding.adFrame.setTag("");
        binding.adFrame.setVisibility(View.GONE);
    }

    public void updateIconMode() {
        if (AppSP.get().isDouble) {
            if (AppSP.get().isDoubleCoverAlone) {
                binding.footer.onModeChange.setImageResource(R.drawable.glyphicons_two_pages_23);
            } else {
                binding.footer.onModeChange.setImageResource(R.drawable.glyphicons_two_pages_12);
            }
        } else if (AppSP.get().isCut) {
            binding.footer.onModeChange.setImageResource(R.drawable.glyphicons_page_split);
        } else if (AppSP.get().isSmartReflow) {
            binding.footer.onModeChange.setImageResource(R.drawable.glyphicons_108_text_resize);
        } else {
            binding.footer.onModeChange.setImageResource(R.drawable.glyphicons_two_page_one);
        }
    }

    @Override
    protected void attachBaseContext(Context context) {
        super.attachBaseContext(MyContextWrapper.wrap(context));
    }

    public void modeOnePage() {
        binding.footer.onModeChange.setImageResource(R.drawable.glyphicons_two_page_one);
        AppSP.get().isDouble = false;
        AppSP.get().isDoubleCoverAlone = false;
        AppSP.get().isCut = false;
        SettingsManager.getBookSettings().updateFromAppState();
        SharedBooks.save(SettingsManager.getBookSettings());

        nullAdapter();
        dc.restartActivity();
    }

    public void updateSeekBarColorAndSize() {
        TintUtil.setTintText(binding.pagesPower, TintUtil.getStatusBarColor());
        TintUtil.setTintText(binding.pagesTime, TintUtil.getStatusBarColor());
        TintUtil.setTintText(binding.pagesCountIndicator, TintUtil.getStatusBarColor());
        TintUtil.setTintText(binding.panelBookTitle, TintUtil.getStatusBarColor());
        TintUtil.setTintText(binding.flippingIntervalView, TintUtil.getStatusBarColor());

        if (false) {
            GradientDrawable bg = (GradientDrawable) binding.pagesPower.getBackground();
            bg.setStroke(1, TintUtil.getStatusBarColor());
        } else {
            binding.pagesPower.setBackgroundColor(Color.TRANSPARENT);
        }

        binding.pagesPower.setTextSize(AppState.get().statusBarTextSizeEasy);
        binding.pagesTime.setTextSize(AppState.get().statusBarTextSizeEasy);
        binding.pagesTime1.setTextSize(AppState.get().statusBarTextSizeEasy);
        binding.pagesCountIndicator.setTextSize(AppState.get().statusBarTextSizeEasy);
        binding.panelBookTitle.setTextSize((AppState.get().statusBarTextSizeEasy + 2));
        binding.flippingIntervalView.setTextSize(AppState.get().statusBarTextSizeEasy);

        int progressColor = AppState.get().isDayNotInvert ? AppState.get().statusBarColorDay : MagicHelper.otherColor(AppState.get().statusBarColorNight, +0.2f);
        binding.progressDraw.updateColor(progressColor);

        binding.progressDraw.getLayoutParams().height = Dips.dpToPx(AppState.get().progressLineHeight);
        binding.progressDraw.requestLayout();
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onTTSStatus(TtsStatus status) {
        try {
            binding.ttsActive.setVisibility(TxtUtils.visibleIf(!TTSEngine.get().isShutdown()));
        } catch (Exception e) {
            LOG.e(e);
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onPageNumber(final MessagePageNumber event) {
        try {
            binding.ttsActive.setVisibility(View.VISIBLE);
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
        binding.flippingIntervalView.setText("");
        binding.flippingIntervalView.setVisibility(AppState.get().isShowToolBar ? View.VISIBLE : View.GONE);

        if (AppState.get().isEditMode) {
            AppState.get().isEditMode = false;
            hideShow();
        }

        binding.footer.autoScroll.setVisibility(View.VISIBLE);
        binding.footer.autoScroll.setImageResource(R.drawable.glyphicons_37_file_pause);
    }

    @Subscribe
    public void onFlippingStop(FlippingStop event) {
        isFlipping = false;
        flippingHandler.removeCallbacks(flippingRunnable);
        flippingHandler.removeCallbacksAndMessages(null);
        binding.flippingIntervalView.setVisibility(View.GONE);
        binding.footer.autoScroll.setImageResource(R.drawable.glyphicons_37_file_play);
    }

    public void showHideHistory() {
        binding.footer.linkHistory.setVisibility(!dc.getLinkHistory().isEmpty() ? View.VISIBLE : View.GONE);
    }

    public void showHideInfoToolBar() {
        int isVisible = AppState.get().isShowToolBar ? View.VISIBLE : View.GONE;
        binding.pagesTime.setVisibility(isVisible);
        binding.pagesTime1.setVisibility(AppState.get().fullScreenMode == AppState.FULL_SCREEN_NORMAL ? View.GONE : View.VISIBLE);

        binding.pagesCountIndicator.setVisibility(isVisible);
        binding.pagesPower.setVisibility(isVisible);
        binding.bottomIndicators.setVisibility(isVisible);

        binding.panelBookTitle.setVisibility(AppState.get().isShowPanelBookNameBookMode ? View.VISIBLE : View.GONE);

        binding.progressDraw.setVisibility(AppState.get().isShowReadingProgress ? View.VISIBLE : View.GONE);

        if (AppState.get().isShowToolBar) {
            binding.pagesPower.setVisibility(AppState.get().isShowBattery ? View.VISIBLE : View.INVISIBLE);
            binding.pagesTime.setVisibility(AppState.get().isShowTime ? View.VISIBLE : View.INVISIBLE);
        }

        RelativeLayout.LayoutParams layoutParams = (RelativeLayout.LayoutParams) binding.bottomPanel.getLayoutParams();

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
            binding.footer.onModeChange.setImageResource(R.drawable.glyphicons_two_page_one);
            AppSP.get().isCrop = false;
            AppSP.get().isCut = false;
            AppSP.get().isDouble = false;

            binding.onCrop.underline(AppSP.get().isCrop);
            binding.onCrop.invalidate();
            reloadDoc.run();
        }
        DragingDialogs.searchMenu(binding.anchor, dc, "");
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        Android6.onRequestPermissionsResult(this, requestCode, permissions, grantResults);
    }

    public void updateLockMode() {
        if (AppSP.get().isLocked) {
            binding.footer.lockUnlock.setImageResource(R.drawable.glyphicons_204_lock);
        } else {
            binding.footer.lockUnlock.setImageResource(R.drawable.glyphicons_205_unlock);
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
        try {
            //ImageLoader.getInstance().clearAllTasks();
            IMG.clearMemoryCache();
            closeDialogs();
            binding.pager2.setAdapter(null);
        } catch (Exception e) {
            LOG.e(e);
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

        if (binding.ttsActive != null) {
            binding.ttsActive.setVisibility(TxtUtils.visibleIf(TTSEngine.get().isTempPausing()));
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
        binding.pager2.setCurrentItem(dc.getCurentPage() + 1, isAnimate);
        dc.checkReadingTimer();
    }

    public synchronized void prevPage() {
        flippingTimer = 0;

        boolean isAnimate = AppState.get().isScrollAnimation;
        if (System.currentTimeMillis() - lastClick < lastClickMaxTime) {
            isAnimate = false;
        }
        lastClick = System.currentTimeMillis();
        binding.pager2.setCurrentItem(dc.getCurentPage() - 1, isAnimate);
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
            dc.onCloseActivityFinal(() -> MainTabs2.closeApp(dc.getActivity()));
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
                DragingDialogs.showFootNotes(binding.anchor, dc, this::showHideHistory);
            } else {
                if (AppState.get().isRememberDictionary) {
                    final String text = AppState.get().selectedText;
                    DictsHelper.runIntent(dc.getActivity(), text);
                    dc.clearSelectedText();
                } else {
                    DragingDialogs.selectTextMenu(binding.anchor, dc, true, onRefresh);
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
        dc.initAnchor(binding.anchor);
    }

    public void updateUI(int page) {
        if (dc == null || binding.pager2.getAdapter() == null) {
            return;
        }

        if (page <= binding.pager2.getAdapter().getCount() - 1) {
            binding.pager2.setCurrentItem(page, false);
        }

        Info info = OutlineHelper.getForamtingInfo(dc, false);
        binding.footer.maxSeek.setText(info.textPage);
        binding.footer.currentSeek.setText(info.textMax);
        binding.pagesCountIndicator.setText(info.chText);

        binding.footer.currentSeek.setContentDescription(dc.getString(R.string.m_current_page) + " " + info.textMax);
        binding.footer.maxSeek.setContentDescription(dc.getString(R.string.m_total_pages) + " " + info.textPage);

        binding.footer.seekBar.setProgress(page);
        if (dc != null) {
            dc.currentPage = page;
        }

        binding.pagesTime.setText(UiSystemUtils.getSystemTime(this));
        binding.pagesTime1.setText(UiSystemUtils.getSystemTime(this));

        int myLevel = UiSystemUtils.getPowerLevel(this);
        binding.pagesPower.setText(myLevel + "%");
        if (myLevel == -1) {
            binding.pagesPower.setVisibility(View.GONE);
        }
        if (TxtUtils.isNotEmpty(dc.getCurrentChapter())) {
            binding.chapter.setText(dc.getCurrentChapter());
            binding.chapter.setVisibility(View.VISIBLE);
        } else {
            binding.chapter.setVisibility(View.GONE);
        }

        LOG.d("_PAGE", "Update UI", page);
        dc.saveCurrentPage();

        if (dc.floatingBookmark != null) {
            dc.floatingBookmark.p = dc.getPercentage();
            binding.floatingBookmark.setText("{" + dc.getCurentPageFirst1() + "}");
            binding.floatingBookmark.setVisibility(View.VISIBLE);

            BookmarksData.get().add(dc.floatingBookmark);
            showPagesHelper();
        } else {
            binding.floatingBookmark.setVisibility(View.GONE);
        }
    }

    public void loadUI() {
        binding.title.setText(dc.getTitle());
        binding.panelBookTitle.setText(dc.getTitle());
        createAdapter();

        binding.pager2.addOnPageChangeListener(onViewPagerChangeListener);
        binding.pager2.setCurrentItem(dc.getCurentPage(), false);

        binding.footer.seekBar.setMax(dc.getPageCount() - 1);
        binding.footer.seekBar.setProgress(dc.getCurentPage());

        binding.bottomIndicators.setOnTouchListener(new HorizontallSeekTouchEventListener(onSeek, dc.getPageCount(), false));
        binding.progressDraw.setOnTouchListener(new HorizontallSeekTouchEventListener(onSeek, dc.getPageCount(), false));
        binding.bottomIndicators.setOnClickListener(v -> {
            if (AppState.get().tapZoneBottom == AppState.TAP_DO_NOTHING) {
                // do nothing
            } else if (AppState.get().tapZoneBottom == AppState.TAP_NEXT_PAGE) {
                nextPage();
            } else if (AppState.get().tapZoneBottom == AppState.TAP_PREV_PAGE) {
                prevPage();
            }
        });

        updateLockMode();

        tinUI();

        onViewPagerChangeListener.onPageSelected(dc.getCurentPage());

        binding.progressDraw.updatePageCount(dc.getPageCount());

        dc.getOutline(result -> {
            binding.bookClose.setVisibility(View.VISIBLE);
            binding.progressDraw.updateDivs(result);
            updateUI(dc.getCurrentPage());
            if (TxtUtils.isListEmpty(result)) {
                TintUtil.setTintImageWithAlpha(binding.footer.onDocDontext, Color.LTGRAY);
            }
            showPagesHelper();
            return false;
        }, false);

        showHelp();
    }

    public void showHelp() {
        if (AppSP.get().isFirstTimeHorizontal) {
            handler.postDelayed(() -> {
                AppSP.get().isFirstTimeHorizontal = false;
                AppState.get().isEditMode = true;
                hideShow();
                Views.showHelpToast(binding.footer.lockUnlock);
            }, 1000);
        }
    }

    private void tinUI() {
        TintUtil.setTintBgSimple(binding.actionBar, AppState.get().transparencyUI);
        TintUtil.setTintBgSimple(binding.bottomBar, AppState.get().transparencyUI);
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
                dialog.setPositiveButton(R.string.yes, (dialog1, which) -> {
                    onRotateScreen();
                    isInitPosistion = currentPosistion;
                });
                rotatoinDialog = dialog.show();
                rotatoinDialog.getWindow().setLayout((int) (Dips.screenMinWH() * 0.8f), LayoutParams.WRAP_CONTENT);
            }
        } else {
            Keyboards.hideNavigationOnCreate(this);
            dc.udpateImageSize(dc.isTextFormat(), binding.pager2.getWidth(), binding.pager2.getHeight());
            onRotateScreen();
        }

        isInitOrientation = AppState.get().orientation;
    }

    public void authoFit() {
        if (handler == null) {
            return;
        }
        handler.postDelayed(() -> {
            PageImageState.get().isAutoFit = true;
            if (dc != null) {
                dc.cleanImageMatrix();
            }
            EventBus.getDefault().post(new MessageAutoFit(binding.pager2.getCurrentItem()));
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
            authoFit();
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
        binding.pager2.setOffscreenPageLimit(pagesInMemory);
        LOG.d("setOffscreenPageLimit", pagesInMemory);
        binding.pager2.setAdapter(pagerAdapter);
        binding.pager2.setSaveEnabled(false);
        binding.pager2.setSaveFromParentEnabled(false);
    }

    public void hideShow() {
        hideShow(true);
    }

    public void updateBannnerTop() {
        try {
            ((RelativeLayout.LayoutParams) binding.adFrame.getLayoutParams()).topMargin = binding.actionBar.getHeight() + Dips.dpToPx(24);
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
            binding.footer.modeName.setText(R.string.mode_horizontally);
        }

        if (!animated || AppState.get().appTheme == AppState.THEME_INK) {
            binding.actionBar.setVisibility(AppState.get().isEditMode ? View.VISIBLE : View.GONE);
            binding.bottomBar.setVisibility(AppState.get().isEditMode ? View.VISIBLE : View.GONE);
            binding.adFrame.setVisibility(AppState.get().isEditMode ? View.VISIBLE : View.GONE);

            DocumentController.chooseFullScreen(this, AppState.get().fullScreenMode);
            ttsFixPosition();
            return;
        }

        final TranslateAnimation hideActionBar = new TranslateAnimation(0, 0, 0, -binding.actionBar.getHeight());
        final TranslateAnimation hideBottomBar = new TranslateAnimation(0, 0, 0, binding.bottomBar.getHeight());

        final TranslateAnimation showActoinBar = new TranslateAnimation(0, 0, -binding.actionBar.getHeight(), 0);
        final TranslateAnimation showBottomBar = new TranslateAnimation(0, 0, binding.bottomBar.getHeight(), 0);

        final TranslateAnimation adsShow = new TranslateAnimation(-binding.adFrame.getWidth(), 0, 0, 0);
        final TranslateAnimation adsHide = new TranslateAnimation(0, -binding.adFrame.getWidth(), 0, 0);

        updateAnimation(hideActionBar);
        updateAnimation(hideBottomBar);

        updateAnimation(showActoinBar);
        updateAnimation(showBottomBar);

        updateAnimation(adsShow);
        updateAnimation(adsHide);

        if (AppState.get().isEditMode) {
            DocumentController.turnOnButtons(this);

            if (binding.anchor.getVisibility() == View.GONE) {
                binding.adFrame.startAnimation(adsShow);
            }

            binding.actionBar.startAnimation(showActoinBar);
            binding.bottomBar.startAnimation(showBottomBar);

            showBottomBar.setAnimationListener(new AnimationListener() {

                @Override
                public void onAnimationStart(final Animation animation) {
                }

                @Override
                public void onAnimationRepeat(final Animation animation) {
                }

                @Override
                public void onAnimationEnd(final Animation animation) {
                    binding.actionBar.setVisibility(View.VISIBLE);
                    binding.bottomBar.setVisibility(View.VISIBLE);
                    binding.adFrame.setVisibility(View.VISIBLE);
                    binding.adFrame.setTag(null);

                    Keyboards.invalidateEink(binding.parentParent);

                    ttsFixPosition();
                }
            });
        } else {
            DocumentController.turnOffButtons(this);

            if (binding.anchor.getVisibility() == View.GONE && binding.adFrame.getVisibility() == View.VISIBLE) {
                binding.adFrame.startAnimation(adsHide);
            }
            binding.actionBar.startAnimation(hideActionBar);
            binding.bottomBar.startAnimation(hideBottomBar);

            hideBottomBar.setAnimationListener(new AnimationListener() {
                @Override
                public void onAnimationStart(final Animation animation) {
                }

                @Override
                public void onAnimationRepeat(final Animation animation) {
                }

                @Override
                public void onAnimationEnd(final Animation animation) {
                    binding.actionBar.setVisibility(View.GONE);
                    binding.bottomBar.setVisibility(View.GONE);
                    binding.adFrame.setVisibility(View.GONE);

                    Keyboards.invalidateEink(binding.parentParent);

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
        RelativeLayout.LayoutParams layoutParams = (RelativeLayout.LayoutParams) binding.ttsActive.getLayoutParams();
        if (AppState.get().isEditMode) {
            layoutParams.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM, 0);
        } else {
            layoutParams.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
        }
        binding.ttsActive.setLayoutParams(layoutParams);
    }

    @Override
    public boolean onKeyUp(final int keyCode, final KeyEvent event) {
        if (isMyKey) {
            return true;
        }
        if (binding.anchor.getVisibility() == View.GONE) {
            if (keyCode >= KeyEvent.KEYCODE_1 && keyCode <= KeyEvent.KEYCODE_9) {
                dc.onGoToPage(keyCode - KeyEvent.KEYCODE_1 + 1);
                return true;
            }
            if (keyCode == KeyEvent.KEYCODE_0) {
                DragingDialogs.gotoPageDialog(binding.anchor, dc);
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

                    binding.anchor.setTag("");
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

        if (binding.anchor.getChildCount() > 0 && binding.anchor.getVisibility() == View.VISIBLE) {
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
