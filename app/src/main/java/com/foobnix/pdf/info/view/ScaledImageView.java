package com.foobnix.pdf.info.view;

import com.foobnix.android.utils.Dips;
import com.foobnix.android.utils.LOG;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.widget.ImageView;

public class ScaledImageView extends ImageView {
    public ScaledImageView(final Context context, final AttributeSet attrs) {
        super(context, attrs);
    }

    public ScaledImageView(final Context context) {
        super(context);
    }

    @Override
    protected void onMeasure(final int widthMeasureSpec, final int heightMeasureSpec) {
        final Drawable d = getDrawable();

        if (d != null) {
            int width;
            int height;
            if (MeasureSpec.getMode(heightMeasureSpec) == MeasureSpec.EXACTLY) {
                height = MeasureSpec.getSize(heightMeasureSpec);
                width = (int) Math.ceil(height * (float) d.getIntrinsicWidth() / d.getIntrinsicHeight());
            } else {
                width = Math.min(Dips.screenWidth(), MeasureSpec.getSize(widthMeasureSpec));
                height = (int) Math.ceil(width * (float) d.getIntrinsicHeight() / d.getIntrinsicWidth());


            }
            width = Math.min((int) (Dips.screenWidth() * 0.9), width);
            height = Math.min((int) (Dips.screenHeight() * 0.9), height);
            LOG.d("ScaledImageView", width, height);
            setMeasuredDimension(width, height);
        } else {
            super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        }
    }
}