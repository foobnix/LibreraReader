package com.foobnix.pdf.info.view;

import android.content.Context;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import com.foobnix.android.utils.Dips;
import com.foobnix.pdf.info.R;

/**
 * The drawing panel's two groups - the tools, then the colours with the eraser after them -
 * stood side by side wherever the width holds both, as in landscape, and one over the other
 * where it does not; centred either way.
 */
public class DrawPanelLayout extends LinearLayout {

    public DrawPanelLayout(Context context) {
        super(context);
    }

    public DrawPanelLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    /** How wide the panel is with its groups side by side. */
    public int oneLineWidth() {
        sizeSwatches(SWATCH_DP);
        placeDelete(true);
        int width = getPaddingLeft() + getPaddingRight();
        int shown = 0;
        for (int i = 0; i < getChildCount(); i++) {
            final View child = getChildAt(i);
            if (child.getVisibility() == GONE) {
                continue;
            }
            // Margins left out: between the groups it is the gap that counts
            child.measure(MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED),
                          MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED));
            width += child.getMeasuredWidth();
            shown++;
        }
        return width + Math.max(0, shown - 1) * gap();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        final int available = MeasureSpec.getSize(widthMeasureSpec);
        final boolean measured = MeasureSpec.getMode(widthMeasureSpec) != MeasureSpec.UNSPECIFIED;
        // The eraser always stands after the colours
        sizeSwatches(SWATCH_DP);
        placeDelete(true);
        final boolean oneLine = measured && oneLineWidth() <= available;
        if (!oneLine && measured) {
            // One over the other, the colours give up some of their size where their row would
            // not hold the eraser after them, rather than the eraser going out of sight
            final ViewGroup colors = findViewById(R.id.drawColors);
            final int over = colors == null ? 0
                    : getPaddingLeft() + naturalWidth(colors) + getPaddingRight() - available;
            if (over > 0) {
                final int swatches = swatchCount();
                final int each = Dips.dpToPx(SWATCH_DP) - (over + swatches - 1) / Math.max(1, swatches);
                sizeSwatchesPx(Math.max(Dips.dpToPx(20), each));
            }
        }
        setOneLine(oneLine);
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }

    /** The side of a colour swatch, in dp, where the row has room for it. */
    private static final int SWATCH_DP = 32;

    private int swatchCount() {
        final ViewGroup colors = findViewById(R.id.drawColors);
        final View delete = findViewById(R.id.drawDelete);
        int count = 0;
        for (int i = 0; colors != null && i < colors.getChildCount(); i++) {
            if (colors.getChildAt(i) != delete) {
                count++;
            }
        }
        return count;
    }

    private void sizeSwatches(int dp) {
        sizeSwatchesPx(Dips.dpToPx(dp));
    }

    /** Sizes the colour swatches, in place, as the eraser is left as it is. */
    private void sizeSwatchesPx(int px) {
        final ViewGroup colors = findViewById(R.id.drawColors);
        final View delete = findViewById(R.id.drawDelete);
        for (int i = 0; colors != null && i < colors.getChildCount(); i++) {
            final View swatch = colors.getChildAt(i);
            if (swatch == delete) {
                continue;
            }
            final ViewGroup.LayoutParams lp = swatch.getLayoutParams();
            lp.width = px;
            lp.height = px;
        }
    }

    /** Puts the eraser at the end of the colours, or at the end of the tools. */
    private void placeDelete(boolean afterColors) {
        final View delete = findViewById(R.id.drawDelete);
        final ViewGroup colors = findViewById(R.id.drawColors);
        final ViewGroup tools = (ViewGroup) getChildAt(0);
        if (delete == null || colors == null || tools == null) {
            return;
        }
        final ViewGroup target = afterColors ? colors : tools;
        final LayoutParams lp = (LayoutParams) delete.getLayoutParams();
        // The swatches keep a gap after each already
        lp.leftMargin = afterColors ? 0 : Dips.DP_4;
        if (delete.getParent() != target) {
            ((ViewGroup) delete.getParent()).removeView(delete);
            target.addView(delete);
        }
    }

    private void setOneLine(boolean oneLine) {
        setOrientation(oneLine ? HORIZONTAL : VERTICAL);
        setGravity(oneLine ? Gravity.CENTER : Gravity.CENTER_HORIZONTAL);
        // The groups after the first stand beside it rather than under it. The fields are set
        // in place: setLayoutParams would ask for another layout from inside this one.
        for (int i = 1; i < getChildCount(); i++) {
            final LayoutParams lp = (LayoutParams) getChildAt(i).getLayoutParams();
            lp.width = ViewGroup.LayoutParams.WRAP_CONTENT;
            lp.leftMargin = oneLine ? gap() : 0;
            lp.topMargin = oneLine ? 0 : gap();
        }
    }

    private static int naturalWidth(View child) {
        final int unspecified = MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED);
        child.measure(unspecified, unspecified);
        final ViewGroup.LayoutParams lp = child.getLayoutParams();
        if (lp instanceof MarginLayoutParams) {
            return child.getMeasuredWidth() + ((MarginLayoutParams) lp).leftMargin + ((MarginLayoutParams) lp).rightMargin;
        }
        return child.getMeasuredWidth();
    }

    private static int gap() {
        return Dips.dpToPx(6);
    }
}
