package com.foobnix.android.utils;

import android.graphics.Bitmap;
import android.view.View;

import com.foobnix.pdf.info.IMG;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.listener.SimpleImageLoadingListener;

import java.util.Random;

public class Safe {

    public static final String TXT_SAFE_RUN = "SAFE_RUN-";

    static Random r = new Random();

    public static void run(final Runnable action) {
        LOG.d(TXT_SAFE_RUN, "run");
        ImageLoader.getInstance().clearAllTasks();
        LOG.d(TXT_SAFE_RUN, "clearAllTasks");
        ImageLoader.getInstance().loadImage(TXT_SAFE_RUN, IMG.noneOptions, new SimpleImageLoadingListener() {
            @Override
            public void onLoadingComplete(String imageUri, View view, Bitmap loadedImage) {
                LOG.d(TXT_SAFE_RUN, "end", imageUri, "action", action);
                if (action != null) {
                    ImageLoader.getInstance().clearAllTasks();
                    action.run();
                }
            }
        });

    }
}
