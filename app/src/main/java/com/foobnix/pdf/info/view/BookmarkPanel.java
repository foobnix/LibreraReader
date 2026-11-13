package com.foobnix.pdf.info.view;

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.RippleDrawable;
import android.view.Gravity;
import android.view.View;
import android.view.View.OnLongClickListener;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.core.graphics.ColorUtils;
import androidx.viewpager.widget.ViewPager.LayoutParams;

import com.foobnix.android.utils.Dips;
import com.foobnix.android.utils.TxtUtils;
import com.foobnix.model.AppBookmark;
import com.foobnix.model.AppState;
import com.foobnix.pdf.info.BookmarksData;
import com.foobnix.pdf.info.R;
import com.foobnix.pdf.info.TintUtil;
import com.foobnix.pdf.info.wrapper.DocumentController;
import com.foobnix.pdf.info.wrapper.MagicHelper;

import java.util.List;

public class BookmarkPanel {

    private static final int SIZE = Dips.DP_32;

    public static void showPagesHelper(final LinearLayout pageshelper, final View musicButtonPanel,
                                       final DocumentController dc, final ImageView pagesBookmark,
                                       final String quickBookmark, final Runnable onRefresh) {
        pageshelper.removeAllViews();
        if (dc == null) {
            return;
        }
        // With the panels up the bookmarks step aside: they would stand under the bars
        if (AppState.get().isEditMode) {
            musicButtonPanel.setVisibility(View.GONE);
            return;
        }

        // The bookmarks, and the button that adds one, are shown only in the modes they are
        // asked for in
        final boolean listShown = AppState.get().isShowBookmarsPanelInMusicMode && dc.isMusicianMode()
                || AppState.get().isShowBookmarsPanelInBookMode && dc.isBookMode()
                || AppState.get().isShowBookmarsPanelInScrollMode && dc.isScrollMode();
        if (!listShown) {
            musicButtonPanel.setVisibility(View.GONE);
            return;
        }
        musicButtonPanel.setVisibility(View.VISIBLE);


        // Each bookmark stands on the page as the panels' own buttons do: a round of the panels'
        // colour, as see-through as they are, with their text colour on it
        final int bookmarkColor = MagicHelper.getTextOrIconColor();
        TintUtil.setTintImageNoAlpha(pagesBookmark, bookmarkColor);
        pagesBookmark.setBackground(panelRound());
        pagesBookmark.setPadding(Dips.DP_8, Dips.DP_8, Dips.DP_8, Dips.DP_8);
        pagesBookmark.getLayoutParams().width = pagesBookmark.getLayoutParams().height = SIZE;
        pagesBookmark.requestLayout();

        List<AppBookmark> all = BookmarksData.get().getBookmarksByBook(dc.getCurrentBook());
        for (final AppBookmark appBookmark : all) {

            final int num = appBookmark.getPage(dc.getPageCount());
            TextView t = new TextView(pageshelper.getContext());
            boolean isQuick = appBookmark.text.equals(quickBookmark);
            if (AppState.get().isShowBookmarsPanelText && !isQuick) {
                String substringSmart = TxtUtils.substringSmart(appBookmark.text, 20);
                substringSmart = TxtUtils.firstUppercase(substringSmart).trim();
                t.setText(substringSmart);
            } else {
                t.setText("" + (num));
            }

            if (appBookmark.isF) {
                t.setText(t.getText() + " :{" + appBookmark.getPage(dc.getPageCount()) + "}");
            }

            t.setSingleLine();
            t.setGravity(Gravity.CENTER);
            t.setTypeface(Typeface.DEFAULT, Typeface.NORMAL);
            t.setTextSize(14);
            t.setBackground(panelRound());
            t.setTextColor(bookmarkColor);


            t.setOnClickListener(new View.OnClickListener() {

                @Override
                public void onClick(View v) {
                    //if (appBookmark.getPercent() > 0) {
                    //    dc.onScrollYPercent(appBookmark.getPercent());
                    //} else {
                    //}
                    if (appBookmark.isF) {
                        dc.floatingBookmark = appBookmark;
                    } else {
                        dc.floatingBookmark = null;
                    }

                    dc.goToBookmark(appBookmark, () -> { });
                }
            });
            t.setOnLongClickListener(new OnLongClickListener() {

                @Override
                public boolean onLongClick(View v) {
                    AlertDialogs.showOkDialog(dc.getActivity(), dc.getString(R.string.delete_this_bookmark_), new Runnable() {

                        @Override
                        public void run() {
                            if (appBookmark.isF) {
                                dc.floatingBookmark = null;
                                onRefresh.run();
                            }
                            BookmarksData.get().remove(appBookmark);
                            showPagesHelper(pageshelper, musicButtonPanel, dc, pagesBookmark, quickBookmark, onRefresh);

                        }
                    });

                    return true;
                }
            });
            // A circle, or a pill where the page number or the text needs more room; spaced
            // as the buttons in the bar above are
            t.setPadding(Dips.DP_8, 0, Dips.DP_8, 0);
            t.setMinWidth(SIZE);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT, SIZE);
            params.leftMargin = pageshelper.getChildCount() == 0 ? 0 : Dips.DP_4;
            pageshelper.addView(t, params);
        }
    }

    /** A pill - a circle when as wide as tall - of the panels' colour, answering a press. */
    private static RippleDrawable panelRound() {
        final GradientDrawable fill = new GradientDrawable();
        fill.setColor(ColorUtils.setAlphaComponent(TintUtil.color, AppState.get().transparencyUI));
        fill.setCornerRadius(SIZE / 2f);
        final GradientDrawable mask = new GradientDrawable();
        mask.setColor(Color.WHITE);
        mask.setCornerRadius(SIZE / 2f);
        return new RippleDrawable(ColorStateList.valueOf(Color.parseColor("#3c888888")), fill, mask);
    }
}
