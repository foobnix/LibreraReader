/**
 *
 */
package com.foobnix.pdf.info.wrapper;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.graphics.Color;
import android.os.Build;
import android.os.Handler;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.MenuItem;
import android.view.MenuItem.OnMenuItemClickListener;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnLongClickListener;
import android.view.ViewTreeObserver.OnGlobalLayoutListener;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.core.graphics.ColorUtils;

import com.foobnix.android.utils.Apps;
import com.foobnix.android.utils.Dips;
import com.foobnix.android.utils.IntegerResponse;
import com.foobnix.android.utils.Keyboards;
import com.foobnix.android.utils.LOG;
import com.foobnix.android.utils.TxtUtils;
import com.foobnix.android.utils.Vibro;
import com.foobnix.android.utils.Views;
import com.foobnix.model.AppSP;
import com.foobnix.model.AppState;
import com.foobnix.pdf.info.AppsConfig;
import com.foobnix.pdf.info.BookmarksData;
import com.foobnix.pdf.info.DictsHelper;
import com.foobnix.pdf.info.ExtUtils;
import com.foobnix.pdf.info.OutlineHelper;
import com.foobnix.pdf.info.OutlineHelper.Info;
import com.foobnix.pdf.info.R;
import com.foobnix.pdf.info.TintUtil;
import com.foobnix.pdf.info.UiSystemUtils;
import com.foobnix.pdf.info.model.OutlineLinkWrapper;
import com.foobnix.pdf.info.view.AlertDialogs;
import com.foobnix.pdf.info.view.AnchorHelper;
import com.foobnix.pdf.info.view.BookmarkPanel;
import com.foobnix.pdf.info.view.BrightnessHelper;
import com.foobnix.pdf.info.view.CustomSeek;
import com.foobnix.pdf.info.view.Dialogs;
import com.foobnix.pdf.info.view.DialogsPlaylist;
import com.foobnix.pdf.info.view.DragingDialogs;
import com.foobnix.pdf.info.view.DrawView;
import com.foobnix.pdf.info.view.HorizontallSeekTouchEventListener;
import com.foobnix.pdf.info.view.HypenPanelHelper;
import com.foobnix.pdf.info.view.MyPopupMenu;
import com.foobnix.pdf.info.view.ProgressDraw;
import com.foobnix.pdf.info.view.UnderlineImageView;
import com.foobnix.pdf.info.widget.DraggbleTouchListener;
import com.foobnix.pdf.info.widget.ShareDialog;
import com.foobnix.pdf.search.activity.HorizontalViewActivity;
import com.foobnix.pdf.search.activity.msg.MessagePageXY;
import com.foobnix.pdf.search.activity.msg.MessegeBrightness;
import com.foobnix.pdf.search.view.CloseAppDialog;
import com.foobnix.sys.TempHolder;
import com.foobnix.tts.MessagePageNumber;
import com.foobnix.tts.TTSControlsView;
import com.foobnix.tts.TTSEngine;
import com.foobnix.tts.TTSService;
import com.foobnix.tts.TtsStatus;
import com.foobnix.ui2.AppDB;
import com.foobnix.ui2.MainTabs2;

import org.ebookdroid.BookType;
import org.ebookdroid.LibreraApp;
import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

/**
 * @author iivanenko
 */
public class DocumentWrapperUI {

    final DocumentController dc;
    final Handler handler = new Handler();
    final Handler handlerTimer = new Handler();
    public View.OnClickListener onLockUnlock = new View.OnClickListener() {

        @Override
        public void onClick(final View arg0) {
            AppSP.get().isLocked = !AppSP.get().isLocked;
            updateLock();
        }
    };
    public View.OnClickListener onNextType = new View.OnClickListener() {

        @Override
        public void onClick(final View arg0) {
            LOG.d("DEBUG", "Click");
            doChooseNextType(arg0);
        }
    };
    public View.OnClickListener onSun = new View.OnClickListener() {

        @Override
        public void onClick(final View arg0) {
            arg0.setEnabled(false);
            dc.onNightMode();
        }
    };
    public View.OnClickListener toPage = new View.OnClickListener() {

        @Override
        public void onClick(final View arg0) {
            dc.toPageDialog();
        }
    };
    public View.OnClickListener onClose = new View.OnClickListener() {

        @Override
        public void onClick(final View arg0) {
            //ImageLoader.getInstance().clearAllTasks();
            //Glide.with(LibreraApp.context).
            closeDialogs();
            closeAndRunList();
        }
    };
    public View.OnClickListener onMoveLeft = new View.OnClickListener() {

        @Override
        public void onClick(final View arg0) {
            dc.onSrollLeft();
        }
    };
    public View.OnClickListener onMoveCenter = new View.OnClickListener() {

        @Override
        public void onClick(final View arg0) {
            dc.alignDocument();
        }
    };
    public View.OnClickListener onMoveRight = new View.OnClickListener() {

        @Override
        public void onClick(final View arg0) {
            dc.onSrollRight();
        }
    };
    public View.OnClickListener onNextPage = new View.OnClickListener() {

        @Override
        public void onClick(final View arg0) {
            nextChose(false);
        }
    };
    public View.OnClickListener onPrevPage = new View.OnClickListener() {

        @Override
        public void onClick(final View arg0) {
            prevChose(false);
        }
    };
    public View.OnClickListener onPlus = new View.OnClickListener() {

        public void onClick(final View arg0) {
            dc.onZoomInc();
        }
    };
    public View.OnClickListener onMinus = new View.OnClickListener() {

        @Override
        public void onClick(final View arg0) {
            dc.onZoomDec();
        }
    };
    public View.OnClickListener onModeChangeClick = new View.OnClickListener() {

        @Override
        public void onClick(final View v) {
            MyPopupMenu p = new MyPopupMenu(v.getContext(), v);

            p.getMenu().add(R.string.one_page).setIcon(R.drawable.glyphicons_two_page_one).setOnMenuItemClickListener(new OnMenuItemClickListener() {

                @Override
                public boolean onMenuItemClick(MenuItem item) {
                    closeDialogs();
                    onModeChange.setImageResource(R.drawable.glyphicons_two_page_one);
                    AppSP.get().isCut = !false;
                    onCut.onClick(null);
                    hideShowEditIcon();
                    return false;
                }
            });
            p.getMenu().add(R.string.half_page).setIcon(R.drawable.glyphicons_page_split).setOnMenuItemClickListener(new OnMenuItemClickListener() {

                @Override
                public boolean onMenuItemClick(MenuItem item) {
                    closeDialogs();
                    onModeChange.setImageResource(R.drawable.glyphicons_page_split);
                    AppSP.get().isCut = !true;
                    onCut.onClick(null);
                    hideShowEditIcon();
                    return false;
                }
            });
            p.show();
            Keyboards.hideNavigation(dc.getActivity());

        }
    };
    Activity a;
    String bookTitle;
    TextView toastBrightnessText, floatingBookmarkTextView, pagesCountIndicator, currentSeek, maxSeek, currentTime, bookName, nextTypeBootom, batteryLevel, lirbiLogo, reverseKeysIndicator;
    ImageView onDocDontext, toolBarButton, linkHistory, lockUnlock, lockUnlockTop, textToSpeachTop, clockIcon, batteryIcon, fullscreen;
    ImageView showSearch, nextScreenType, moveCenter, autoScroll, textToSpeach, onModeChange, imageMenuArrow, editTop2, goToPage1, goToPage1Top;
    View adFrame, titleBar, overlay, menuLayout, moveLeft, moveRight, bottomBar, onCloseBook, seekSpeedLayot, zoomPlus, zoomMinus;
    public View.OnLongClickListener onCloseLongClick = new View.OnLongClickListener() {

        @Override
        public boolean onLongClick(final View v) {
            Vibro.vibrate();
            CloseAppDialog.showOnLongClickDialog(a, v, getController());
            hideAds();
            return true;
        }
    };
    View line1, line2, lineFirst, lineClose, closeTop, pagesBookmark, musicButtonPanel, parentParent, documentTitleBar;
    TTSControlsView ttsActive;
    SeekBar seekBar, speedSeekBar;
    FrameLayout anchor;
    public View.OnClickListener onShowContext = new View.OnClickListener() {

        @Override
        public void onClick(final View arg0) {
            DragingDialogs.showContent(anchor, dc);
        }
    };
    public View.OnClickListener onBCclick = new View.OnClickListener() {

        @Override
        public void onClick(final View arg0) {
            DragingDialogs.contrastAndBrigtness(anchor, dc, new Runnable() {

                @Override
                public void run() {
                    onBC.underline(AppState.get().isEnableBC);
                    dc.updateRendering();
                }
            }, null);
        }
    };
    public View.OnClickListener onShowSearch = new View.OnClickListener() {

        @Override
        public void onClick(final View arg0) {
            showSearchDialog();
        }

    };
    ImageView anchorX, anchorY;
    DrawView drawView;
    public View.OnClickListener onShowHideEditPanel = new View.OnClickListener() {

        @Override
        public void onClick(final View arg0) {
            if (AppSP.get().isCrop) {
                onCrop.onClick(null);
            }

            DragingDialogs.editColorsPanel(anchor, dc, drawView, false);
        }
    };
    ProgressDraw progressDraw;
    public View.OnClickListener onHideShowToolBar = new View.OnClickListener() {

        @Override
        public void onClick(final View arg0) {
            LOG.d("DEBUG", "Click");
            doHideShowToolBar();
        }
    };
    UnderlineImageView crop, cut, onBC;
    LinearLayout pageshelper;
    String quickBookmark;
    Runnable clearFlags = new Runnable() {

        @Override
        public void run() {
            try {
                dc.getActivity().getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                LOG.d("FLAG clearFlags", "FLAG_KEEP_SCREEN_ON", "clear");
            } catch (Exception e) {
                LOG.e(e);
            }
        }
    };
    Runnable updateTimePower = new Runnable() {

        @Override
        public void run() {
            try {
                if (currentTime != null) {
                    currentTime.setText(UiSystemUtils.getSystemTime(dc.getActivity()));

                    int myLevel = UiSystemUtils.getPowerLevel(dc.getActivity());
                    batteryLevel.setText(myLevel + "%");
                }
            } catch (Exception e) {
                LOG.e(e);
            }
            LOG.d("Update time and power");
            handlerTimer.postDelayed(updateTimePower, AppState.APP_UPDATE_TIME_IN_UI);

        }
    };
    View.OnClickListener onRecent = new View.OnClickListener() {

        @Override
        public void onClick(View v) {
            DragingDialogs.recentBooks(anchor, dc);
        }
    };
    View.OnClickListener onTextToSpeach = new View.OnClickListener() {

        @Override
        public void onClick(final View v) {
            if (AppSP.get().isCut) {
                onModeChange.setImageResource(R.drawable.glyphicons_two_page_one);
                onCut.onClick(null);
                return;
            }
            DragingDialogs.textToSpeachDialog(anchor, dc);
        }
    };
    View.OnClickListener onThumbnail = new View.OnClickListener() {

        @Override
        public void onClick(final View v) {
            DragingDialogs.gotoPageDialog(anchor, dc);
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
            dc.onGoToPage(progress + 1);
            Apps.accessibilityText(a, a.getString(R.string.m_current_page) + " " + dc.getCurentPageFirst1());
            //updateUI();
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
    Runnable hideSeekBar = new Runnable() {

        @Override
        public void run() {
            if (!dc.isMusicianMode()) {
                seekSpeedLayot.setVisibility(View.GONE);
            }

        }
    };
    Runnable onRefresh = new Runnable() {

        @Override
        public void run() {
            dc.saveCurrentPageAsync();
            initToolBarPlusMinus();
            updateSeekBarColorAndSize();
            hideShow();
            updateUI();
            TTSEngine.get().stop();
            BrightnessHelper.updateOverlay(overlay);
            showPagesHelper();
            hideShowPrevNext();
        }
    };
    public View.OnClickListener onAutoScroll = new View.OnClickListener() {

        @Override
        public void onClick(final View arg0) {
            onAutoScrollClick();
        }
    };
    public View.OnClickListener onLinkHistory = new View.OnClickListener() {

        @Override
        public void onClick(final View arg0) {
            dc.onLinkHistory();
            updateUI();
        }
    };
    public View.OnClickListener onBookmarks = new View.OnClickListener() {

        @Override
        public void onClick(final View arg0) {
            DragingDialogs.showBookmarksDialog(anchor, dc, new Runnable() {

                @Override
                public void run() {
                    showHideHistory();
                    showPagesHelper();
                    updateUI();
                }
            });
        }
    };
    public View.OnLongClickListener onBookmarksLong = new View.OnLongClickListener() {

        @Override
        public boolean onLongClick(final View arg0) {
            DragingDialogs.addBookmarksLong(anchor, dc);
            showPagesHelper();
            return true;
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
            DocumentController.showFullScreenPopup(dc.getActivity(), v, id -> {
                AppState.get().fullScreenMode = id;
                fullscreen.setImageResource(DocumentController.getFullScreenIcon(a, AppState.get().fullScreenMode));

                if (dc.isTextFormat()) {
                    onRefresh.run();
                    dc.restartActivity();
                }
                DocumentController.chooseFullScreen(a, AppState.get().fullScreenMode);
                return true;
            }, AppState.get().fullScreenMode);
        }
    };
    public View.OnClickListener onCrop = new View.OnClickListener() {

        @Override
        public void onClick(final View v) {
            DragingDialogs.customCropDialog(anchor, dc, new Runnable() {

                @Override
                public void run() {
                    dc.onCrop();
                    updateUI();

                    AppState.get().isEditMode = false;
                    hideShow();
                    hideShowEditIcon();
                }
            });
        }
    };
    public View.OnLongClickListener onCropLong = new View.OnLongClickListener() {

        @Override
        public boolean onLongClick(View v) {
            AppSP.get().isCrop = !AppSP.get().isCrop;

            dc.onCrop();
            updateUI();

            AppState.get().isEditMode = false;
            hideShow();
            hideShowEditIcon();
            return true;
        }
    };
    public View.OnClickListener onCut = new View.OnClickListener() {

        @Override
        public void onClick(final View arg0) {
            AppSP.get().isCrop = false; // no crop with cut
            AppState.get().cutP = 50;
            AppSP.get().isCut = !AppSP.get().isCut;

            crop.setVisibility(AppSP.get().isCut ? View.GONE : View.VISIBLE);


            dc.onCrop();// crop false
            dc.updateRendering();
            dc.alignDocument();

            updateUI();

            progressDraw.updatePageCount(dc.getPageCount() - 1);
            titleBar.setOnTouchListener(new HorizontallSeekTouchEventListener(onSeek, dc.getPageCount(), false));
            progressDraw.setOnTouchListener(new HorizontallSeekTouchEventListener(onSeek, dc.getPageCount(), false));

        }
    };
    public View.OnClickListener onPrefTop = new View.OnClickListener() {

        @Override
        public void onClick(final View arg0) {
            DragingDialogs.preferences(anchor, dc, onRefresh, new Runnable() {

                @Override
                public void run() {
                    updateUI();

                }
            });
        }
    };
    Runnable updateUIRunnable = new Runnable() {

        @Override
        public void run() {
            updateUI();
        }
    };
    View.OnClickListener onItemMenu = new View.OnClickListener() {

        @Override
        public void onClick(final View v) {
            ShareDialog.show(a, dc.getCurrentBook(), new Runnable() {

                @Override
                public void run() {
                    if (dc.getCurrentBook().delete()) {
                        TempHolder.listHash++;
                        AppDB.get().deleteBy(dc.getCurrentBook().getPath());
                        dc.getActivity().finish();
                    }
                }
            }, dc.getCurentPage() - 1, dc, new Runnable() {

                @Override
                public void run() {
                    hideShow();

                }
            });
            Keyboards.hideNavigation(a);
            hideAds();
        }
    };
    View.OnClickListener onLirbiLogoClick = new View.OnClickListener() {

        @Override
        public void onClick(final View v) {
            doShowHideWrapperControlls();
        }
    };
    View.OnClickListener onGoToPAge1 = new View.OnClickListener() {

        @Override
        public void onClick(View v) {
            dc.onScrollY(0);
            updateUI();
        }
    };
    public View.OnClickListener onNormalMode = new View.OnClickListener() {

        @Override
        public void onClick(final View arg0) {
            AppSP.get().readingMode = AppState.READING_MODE_BOOK;
            initUI(a);
            hideShow();
        }
    };

    public DocumentWrapperUI(final DocumentController controller) {
        AppState.get().annotationDrawColor = "";
        AppState.get().editWith = AppState.EDIT_NONE;

        this.dc = controller;
        controller.setUi(this);

        EventBus.getDefault().register(this);

    }

    public static boolean isCJK(int ch) {
        Character.UnicodeBlock block = Character.UnicodeBlock.of(ch);
        if (Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS.equals(block) || Character.UnicodeBlock.CJK_COMPATIBILITY_IDEOGRAPHS.equals(block) || Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_A.equals(block)) {
            return true;
        }
        return false;
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onPageNumber(MessagePageNumber event) {
        try {
            if (dc != null) {
                dc.onGoToPage(event.getPage() + 1);
                ttsActive.setVisibility(View.VISIBLE);
            }
        } catch (Exception e) {
            LOG.e(e);
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onTTSStatus(TtsStatus status) {
        try {
            ttsActive.setVisibility(TxtUtils.visibleIf(!TTSEngine.get().isShutdown()));
        } catch (Exception e) {
            LOG.e(e);
        }

    }

    public void onSingleTap() {
        if (dc.isMusicianMode()) {
            onAutoScrollClick();
        } else {
            doShowHideWrapperControlls();
        }
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

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public void onLongPress(MotionEvent ev) {
        if (dc.isTextFormat() && TxtUtils.isFooterNote(AppState.get().selectedText)) {
            DragingDialogs.showFootNotes(anchor, dc, new Runnable() {

                @Override
                public void run() {
                    showHideHistory();
                }
            });
        } else {
            if (AppState.get().isRememberDictionary) {
                DictsHelper.runIntent(dc.getActivity(), AppState.get().selectedText);
                dc.clearSelectedText();
            } else {
                DragingDialogs.selectTextMenu(anchor, dc, true, updateUIRunnable);
            }
        }
    }

    public void showSelectTextMenu() {
        DragingDialogs.selectTextMenu(anchor, dc, true, updateUIRunnable);

    }

    public boolean checkBack(final KeyEvent event) {
        int keyCode = event.getKeyCode();
        if (keyCode == 0) {
            keyCode = event.getScanCode();
        }

        if (anchor == null) {
            closeAndRunList();
            return true;
        }
        if (AppState.get().isAutoScroll) {
            AppState.get().isAutoScroll = false;
            updateUI();
            return true;
        }

        if (dc.floatingBookmark != null) {
            dc.floatingBookmark = null;
            onRefresh.run();
            return true;
        }

        if (KeyEvent.KEYCODE_BACK == keyCode) {
            if (closeDialogs()) {
                return true;
            } else if (!dc.getLinkHistory().isEmpty()) {
                dc.onLinkHistory();
                return true;
            }
        }
        return false;
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
            dc.onGoToPage(keyCode - KeyEvent.KEYCODE_1 + 1);
            return true;
        }
        if (keyCode == KeyEvent.KEYCODE_0) {
            dc.toPageDialog();
            return true;
        }

        if (KeyEvent.KEYCODE_F == keyCode) {
            dc.alignDocument();
            return true;
        }

        if (KeyEvent.KEYCODE_ENTER == keyCode) {
            closeDialogs();
            AppState.get().isEditMode = false;
            hideShow();
            if (TTSEngine.get().isTempPausing()) {
                TTSService.playPause(dc.getActivity(), dc);
            } else {
                onAutoScrollClick();
            }
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

        if (AppState.get().isUseVolumeKeys && AppState.get().isZoomInOutWithVolueKeys) {
            if (keyCode == KeyEvent.KEYCODE_VOLUME_UP) {
                dc.onZoomInc();
                return true;
            }

            if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN) {
                dc.onZoomDec();
                return true;
            }

        }

        if (AppState.get().isScrollSpeedByVolumeKeys && AppState.get().isUseVolumeKeys && AppState.get().isAutoScroll) {
            if (KeyEvent.KEYCODE_VOLUME_UP == keyCode) {
                if (AppState.get().autoScrollSpeed > 1) {
                    AppState.get().autoScrollSpeed -= 1;
                    dc.onAutoScroll();
                    updateUI();
                }
                return true;
            }
            if (KeyEvent.KEYCODE_VOLUME_DOWN == keyCode) {
                if (AppState.get().autoScrollSpeed <= AppState.MAX_SPEED) {
                    AppState.get().autoScrollSpeed += 1;
                }
                dc.onAutoScroll();
                updateUI();
                return true;
            }
        }

        if (!TTSEngine.get().isPlaying()) {
            if (AppState.get().isUseVolumeKeys && AppState.get().getNextKeys().contains(keyCode)) {
                if (closeDialogs()) {
                    return true;
                }
                nextChose(false, event.getRepeatCount());
                return true;
            }

            if (AppState.get().isUseVolumeKeys && AppState.get().getPrevKeys().contains(keyCode)) {
                if (closeDialogs()) {
                    return true;
                }
                prevChose(false, event.getRepeatCount());
                return true;
            }
        }

        if (AppState.get().isUseVolumeKeys && KeyEvent.KEYCODE_HEADSETHOOK == keyCode) {
            if (TTSEngine.get().isPlaying()) {
                if (AppState.get().isFastBookmarkByTTS) {
                    TTSEngine.get().fastTTSBookmakr(dc);
                } else {
                    TTSEngine.get().stop();
                }
            } else {
                //TTSEngine.get().playCurrent();
                TTSService.playPause(dc.getActivity(), dc);
                anchor.setTag("");
            }
            //TTSNotification.showLast();
            //DragingDialogs.textToSpeachDialog(anchor, dc);
            return true;
        }

        if (keyCode == KeyEvent.KEYCODE_DPAD_DOWN) {
            dc.onScrollDown();
            return true;
        }
        if (keyCode == KeyEvent.KEYCODE_DPAD_UP) {
            dc.onScrollUp();
            return true;
        }

        if (keyCode == 70) {
            dc.onZoomInc();
            return true;
        }

        if (keyCode == 69) {
            dc.onZoomDec();
            return true;
        }

//        if (PageImageState.get().hasSelectedWords()) {
//            dc.clearSelectedText();
//            return true;
//        }

        return false;

    }

    public void closeAndRunList() {
        EventBus.getDefault().unregister(this);

        if (handler != null) {
            handler.removeCallbacksAndMessages(null);
        }

        if (titleBar != null) {
            titleBar.removeCallbacks(null);
        }
        dc.saveCurrentPageAsync();
        dc.onCloseActivityAdnShowInterstial();
        dc.closeActivity();

    }

    public void updateSpeedLabel() {

        Info info = OutlineHelper.getForamtingInfo(dc, true);

        maxSeek.setText(info.textPage);
        currentSeek.setText(info.textMax);
        pagesCountIndicator.setText(info.chText);

        currentSeek.setContentDescription(dc.getString(R.string.m_current_page) + " " + info.textMax);
        maxSeek.setContentDescription(dc.getString(R.string.m_total_pages) + " " + info.textPage);
    }

    public void updateUI() {
        final int max = dc.getPageCount();
        final int current = dc.getCurentPage();

        updateSpeedLabel();

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
        if (true || dc.isMusicianMode()) {
            reverseKeysIndicator.setVisibility(View.GONE);
        }

        moveLeft.setVisibility(Dips.isSmallScreen() && Dips.isVertical() ? View.GONE : View.VISIBLE);
        moveRight.setVisibility(Dips.isSmallScreen() && Dips.isVertical() ? View.GONE : View.VISIBLE);
        zoomPlus.setVisibility(Dips.isSmallScreen() && Dips.isVertical() ? View.GONE : View.VISIBLE);
        zoomMinus.setVisibility(Dips.isSmallScreen() && Dips.isVertical() ? View.GONE : View.VISIBLE);

        if (dc.isTextFormat()) {
            moveLeft.setVisibility(View.GONE);
            moveRight.setVisibility(View.GONE);
            zoomPlus.setVisibility(View.GONE);
            zoomMinus.setVisibility(View.GONE);
            crop.setVisibility(View.GONE);
            cut.setVisibility(View.GONE);
            onModeChange.setVisibility(View.GONE);
            if (Dips.isEInk() || AppState.get().appTheme == AppState.THEME_INK || AppState.get().isEnableBC) {
                onBC.setVisibility(View.VISIBLE);
            } else {
                onBC.setVisibility(View.GONE);
            }
            if (AppSP.get().isCrop) {
                crop.setVisibility(View.VISIBLE);
            }
            if (AppSP.get().isCut) {
                cut.setVisibility(View.VISIBLE);
            }
        }

        crop.underline(AppSP.get().isCrop);
        cut.underline(AppSP.get().isCut);

        progressDraw.updateProgress(current - 1);

        if (AppState.get().inactivityTime > 0) {
            dc.getActivity().getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
            LOG.d("FLAG addFlags", "FLAG_KEEP_SCREEN_ON", "add", AppState.get().inactivityTime);
            handler.removeCallbacks(clearFlags);
            handler.postDelayed(clearFlags, TimeUnit.MINUTES.toMillis(AppState.get().inactivityTime));
        }

        if (AppState.get().isAutoScroll) {
            pagesBookmark.setVisibility(View.GONE);
        } else {
            pagesBookmark.setVisibility(View.VISIBLE);
        }


        dc.saveCurrentPage();
        //SharedBooks.save(bs);

        LOG.d("dc.floatingBookmark", dc.floatingBookmark);
        if (dc.floatingBookmark != null) {
            dc.floatingBookmark.p = dc.getPercentage();
            floatingBookmarkTextView.setText("{" + dc.getCurentPageFirst1() + "}");
            floatingBookmarkTextView.setVisibility(View.VISIBLE);

            BookmarksData.get().add(dc.floatingBookmark);
            showPagesHelper();
        } else {
            floatingBookmarkTextView.setVisibility(View.GONE);
        }

        try {
            if (!dc.isTextFormat()) {
                TempHolder.get().documentTitleBarHeight = documentTitleBar.getHeight();
            } else {
                TempHolder.get().documentTitleBarHeight = 0;
            }
        } catch (Exception e) {
            TempHolder.get().documentTitleBarHeight = 0;
        }

        final int dashHeight = Math.min(Dips.dpToPx(220), Dips.screenHeight() / 3);
        line1.getLayoutParams().height = dashHeight;
        line2.getLayoutParams().height = dashHeight;

        line1.setLayoutParams(line1.getLayoutParams());
        line2.setLayoutParams(line2.getLayoutParams());

    }

    public void hideShowPrevNext() {
        if (dc.isMusicianMode()) {
            if (AppState.get().isShowRectangularTapZones) {
                line1.setVisibility(View.VISIBLE);
                line2.setVisibility(View.VISIBLE);
            } else {
                line1.setVisibility(View.GONE);
                line2.setVisibility(View.GONE);

            }
        }
    }

    public void showChapter() {

        if (AppState.get().isShowPanelBookNameScrollMode) {
//            if (TxtUtils.isNotEmpty(dc.getCurrentChapter())) {
//                bookName.setText(bookTitle + " " + TxtUtils.LONG_DASH1 + " " + dc.getCurrentChapter().trim());
//                LOG.d("bookName.setText(1)", bookTitle);
//            } else {
//                bookName.setText(bookTitle);
//                LOG.d("bookName.setText(2)", bookTitle);
//            }
            bookName.setText(bookTitle);
            pagesCountIndicator.setGravity(Gravity.RIGHT);
            //bookName.setVisibility(View.VISIBLE);
            ((LinearLayout.LayoutParams) pagesCountIndicator.getLayoutParams()).weight = 0;
        } else {
            pagesCountIndicator.setGravity(Gravity.LEFT);
            ((LinearLayout.LayoutParams) pagesCountIndicator.getLayoutParams()).weight = 1;
            //bookName.setVisibility(View.GONE);
            bookName.setText("");
        }


    }

    public void updateLock() {
        // int mode = View.VISIBLE;

        if (AppSP.get().isLocked) {
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
//        if (AppState.get().l) {
//            TintUtil.setTintImageWithAlpha(moveCenter, Color.LTGRAY);
//        } else {
//            TintUtil.setTintImageWithAlpha(moveCenter, Color.WHITE);
//        }

    }

    public void showHideHistory() {
        linkHistory.setVisibility(dc.getLinkHistory().isEmpty() ? View.GONE : View.VISIBLE);
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

            AnchorHelper.setXY(anchorX, event.getX(), event.getY());
            AnchorHelper.setXY(anchorY, event.getX1(), event.getY1());

        }

    }

    public void initUI(final Activity a) {
        this.a = a;
        quickBookmark = a.getString(R.string.fast_bookmark);

        a.findViewById(R.id.showHypenLangPanel).setVisibility(View.GONE);

        parentParent = a.findViewById(R.id.parentParent);
        documentTitleBar = a.findViewById(R.id.document_title_bar);
        linkHistory = (ImageView) a.findViewById(R.id.linkHistory);
        linkHistory.setOnClickListener(onLinkHistory);

        menuLayout = a.findViewById(R.id.menuLayout);

        bottomBar = a.findViewById(R.id.bottomBar);
        imageMenuArrow = (ImageView) a.findViewById(R.id.imageMenuArrow);
        adFrame = a.findViewById(R.id.adFrame);

        seekBar = (SeekBar) a.findViewById(R.id.seekBar);
        seekBar.setAccessibilityDelegate(new View.AccessibilityDelegate());
        speedSeekBar = (SeekBar) a.findViewById(R.id.seekBarSpeed);
        seekSpeedLayot = a.findViewById(R.id.seekSpeedLayot);
        anchor = (FrameLayout) a.findViewById(R.id.anchor);

        anchorX = (ImageView) a.findViewById(R.id.anchorX);
        anchorY = (ImageView) a.findViewById(R.id.anchorY);

        floatingBookmarkTextView = a.findViewById(R.id.floatingBookmark);
        floatingBookmarkTextView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dc.floatingBookmark = null;
                onRefresh.run();
                onBookmarks.onClick(v);
            }
        });
        floatingBookmarkTextView.setOnLongClickListener(new OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
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
                EventBus.getDefault().post(new MessagePageXY(MessagePageXY.TYPE_SELECT_TEXT, dc.getCurentPage(), x, y, x1, y1));
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
                    final String text = AppState.get().selectedText;
                    DictsHelper.runIntent(dc.getActivity(), text);
                    dc.clearSelectedText();

                } else {
                    DragingDialogs.selectTextMenu(anchor, dc, true, updateUIRunnable);
                }

            }
        };

        touch1.setOnMoveFinish(onMoveFinish);
        touch2.setOnMoveFinish(onMoveFinish);

        touch1.setOnMove(onMoveAction);
        touch2.setOnMove(onMoveAction);

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

        moveCenter = a.findViewById(R.id.moveCenter);
        moveCenter.setOnClickListener(onMoveCenter);

        moveRight = a.findViewById(R.id.moveRight);
        moveRight.setOnClickListener(onMoveRight);

        ImageView brightness = (ImageView) a.findViewById(R.id.brightness);
        brightness.setOnClickListener(onSun);
        brightness.setImageResource(!AppState.get().isDayNotInvert ? R.drawable.glyphicons_232_sun : R.drawable.glyphicons_2_moon);

        // if (Dips.isEInk(dc.getActivity())) {
        // brightness.setVisibility(View.GONE);
        // AppState.get().isDayNotInvert = true;
        // }

        onBC = (UnderlineImageView) a.findViewById(R.id.onBC);
        onBC.setOnClickListener(onBCclick);
        onBC.underline(AppState.get().isEnableBC);

        a.findViewById(R.id.toPage).setOnClickListener(toPage);

        crop = (UnderlineImageView) a.findViewById(R.id.crop);
        crop.setOnClickListener(onCrop);
        crop.setOnLongClickListener(onCropLong);

        if (AppSP.get().isCut) {
            crop.setVisibility(View.GONE);
        }

        cut = (UnderlineImageView) a.findViewById(R.id.cut);
        cut.setOnClickListener(onCut);
        cut.setVisibility(View.GONE);

        onModeChange = (ImageView) a.findViewById(R.id.onModeChange);
        onModeChange.setOnClickListener(onModeChangeClick);
        onModeChange.setImageResource(AppSP.get().isCut ? R.drawable.glyphicons_page_split : R.drawable.glyphicons_two_page_one);

        View prefTop = a.findViewById(R.id.prefTop);
        prefTop.setOnClickListener(onPrefTop);

        fullscreen = (ImageView) a.findViewById(R.id.fullscreen);
        fullscreen.setOnClickListener(onFull);
        fullscreen.setImageResource(DocumentController.getFullScreenIcon(a, AppState.get().fullScreenMode));


        onCloseBook = a.findViewById(R.id.close);
        Apps.accessibilityButtonSize(onCloseBook);

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

        ttsActive = a.findViewById(R.id.ttsActive);
        ttsActive.setDC(dc);
        ttsActive.addOnDialogRunnable(new Runnable() {

            @Override
            public void run() {
                AppState.get().isEditMode = true;
                hideShow();
                DragingDialogs.textToSpeachDialog(anchor, dc);
            }
        });

        batteryIcon = (ImageView) a.findViewById(R.id.batteryIcon);
        clockIcon = (ImageView) a.findViewById(R.id.clockIcon);

        textToSpeach = (ImageView) a.findViewById(R.id.textToSpeach);
        textToSpeach.setOnClickListener(onTextToSpeach);
        textToSpeach.setOnLongClickListener(v -> {
            AlertDialogs.showTTSDebug(dc);
            hideShow();
            return true;
        });

        drawView = (DrawView) a.findViewById(R.id.drawView);

        View bookmarks = a.findViewById(R.id.onBookmarks);
        bookmarks.setOnClickListener(onBookmarks);
        bookmarks.setOnLongClickListener(onBookmarksLong);

        toastBrightnessText = (TextView) a.findViewById(R.id.toastBrightnessText);
        toastBrightnessText.setVisibility(View.GONE);
        TintUtil.setDrawableTint(toastBrightnessText.getCompoundDrawables()[0], Color.WHITE);

        TextView modeName = (TextView) a.findViewById(R.id.modeName);
        modeName.setText(AppState.get().nameVerticalMode);

        pagesCountIndicator = (TextView) a.findViewById(R.id.currentPageIndex);
        pagesCountIndicator.setVisibility(View.GONE);

        currentSeek = (TextView) a.findViewById(R.id.currentSeek);
        maxSeek = (TextView) a.findViewById(R.id.maxSeek);
        bookName = (TextView) a.findViewById(R.id.bookName);

        currentTime = (TextView) a.findViewById(R.id.currentTime);
        batteryLevel = (TextView) a.findViewById(R.id.currentBattery);

        currentSeek.setOnLongClickListener(new OnLongClickListener() {

            @Override
            public boolean onLongClick(View v) {
                Dialogs.showDeltaPage(anchor, dc, dc.getCurentPageFirst1(), updateUIRunnable);
                return true;
            }
        });
        maxSeek.setOnLongClickListener(new OnLongClickListener() {

            @Override
            public boolean onLongClick(View v) {
                Dialogs.showDeltaPage(anchor, dc, dc.getCurentPageFirst1(), updateUIRunnable);
                return true;
            }
        });

        View thumbnail = a.findViewById(R.id.thumbnail);
        thumbnail.setOnClickListener(onThumbnail);

        View bookMenu = a.findViewById(R.id.bookMenu);
        bookMenu.setOnClickListener(onItemMenu);
        modeName.setOnClickListener(onItemMenu);
        modeName.setOnLongClickListener(onCloseLongClick);
//        modeName.setOnLongClickListener(new OnLongClickListener() {
//
//            @Override
//            public boolean onLongClick(View v) {
//                dc.onChangeTextSelection();
//                AppState.get().isEditMode = false;
//                hideShow();
//                return true;
//            }
//        });

        progressDraw = (ProgressDraw) a.findViewById(R.id.progressDraw);

        AppState.get().isAutoScroll = false;

        ImageView recent = (ImageView) a.findViewById(R.id.onRecent);
        recent.setOnClickListener(onRecent);

        anchor.getViewTreeObserver().addOnGlobalLayoutListener(new OnGlobalLayoutListener() {

            @TargetApi(Build.VERSION_CODES.HONEYCOMB)
            @Override
            public void onGlobalLayout() {
                if (anchor.getVisibility() == View.VISIBLE || dc.isMusicianMode()) {
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

        TintUtil.setTintBgSimple(a.findViewById(R.id.menuLayout), AppState.get().transparencyUI);
        TintUtil.setTintBgSimple(a.findViewById(R.id.bottomBar1), AppState.get().transparencyUI);
        TintUtil.setBackgroundFillColorBottomRight(lirbiLogo, ColorUtils.setAlphaComponent(TintUtil.color, AppState.get().transparencyUI));
        tintSpeed();

        pageshelper = (LinearLayout) a.findViewById(R.id.pageshelper);
        musicButtonPanel = a.findViewById(R.id.musicButtonPanel);
        musicButtonPanel.setVisibility(View.GONE);

        pagesBookmark = a.findViewById(R.id.pagesBookmark);
        pagesBookmark.setOnClickListener(onBookmarks);
        pagesBookmark.setOnLongClickListener(onBookmarksLong);

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

        if (dc.isMusicianMode()) {
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
            modeName.setText(AppState.get().nameMusicianMode);
        }

        currentSeek.setVisibility(View.GONE);
        maxSeek.setVisibility(View.GONE);
        seekBar.setVisibility(View.INVISIBLE);

        hideShowPrevNext();
        dc.initAnchor(anchor);

    }

    public void updateSeekBarColorAndSize() {
        lirbiLogo.setText(AppState.get().musicText);
        // TintUtil.setBackgroundFillColorBottomRight(ttsActive,
        // ColorUtils.setAlphaComponent(TintUtil.color, 230));

        TintUtil.setTintText(bookName, TintUtil.getStatusBarColor());
        TintUtil.setTintImageWithAlpha(textToSpeachTop, TintUtil.getStatusBarColor());
        TintUtil.setTintImageWithAlpha(lockUnlockTop, TintUtil.getStatusBarColor());
        TintUtil.setTintImageWithAlpha(nextScreenType, TintUtil.getStatusBarColor());
        TintUtil.setTintText(pagesCountIndicator, TintUtil.getStatusBarColor());
        TintUtil.setTintText(currentTime, TintUtil.getStatusBarColor());
        TintUtil.setTintText(batteryLevel, TintUtil.getStatusBarColor());
        TintUtil.setTintText(reverseKeysIndicator, ColorUtils.setAlphaComponent(TintUtil.getStatusBarColor(), 200));

        TintUtil.setTintImageWithAlpha(goToPage1Top, TintUtil.getStatusBarColor());
        TintUtil.setTintImageWithAlpha((ImageView) closeTop, TintUtil.getStatusBarColor());
        TintUtil.setTintImageWithAlpha(toolBarButton, TintUtil.getStatusBarColor());
        TintUtil.setTintImageWithAlpha(clockIcon, TintUtil.getStatusBarColor()).setAlpha(200);
        TintUtil.setTintImageWithAlpha(batteryIcon, TintUtil.getStatusBarColor()).setAlpha(200);

        int titleColor = AppState.get().isDayNotInvert ? MagicHelper.otherColor(AppState.get().colorDayBg, -0.05f) : MagicHelper.otherColor(AppState.get().colorNigthBg, 0.05f);
        titleBar.setBackgroundColor(titleColor);

        int progressColor = AppState.get().isDayNotInvert ? AppState.get().statusBarColorDay : MagicHelper.otherColor(AppState.get().statusBarColorNight, +0.2f);
        progressDraw.updateColor(progressColor);
        progressDraw.getLayoutParams().height = Dips.dpToPx(AppState.get().progressLineHeight);
        progressDraw.requestLayout();

        // textSize
        bookName.setTextSize(AppState.get().statusBarTextSizeAdv);
        pagesCountIndicator.setTextSize(AppState.get().statusBarTextSizeAdv);
        currentTime.setTextSize(AppState.get().statusBarTextSizeAdv);
        batteryLevel.setTextSize(AppState.get().statusBarTextSizeAdv);
        reverseKeysIndicator.setTextSize(AppState.get().statusBarTextSizeAdv);
        lirbiLogo.setTextSize(AppState.get().statusBarTextSizeAdv);

        int iconSize = Dips.spToPx(AppState.get().statusBarTextSizeAdv);
        int smallIconSize = iconSize - Dips.dpToPx(5);

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
        BrightnessHelper.onMessegeBrightness(handler, msg, toastBrightnessText, overlay);
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
        DragingDialogs.editColorsPanel(anchor, dc, drawView, true);
    }

    public void doDoubleTap(int x, int y) {
        if (dc.isMusicianMode()) {
            if (AppState.get().doubleClickAction1 == AppState.DOUBLE_CLICK_ADJUST_PAGE) {
                dc.alignDocument();
            }
        } else {
            if (AppState.get().doubleClickAction1 == AppState.DOUBLE_CLICK_ZOOM_IN_OUT) {
                dc.onZoomInOut(x, y);
                AppState.get().isEditMode = false;
                hideShow();
            } else if (AppState.get().doubleClickAction1 == AppState.DOUBLE_CLICK_ADJUST_PAGE) {
                dc.alignDocument();
            } else if (AppState.get().doubleClickAction1 == AppState.DOUBLE_CLICK_CENTER_HORIZONTAL) {
                dc.centerHorizontal();
            } else if (AppState.get().doubleClickAction1 == AppState.DOUBLE_CLICK_AUTOSCROLL) {
                onAutoScrollClick();
            } else if (AppState.get().doubleClickAction1 == AppState.DOUBLE_CLICK_CLOSE_BOOK) {
                closeAndRunList();
            } else if (AppState.get().doubleClickAction1 == AppState.DOUBLE_CLICK_CLOSE_HIDE_APP) {
                Apps.showDesctop(a);
            } else if (AppState.get().doubleClickAction1 == AppState.DOUBLE_CLICK_START_STOP_TTS) {
                TTSService.playPause(dc.getActivity(), dc);

            } else if (AppState.get().doubleClickAction1 == AppState.DOUBLE_CLICK_CLOSE_BOOK_AND_APP) {
                dc.onCloseActivityFinal(new Runnable() {

                    @Override
                    public void run() {
                        MainTabs2.closeApp(dc.getActivity());
                    }
                });
                dc.closeActivity();
            }
        }
    }

    public void doShowHideWrapperControlls() {
        AppState.get().isEditMode = !AppState.get().isEditMode;
        hideShow();
        Keyboards.invalidateEink(parentParent);

    }

    public void showHideHavigationBar() {
        if (!AppState.get().isEditMode && AppState.get().fullScreenMode == AppState.FULL_SCREEN_FULLSCREEN) {
            Keyboards.hideNavigation(a);
        }
    }

    public void doChooseNextType(View view) {
        final MyPopupMenu popupMenu = new MyPopupMenu(view.getContext(), view);

        String pages = dc.getString(R.string.by_pages);
        String screen = dc.getString(R.string.of_screen).toLowerCase(Locale.US);
        String screens = dc.getString(R.string.by_screans);
        final List<Integer> values = Arrays.asList(AppState.NEXT_SCREEN_SCROLL_BY_PAGES, 100, 95, 75, 50, 25, 10);

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
                    Keyboards.hideNavigation(dc.getActivity());
                    return false;
                }
            });
        }

        popupMenu.getMenu().add(R.string.custom_value).setOnMenuItemClickListener(new OnMenuItemClickListener() {

            @Override
            public boolean onMenuItemClick(MenuItem item) {
                Activity a = dc.getActivity();
                final AlertDialog.Builder builder = new AlertDialog.Builder(a);
                builder.setTitle(R.string.custom_value);

                final CustomSeek myValue = new CustomSeek(a);
                myValue.init(1, 100, AppState.get().nextScreenScrollMyValue);
                myValue.setOnSeekChanged(new IntegerResponse() {

                    @Override
                    public boolean onResultRecive(int result) {
                        AppState.get().nextScreenScrollMyValue = result;
                        myValue.setValueText(AppState.get().nextScreenScrollMyValue + "%");
                        return false;
                    }
                });
                myValue.setValueText(AppState.get().nextScreenScrollMyValue + "%");

                builder.setView(myValue);

                builder.setPositiveButton(R.string.apply, new OnClickListener() {

                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        AppState.get().nextScreenScrollBy = AppState.get().nextScreenScrollMyValue;
                        initNextType();
                        Keyboards.hideNavigation(dc.getActivity());

                    }
                });
                builder.setNegativeButton(R.string.cancel, new OnClickListener() {

                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                    }
                });

                builder.show();

                return false;
            }

        });

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

        batteryLevel.setVisibility(AppState.get().isShowBattery ? View.VISIBLE : View.GONE);
        batteryIcon.setVisibility(AppState.get().isShowBattery ? View.VISIBLE : View.GONE);

        currentTime.setVisibility(AppState.get().isShowTime ? View.VISIBLE : View.GONE);
        clockIcon.setVisibility(AppState.get().isShowTime ? View.VISIBLE : View.GONE);

    }

    public void initNextType() {
        if (AppState.get().nextScreenScrollBy == AppState.NEXT_SCREEN_SCROLL_BY_PAGES) {
            nextTypeBootom.setText(R.string.by_pages);
            nextScreenType.setImageResource(R.drawable.glyphicons_full_page);

        } else {
            if (AppState.get().nextScreenScrollBy == 100) {
                nextTypeBootom.setText(dc.getString(R.string.by_screans));
            } else {
                nextTypeBootom.setText(AppState.get().nextScreenScrollBy + "% " + dc.getString(R.string.of_screen));
            }
            nextScreenType.setImageResource(R.drawable.glyphicons_halp_page);

        }

    }

    public void hideShow() {
        if (AppState.get().isEnableAccessibility) {
            AppState.get().isEditMode = true;
        }

        if (AppState.get().isEditMode) {
            DocumentController.turnOnButtons(a);
            show();
        } else {
            DocumentController.turnOffButtons(a);

            hide();
        }
        initToolBarPlusMinus();

        if (AppState.get().isAutoScroll) {
            autoScroll.setImageResource(R.drawable.glyphicons_37_file_pause);
        } else {
            autoScroll.setImageResource(R.drawable.glyphicons_37_file_play);
        }

        if (dc.isMusicianMode()) {
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

        if (dc.isMusicianMode()) {
            lirbiLogo.setVisibility(View.VISIBLE);
        } else {
            lirbiLogo.setVisibility(View.GONE);
        }

        // hideSeekBarInReadMode();
        // showHideHavigationBar();
        DocumentController.chooseFullScreen(dc.getActivity(), AppState.get().fullScreenMode);
        showPagesHelper();


        //try eink fix

    }

    public void hide() {
        menuLayout.setVisibility(View.GONE);
        bottomBar.setVisibility(View.GONE);
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

    public void show() {
        menuLayout.setVisibility(View.VISIBLE);

        titleBar.setVisibility(View.VISIBLE);

        updateLock();

        bottomBar.setVisibility(View.VISIBLE);
        adFrame.setVisibility(View.VISIBLE);
        adFrame.setClickable(true);
        adFrame.setTag(null);

        imageMenuArrow.setImageResource(android.R.drawable.arrow_up_float);


        // if (AppState.get().isAutoScroll &&
        // AppState.get().isEditMode) {
        // seekSpeedLayot.setVisibility(View.VISIBLE);
        // }

    }

    public void showSearchDialog() {
        if (AppSP.get().isCut) {
            onModeChange.setImageResource(R.drawable.glyphicons_two_page_one);
            AppSP.get().isCut = !false;
            onCut.onClick(null);
        }
        if (AppSP.get().isCrop) {
            onCrop.onClick(null);
        }

        DragingDialogs.searchMenu(anchor, dc, "");
    }

    public void onAutoScrollClick() {
        if (dc.isVisibleDialog()) {
            return;
        }

        AppState.get().isAutoScroll = !AppState.get().isAutoScroll;
        // changeAutoScrollButton();
        dc.onAutoScroll();
        updateUI();
    }

    private boolean closeDialogs() {
        return dc.closeDialogs();
    }

    public void hideAds() {
        adFrame.setTag("");
        adFrame.setVisibility(View.GONE);
    }

    public void nextChose(boolean animate) {
        nextChose(animate, 0);
    }

    public void nextChose(boolean animate, int repeatCount) {
        LOG.d("nextChose");
        dc.checkReadingTimer();

        if (AppState.get().isEditMode) {
            AppState.get().isEditMode = false;
        }

        if (AppState.get().nextScreenScrollBy == AppState.NEXT_SCREEN_SCROLL_BY_PAGES) {
            dc.onNextPage(animate);
        } else {
            if (AppState.get().nextScreenScrollBy <= 50 && repeatCount == 0) {
                animate = true;
            }
            dc.onNextScreen(animate);
        }

        //updateUI();

    }

    public void prevChose(boolean animate) {
        prevChose(animate, 0);
    }

    public void prevChose(boolean animate, int repeatCount) {
        dc.checkReadingTimer();

        if (AppState.get().isEditMode) {
            AppState.get().isEditMode = false;
        }

        if (AppState.get().nextScreenScrollBy == AppState.NEXT_SCREEN_SCROLL_BY_PAGES) {
            dc.onPrevPage(animate);
        } else {
            if (AppState.get().nextScreenScrollBy <= 50 && repeatCount == 0) {
                animate = true;
            }
            dc.onPrevScreen(animate);
        }

        //updateUI();
    }

    public void setTitle(final String title) {
        this.bookTitle = title;

        hideShowEditIcon();

    }

    public void hideShowEditIcon() {
        if (dc != null && !BookType.PDF.is(dc.getCurrentBook().getPath())) {
            editTop2.setVisibility(View.GONE);
        } else if (AppSP.get().isCrop || AppSP.get().isCut) {
            editTop2.setVisibility(View.GONE);
        } else {
            boolean passwordProtected = dc.isPasswordProtected();
            LOG.d("passwordProtected", passwordProtected);
            if (dc != null && passwordProtected) {
                editTop2.setVisibility(View.GONE);
            } else {
                if (LibreraApp.MUPDF_VERSION == AppsConfig.MUPDF_1_11) {
                    editTop2.setVisibility(View.VISIBLE);
                } else {
                    editTop2.setVisibility(View.VISIBLE);
                }
            }
        }

    }

    public DocumentController getController() {
        return dc;
    }

    public DrawView getDrawView() {
        return drawView;
    }

    public void showHelp() {
        if (AppSP.get().isFirstTimeVertical) {
            handler.postDelayed(new Runnable() {

                @Override
                public void run() {
                    AppSP.get().isFirstTimeVertical = false;
                    AppState.get().isEditMode = true;
                    hideShow();
                    Views.showHelpToast(lockUnlock);

                }
            }, 1000);
        }
    }

    public void showPagesHelper() {
        try {
            BookmarkPanel.showPagesHelper(pageshelper, musicButtonPanel, dc, pagesBookmark, quickBookmark, onRefresh);
        } catch (Exception e) {
            LOG.e(e);
        }
    }

    public void showOutline(final List<OutlineLinkWrapper> list, final int count) {
        try {
            dc.activity.runOnUiThread(new Runnable() {

                @Override
                public void run() {
                    Apps.accessibilityText(a, a.getString(R.string.book_is_open), a.getString(R.string.m_current_page), " " + dc.getCurentPageFirst1());

                    progressDraw.updateDivs(list);
                    progressDraw.updatePageCount(dc.getPageCount() - 1);
                    titleBar.setOnTouchListener(new HorizontallSeekTouchEventListener(onSeek, dc.getPageCount(), false));
                    progressDraw.setOnTouchListener(new HorizontallSeekTouchEventListener(onSeek, dc.getPageCount(), false));
                    if (TxtUtils.isListEmpty(list)) {
                        TintUtil.setTintImageWithAlpha(onDocDontext, Color.LTGRAY);
                    }

                    if (ExtUtils.isNoTextLayerForamt(dc.getCurrentBook().getPath())) {
                        TintUtil.setTintImageWithAlpha(textToSpeach, Color.LTGRAY);
                    }
                    if (dc.isTextFormat()) {
                        // TintUtil.setTintImage(lockUnlock, Color.LTGRAY);
                    }

                    currentSeek.setVisibility(View.VISIBLE);
                    maxSeek.setVisibility(View.VISIBLE);
                    seekBar.setVisibility(View.VISIBLE);

                    onCloseBook.setVisibility(View.VISIBLE);
                    pagesCountIndicator.setVisibility(View.VISIBLE);

                    showHelp();

                    hideShowEditIcon();

                    updateSpeedLabel();

                    DialogsPlaylist.dispalyPlaylist(a, dc);
                    HypenPanelHelper.init(parentParent, dc);


                    showPagesHelper();

                }
            });
        } catch (Exception e) {
            LOG.e(e);
        }

    }

    public void onResume() {
        LOG.d("DocumentWrapperUI", "onResume");
        handlerTimer.post(updateTimePower);

        if (dc != null) {
            dc.goToPageByTTS();
        }

        if (ttsActive != null) {
            ttsActive.setVisibility(TxtUtils.visibleIf(TTSEngine.get().isTempPausing()));
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

    public void onConfigChanged() {
        try {
            updateUI();
        } catch (Exception e) {
            LOG.e(e);
        }
    }

    public void onLoadBookFinish() {
        onCloseBook.setVisibility(View.VISIBLE);
    }
}
