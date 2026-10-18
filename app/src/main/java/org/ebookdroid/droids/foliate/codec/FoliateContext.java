package org.ebookdroid.droids.foliate.codec;

import android.graphics.Bitmap;

import com.foobnix.pdf.info.AppsConfig;
import com.foobnix.LibreraApp;

import org.ebookdroid.core.codec.AbstractCodecContext;
import org.ebookdroid.core.codec.CodecDocument;

/**
 * Reflowable books laid out by foliate-js in a WebView instead of by MuPDF.
 *
 * It is the second engine for every format Librera converts to an {@code .epub}. The book itself is
 * prepared by the format context exactly as before - see
 * {@link org.ebookdroid.droids.mupdf.codec.PdfContext#openReflowable(String, String)}, which is
 * where the two engines part ways - so the conversion, the hyphenation, the inlined footnotes and
 * the cache keys are all shared.
 */
public class FoliateContext extends AbstractCodecContext {

    /**
     * Whether this engine can be used right now.
     *
     * It needs an activity on screen: the WebView it paginates in has to be attached to a window
     * or Chromium never navigates the iframe the book is laid out in (see
     * {@link FoliateRenderer}). While a book is being read there always is one. Away from the
     * reader - the library's own covers, a background scan - there is not, and the book falls back
     * to MuPDF, which is no loss: a reflowable book's cover is read out of the archive rather than
     * rendered (see {@code ImageExtractor.proccessCoverPage}).
     */
    public static boolean isAvailable() {
        return LibreraApp.context != null && LibreraApp.getResumedActivity() != null;
    }

    @Override
    public CodecDocument openDocumentInner(final String fileName, final String password) {
        final FoliateDocument document = new FoliateDocument(this, fileName);
        if (document.openEngine()) {
            return document;
        }
        document.disposeEngine();
        return null;
    }

    @Override
    public Bitmap.Config getBitmapConfig() {
        return AppsConfig.CURRENT_BITMAP_ARGB;
    }

    @Override
    public boolean isParallelPageAccessAvailable() {
        // One WebView, one iframe: every call is serialized inside FoliateRenderer
        return false;
    }
}
