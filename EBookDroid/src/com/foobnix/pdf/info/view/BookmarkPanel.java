package com.foobnix.pdf.info.view;

import java.util.List;

import com.foobnix.android.utils.Dips;
import com.foobnix.android.utils.TxtUtils;
import com.foobnix.pdf.info.AppSharedPreferences;
import com.foobnix.pdf.info.R;
import com.foobnix.pdf.info.wrapper.AppBookmark;
import com.foobnix.pdf.info.wrapper.AppState;
import com.foobnix.pdf.info.wrapper.DocumentController;

import android.graphics.Typeface;
import android.support.v4.view.ViewPager.LayoutParams;
import android.view.Gravity;
import android.view.View;
import android.view.View.OnLongClickListener;
import android.widget.LinearLayout;
import android.widget.TextView;

public class BookmarkPanel {

    public static void showPagesHelper(final LinearLayout pageshelper, final View musicButtonPanel, final DocumentController dc, final View pagesBookmark, final String quickBookmark) {
        pageshelper.removeAllViews();

        if (AppState.get().isShowBookmarsPanelInMusicMode && dc.isMusicianMode()) {
            musicButtonPanel.setVisibility(View.VISIBLE);
        } else if (AppState.get().isShowBookmarsPanelInBookMode && dc.isBookMode()) {
            musicButtonPanel.setVisibility(View.VISIBLE);
        } else if (AppState.get().isShowBookmarsPanelInScrollMode && dc.isScrollMode()) {
            musicButtonPanel.setVisibility(View.VISIBLE);
        } else {
            musicButtonPanel.setVisibility(View.GONE);
            return;
        }

        if (AppState.get().isDayNotInvert) {
            pagesBookmark.setBackgroundResource(R.drawable.bg_border_ltgray_dash2);
        } else {
            pagesBookmark.setBackgroundResource(R.drawable.bg_border_ltgray_dash2_night);
        }
        pagesBookmark.setPadding(Dips.DP_15, Dips.DP_15, Dips.DP_15, Dips.DP_15);

        List<AppBookmark> all = AppSharedPreferences.get().getBookmarksByBook(dc.getCurrentBook());
        for (final AppBookmark bookmarks : all) {
            final int num = bookmarks.getPage();
            TextView t = new TextView(pageshelper.getContext());
            boolean isQuick = bookmarks.getText().equals(quickBookmark);
            if (AppState.get().isShowBookmarsPanelText && !isQuick) {
                String substringSmart = TxtUtils.substringSmart(bookmarks.getText(), 20);
                substringSmart = TxtUtils.firstUppercase(substringSmart);
                t.setText(substringSmart);
            } else {
                t.setText("" + (num));
            }
            t.setGravity(Gravity.CENTER);
            t.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
            t.setTextSize(16);
            if (AppState.get().isDayNotInvert) {
                t.setBackgroundResource(R.drawable.bg_border_ltgray_dash2);
            } else {
                t.setBackgroundResource(R.drawable.bg_border_ltgray_dash2_night);
            }
            t.setOnClickListener(new View.OnClickListener() {

                @Override
                public void onClick(View v) {
                    dc.onGoToPage(num);
                }
            });
            t.setOnLongClickListener(new OnLongClickListener() {

                @Override
                public boolean onLongClick(View v) {
                    AlertDialogs.showOkDialog(dc.getActivity(), dc.getString(R.string.delete_this_bookmark_), new Runnable() {

                        @Override
                        public void run() {
                            AppSharedPreferences.get().removeBookmark(bookmarks);
                            showPagesHelper(pageshelper, musicButtonPanel, dc, pagesBookmark, quickBookmark);
                        }
                    });

                    return true;
                }
            });
            if (AppState.get().isShowBookmarsPanelText && !isQuick) {
                t.setPadding(Dips.DP_8, Dips.DP_2, Dips.DP_8, Dips.DP_2);
                t.setMinWidth(Dips.DP_60);
                LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT, Dips.DP_60);
                params.leftMargin = Dips.DP_3;
                pageshelper.addView(t, params);
            } else {
                LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(Dips.DP_60, Dips.DP_60);
                params.leftMargin = Dips.DP_3;
                pageshelper.addView(t, params);
            }
        }
    }
}
