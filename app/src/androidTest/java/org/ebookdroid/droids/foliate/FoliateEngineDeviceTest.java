package org.ebookdroid.droids.foliate;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.RectF;
import android.os.Handler;
import android.os.Looper;

import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import com.foobnix.android.utils.Dips;
import com.foobnix.ext.CacheZipUtils;
import com.foobnix.model.AppProfile;
import com.foobnix.model.AppState;
import com.foobnix.model.AppAnnotation;
import com.foobnix.pdf.info.AnnotationsData;
import com.foobnix.pdf.info.model.AnnotationType;
import com.foobnix.pdf.info.AnnotationsFinder;
import com.foobnix.pdf.info.AppsConfig;
import com.foobnix.pdf.info.model.BookCSS;
import com.foobnix.sys.TempHolder;

import org.ebookdroid.BookType;
import org.ebookdroid.common.bitmaps.BitmapRef;
import org.ebookdroid.core.codec.CodecContext;
import org.ebookdroid.core.codec.CodecDocument;
import org.ebookdroid.core.codec.CodecPage;
import org.ebookdroid.core.codec.CodecPageInfo;
import org.ebookdroid.core.codec.OutlineLink;
import org.ebookdroid.core.codec.PageLink;
import org.ebookdroid.droids.EpubContext;
import org.ebookdroid.droids.foliate.codec.FoliateContext;
import org.ebookdroid.droids.foliate.codec.FoliateDocument;
import org.ebookdroid.droids.mupdf.codec.TextWord;
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
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * The foliate-js engine against MuPDF, on a device, with an EPUB the test writes itself.
 *
 * It has three chapters whose text is known, a table of contents and an internal link, so the page
 * index, the rendering, the words, the page text, the contents and the bookmark anchor can all be
 * checked against something exact.
 */
@RunWith(AndroidJUnit4.class)
public class FoliateEngineDeviceTest {

    // A real screen's worth of page: with a small one the reader's own margins take most of
    // the width and a heading no longer fits the column, which is a layout question rather
    // than an engine one.
    private static final int PAGE_WIDTH = 1080;
    private static final int PAGE_HEIGHT = 1800;

    private static final String[] TITLES = {"Chapter One", "Chapter Two", "Chapter Three"};

    /** A phrase per chapter, each unique, so a page can be identified by its text. */
    private static final String[] PHRASES = {
            "The first chapter speaks of beginnings and of carefully numbered pages.",
            "The second chapter speaks of middles and of words that must be selectable.",
            "The third chapter speaks of endings and of a contents list that resolves.",
    };

    /** The engine needs an activity on screen to attach its WebView to; see FoliateHostActivity. */
    private ActivityScenario<FoliateHostActivity> host;

    private File file;
    private int engineBefore;
    private int fontBefore;
    private boolean baseCssBefore;

    @Before
    public void setUp() throws Exception {
        final Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        AppsConfig.IS_LOG = true;
        assertNotNull(AppsConfig.MUPDF_FZ_VERSION);
        CacheZipUtils.init(context);
        CacheZipUtils.createAllCacheDirs();
        AppProfile.init(context);
        TempHolder.get().loadingCancelled.set(false);

        host = ActivityScenario.launch(FoliateHostActivity.class);
        // The engine reads the activity from Application.registerActivityLifecycleCallbacks, so
        // the launch has to have been processed before a book is opened.
        InstrumentationRegistry.getInstrumentation().waitForIdleSync();

        engineBefore = AppState.get().epubEngine;
        fontBefore = BookCSS.get().fontSizeSp;
        baseCssBefore = AppState.get().epubFoliateBaseCss;

        file = new File(context.getCacheDir(), "foliate-engine-test.epub");
        final OutputStream out = new FileOutputStream(file);
        try {
            out.write(epub());
        } finally {
            out.close();
        }
    }

    @After
    public void tearDown() {
        AppState.get().epubEngine = engineBefore;
        BookCSS.get().fontSizeSp = fontBefore;
        AppState.get().epubFoliateBaseCss = baseCssBefore;
        if (file != null) {
            file.delete();
        }
        if (host != null) {
            host.close();
        }
    }

    // ---------------------------------------------------------------- the setting

    @Test
    public void theSettingPicksTheEngine() {
        AppState.get().epubEngine = AppState.EPUB_ENGINE_MUPDF;
        CodecDocument document = open();
        try {
            assertFalse("MuPDF by default, got " + document, document instanceof FoliateDocument);
        } finally {
            document.recycle();
        }

        AppState.get().epubEngine = AppState.EPUB_ENGINE_FOLIATE;
        document = open();
        try {
            assertTrue("foliate once it is asked for, got " + document,
                    document instanceof FoliateDocument);
        } finally {
            document.recycle();
        }
    }

    @Test
    public void foliateIsAvailable() {
        assertTrue("a WebView is available", FoliateContext.isAvailable());
    }

    // ---------------------------------------------------------------- the page index

    @Test
    public void thePageCountIsRealAndCached() {
        CodecDocument document = open(AppState.EPUB_ENGINE_FOLIATE);
        final int pages;
        try {
            pages = document.getPageCount(PAGE_WIDTH, PAGE_HEIGHT, BookCSS.get().fontSizeSp);
            // Three chapters, so at least three pages, and a real number rather than an estimate
            assertTrue("a page per chapter at least, got " + pages, pages >= TITLES.length);
            assertTrue("and not an absurd number, got " + pages, pages < 1000);

            // Every page of a reflowable book is the viewport
            final CodecPageInfo info = document.getPageInfo(0);
            assertNotNull(info);
            assertEquals(PAGE_WIDTH, info.width);
            assertEquals(PAGE_HEIGHT, info.height);
            assertEquals(PAGE_WIDTH, document.getUnifiedPageInfo().width);
        } finally {
            document.recycle();
        }

        // Re-opening the same book at the same size reads the index back instead of walking it
        document = open(AppState.EPUB_ENGINE_FOLIATE);
        try {
            final long started = System.currentTimeMillis();
            final int again = document.getPageCount(PAGE_WIDTH, PAGE_HEIGHT, BookCSS.get().fontSizeSp);
            final long took = System.currentTimeMillis() - started;
            assertEquals("the same count on the second open", pages, again);
            assertTrue("and it came from the cache, took " + took + "ms", took < 3000);
        } finally {
            document.recycle();
        }
    }

    @Test
    public void aBiggerFontMeansMorePages() {
        CodecDocument document = open(AppState.EPUB_ENGINE_FOLIATE);
        final int small;
        try {
            BookCSS.get().fontSizeSp = 14;
            small = document.getPageCount(PAGE_WIDTH, PAGE_HEIGHT, BookCSS.get().fontSizeSp);
        } finally {
            document.recycle();
        }

        BookCSS.get().fontSizeSp = 34;
        document = open(AppState.EPUB_ENGINE_FOLIATE);
        try {
            final int large = document.getPageCount(PAGE_WIDTH, PAGE_HEIGHT, BookCSS.get().fontSizeSp);
            assertTrue("a larger font paginates into more pages: " + small + " -> " + large,
                    large > small);
        } finally {
            document.recycle();
        }
    }

    // ---------------------------------------------------------------- rendering

    /**
     * No page of a chapter's text draws as nothing. A page drawn before the WebView had laid it
     * out is blank, and in paged mode that blank is cached and never drawn again - which is how
     * it reaches the reader.
     */
    @Test
    public void noPageOfTextDrawsBlank() {
        final CodecDocument document = open(AppState.EPUB_ENGINE_FOLIATE);
        try {
            final int pages = document.getPageCount(PAGE_WIDTH, PAGE_HEIGHT, BookCSS.get().fontSizeSp);
            int blank = 0;
            final int checked = Math.min(pages, 12);
            for (int page = 0; page < checked; page++) {
                final CodecPage codecPage = document.getPageInner(page);
                assertNotNull("page " + page + " exists", codecPage);
                try {
                    final BitmapRef ref = codecPage.renderBitmap(PAGE_WIDTH / 2, PAGE_HEIGHT / 2,
                            new RectF(0, 0, 1, 1), false);
                    assertNotNull("page " + page + " was drawn", ref);
                    final Bitmap bitmap = ref.getBitmap();
                    final int first = bitmap.getPixel(0, 0);
                    boolean uniform = true;
                    for (int y = 0; y < bitmap.getHeight() && uniform; y += 4) {
                        for (int x = 0; x < bitmap.getWidth(); x += 4) {
                            if (bitmap.getPixel(x, y) != first) {
                                uniform = false;
                                break;
                            }
                        }
                    }
                    if (uniform) {
                        blank++;
                    }
                } finally {
                    codecPage.recycle();
                }
            }
            // Every page of this book carries text, so none of them may come out empty
            assertEquals("pages that drew as nothing, of " + checked, 0, blank);
        } finally {
            document.recycle();
        }
    }

    @Test
    public void pagesRender() {
        final CodecDocument document = open(AppState.EPUB_ENGINE_FOLIATE);
        try {
            document.getPageCount(PAGE_WIDTH, PAGE_HEIGHT, BookCSS.get().fontSizeSp);
            final CodecPage page = document.getPage(0);
            assertNotNull(page);

            final BitmapRef ref = page.renderBitmap(PAGE_WIDTH, PAGE_HEIGHT, new RectF(0, 0, 1, 1), false);
            assertNotNull("the page was rendered", ref);
            final Bitmap bitmap = ref.getBitmap();
            assertEquals(PAGE_WIDTH, bitmap.getWidth());
            assertEquals(PAGE_HEIGHT, bitmap.getHeight());

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
            assertTrue("the page is light, light pixels: " + light, light > dark);

            // A slice is cut from the same capture rather than laid out again
            final BitmapRef slice = page.renderBitmap(PAGE_WIDTH, PAGE_HEIGHT / 2,
                    new RectF(0, 0, 1f, 0.5f), false);
            assertNotNull(slice);
            assertEquals(PAGE_WIDTH, slice.getBitmap().getWidth());
            assertEquals(PAGE_HEIGHT / 2, slice.getBitmap().getHeight());
        } finally {
            document.recycle();
        }
    }

    /**
     * A page is the page that was asked for, whoever else is reading the book at the time.
     *
     * There is one book in one WebView, so positioning it and drawing it have to be one
     * operation. In the app the reader draws pages on one thread while TTS reads a page's text,
     * the search walks the pages and the reader asks for the next page's links - and a page drawn
     * after something else has moved the book is the wrong page, or a blank one. Paged mode shows
     * it more than scroll mode because its bitmaps are cached and never redrawn.
     */
    @Test
    public void pagesAreDrawnWhileOthersReadTheBook() throws Exception {
        final CodecDocument document = open(AppState.EPUB_ENGINE_FOLIATE);
        try {
            final int pages = document.getPageCount(PAGE_WIDTH, PAGE_HEIGHT, BookCSS.get().fontSizeSp);
            final int count = Math.min(5, pages);

            // What each page looks like with nothing else going on
            final List<String> expected = new ArrayList<String>();
            for (int page = 0; page < count; page++) {
                expected.add(fingerprint(document, page));
            }

            // The same pages again, while another thread walks the book reading text - which is
            // what TTS and the search do
            final CountDownLatch stop = new CountDownLatch(1);
            final Thread reader = new Thread("@T foliate test reader") {
                @Override
                public void run() {
                    int page = 0;
                    while (stop.getCount() > 0) {
                        document.getPage(page % Math.max(1, pages)).getPageHTML();
                        page++;
                    }
                }
            };
            reader.start();
            try {
                for (int round = 0; round < 2; round++) {
                    for (int page = 0; page < count; page++) {
                        assertEquals("page " + page + " is itself, round " + round,
                                expected.get(page), fingerprint(document, page));
                    }
                }
            } finally {
                stop.countDown();
                reader.join(20000);
            }
        } finally {
            document.recycle();
        }
    }

    /** A page's pixels, reduced to something two renders of the same page must agree on. */
    private static String fingerprint(final CodecDocument document, final int page) {
        final CodecPage codecPage = document.getPageInner(page);
        assertNotNull(codecPage);
        try {
            final BitmapRef ref = codecPage.renderBitmap(PAGE_WIDTH / 4, PAGE_HEIGHT / 4,
                    new RectF(0, 0, 1, 1), false);
            assertNotNull("page " + page + " was drawn", ref);
            final Bitmap bitmap = ref.getBitmap();
            // A coarse signature: how dark each band of the page is. Two renders of one page
            // agree on it; two different pages, or a blank one, do not.
            final StringBuilder out = new StringBuilder();
            final int bands = 8;
            for (int band = 0; band < bands; band++) {
                long ink = 0;
                final int from = bitmap.getHeight() * band / bands;
                final int to = bitmap.getHeight() * (band + 1) / bands;
                for (int y = from; y < to; y += 2) {
                    for (int x = 0; x < bitmap.getWidth(); x += 2) {
                        if (Color.red(bitmap.getPixel(x, y)) < 128) {
                            ink++;
                        }
                    }
                }
                out.append(ink / 8).append(':');
            }
            return out.toString();
        } finally {
            codecPage.recycle();
        }
    }

    // ---------------------------------------------------------------- the text layer

    @Test
    public void wordsAreSelectableAndOnThePage() {
        final CodecDocument document = open(AppState.EPUB_ENGINE_FOLIATE);
        try {
            document.getPageCount(PAGE_WIDTH, PAGE_HEIGHT, BookCSS.get().fontSizeSp);
            final CodecPage page = document.getPage(0);
            final TextWord[][] text = page.getText();
            assertNotNull(text);
            assertTrue("the page has a line of words", text.length > 0);

            final StringBuilder joined = new StringBuilder();
            int words = 0;
            for (TextWord[] line : text) {
                for (TextWord word : line) {
                    if (word.w != null && word.w.trim().length() > 0) {
                        words++;
                        joined.append(word.w).append(' ');
                    }
                    assertTrue("'" + word.w + "' is normalized: " + word,
                            word.left >= 0 && word.right <= 1 && word.top >= 0 && word.bottom <= 1);
                    assertNotNull("a word keeps its page rectangle", word.getOriginal());
                }
            }
            assertTrue("the first page has words, got " + words, words > 3);

            // A rectangle that crosses a line break is the union of both lines - as wide as the
            // column and twice as tall - and draws a selection as a block over the text instead
            // of a highlight on it. Every word has to sit on one line.
            final List<Float> heights = new ArrayList<Float>();
            for (TextWord[] line : text) {
                for (TextWord word : line) {
                    heights.add(word.height());
                }
            }
            java.util.Collections.sort(heights);
            final float median = heights.get(heights.size() / 2);
            assertTrue("the words have a height at all: " + median, median > 0);
            for (TextWord[] line : text) {
                for (TextWord word : line) {
                    assertTrue("'" + word.w + "' is one line tall: " + word.height()
                                    + " against a median of " + median + " " + word,
                            word.height() < median * 2.5f);
                    assertTrue("'" + word.w + "' is not a band across the page: " + word,
                            word.width() < 0.95f);
                    if (word.w != null && word.w.trim().isEmpty()) {
                        // The space a line wraps on runs from the last word to the margin; drawn,
                        // it is a band down the right of every selected line
                        assertTrue("the space is a space, not the rest of the line: " + word,
                                word.width() * PAGE_WIDTH < 3 * word.height() * PAGE_HEIGHT);
                    }
                }
            }
            final String all = joined.toString();
            assertTrue("the first chapter's words are on its first page: " + all,
                    all.contains("first") || all.contains("beginnings") || all.contains(TITLES[0].split(" ")[0]));
        } finally {
            document.recycle();
        }
    }

    /**
     * Every word of the page is selectable - not just every word that is selectable is a word.
     *
     * The letters of a page's words, run together, have to be the letters of its text. A word
     * dropped from the selection is invisible to any other check: it simply is not there, and
     * everything that is there is still correct. A word broken across lines comes back as its two
     * halves, which is why this compares the letters rather than the words.
     */
    @Test
    public void everyWordOfThePageIsSelectable() {
        final CodecDocument document = open(AppState.EPUB_ENGINE_FOLIATE);
        try {
            final int pages = document.getPageCount(PAGE_WIDTH, PAGE_HEIGHT, BookCSS.get().fontSizeSp);
            int compared = 0;
            for (int page = 0; page < Math.min(pages, 8); page++) {
                final CodecPage codecPage = document.getPageInner(page);
                assertNotNull(codecPage);
                try {
                    final StringBuilder selectable = new StringBuilder();
                    for (TextWord[] line : codecPage.getText()) {
                        for (TextWord word : line) {
                            if (word.w != null) {
                                selectable.append(word.w);
                            }
                        }
                    }
                    final String fromWords = letters(selectable.toString());
                    final String fromText = letters(stripTags(codecPage.getPageHTML()));
                    if (fromText.isEmpty()) {
                        continue;
                    }
                    compared++;
                    assertEquals("page " + page + " has a word that cannot be selected",
                            fromText, fromWords);
                } finally {
                    codecPage.recycle();
                }
            }
            assertTrue("there were pages to compare, got " + compared, compared > 3);
        } finally {
            document.recycle();
        }
    }

    /** The page's pseudo-HTML as plain text. */
    private static String stripTags(final String html) {
        return html == null ? "" : html
                .replaceAll("<[^>]*>", " ")
                .replace("&lt;", "<")
                .replace("&gt;", ">")
                .replace("&quot;", "\"")
                .replace("&amp;", "&");
    }

    /** Just the letters, so whitespace and the hyphen a break draws cannot make a difference. */
    private static String letters(final String text) {
        final StringBuilder out = new StringBuilder();
        for (int i = 0; i < text.length(); i++) {
            final char c = text.charAt(i);
            if (!Character.isWhitespace(c) && c != '\u00AD') {
                out.append(c);
            }
        }
        return out.toString();
    }

    @Test
    public void thePageTextIsReadableByTts() {
        final CodecDocument document = open(AppState.EPUB_ENGINE_FOLIATE);
        try {
            document.getPageCount(PAGE_WIDTH, PAGE_HEIGHT, BookCSS.get().fontSizeSp);
            final String html = document.getPage(0).getPageHTML();
            assertNotNull(html);
            assertTrue("a paragraph was written: " + html, html.contains("<p>") && html.contains("</p>"));
            assertTrue("with the chapter's own text: " + html,
                    html.contains("beginnings") || html.contains("first chapter"));
        } finally {
            document.recycle();
        }
    }

    @Test
    public void linksAreFoundOnTheirPage() {
        final CodecDocument document = open(AppState.EPUB_ENGINE_FOLIATE);
        try {
            final int pages = document.getPageCount(PAGE_WIDTH, PAGE_HEIGHT, BookCSS.get().fontSizeSp);
            // The two links are at the end of the first chapter, so they are on one of its pages
            final int firstOfSecond = Integer.parseInt(
                    document.getOutline().get(1).getLink().substring(1));
            PageLink internal = null;
            PageLink external = null;
            for (int page = 0; page < Math.min(pages, firstOfSecond); page++) {
                for (PageLink link : document.getPage(page).getPageLinks()) {
                    assertNotNull(link.sourceRect);
                    assertTrue("the link is on the page: " + link.sourceRect,
                            link.sourceRect.left >= 0 && link.sourceRect.right <= 1
                                    && link.sourceRect.top >= 0 && link.sourceRect.bottom <= 1);
                    if ("https://librera.mobi".equals(link.url)) {
                        external = link;
                    } else if (link.targetPage >= 0) {
                        internal = link;
                    }
                }
            }
            assertNotNull("the web link was found", external);
            assertNotNull("the link to the third chapter was found", internal);
            final int thirdChapter = Integer.parseInt(
                    document.getOutline().get(2).getLink().substring(1));
            // targetPage is 0-based, the outline's page is 1-based
            assertEquals("and it points at that chapter", thirdChapter - 1, internal.targetPage);
        } finally {
            document.recycle();
        }
    }

    /**
     * Only Librera's hyphens. A soft hyphen is Librera asking for a word to be breakable there,
     * and the engine must break nowhere else - so every word on the page is either a word of the
     * book or one of the halves its soft hyphens allow, never an arbitrary piece. The book asks
     * for <code>hyphens:auto</code>, so nothing but the override stops the browser obliging.
     */
    @Test
    public void onlyLibrerasOwnHyphensApply() {
        final boolean hyphensBefore = BookCSS.get().isAutoHypens;
        BookCSS.get().isAutoHypens = false;
        try {
            final CodecDocument document = open(AppState.EPUB_ENGINE_FOLIATE);
            try {
                final int pages = document.getPageCount(PAGE_WIDTH, PAGE_HEIGHT,
                        BookCSS.get().fontSizeSp);
                // Every word of the book, plus the halves of the ones it allows to be broken.
                // A soft hyphen is Librera asking for a break there, so a word that carries one
                // may legitimately come back as the part before it and the part after.
                final Set<String> source = new HashSet<String>();
                for (String word : sourceWords()) {
                    source.add(word.replace("\u00AD", ""));
                    for (int i = 0; i < word.length(); i++) {
                        if (word.charAt(i) == '\u00AD') {
                            source.add(word.substring(0, i).replace("\u00AD", ""));
                            source.add(word.substring(i + 1).replace("\u00AD", ""));
                        }
                    }
                }
                int checked = 0;
                for (int page = 0; page < Math.min(pages, 6); page++) {
                    for (TextWord[] line : document.getPage(page).getText()) {
                        for (TextWord word : line) {
                            final String w = word.w == null ? "" : word.w.trim();
                            if (w.isEmpty()) {
                                continue;
                            }
                            checked++;
                            assertTrue("'" + w + "' is a word of the book, not half of one"
                                            + " - the browser hyphenated it",
                                    source.contains(w));
                        }
                    }
                }
                assertTrue("there were words to check, got " + checked, checked > 20);
            } finally {
                document.recycle();
            }
        } finally {
            BookCSS.get().isAutoHypens = hyphensBefore;
        }
    }

    /**
     * The page's margins are the reader's, once. The paginator caps the text column at 720px and
     * puts a 7% gap and a 48px band around it by default, and this book asks for 2em of its own
     * body margin on top - none of which may reach the page.
     */
    @Test
    public void thePageHasTheReadersMarginsAndNoOthers() {
        final CodecDocument document = open(AppState.EPUB_ENGINE_FOLIATE);
        try {
            document.getPageCount(PAGE_WIDTH, PAGE_HEIGHT, BookCSS.get().fontSizeSp);
            float left = 1f;
            float right = 0f;
            for (TextWord[] line : document.getPage(0).getText()) {
                for (TextWord word : line) {
                    if (word.w == null || word.w.trim().isEmpty()) {
                        continue;
                    }
                    left = Math.min(left, word.left);
                    right = Math.max(right, word.right);
                }
            }
            assertTrue("the page has text to measure", right > left);

            // What BookCSS asks for: `margin * 2 / 10` ems a side, at the reading font size
            final float em = Dips.spToPx(BookCSS.get().fontSizeSp);
            final float expected = (BookCSS.get().marginLeft + BookCSS.get().marginRight)
                    * 0.2f * em / 2f / PAGE_WIDTH;
            assertTrue("the margins are the reader's, left " + left + " against " + expected,
                    left > expected * 0.5f && left < expected * 1.8f);
            assertTrue("on both sides, right " + right + " against " + (1 - expected),
                    right < 1 - expected * 0.5f && right > 1 - expected * 1.8f);
        } finally {
            document.recycle();
        }
    }

    /** Every word the book's chapters are written from, as the page should hand them back. */
    private static List<String> sourceWords() {
        final List<String> words = new ArrayList<String>();
        final StringBuilder all = new StringBuilder();
        for (String title : TITLES) {
            all.append(title).append(' ');
        }
        for (String phrase : PHRASES) {
            all.append(phrase).append(' ');
        }
        all.append(" Paragraph 1 2 3 4 5 6 7 8 9 10 11 12 of");
        for (String title : TITLES) {
            all.append(' ').append(title).append('.');
        }
        all.append(" This is paragraph 1 2 3 4 5 6 7 8 9 10 11 12 of the chapter, written out at")
           .append(" length so that the engine has something to paginate and some\u00ADthing")
           .append(" un\u00ADcom\u00ADmon\u00ADly long to measure. on to the third chapter")
           .append(" and away to the web");
        for (String word : all.toString().split("\\s+")) {
            final String trimmed = word.trim();
            if (trimmed.length() > 0) {
                // Kept with their soft hyphens: the caller works the allowed breaks out of them
                words.add(trimmed);
            }
        }
        return words;
    }

    // ---------------------------------------------------------------- contents

    @Test
    public void theContentsResolveToPages() {
        final CodecDocument document = open(AppState.EPUB_ENGINE_FOLIATE);
        try {
            final int pages = document.getPageCount(PAGE_WIDTH, PAGE_HEIGHT, BookCSS.get().fontSizeSp);
            final List<OutlineLink> outline = document.getOutline();
            assertEquals("a chapter per entry: " + outline, TITLES.length, outline.size());
            for (int i = 0; i < outline.size(); i++) {
                final OutlineLink link = outline.get(i);
                assertEquals(TITLES[i], link.getTitle());
                final String target = link.getLink();
                assertNotNull(target);
                assertTrue("the link is a page: " + target, target.startsWith("#"));
                // "#0" is what sends the contents panel into MuPdfLinks with a handle that is not
                // a MuPDF document's
                assertFalse("and never #0: " + target, target.startsWith("#0"));
                final int page = Integer.parseInt(target.substring(1));
                assertTrue("inside the book: " + page + " of " + pages, page >= 1 && page <= pages);
                // The footnote lookup names the current chapter by this
                assertTrue("the entry keeps its href: " + link.linkUri,
                        link.linkUri != null && link.linkUri.length() > 0);
            }
            // The chapters are in order, so their pages must be too
            assertTrue("the chapters are in reading order: " + outline,
                    page(outline.get(0)) <= page(outline.get(1))
                            && page(outline.get(1)) <= page(outline.get(2)));
        } finally {
            document.recycle();
        }
    }

    private static int page(final OutlineLink link) {
        return Integer.parseInt(link.getLink().substring(1));
    }

    // ---------------------------------------------------------------- the position anchor

    @Test
    public void aBookmarkFindsItsPageAgain() {
        final CodecDocument document = open(AppState.EPUB_ENGINE_FOLIATE);
        try {
            final int pages = document.getPageCount(PAGE_WIDTH, PAGE_HEIGHT, BookCSS.get().fontSizeSp);
            final int target = Math.min(pages, 2);
            final String text = document.getBookmarkText(target);
            assertNotNull("the page offers an anchor", text);
            assertTrue("of some length: '" + text + "'", text.length() > 8);
            assertEquals("the page is found back from it", target,
                    document.findBookmarkPage(target, text, text));
            // The percentage can be off by a page either way; the text is what corrects it
            final int found = document.findBookmarkPage(Math.max(1, target - 1), text, text);
            assertEquals("even from a page out", target, found);
        } finally {
            document.recycle();
        }
    }

    // ---------------------------------------------------------------- the exact place

    /**
     * A CFI names the page it was taken on, and keeps naming the same words after the book has
     * been laid out again at a different size - which is the whole point of it, and what the
     * percentage cannot do.
     */
    @Test
    public void aLocationFindsItsPageAgain() {
        final int fontBefore = BookCSS.get().fontSizeSp;
        try {
            BookCSS.get().fontSizeSp = 18;
            final String cfi;
            final String wordsThere;
            CodecDocument document = open(AppState.EPUB_ENGINE_FOLIATE);
            try {
                final int pages = document.getPageCount(PAGE_WIDTH, PAGE_HEIGHT, BookCSS.get().fontSizeSp);
                final int target = Math.min(pages, 4);
                cfi = document.getLocation(target);
                assertNotNull("the engine named the place", cfi);
                assertTrue("and it looks like a CFI: " + cfi, cfi.startsWith("epubcfi("));
                assertEquals("it names the page it was taken on", target,
                        document.findLocationPage(cfi));
                wordsThere = pageWords(document, target - 1);
                assertTrue("that page has text to compare", wordsThere.length() > 20);
            } finally {
                document.recycle();
            }

            // The same place, in a book paginated differently
            BookCSS.get().fontSizeSp = 30;
            document = open(AppState.EPUB_ENGINE_FOLIATE);
            try {
                final int pages = document.getPageCount(PAGE_WIDTH, PAGE_HEIGHT, BookCSS.get().fontSizeSp);
                final int found = document.findLocationPage(cfi);
                assertTrue("the place is still in the book: " + found + " of " + pages,
                        found > 0 && found <= pages);
                // Not the same page number - the book is longer now - but the same words
                final String nowThere = pageWords(document, found - 1);
                final String opening = wordsThere.substring(0, Math.min(30, wordsThere.length()));
                assertTrue("the place still points at its own words: looked for '" + opening
                                + "' and found '" + nowThere.substring(0, Math.min(60, nowThere.length())) + "'",
                        nowThere.contains(opening));
            } finally {
                document.recycle();
            }
        } finally {
            BookCSS.get().fontSizeSp = fontBefore;
        }
    }

    /** A place one engine wrote means nothing to another, and it says so rather than guessing. */
    @Test
    public void aLocationFromAnotherEngineIsRefused() {
        final CodecDocument mupdf = open(AppState.EPUB_ENGINE_MUPDF);
        try {
            assertNull("MuPDF names no place", mupdf.getLocation(1));
            assertEquals("and resolves none", -1,
                    mupdf.findLocationPage("epubcfi(/6/4!/4/2/2[x]/1:0)"));
        } finally {
            mupdf.recycle();
        }

        final CodecDocument foliate = open(AppState.EPUB_ENGINE_FOLIATE);
        try {
            foliate.getPageCount(PAGE_WIDTH, PAGE_HEIGHT, BookCSS.get().fontSizeSp);
            assertEquals("and nonsense is refused rather than guessed at", -1,
                    foliate.findLocationPage("not a cfi at all"));
            assertEquals("as is nothing", -1, foliate.findLocationPage(""));
        } finally {
            foliate.recycle();
        }
    }

    /**
     * A mark made at one font size is on the same words at another.
     *
     * This is the whole point of anchoring a mark with a CFI rather than with a rectangle: the
     * reader highlights three words, changes the font, and the highlight is still on those three
     * words on whatever page they have moved to.
     */
    @Test
    public void aMarkFindsItsWordsAfterARelayout() {
        final int fontBefore = BookCSS.get().fontSizeSp;
        try {
            BookCSS.get().fontSizeSp = 18;
            final String cfi;
            final String marked;
            final int markedPage = 2;

            CodecDocument document = open(AppState.EPUB_ENGINE_FOLIATE);
            try {
                document.getPageCount(PAGE_WIDTH, PAGE_HEIGHT, BookCSS.get().fontSizeSp);
                final List<TextWord> selection = someWordsOn(document, markedPage - 1);
                marked = AnnotationsFinder.textOf(selection);
                assertTrue("there are words to mark: " + marked, marked.split(" ").length >= 3);

                cfi = document.getLocation(markedPage, AnnotationsFinder.mergeLines(selection));
                assertNotNull("the engine named the marked words", cfi);
                assertTrue("and it looks like a CFI: " + cfi, cfi.startsWith("epubcfi("));

                // On the page it was made on, it draws where it was made
                final CodecPage page = document.getPageInner(markedPage - 1);
                try {
                    assertFalse("the mark has rectangles on its own page",
                            page.getLocationRects(cfi).isEmpty());
                } finally {
                    page.recycle();
                }
            } finally {
                document.recycle();
            }

            // The same mark, in a book paginated differently
            BookCSS.get().fontSizeSp = 30;
            document = open(AppState.EPUB_ENGINE_FOLIATE);
            try {
                final int pages = document.getPageCount(PAGE_WIDTH, PAGE_HEIGHT, BookCSS.get().fontSizeSp);
                final int found = document.findLocationPage(cfi);
                assertTrue("the marked words are still in the book: " + found + " of " + pages,
                        found > 0 && found <= pages);

                final CodecPage page = document.getPageInner(found - 1);
                try {
                    final List<RectF> rects = page.getLocationRects(cfi);
                    assertFalse("the mark has rectangles on the page it moved to", rects.isEmpty());

                    // What the mark now covers, read off the page rather than taken on trust
                    final StringBuilder covered = new StringBuilder();
                    for (TextWord[] line : page.getText()) {
                        for (TextWord word : line) {
                            if (word == null || word.w == null || word.w.trim().isEmpty()) {
                                continue;
                            }
                            for (RectF rect : rects) {
                                if (RectF.intersects(rect, word)) {
                                    covered.append(word.w.trim()).append(' ');
                                    break;
                                }
                            }
                        }
                    }
                    final String first = marked.split(" ")[0];
                    assertTrue("the mark is still on its own words: marked '" + marked
                                    + "', now covers '" + covered.toString().trim() + "'",
                            covered.toString().contains(first));
                } finally {
                    page.recycle();
                }
            } finally {
                document.recycle();
            }
        } finally {
            BookCSS.get().fontSizeSp = fontBefore;
        }
    }

    /**
     * A mark that has been saved and read back again draws on the page it belongs to.
     *
     * The halves are tested apart - the engine naming the words, and the store keeping them -
     * and this is the join: what a page being decoded actually asks for, from the store the
     * reader's marks came out of.
     */
    @Test
    public void aStoredMarkIsFoundOnThePageBeingDrawn() {
        AnnotationsData.get().removeAll(file.getPath());
        final CodecDocument document = open(AppState.EPUB_ENGINE_FOLIATE);
        try {
            final int pages = document.getPageCount(PAGE_WIDTH, PAGE_HEIGHT, BookCSS.get().fontSizeSp);
            assertTrue("a book of several pages to tell apart, got " + pages, pages >= 3);

            final int marked = 2;
            final List<TextWord> selection = someWordsOn(document, marked - 1);
            final String cfi = document.getLocation(marked, AnnotationsFinder.mergeLines(selection));
            assertNotNull("the engine named the marked words", cfi);

            final AppAnnotation mark = new AppAnnotation(file.getPath());
            mark.setType(AnnotationType.HIGHLIGHT);
            mark.color = "#00FF00";
            mark.text = AnnotationsFinder.textOf(selection);
            mark.page = marked - 1;
            mark.cfi = cfi;
            AnnotationsData.get().add(mark);

            final AnnotationsFinder finder = AnnotationsFinder.forBook(file.getPath());
            assertFalse("the store has the mark", finder.isEmpty());

            CodecPage page = document.getPageInner(marked - 1);
            try {
                final List<AnnotationsFinder.Drawn> drawn = finder.forPage(document, page, marked - 1);
                assertEquals("one mark on its own page", 1, drawn.size());
                assertEquals(mark.t, drawn.get(0).annotation.t);
                assertEquals("#00FF00", drawn.get(0).annotation.color);
                assertFalse("with somewhere to draw it", drawn.get(0).rects.isEmpty());
            } finally {
                page.recycle();
            }

            // And nothing on a page it is not on
            final int elsewhere = marked == 1 ? 1 : 0;
            page = document.getPageInner(elsewhere);
            try {
                assertTrue("no mark on another page",
                        finder.forPage(document, page, elsewhere).isEmpty());
            } finally {
                page.recycle();
            }
        } finally {
            document.recycle();
            AnnotationsData.get().removeAll(file.getPath());
        }
    }

    /** Three words in a row on one line of a page, as a reader would select them. */
    private static List<TextWord> someWordsOn(final CodecDocument document, final int page) {
        final List<TextWord> selection = new ArrayList<TextWord>();
        final CodecPage codecPage = document.getPageInner(page);
        assertNotNull(codecPage);
        try {
            for (TextWord[] line : codecPage.getText()) {
                final List<TextWord> real = new ArrayList<TextWord>();
                for (TextWord word : line) {
                    if (word != null && word.w != null && word.w.trim().length() > 2) {
                        real.add(word);
                    }
                }
                if (real.size() >= 3) {
                    selection.addAll(real.subList(0, 3));
                    break;
                }
            }
        } finally {
            codecPage.recycle();
        }
        return selection;
    }

    /** The words of a page, as one string. */
    private static String pageWords(final CodecDocument document, final int page) {
        final StringBuilder out = new StringBuilder();
        final CodecPage codecPage = document.getPageInner(page);
        assertNotNull(codecPage);
        try {
            for (TextWord[] line : codecPage.getText()) {
                for (TextWord word : line) {
                    if (word.w != null) {
                        out.append(word.w);
                    }
                }
            }
        } finally {
            codecPage.recycle();
        }
        return out.toString().replaceAll("\\s+", " ").trim();
    }

    /**
     * The guard that keeps the app alive: these are asked for on the UI thread, where waiting for
     * the WebView would wait for the thread the caller is holding.
     */
    @Test
    public void theMainThreadIsNeverBlocked() throws Exception {
        final CodecDocument document = open(AppState.EPUB_ENGINE_FOLIATE);
        try {
            document.getPageCount(PAGE_WIDTH, PAGE_HEIGHT, BookCSS.get().fontSizeSp);
            final CountDownLatch done = new CountDownLatch(1);
            final long[] took = new long[1];
            new Handler(Looper.getMainLooper()).post(new Runnable() {
                @Override
                public void run() {
                    final long started = System.currentTimeMillis();
                    document.getBookmarkText(1);
                    document.findBookmarkPage(1, "the first chapter speaks", null);
                    took[0] = System.currentTimeMillis() - started;
                    done.countDown();
                }
            });
            assertTrue("the main thread came back", done.await(5, TimeUnit.SECONDS));
            assertTrue("promptly, took " + took[0] + "ms", took[0] < 1000);
        } finally {
            document.recycle();
        }
    }

    // ---------------------------------------------------------------- helpers

    private CodecDocument open() {
        final CodecContext context = BookType.getCodecContextByPath(file.getPath());
        assertNotNull(context);
        assertTrue("an epub is read by the epub context", context instanceof EpubContext);
        final CodecDocument document = context.openDocument(file.getPath(), "");
        assertNotNull("the book was opened", document);
        return document;
    }

    private CodecDocument open(final int engine) {
        AppState.get().epubEngine = engine;
        final CodecDocument document = open();
        if (engine == AppState.EPUB_ENGINE_FOLIATE) {
            assertTrue("foliate opened it, not MuPDF: " + document, document instanceof FoliateDocument);
        }
        return document;
    }

    /** A three-chapter EPUB, so the test does not depend on a book being pushed to the device. */
    private static byte[] epub() throws Exception {
        final ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        final ZipOutputStream zip = new ZipOutputStream(bytes);

        // "mimetype" has to be first and stored, not deflated
        final byte[] mimetype = "application/epub+zip".getBytes(StandardCharsets.US_ASCII);
        final ZipEntry entry = new ZipEntry("mimetype");
        entry.setMethod(ZipEntry.STORED);
        entry.setSize(mimetype.length);
        entry.setCompressedSize(mimetype.length);
        final java.util.zip.CRC32 crc = new java.util.zip.CRC32();
        crc.update(mimetype);
        entry.setCrc(crc.getValue());
        zip.putNextEntry(entry);
        zip.write(mimetype);
        zip.closeEntry();

        write(zip, "META-INF/container.xml",
                "<?xml version=\"1.0\"?>"
                        + "<container version=\"1.0\" xmlns=\"urn:oasis:names:tc:opendocument:xmlns:container\">"
                        + "<rootfiles><rootfile full-path=\"OEBPS/content.opf\""
                        + " media-type=\"application/oebps-package+xml\"/></rootfiles></container>");

        final StringBuilder manifest = new StringBuilder();
        final StringBuilder spine = new StringBuilder();
        final StringBuilder nav = new StringBuilder();
        for (int i = 0; i < TITLES.length; i++) {
            final String name = "chapter" + (i + 1) + ".xhtml";
            manifest.append("<item id=\"c").append(i + 1).append("\" href=\"").append(name)
                    .append("\" media-type=\"application/xhtml+xml\"/>");
            spine.append("<itemref idref=\"c").append(i + 1).append("\"/>");
            nav.append("<navPoint id=\"n").append(i + 1).append("\" playOrder=\"").append(i + 1)
                    .append("\"><navLabel><text>").append(TITLES[i]).append("</text></navLabel>")
                    .append("<content src=\"").append(name).append("\"/></navPoint>");

            // Enough text that a chapter is more than one page at a readable size, plus a link
            final StringBuilder body = new StringBuilder();
            body.append("<h1>").append(TITLES[i]).append("</h1>");
            for (int p = 0; p < 12; p++) {
                // Laid out the way a real book's source is - the text broken across source lines
                // with indentation, so the whitespace between two words contains a newline and is
                // what a line wraps on. A word carrying soft hyphens, as the conversion inserts
                // them, so a word can break across lines too.
                body.append("<p style=\"text-align:justify\">\n    ")
                        // The number first: a bookmark is found again by the text at the top of
                        // its page, so no two pages may start with the same words
                        .append("Paragraph ").append(p + 1).append(" of ").append(TITLES[i])
                        .append(".\n    ")
                        .append(PHRASES[i]).append("\n    This is paragraph ").append(p + 1)
                        .append(" of the chapter,\n    written out at length so that the engine\n")
                        .append("    has something to paginate and some\u00ADthing un\u00ADcom\u00ADmon")
                        .append("\u00ADly long to measure.\n  </p>\n");
            }
            if (i == 0) {
                body.append("<p><a href=\"chapter3.xhtml\">on to the third chapter</a></p>");
                body.append("<p><a href=\"https://librera.mobi\">and away to the web</a></p>");
            }
            write(zip, "OEBPS/" + name,
                    "<?xml version=\"1.0\" encoding=\"utf-8\"?>"
                            + "<html xmlns=\"http://www.w3.org/1999/xhtml\"><head><title>"
                            + TITLES[i] + "</title>"
                            // A book that asks for the browser's own hyphenation, and for margins
                            // of its own - both of which Librera has to override
                            + "<style>html,body{margin:2em;padding:1em;}"
                            + "p{hyphens:auto;-webkit-hyphens:auto;}</style>"
                            + "</head><body>" + body + "</body></html>");
        }

        write(zip, "OEBPS/toc.ncx",
                "<?xml version=\"1.0\" encoding=\"utf-8\"?>"
                        + "<ncx xmlns=\"http://www.daisy.org/z3986/2005/ncx/\" version=\"2005-1\">"
                        + "<head><meta name=\"dtb:uid\" content=\"librera-foliate-test\"/></head>"
                        + "<docTitle><text>Foliate Test Book</text></docTitle>"
                        + "<navMap>" + nav + "</navMap></ncx>");

        write(zip, "OEBPS/content.opf",
                "<?xml version=\"1.0\" encoding=\"utf-8\"?>"
                        + "<package xmlns=\"http://www.idpf.org/2007/opf\" version=\"2.0\""
                        + " unique-identifier=\"bookid\">"
                        + "<metadata xmlns:dc=\"http://purl.org/dc/elements/1.1/\">"
                        + "<dc:identifier id=\"bookid\">librera-foliate-test</dc:identifier>"
                        + "<dc:title>Foliate Test Book</dc:title>"
                        + "<dc:language>en</dc:language></metadata>"
                        + "<manifest>" + manifest
                        + "<item id=\"ncx\" href=\"toc.ncx\" media-type=\"application/x-dtbncx+xml\"/>"
                        + "</manifest><spine toc=\"ncx\">" + spine + "</spine></package>");

        zip.close();
        return bytes.toByteArray();
    }

    private static void write(final ZipOutputStream zip, final String name, final String content)
            throws Exception {
        zip.putNextEntry(new ZipEntry(name));
        zip.write(content.getBytes(StandardCharsets.UTF_8));
        zip.closeEntry();
    }
}
