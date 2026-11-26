package com.foobnix.pdf.search.activity;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import org.ebookdroid.common.bitmaps.RawBitmap;
import org.ebookdroid.common.bitmaps.BitmapRef;
import android.graphics.Rect;
import android.graphics.Paint.Style;
import android.graphics.PointF;
import android.graphics.RectF;
import android.graphics.drawable.BitmapDrawable;
import android.os.Handler;
import android.os.Looper;
import android.util.AttributeSet;
import android.util.Pair;

import com.foobnix.pdf.info.wrapper.DocumentController;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.Scroller;
import android.widget.Toast;

import com.foobnix.LibreraApp;
import com.foobnix.android.utils.Apps;
import com.foobnix.android.utils.Dips;
import com.foobnix.android.utils.LOG;
import com.foobnix.android.utils.TxtUtils;
import com.foobnix.android.utils.Vibro;
import com.foobnix.model.AppSP;
import com.foobnix.model.AppState;
import com.foobnix.pdf.info.AnnotationsData;
import com.foobnix.pdf.info.AnnotationsFinder;
import com.foobnix.tts.ReadingHighlight;
import com.foobnix.pdf.info.AnnotationsPainter;
import com.foobnix.pdf.info.R;
import com.foobnix.pdf.info.model.BookCSS;
import com.foobnix.pdf.info.view.BrightnessHelper;
import com.foobnix.pdf.info.view.Dialogs;
import com.foobnix.pdf.info.wrapper.MagicHelper;
import com.foobnix.pdf.search.activity.msg.InvalidateMessage;
import com.foobnix.pdf.search.activity.msg.MessageAutoFit;
import com.foobnix.pdf.search.activity.msg.MessageCenterHorizontally;
import com.foobnix.pdf.search.activity.msg.MessageEditMark;
import com.foobnix.pdf.search.activity.msg.MessageEraseAnnotation;
import com.foobnix.pdf.search.activity.msg.MessageSelectAnnotation;
import com.foobnix.pdf.search.activity.msg.MessageEvent;
import com.foobnix.pdf.search.activity.msg.MessagePageXY;
import com.foobnix.pdf.search.activity.msg.MovePageAction;
import com.foobnix.pdf.search.activity.msg.TextWordsMessage;
import com.foobnix.sys.ClickUtils;
import com.foobnix.sys.TempHolder;

import org.ebookdroid.core.codec.Annotation;
import org.ebookdroid.core.codec.PageLink;
import org.ebookdroid.droids.mupdf.codec.TextWord;
import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class PageImaveView extends View {

    public static int MIN = Dips.dpToPx(15);
    static volatile boolean isFirstZoomInOut = true;
    static volatile boolean prevLock = false;
    static Paint rect = new Paint();

    static {
        rect.setColor(Color.DKGRAY);
        rect.setStrokeWidth(Dips.dpToPx(1));
        rect.setStyle(Style.STROKE);

    }

    final Paint pageBgPaint = new Paint();
    final RectF pageBgRect = new RectF();
    int drawableHeight, drawableWidth;
    GestureDetector gestureDetector;
    Scroller scroller;
    ImageSimpleGestureListener imageGestureListener;
    Paint paintWrods = new Paint();
    float x, y, xInit, yInit, cx, cy, distance = 0;
    ClickUtils clickUtils;
    BrightnessHelper brightnessHelper;
    int dp1 = Dips.dpToPx(1);
    private BitmapDrawable imageDrawable;
    private Handler handler;
    Runnable scrolling = new Runnable() {

        @Override
        public void run() {
            if (scroller.isFinished()) {
                return;
            }
            final boolean more = scroller.computeScrollOffset();
            final int xx = scroller.getCurrX();
            final int yy = scroller.getCurrY();

            final float dx = xx - x;
            final float dy = yy - y;

            imageMatrix().postTranslate(dx, dy);
            y = yy;
            x = xx;
            invalidate();

            if (more) {
                // Once a frame: posted to the handler it ran over and over between frames, and
                // kept the main thread busy while the page was flung
                postOnAnimation(scrolling);
            }

        }
    };
    private boolean isReadyForMove = false;
    private boolean isLognPress = false;
    private boolean isIgronerClick = false;
    private int isMoveNextPrev = 0;
    private int pageNumber;

    public PageImaveView(final Context context, final AttributeSet attrs) {
        super(context, attrs);

        handler = new Handler(Looper.getMainLooper());
        scroller = new Scroller(getContext(), new AccelerateDecelerateInterpolator());
        imageGestureListener = new ImageSimpleGestureListener();
        gestureDetector = new GestureDetector(context, imageGestureListener);

        paintWrods.setStrokeWidth(Dips.dpToPx(1));
        paintWrods.setTextSize(30);

        EventBus.getDefault().register(this);
        PAGE_VIEWS.add(this);
        clickUtils = new ClickUtils();
        brightnessHelper = new BrightnessHelper(context);
        setSaveEnabled(false);
    }

    public RectF transform(RectF origin, int number) {
        RectF r = new RectF(origin);

        if (number == 0) {
            // In crop mode the bitmap on screen is only the cropped region of the page, while
            // text/link rectangles are reported by the codec in full-page space. Move them into
            // cropped space first, otherwise everything lands offset and undersized.
            final RectF crop = PageImageState.get().getPageCrop(pageNumber);
            if (crop != null && crop.width() > 0 && crop.height() > 0) {
                r.left = (r.left - crop.left) / crop.width();
                r.right = (r.right - crop.left) / crop.width();
                r.top = (r.top - crop.top) / crop.height();
                r.bottom = (r.bottom - crop.top) / crop.height();
            }
        }

        r.left = r.left * drawableWidth;
        r.right = r.right * drawableWidth;

        r.top = r.top * drawableHeight;
        r.bottom = r.bottom * drawableHeight;

        if (number == 1) {
            r.left = r.left / 2;
            r.right = r.right / 2;
        } else if (number == 2) {
            r.left = drawableWidth / 2 + r.left / 2;
            r.right = drawableWidth / 2 + r.right / 2;
        }

        return r;
    }

    public TextWord[][] getPageText(int number) {
        return getPageText(pageNumber, number);
    }

    public TextWord[][] getPageText(int pageNumber, int number) {
        try {
            if (AppSP.get().isDouble && number != 0) {

                int page = pageNumber * 2;
                if (AppSP.get().isDoubleCoverAlone) {
                    page--;
                }

                TextWord[][] t = null;
                if (number == 1) {
                    t = PageImageState.get().pagesText.get(page);
                } else if (number == 2) {
                    t = PageImageState.get().pagesText.get(page + 1);
                }

                for (int i = 0; i < t.length; i++) {
                    for (int j = 0; j < t[i].length; j++) {
                        // TextWord textWord = t[i][j];
                        // textWord.number = number;
                        t[i][j].number = number;
                    }
                }

                return t;
            } else {
                return PageImageState.get().pagesText.get(pageNumber);
            }
        } catch (Exception e) {
            LOG.e(e);
            return null;
        }
    }

    public synchronized Pair<List<PageLink>, List<Annotation>> getPageLinks(int number) {
        if (AppSP.get().isCut || !isCropMappable(number)) {
            return new Pair<>(Collections.emptyList(), Collections.emptyList());
        }
        return getPageLinksInner(number);

    }

    /**
     * True when page coordinates can be mapped onto the bitmap currently on screen. Without
     * cropping that is always the case; with cropping it needs the crop rectangle that
     * {@link com.foobnix.sys.ImageExtractor} publishes for plain single-page renders.
     */
    public boolean isCropMappable(int number) {
        if (!AppSP.get().isCrop) {
            return true;
        }
        if (number != 0 || AppSP.get().isDouble) {
            return false;
        }
        final RectF crop = PageImageState.get().getPageCrop(pageNumber);
        return crop != null && crop.width() > 0 && crop.height() > 0;
    }

    public synchronized Pair<List<PageLink>, List<Annotation>> getPageLinksInner(int number) {

        try {
            List<PageLink> t = null;
            List<Annotation> a = null;

            if (AppSP.get().isDouble && number != 0) {

                int page = pageNumber * 2;
                if (AppSP.get().isDoubleCoverAlone) {
                    page--;
                }
                if (number == 1) {
                    t = PageImageState.get().pagesLinks.get(page);
                    a = PageImageState.get().pagesAnnotation.get(page);
                } else if (number == 2) {
                    t = PageImageState.get().pagesLinks.get(page + 1);
                    a = PageImageState.get().pagesAnnotation.get(page + 1);
                }

                if (t != null) {
                    for (PageLink l : t) {
                        LOG.d("PageLinks targetPage after", l.targetPage);
                        l.number = number;
                        LOG.d("PageLinks targetPage before", l.targetPage);
                    }
                }
            } else {
                t = PageImageState.get().pagesLinks.get(pageNumber);
                a = PageImageState.get().pagesAnnotation.get(pageNumber);
            }

            if (t == null) {
                t = Collections.emptyList();
            }
            if (a == null) {
                a = Collections.emptyList();
            }
            return new Pair<>(t, a);
        } catch (Exception e) {
            LOG.e(e);
            return null;
        }

    }

    /** The reader's own marks for one of the pages on screen, keyed as the links are. */
    public synchronized List<AnnotationsFinder.Drawn> getPageMarks(int number) {
        if (AppSP.get().isCut || !isCropMappable(number)) {
            return Collections.emptyList();
        }
        List<AnnotationsFinder.Drawn> marks = null;
        if (AppSP.get().isDouble && number != 0) {
            int page = pageNumber * 2;
            if (AppSP.get().isDoubleCoverAlone) {
                page--;
            }
            marks = PageImageState.get().pagesMarks.get(number == 1 ? page : page + 1);
        } else {
            marks = PageImageState.get().pagesMarks.get(pageNumber);
        }
        return marks != null ? marks : Collections.<AnnotationsFinder.Drawn>emptyList();
    }

    private void drawMarks(Canvas canvas, int number) {
        for (AnnotationsFinder.Drawn mark : getPageMarks(number)) {
            for (RectF rect : mark.rects) {
                AnnotationsPainter.draw(canvas, transform(rect, number), mark.annotation);
            }
        }
        for (RectF rect : ReadingHighlight.on(docPageOf(number))) {
            AnnotationsPainter.drawReading(canvas, transform(rect, number));
        }
    }

    public void setPageNumber(int pageNumber) {
        this.pageNumber = pageNumber;
    }

    public Matrix imageMatrix() {
        return PageImageState.get().getMatrix();
    }

    @Subscribe
    public void onInvalidate(InvalidateMessage event) {
        invalidate();
    }

    @Subscribe
    public void onSelectTextByAnchors(MessagePageXY event) {
        if (pageNumber == event.getPage() && MessagePageXY.TYPE_SELECT_TEXT == event.getType()) {
            selectText(event.getX1(), event.getY1(), event.getX(), event.getY());
        }

    }

    ;

    @Subscribe
    public void onAutoFit(MessageAutoFit event) {
        LOG.d("onAutoFit recive");
        if (pageNumber == event.getPage()) {
            LOG.d("onAutoFit recive", event.getPage(), pageNumber);
            autoFit();
            invalidate();
            isFirstZoomInOut = true;
        }
    }

    ;

    @Subscribe
    public void onMovePage(MovePageAction event) {
        if (pageNumber != event.getPage()) {
            return;
        }
        int k = Dips.dpToPx(3);
        float scale = 0.03f;

        final float values[] = new float[9];
        imageMatrix().getValues(values);
        float mScale = values[Matrix.MSCALE_X];

        int w = (drawableWidth) / 2;
        int h = (drawableHeight) / 2;

        if (MovePageAction.CENTER == event.getAction()) {
            LOG.d("Action center", event.getPage());
            PageImageState.get().isAutoFit = true;
            onAutoFit(new MessageAutoFit(event.getPage()));
            return;
        }

        if (MovePageAction.LEFT == event.getAction()) {
            imageMatrix().postTranslate(-1 * k, 0);
        } else if (MovePageAction.RIGHT == event.getAction()) {
            imageMatrix().postTranslate(k, 0);
        } else if (MovePageAction.UP == event.getAction()) {
            imageMatrix().postTranslate(0, k * -1);
        } else if (MovePageAction.DOWN == event.getAction()) {
            imageMatrix().postTranslate(0, k);
        } else if (MovePageAction.ZOOM_PLUS == event.getAction()) {
            imageMatrix().postScale(1 + scale, 1 + scale, w, h);
        } else if (MovePageAction.ZOOM_MINUS == event.getAction()) {
            imageMatrix().postScale(1 - scale, 1 - scale, w, h);
        }
        LOG.d("MMM SCALE", mScale);

        PageImageState.get().isAutoFit = false;
        invalidateAndMsg();

    }

    @Subscribe
    public void onCenterHorizontally(MessageCenterHorizontally event) {
        LOG.d("onCenterHorizontally recive");
        if (pageNumber == event.getPage()) {
            LOG.d("onAutoFit recive", event.getPage(), pageNumber);
            centerHorizontally();
            invalidate();
        }
    }

    @Subscribe
    public void onTextWords(TextWordsMessage event) {
        if (event.getPage() == pageNumber) {
            // pageText = event.getMessages();
            selectText(xInit, yInit, xInit, yInit);
        }
    }

    public void invalidateAndMsg() {
        EventBus.getDefault().post(new InvalidateMessage());
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        LOG.d("ImagePageFragment", "onDetachedFromWindow");
//        if (bitmap != null && !bitmap.isRecycled()) {
//            LOG.d("recycle onDetachedFromWindow");
//            bitmap.recycle();
//            bitmap = null;
//        }
        imageDrawable = null;
        pinching = false;
        removeCallbacks(pinchRefresh);
        dropDetail();
        PAGE_VIEWS.remove(this);
        EventBus.getDefault().unregister(this);
    }

    @Override
    public boolean onTouchEvent(final MotionEvent event) {
        gestureDetector.onTouchEvent(event);
        imageGestureListener.onTouchEvent(event);
        return true;
    }

    @Override
    protected void onDraw(final Canvas canvas) {
        super.onDraw(canvas);
        try {
//            if (AppState.get().isOLED && !AppState.get().isDayNotInvert /* && MagicHelper.getBgColor() == Color.BLACK */) {
//                canvas.drawColor(Color.BLACK);
//            } else {
//                canvas.drawColor(MagicHelper.ligtherColor(MagicHelper.getBgColor()));
//            }
            if (imageDrawable == null) {
                drawPageNotReady(canvas);
            } else {
                canvas.drawColor(MagicHelper.getForegroundColor());
            }

            final int saveCount = canvas.getSaveCount();
            canvas.save();

            canvas.concat(imageMatrix());

            if (imageDrawable != null) {
                imageDrawable.draw(canvas);
                // The part on screen drawn again at the screen's own sharpness, over the page
                if (detail != null && !detail.isRecycled()) {
                    canvas.drawBitmap(detail, null, detailRegion, detailPaint);
                }
            }

            if (PageImageState.get().isShowCuttingLine && AppSP.get().isCut == false) {
                int offset = drawableWidth * AppState.get().cutP / 100;
                canvas.drawLine(offset, 0, offset, drawableHeight, paintWrods);
            }

            if (AppSP.get().isDouble) {
                drawMarks(canvas, 1);
                drawMarks(canvas, 2);
            } else {
                drawMarks(canvas, 0);
            }

            if (selectedAnnotation != null && isCropMappable(selectedNumber)) {
                // Shaded as scroll mode shades the annotation it has picked out
                paintWrods.setColor(AppState.get().isDayNotInvert ? Color.BLUE : Color.YELLOW);
                paintWrods.setAlpha(60);
                canvas.drawRect(transform(selectedAnnotation, selectedNumber), paintWrods);
            }

            List<TextWord> selectedWords = PageImageState.get().getSelectedWords(pageNumber);
            if (selectedWords != null) {
                for (TextWord tw : selectedWords) {
                    drawWord(canvas, tw);
                }
            }

//            if (AppState.get().isOLED && !AppState.get().isDayNotInvert /* && !TempHolder.get().isTextFormat */) {
//                canvas.drawRect(-dp1, 0, drawableWidth + dp1, drawableHeight, rect);
//            }

            if (!AppSP.get().isCut && isCropMappable(0)) {

                paintWrods.setColor(AppState.get().isDayNotInvert ? Color.BLUE : Color.YELLOW);
                paintWrods.setAlpha(60);

                if (!BookCSS.get().isTextFormat()) {
                    if (AppSP.get().isDouble) {
                        for (PageLink pl : getPageLinks(1).first) {
                            drawLink(canvas, pl);
                        }

                        for (PageLink pl : getPageLinks(2).first) {
                            drawLink(canvas, pl);
                        }
                    } else {
                        for (PageLink pl : getPageLinks(0).first) {
                            drawLink(canvas, pl);
                        }
                    }
                }
            }

            canvas.restoreToCount(saveCount);

        } catch (Exception e) {
            LOG.e(e);
        }
        scheduleDetail();
    }

    // The part of the page on screen, drawn again at the screen's own sharpness when zoomed past
    // the page's own size - the page itself is made only as large as the screen. Its place is
    // in the page bitmap's own pixels, so it moves and grows with the page.
    private Bitmap detail;
    private final RectF detailRegion = new RectF();
    private final Paint detailPaint = new Paint(Paint.FILTER_BITMAP_FLAG);
    private final float[] detailMatrix = new float[9];
    private volatile int detailGeneration;
    private int detailRetries;
    // The zoom the part was drawn at: moved at the same zoom, the part drawn still serves
    private float detailScale;
    // The page's place on screen the last part was drawn for: drawn for it, it is not drawn again
    private final float[] renderedMatrix = new float[9];
    // Parts of this page being drawn: a new one waits for them, they are not thrown away
    private final java.util.concurrent.atomic.AtomicInteger detailPending =
            new java.util.concurrent.atomic.AtomicInteger();
    private static final java.util.concurrent.ExecutorService DETAIL =
            java.util.concurrent.Executors.newSingleThreadExecutor();
    private final Runnable renderDetail = this::renderDetail;
    private final Runnable renderDetailNow = this::renderDetail;

    // Room drawn round the screen: a quarter of it on each side on the page read, so a move shows
    // sharp parts at once; a tenth on the pages either side, which need only their first look
    private static final float DETAIL_MARGIN = 0.25f;
    private static final float DETAIL_MARGIN_NEAR = 0.1f;
    // A move that brings the screen this near the sharp part's edge draws the next part at once,
    // while the page still moves, so the edge is not reached and no blurred strip shows
    private static final float DETAIL_LOOKAHEAD = 0.1f;
    // Two fingers held on the page: the part on screen is drawn at the zoom they hold this often
    private static final long PINCH_REFRESH_MS = 500;
    private boolean pinching;
    private final Runnable pinchRefresh = this::pinchRefresh;

    // The pages made: the one on screen and those either side of it share the zoom
    private static final java.util.Set<PageImaveView> PAGE_VIEWS =
            java.util.Collections.newSetFromMap(new java.util.WeakHashMap<>());

    /**
     * A zoom or a move of the page draws the part on screen again, a moment after it stops - on
     * the page on screen first, then on the pages either side of it, at the same zoom, so the
     * page turned to is sharp as it comes. The others are not drawn while off screen, so they
     * are asked here. A move at the same zoom that nears the edge of the part drawn draws the
     * next part at once, and two fingers held on the page draw it every half second.
     */
    private void scheduleDetail() {
        if (requestDetail()) {
            for (PageImaveView view : new java.util.ArrayList<>(PAGE_VIEWS)) {
                if (view != this) {
                    view.requestDetail();
                }
            }
        }
    }

    /** The pages either side of the one turned to are drawn sharp too, at the zoom it has. */
    public static void requestDetailAround() {
        for (PageImaveView view : new java.util.ArrayList<>(PAGE_VIEWS)) {
            view.requestDetail();
        }
    }

    /** @return whether the zoom had moved since this page's part was last drawn for it */
    private boolean requestDetail() {
        if (!isNearCurrentPage()) {
            // Two pages away or more: its part is let go of, kept for the pages it may be
            dropDetail();
            java.util.Arrays.fill(detailMatrix, 0);
            return false;
        }
        final float[] values = new float[9];
        imageMatrix().getValues(values);
        if (java.util.Arrays.equals(values, detailMatrix)) {
            return false;
        }
        System.arraycopy(values, 0, detailMatrix, 0, 9);
        final boolean current = pageNumber == PageImageState.currentPage;
        // Moved a little at the same zoom - a page turned to by a swipe that nudged the page -
        // the part drawn, with the room kept round it, still covers the screen and a little
        // past it on the page read: nothing to draw
        if (detailCovers(current ? DETAIL_LOOKAHEAD : 0)) {
            return false;
        }
        detailRetries = 0;
        if (current && !pinching && isDetailSameScale() && detailPending.get() == 0) {
            // Moved at the zoom the part was drawn at, near its edge or past it: the next part
            // is drawn now, ahead of the move, not once it stops
            removeCallbacks(renderDetailNow);
            post(renderDetailNow);
        }
        removeCallbacks(renderDetail);
        postDelayed(renderDetail, current ? 300 : 900);
        return true;
    }

    /** Whether the part drawn sharp was drawn at the zoom the page has now. */
    private boolean isDetailSameScale() {
        if (detail == null || detail.isRecycled() || detailScale == 0) {
            return false;
        }
        final float[] values = new float[9];
        imageMatrix().getValues(values);
        return Math.abs(values[Matrix.MSCALE_X] / detailScale - 1) <= 0.01f;
    }

    /**
     * Whether the part drawn sharp covers the screen at the zoom the page has now.
     *
     * @param lookahead how far past the screen, in parts of its size, it must reach too
     */
    private boolean detailCovers(final float lookahead) {
        if (!isDetailSameScale()) {
            return false;
        }
        final Matrix inverse = new Matrix();
        if (!imageMatrix().invert(inverse)) {
            return false;
        }
        final RectF shown = new RectF(0, 0, getWidth(), getHeight());
        shown.inset(-getWidth() * lookahead, -getHeight() * lookahead);
        inverse.mapRect(shown);
        shown.intersect(0, 0, drawableWidth, drawableHeight);
        final RectF reach = new RectF(detailRegion);
        reach.inset(-1, -1);
        return reach.contains(shown);
    }

    private boolean isNearCurrentPage() {
        return Math.abs(pageNumber - PageImageState.currentPage) <= 1;
    }

    private void dropDetail() {
        detailGeneration++;
        detailScale = 0;
        java.util.Arrays.fill(renderedMatrix, 0);
        if (detail != null) {
            detail = null;
            invalidate();
        }
    }

    /**
     * Only one page to the screen, neither cut in two nor turned - the others are made up of
     * parts of the page the codec does not draw by itself; a cropped page once the part it
     * keeps is known - and only the page on screen and those either side of it, which share
     * the zoom and are turned to next.
     */
    private boolean isDetailWanted() {
        return imageDrawable != null && drawableWidth != 0 && drawableHeight != 0 && getWidth() != 0
                && !AppSP.get().isDouble && !AppSP.get().isCut && AppState.get().rotate == 0
                && isCropMappable(0) && isAttachedToWindow() && isNearCurrentPage();
    }

    private void renderDetail() {
        final float[] values = new float[9];
        imageMatrix().getValues(values);
        final float scale = values[Matrix.MSCALE_X];
        if (scale <= 1.05f) {
            dropDetail();
            return;
        }
        if (!isDetailWanted()) {
            // Not on screen yet - the page after, made while this one was read, still sliding in -
            // or its crop not known yet: tried again a few times, and on its next drawing
            dropDetail();
            java.util.Arrays.fill(detailMatrix, 0);
            if (imageDrawable != null && detailRetries++ < 5) {
                removeCallbacks(renderDetail);
                postDelayed(renderDetail, 400);
            }
            return;
        }
        detailRetries = 0;
        final Matrix inverse = new Matrix();
        if (!imageMatrix().invert(inverse)) {
            return;
        }
        final boolean current = pageNumber == PageImageState.currentPage;
        if (detailCovers(current ? DETAIL_LOOKAHEAD : 0)) {
            return;
        }
        if (detail != null && java.util.Arrays.equals(values, renderedMatrix)) {
            // Drawn already for the page just where it is now
            return;
        }
        if (detailPending.get() > 0) {
            // A part still being drawn - while zooming, or ahead of a move - is waited for, not
            // thrown away: it may well cover the screen
            removeCallbacks(renderDetail);
            postDelayed(renderDetail, 100);
            return;
        }
        System.arraycopy(values, 0, renderedMatrix, 0, 9);
        // The part on screen, with room kept round each side - a quarter of the screen on the
        // page read, so a move shows sharp parts at once, a tenth on the pages either side
        final float margin = current ? DETAIL_MARGIN : DETAIL_MARGIN_NEAR;
        final RectF region = new RectF(0, 0, getWidth(), getHeight());
        region.inset(-getWidth() * margin, -getHeight() * margin);
        inverse.mapRect(region);
        if (!region.intersect(0, 0, drawableWidth, drawableHeight)) {
            dropDetail();
            return;
        }
        final int width = Math.round(region.width() * scale);
        final int height = Math.round(region.height() * scale);
        if (width <= 0 || height <= 0) {
            return;
        }
        final RectF slice = new RectF(region.left / drawableWidth, region.top / drawableHeight,
                                      region.right / drawableWidth, region.bottom / drawableHeight);
        // A cropped page is the part of the page it keeps: the slice is of that part
        if (AppSP.get().isCrop) {
            final RectF crop = PageImageState.get().getPageCrop(pageNumber);
            if (crop == null) {
                return;
            }
            slice.set(crop.left + slice.left * crop.width(), crop.top + slice.top * crop.height(),
                      crop.left + slice.right * crop.width(), crop.top + slice.bottom * crop.height());
        }
        // The part of the page image, in fractions of it, that the book's background is laid
        // under: the background lies over the image as made, cropped or not
        final RectF textureRegion = new RectF(region.left / drawableWidth, region.top / drawableHeight,
                                              region.right / drawableWidth, region.bottom / drawableHeight);
        final int page = pageNumber;
        final String url = pageUrl;
        final int generation = ++detailGeneration;
        final boolean invert = !AppState.get().isDayNotInvert;
        final boolean background = MagicHelper.isNeedBookBackgroundImage();
        detailPending.incrementAndGet();
        final boolean pinch = pinching;
        DETAIL.execute(() -> {
            final long start = System.currentTimeMillis();
            Bitmap bitmap = null;
            try {
                // Drawn under the page's crash marks: not over a page that took the app down - a
                // small "crash" picture in its place reads here as a page zoomed far in - and not
                // again where a part of it did, the page itself still shown
                final com.foobnix.sys.ImageExtractor extractor =
                        com.foobnix.sys.ImageExtractor.getInstance(LibreraApp.context);
                if (generation == detailGeneration && url != null && !extractor.crashedBefore(url)) {
                    bitmap = extractor.drawGuarded(url + "#detail",
                            () -> drawDetail(page, width, height, slice, invert, background, textureRegion));
                }
                // Put on the GPU here, off the screen's threads: a part is as large as 2.25
                // screens, and drawn as it was, the render thread stopped to upload it in the
                // middle of a fling - a dropped frame or two for every part, many in a fast one
                if (bitmap != null && android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                    final Bitmap onGpu = bitmap.copy(Bitmap.Config.HARDWARE, false);
                    if (onGpu != null) {
                        bitmap.recycle();
                        bitmap = onGpu;
                    }
                }
            } catch (Throwable e) {
                LOG.e(e);
            }
            final Bitmap made = bitmap;
            // On the main thread, through the handler: a view's own post waits for it to be
            // on screen again, and the part would stay counted as being drawn
            handler.post(() -> {
                detailPending.decrementAndGet();
                if (made == null || generation != detailGeneration || !isDetailWanted()) {
                    if (made != null) {
                        made.recycle();
                    }
                    if (generation == detailGeneration) {
                        // Not drawn: tried again on the next drawing
                        java.util.Arrays.fill(renderedMatrix, 0);
                    }
                    java.util.Arrays.fill(detailMatrix, 0);
                    return;
                }
                if (detail != null) {
                    detail.recycle();
                }
                detail = made;
                detailRegion.set(region);
                detailScale = scale;
                LOG.d("PageDetail", page, width + "x" + height, "scale", scale, pinch ? "pinch" : "",
                      System.currentTimeMillis() - start, "ms");
                // Looked at again on the next drawing: the page may have moved on while the
                // part was drawn, past it, and the next part is then drawn at once
                java.util.Arrays.fill(detailMatrix, 0);
                invalidate();
            });
        });
    }

    /** Draws a part of the page, as the page it lies over is: night's inversion, the background. */
    private static Bitmap drawDetail(final int page, final int width, final int height, final RectF slice,
                                     final boolean invert, final boolean background, final RectF textureRegion) {
        final org.ebookdroid.core.codec.CodecDocument doc = com.foobnix.sys.ImageExtractor.codeCache;
        if (doc == null || doc.isRecycled()) {
            return null;
        }
        Bitmap bitmap;
        final org.ebookdroid.core.codec.CodecPage codecPage = doc.getPage(page);
        try {
            final BitmapRef ref = codecPage.renderBitmap(width, height, slice, false);
            bitmap = ref == null ? null : ref.getBitmap();
        } finally {
            if (!codecPage.isRecycled()) {
                codecPage.recycle();
            }
        }
        if (bitmap == null) {
            return null;
        }
        if (invert) {
            final RawBitmap raw = new RawBitmap(bitmap, new Rect(0, 0, bitmap.getWidth(), bitmap.getHeight()));
            raw.invert();
            bitmap.recycle();
            bitmap = raw.toBitmap().getBitmap();
        }
        if (background) {
            bitmap = MagicHelper.updateWithBackground(bitmap, textureRegion);
        }
        return bitmap;
    }

    /**
     * Two fingers held on the page: the part on screen is drawn at the zoom they hold every half
     * second, a part at a time, so the page is sharp while it is still being zoomed and moved.
     */
    private void pinchRefresh() {
        if (!pinching) {
            return;
        }
        // Zoomed, or moved with two fingers near the part's edge
        if (pageNumber == PageImageState.currentPage && detailPending.get() == 0
                && !detailCovers(DETAIL_LOOKAHEAD)) {
            renderDetail();
        }
        postDelayed(pinchRefresh, PINCH_REFRESH_MS);
    }

    private void setPinching(final boolean on) {
        if (pinching == on) {
            return;
        }
        pinching = on;
        removeCallbacks(pinchRefresh);
        if (on) {
            postDelayed(pinchRefresh, PINCH_REFRESH_MS);
        } else {
            // Let go: the part is drawn for where the fingers left the page
            java.util.Arrays.fill(detailMatrix, 0);
            invalidate();
        }
    }

    /**
     * Until the page is made the screen is the book's own page, its colour and background image,
     * as the scrolled view shows a page still being made. The colour around the page, black for
     * some, flashed over the whole screen while turning pages quickly.
     */
    private void drawPageNotReady(final Canvas canvas) {
        canvas.drawColor(MagicHelper.getBgColor());
        if (MagicHelper.isNeedBookBackgroundImage()) {
            final Bitmap bg = MagicHelper.getBackgroundImage();
            if (bg != null && !bg.isRecycled()) {
                pageBgRect.set(0, 0, getWidth(), getHeight());
                pageBgPaint.setAlpha(255 - MagicHelper.getTransparencyInt());
                canvas.drawBitmap(bg, null, pageBgRect, pageBgPaint);
            }
        }
    }

    // The page's url, as the page is drawn from it: its crash marks are kept by it
    private String pageUrl;

    public void setPageUrl(final String pageUrl) {
        this.pageUrl = pageUrl;
    }

    public void addBitmap(Bitmap bitmap) {
        dropDetail();
        detailRetries = 0;
        java.util.Arrays.fill(detailMatrix, 0);
        // Uploaded to the GPU now, while the page waits beside the one read, not as it slides
        // in on the turn
        bitmap.prepareToDraw();
        imageDrawable = new BitmapDrawable(getResources(), bitmap);
        drawableHeight = bitmap.getHeight();
        drawableWidth = bitmap.getWidth();
        imageDrawable.setBounds(0, 0, drawableWidth, drawableHeight);

        // The zoom is shared by every page: a page made beside the one on screen fitting it to its
        // own size moved the page being read, sized or cropped differently, after the turn
        if (pageNumber == PageImageState.currentPage) {
            autoFit();
        }
        invalidate();
        // A page made off screen is not drawn, so it asks for its sharp part itself
        post(this::requestDetail);

        LOG.d("addBitmap", pageNumber, bitmap.getWidth(), bitmap.getHeight(), "Real WH:", getWidth(), getHeight());
    }

    /** Lets go of the page. Its size stays, so the same page added back keeps the zoom. */
    public void clearBitmap() {
        dropDetail();
        imageDrawable = null;
        invalidate();
    }

    public void recycle() {
        try {
            if (imageDrawable != null && imageDrawable.getBitmap() != null) {
                imageDrawable.getBitmap().recycle();
            }
        } catch (Exception e) {
            LOG.e(e);
        }
    }

    public void centerHorizontally() {
        float[] f = new float[9];
        imageMatrix().getValues(f);
        float tx = f[Matrix.MTRANS_X];
        float sx = f[Matrix.MSCALE_X];

        imageMatrix().postTranslate(getWidth() / 2 - tx - drawableWidth * sx / 2, 0);
        LOG.d("centerHorizontally", getWidth(), tx, drawableWidth * sx);
    }

    public void autoFit() {
        if (!PageImageState.get().isAutoFit) {
            return;
        }

        final int w = getWidth();
        final int h = getHeight();
        final float scaleH = (float) h / drawableHeight;
        final float scaleW = (float) w / drawableWidth;

        imageMatrix().reset();
        if (scaleH < scaleW) {
            LOG.d("image pre scale scaleH", scaleH);
            imageMatrix().preScale(scaleH, scaleH);
            imageMatrix().postTranslate(Math.abs(getWidth() - drawableWidth * scaleH) / 2, 0);
        } else {
            LOG.d("image pre scale scaleW", scaleW);
            imageMatrix().preScale(scaleW, scaleW);
            imageMatrix().postTranslate(0, Math.abs(getHeight() - drawableHeight * scaleW) / 2);
        }
    }

    private float centerX(final MotionEvent event) {
        return (event.getX() + event.getX(1)) / 2;
    }

    private float centerY(final MotionEvent event) {
        return (event.getY() + event.getY(1)) / 2;
    }

    private float discance(final MotionEvent event) {
        final float x1 = event.getX();
        final float y1 = event.getY();

        final float x2 = event.getX(1);
        final float y2 = event.getY(1);

        return (float) Math.sqrt(Math.pow(x1 - x2, 2) + Math.pow(y1 - y2, 2));
    }

    public float getMyScale() {
        final int w = getWidth();
        final int h = getHeight();

        LOG.d("WxH", w, h);
        LOG.d("image WxH", drawableWidth, drawableHeight);

        final float scaleH = (float) h / drawableHeight;
        final float scaleW = (float) w / drawableWidth;
        float min = Math.min(scaleH, scaleW);
        LOG.d("scale min", min, scaleH, scaleW);
        return min;

    }

    public void drawWord(Canvas c, TextWord t) {
        RectF o = transform(t, t.number);
        c.drawRect(o, paintWrods);
    }

    public void drawLink(Canvas c, PageLink pl) {
        RectF o = transform(pl.sourceRect, pl.number);
        c.drawLine(o.left, o.bottom, o.right, o.bottom, paintWrods);
        // c.drawText("" + pl.targetPage, o.right, o.bottom, paintWrods);
    }

    /**
     * One of Librera's own marks under a tap: the eraser removes it, any other tap offers to
     * change it.
     *
     * @return whether a mark was there, in which case the tap was for it and nothing else
     */
    public boolean onMarkTap(float x1, float y1) {
        RectF tr = new RectF();
        imageMatrix().mapRect(tr);
        float[] f = new float[9];
        imageMatrix().getValues(f);
        final float scaleX = f[Matrix.MSCALE_X];
        if (scaleX == 0) {
            return false;
        }
        final float x = (x1 - tr.left) / scaleX;
        final float y = (y1 - tr.top) / scaleX;
        final RectF tapRect = new RectF(x, y, x, y);

        int number = 0;
        if (AppSP.get().isDouble) {
            number = x < drawableWidth / 2 ? 1 : 2;
        }
        for (AnnotationsFinder.Drawn mark : getPageMarks(number)) {
            for (RectF rect : mark.rects) {
                if (!RectF.intersects(transform(rect, number), tapRect)) {
                    continue;
                }
                final int page = docPageOf(number);
                final Runnable taken = () -> {
                    final List<AnnotationsFinder.Drawn> shown = PageImageState.get().pagesMarks.get(page);
                    if (shown != null) {
                        final List<AnnotationsFinder.Drawn> marks = new ArrayList<AnnotationsFinder.Drawn>(shown);
                        marks.remove(mark);
                        PageImageState.get().pagesMarks.put(page, marks);
                    }
                    invalidate();
                };
                if (AppState.get().editWith == AppState.EDIT_DELETE) {
                    AnnotationsData.get().remove(mark.annotation);
                    AnnotationsFinder.invalidate();
                    taken.run();
                } else {
                    EventBus.getDefault().post(new MessageEditMark(mark.annotation, this::invalidate, taken));
                }
                return true;
            }
        }
        return false;
    }

    /**
     * An annotation of the PDF under a tap with the eraser, as scroll mode erases one: the reader
     * erases it.
     *
     * @return whether one was there, in which case the tap was for it and nothing else
     */
    private boolean onEraseTap(float x, float y) {
        if (AppState.get().editWith != AppState.EDIT_DELETE) {
            return false;
        }
        final Pair<PageLink, Annotation> pair = getPageLinkClicked(x, y);
        if (pair == null || pair.second == null) {
            return false;
        }
        EventBus.getDefault().post(new MessageEraseAnnotation(pair.second));
        return true;
    }

    /** Which page of the book one of the pages on screen is, keyed as the links and text are. */
    private int docPageOf(int number) {
        if (!AppSP.get().isDouble || number == 0) {
            return pageNumber;
        }
        int first = pageNumber * 2;
        if (AppSP.get().isDoubleCoverAlone) {
            first--;
        }
        return number == 1 ? first : first + 1;
    }

    /**
     * The page of the book under a point on screen, and where on it the point is - 0..1 of the
     * whole page, crop or not, as {@link #transform} takes it - for drawing on the page.
     *
     * @return null off the page, or where the bitmap on screen cannot be mapped back
     */
    public Pair<Integer, PointF> toPage(float x1, float y1) {
        final float scale = imageScale();
        if (scale == 0 || drawableWidth == 0 || drawableHeight == 0) {
            return null;
        }
        int number = 0;
        if (AppSP.get().isDouble) {
            number = (x1 - imageLeft()) / scale < drawableWidth / 2 ? 1 : 2;
        }
        return toPage(number, x1, y1);
    }

    /**
     * Where a point on screen is on one of the pages on screen - 0 the only one, 1 and 2 those of
     * a spread - as {@link #toPage(float, float)}; a point just past its edge is on the edge.
     */
    public Pair<Integer, PointF> toPage(int number, float x1, float y1) {
        final RectF onScreen = pageOnScreen(number);
        if (onScreen == null || !isCropMappable(number)) {
            return null;
        }
        final float slop = 1;
        if (x1 < onScreen.left - slop || x1 > onScreen.right + slop || y1 < onScreen.top - slop || y1 > onScreen.bottom + slop) {
            return null;
        }
        float nx = Math.max(0, Math.min(1, (x1 - onScreen.left) / onScreen.width()));
        float ny = Math.max(0, Math.min(1, (y1 - onScreen.top) / onScreen.height()));
        if (number == 0) {
            final RectF crop = PageImageState.get().getPageCrop(pageNumber);
            if (crop != null && crop.width() > 0 && crop.height() > 0) {
                nx = crop.left + nx * crop.width();
                ny = crop.top + ny * crop.height();
            }
        }
        final int page = docPageOf(number);
        if (page < 0) {
            return null;
        }
        return new Pair<>(page, new PointF(nx, ny));
    }

    /** The pages on screen by number, as {@link #toPage(int, float, float)} takes them. */
    public int[] pagesOnScreen() {
        return AppSP.get().isDouble ? new int[]{1, 2} : new int[]{0};
    }

    /** Where one of the pages on screen stands on the view, or null before it is shown. */
    public RectF pageOnScreen(int number) {
        final float scale = imageScale();
        if (scale == 0 || drawableWidth == 0 || drawableHeight == 0) {
            return null;
        }
        final float left = imageLeft();
        final float top = imageTop();
        final float width = drawableWidth * scale;
        final float height = drawableHeight * scale;
        if (number == 1) {
            return new RectF(left, top, left + width / 2, top + height);
        }
        if (number == 2) {
            return new RectF(left + width / 2, top, left + width, top + height);
        }
        return new RectF(left, top, left + width, top + height);
    }

    private float imageScale() {
        float[] f = new float[9];
        imageMatrix().getValues(f);
        return f[Matrix.MSCALE_X];
    }

    private float imageLeft() {
        RectF tr = new RectF();
        imageMatrix().mapRect(tr);
        return tr.left;
    }

    private float imageTop() {
        RectF tr = new RectF();
        imageMatrix().mapRect(tr);
        return tr.top;
    }

    // The annotation picked out from the drawing panel, and which page of a spread it is on
    private Annotation selectedAnnotation;
    private int selectedNumber;

    /**
     * Picks out the annotation of the PDF under a point on screen, as scroll mode does, or lets
     * go of the one picked out when there is none there.
     *
     * @return the annotation now picked out, or null
     */
    public Annotation selectAnnotationAt(float x, float y) {
        selectedAnnotation = drawingAt(x, y);
        selectedNumber = lastClickedNumber;
        invalidate();
        return selectedAnnotation;
    }

    /**
     * The drawing under a point on screen, or null. Only drawings count: a page can carry a
     * link over all of it, which would otherwise be found first.
     */
    public Annotation drawingAt(float x1, float y1) {
        RectF tr = new RectF();
        imageMatrix().mapRect(tr);
        float[] f = new float[9];
        imageMatrix().getValues(f);
        final float scaleX = f[Matrix.MSCALE_X];
        if (scaleX == 0) {
            return null;
        }
        final float x = (x1 - tr.left) / scaleX;
        final float y = (y1 - tr.top) / scaleX;
        final RectF tapRect = new RectF(x, y, x, y);
        int number = 0;
        if (AppSP.get().isDouble) {
            number = x < drawableWidth / 2 ? 1 : 2;
        }
        lastClickedNumber = number;
        // The last one drawn first: where drawings overlap, it is the one on top
        final List<Annotation> annotations = getPageLinks(number).second;
        for (int i = annotations.size() - 1; i >= 0; i--) {
            final Annotation annotation = annotations.get(i);
            if (annotation != null && annotation.type.isDrawing()
                    && RectF.intersects(transform(annotation, number), tapRect)) {
                return annotation;
            }
        }
        return null;
    }

    /** The annotations a box on screen reaches, on either page of a spread, with where each stands on screen. */
    public List<Pair<Annotation, RectF>> annotationsIn(RectF screen) {
        final List<Pair<Annotation, RectF>> found = new ArrayList<>();
        final int[] numbers = AppSP.get().isDouble ? new int[]{1, 2} : new int[]{0};
        for (final int number : numbers) {
            for (final Annotation annotation : getPageLinks(number).second) {
                if (!DocumentController.isPickable(annotation)) {
                    continue;
                }
                final RectF rect = transform(annotation, number);
                imageMatrix().mapRect(rect);
                if (RectF.intersects(rect, screen)) {
                    found.add(new Pair<>(annotation, rect));
                }
            }
        }
        return found;
    }

    /** Where the annotation picked out stands on screen, or null. */
    public RectF selectedAnnotationOnScreen() {
        if (selectedAnnotation == null || !isCropMappable(selectedNumber)) {
            return null;
        }
        final RectF rect = transform(selectedAnnotation, selectedNumber);
        imageMatrix().mapRect(rect);
        return rect;
    }

    /** Picks out an annotation tapped on, as the drawing panel does. */
    public void selectAnnotation(Annotation annotation, int number) {
        selectedAnnotation = annotation;
        selectedNumber = number;
        invalidate();
    }

    public void clearAnnotationSelection() {
        selectedAnnotation = null;
        invalidate();
    }

    private int lastClickedNumber;

    public Pair<PageLink, Annotation> getPageLinkClicked(float x1, float y1) {

        RectF tr = new RectF();
        imageMatrix().mapRect(tr);

        float x = x1 - tr.left;
        float y = y1 - tr.top;

        float[] f = new float[9];
        imageMatrix().getValues(f);

        float scaleX = f[Matrix.MSCALE_X];

        x = x / scaleX;
        y = y / scaleX;

        RectF tapRect = new RectF(x, y, x, y);

        x1 = x1 - tr.left;
        x1 = x1 / scaleX;

        int firstNumber = 0;
        if (AppSP.get().isDouble) {
            firstNumber = x1 < drawableWidth / 2 ? 1 : 2;
        }
        lastClickedNumber = firstNumber;
        //


        //


        PageLink p = null;
        Annotation a = null;
        List<PageLink> pageLinks = getPageLinks(firstNumber).first;
        List<Annotation> annotations = getPageLinks(firstNumber).second;

        LOG.d("getPageLinkClicked", x1, "w", drawableWidth, firstNumber, "links", pageLinks.size());

        for (PageLink link : pageLinks) {
            if (link == null) {
                continue;
            }
            RectF wordRect = transform(link.sourceRect, firstNumber);
            boolean intersects = RectF.intersects(wordRect, tapRect);
            if (intersects) {
                p = link;
                break;
            }
        }
        for (Annotation link : annotations) {
            if (link == null) {
                continue;
            }
            RectF wordRect = transform(link, firstNumber);
            boolean intersects = RectF.intersects(wordRect, tapRect);
            if (intersects) {
                a = link;
                LOG.d("Link Clicked", a.text);
                break;
            }
        }

        return new Pair<>(p, a);

    }

    public String selectText(float x1, float y1, float xInit, float yInit) {
        if (!AppSP.get().isDouble && getPageText(0) == null) {
            LOG.d("get pag No page text", pageNumber);
            return null;
        }

        boolean single = Math.abs(x1 - xInit) < MIN && Math.abs(y1 - yInit) < MIN;

        RectF tr = new RectF();
        imageMatrix().mapRect(tr);

        float x = x1 - tr.left;
        float y = y1 - tr.top;

        xInit = xInit - tr.left;
        yInit = yInit - tr.top;

        float[] f = new float[9];
        imageMatrix().getValues(f);

        float scaleX = f[Matrix.MSCALE_X];

        x = x / scaleX;
        y = y / scaleX;

        xInit = xInit / scaleX;
        yInit = yInit / scaleX;

        RectF tapRect = new RectF(xInit, yInit, x, y);
        if (yInit > y) {
            tapRect.sort();
        }

        PageImageState.get().cleanSelectedWords();

        StringBuilder build = new StringBuilder();

        boolean isHyphenWorld = false;
        TextWord prevWord = null;

        int firstNumber = 0;
        if (AppSP.get().isDouble) {
            firstNumber = xInit < drawableWidth / 2 ? 1 : 2;
        }
        TempHolder.get().textFromPage = firstNumber;

        LOG.d("firstNumber", firstNumber);
        TextWord[][] pageText = getPageText(firstNumber);
        if (pageText == null) {
            return null;
        }
        for (TextWord line[] : pageText) {
            if (line == null) {
                continue;
            }
            final TextWord current[] = line;
            for (TextWord textWord : current) {
                if (textWord == null) {
                    continue;
                }
                if (!BookCSS.get().isTextFormat() && (textWord.left < 0 || textWord.top < 0)) {
                    continue;
                }

                RectF wordRect = transform(textWord, firstNumber);
                if (single) {
                    boolean intersects = RectF.intersects(wordRect, tapRect);
                    if (intersects || isHyphenWorld) {
                        LOG.d("ADD TEXT", textWord);

                        if (prevWord != null && prevWord.w.endsWith("-") && !isHyphenWorld) {
                            build.append(prevWord.w.replace("-", ""));
                            PageImageState.get().addWord(pageNumber, prevWord);
                        }

                        if (!isHyphenWorld) {
                            PageImageState.get().addWord(pageNumber, textWord);
                        }

                        if (isHyphenWorld && TxtUtils.isNotEmpty(textWord.getWord())) {
                            PageImageState.get().addWord(pageNumber, textWord);
                            isHyphenWorld = false;
                        }
                        if (textWord.getWord().endsWith("-")) {
                            isHyphenWorld = true;
                        }
                        build.append(textWord.getWord() + " ");
                    }
                } else {
                    if (y > yInit) {
                        if (wordRect.top < tapRect.top && wordRect.bottom > tapRect.top && wordRect.right > tapRect.left) {
                            PageImageState.get().addWord(pageNumber, textWord);
                            build.append(textWord.getWord() + TxtUtils.space());
                        } else if (wordRect.top < tapRect.bottom && wordRect.bottom > tapRect.bottom && wordRect.left < tapRect.right) {
                            PageImageState.get().addWord(pageNumber, textWord);
                            build.append(textWord.getWord() + TxtUtils.space());
                        } else if (wordRect.top > tapRect.top && wordRect.bottom < tapRect.bottom) {
                            PageImageState.get().addWord(pageNumber, textWord);
                            build.append(textWord.getWord() + TxtUtils.space());
                        }
                    } else if (RectF.intersects(wordRect, tapRect)) {
                        PageImageState.get().addWord(pageNumber, textWord);
                        if (AppState.get().selectingByLetters) {
                            build.append(textWord.w);
                        } else {
                            build.append(textWord.w.trim() + " ");
                        }
                    }
                }

                if (TxtUtils.isNotEmpty(textWord.w)) {
                    prevWord = textWord;
                }

            }
            String k;
            if (AppState.get().selectingByLetters && current.length >= 2 && !(k = current[current.length - 1].getWord()).equals(" ") && !k.equals("-")) {
                build.append(" ");
            }
        }


        String txt = build.toString();
        if (txt.endsWith("- ")) {
            try {
                if (firstNumber == 0) {
                    TextWord[][] texts = getPageText(pageNumber + 1, 0);
                    if (texts[0].length > 1) {
                        txt += texts[0][1].w;
                    } else {
                        txt += texts[0][0].w;
                    }
                } else if (firstNumber == 1) {
                    txt += getPageText(pageNumber, 2)[0][1].w;
                } else {
                    txt += getPageText(pageNumber + 1, 1)[0][1].w;
                }
            } catch (Exception e) {
                LOG.e(e);
            }
        }
        txt = TxtUtils.filterString(txt);


        AppState.get().selectedText = txt;
        invalidate();
        return txt;
    }

    class ImageSimpleGestureListener extends SimpleTouchOnGestureListener {

        @Override
        public boolean onDoubleTap(final MotionEvent e) {
            clickUtils.init();
            isIgronerClick = true;
            if (clickUtils.isClickCenter(e.getX(), e.getY())) {
                isLognPress = true;

                if (AppState.get().doubleClickAction1 == AppState.DOUBLE_CLICK_NOTHING) {

                } else if (AppState.get().doubleClickAction1 == AppState.DOUBLE_CLICK_ZOOM_IN_OUT) {
                    if (isFirstZoomInOut) {
                        imageMatrix().preTranslate(getWidth() / 2 - e.getX(), getHeight() / 2 - e.getY());
                        imageMatrix().postScale(2.5f, 2.5f, getWidth() / 2, getHeight() / 2);
                        isFirstZoomInOut = false;
                        prevLock = AppSP.get().isLocked;
                        AppSP.get().isLocked = false;
                        invalidateAndMsg();
                        PageImageState.get().isAutoFit = false;

                    } else {
                        AppSP.get().isLocked = prevLock;
                        if (BookCSS.get().isTextFormat()) {
                            AppSP.get().isLocked = true;
                        }
                        isLognPress = true;
                        PageImageState.get().isAutoFit = true;
                        autoFit();
                        invalidateAndMsg();
                        isFirstZoomInOut = true;

                    }
                } else if (AppState.get().doubleClickAction1 == AppState.DOUBLE_CLICK_CLOSE_BOOK) {
                    EventBus.getDefault().post(new MessageEvent(MessageEvent.MESSAGE_CLOSE_BOOK, e.getX(), e.getY()));
                } else if (AppState.get().doubleClickAction1 == AppState.DOUBLE_CLICK_AUTOSCROLL) {
                    EventBus.getDefault().post(new MessageEvent(MessageEvent.MESSAGE_AUTO_SCROLL));
                } else if (AppState.get().doubleClickAction1 == AppState.DOUBLE_CLICK_CLOSE_BOOK_AND_APP) {
                    EventBus.getDefault().post(new MessageEvent(MessageEvent.MESSAGE_CLOSE_BOOK_APP, e.getX(), e.getY()));
                } else if (AppState.get().doubleClickAction1 == AppState.DOUBLE_CLICK_CLOSE_HIDE_APP) {
                    Apps.showDesctop(getContext());
                } else if (AppState.get().doubleClickAction1 == AppState.DOUBLE_CLICK_START_STOP_TTS) {
                    EventBus.getDefault().post(new MessageEvent(MessageEvent.MESSAGE_PLAY_PAUSE, e.getX(), e.getY()));
                } else if (AppState.get().doubleClickAction1 == AppState.DOUBLE_CLICK_SHARE_PAGE) {
                    EventBus.getDefault().post(new MessageEvent(MessageEvent.MESSAGE_SHARE_PAGE, e.getX(), e.getY()));
                } else if (AppState.get().doubleClickAction1 == AppState.DOUBLE_CLICK_CENTER_HORIZONTAL) {
                    PageImageState.get().isAutoFit = false;
                    onCenterHorizontally(new MessageCenterHorizontally(pageNumber));
                } else {
                    PageImageState.get().isAutoFit = true;
                    autoFit();
                    invalidateAndMsg();
                }

                EventBus.getDefault().post(new MessageEvent(MessageEvent.MESSAGE_DOUBLE_TAP, e.getX(), e.getY()));
                return true;
            }

            return true;
        }

        ;

        @Override
        public boolean onFling(final MotionEvent e1, final MotionEvent e2, final float velocityX, final float velocityY) {

            if (e1.getX() < BrightnessHelper.BRIGHTNESS_WIDTH) {
                return false;
            }
            if (AppState.get().selectedText != null) {
                return false;
            }
            if (AppSP.get().isLocked) {
                return false;
            }
            if (isReadyForMove) {
                isIgronerClick = true;
                scroller.fling((int) e2.getX(), (int) e2.getY(), (int) velocityX / 3, (int) velocityY / 3, Integer.MIN_VALUE, Integer.MAX_VALUE, Integer.MIN_VALUE, Integer.MAX_VALUE);
                removeCallbacks(scrolling);
                postOnAnimation(scrolling);
            }
            return true;
        }

        @Override
        public void onLongPress(MotionEvent e) {
            isIgronerClick = true;

            if (AppState.get().isSelectTexByTouch) {
                //EventBus.getDefault().post(new MessageEvent(MessageEvent.MESSAGE_PERFORM_CLICK, e.getX(), e.getY()));
                isLognPress = false;
                isIgronerClick = true;
                AppState.get().selectedText = null;
                EventBus.getDefault().post(new MessagePageXY(MessagePageXY.TYPE_HIDE));
                if (new ClickUtils().isClickCenter(e.getX(), e.getY())) {
                    EventBus.getDefault().post(new MessageEvent(MessageEvent.MESSAGE_PERFORM_CLICK, e.getX(), e.getY()));
                }
                LOG.d("PageImaveView MESSAGE_PERFORM_CLICK", 3);


                return;
            }

            if (!AppState.get().isAllowTextSelection) {
                Toast.makeText(LibreraApp.context, R.string.text_highlight_mode_is_disable, Toast.LENGTH_LONG).show();
                return;
            }

            if(AppState.get().isCropNotification) {
                Vibro.vibrate();
            }
            if (AppSP.get().isCut || !isCropMappable(0)) {
                if(AppState.get().isCropNotification) {
                    Toast.makeText(LibreraApp.context, R.string.the_page_is_clipped_the_text_selection_does_not_work,
                                 Toast.LENGTH_LONG)
                         .show();
                }
                return;
            }
            isLognPress = true;
            xInit = e.getX();
            yInit = e.getY();
            String selectText = selectText(xInit, yInit, e.getX(), e.getY());
            if (TxtUtils.isEmpty(selectText)) {
                AppState.get().selectedText = null;
                EventBus.getDefault().post(new MessagePageXY(MessagePageXY.TYPE_HIDE));

            }
        }

        @Override
        public boolean onTouchEvent(final MotionEvent event) {
            final int action = event.getAction() & MotionEvent.ACTION_MASK;

            if (action == MotionEvent.ACTION_DOWN) {
                AppState.get().selectedText = null;
                LOG.d("TEST", "action ACTION_DOWN");
                scroller.forceFinished(true);
                x = event.getX();
                y = event.getY();
                brightnessHelper.onActoinDown(x, y);
                isReadyForMove = false;
                if (AppState.get().isSelectTexByTouch) {
                    isLognPress = true;
                    isIgronerClick = true;
                    xInit = x;
                    yInit = y;
                } else {
                    isLognPress = false;
                }
                isMoveNextPrev = 0;
                EventBus.getDefault().post(new MessagePageXY(MessagePageXY.TYPE_HIDE));
            } else if (action == MotionEvent.ACTION_MOVE) {
                if (event.getPointerCount() == 1) {
                    LOG.d("TEST", "action ACTION_MOVE 1");
                    final float dx = event.getX() - x;
                    final float dy = event.getY() - y;

                    if (isLognPress) {
                        String selectText = selectText(event.getX(), event.getY(), xInit, yInit);
                        if (selectText != null && selectText.contains(" ")) {
                            EventBus.getDefault().post(new MessagePageXY(MessagePageXY.TYPE_SHOW, -1, xInit, yInit, event.getX(), event.getY()));
                        }
                    } else {

                        if (AppSP.get().isLocked) {
                            isReadyForMove = false;
                            if (AppState.get().isSelectTexByTouch) {
                                isIgronerClick = true;
                            } else {
                                isIgronerClick = false;
                            }
                            if (AppState.get().isEnableVerticalSwipe && Math.abs(dy) > Math.abs(dx) && Math.abs(dy) > Dips.DP_10) {
                                if (AppState.get().isSwipeGestureReverse) {
                                    isMoveNextPrev = dy > 0 ? -1 : 1;
                                } else {
                                    isMoveNextPrev = dy > 0 ? 1 : -1;
                                }
                            }

                        } else {
                            if (AppState.get().rotateViewPager == 0) {
                                if (Math.abs(dy) > Math.abs(dx) && (Math.abs(dy) + Math.abs(dx) > Dips.DP_10)) {
                                    isReadyForMove = true;
                                    isIgronerClick = true;
                                }
                            } else {
                                if (Math.abs(dx) > Math.abs(dy) && (Math.abs(dx) + Math.abs(dy) > Dips.DP_10)) {
                                    isReadyForMove = true;
                                    isIgronerClick = true;
                                }
                            }
                        }

                        boolean isBrightness = brightnessHelper.onActionMove(event);
                        if (isBrightness) {
                            isIgronerClick = true;
                            isMoveNextPrev = 0;
                        }

                        if (!isBrightness && isReadyForMove && !AppSP.get().isLocked) {

                            imageMatrix().postTranslate(dx, dy);

                            PageImageState.get().isAutoFit = false;
                            invalidateAndMsg();

                            x = event.getX();
                            y = event.getY();
                        }

                    }

                }

                if (event.getPointerCount() == 2) {
                    isIgronerClick = true;
                    setPinching(true);

                    LOG.d("TEST", "action ACTION_MOVE 2");
                    if (cx == 0) {
                        cx = centerX(event);
                        cy = centerY(event);
                    }
                    final float nDistance = discance(event);

                    if (distance == 0) {
                        distance = nDistance;
                    }

                    final float scale = nDistance / distance;
                    distance = nDistance;
                    final float centerX = centerX(event);
                    final float centerY = centerY(event);

                    final float values[] = new float[9];
                    imageMatrix().getValues(values);

                    if (AppState.get().isZoomInOutWithLock || !AppSP.get().isLocked) {
                        LOG.d("postScale", scale, values[Matrix.MSCALE_X]);
                        if (values[Matrix.MSCALE_X] > 0.3f || scale > 1) {
                            imageMatrix().postScale(scale, scale, centerX, centerY);
                            EventBus.getDefault().post(new MessagePageXY(MessagePageXY.TYPE_HIDE));
                        }
                    }
                    final float dx = centerX - cx;
                    final float dy = centerY - cy;
                    if (AppState.get().isZoomInOutWithLock || !AppSP.get().isLocked) {
                        imageMatrix().postTranslate(dx, dy);
                        EventBus.getDefault().post(new MessagePageXY(MessagePageXY.TYPE_HIDE));
                    }
                    cx = centerX(event);
                    cy = centerY(event);

                    PageImageState.get().isAutoFit = false;
                    invalidateAndMsg();

                }
            } else if (action == MotionEvent.ACTION_POINTER_UP) {
                LOG.d("TEST", "action ACTION_POINTER_UP");
                setPinching(false);
                // isDoubleTouch = true;
                int actionIndex = event.getActionIndex();
                LOG.d("TEST", "actionIndex " + actionIndex);
                if (actionIndex == 1) {
                    x = event.getX();
                    y = event.getY();
                } else {
                    x = event.getX(1);
                    y = event.getY(1);
                }
                cx = 0;
                distance = 0;

            } else if (action == MotionEvent.ACTION_UP) {
                setPinching(false);
                brightnessHelper.onActionUp();

                LOG.d("TEST", "action ACTION_UP", "long: " + isLognPress);
                distance = 0;
                isReadyForMove = false;
                cx = 0;
                cy = 0;

                if (isLognPress) {
                    String selectText = selectText(event.getX(), event.getY(), xInit, yInit);
                    if (selectText != null && selectText.contains(" ")) {
                        EventBus.getDefault().post(new MessagePageXY(MessagePageXY.TYPE_SHOW, -1, xInit, yInit, event.getX(), event.getY()));
                    }

                } else if (BookCSS.get().isTextFormat()) {
                    if (!TempHolder.isSeaching) {
                        selectText(event.getX(), event.getY(), event.getX(), event.getY());
                        if (!TxtUtils.isFooterNote(AppState.get().selectedText)) {
                            PageImageState.get().cleanSelectedWords();
                            AppState.get().selectedText = null;
                            invalidate();

                            EventBus.getDefault().post(new MessagePageXY(MessagePageXY.TYPE_HIDE));
                        }
                    }
                }

                if (!isIgronerClick && onMarkTap(event.getX(), event.getY())) {
                    // The tap was on a mark, and that is all it meant
                    isIgronerClick = false;
                } else if (!isIgronerClick && onEraseTap(event.getX(), event.getY())) {
                    // The eraser took an annotation of the PDF, and that is all the tap meant
                    isIgronerClick = false;
                } else if (!isIgronerClick) {
                    int target = 0;
                    Pair<PageLink, Annotation> pair = getPageLinkClicked(event.getX(), event.getY());
                    PageLink pageLink = pair.first;
                    if (pageLink != null) {
                        target = pageLink.targetPage;
                        if (AppSP.get().isDouble && target != -1) {
                            target = pageLink.targetPage / 2;
                        }
                        TempHolder.get().linkPage = target;
                        LOG.d("Go to targetPage", target);
                    }
                    final Annotation drawing = drawingAt(event.getX(), event.getY());
                    if (drawing != null) {
                        // A drawing - ahead of a link under it: picked out, with the drawing panel
                        // to move, restyle or erase it
                        selectAnnotation(drawing, lastClickedNumber);
                        EventBus.getDefault().post(new MessageSelectAnnotation(drawing));
                        isIgronerClick = false;
                        return true;
                    }
                    if (pair.second != null && TxtUtils.isNotEmpty(pair.second.text)) {
                        Dialogs.showTextDialog(getContext(), pair.second.text);
                    }

                    if (isMoveNextPrev != 0) {
                        LOG.d("isMoveNextPrev", isMoveNextPrev);
                        EventBus.getDefault().post(new MessageEvent(MessageEvent.MESSAGE_GOTO_PAGE_SWIPE, isMoveNextPrev));
                    } else if (TxtUtils.isNotEmpty(AppState.get().selectedText)) {
                        EventBus.getDefault().post(new MessageEvent(MessageEvent.MESSAGE_SELECTED_TEXT));
                    } else if (pageLink != null) {
                        EventBus.getDefault().post(new MessageEvent(MessageEvent.MESSAGE_GOTO_PAGE_BY_LINK, target, pageLink.url));
                    } else {
                        LOG.d("PageImaveView MESSAGE_PERFORM_CLICK", 1);
                        EventBus.getDefault().post(new MessageEvent(MessageEvent.MESSAGE_PERFORM_CLICK, event.getX(), event.getY()));
                    }
                } else {

                    if (TxtUtils.isNotEmpty(AppState.get().selectedText)) {
                        EventBus.getDefault().post(new MessageEvent(MessageEvent.MESSAGE_SELECTED_TEXT));
                    } else if (AppState.get().isSelectTexByTouch && !new ClickUtils().isClickCenter(event.getX(), event.getY())) {
                        LOG.d("PageImaveView MESSAGE_PERFORM_CLICK", 2);
                        EventBus.getDefault().post(new MessageEvent(MessageEvent.MESSAGE_PERFORM_CLICK, event.getX(), event.getY()));
                    }


                }

                isIgronerClick = false;
            } else if (action == MotionEvent.ACTION_CANCEL) {
                LOG.d("TEST", "action ACTION_CANCEL");
                setPinching(false);
            }

            return true;
        }

    }

}
