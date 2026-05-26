package com.foobnix.model;

import org.junit.Test;

import static org.junit.Assert.*;

public class ParagraphConfigTest {

    @Test
    public void roundTrip() {
        ParagraphConfig original = new ParagraphConfig();
        original.paragraphHeight = 2;
        original.paragraphGapRules = "1=0\n2=0.8\n3=1.4\n4+=2.0";
        original.lineMdRecognition = false;
        original.enabled = true;

        String json = original.toJson();
        assertNotNull(json);

        ParagraphConfig restored = ParagraphConfig.fromJson(json);
        assertNotNull(restored);
        assertEquals(2, restored.paragraphHeight);
        assertEquals("1=0\n2=0.8\n3=1.4\n4+=2.0", restored.paragraphGapRules);
        assertFalse(restored.lineMdRecognition);
        assertTrue(restored.enabled);
    }

    @Test
    public void defaultConfigHasExpectedValues() {
        ParagraphConfig defaults = ParagraphConfig.defaultConfig();
        assertNotNull(defaults);
        assertEquals(1, defaults.paragraphHeight);
        assertNull(defaults.paragraphGapRules);
        assertTrue(defaults.lineMdRecognition);
        assertFalse(defaults.enabled);
    }

    @Test
    public void fromJsonInvalidReturnsNull() {
        assertNull(ParagraphConfig.fromJson(null));
        assertNull(ParagraphConfig.fromJson(""));
        assertNull(ParagraphConfig.fromJson("not valid json!!!"));
    }
}