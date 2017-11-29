package com.foobnix.pdf.search.activity;

import java.io.File;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.ebookdroid.common.settings.SettingsManager;
import org.ebookdroid.droids.mupdf.codec.exceptions.MuPdfPasswordException;
import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import com.foobnix.android.utils.Dips;
import com.foobnix.android.utils.Keyboards;
import com.foobnix.android.utils.LOG;
import com.foobnix.android.utils.ResultResponse;
import com.foobnix.android.utils.TxtUtils;
import com.foobnix.ext.CacheZipUtils;
import com.foobnix.pdf.CopyAsyncTask;
import com.foobnix.pdf.info.ADS;
import com.foobnix.pdf.info.Analytics;
import com.foobnix.pdf.info.Android6;
import com.foobnix.pdf.info.AppsConfig;
import com.foobnix.pdf.info.DictsHelper;
import com.foobnix.pdf.info.ExtUtils;
import com.foobnix.pdf.info.IMG;
import com.foobnix.pdf.info.R;
import com.foobnix.pdf.info.TintUtil;
import com.foobnix.pdf.info.UiSystemUtils;
import com.foobnix.pdf.info.model.BookCSS;
import com.foobnix.pdf.info.model.OutlineLinkWrapper;
import com.foobnix.pdf.info.view.AlertDialogs;
import com.foobnix.pdf.info.view.BrigtnessDraw;
import com.foobnix.pdf.info.view.Dialogs;
import com.foobnix.pdf.info.view.DragingDialogs;
import com.foobnix.pdf.info.view.DragingPopup;
import com.foobnix.pdf.info.view.HorizontallSeekTouchEventListener;
import com.foobnix.pdf.info.view.MyPopupMenu;
import com.foobnix.pdf.info.view.ProgressDraw;
import com.foobnix.pdf.info.view.UnderlineImageView;
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
import com.foobnix.tts.MessagePageNumber;
import com.foobnix.tts.TTSEngine;
import com.foobnix.tts.TTSNotification;
import com.foobnix.tts.TtsStatus;
import com.foobnix.ui2.AppDB;
import com.foobnix.ui2.MainTabs2;
import com.foobnix.ui2.MyContextWrapper;
import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.InterstitialAd;
import com.google.android.gms.ads.NativeExpressAdView;

import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
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
import android.support.v4.graphics.ColorUtils;
import android.support.v4.view.ViewPager.OnPageChangeListener;
import android.text.InputType;
import android.view.KeyEvent;
import android.view.MenuItem;
import android.view.MenuItem.OnMenuItemClickListener;
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
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

public class HorizontalViewActivity extends FragmentActivity {
    private static final String PAGE = "page";
    private static final String PERCENT_EXTRA = "percent";

    VerticalViewPager viewPager;
    SeekBar seekBar;
    private TextView maxSeek, currentSeek, pagesCountIndicator, flippingIntervalView, pagesTime, pagesPower, titleTxt, chapterView;
    View adFrame, bottomBar, onPageFlip1, bottomIndicators, moveCenter, onClose;
    LinearLayout actionBar;
    private FrameLayout anchor;

    private AdView adView;
    private NativeExpressAdView adViewNative;

    ImageView lockModelImage, linkHistory, ttsActive, onModeChange, outline, onMove, onBC, textToSpeach;

    DocumentControllerHorizontalView documentController;

    Handler handler, handlerTimer;
    CopyAsyncTask loadinAsyncTask;

    Dialog rotatoinDialog;
    volatile Boolean isInitPosistion = null;

    ProgressDraw progressDraw;
    BrigtnessDraw brigtnessProgressView;

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
        handlerTimer = new Handler();
        flippingHandler = new Handler();
        flippingTimer = 0;

        long crateBegin = System.currentTimeMillis();

        requestWindowFeature(Window.FEATURE_NO_TITLE);
        DocumentController.applyBrigtness(this);

        if (AppState.getInstance().isInvert) {
            setTheme(R.style.StyledIndicatorsWhite);
        } else {
            setTheme(R.style.StyledIndicatorsBlack);
        }

        super.onCreate(savedInstanceState);

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
        brigtnessProgressView.setOverlay(findViewById(R.id.overlay));
        brigtnessProgressView.setOnSingleClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                if (AppState.get().tapZoneLeft == AppState.TAP_PREV_PAGE) {
                    prevPage();
                } else {
                    nextPage();
                }
            }
        });

        actionBar = (LinearLayout) findViewById(R.id.actionBar);

        bottomBar = findViewById(R.id.bottomBar);
        bottomIndicators = findViewById(R.id.bottomIndicators);
        adFrame = findViewById(R.id.adFrame);
        anchor = (FrameLayout) findViewById(R.id.anchor);
        moveCenter = findViewById(R.id.moveCenter);

        currentSeek = (TextView) findViewById(R.id.currentSeek);
        maxSeek = (TextView) findViewById(R.id.maxSeek);
        pagesCountIndicator = (TextView) findViewById(R.id.pagesCountIndicator);
        flippingIntervalView = (TextView) findViewById(R.id.flippingIntervalView);
        pagesTime = (TextView) findViewById(R.id.pagesTime);
        pagesPower = (TextView) findViewById(R.id.pagesPower);
        linkHistory = (ImageView) findViewById(R.id.linkHistory);
        onMove = (ImageView) findViewById(R.id.onMove);
        onMove.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                DragingDialogs.onMoveDialog(anchor, documentController, onRefresh, reloadDoc);
            }
        });

        currentSeek.setOnLongClickListener(new OnLongClickListener() {

            @Override
            public boolean onLongClick(View v) {
                Dialogs.showDeltaPage(anchor, documentController, documentController.getCurentPageFirst1(), onRefresh);
                return true;
            }
        });

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
        linkHistory.setVisibility(View.GONE);

        seekBar = (SeekBar) findViewById(R.id.seekBar);

        if (AppState.get().isRTL) {
            if (Build.VERSION.SDK_INT >= 11) {
                seekBar.setRotation(180);
            }
        }

        // loaginTask.execute();

        // ADS.activate(this, adView);
        ADS.activateNative(this, adViewNative);

        onPageFlip1 = findViewById(R.id.onPageFlip);
        onPageFlip1.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                DragingDialogs.pageFlippingDialog(anchor, documentController, onRefresh);
            }
        });

        final ImageView onFullScreen = (ImageView) findViewById(R.id.onFullScreen);
        onFullScreen.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                AppState.get().isFullScreen = !AppState.get().isFullScreen;
                onFullScreen.setImageResource(AppState.get().isFullScreen ? R.drawable.glyphicons_487_fit_frame_to_image : R.drawable.glyphicons_488_fit_image_to_frame);
                DocumentController.chooseFullScreen(HorizontalViewActivity.this, AppState.get().isFullScreen);
                if (documentController.isTextFormat()) {
                    if (onRefresh != null) {
                        onRefresh.run();
                    }
                    documentController.restartActivity();
                }
            }
        });
        onFullScreen.setImageResource(AppState.get().isFullScreen ? R.drawable.glyphicons_487_fit_frame_to_image : R.drawable.glyphicons_488_fit_image_to_frame);

        ImageView dayNightButton = (ImageView) findViewById(R.id.bookNight);
        dayNightButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(final View v) {
                AppState.get().isInvert = !AppState.get().isInvert;
                documentController.restartActivity();
            }
        });
        if (Dips.isEInk(this)) {
            dayNightButton.setVisibility(View.GONE);
            AppState.get().isInvert = true;
        }

        onBC = (ImageView) findViewById(R.id.onBC);
        onBC.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(final View v) {
                DragingDialogs.contrastAndBrigtness(anchor, documentController, reloadDocBrigntness, reloadDoc);
            }
        });

        dayNightButton.setImageResource(!AppState.get().isInvert ? R.drawable.glyphicons_232_sun : R.drawable.glyphicons_2_moon);

        moveCenter.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                PageImageState.get().isAutoFit = true;
                EventBus.getDefault().post(new MessageAutoFit(viewPager.getCurrentItem()));
            }
        });

        onMove.setVisibility(AppState.get().isInkMode ? View.VISIBLE : View.GONE);
        onBC.setVisibility(AppState.get().isInkMode ? View.VISIBLE : View.GONE);

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

        outline = (ImageView) findViewById(R.id.content);
        outline.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(final View v) {
                DragingDialogs.showContent(anchor, documentController);
            }
        });
        findViewById(R.id.bookmarks).setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(final View v) {
                DragingDialogs.addBookmarks(anchor, documentController, new Runnable() {

                    @Override
                    public void run() {
                        showHideHistory();
                    }
                });
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

        textToSpeach = (ImageView) findViewById(R.id.bookTTS);
        textToSpeach.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(final View v) {
                LOG.d("bookTTS", AppState.get().isDoubleCoverAlone, AppState.get().isDouble, AppState.get().isCut);
                if (AppState.get().isDouble || AppState.get().isCut) {
                    modeOnePage();
                    return;
                }
                DragingDialogs.textToSpeachDialog(anchor, documentController);
            }
        });
        ttsActive = (ImageView) findViewById(R.id.ttsActive);
        onTTSStatus(null);
        ttsActive.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(final View v) {
                DragingDialogs.textToSpeachDialog(anchor, documentController);
            }
        });

        onModeChange = (ImageView) findViewById(R.id.onModeChange);
        onModeChange.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                MyPopupMenu p = new MyPopupMenu(v.getContext(), v);
                p.getMenu().add(R.string.one_page).setIcon(R.drawable.glyphicons_two_page_one).setOnMenuItemClickListener(new OnMenuItemClickListener() {

                    @Override
                    public boolean onMenuItemClick(MenuItem item) {
                        onModeChange.setImageResource(R.drawable.glyphicons_two_page_one);
                        modeOnePage();
                        documentController.cleanImageMatrix();
                        return false;
                    }
                });
                p.getMenu().add(R.string.two_pages).setIcon(R.drawable.glyphicons_two_pages_12).setOnMenuItemClickListener(new OnMenuItemClickListener() {

                    @Override
                    public boolean onMenuItemClick(MenuItem item) {
                        onModeChange.setImageResource(R.drawable.glyphicons_two_pages_12);
                        AppState.get().isDouble = true;
                        AppState.get().isCut = false;
                        AppState.get().isDoubleCoverAlone = false;
                        SettingsManager.getBookSettings().updateFromAppState();
                        documentController.restartActivity();
                        documentController.cleanImageMatrix();
                        return false;
                    }
                });
                p.getMenu().add(R.string.two_pages_cover).setIcon(R.drawable.glyphicons_two_pages_23).setOnMenuItemClickListener(new OnMenuItemClickListener() {

                    @Override
                    public boolean onMenuItemClick(MenuItem item) {
                        onModeChange.setImageResource(R.drawable.glyphicons_two_pages_23);
                        AppState.get().isDouble = true;
                        AppState.get().isCut = false;
                        AppState.get().isDoubleCoverAlone = true;
                        SettingsManager.getBookSettings().updateFromAppState();
                        documentController.restartActivity();
                        documentController.cleanImageMatrix();
                        return false;
                    }
                });
                p.getMenu().add(R.string.half_page).setIcon(R.drawable.glyphicons_page_split).setOnMenuItemClickListener(new OnMenuItemClickListener() {

                    @Override
                    public boolean onMenuItemClick(MenuItem item) {
                        onModeChange.setImageResource(R.drawable.glyphicons_page_split);
                        AppState.get().isDouble = false;
                        AppState.get().isCut = true;
                        AppState.get().isCrop = false;
                        SettingsManager.getBookSettings().updateFromAppState();
                        TTSEngine.get().stop();
                        reloadDoc.run();
                        return false;
                    }
                });
                if (false) {
                    p.getMenu().add(R.string.crop_white_borders).setIcon(R.drawable.glyphicons_94_crop).setOnMenuItemClickListener(new OnMenuItemClickListener() {

                        @Override
                        public boolean onMenuItemClick(MenuItem item) {
                            AppState.get().isCrop = !AppState.get().isCrop;
                            SettingsManager.getBookSettings().cropPages = AppState.get().isCrop;
                            reloadDoc.run();
                            return false;
                        }
                    });
                }
                p.show();
                Keyboards.hideNavigation(HorizontalViewActivity.this);

            }
        });

        final UnderlineImageView onCrop = (UnderlineImageView) findViewById(R.id.onCrop);
        onCrop.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                AppState.get().isCrop = !AppState.get().isCrop;
                SettingsManager.getBookSettings().cropPages = AppState.get().isCrop;
                reloadDoc.run();
                onCrop.underline(AppState.get().isCrop);

                PageImageState.get().isAutoFit = true;
                EventBus.getDefault().post(new MessageAutoFit(viewPager.getCurrentItem()));
            }
        });

        findViewById(R.id.bookMenu).setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(final View v) {
                ShareDialog.show(HorizontalViewActivity.this, documentController.getCurrentBook(), new Runnable() {

                    @Override
                    public void run() {
                        if (documentController.getCurrentBook().delete()) {
                            TempHolder.listHash++;
                            AppDB.get().deleteBy(documentController.getCurrentBook().getPath());
                            documentController.getActivity().finish();
                        }
                    }
                }, documentController.getCurentPage(), null);
                Keyboards.hideNavigation(HorizontalViewActivity.this);

            }
        });

        findViewById(R.id.bookPref).setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(final View v) {
                DragingDialogs.preferences(anchor, documentController, onRefresh, reloadDoc);
            }
        });

        onClose = findViewById(R.id.bookClose);
        onClose.setVisibility(View.GONE);

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

        Keyboards.hideNavigationOnCreate(HorizontalViewActivity.this);

        currentSeek.setVisibility(View.GONE);
        maxSeek.setVisibility(View.GONE);
        seekBar.setVisibility(View.GONE);
        bottomIndicators.setVisibility(View.GONE);

        titleTxt.setText(DocumentControllerHorizontalView.getTempTitle(this));
        loadinAsyncTask = new CopyAsyncTask() {
            AlertDialog dialog;
            private boolean isCancelled = false;

            @Override
            protected void onPreExecute() {
                dialog = Dialogs.loadingBook(HorizontalViewActivity.this, new Runnable() {

                    @Override
                    public void run() {
                        isCancelled = true;
                        TempHolder.get().loadingCancelled = true;
                        finish();
                        CacheZipUtils.removeFiles(CacheZipUtils.CACHE_BOOK_DIR.listFiles());
                    }
                }, true);
            };

            @Override
            protected Object doInBackground(Object... params) {
                try {
                    while (viewPager.getHeight() == 0) {
                        try {
                            Thread.sleep(250);
                        } catch (InterruptedException e) {
                        }
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
            };

            @Override
            protected void onPostExecute(Object result) {
                try {
                    onClose.setVisibility(View.VISIBLE);
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

                    currentSeek.setVisibility(View.VISIBLE);
                    maxSeek.setVisibility(View.VISIBLE);
                    seekBar.setVisibility(View.VISIBLE);
                    bottomIndicators.setVisibility(View.VISIBLE);
                    onModeChange.setVisibility(View.VISIBLE);

                    documentController.initHandler();
                    AppState.get().lastA = HorizontalViewActivity.class.getSimpleName();
                    AppState.get().lastMode = HorizontalViewActivity.class.getSimpleName();
                    LOG.d("lasta save", AppState.get().lastA);

                    AppState.get().isEditMode = true;
                    PageImageState.get().isAutoFit = PageImageState.get().needAutoFit;

                    if (ExtUtils.isTextFomat(getIntent())) {
                        PageImageState.get().isAutoFit = true;
                        moveCenter.setVisibility(View.GONE);
                    } else if (AppState.get().isLockPDF) {
                        // moveCenter.setVisibility(View.VISIBLE);
                        AppState.get().isLocked = true;
                    }

                    if (ExtUtils.isNoTextLayerForamt(documentController.getCurrentBook().getPath())) {
                        TintUtil.setTintImage(textToSpeach, Color.LTGRAY);
                    }
                    if (documentController.isTextFormat()) {
                        // TintUtil.setTintImage(lockModelImage, Color.LTGRAY);
                    }

                    loadUI();

                    updateUI(documentController.getPageFromUri());

                    EventBus.getDefault().post(new MessageAutoFit(documentController.getPageFromUri()));
                    AppState.get().isEditMode = true;
                    seekBar.setOnSeekBarChangeListener(onSeek);
                    RecentUpates.updateAll(HorizontalViewActivity.this);
                    showHideInfoToolBar();

                    testScreenshots();

                    isInitPosistion = Dips.screenHeight() > Dips.screenWidth();

                    updateIconMode();

                    onCrop.setVisibility(documentController.isTextFormat() ? View.GONE : View.VISIBLE);
                    onMove.setVisibility(AppState.get().isInkMode && !documentController.isTextFormat() ? View.VISIBLE : View.GONE);
                    onBC.setVisibility(AppState.get().isInkMode ? View.VISIBLE : View.GONE);

                    onCrop.underline(AppState.get().isCrop);
                    onCrop.invalidate();

                }

            };
        };
        loadinAsyncTask.executeOnExecutor(Executors.newSingleThreadExecutor());
        updateIconMode();

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

    public void updateIconMode() {
        if (AppState.get().isDouble) {
            if (AppState.get().isDoubleCoverAlone) {
                onModeChange.setImageResource(R.drawable.glyphicons_two_pages_23);
            } else {
                onModeChange.setImageResource(R.drawable.glyphicons_two_pages_12);
            }
        } else if (AppState.get().isCut) {
            onModeChange.setImageResource(R.drawable.glyphicons_page_split);
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
        AppState.get().isDouble = false;
        AppState.get().isDoubleCoverAlone = false;
        AppState.get().isCut = false;
        SettingsManager.getBookSettings().updateFromAppState();
        documentController.restartActivity();
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
            documentController.getOutline(null, false);
            updateReadPercent();

            updateUI(viewPager.getCurrentItem());
            showHideInfoToolBar();
            updateSeekBarColorAndSize();
            hideShow();
            TTSEngine.get().stop();

        }
    };

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onTTSStatus(TtsStatus status) {
        ttsActive.setVisibility(TTSEngine.get().isPlaying() ? View.VISIBLE : View.GONE);
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onPageNumber(final MessagePageNumber event) {
        ttsActive.setVisibility(View.VISIBLE);
        documentController.onGoToPage(event.getPage() + 1);
    }

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
            DragingDialogs.selectTextMenu(anchor, documentController, true, onRefresh);
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
        linkHistory.setVisibility(!documentController.getLinkHistory().isEmpty() ? View.VISIBLE : View.GONE);

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
        DragingDialogs.searchMenu(anchor, documentController, "");
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

            documentController.getOutline(null, false);
            documentController.saveCurrentPage();
            createAdapter();

            loadUI();
        }
    };
    Runnable reloadDocBrigntness = new Runnable() {

        @Override
        public void run() {
            IMG.clearMemoryCache();
            ImagePageFragment f2 = (ImagePageFragment) getSupportFragmentManager().findFragmentByTag("f" + (viewPager.getCurrentItem()));
            if (f2 != null) {
                f2.loadImage();
            }
            return;
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
            if (TTSEngine.get().isPlaying()) {
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
        if (viewPager != null) {
            try {
                viewPager.setAdapter(null);
            } catch (Exception e) {
                LOG.e(e);
            }
        }

        ADS.destory(adView);
        ADS.destoryNative(adViewNative);
        // AppState.get().isCut = false;
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
        handlerTimer.post(updateTimePower);
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
        handlerTimer.removeCallbacks(updateTimePower);

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
                    DragingDialogs.selectTextMenu(anchor, documentController, true, onRefresh);
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

    public void initAsync(int w, int h) {
        documentController = new DocumentControllerHorizontalView(this, w, h) {
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

    Runnable updateTimePower = new Runnable() {

        @Override
        public void run() {
            try {
                if (pagesTime != null) {
                    pagesTime.setText(UiSystemUtils.getSystemTime(HorizontalViewActivity.this));

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

    public void updateUI(int page) {
        if (documentController == null) {
            return;
        }

        if (page <= viewPager.getAdapter().getCount() - 1) {
            viewPager.setCurrentItem(page, false);
        }

        String textPage = TxtUtils.deltaPage(page + 1);
        if (AppState.get().isRTL) {
            maxSeek.setText("" + textPage);
        } else {
            currentSeek.setText("" + textPage);
        }
        pagesCountIndicator.setText(textPage + "∕" + documentController.getPageCount());
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

        onTTSStatus(null);
        LOG.d("_PAGE", "Update UI", page);
    }

    public void loadUI() {
        titleTxt.setText(documentController.getTitle());
        createAdapter();

        viewPager.addOnPageChangeListener(onViewPagerChangeListener);
        viewPager.setCurrentItem(documentController.getCurentPage(), false);

        seekBar.setMax(documentController.getPageCount() - 1);
        seekBar.setProgress(documentController.getCurentPage());

        bottomIndicators.setOnTouchListener(new HorizontallSeekTouchEventListener(onSeek, documentController.getPageCount(), false));
        progressDraw.setOnTouchListener(new HorizontallSeekTouchEventListener(onSeek, documentController.getPageCount(), false));

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
                if (TxtUtils.isListEmpty(result)) {
                    TintUtil.setTintImage(outline, Color.LTGRAY);
                }
                return false;
            }
        }, false);

    }

    private void tinUI() {
        TintUtil.setTintBgSimple(actionBar, 230);
        TintUtil.setTintBgSimple(bottomBar, 230);
        TintUtil.setStatusBarColor(this);
        TintUtil.setBackgroundFillColorBottomRight(ttsActive, ColorUtils.setAlphaComponent(TintUtil.color, 230));
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
                                if (AppState.get().isDouble) {
                                    int page1 = pos * 2;
                                    int page2 = pos * 2 + 1;

                                    if (AppState.get().isDoubleCoverAlone && pos >= 1) {
                                        page1--;
                                        page2--;
                                    }

                                    PageImageState.get().pagesText.put(page1, documentController.getPageText(page1));
                                    PageImageState.get().pagesLinks.put(page1, documentController.getLinksForPage(page1));

                                    PageImageState.get().pagesText.put(page2, documentController.getPageText(page2));
                                    PageImageState.get().pagesLinks.put(page2, documentController.getLinksForPage(page2));

                                } else if (!AppState.get().isCut) {
                                    PageImageState.get().pagesText.put(pos, documentController.getPageText(pos));
                                    PageImageState.get().pagesLinks.put(pos, documentController.getLinksForPage(pos));
                                }
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
        clickUtils.init();
        if (isInitPosistion == null) {
            return;
        }
        handler.removeCallbacksAndMessages(null);

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
            Keyboards.hideNavigationOnCreate(this);
            documentController.udpateImageSize(viewPager.getWidth(), viewPager.getHeight());
            onRotateScreen();
        }

    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public void onRotateScreen() {
        // ADS.activate(this, adView);
        ADS.activateNative(this, adViewNative);

        AppState.get().save(this);
        if (ExtUtils.isTextFomat(getIntent())) {
            updateReadPercent();
            documentController.restartActivity();
        } else {
            if (viewPager != null) {
                handler.postDelayed(new Runnable() {

                    @Override
                    public void run() {
                        PageImageState.get().isAutoFit = true;
                        EventBus.getDefault().post(new MessageAutoFit(viewPager.getCurrentItem()));
                    }
                }, 50);

            }
        }
    }

    UpdatableFragmentPagerAdapter pagerAdapter;

    public void createAdapter() {
        IMG.clearMemoryCache();
        viewPager.setAdapter(null);
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
                b.putString(ImagePageFragment.PAGE_PATH, documentController.getPageUrl(position).toString());

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

        if (AppState.get().isInkMode) {
            actionBar.setVisibility(AppState.get().isEditMode ? View.VISIBLE : View.GONE);
            bottomBar.setVisibility(AppState.get().isEditMode ? View.VISIBLE : View.GONE);
            adFrame.setVisibility(AppState.get().isEditMode ? View.VISIBLE : View.GONE);

            DocumentController.chooseFullScreen(this, AppState.get().isFullScreen);
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
        LOG.d("onKeyDown", keyCode);

        isMyKey = false;

        if (AppState.get().isUseVolumeKeys) {

            if (AppState.get().isUseVolumeKeys && KeyEvent.KEYCODE_HEADSETHOOK == keyCode) {
                if (TTSEngine.get().isPlaying()) {
                    TTSEngine.get().stop();
                } else {
                    TTSEngine.get().playCurrent();
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
        if (false && CloseAppDialog.checkLongPress(this, event)) {
            CloseAppDialog.showOnLongClickDialog(HorizontalViewActivity.this, null, documentController);
            return true;
        }
        return super.onKeyLongPress(keyCode, event);
    }

    @Override
    public void onBackPressed() {
        if (anchor != null && anchor.getChildCount() > 0 && anchor.getVisibility() == View.VISIBLE) {
            documentController.clearSelectedText();
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

        CloseAppDialog.showOnLongClickDialog(HorizontalViewActivity.this, null, documentController);
        // closeActivity();
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
