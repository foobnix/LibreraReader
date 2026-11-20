package com.foobnix.ext;

import com.foobnix.android.utils.LOG;
import com.foobnix.ext.office.DocToEpub;
import com.foobnix.ext.office.DocxToEpub;
import com.foobnix.ext.office.RtfToEpub;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.MockedStatic;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import javax.xml.parsers.DocumentBuilderFactory;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

// .docx, .doc and .rtf to EPUB with Librera's own converters; the files in resources/office were
// made for this (python-docx, macOS textutil, and an RTF written by hand)
public class OfficeConvertersTest {

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

    @Test
    public void docx() throws Exception {
        final Map<String, byte[]> epub = convert("test.docx", "docx");
        final String text = chapters(epub);
        assertTrue(text, text.contains("<h1>Chapter One</h1>"));
        assertTrue(text, text.contains("<b>bold</b>"));
        assertTrue(text, text.contains("<i>italic</i>"));
        assertTrue(text, text.contains("<u> underlined</u>"));
        assertTrue(text, text.contains("<sub>2</sub>"));
        assertTrue(text, text.contains("<sup>2</sup>"));
        assertTrue(text, text.contains("&amp; &lt;escape&gt; &quot;quotes&quot;"));
        assertTrue(text, text.contains("<ul><li>First bullet</li><li>Second bullet</li></ul>"));
        assertTrue(text, text.contains("<ol><li>Numbered one</li></ol>"));
        assertTrue(text, text.contains("<h2>Sub section</h2>"));
        assertTrue(text, text.contains("<p class=\"center\">Centered Кирилиця ünïcödé</p>"));
        assertTrue(text, text.contains("<td><p>A1</p></td><td><p>B1</p></td>"));
        assertTrue(text, text.contains("<img src=\"images/image1.png\""));
        assertNotNull(epub.get("OEBPS/images/image1.png"));
        final String opf = new String(epub.get("OEBPS/content.opf"), "UTF-8");
        assertTrue(opf, opf.contains("<dc:title>Docx Test Title</dc:title>"));
        assertTrue(opf, opf.contains("<dc:creator>Test Author</dc:creator>"));
        final String nav = new String(epub.get("OEBPS/nav.xhtml"), "UTF-8");
        assertTrue(nav, nav.contains(">Chapter One<") && nav.contains(">Sub section<") && nav.contains(">Chapter Two<"));
    }

    @Test
    public void rtf() throws Exception {
        final Map<String, byte[]> epub = convert("test.rtf", "rtf");
        final String text = chapters(epub);
        assertTrue(text, text.contains("<h1>RTF Chapter One</h1>"));
        assertTrue(text, text.contains("<b>bold</b>"));
        assertTrue(text, text.contains("<i>italic</i>"));
        assertTrue(text, text.contains("<u>underlined</u>"));
        assertTrue(text, text.contains("<sub>2</sub>") && text.contains("<sup>2</sup>"));
        assertTrue(text, text.contains("<s>gone</s>"));
        // windows-1251 by the font's \fcharset204, and its unicode escapes
        assertTrue(text, text.contains("<p class=\"center\">Привіт and unicode ЖЖ.</p>"));
        assertTrue(text, text.contains("<a href=\"https://librera.mobi\">link text</a>"));
        assertTrue(text, text.contains("href=\"notes.xhtml#note1\">[1]</a>"));
        assertTrue(text, text.contains("<tr><td><p>Cell A1</p></td><td><p>Cell B1</p></td></tr>"));
        assertTrue(text, text.contains("<h2>Second Heading</h2>"));
        assertTrue(text, text.contains("<img src=\"images/image1.png\""));
        // the metafile copy of the picture is left out
        assertEquals(1, count(epub, "OEBPS/images/"));
        assertTrue(text, text.contains("Last paragraph {braces} and \\backslash."));
        assertFalse(text, text.contains("hidden"));
        final String notes = new String(epub.get("OEBPS/notes.xhtml"), "UTF-8");
        assertTrue(notes, notes.contains("The footnote text."));
        final String opf = new String(epub.get("OEBPS/content.opf"), "UTF-8");
        assertTrue(opf, opf.contains("<dc:title>RTF Test Title</dc:title>"));
        assertTrue(opf, opf.contains("<dc:creator>RTF Author</dc:creator>"));

        final File rtf = resource("test.rtf");
        final byte[] cover = RtfToEpub.coverImage(rtf.getPath());
        assertNotNull(cover);
        assertEquals((byte) 0x89, cover[0]);
    }

    @Test
    public void rtfFromTextEdit() throws Exception {
        final String text = chapters(convert("textutil.rtf", "rtf"));
        assertTrue(text, text.contains("Doc Heading One"));
        assertTrue(text, text.contains("<b>bold text</b>"));
        assertTrue(text, text.contains("Кирилиця"));
        assertTrue(text, text.contains("Cell B2"));
    }

    @Test
    public void doc() throws Exception {
        final Map<String, byte[]> epub = convert("test.doc", "doc");
        final String text = chapters(epub);
        assertTrue(text, text.contains("Doc Heading One"));
        assertTrue(text, text.contains("<b>bold text</b>"));
        assertTrue(text, text.contains("<i>italic text</i>"));
        assertTrue(text, text.contains("<u>underlined</u>"));
        assertTrue(text, text.contains("<sub>2</sub>") && text.contains("<sup>2</sup>"));
        assertTrue(text, text.contains("Centered Кирилиця line"));
        assertTrue(text, text.contains("Cell A1") && text.contains("Cell B2"));
        assertTrue(text, text.contains("<td>"));
        assertTrue(text, text.contains("Final paragraph &amp; end."));
    }

    @Test
    public void notAnOfficeFile() throws Exception {
        final File text = folder.newFile("plain.doc");
        final FileOutputStream out = new FileOutputStream(text);
        out.write("just text".getBytes("UTF-8"));
        out.close();
        assertFalse(DocToEpub.isDoc(text.getPath()));
        assertFalse(DocToEpub.convert(text.getPath(), folder.newFile("a.epub")));
        assertFalse(RtfToEpub.convert(text.getPath(), folder.newFile("b.epub")));
        assertFalse(DocxToEpub.convert(text.getPath(), folder.newFile("c.epub")));
    }

    // ---- helpers -----------------------------------------------------------------------------

    private Map<String, byte[]> convert(String name, String kind) throws Exception {
        final File source = resource(name);
        final File epub = folder.newFile(name + ".epub");
        final boolean converted;
        if ("docx".equals(kind)) {
            converted = DocxToEpub.convert(source.getPath(), epub);
        } else if ("doc".equals(kind)) {
            converted = DocToEpub.convert(source.getPath(), epub);
        } else {
            converted = RtfToEpub.convert(source.getPath(), epub);
        }
        assertTrue(name + " converted", converted);
        final Map<String, byte[]> entries = new LinkedHashMap<String, byte[]>();
        final ZipFile zip = new ZipFile(epub);
        try {
            final Enumeration<? extends ZipEntry> all = zip.entries();
            boolean first = true;
            while (all.hasMoreElements()) {
                final ZipEntry entry = all.nextElement();
                if (first) {
                    assertEquals("mimetype", entry.getName());
                    assertEquals(ZipEntry.STORED, entry.getMethod());
                    first = false;
                }
                entries.put(entry.getName(), read(zip.getInputStream(entry)));
            }
        } finally {
            zip.close();
        }
        // every document of the book is well formed XML, as foliate needs it
        final DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        for (Map.Entry<String, byte[]> entry : entries.entrySet()) {
            final String path = entry.getKey();
            if (path.endsWith(".xhtml") || path.endsWith(".opf") || path.endsWith(".ncx") || path.endsWith(".xml")) {
                factory.newDocumentBuilder().parse(new ByteArrayInputStream(entry.getValue()));
            }
        }
        return entries;
    }

    private static String chapters(Map<String, byte[]> epub) throws IOException {
        final StringBuilder text = new StringBuilder();
        for (Map.Entry<String, byte[]> entry : epub.entrySet()) {
            if (entry.getKey().startsWith("OEBPS/chapter")) {
                text.append(new String(entry.getValue(), "UTF-8"));
            }
        }
        return text.toString();
    }

    private static int count(Map<String, byte[]> epub, String prefix) {
        int n = 0;
        for (String name : epub.keySet()) {
            if (name.startsWith(prefix)) {
                n++;
            }
        }
        return n;
    }

    private static File resource(String name) {
        return new File(OfficeConvertersTest.class.getClassLoader().getResource("office/" + name).getFile());
    }

    private static byte[] read(InputStream in) throws IOException {
        try {
            final ByteArrayOutputStream out = new ByteArrayOutputStream();
            final byte[] buffer = new byte[8192];
            int n;
            while ((n = in.read(buffer)) > 0) {
                out.write(buffer, 0, n);
            }
            return out.toByteArray();
        } finally {
            in.close();
        }
    }
}
