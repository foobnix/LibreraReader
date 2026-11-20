package com.foobnix.pdf.info.view;

import android.app.Activity;
import android.content.res.Configuration;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;

import com.foobnix.pdf.info.R;

/**
 * Turned on its side the reading panels stand two of their rows side by side, each in its half of
 * the width, so the panels take half the height they did off the page: the reading controls
 * beside the buttons under them, the seek line beside the row under it, and on the page-at-a-time
 * screen the book's name beside its row of buttons. Upright the rows go back where they were.
 * <p>
 * The screens keep their views through a turn, so the rows are moved rather than inflated again;
 * whatever is set on them, and the listeners they carry, go with them.
 */
public final class LandscapeSplit {

    private LandscapeSplit() {
    }

    /** Where the rows stood before they were put side by side, to put them back. */
    private static final class Placed {
        LinearLayout row;
        ViewGroup parent;
        int firstIndex;
        int secondIndex;
        ViewGroup.LayoutParams firstLp;
        ViewGroup.LayoutParams secondLp;
        ViewGroup tailParent;
        int tailIndex;
        ViewGroup.LayoutParams tailLp;
    }

    public static boolean isLandscape(Activity a) {
        return a.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE;
    }

    /** The bottom panel's two pairs of rows, in both reading screens. */
    public static void footer(Activity a) {
        final boolean split = isLandscape(a);
        apply(a.findViewById(R.id.playListParent), a.findViewById(R.id.footerButtons), null, split);
        apply(a.findViewById(R.id.seekLine), a.findViewById(R.id.footerLastRow), null, split);
    }

    /**
     * Puts {@code first} and {@code second}, rows one above the other in the same column, side by
     * side in one row in their place, each in the middle of its half, with {@code tail} (from
     * anywhere, or null) at the row's end; or, with {@code split} false, back where they were.
     */
    public static void apply(View first, View second, View tail, boolean split) {
        if (first == null || second == null) {
            return;
        }
        final Object tag = first.getTag(R.id.landscapeSplit);
        final Placed placed = tag instanceof Placed ? (Placed) tag : null;
        if (split && placed == null) {
            join(first, second, tail);
        } else if (!split && placed != null) {
            part(first, second, tail, placed);
        }
    }

    private static void join(View first, View second, View tail) {
        if (!(first.getParent() instanceof LinearLayout) || second.getParent() != first.getParent()) {
            return;
        }
        final Placed placed = new Placed();
        placed.parent = (ViewGroup) first.getParent();
        placed.firstIndex = placed.parent.indexOfChild(first);
        placed.secondIndex = placed.parent.indexOfChild(second);
        placed.firstLp = first.getLayoutParams();
        placed.secondLp = second.getLayoutParams();
        if (tail != null && tail.getParent() instanceof ViewGroup) {
            placed.tailParent = (ViewGroup) tail.getParent();
            placed.tailIndex = placed.tailParent.indexOfChild(tail);
            placed.tailLp = tail.getLayoutParams();
        }

        placed.parent.removeView(second);
        placed.parent.removeView(first);

        final LinearLayout row = new LinearLayout(first.getContext());
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.addView(cell(first, placed.firstLp));
        row.addView(cell(second, placed.secondLp));
        if (placed.tailParent != null) {
            placed.tailParent.removeView(tail);
            final LinearLayout.LayoutParams lp =
                    new LinearLayout.LayoutParams(placed.tailLp.width, placed.tailLp.height);
            if (placed.tailLp instanceof ViewGroup.MarginLayoutParams) {
                lp.leftMargin = ((ViewGroup.MarginLayoutParams) placed.tailLp).leftMargin;
                lp.rightMargin = ((ViewGroup.MarginLayoutParams) placed.tailLp).rightMargin;
            }
            lp.gravity = Gravity.CENTER_VERTICAL;
            row.addView(tail, lp);
        }
        placed.row = row;
        placed.parent.addView(row, placed.firstIndex,
                              new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                                                            ViewGroup.LayoutParams.WRAP_CONTENT));
        first.setTag(R.id.landscapeSplit, placed);
    }

    /**
     * Half the row, the view in its middle as wide as it was in its column: a full-width row fills
     * its half, a row of buttons stands in the middle of it, and gives up room there as it did
     * where the screen was narrower than it.
     */
    private static FrameLayout cell(View view, ViewGroup.LayoutParams was) {
        final FrameLayout cell = new FrameLayout(view.getContext());
        final FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(was.width, was.height);
        copyMargins(was, lp);
        lp.gravity = Gravity.CENTER;
        cell.addView(view, lp);
        cell.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        return cell;
    }

    private static void part(View first, View second, View tail, Placed placed) {
        first.setTag(R.id.landscapeSplit, null);
        // The height and margins the panels set on the rows while they stood side by side
        // (the reading controls' row folds to nothing while nothing is read) stay with them
        keep(first, placed.firstLp);
        keep(second, placed.secondLp);
        detach(first);
        detach(second);
        if (tail != null && placed.tailParent != null) {
            detach(tail);
            placed.tailParent.addView(tail, Math.min(placed.tailIndex, placed.tailParent.getChildCount()),
                                      placed.tailLp);
        }
        placed.parent.removeView(placed.row);
        placed.parent.addView(first, Math.min(placed.firstIndex, placed.parent.getChildCount()), placed.firstLp);
        placed.parent.addView(second, Math.min(placed.secondIndex, placed.parent.getChildCount()), placed.secondLp);
    }

    private static void keep(View view, ViewGroup.LayoutParams was) {
        final ViewGroup.LayoutParams now = view.getLayoutParams();
        if (now == null || now == was) {
            return;
        }
        was.height = now.height;
        copyMargins(now, was);
    }

    private static void copyMargins(ViewGroup.LayoutParams from, ViewGroup.LayoutParams to) {
        if (from instanceof ViewGroup.MarginLayoutParams && to instanceof ViewGroup.MarginLayoutParams) {
            final ViewGroup.MarginLayoutParams f = (ViewGroup.MarginLayoutParams) from;
            ((ViewGroup.MarginLayoutParams) to).setMargins(f.leftMargin, f.topMargin, f.rightMargin, f.bottomMargin);
        }
    }

    private static void detach(View view) {
        if (view.getParent() instanceof ViewGroup) {
            ((ViewGroup) view.getParent()).removeView(view);
        }
    }
}
