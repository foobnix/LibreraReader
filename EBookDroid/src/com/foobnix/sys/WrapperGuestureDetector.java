package com.foobnix.sys;

import org.ebookdroid.BookType;
import org.ebookdroid.common.settings.SettingsManager;
import org.ebookdroid.common.touch.IGestureDetector;
import org.ebookdroid.common.touch.IMultiTouchListener;
import org.ebookdroid.common.touch.TouchManager;
import org.ebookdroid.core.AbstractViewController;
import org.ebookdroid.core.codec.Annotation;

import com.foobnix.android.utils.Dips;
import com.foobnix.android.utils.LOG;
import com.foobnix.android.utils.TxtUtils;
import com.foobnix.pdf.info.ExtUtils;
import com.foobnix.pdf.info.model.AnnotationType;
import com.foobnix.pdf.info.wrapper.AppState;
import com.foobnix.pdf.info.wrapper.DocumentController;

import android.content.Context;
import android.graphics.Rect;
import android.os.Vibrator;
import android.view.GestureDetector.SimpleOnGestureListener;
import android.view.MotionEvent;

public class WrapperGuestureDetector extends SimpleOnGestureListener implements IMultiTouchListener {

    private final AbstractViewController avc;
    private final DocumentController docCtrl;
    private boolean isTextFormat;

    private boolean isLongMovement = false;

    ClickUtils clickUtils;

    public WrapperGuestureDetector(final AbstractViewController avc, final DocumentController listener) {
        this.avc = avc;

        this.docCtrl = listener;
        try {
            String path = avc.base.getListener().getCurrentBook().getPath();
            isTextFormat = ExtUtils.isTextFomat(path) && !BookType.TXT.is(path);
        } catch (Exception e) {
            LOG.e(e);
        }

        contex = avc.getView().getView().getContext();

        clickUtils = new ClickUtils();
        updateBorders();
    }

    public void updateBorders() {
        clickUtils.initMusician();
    }

    public IGestureDetector innerDetector = new IGestureDetector() {

        float x, y;

        @Override
        public boolean onTouchEvent(MotionEvent ev) {
            if (ev.getAction() == MotionEvent.ACTION_DOWN) {
                x = ev.getX();
                y = ev.getY();
            }
            if (ev.getAction() == MotionEvent.ACTION_UP) {
                if (isLongMovement) {
                    if (TxtUtils.isNotEmpty(AppState.get().selectedText)) {
                        docCtrl.onLongPress(ev);
                    }
                }
                isLongMovement = false;
            }
            int delta = Dips.dpToPx(15);
            if (isLongMovement && (Math.abs(y - ev.getY()) > delta || Math.abs(x - ev.getX()) > delta)) {
                AppState.get().selectedText = avc.processLongTap(false, null, ev);
                x = ev.getX();
                y = ev.getY();
            }
            return false;
        }

        @Override
        public boolean enabled() {
            return true;
        }
    };

    /**
     * {@inheritDoc}
     * 
     * @see android.view.GestureDetector.SimpleOnGestureListener#onDoubleTap(android.view.MotionEvent)
     */
    @Override
    public boolean onDoubleTap(final MotionEvent e) {
        if (clickUtils.isClickCenter(e.getX(), e.getY())) {
            docCtrl.onDoubleTap((int) e.getX(), (int) e.getY());
            // listener.onZoomInc();
        }
        return true;
    }

    private boolean isScrollFinished = true;
    private Context contex;

    @Override
    public boolean onDown(final MotionEvent e) {
        isScrollFinished = avc.getView().getScroller().isFinished();
        if (!isScrollFinished) {
            avc.getView().getScroller().forceFinished(true);
        }

        clickUtils.initMusician();

        return true;
    }

    Annotation annotation;

    @Override
    public boolean onSingleTapConfirmed(final MotionEvent e) {
        if (isScrollFinished) {

            if (alowConfirm && clickUtils.isClickCenter(e.getX(), e.getY())) {
                docCtrl.onSingleTap();
            }
        }
        return true;
    }

    boolean alowConfirm = false;

    @Override
    public boolean onSingleTapUp(final MotionEvent e) {
        if (isScrollFinished) {// && !AppState.get().isEditPanelVisible) {
            // if (isScrollFinished && AppState.getInstance().isLocked) {
            updateBorders();
            if (!AppState.get().isMusicianMode && !AppState.get().isIgnoreAnnotatations || AppState.getInstance().editWith == AppState.EDIT_DELETE) {
                alowConfirm = false;
                Annotation annotation2 = avc.isAnnotationTap(e.getX(), e.getY());

                if (annotation2 == null && annotation != null) {
                    annotation = null;
                    avc.selectAnnotation(null);
                    return true;
                }
                annotation = annotation2;

                if (annotation != null) {
                    avc.selectAnnotation(annotation);
                    if (AppState.getInstance().editWith == AppState.EDIT_DELETE) {
                        docCtrl.onAnnotationTap(annotation.getPageHandler(), annotation.getPage(), annotation.getIndex());
                        avc.selectAnnotation(null);
                        annotation = null;
                    } else {
                        if (annotation.type == AnnotationType.TEXT) {
                            docCtrl.showAnnotation(annotation);
                        } else {
                            docCtrl.showEditDialogIfNeed();
                        }
                    }
                    return true;
                }

                if (isTextFormat) {
                    String text = avc.processLongTap(true, e, e);
                    if (TxtUtils.isFooterNote(text)) {
                        AppState.get().selectedText = text;
                        // TempHolder.get().linkPage = avc.getLinkPage(e.getX(),
                        // e.getY());
                        docCtrl.onLongPress(e);
                        return false;
                    }
                    if (text != null) {
                        docCtrl.clearSelectedText();
                        docCtrl.closeFooterNotesDialog();
                    }
                }

                if (AppState.get().selectedText != null) {
                    docCtrl.clearSelectedText();
                    docCtrl.closeFooterNotesDialog();
                    AppState.get().selectedText = null;
                    return true;
                }
            }
            if (!AppState.get().isMusicianMode) {
                boolean processTap = avc.processTap(TouchManager.Touch.SingleTap, e);
                if (processTap) {
                    return false;
                }
            }
            if (clickUtils.isClickRight(e.getX(), e.getY())) {
                docCtrl.onRightPress();

            } else if (clickUtils.isClickLeft(e.getX(), e.getY())) {
                docCtrl.onLeftPress();

            } else if (clickUtils.isClickTop(e.getX(), e.getY())) {
                docCtrl.onTopPress();

            } else if (clickUtils.isClickBottom(e.getX(), e.getY())) {
                docCtrl.onBottomPress();

            } else if (clickUtils.isClickCenter(e.getX(), e.getY())) {
                // docCtrl.onSingleTap();
                alowConfirm = true;
                return false;
            }

        }
        return true;
    }

    /**
     * {@inheritDoc}
     * 
     * @see android.view.GestureDetector.SimpleOnGestureListener#onFling(android.view.MotionEvent,
     *      android.view.MotionEvent, float, float)
     */
    @Override
    public boolean onFling(final MotionEvent e1, final MotionEvent e2, final float vX, final float vY) {
        if (AppState.get().isMusicianMode) {
            return false;
        }
        final Rect l = avc.getScrollLimits();
        float x = vX, y = vY;
        if (Math.abs(vX / vY) < 0.5) {
            x = 0;
        }
        if (Math.abs(vY / vX) < 0.5) {
            y = 0;
        }
        if (isNoLock()) {
            avc.getView().startFling(x, y, l);
            avc.getView().redrawView();
        } else {
            avc.getView().startFling(0, y, l);
            avc.getView().redrawView();

        }
        return true;
    }

    /**
     * {@inheritDoc}
     * 
     * @see android.view.GestureDetector.SimpleOnGestureListener#onScroll(android.view.MotionEvent,
     *      android.view.MotionEvent, float, float)
     */
    @Override
    public boolean onScroll(final MotionEvent e1, final MotionEvent e2, final float distanceX, final float distanceY) {
        final float x = distanceX, y = distanceY;

        if (isNoLock()) {
            // if (!AppState.get().isMusicianMode ||
            // (AppState.get().isMusicianMode && isClickRigth(e1))) {
            avc.getView().scrollBy((int) x, (int) y);
            // }
        } else {
            // if (!AppState.get().isMusicianMode ||
            // (AppState.get().isMusicianMode && isClickRigth(e1))) {
            avc.getView().scrollBy(0, (int) y);
            // }

        }
        return true;
    }

    private boolean isNoLock() {
        // return AppState.XgetInstance().isEditMode() ||
        // !AppState.getInstance().isLocked;
        return !AppState.getInstance().isLocked;
    }

    /**
     * {@inheritDoc}
     * 
     * @see android.view.GestureDetector.SimpleOnGestureListener#onLongPress(android.view.MotionEvent)
     */
    @Override
    public void onLongPress(final MotionEvent e) {
        isLongMovement = true;

        if (!AppState.get().longTapEnable) {
            return;
        }

        if (SettingsManager.getBookSettings() != null && SettingsManager.getBookSettings().cropPages) {
            docCtrl.onCrop();
        }
        AppState.get().selectedText = avc.processLongTap(true, e, e);
        if (AppState.get().isVibration) {
            Vibrator v = (Vibrator) avc.getBase().getContext().getSystemService(Context.VIBRATOR_SERVICE);
            v.vibrate(50);
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.ebookdroid.common.touch.IMultiTouchListener#onTwoFingerPinch(float,
     *      float)
     */

    @Override
    public void onTwoFingerPinch(final MotionEvent e, final float oldDistance, final float newDistance) {
        final float factor = (float) Math.sqrt(newDistance / oldDistance);
        if (isNoLock()) {
            avc.base.getZoomModel().scaleZoom(factor);
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.ebookdroid.common.touch.IMultiTouchListener#onTwoFingerPinchEnd()
     */
    @Override
    public void onTwoFingerPinchEnd(final MotionEvent e) {
        if (isNoLock()) {
            avc.base.getZoomModel().commit();
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.ebookdroid.common.touch.IMultiTouchListener#onTwoFingerTap()
     */
    @Override
    public void onTwoFingerTap(final MotionEvent e) {
        if (isScrollFinished && clickUtils.isClickCenter(e.getX(), e.getY()) && avc.getView().getScroller().isFinished()) {
            docCtrl.onSingleTap();
        }
    }

}
