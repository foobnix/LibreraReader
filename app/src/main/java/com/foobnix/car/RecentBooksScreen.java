package com.foobnix.car;


import static androidx.car.app.model.Row.IMAGE_TYPE_LARGE;

import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.car.app.CarContext;
import androidx.car.app.Screen;
import androidx.car.app.model.Action;
import androidx.car.app.model.ActionStrip;
import androidx.car.app.model.CarIcon;
import androidx.car.app.model.ItemList;
import androidx.car.app.model.ListTemplate;
import androidx.car.app.model.OnClickListener;
import androidx.car.app.model.Row;
import androidx.car.app.model.Template;
import androidx.core.graphics.drawable.IconCompat;
import androidx.lifecycle.DefaultLifecycleObserver;
import androidx.lifecycle.LifecycleOwner;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;
import com.foobnix.LibreraApp;
import com.foobnix.android.utils.LOG;
import com.foobnix.android.utils.TxtUtils;
import com.foobnix.dao2.FileMeta;
import com.foobnix.model.AppData;
import com.foobnix.model.AppSP;
import com.foobnix.model.SimpleMeta;
import com.foobnix.pdf.info.IMG;
import com.foobnix.pdf.info.R;
import com.foobnix.pdf.info.wrapper.DocumentController;
import com.foobnix.pdf.search.activity.HorizontalModeController;
import com.foobnix.sys.ImageExtractor;
import com.foobnix.tts.TTSEngine;
import com.foobnix.tts.TTSService;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class RecentBooksScreen extends Screen {

    public RecentBooksScreen(@NonNull CarContext carContext) {
        super(carContext);
        //getLifecycle().addObserver(this);
    }

    boolean isPlaying = false;
    FileMeta selected;

    Map<String, Bitmap> cache = new HashMap();


    @NonNull
    @Override

    public Template onGetTemplate() {
        ItemList.Builder listBuilder = new ItemList.Builder();


        List<FileMeta> allRecent = AppData.get().getAllRecent(false);
        if (allRecent.size() > 5) {
            allRecent = allRecent.subList(0, 5);
        }


        CarIcon icon = new CarIcon.Builder(IconCompat.createWithResource(getCarContext(), R.drawable.glyphicons_72_book)).build();

        for (FileMeta meta : allRecent) {

            Bitmap bitmap = cache.get(meta.getPath());
            if (bitmap == null) {
                String url = IMG.toUrl(meta.getPath(), ImageExtractor.COVER_PAGE_WITH_EFFECT, IMG.getImageSize());
                Glide.with(LibreraApp.context).asBitmap().load(url).into(new CustomTarget<Bitmap>() {


                    @Override
                    public void onResourceReady(@NonNull Bitmap resource, @Nullable Transition<? super Bitmap> transition) {
                        cache.put(meta.getPath(), resource);
                        invalidate();
                    }

                    @Override
                    public void onLoadCleared(Drawable placeholder) {

                    }

                });
            } else {
                icon = new CarIcon.Builder(IconCompat.createWithBitmap(bitmap)).build();
            }

            Row build = new Row.Builder()
                    .setTitle(meta.getTitle())
                    .setOnClickListener(new OnClickListener() {
                        @Override
                        public void onClick() {
                            selected = meta;
                            AppData.get().addRecent(new SimpleMeta(meta.getPath()));

                            TTSEngine.get().stop();
                            AppSP.get().lastBookPath = selected.getPath();

                            isPlaying = false;
                            invalidate();
                        }
                    })
                    .addText(TxtUtils.nullToEmpty(meta.getAuthor()))
                    .setImage(new CarIcon.Builder(icon).build(), IMAGE_TYPE_LARGE)
                    .build();
            listBuilder.addItem(build);
        }


        Action playPause = new Action.Builder()
                .setTitle(isPlaying ? getCarContext().getString(R.string.pause) : getCarContext().getString(R.string.play))
                .setOnClickListener(
                        () -> {
                            if (TTSEngine.get().isPlaying()) {
                                TTSEngine.get().stop();
                                isPlaying = false;
                            } else {
                                TTSService.playBookPage(AppSP.get().lastBookPage, AppSP.get().lastBookPath, "", AppSP.get().lastBookWidth, AppSP.get().lastBookHeight, AppSP.get().lastFontSize, AppSP.get().lastBookTitle);
                                isPlaying = true;
                            }
                            invalidate();
                        })
                .build();
        return new ListTemplate.Builder()
                .setSingleList(listBuilder.build())
                .setTitle(selected == null ? "Librera" : selected.getTitle())
                .setActionStrip(
                        new ActionStrip.Builder()
                                .addAction(new Action.Builder().setIcon(getIcon(R.drawable.glyphicons_86_reload)).setOnClickListener(() -> invalidate()).build())
                                .addAction(playPause)
                                .build())

                .build();
    }
    public CarIcon getIcon(int id){
        return new CarIcon.Builder(IconCompat.createWithResource(getCarContext(), id)).build();
    }


    public void onDestroy(@NonNull LifecycleOwner owner) {
        LOG.d("ListTemplateDemoScreen onDestroy");
        for (Bitmap b : cache.values()) {
            b.recycle();
            b = null;
        }
        cache.clear();
        cache = null;
    }
}
