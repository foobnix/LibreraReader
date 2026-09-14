package com.foobnix.ext;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import com.foobnix.android.utils.LOG;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.MockedStatic;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

// on usual books the rewrite gives what master gave, with zip4j and with java.util.zip
public class FooterNotesLegacyCompareTest {

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    private MockedStatic<LOG> log;

    @Before
    public void setUp() {
        log = TestBooks.setUp();
    }

    @After
    public void tearDown() {
        log.close();
    }

    private void assertSameAsLegacy(Object... nameAndContent) throws Exception {
        String path = TestBooks.epub(folder.newFile(), false, nameAndContent).getPath();
        Map<String, String> legacy = LegacyFooterNotes.epub(path);
        assertFalse(legacy.isEmpty());
        assertEquals("zip4j", legacy, EpubExtractor.get().getFooterNotes(path, false));
        assertEquals("java.util.zip", legacy, EpubExtractor.get().getFooterNotes(path, true));
    }

    @Test
    public void gutenbergNotesAtChapterEnd() throws Exception {
        String chapter = "<p>First paragraph with a note<a id=\"FNanchor_1_1\" href=\"#Footnote_1_1\" class=\"fnanchor\">[1]</a> and more words.</p>"
                + "<p>Second<a id=\"FNanchor_2_2\" href=\"#Footnote_2_2\" class=\"fnanchor\">[2]</a>.</p>"
                + "<div class=\"footnotes\"><div class=\"footnote\"><p><a id=\"Footnote_1_1\" href=\"#FNanchor_1_1\" class=\"label\">[1]</a>"
                + " The first footnote, long enough.</p></div>"
                + "<div class=\"footnote\"><p><a id=\"Footnote_2_2\" href=\"#FNanchor_2_2\" class=\"label\">[2]</a> Short.</p></div></div>";
        assertSameAsLegacy("OEBPS/ch1.xhtml", chapter, "OEBPS/ch2.xhtml", chapter.replace("_1", "_3").replace("_2", "_4"));
    }

    @Test
    public void calibreNotesFileWithBackLinks() throws Exception {
        assertSameAsLegacy(
                "OEBPS/Text/part1.xhtml", "<p>Text <a href=\"../Text/notes.xhtml#n1\" id=\"back1\"><sup>[1]</sup></a> text"
                        + " <a href=\"../Text/notes.xhtml#n2\" id=\"back2\"><sup>[2]</sup></a>.</p>",
                "OEBPS/Text/part2.xhtml", "<p>More <a href=\"../Text/notes.xhtml#n3\" id=\"back3\">[3]</a></p>",
                "OEBPS/Text/notes.xhtml", "<p id=\"n1\"><a href=\"part1.xhtml#back1\">[1]</a> Note one text is here.</p>"
                        + "<p id=\"n2\"><a href=\"part1.xhtml#back2\">[2]</a> Note two.</p>"
                        + "<p id=\"n3\"><a href=\"part2.xhtml#back3\">[3]</a> Note three is the last one.</p>");
    }

    @Test
    public void curlyBracesAndEntities() throws Exception {
        assertSameAsLegacy("OEBPS/c.xhtml", "<p>Word<a href=\"#x1\">{1}</a> and <a href=\"#x2\">&#91;2&#93;</a></p>"
                + "<aside id=\"x1\">{1} Curly note text long enough.</aside>"
                + "<aside id=\"x2\"><p>[2]</p><p>Entity note text long enough.</p></aside>");
    }

    @Test
    public void fb2SameAsLegacy() throws Exception {
        String bodies = "<body><section><p>A<a l:href=\"#n1\" type=\"note\">[1]</a> B<a l:href=\"#n2\" type=\"note\">[2]</a></p></section></body>\n"
                + "<body name=\"notes\"><section id=\"n1\"><title><p>1</p></title><p>Note one.</p></section>"
                + "<section id=\"n2\"><title><p>2</p></title><p>Note <emphasis>two</emphasis>.</p></section></body>\n"
                + Fb2FooterNotesTest.BINARY;
        File file = TestBooks.fb2(folder.newFile("book.fb2"), "utf-8", bodies);
        Map<String, String> legacy = new HashMap<>(LegacyFooterNotes.fb2(file.getPath()));
        legacy.remove(null);
        legacy.remove("null#OEBPS/fb2.fb2");
        assertFalse(legacy.isEmpty());
        assertEquals(legacy, Fb2Extractor.get().getFooterNotes(file.getPath()));
    }

    @Test
    public void includeFooterNotesSameAsLegacy() {
        String name = "OEBPS/ch1.xhtml";
        Map<String, String> notes = new HashMap<>();
        notes.put("[1]#" + name, "[1] First note.");
        notes.put("[2]#" + name, "2 Second note.");
        notes.put("{3}#" + name, "{3}[3] Third <note> & more.");
        String[] lines = {
                "<p>Text<a href=\"#n1\">[1]</a> more</p>",
                // the old code put a note after the next '>' when it was near, even over text: keep it far
                "<p>A[1] B[2] C{3} and the rest of the sentence</p>",
                "<p>Unknown [9] and [long one] and {x}</p>",
                "<p>Без примечаний</p>",
                "    <p class=\"x\">Indented[2]</p>",
                "<p>x{3}</a> and more words</p>",
        };
        for (String line : lines) {
            assertEquals(line, LegacyFooterNotes.includeFooterNotes(line, notes, name), Fb2Extractor.includeFooterNotes(line, notes, name));
        }
    }
}
