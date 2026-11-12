package org.ebookdroid.droids.pdfium.codec;

import com.foobnix.android.utils.LOG;
import com.foobnix.pdf.info.model.AnnotationType;

import io.legere.pdfiumandroid.PdfPage;

/**
 * Annotations through PDFium itself (libpdfannot.so, Builder/jni/librera_pdfannot.c): listing
 * them, drawing ink, marking text, moving, restyling and erasing them - so drawing on a PDF works
 * with the PDFium engine whether MuPDF is in the build or not. pdfiumandroid saves the document.
 *
 * The calls take pdfiumandroid's own native page, which is PDFium's page itself (its document
 * pointer is not PDFium's document, so nothing here takes one); coordinates are PDF user space,
 * and the caller holds {@link com.foobnix.sys.TempHolder#lock}, under which the app makes every
 * PDFium call.
 */
public final class PdfiumAnnots {

    private static Boolean available;

    private PdfiumAnnots() {
    }

    /** Whether the library loaded and found every PDFium function it uses. */
    public static synchronized boolean isAvailable() {
        if (available == null) {
            boolean ok = false;
            try {
                System.loadLibrary("pdfannot");
                ok = init();
            } catch (Throwable e) {
                LOG.e(e);
            }
            LOG.d("PDFIUM! annotations through PDFium", ok);
            available = ok;
        }
        return available;
    }

    static long pagePointer(final PdfPage page) {
        return page.getPage$pdfiumandroid().getPagePtr();
    }

    /** PDFium's annotation subtypes, by number, as Librera names them. */
    private static final AnnotationType[] SUBTYPES = {
            AnnotationType.UNKNOWN, AnnotationType.TEXT, AnnotationType.LINK, AnnotationType.FREETEXT,
            AnnotationType.LINE, AnnotationType.SQUARE, AnnotationType.CIRCLE, AnnotationType.POLYGON,
            AnnotationType.POLYLINE, AnnotationType.HIGHLIGHT, AnnotationType.UNDERLINE, AnnotationType.SQUIGGLY,
            AnnotationType.STRIKEOUT, AnnotationType.STAMP, AnnotationType.CARET, AnnotationType.INK,
            AnnotationType.POPUP, AnnotationType.FILEATTACHMENT, AnnotationType.SOUND, AnnotationType.MOVIE,
            AnnotationType.WIDGET, AnnotationType.SCREEN, AnnotationType.PRINTERMARK, AnnotationType.TRAPNET,
            AnnotationType.WATERMARK, AnnotationType.A3D, AnnotationType.RICH_MEDIA, AnnotationType.WIDGET,
            AnnotationType.REDACT};

    static AnnotationType type(final int subtype) {
        return subtype >= 0 && subtype < SUBTYPES.length ? SUBTYPES[subtype] : AnnotationType.UNKNOWN;
    }

    /** PDFium's subtype for a text mark, or -1 for a type that is not one. */
    static int markupSubtype(final AnnotationType type) {
        switch (type) {
            case HIGHLIGHT:
                return 9;
            case UNDERLINE:
                return 10;
            case SQUIGGLY:
                return 11;
            case STRIKEOUT:
                return 12;
            default:
                return -1;
        }
    }

    private static native boolean init();

    /** Every annotation of the page as 5 floats: PDFium's subtype, then left, top, right, bottom. */
    static native float[] list(long page);

    static native String contents(long page, int index);

    /** @param xy the strokes' points as x, y pairs; @param counts the points of each stroke */
    static native boolean addInk(long page, float[] xy, int[] counts, float r, float g, float b, float width);

    /** @param quads 8 floats a quad, x1 y1 x2 y2 x3 y3 x4 y4 as PDF QuadPoints */
    static native boolean addMarkup(long page, int subtype, float[] quads, float r, float g, float b);

    static native boolean remove(long page, int index);

    static native boolean move(long page, int index, float dx, float dy);

    /** @param rgb the new colour, or null to keep it; @param width the new width, or 0 to keep it */
    static native boolean style(long page, int index, float[] rgb, float width);
}
