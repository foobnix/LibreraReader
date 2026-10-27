package com.foobnix.ext;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.foobnix.android.utils.LOG;
import com.foobnix.sys.TempHolder;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.MockedStatic;

import java.util.Map;

public class Fb2FooterNotesTest {

    static final String TEXT = "<body><section><p>Text<a l:href=\"#n1\" type=\"note\">[1]</a> and more.</p></section></body>\n";
    static final String NOTES = "<body name=\"notes\"><section id=\"n1\"><title><p>1</p></title><p>Note one.</p></section></body>\n";
    static final String BINARY = "<binary id=\"img.jpg\" content-type=\"image/jpeg\">QUJDREVGRw==</binary>\n";

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
        TempHolder.get().loadingCancelled.set(false);
    }

    private Map<String, String> notes(String encoding, String bodies) throws Exception {
        return Fb2Extractor.get().getFooterNotes(TestBooks.fb2(folder.newFile("book.fb2"), encoding, bodies).getPath());
    }

    private Map<String, String> notes(String bodies) throws Exception {
        return notes("utf-8", bodies);
    }

    @Test
    public void notes() throws Exception {
        Map<String, String> notes = notes(TEXT + NOTES + BINARY);
        assertEquals("1 Note one.", notes.get("[1]#OEBPS/fb2.fb2"));
        assertEquals("1 Note one.", notes.get("[1]"));
    }

    @Test
    public void notesAfterBinary() throws Exception {
        Map<String, String> notes = notes(TEXT + BINARY + NOTES);
        assertEquals("1 Note one.", notes.get("[1]#OEBPS/fb2.fb2"));
    }

    @Test
    public void noteLinkedTwice() throws Exception {
        String text = "<body><section><p>A<a l:href=\"#n1\">[1]</a> B<a l:href=\"#n1\">{1}</a></p></section></body>\n";
        Map<String, String> notes = notes(text + NOTES);
        assertEquals("1 Note one.", notes.get("[1]#OEBPS/fb2.fb2"));
        assertEquals("1 Note one.", notes.get("{1}#OEBPS/fb2.fb2"));
    }

    @Test
    public void linkWithoutFooterNoteTextUsesItsTarget() throws Exception {
        String text = "<body><section><p>A<a l:href=\"#n1\">1</a></p></section></body>\n";
        Map<String, String> notes = notes(text + NOTES);
        assertEquals("1 Note one.", notes.get("[#n1]#OEBPS/fb2.fb2"));
    }

    @Test
    public void sectionWithoutLinkIsNotStored() throws Exception {
        Map<String, String> notes = notes("<body><section id=\"c1\"><p>Chapter text</p></section></body>" + NOTES);
        assertFalse(notes.containsKey(null));
        assertFalse(notes.containsKey("null#OEBPS/fb2.fb2"));
    }

    @Test
    public void entitiesAndTagsInNoteText() throws Exception {
        String notesBody = "<body name=\"notes\"><section id=\"n1\"><title><p>1</p></title><p>Tom &amp; <emphasis>Jerry</emphasis> note.</p></section></body>";
        Map<String, String> notes = notes(TEXT + notesBody);
        assertEquals("1 Tom & Jerry note.", notes.get("[1]#OEBPS/fb2.fb2"));
    }

    @Test
    public void windows1251File() throws Exception {
        String text = "<body><section><p>Текст<a l:href=\"#n1\">[1]</a></p></section></body>\n";
        String notesBody = "<body name=\"notes\"><section id=\"n1\"><title><p>1</p></title><p>Примечание.</p></section></body>";
        Map<String, String> notes = notes("windows-1251", text + notesBody + BINARY);
        assertEquals("1 Примечание.", notes.get("[1]#OEBPS/fb2.fb2"));
    }

    @Test
    public void binaryAndBodyTagsAcrossReadBoundary() throws Exception {
        // the pre-scan reads 64 KB at a time, a tag split between two reads must still be found
        String head = "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
                + "<FictionBook xmlns=\"http://www.gribuser.ru/xml/fictionbook/2.0\" xmlns:l=\"http://www.w3.org/1999/xlink\">\n" + TEXT;
        for (int shift = -10; shift <= 10; shift++) {
            StringBuilder padding = new StringBuilder("<!--");
            while (head.length() + padding.length() + 3 < 64 * 1024 + shift) {
                padding.append('x');
            }
            padding.append("-->");
            java.io.File file = folder.newFile("boundary" + shift + ".fb2");
            // <binary starts near the boundary, then a <body with the notes follows it
            java.nio.file.Files.write(file.toPath(), (head + padding + BINARY + NOTES + "</FictionBook>").getBytes("UTF-8"));
            for (boolean bytes : new boolean[]{false, true}) {
                assertTrue("shift " + shift, Fb2Extractor.hasBodyAfterBinary(file.getPath(), 0, bytes));
                assertTrue("shift " + shift, Fb2Extractor.hasBodyAfterBinary(file.getPath(), 64 * 1024 - 100, bytes));
            }
            assertEquals("shift " + shift, "1 Note one.", Fb2Extractor.get().getFooterNotes(file.getPath()).get("[1]#OEBPS/fb2.fb2"));

            java.io.File plain = folder.newFile("plain" + shift + ".fb2");
            java.nio.file.Files.write(plain.toPath(), (head + NOTES + padding + BINARY + "</FictionBook>").getBytes("UTF-8"));
            assertFalse("shift " + shift, Fb2Extractor.hasBodyAfterBinary(plain.getPath(), 0, false));
            assertFalse("shift " + shift, Fb2Extractor.hasBodyAfterBinary(plain.getPath(), 0, true));
            assertEquals("shift " + shift, "1 Note one.", Fb2Extractor.get().getFooterNotes(plain.getPath()).get("[1]#OEBPS/fb2.fb2"));
        }
    }

    @Test
    public void cancelledLoadingStops() throws Exception {
        TempHolder.get().loadingCancelled.set(true);
        assertTrue(notes(TEXT + NOTES).isEmpty());
    }
}
