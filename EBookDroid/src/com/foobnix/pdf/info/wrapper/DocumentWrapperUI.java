/**
 * 
 */
package com.foobnix.pdf.info.wrapper;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import org.ebookdroid.common.settings.SettingsManager;
import org.ebookdroid.common.settings.books.BookSettings;
import org.ebookdroid.ui.viewer.VerticalViewActivity;
import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import com.foobnix.android.utils.Dips;
import com.foobnix.android.utils.Keyboards;
import com.foobnix.android.utils.LOG;
import com.foobnix.android.utils.TxtUtils;
import com.foobnix.android.utils.Vibro;
import com.foobnix.android.utils.Views;
import com.foobnix.pdf.info.DictsHelper;
import com.foobnix.pdf.info.ExtUtils;
import com.foobnix.pdf.info.R;
import com.foobnix.pdf.info.TintUtil;
import com.foobnix.pdf.info.UiSystemUtils;
import com.foobnix.pdf.info.model.BookCSS;
import com.foobnix.pdf.info.model.OutlineLinkWrapper;
import com.foobnix.pdf.info.view.BrightnessHelper;
import com.foobnix.pdf.info.view.Dialogs;
import com.foobnix.pdf.info.view.DragingDialogs;
import com.foobnix.pdf.info.view.DragingPopup;
import com.foobnix.pdf.info.view.DrawView;
import com.foobnix.pdf.info.view.HorizontallSeekTouchEventListener;
import com.foobnix.pdf.info.view.MyPopupMenu;
import com.foobnix.pdf.info.view.ProgressDraw;
import com.foobnix.pdf.info.view.UnderlineImageView;
import com.foobnix.pdf.info.widget.RecentUpates;
import com.foobnix.pdf.info.widget.ShareDialog;
import com.foobnix.pdf.search.activity.HorizontalModeController;
import com.foobnix.pdf.search.activity.msg.MessegeBrightness;
import com.foobnix.pdf.search.view.CloseAppDialog;
import com.foobnix.sys.TempHolder;
import com.foobnix.tts.MessagePageNumber;
import com.foobnix.tts.TTSEngine;
import com.foobnix.tts.TtsStatus;
import com.foobnix.ui2.AppDB;
import com.foobnix.ui2.MainTabs2;
import com.nostra13.universalimageloader.core.ImageLoader;

import android.annotation.TargetApi;
import android.app.Activity;
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
import android.view.View.OnLongClickListener;
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

    final DocumentController controller;
    Activity a;
    String bookTitle;

    DocumentGestureListener documentListener;
    DocumentGuestureDetector documentGestureDetector;
    GestureDetector gestureDetector;

    TextView toastBrightnessText, currentPageIndex, currentSeek, maxSeek, currentTime, bookName, nextTypeBootom, batteryLevel, lirbiLogo, reverseKeysIndicator;
    ImageView onDocDontext, toolBarButton, linkHistory, lockUnlock, lockUnlockTop, textToSpeachTop, clockIcon, batteryIcon;
    ImageView showSearch, nextScreenType, autoScroll, textToSpeach, ttsActive, onModeChange, imageMenuArrow, editTop2, goToPage1, goToPage1Top;
    View adFrame, titleBar, overlay, menuLayout, moveLeft, moveRight, pages, lineNavgination, onCloseBook, seekSpeedLayot, zoomPlus, zoomMinus;
    View line1, line2, lineFirst, lineClose, closeTop;
    SeekBar seekBar, speedSeekBar;
    FrameLayout anchor;
    DrawView drawView;
    ProgressDraw progressDraw;
    UnderlineImageView crop, cut;

    final Handler handler = new Handler();
    final Handler handlerTimer = new Handler();

    public DocumentWrapperUI(final DocumentController controller) {
        AppState.get().annotationDrawColor = "";
        AppState.get().editWith = AppState.EDIT_NONE;

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
        EventBus.getDefault().register(this);

    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onPageNumber(MessagePageNumber event) {
        try {
            if (controller != null) {
                controller.onGoToPage(event.getPage() + 1);
                ttsActive.setVisibility(View.VISIBLE);
            }
        } catch (Exception e) {
            LOG.e(e);
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onTTSStatus(TtsStatus status) {
        try {
            if (ttsActive != null) {
                ttsActive.setVisibility(TTSEngine.get().isPlaying() ? View.VISIBLE : View.GONE);
            }
        } catch (Exception e) {
            LOG.e(e);
        }

    }

    public void onSingleTap() {
        if (AppState.get().isMusicianMode) {
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
                DragingDialogs.selectTextMenu(anchor, controller, true, updateUIRunnable);
            }
        }
    }

    Runnable updateUIRunnable = new Runnable() {

        @Override
        public void run() {
            updateUI();
        }
    };

    public void showSelectTextMenu() {
        DragingDialogs.selectTextMenu(anchor, controller, true, updateUIRunnable);
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
                controller.clearSelectedText();
                anchor.setTag("backSpace");
                anchor.removeAllViews();
                anchor.setVisibility(View.GONE);
                if (prefDialog != null) {
                    prefDialog.closeDialog();
                }
            } else if (!controller.getLinkHistory().isEmpty()) {
                controller.onLinkHistory();
            } else {
                if (((VerticalViewActivity) getController().getActivity()).isInterstialShown()) {
                    return false;
                } else {

                    CloseAppDialog.showOnLongClickDialog(getController().getActivity(), null, controller);
                }
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

        if (AppState.get().isUseVolumeKeys && AppState.get().isAutoScroll && KeyEvent.KEYCODE_VOLUME_UP == keyCode) {
            if (AppState.get().autoScrollSpeed > 1) {
                AppState.get().autoScrollSpeed -= 1;
                controller.onAutoScroll();
                updateUI();
            }
            return true;
        }
        if (AppState.get().isUseVolumeKeys && AppState.get().isAutoScroll && KeyEvent.KEYCODE_VOLUME_DOWN == keyCode) {
            if (AppState.get().autoScrollSpeed <= AppState.MAX_SPEED) {
                AppState.get().autoScrollSpeed += 1;
            }
            controller.onAutoScroll();
            updateUI();
            return true;
        }

        if (AppState.get().isUseVolumeKeys && AppState.get().getNextKeys().contains(keyCode)) {
            nextChose(false);
            return true;
        }
        if (AppState.get().isUseVolumeKeys && AppState.get().getPrevKeys().contains(keyCode)) {
            prevChose(false);
            return true;
        }

        if (AppState.get().isUseVolumeKeys && KeyEvent.KEYCODE_HEADSETHOOK == keyCode) {
            if (TTSEngine.get().isPlaying()) {
                TTSEngine.get().stop();
            } else {
                TTSEngine.get().playCurrent();
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
        EventBus.getDefault().unregister(this);

        AppState.get().lastA = null;
        if (handler != null) {
            handler.removeCallbacksAndMessages(null);
        }

        if (titleBar != null) {
            titleBar.removeCallbacks(null);
        }
        controller.onCloseActivity();

        if (isLong && !MainTabs2.isInStack) {
            MainTabs2.startActivity(a, UITab.getCurrentTabIndex(UITab.SearchFragment));
        }
    }

    public void updateSpeedLabel() {
        final int max = controller.getPageCount();
        final int currentNumber = controller.getCurentPage();

        String current = TxtUtils.deltaPage(currentNumber);

        if (AppState.get().isAutoScroll) {
            currentPageIndex.setText(String.format("{%s} %s/%s", AppState.get().autoScrollSpeed, current, max));
        } else {
            currentPageIndex.setText(String.format("%s/%s", current, max));
        }
        currentPageIndex.setVisibility(max == 0 ? View.GONE : View.VISIBLE);
    }

    public void updateUI() {
        final int max = controller.getPageCount();
        final int current = controller.getCurentPage();

        updateSpeedLabel();
        currentSeek.setText(TxtUtils.deltaPage(current));
        maxSeek.setText(String.valueOf(max));

        seekBar.setOnSeekBarChangeListener(null);
        seekBar.setMax(max - 1);
        seekBar.setProgress(current - 1);
        seekBar.setOnSeekBarChangeListener(onSeek);

        speedSeekBar.setOnSeekBarChangeListener(null);
        speedSeekBar.setMax(AppState.MAX_SPEED);
        speedSeekBar.setProgress(AppState.get().autoScrollSpeed);
        speedSeekBar.setOnSeekBarChangeListener(onSpeed);

        // time
        currentTime.setText(UiSystemUtils.getSystemTime(a));

        final int myLevel = UiSystemUtils.getPowerLevel(a);
        batteryLevel.setText(myLevel + "%");
        if (myLevel == -1) {
            batteryLevel.setVisibility(View.GONE);
        }

        showChapter();

        hideShow();
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
            onModeChange.setVisibility(View.GONE);
        }

        crop.underline(AppState.get().isCrop);
        cut.underline(AppState.get().isCut);

        progressDraw.updateProgress(current - 1);
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

        if (AppState.get().isLocked) {
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

    Runnable updateTimePower = new Runnable() {

        @Override
        public void run() {
            try {
                if (currentTime != null) {
                    currentTime.setText(UiSystemUtils.getSystemTime(controller.getActivity()));

                    int myLevel = UiSystemUtils.getPowerLevel(controller.getActivity());
                    batteryLevel.setText(myLevel + "%");
                }
            } catch (Exception e) {
                LOG.e(e);
            }
            LOG.d("Update time and power");
            handlerTimer.postDelayed(updateTimePower, AppState.APP_UPDATE_TIME_IN_UI);

        }
    };

    public void initUI(final Activity a) {
        this.a = a;

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

        overlay = a.findViewById(R.id.overlay);
        overlay.setVisibility(View.VISIBLE);

        reverseKeysIndicator = (TextView) a.findViewById(R.id.reverseKeysIndicator);
        // reverseKeysIndicator.setOnClickListener(onReverseKeys);

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

        lirbiLogo = (TextView) a.findViewById(R.id.lirbiLogo);
        lirbiLogo.setText(AppState.get().musicText);
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

        final ImageView moveCenter = (ImageView) a.findViewById(R.id.moveCenter);
        moveCenter.setOnClickListener(onMoveCenter);

        moveRight = a.findViewById(R.id.moveRight);
        moveRight.setOnClickListener(onMoveRight);

        ImageView brightness = (ImageView) a.findViewById(R.id.brightness);
        brightness.setOnClickListener(onSun);
        brightness.setImageResource(!AppState.get().isInvert ? R.drawable.glyphicons_232_sun : R.drawable.glyphicons_2_moon);
        brightness.setVisibility(AppState.get().isInkMode ? View.GONE : View.VISIBLE);

        ImageView onBC = (ImageView) a.findViewById(R.id.onBC);
        onBC.setOnClickListener(onBCclick);
        onBC.setVisibility(AppState.get().isInkMode ? View.VISIBLE : View.GONE);

        a.findViewById(R.id.toPage).setOnClickListener(toPage);

        crop = (UnderlineImageView) a.findViewById(R.id.crop);
        crop.setOnClickListener(onCrop);

        if (AppState.get().isCut) {
            crop.setVisibility(View.GONE);
        }

        cut = (UnderlineImageView) a.findViewById(R.id.cut);
        cut.setOnClickListener(onCut);
        cut.setVisibility(View.GONE);

        onModeChange = (ImageView) a.findViewById(R.id.onModeChange);
        onModeChange.setOnClickListener(onModeChangeClick);
        onModeChange.setImageResource(AppState.get().isCut ? R.drawable.glyphicons_page_split : R.drawable.glyphicons_two_page_one);

        View prefTop = a.findViewById(R.id.prefTop);
        prefTop.setOnClickListener(onPrefTop);

        ImageView fullscreen = (ImageView) a.findViewById(R.id.fullscreen);
        fullscreen.setOnClickListener(onFull);
        fullscreen.setImageResource(AppState.get().isFullScreen ? R.drawable.glyphicons_487_fit_frame_to_image : R.drawable.glyphicons_488_fit_image_to_frame);

        onCloseBook = a.findViewById(R.id.close);
        onCloseBook.setOnClickListener(onClose);
        onCloseBook.setOnLongClickListener(onCloseLongClick);
        onCloseBook.setVisibility(View.INVISIBLE);

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

        onDocDontext = (ImageView) a.findViewById(R.id.onDocDontext);
        onDocDontext.setOnClickListener(onShowContext);

        lockUnlock = (ImageView) a.findViewById(R.id.lockUnlock);
        lockUnlockTop = (ImageView) a.findViewById(R.id.lockUnlockTop);
        lockUnlock.setOnClickListener(onLockUnlock);
        lockUnlockTop.setOnClickListener(onLockUnlock);

        textToSpeachTop = (ImageView) a.findViewById(R.id.textToSpeachTop);
        textToSpeachTop.setOnClickListener(onTextToSpeach);

        ttsActive = (ImageView) a.findViewById(R.id.ttsActive);
        onTTSStatus(null);
        ttsActive.setOnClickListener(onTextToSpeach);

        batteryIcon = (ImageView) a.findViewById(R.id.batteryIcon);
        clockIcon = (ImageView) a.findViewById(R.id.clockIcon);

        textToSpeach = (ImageView) a.findViewById(R.id.textToSpeach);
        textToSpeach.setOnClickListener(onTextToSpeach);

        drawView = (DrawView) a.findViewById(R.id.drawView);

        View bookmarks = a.findViewById(R.id.onBookmarks);
        bookmarks.setOnClickListener(onBookmarks);
        bookmarks.setOnLongClickListener(onBookmarksLong);

        toastBrightnessText = (TextView) a.findViewById(R.id.toastBrightnessText);
        toastBrightnessText.setVisibility(View.GONE);
        TintUtil.setDrawableTint(toastBrightnessText.getCompoundDrawables()[0], Color.WHITE);

        currentPageIndex = (TextView) a.findViewById(R.id.currentPageIndex);
        currentSeek = (TextView) a.findViewById(R.id.currentSeek);
        maxSeek = (TextView) a.findViewById(R.id.maxSeek);
        bookName = (TextView) a.findViewById(R.id.bookName);

        currentTime = (TextView) a.findViewById(R.id.currentTime);
        batteryLevel = (TextView) a.findViewById(R.id.currentBattery);

        if (AppState.get().isUseTypeFace) {
            reverseKeysIndicator.setTypeface(BookCSS.getNormalTypeFace());
            bookName.setTypeface(BookCSS.getNormalTypeFace());
            currentSeek.setTypeface(BookCSS.getNormalTypeFace());
            currentPageIndex.setTypeface(BookCSS.getNormalTypeFace());
            maxSeek.setTypeface(BookCSS.getNormalTypeFace());
            currentTime.setTypeface(BookCSS.getNormalTypeFace());
            batteryLevel.setTypeface(BookCSS.getNormalTypeFace());
        }

        currentSeek.setOnLongClickListener(new OnLongClickListener() {

            @Override
            public boolean onLongClick(View v) {
                Dialogs.showDeltaPage(anchor, controller, controller.getCurentPageFirst1(), updateUIRunnable);
                return true;
            }
        });

        View thumbnail = a.findViewById(R.id.thumbnail);
        thumbnail.setOnClickListener(onThumbnail);

        View itemMenu = a.findViewById(R.id.itemMenu);
        itemMenu.setOnClickListener(onItemMenu);

        progressDraw = (ProgressDraw) a.findViewById(R.id.progressDraw);

        AppState.get().isAutoScroll = false;

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
        BrightnessHelper.updateOverlay(overlay);

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

        textToSpeachTop.setVisibility(View.GONE);
        lockUnlockTop.setVisibility(View.GONE);
        nextScreenType.setVisibility(View.GONE);
        goToPage1Top.setVisibility(View.GONE);

        if (AppState.get().isMusicianMode) {
            AppState.get().isEditMode = false;
            line1.setVisibility(View.VISIBLE);
            line2.setVisibility(View.VISIBLE);
            lineFirst.setVisibility(View.VISIBLE);
            lineClose.setVisibility(View.VISIBLE);

            goToPage1.setVisibility(View.VISIBLE);
            goToPage1Top.setVisibility(View.VISIBLE);
            lockUnlockTop.setVisibility(View.VISIBLE);
            closeTop.setVisibility(View.VISIBLE);

            reverseKeysIndicator.setVisibility(View.GONE);
            textToSpeachTop.setVisibility(View.GONE);
            progressDraw.setVisibility(View.GONE);
            textToSpeach.setVisibility(View.GONE);
        }

        currentSeek.setVisibility(View.GONE);
        maxSeek.setVisibility(View.GONE);
        seekBar.setVisibility(View.INVISIBLE);

    }

    public void updateSeekBarColorAndSize() {
        lirbiLogo.setText(AppState.get().musicText);
        TintUtil.setBackgroundFillColorBottomRight(ttsActive, ColorUtils.setAlphaComponent(TintUtil.color, 230));

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

        int progressColor = AppState.get().isInvert ? AppState.get().statusBarColorDay : MagicHelper.otherColor(AppState.get().statusBarColorNight, +0.2f);
        progressDraw.updateColor(progressColor);
        progressDraw.getLayoutParams().height = Dips.dpToPx(AppState.get().progressLineHeight);
        progressDraw.requestLayout();

        // textSize
        bookName.setTextSize(AppState.get().statusBarTextSizeAdv);
        currentPageIndex.setTextSize(AppState.get().statusBarTextSizeAdv);
        currentTime.setTextSize(AppState.get().statusBarTextSizeAdv);
        batteryLevel.setTextSize(AppState.get().statusBarTextSizeAdv);
        reverseKeysIndicator.setTextSize(AppState.get().statusBarTextSizeAdv);
        lirbiLogo.setTextSize(AppState.get().statusBarTextSizeAdv);

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

        // lirbiLogo.getLayoutParams().height = panelSize;

    }

    @Subscribe
    public void onMessegeBrightness(MessegeBrightness msg) {
        BrightnessHelper.onMessegeBrightness(msg, toastBrightnessText, overlay);
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
                        TempHolder.listHash++;
                        AppDB.get().deleteBy(controller.getCurrentBook().getPath());
                        controller.getActivity().finish();
                    }
                }
            }, controller.getCurentPage() - 1, DocumentWrapperUI.this, controller);
            Keyboards.hideNavigation(a);
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
            if (AppState.get().isCut) {
                onCut.onClick(null);
                return;
            }
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
            controller.onGoToPage(progress + 1);
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
            AppState.get().autoScrollSpeed = progress + 1;
            updateSpeedLabel();

            // hideSeekBarInReadMode();
        }
    };

    public void doDoubleTap(int x, int y) {
        if (AppState.get().isMusicianMode) {
            controller.alignDocument();
        } else {
            if (AppState.get().doubleClickAction == AppState.DOUBLE_CLICK_ZOOM_IN_OUT) {
                controller.onZoomInOut(x, y);
                AppState.get().isEditMode = false;
                hideShow();
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
        AppState.get().isEditMode = !AppState.get().isEditMode;
        hideShow();

    }

    public void showHideHavigationBar() {
        if (!AppState.get().isEditMode && AppState.get().isFullScreen) {
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
        AppState.get().isShowToolBar = !AppState.get().isShowToolBar;
        initToolBarPlusMinus();
    }

    public void initToolBarPlusMinus() {
        if (AppState.get().isShowToolBar) {
            toolBarButton.setImageResource(R.drawable.glyphicons_336_pushpin);
        } else {
            toolBarButton.setImageResource(R.drawable.glyphicons_200_ban);
        }
        if (AppState.get().isEditMode || AppState.get().isShowToolBar) {
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

            if (AppState.get().isUseTypeFace) {
                nextTypeBootom.setTypeface(BookCSS.getNormalTypeFace());
            }
        } else {
            if (AppState.get().nextScreenScrollBy == 100) {
                nextTypeBootom.setText(controller.getString(R.string.by_screans));
            } else {
                nextTypeBootom.setText(AppState.get().nextScreenScrollBy + "% " + controller.getString(R.string.of_screen));
            }
            nextScreenType.setImageResource(R.drawable.glyphicons_halp_page);

            if (AppState.get().isUseTypeFace) {
                nextTypeBootom.setTypeface(BookCSS.getNormalTypeFace());
            }
        }

    }

    public void hideShow() {
        if (AppState.get().isEditMode) {
            show();
        } else {
            hide();
        }
        initToolBarPlusMinus();

        if (AppState.get().isAutoScroll) {
            autoScroll.setImageResource(R.drawable.glyphicons_175_pause);
        } else {
            autoScroll.setImageResource(R.drawable.glyphicons_174_play);
        }

        if (AppState.get().isMusicianMode) {
            if (AppState.get().isAutoScroll) {
                seekSpeedLayot.setVisibility(View.VISIBLE);
            } else {
                seekSpeedLayot.setVisibility(View.GONE);
            }
        } else {
            if (AppState.get().isEditMode && AppState.get().isAutoScroll) {
                seekSpeedLayot.setVisibility(View.VISIBLE);
            } else {
                seekSpeedLayot.setVisibility(View.GONE);
            }
        }

        if (AppState.get().isMusicianMode) {
            lirbiLogo.setVisibility(View.VISIBLE);
        } else {
            lirbiLogo.setVisibility(View.GONE);
        }

        // hideSeekBarInReadMode();
        // showHideHavigationBar();
        DocumentController.chooseFullScreen(controller.getActivity(), AppState.get().isFullScreen);
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
        if (!AppState.get().isEditMode) {
            handler.removeCallbacks(hideSeekBar);
            handler.postDelayed(hideSeekBar, 5000);
        }
    }

    Runnable hideSeekBar = new Runnable() {

        @Override
        public void run() {
            if (!AppState.get().isMusicianMode) {
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

        // if (AppState.get().isAutoScroll &&
        // AppState.get().isEditMode) {
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
        if (AppState.get().isCut) {
            onModeChange.setImageResource(R.drawable.glyphicons_two_page_one);
            AppState.get().isCut = !false;
            onCut.onClick(null);
        }

        DragingDialogs.searchMenu(anchor, controller, "");
    }

    public View.OnClickListener onAutoScroll = new View.OnClickListener() {

        @Override
        public void onClick(final View arg0) {
            onAutoScrollClick();
        }
    };

    public void onAutoScrollClick() {
        AppState.get().isAutoScroll = !AppState.get().isAutoScroll;
        // changeAutoScrollButton();
        controller.onAutoScroll();
        updateUI();
    }

    // public void changeAutoScrollButton() {
    // if (AppState.get().isAutoScroll) {
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
            AppState.get().isLocked = !AppState.get().isLocked;
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
            DragingDialogs.addBookmarks(anchor, controller, new Runnable() {

                @Override
                public void run() {
                    showHideHistory();
                }
            });
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
            hideShow();
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
        public void onClick(final View v) {
            AppState.get().isFullScreen = !AppState.get().isFullScreen;
            ((ImageView) v).setImageResource(AppState.get().isFullScreen ? R.drawable.glyphicons_487_fit_frame_to_image : R.drawable.glyphicons_488_fit_image_to_frame);
            DocumentController.chooseFullScreen(a, AppState.get().isFullScreen);

            // if (controller.isTextFormat()) {
            // onRefresh.run();
            // controller.restartActivity();
            // }
        }
    };
    public View.OnClickListener onScreenMode = new View.OnClickListener() {

        @Override
        public void onClick(final View arg0) {
            a.finish();
            a.startActivity(a.getIntent());
        }
    };

    public View.OnClickListener onBCclick = new View.OnClickListener() {

        @Override
        public void onClick(final View arg0) {
            DragingDialogs.contrastAndBrigtness(anchor, controller, new Runnable() {

                @Override
                public void run() {
                    controller.updateRendering();
                }
            }, null);
        }
    };

    public View.OnClickListener onSun = new View.OnClickListener() {

        @Override
        public void onClick(final View arg0) {
            arg0.setEnabled(false);
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
            controller.onCrop();
            updateUI();

        }
    };

    public View.OnClickListener onModeChangeClick = new View.OnClickListener() {

        @Override
        public void onClick(final View v) {
            MyPopupMenu p = new MyPopupMenu(v.getContext(), v);

            p.getMenu().add(R.string.one_page).setIcon(R.drawable.glyphicons_two_page_one).setOnMenuItemClickListener(new OnMenuItemClickListener() {

                @Override
                public boolean onMenuItemClick(MenuItem item) {
                    onModeChange.setImageResource(R.drawable.glyphicons_two_page_one);
                    AppState.get().isCut = !false;
                    onCut.onClick(null);
                    return false;
                }
            });
            p.getMenu().add(R.string.half_page).setIcon(R.drawable.glyphicons_page_split).setOnMenuItemClickListener(new OnMenuItemClickListener() {

                @Override
                public boolean onMenuItemClick(MenuItem item) {
                    onModeChange.setImageResource(R.drawable.glyphicons_page_split);
                    AppState.get().isCut = !true;
                    onCut.onClick(null);
                    return false;
                }
            });
            p.show();
            Keyboards.hideNavigation(controller.getActivity());

        }
    };
    public View.OnClickListener onCut = new View.OnClickListener() {

        @Override
        public void onClick(final View arg0) {
            AppState.get().isCrop = true;
            AppState.get().cutP = 50;
            AppState.get().isCut = !AppState.get().isCut;

            BookSettings bookSettings = SettingsManager.getBookSettings();
            if (bookSettings != null) {
                bookSettings.updateFromAppState();
                bookSettings.save();
            }

            crop.setVisibility(AppState.get().isCut ? View.GONE : View.VISIBLE);

            SettingsManager.toggleCropMode(true);

            controller.onCrop();// crop false
            controller.updateRendering();
            controller.alignDocument();

            updateUI();

            progressDraw.updatePageCount(controller.getPageCount() - 1);
            titleBar.setOnTouchListener(new HorizontallSeekTouchEventListener(onSeek, controller.getPageCount(), false));
            progressDraw.setOnTouchListener(new HorizontallSeekTouchEventListener(onSeek, controller.getPageCount(), false));

        }
    };

    DragingPopup prefDialog;
    public View.OnClickListener onPrefTop = new View.OnClickListener() {

        @Override
        public void onClick(final View arg0) {
            prefDialog = DragingDialogs.preferences(anchor, controller, onRefresh, new Runnable() {

                @Override
                public void run() {
                    updateUI();

                }
            });
        }
    };

    Runnable onRefresh = new Runnable() {

        @Override
        public void run() {
            double value = (getController().getCurentPage() + 0.0001) / getController().getPageCount();
            a.getIntent().putExtra(HorizontalModeController.PERCENT_EXTRA, value);
            // titleBar.setBackgroundColor(MagicHelper.getBgColor());
            initToolBarPlusMinus();
            updateSeekBarColorAndSize();
            hideShow();
            TTSEngine.get().stop();
            BrightnessHelper.updateOverlay(overlay);
        }
    };

    public View.OnClickListener onClose = new View.OnClickListener() {

        @Override
        public void onClick(final View arg0) {
            ImageLoader.getInstance().clearAllTasks();
            anchor.setTag("backSpace");
            anchor.removeAllViews();
            anchor.setVisibility(View.GONE);

            closeAndRunList(false);
        }
    };
    public View.OnLongClickListener onCloseLongClick = new View.OnLongClickListener() {

        @Override
        public boolean onLongClick(final View v) {
            Vibro.vibrate();
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

    public void showHelp() {
        if (AppState.get().isFirstTimeVertical) {
            handler.postDelayed(new Runnable() {

                @Override
                public void run() {
                    AppState.get().isFirstTimeVertical = false;
                    AppState.get().isEditMode = true;
                    hideShow();
                    Views.showHelpToast(lockUnlock);

                }
            }, 1000);
        }
    }

    public void showOutline(final List<OutlineLinkWrapper> list, final int count) {
        try {
            controller.activity.runOnUiThread(new Runnable() {

                @Override
                public void run() {
                    progressDraw.updateDivs(list);
                    progressDraw.updatePageCount(controller.getPageCount() - 1);
                    titleBar.setOnTouchListener(new HorizontallSeekTouchEventListener(onSeek, controller.getPageCount(), false));
                    progressDraw.setOnTouchListener(new HorizontallSeekTouchEventListener(onSeek, controller.getPageCount(), false));
                    if (TxtUtils.isListEmpty(list)) {
                        TintUtil.setTintImage(onDocDontext, Color.LTGRAY);
                    }

                    if (ExtUtils.isNoTextLayerForamt(controller.getCurrentBook().getPath())) {
                        TintUtil.setTintImage(textToSpeach, Color.LTGRAY);
                    }
                    if (controller.isTextFormat()) {
                        // TintUtil.setTintImage(lockUnlock, Color.LTGRAY);
                    }

                    currentSeek.setVisibility(View.VISIBLE);
                    maxSeek.setVisibility(View.VISIBLE);
                    seekBar.setVisibility(View.VISIBLE);

                    onCloseBook.setVisibility(View.VISIBLE);

                    showHelp();

                    RecentUpates.updateAll(controller.getActivity());

                }
            });
        } catch (Exception e) {
            LOG.e(e);
        }

    }

    public void onResume() {
        LOG.d("DocumentWrapperUI", "onResume");
        handlerTimer.post(updateTimePower);

        if (controller != null && TTSEngine.get().isPlaying()) {
            controller.onGoToPage(AppState.get().lastBookPage);
        }
    }

    public void onPause() {
        LOG.d("DocumentWrapperUI", "onPause");
        handlerTimer.removeCallbacks(updateTimePower);

    }

    public void onDestroy() {
        LOG.d("DocumentWrapperUI", "onDestroy");
        handlerTimer.removeCallbacksAndMessages(null);
        handler.removeCallbacksAndMessages(null);

    }

}
