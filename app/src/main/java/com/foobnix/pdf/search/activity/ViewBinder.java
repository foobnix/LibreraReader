package com.foobnix.pdf.search.activity;

import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.foobnix.android.utils.LOG;
import com.foobnix.pdf.info.R;
import com.foobnix.model.AppState;
import com.foobnix.pdf.info.view.UnderlineImageView;
import com.foobnix.ui2.AdsFragmentActivity;

public class ViewBinder {

    public static void updateBrightness(UnderlineImageView onBC){
        LOG.d("updateBrightness",AppState.get().isShowContrastButton,AppState.get().isEnableBCOptional1);
        onBC.setVisibility(View.GONE);
        if (AppState.get().isShowContrastButton || AppState.get().isEnableBCOptional1) {
            onBC.setVisibility(View.VISIBLE);
        }

        onBC.underline(AppState.get().isEnableBCOptional1);


    }

    public static void hideShowRewardButton(AdsFragmentActivity a, View button) {
        try {
            if (a == null || button == null) {
                return;
            }
            if (a.isRewardActivated() || !a.isRewardLoaded()) {
                button.setVisibility(View.GONE);
            } else {
                button.setVisibility(View.VISIBLE);
            }
        } catch (Exception e) {
            LOG.e(e);
        }
    }

    public static View initRewardButton(AdsFragmentActivity a, int id) {
        View showRewardVideo = a.findViewById(id);

        try {

            if (a.isRewardActivated() || !a.isRewardLoaded()) {
                showRewardVideo.setVisibility(View.GONE);
            } else {
                showRewardVideo.setVisibility(View.VISIBLE);
            }

            sizeMarkToMessage(showRewardVideo.findViewById(R.id.showRewardVideoIcon),
                    showRewardVideo.findViewById(R.id.showRewardVideoText));

            View.OnClickListener onWatch = new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    a.showRewardVideo(rewardItem -> {

                        showRewardVideo.setVisibility(View.GONE);
                    });
                }
            };

            // The whole row answers a press, as it did when it was one line of text, and so
            // does the button that now says what pressing it does.
            showRewardVideo.setOnClickListener(onWatch);
            View watch = showRewardVideo.findViewById(R.id.showRewardVideoButton);
            if (watch != null) {
                watch.setOnClickListener(onWatch);
            }

        } catch (Exception e) {
            LOG.e(e);
        }
        return showRewardVideo;
    }

    /**
     * Cuts the video mark to the height of the two lines it stands beside. The message is two
     * lines whatever size the reader has the text at, so the height is taken from the line the
     * message is actually drawn at rather than fixed in the layout; the mark's own width
     * follows from it.
     */
    private static void sizeMarkToMessage(View mark, TextView message) {
        if (mark == null || message == null) {
            return;
        }
        ViewGroup.LayoutParams lp = mark.getLayoutParams();
        if (lp != null) {
            // Half again the line, not the full two: a mark that fills the row edge to edge
            // crowds the words it stands in front of.
            lp.height = message.getLineHeight() * 3 / 2;
            mark.setLayoutParams(lp);
        }
    }
}


