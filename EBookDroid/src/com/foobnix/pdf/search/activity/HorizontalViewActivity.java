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
import com.foobnix.android.utils.Vibro;
import com.foobnix.android.utils.Views;
import com.foobnix.ext.CacheZipUtils;
import com.foobnix.pdf.CopyAsyncTask;
import com.foobnix.pdf.info.ADS;
import com.foobnix.pdf.info.Android6;
import com.foobnix.pdf.info.DictsHelper;
import com.foobnix.pdf.info.ExtUtils;
import com.foobnix.pdf.info.IMG;
import com.foobnix.pdf.info.R;
import com.foobnix.pdf.info.TintUtil;
import com.foobnix.pdf.info.UiSystemUtils;
import com.foobnix.pdf.info.model.BookCSS;
import com.foobnix.pdf.info.model.OutlineLinkWrapper;
import com.foobnix.pdf.info.view.AlertDialogs;
import com.foobnix.pdf.info.view.BrightnessHelper;
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
import com.foobnix.pdf.search.activity.msg.MessegeBrightness;
import com.foobnix.pdf.search.view.CloseAppDialog;
import com.foobnix.pdf.search.view.VerticalViewPager;
import com.foobnix.sys.ClickUtils;
import com.foobnix.sys.TempHolder;
import com.foobnix.tts.MessagePageNumber;
import com.foobnix.tts.TTSEngine;
import com.foobnix.tts.TTSNotification;
import com.foobnix.tts.TtsStatus;
import com.foobnix.ui2.AdsFragmentActivity;
import com.foobnix.ui2.AppDB;
import com.foobnix.ui2.MainTabs2;
import com.foobnix.ui2.MyContextWrapper;
import com.nostra13.universalimageloader.core.ImageLoader;

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
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

public class HorizontalViewActivity extends AdsFragmentActivity {
    private static final String PAGE = "page";
    private static final String PERCENT_EXTRA = "percent";

    VerticalViewPager viewPager;
    SeekBar seekBar;
    TextView toastBrightnessText, maxSeek, currentSeek, pagesCountIndicator, flippingIntervalView, pagesTime, pagesPower, titleTxt, chapterView;
    View adFrame, bottomBar, bottomIndicators, moveCenter, onClose, overlay;
    LinearLayout actionBar;
    FrameLayout anchor;
    UnderlineImageView onCrop, onBC;

    ImageView lockModelImage, linkHistory, ttsActive, onModeChange, outline, onMove, textToSpeach, onPageFlip1;

    HorizontalModeController dc;

    Handler handler, handlerTimer;
    CopyAsyncTask loadinAsyncTask;

    Dialog rotatoinDialog;
    volatile Boolean isInitPosistion = null;
    volatile int isInitOrientation;

    ProgressDraw progressDraw;

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
        intetrstialTimeoutSec = ADS.FULL_SCREEN_TIMEOUT_SEC;
        DocumentController.doRotation(this);
        handler = new Handler();
        handlerTimer = new Handler();
        flippingHandler = new Handler();
        flippingTimer = 0;

        long crateBegin = System.currentTimeMillis();

        requestWindowFeature(Window.FEATURE_NO_TITLE);
        BrightnessHelper.applyBrigtness(this);

        if (AppState.get().isInvert) {
            setTheme(R.style.StyledIndicatorsWhite);
        } else {
            setTheme(R.style.StyledIndicatorsBlack);
        }

        super.onCreate(savedInstanceState);

        boolean isTextFomat = ExtUtils.isTextFomat(getIntent());

        clickUtils = new ClickUtils();

        AppState.get().load(this);

        // AppState.get().isCut = false;
        PageImageState.get().isShowCuttingLine = false;

        PageImageState.get().cleanSelectedWords();

        setContentView(R.layout.activity_horiziontal_view);

        Android6.checkPermissions(this);

        viewPager = (VerticalViewPager) findViewById(R.id.pager);

        overlay = findViewById(R.id.overlay);
        overlay.setVisibility(View.VISIBLE);

        progressDraw = (ProgressDraw) findViewById(R.id.progressDraw);

        actionBar = (LinearLayout) findViewById(R.id.actionBar);

        bottomBar = findViewById(R.id.bottomBar);
        bottomIndicators = findViewById(R.id.bottomIndicators);
        adFrame = findViewById(R.id.adFrame);
        anchor = (FrameLayout) findViewById(R.id.anchor);
        moveCenter = findViewById(R.id.moveCenter);

        currentSeek = (TextView) findViewById(R.id.currentSeek);
        maxSeek = (TextView) findViewById(R.id.maxSeek);

        toastBrightnessText = (TextView) findViewById(R.id.toastBrightnessText);
        toastBrightnessText.setVisibility(View.GONE);
        TintUtil.setDrawableTint(toastBrightnessText.getCompoundDrawables()[0], Color.WHITE);

        TextView modeName = (TextView) findViewById(R.id.modeName);
        modeName.setText(AppState.get().nameHorizontalMode);

        pagesCountIndicator = (TextView) findViewById(R.id.pagesCountIndicator);
        flippingIntervalView = (TextView) findViewById(R.id.flippingIntervalView);
        pagesTime = (TextView) findViewById(R.id.pagesTime);
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

        final ImageView onFullScreen = (ImageView) findViewById(R.id.onFullScreen);
        onFullScreen.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                AppState.get().isFullScreen = !AppState.get().isFullScreen;
                onFullScreen.setImageResource(AppState.get().isFullScreen ? R.drawable.glyphicons_487_fit_frame_to_image : R.drawable.glyphicons_488_fit_image_to_frame);
                DocumentController.chooseFullScreen(HorizontalViewActivity.this, AppState.get().isFullScreen);
                if (dc.isTextFormat()) {
                    if (onRefresh != null) {
                        onRefresh.run();
                    }
                    nullAdapter();
                    dc.restartActivity();
                }
            }
        });
        onFullScreen.setImageResource(AppState.get().isFullScreen ? R.drawable.glyphicons_487_fit_frame_to_image : R.drawable.glyphicons_488_fit_image_to_frame);

        ImageView dayNightButton = (ImageView) findViewById(R.id.bookNight);
        dayNightButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(final View v) {
                v.setEnabled(false);
                AppState.get().isInvert = !AppState.get().isInvert;
                nullAdapter();
                dc.restartActivity();
            }
        });
        if (Dips.isEInk(this)) {
            dayNightButton.setVisibility(View.GONE);
            AppState.get().isInvert = true;
        }

        onBC = (UnderlineImageView) findViewById(R.id.onBC);
        onBC.underline(AppState.get().bolderTextOnImage || AppState.get().brigtnessImage != 0 || AppState.get().contrastImage != 0);
        onBC.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(final View v) {
                DragingDialogs.contrastAndBrigtness(anchor, dc, reloadDocBrigntness, reloadDoc);
            }
        });

        dayNightButton.setImageResource(!AppState.get().isInvert ? R.drawable.glyphicons_232_sun : R.drawable.glyphicons_2_moon);

        moveCenter.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                authoFit();
            }
        });

        onBC.setVisibility(isTextFomat ? View.GONE : View.VISIBLE);
        if (Dips.isEInk(this) || AppState.get().bolderTextOnImage || AppState.get().brigtnessImage != 0 || AppState.get().contrastImage != 0) {
            onBC.setVisibility(View.VISIBLE);
        }
        onMove.setVisibility(Dips.isEInk(this) && !isTextFomat ? View.VISIBLE : View.GONE);

        findViewById(R.id.thumbnail).setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(final View v) {
                DragingDialogs.thumbnailDialog(anchor, dc);

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
                DragingDialogs.showContent(anchor, dc);
            }
        });
        findViewById(R.id.onBookmarks).setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(final View v) {
                DragingDialogs.addBookmarks(anchor, dc, new Runnable() {

                    @Override
                    public void run() {
                        showHideHistory();
                    }
                });
            }
        });

        findViewById(R.id.onBookmarks).setOnLongClickListener(new View.OnLongClickListener() {

            @Override
            public boolean onLongClick(final View arg0) {
                DragingDialogs.addBookmarksLong(anchor, dc);
                return true;
            }
        });

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
                LOG.d("bookTTS", AppState.get().isDoubleCoverAlone, AppState.get().isDouble, AppState.get().isCut);
                if (AppState.get().isDouble || AppState.get().isCut) {
                    modeOnePage();
                    return;
                }
                DragingDialogs.textToSpeachDialog(anchor, dc);
            }
        });
        ttsActive = (ImageView) findViewById(R.id.ttsActive);
        onTTSStatus(null);
        ttsActive.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(final View v) {
                DragingDialogs.textToSpeachDialog(anchor, dc);
            }
        });
        ttsActive.setOnLongClickListener(new OnLongClickListener() {

            @Override
            public boolean onLongClick(View v) {
                Vibro.vibrate();
                ttsActive.setVisibility(View.GONE);
                return true;
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
                        AppState.get().isDouble = false;
                        AppState.get().isDoubleCoverAlone = false;
                        AppState.get().isCut = false;
                        SettingsManager.getBookSettings().updateFromAppState();

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
                        onModeChange.setImageResource(R.drawable.glyphicons_two_pages_12);
                        AppState.get().isDouble = true;
                        AppState.get().isCut = false;
                        AppState.get().isDoubleCoverAlone = false;
                        SettingsManager.getBookSettings().updateFromAppState();
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
                            onModeChange.setImageResource(R.drawable.glyphicons_two_pages_23);
                            AppState.get().isDouble = true;
                            AppState.get().isCut = false;
                            AppState.get().isDoubleCoverAlone = true;
                            SettingsManager.getBookSettings().updateFromAppState();
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
                            onModeChange.setImageResource(R.drawable.glyphicons_page_split);
                            AppState.get().isDouble = false;
                            AppState.get().isCut = true;
                            AppState.get().isCrop = false;
                            SettingsManager.getBookSettings().updateFromAppState();
                            TTSEngine.get().stop();

                            dc.cleanImageMatrix();
                            reloadDoc.run();
                            authoFit();
                            return false;
                        }
                    });
                }
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

        onCrop = (UnderlineImageView) findViewById(R.id.onCrop);
        onCrop.setVisibility(isTextFomat ? View.GONE : View.VISIBLE);
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

        final View bookMenu = findViewById(R.id.bookMenu);
        bookMenu.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(final View v) {
                ShareDialog.show(HorizontalViewActivity.this, dc.getCurrentBook(), new Runnable() {

                    @Override
                    public void run() {
                        if (dc.getCurrentBook().delete()) {
                            TempHolder.listHash++;
                            AppDB.get().deleteBy(dc.getCurrentBook().getPath());
                            dc.getActivity().finish();
                        }
                    }
                }, dc.getCurentPage(), null, dc);
                Keyboards.hideNavigation(HorizontalViewActivity.this);

            }
        });
        modeName.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                bookMenu.performClick();
            }
        });

        findViewById(R.id.bookPref).setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(final View v) {
                DragingDialogs.preferences(anchor, dc, onRefresh, reloadDoc);
            }
        });

        onClose = findViewById(R.id.bookClose);
        onClose.setVisibility(View.INVISIBLE);

        onClose.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(final View v) {
                nullAdapter();
                closeDialogs();
                showInterstial();
            }

        });
        onClose.setOnLongClickListener(new OnLongClickListener() {

            @Override
            public boolean onLongClick(final View v) {
                Vibro.vibrate();
                CloseAppDialog.showOnLongClickDialog(HorizontalViewActivity.this, v, dc);
                return true;
            }
        });

        findViewById(R.id.editTop2).setVisibility(View.GONE);
        findViewById(R.id.nextTypeBootom).setVisibility(View.GONE);

        lockModelImage = (ImageView) findViewById(R.id.lockUnlock);
        lockModelImage.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                AppState.get().isLocked = !AppState.get().isLocked;
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
            private boolean isCancelled = false;

            @Override
            protected void onPreExecute() {
                TempHolder.get().loadingCancelled = false;
                dialog = Dialogs.loadingBook(HorizontalViewActivity.this, new Runnable() {

                    @Override
                    public void run() {
                        isCancelled = true;
                        TempHolder.get().loadingCancelled = true;
                        finish();
                        CacheZipUtils.removeFiles(CacheZipUtils.CACHE_BOOK_DIR.listFiles());
                    }
                });
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
                            dc.onCloseActivityFinal(null);
                        }

                    });
                    dialog.setPositiveButton(R.string.open_file, new DialogInterface.OnClickListener() {

                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            final String txt = input.getText().toString();
                            if (TxtUtils.isNotEmpty(txt)) {
                                dialog.dismiss();
                                dc.onCloseActivityFinal(new Runnable() {

                                    @Override
                                    public void run() {
                                        getIntent().putExtra(HorizontalModeController.PASSWORD_EXTRA, txt);
                                        startActivity(getIntent());
                                    }
                                });

                            } else {
                                dc.onCloseActivityFinal(null);
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

                    dc.initHandler();
                    AppState.get().lastClosedActivity = HorizontalViewActivity.class.getSimpleName();
                    AppState.get().lastMode = HorizontalViewActivity.class.getSimpleName();
                    LOG.d("lasta save", AppState.get().lastClosedActivity);

                    PageImageState.get().isAutoFit = PageImageState.get().needAutoFit;

                    if (ExtUtils.isTextFomat(getIntent())) {
                        PageImageState.get().isAutoFit = true;
                        // moveCenter.setVisibility(View.GONE);
                    } else if (AppState.get().isLockPDF) {
                        // moveCenter.setVisibility(View.VISIBLE);
                        AppState.get().isLocked = true;
                    }

                    if (ExtUtils.isNoTextLayerForamt(dc.getCurrentBook().getPath())) {
                        TintUtil.setTintImageWithAlpha(textToSpeach, Color.LTGRAY);
                    }
                    if (dc.isTextFormat()) {
                        // TintUtil.setTintImage(lockModelImage, Color.LTGRAY);
                    }

                    loadUI();

                    // AppState.get().isEditMode = false; //remember last
                    updateUI(dc.getPageFromUri());
                    hideShow();

                    EventBus.getDefault().post(new MessageAutoFit(dc.getPageFromUri()));
                    seekBar.setOnSeekBarChangeListener(onSeek);
                    showHideInfoToolBar();

                    testScreenshots();

                    isInitPosistion = Dips.screenHeight() > Dips.screenWidth();
                    isInitOrientation = AppState.get().orientation;

                    updateIconMode();

                    onCrop.setVisibility(dc.isTextFormat() ? View.GONE : View.VISIBLE);
                    onMove.setVisibility(AppState.get().isInkMode && !dc.isTextFormat() ? View.VISIBLE : View.GONE);
                    onBC.setVisibility(dc.isTextFormat() ? View.GONE : View.VISIBLE);
                    if (Dips.isEInk(dc.getActivity()) || AppState.get().bolderTextOnImage || AppState.get().brigtnessImage != 0 || AppState.get().contrastImage != 0) {
                        onBC.setVisibility(View.VISIBLE);
                    }

                    onCrop.underline(AppState.get().isCrop);
                    onCrop.invalidate();

                    // RecentUpates.updateAll(HorizontalViewActivity.this);

                }

            };
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

    @Subscribe
    public void onMessegeBrightness(MessegeBrightness msg) {
        BrightnessHelper.onMessegeBrightness(msg, toastBrightnessText, overlay);
    }

    private void closeDialogs() {
        anchor.setVisibility(View.GONE);
        anchor.setTag("backGo");
        anchor.removeAllViews();
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
        nullAdapter();
        dc.restartActivity();
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
            dc.getOutline(null, false);
            updateReadPercent();

            updateUI(viewPager.getCurrentItem());
            showHideInfoToolBar();
            updateSeekBarColorAndSize();
            BrightnessHelper.updateOverlay(overlay);
            hideShow();
            TTSEngine.get().stop();

        }
    };

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onTTSStatus(TtsStatus status) {
        try {
            ttsActive.setVisibility(TTSEngine.get().isPlaying() ? View.VISIBLE : View.GONE);
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
        onPageFlip1.setImageResource(R.drawable.glyphicons_175_pause);
    }

    @Subscribe
    public void onFlippingStop(FlippingStop event) {
        flippingHandler.removeCallbacks(flippingRunnable);
        flippingHandler.removeCallbacksAndMessages(null);
        flippingIntervalView.setVisibility(View.GONE);
        onPageFlip1.setImageResource(R.drawable.glyphicons_174_play);

    }

    Runnable flippingRunnable = new Runnable() {

        @Override
        public void run() {

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

    public void testScreenshots() {

        if (getIntent().hasExtra("id1")) {
            DragingDialogs.thumbnailDialog(anchor, dc);

        }
        if (getIntent().hasExtra("id2")) {
            DragingDialogs.showContent(anchor, dc);
        }
        if (getIntent().hasExtra("id3")) {
            findViewById(R.id.bookPref).performClick();
        }

        if (getIntent().hasExtra("id4")) {
            DragingDialogs.selectTextMenu(anchor, dc, true, onRefresh);
        }

        if (getIntent().hasExtra("id5")) {
            DragingDialogs.textToSpeachDialog(anchor, dc);
        }

        if (getIntent().hasExtra("id6")) {
            DragingDialogs.moreBookSettings(anchor, dc, null, null);
        }
        if (getIntent().hasExtra("id7")) {
            FileInformationDialog.showFileInfoDialog(dc.getActivity(), new File(dc.getBookPath()), null);
        }

        if (false) {
            AppState.get().isEditMode = getIntent().getBooleanExtra("isEditMode", false);
            hideShow();
        }
    }

    public void showHideHistory() {
        linkHistory.setVisibility(!dc.getLinkHistory().isEmpty() ? View.VISIBLE : View.GONE);

    }

    public void showHideInfoToolBar() {
        int isVisible = AppState.get().isShowToolBar ? View.VISIBLE : View.GONE;
        pagesTime.setVisibility(isVisible);
        pagesCountIndicator.setVisibility(isVisible);
        pagesPower.setVisibility(isVisible);
        bottomIndicators.setVisibility(isVisible);

        progressDraw.setVisibility(AppState.get().isShowReadingProgress ? View.VISIBLE : View.GONE);

    }

    private void showSearchDialog() {
        if (AppState.get().isCrop || AppState.get().isCut) {
            onModeChange.setImageResource(R.drawable.glyphicons_two_page_one);
            AppState.get().isCrop = false;
            AppState.get().isCut = false;

            onCrop.underline(AppState.get().isCrop);
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
        if (AppState.get().isLocked) {
            lockModelImage.setImageResource(R.drawable.glyphicons_204_lock);
        } else {
            lockModelImage.setImageResource(R.drawable.glyphicons_205_unlock);
        }

    }

    Runnable reloadDoc = new Runnable() {

        @Override
        public void run() {
            onBC.underline(AppState.get().bolderTextOnImage || AppState.get().brigtnessImage != 0 || AppState.get().contrastImage != 0);
            dc.getOutline(null, false);
            dc.saveCurrentPage();
            createAdapter();

            loadUI();
        }
    };
    Runnable reloadDocBrigntness = new Runnable() {

        @Override
        public void run() {
            onBC.underline(AppState.get().bolderTextOnImage || AppState.get().brigtnessImage != 0 || AppState.get().contrastImage != 0);
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
        // Analytics.onStart(this);
        EventBus.getDefault().register(this);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

    }

    @Override
    public void onStop() {
        EventBus.getDefault().unregister(this);
        super.onStop();
        // Analytics.onStop(this);
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        if (flippingHandler != null) {
            flippingHandler.removeCallbacksAndMessages(null);
        }

        RecentUpates.updateAll(this);

    }

    Runnable closeRunnable = new Runnable() {

        @Override
        public void run() {
            LOG.d("Close App");
            if (dc != null) {
                dc.onCloseActivityAdnShowInterstial();
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
        nullAdapter();

        // AppState.get().isCut = false;
        PageImageState.get().clearResouces();

    }

    public void nullAdapter() {
        if (viewPager != null) {
            try {
                ImageLoader.getInstance().clearAllTasks();
                closeDialogs();
                viewPager.setAdapter(null);
            } catch (Exception e) {
                LOG.e(e);
            }
        }
    }

    public void updateReadPercent() {
        if (dc != null) {
            double value = (dc.getCurentPage() + 0.0001) / dc.getPageCount();
            getIntent().putExtra(PERCENT_EXTRA, value);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        DocumentController.chooseFullScreen(this, AppState.get().isFullScreen);
        DocumentController.doRotation(this);

        if (clickUtils != null) {
            clickUtils.init();
        }

        if (dc != null) {
            dc.onResume();
        }

        if (dc != null && TTSEngine.get().isPlaying()) {
            dc.onGoToPage(AppState.get().lastBookPage + 1);
        }

        handler.removeCallbacks(closeRunnable);
        handlerTimer.post(updateTimePower);
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (dc != null) {
            dc.saveCurrentPage();
        }
        AppState.get().save(this);
        TempHolder.isSeaching = false;

        handler.postDelayed(closeRunnable, AppState.APP_CLOSE_AUTOMATIC);
        handlerTimer.removeCallbacks(updateTimePower);

    }

    public void nextPage() {
        flippingTimer = 0;
        viewPager.setCurrentItem(dc.getCurentPage() + 1, AppState.get().isScrollAnimation);
        dc.checkReadingTimer();
    }

    public void prevPage() {
        flippingTimer = 0;
        viewPager.setCurrentItem(dc.getCurentPage() - 1, AppState.get().isScrollAnimation);
        dc.checkReadingTimer();
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    @Subscribe
    public void onEvent(MessageEvent ev) {
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
            if (dc.isTextFormat() && TxtUtils.isFooterNote(AppState.get().selectedText)) {
                showFootNotes = DragingDialogs.showFootNotes(anchor, dc, new Runnable() {

                    @Override
                    public void run() {
                        showHideHistory();
                    }
                });
            } else {

                if (AppState.get().isRememberDictionary) {
                    DictsHelper.runIntent(anchor.getContext(), AppState.get().selectedText);
                } else {
                    DragingDialogs.selectTextMenu(anchor, dc, true, onRefresh);
                }
            }
        } else if (ev.getMessage().equals(MessageEvent.MESSAGE_GOTO_PAGE)) {
            if (ev.getPage() == -1 && TxtUtils.isNotEmpty(ev.getBody())) {
                AlertDialogs.openUrl(this, ev.getBody());
            } else {
                dc.getLinkHistory().add(dc.getCurentPage() + 1);
                dc.onGoToPage(ev.getPage() + 1);
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
        dc.init(this);
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
        if (dc == null) {
            return;
        }

        if (page <= viewPager.getAdapter().getCount() - 1) {
            viewPager.setCurrentItem(page, false);
        }

        int max = dc.getPageCount();
        String textPage = TxtUtils.deltaPage(page + 1, max);
        String textMax = TxtUtils.deltaPageMax(max);
        if (AppState.get().isRTL) {
            maxSeek.setText("" + textPage);
            currentSeek.setText(textMax);
        } else {
            maxSeek.setText("" + textMax);
            currentSeek.setText("" + textPage);
        }
        pagesCountIndicator.setText(textPage + " ∕ " + textMax);

        seekBar.setProgress(page);
        if (dc != null) {
            dc.currentPage = page;
        }

        pagesTime.setText(UiSystemUtils.getSystemTime(this));

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

        onTTSStatus(null);
        LOG.d("_PAGE", "Update UI", page);
    }

    public void loadUI() {
        titleTxt.setText(dc.getTitle());
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
                nextPage();
            }
        });

        // currentSeek.setText("" + (documentController.getCurentPage() + 1));
        // maxSeek.setText("" + TxtUtils.deltaPage(documentController.getPageCount()));
        //
        // if (AppState.get().isRTL) {
        // maxSeek.setText("" + TxtUtils.deltaPage((documentController.getCurentPage() +
        // 1)));
        // currentSeek.setText("" + documentController.getPageCount());
        // }

        updateLockMode();

        tinUI();

        onViewPagerChangeListener.onPageSelected(dc.getCurentPage());

        progressDraw.updatePageCount(dc.getPageCount());
        dc.getOutline(new ResultResponse<List<OutlineLinkWrapper>>() {

            @Override
            public boolean onResultRecive(List<OutlineLinkWrapper> result) {
                progressDraw.updateDivs(result);
                if (TxtUtils.isListEmpty(result)) {
                    TintUtil.setTintImageWithAlpha(outline, Color.LTGRAY);
                }
                return false;
            }
        }, false);

        showHelp();

    }

    public void showHelp() {
        if (AppState.get().isFirstTimeHorizontal) {
            handler.postDelayed(new Runnable() {

                @Override
                public void run() {
                    AppState.get().isFirstTimeHorizontal = false;
                    AppState.get().isEditMode = true;
                    hideShow();
                    Views.showHelpToast(lockModelImage);

                }
            }, 1000);
        }

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

            handler.postDelayed(new Runnable() {

                @Override
                public void run() {
                    dc.saveCurrentPage();
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

        final boolean currentPosistion = Dips.screenHeight() > Dips.screenWidth();
        if (ExtUtils.isTextFomat(getIntent()) /* && isInitOrientation == AppState.get().orientation */) {

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
            dc.udpateImageSize(viewPager.getWidth(), viewPager.getHeight());
            onRotateScreen();
        }

        isInitOrientation = AppState.get().orientation;

    }

    public void authoFit() {
        handler.postDelayed(new Runnable() {

            @Override
            public void run() {
                PageImageState.get().isAutoFit = true;
                dc.cleanImageMatrix();
                EventBus.getDefault().post(new MessageAutoFit(viewPager.getCurrentItem()));
            }
        }, 50);
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public void onRotateScreen() {
        // ADS.activate(this, adView);
        activateAds();

        AppState.get().save(this);
        if (ExtUtils.isTextFomat(getIntent())) {
            updateReadPercent();
            nullAdapter();
            dc.restartActivity();
        } else {
            if (viewPager != null) {
                authoFit();
            }
        }
    }

    UpdatableFragmentPagerAdapter pagerAdapter;

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

    public boolean prev = true;

    public void hideShow() {
        hideShow(true);
    }

    public void hideShow(boolean animated) {
        if (prev == AppState.get().isEditMode) {
            return;
        }
        prev = AppState.get().isEditMode;

        if (!animated || AppState.get().isInkMode) {
            actionBar.setVisibility(AppState.get().isEditMode ? View.VISIBLE : View.GONE);
            bottomBar.setVisibility(AppState.get().isEditMode ? View.VISIBLE : View.GONE);
            adFrame.setVisibility(AppState.get().isEditMode ? View.VISIBLE : View.GONE);

            DocumentController.chooseFullScreen(this, AppState.get().isFullScreen);
            return;
        }

        ((RelativeLayout.LayoutParams) adFrame.getLayoutParams()).topMargin = actionBar.getHeight() + Dips.dpToPx(10);

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

        if (pagerAdapter != null) {
            DocumentController.chooseFullScreen(this, AppState.get().isFullScreen);
            pagerAdapter.notifyDataSetChanged();
        }
    }

    private boolean isMyKey = false;

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
                DragingDialogs.thumbnailDialog(anchor, dc);
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

        isMyKey = false;

        if (AppState.get().isUseVolumeKeys) {

            int repeatCount = event.getRepeatCount();
            if (repeatCount >= 1 && repeatCount < 6) {
                return true;
            }
            LOG.d("onKeyDown", keyCode, repeatCount);

            if (AppState.get().isUseVolumeKeys && KeyEvent.KEYCODE_HEADSETHOOK == keyCode) {
                if (TTSEngine.get().isPlaying()) {
                    TTSEngine.get().stop();
                } else {
                    TTSEngine.get().playCurrent();
                    anchor.setTag("");
                }
                DragingDialogs.textToSpeachDialog(anchor, dc);
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

        if (anchor != null && anchor.getChildCount() > 0 && anchor.getVisibility() == View.VISIBLE) {
            dc.clearSelectedText();
            closeDialogs();
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
        AppState.get().lastClosedActivity = null;
        if (handler != null) {
            handler.removeCallbacksAndMessages(null);
        }
        nullAdapter();
        dc.onCloseActivityFinal(null);
    }

    private void updateAnimation(final TranslateAnimation a) {
        a.setDuration(250);
    }
}
