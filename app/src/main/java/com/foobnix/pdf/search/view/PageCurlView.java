package com.foobnix.pdf.search.view;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.Picture;
import android.graphics.LinearGradient;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PointF;
import android.graphics.RectF;
import android.graphics.Region;
import android.graphics.Shader;
import android.os.Build;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.DecelerateInterpolator;

import com.foobnix.android.utils.Dips;
import com.foobnix.android.utils.LOG;
import com.foobnix.model.AppSP;
import com.foobnix.model.AppState;
import com.foobnix.pdf.info.wrapper.MagicHelper;

/**
 * Turns the pages of the paged view as the pages of a paper book: the page is lifted by its
 * corner and folded over, the page under it showing as it goes.
 * <p>
 * Drawn over the pager while a page turns, from the pager's own page views - drawn into this
 * view's canvas, not copied into bitmaps: a page zoomed in has its sharp part on the GPU, and a
 * software canvas cannot draw that. Hidden again as soon as the turn ends, the pager then
 * showing the page the turn came to.
 * <p>
 * One page: the page is bound at its left edge, and the back of the folded part is paper, the
 * page's print showing through it. Two pages: only the right page turns, around the middle of
 * the spread, and its back is the left page of the next spread - as the leaf of a book.
 * Right-to-left books are the same, mirrored.
 * <p>
 * All the geometry is in "book" coordinates: left to right as the book is read, so the
 * mirrored book is worked out as the other one and drawn mirrored.
 */
public class PageCurlView extends View {

    public interface Host {
        /** The view of the page {@code delta} pages on from the one shown, null when there is none. */
        View pageView(int delta);

        /** The turn is over: the page {@code delta} pages on is to be shown, without animation. */
        void turned(int delta);

        /** Where the page's picture stands on its view, or null before it is shown. */
        RectF pageRect(View pageView);
    }

    private static final long TURN_MS = 450;

    private Host host;

    /** The page that is folded over, and the one under it. */
    private View top, under;
    /** +1 turning to the next page (the shown one is folded away), -1 to the previous one. */
    private int delta;
    /**
     * The shown page is the one lifted, and the turn is over when it lies on the other side;
     * otherwise the page turned to comes back over the shown one.
     */
    private boolean peel;
    private boolean twoPages, mirror;
    private int paper;
    /** x of the binding: the edge the turning page is held at. */
    private float spine;
    /**
     * The turning page's other edges, in book coordinates: the page's picture, not the view - a
     * page narrower or shorter than the screen is turned by its own corner, not the screen's.
     */
    private float sheetR, sheetT, sheetB;
    /** The corner of the turning page as it lies flat, and where it is now. */
    private final PointF corner = new PointF();
    private final PointF point = new PointF();
    /** The corner when the page is turned all the way over. */
    private final PointF turnedOver = new PointF();

    private ValueAnimator animator;

    private boolean dragging;
    /** Where the finger went down, in the book's way. */
    private float grabX, grabY;

    /**
     * The turning page as pictures, for bending it: its front, and its back - paper with the
     * front's print showing through, or of two pages the next spread's left page. As large as
     * the turning page, with a clear line of {@link #pad} round it where it is drawn as
     * triangles: past the page's edge the picture then gives nothing, softly. On Android 9 it is
     * drawn as a bitmap mesh, upside down when the page is held by its top corner, so the mesh
     * goes from the part furthest from the corner on: the parts nearer it lie over the others.
     * Null where pictures cannot be made: the page is then folded flat.
     */
    private Bitmap frontSheet, backSheet;
    private BitmapShader frontShader, backShader;
    private boolean flipped;
    private int pad;
    /**
     * The bent page as triangles laid along the fold, Android 10 on: the light changes across
     * the roll only, so with the points in lines along it the shading is even. On a mesh of the
     * page's own squares a thin roll came out in blocks.
     */
    private static final boolean VERTICES = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q;
    private static final int ROWS = 64;
    /** Steps round a quarter of the roll. */
    private static final int ROLL = 16;
    /** The flat page's far edge, then the quarter roll up to its top. */
    private static final int FRONT_COLS = ROLL + 2;
    /** A soft edge, the quarter roll down from its top, then the back lying flat in two steps. */
    private static final int BACK_COLS = ROLL + 4;
    private final float[] verts = new float[(ROWS + 1) * BACK_COLS * 2];
    private final float[] texs = new float[verts.length];
    private final int[] vertColors = new int[verts.length];
    private final short[] frontIndices = indices(FRONT_COLS);
    private final short[] backIndices = indices(BACK_COLS);
    private final Paint vertexPaint = new Paint(Paint.FILTER_BITMAP_FLAG);
    private static final int MESH_W = 32;
    private static final int MESH_H = 64;
    private final float[] mesh = new float[(MESH_W + 1) * (MESH_H + 1) * 2];
    private final int[] meshColors = new int[(MESH_W + 1) * (MESH_H + 1)];
    private final Paint meshPaint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);

    private final float[] sheet = new float[8];
    private final float[] flap = new float[16];
    private final float[] kept = new float[16];
    private final float[] back = new float[16];
    private final Path path = new Path();
    private final Path flapPath = new Path();
    private final Path backPath = new Path();
    private final Matrix reflect = new Matrix();
    private final Matrix spineMirror = new Matrix();
    private final float[] values = new float[9];
    private final Paint shade = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint edge = new Paint(Paint.ANTI_ALIAS_FLAG);

    public PageCurlView(Context context) {
        super(context);
        init();
    }

    public PageCurlView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        edge.setStyle(Paint.Style.STROKE);
        edge.setStrokeJoin(Paint.Join.ROUND);
        // Hidden, not gone: laid out with the pager all the time, so it has its size when a
        // turn starts - gone, it had none, and no page ever turned
        setVisibility(INVISIBLE);
    }

    /** Pages are turned as in a paper book: the setting is on, and the pages are side by side. */
    public static boolean isOn() {
        return AppState.get().isBookPageCurl && AppState.get().rotateViewPager == 0;
    }

    public void setHost(Host host) {
        this.host = host;
    }

    /** A page is turning, by the finger or by itself. */
    public boolean isBusy() {
        return top != null;
    }

    public boolean isDragging() {
        return dragging;
    }

    /** Takes a turn over: the pages and the book's way at the time the turn starts. */
    private boolean begin(int delta) {
        if (host == null || getWidth() == 0 || getHeight() == 0) {
            return false;
        }
        final View shown = host.pageView(0);
        final View other = host.pageView(delta);
        if (shown == null || other == null) {
            LOG.d("PageCurl", "no page to turn to", delta, shown != null, other != null);
            return false;
        }
        this.delta = delta;
        twoPages = AppSP.get().isDouble && !AppSP.get().isCut;
        // Of two pages, back is as in a book: the left page is lifted by its outer corner and
        // laid over to the right, the previous spread under it and on its back - the turn
        // forward, mirrored. Of one page, the previous page comes back over the shown one.
        peel = delta > 0 || twoPages;
        top = peel ? shown : other;
        under = peel ? other : shown;
        mirror = AppSP.get().isRTL != (delta < 0 && twoPages);
        paper = MagicHelper.getBgColor();
        // The pictures of the two pages together, on the screen
        final int w = getWidth();
        final int h = getHeight();
        RectF rect = null;
        for (View v : new View[]{shown, other}) {
            final RectF r = host.pageRect(v);
            if (r != null && r.width() > 1 && r.height() > 1) {
                if (rect == null) {
                    rect = new RectF(r);
                } else {
                    rect.union(r);
                }
            }
        }
        if (rect == null || !rect.intersect(0, 0, w, h) || rect.width() < w * 0.2f || rect.height() < h * 0.1f) {
            rect = new RectF(0, 0, w, h);
        }
        final float left = mirror ? w - rect.right : rect.left;
        final float right = mirror ? w - rect.left : rect.right;
        spine = twoPages ? (left + right) / 2f : left;
        sheetR = right;
        sheetT = rect.top;
        sheetB = rect.bottom;
        setVisibility(VISIBLE);
        return true;
    }

    private void setCorner(boolean topCorner) {
        final float y = topCorner ? sheetT : sheetB;
        corner.set(sheetR, y);
        turnedOver.set(2 * spine - sheetR, y);
    }

    /** Turns the page by itself, as by a tap or a key: lifted by its bottom corner. */
    public boolean turn(int delta) {
        finishNow();
        if (!begin(delta)) {
            return false;
        }
        setCorner(false);
        prepareSheets();
        final PointF from = new PointF();
        final PointF to = new PointF();
        from.set(peel ? corner : turnedOver);
        to.set(peel ? turnedOver : corner);
        animate(from, to, (sheetB - sheetT) * 0.12f, TURN_MS, new AccelerateDecelerateInterpolator(), true);
        return true;
    }

    /** The finger has started to move a page, from where it went down; x and y on this view. */
    public boolean startDrag(int delta, float x, float y) {
        finishNow();
        if (!begin(delta)) {
            return false;
        }
        // Held by the corner nearest the finger
        setCorner(y < (sheetT + sheetB) / 2f);
        prepareSheets();
        grabX = book(x);
        grabY = y;
        dragging = true;
        drag(x, y);
        return true;
    }

    public void drag(float x, float y) {
        if (!dragging) {
            return;
        }
        final float bx = book(x);
        final float sheetW = sheetR - spine;
        // The page moves by as much as the finger from where it went down, so it starts to lift
        // from where it lies instead of jumping to the finger; on the way the corner comes
        // under the finger
        final float travel = (float) Math.hypot(bx - grabX, y - grabY);
        final float k = Math.max(0f, 1f - travel / Math.max(1f, sheetW * 0.6f));
        final float py = y + (corner.y - grabY) * k;
        if (peel) {
            setPoint(bx + (corner.x - grabX) * k, py);
        } else {
            // The page comes back from over the spine: all turned over where the finger went
            // down, flat again at the page's far edge
            final float t = Math.max(0, Math.min(1, (bx - grabX) / Math.max(sheetW * 0.5f, sheetR - grabX)));
            setPoint(turnedOver.x + t * (corner.x - turnedOver.x), py);
        }
        invalidate();
    }

    /** The finger is lifted, moving at {@code velocityX} pixels a second across the screen. */
    public void release(float velocityX) {
        if (!dragging) {
            return;
        }
        dragging = false;
        final float vx = mirror ? -velocityX : velocityX;
        final float fling = Dips.dpToPx(400);
        final float done = (corner.x - point.x) / (corner.x - turnedOver.x);
        final boolean turned;
        if (peel) {
            turned = vx < -fling || (vx <= fling && done > 0.2f);
        } else {
            turned = vx > fling || (vx >= -fling && done < 0.8f);
        }
        final PointF to = new PointF();
        to.set(turned == peel ? turnedOver : corner);
        final float way = Math.abs(to.x - point.x) / Math.abs(corner.x - turnedOver.x);
        animate(new PointF(point.x, point.y), to, 0, (long) (120 + 280 * way), new DecelerateInterpolator(), turned);
    }

    /** The finger's move is taken away: the page falls back. */
    public void cancelDrag() {
        if (!dragging) {
            return;
        }
        dragging = false;
        final PointF to = new PointF();
        to.set(peel ? corner : turnedOver);
        animate(new PointF(point.x, point.y), to, 0, 200, new DecelerateInterpolator(), false);
    }

    /** A turn on its way is brought to its end at once: the page it goes to is shown. */
    public void finishNow() {
        if (animator != null) {
            animator.end();
        } else if (dragging) {
            dragging = false;
            hide();
        }
    }

    private void animate(final PointF from, final PointF to, final float lift, long duration,
                         android.animation.TimeInterpolator interpolator, final boolean turned) {
        if (animator != null) {
            animator.cancel();
        }
        final float liftDir = corner.y == 0 ? 1 : -1;
        final ValueAnimator a = ValueAnimator.ofFloat(0, 1);
        a.setDuration(duration);
        a.setInterpolator(interpolator);
        a.addUpdateListener(animation -> {
            final float t = (float) animation.getAnimatedValue();
            final float x = from.x + (to.x - from.x) * t;
            final float y = from.y + (to.y - from.y) * t + liftDir * lift * (float) Math.sin(Math.PI * t);
            setPoint(x, y);
            invalidate();
        });
        a.addListener(new AnimatorListenerAdapter() {
            boolean cancelled;

            @Override
            public void onAnimationCancel(Animator animation) {
                cancelled = true;
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                if (animator == a) {
                    animator = null;
                }
                if (cancelled) {
                    return;
                }
                final int d = delta;
                hide();
                if (turned && host != null) {
                    // In the same pass as the hiding: the next frame is the pager's, showing
                    // the page the turn came to
                    host.turned(d);
                }
            }
        });
        animator = a;
        a.start();
    }

    private void hide() {
        top = null;
        under = null;
        recycleSheets();
        setVisibility(INVISIBLE);
    }

    private void recycleSheets() {
        if (frontSheet != null) {
            frontSheet.recycle();
            frontSheet = null;
        }
        if (backSheet != null) {
            backSheet.recycle();
            backSheet = null;
        }
        frontShader = null;
        backShader = null;
        vertexPaint.setShader(null);
    }

    /**
     * Makes the pictures of the turning page, from the page views as they are drawn now. Through
     * a Picture: a page zoomed in has its sharp part on the GPU, which a software canvas cannot
     * draw, and a Picture made into a bitmap is drawn by the GPU then.
     */
    private void prepareSheets() {
        recycleSheets();
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) {
            return;
        }
        final int sw = Math.max(1, Math.round(sheetR - spine));
        final int h = Math.max(1, Math.round(sheetB - sheetT));
        flipped = !VERTICES && corner.y == sheetT;
        pad = VERTICES ? 1 : 0;
        final int bw = sw + 2 * pad;
        final int bh = h + 2 * pad;
        try {
            final long start = System.currentTimeMillis();
            Picture picture = new Picture();
            Canvas c = picture.beginRecording(bw, bh);
            c.translate(pad, pad);
            c.clipRect(0, 0, sw, h);
            sheetSpace(c, h);
            c.translate(-spine, -sheetT);
            drawPage(c, top);
            picture.endRecording();
            frontSheet = Bitmap.createBitmap(picture, bw, bh, Bitmap.Config.ARGB_8888);

            picture = new Picture();
            c = picture.beginRecording(bw, bh);
            c.translate(pad, pad);
            c.clipRect(0, 0, sw, h);
            if (twoPages) {
                // The back at x is the next spread's left page at the spine's other side
                sheetSpace(c, h);
                c.translate(spine, -sheetT);
                c.scale(-1, 1);
                drawPage(c, under);
            } else {
                c.drawColor(paper);
                c.drawBitmap(frontSheet, -pad, -pad, null);
                // Paper: the print of the other side shows through, faint
                c.drawColor((paper & 0x00FFFFFF) | 0xF0000000);
            }
            picture.endRecording();
            backSheet = Bitmap.createBitmap(picture, bw, bh, Bitmap.Config.ARGB_8888);
            if (VERTICES) {
                frontShader = new BitmapShader(frontSheet, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP);
                backShader = new BitmapShader(backSheet, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP);
            }
            LOG.d("PageCurl", "sheets", sw + "x" + h, twoPages ? "two" : "one", mirror ? "rtl" : "",
                  System.currentTimeMillis() - start, "ms");
        } catch (Throwable e) {
            LOG.e(e);
            recycleSheets();
        }
    }

    /** Turns the page upside down in the picture when it is held by its top corner. */
    private void sheetSpace(Canvas c, int h) {
        if (flipped) {
            c.translate(0, h);
            c.scale(1, -1);
        }
    }

    private float book(float x) {
        return mirror ? getWidth() - x : x;
    }

    /**
     * Puts the corner where it can be: the page stays bound at the spine and in one piece. It
     * may go past the page's top or bottom with the finger, from either corner.
     */
    private void setPoint(float x, float y) {
        final float sheetW = sheetR - spine;
        final float h = sheetB - sheetT;
        x = Math.min(x, sheetR);
        // No further from the bound corner on its edge than the page is wide
        float dx = x - spine;
        float dy = y - corner.y;
        float d = (float) Math.hypot(dx, dy);
        if (d > sheetW && d > 0) {
            x = spine + dx * sheetW / d;
            y = corner.y + dy * sheetW / d;
        }
        // Nor from the other bound corner than the page's diagonal
        final float otherY = sheetT + sheetB - corner.y;
        final float diagonal = (float) Math.hypot(sheetW, h);
        dx = x - spine;
        dy = y - otherY;
        d = (float) Math.hypot(dx, dy);
        if (d > diagonal && d > 0) {
            x = spine + dx * diagonal / d;
            y = otherY + dy * diagonal / d;
        }
        point.set(x, y);
    }


    @Override
    protected void onDraw(Canvas canvas) {
        if (top == null || under == null) {
            return;
        }
        try {
            drawTurn(canvas);
        } catch (Exception e) {
            LOG.e(e);
        }
        if (dragging) {
            // The pages may still be coming in under a finger held still
            postInvalidateOnAnimation();
        }
    }

    private void drawTurn(Canvas canvas) {
        if (frontSheet != null && backSheet != null) {
            drawCurl(canvas);
            return;
        }
        final int w = getWidth();
        final int h = getHeight();
        canvas.save();
        if (mirror) {
            canvas.scale(-1, 1, w / 2f, 0);
        }
        canvas.drawColor(paper);

        final float cx = corner.x;
        final float cy = corner.y;
        final float px = point.x;
        final float py = point.y;
        final float dist = (float) Math.hypot(cx - px, cy - py);
        if (dist < 0.5f) {
            drawPage(canvas, top);
            canvas.restore();
            return;
        }
        // The fold: half way between the corner and where it is, across the way it went
        final float nx = (cx - px) / dist;
        final float ny = (cy - py) / dist;
        final float mx = (cx + px) / 2;
        final float my = (cy + py) / 2;

        fillSheet();
        final int flapN = clip(sheet, 4, mx, my, nx, ny, flap);
        final int keptN = clip(sheet, 4, mx, my, -nx, -ny, kept);

        // The page the turn shows, all of it: what is not covered is uncovered by the fold
        drawPage(canvas, under);

        // The turning page where it is not lifted, and, of two pages, the page that stays
        path.reset();
        addPolygon(path, kept, keptN);
        if (spine > 0) {
            path.addRect(0, 0, spine, h, Path.Direction.CW);
        }
        canvas.save();
        canvas.clipPath(path);
        canvas.drawColor(paper);
        drawPage(canvas, top);
        canvas.restore();

        if (flapN < 3) {
            canvas.restore();
            return;
        }

        flapPath.reset();
        addPolygon(flapPath, flap, flapN);

        // The lifted page's shadow on the page under it, along the fold
        final float shadowW = Math.max(1, Math.min(dist * 0.35f, w * 0.12f));
        canvas.save();
        canvas.clipPath(flapPath);
        shade.setShader(new LinearGradient(mx, my, mx + nx * shadowW, my + ny * shadowW,
                new int[]{0x70000000, 0x00000000}, null, Shader.TileMode.CLAMP));
        canvas.drawPaint(shade);
        canvas.restore();

        // The lifted part folded over: its mirror image across the fold
        final float dxl = -ny;
        final float dyl = nx;
        final float a = 2 * dxl * dxl - 1;
        final float b = 2 * dxl * dyl;
        values[0] = a;
        values[1] = b;
        values[2] = mx - (a * mx + b * my);
        values[3] = b;
        values[4] = -a;
        values[5] = my - (b * mx - a * my);
        values[6] = 0;
        values[7] = 0;
        values[8] = 1;
        reflect.setValues(values);
        reflect.mapPoints(back, 0, flap, 0, flapN);
        backPath.reset();
        addPolygon(backPath, back, flapN);

        // Its shadow on what it lies over
        canvas.save();
        clipOut(canvas, backPath);
        final float e = Dips.dpToPx(1);
        drawEdge(canvas, 16 * e, 0x0A000000);
        drawEdge(canvas, 9 * e, 0x10000000);
        drawEdge(canvas, 4 * e, 0x18000000);
        canvas.restore();

        // The back of the page
        canvas.save();
        canvas.clipPath(backPath);
        canvas.drawColor(paper);
        canvas.save();
        canvas.concat(reflect);
        if (twoPages) {
            // The back of the right page is the next spread's left page
            spineMirror.setValues(new float[]{-1, 0, 2 * spine, 0, 1, 0, 0, 0, 1});
            canvas.concat(spineMirror);
            drawPage(canvas, under);
        } else {
            drawPage(canvas, top);
        }
        canvas.restore();
        if (!twoPages) {
            // Paper: the print of the other side shows through, faint
            canvas.drawColor((paper & 0x00FFFFFF) | 0xD8000000);
        }
        // The paper bends at the fold: darker there, lighter on the flat of it, darker at its edge
        final float len = dist / 2;
        shade.setShader(new LinearGradient(mx, my, mx - nx * len, my - ny * len,
                new int[]{0x50000000, 0x14000000, 0x00000000, 0x1E000000},
                new float[]{0f, 0.12f, 0.55f, 1f}, Shader.TileMode.CLAMP));
        canvas.drawPaint(shade);
        canvas.restore();

        canvas.restore();
    }

    /**
     * The page bent round a roll, as paper is: flat up to the roll, round over it, and flat
     * again on its way back, the corner where the finger is. Drawn as a mesh of the page's
     * pictures: its front where it faces up, its back where it is turned over.
     */
    private void drawCurl(Canvas canvas) {
        final int w = getWidth();
        final int h = getHeight();
        final float sheetW = sheetR - spine;
        final float sheetH = sheetB - sheetT;
        canvas.save();
        if (mirror) {
            canvas.scale(-1, 1, w / 2f, 0);
        }
        canvas.drawColor(paper);

        final float cx = corner.x;
        final float cy = corner.y;
        final float dist = (float) Math.hypot(cx - point.x, cy - point.y);

        drawPage(canvas, under);
        if (spine > 0) {
            // The page that stays, on the other side of the spine
            canvas.save();
            canvas.clipRect(0, 0, spine, h);
            drawPage(canvas, top);
            canvas.restore();
        }
        if (dist < 0.5f) {
            canvas.save();
            canvas.clipRect(spine, sheetT, sheetR, sheetB);
            drawPage(canvas, top);
            canvas.restore();
            canvas.restore();
            return;
        }
        final float nx = (cx - point.x) / dist;
        final float ny = (cy - point.y) / dist;
        // The roll at the corner: small as the page starts to lift and as it comes down flat on
        // the other side
        final float pi = (float) Math.PI;
        final float r = Math.max(0.01f, Math.min(sheetW * 0.16f, Math.min(dist / pi, (2 * sheetW - dist) / pi)));
        // Where the page leaves the flat: the corner goes round the roll and back, onto the finger
        final float sc = (dist + pi * r) / 2;
        final float lx = cx - sc * nx;
        final float ly = cy - sc * ny;

        fillSheet();

        // The shadow of the roll on the page it uncovers: dark at the bend
        final int openN = clip(sheet, 4, lx, ly, nx, ny, flap);
        if (openN >= 3) {
            flapPath.reset();
            addPolygon(flapPath, flap, openN);
            final float shadowW = Math.max(1, r + Math.min(r * 2.5f, sheetW * 0.18f));
            canvas.save();
            canvas.clipPath(flapPath);
            shade.setShader(new LinearGradient(lx, ly, lx + nx * shadowW, ly + ny * shadowW,
                    new int[]{0xA0000000, 0x58000000, 0x18000000, 0x00000000},
                    new float[]{0f, r / shadowW * 0.8f, Math.min(0.95f, r / shadowW + 0.25f), 1f},
                    Shader.TileMode.CLAMP));
            canvas.drawPaint(shade);
            canvas.restore();
        }

        // The front, then the back over it: the back is always the higher of the two
        if (VERTICES) {
            buildVertices(lx, ly, nx, ny, r, sheetW, false);
            vertexPaint.setShader(frontShader);
            canvas.drawVertices(Canvas.VertexMode.TRIANGLES, (ROWS + 1) * FRONT_COLS * 2, verts, 0, texs, 0,
                    vertColors, 0, frontIndices, 0, frontIndices.length, vertexPaint);
            buildVertices(lx, ly, nx, ny, r, sheetW, true);
            vertexPaint.setShader(backShader);
            canvas.drawVertices(Canvas.VertexMode.TRIANGLES, (ROWS + 1) * BACK_COLS * 2, verts, 0, texs, 0,
                    vertColors, 0, backIndices, 0, backIndices.length, vertexPaint);
        } else {
            buildMesh(lx, ly, nx, ny, r, sheetW, sheetH, false);
            canvas.drawBitmapMesh(frontSheet, MESH_W, MESH_H, mesh, 0, meshColors, 0, meshPaint);
            buildMesh(lx, ly, nx, ny, r, sheetW, sheetH, true);
            canvas.drawBitmapMesh(backSheet, MESH_W, MESH_H, mesh, 0, meshColors, 0, meshPaint);
        }

        canvas.restore();
    }


    /**
     * How much across a drag from this point has to go, of how far it goes down, to turn the
     * page: as much anywhere, and half as much from a corner of the page, which is pulled on a
     * slant. The pager and the page ask the same, so one of the two takes each drag.
     *
     * @param page the page on the screen; its margins round it count as its corners too
     */
    public static float steep(RectF page, float x, float y) {
        if (page == null || page.width() <= 0 || page.height() <= 0) {
            return 1f;
        }
        final float cw = page.width() * 0.35f;
        final float ch = page.height() * 0.3f;
        final boolean side = x < page.left + cw || x > page.right - cw;
        final boolean end = y < page.top + ch || y > page.bottom - ch;
        return side && end ? 0.5f : 1f;
    }

    /** {@link #steep(RectF, float, float)} for the page shown now. */
    public float steepAt(float x, float y) {
        final View shown = host == null ? null : host.pageView(0);
        RectF page = shown == null ? null : host.pageRect(shown);
        if (page == null) {
            page = new RectF(0, 0, getWidth(), getHeight());
        }
        return steep(page, x, y);
    }

    /** The triangles of a grid of points, {@code cols} to a row. */
    private static short[] indices(int cols) {
        final short[] res = new short[ROWS * (cols - 1) * 6];
        int k = 0;
        for (int j = 0; j < ROWS; j++) {
            for (int i = 0; i < cols - 1; i++) {
                final int a = j * cols + i;
                final int b = a + cols;
                res[k++] = (short) a;
                res[k++] = (short) (a + 1);
                res[k++] = (short) b;
                res[k++] = (short) (a + 1);
                res[k++] = (short) (b + 1);
                res[k++] = (short) b;
            }
        }
        return res;
    }

    /**
     * The same bend as {@link #buildMesh}, with the points in rows across the fold and in lines
     * round the roll: each line is one angle of the roll, so one light, all along the fold. The points take the page's picture from where they were on the flat page;
     * those off the page get its clear border.
     */
    private void buildVertices(float lx, float ly, float nx, float ny, float r, float sheetW, boolean back) {
        final float pi = (float) Math.PI;
        final float ax = -ny;
        final float ay = nx;
        float tMin = Float.MAX_VALUE;
        float tMax = -Float.MAX_VALUE;
        float dMin = 0;
        float dMax = 0;
        for (int i = 0; i < 4; i++) {
            final float px = sheet[2 * i] - lx;
            final float py = sheet[2 * i + 1] - ly;
            final float t = px * ax + py * ay;
            final float d = px * nx + py * ny;
            tMin = Math.min(tMin, t);
            tMax = Math.max(tMax, t);
            dMin = Math.min(dMin, d);
            dMax = Math.max(dMax, d);
        }
        final int cols = back ? BACK_COLS : FRONT_COLS;
        final float greyEnd = sheetW * 0.6f;
        int k = 0;
        int c = 0;
        for (int j = 0; j <= ROWS; j++) {
            final float t = tMin + (tMax - tMin) * j / ROWS;
            final float rr = r;
            final float ox = lx + t * ax;
            final float oy = ly + t * ay;
            for (int i = 0; i < cols; i++) {
                final float d;
                float moved;
                final float light;
                int alpha = 255;
                if (!back) {
                    if (i == 0) {
                        // The page lying flat, up to the fold
                        d = dMin - 1;
                        moved = d;
                        light = 1f;
                    } else {
                        // Turning away from the eye up the roll, darkest at its top
                        final float angle = pi / 2 * (i - 1) / ROLL;
                        d = angle * rr;
                        moved = rr * (float) Math.sin(angle);
                        light = 1f - 0.4f * (float) Math.sin(angle);
                    }
                } else if (i <= ROLL + 1) {
                    // The back coming round towards the eye again, lit at the turn
                    final float angle = pi / 2 + pi / 2 * Math.max(0, i - 1) / ROLL;
                    d = angle * rr;
                    moved = rr * (float) Math.sin(angle);
                    light = Math.min(1f, 0.62f - 0.5f * (float) Math.cos(angle));
                    if (i == 0) {
                        // The roll's outline fades out over a pixel instead of ending in steps
                        moved += 1;
                        alpha = 0;
                    }
                } else {
                    // The back as it lies over: a little greyer away from the roll
                    final float over = i == ROLL + 2 ? greyEnd : Math.max(dMax, greyEnd) + 1;
                    d = pi * rr + over;
                    moved = -over;
                    light = 1f - 0.14f * Math.min(1f, over / greyEnd);
                }
                verts[k] = ox + moved * nx;
                verts[k + 1] = oy + moved * ny;
                texs[k] = ox + d * nx - spine + pad;
                texs[k + 1] = oy + d * ny - sheetT + pad;
                k += 2;
                final int v = Math.round(255 * Math.max(0f, Math.min(1f, light)));
                vertColors[c++] = (alpha << 24) | (v << 16) | (v << 8) | v;
            }
        }
    }

    /**
     * Places the mesh points of the page bent round the roll, and their light. The front is
     * drawn up to the top of the roll and the back from there on: the points outside the part
     * drawn are put on the top of the roll, and their triangles have no size.
     *
     * @param r the roll's radius
     */
    private void buildMesh(float lx, float ly, float nx, float ny, float r, float sheetW, float h, boolean back) {
        final float pi = (float) Math.PI;
        int k = 0;
        int c = 0;
        for (int j = 0; j <= MESH_H; j++) {
            final float by = h * j / (float) MESH_H;
            final float y = sheetT + (flipped ? h - by : by);
            for (int i = 0; i <= MESH_W; i++) {
                final float x = spine + sheetW * i / (float) MESH_W;
                final float d = (x - lx) * nx + (y - ly) * ny;
                final float rr = r;
                final float half = pi * rr / 2;
                final float s = back ? Math.max(half, d) : Math.min(half, d);
                final float moved;
                float light;
                if (s <= 0) {
                    moved = s;
                    light = 1f;
                } else if (s < pi * rr) {
                    final float angle = s / rr;
                    moved = rr * (float) Math.sin(angle);
                    // Turning away from the eye up the roll, darkest at its top; the back
                    // coming round towards it again, lit at the turn
                    light = angle <= pi / 2 ? 1f - 0.4f * (float) Math.sin(angle)
                                            : Math.min(1f, 0.62f - 0.5f * (float) Math.cos(angle));
                } else {
                    moved = -(s - pi * rr);
                    // The back as it lies over: a little greyer away from the roll
                    light = 1f - 0.14f * Math.min(1f, (s - pi * rr) / (sheetW * 0.6f));
                }
                // Where the point is on the page, and where it is moved to along the bend
                final float px = x + (moved - d) * nx;
                final float py = y + (moved - d) * ny;
                mesh[k++] = px;
                mesh[k++] = py;
                final int v = Math.round(255 * Math.max(0f, Math.min(1f, light)));
                meshColors[c++] = 0xFF000000 | (v << 16) | (v << 8) | v;
            }
        }
    }

    private void fillSheet() {
        sheet[0] = spine;
        sheet[1] = sheetT;
        sheet[2] = sheetR;
        sheet[3] = sheetT;
        sheet[4] = sheetR;
        sheet[5] = sheetB;
        sheet[6] = spine;
        sheet[7] = sheetB;
    }

    private void drawEdge(Canvas canvas, float width, int color) {
        edge.setStrokeWidth(width);
        edge.setColor(color);
        canvas.drawPath(backPath, edge);
    }

    @SuppressWarnings("deprecation")
    private static void clipOut(Canvas canvas, Path p) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            canvas.clipOutPath(p);
        } else {
            canvas.clipPath(p, Region.Op.DIFFERENCE);
        }
    }

    /** Draws a page view as the pager shows it; in a mirrored book, back to its own way. */
    private void drawPage(Canvas canvas, View page) {
        if (page == null) {
            return;
        }
        canvas.save();
        if (mirror) {
            canvas.scale(-1, 1, getWidth() / 2f, 0);
        }
        page.draw(canvas);
        canvas.restore();
    }

    private static void addPolygon(Path p, float[] pts, int n) {
        if (n < 3) {
            return;
        }
        p.moveTo(pts[0], pts[1]);
        for (int i = 1; i < n; i++) {
            p.lineTo(pts[2 * i], pts[2 * i + 1]);
        }
        p.close();
    }

    /**
     * The part of the polygon on the side of the line through (mx, my) that (nx, ny) points to.
     * Returns its number of points.
     */
    private static int clip(float[] src, int n, float mx, float my, float nx, float ny, float[] dst) {
        int k = 0;
        for (int i = 0; i < n; i++) {
            final int j = (i + 1) % n;
            final float ax = src[2 * i];
            final float ay = src[2 * i + 1];
            final float bx = src[2 * j];
            final float by = src[2 * j + 1];
            final float da = (ax - mx) * nx + (ay - my) * ny;
            final float db = (bx - mx) * nx + (by - my) * ny;
            if (da >= 0) {
                dst[k++] = ax;
                dst[k++] = ay;
            }
            if ((da >= 0) != (db >= 0)) {
                final float t = da / (da - db);
                dst[k++] = ax + t * (bx - ax);
                dst[k++] = ay + t * (by - ay);
            }
        }
        return k / 2;
    }

    @Override
    protected void onDetachedFromWindow() {
        if (animator != null) {
            animator.cancel();
            animator = null;
        }
        dragging = false;
        top = null;
        under = null;
        recycleSheets();
        super.onDetachedFromWindow();
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        if (isBusy()) {
            finishNow();
        }
    }
}
