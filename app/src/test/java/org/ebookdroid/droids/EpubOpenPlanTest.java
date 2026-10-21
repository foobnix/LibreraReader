package org.ebookdroid.droids;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/** The decisions an EPUB open makes before MuPDF is handed a file. */
public class EpubOpenPlanTest {

    private static final String ORIGINAL = "/books/book.epub";
    private static final String CACHE = "/cache/123.epub";

    @Test
    public void nothingIsRewrittenWithoutProcessing() {
        assertFalse(EpubOpenPlan.isRewritten(false, true));
        assertFalse(EpubOpenPlan.isRewritten(false, false));
    }

    @Test
    public void rewrittenOnlyWhenTheCacheIsThere() {
        assertTrue(EpubOpenPlan.isRewritten(true, true));
        assertFalse(EpubOpenPlan.isRewritten(true, false));
    }

    // This is the fix: a book whose rewritten copy is on disk carries the notes in its text
    // already, so the open does not wait for the footnote map to be read or extracted.
    @Test
    public void cacheHitDoesNotLoadNotesBeforeOpening() {
        assertFalse(EpubOpenPlan.needsNotesBeforeOpen(true, true, true));
    }

    @Test
    public void coldOpenLoadsNotesBeforeRewriting() {
        // the notes are written into the text while rewriting, so they have to be there first
        assertTrue(EpubOpenPlan.needsNotesBeforeOpen(true, true, false));
    }

    @Test
    public void notesAreNotLoadedWhenTheyAreNotShownInText() {
        assertFalse(EpubOpenPlan.needsNotesBeforeOpen(false, true, false));
        assertFalse(EpubOpenPlan.needsNotesBeforeOpen(false, true, true));
        assertFalse(EpubOpenPlan.needsNotesBeforeOpen(false, false, false));
    }

    @Test
    public void theOriginalIsOpenedWhenNothingIsProcessed() {
        assertEquals(ORIGINAL, EpubOpenPlan.bookPath(false, false, CACHE, ORIGINAL));
        // even a cache file left over from another setting is not opened
        assertEquals(ORIGINAL, EpubOpenPlan.bookPath(false, true, CACHE, ORIGINAL));
    }

    @Test
    public void theRewrittenCopyIsOpenedWhenItExists() {
        assertEquals(CACHE, EpubOpenPlan.bookPath(true, true, CACHE, ORIGINAL));
    }

    @Test
    public void aFailedRewriteFallsBackToTheOriginal() {
        // the rewrite produced no file; opening the missing cache path would fail the whole book
        assertEquals(ORIGINAL, EpubOpenPlan.bookPath(true, false, CACHE, ORIGINAL));
    }
}
