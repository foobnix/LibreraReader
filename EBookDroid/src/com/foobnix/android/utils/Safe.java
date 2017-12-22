package com.foobnix.android.utils;

import java.util.Random;

import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.assist.FailReason;
import com.nostra13.universalimageloader.core.listener.SimpleImageLoadingListener;

import android.view.View;

public class Safe {

    public static final String TXT_SAFE_RUN = "SAFE_RUN-";

    static Random r = new Random();

    public static void run(final Runnable action) {
        ImageLoader.getInstance().clearAllTasks();

        ImageLoader.getInstance().loadImage(TXT_SAFE_RUN + r.nextInt(), new SimpleImageLoadingListener() {
            @Override
            public void onLoadingFailed(String imageUri, View view, FailReason failReason) {
                LOG.d(TXT_SAFE_RUN, "Safe run");
                if (action != null) {
                    ImageLoader.getInstance().clearAllTasks();
                    action.run();
                }
            }
        });

    }
}
