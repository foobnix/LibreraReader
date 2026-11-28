package com.foobnix.pdf.search.menu;

import android.content.Context;
import android.view.View;
import android.widget.TextView;

import com.foobnix.android.utils.TxtUtils;
import com.foobnix.model.AppState;
import com.foobnix.pdf.info.R;
import com.foobnix.pdf.info.view.MyPopupMenu;

/**
 * How the paged view goes from page to page: at once, sliding across or up, or turned as the
 * page of a book - side by side, or up and down as a calendar. One choice over the settings it
 * is made of: the slide, the page curl and the pager's direction.
 */
public final class PageEffect {

    public static final int NONE = 0;
    public static final int SIMPLE_HORIZONTAL = 1;
    public static final int SIMPLE_VERTICAL = 2;
    public static final int BOOK_HORIZONTAL = 3;
    public static final int BOOK_VERTICAL = 4;

    private static final int[] NAMES = {
            R.string.page_effect_none,
            R.string.page_effect_simple_horizontal,
            R.string.page_effect_simple_vertical,
            R.string.page_effect_book_horizontal,
            R.string.page_effect_book_vertical};

    /** Each effect drawn: a page, the way it goes, and the curl of a book's page. */
    public static final int[] ICONS = {
            R.drawable.page_effect_none,
            R.drawable.page_effect_simple_horizontal,
            R.drawable.page_effect_simple_vertical,
            R.drawable.page_effect_book_horizontal,
            R.drawable.page_effect_book_vertical};

    public interface OnChosen {
        void onChosen(int effect);
    }

    private PageEffect() {
    }

    public static int get() {
        final AppState s = AppState.get();
        final boolean vertical = s.rotateViewPager == 90;
        if (s.isBookPageCurl) {
            return vertical ? BOOK_VERTICAL : BOOK_HORIZONTAL;
        }
        if (s.isScrollAnimation) {
            return vertical ? SIMPLE_VERTICAL : SIMPLE_HORIZONTAL;
        }
        return NONE;
    }

    /**
     * Sets the settings the effect is made of. None keeps the pages going the way they went.
     * Returns whether that way changed: the pager is made for one way, so the book is opened
     * again then.
     */
    public static boolean set(int effect) {
        final AppState s = AppState.get();
        final int before = s.rotateViewPager;
        s.isBookPageCurl = effect == BOOK_HORIZONTAL || effect == BOOK_VERTICAL;
        // Also under the curl: a turn it cannot make (no page view yet) slides instead
        s.isScrollAnimation = effect != NONE;
        if (effect != NONE) {
            s.rotateViewPager = effect == SIMPLE_VERTICAL || effect == BOOK_VERTICAL ? 90 : 0;
        }
        return before != s.rotateViewPager;
    }

    public static String name(Context c, int effect) {
        return c.getString(NAMES[effect]);
    }

    /** The effects to pick from under the anchor, the current one ticked (-1: none of them). */
    public static void choose(View anchor, int current, OnChosen onChosen) {
        final MyPopupMenu p = new MyPopupMenu(anchor.getContext(), anchor);
        for (int i = 0; i < NAMES.length; i++) {
            final int effect = i;
            final MyPopupMenu.Menu item = p.getMenu().add(NAMES[i]);
            if (i == current) {
                item.setIcon(R.drawable.glyphicons_739_check);
            }
            item.setOnMenuItemClickListener(menuItem -> {
                onChosen.onChosen(effect);
                return false;
            });
        }
        p.show();
    }

    /**
     * A settings row's link that shows the effect and opens the choice. {@code onChanged} runs
     * after a choice, the direction it may have changed with it.
     */
    public static void bind(final TextView link, final Runnable onChanged) {
        show(link);
        link.setOnClickListener(v -> choose(v, get(), effect -> {
            set(effect);
            show(link);
            if (onChanged != null) {
                onChanged.run();
            }
        }));
    }

    /** Shows the effect as it is set now on a link {@link #bind} made. */
    public static void show(TextView link) {
        link.setText(name(link.getContext(), get()));
        TxtUtils.underlineTextView(link);
    }
}
