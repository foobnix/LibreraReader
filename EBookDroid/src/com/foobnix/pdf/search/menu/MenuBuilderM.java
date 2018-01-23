package com.foobnix.pdf.search.menu;

import com.foobnix.pdf.info.R;
import com.foobnix.pdf.info.wrapper.AppState;
import com.foobnix.pdf.info.wrapper.DocumentController;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.view.MenuItem;
import android.view.MenuItem.OnMenuItemClickListener;
import android.view.View;
import android.widget.PopupMenu;

@SuppressLint("NewApi")
public class MenuBuilderM {





    public static PopupMenu bookMenu(final View view, final Activity a, final String pageUrl, final Runnable onReloadDocument) {
        final PopupMenu popupMenu = new PopupMenu(view.getContext(), view);

        addOrientationMenu(view, a, popupMenu);

        final MenuItem fullscren = popupMenu.getMenu().add(R.string.full_screen);
        fullscren.setCheckable(true);
        fullscren.setChecked(AppState.get().isFullScreen);
        fullscren.setOnMenuItemClickListener(new OnMenuItemClickListener() {

            @Override
            public boolean onMenuItemClick(final MenuItem item) {
                AppState.get().isFullScreen = !AppState.get().isFullScreen;
                fullscren.setChecked(AppState.get().isFullScreen);
                DocumentController.chooseFullScreen(a, AppState.get().isFullScreen);
                return false;
            }
        });

        final MenuItem keys = popupMenu.getMenu().add(R.string.reverse_keys);
        keys.setCheckable(true);
        keys.setChecked(AppState.get().isReverseKeys);
        keys.setOnMenuItemClickListener(new OnMenuItemClickListener() {

            @Override
            public boolean onMenuItemClick(final MenuItem item) {
                AppState.get().isReverseKeys = !AppState.get().isReverseKeys;
                keys.setChecked(AppState.get().isReverseKeys);
                return false;
            }
        });

        final MenuItem crop = popupMenu.getMenu().add(R.string.crop_white_borders);
        crop.setCheckable(true);
        crop.setChecked(AppState.get().isCrop);
        crop.setOnMenuItemClickListener(new OnMenuItemClickListener() {

            @Override
            public boolean onMenuItemClick(final MenuItem item) {
                AppState.get().isCrop = !AppState.get().isCrop;
                crop.setChecked(AppState.get().isCrop);
                onReloadDocument.run();
                return false;
            }
        });
        final MenuItem invert = popupMenu.getMenu().add(R.string.invert_colors);
        invert.setCheckable(true);
        invert.setChecked(AppState.get().isDayNotInvert);
        invert.setOnMenuItemClickListener(new OnMenuItemClickListener() {

            @Override
            public boolean onMenuItemClick(final MenuItem item) {
                AppState.get().isDayNotInvert = !AppState.get().isDayNotInvert;
                invert.setChecked(AppState.get().isDayNotInvert);
                onReloadDocument.run();
                return false;
            }
        });

        addRotateMenu(view, popupMenu, onReloadDocument);


        return popupMenu;
    }

    public static PopupMenu addOrientationMenu(final View view, final Activity a, final PopupMenu popupMenu) {
        String title = a.getString(R.string.orientation) + ": ";

        if (AppState.get().orientation == ActivityInfo.SCREEN_ORIENTATION_SENSOR) {
            title += a.getString(R.string.automatic);
        } else if (AppState.get().orientation == ActivityInfo.SCREEN_ORIENTATION_PORTRAIT) {
            title += a.getString(R.string.portrait);
        } else if (AppState.get().orientation == ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE) {
            title += a.getString(R.string.landscape);
        }

        a.getString(R.string.automatic);

        final PopupMenu orientationMenu = new PopupMenu(view.getContext(), view);

        if (popupMenu != null) {
            final MenuItem orientation = popupMenu.getMenu().add(title);
            orientation.setOnMenuItemClickListener(new OnMenuItemClickListener() {

                @Override
                public boolean onMenuItemClick(final MenuItem item) {
                    orientationMenu.show();
                    return false;
                }
            });
        }

        final MenuItem auto = orientationMenu.getMenu().add(R.string.automatic);
        auto.setOnMenuItemClickListener(new OnMenuItemClickListener() {

            @Override
            public boolean onMenuItemClick(final MenuItem item) {
                AppState.get().orientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR;
                DocumentController.doRotation(a);
                return false;
            }
        });
        final MenuItem port = orientationMenu.getMenu().add(R.string.portrait);
        port.setOnMenuItemClickListener(new OnMenuItemClickListener() {

            @Override
            public boolean onMenuItemClick(final MenuItem item) {
                AppState.get().orientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT;
                DocumentController.doRotation(a);
                return false;
            }
        });

        final MenuItem land = orientationMenu.getMenu().add(R.string.landscape);
        land.setOnMenuItemClickListener(new OnMenuItemClickListener() {

            @Override
            public boolean onMenuItemClick(final MenuItem item) {
                AppState.get().orientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE;
                DocumentController.doRotation(a);
                return false;
            }
        });
        return orientationMenu;
    }

    public static PopupMenu addRotateMenu(final View view, final PopupMenu popupMenu, final Runnable ranRunnable) {

        Context a = view.getContext();
        String title = a.getString(R.string.rotate);

        if (AppState.get().rotate > 0) {
            title = a.getString(R.string.rotate) + ": " + AppState.get().rotate;
        }

        final PopupMenu rotateMenu = new PopupMenu(view.getContext(), view);

        if (popupMenu != null) {
            final MenuItem rotate = popupMenu.getMenu().add(title);
            rotate.setOnMenuItemClickListener(new OnMenuItemClickListener() {

                @Override
                public boolean onMenuItemClick(final MenuItem item) {
                    rotateMenu.show();
                    return false;
                }
            });
        }

        final MenuItem r1 = rotateMenu.getMenu().add(R.string.rotate_default);
        r1.setOnMenuItemClickListener(new OnMenuItemClickListener() {

            @Override
            public boolean onMenuItemClick(final MenuItem item) {
                AppState.get().rotate = 0;
                ranRunnable.run();
                return false;
            }
        });

        final MenuItem r2 = rotateMenu.getMenu().add("90°");
        r2.setOnMenuItemClickListener(new OnMenuItemClickListener() {

            @Override
            public boolean onMenuItemClick(final MenuItem item) {
                AppState.get().rotate = 90;
                ranRunnable.run();
                return false;
            }
        });
        final MenuItem r3 = rotateMenu.getMenu().add("180°");
        r3.setOnMenuItemClickListener(new OnMenuItemClickListener() {

            @Override
            public boolean onMenuItemClick(final MenuItem item) {
                AppState.get().rotate = 180;
                ranRunnable.run();

                return false;
            }
        });

        final MenuItem r4 = rotateMenu.getMenu().add("270°");
        r4.setOnMenuItemClickListener(new OnMenuItemClickListener() {

            @Override
            public boolean onMenuItemClick(final MenuItem item) {
                AppState.get().rotate = 270;
                ranRunnable.run();

                return false;
            }
        });
        return rotateMenu;
    }

}
