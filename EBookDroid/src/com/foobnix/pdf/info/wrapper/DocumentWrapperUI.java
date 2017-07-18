/**
 * 
 */
package com.foobnix.pdf.info.wrapper;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import org.ebookdroid.common.settings.SettingsManager;
import org.ebookdroid.common.settings.books.BookSettings;

import com.foobnix.android.utils.Dips;
import com.foobnix.android.utils.Keyboards;
import com.foobnix.android.utils.LOG;
import com.foobnix.android.utils.TxtUtils;
import com.foobnix.pdf.info.ADS;
import com.foobnix.pdf.info.AppsConfig;
import com.foobnix.pdf.info.DictsHelper;
import com.foobnix.pdf.info.R;
import com.foobnix.pdf.info.TTSModule;
import com.foobnix.pdf.info.TintUtil;
import com.foobnix.pdf.info.UiSystemUtils;
import com.foobnix.pdf.info.model.BookCSS;
import com.foobnix.pdf.info.model.OutlineLinkWrapper;
import com.foobnix.pdf.info.view.BrigtnessDraw;
import com.foobnix.pdf.info.view.DragingDialogs;
import com.foobnix.pdf.info.view.DragingPopup;
import com.foobnix.pdf.info.view.DrawView;
import com.foobnix.pdf.info.view.ProgressDraw;
import com.foobnix.pdf.info.view.ProgressSeekTouchEventListener;
import com.foobnix.pdf.info.widget.ShareDialog;
import com.foobnix.pdf.search.view.CloseAppDialog;
import com.foobnix.ui2.MainTabs2;
import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.InterstitialAd;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.os.Build;
import android.os.Handler;
import android.support.v4.graphics.ColorUtils;
import android.view.GestureDetector;
import android.view.KeyEvent;
import android.view.MenuItem;
import android.view.MenuItem.OnMenuItemClickListener;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewTreeObserver.OnGlobalLayoutListener;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.SeekBar;
import android.widget.TextView;

/**
 * @author iivanenko
 * 
 */
public class DocumentWrapperUI {

    private static final int TRANSPARENT_UI = 240;
    View menuLayout, moveLeft, moveRight, pages, lineNavgination;
    ImageView imageMenuArrow;
    View adFrame;
    SeekBar seekBar;
    private final DocumentController controller;
    private TextView currentPageIndex;
    private TextView currentSeek;
    private TextView maxSeek;
    private TextView currentTime;
    private TextView bookName;
    private Activity a;
    private String bookTitle;
    private DocumentGestureListener documentListener;
    private View onDocDontext;
    private ImageView toolBarButton;
    private View titleBar;
    private TextView nextTypeBootom;
    private DocumentGuestureDetector documentGestureDetector;
    private GestureDetector gestureDetector;
    private final AppConfig appConfig;
    private ImageView linkHistory;
    private ImageView lockUnlock;
    private ImageView lockUnlockTop, textToSpeachTop, clockIcon, batteryIcon;
    private ImageView showSearch;
    ProgressDraw progressDraw;
    private ImageView nextScreenType, crop, cut, autoScroll, textToSpeach;
    private SeekBar speedSeekBar;
    private View seekSpeedLayot, zoomPlus, zoomMinus;
    private FrameLayout anchor;
    private TextView batteryLevel;

    private ImageView editTop2, lirbiLogo;
    private DrawView drawView;
    private View line1, line2, lineFirst, lineClose, closeTop;
    private ImageView goToPage1, goToPage1Top;
    private TextView reverseKeysIndicator;
    private BrigtnessDraw brigtnessProgressView;

    private final Handler handler = new Handler();

    InterstitialAd mInterstitialAd;

    public DocumentWrapperUI(final AppConfig appConfig, final DocumentController controller) {
        AppState.getInstance().annotationDrawColor = "";
        AppState.getInstance().editWith = AppState.EDIT_NONE;

        this.appConfig = appConfig;
        this.controller = controller;
        controller.setUi(this);

        documentListener = new DocumentGestureListener() {

            @Override
            public void onDoubleTap() {
                doShowHideWrapperControlls();
            }

            @Override
            public void onNextPage() {
                nextChose(false);

            }

            @Override
            public void onPrevPage() {
                prevChose(false);
            }

            @Override
            public void onSingleTap() {
                // onSingleTap();
            }

        };

    }

    public void onSingleTap() {
        if (AppState.getInstance().isMusicianMode) {
            onAutoScrollClick();
        } else {
            doShowHideWrapperControlls();
        }
    }

    DragingPopup showFootNotes;

    public static boolean isCJK(int ch) {
        Character.UnicodeBlock block = Character.UnicodeBlock.of(ch);
        if (Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS.equals(block) || Character.UnicodeBlock.CJK_COMPATIBILITY_IDEOGRAPHS.equals(block) || Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_A.equals(block)) {
            return true;
        }
        return false;
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public void onLongPress(MotionEvent ev) {
        if (controller.isTextFormat() && TxtUtils.isFooterNote(AppState.get().selectedText)) {
            showFootNotes = DragingDialogs.showFootNotes(anchor, controller, new Runnable() {

                @Override
                public void run() {
                    showHideHistory();
                }
            });
        } else {
            if (AppState.get().isRememberDictionary) {
                DictsHelper.runIntent(anchor.getContext(), AppState.get().selectedText);
            } else {
                DragingDialogs.selectTextMenu(anchor, controller, true);
            }
        }
    }

    public void showSelectTextMenu() {
        DragingDialogs.selectTextMenu(anchor, controller, true);
    }

    public boolean checkBack(final KeyEvent event) {
        int keyCode = event.getKeyCode();
        if (keyCode == 0) {
            keyCode = event.getScanCode();
        }

        if (anchor == null) {
            closeAndRunList(false);
            return true;
        }
        if (AppState.get().isAutoScroll) {
            AppState.get().isAutoScroll = false;
            updateUI();
            return true;
        }

        if (KeyEvent.KEYCODE_BACK == keyCode) {
            if (anchor.getChildCount() > 0 && anchor.getVisibility() == View.VISIBLE) {
                anchor.setTag("backSpace");
                anchor.removeAllViews();
                anchor.setVisibility(View.GONE);
                if (prefDialog != null) {
                    prefDialog.closeDialog();
                }
            } else if (!controller.getLinkHistory().isEmpty()) {
                controller.onLinkHistory();
            } else {
                closeAndRunList(false);
            }
        }
        return true;
    }

    public boolean dispatchKeyEventUp(final KeyEvent event) {
        int keyCode = event.getKeyCode();
        if (keyCode == 0) {
            keyCode = event.getScanCode();
        }

        if (KeyEvent.KEYCODE_MENU == keyCode || KeyEvent.KEYCODE_M == keyCode) {
            doShowHideWrapperControlls();
            return true;
        }

        return false;

    }

    public boolean dispatchKeyEventDown(final KeyEvent event) {
        int keyCode = event.getKeyCode();
        if (keyCode == 0) {
            keyCode = event.getScanCode();
        }

        if (keyCode >= KeyEvent.KEYCODE_1 && keyCode <= KeyEvent.KEYCODE_9) {
            controller.onGoToPage(keyCode - KeyEvent.KEYCODE_1 + 1);
            return true;
        }
        if (keyCode == KeyEvent.KEYCODE_0) {
            controller.toPageDialog();
            return true;
        }

        if (KeyEvent.KEYCODE_F == keyCode) {
            controller.alignDocument();
            return true;
        }

        if (KeyEvent.KEYCODE_S == keyCode || KeyEvent.KEYCODE_SEARCH == keyCode) {
            showSearchDialog();
            return true;
        }

        if (KeyEvent.KEYCODE_A == keyCode || KeyEvent.KEYCODE_SPACE == keyCode) {
            onAutoScrollClick();
            return true;
        }

        if (AppState.get().isUseVolumeKeys && AppState.getInstance().isAutoScroll && KeyEvent.KEYCODE_VOLUME_UP == keyCode) {
            if (AppState.getInstance().autoScrollSpeed > 1) {
                AppState.getInstance().autoScrollSpeed -= 1;
                controller.onAutoScroll();
                updateUI();
            }
            return true;
        }
        if (AppState.get().isUseVolumeKeys && AppState.getInstance().isAutoScroll && KeyEvent.KEYCODE_VOLUME_DOWN == keyCode) {
            if (AppState.getInstance().autoScrollSpeed <= AppState.MAX_SPEED) {
                AppState.getInstance().autoScrollSpeed += 1;
            }
            controller.onAutoScroll();
            updateUI();
            return true;
        }

        if (AppState.get().isUseVolumeKeys && AppState.getInstance().getNextKeys().contains(keyCode)) {
            nextChose(false);
            return true;
        }
        if (AppState.get().isUseVolumeKeys && AppState.getInstance().getPrevKeys().contains(keyCode)) {
            prevChose(false);
            return true;
        }

        if (AppState.get().isUseVolumeKeys && KeyEvent.KEYCODE_HEADSETHOOK == keyCode) {
            if (TTSModule.getInstance().isPlaying()) {
                TTSModule.getInstance().stop();
            } else {
                TTSModule.getInstance().play();
                anchor.setTag("");
            }
            DragingDialogs.textToSpeachDialog(anchor, controller);
            return true;
        }

        if (keyCode == KeyEvent.KEYCODE_DPAD_DOWN) {
            controller.onScrollDown();
            return true;
        }
        if (keyCode == KeyEvent.KEYCODE_DPAD_UP) {
            controller.onScrollUp();
            return true;
        }

        if (keyCode == 70) {
            controller.onZoomInc();
            return true;
        }

        if (keyCode == 69) {
            controller.onZoomDec();
            return true;
        }

        return false;

    }

    public void closeAndRunList(final boolean isLong) {
        AppState.get().lastA = null;
        if (handler != null) {
            handler.removeCallbacksAndMessages(null);
        }

        if (mInterstitialAd != null && mInterstitialAd.isLoaded()) {
            mInterstitialAd.show();
        } else {
            if (titleBar != null) {
                titleBar.removeCallbacks(null);
            }
            controller.onCloseActivity();

            if (isLong && !MainTabs2.isInStack) {
                MainTabs2.startActivity(a, UITab.getCurrentTabIndex(UITab.SearchFragment2));
            }
        }
    }

    private void closeAndRunList1(final boolean isLong) {
        if (!MainTabs2.isInStack) {
            AlertDialog.Builder dialog = new AlertDialog.Builder(a);
            CharSequence items[] = new CharSequence[] { "Return to PDF", "Return to app" };
            dialog.setItems(items, new DialogInterface.OnClickListener() {

                @Override
                public void onClick(DialogInterface dialog, int which) {
                    if (which == 0) {
                        titleBar.removeCallbacks(null);
                        controller.onCloseActivity();

                        final Intent intent = new Intent(a, MainTabs2.class);
                        a.startActivity(intent);
                        a.overridePendingTransition(0, 0);
                    }
                    if (which == 1) {
                        titleBar.removeCallbacks(null);
                        controller.onCloseActivity();
                    }

                }
            });
            dialog.show();

        } else {

            titleBar.removeCallbacks(null);
            controller.onCloseActivity();
        }
    }

    public void updateSpeedLabel() {
        final int max = controller.getPageCount();
        final int current = controller.getCurentPage();

        if (AppState.getInstance().isAutoScroll) {
            currentPageIndex.setText(String.format("{%s} %s/%s", AppState.getInstance().autoScrollSpeed, current, max));
        } else {
            currentPageIndex.setText(String.format("%s/%s", current, max));
        }
    }

    public void updateUI() {
        final int max = controller.getPageCount();
        final int current = controller.getCurentPage();

        updateSpeedLabel();
        currentSeek.setText(String.valueOf(current));
        maxSeek.setText(String.valueOf(max));

        seekBar.setOnSeekBarChangeListener(null);
        seekBar.setMax(max);
        seekBar.setProgress(current);
        seekBar.setOnSeekBarChangeListener(onSeek);

        speedSeekBar.setOnSeekBarChangeListener(null);
        speedSeekBar.setMax(AppState.MAX_SPEED);
        speedSeekBar.setProgress(AppState.getInstance().autoScrollSpeed);
        speedSeekBar.setOnSeekBarChangeListener(onSpeed);

        // time
        currentTime.setText(UiSystemUtils.getSystemTime(a));

        final int myLevel = UiSystemUtils.getPowerLevel(a);
        batteryLevel.setText(myLevel + "%");
        if (myLevel == -1) {
            batteryLevel.setVisibility(View.GONE);
        }

        showChapter();

        showHide();
        initNextType();
        initToolBarPlusMinus();

        showHideHistory();
        updateLock();

        reverseKeysIndicator.setVisibility(AppState.get().isReverseKeys ? View.VISIBLE : View.GONE);
        if (true || AppState.get().isMusicianMode) {
            reverseKeysIndicator.setVisibility(View.GONE);
        }

        moveLeft.setVisibility(Dips.screenWidth() < Dips.dpToPx(480) ? View.GONE : View.VISIBLE);
        moveRight.setVisibility(Dips.screenWidth() < Dips.dpToPx(480) ? View.GONE : View.VISIBLE);
        if (controller.isTextFormat()) {
            moveLeft.setVisibility(View.GONE);
            moveRight.setVisibility(View.GONE);
            zoomPlus.setVisibility(View.GONE);
            zoomMinus.setVisibility(View.GONE);
            crop.setVisibility(View.GONE);
            cut.setVisibility(View.GONE);
        }

        if (AppState.get().isCrop) {
            TintUtil.setTintImage(crop, Color.LTGRAY);
        } else {
            TintUtil.setTintImage(crop, Color.WHITE);
        }

        if (AppState.get().isCut) {
            TintUtil.setTintImage(cut, Color.LTGRAY);
        } else {
            TintUtil.setTintImage(cut, Color.WHITE);
        }

        progressDraw.updateProgress(current);
    }

    public void showChapter() {
        if (TxtUtils.isNotEmpty(controller.getCurrentChapter())) {
            bookName.setText(bookTitle + " – " + controller.getCurrentChapter().trim());
        } else {
            bookName.setText(bookTitle);

        }
    }

    public void updateLock() {
        // int mode = View.VISIBLE;

        if (AppState.getInstance().isLocked) {
            lockUnlock.setImageResource(R.drawable.glyphicons_204_lock);
            lockUnlockTop.setImageResource(R.drawable.glyphicons_204_lock);
            // lockUnlock.setColorFilter(a.getResources().getColor(R.color.tint_yellow));
            // lockUnlockTop.setColorFilter(a.getResources().getColor(R.color.tint_yellow));
            // mode = View.VISIBLE;
        } else {
            lockUnlock.setImageResource(R.drawable.glyphicons_205_unlock);
            lockUnlockTop.setImageResource(R.drawable.glyphicons_205_unlock);
            // lockUnlock.setColorFilter(a.getResources().getColor(R.color.tint_white));
            // lockUnlockTop.setColorFilter(a.getResources().getColor(R.color.tint_white));
            // mode = View.GONE;
        }

    }

    public void showHideHistory() {
        linkHistory.setVisibility(controller.getLinkHistory().isEmpty() ? View.GONE : View.VISIBLE);
    }

    public void initUI(final Activity a) {
        this.a = a;

        // AppState.get().isMusicianMode &&
        if (!AppsConfig.checkIsProInstalled(a) && AppsConfig.ADMOB_FULLSCREEN != null) {
            handler.postDelayed(new Runnable() {

                @Override
                public void run() {
                    mInterstitialAd = new InterstitialAd(a);
                    mInterstitialAd.setAdUnitId(AppsConfig.ADMOB_FULLSCREEN);
                    mInterstitialAd.setAdListener(new AdListener() {
                        @Override
                        public void onAdClosed() {
                            closeAndRunList(false);
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

        linkHistory = (ImageView) a.findViewById(R.id.linkHistory);
        linkHistory.setOnClickListener(onLinkHistory);

        menuLayout = a.findViewById(R.id.menuLayout);

        pages = a.findViewById(R.id.pagsLayout);
        lineNavgination = a.findViewById(R.id.lineNavgination);
        imageMenuArrow = (ImageView) a.findViewById(R.id.imageMenuArrow);
        adFrame = a.findViewById(R.id.adFrame);

        seekBar = (SeekBar) a.findViewById(R.id.seekBar);
        speedSeekBar = (SeekBar) a.findViewById(R.id.seekBarSpeed);
        seekSpeedLayot = a.findViewById(R.id.seekSpeedLayot);
        anchor = (FrameLayout) a.findViewById(R.id.anchor);

        titleBar = a.findViewById(R.id.titleBar);
        titleBar.setOnClickListener(onMenu);

        brigtnessProgressView = (BrigtnessDraw) a.findViewById(R.id.brigtnessProgressView);
        brigtnessProgressView.setActivity(a);

        reverseKeysIndicator = (TextView) a.findViewById(R.id.reverseKeysIndicator);
        // reverseKeysIndicator.setOnClickListener(onReverseKeys);
        reverseKeysIndicator.setTypeface(BookCSS.getNormalTypeFace());

        zoomPlus = a.findViewById(R.id.zoomPlus);
        zoomPlus.setOnClickListener(onPlus);

        zoomMinus = a.findViewById(R.id.zoomMinus);
        zoomMinus.setOnClickListener(onMinus);

        line1 = a.findViewById(R.id.line1);
        line1.setOnClickListener(onPrevPage);

        line2 = a.findViewById(R.id.line2);
        line2.setOnClickListener(onNextPage);

        lineClose = a.findViewById(R.id.lineClose);
        lineClose.setOnClickListener(onClose);

        closeTop = a.findViewById(R.id.closeTop);
        closeTop.setOnClickListener(onClose);
        closeTop.setOnLongClickListener(onCloseLongClick);

        lineFirst = a.findViewById(R.id.lineFirst);
        lineFirst.setOnClickListener(onGoToPAge1);

        lirbiLogo = (ImageView) a.findViewById(R.id.lirbiLogo);
        // lirbiLogo.setText("Lirbi " + a.getString(R.string.musician));
        lirbiLogo.setOnClickListener(onLirbiLogoClick);

        editTop2 = (ImageView) a.findViewById(R.id.editTop2);
        editTop2.setOnClickListener(onShowHideEditPanel);

        goToPage1 = (ImageView) a.findViewById(R.id.goToPage1);
        goToPage1Top = (ImageView) a.findViewById(R.id.goToPage1Top);
        goToPage1.setOnClickListener(onGoToPAge1);
        goToPage1Top.setOnClickListener(onGoToPAge1);

        toolBarButton = (ImageView) a.findViewById(R.id.imageToolbar);
        toolBarButton.setOnClickListener(onHideShowToolBar);

        // nextPage.setOnClickListener(onNextPage);
        // prevPage.setOnClickListener(onPrevPage);

        moveLeft = a.findViewById(R.id.moveLeft);
        moveLeft.setOnClickListener(onMoveLeft);

        View moveCenter = a.findViewById(R.id.moveCenter);
        moveCenter.setOnClickListener(onMoveCenter);

        moveRight = a.findViewById(R.id.moveRight);
        moveRight.setOnClickListener(onMoveRight);

        ImageView brightness = (ImageView) a.findViewById(R.id.brightness);
        brightness.setOnClickListener(onSun);
        brightness.setImageResource(!AppState.get().isInvert ? R.drawable.glyphicons_232_sun : R.drawable.glyphicons_2_moon);

        a.findViewById(R.id.toPage).setOnClickListener(toPage);

        crop = (ImageView) a.findViewById(R.id.crop);
        crop.setOnClickListener(onCrop);

        if (AppState.get().isCut) {
            crop.setVisibility(View.GONE);
        }

        cut = (ImageView) a.findViewById(R.id.cut);
        cut.setOnClickListener(onCut);

        View prefTop = a.findViewById(R.id.prefTop);
        prefTop.setOnClickListener(onPrefTop);

        View fullscreen = a.findViewById(R.id.fullscreen);
        fullscreen.setOnClickListener(onFull);

        View close = a.findViewById(R.id.close);
        close.setOnClickListener(onClose);
        close.setOnLongClickListener(onCloseLongClick);

        showSearch = (ImageView) a.findViewById(R.id.onShowSearch);
        showSearch.setOnClickListener(onShowSearch);
        autoScroll = ((ImageView) a.findViewById(R.id.autoScroll));
        autoScroll.setOnClickListener(onAutoScroll);

        // ((View)
        // a.findViewById(R.id.onScreenMode)).setOnClickListener(onScreenMode);

        nextTypeBootom = (TextView) a.findViewById(R.id.nextTypeBootom);

        nextTypeBootom.setOnClickListener(onNextType);

        nextScreenType = ((ImageView) a.findViewById(R.id.imageNextScreen));
        nextScreenType.setOnClickListener(onNextType);

        onDocDontext = a.findViewById(R.id.onDocDontext);
        onDocDontext.setOnClickListener(onShowContext);

        lockUnlock = (ImageView) a.findViewById(R.id.lockUnlock);
        lockUnlockTop = (ImageView) a.findViewById(R.id.lockUnlockTop);
        lockUnlock.setOnClickListener(onLockUnlock);
        lockUnlockTop.setOnClickListener(onLockUnlock);

        textToSpeachTop = (ImageView) a.findViewById(R.id.textToSpeachTop);
        textToSpeachTop.setOnClickListener(onTextToSpeach);
        textToSpeachTop.setVisibility(TTSModule.isAvailableTTS() ? View.VISIBLE : View.GONE);

        batteryIcon = (ImageView) a.findViewById(R.id.batteryIcon);
        clockIcon = (ImageView) a.findViewById(R.id.clockIcon);

        textToSpeach = (ImageView) a.findViewById(R.id.textToSpeach);
        textToSpeach.setOnClickListener(onTextToSpeach);
        textToSpeach.setVisibility(TTSModule.isAvailableTTS() ? View.VISIBLE : View.GONE);

        drawView = (DrawView) a.findViewById(R.id.drawView);

        View bookmarks = a.findViewById(R.id.onBookmarks);
        bookmarks.setOnClickListener(onBookmarks);
        bookmarks.setOnLongClickListener(onBookmarksLong);

        currentPageIndex = (TextView) a.findViewById(R.id.currentPageIndex);
        currentSeek = (TextView) a.findViewById(R.id.currentSeek);
        maxSeek = (TextView) a.findViewById(R.id.maxSeek);
        bookName = (TextView) a.findViewById(R.id.bookName);

        currentTime = (TextView) a.findViewById(R.id.currentTime);
        batteryLevel = (TextView) a.findViewById(R.id.currentBattery);

        bookName.setTypeface(BookCSS.getNormalTypeFace());
        currentSeek.setTypeface(BookCSS.getNormalTypeFace());
        currentPageIndex.setTypeface(BookCSS.getNormalTypeFace());
        maxSeek.setTypeface(BookCSS.getNormalTypeFace());
        currentTime.setTypeface(BookCSS.getNormalTypeFace());
        batteryLevel.setTypeface(BookCSS.getNormalTypeFace());

        View thumbnail = a.findViewById(R.id.thumbnail);
        thumbnail.setOnClickListener(onThumbnail);

        View itemMenu = a.findViewById(R.id.itemMenu);
        itemMenu.setOnClickListener(onItemMenu);

        progressDraw = (ProgressDraw) a.findViewById(R.id.progressDraw);

        if (!appConfig.isContentEnable()) {
            onDocDontext.setVisibility(View.GONE);
        }
        if (!appConfig.isSearchEnable()) {
            a.findViewById(R.id.onShowSearch).setVisibility(View.GONE);
        }
        if (!appConfig.isDayNightModeEnable()) {
            a.findViewById(R.id.brightness).setVisibility(View.GONE);
        }
        AppState.getInstance().isAutoScroll = false;

        ImageView recent = (ImageView) a.findViewById(R.id.onRecent);
        recent.setOnClickListener(onRecent);

        anchor.getViewTreeObserver().addOnGlobalLayoutListener(new OnGlobalLayoutListener() {

            @TargetApi(Build.VERSION_CODES.HONEYCOMB)
            @Override
            public void onGlobalLayout() {
                if (anchor.getVisibility() == View.VISIBLE || AppState.get().isMusicianMode) {
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

                if (anchor.getX() < 0) {
                    anchor.setX(0);
                }
                if (anchor.getY() < 0) {
                    anchor.setY(0);
                }
            }

        });
        updateSeekBarColorAndSize();

        // bottom 1
        TintUtil.setStatusBarColor(a);


        TintUtil.setTintBgSimple(a.findViewById(R.id.menuLayout), TRANSPARENT_UI);
        TintUtil.setTintBgSimple(a.findViewById(R.id.document_footer), TRANSPARENT_UI);
        TintUtil.setBackgroundFillColorBottomRight(lirbiLogo, ColorUtils.setAlphaComponent(TintUtil.color, TRANSPARENT_UI));
        tintSpeed();

        line1.setVisibility(View.GONE);
        line2.setVisibility(View.GONE);
        lineFirst.setVisibility(View.GONE);
        lineClose.setVisibility(View.GONE);
        goToPage1.setVisibility(View.GONE);
        goToPage1Top.setVisibility(View.GONE);
        closeTop.setVisibility(View.GONE);
        textToSpeachTop.setVisibility(TTSModule.isAvailableTTS() ? View.VISIBLE : View.GONE);

        if (AppState.get().isMusicianMode) {
            AppState.get().isEditMode = false;
            line1.setVisibility(View.VISIBLE);
            line2.setVisibility(View.VISIBLE);
            lineFirst.setVisibility(View.VISIBLE);
            lineClose.setVisibility(View.VISIBLE);

            goToPage1.setVisibility(View.VISIBLE);
            goToPage1Top.setVisibility(View.VISIBLE);
            closeTop.setVisibility(View.VISIBLE);

            reverseKeysIndicator.setVisibility(View.GONE);
            textToSpeachTop.setVisibility(View.GONE);
            progressDraw.setVisibility(View.GONE);
            brigtnessProgressView.setVisibility(View.GONE);
        }

    }

    public void updateSeekBarColorAndSize() {



        TintUtil.setTintText(bookName, TintUtil.getStatusBarColor());
        TintUtil.setTintImage(textToSpeachTop, TintUtil.getStatusBarColor());
        TintUtil.setTintImage(lockUnlockTop, TintUtil.getStatusBarColor());
        TintUtil.setTintImage(nextScreenType, TintUtil.getStatusBarColor());
        TintUtil.setTintText(currentPageIndex, TintUtil.getStatusBarColor());
        TintUtil.setTintText(currentTime, TintUtil.getStatusBarColor());
        TintUtil.setTintText(batteryLevel, TintUtil.getStatusBarColor());
        TintUtil.setTintText(reverseKeysIndicator, ColorUtils.setAlphaComponent(TintUtil.getStatusBarColor(), 200));

        TintUtil.setTintImage(goToPage1Top, TintUtil.getStatusBarColor());
        TintUtil.setTintImage((ImageView) closeTop, TintUtil.getStatusBarColor());

        TintUtil.setTintImage(toolBarButton, TintUtil.getStatusBarColor());
        TintUtil.setTintImage(clockIcon, TintUtil.getStatusBarColor()).setAlpha(200);
        TintUtil.setTintImage(batteryIcon, TintUtil.getStatusBarColor()).setAlpha(200);

        int titleColor = AppState.get().isInvert ? MagicHelper.otherColor(AppState.get().colorDayBg, -0.05f) : MagicHelper.otherColor(AppState.get().colorNigthBg, 0.05f);
        titleBar.setBackgroundColor(titleColor);

        int progressColor = AppState.get().isInvert ? MagicHelper.otherColor(AppState.get().statusBarColorDay, -0.2f) : MagicHelper.otherColor(AppState.get().statusBarColorNight, +0.2f);
        progressDraw.updateColor(progressColor);
        progressDraw.getLayoutParams().height = Dips.dpToPx(AppState.get().progressLineHeight);
        progressDraw.requestLayout();

        // textSize
        bookName.setTextSize(AppState.get().statusBarTextSizeAdv);
        currentPageIndex.setTextSize(AppState.get().statusBarTextSizeAdv);
        currentTime.setTextSize(AppState.get().statusBarTextSizeAdv);
        batteryLevel.setTextSize(AppState.get().statusBarTextSizeAdv);
        reverseKeysIndicator.setTextSize(AppState.get().statusBarTextSizeAdv);

        int iconSize = Dips.spToPx(AppState.get().statusBarTextSizeAdv);
        int smallIconSize = iconSize - Dips.dpToPx(5);
        int panelSize = (int) (iconSize * 1.5);

        textToSpeachTop.getLayoutParams().height = textToSpeachTop.getLayoutParams().width = iconSize;
        lockUnlockTop.getLayoutParams().height = lockUnlockTop.getLayoutParams().width = iconSize;
        nextScreenType.getLayoutParams().height = nextScreenType.getLayoutParams().width = iconSize;
        goToPage1Top.getLayoutParams().height = goToPage1Top.getLayoutParams().width = iconSize;
        closeTop.getLayoutParams().height = closeTop.getLayoutParams().width = iconSize;
        toolBarButton.getLayoutParams().height = toolBarButton.getLayoutParams().width = iconSize;

        clockIcon.getLayoutParams().height = clockIcon.getLayoutParams().width = smallIconSize;
        batteryIcon.getLayoutParams().height = batteryIcon.getLayoutParams().width = smallIconSize;

        lirbiLogo.getLayoutParams().height = panelSize;

    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    public void tintSpeed() {
        if (Build.VERSION.SDK_INT >= 16) {
            // speedSeekBar.getProgressDrawable().getCurrent().setColorFilter(TintUtil.color,
            // PorterDuff.Mode.SRC_IN);
            // speedSeekBar.getThumb().setColorFilter(TintUtil.color,
            // PorterDuff.Mode.SRC_IN);
        }
    }

    public void showEditDialogIfNeed() {
        DragingDialogs.editColorsPanel(anchor, controller, drawView, true);
    }

    View.OnClickListener onRecent = new View.OnClickListener() {

        @Override
        public void onClick(View v) {
            DragingDialogs.recentBooks(anchor, controller);
        }
    };

    View.OnClickListener onItemMenu = new View.OnClickListener() {

        @Override
        public void onClick(final View v) {
            ShareDialog.show(a, controller.getCurrentBook(), new Runnable() {

                @Override
                public void run() {
                    if (controller.getCurrentBook().delete()) {
                        controller.getActivity().finish();
                    }
                }
            }, controller.getCurentPage() - 1, DocumentWrapperUI.this);
        }
    };

    View.OnClickListener onLirbiLogoClick = new View.OnClickListener() {

        @Override
        public void onClick(final View v) {
            doShowHideWrapperControlls();
        }
    };

    View.OnClickListener onTextToSpeach = new View.OnClickListener() {

        @Override
        public void onClick(final View v) {
            DragingDialogs.textToSpeachDialog(anchor, controller);
        }
    };

    View.OnClickListener onThumbnail = new View.OnClickListener() {

        @Override
        public void onClick(final View v) {
            DragingDialogs.thumbnailDialog(anchor, controller);
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
            controller.onGoToPage(progress);
            updateUI();
        }
    };

    SeekBar.OnSeekBarChangeListener onSpeed = new SeekBar.OnSeekBarChangeListener() {

        @Override
        public void onStopTrackingTouch(final SeekBar seekBar) {
        }

        @Override
        public void onStartTrackingTouch(final SeekBar seekBar) {
        }

        @Override
        public void onProgressChanged(final SeekBar seekBar, final int progress, final boolean fromUser) {
            AppState.getInstance().autoScrollSpeed = progress + 1;
            updateSpeedLabel();

            // hideSeekBarInReadMode();
        }
    };

    public void doDoubleTap(int x, int y) {
        if (AppState.getInstance().isMusicianMode) {
            controller.alignDocument();
        } else {
            if (AppState.get().doubleClickAction == AppState.DOUBLE_CLICK_ZOOM_IN_OUT) {
                controller.onZoomInOut(x, y);
                AppState.get().isEditMode = false;
                showHide();
            } else if (AppState.get().doubleClickAction == AppState.DOUBLE_CLICK_ADJUST_PAGE) {
                controller.alignDocument();
            } else if (AppState.get().doubleClickAction == AppState.DOUBLE_CLICK_CENTER_HORIZONTAL) {
                controller.centerHorizontal();
            } else if (AppState.get().doubleClickAction == AppState.DOUBLE_CLICK_AUTOSCROLL) {
                onAutoScrollClick();
            }
        }
    }

    public void doShowHideWrapperControlls() {
        AppState.getInstance().isEditMode = !AppState.getInstance().isEditMode;
        showHide();

    }

    public void showHideHavigationBar() {
        if (!AppState.getInstance().isEditMode && AppState.getInstance().isFullScrean()) {
            Keyboards.hideNavigation(a);
        }
    }

    public void doChooseNextType(View view) {
        final PopupMenu popupMenu = new PopupMenu(view.getContext(), view);

        String pages = controller.getString(R.string.by_pages);
        String screen = controller.getString(R.string.of_screen).toLowerCase(Locale.US);
        String screens = controller.getString(R.string.by_screans);
        final List<Integer> values = Arrays.asList(AppState.NEXT_SCREEN_SCROLL_BY_PAGES, 100, 95, 75, 50, 25, 5);

        for (int i = 0; i < values.size(); i++) {
            final int n = i;
            String name = i == AppState.NEXT_SCREEN_SCROLL_BY_PAGES ? pages : values.get(i) + "% " + screen;
            if (values.get(i) == 100) {
                name = screens;
            }

            popupMenu.getMenu().add(name).setOnMenuItemClickListener(new OnMenuItemClickListener() {

                @Override
                public boolean onMenuItemClick(MenuItem item) {
                    AppState.get().nextScreenScrollBy = values.get(n);
                    initNextType();
                    Keyboards.hideNavigation(controller.getActivity());
                    return false;
                }
            });
        }
        popupMenu.show();

    }

    public void doHideShowToolBar() {
        AppState.getInstance().isShowToolBar = !AppState.getInstance().isShowToolBar;
        initToolBarPlusMinus();
    }

    public void initToolBarPlusMinus() {
        if (AppState.getInstance().isShowToolBar) {
            toolBarButton.setImageResource(R.drawable.glyphicons_336_pushpin);
        } else {
            toolBarButton.setImageResource(R.drawable.glyphicons_200_ban);
        }
        if (AppState.getInstance().isEditMode || AppState.getInstance().isShowToolBar) {
            titleBar.setVisibility(View.VISIBLE);
        } else {
            titleBar.setVisibility(View.GONE);
        }

        progressDraw.setVisibility(AppState.get().isShowReadingProgress ? View.VISIBLE : View.GONE);

        toolBarButton.setVisibility(View.VISIBLE);

    }

    public void initNextType() {
        if (AppState.get().nextScreenScrollBy == AppState.NEXT_SCREEN_SCROLL_BY_PAGES) {
            nextTypeBootom.setText(R.string.by_pages);
            nextScreenType.setImageResource(R.drawable.glyphicons_full_page);
            nextTypeBootom.setTypeface(BookCSS.getNormalTypeFace());
        } else {
            if (AppState.get().nextScreenScrollBy == 100) {
                nextTypeBootom.setText(controller.getString(R.string.by_screans));
            } else {
                nextTypeBootom.setText(AppState.get().nextScreenScrollBy + "% " + controller.getString(R.string.of_screen));
            }
            nextScreenType.setImageResource(R.drawable.glyphicons_halp_page);
            nextTypeBootom.setTypeface(BookCSS.getNormalTypeFace());
        }

    }

    public void showHide() {
        if (AppState.getInstance().isEditMode) {
            show();
        } else {
            hide();
        }
        initToolBarPlusMinus();

        if (AppState.getInstance().isAutoScroll) {
            autoScroll.setImageResource(R.drawable.glyphicons_175_pause);
        } else {
            autoScroll.setImageResource(R.drawable.glyphicons_174_play);
        }

        if (AppState.getInstance().isMusicianMode) {
            if (AppState.getInstance().isAutoScroll) {
                seekSpeedLayot.setVisibility(View.VISIBLE);
            } else {
                seekSpeedLayot.setVisibility(View.GONE);
            }
        } else {
            if (AppState.getInstance().isEditMode && AppState.getInstance().isAutoScroll) {
                seekSpeedLayot.setVisibility(View.VISIBLE);
            } else {
                seekSpeedLayot.setVisibility(View.GONE);
            }
        }

        if (AppState.getInstance().isMusicianMode) {
            lirbiLogo.setVisibility(View.VISIBLE);
        } else {
            lirbiLogo.setVisibility(View.GONE);
        }

        // hideSeekBarInReadMode();
        showHideHavigationBar();
    }

    public void hide() {
        menuLayout.setVisibility(View.GONE);
        pages.setVisibility(View.GONE);
        lineNavgination.setVisibility(View.GONE);
        adFrame.setVisibility(View.GONE);
        adFrame.setClickable(false);
        imageMenuArrow.setImageResource(android.R.drawable.arrow_down_float);

        // speedSeekBar.setVisibility(View.GONE);

    }

    public void _hideSeekBarInReadMode() {
        if (!AppState.getInstance().isEditMode) {
            handler.removeCallbacks(hideSeekBar);
            handler.postDelayed(hideSeekBar, 5000);
        }
    }

    Runnable hideSeekBar = new Runnable() {

        @Override
        public void run() {
            if (!AppState.getInstance().isMusicianMode) {
                seekSpeedLayot.setVisibility(View.GONE);
            }

        }
    };

    public void show() {
        menuLayout.setVisibility(View.VISIBLE);

        titleBar.setVisibility(View.VISIBLE);

        updateLock();

        pages.setVisibility(View.VISIBLE);
        lineNavgination.setVisibility(View.VISIBLE);
        adFrame.setVisibility(View.VISIBLE);
        adFrame.setClickable(true);

        imageMenuArrow.setImageResource(android.R.drawable.arrow_up_float);

        // if (AppState.getInstance().isAutoScroll &&
        // AppState.getInstance().isEditMode) {
        // seekSpeedLayot.setVisibility(View.VISIBLE);
        // }

    }

    public View.OnClickListener onShowSearch = new View.OnClickListener() {

        @Override
        public void onClick(final View arg0) {
            showSearchDialog();
        }

    };

    public void showSearchDialog() {
        DragingDialogs.searchMenu(anchor, controller);
    }

    public View.OnClickListener onAutoScroll = new View.OnClickListener() {

        @Override
        public void onClick(final View arg0) {
            onAutoScrollClick();
        }
    };

    public void onAutoScrollClick() {
        AppState.getInstance().isAutoScroll = !AppState.getInstance().isAutoScroll;
        // changeAutoScrollButton();
        controller.onAutoScroll();
        updateUI();
    }

    // public void changeAutoScrollButton() {
    // if (AppState.getInstance().isAutoScroll) {
    // autoScroll.setImageResource(android.R.drawable.ic_media_pause);
    // seekSpeedLayot.setVisibility(View.VISIBLE);
    // } else {
    // autoScroll.setImageResource(android.R.drawable.ic_media_play);
    // seekSpeedLayot.setVisibility(View.GONE);
    // }
    //
    // }

    public View.OnClickListener onLinkHistory = new View.OnClickListener() {

        @Override
        public void onClick(final View arg0) {
            controller.onLinkHistory();
            updateUI();
        }
    };

    public View.OnClickListener onShowContext = new View.OnClickListener() {

        @Override
        public void onClick(final View arg0) {
            DragingDialogs.showContent(anchor, controller);
        }
    };
    public View.OnClickListener onLockUnlock = new View.OnClickListener() {

        @Override
        public void onClick(final View arg0) {
            AppState.getInstance().isLocked = !AppState.getInstance().isLocked;
            updateLock();
        }
    };

    public View.OnClickListener onShowHideEditPanel = new View.OnClickListener() {

        @Override
        public void onClick(final View arg0) {
            DragingDialogs.editColorsPanel(anchor, controller, drawView, false);
        }
    };

    public View.OnClickListener onBookmarks = new View.OnClickListener() {

        @Override
        public void onClick(final View arg0) {
            DragingDialogs.addBookmarks(anchor, controller);
        }
    };
    public View.OnLongClickListener onBookmarksLong = new View.OnLongClickListener() {

        @Override
        public boolean onLongClick(final View arg0) {
            DragingDialogs.addBookmarksLong(anchor, controller);
            return true;
        }
    };

    public View.OnClickListener onHideShowToolBar = new View.OnClickListener() {

        @Override
        public void onClick(final View arg0) {
            LOG.d("DEBUG", "Click");
            doHideShowToolBar();
        }
    };

    View.OnClickListener onGoToPAge1 = new View.OnClickListener() {

        @Override
        public void onClick(View v) {
            controller.onScrollY(0);
            updateUI();
        }
    };

    public View.OnClickListener onNormalMode = new View.OnClickListener() {

        @Override
        public void onClick(final View arg0) {
            AppState.get().isMusicianMode = false;
            initUI(a);
            showHide();
        }
    };

    public View.OnClickListener onNextType = new View.OnClickListener() {

        @Override
        public void onClick(final View arg0) {
            LOG.d("DEBUG", "Click");
            doChooseNextType(arg0);
        }
    };

    public View.OnClickListener onMenu = new View.OnClickListener() {

        @Override
        public void onClick(final View arg0) {
            LOG.d("DEBUG", "Click");
            doShowHideWrapperControlls();
        }
    };

    public View.OnClickListener onReverseKeys = new View.OnClickListener() {

        @Override
        public void onClick(final View arg0) {
            LOG.d("DEBUG", "Click");
            AppState.get().isReverseKeys = !AppState.get().isReverseKeys;
            updateUI();
        }
    };

    public View.OnClickListener onFull = new View.OnClickListener() {

        @Override
        public void onClick(final View arg0) {
            AppState.getInstance().setFullScrean(!AppState.getInstance().isFullScrean());
            DocumentController.chooseFullScreen(a, AppState.getInstance().isFullScrean());
        }
    };
    public View.OnClickListener onScreenMode = new View.OnClickListener() {

        @Override
        public void onClick(final View arg0) {
            a.finish();
            a.startActivity(a.getIntent());
        }
    };

    public View.OnClickListener onSun = new View.OnClickListener() {

        @Override
        public void onClick(final View arg0) {
            controller.onNightMode();
        }
    };

    public View.OnClickListener toPage = new View.OnClickListener() {

        @Override
        public void onClick(final View arg0) {
            controller.toPageDialog();
        }
    };

    public View.OnClickListener onCrop = new View.OnClickListener() {

        @Override
        public void onClick(final View arg0) {
            AppState.get().isCrop = !AppState.get().isCrop;
            controller.onCrop();
            updateUI();

        }
    };
    public View.OnClickListener onCut = new View.OnClickListener() {

        @Override
        public void onClick(final View arg0) {
            AppState.get().isCrop = false;
            AppState.get().cutP = 50;
            AppState.get().isCut = !AppState.get().isCut;

            BookSettings bookSettings = SettingsManager.getBookSettings();
            if (bookSettings != null) {
                bookSettings.splitPages = AppState.get().isCut;
            }
            SettingsManager.storeBookSettings();

            crop.setVisibility(AppState.get().isCut ? View.GONE : View.VISIBLE);

            SettingsManager.toggleCropMode(true);

            controller.onCrop();
            controller.updateRendering();
            controller.alignDocument();

            updateUI();

        }
    };

    DragingPopup prefDialog;
    public View.OnClickListener onPrefTop = new View.OnClickListener() {

        @Override
        public void onClick(final View arg0) {
            prefDialog = DragingDialogs.preferences(anchor, controller, new Runnable() {

                @Override
                public void run() {
                    double value = (getController().getCurentPage() + 0.0001) / getController().getPageCount();
                    a.getIntent().putExtra("percent", value);
                    // titleBar.setBackgroundColor(MagicHelper.getBgColor());
                    initToolBarPlusMinus();
                    updateSeekBarColorAndSize();
                    showHide();
                }
            }, new Runnable() {

                @Override
                public void run() {
                    updateUI();

                }
            });
        }
    };

    public View.OnClickListener onClose = new View.OnClickListener() {

        @Override
        public void onClick(final View arg0) {
            closeAndRunList(false);
        }
    };
    public View.OnLongClickListener onCloseLongClick = new View.OnLongClickListener() {

        @Override
        public boolean onLongClick(final View v) {
            CloseAppDialog.showOnLongClickDialog(a, v, getController());
            return true;
        };
    };

    public View.OnClickListener onMoveLeft = new View.OnClickListener() {

        @Override
        public void onClick(final View arg0) {
            controller.onSrollLeft();
        }
    };

    public View.OnClickListener onMoveCenter = new View.OnClickListener() {

        @Override
        public void onClick(final View arg0) {
            controller.alignDocument();
        }
    };

    public View.OnClickListener onMoveRight = new View.OnClickListener() {

        @Override
        public void onClick(final View arg0) {
            controller.onSrollRight();
        }
    };

    public View.OnClickListener onNextPage = new View.OnClickListener() {

        @Override
        public void onClick(final View arg0) {
            nextChose(false);
        }
    };

    public void nextChose(final boolean animate) {
        controller.checkReadingTimer();

        if (controller.closeFooterNotesDialog()) {
            return;
        }

        if (AppState.get().nextScreenScrollBy == AppState.NEXT_SCREEN_SCROLL_BY_PAGES) {
            controller.onNextPage(animate);
        } else {
            controller.onNextScreen(animate);
        }
        if (AppState.get().isEditMode) {
            AppState.get().isEditMode = false;
        }
        updateUI();

    }

    public void prevChose(final boolean animate) {
        controller.checkReadingTimer();

        if (controller.closeFooterNotesDialog()) {
            return;
        }

        if (AppState.get().nextScreenScrollBy == AppState.NEXT_SCREEN_SCROLL_BY_PAGES) {
            controller.onPrevPage(animate);
        } else {
            controller.onPrevScreen(animate);
        }
        if (AppState.get().isEditMode) {
            AppState.get().isEditMode = false;
        }
        updateUI();
    }

    public View.OnClickListener onPrevPage = new View.OnClickListener() {

        @Override
        public void onClick(final View arg0) {
            prevChose(false);
        }
    };

    public View.OnClickListener onPlus = new View.OnClickListener() {

        @Override
        public void onClick(final View arg0) {
            controller.onZoomInc();
        }
    };
    public View.OnClickListener onMinus = new View.OnClickListener() {

        @Override
        public void onClick(final View arg0) {
            controller.onZoomDec();
        }
    };

    public void setTitle(final String title) {
        this.bookTitle = title;
        if (controller != null && controller.getCurrentBook() != null && !controller.getCurrentBook().getName().toLowerCase(Locale.US).endsWith(".pdf")) {
            editTop2.setVisibility(View.GONE);
        }

    }

    public void nextPage() {
        controller.onNextPage(false);
    }

    public void prevPage() {
        controller.onPrevPage(false);
    }

    public DocumentGestureListener getDocumentListener() {
        return documentListener;
    }

    public void setDocumentListener(final DocumentGestureListener wrapperListener) {
        this.documentListener = wrapperListener;
    }

    // public DocumentGuestureDetector getDocumentGestureDetector() {
    // return documentGestureDetector;
    // }

    public GestureDetector getGestureDetector() {
        return gestureDetector;
    }

    public void setGestureDetector(final GestureDetector gestureDetector) {
        this.gestureDetector = gestureDetector;
    }

    public DocumentController getController() {
        return controller;
    }

    public DrawView getDrawView() {
        return drawView;
    }

    public void showOutline(final List<OutlineLinkWrapper> list, final int count) {
        try {
            controller.activity.runOnUiThread(new Runnable() {

                @Override
                public void run() {
                    progressDraw.updateDivs(list);
                    progressDraw.updatePageCount(controller.getPageCount());
                    titleBar.setOnTouchListener(new ProgressSeekTouchEventListener(onSeek, controller.getPageCount(), false));
                    progressDraw.setOnTouchListener(new ProgressSeekTouchEventListener(onSeek, controller.getPageCount(), false));
                }
            });
        } catch (Exception e) {
            LOG.e(e);
        }

    }

}
