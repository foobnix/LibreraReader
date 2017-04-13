package com.foobnix.pdf.search.view;

import java.util.Arrays;
import java.util.List;

import com.foobnix.android.utils.Dips;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.view.View;

public class SpeedBgView extends View {

    private static int LEN = 10;
    Paint paint = new Paint();
    {
        paint.setColor(Color.parseColor("#90EE90"));
        paint.setStyle(Style.FILL);
        paint.setStrokeWidth(2);
        paint.setTextSize(26);
        paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.NORMAL));
    }

    public SpeedBgView(final Context context, final AttributeSet attrs) {
        super(context, attrs);
        setBackgroundColor(Color.TRANSPARENT);
        paint.setStrokeWidth(Dips.dpToPx(1));
        LEN = Dips.dpToPx(10);
        paint.setColor(Color.LTGRAY);
    }

    public SpeedBgView(final Context context) {
        super(context);
        setBackgroundColor(Color.TRANSPARENT);
        paint.setStrokeWidth(Dips.dpToPx(1));
        LEN = Dips.dpToPx(10);
        paint.setColor(Color.LTGRAY);
    }

    @Override
    protected void onDraw(final Canvas canvas) {
        super.onDraw(canvas);
        canvas.save();

        final int width = this.getMeasuredWidth();
        final int height = this.getMeasuredHeight();

        final float k = (float) width / 150;

        final List<Integer> asList = Arrays.asList(1, 25, 50, 75, 100, 125, 149);
        for (final int num : asList) {
            canvas.drawLine(k * num, 0, k * num, height, paint);
            canvas.drawText("" + num, k * num + 10, 20, paint);
        }
        // canvas.drawText("" + 250, k * 250 - 40, 20, paint);

        final List<Integer> small = Arrays.asList(25, 75, 125);
        for (final int num : small) {
            // canvas.drawLine(k * num, LEN, k * num, height - LEN, paint);
        }

        canvas.restore();
    }

    public void setBorderColor(final int color) {
        paint.setColor(color);
        invalidate();
    }

}
