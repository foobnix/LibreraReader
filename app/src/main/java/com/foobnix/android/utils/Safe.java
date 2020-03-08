package com.foobnix.android.utils;

import android.graphics.Bitmap;
import android.view.View;
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

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class Safe {

    public static final String TXT_SAFE_RUN = "file://SAFE_RUN-";
    public static List<Target> targets = new ArrayList<>();
    public static List<ImageView> targets2 = new ArrayList<>();
    static Random r = new Random();
    static int counter;

    public static void run(final Runnable action) {
        clearAll(null);


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
                        clearAll(target);
                        if (action != null) {
                            action.run();
                        }
                        return false;
                    }
                }).submit();


    }

    public static void clearAll() {
        clearAll(null);
    }
    public static void clearAll(Target exlude) {

        for (Target t : targets) {
            if (exlude != null && exlude.equals(t)) {
                continue;
            }
            Glide.with(LibreraApp.context).clear(t);
        }
        for (View v : targets2) {
            Glide.with(LibreraApp.context).clear(v);
        }

    }

    public static SimpleTarget<Bitmap> target(SimpleTarget<Bitmap> add) {
        targets.add(add);
        return add;
    }

    public static ImageView targetView(ImageView view) {
        targets2.add(view);
        return view;
    }


}
