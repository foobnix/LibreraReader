package com.foobnix.ext;

import org.junit.Test;
import static org.junit.Assert.*;

public class ConversionCacheKeyTest {
    @Test public void collidingLegacyNamesHaveDistinctReleaseScopedKeys() {
        String first = "/Books/Aa.epub|length=100|modified=200";
        String second = "/Books/BB.epub|length=100|modified=200";
        assertEquals(first.hashCode(), second.hashCode());
        assertNotEquals(ConversionCache.key(first, 42), ConversionCache.key(second, 42));
        assertEquals(64, ConversionCache.key(first, 42).length());
        assertEquals(ConversionCache.key(first, 42), ConversionCache.key(first, 42));
    }
    @Test public void upgradingTheReleaseInvalidatesPreviouslyConvertedContent() {
        assertNotEquals(ConversionCache.key("book|settings", 42), ConversionCache.key("book|settings", 43));
        assertNotEquals(ConversionCache.key("book|settings", 42), ConversionCache.key("book|changed-settings", 42));
    }
}
