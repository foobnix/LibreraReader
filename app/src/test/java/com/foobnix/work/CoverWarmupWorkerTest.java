package com.foobnix.work;

import com.foobnix.dao2.FileMeta;
import com.foobnix.ui2.FileMetaCore;
import org.junit.Test;
import java.util.Collections;
import static org.junit.Assert.*;

public class CoverWarmupWorkerTest {
    private FileMeta book(int state) {
        FileMeta book = new FileMeta("content://generic-provider/document/book");
        book.setIsSearchBook(true); book.setState(state); book.setSize(100L); book.setDate(200L); return book;
    }
    @Test public void genericSafCoverWarmsWithoutWaitingForFirstVisibleRow() {
        assertEquals(1, CoverWarmupWorker.booksToWarm(Collections.singleton(book(FileMetaCore.STATE_FULL)), true, path -> false).size());
    }
    @Test public void incompleteGenericSafBookWaitsForMetadataRatherThanDownloadingAgain() {
        assertTrue(CoverWarmupWorker.booksToWarm(Collections.singleton(book(FileMetaCore.STATE_BASIC)), true, path -> false).isEmpty());
    }
    @Test public void calibreSidecarCanWarmCoverBeforeBookExtractionFinishes() {
        assertEquals(1, CoverWarmupWorker.booksToWarm(Collections.singleton(book(FileMetaCore.STATE_BASIC)), true, path -> true).size());
    }
    @Test public void disabledImagesDoNotEvenLookUpSidecars() {
        assertTrue(CoverWarmupWorker.booksToWarm(Collections.singleton(book(FileMetaCore.STATE_FULL)), false,
                path -> { fail("Images disabled but sidecar queried"); return true; }).isEmpty());
    }
    @Test public void recentsOutsideLibraryAndRemovedRowsAreNotWarmed() {
        FileMeta book = book(FileMetaCore.STATE_FULL); book.setIsSearchBook(false);
        assertTrue(CoverWarmupWorker.booksToWarm(Collections.singleton(book), true, path -> true).isEmpty());
    }
    @Test public void queuedRequestRetainsRevisionEvenWhenSharedDaoRowChanges() {
        FileMeta book = book(FileMetaCore.STATE_FULL);
        FileMeta queued = CoverWarmupWorker.booksToWarm(Collections.singleton(book), true, path -> false).get(0);
        book.setDate(300L); book.setSize(400L);
        assertNotSame(book, queued); assertEquals(book.getPath(), queued.getPath());
        assertEquals(Long.valueOf(200), queued.getDate()); assertEquals(Long.valueOf(100), queued.getSize());
    }
}
