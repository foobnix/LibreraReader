package com.foobnix.ext;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;

public class ResolvePathTest {

    private static final String BASE = "OEBPS/Text/ch1.xhtml";

    @Test
    public void sameFolder() {
        assertEquals("OEBPS/Text/notes.xhtml", EpubExtractor.resolvePath(BASE, "notes.xhtml"));
        assertEquals("OEBPS/Text/notes.xhtml", EpubExtractor.resolvePath(BASE, "./notes.xhtml"));
    }

    @Test
    public void otherFolders() {
        assertEquals("OEBPS/Notes/notes.xhtml", EpubExtractor.resolvePath(BASE, "../Notes/notes.xhtml"));
        assertEquals("OEBPS/Text/notes.xhtml", EpubExtractor.resolvePath(BASE, "a/../notes.xhtml"));
        assertEquals("notes.xhtml", EpubExtractor.resolvePath(BASE, "../../../notes.xhtml"));
        assertEquals("OEBPS/Text/sub/notes.xhtml", EpubExtractor.resolvePath(BASE, "sub//notes.xhtml"));
    }

    @Test
    public void fileInRoot() {
        assertEquals("notes.xhtml", EpubExtractor.resolvePath("ch1.xhtml", "notes.xhtml"));
    }

    @Test
    public void absolutePath() {
        assertEquals("OEBPS/notes.xhtml", EpubExtractor.resolvePath(BASE, "/OEBPS/notes.xhtml"));
    }

    @Test
    public void encodedAndQuery() {
        assertEquals("OEBPS/Text/my notes.xhtml", EpubExtractor.resolvePath(BASE, "my%20notes.xhtml"));
        assertEquals("OEBPS/Text/a+b.xhtml", EpubExtractor.resolvePath(BASE, "a+b.xhtml"));
        assertEquals("OEBPS/Text/notes.xhtml", EpubExtractor.resolvePath(BASE, "notes.xhtml?v=2"));
        assertEquals("OEBPS/Text/100%.xhtml", EpubExtractor.resolvePath(BASE, "100%.xhtml"));
    }

    @Test
    public void externalLinks() {
        assertNull(EpubExtractor.resolvePath(BASE, "http://example.com/notes.xhtml"));
        assertNull(EpubExtractor.resolvePath(BASE, "mailto:someone@example.com"));
    }
}
