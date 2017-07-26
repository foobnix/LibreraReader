package com.foobnix.pdf.search.activity;

import java.io.File;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.ebookdroid.droids.mupdf.codec.exceptions.MuPdfPasswordException;
import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;

import com.foobnix.android.utils.Dips;
import com.foobnix.android.utils.LOG;
import com.foobnix.android.utils.ResultResponse;
import com.foobnix.android.utils.TxtUtils;
import com.foobnix.pdf.CopyAsyncTask;
import com.foobnix.pdf.info.ADS;
import com.foobnix.pdf.info.Analytics;
import com.foobnix.pdf.info.Android6;
import com.foobnix.pdf.info.AppsConfig;
import com.foobnix.pdf.info.DictsHelper;
import com.foobnix.pdf.info.ExtUtils;
import com.foobnix.pdf.info.IMG;
import com.foobnix.pdf.info.R;
import com.foobnix.pdf.info.TTSModule;
import com.foobnix.pdf.info.TintUtil;
import com.foobnix.pdf.info.UiSystemUtils;
import com.foobnix.pdf.info.model.BookCSS;
import com.foobnix.pdf.info.model.OutlineLinkWrapper;
import com.foobnix.pdf.info.view.AlertDialogs;
import com.foobnix.pdf.info.view.BrigtnessDraw;
import com.foobnix.pdf.info.view.DragingDialogs;
import com.foobnix.pdf.info.view.DragingPopup;
import com.foobnix.pdf.info.view.HorizontallSeekTouchEventListener;
import com.foobnix.pdf.info.view.ProgressDraw;
import com.foobnix.pdf.info.widget.FileInformationDialog;
import com.foobnix.pdf.info.widget.RecentUpates;
import com.foobnix.pdf.info.widget.ShareDialog;
import com.foobnix.pdf.info.wrapper.AppState;
import com.foobnix.pdf.info.wrapper.DocumentController;
import com.foobnix.pdf.info.wrapper.MagicHelper;
import com.foobnix.pdf.search.activity.msg.FlippingStart;
import com.foobnix.pdf.search.activity.msg.FlippingStop;
import com.foobnix.pdf.search.activity.msg.InvalidateMessage;
import com.foobnix.pdf.search.activity.msg.MessageAutoFit;
import com.foobnix.pdf.search.activity.msg.MessageEvent;
import com.foobnix.pdf.search.view.CloseAppDialog;
import com.foobnix.pdf.search.view.VerticalViewPager;
import com.foobnix.sys.ClickUtils;
import com.foobnix.sys.TempHolder;
import com.foobnix.ui2.MainTabs2;
import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.InterstitialAd;
import com.google.android.gms.ads.NativeExpressAdView;

import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Parcelable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.view.ViewPager.OnPageChangeListener;
import android.text.InputType;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.view.ViewTreeObserver.OnGlobalLayoutListener;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.view.animation.TranslateAnimation;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

public class HorizontalViewActivity extends FragmentActivity {
    private static final String PAGE = "page";
    private static final String PERCENT_EXTRA = "percent";

    VerticalViewPager viewPager;
    SeekBar seekBar;
    private TextView maxSeek, currentSeek, pagesCountIndicator, flippingIntervalView, pagesTime, pagesPower, titleTxt, chapterView;
    View actionBar, adFrame, bottomBar, onPageFlip1, bottomIndicators;
    private FrameLayout anchor;

    private AdView adView;
    private NativeExpressAdView adViewNative;

    ImageView lockModelImage, linkHistory;

    DocumentControllerHorizontalView documentController;

    Handler handler;
    CopyAsyncTask loadinAsyncTask;

    Dialog rotatoinDialog;
    volatile Boolean isInitPosistion = null;

    ProgressDraw progressDraw;
    BrigtnessDraw brigtnessProgressView;

    @Override
    protected void onNewIntent(final Intent intent) {

        if (!intent.filterEquals(getIntent())) {
            finish();
            startActivity(intent);
        }
    }

    InterstitialAd mInterstitialAd;

    ClickUtils clickUtils;

    Handler flippingHandler;
    int flippingTimer = 0;

    protected void onCreateTest(final Bundle savedInstanceState) {
        DocumentController.doRotation(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_horiziontal_view);
    }

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        DocumentController.doRotation(this);
        handler = new Handler();
        flippingHandler = new Handler();
        flippingTimer = 0;

        if (!AppsConfig.checkIsProInstalled(this) && AppsConfig.ADMOB_FULLSCREEN != null) {
            handler.postDelayed(new Runnable() {

                @Override
                public void run() {
                    mInterstitialAd = new InterstitialAd(HorizontalViewActivity.this);
                    mInterstitialAd.setAdUnitId(AppsConfig.ADMOB_FULLSCREEN);
                    mInterstitialAd.setAdListener(new AdListener() {
                        @Override
                        public void onAdClosed() {
                            closeActivity();
                        }
                    });

                    try {
                        mInterstitialAd.loadAd(ADS.adRequest);
                    } catch (Exception e) {
                        LOG.e(e);
                    }
                }
            }, ADS.FULL_SCREEN_TIMEOUT);

        }

        long crateBegin = System.currentTimeMillis();

        requestWindowFeature(Window.FEATURE_NO_TITLE);
        DocumentController.applyBrigtness(this);

        if (AppState.getInstance().isInvert) {
            setTheme(R.style.StyledIndicatorsWhite);
        } else {
            setTheme(R.style.StyledIndicatorsBlack);
        }

        super.onCreate(savedInstanceState);

        clickUtils = new ClickUtils();

        AppState.get().load(this);

        // AppState.get().isCut = false;
        PageImageState.get().isShowCuttingLine = false;

        PageImageState.get().cleanSelectedWords();

        setContentView(R.layout.activity_horiziontal_view);

        Android6.checkPermissions(this);

        viewPager = (VerticalViewPager) findViewById(R.id.pager);
        viewPager.setOffscreenPageLimit(AppState.get().pagesInMemory);
        LOG.d("setOffscreenPageLimit", AppState.get().pagesInMemory);

        progressDraw = (ProgressDraw) findViewById(R.id.progressDraw);
        brigtnessProgressView = (BrigtnessDraw) findViewById(R.id.brigtnessProgressView);
        brigtnessProgressView.setActivity(this);

        actionBar = findViewById(R.id.actionBar);
        bottomBar = findViewById(R.id.bottomBar);
        bottomIndicators = findViewById(R.id.bottomIndicators);
        adFrame = findViewById(R.id.adFrame);
        anchor = (FrameLayout) findViewById(R.id.anchor);

        currentSeek = (TextView) findViewById(R.id.currentSeek);
        maxSeek = (TextView) findViewById(R.id.maxSeek);
        pagesCountIndicator = (TextView) findViewById(R.id.pagesCountIndicator);
        flippingIntervalView = (TextView) findViewById(R.id.flippingIntervalView);
        pagesTime = (TextView) findViewById(R.id.pagesTime);
        pagesPower = (TextView) findViewById(R.id.pagesPower);
        linkHistory = (ImageView) findViewById(R.id.linkHistory);

        updateSeekBarColorAndSize();

        titleTxt = (TextView) findViewById(R.id.title);
        chapterView = (TextView) findViewById(R.id.chapter);

        if (AppState.get().isUseTypeFace) {
            currentSeek.setTypeface(BookCSS.getNormalTypeFace());
            maxSeek.setTypeface(BookCSS.getNormalTypeFace());
            pagesCountIndicator.setTypeface(BookCSS.getNormalTypeFace());
            pagesTime.setTypeface(BookCSS.getNormalTypeFace());
            pagesPower.setTypeface(BookCSS.getNormalTypeFace());
            titleTxt.setTypeface(BookCSS.getNormalTypeFace());
            chapterView.setTypeface(BookCSS.getNormalTypeFace());
        }

        linkHistory.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                documentController.onLinkHistory();
                showHideHistory();
            }
        });

        seekBar = (SeekBar) findViewById(R.id.seekBar);

        if (AppState.get().isRTL) {
            if (Build.VERSION.SDK_INT >= 11) {
                seekBar.setRotation(180);
            }
        }

        // loaginTask.execute();

        // ADS.activate(this, adView);
        ADS.activateNative(this, adViewNative);

        onPageFlip1 = findViewById(R.id.onPageFlip1);
        onPageFlip1.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                DragingDialogs.pageFlippingDialog(anchor, documentController, onRefresh);
            }
        });

        ImageView dayNightButton = (ImageView) findViewById(R.id.bookNight);
        dayNightButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(final View v) {
                AppState.get().isInvert = !AppState.get().isInvert;
                documentController.restartActivity();
            }
        });
        dayNightButton.setImageResource(!AppState.get().isInvert ? R.drawable.glyphicons_232_sun : R.drawable.glyphicons_2_moon);

        findViewById(R.id.thumbnail).setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(final View v) {
                DragingDialogs.thumbnailDialog(anchor, documentController);

            }
        });
        findViewById(R.id.search).setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(final View v) {
                showSearchDialog();
            }

        });

        findViewById(R.id.content).setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(final View v) {
                DragingDialogs.showContent(anchor, documentController);
            }
        });
        findViewById(R.id.bookmarks).setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(final View v) {
                DragingDialogs.addBookmarks(anchor, documentController);
            }
        });
        findViewById(R.id.bookmarks).setOnLongClickListener(new View.OnLongClickListener() {

            @Override
            public boolean onLongClick(final View arg0) {
                DragingDialogs.addBookmarksLong(anchor, documentController);
                return true;
            }
        });

        findViewById(R.id.bookRecent).setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(final View v) {
                DragingDialogs.recentBooks(anchor, documentController);
            }
        });

        View tts = findViewById(R.id.bookTTS);
        tts.setVisibility(TTSModule.isAvailableTTS() ? View.VISIBLE : View.GONE);
        tts.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(final View v) {
                DragingDialogs.textToSpeachDialog(anchor, documentController);
            }
        });

        findViewById(R.id.bookMenu).setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(final View v) {
                ShareDialog.show(HorizontalViewActivity.this, documentController.getCurrentBook(), new Runnable() {

                    @Override
                    public void run() {
                        if (documentController.getCurrentBook().delete()) {
                            documentController.getActivity().finish();
                        }
                    }
                }, documentController.getCurentPage(), null);

            }
        });

        findViewById(R.id.bookPref).setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(final View v) {
                DragingDialogs.preferences(anchor, documentController, onRefresh, reloadDoc);
            }
        });

        View onClose = findViewById(R.id.bookClose);
        onClose.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(final View v) {
                closeActivity();
            }
        });
        onClose.setOnLongClickListener(new OnLongClickListener() {

            @Override
            public boolean onLongClick(final View v) {
                CloseAppDialog.showOnLongClickDialog(HorizontalViewActivity.this, v, documentController);
                return false;
            }
        });

        lockModelImage = (ImageView) findViewById(R.id.lockMode);
        lockModelImage.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                AppState.get().isLocked = !AppState.get().isLocked;
                updateLockMode();
            }
        });

        loadinAsyncTask = new CopyAsyncTask() {
            ProgressDialog dialog;
            private boolean isCancelled = false;

            @Override
            protected void onPreExecute() {
                dialog = ProgressDialog.show(HorizontalViewActivity.this, "", getString(R.string.msg_loading));
                dialog.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
            };

            @Override
            protected Object doInBackground(Object... params) {
                try {
                    initAsync();
                } catch (MuPdfPasswordException e) {
                    return -1;
                } catch (RuntimeException e) {
                    return -2;
                }
                return 0;
            }

            @Override
            protected void onCancelled() {
                try {
                    dialog.dismiss();
                } catch (Exception e) {
                }
                isCancelled = true;
            };

            @Override
            protected void onPostExecute(Object result) {
                try {
                    LOG.d("RESULT", result);
                    dialog.dismiss();
                } catch (Exception e) {
                }
                if (isCancelled) {
                    return;
                }
                if ((Integer) result == -2) {
                    Toast.makeText(HorizontalViewActivity.this, R.string.msg_unexpected_error, Toast.LENGTH_SHORT).show();
                    AppState.get().isEditMode = true;
                    hideShow();
                    return;
                }

                if ((Integer) result == -1) {
                    final EditText input = new EditText(HorizontalViewActivity.this);
                    input.setSingleLine(true);
                    input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);

                    AlertDialog.Builder dialog = new AlertDialog.Builder(HorizontalViewActivity.this);
                    dialog.setTitle(R.string.enter_password);
                    dialog.setView(input);
                    dialog.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {

                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                            closeActivity();
                        }

                    });
                    dialog.setPositiveButton(R.string.open_file, new DialogInterface.OnClickListener() {

                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            String txt = input.getText().toString();
                            if (TxtUtils.isNotEmpty(txt)) {
                                dialog.dismiss();
                                closeActivity();

                                getIntent().putExtra(DocumentControllerHorizontalView.PASSWORD_EXTRA, txt);
                                startActivity(getIntent());
                            } else {
                                closeActivity();
                            }
                        }
                    });
                    AlertDialog show = dialog.show();

                } else {
                    documentController.initHandler();
                    AppState.get().lastA = HorizontalViewActivity.class.getSimpleName();
                    AppState.get().lastMode = HorizontalViewActivity.class.getSimpleName();
                    LOG.d("lasta save", AppState.get().lastA);

                    AppState.get().isEditMode = true;
                    PageImageState.get().isAutoFit = PageImageState.get().needAutoFit;

                    if (ExtUtils.isTextFomat(getIntent())) {
                        PageImageState.get().isAutoFit = true;
                    } else if (AppState.get().isLockPDF) {
                        AppState.get().isLocked = true;
                    }

                    loadUI();

                    updateUI(documentController.getPageFromUri());

                    EventBus.getDefault().post(new MessageAutoFit(documentController.getPageFromUri()));
                    AppState.get().isEditMode = true;
                    seekBar.setOnSeekBarChangeListener(onSeek);
                    bottomIndicators.setOnTouchListener(new HorizontallSeekTouchEventListener(onSeek, documentController.getPageCount(), false));
                    progressDraw.setOnTouchListener(new HorizontallSeekTouchEventListener(onSeek, documentController.getPageCount(), false));
                    RecentUpates.updateAll(HorizontalViewActivity.this);
                    showHideInfoToolBar();

                    testScreenshots();

                    isInitPosistion = Dips.screenHeight() > Dips.screenWidth();

                }

            };
        };
        loadinAsyncTask.execute();

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
                    if (AppState.get().isEditMode) {
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

    public void updateSeekBarColorAndSize() {

        TintUtil.setTintText(pagesPower, TintUtil.getStatusBarColor());
        TintUtil.setTintText(pagesTime, TintUtil.getStatusBarColor());
        TintUtil.setTintText(pagesCountIndicator, TintUtil.getStatusBarColor());
        TintUtil.setTintText(flippingIntervalView, TintUtil.getStatusBarColor());

        if (false) {
            GradientDrawable bg = (GradientDrawable) pagesPower.getBackground();
            bg.setStroke(1, TintUtil.getStatusBarColor());
        } else {
            pagesPower.setBackgroundColor(Color.TRANSPARENT);
        }

        pagesPower.setTextSize(AppState.get().statusBarTextSizeEasy);
        pagesTime.setTextSize(AppState.get().statusBarTextSizeEasy);
        pagesCountIndicator.setTextSize(AppState.get().statusBarTextSizeEasy);
        flippingIntervalView.setTextSize(AppState.get().statusBarTextSizeEasy);

        int progressColor = AppState.get().isInvert ? AppState.get().statusBarColorDay : MagicHelper.otherColor(AppState.get().statusBarColorNight, +0.2f);
        progressDraw.updateColor(progressColor);

        progressDraw.getLayoutParams().height = Dips.dpToPx(AppState.get().progressLineHeight);
        progressDraw.requestLayout();

    }

    Runnable onRefresh = new Runnable() {

        @Override
        public void run() {
            documentController.getOutline(null);
            updateReadPercent();

            showHideInfoToolBar();
            updateSeekBarColorAndSize();
            hideShow();

        }
    };

    @Subscribe
    public void onFlippingStart(FlippingStart event) {
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
    }

    @Subscribe
    public void onFlippingStop(FlippingStop event) {
        flippingHandler.removeCallbacks(flippingRunnable);
        flippingHandler.removeCallbacksAndMessages(null);
        flippingIntervalView.setVisibility(View.GONE);
    }

    Runnable flippingRunnable = new Runnable() {

        @Override
        public void run() {

            if (flippingTimer >= AppState.get().flippingInterval) {
                flippingTimer = 0;
                if (documentController.getCurentPage() == documentController.getPageCount() - 1) {
                    if (AppState.get().isLoopAutoplay) {
                        documentController.onGoToPage(1);
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

    public void testScreenshots() {

        if (getIntent().hasExtra("id1")) {
            DragingDialogs.thumbnailDialog(anchor, documentController);

        }
        if (getIntent().hasExtra("id2")) {
            DragingDialogs.showContent(anchor, documentController);
        }
        if (getIntent().hasExtra("id3")) {
            findViewById(R.id.bookPref).performClick();
        }

        if (getIntent().hasExtra("id4")) {
            DragingDialogs.selectTextMenu(anchor, documentController, true);
        }

        if (getIntent().hasExtra("id5")) {
            DragingDialogs.textToSpeachDialog(anchor, documentController);
        }

        if (getIntent().hasExtra("id6")) {
            DragingDialogs.moreBookSettings(anchor, documentController, null, null);
        }
        if (getIntent().hasExtra("id7")) {
            FileInformationDialog.showFileInfoDialog(documentController.getActivity(), new File(documentController.getBookPath()), null);
        }

        AppState.get().isEditMode = getIntent().getBooleanExtra("isEditMode", false);
        hideShow();
    }

    public void showHideHistory() {
        linkHistory.setVisibility(documentController.getLinkHistory().isEmpty() ? View.GONE : View.VISIBLE);
    }

    public void showHideInfoToolBar() {
        int isVisible = AppState.get().isShowToolBar ? View.VISIBLE : View.GONE;
        pagesTime.setVisibility(isVisible);
        pagesCountIndicator.setVisibility(isVisible);
        pagesPower.setVisibility(isVisible);
        bottomIndicators.setVisibility(isVisible);

        progressDraw.setVisibility(AppState.get().isShowReadingProgress ? View.VISIBLE : View.GONE);

        brigtnessProgressView.setVisibility(AppState.get().isBrighrnessEnable ? View.VISIBLE : View.GONE);

    }

    private void showSearchDialog() {
        if (AppState.get().isCrop || AppState.get().isCut) {
            AppState.get().isCrop = false;
            AppState.get().isCut = false;
            reloadDoc.run();
        }
        DragingDialogs.searchMenu(anchor, documentController);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        Android6.onRequestPermissionsResult(this, requestCode, permissions, grantResults);
    }

    public void updateLockMode() {
        if (AppState.get().isLocked) {
            lockModelImage.setImageResource(R.drawable.glyphicons_204_lock);
        } else {
            lockModelImage.setImageResource(R.drawable.glyphicons_205_unlock);
        }

    }

    Runnable reloadDoc = new Runnable() {

        @Override
        public void run() {
            documentController.getOutline(null);
            documentController.saveCurrentPage();
            createAdapter();

            loadUI();
        }
    };

    @Override
    public void onStart() {
        super.onStart();
        Analytics.onStart(this);
        EventBus.getDefault().register(this);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

    }

    @Override
    public void onStop() {
        EventBus.getDefault().unregister(this);
        super.onStop();
        Analytics.onStop(this);
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        if (flippingHandler != null) {
            flippingHandler.removeCallbacksAndMessages(null);
        }

    }

    Runnable closeRunnable = new Runnable() {

        @Override
        public void run() {
            if (TTSModule.getInstance().isPlaying()) {
                LOG.d("TTS is playing");
                return;
            }

            LOG.d("Close App");
            if (documentController != null) {
                documentController.onCloseActivity();
            } else {
                finish();
            }
            MainTabs2.closeApp(HorizontalViewActivity.this);
        }
    };

    @Override
    protected void onDestroy() {
        super.onDestroy();
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
        ADS.destory(adView);
        ADS.destoryNative(adViewNative);
        // AppState.get().isCut = false;
        if (TTSModule.getInstance() != null) {
            TTSModule.getInstance().shutdownTTS();
        }
        PageImageState.get().clearResouces();

    }

    public void updateReadPercent() {
        if (documentController != null) {
            double value = (documentController.getCurentPage() + 0.0001) / documentController.getPageCount();
            getIntent().putExtra(PERCENT_EXTRA, value);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        ADS.onResume(adView);
        ADS.onResumeNative(adViewNative);
        DocumentController.chooseFullScreen(this, AppState.get().isFullScreen);
        DocumentController.doRotation(this);

        if (clickUtils != null) {
            clickUtils.init();
        }

        if (documentController != null) {
            documentController.onResume();
        }
        handler.removeCallbacks(closeRunnable);
    }

    @Override
    protected void onPause() {
        super.onPause();
        ADS.onPause(adView);
        ADS.onPauseNative(adViewNative);
        if (documentController != null) {
            documentController.saveCurrentPage();
        }
        AppState.get().save(this);
        TempHolder.isSeaching = false;

        handler.postDelayed(closeRunnable, AppState.APP_CLOSE_AUTOMATIC);

    }

    public void nextPage() {
        flippingTimer = 0;
        viewPager.setCurrentItem(documentController.getCurentPage() + 1, AppState.get().isScrollAnimation);
        documentController.checkReadingTimer();
    }

    public void prevPage() {
        flippingTimer = 0;
        viewPager.setCurrentItem(documentController.getCurentPage() - 1, AppState.get().isScrollAnimation);
        documentController.checkReadingTimer();
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    @Subscribe
    public void onEvent(MessageEvent ev) {
        if (ev.getMessage().equals(MessageEvent.MESSAGE_PERFORM_CLICK)) {
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
                handler.removeCallbacks(doShowHideWrapperControllsRunnable);
                handler.postDelayed(doShowHideWrapperControllsRunnable, 250);
                // Toast.makeText(this, "Click", Toast.LENGTH_SHORT).show();
            }
        } else if (ev.getMessage().equals(MessageEvent.MESSAGE_DOUBLE_TAP)) {
            handler.removeCallbacks(doShowHideWrapperControllsRunnable);
            updateLockMode();
            // Toast.makeText(this, "DB", Toast.LENGTH_SHORT).show();
        } else if (ev.getMessage().equals(MessageEvent.MESSAGE_SELECTED_TEXT)) {
            if (documentController.isTextFormat() && TxtUtils.isFooterNote(AppState.get().selectedText)) {
                showFootNotes = DragingDialogs.showFootNotes(anchor, documentController, new Runnable() {

                    @Override
                    public void run() {
                        showHideHistory();
                    }
                });
            } else {

                if (AppState.get().isRememberDictionary) {
                    DictsHelper.runIntent(anchor.getContext(), AppState.get().selectedText);
                } else {
                    DragingDialogs.selectTextMenu(anchor, documentController, true);
                }
            }
        } else if (ev.getMessage().equals(MessageEvent.MESSAGE_GOTO_PAGE)) {
            if (ev.getPage() == -1 && TxtUtils.isNotEmpty(ev.getBody())) {
                AlertDialogs.openUrl(this, ev.getBody());
            } else {
                documentController.getLinkHistory().add(documentController.getCurentPage() + 1);
                documentController.onGoToPage(ev.getPage() + 1);
                showHideHistory();
            }
        }
    }

    Runnable doShowHideWrapperControllsRunnable = new Runnable() {

        @Override
        public void run() {
            doShowHideWrapperControlls();
        }
    };

    DragingPopup showFootNotes;

    private void doShowHideWrapperControlls() {

        if (showFootNotes != null && showFootNotes.isVisible()) {
            showFootNotes.closeDialog();
            return;
        }

        AppState.get().isEditMode = !AppState.get().isEditMode;
        hideShow();
    }

    Runnable onCloseDialog = new Runnable() {

        @Override
        public void run() {
            if (AppState.get().selectedText != null) {
                AppState.get().selectedText = null;
                EventBus.getDefault().post(new InvalidateMessage());
            }
        }
    };

    public void initAsync() {
        documentController = new DocumentControllerHorizontalView(this) {
            @Override
            public void onGoToPageImpl(int page) {
                updateUI(page);
                EventBus.getDefault().post(new InvalidateMessage());
            }

            @Override
            public void notifyAdapterDataChanged() {
            }
        };
        documentController.init(this);
    }

    public void updateUI(int page) {
        if (documentController == null) {
            return;
        }

        if (page <= viewPager.getAdapter().getCount() - 1) {
            viewPager.setCurrentItem(page, false);
        }

        if (AppState.get().isRTL) {
            maxSeek.setText("" + (page + 1));
        } else {
            currentSeek.setText("" + (page + 1));
        }
        pagesCountIndicator.setText((page + 1) + "∕" + documentController.getPageCount());
        seekBar.setProgress(page);
        if (documentController != null) {
            documentController.currentPage = page;
        }

        pagesTime.setText(UiSystemUtils.getSystemTime(this));

        int myLevel = UiSystemUtils.getPowerLevel(this);
        pagesPower.setText(myLevel + "%");
        if (myLevel == -1) {
            pagesPower.setVisibility(View.GONE);
        }
        if (TxtUtils.isNotEmpty(documentController.getCurrentChapter())) {
            chapterView.setText(documentController.getCurrentChapter());
            chapterView.setVisibility(View.VISIBLE);
        } else {
            chapterView.setVisibility(View.GONE);
        }

        LOG.d("_PAGE", "Update UI", page);
    }

    public void loadUI() {
        titleTxt.setText(documentController.getTitle());
        createAdapter();

        viewPager.addOnPageChangeListener(onViewPagerChangeListener);
        viewPager.setCurrentItem(documentController.getCurentPage(), false);

        seekBar.setMax(documentController.getPageCount() - 1);
        seekBar.setProgress(documentController.getCurentPage());

        currentSeek.setText("" + (documentController.getCurentPage() + 1));
        maxSeek.setText("" + documentController.getPageCount());

        if (AppState.get().isRTL) {
            maxSeek.setText("" + (documentController.getCurentPage() + 1));
            currentSeek.setText("" + documentController.getPageCount());
        }

        updateLockMode();

        tinUI();

        onViewPagerChangeListener.onPageSelected(documentController.getCurentPage());

        progressDraw.updatePageCount(documentController.getPageCount());
        documentController.getOutline(new ResultResponse<List<OutlineLinkWrapper>>() {

            @Override
            public boolean onResultRecive(List<OutlineLinkWrapper> result) {
                progressDraw.updateDivs(result);
                return false;
            }
        });

    }

    private void tinUI() {
        TintUtil.setTintBgSimple(actionBar, 230);
        TintUtil.setTintBgSimple(bottomBar, 230);
        TintUtil.setStatusBarColor(this);
    }

    OnPageChangeListener onViewPagerChangeListener = new OnPageChangeListener() {

        @Override
        public void onPageSelected(final int pos) {
            PageImageState.currentPage = pos;
            updateUI(pos);

            if (PageImageState.get().isAutoFit) {
                EventBus.getDefault().post(new MessageAutoFit(pos));
            }
            if (PageImageState.get().pagesText != null && PageImageState.get().pagesText.get(pos) == null) {
                handler.removeCallbacksAndMessages(null);
                handler.postDelayed(new Runnable() {

                    @Override
                    public void run() {
                        new Thread() {
                            @Override
                            public void run() {
                                PageImageState.get().pagesText.put(pos, documentController.getPageText(pos));
                                PageImageState.get().pagesLinks.put(pos, documentController.getLinksForPage(pos));
                                LOG.d("onPageSelected", "load", pos);
                            };
                        }.start();
                    }
                }, 100);

                handler.postDelayed(new Runnable() {

                    @Override
                    public void run() {
                        documentController.saveCurrentPage();
                        LOG.d("PAGE SAVED");
                    }
                }, 2000);

                handler.postDelayed(new Runnable() {

                    @Override
                    public void run() {
                        try {
                            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                        } catch (Exception e) {
                            LOG.e(e);
                        }
                    }
                }, TimeUnit.MINUTES.toMillis(AppState.get().inactivityTime));

            }
            LOG.d("onPageSelected", pos);

            progressDraw.updateProgress(pos);
        }

        @Override
        public void onPageScrolled(int arg0, float arg1, int arg2) {
            // TODO Auto-generated method stub

        }

        @Override
        public void onPageScrollStateChanged(int arg0) {
            // TODO Auto-generated method stub

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
        }
    };

    // @Override
    @Override
    public void onConfigurationChanged(final Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        if (isInitPosistion == null) {
            return;
        }

        if (ExtUtils.isTextFomat(getIntent())) {

            final boolean currentPosistion = Dips.screenHeight() > Dips.screenWidth();

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
            }
        } else {
            onRotateScreen();
        }

        handler.removeCallbacksAndMessages(null);
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public void onRotateScreen() {
        // ADS.activate(this, adView);
        ADS.activateNative(this, adViewNative);

        AppState.get().save(this);
        if (ExtUtils.isTextFomat(getIntent())) {
            updateReadPercent();
            recreate();
        } else {
            PageImageState.get().isAutoFit = true;
            if (viewPager != null) {
                handler.postDelayed(new Runnable() {

                    @Override
                    public void run() {
                        EventBus.getDefault().post(new MessageAutoFit(documentController.currentPage));
                    }
                }, 50);

            }
        }
    }

    UpdatableFragmentPagerAdapter pagerAdapter;

    public void createAdapter() {
        IMG.clearMemoryCache();
        pagerAdapter = null;
        pagerAdapter = new UpdatableFragmentPagerAdapter(getSupportFragmentManager()) {

            @Override
            public int getCount() {
                return documentController.getPageCount();
            }

            @Override
            public Fragment getItem(final int position) {
                final ImagePageFragment imageFragment = new ImagePageFragment();

                final Bundle b = new Bundle();
                b.putInt(ImagePageFragment.POS, position);
                b.putString(ImagePageFragment.PAGE_PATH, documentController.getPagePath(position));

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
        viewPager.setAdapter(pagerAdapter);
        viewPager.setSaveEnabled(false);
        viewPager.setSaveFromParentEnabled(false);
    }

    public boolean prev = true;

    public void hideShow() {
        if (prev == AppState.get().isEditMode) {
            return;
        }
        prev = AppState.get().isEditMode;

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

                }
            });

        } else {
            if (anchor.getVisibility() == View.GONE) {
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

                }

            });

        }

        DocumentController.chooseFullScreen(this, AppState.get().isFullScreen);
    }

    private boolean isMyKey = false;

    @Override
    public boolean onKeyUp(final int keyCode, final KeyEvent event) {
        if (isMyKey) {
            return true;
        }
        if (anchor != null && anchor.getVisibility() == View.GONE) {
            if (keyCode >= KeyEvent.KEYCODE_1 && keyCode <= KeyEvent.KEYCODE_9) {
                documentController.onGoToPage(keyCode - KeyEvent.KEYCODE_1 + 1);
                return true;
            }
            if (keyCode == KeyEvent.KEYCODE_0) {
                DragingDialogs.thumbnailDialog(anchor, documentController);
                return true;
            }

            if (KeyEvent.KEYCODE_MENU == keyCode || KeyEvent.KEYCODE_M == keyCode) {
                doShowHideWrapperControlls();
                return true;
            }
            if (KeyEvent.KEYCODE_F == keyCode) {
                documentController.alignDocument();
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

        isMyKey = false;

        if (AppState.get().isUseVolumeKeys) {

            if (AppState.get().isUseVolumeKeys && KeyEvent.KEYCODE_HEADSETHOOK == keyCode) {
                if (TTSModule.getInstance().isPlaying()) {
                    TTSModule.getInstance().stop();
                } else {
                    TTSModule.getInstance().play();
                    anchor.setTag("");
                }
                DragingDialogs.textToSpeachDialog(anchor, documentController);
                return true;
            }

            if (AppState.get().getNextKeys().contains(keyCode)) {
                if (showFootNotes != null && showFootNotes.isVisible()) {
                    showFootNotes.closeDialog();
                    isMyKey = true;
                    return true;
                }
                nextPage();
                flippingTimer = 0;
                isMyKey = true;
                return true;
            } else if (AppState.get().getPrevKeys().contains(keyCode)) {
                if (showFootNotes != null && showFootNotes.isVisible()) {
                    showFootNotes.closeDialog();
                    isMyKey = true;
                    return true;
                }
                prevPage();
                flippingTimer = 0;
                isMyKey = true;
                return true;
            }
        }

        return super.onKeyDown(keyCode, event);
    }

    @Override
    public boolean onKeyLongPress(final int keyCode, final KeyEvent event) {
        if (CloseAppDialog.checkLongPress(this, event)) {
            CloseAppDialog.showOnLongClickDialog(HorizontalViewActivity.this, null, documentController);
            return true;
        }
        return super.onKeyLongPress(keyCode, event);
    }

    @Override
    public void onBackPressed() {
        if (anchor != null && anchor.getChildCount() > 0 && anchor.getVisibility() == View.VISIBLE) {
            anchor.setVisibility(View.GONE);
            anchor.setTag("backGo");
            anchor.removeAllViews();
            return;
        }

        if (documentController != null && !documentController.getLinkHistory().isEmpty()) {
            documentController.onLinkHistory();
            showHideHistory();
            return;
        }

        closeActivity();
    }

    public void closeActivity() {
        AppState.get().lastA = null;
        if (handler != null) {
            handler.removeCallbacksAndMessages(null);
        }

        if (mInterstitialAd != null && mInterstitialAd.isLoaded()) {
            mInterstitialAd.show();
        } else {
            if (documentController != null) {
                documentController.onCloseActivity();
            } else {
                finish();
            }
        }

    }

    private void updateAnimation(final TranslateAnimation a) {
        a.setDuration(250);
    }
}
