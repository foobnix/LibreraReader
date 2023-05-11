package com;

import static org.junit.Assert.assertEquals;

import com.foobnix.ui2.FileMetaCore;

import org.junit.Test;

public class TestYearFormat {

    @Test
    public void testDate() {
        assertEquals(2010, FileMetaCore.extractYear("2010"));
        assertEquals(2010, FileMetaCore.extractYear("03/16/2010 11:19:32 PM"));
        assertEquals(2010, FileMetaCore.extractYear("03.16.2010 11:19:32 PM"));
        assertEquals(2010, FileMetaCore.extractYear("03/16 2010 11:19:32 PM"));
        assertEquals(2010, FileMetaCore.extractYear("2010123213 11:19:32 PM"));

    }
}
