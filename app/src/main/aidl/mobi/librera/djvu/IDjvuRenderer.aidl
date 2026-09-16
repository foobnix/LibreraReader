/*
 * DjVu rendering over Binder: Librera PRO opens and draws DjVu documents with its djvulibre
 * for another app (LibreraX), which keeps no DjVu code of its own.
 *
 * This file is the whole protocol, and it is the same file on both sides: the package, the
 * interface name and the transaction numbers below must never change. A new call gets the next
 * number and [getApiVersion] goes up by one; an old call is never removed or renumbered.
 *
 * Documents are handed over as file descriptors, read-only, never as paths: the service reads
 * only what the client could read itself. A document handle is only good for the app that
 * opened it.
 *
 * Page coordinates are fractions of the page, 0..1, with the origin at the top left.
 */
package mobi.librera.djvu;

interface IDjvuRenderer {

    /** 1 for the calls below. */
    int getApiVersion() = 1;

    /**
     * Opens a document, reading it from [fd]; the service keeps its own copy of the descriptor
     * until [close]. Returns a handle for the calls below, or throws IllegalStateException
     * when the document cannot be read.
     */
    long open(in ParcelFileDescriptor fd) = 2;

    /** Frees a document and everything decoded for it. An unknown handle is ignored. */
    void close(long doc) = 3;

    int getPageCount(long doc) = 4;

    /** {width, height, dpi} of a page, in pixels of the page at its own resolution. */
    int[] getPageSize(long doc, int page) = 5;

    /**
     * Draws the part [left, top, right, bottom] of a page into a picture [width] x [height] and
     * returns the read end of a pipe the pixels come down: width * height * 4 bytes, row by row
     * from the top, each pixel R, G, B, A — the byte order Bitmap.copyPixelsFromBuffer takes for
     * ARGB_8888. The page is drawn in its own colours, opaque.
     */
    ParcelFileDescriptor render(long doc, int page, int width, int height,
            float left, float top, float right, float bottom) = 6;

    /**
     * The words of a page's text layer, in reading order: "words" a String[], and "rects" a
     * float[] of four numbers a word, left, top, right, bottom. Empty arrays for a page with no
     * text layer.
     */
    Bundle getPageText(long doc, int page) = 7;

    /**
     * The table of contents, flattened: "titles" a String[], "levels" an int[] (0 at the top),
     * and "pages" an int[] of 0-based page numbers, -1 where an entry points nowhere in the
     * document.
     */
    Bundle getOutline(long doc) = 8;

    /** "title" and "author" as the document gives them, either missing where it does not. */
    Bundle getMeta(long doc) = 9;
}
