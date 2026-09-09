package com.foobnix.pdf.info.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.widget.TextView;

import com.foobnix.android.utils.Dips;

public class BorderTextView extends TextView {

    /** The round the swatch is cut to, and that its border has to follow. */
    public static final int RADIUS_DP = 8;

    Paint paint = new Paint();
    {
        paint.setColor(Color.LTGRAY);
        paint.setStrokeWidth(Dips.dpToPx(1));
        paint.setAntiAlias(true);
        paint.setDither(true);
        paint.setStyle(Style.STROKE);
    }

    public BorderTextView(Context context) {
        super(context);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        // Drawn square, the border left its corners behind as specks outside the round the
        // swatch is clipped to. It is drawn to the same round instead, and held inside the
        // view by half its own width so the stroke does not straddle the edge.
        final float inset = paint.getStrokeWidth() / 2f;
        final float radius = Dips.dpToPx(RADIUS_DP);
        canvas.drawRoundRect(inset, inset, getWidth() - inset, getHeight() - inset, radius, radius, paint);
        super.onDraw(canvas);
    }

}
