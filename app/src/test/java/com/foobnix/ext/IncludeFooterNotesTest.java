package com.foobnix.ext;

import static org.junit.Assert.assertEquals;

import com.foobnix.android.utils.LOG;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.util.HashMap;
import java.util.Map;

public class IncludeFooterNotesTest {

    private static final String FILE = "OEBPS/ch1.xhtml";

    // LOG.d reads AppsConfig.IS_LOG, whose static init loads the MuPDF native library
    private MockedStatic<LOG> log;

    @Before
    public void setUp() {
        log = Mockito.mockStatic(LOG.class);
    }

    @After
    public void tearDown() {
        log.close();
    }

    private static Map<String, String> notes(String... refAndText) {
        Map<String, String> map = new HashMap<>();
        for (int i = 0; i < refAndText.length; i += 2) {
            map.put(refAndText[i] + "#" + FILE, refAndText[i + 1]);
        }
        return map;
    }

    private static String include(String line, Map<String, String> notes) {
        return Fb2Extractor.includeFooterNotes(line, notes, FILE);
    }

    @Test
    public void nullNotesReturnsLineUnchanged() {
        assertEquals("<p>Text[1]</p>", Fb2Extractor.includeFooterNotes("<p>Text[1]</p>", null, FILE));
    }

    @Test
    public void lineWithoutBracketsIsUnchanged() {
        assertEquals("<p>Plain text</p>", include("<p>Plain text</p>", notes("[1]", "One")));
    }

    @Test
    public void insertsNoteAfterReference() {
        assertEquals("<p>Text[1] <t>[Note text]</t> more</p>",
                include("<p>Text[1] more</p>", notes("[1]", "Note text")));
    }

    @Test
    public void insertsNoteAfterClosingLinkTag() {
        assertEquals("<a href=\"#n1\">[1]</a> <t>[One]</t> text",
                include("<a href=\"#n1\">[1]</a> text", notes("[1]", "One")));
    }

    @Test
    public void supportsCurlyBraces() {
        assertEquals("x{2}</a> <t>[Two]</t>", include("x{2}</a>", notes("{2}", "Two")));
    }

    @Test
    public void unknownReferenceIsUnchanged() {
        assertEquals("a[9] b", include("a[9] b", notes("[1]", "One")));
    }

    @Test
    public void notesFromAnotherFileAreIgnored() {
        Map<String, String> other = new HashMap<>();
        other.put("[1]#OEBPS/ch2.xhtml", "One");
        assertEquals("a[1] b", include("a[1] b", other));
    }

    @Test
    public void insertsEveryReferenceOnLine() {
        assertEquals("A[1] <t>[One]</t> B[2] <t>[Two]</t> C",
                include("A[1] B[2] C", notes("[1]", "One", "[2]", "Two")));
    }

    @Test
    public void referenceHasAtMostFourCharsInside() {
        assertEquals("x[1234] <t>[Four]</t>", include("x[1234]", notes("[1234]", "Four")));
        assertEquals("x[12345]", include("x[12345]", notes("[12345]", "Five")));
    }

    @Test
    public void innermostBracketIsTheReference() {
        assertEquals("x[a[1] <t>[One]</t>", include("x[a[1]", notes("[1]", "One")));
    }

    @Test
    public void stripsLeadingNoteNumberFromText() {
        assertEquals("x[1] <t>[Note]</t>", include("x[1]", notes("[1]", "[1] Note")));
        assertEquals("x[1] <t>[Note]</t>", include("x[1]", notes("[1]", "{1}[1] Note")));
        assertEquals("x[1] <t>[Note]</t>", include("x[1]", notes("[1]", "1 Note")));
        assertEquals("x[1] <t>[Note]</t>", include("x[1]", notes("[1]", " [1] Note")));
        assertEquals("x[1] <t>[Note]</t>", include("x[1]", notes("[1]", "1) Note")));
    }

    @Test
    public void keepsNumbersThatAreNotTheNoteNumber() {
        assertEquals("x[1] <t>[1.5 liters]</t>", include("x[1]", notes("[1]", "1.5 liters")));
        assertEquals("x[1] <t>[1st edition]</t>", include("x[1]", notes("[1]", "1st edition")));
        assertEquals("x[1] <t>[12 apostles]</t>", include("x[1]", notes("[1]", "12 apostles")));
    }

    @Test
    public void escapesHtmlInNoteText() {
        assertEquals("x[1] <t>[a &lt; b &amp; c]</t>", include("x[1]", notes("[1]", "a < b & c")));
    }

    @Test
    public void referenceAtStartOfLine() {
        assertEquals("[1] <t>[One]</t> starts the line",
                include("[1] starts the line", notes("[1]", "One")));
    }

    @Test
    public void adjacentReferencesInsideOneLink() {
        assertEquals("<a href=\"#n1\">[1][2]</a> <t>[One]</t> <t>[Two]</t>",
                include("<a href=\"#n1\">[1][2]</a>", notes("[1]", "One", "[2]", "Two")));
    }

    @Test
    public void noteTextStartingWithNumberKeepsIt() {
        assertEquals("x[3] <t>[1812 was the year]</t>",
                include("x[3]", notes("[3]", "[3] 1812 was the year")));
    }

    @Test
    public void noteNumberWithDotIsStripped() {
        assertEquals("x[1] <t>[See page 5]</t>", include("x[1]", notes("[1]", "1. See page 5")));
    }

    @Test
    public void noteIsNotInsertedAfterFollowingOpeningTag() {
        assertEquals("Text[1] <t>[One]</t> <b>bold</b>",
                include("Text[1] <b>bold</b>", notes("[1]", "One")));
    }

    @Test
    public void noteIsPlacedOutsideSuperscript() {
        assertEquals("<sup><a href=\"#n1\">[1]</a></sup> <t>[One]</t> text",
                include("<sup><a href=\"#n1\">[1]</a></sup> text", notes("[1]", "One")));
    }

    @Test
    public void referenceInsideAttributeIsIgnored() {
        String line = "<img alt=\"see [1] for details\" src=\"a.png\"/>";
        assertEquals(line, include(line, notes("[1]", "One")));
    }

    @Test
    public void referenceInTagContinuingOnNextLineIsIgnored() {
        assertEquals("a[1] <t>[One]</t> <img alt=\"see [1]",
                include("a[1] <img alt=\"see [1]", notes("[1]", "One")));
    }

    @Test
    public void referenceInCommentIsIgnored() {
        assertEquals("<!-- a > [1] --> b", include("<!-- a > [1] --> b", notes("[1]", "One")));
    }

    @Test
    public void longLineIsProcessedInLinearTime() {
        StringBuilder line = new StringBuilder();
        for (int i = 0; i < 50_000; i++) {
            line.append("<a href=\"#n1\">[1]</a> word ");
        }
        long start = System.nanoTime();
        String out = include(line.toString(), notes("[1]", "One"));
        long ms = (System.nanoTime() - start) / 1_000_000;
        assertEquals(line.length() + 50_000 * " <t>[One]</t>".length(), out.length());
        assertEquals("took " + ms + " ms", true, ms < 2000);
    }
}
