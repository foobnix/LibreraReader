package com.foobnix.pdf.info.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.widget.ImageView;

import com.foobnix.android.utils.Dips;

public class UnderlineImageView extends ImageView {

    Paint p = new Paint();
    int dp1 = Dips.dpToPx(2);
    boolean leftPadding = true;
    private boolean isUnderline = false;

    public UnderlineImageView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public void setUnderlineValue(int value) {
        dp1 = value;
    }

    public void setLeftPadding(boolean value) {
        leftPadding = value;
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
            int padding = leftPadding ? dp1 * 2 : 0;
            canvas.drawLine(padding, getHeight() - dp1, getWidth() - padding, getHeight() - dp1, p);
        }
        canvas.restore();
    }

}
