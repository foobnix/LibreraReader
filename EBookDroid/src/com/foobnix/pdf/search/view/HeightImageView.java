package com.foobnix.pdf.search.view;

import com.foobnix.android.utils.LOG;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.widget.ImageView;

public class HeightImageView extends ImageView {

    public HeightImageView(final Context context, final AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected void onMeasure(final int widthMeasureSpec, final int heightMeasureSpec) {
        try {
            final Drawable d = this.getDrawable();
            if (d != null) {
                int h = MeasureSpec.getSize(heightMeasureSpec);
                int w = 0;
                if (d.getIntrinsicWidth() > d.getIntrinsicHeight()) {
                    w = (int) Math.ceil(h * d.getIntrinsicWidth() / d.getIntrinsicHeight());
                } else {
                    w = (int) Math.ceil(h * d.getIntrinsicHeight() / d.getIntrinsicWidth());
                }
                this.setMeasuredDimension(w, h);
            } else {
                super.onMeasure(widthMeasureSpec, heightMeasureSpec);
            }
        } catch (Exception e) {
            LOG.e(e);
            super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        }
    }
}