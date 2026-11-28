package com.foobnix.pdf.search.menu;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.view.MenuItem;
import android.view.MenuItem.OnMenuItemClickListener;
import android.view.View;
import android.widget.PopupMenu;

import androidx.appcompat.widget.TooltipCompat;

import com.foobnix.android.utils.Dips;
import com.foobnix.model.AppSP;
import com.foobnix.model.AppState;
import com.foobnix.pdf.info.ExtUtils;
import com.foobnix.pdf.info.R;
import com.foobnix.pdf.info.view.MyPopupMenu;
import com.foobnix.pdf.info.wrapper.DocumentController;

@SuppressLint("NewApi")
public class MenuBuilderM {

    /** How the pages of the book are laid out on the screen. */
    public static final int LAYOUT_HALF = 0;
    public static final int LAYOUT_ONE = 1;
    public static final int LAYOUT_TWO = 2;
    public static final int LAYOUT_COVER_TWO = 3;

    private static final int[] LAYOUT_ICONS = {
            R.drawable.my_glyphicons_page_split,
            R.drawable.my_glyphicons_two_page_one,
            R.drawable.my_glyphicons_two_pages_12,
            R.drawable.my_glyphicons_two_pages_23};
    private static final int[] LAYOUT_NAMES = {
            R.string.half_page,
            R.string.one_page,
            R.string.two_pages,
            R.string.two_pages_cover};

    public interface OnLayout {
        void onLayout(int layout);
    }

    public static int layoutIcon(int layout) {
        return LAYOUT_ICONS[layout];
    }

    /** What the pages are laid out as, in words. */
    public static String layoutName(Context c, int layout) {
        return c.getString(LAYOUT_NAMES[layout]);
    }

    /** The layout the book is in now. */
    public static int currentLayout() {
        final AppSP sp = AppSP.get();
        if (sp.isCut) {
            return LAYOUT_HALF;
        }
        if (!sp.isDouble) {
            return LAYOUT_ONE;
        }
        return sp.isDoubleCoverAlone ? LAYOUT_COVER_TWO : LAYOUT_TWO;
    }

    /**
     * The layouts to choose from, in the order given, on one row under their name. One of them
     * alone is no choice: then the menu shows none.
     */
    public static void addPageLayouts(final MyPopupMenu p, final int[] layouts, final OnLayout onLayout) {
        addPageLayouts(p, layouts, currentLayout(), onLayout);
    }

    /** As above, {@code current} the one shown taken. */
    public static void addPageLayouts(final MyPopupMenu p, final int[] layouts, final int current, final OnLayout onLayout) {
        if (layouts.length < 2) {
            return;
        }
        final Context c = p.getContext();
        final int[] icons = new int[layouts.length];
        final String[] names = new String[layouts.length];
        int selected = -1;
        for (int i = 0; i < layouts.length; i++) {
            icons[i] = LAYOUT_ICONS[layouts[i]];
            names[i] = c.getString(LAYOUT_NAMES[layouts[i]]);
            if (layouts[i] == current) {
                selected = i;
            }
        }
        // Two pages only show in the paged view: elsewhere the book is in one
        for (int i = 0; selected == -1 && i < layouts.length; i++) {
            if (layouts[i] == LAYOUT_ONE) {
                selected = i;
            }
        }
        p.getMenu().add(R.string.pages_layout).asLabel();
        p.getMenu()
         .setChoiceIcons(selected, i -> onLayout.onLayout(layouts[i]), icons)
         .setChoiceDescriptions(names);
    }

    /** The angle the page is turned by, on one row under its name. */
    /**
     * The view the book is read in: vertical, paged or musician's (not on e-ink, as the open
     * dialog has it).
     */
    public static void addViewModes(final View anchor, final MyPopupMenu p, final DocumentController dc) {
        final java.util.List<Integer> modes = new java.util.ArrayList<>();
        final java.util.List<String> names = new java.util.ArrayList<>();
        modes.add(AppState.READING_MODE_SCROLL);
        names.add(AppState.get().nameVerticalMode);
        modes.add(AppState.READING_MODE_BOOK);
        names.add(AppState.get().nameHorizontalMode);
        if (!Dips.isEInk()) {
            // Short in a row of choices, unless the reader named the mode themselves
            final String musician = AppState.get().nameMusicianMode;
            modes.add(AppState.READING_MODE_MUSICIAN);
            names.add(musician == null || musician.isEmpty()
                    || musician.equals(anchor.getContext().getString(R.string.mode_musician))
                    ? anchor.getContext().getString(R.string.mode_musician_short) : musician);
        }

        final int current = modes.indexOf(AppSP.get().readingMode);
        // The names say what they are: no title over them
        p.getMenu().setChoices(current, i -> {
            if (i == current) {
                return;
            }
            anchor.post(() -> ExtUtils.switchReadingMode(dc.getActivity(), dc, modes.get(i)));
        }, names.toArray(new String[0]));
    }

    public static void addRotateChoices(final MyPopupMenu p, final Runnable onRotate) {
        final int[] angles = {0, 90, 180, 270};
        int selected = -1;
        for (int i = 0; i < angles.length; i++) {
            if (angles[i] == AppState.get().rotate) {
                selected = i;
            }
        }
        p.getMenu().add(R.string.rotate).asLabel();
        // A page turned that far, its number in it; none taken is upright
        p.getMenu().setChoiceIcons(selected >= 0 ? selected : 0, i -> {
            AppState.get().rotate = angles[i];
            onRotate.run();
        }, R.drawable.page_rotate_0, R.drawable.page_rotate_90, R.drawable.page_rotate_180, R.drawable.page_rotate_270)
         .setChoiceDescriptions("0\u00B0", "90\u00B0", "180\u00B0", "270\u00B0");
    }

    /**
     * How the paged view goes from page to page, every effect on the rows under its name. The
     * vertical view has no pages to go to: there it shows none, and an effect goes over to the
     * paged view.
     */
    public static void addPageEffects(final View anchor, final MyPopupMenu p, final DocumentController dc) {
        final Context c = anchor.getContext();
        final boolean paged = AppSP.get().readingMode == AppState.READING_MODE_BOOK;
        final String[] names = new String[5];
        for (int i = 0; i < names.length; i++) {
            names[i] = PageEffect.name(c, i);
        }
        p.getMenu().add(R.string.page_effect).asLabel();
        p.getMenu()
         .setChoiceIcons(paged ? PageEffect.get() : PageEffect.NONE, effect -> {
             if (!paged) {
                 if (effect != PageEffect.NONE) {
                     PageEffect.set(effect);
                     anchor.post(() -> ExtUtils.switchReadingMode(dc.getActivity(), dc));
                 }
             } else {
                 final boolean remake = PageEffect.set(effect);
                 // The page button says the effect with the pages
                 final String text = layoutName(c, currentLayout()) + ", " + PageEffect.name(c, PageEffect.get());
                 anchor.setContentDescription(text);
                 TooltipCompat.setTooltipText(anchor, text);
                 if (remake) {
                     // The pager is made for one way of going: another way needs it made again
                     anchor.post(dc::restartActivity);
                 }
             }
         }, PageEffect.ICONS)
         .setChoiceDescriptions(names);
    }

    public static PopupMenu bookMenu(final View view, final Activity a, final String pageUrl, final Runnable onReloadDocument) {
        final PopupMenu popupMenu = new PopupMenu(view.getContext(), view);

        addOrientationMenu(view, a, popupMenu);

//        final MenuItem fullscren = popupMenu.getMenu().add(R.string.full_screen);
//        fullscren.setCheckable(true);
//        fullscren.setChecked(AppState.get().fullScreen);
//        fullscren.setOnMenuItemClickListener(new OnMenuItemClickListener() {
//
//            @Override
//            public boolean onMenuItemClick(final MenuItem item) {
//                AppState.get().isFullScreen = !AppState.get().isFullScreen;
//                fullscren.setChecked(AppState.get().isFullScreen);
//                DocumentController.chooseFullScreen(a, AppState.get().isFullScreen);
//                return false;
//            }
//        });

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
        crop.setChecked(AppSP.get().isCrop);
        crop.setOnMenuItemClickListener(new OnMenuItemClickListener() {

            @Override
            public boolean onMenuItemClick(final MenuItem item) {
                AppSP.get().isCrop = !AppSP.get().isCrop;
                crop.setChecked(AppSP.get().isCrop);
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

        final MenuItem r1 = rotateMenu.getMenu().add(R.string.default_label);
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
