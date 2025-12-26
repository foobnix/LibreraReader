package com.google.android.gms.ads.rewarded;

import android.app.Activity;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.OnUserEarnedRewardListener;

public class RewardedAd {

    public static RewardedAd load(Activity a, String s, AdRequest build, RewardedAdLoadCallback rewardedAdLoadCallback) {
        return new RewardedAd();
    }

    public void show(Activity a, OnUserEarnedRewardListener listener) {
    }
}
