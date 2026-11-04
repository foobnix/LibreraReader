package com.foobnix.ext;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.Locale;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class PageEstimateTest {

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    private static String repeat(String s, int times) {
        StringBuilder b = new StringBuilder();
        for (int i = 0; i < times; i++) {
            b.append(s);
        }
        return b.toString();
    }

    private File epub(String... chapters) throws Exception {
        File epub = folder.newFile();
        try (ZipOutputStream zip = new ZipOutputStream(new FileOutputStream(epub))) {
            zip.putNextEntry(new ZipEntry("mimetype"));
            zip.write("application/epub+zip".getBytes(StandardCharsets.US_ASCII));
            zip.putNextEntry(new ZipEntry("META-INF/container.xml"));
            zip.write(repeat("<container/>", 1000).getBytes(StandardCharsets.UTF_8));
            for (int i = 0; i < chapters.length; i++) {
                zip.putNextEntry(new ZipEntry("OEBPS/ch" + i + ".xhtml"));
                zip.write(chapters[i].getBytes(StandardCharsets.UTF_8));
            }
            zip.putNextEntry(new ZipEntry("OEBPS/cover.jpg"));
            zip.write(new byte[500000]);
        }
        File named = new File(epub.getPath() + ".epub");
        assertTrue(epub.renameTo(named));
        return named;
    }

    @Test
    public void anEpubIsTheSizeOfItsChaptersNotOfItsPictures() throws Exception {
        // 225,000 bytes of English with its markup is 180,000 characters: 100 pages
        File book = epub(repeat("x", 225000));
        assertEquals(100, PageEstimate.of(book.getPath(), "en", null));
    }

    @Test
    public void russianTakesTwoBytesALetter() throws Exception {
        File book = epub(repeat("x", 2050 * 180));
        assertEquals(100, PageEstimate.of(book.getPath(), "ru", null));
        // With no language, the title tells the script
        assertEquals(100, PageEstimate.of(book.getPath(), null, "Война и мир"));
    }

    @Test
    public void anEpubOfPicturesIsAPageAChapter() throws Exception {
        File book = epub("<img src='1.jpg'/>", "<img src='2.jpg'/>", "<img src='3.jpg'/>");
        assertEquals(3, PageEstimate.of(book.getPath(), "en", null));
    }

    @Test
    public void thePicturesAtTheEndOfAnFb2AreNotText() throws Exception {
        String text = "<?xml version=\"1.0\" encoding=\"windows-1251\"?><FictionBook><body>"
                + repeat("<p>Some words of a story, told.</p>\n", 6000) + "</body>";
        String pictures = "<binary id=\"1.jpg\">" + repeat("QUJDREVGR0hJSktMTU5PUFFSU1RVVldYWVo0MTIz\n", 40000) + "</binary></FictionBook>";
        File fb2 = folder.newFile("book.fb2");
        Files.write(fb2.toPath(), (text + pictures).getBytes(StandardCharsets.ISO_8859_1));
        long expected = (long) (text.length() / 1.15);
        long chars = PageEstimate.chars(fb2.getPath(), 1.8);
        // Found within the 4 KB the halving stops at, and a single-byte FB2 is a byte a letter
        // whatever the language
        assertTrue("chars " + chars + " expected about " + expected, Math.abs(chars - expected) < 8192);
    }

    @Test
    public void aTextFileIsMeasuredByItsEncoding() throws Exception {
        File utf8 = folder.newFile("utf8.txt");
        Files.write(utf8.toPath(), repeat("Привет мир. ", 300).getBytes(StandardCharsets.UTF_8));
        assertEquals(21.0 / 12, PageEstimate.sniff(utf8, 1), 0.02);

        File cp1251 = folder.newFile("cp1251.txt");
        Files.write(cp1251.toPath(), repeat("Привет мир. ", 300).getBytes(Charset.forName("windows-1251")));
        assertEquals(1, PageEstimate.sniff(cp1251, 1.8), 0.001);
    }

    @Test
    public void theLanguageGivesTheBytesOfALetter() {
        assertEquals(1, PageEstimate.bytesPerChar("en", "Война"), 0.001);
        assertEquals(1.8, PageEstimate.bytesPerChar("ru-RU", null), 0.001);
        assertEquals(2.6, PageEstimate.bytesPerChar("th", null), 0.001);
        // Three bytes, but a character as much of a page as three letters
        assertEquals(1, PageEstimate.bytesPerChar("zh", null), 0.001);
        assertEquals(1.8, PageEstimate.bytesPerChar(null, "Ἰλιάς"), 0.001);
        assertEquals(1, PageEstimate.bytesPerChar(null, null), 0.001);
    }

    @Test
    public void onlyReflowableFormatsAreEstimated() {
        assertTrue(PageEstimate.isEstimated("/b/Book.EPUB"));
        assertTrue(PageEstimate.isEstimated("/b/book.fb2.zip"));
        assertTrue(PageEstimate.isEstimated("/b/book.azw3"));
        assertTrue(PageEstimate.isEstimated("/b/book.zip", "mobi"));
        assertFalse(PageEstimate.isEstimated("/b/book.zip", "pdf"));
        assertFalse(PageEstimate.isEstimated("/b/book.pdf"));
        assertFalse(PageEstimate.isEstimated("/b/book.djvu"));
        assertFalse(PageEstimate.isEstimated("/b/comic.cbz"));
        assertFalse(PageEstimate.isEstimated("/b/archive.zip"));
        assertEquals(0, PageEstimate.of("/no/such/book.epub", null, null));
    }

    /**
     * Reports the pages and the time of every book under the folder LIBRERA_PAGES_CORPUS names,
     * a folder per format. The language is told by the file name, as the scan tells it by the
     * title when the book gives none.
     */
    @Test
    public void corpus() throws Exception {
        String dir = System.getenv("LIBRERA_PAGES_CORPUS");
        if (dir == null) {
            return;
        }
        File[] formats = new File(dir).listFiles(File::isDirectory);
        Arrays.sort(formats);
        for (File format : formats) {
            File[] books = format.listFiles(File::isFile);
            Arrays.sort(books);
            long total = 0;
            for (File book : books) {
                long start = System.nanoTime();
                int pages = PageEstimate.of(book.getPath(), null, book.getName());
                total += System.nanoTime() - start;
                System.out.println(String.format(Locale.US, "%-5s %6d pages %8d KB  %s", format.getName(), pages, book.length() / 1024,
                        book.getName()));
            }
            System.out.println(String.format(Locale.US, "== %-5s %d books, %.1f ms", format.getName(), books.length, total / 1e6));
        }
    }
}
