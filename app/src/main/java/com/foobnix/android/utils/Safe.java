package com.foobnix.android.utils;

import android.graphics.Bitmap;
import android.widget.ImageView;

import androidx.annotation.Nullable;

import com.bumptech.glide.Glide;
import com.bumptech.glide.Priority;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.SimpleTarget;
import com.bumptech.glide.request.target.Target;

import org.ebookdroid.LibreraApp;

import java.util.Random;

public class Safe {

    public static final String TXT_SAFE_RUN = "file://SAFE_RUN-";
    static Random r = new Random();
    static int counter;

    public static void run(final Runnable action) {
        Glide.with(LibreraApp.context)
                .asBitmap().load(TXT_SAFE_RUN)
                .skipMemoryCache(true)
                .diskCacheStrategy(DiskCacheStrategy.NONE)
                .priority(Priority.HIGH)
                .listener(new RequestListener<Bitmap>() {
                    @Override
                    public boolean onLoadFailed(@Nullable GlideException e, Object model, Target<Bitmap> target, boolean isFirstResource) {
                        return false;
                    }

                    @Override
                    public boolean onResourceReady(Bitmap resource, Object model, Target<Bitmap> target, DataSource dataSource, boolean isFirstResource) {
                        if (action != null) {
                            action.run();
                        }
                        return false;
                    }
                }).submit();


    }



    public static SimpleTarget<Bitmap> target(SimpleTarget<Bitmap> add) {
        return add;
    }

    public static ImageView target(ImageView view) {
        //targets2.add(view);
        return view;
    }


}
