package com.foobnix.pdf.info;

import android.content.Context;
import android.view.View;
import android.widget.PopupMenu;
import android.widget.Toast;

import com.foobnix.ext.CacheZipUtils;
import com.foobnix.model.AppBookmark;
import com.foobnix.model.AppState;
import com.foobnix.pdf.info.presentation.BookmarksAdapter;
import com.foobnix.sys.ImageExtractor;
import com.foobnix.sys.TempHolder;

import org.ebookdroid.droids.foliate.codec.FoliateContext;

/** The choice of {@link AppState#epubEngine}, the same in the settings and in an open book. */
public final class EpubEngineMenu {

    /** The engines {@link AppState#epubEngine} can be set to, in the order they are offered. */
    private static final int[] EPUB_ENGINES = {AppState.EPUB_ENGINE_MUPDF, AppState.EPUB_ENGINE_FOLIATE};

    private EpubEngineMenu() {
    }

    /** The engine's name as the preference shows it: MuPDF is the default one. */
    public static String name(final Context c, final int engine) {
        return engine == AppState.EPUB_ENGINE_FOLIATE ? "HTML+JS" : c.getString(R.string.default_label);
    }

    /**
     * A mark in a text book can be shown on its page only by foliate-js, so one tapped under
     * MuPDF says which setting it takes, as the buttons that make a mark do.
     */
    public static void hintForMark(final Context c, final AppBookmark item) {
        if (item instanceof BookmarksAdapter.Mark && Engines.IS_ENGINE_CHOICE
                && AppState.get().getEpubEngine() != AppState.EPUB_ENGINE_FOLIATE
                && FoliateContext.isWebViewSupported() && ExtUtils.isTextFomat(item.getPath())) {
            Toast.makeText(c, R.string.msg_mark_needs_foliate, Toast.LENGTH_LONG).show();
        }
    }

    /**
     * Dropping the cached outline matters as much as dropping the covers: the TOC's page numbers
     * are serialized per book, and they came from whichever engine produced them.
     *
     * @param onChanged runs when another engine was picked
     */
    public static void show(final View anchor, final Runnable onChanged) {
        final PopupMenu popupMenu = new PopupMenu(anchor.getContext(), anchor);
        for (final int engine : EPUB_ENGINES) {
            popupMenu.getMenu().add(name(anchor.getContext(), engine)).setOnMenuItemClickListener(item -> {
                if (engine == AppState.EPUB_ENGINE_FOLIATE && !FoliateContext.isAvailable()) {
                    Toast.makeText(anchor.getContext(), R.string.msg_epub_engine_unavailable, Toast.LENGTH_LONG).show();
                    return false;
                }
                if (AppState.get().epubEngine != engine) {
                    AppState.get().epubEngine = engine;
                    ImageExtractor.clearCodeDocument();
                    CacheZipUtils.removeOutlineCache();
                    TempHolder.listHash++;
                    onChanged.run();
                }
                return false;
            });
        }
        popupMenu.show();
    }
}
