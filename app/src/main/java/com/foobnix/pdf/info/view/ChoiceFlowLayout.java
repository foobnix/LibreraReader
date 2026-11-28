package com.foobnix.pdf.info.view;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;

import com.foobnix.android.utils.Dips;

/**
 * Lays its children out in a line, going on to the next line where the next one does not fit.
 * Asked how wide it would like to be, it keeps to {@link #MAX_WIDTH_DP}: a menu that measures its
 * rows that way stays narrow, and its long rows of choices wrap.
 */
public class ChoiceFlowLayout extends ViewGroup {

    static final int MAX_WIDTH_DP = 300;

    private final int gap = Dips.dpToPx(6);

    public ChoiceFlowLayout(Context context) {
        super(context);
    }

    public ChoiceFlowLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        final int mode = MeasureSpec.getMode(widthMeasureSpec);
        final int horizontalPadding = getPaddingLeft() + getPaddingRight();
        final int limit = (mode == MeasureSpec.UNSPECIFIED ? Dips.dpToPx(MAX_WIDTH_DP)
                : MeasureSpec.getSize(widthMeasureSpec)) - horizontalPadding;

        final int unspecified = MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED);
        int x = 0, y = 0, lineHeight = 0, widest = 0;
        for (int i = 0; i < getChildCount(); i++) {
            final View child = getChildAt(i);
            if (child.getVisibility() == GONE) {
                continue;
            }
            final LayoutParams lp = child.getLayoutParams();
            child.measure(lp != null && lp.width > 0 ? MeasureSpec.makeMeasureSpec(lp.width, MeasureSpec.EXACTLY) : unspecified,
                    lp != null && lp.height > 0 ? MeasureSpec.makeMeasureSpec(lp.height, MeasureSpec.EXACTLY) : unspecified);
            final int w = child.getMeasuredWidth();
            if (x > 0 && x + w > limit) {
                x = 0;
                y += lineHeight + gap;
                lineHeight = 0;
            }
            x += w;
            widest = Math.max(widest, x);
            x += gap;
            lineHeight = Math.max(lineHeight, child.getMeasuredHeight());
        }
        final int width = widest + horizontalPadding;
        final int height = y + lineHeight + getPaddingTop() + getPaddingBottom();
        setMeasuredDimension(resolveSize(width, widthMeasureSpec), resolveSize(height, heightMeasureSpec));
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        final int limit = r - l - getPaddingRight();
        int x = getPaddingLeft(), y = getPaddingTop(), lineHeight = 0;
        for (int i = 0; i < getChildCount(); i++) {
            final View child = getChildAt(i);
            if (child.getVisibility() == GONE) {
                continue;
            }
            final int w = child.getMeasuredWidth();
            final int h = child.getMeasuredHeight();
            if (x > getPaddingLeft() && x + w > limit) {
                x = getPaddingLeft();
                y += lineHeight + gap;
                lineHeight = 0;
            }
            child.layout(x, y, x + w, y + h);
            x += w + gap;
            lineHeight = Math.max(lineHeight, h);
        }
    }
}
