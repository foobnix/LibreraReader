package com.foobnix.ext;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.foobnix.android.utils.LOG;
import com.foobnix.sys.TempHolder;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.mockito.MockedStatic;

import java.io.File;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;

// every test runs with zip4j and with java.util.zip
@RunWith(Parameterized.class)
public class EpubFooterNotesTest {

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> backends() {
        return Arrays.asList(new Object[][]{{"zip4j", false}, {"java.util.zip", true}});
    }

    @Parameterized.Parameter(0)
    public String backend;

    @Parameterized.Parameter(1)
    public boolean javaZip;

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

    private Map<String, String> notes(File file) {
        return EpubExtractor.get().getFooterNotes(file.getPath(), javaZip);
    }

    private Map<String, String> epubNotes(Object... nameAndContent) throws Exception {
        return notes(TestBooks.epub(folder.newFile("book.epub"), false, nameAndContent));
    }

    @Test
    public void linkToNotesFile() throws Exception {
        Map<String, String> notes = epubNotes(
                "OEBPS/Text/ch1.xhtml", "<p>Text<a href=\"notes.xhtml#n1\" id=\"r1\">[1]</a> more text here.</p>",
                "OEBPS/Text/notes.xhtml", "<p id=\"n1\"><a href=\"ch1.xhtml#r1\">[1]</a> The first note is long enough.</p>");
        assertEquals("[1] The first note is long enough.", notes.get("[1]#OEBPS/Text/ch1.xhtml"));
        // the back link from the notes shows the paragraph of the reference
        assertEquals("[1]   Text[1] more text here.", notes.get("[1]#OEBPS/Text/notes.xhtml"));
    }

    @Test
    public void relativePathToOtherFolder() throws Exception {
        Map<String, String> notes = epubNotes(
                "OEBPS/Text/ch1.xhtml", "<p>Text<a href=\"../Notes/notes.xhtml#n1\">[1]</a></p>",
                "OEBPS/Notes/notes.xhtml", "<p id=\"n1\">[1] A note in another folder.</p>");
        assertEquals("[1] A note in another folder.", notes.get("[1]#OEBPS/Text/ch1.xhtml"));
    }

    @Test
    public void absolutePathAndQuery() throws Exception {
        Map<String, String> notes = epubNotes(
                "OEBPS/Text/ch1.xhtml", "<p>A<a href=\"/OEBPS/notes.xhtml#n1\">[1]</a> B<a href=\"../notes.xhtml?v=2#n2\">[2]</a></p>",
                "OEBPS/notes.xhtml", "<p id=\"n1\">[1] Found by the absolute path.</p><p id=\"n2\">[2] Found without the query.</p>");
        assertEquals("[1] Found by the absolute path.", notes.get("[1]#OEBPS/Text/ch1.xhtml"));
        assertEquals("[2] Found without the query.", notes.get("[2]#OEBPS/Text/ch1.xhtml"));
    }

    @Test
    public void linkInSameFile() throws Exception {
        Map<String, String> notes = epubNotes(
                "OEBPS/ch1.xhtml", "<p>Text<a href=\"#n1\">[1]</a></p><p id=\"n1\">[1] A note at the end of the chapter.</p>");
        assertEquals("[1] A note at the end of the chapter.", notes.get("[1]#OEBPS/ch1.xhtml"));
    }

    @Test
    public void fileNameThatEndsWithAnotherFileName() throws Exception {
        // "footnotes.xhtml" ends with "notes.xhtml", the old name matching mixed them up
        Map<String, String> notes = epubNotes(
                "OEBPS/ch1.xhtml", "<p>A<a href=\"notes.xhtml#n1\">[1]</a> B<a href=\"footnotes.xhtml#f1\">[2]</a></p>",
                "OEBPS/footnotes.xhtml", "<p id=\"f1\">[2] Second note, from footnotes.</p>",
                "OEBPS/notes.xhtml", "<p id=\"n1\">[1] First note, from notes file.</p>");
        assertEquals("[1] First note, from notes file.", notes.get("[1]#OEBPS/ch1.xhtml"));
        assertEquals("[2] Second note, from footnotes.", notes.get("[2]#OEBPS/ch1.xhtml"));
    }

    @Test
    public void sameFileLinkedWithDifferentPaths() throws Exception {
        // the old code parsed the file once per spelling of its path, the second parse got nothing
        Map<String, String> notes = epubNotes(
                "OEBPS/Text/ch1.xhtml", "<p>A<a href=\"notes.xhtml#n1\">[1]</a></p>",
                "OEBPS/Text/ch2.xhtml", "<p>B<a href=\"../Text/notes.xhtml#n2\">[2]</a></p>",
                "OEBPS/Text/notes.xhtml", "<p id=\"n1\">[1] Note from the first chapter.</p><p id=\"n2\">[2] Note from the second chapter.</p>");
        assertEquals("[1] Note from the first chapter.", notes.get("[1]#OEBPS/Text/ch1.xhtml"));
        assertEquals("[2] Note from the second chapter.", notes.get("[2]#OEBPS/Text/ch2.xhtml"));
    }

    @Test
    public void missingPathFallsBackToFileName() throws Exception {
        Map<String, String> notes = epubNotes(
                "OEBPS/ch1.xhtml", "<p>Text<a href=\"wrong/notes.xhtml#n1\">[1]</a></p>",
                "OEBPS/Text/notes.xhtml", "<p id=\"n1\">[1] Found by its file name.</p>");
        assertEquals("[1] Found by its file name.", notes.get("[1]#OEBPS/ch1.xhtml"));
    }

    @Test
    public void shortNoteTakesFollowingSiblings() throws Exception {
        Map<String, String> notes = epubNotes(
                "OEBPS/ch1.xhtml", "<p>Text<a href=\"#n2\">[2]</a></p>"
                        + "<div><span id=\"n2\">[2]</span><span>Short.</span><span> Second sibling text.</span></div>");
        assertEquals("[2] Short. Second sibling text.", notes.get("[2]#OEBPS/ch1.xhtml"));
    }

    @Test
    public void linkTextWithTagsAndEntities() throws Exception {
        Map<String, String> notes = epubNotes(
                "OEBPS/ch1.xhtml", "<p>Text<a href=\"#n1\"><sup>&#91;1&#93;</sup></a></p><p id=\"n1\">[1] Note behind an entity link.</p>");
        assertEquals("[1] Note behind an entity link.", notes.get("[1]#OEBPS/ch1.xhtml"));
    }

    @Test
    public void singleQuotedAttributes() throws Exception {
        Map<String, String> notes = epubNotes(
                "OEBPS/ch1.xhtml", "<p>Text<a class='fn' href='#n1' id='r1'>[1]</a></p><p id='n1'>[1] Note with single quotes.</p>");
        assertEquals("[1] Note with single quotes.", notes.get("[1]#OEBPS/ch1.xhtml"));
    }

    @Test
    public void ignoresLinksThatAreNotFooterNotes() throws Exception {
        Map<String, String> notes = epubNotes(
                "OEBPS/ch1.xhtml", "<p><a href=\"#c1\">Chapter 1</a> <a href=\"http://example.com/a.xhtml#n1\">[1]</a>"
                        + "<!-- <a href=\"#c1\">[3]</a> --></p><h1 id=\"c1\">Chapter 1 title is long</h1>");
        assertEquals(0, notes.size());
    }

    @Test
    public void duplicateIdsPickTheElementWithTheLinkText() throws Exception {
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

    @Test
    public void storedEntries() throws Exception {
        Map<String, String> notes = notes(TestBooks.epub(folder.newFile("stored.epub"), true,
                "OEBPS/ch1.xhtml", "<p>Text<a href=\"#n1\">[1]</a></p><p id=\"n1\">[1] Note in a stored entry.</p>"));
        assertEquals("[1] Note in a stored entry.", notes.get("[1]#OEBPS/ch1.xhtml"));
    }

    @Test
    public void directoryAndImageEntriesAreSkipped() throws Exception {
        Map<String, String> notes = epubNotes(
                "OEBPS/images/", new byte[0],
                "OEBPS/images/cover.png", new byte[]{(byte) 0x89, 'P', 'N', 'G', '[', '1', ']'},
                "OEBPS/ch1.xhtml", "<p>Text<a href=\"#n1\">[1]</a></p><p id=\"n1\">[1] Note next to images.</p>");
        assertEquals(1, notes.size());
        assertEquals("[1] Note next to images.", notes.get("[1]#OEBPS/ch1.xhtml"));
    }

    @Test
    public void nonAsciiFileNamesAndEncodedHref() throws Exception {
        String href = URLEncoder.encode("примечания", "UTF-8") + ".xhtml#n1";
        Map<String, String> notes = epubNotes(
                "OEBPS/Текст/глава.xhtml", "<p>Текст<a href=\"" + href + "\">[1]</a></p>",
                "OEBPS/Текст/примечания.xhtml", "<p id=\"n1\">[1] Примечание к первой главе.</p>");
        assertEquals("[1] Примечание к первой главе.", notes.get("[1]#OEBPS/Текст/глава.xhtml"));
    }

    @Test
    public void windows1251Documents() throws Exception {
        Charset cp1251 = Charset.forName("windows-1251");
        String chapter = "<?xml version=\"1.0\" encoding=\"windows-1251\"?>\n<html><body><p>Текст<a href=\"#n1\">[1] см.</a></p>"
                + "<p id=\"n1\">[1] Примечание в кодировке windows-1251.</p></body></html>";
        Map<String, String> notes = epubNotes("OEBPS/ch1.xhtml", chapter.getBytes(cp1251));
        assertEquals("[1] Примечание в кодировке windows-1251.", notes.get("[1] см.#OEBPS/ch1.xhtml"));
    }

    @Test
    public void utf16Document() throws Exception {
        String chapter = "﻿<html><body><p>Text<a href=\"#n1\">[1]</a></p><p id=\"n1\">[1] Note in a UTF-16 file.</p></body></html>";
        Map<String, String> notes = epubNotes("OEBPS/ch1.xhtml", chapter.getBytes(StandardCharsets.UTF_16LE));
        assertEquals("[1] Note in a UTF-16 file.", notes.get("[1]#OEBPS/ch1.xhtml"));
    }

    @Test
    public void notAZipGivesNoNotes() throws Exception {
        File file = folder.newFile("broken.epub");
        Files.write(file.toPath(), "this is not a zip file".getBytes(StandardCharsets.US_ASCII));
        assertTrue(notes(file).isEmpty());
    }

    @Test
    public void cancelledLoadingGivesNoNotes() throws Exception {
        File file = TestBooks.epub(folder.newFile("book.epub"), false,
                "OEBPS/ch1.xhtml", "<p>Text<a href=\"#n1\">[1]</a></p><p id=\"n1\">[1] Never read.</p>");
        TempHolder.get().loadingCancelled.set(true);
        assertTrue(notes(file).isEmpty());
    }
}
