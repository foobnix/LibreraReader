package com.foobnix.pdf.info.view;

import android.view.View;

import com.foobnix.model.AppState;
import com.foobnix.pdf.info.R;

/**
 * The seek line's row in the bottom panel of both reading screens. It holds the seek bar, which a
 * setting shows or hides, and the back button that comes after a link is followed: on a phone
 * there was no room for that button at the end of the row of buttons. The row stands while
 * either is there, so turning the seek bar off does not take the back button with it.
 */
public final class SeekLine {

    private SeekLine() {
    }

    /** @param panel whether the panel the row is in is shown */
    public static void show(View seekLine, boolean panel) {
        if (seekLine == null) {
            return;
        }
        final boolean bar = AppState.get().isShowBottomSeekBar;
        final View parts = seekLine.findViewById(R.id.seekParts);
        if (parts != null) {
            parts.setVisibility(bar ? View.VISIBLE : View.GONE);
        }
        final View back = seekLine.findViewById(R.id.linkHistory);
        final boolean backShown = back != null && back.getVisibility() == View.VISIBLE;
        seekLine.setVisibility(panel && (bar || backShown) ? View.VISIBLE : View.GONE);
    }
}
