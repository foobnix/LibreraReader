package com.foobnix.ext;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import com.foobnix.android.utils.LOG;
import com.foobnix.sys.TempHolder;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class FooterNotesExtractTest {

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    private MockedStatic<LOG> log;

    @Before
    public void setUp() {
        log = Mockito.mockStatic(LOG.class);
        // not a constant in android.jar, so it is null in JVM tests and kxml rejects it
        android.util.Xml.FEATURE_RELAXED = "http://xmlpull.org/v1/doc/features.html#relaxed";
        TempHolder.get().loadingCancelled.set(false);
    }

    @After
    public void tearDown() {
        log.close();
    }

    private static String xhtml(String body) {
        return "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
                + "<html xmlns=\"http://www.w3.org/1999/xhtml\"><head><title>t</title></head><body>\n"
                + body + "\n</body></html>";
    }

    private Map<String, String> epubNotes(String... nameAndBody) throws Exception {
        File file = folder.newFile("book.epub");
        try (ZipOutputStream zip = new ZipOutputStream(new FileOutputStream(file))) {
            zip.putNextEntry(new ZipEntry("mimetype"));
            zip.write("application/epub+zip".getBytes(StandardCharsets.US_ASCII));
            for (int i = 0; i < nameAndBody.length; i += 2) {
                zip.putNextEntry(new ZipEntry(nameAndBody[i]));
                zip.write(xhtml(nameAndBody[i + 1]).getBytes(StandardCharsets.UTF_8));
            }
        }
        return EpubExtractor.get().getFooterNotes(file.getPath());
    }

    private Map<String, String> fb2Notes(String bodies) throws Exception {
        File file = folder.newFile("book.fb2");
        String fb2 = "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
                + "<FictionBook xmlns=\"http://www.gribuser.ru/xml/fictionbook/2.0\" xmlns:l=\"http://www.w3.org/1999/xlink\">\n"
                + bodies + "\n</FictionBook>";
        Files.write(file.toPath(), fb2.getBytes(StandardCharsets.UTF_8));
        return Fb2Extractor.get().getFooterNotes(file.getPath());
    }

    // ---------- EPUB ----------

    @Test
    public void epubLinkToNotesFile() throws Exception {
        Map<String, String> notes = epubNotes(
                "OEBPS/Text/ch1.xhtml", "<p>Text<a href=\"notes.xhtml#n1\" id=\"r1\">[1]</a> more text here.</p>",
                "OEBPS/Text/notes.xhtml", "<p id=\"n1\"><a href=\"ch1.xhtml#r1\">[1]</a> The first note is long enough.</p>");
        assertEquals("[1] The first note is long enough.", notes.get("[1]#OEBPS/Text/ch1.xhtml"));
        // the back link from the notes shows the paragraph of the reference
        assertEquals("[1]   Text[1] more text here.", notes.get("[1]#OEBPS/Text/notes.xhtml"));
    }

    @Test
    public void epubRelativePathToOtherFolder() throws Exception {
        Map<String, String> notes = epubNotes(
                "OEBPS/Text/ch1.xhtml", "<p>Text<a href=\"../Notes/notes.xhtml#n1\">[1]</a></p>",
                "OEBPS/Notes/notes.xhtml", "<p id=\"n1\">[1] A note in another folder.</p>");
        assertEquals("[1] A note in another folder.", notes.get("[1]#OEBPS/Text/ch1.xhtml"));
    }

    @Test
    public void epubLinkInSameFile() throws Exception {
        Map<String, String> notes = epubNotes(
                "OEBPS/ch1.xhtml", "<p>Text<a href=\"#n1\">[1]</a></p><p id=\"n1\">[1] A note at the end of the chapter.</p>");
        assertEquals("[1] A note at the end of the chapter.", notes.get("[1]#OEBPS/ch1.xhtml"));
    }

    @Test
    public void epubFileNameThatEndsWithAnotherFileName() throws Exception {
        // "footnotes.xhtml" ends with "notes.xhtml", the old name matching mixed them up
        Map<String, String> notes = epubNotes(
                "OEBPS/ch1.xhtml", "<p>A<a href=\"notes.xhtml#n1\">[1]</a> B<a href=\"footnotes.xhtml#f1\">[2]</a></p>",
                "OEBPS/footnotes.xhtml", "<p id=\"f1\">[2] Second note, from footnotes.</p>",
                "OEBPS/notes.xhtml", "<p id=\"n1\">[1] First note, from notes file.</p>");
        assertEquals("[1] First note, from notes file.", notes.get("[1]#OEBPS/ch1.xhtml"));
        assertEquals("[2] Second note, from footnotes.", notes.get("[2]#OEBPS/ch1.xhtml"));
    }

    @Test
    public void epubMissingPathFallsBackToFileName() throws Exception {
        Map<String, String> notes = epubNotes(
                "OEBPS/ch1.xhtml", "<p>Text<a href=\"wrong/notes.xhtml#n1\">[1]</a></p>",
                "OEBPS/Text/notes.xhtml", "<p id=\"n1\">[1] Found by its file name.</p>");
        assertEquals("[1] Found by its file name.", notes.get("[1]#OEBPS/ch1.xhtml"));
    }

    @Test
    public void epubShortNoteTakesFollowingSiblings() throws Exception {
        Map<String, String> notes = epubNotes(
                "OEBPS/ch1.xhtml", "<p>Text<a href=\"#n2\">[2]</a></p>"
                        + "<div><span id=\"n2\">[2]</span><span>Short.</span><span> Second sibling text.</span></div>");
        assertEquals("[2] Short. Second sibling text.", notes.get("[2]#OEBPS/ch1.xhtml"));
    }

    @Test
    public void epubLinkTextWithTagsAndEntities() throws Exception {
        Map<String, String> notes = epubNotes(
                "OEBPS/ch1.xhtml", "<p>Text<a href=\"#n1\"><sup>&#91;1&#93;</sup></a></p><p id=\"n1\">[1] Note behind an entity link.</p>");
        assertEquals("[1] Note behind an entity link.", notes.get("[1]#OEBPS/ch1.xhtml"));
    }

    @Test
    public void epubIgnoresLinksThatAreNotFooterNotes() throws Exception {
        Map<String, String> notes = epubNotes(
                "OEBPS/ch1.xhtml", "<p><a href=\"#c1\">Chapter 1</a> <a href=\"http://example.com/a.xhtml#n1\">[1]</a>"
                        + "<!-- <a href=\"#c1\">[3]</a> --></p><h1 id=\"c1\">Chapter 1 title is long</h1>");
        assertEquals(0, notes.size());
    }

    @Test
    public void epubDuplicateIdsPickTheElementWithTheLinkText() throws Exception {
        // PocketBook files repeat ids: the list around the notes and notes of other chapters share "n_1"
        Map<String, String> notes = epubNotes(
                "OEBPS/ch1.xhtml", "<p>A<a href=\"notes.xhtml#n_1\">[1]</a> B<a href=\"notes.xhtml#n_2\">[2]</a></p>",
                "OEBPS/notes.xhtml", "<div id=\"n_1\"><p>Notes</p><p><a id=\"n_1\">[1]</a> First note of chapter one.</p>"
                        + "<p><a id=\"n_2\">[2]</a> Second note of chapter one.</p></div>"
                        + "<p><a id=\"n_1\">[15]</a> Fifteenth note of another chapter.</p>"
                        + "<p><a id=\"n_2\">[22]</a> Twenty second note of another chapter.</p>");
        assertEquals("[1]   [1] First note of chapter one.", notes.get("[1]#OEBPS/ch1.xhtml"));
        assertEquals("[2]   [2] Second note of chapter one.", notes.get("[2]#OEBPS/ch1.xhtml"));
    }

    // ---------- FB2 ----------

    private static final String FB2_TEXT = "<body><section><p>Text<a l:href=\"#n1\" type=\"note\">[1]</a> and more.</p></section></body>\n";
    private static final String FB2_NOTES = "<body name=\"notes\"><section id=\"n1\"><title><p>1</p></title><p>Note one.</p></section></body>\n";
    private static final String FB2_BINARY = "<binary id=\"img.jpg\" content-type=\"image/jpeg\">QUJDREVGRw==</binary>\n";

    @Test
    public void fb2Notes() throws Exception {
        Map<String, String> notes = fb2Notes(FB2_TEXT + FB2_NOTES + FB2_BINARY);
        assertEquals("1 Note one.", notes.get("[1]#OEBPS/fb2.fb2"));
        assertEquals("1 Note one.", notes.get("[1]"));
    }

    @Test
    public void fb2NotesAfterBinary() throws Exception {
        Map<String, String> notes = fb2Notes(FB2_TEXT + FB2_BINARY + FB2_NOTES);
        assertEquals("1 Note one.", notes.get("[1]#OEBPS/fb2.fb2"));
    }

    @Test
    public void fb2NoteLinkedTwice() throws Exception {
        String text = "<body><section><p>A<a l:href=\"#n1\">[1]</a> B<a l:href=\"#n1\">{1}</a></p></section></body>\n";
        Map<String, String> notes = fb2Notes(text + FB2_NOTES);
        assertEquals("1 Note one.", notes.get("[1]#OEBPS/fb2.fb2"));
        assertEquals("1 Note one.", notes.get("{1}#OEBPS/fb2.fb2"));
    }

    @Test
    public void fb2SectionWithoutLinkIsNotStored() throws Exception {
        Map<String, String> notes = fb2Notes("<body><section id=\"c1\"><p>Chapter text</p></section></body>" + FB2_NOTES);
        assertFalse(notes.containsKey(null));
        assertFalse(notes.containsKey("null#OEBPS/fb2.fb2"));
    }
}
