package com.foobnix.model;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class EpubLocatorTest {

    @Test
    public void roundTrip() {
        EpubLocator original = new EpubLocator(3, "chapter1.xhtml", "/html/body/p[2]", 1, 15);
        String json = original.toJson();
        assertNotNull(json);

        EpubLocator restored = EpubLocator.fromJson(json);
        assertNotNull(restored);
        assertEquals(original.spineIndex, restored.spineIndex);
        assertEquals(original.chapterHref, restored.chapterHref);
        assertEquals(original.domPath, restored.domPath);
        assertEquals(original.textNodeIndex, restored.textNodeIndex);
        assertEquals(original.charIndex, restored.charIndex);
    }

    @Test
    public void invalidJsonReturnsNull() {
        assertNull(EpubLocator.fromJson("garbage"));
    }

    @Test
    public void isValidFalseWhenHrefNull() {
        EpubLocator locator = new EpubLocator();
        locator.chapterHref = null;
        assertFalse(locator.isValid());
    }

    @Test
    public void isValidFalseWhenHrefEmpty() {
        EpubLocator locator = new EpubLocator();
        locator.chapterHref = "";
        assertFalse(locator.isValid());
    }

    @Test
    public void isValidTrueWhenHrefSet() {
        EpubLocator locator = new EpubLocator(0, "chap.xhtml", null, 0, 0);
        assertTrue(locator.isValid());
    }
}