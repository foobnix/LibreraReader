package com.foobnix.pdf.info.view;

import android.app.Activity;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;

import com.foobnix.pdf.info.R;
import com.foobnix.tts.TTSControlsView;

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

    /**
     * The screen's own measure, not the activity's configuration: the reading screens wrap their
     * context for the app's language (MyContextWrapper), and that configuration keeps the
     * orientation the screen was opened in through every turn.
     */
    public static boolean isLandscape(Activity a) {
        return DialogsPlaylist.isLandscape();
    }

    /** The bottom panel's two pairs of rows, in both reading screens. */
    public static void footer(Activity a) {
        final boolean split = isLandscape(a);
        final View controls = a.findViewById(R.id.playListParent);
        if (!split) {
            recentOnTop(a, controls, false);
        }
        apply(controls, a.findViewById(R.id.footerButtons), null, split);
        apply(a.findViewById(R.id.seekLine), a.findViewById(R.id.footerLastRow), null, split);
        if (split) {
            recentOnTop(a, controls, true);
        }
        DialogsPlaylist.fitStrip(a);
        // The reading controls go back into their row, or up a line, for where the tabs now are
        final View tts = a.findViewById(R.id.ttsActive);
        if (tts instanceof TTSControlsView) {
            tts.post(((TTSControlsView) tts)::standInPanel);
        }
    }

    /**
     * The recent books at the head of the panel while it stands on its side: the tabs of their
     * lists, then the strip of covers, over the reading controls and the buttons. Upright the
     * tabs share the controls' row and the strip is under it, and the controls go up a line
     * while the tabs are out; on its side that row is half the bar, too narrow for both.
     */
    private static void recentOnTop(Activity a, View controls, boolean top) {
        final View tabs = a.findViewById(R.id.playListTabsScroll);
        final View strip = a.findViewById(R.id.playlistRecycleView);
        if (controls == null || tabs == null || strip == null || !(controls instanceof ViewGroup)) {
            return;
        }
        final Object tag = tabs.getTag(R.id.landscapeSplit);
        if (top && tag == null) {
            final Object placed = controls.getTag(R.id.landscapeSplit);
            if (!(placed instanceof Placed) || strip.getParent() != ((Placed) placed).parent) {
                return;
            }
            final ViewGroup column = ((Placed) placed).parent;
            final LinearLayout row = ((Placed) placed).row;
            final Placed was = new Placed();
            was.parent = (ViewGroup) controls;
            was.firstIndex = was.parent.indexOfChild(tabs);
            was.firstLp = tabs.getLayoutParams();
            detach(tabs);
            column.removeView(strip);
            final int at = column.indexOfChild(row);
            column.addView(strip, at);
            column.addView(tabs, at, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                                                                   ViewGroup.LayoutParams.WRAP_CONTENT));
            tabs.setTag(R.id.landscapeSplit, was);
        } else if (!top && tag instanceof Placed) {
            final Placed was = (Placed) tag;
            tabs.setTag(R.id.landscapeSplit, null);
            final Object placed = controls.getTag(R.id.landscapeSplit);
            detach(tabs);
            was.parent.addView(tabs, Math.min(was.firstIndex, was.parent.getChildCount()), was.firstLp);
            // The strip back under the row, where the split puts the controls' row back
            if (placed instanceof Placed && strip.getParent() == ((Placed) placed).parent) {
                final ViewGroup column = ((Placed) placed).parent;
                column.removeView(strip);
                column.addView(strip, column.indexOfChild(((Placed) placed).row) + 1);
            }
        }
    }

    /** True while the tabs of the recent lists stand in the reading controls' row. */
    public static boolean tabsBesideControls(View tabs) {
        return tabs != null && tabs.getParent() instanceof View
                && ((View) tabs.getParent()).getId() == R.id.playListParent;
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
