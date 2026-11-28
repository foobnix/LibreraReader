package com.foobnix.pdf.info.view;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.widget.ImageView;

import androidx.core.content.ContextCompat;

import com.foobnix.android.utils.Dips;

/**
 * An icon button that shows a second icon beside its own: the page button's layout, then how
 * the page is turned. The first is the image as set in any of the usual ways, so whoever changes
 * the layout icon need not know of the second; both are drawn in the image's tint.
 */
public class TwoIconImageView extends ImageView {

    private final int gap = Dips.dpToPx(4);
    private final Rect saved = new Rect();
    private Drawable second;

    public TwoIconImageView(Context context) {
        super(context);
    }

    public TwoIconImageView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public void setSecondIcon(int res) {
        second = res == 0 ? null : ContextCompat.getDrawable(getContext(), res).mutate();
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        final Drawable first = getDrawable();
        if (second == null || first == null) {
            super.onDraw(canvas);
            return;
        }
        final int w = getWidth() - getPaddingLeft() - getPaddingRight();
        final int h = getHeight() - getPaddingTop() - getPaddingBottom();
        final int side = Math.max(0, Math.min(h, (w - gap) / 2));
        final int left = getPaddingLeft() + (w - side * 2 - gap) / 2;
        final int top = getPaddingTop() + (h - side) / 2;

        final ColorStateList tint = getImageTintList();
        if (tint != null) {
            second.setTintList(tint);
        } else if (getColorFilter() != null) {
            second.setColorFilter(getColorFilter());
        }
        second.setState(getDrawableState());

        saved.set(first.getBounds());
        first.setBounds(left, top, left + side, top + side);
        first.draw(canvas);
        first.setBounds(saved);

        final int left2 = left + side + gap;
        second.setBounds(left2, top, left2 + side, top + side);
        second.draw(canvas);
    }
}
