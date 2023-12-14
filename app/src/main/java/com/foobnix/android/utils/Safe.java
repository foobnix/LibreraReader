package com.foobnix.android.utils;

import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;
import com.foobnix.LibreraApp;

import java.util.Random;

public class Safe {

    public static final String TXT_SAFE_RUN = "file://SAFE_RUN-";
    static Random r = new Random();
    static int counter;

    public static void run(final Runnable action) {
        run(action, false);
    }

    public static void run(final Runnable action, boolean fromLibrary) {


        if (LibreraApp.context == null || action == null) {
            return;
        }
        LOG.d("Safe fromLibrary", fromLibrary);
        if (fromLibrary && Build.VERSION.SDK_INT >= 29) {
            if (action != null) {
                action.run();
            }
            return;
        }


        LOG.d("Safe-isPaused", Glide.with(LibreraApp.context).isPaused());
        if (Glide.with(LibreraApp.context).isPaused()) {
            Glide.with(LibreraApp.context).resumeRequestsRecursive();
        }

        Glide.with(LibreraApp.context)
                .asBitmap().load(TXT_SAFE_RUN)
                .skipMemoryCache(true)
                .diskCacheStrategy(DiskCacheStrategy.NONE)
                .into(new CustomTarget<Bitmap>() {
                    @Override
                    public void onResourceReady(@NonNull Bitmap resource, @Nullable Transition<? super Bitmap> transition) {
                        if (action != null) {
                            action.run();
                        }
                    }

                    @Override
                    public void onLoadCleared(@Nullable Drawable placeholder) {

                    }

                });


    }


}
