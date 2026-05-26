package com.foobnix.model;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class TxtLocatorTest {

    @Test
    public void roundTrip() {
        TxtLocator original = new TxtLocator(1024L, "UTF-8");
        String json = original.toJson();

        TxtLocator restored = TxtLocator.fromJson(json);
        assertEquals(original.byteOffset, restored.byteOffset);
        assertEquals(original.encoding, restored.encoding);
    }

    @Test
    public void invalidJsonReturnsNull() {
        assertNull(TxtLocator.fromJson(null));
        assertNull(TxtLocator.fromJson(""));
        assertNull(TxtLocator.fromJson("not valid json"));
    }

    @Test
    public void isValidFalseWhenNegativeOffset() {
        TxtLocator locator = new TxtLocator(-1L, "UTF-8");
        assertFalse(locator.isValid());
    }

    @Test
    public void isValidFalseWhenEncodingNull() {
        TxtLocator locator = new TxtLocator(0L, null);
        assertFalse(locator.isValid());

        TxtLocator emptyEncoding = new TxtLocator(0L, "");
        assertFalse(emptyEncoding.isValid());
    }

    @Test
    public void isValidTrueWhenValid() {
        TxtLocator locator = new TxtLocator(0L, "UTF-8");
        assertTrue(locator.isValid());
    }
}