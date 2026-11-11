package com.foobnix.pdf.search.activity;

import android.view.View;

import com.foobnix.android.utils.LOG;
import com.foobnix.model.AppState;
import com.foobnix.pdf.info.R;
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

}


