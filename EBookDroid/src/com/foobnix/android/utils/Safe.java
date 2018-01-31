package com.foobnix.android.utils;

import java.util.Random;

import com.foobnix.pdf.info.IMG;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.listener.SimpleImageLoadingListener;

import android.graphics.Bitmap;
import android.view.View;

public class Safe {

    public static final String TXT_SAFE_RUN = "SAFE_RUN-";

    static Random r = new Random();

    public static void run(final Runnable action) {
        ImageLoader.getInstance().clearAllTasks();

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
