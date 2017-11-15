package com.foobnix.pdf.info.view;

import com.foobnix.android.utils.Dips;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.widget.ImageView;

public class UnderlineImageView extends ImageView {

    Paint p = new Paint();
    int dp1 = Dips.dpToPx(2);

    private boolean isUnderline = false;

    public UnderlineImageView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public void underline(boolean value) {
        isUnderline = value;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        canvas.save();
        if (isUnderline) {
            p.setColor(Color.WHITE);
            p.setStrokeWidth(dp1);
            canvas.drawLine(dp1 * 2, getHeight() - dp1, getWidth() - dp1 * 2, getHeight() - dp1, p);
        }
        canvas.restore();
    }

}
