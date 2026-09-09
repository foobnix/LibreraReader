package com.foobnix.pdf.info.view;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.widget.ImageView;

import com.foobnix.android.utils.Dips;
import com.foobnix.android.utils.LOG;

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

        if (d == null || d.getIntrinsicWidth() <= 0 || d.getIntrinsicHeight() <= 0) {
            super.onMeasure(widthMeasureSpec, heightMeasureSpec);
            return;
        }

        // The picture is given the room it is offered, and never more than nine tenths of the
        // screen either way. It takes the width first and the height that its own shape asks
        // for; if that comes out taller than the room, the height is what it is given and the
        // width follows from the shape again. Whichever side runs out first, the view ends up
        // the shape of the picture, so no ground is left showing above or below it.
        int maxWidth = (int) (Dips.screenWidth() * 0.9);
        int maxHeight = (int) (Dips.screenHeight() * 0.9);

        if (MeasureSpec.getMode(widthMeasureSpec) != MeasureSpec.UNSPECIFIED) {
            maxWidth = Math.min(maxWidth, MeasureSpec.getSize(widthMeasureSpec));
        }
        if (MeasureSpec.getMode(heightMeasureSpec) != MeasureSpec.UNSPECIFIED) {
            maxHeight = Math.min(maxHeight, MeasureSpec.getSize(heightMeasureSpec));
        }

        final float ratio = (float) d.getIntrinsicHeight() / d.getIntrinsicWidth();

        int width = maxWidth;
        int height = (int) Math.ceil(width * ratio);
        if (height > maxHeight) {
            height = maxHeight;
            width = (int) Math.ceil(height / ratio);
        }

        LOG.d("ScaledImageView", width, height);
        setMeasuredDimension(width, height);
    }
}
