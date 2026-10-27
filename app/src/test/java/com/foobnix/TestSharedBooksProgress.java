package com.foobnix;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.ebookdroid.common.settings.books.SharedBooks;
import org.junit.Before;
import org.junit.Test;

/**
 * The two rules the progress store turns on, kept away from the files and the database so they
 * can be stated plainly: which device's record of a book wins, and when a book is worth
 * writing again.
 */
public class TestSharedBooksProgress {

    private static final long NOW = 1_700_000_000_000L;
    private static final long HOUR = 60L * 60 * 1000;

    @Before
    public void clearWriteHistory() {
        SharedBooks.written.clear();
    }

    @Test
    public void theNewestRecordCarriesThePosition() {
        long[] times = {NOW - 5 * HOUR, NOW - HOUR, NOW - 3 * HOUR};
        assertEquals(1, SharedBooks.newestRecord(times, NOW));
    }

    @Test
    public void nothingToChooseFromIsSaidSo() {
        assertEquals(-1, SharedBooks.newestRecord(new long[0], NOW));
    }

    @Test
    public void theOnlyRecordWinsEvenWhenItIsOld() {
        assertEquals(0, SharedBooks.newestRecord(new long[]{NOW - 400 * HOUR}, NOW));
    }

    @Test
    public void aRecordFromTheFutureDoesNotWin() {
        // A device with its clock set a year forward would otherwise hold every book for good.
        long[] times = {NOW - HOUR, NOW + 365 * 24 * HOUR};
        assertEquals(0, SharedBooks.newestRecord(times, NOW));
    }

    @Test
    public void aClockAnHourFastIsStillBelieved() {
        // Clocks disagree by minutes all the time; only the absurd is passed over.
        long[] times = {NOW - HOUR, NOW + HOUR};
        assertEquals(1, SharedBooks.newestRecord(times, NOW));
    }

    @Test
    public void everyRecordFromTheFutureLeavesNothingToChoose() {
        long[] times = {NOW + 400 * HOUR, NOW + 500 * HOUR};
        assertEquals(-1, SharedBooks.newestRecord(times, NOW));
    }

    @Test
    public void theLaterOfTwoEqualTimesWins() {
        long[] times = {NOW, NOW};
        assertEquals(1, SharedBooks.newestRecord(times, NOW));
    }

    @Test
    public void aBookIsWrittenOnceForOneContent() {
        assertTrue(SharedBooks.hasChanged("book.epub", 111));
        assertFalse(SharedBooks.hasChanged("book.epub", 111));
    }

    @Test
    public void aBookMovedOnIsWrittenAgain() {
        assertTrue(SharedBooks.hasChanged("book.epub", 111));
        assertTrue(SharedBooks.hasChanged("book.epub", 222));
        assertFalse(SharedBooks.hasChanged("book.epub", 222));
    }

    @Test
    public void oneBookDoesNotSpeakForAnother() {
        // The last written content used to be held in a single slot shared by every book, so a
        // book could be skipped because a different one had just been written.
        assertTrue(SharedBooks.hasChanged("first.epub", 111));
        assertTrue(SharedBooks.hasChanged("second.epub", 222));
        assertFalse(SharedBooks.hasChanged("first.epub", 111));
        assertTrue(SharedBooks.hasChanged("first.epub", 333));
    }

    @Test
    public void aBookReturnedToItsOldPlaceIsWrittenAgain() {
        assertTrue(SharedBooks.hasChanged("book.epub", 111));
        assertTrue(SharedBooks.hasChanged("book.epub", 222));
        assertTrue(SharedBooks.hasChanged("book.epub", 111));
    }
}
