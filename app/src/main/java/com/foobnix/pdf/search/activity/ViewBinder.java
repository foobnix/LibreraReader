package com.foobnix.pdf.search.activity;

import android.app.Activity;
import android.view.View;
import android.widget.TextView;

import com.foobnix.android.utils.LOG;
import com.foobnix.model.AppState;
import com.foobnix.pdf.info.R;
import com.foobnix.ui2.AdsFragmentActivity;

public class ViewBinder {

    public static void hideShowRewardButton(AdsFragmentActivity a, TextView button) {
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

    public static TextView initRewardButton(AdsFragmentActivity a, int id) {
        TextView showRewardVideo = a.findViewById(id);

        try {

            if (a.isRewardActivated() || !a.isRewardLoaded()) {
                showRewardVideo.setVisibility(View.GONE);
            } else {
                showRewardVideo.setVisibility(View.VISIBLE);
            }

            showRewardVideo.setText("\uD83C\uDF81" + " " + showRewardVideo.getText());
            showRewardVideo.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    a.showRewardVideo(rewardItem -> {
                        AppState.get().rewardShowedDate = System.currentTimeMillis();
                        showRewardVideo.setVisibility(View.GONE);
                    });
                }
            });

        } catch (Exception e) {
            LOG.e(e);
        }
        return showRewardVideo;
    }
}


