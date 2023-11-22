package com.foobnix.car;


import static androidx.car.app.model.Row.IMAGE_TYPE_LARGE;

import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.Drawable;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.OptIn;
import androidx.car.app.CarContext;
import androidx.car.app.Screen;
import androidx.car.app.annotations.ExperimentalCarApi;
import androidx.car.app.constraints.ConstraintManager;
import androidx.car.app.model.Action;
import androidx.car.app.model.ActionStrip;
import androidx.car.app.model.Badge;
import androidx.car.app.model.CarColor;
import androidx.car.app.model.CarIcon;
import androidx.car.app.model.GridItem;
import androidx.car.app.model.GridTemplate;
import androidx.car.app.model.ItemList;
import androidx.car.app.model.MessageTemplate;
import androidx.car.app.model.OnClickListener;
import androidx.car.app.model.Pane;
import androidx.car.app.model.PaneTemplate;
import androidx.car.app.model.Row;
import androidx.car.app.model.SearchTemplate;
import androidx.car.app.model.Tab;
import androidx.car.app.model.TabContents;
import androidx.car.app.model.TabTemplate;
import androidx.car.app.model.Template;
import androidx.core.graphics.drawable.IconCompat;
import androidx.lifecycle.LifecycleOwner;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;
import com.foobnix.LibreraApp;
import com.foobnix.android.utils.LOG;
import com.foobnix.dao2.FileMeta;
import com.foobnix.model.AppData;
import com.foobnix.model.AppSP;
import com.foobnix.model.SimpleMeta;
import com.foobnix.pdf.info.IMG;
import com.foobnix.pdf.info.R;
import com.foobnix.sys.ImageExtractor;
import com.foobnix.tts.TTSEngine;
import com.foobnix.tts.TTSService;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class RecentBooksScreen extends Screen {
    static String TAG = "RecentBooksScreen";
    boolean isPlaying = false;
    FileMeta selected;
    Map<String, Bitmap> cache = new HashMap();
    boolean isSearchTempaate = false;

    public RecentBooksScreen(@NonNull CarContext carContext) {
        super(carContext);
        ConstraintManager manager = getCarContext().getCarService(ConstraintManager.class);
        int gridItemLimit = manager.getContentLimit(ConstraintManager.CONTENT_LIMIT_TYPE_GRID);
        int listItemLimit = manager.getContentLimit(ConstraintManager.CONTENT_LIMIT_TYPE_LIST);
        LOG.d(TAG,"gridItemLimit", gridItemLimit);
        LOG.d(TAG,"listItemLimit", listItemLimit);


        //getLifecycle().addObserver(this);
    }

    @OptIn(markerClass = ExperimentalCarApi.class)
    @NonNull
    @Override
    public Template onGetTemplate() {

        LOG.d(TAG, "onGetTemplate getCarAppApiLevel", getCarContext().getCarAppApiLevel());
        if (isSearchTempaate) {
            return new MessageTemplate.Builder("Message template")
                    .setTitle("Librera").build();
        }
        if (false) {
            return new TabTemplate.Builder(new TabTemplate.TabCallback() {
                @Override
                public void onTabSelected(@NonNull String tabContentId) {
                    TabTemplate.TabCallback.super.onTabSelected(tabContentId);
                }
            })
                    .addTab(new Tab.Builder().setContentId("id1")
                            .setIcon(CarIcon.APP_ICON).setTitle("Hello1")
                            .build())
                    .addTab(new Tab.Builder()
                            .setContentId("id2").setIcon(CarIcon.APP_ICON).setTitle("Hello2").build())
                    .setTabContents(new TabContents.Builder(new MessageTemplate.Builder("ID1").build()).build())
                    .setLoading(false)
                    .setActiveTabContentId("id1")
                    .setHeaderAction(Action.APP_ICON).build();
        }
        if (false) {
            return new PaneTemplate.Builder(
                    new Pane.Builder()

                            .addRow(new Row.Builder().setTitle("Title").addText("Hello").build())
                            .addRow(new Row.Builder().setTitle("title").setImage(CarIcon.APP_ICON, IMAGE_TYPE_LARGE).build())
                            .addAction(new Action.Builder().setIcon(CarIcon.APP_ICON).setTitle("PLAY/PAUSE").setOnClickListener(() -> {
                            }).build())
                            .addAction(new Action.Builder().setIcon(CarIcon.APP_ICON).setTitle("Next").setOnClickListener(() -> {
                            }).build())
                            .setImage(CarIcon.BACK)
                            .build()

            )
                    .setTitle("Demo Pane")

                    .setHeaderAction(Action.BACK)

                    .build();
        }


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

            GridItem build = new GridItem.Builder()
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
                    //.addText(TxtUtils.nullToEmpty(meta.getAuthor()))
                    .setImage(new CarIcon.Builder(icon).build(), GridItem.IMAGE_TYPE_LARGE, new Badge.Builder().setIcon(CarIcon.PAN).build())

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
        return new GridTemplate.Builder()
                .setItemSize(GridTemplate.ITEM_SIZE_LARGE)
                .setItemImageShape(GridTemplate.ITEM_IMAGE_SHAPE_UNSET)
                .setSingleList(listBuilder.build())
                .setTitle(selected == null ? "Librera - Recent" : selected.getTitle())
                .setHeaderAction(new Action.Builder().setIcon(getIcon(R.drawable.glyphicons_422_book_library)).build())
                .setActionStrip(
                        new ActionStrip.Builder()
                                .addAction(new Action.Builder().setIcon(getIcon(R.drawable.glyphicons_600_menu)).setOnClickListener(() -> invalidate()).build())
                                //.addAction(playPause)
                                .addAction(new Action.Builder().setIcon(getIcon(R.drawable.glyphicons_28_search)).setOnClickListener(() -> {
                                }).build())
                                .build())
                //.addAction(new Action.Builder().setBackgroundColor(CarColor.BLUE).setIcon(CarIcon.ALERT).build())
                .build();

        // Create a row with the search icon


        //    return new SearchTemplate.Builder(getCallBack(this))
        //          .setItemList(listBuilder.build()).build();


    }


    private SearchTemplate.SearchCallback getCallBack(Screen screen) {
        return new SearchTemplate.SearchCallback() {
            @Override
            public void onSearchTextChanged(@NonNull String searchText) {
                LOG.d(TAG, "onSearchTextChanged", searchText);
            }

            @Override
            public void onSearchSubmitted(@NonNull String searchText) {
                LOG.d(TAG, "onSearchSubmitted", searchText);
                isSearchTempaate = true;
                screen.invalidate();


            }
        };

    }

    public CarIcon getIcon(int id) {
        return new CarIcon.Builder(IconCompat.createWithResource(getCarContext(), id)).setTint(CarColor.createCustom(Color.WHITE, Color.WHITE)).build();
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
