package com.foobnix.pdf.info;

import android.content.Context;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.PopupMenu;

import com.foobnix.model.AppState;

/** The choice of {@link AppState#annotationsSaveMode}, the same in the settings and in an open book. */
public final class AnnotationsSaveMenu {

    /** The choices, in the order they are offered. */
    private static final int[] MODES = {AppState.ANNOTATIONS_DONT_SAVE, AppState.ANNOTATIONS_ASK, AppState.ANNOTATIONS_SAVE_AUTO};

    private AnnotationsSaveMenu() {
    }

    public static String name(final Context c, final int mode) {
        if (mode == AppState.ANNOTATIONS_DONT_SAVE) {
            return c.getString(R.string.dont_save);
        }
        if (mode == AppState.ANNOTATIONS_SAVE_AUTO) {
            return c.getString(R.string.save_automatically);
        }
        return c.getString(R.string.ask_to_save);
    }

    /** @param onChanged runs when another choice was picked, to show it. */
    public static void show(final View anchor, final Runnable onChanged) {
        final PopupMenu popupMenu = new PopupMenu(anchor.getContext(), anchor);
        final Menu menu = popupMenu.getMenu();
        for (final int mode : MODES) {
            final MenuItem item = menu.add(0, mode, Menu.NONE, name(anchor.getContext(), mode));
            item.setOnMenuItemClickListener(it -> {
                if (AppState.get().annotationsSaveMode != mode) {
                    AppState.get().annotationsSaveMode = mode;
                    AppState.get().save(anchor.getContext());
                    onChanged.run();
                }
                return false;
            });
        }
        popupMenu.show();
    }
}
