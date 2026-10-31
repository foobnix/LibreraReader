package com.foobnix;

import static org.junit.Assert.assertEquals;

import com.foobnix.android.utils.TxtUtils;

import org.junit.Test;

public class TestIndexOfLetters {

    @Test
    public void punctuationTheSpeechLostDoesNotMatter() {
        assertEquals(5, TxtUtils.indexOfLetters("end. and sadly. every hour", "and sadly; every"));
    }

    @Test
    public void quotesAreSkipped() {
        assertEquals(5, TxtUtils.indexOfLetters("said Whatever may happen", "“Whatever may"));
    }

    @Test
    public void wordsCutByAHyphenAreFound() {
        assertEquals(0, TxtUtils.indexOfLetters("disin-herited and", "disinherited"));
    }

    @Test
    public void caseDoesNotMatter() {
        assertEquals(2, TxtUtils.indexOfLetters("a Sir", "sir"));
    }

    @Test
    public void wordsNotThereAreNotFound() {
        assertEquals(-1, TxtUtils.indexOfLetters("some text", "other"));
    }

    @Test
    public void nothingToLookForIsNotFound() {
        assertEquals(-1, TxtUtils.indexOfLetters("some text", "..."));
        assertEquals(-1, TxtUtils.indexOfLetters(null, "text"));
    }
}
