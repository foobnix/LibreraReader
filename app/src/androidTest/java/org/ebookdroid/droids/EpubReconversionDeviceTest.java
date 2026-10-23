package org.ebookdroid.droids;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import android.content.Context;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import com.foobnix.ext.CacheZipUtils;
import com.foobnix.model.AppProfile;
import com.foobnix.model.AppSP;
import com.foobnix.model.AppState;
import com.foobnix.pdf.info.AppsConfig;
import com.foobnix.pdf.info.model.BookCSS;
import com.foobnix.sys.TempHolder;

import org.ebookdroid.core.codec.AbstractCodecContext;
import org.ebookdroid.core.codec.CodecDocument;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.CRC32;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Changing how a book looks must not convert it again.
 *
 * With hyphenation on, EpubContext rewrites the book with soft hyphens before an engine opens it
 * and keeps the result in the book cache, named after the settings the rewrite depends on. Every
 * style setting restarts the reader, which opens the book again; if a setting is part of that name
 * without the rewrite reading it, the whole book is converted again for nothing - seconds on a
 * long one in a debug build. This opens a book, then re-opens it after each style change the way
 * the restart does, and checks the same converted file is used.
 */
@RunWith(AndroidJUnit4.class)
public class EpubReconversionDeviceTest {

    /** A date no conversion writes, so a converted file that keeps it was not written again. */
    private static final long STAMP = 1_000_000_000_000L;

    private File book;
    private boolean hyphensBefore;
    private String langBefore;
    private int engineBefore;

    /** A style change: what it is, how to make it, and how to undo it. */
    private static final class Change {
        final String name;
        final Runnable make;
        final Runnable undo;

        Change(final String name, final Runnable make, final Runnable undo) {
            this.name = name;
            this.make = make;
            this.undo = undo;
        }
    }

    @Before
    public void setUp() throws Exception {
        final Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        AppsConfig.IS_LOG = false;
        CacheZipUtils.init(context);
        CacheZipUtils.createAllCacheDirs();
        AppProfile.init(context);
        TempHolder.get().loadingCancelled.set(false);
        hyphensBefore = BookCSS.get().isAutoHypens;
        langBefore = AppSP.get().hypenLang;
        engineBefore = AppState.get().epubEngine;
        BookCSS.get().isAutoHypens = true;
        AppSP.get().hypenLang = "en";
        AppState.get().epubEngine = AppState.EPUB_ENGINE_MUPDF;

        book = new File(context.getCacheDir(), "reconversion-test.epub");
        final OutputStream out = new FileOutputStream(book);
        try {
            out.write(epub());
        } finally {
            out.close();
        }
    }

    @After
    public void tearDown() {
        BookCSS.get().isAutoHypens = hyphensBefore;
        AppSP.get().hypenLang = langBefore;
        AppState.get().epubEngine = engineBefore;
        if (book != null) {
            book.delete();
        }
    }

    @Test
    public void aStyleChangeReusesTheConvertedBook() {
        final File converted = openAndStamp();
        final BookCSS css = BookCSS.get();
        final AppState app = AppState.get();
        final List<Change> changes = new ArrayList<Change>();

        final int fontSize = css.fontSizeSp;
        changes.add(new Change("font size", () -> css.fontSizeSp = fontSize + 2, () -> css.fontSizeSp = fontSize));
        final int foliateSize = css.fontSizeSpFoliate;
        changes.add(new Change("foliate font size", () -> css.fontSizeSpFoliate = foliateSize + 2,
                () -> css.fontSizeSpFoliate = foliateSize));
        final String font = css.normalFont;
        changes.add(new Change("font", () -> css.normalFont = "/system/fonts/NotoSerif-Regular.ttf",
                () -> css.normalFont = font));
        final int lineHeight = css.lineHeight12;
        changes.add(new Change("line height", () -> css.lineHeight12 = lineHeight + 2,
                () -> css.lineHeight12 = lineHeight));
        final int marginLeft = css.marginLeft;
        changes.add(new Change("side margin", () -> css.marginLeft = marginLeft + 3, () -> css.marginLeft = marginLeft));
        final int marginTop = css.marginTop;
        changes.add(new Change("top margin", () -> css.marginTop = marginTop + 3, () -> css.marginTop = marginTop));
        final int indent = css.textIndent;
        changes.add(new Change("first-line indent", () -> css.textIndent = indent + 2, () -> css.textIndent = indent));
        final int paragraph = css.paragraphHeight;
        changes.add(new Change("paragraph spacing", () -> css.paragraphHeight = paragraph + 2,
                () -> css.paragraphHeight = paragraph));
        final int emptyLine = css.emptyLine;
        changes.add(new Change("empty line", () -> css.emptyLine = emptyLine + 1, () -> css.emptyLine = emptyLine));
        final int weight = css.fontWeight;
        changes.add(new Change("font weight", () -> css.fontWeight = weight == 700 ? 400 : 700,
                () -> css.fontWeight = weight));
        final int align = css.textAlign;
        changes.add(new Change("text align", () -> css.textAlign = (align + 1) % 4, () -> css.textAlign = align));
        final String custom = css.customCSS2;
        changes.add(new Change("custom CSS", () -> css.customCSS2 = "p{color:#123456}", () -> css.customCSS2 = custom));
        final String links = css.linkColorDays;
        changes.add(new Change("link colour", () -> css.linkColorDays = "#123456", () -> css.linkColorDays = links));
        final boolean capital = css.isCapitalLetter;
        changes.add(new Change("capital letter", () -> css.isCapitalLetter = !capital,
                () -> css.isCapitalLetter = capital));
        final float imageScale = css.imageScale;
        changes.add(new Change("image scale", () -> css.imageScale = imageScale + 0.5f,
                () -> css.imageScale = imageScale));
        final boolean day = app.isDayNotInvert;
        changes.add(new Change("night mode", () -> app.isDayNotInvert = !day, () -> app.isDayNotInvert = day));
        final int fullScreen = app.fullScreenMode;
        changes.add(new Change("full screen", () -> app.fullScreenMode = fullScreen == AppState.FULL_SCREEN_NORMAL
                ? AppState.FULL_SCREEN_FULLSCREEN : AppState.FULL_SCREEN_NORMAL, () -> app.fullScreenMode = fullScreen));
        final boolean scaleImages = app.enableImageScale;
        changes.add(new Change("image scaling on/off", () -> app.enableImageScale = !scaleImages,
                () -> app.enableImageScale = scaleImages));

        final List<String> converting = new ArrayList<String>();
        for (Change change : changes) {
            change.make.run();
            try {
                open();
                if (!reused(converted)) {
                    converting.add(change.name);
                    // Back to the book this started from, for the next change
                    change.undo.run();
                    openAndStamp();
                    continue;
                }
            } finally {
                change.undo.run();
            }
        }
        assertEquals("style changes that converted the book again", "[]", converting.toString());
    }

    @Test
    public void aSettingTheConversionReadsConvertsItAgain() {
        // The other side of the check above: the test can see a conversion when there is one
        final File converted = openAndStamp();
        AppSP.get().hypenLang = "de";
        try {
            open();
            assertTrue("hyphenating in another language converts the book again", !reused(converted));
        } finally {
            AppSP.get().hypenLang = "en";
        }
    }

    /** Opens the book as the reader does, and marks the converted copy it left. */
    private File openAndStamp() {
        open();
        final File converted = convertedFile();
        assertTrue("the book was converted: " + converted, converted.isFile());
        assertTrue(converted.setLastModified(STAMP));
        return converted;
    }

    /** Whether the converted copy now named by the settings is the one stamped, untouched. */
    private boolean reused(final File stamped) {
        final File now = convertedFile();
        return now.equals(stamped) && now.isFile() && now.lastModified() == STAMP;
    }

    private File convertedFile() {
        return new EpubContext().getCacheFileName(book.getPath() + AbstractCodecContext.getFileNameSalt(book.getPath()));
    }

    /** What restarting the reader does with the book: a fresh context opening it. */
    private void open() {
        final CodecDocument document = new EpubContext().openDocument(book.getPath(), "");
        assertNotNull("the book opened", document);
        document.recycle();
    }

    /** A small EPUB with words long enough to be hyphenated. */
    private static byte[] epub() throws Exception {
        final ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        final ZipOutputStream zip = new ZipOutputStream(bytes);
        final byte[] mimetype = "application/epub+zip".getBytes(StandardCharsets.US_ASCII);
        final ZipEntry entry = new ZipEntry("mimetype");
        entry.setMethod(ZipEntry.STORED);
        entry.setSize(mimetype.length);
        entry.setCompressedSize(mimetype.length);
        final CRC32 crc = new CRC32();
        crc.update(mimetype);
        entry.setCrc(crc.getValue());
        zip.putNextEntry(entry);
        zip.write(mimetype);
        zip.closeEntry();
        write(zip, "META-INF/container.xml", "<?xml version=\"1.0\"?>"
                + "<container version=\"1.0\" xmlns=\"urn:oasis:names:tc:opendocument:xmlns:container\">"
                + "<rootfiles><rootfile full-path=\"OEBPS/content.opf\""
                + " media-type=\"application/oebps-package+xml\"/></rootfiles></container>");
        write(zip, "OEBPS/content.opf", "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                + "<package xmlns=\"http://www.idpf.org/2007/opf\" version=\"2.0\" unique-identifier=\"id\">"
                + "<metadata xmlns:dc=\"http://purl.org/dc/elements/1.1/\"><dc:title>Reconversion</dc:title>"
                + "<dc:language>en</dc:language><dc:identifier id=\"id\">reconversion</dc:identifier></metadata>"
                + "<manifest><item id=\"c1\" href=\"chapter1.xhtml\" media-type=\"application/xhtml+xml\"/></manifest>"
                + "<spine><itemref idref=\"c1\"/></spine></package>");
        final StringBuilder text = new StringBuilder();
        for (int i = 0; i < 40; i++) {
            text.append("<p>Extraordinarily comprehensive documentation accompanies the international ")
                    .append("collaboration, notwithstanding unquestionably complicated circumstances.</p>");
        }
        write(zip, "OEBPS/chapter1.xhtml", "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                + "<html xmlns=\"http://www.w3.org/1999/xhtml\"><head><title>One</title></head><body>"
                + "<h1>Chapter One</h1>" + text + "</body></html>");
        zip.close();
        return bytes.toByteArray();
    }

    private static void write(final ZipOutputStream zip, final String name, final String content) throws Exception {
        zip.putNextEntry(new ZipEntry(name));
        zip.write(content.getBytes(StandardCharsets.UTF_8));
        zip.closeEntry();
    }
}
