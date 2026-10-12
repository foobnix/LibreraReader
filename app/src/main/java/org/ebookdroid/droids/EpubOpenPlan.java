package org.ebookdroid.droids;

/**
 * What has to happen before an EPUB is handed to MuPDF. Text replacement, hyphens, reference
 * mode, inline footnotes and BBCode are all applied by rewriting the book into a cache file once;
 * every later open reads that rewritten copy, which already carries the result in its text.
 * <p>
 * Kept apart from {@link EpubContext} so the decisions can be read, and tested, on their own.
 */
final class EpubOpenPlan {

    private EpubOpenPlan() {
    }

    /** The rewritten book is on disk: nothing has to be extracted or rewritten for this open. */
    static boolean isRewritten(boolean process, boolean cacheExists) {
        return process && cacheExists;
    }

    /**
     * Whether the footnote map has to be read before MuPDF opens the book. It only has to be
     * when the notes are about to be written into the text, which happens while rewriting: a
     * rewritten book carries them already, so the map can be loaded afterwards, off the hot path.
     */
    static boolean needsNotesBeforeOpen(boolean footNotesInText, boolean process, boolean cacheExists) {
        return footNotesInText && !isRewritten(process, cacheExists);
    }

    /**
     * The book MuPDF opens: the rewritten copy when there is one, the original otherwise. A
     * rewrite that failed leaves no cache file, and the original book is still readable.
     */
    static String bookPath(boolean process, boolean cacheIsFile, String cachePath, String fileName) {
        return process && cacheIsFile ? cachePath : fileName;
    }
}
