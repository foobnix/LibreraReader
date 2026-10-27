package org.ebookdroid.droids.pdfium.codec;

import android.graphics.Bitmap;

import com.foobnix.android.utils.LOG;
import com.foobnix.pdf.info.AppsConfig;

import org.ebookdroid.core.codec.AbstractCodecContext;
import org.ebookdroid.core.codec.CodecDocument;

import io.legere.pdfiumandroid.PdfiumCore;
import io.legere.pdfiumandroid.api.AlreadyClosedBehavior;
import io.legere.pdfiumandroid.api.Config;
import io.legere.pdfiumandroid.api.ConfigKt;
import io.legere.pdfiumandroid.api.DefaultLogger;

/**
 * PDF documents rendered by PDFium (PdfiumAndroidKt) instead of the bundled MuPDF.
 *
 * It is the second engine behind {@link org.ebookdroid.droids.mupdf.codec.PdfContext}: the app sees
 * the same {@link org.ebookdroid.core.codec.CodecDocument} / {@link org.ebookdroid.core.codec.CodecPage}
 * contract, only the pixels, the text layer and the outline come from PDFium.
 * {@link org.ebookdroid.BookType#getCodecContextByPath(String)} picks between the two from
 * {@link com.foobnix.model.AppState#pdfEngine}.
 */
public class PdfiumContext extends AbstractCodecContext {

    /** PDFium keeps its state in one native library, so one core is enough for the whole app. */
    private static volatile PdfiumCore core;

    /** Set when the native libraries could not be loaded: the app then stays on MuPDF. */
    private static volatile boolean unavailable;

    /**
     * @return the shared core, or <code>null</code> when the native libraries are missing
     */
    public static PdfiumCore core() {
        if (unavailable) {
            return null;
        }
        PdfiumCore local = core;
        if (local != null) {
            return local;
        }
        synchronized (PdfiumContext.class) {
            if (core == null && !unavailable) {
                try {
                    // The constructor blocks until libpdfium/libpdfiumandroid are loaded and
                    // throws when they are not there (a flavour built without the .so files).
                    core = new PdfiumCore();
                    // A page or document used after it was closed logs instead of throwing: the
                    // decode threads here race with recycle() the same way they do with MuPDF.
                    ConfigKt.setPdfiumConfig(
                            new Config(new DefaultLogger(), AlreadyClosedBehavior.LOG, PAGE_RETENTION));
                    LOG.d("PDFIUM! core created");
                } catch (Throwable e) {
                    unavailable = true;
                    LOG.e(e);
                }
            }
            return core;
        }
    }

    /**
     * How many pages PDFium keeps open after Librera released them. Librera renders a page in
     * tiles and reopens the same page for its text and links right after, so retaining a few
     * makes those reopens free.
     */
    private static final int PAGE_RETENTION = 8;

    /**
     * @return <code>true</code> when PDFium can be used on this device
     */
    public static boolean isAvailable() {
        return core() != null;
    }

    @Override
    public CodecDocument openDocumentInner(String fileName, final String password) {
        return PdfiumDocument.open(this, fileName, password);
    }

    @Override
    public Bitmap.Config getBitmapConfig() {
        return AppsConfig.CURRENT_BITMAP_ARGB;
    }

    @Override
    public boolean isParallelPageAccessAvailable() {
        // PDFium serializes every call on a lock of its own, like MuPDF does.
        return false;
    }
}
