package com.foobnix.ext;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;

/** A book's date of publication is read whole, and Calibre's "no date" is no date. */
public class PubDateTest {

    @Test
    public void datesAreReadWhole() {
        assertEquals("2004-03-15", PubDate.dateOf("2004-03-15T04:00:00+00:00"));
        assertEquals("2004-03-15", PubDate.dateOf("D:20040315120000Z"));
        assertEquals("2004-03", PubDate.dateOf("2004-03"));
        assertEquals("2004", PubDate.dateOf("2004"));
        assertEquals("2004", PubDate.dateOf("2004-13-40"));
        assertEquals(2004, PubDate.yearOf("2004-03-15"));
        assertEquals(2004, PubDate.yearOf("D:20040315120000Z"));
    }

    @Test
    public void calibresUndefinedDateIsNoDate() {
        assertNull(PubDate.dateOf("0101-01-01T00:00:00+00:00"));
        assertNull(PubDate.dateOf("0100-12-31T21:00:00+00:00"));
        assertEquals(-1, PubDate.yearOf("0101-01-01T00:00:00+00:00"));
        assertEquals(-1, PubDate.yearOf("0100-12-31T21:00:00+00:00"));
        assertEquals(-1, PubDate.yearOf("0101"));
        assertEquals(-1, PubDate.yearOf(null));
    }

    @Test
    public void oneStringGivesTheWholeDateItHas() {
        assertEquals("2004-03-15", PubDate.published("2004-03-15T04:00:00+00:00"));
        assertEquals("2004-03-15", PubDate.published("D:20040315120000Z"));
        assertEquals("2004-03", PubDate.published("2004-03"));
        assertEquals("2004", PubDate.published("2004"));
        assertEquals("2010", PubDate.published("03/16/2010 11:19:32 PM"));
        assertNull(PubDate.published((String) null));
        assertNull(PubDate.published("0101-01-01T00:00:00+00:00"));
    }

    /** The whole date loses nothing the year alone was read out of the same string. */
    @Test
    public void theYearOfTheWholeDateIsTheYearTheStringGives() {
        for (String s : new String[]{"2010", "03/16/2010 11:19:32 PM", "03.16.2010 11:19:32 PM",
                                     "03/16 1010 2010 11:19:32 PM", "03/16 1985 1010 11:19:32 PM",
                                     "2010123213 11:19:32 PM", "122010123213 11:19:32 PM",
                                     "D:20040315120000Z", "2004-03-15T04:00:00+00:00"}) {
            assertEquals(s, PubDate.yearOf(s), PubDate.yearOf(PubDate.published(s)));
        }
    }

    @Test
    public void theEarliestDateThatIsNotAModificationIsTheOne() {
        assertEquals("1999-05-01", PubDate.published(Arrays.asList(
                new String[]{"modification", "1990-01-01"},
                new String[]{null, "2010-02-02"},
                new String[]{"original-publication", "1999-05-01"},
                new String[]{null, "0101-01-01T00:00:00+00:00"})));
        assertNull(PubDate.published(Collections.singletonList(
                new String[]{null, "0101-01-01T00:00:00+00:00"})));
    }
}
