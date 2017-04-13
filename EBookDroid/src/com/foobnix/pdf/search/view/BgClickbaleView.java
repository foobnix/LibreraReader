package com.foobnix.pdf.search.view;

import com.foobnix.android.utils.Dips;
import com.foobnix.pdf.info.R;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.graphics.Paint.Align;
import android.graphics.Paint.Style;
import android.graphics.Typeface;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.View;

public class BgClickbaleView extends View {

    private static int LEN = 10;

    Paint paint = new Paint();

    private String txt;
    {
        paint.setColor(Color.LTGRAY);
        paint.setStyle(Style.STROKE);
        paint.setPathEffect(new DashPathEffect(new float[] { 10, 20 }, 0));
        paint.setTextSize(26);
        paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        paint.setAntiAlias(true);
        paint.setTextAlign(Align.CENTER);
    }

    public BgClickbaleView(final Context context, final AttributeSet attrs) {
        super(context, attrs);
        setBackgroundColor(Color.TRANSPARENT);
        paint.setStrokeWidth(Dips.dpToPx(1));
        LEN = Dips.dpToPx(10);
        paint.setColor(Color.LTGRAY);

        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.CustomSeek);
        txt = a.getString(R.styleable.CustomSeek_text);

    }

    @Override
    protected void onDraw(final Canvas canvas) {
        super.onDraw(canvas);
        canvas.save();

        final int width = this.getMeasuredWidth();
        final int height = this.getMeasuredHeight();

        canvas.drawRect(0, 0, width - 1, height - 1, paint);

        if (!TextUtils.isEmpty(txt)) {
            canvas.drawText(txt, width / 2, height / 2, paint);
        }

        canvas.restore();
    }

    public void setBorderColor(final int color) {
        paint.setColor(color);
        invalidate();
    }

}
