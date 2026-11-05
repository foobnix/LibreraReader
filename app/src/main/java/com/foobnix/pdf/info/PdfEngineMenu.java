package com.foobnix.pdf.info;

import android.content.Context;
import android.view.View;
import android.widget.PopupMenu;
import android.widget.Toast;

import com.foobnix.model.AppState;
import com.foobnix.sys.TempHolder;

import org.ebookdroid.droids.pdfium.codec.PdfiumContext;

/** The choice of {@link AppState#pdfEngine}, the same in the settings and in an open book. */
public final class PdfEngineMenu {

    /** The engines {@link AppState#pdfEngine} can be set to, in the order they are offered. */
    private static final int[] PDF_ENGINES = {AppState.PDF_ENGINE_MUPDF, AppState.PDF_ENGINE_PDFIUM};

    private PdfEngineMenu() {
    }

    /** The engine's name as the preference shows it: MuPDF is the default one, as for EPUB. */
    public static String name(final Context c, final int engine) {
        return engine == AppState.PDF_ENGINE_PDFIUM ? "PDFium" : c.getString(R.string.default_label);
    }

    /**
     * The choice is read when a document is opened, so the books and the covers already on
     * screen keep the engine they were opened with: a cover's url names its engine, see
     * IMG.toUrl, so the library redraws them with the new one.
     *
     * @param onChanged runs when another engine was picked. The document already open is left to
     *                  it to let go of: a reader writes the strokes drawn on it first.
     */
    public static void show(final View anchor, final Runnable onChanged) {
        final PopupMenu popupMenu = new PopupMenu(anchor.getContext(), anchor);
        for (final int engine : PDF_ENGINES) {
            popupMenu.getMenu().add(name(anchor.getContext(), engine)).setOnMenuItemClickListener(item -> {
                if (engine == AppState.PDF_ENGINE_PDFIUM && !PdfiumContext.isAvailable()) {
                    Toast.makeText(anchor.getContext(), R.string.msg_pdf_engine_unavailable, Toast.LENGTH_LONG).show();
                    return false;
                }
                if (AppState.get().pdfEngine != engine) {
                    AppState.get().pdfEngine = engine;
                    TempHolder.listHash++;
                    onChanged.run();
                }
                return false;
            });
        }
        popupMenu.show();
    }
}
