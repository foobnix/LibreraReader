package org.ebookdroid.core;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import org.ebookdroid.common.settings.AppSettings;
import org.ebookdroid.common.settings.SettingsManager;
import org.ebookdroid.common.settings.books.BookSettings;
import org.ebookdroid.common.settings.types.DocumentViewMode;
import org.ebookdroid.common.settings.types.PageAlign;
import org.ebookdroid.common.settings.types.PageType;
import org.ebookdroid.common.touch.DefaultGestureDetector;
import org.ebookdroid.common.touch.IGestureDetector;
import org.ebookdroid.common.touch.IMultiTouchListener;
import org.ebookdroid.common.touch.MultiTouchGestureDetector;
import org.ebookdroid.common.touch.TouchManager;
import org.ebookdroid.common.touch.TouchManager.Touch;
import org.ebookdroid.core.codec.Annotation;
import org.ebookdroid.core.codec.PageLink;
import org.ebookdroid.core.models.DocumentModel;
import org.ebookdroid.droids.mupdf.codec.TextWord;
import org.ebookdroid.ui.viewer.IActivityController;
import org.ebookdroid.ui.viewer.IView;
import org.ebookdroid.ui.viewer.IViewController;
import org.emdev.ui.actions.AbstractComponentController;
import org.emdev.ui.actions.ActionEx;
import org.emdev.ui.actions.params.Constant;
import org.emdev.ui.progress.IProgressIndicator;
import org.emdev.utils.LengthUtils;

import com.foobnix.android.utils.LOG;
import com.foobnix.android.utils.TxtUtils;
import com.foobnix.pdf.info.R;
import com.foobnix.pdf.info.view.AlertDialogs;
import com.foobnix.pdf.info.wrapper.AppState;
import com.foobnix.sys.AdvGuestureDetector;
import com.foobnix.sys.TempHolder;

import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.RectF;
import android.view.GestureDetector.SimpleOnGestureListener;
import android.view.MotionEvent;

public abstract class AbstractViewController extends AbstractComponentController<IView> implements IViewController {

    public static final int DOUBLE_TAP_TIME = 500;

    public final IActivityController base;

    public final DocumentModel model;

    public final DocumentViewMode mode;

    protected volatile boolean isInitialized = false;

    protected boolean isShown = false;

    protected final AtomicBoolean inZoom = new AtomicBoolean();

    protected final AtomicBoolean inQuickZoom = new AtomicBoolean();

    protected final PageIndex pageToGo;

    protected int firstVisiblePage;

    protected int lastVisiblePage;

    protected boolean layoutLocked;

    private List<IGestureDetector> detectors;

    public AbstractViewController(final IActivityController base, final DocumentViewMode mode) {
        super(base, base.getView());

        this.base = base;
        this.mode = mode;
        this.model = base.getDocumentModel();

        this.firstVisiblePage = -1;
        this.lastVisiblePage = -1;

        this.pageToGo = SettingsManager.getBookSettings().getCurrentPage();

        createAction(R.id.adFrame, new Constant("direction", -1));
        createAction(R.id.adFrame, new Constant("direction", +1));

    }

    protected List<IGestureDetector> getGestureDetectors() {
        if (detectors == null) {
            detectors = initGestureDetectors(new ArrayList<IGestureDetector>());
        }
        return detectors;
    }

    protected List<IGestureDetector> initGestureDetectors(final List<IGestureDetector> list) {
        final AdvGuestureDetector listener = new AdvGuestureDetector(this, base.getListener());
        list.add(listener.innerDetector);
        list.add(new MultiTouchGestureDetector(listener));
        list.add(new DefaultGestureDetector(base.getContext(), listener));
        return list;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.ebookdroid.ui.viewer.IViewController#getView()
     */
    @Override
    public final IView getView() {
        return base.getView();
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.ebookdroid.ui.viewer.IViewController#getBase()
     */
    @Override
    public final IActivityController getBase() {
        return base;
    }

    @Override
    public final void init(final IProgressIndicator task) {
        if (!isInitialized) {
            try {
                model.initPages(base, task);
            } finally {
                isInitialized = true;
            }
        }
    }

    @Override
    public boolean isInitialized() {
        return isInitialized;
    }

    /**
     * 
     */
    @Override
    public final void onDestroy() {
        // isShown = false;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.ebookdroid.ui.viewer.IViewController#show()
     */
    @Override
    public final void show() {
        if (!isInitialized) {
            return;
        }
        if (!isShown) {
            isShown = true;

            invalidatePageSizes(InvalidateSizeReason.INIT, null);

            final BookSettings bs = SettingsManager.getBookSettings();
            final Page page = pageToGo.getActualPage(model, bs);
            final int toPage = page != null ? page.index.viewIndex : 0;

            goToPage(toPage, bs.offsetX, bs.offsetY);

        }
    }

    protected final void updatePosition(final Page page, final ViewState viewState) {
        if (page != null) {
            final PointF pos = viewState.getPositionOnPage(page);
            SettingsManager.positionChanged(pos.x, pos.y);
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.ebookdroid.core.events.ZoomListener#zoomChanged(float, float,
     *      boolean)
     */
    @Override
    public final void zoomChanged(final float oldZoom, final float newZoom, final boolean committed) {
        if (!isShown) {
            return;
        }

        inZoom.set(!committed);
        EventPool.newEventZoom(this, oldZoom, newZoom, committed).process();

        if (!committed) {
            inQuickZoom.set(false);
        }
    }

    public final void quickZoom(final ActionEx action) {
        if (inZoom.get()) {
            return;
        }
        float zoomFactor = 2.0f;
        if (inQuickZoom.compareAndSet(true, false)) {
            zoomFactor = 1.0f / zoomFactor;
        } else {
            inQuickZoom.set(true);
        }
        base.getZoomModel().scaleAndCommitZoom(zoomFactor);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.ebookdroid.ui.viewer.IViewController#updateMemorySettings()
     */
    @Override
    public final void updateMemorySettings() {
        EventPool.newEventReset(this, null, false).process();
    }

    public final int getScrollX() {
        return getView().getScrollX();
    }

    public final int getWidth() {
        return getView().getWidth();
    }

    public final int getScrollY() {
        return getView().getScrollY();
    }

    public final int getHeight() {
        return getView().getHeight();
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.ebookdroid.ui.viewer.IViewController#onTouchEvent(android.view.MotionEvent)
     */
    @Override
    public final boolean onTouchEvent(final MotionEvent ev) {
        if (true) {
            final int delay = AppSettings.getInstance().touchProcessingDelay;
            if (delay > 0) {
                try {
                    Thread.sleep(Math.min(200, delay));
                } catch (final InterruptedException e) {
                    Thread.interrupted();
                }
            }
        }

        for (final IGestureDetector d : getGestureDetectors()) {
            if (d.enabled() && d.onTouchEvent(ev)) {
                return true;
            }
        }

        return false;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.ebookdroid.ui.viewer.IViewController#onLayoutChanged(boolean,
     *      boolean, android.graphics.Rect, android.graphics.Rect)
     */
    @Override
    public boolean onLayoutChanged(final boolean layoutChanged, final boolean layoutLocked, final Rect oldLaout, final Rect newLayout) {
        if (layoutChanged && !layoutLocked) {
            if (isShown) {
                EventPool.newEventReset(this, InvalidateSizeReason.LAYOUT, true).process();
                return true;
            }
        }
        return false;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.ebookdroid.ui.viewer.IViewController#toggleRenderingEffects()
     */
    @Override
    public final void toggleRenderingEffects() {
        EventPool.newEventReset(this, null, true).process();
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.ebookdroid.ui.viewer.IViewController#invalidateScroll()
     */
    @Override
    public final void invalidateScroll() {
        if (!isShown) {
            return;
        }
        getView().invalidateScroll();
    }

    /**
     * Sets the page align flag.
     * 
     * @param align
     *            the new flag indicating align
     */
    @Override
    public final void setAlign(final PageAlign align) {
        EventPool.newEventReset(this, InvalidateSizeReason.PAGE_ALIGN, false).process();
    }

    /**
     * Checks if view is initialized.
     * 
     * @return true, if is initialized
     */
    protected final boolean isShown() {
        return isShown;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.ebookdroid.ui.viewer.IViewController#getFirstVisiblePage()
     */
    @Override
    public final int getFirstVisiblePage() {
        return firstVisiblePage;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.ebookdroid.ui.viewer.IViewController#getLastVisiblePage()
     */
    @Override
    public final int getLastVisiblePage() {
        return lastVisiblePage;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.ebookdroid.ui.viewer.IViewController#redrawView()
     */
    @Override
    public final void redrawView() {
        getView().redrawView(new ViewState(this));
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.ebookdroid.ui.viewer.IViewController#redrawView(org.ebookdroid.core.ViewState)
     */
    @Override
    public final void redrawView(final ViewState viewState) {
        getView().redrawView(viewState);
    }

    public final void verticalConfigScroll(final ActionEx action) {
        final Integer direction = action.getParameter("direction");
        verticalConfigScroll(direction);
    }

    float xLong;
    float yLong;

    @Override
    public void clearSelectedText() {
        for (final Page page : model.getPages()) {
            page.selectedText.clear();
        }
        redrawView();
    }

    public final String processLongTap(boolean single, final MotionEvent e1, final MotionEvent e2, boolean draw) {
        if (e1 != null) {
            xLong = e1.getX();
            yLong = e1.getY();
        }

        float x2 = e2.getX();
        float y2 = e2.getY();

        final float zoom = base.getZoomModel().getZoom();

        final RectF tapRect = new RectF(xLong, yLong, x2, y2);
        if (yLong > y2) {
            tapRect.sort();
        }
        tapRect.offset(getScrollX(), getScrollY());

        StringBuilder build = new StringBuilder();

        boolean isHyphenWorld = false;

        LOG.d("Add Word page", "----", firstVisiblePage, lastVisiblePage + 1);
        for (final Page page : model.getPages(firstVisiblePage, lastVisiblePage + 1)) {
            if (draw)
            page.selectedText.clear();
            LOG.d("Add Word page", page.hashCode());
            final RectF bounds = page.getBounds(zoom);
            TextWord prevWord = null;
            if (RectF.intersects(bounds, tapRect)) {
                if (LengthUtils.isNotEmpty(page.texts)) {

                    for (final TextWord[] lines : page.texts) {
                        final TextWord current[] = lines;
                        for (final TextWord line : current) {
                            if (!AppState.get().isTextFormat() && (line.left < 0 || line.top < 0)) {
                                continue;
                            }
                            RectF wordRect = page.getPageRegion(bounds, line);
                            if (wordRect == null) {
                                continue;
                            }

                            if (isHyphenWorld || (single && RectF.intersects(wordRect, tapRect))) {
                                if (prevWord != null && prevWord.w.endsWith("-") && !isHyphenWorld) {
                                    build.append(prevWord.w.replace("-", ""));
                                    if (draw)
                                        page.selectedText.add(prevWord);
                                }

                                build.append(line.w + " ");

                                if (!isHyphenWorld) {
                                    if (draw)
                                    page.selectedText.add(line);
                                }

                                LOG.d("Add Word", line.w);

                                if (isHyphenWorld && TxtUtils.isNotEmpty(line.w)) {
                                    if (draw)
                                    page.selectedText.add(line);
                                    isHyphenWorld = false;
                                }

                                if (line.w.endsWith("-")) {
                                    isHyphenWorld = true;
                                }

                                // get links
                                if (LengthUtils.isNotEmpty(page.links)) {
                                    for (final PageLink link : page.links) {
                                        final RectF linkRect = page.getLinkSourceRect(bounds, link);
                                        if (linkRect == null) {
                                            continue;
                                        }

                                        if (RectF.intersects(linkRect, wordRect)) {
                                            TempHolder.get().linkPage = link.targetPage;
                                        }
                                    }
                                }

                            } else if (!single) {
                                if (y2 > yLong) {
                                    if (wordRect.top < tapRect.top && wordRect.bottom > tapRect.top && wordRect.right > tapRect.left) {
                                        if (draw)
                                            page.selectedText.add(line);
                                        build.append(line.w + TxtUtils.space());

                                        LOG.d("Add Word", line.w);
                                    } else if (wordRect.top < tapRect.bottom && wordRect.bottom > tapRect.bottom && wordRect.left < tapRect.right) {
                                        if (draw)
                                            page.selectedText.add(line);
                                        build.append(line.w + TxtUtils.space());

                                        LOG.d("Add Word", line.w);
                                    } else if (wordRect.top > tapRect.top && wordRect.bottom < tapRect.bottom) {
                                        if (draw)
                                            page.selectedText.add(line);
                                        build.append(line.w + TxtUtils.space());

                                        LOG.d("Add Word", line.w);
                                    }

                                } else if (RectF.intersects(wordRect, tapRect)) {
                                    if (draw)
                                        page.selectedText.add(line);
                                    if (AppState.get().selectingByLetters) {
                                        build.append(line.w);
                                    } else {
                                        build.append(line.w.trim() + " ");
                                    }

                                    LOG.d("Add Word", line.w);
                                }
                            }
                            if (TxtUtils.isNotEmpty(line.w)) {
                                prevWord = line;
                            }
                        }
                        String k;
                        if (AppState.get().selectingByLetters && current.length >= 2 && !(k = current[current.length - 1].getWord()).equals(" ") && !k.equals("-")) {
                            build.append(" ");
                        }
                    }

                }

            }

        }
        if (build.length() > 0) {
            redrawView();
            String filterString = TxtUtils.filterString(build.toString());
            LOG.d("Add Word SELECT TEXT", filterString);
            return filterString;
        }

        return null;

    }

    public final boolean processTap(final TouchManager.Touch type, final MotionEvent e) {
        final float x = e.getX();
        final float y = e.getY();

        if (type == Touch.SingleTap) {
            if (processLinkTap(x, y)) {
                return true;
            }
        }

        return processActionTap(type, x, y);
    }

    protected boolean processActionTap(final TouchManager.Touch type, final float x, final float y) {
        // final Integer actionId = TouchManager.getAction(type, x, y,
        // getWidth(), getHeight());
        final Integer actionId = null;
        final ActionEx action = actionId != null ? getOrCreateAction(actionId) : null;
        if (action != null && action.getMethod().isValid()) {
            action.run();
            return true;
        }
        return false;
    }

    public void selectAnnotation(Annotation annotation) {
        if (annotation == null) {
            for (final Page page : model.getPages(firstVisiblePage, lastVisiblePage + 1)) {
                page.selectionAnnotion = null;
            }
            base.getDocumentController().redrawView();
            return;
        }
        Page pageByDocIndex = base.getDocumentModel().getPageByDocIndex(annotation.getPage() - 1);
        pageByDocIndex.selectionAnnotion = annotation;
        base.getDocumentController().redrawView();
    }

    public final Annotation isAnnotationTap(final float x, final float y) {

        final float zoom = base.getZoomModel().getZoom();
        final RectF rect = new RectF(x, y, x, y);
        rect.offset(getScrollX(), getScrollY());

        for (final Page page : model.getPages(firstVisiblePage, lastVisiblePage + 1)) {
            final RectF bounds = page.getBounds(zoom);
            if (RectF.intersects(bounds, rect)) {
                if (page.annotations == null) {
                    continue;
                }
                for (Annotation a : page.annotations) {
                    RectF wordRect = page.getPageRegion(bounds, a);
                    if (wordRect == null) {
                        continue;
                    }
                    boolean intersects = RectF.intersects(wordRect, rect);
                    LOG.d("Annotation", wordRect, rect, intersects);
                    if (intersects) {
                        LOG.d("Intersects with Annotation", a);
                        return a;
                    }
                }
            }
        }
        return null;

    }

    protected final boolean processLinkTap(final float x, final float y) {
        final float zoom = base.getZoomModel().getZoom();
        final RectF rect = new RectF(x, y, x, y);
        rect.offset(getScrollX(), getScrollY());

        for (final Page page : model.getPages(firstVisiblePage, lastVisiblePage + 1)) {
            final RectF bounds = page.getBounds(zoom);
            if (RectF.intersects(bounds, rect)) {
                if (LengthUtils.isNotEmpty(page.links)) {
                    for (final PageLink link : page.links) {
                        if (processLinkTap(page, link, bounds, rect)) {
                            return true;
                        }
                    }
                }
                return false;
            }
        }
        return false;
    }

    public int getLinkPage(final float x, final float y) {
        final float zoom = base.getZoomModel().getZoom();
        final RectF rect = new RectF(x, y, x, y);
        rect.offset(getScrollX(), getScrollY());

        for (final Page page : model.getPages(firstVisiblePage, lastVisiblePage + 1)) {
            final RectF bounds = page.getBounds(zoom);
            if (RectF.intersects(bounds, rect)) {
                if (LengthUtils.isNotEmpty(page.links)) {
                    for (final PageLink link : page.links) {
                        final RectF linkRect = page.getLinkSourceRect(bounds, link);

                        if (linkRect == null || !RectF.intersects(linkRect, rect)) {
                            return -1;
                        }

                        // if (link != null && link.url != null &&
                        // link.url.startsWith("http")) {
                        // openUrl(link.url);
                        // return true;
                        // }

                        if (link != null) {
                            return link.targetPage;
                        }

                    }
                }
                return -1;
            }
        }
        return -1;
    }

    protected final boolean processLinkTap(final Page page, final PageLink link, final RectF pageBounds, final RectF tapRect) {

        LOG.d("TEST", "processLinkTap");

        final RectF linkRect = page.getLinkSourceRect(pageBounds, link);

        if (linkRect == null || !RectF.intersects(linkRect, tapRect)) {
            return false;
        }

        if (link != null && link.url != null && link.url.startsWith("http")) {
            AlertDialogs.openUrl(base.getActivity(), link.url);
            return true;
        }

        if (link != null) {
            goToLink(link.targetPage, link.targetRect, true);
        }
        return true;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.ebookdroid.ui.viewer.IViewController#goToLink(int,
     *      android.graphics.RectF)
     */
    @Override
    public ViewState goToLink(final int pageDocIndex, final RectF targetRect, final boolean addToHistory) {
        if (pageDocIndex >= 0) {
            Page target = model.getPageByDocIndex(pageDocIndex);
            if (target == null) {
                return null;
            }
            float offsetX = 0;
            float offsetY = 0;
            if (targetRect != null) {
                offsetX = targetRect.left;
                offsetY = targetRect.top;
                if (target.type == PageType.LEFT_PAGE && offsetX >= 0.5f) {
                    target = model.getPageObject(target.index.viewIndex + 1);
                    offsetX -= 0.5f;
                }
            }
            if (target != null) {
                return base.jumpToPage(target.index.viewIndex, offsetX, offsetY, addToHistory);
            }
        }
        return null;
    }

    protected class GestureListener extends SimpleOnGestureListener implements IMultiTouchListener {

        /**
         * {@inheritDoc}
         * 
         * @see android.view.GestureDetector.SimpleOnGestureListener#onDoubleTap(android.view.MotionEvent)
         */
        @Override
        public boolean onDoubleTap(final MotionEvent e) {
            return processTap(TouchManager.Touch.DoubleTap, e);
        }

        /**
         * {@inheritDoc}
         * 
         * @see android.view.GestureDetector.SimpleOnGestureListener#onDown(android.view.MotionEvent)
         */
        @Override
        public boolean onDown(final MotionEvent e) {
            getView().forceFinishScroll();
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
            final Rect l = getScrollLimits();
            float x = vX, y = vY;
            if (Math.abs(vX / vY) < 0.5) {
                x = 0;
            }
            if (Math.abs(vY / vX) < 0.5) {
                y = 0;
            }
            getView().startFling(x, y, l);
            getView().redrawView();
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
            float x = distanceX, y = distanceY;
            if (Math.abs(distanceX / distanceY) < 0.5) {
                x = 0;
            }
            if (Math.abs(distanceY / distanceX) < 0.5) {
                y = 0;
            }
            getView().scrollBy((int) x, (int) y);
            return true;
        }

        /**
         * {@inheritDoc}
         * 
         * @see android.view.GestureDetector.SimpleOnGestureListener#onSingleTapUp(android.view.MotionEvent)
         */
        @Override
        public boolean onSingleTapUp(final MotionEvent e) {
            return true;
        }

        /**
         * {@inheritDoc}
         * 
         * @see android.view.GestureDetector.SimpleOnGestureListener#onSingleTapConfirmed(android.view.MotionEvent)
         */
        @Override
        public boolean onSingleTapConfirmed(final MotionEvent e) {
            return processTap(TouchManager.Touch.SingleTap, e);
        }

        /**
         * {@inheritDoc}
         * 
         * @see android.view.GestureDetector.SimpleOnGestureListener#onLongPress(android.view.MotionEvent)
         */
        @Override
        public void onLongPress(final MotionEvent e) {
            // LongTap operation cause side-effects
            // processTap(TouchManager.Touch.LongTap, e);
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
            base.getZoomModel().scaleZoom(factor);
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.ebookdroid.common.touch.IMultiTouchListener#onTwoFingerPinchEnd()
         */
        @Override
        public void onTwoFingerPinchEnd(final MotionEvent e) {
            base.getZoomModel().commit();
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.ebookdroid.common.touch.IMultiTouchListener#onTwoFingerTap()
         */
        @Override
        public void onTwoFingerTap(final MotionEvent e) {
            processTap(TouchManager.Touch.TwoFingerTap, e);
        }
    }

}
