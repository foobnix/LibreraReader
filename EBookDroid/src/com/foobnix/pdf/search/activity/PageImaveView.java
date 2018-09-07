package com.foobnix.pdf.search.activity;

import java.util.Collections;
import java.util.List;

import org.ebookdroid.LibreraApp;
import org.ebookdroid.core.codec.PageLink;
import org.ebookdroid.droids.mupdf.codec.TextWord;
import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;

import com.foobnix.android.utils.Apps;
import com.foobnix.android.utils.Dips;
import com.foobnix.android.utils.LOG;
import com.foobnix.android.utils.TxtUtils;
import com.foobnix.android.utils.Vibro;
import com.foobnix.pdf.info.R;
import com.foobnix.pdf.info.view.BrightnessHelper;
import com.foobnix.pdf.info.wrapper.AppState;
import com.foobnix.pdf.info.wrapper.MagicHelper;
import com.foobnix.pdf.search.activity.msg.InvalidateMessage;
import com.foobnix.pdf.search.activity.msg.MessageAutoFit;
import com.foobnix.pdf.search.activity.msg.MessageCenterHorizontally;
import com.foobnix.pdf.search.activity.msg.MessageEvent;
import com.foobnix.pdf.search.activity.msg.MovePageAction;
import com.foobnix.pdf.search.activity.msg.TextWordsMessage;
import com.foobnix.sys.ClickUtils;
import com.foobnix.sys.TempHolder;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.RectF;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Handler;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.Scroller;
import android.widget.Toast;

public class PageImaveView extends View {

    private Drawable imageDrawable;
    int drawableHeight, drawableWidth;
    GestureDetector gestureDetector;
    private Handler handler;
    Scroller scroller;
    ImageSimpleGestureListener imageGestureListener;

    Paint paintWrods = new Paint();

    private boolean isReadyForMove = false;
    private boolean isLognPress = false;
    private boolean isIgronerClick = false;

    private int isMoveNextPrev = 0;

    float x, y, xInit, yInit, cx, cy, distance = 0;

    private int pageNumber;
    ClickUtils clickUtils;

    BrightnessHelper brightnessHelper;

    public PageImaveView(final Context context, final AttributeSet attrs) {
        super(context, attrs);

        handler = new Handler();
        scroller = new Scroller(getContext(), new AccelerateDecelerateInterpolator());
        imageGestureListener = new ImageSimpleGestureListener();
        gestureDetector = new GestureDetector(context, imageGestureListener);

        paintWrods.setStrokeWidth(Dips.dpToPx(1));
        paintWrods.setTextSize(30);

        EventBus.getDefault().register(this);
        clickUtils = new ClickUtils();
        brightnessHelper = new BrightnessHelper();
    }

    public RectF transform(RectF origin, int number) {
        RectF r = new RectF(origin);
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
        try {
            if (AppState.get().isDouble && number != 0) {

                int page = pageNumber * 2;
                if (AppState.get().isDoubleCoverAlone) {
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

    public synchronized List<PageLink> getPageLinks(int number) {
        if (AppState.get().isCut || AppState.get().isCrop) {
            return Collections.emptyList();
        }

        List<PageLink> all = getPageLinksInner(number);
        if (all == null) {
            return Collections.emptyList();
        }
        return all;

    }

    public synchronized List<PageLink> getPageLinksInner(int number) {

        try {
            if (AppState.get().isDouble && number != 0) {

                int page = pageNumber * 2;
                if (AppState.get().isDoubleCoverAlone) {
                    page--;
                }

                List<PageLink> t = null;
                if (number == 1) {
                    t = PageImageState.get().pagesLinks.get(page);
                } else if (number == 2) {
                    t = PageImageState.get().pagesLinks.get(page + 1);
                }

                for (PageLink l : t) {
                    LOG.d("PageLinks targetPage after", l.targetPage);
                    l.number = number;
                    LOG.d("PageLinks targetPage before", l.targetPage);
                }

                return t;
            } else {
                return PageImageState.get().pagesLinks.get(pageNumber);
            }
        } catch (Exception e) {
            LOG.e(e);
            return null;
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
    public void onAutoFit(MessageAutoFit event) {
        LOG.d("onAutoFit recive");
        if (pageNumber == event.getPage()) {
            LOG.d("onAutoFit recive", event.getPage(), pageNumber);
            autoFit();
            invalidate();
            isFirstZoomInOut = true;
        }
    }

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

    static volatile boolean isFirstZoomInOut = true;
    static volatile boolean prevLock = false;

    class ImageSimpleGestureListener extends GestureDetector.SimpleOnGestureListener {

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
                        prevLock = AppState.get().isLocked;
                        AppState.get().isLocked = false;
                        invalidateAndMsg();
                        PageImageState.get().isAutoFit = false;

                    } else {
                        AppState.get().isLocked = prevLock;
                        if (AppState.get().isTextFormat()) {
                            AppState.get().isLocked = true;
                        }
                        isLognPress = true;
                        PageImageState.get().isAutoFit = true;
                        autoFit();
                        invalidateAndMsg();
                        isFirstZoomInOut = true;

                    }
                } else if (AppState.get().doubleClickAction1 == AppState.DOUBLE_CLICK_CLOSE_BOOK) {
                    EventBus.getDefault().post(new MessageEvent(MessageEvent.MESSAGE_CLOSE_BOOK, e.getX(), e.getY()));
                } else if (AppState.get().doubleClickAction1 == AppState.DOUBLE_CLICK_CLOSE_BOOK_AND_APP) {
                    EventBus.getDefault().post(new MessageEvent(MessageEvent.MESSAGE_CLOSE_BOOK_APP, e.getX(), e.getY()));
                } else if (AppState.get().doubleClickAction1 == AppState.DOUBLE_CLICK_CLOSE_HIDE_APP) {
                    Apps.showDesctop(getContext());
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
        };

        @Override
        public boolean onFling(final MotionEvent e1, final MotionEvent e2, final float velocityX, final float velocityY) {

            if (e1.getX() < BrightnessHelper.BRIGHTNESS_WIDTH) {
                return false;
            }
            if (AppState.get().selectedText != null) {
                return false;
            }
            if (AppState.get().isLocked) {
                return false;
            }
            if (isReadyForMove) {
                isIgronerClick = true;
                scroller.fling((int) e2.getX(), (int) e2.getY(), (int) velocityX / 3, (int) velocityY / 3, Integer.MIN_VALUE, Integer.MAX_VALUE, Integer.MIN_VALUE, Integer.MAX_VALUE);
                handler.post(scrolling);
            }
            return true;
        }

        @Override
        public void onLongPress(MotionEvent e) {
            isIgronerClick = true;

            if (!AppState.get().isAllowTextSelection) {
                if (TempHolder.get().isAllowTextSelectionFirstTime) {
                    Toast.makeText(LibreraApp.context, R.string.text_highlight_mode_is_disable, Toast.LENGTH_LONG).show();
                    TempHolder.get().isAllowTextSelectionFirstTime = false;
                }
                return;
            }

            Vibro.vibrate();
            if (AppState.get().isCut || AppState.get().isCrop) {
                Toast.makeText(LibreraApp.context, R.string.the_page_is_clipped_the_text_selection_does_not_work, Toast.LENGTH_LONG).show();
                return;
            }
            isLognPress = true;
            xInit = e.getX();
            yInit = e.getY();
            String selectText = selectText(xInit, yInit, e.getX(), e.getY());
            if (TxtUtils.isEmpty(selectText)) {
                AppState.get().selectedText = null;
            }
        }

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
                isLognPress = false;
                isMoveNextPrev = 0;
            } else if (action == MotionEvent.ACTION_MOVE) {
                if (event.getPointerCount() == 1) {
                    LOG.d("TEST", "action ACTION_MOVE 1");
                    final float dx = event.getX() - x;
                    final float dy = event.getY() - y;

                    if (isLognPress) {
                        selectText(event.getX(), event.getY(), xInit, yInit);
                    } else {

                        if (AppState.get().isLocked) {
                            isReadyForMove = false;
                            isIgronerClick = false;
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

                        if (!isBrightness && isReadyForMove && !AppState.get().isLocked) {

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

                    if (AppState.get().isAllowMoveTwoFingerWithLock || !AppState.get().isLocked) {
                        LOG.d("postScale", scale, values[Matrix.MSCALE_X]);
                        if (values[Matrix.MSCALE_X] > 0.3f || scale > 1) {
                            imageMatrix().postScale(scale, scale, centerX, centerY);
                        }
                    }
                    final float dx = centerX - cx;
                    final float dy = centerY - cy;
                    if (AppState.get().isAllowMoveTwoFingerWithLock || !AppState.get().isLocked) {
                        imageMatrix().postTranslate(dx, dy);
                    }
                    cx = centerX(event);
                    cy = centerY(event);

                    PageImageState.get().isAutoFit = false;
                    invalidateAndMsg();
                }
            } else if (action == MotionEvent.ACTION_POINTER_UP) {
                LOG.d("TEST", "action ACTION_POINTER_UP");
                // isDoubleTouch = true;
                int actionIndex = 1;
                if (Build.VERSION.SDK_INT > 7) {
                    actionIndex = event.getActionIndex();
                }
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
                brightnessHelper.onActionUp();

                LOG.d("TEST", "action ACTION_UP", "long: " + isLognPress);
                distance = 0;
                isReadyForMove = false;
                cx = 0;
                cy = 0;

                if (isLognPress) {
                    selectText(event.getX(), event.getY(), xInit, yInit);
                } else if (AppState.get().isTextFormat()) {
                    selectText(event.getX(), event.getY(), event.getX(), event.getY());
                    if (!TxtUtils.isFooterNote(AppState.get().selectedText)) {
                        PageImageState.get().cleanSelectedWords();
                        AppState.get().selectedText = null;
                        invalidate();
                    }
                }

                if (!isIgronerClick) {
                    int target = 0;
                    PageLink pageLink = getPageLinkClicked(event.getX(), event.getY());
                    if (pageLink != null) {
                        target = pageLink.targetPage;
                        if (AppState.get().isDouble && target != -1) {
                            target = pageLink.targetPage / 2;
                        }
                        TempHolder.get().linkPage = target;
                        LOG.d("Go to targetPage", target);
                    }

                    if (isMoveNextPrev != 0) {
                        LOG.d("isMoveNextPrev", isMoveNextPrev);
                        EventBus.getDefault().post(new MessageEvent(MessageEvent.MESSAGE_GOTO_PAGE_SWIPE, isMoveNextPrev));
                    } else if (TxtUtils.isNotEmpty(AppState.get().selectedText)) {
                        EventBus.getDefault().post(new MessageEvent(MessageEvent.MESSAGE_SELECTED_TEXT));
                    } else if (pageLink != null) {
                        EventBus.getDefault().post(new MessageEvent(MessageEvent.MESSAGE_GOTO_PAGE_BY_LINK, target, pageLink.url));
                    } else {
                        EventBus.getDefault().post(new MessageEvent(MessageEvent.MESSAGE_PERFORM_CLICK, event.getX(), event.getY()));
                    }
                } else if (TxtUtils.isNotEmpty(AppState.get().selectedText)) {
                    EventBus.getDefault().post(new MessageEvent(MessageEvent.MESSAGE_SELECTED_TEXT));
                }

                isIgronerClick = false;
            } else if (action == MotionEvent.ACTION_CANCEL) {
                LOG.d("TEST", "action ACTION_CANCEL");
            }

            return true;
        }

    };

    public void invalidateAndMsg() {
        EventBus.getDefault().post(new InvalidateMessage());
    };

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
                handler.post(scrolling);
            }

        }
    };
    private Bitmap bitmap;

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        LOG.d("ImagePageFragment", "onDetachedFromWindow");
        if (bitmap != null && !bitmap.isRecycled()) {
            LOG.d("recycle onDetachedFromWindow");
            bitmap.recycle();
            bitmap = null;
        }
        imageDrawable = null;
        EventBus.getDefault().unregister(this);
    }

    @Override
    public boolean onTouchEvent(final MotionEvent event) {
        gestureDetector.onTouchEvent(event);
        imageGestureListener.onTouchEvent(event);
        return true;
    }

    static Paint rect = new Paint();
    static {
        rect.setColor(Color.DKGRAY);
        rect.setStrokeWidth(Dips.dpToPx(1));
        rect.setStyle(Style.STROKE);

    }
    int dp1 = Dips.dpToPx(1);

    @Override
    protected void onDraw(final Canvas canvas) {
        super.onDraw(canvas);
        try {
            if (AppState.get().isOLED && !AppState.get().isDayNotInvert /* && MagicHelper.getBgColor() == Color.BLACK */) {
                canvas.drawColor(Color.BLACK);
            } else {
                canvas.drawColor(MagicHelper.ligtherColor(MagicHelper.getBgColor()));
            }

            final int saveCount = canvas.getSaveCount();
            canvas.save();

            canvas.concat(imageMatrix());

            if (imageDrawable != null) {
                imageDrawable.draw(canvas);
            }

            if (PageImageState.get().isShowCuttingLine && AppState.get().isCut == false) {
                int offset = drawableWidth * AppState.get().cutP / 100;
                canvas.drawLine(offset, 0, offset, drawableHeight, paintWrods);
            }

            List<TextWord> selectedWords = PageImageState.get().getSelectedWords(pageNumber);
            if (selectedWords != null) {
                for (TextWord tw : selectedWords) {
                    drawWord(canvas, tw);
                }
            }

            if (AppState.get().isOLED && !AppState.get().isDayNotInvert /* && !TempHolder.get().isTextFormat */) {
                canvas.drawRect(-dp1, 0, drawableWidth + dp1, drawableHeight, rect);
            }

            if (!AppState.get().isCut && !AppState.get().isCrop) {

                paintWrods.setColor(AppState.get().isDayNotInvert ? Color.BLUE : Color.YELLOW);
                paintWrods.setAlpha(60);

                if (!AppState.get().isTextFormat()) {
                    if (AppState.get().isDouble) {
                        for (PageLink pl : getPageLinks(1)) {
                            drawLink(canvas, pl);
                        }

                        for (PageLink pl : getPageLinks(2)) {
                            drawLink(canvas, pl);
                        }
                    } else {
                        for (PageLink pl : getPageLinks(0)) {
                            drawLink(canvas, pl);
                        }
                    }
                }
            }

            canvas.restoreToCount(saveCount);

        } catch (Exception e) {
            LOG.e(e);
        }
    }

    public void addBitmap(Bitmap bitmap) {
        this.bitmap = bitmap;
        imageDrawable = new BitmapDrawable(getResources(), bitmap);
        drawableHeight = bitmap.getHeight();
        drawableWidth = bitmap.getWidth();
        imageDrawable.setBounds(0, 0, drawableWidth, drawableHeight);

        autoFit();
        invalidate();

        LOG.d("addBitmap", bitmap.getWidth(), bitmap.getHeight(), "Real WH:", getWidth(), getHeight());
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

    public static int MIN = Dips.dpToPx(15);

    public PageLink getPageLinkClicked(float x1, float y1) {

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
        if (AppState.get().isDouble) {
            firstNumber = x1 < drawableWidth / 2 ? 1 : 2;
        }

        List<PageLink> pageLinks = getPageLinks(firstNumber);
        if (pageLinks == null) {
            return null;
        }

        LOG.d("getPageLinkClicked", x1, "w", drawableWidth, firstNumber, "links", pageLinks.size());

        for (PageLink link : pageLinks) {
            if (link == null) {
                continue;
            }
            RectF wordRect = transform(link.sourceRect, firstNumber);
            boolean intersects = RectF.intersects(wordRect, tapRect);
            if (intersects) {
                return link;
            }
        }

        return null;

    }

    public String selectText(float x1, float y1, float xInit, float yInit) {
        if (!AppState.get().isDouble && getPageText(0) == null) {
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
        if (AppState.get().isDouble) {
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
                if (!AppState.get().isTextFormat() && (textWord.left < 0 || textWord.top < 0)) {
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

        String txt = TxtUtils.filterString(build.toString());
        AppState.get().selectedText = txt;
        invalidate();
        return txt;
    }

}
