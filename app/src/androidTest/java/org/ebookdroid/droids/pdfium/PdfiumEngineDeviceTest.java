package org.ebookdroid.droids.pdfium;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.PointF;
import android.graphics.RectF;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import com.foobnix.ext.CacheZipUtils;
import com.foobnix.model.AppProfile;
import com.foobnix.model.AppState;
import com.foobnix.pdf.info.AppsConfig;
import com.foobnix.pdf.info.model.AnnotationType;
import com.foobnix.sys.Colors;
import com.foobnix.sys.TempHolder;

import org.ebookdroid.BookType;
import org.ebookdroid.common.bitmaps.BitmapRef;
import org.ebookdroid.core.codec.AbstractCodecContext;
import org.ebookdroid.core.codec.Annotation;
import org.ebookdroid.core.codec.CodecContext;
import org.ebookdroid.core.codec.CodecDocument;
import org.ebookdroid.core.codec.CodecPage;
import org.ebookdroid.core.codec.CodecPageInfo;
import org.ebookdroid.core.codec.OutlineLink;
import org.ebookdroid.core.codec.PageLink;
import org.ebookdroid.droids.mupdf.codec.PdfContext;
import org.ebookdroid.droids.mupdf.codec.TextWord;
import org.ebookdroid.droids.pdfium.codec.PdfiumContext;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * The PDFium engine against MuPDF, on a device, with a PDF written by the test itself: the page's
 * size, its words and their places, its outline, its links and its pixels have to come out the
 * same whichever engine read them, since the rest of Librera cannot tell them apart.
 *
 * The PDF has one 200x100 page with "Hello World" near its top, a link in its lower left corner
 * and a one-entry outline.
 */
@RunWith(AndroidJUnit4.class)
public class PdfiumEngineDeviceTest {

    private static final int PAGE_WIDTH = 200;
    private static final int PAGE_HEIGHT = 100;
    private static final String TEXT = "Hello World";
    private static final String URL = "https://librera.mobi";
    private static final String CHAPTER = "Chapter One";

    /** The two engines read the same page, so their geometry may differ by this much of it. */
    private static final float TOLERANCE = 0.03f;

    /**
     * How far apart a word's top and bottom may be. The engines box a line differently: MuPDF
     * boxes it by the font's ascent and descent, PDFium tight around the glyphs on it, so
     * PDFium's box is the shorter one by up to about half the font size - 12pt of this 100pt
     * page. Left and right are the glyphs' own either way and have to agree closely.
     */
    private static final float HEIGHT_TOLERANCE = 0.12f;

    private File file;
    private int engineBefore;

    @Before
    public void setUp() throws Exception {
        final Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        AppsConfig.IS_LOG = false;
        // Touching AppsConfig loads libmupdf.so
        assertNotNull(AppsConfig.MUPDF_FZ_VERSION);
        CacheZipUtils.init(context);
        CacheZipUtils.createAllCacheDirs();
        // MuPDF reads the user's CSS and its accelerator path out of the profile
        AppProfile.init(context);
        TempHolder.get().loadingCancelled.set(false);
        engineBefore = AppState.get().pdfEngine;

        file = new File(context.getCacheDir(), "pdfium-engine-test.pdf");
        final OutputStream out = new FileOutputStream(file);
        try {
            out.write(onePagePdf());
        } finally {
            out.close();
        }
    }

    @After
    public void tearDown() {
        AppState.get().pdfEngine = engineBefore;
        if (file != null) {
            file.delete();
        }
    }

    // ---------------------------------------------------------------- the setting

    @Test
    public void theSettingPicksTheEngine() {
        AppState.get().pdfEngine = AppState.PDF_ENGINE_MUPDF;
        assertTrue("MuPDF by default", BookType.getCodecContextByPath(file.getPath()) instanceof PdfContext);

        AppState.get().pdfEngine = AppState.PDF_ENGINE_PDFIUM;
        final CodecContext chosen = BookType.getCodecContextByPath(file.getPath());
        assertTrue("PDFium once it is asked for, got " + chosen, chosen instanceof PdfiumContext);

        // .xps shares BookType.PDF but PDFium cannot read it
        assertTrue("XPS stays on MuPDF",
                BookType.getCodecContextByPath("/books/sample.xps") instanceof PdfContext);
        assertTrue("a request without a name stays on MuPDF",
                BookType.getCodecContextByType(BookType.PDF) instanceof PdfContext);
    }

    @Test
    public void pdfiumIsAvailable() {
        assertTrue("the PDFium native libraries are in the build", PdfiumContext.isAvailable());
    }

    // ---------------------------------------------------------------- the page

    @Test
    public void bothEnginesReadTheSamePage() {
        final CodecDocument mupdf = open(AppState.PDF_ENGINE_MUPDF);
        final CodecDocument pdfium = open(AppState.PDF_ENGINE_PDFIUM);
        try {
            assertEquals("page count", 1, mupdf.getPageCount());
            assertEquals("page count", 1, pdfium.getPageCount());

            final CodecPageInfo expected = mupdf.getPageInfo(0);
            final CodecPageInfo actual = pdfium.getPageInfo(0);
            assertNotNull(actual);
            assertEquals("page width", expected.width, actual.width);
            assertEquals("page height", expected.height, actual.height);
            assertEquals("page width is the MediaBox", PAGE_WIDTH, actual.width);
            assertEquals("page height is the MediaBox", PAGE_HEIGHT, actual.height);

            final CodecPage page = pdfium.getPage(0);
            assertNotNull(page);
            assertEquals(PAGE_WIDTH, page.getWidth());
            assertEquals(PAGE_HEIGHT, page.getHeight());
            assertFalse(page.isRecycled());
        } finally {
            mupdf.recycle();
            pdfium.recycle();
        }
    }

    @Test
    public void bothEnginesFindTheSameWordsInTheSamePlaces() {
        final CodecDocument mupdf = open(AppState.PDF_ENGINE_MUPDF);
        final CodecDocument pdfium = open(AppState.PDF_ENGINE_PDFIUM);
        try {
            final List<TextWord> expected = words(mupdf);
            final List<TextWord> actual = words(pdfium);

            assertEquals("the words PDFium found: " + join(actual), join(expected), join(actual));
            assertEquals("Hello World", join(actual));

            for (int i = 0; i < expected.size(); i++) {
                final TextWord e = expected.get(i);
                final TextWord a = actual.get(i);
                assertClose("left of '" + e.w + "'", e.left, a.left);
                assertClose("right of '" + e.w + "'", e.right, a.right);
                assertEquals("top of '" + e.w + "'", e.top, a.top, HEIGHT_TOLERANCE);
                assertEquals("bottom of '" + e.w + "'", e.bottom, a.bottom, HEIGHT_TOLERANCE);
                // PDFium's tighter box has to sit inside the line MuPDF reports, not beside it
                assertTrue("'" + e.w + "' is inside MuPDF's line: " + a + " vs " + e,
                        a.top >= e.top - TOLERANCE && a.bottom <= e.bottom + TOLERANCE);

                // Normalized to the page, and the text sits in its upper half
                assertTrue("'" + e.w + "' is on the page: " + a, a.left >= 0 && a.right <= 1);
                assertTrue("'" + e.w + "' is on the page: " + a, a.top >= 0 && a.bottom <= 1);
                assertTrue("'" + e.w + "' is near the top: " + a, a.bottom < 0.5f);
            }

            // The quad points of a markup annotation are taken from the original, in page space
            final RectF original = actual.get(0).getOriginal();
            assertNotNull("the word keeps its page-space rectangle", original);
            assertTrue("in points, not normalized: " + original, original.right > 1);
            assertTrue("inside the page: " + original, original.right <= PAGE_WIDTH + 1);
            assertTrue("inside the page: " + original, original.bottom <= PAGE_HEIGHT + 1);
        } finally {
            mupdf.recycle();
            pdfium.recycle();
        }
    }

    @Test
    public void pdfiumRendersThePage() {
        final CodecDocument pdfium = open(AppState.PDF_ENGINE_PDFIUM);
        try {
            final CodecPage page = pdfium.getPage(0);
            final BitmapRef ref = page.renderBitmap(400, 200, new RectF(0, 0, 1, 1), false);
            assertNotNull("the page was rendered", ref);
            final Bitmap bitmap = ref.getBitmap();
            assertEquals(400, bitmap.getWidth());
            assertEquals(200, bitmap.getHeight());

            // White paper with black text on it: both have to be there
            int dark = 0;
            int light = 0;
            for (int x = 0; x < bitmap.getWidth(); x += 2) {
                for (int y = 0; y < bitmap.getHeight(); y += 2) {
                    final int pixel = bitmap.getPixel(x, y);
                    assertEquals("the page is opaque", 255, Color.alpha(pixel));
                    if (Color.red(pixel) < 100) {
                        dark++;
                    } else if (Color.red(pixel) > 200) {
                        light++;
                    }
                }
            }
            assertTrue("the text was drawn, dark pixels: " + dark, dark > 20);
            assertTrue("the paper is white, light pixels: " + light, light > dark * 4);

            // The top left quarter is the slice the top left of the page is on
            final BitmapRef slice = page.renderBitmap(200, 100, new RectF(0, 0, 0.5f, 0.5f), false);
            assertNotNull(slice);
            assertEquals(200, slice.getBitmap().getWidth());
            assertEquals(100, slice.getBitmap().getHeight());
        } finally {
            pdfium.recycle();
        }
    }

    @Test
    public void pdfiumReadsTheOutline() {
        final CodecDocument pdfium = open(AppState.PDF_ENGINE_PDFIUM);
        try {
            final List<OutlineLink> outline = pdfium.getOutline();
            assertEquals("one chapter: " + outline, 1, outline.size());
            assertEquals(CHAPTER, outline.get(0).getTitle());
            assertEquals("the first page, 1-based", "#1", outline.get(0).getLink());
            assertEquals(0, outline.get(0).getLevel());
        } finally {
            pdfium.recycle();
        }
    }

    @Test
    public void pdfiumReadsTheLinks() {
        final CodecDocument pdfium = open(AppState.PDF_ENGINE_PDFIUM);
        try {
            final List<PageLink> links = pdfium.getPage(0).getPageLinks();
            PageLink found = null;
            for (PageLink link : links) {
                if (URL.equals(link.url)) {
                    found = link;
                }
            }
            assertNotNull("the link was found among " + links, found);
            assertNotNull(found.sourceRect);
            // /Rect [10 10 60 30] of a 200x100 page, y measured from the top
            assertClose("link left", 10f / PAGE_WIDTH, found.sourceRect.left);
            assertClose("link right", 60f / PAGE_WIDTH, found.sourceRect.right);
            assertClose("link top", 1f - 30f / PAGE_HEIGHT, found.sourceRect.top);
            assertClose("link bottom", 1f - 10f / PAGE_HEIGHT, found.sourceRect.bottom);
        } finally {
            pdfium.recycle();
        }
    }

    @Test
    public void pdfiumReadsThePageText() {
        final CodecDocument mupdf = open(AppState.PDF_ENGINE_MUPDF);
        final CodecDocument pdfium = open(AppState.PDF_ENGINE_PDFIUM);
        try {
            final String html = pdfium.getPage(0).getPageHTML();
            assertTrue("the text is in the page's html: " + html, html.contains(TEXT));
            assertTrue("a paragraph was written: " + html, html.contains("<p>") && html.contains("</p>"));
            assertTrue("MuPDF writes the same text", mupdf.getPage(0).getPageHTML().contains(TEXT));

            // Only PDFium's: MuPdfPage.getCharCount has no native behind it in the bundled libmupdf.so
            assertEquals("the characters of the page", TEXT.length(), pdfium.getPage(0).getCharCount());

            // What a bookmark keeps of the page it was made on
            assertEquals(TEXT, pdfium.getBookmarkText(1));
            assertEquals("the page is found back from a bookmark's text", 1,
                    pdfium.findBookmarkPage(1, TEXT, TEXT));
        } finally {
            mupdf.recycle();
            pdfium.recycle();
        }
    }

    @Test
    public void aFreshDocumentHasNoChanges() {
        final CodecDocument pdfium = open(AppState.PDF_ENGINE_PDFIUM);
        try {
            // Nothing was drawn, so the document is unchanged and MuPDF was never opened for it
            assertFalse(pdfium.hasChanges());
            assertNotNull("the page lists its annotations", pdfium.getPage(0).getAnnotations());
        } finally {
            pdfium.recycle();
        }
    }

    /**
     * Highlighting a word while PDFium is the engine: MuPDF writes the annotation, draws the page
     * from then on so it shows at once, and saves it into a file PDFium reads back.
     */
    @Test
    public void aHighlightIsWrittenAndSavedAndReadBack() {
        final File copy = new File(file.getParentFile(), "pdfium-engine-test-annotated.pdf");
        copy.delete();
        final CodecDocument pdfium = open(AppState.PDF_ENGINE_PDFIUM);
        try {
            final CodecPage page = pdfium.getPage(0);
            final RectF word = words(pdfium).get(0).getOriginal();
            assertNotNull(word);

            final int before = colored(page.renderBitmap(400, 200, new RectF(0, 0, 1, 1), false).getBitmap());

            // The quad points Librera builds from a selected word, in the page's own points
            final PointF[] quadPoints = {
                    new PointF(word.left, word.bottom),
                    new PointF(word.right, word.bottom),
                    new PointF(word.right, word.top),
                    new PointF(word.left, word.top),
            };
            page.addMarkupAnnotation(quadPoints, AnnotationType.HIGHLIGHT, Colors.toMupdfColor(Color.YELLOW));

            assertTrue("the document now has changes", pdfium.hasChanges());
            final List<Annotation> annotations = page.getAnnotations();
            assertEquals("the highlight is listed: " + annotations, 1, annotations.size());
            assertEquals(AnnotationType.HIGHLIGHT, annotations.get(0).type);
            assertTrue("the annotation knows its page", page.getPageHandle() != 0);
            assertEquals("and it is this page's handle", page.getPageHandle(),
                    annotations.get(0).getPageHandler());

            // The reader redraws the page after writing an annotation, which decodes it afresh;
            // the highlight is on it from then on, drawn by MuPDF while PDFium cannot see it
            page.recycle();
            final int after = colored(pdfium.getPage(0).renderBitmap(400, 200, new RectF(0, 0, 1, 1), false)
                                            .getBitmap());
            assertTrue("the highlight was drawn: " + before + " -> " + after, after > before + 100);

            pdfium.saveAnnotations(copy.getPath());
            assertTrue("the annotated copy was written", copy.isFile() && copy.length() > 0);
        } finally {
            pdfium.recycle();
        }

        // Read the saved file back with PDFium alone: it draws the annotations a document carries
        AppState.get().pdfEngine = AppState.PDF_ENGINE_PDFIUM;
        final CodecContext context = BookType.getCodecContextByPath(copy.getPath());
        assertTrue(context instanceof PdfiumContext);
        final CodecDocument reopened = context.openDocument(copy.getPath(), "");
        try {
            assertNotNull("PDFium opened the saved copy", reopened);
            assertFalse("and sees nothing left to save", reopened.hasChanges());
            final Bitmap bitmap = reopened.getPage(0).renderBitmap(400, 200, new RectF(0, 0, 1, 1), false)
                                          .getBitmap();
            assertTrue("PDFium draws the saved highlight", colored(bitmap) > 100);
        } finally {
            if (reopened != null) {
                reopened.recycle();
            }
            copy.delete();
        }
    }

    /** Pixels that are neither black nor white: the yellow of a highlight. */
    private static int colored(final Bitmap bitmap) {
        int count = 0;
        for (int x = 0; x < bitmap.getWidth(); x += 2) {
            for (int y = 0; y < bitmap.getHeight(); y += 2) {
                final int pixel = bitmap.getPixel(x, y);
                final int r = Color.red(pixel);
                final int g = Color.green(pixel);
                final int b = Color.blue(pixel);
                if (Math.abs(r - b) > 40 || Math.abs(g - b) > 40) {
                    count++;
                }
            }
        }
        return count;
    }

    // ---------------------------------------------------------------- helpers

    private CodecDocument open(final int engine) {
        AppState.get().pdfEngine = engine;
        final CodecContext context = BookType.getCodecContextByPath(file.getPath());
        assertNotNull("a context for engine " + engine, context);
        CodecDocument document = context.openDocument(file.getPath(), "");
        if (document == null) {
            // openDocument swallows what went wrong; ask the engine again to see it
            String why = "no exception";
            try {
                ((AbstractCodecContext) context).openDocumentInner(file.getPath(), "");
            } catch (Throwable e) {
                why = e.toString();
            }
            assertNotNull("the document was opened by engine " + engine + ": " + why, document);
        }
        return document;
    }

    /** The page's words, skipping the spaces both engines report as words of their own. */
    private static List<TextWord> words(final CodecDocument document) {
        final List<TextWord> words = new ArrayList<TextWord>();
        final TextWord[][] lines = document.getPage(0).getText();
        assertNotNull(lines);
        for (TextWord[] line : lines) {
            for (TextWord word : line) {
                if (word.w != null && word.w.trim().length() > 0) {
                    words.add(word);
                }
            }
        }
        return words;
    }

    private static String join(final List<TextWord> words) {
        final StringBuilder out = new StringBuilder();
        for (TextWord word : words) {
            if (out.length() > 0) {
                out.append(' ');
            }
            out.append(word.w);
        }
        return out.toString();
    }

    private static void assertClose(final String what, final float expected, final float actual) {
        assertEquals(what, expected, actual, TOLERANCE);
    }

    /**
     * A PDF of one page with text, a web link and an outline, written out here so the test does
     * not depend on a book being pushed to the device.
     */
    private static byte[] onePagePdf() {
        final String content = "BT /F1 12 Tf 20 70 Td (" + TEXT + ") Tj ET\n";
        final List<String> objects = new ArrayList<String>();
        objects.add("<< /Type /Catalog /Pages 2 0 R /Outlines 6 0 R >>");
        objects.add("<< /Type /Pages /Kids [3 0 R] /Count 1 >>");
        objects.add("<< /Type /Page /Parent 2 0 R /MediaBox [0 0 " + PAGE_WIDTH + " " + PAGE_HEIGHT + "]"
                + " /Resources << /Font << /F1 5 0 R >> >> /Contents 4 0 R /Annots [8 0 R] >>");
        objects.add("<< /Length " + content.length() + " >>\nstream\n" + content + "endstream");
        objects.add("<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica >>");
        objects.add("<< /Type /Outlines /First 7 0 R /Last 7 0 R /Count 1 >>");
        objects.add("<< /Title (" + CHAPTER + ") /Parent 6 0 R /Dest [3 0 R /XYZ 0 " + PAGE_HEIGHT + " 0] >>");
        objects.add("<< /Type /Annot /Subtype /Link /Rect [10 10 60 30] /Border [0 0 0]"
                + " /A << /S /URI /URI (" + URL + ") >> >>");

        final StringBuilder out = new StringBuilder("%PDF-1.4\n");
        final int[] offsets = new int[objects.size()];
        for (int i = 0; i < objects.size(); i++) {
            offsets[i] = out.length();
            out.append(i + 1).append(" 0 obj\n").append(objects.get(i)).append("\nendobj\n");
        }
        final int xref = out.length();
        out.append("xref\n0 ").append(objects.size() + 1).append("\n");
        out.append("0000000000 65535 f \n");
        for (int offset : offsets) {
            out.append(String.format("%010d 00000 n \n", offset));
        }
        out.append("trailer\n<< /Size ").append(objects.size() + 1).append(" /Root 1 0 R >>\n");
        out.append("startxref\n").append(xref).append("\n%%EOF\n");
        // Every byte is ASCII, so the offsets above are byte offsets
        return out.toString().getBytes(StandardCharsets.ISO_8859_1);
    }
}
