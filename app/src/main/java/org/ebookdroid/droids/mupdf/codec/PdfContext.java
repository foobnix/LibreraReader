package org.ebookdroid.droids.mupdf.codec;

import com.foobnix.android.utils.LOG;
import com.foobnix.ext.HtmlToEpub;
import com.foobnix.model.AppState;
import com.foobnix.pdf.info.Engines;

import org.ebookdroid.core.codec.AbstractCodecDocument;
import org.ebookdroid.core.codec.CodecDocument;
import org.ebookdroid.droids.foliate.codec.FoliateContext;
import org.ebookdroid.droids.foliate.codec.FoliateDocument;

import java.util.Locale;

public class PdfContext extends MuPdfContext {

    @Override
    public CodecDocument openDocumentInner(String fileName, final String password) {
        MuPdfDocument muPdfDocument = new MuPdfDocument(this, MuPdfDocument.FORMAT_PDF, fileName, password);
        return muPdfDocument;
    }

    /**
     * Opens a reflowable book with whichever engine {@link AppState#epubEngine} asks for.
     *
     * Every text format - epub, fb2, mobi, docx, odt, md, rtf - is converted to an {@code .epub}
     * by its own context and then opened here, so this one call is where the two engines part
     * ways and nothing about the conversion, the hyphenation, the inlined footnotes or the cache
     * keys has to know which one was picked.
     *
     * @param path the file the context prepared, which is an {@code .epub} for most formats but
     *             the original {@code .html} or {@code .txt} for a couple of them - and those stay
     *             on MuPDF, since foliate is only given books it can read
     */
    protected AbstractCodecDocument openReflowable(final String path, final String password) {
        if (isFoliateWanted(path)) {
            final FoliateDocument document = new FoliateDocument(this, path);
            if (document.openEngine()) {
                return document;
            }
            // The book is still intact and the context untouched, so MuPDF can have it instead
            document.disposeEngine();
            LOG.d("openReflowable foliate failed, falling back to MuPDF", path);
        }
        if (!Engines.IS_MUPDF) {
            // Nothing to fall back to. foliate reads an .epub, so the formats that are converted
            // to one open; the ones handed to this engine as .html or .txt do not, and the reader
            // reports them as files it cannot open rather than dying on a missing native symbol.
            LOG.d("openReflowable has no engine for this book, MuPDF is not in this build", path);
            return null;
        }
        return new MuPdfDocument(this, MuPdfDocument.FORMAT_PDF, path, password);
    }

    /**
     * Opens a reflowable book that its context prepared as a loose {@code .html} rather than an
     * {@code .epub} - {@code .docx}, {@code .odt}, {@code .doc}, {@code .rtf}, {@code .mht},
     * {@code .txt}, {@code .md} and {@code .html} itself.
     *
     * Where MuPDF is in the build it opens such a file as it always has. Where it is not, foliate
     * is the only engine and it reads a book out of an archive, so the file is wrapped in a
     * one-chapter EPUB first - see {@link HtmlToEpub}, which also brings along the images and the
     * stylesheet the page names. The conversions themselves are untouched either way.
     *
     * @param htmlPath the generated page, with its resources beside it in the cache folder
     */
    protected AbstractCodecDocument openReflowableHtml(final String htmlPath, final String password) {
        if (Engines.IS_MUPDF || !isHtml(htmlPath)) {
            // A context that prepared an .epub after all - MdContext does, for a SUMMARY.md - needs
            // none of this and goes straight to the engines
            return openReflowable(htmlPath, password);
        }
        final String epub = HtmlToEpub.wrap(htmlPath);
        // A failed wrap leaves openReflowable to report the book as one it cannot open, which is
        // what it would have done with the page itself
        return openReflowable(epub != null ? epub : htmlPath, password);
    }

    private static boolean isHtml(final String path) {
        if (path == null) {
            return false;
        }
        final String lower = path.toLowerCase(Locale.US);
        return lower.endsWith(".html") || lower.endsWith(".htm") || lower.endsWith(".xhtml");
    }

    /** Whether foliate should lay this book out: it is asked for, it is an epub, and it can run. */
    private static boolean isFoliateWanted(final String path) {
        if (path == null || AppState.get().getEpubEngine() != AppState.EPUB_ENGINE_FOLIATE) {
            return false;
        }
        if (!path.toLowerCase(Locale.US).endsWith(".epub")) {
            return false;
        }
        if (!FoliateContext.isAvailable()) {
            LOG.d("openReflowable foliate is not available, staying on MuPDF");
            return false;
        }
        return true;
    }
}
