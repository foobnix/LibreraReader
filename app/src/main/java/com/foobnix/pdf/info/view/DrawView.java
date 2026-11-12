package com.foobnix.pdf.info.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.CornerPathEffect;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.Path;
import android.graphics.DashPathEffect;
import android.graphics.PointF;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;

import com.foobnix.android.utils.Dips;

import java.util.ArrayList;
import java.util.List;


public class DrawView extends View {
	/** What a stroke draws: the line the finger takes, or a shape spanned from where it went down to where it is. */
	public static final int SHAPE_FREE = 0;
	public static final int SHAPE_LINE = 1;
	public static final int SHAPE_ARROW = 2;
	public static final int SHAPE_RECT = 3;
	public static final int SHAPE_STAR = 4;

	/** A tap rather than a stroke, at a point on the view. */
	public interface OnTap {
		void onTap(float x, float y);
	}

	/** A drawing that can be taken and moved, rather than drawn over. */
	public interface OnDrag {
		/** Where the drawing that can be moved stands on screen, or null. */
		RectF draggable();

		/** It was let go of this far from where it stood. */
		void onDrop(float dx, float dy);
	}

	private final List<PointF> points = new ArrayList<PointF>();
	private final CornerPathEffect freeCorners = new CornerPathEffect(10);
	Paint paint = new Paint();
	{
        paint.setColor(Color.LTGRAY);
		paint.setStrokeWidth(Dips.dpToPx(4));
		paint.setPathEffect(freeCorners);
		paint.setStrokeJoin(Paint.Join.ROUND);
		paint.setStrokeCap(Paint.Cap.ROUND);
		paint.setAntiAlias(true);
		paint.setDither(true);
		paint.setStyle(Style.STROKE);
	}
	private Runnable onFinishDraw;
	private OnTap onTap;
	private OnDrag onDrag;
	// The drawing being moved, while the finger takes it, and how far it has gone
	private RectF dragged;
	private float dragX, dragY;
	private final Paint dragPaint = new Paint();
	{
		dragPaint.setStyle(Style.STROKE);
		dragPaint.setStrokeWidth(Dips.dpToPx(2));
		dragPaint.setPathEffect(new DashPathEffect(new float[] {Dips.dpToPx(6), Dips.dpToPx(4)}, 0));
		dragPaint.setColor(Color.GRAY);
		dragPaint.setAntiAlias(true);
	}
	private int shape = SHAPE_FREE;
	private float downX, downY;
	private boolean moved;
	private final int touchSlop;

	public DrawView(Context context, AttributeSet attrs) {
		super(context, attrs);
		touchSlop = ViewConfiguration.get(context).getScaledTouchSlop();
	}

    public void setColor(int color, float width) {
		paint.setColor(color);
        paint.setStrokeWidth(Dips.dpToPx((int) width * 2));
	}

	/** One of the SHAPE_*, or less than 0 for none: the pen is lifted, and only taps and moves are taken. */
	public void setShape(int shape) {
		this.shape = shape;
		// A shape keeps the corners it is drawn with; the free line is smoothed as it was
		paint.setPathEffect(shape == SHAPE_FREE ? freeCorners : null);
	}

	@Override
	protected void onDraw(Canvas canvas) {
		super.onDraw(canvas);
		canvas.drawPath(pointsPath, paint);
		if (dragged != null) {
			final RectF at = new RectF(dragged);
			at.offset(dragX, dragY);
			canvas.drawRect(at, dragPaint);
		}
	}

	public void clear() {
		points.clear();
		pointsPath.reset();
		invalidate();

	}

	Path pointsPath = new Path();

	@Override
	public boolean onTouchEvent(MotionEvent event) {
		final float x = event.getX();
		final float y = event.getY();
		if (event.getAction() == MotionEvent.ACTION_DOWN) {
			downX = x;
			downY = y;
			moved = false;
			// A finger down on the drawing picked out takes it, to move it
			final RectF draggable = onDrag == null ? null : onDrag.draggable();
			if (draggable != null) {
				final RectF reach = new RectF(draggable);
				reach.inset(-touchSlop, -touchSlop);
				dragged = reach.contains(x, y) ? draggable : null;
			} else {
				dragged = null;
			}
			dragX = 0;
			dragY = 0;
			points.clear();
			pointsPath.reset();
			pointsPath.moveTo(x, y);
			points.add(new PointF(x, y));
			invalidate();
		}
		if (event.getAction() == MotionEvent.ACTION_MOVE) {
			if (!moved && Math.hypot(x - downX, y - downY) < touchSlop) {
				return true;
			}
			moved = true;
			if (dragged != null) {
				dragX = x - downX;
				dragY = y - downY;
			} else if (shape < 0) {
				return true;
			} else if (shape == SHAPE_FREE) {
				pointsPath.lineTo(x, y);
				points.add(new PointF(x, y));
			} else {
				shapeTo(x, y);
			}
			invalidate();
		}
		if (event.getAction() == MotionEvent.ACTION_UP) {
			if (dragged != null && moved) {
				dragged = null;
				clear();
				onDrag.onDrop(dragX, dragY);
				return true;
			}
			dragged = null;
			if (!moved) {
				// A tap picks out what is under it instead of leaving a dot
				clear();
				if (onTap != null) {
					onTap.onTap(x, y);
				}
				return true;
			}
			if (shape < 0) {
				clear();
			} else if (onFinishDraw != null) {
				onFinishDraw.run();
			}

		}

		return true;
	}

	/**
	 * The shape from where the finger went down to where it is now, as the points of one line
	 * through its corners - the way the page keeps a stroke - and the path drawn from them.
	 */
	private void shapeTo(float x, float y) {
		points.clear();
		if (shape == SHAPE_LINE) {
			add(downX, downY);
			add(x, y);
		} else if (shape == SHAPE_ARROW) {
			final double angle = Math.atan2(y - downY, x - downX);
			final double length = Math.hypot(x - downX, y - downY);
			final double head = Math.min(Math.max(Dips.dpToPx(14), paint.getStrokeWidth() * 3), length / 2);
			final double spread = Math.toRadians(28);
			// The shaft to the tip, then out to one barb, back to the tip and out to the other
			add(downX, downY);
			add(x, y);
			add((float) (x - head * Math.cos(angle - spread)), (float) (y - head * Math.sin(angle - spread)));
			add(x, y);
			add((float) (x - head * Math.cos(angle + spread)), (float) (y - head * Math.sin(angle + spread)));
		} else if (shape == SHAPE_RECT) {
			add(downX, downY);
			add(x, downY);
			add(x, y);
			add(downX, y);
			add(downX, downY);
		} else if (shape == SHAPE_STAR) {
			// A regular star, as large as fits the box the finger spans
			final float cx = (downX + x) / 2;
			final float cy = (downY + y) / 2;
			final float outer = Math.min(Math.abs(x - downX), Math.abs(y - downY)) / 2;
			final float inner = outer * 0.4f;
			for (int i = 0; i <= 10; i++) {
				final double a = Math.toRadians(-90 + 36 * (i % 10));
				final float r = i % 2 == 0 ? outer : inner;
				add((float) (cx + r * Math.cos(a)), (float) (cy + r * Math.sin(a)));
			}
		}
		pointsPath.reset();
		for (int i = 0; i < points.size(); i++) {
			final PointF p = points.get(i);
			if (i == 0) {
				pointsPath.moveTo(p.x, p.y);
			} else {
				pointsPath.lineTo(p.x, p.y);
			}
		}
	}

	/** A point of its own each time: the page moves the points it is handed in place. */
	private void add(float x, float y) {
		points.add(new PointF(x, y));
	}

	public List<PointF> getPoints() {
		return points;
	}

	public void setOnFinishDraw(Runnable onFinishDraw) {
		this.onFinishDraw = onFinishDraw;
	}

	public void setOnTap(OnTap onTap) {
		this.onTap = onTap;
	}

	public void setOnDrag(OnDrag onDrag) {
		this.onDrag = onDrag;
	}

}
