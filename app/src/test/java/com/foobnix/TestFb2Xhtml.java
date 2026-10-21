package com.foobnix;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.foobnix.ext.Fb2Xhtml;

import org.junit.Test;

/**
 * The skeleton {@link Fb2Xhtml} puts around FictionBook markup, which is what gives the document
 * a body for foliate's paginator to measure.
 */
public class TestFb2Xhtml {

    /** The whole book through the transform, one line at a time, as the extractor feeds it. */
    private static String run(final String... lines) {
        final Fb2Xhtml xhtml = new Fb2Xhtml();
        final StringBuilder out = new StringBuilder();
        for (final String line : lines) {
            out.append(xhtml.apply(line));
        }
        return out.toString();
    }

    @Test public void rootBecomesAnHtmlDocument() {
        final String out = run("<FictionBook xmlns=\"http://www.gribuser.ru/xml/fictionbook/2.0\""
                + " xmlns:l=\"http://www.w3.org/1999/xlink\">", "</FictionBook>");
        assertTrue(out, out.startsWith("<html xmlns=\"http://www.w3.org/1999/xhtml\""));
        assertTrue(out, out.contains("<body>"));
        assertTrue(out, out.endsWith("</body></html>"));
        assertFalse(out, out.contains("FictionBook"));
    }

    @Test public void theXlinkPrefixIsDeclared() {
        // FB2 writes l:href on its images and notes; undeclared, the document is not XML
        final String out = run("<FictionBook>", "</FictionBook>");
        assertTrue(out, out.contains("xmlns:l=\"http://www.w3.org/1999/xlink\""));
    }

    @Test public void eachFb2BodyBecomesADiv() {
        final String out = run("<FictionBook>", "<body><p>text</p></body>",
                "<body name=\"notes\"><p>note</p></body>", "</FictionBook>");
        assertEquals(2, out.split("<div class=\"fb2-body\">", -1).length - 1);
        assertEquals(2, out.split("</div>", -1).length - 1);
        // the one body is the document's own, not the book's
        assertEquals(1, out.split("<body>", -1).length - 1);
    }

    @Test public void theBookKeepsItsOwnTags() {
        // app-Librera.css lays these out by name, so they are left alone
        final String out = run("<FictionBook>", "<body><section><title><p>Chapter</p></title>"
                + "<subtitle>* * *</subtitle><empty-line/><p>text</p></section></body>",
                "</FictionBook>");
        assertTrue(out, out.contains("<section>"));
        assertTrue(out, out.contains("<title>"));
        assertTrue(out, out.contains("<subtitle>"));
        assertTrue(out, out.contains("<empty-line/>"));
        assertTrue(out, out.contains("Chapter"));
    }

    @Test public void descriptionIsDropped() {
        final String out = run("<FictionBook>",
                "<description><title-info><book-title>Title</book-title></title-info></description>",
                "<body><p>text</p></body>", "</FictionBook>");
        assertFalse(out, out.contains("Title"));
        assertFalse(out, out.contains("book-title"));
        assertTrue(out, out.contains("text"));
    }

    @Test public void binariesAreDropped() {
        final String out = run("<FictionBook>", "<body><p>text</p></body>",
                "<binary id=\"cover.jpg\" content-type=\"image/jpeg\">AAAABBBBCCCC</binary>",
                "</FictionBook>");
        assertFalse(out, out.contains("AAAABBBBCCCC"));
        assertFalse(out, out.contains("<binary"));
        assertTrue(out, out.contains("text"));
    }

    @Test public void aDroppedElementSpansLines() {
        final String out = run("<FictionBook>", "<binary id=\"a\">AAAA", "BBBB", "CCCC</binary>",
                "<body><p>text</p></body>", "</FictionBook>");
        assertFalse(out, out.contains("AAAA"));
        assertFalse(out, out.contains("BBBB"));
        assertFalse(out, out.contains("CCCC"));
        assertTrue(out, out.contains("text"));
    }

    @Test public void stylesheetIsDropped() {
        final String out = run("<FictionBook>", "<stylesheet type=\"text/css\">p{color:red}</stylesheet>",
                "<body><p>text</p></body>", "</FictionBook>");
        assertFalse(out, out.contains("color:red"));
        assertTrue(out, out.contains("text"));
    }

    @Test public void aSelfClosingBinaryDropsNothingAfterIt() {
        final String out = run("<FictionBook>", "<binary id=\"a\"/>", "<body><p>text</p></body>",
                "</FictionBook>");
        assertTrue(out, out.contains("text"));
    }

    @Test public void theWholeBookOnOneLineIsHandled() {
        final String out = run("<FictionBook><description><lang>ru</lang></description>"
                + "<body><p>text</p></body><binary id=\"a\">ZZZZ</binary></FictionBook>");
        assertTrue(out, out.startsWith("<html"));
        assertTrue(out, out.endsWith("</body></html>"));
        assertTrue(out, out.contains("text"));
        assertFalse(out, out.contains("ZZZZ"));
        assertFalse(out, out.contains("ru"));
    }

    @Test public void textOutsideTagsIsKept() {
        final String out = run("<FictionBook>", "<body><p>a &amp; b</p></body>", "</FictionBook>");
        assertTrue(out, out.contains("a &amp; b"));
    }

    @Test public void aTagSplitAcrossLinesIsNotLost() {
        final String out = run("<FictionBook>", "<body><p class=", "\"x\">text</p></body>",
                "</FictionBook>");
        assertTrue(out, out.contains("text"));
    }
}
