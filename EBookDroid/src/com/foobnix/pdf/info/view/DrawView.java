package com.foobnix.pdf.info.view;

import java.util.ArrayList;
import java.util.List;

import com.foobnix.android.utils.Dips;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.CornerPathEffect;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.Path;
import android.graphics.PointF;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;


public class DrawView extends View {
	private final List<PointF> points = new ArrayList<PointF>();
	Paint paint = new Paint();
	{
        paint.setColor(Color.LTGRAY);
		paint.setStrokeWidth(Dips.dpToPx(4));
		paint.setPathEffect(new CornerPathEffect(10));
		paint.setStrokeJoin(Paint.Join.ROUND);
		paint.setStrokeCap(Paint.Cap.ROUND);
		paint.setAntiAlias(true);
		paint.setDither(true);
		paint.setStyle(Style.STROKE);
	}
	private Runnable onFinishDraw;

	public DrawView(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

    public void setColor(int color, float width) {
		paint.setColor(color);
        paint.setStrokeWidth(Dips.dpToPx((int) width * 2));
	}

	@Override
	protected void onDraw(Canvas canvas) {
		super.onDraw(canvas);
		canvas.drawPath(pointsPath, paint);
	}

	public void clear() {
		points.clear();
		pointsPath.reset();
		invalidate();

	}

	Path pointsPath = new Path();

	@Override
	public boolean onTouchEvent(MotionEvent event) {
		if (event.getAction() == MotionEvent.ACTION_MOVE) {
			pointsPath.lineTo(event.getX(), event.getY());
			points.add(new PointF(event.getX(), event.getY()));
			invalidate();
		}
		if (event.getAction() == MotionEvent.ACTION_DOWN) {
			points.clear();
			pointsPath.reset();
			pointsPath.moveTo(event.getX(), event.getY());
			points.add(new PointF(event.getX(), event.getY()));
			invalidate();

		}
		if (event.getAction() == MotionEvent.ACTION_UP) {
			if (onFinishDraw != null) {
				onFinishDraw.run();
			}

		}

		return true;
	}

	public List<PointF> getPoints() {
		return points;
	}

	public void setOnFinishDraw(Runnable onFinishDraw) {
		this.onFinishDraw = onFinishDraw;
	}

}
