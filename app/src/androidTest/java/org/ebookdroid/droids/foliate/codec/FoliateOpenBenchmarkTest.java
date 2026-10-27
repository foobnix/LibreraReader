package org.ebookdroid.droids.foliate.codec;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.ParcelFileDescriptor;
import android.util.Log;

import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import com.foobnix.ext.CacheZipUtils;
import com.foobnix.ext.EpubExtractor;
import com.foobnix.model.AppProfile;
import com.foobnix.model.AppSP;
import com.foobnix.model.AppState;
import com.foobnix.pdf.info.AppsConfig;
import com.foobnix.pdf.info.model.BookCSS;
import com.foobnix.sys.TempHolder;

import org.ebookdroid.core.codec.AbstractCodecContext;
import org.ebookdroid.core.codec.CodecDocument;
import org.ebookdroid.droids.EpubContext;
import org.ebookdroid.droids.foliate.FoliateHostActivity;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.After;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Where the time of a first open of an EPUB goes with the foliate engine, phase by phase and
 * section by section, with MuPDF's own page count of the same book beside it.
 *
 * Phases, in the order the reader goes through them on a book it has never opened:
 * <ul>
 * <li>{@code convert} - EpubExtractor rewriting the book with soft hyphens (cached afterwards)</li>
 * <li>{@code notes} - the footnote map, read on a background thread while the book opens</li>
 * <li>{@code engine} - the WebView, the shell importing foliate, foliate opening the book</li>
 * <li>{@code index} - every section laid out to count its pages (cached afterwards)</li>
 * <li>{@code toc} - every contents entry resolved to a page (cached afterwards)</li>
 * <li>{@code page} - positioning on a page and drawing it, the first and one mid-book</li>
 * </ul>
 *
 * The books are not in the repository: push them to {@code /data/local/tmp/foliate/user}, and a
 * book that is not there is skipped. Everything goes to logcat under {@code FOLIATE_BENCH}.
 */
@RunWith(AndroidJUnit4.class)
public class FoliateOpenBenchmarkTest {

    private static final String TAG = "FOLIATE_BENCH";

    private static final String DIR = "/data/local/tmp/foliate/";

    private static final int PAGE_WIDTH = 1080;
    private static final int PAGE_HEIGHT = 2200;

    /** Pinned, so one run's counts can be compared with another's. */
    private static final int FONT_SP = 21;

    private ActivityScenario<FoliateHostActivity> host;
    private int engineBefore;
    private int foliateFontBefore;
    private String langBefore;

    @Before
    public void setUp() {
        final Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        AppsConfig.IS_LOG = true;
        CacheZipUtils.init(context);
        CacheZipUtils.createAllCacheDirs();
        AppProfile.init(context);
        TempHolder.get().loadingCancelled.set(false);
        host = ActivityScenario.launch(FoliateHostActivity.class);
        InstrumentationRegistry.getInstrumentation().waitForIdleSync();
        engineBefore = AppState.get().epubEngine;
        foliateFontBefore = BookCSS.get().fontSizeSpFoliate;
        langBefore = AppSP.get().hypenLang;
        BookCSS.get().fontSizeSpFoliate = FONT_SP;
    }

    @After
    public void tearDown() {
        AppState.get().epubEngine = engineBefore;
        BookCSS.get().fontSizeSpFoliate = foliateFontBefore;
        AppSP.get().hypenLang = langBefore;
        if (host != null) {
            host.close();
        }
    }

    @Test
    public void books() throws Exception {
        // The first WebView of the process pays for starting Chromium; this one takes that, so
        // the books below are measured alike
        bench("user/quiet-metrics.epub", "en", "warm-up");
        bench("user/quiet-metrics.epub", "en", null);
        bench("user/butareba.epub", "en", null);
        bench("user/liberi-tutti.epub", "it", null);
        bench("user/montecristo-pg1184.zip", "en", null);
        bench("user/churchill.epub", "ru", null);
        bench("mobydick.epub", "en", null);
    }

    private void bench(final String name, final String lang, final String label) throws Exception {
        final Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        File book = copy(context, name);
        if (book == null) {
            Log.i(TAG, name + " skipped: not pushed to " + DIR);
            return;
        }
        final String title = label == null ? name : name + " (" + label + ")";
        final List<String> lines = new ArrayList<String>();

        if (book.getName().endsWith(".zip")) {
            final long started = System.currentTimeMillis();
            book = unzip(book);
            lines.add("unzip " + (System.currentTimeMillis() - started) + "ms");
        }
        assertNotNull(book);
        AppSP.get().hypenLang = lang;

        // ---- convert: what EpubContext does before an engine sees the book
        // Named the way AbstractCodecContext.openDocument names it, so MuPDF below finds it
        final EpubContext epub = new EpubContext();
        final File converted = epub.getCacheFileName(book.getPath()
                + AbstractCodecContext.getFileNameSalt(book.getPath()));
        converted.delete();
        long started = System.currentTimeMillis();
        EpubExtractor.proccessHypens(book.getPath(), converted.getPath(), null);
        final long convert = System.currentTimeMillis() - started;
        assertTrue("converted", converted.isFile());
        lines.add("convert (hyphens " + BookCSS.get().isAutoHypens + ", " + lang + ") " + convert
                + "ms, " + (book.length() / 1024) + " KB -> " + (converted.length() / 1024) + " KB");

        started = System.currentTimeMillis();
        final int notes = size(EpubExtractor.get().getFooterNotes(book.getPath()));
        lines.add("notes " + (System.currentTimeMillis() - started) + "ms, " + notes + " notes");

        // ---- engine
        final FoliateRenderer renderer = new FoliateRenderer(context, converted.getPath());
        try {
            started = System.currentTimeMillis();
            final int sections = renderer.open(PAGE_WIDTH, PAGE_HEIGHT);
            final long engine = System.currentTimeMillis() - started;
            if (sections <= 0) {
                // A fixed-layout book, say: the reader falls back to MuPDF for it
                lines.add("engine " + engine + "ms: foliate could not open it");
                throw new Skip();
            }
            lines.add("engine " + engine + "ms " + renderer.openTimes + ", " + sections + " sections");

            // ---- index
            started = System.currentTimeMillis();
            final JSONObject index = renderer.indexProfile();
            final long indexed = System.currentTimeMillis() - started;
            assertNotNull("indexed", index);
            lines.add("index " + indexed + "ms, " + total(index.getJSONArray("pages")) + " pages (hash "
                    + index.getJSONArray("pages").toString().hashCode() + "): " + profile(index));

            // ---- toc, as the walk above resolved it
            final FoliateIndex pages = FoliateIndex.of(counts(index.getJSONArray("pages")));
            started = System.currentTimeMillis();
            final String toc = tocPages(renderer.toc(), pages);
            final long tocMs = System.currentTimeMillis() - started;

            // and by a walk of its own, in a fresh renderer, to check the two agree
            final FoliateRenderer fresh = new FoliateRenderer(context, converted.getPath());
            String walked = null;
            long walkMs = 0;
            try {
                if (fresh.open(PAGE_WIDTH, PAGE_HEIGHT) > 0) {
                    started = System.currentTimeMillis();
                    walked = tocPages(fresh.toc(), pages);
                    walkMs = System.currentTimeMillis() - started;
                }
            } finally {
                fresh.destroy();
            }
            lines.add("toc " + tocMs + "ms (its own walk " + walkMs + "ms), "
                    + (toc.isEmpty() ? 0 : toc.split(" ").length) + " entries (hash " + toc.hashCode() + "), "
                    + (toc.equals(walked) ? "same pages" : "DIFFERENT PAGES: " + toc + " vs " + walked));

            // ---- pages
            lines.add("page 1 " + page(renderer, 0) + ", mid-book " + page(renderer, sections / 2));
        } catch (Skip e) {
            // what foliate could not open is still worth MuPDF's numbers
        } finally {
            renderer.destroy();
        }

        // ---- MuPDF on the same converted book, for comparison
        lines.add("mupdf " + mupdf(book.getPath(), converted));

        final StringBuilder out = new StringBuilder(title);
        for (String line : lines) {
            out.append("\n    ").append(line);
        }
        for (String line : out.toString().split("\n")) {
            Log.i(TAG, line);
        }
    }

    /** Ends a book's foliate phases early. */
    private static final class Skip extends Exception {
    }

    /** Going to a page and drawing it, as `goTo + capture` ms. */
    private static String page(final FoliateRenderer renderer, final int section) {
        final long started = System.currentTimeMillis();
        final int chars = renderer.goTo(section, 0f);
        final long positioned = System.currentTimeMillis();
        final Bitmap bitmap = renderer.capture(PAGE_WIDTH, PAGE_HEIGHT);
        final long drawn = System.currentTimeMillis();
        if (bitmap != null) {
            bitmap.recycle();
        }
        return (positioned - started) + "+" + (drawn - positioned) + "ms (" + chars + " chars)";
    }

    /**
     * The walk summed by phase, and its five slowest sections, each as
     * `#index pages load/parse/layout/fonts/toc`.
     */
    private static String profile(final JSONObject index) throws Exception {
        final JSONArray pages = index.getJSONArray("pages");
        final JSONArray times = index.optJSONArray("profile");
        if (times == null) {
            return "no profile";
        }
        long load = 0, parse = 0, layout = 0, fonts = 0, toc = 0;
        final List<long[]> rows = new ArrayList<long[]>();
        for (int i = 0; i < times.length(); i++) {
            final JSONObject t = times.getJSONObject(i);
            final long[] row = {i, pages.getInt(i), t.optLong("load"), t.optLong("parse"),
                    t.optLong("layout"), t.optLong("fonts"), t.optLong("toc"), 0};
            row[7] = row[2] + row[3] + row[4] + row[5] + row[6];
            load += row[2];
            parse += row[3];
            layout += row[4];
            fonts += row[5];
            toc += row[6];
            rows.add(row);
        }
        Collections.sort(rows, (a, b) -> Long.compare(b[7], a[7]));
        final StringBuilder slowest = new StringBuilder();
        for (int i = 0; i < Math.min(5, rows.size()); i++) {
            final long[] r = rows.get(i);
            slowest.append(String.format(Locale.US, " #%d %dp %d/%d/%d/%d/%d", r[0], r[1], r[2], r[3], r[4],
                    r[5], r[6]));
        }
        final int n = Math.max(1, times.length());
        return String.format(Locale.US, "load %d + parse %d + layout %d + fonts %d + toc %d ms"
                        + " (per section %.1f/%.1f/%.1f/%.1f/%.1f); slowest load/parse/layout/fonts/toc:%s",
                load, parse, layout, fonts, toc, load / (float) n, parse / (float) n, layout / (float) n,
                fonts / (float) n, toc / (float) n, slowest);
    }

    /** MuPDF's open, page count and outline of the converted book, with no accelerator cached. */
    private static String mupdf(final String original, final File converted) {
        AppState.get().epubEngine = AppState.EPUB_ENGINE_MUPDF;
        // With logging on, EpubContext also unpacks the whole book for debugging; not a cost a
        // reader pays
        AppsConfig.IS_LOG = false;
        try {
            // Where MuPdfDocument keeps the layout it saved, worked out the way it does
            String accel = new EpubContext().getCacheFileName(converted.getPath()).getPath() + "+accel";
            accel = accel.replace(CacheZipUtils.CACHE_BOOK_DIR.getPath(), CacheZipUtils.CACHE_TEMP.getPath());
            new File(accel).delete();
            final EpubContext context = new EpubContext();
            long started = System.currentTimeMillis();
            final CodecDocument document = context.openDocument(original, "");
            final long open = System.currentTimeMillis() - started;
            if (document == null) {
                return "could not open";
            }
            try {
                started = System.currentTimeMillis();
                final int pages = document.getPageCount(PAGE_WIDTH, PAGE_HEIGHT, BookCSS.get().fontSizeSp);
                final long count = System.currentTimeMillis() - started;
                started = System.currentTimeMillis();
                final int outline = document.getOutline().size();
                final long toc = System.currentTimeMillis() - started;
                return "open " + open + "ms, layout " + count + "ms (" + pages + " pages at "
                        + BookCSS.get().fontSizeSp + "sp), outline " + toc + "ms (" + outline + ")";
            } finally {
                document.recycle();
            }
        } catch (Throwable e) {
            return "failed: " + e;
        } finally {
            AppsConfig.IS_LOG = true;
        }
    }

    /** The page each contents entry opens, as the reader numbers it. */
    private static String tocPages(final String toc, final FoliateIndex pages) throws Exception {
        if (toc == null) {
            return "";
        }
        final JSONArray entries = new JSONObject(toc).getJSONArray("toc");
        final StringBuilder out = new StringBuilder();
        for (int i = 0; i < entries.length(); i++) {
            final JSONObject entry = entries.getJSONObject(i);
            if (out.length() > 0) {
                out.append(' ');
            }
            out.append(pages.pageOf(entry.optInt("index", 0), entry.optDouble("fraction", 0)) + 1);
        }
        return out.toString();
    }

    private static int[] counts(final JSONArray pages) throws Exception {
        final int[] counts = new int[pages.length()];
        for (int i = 0; i < counts.length; i++) {
            counts[i] = Math.max(1, pages.getInt(i));
        }
        return counts;
    }

    private static int size(final java.util.Map<?, ?> map) {
        return map == null ? 0 : map.size();
    }

    private static int total(final JSONArray pages) throws Exception {
        int total = 0;
        for (int i = 0; i < pages.length(); i++) {
            total += Math.max(1, pages.getInt(i));
        }
        return total;
    }

    /** The book in the app's own cache, so nothing depends on reading /data/local/tmp directly. */
    private static File copy(final Context context, final String name) throws Exception {
        final File out = new File(context.getCacheDir(), "bench-" + name.replace('/', '-'));
        final ParcelFileDescriptor pfd = InstrumentationRegistry.getInstrumentation().getUiAutomation()
                .executeShellCommand("cat " + DIR + name);
        final InputStream in = new ParcelFileDescriptor.AutoCloseInputStream(pfd);
        final OutputStream os = new FileOutputStream(out);
        long total = 0;
        try {
            final byte[] buffer = new byte[64 * 1024];
            int read;
            while ((read = in.read(buffer)) > 0) {
                os.write(buffer, 0, read);
                total += read;
            }
        } finally {
            in.close();
            os.close();
        }
        if (total == 0) {
            out.delete();
            return null;
        }
        return out;
    }

    /** The one EPUB inside a zip, beside it. */
    private static File unzip(final File zip) throws Exception {
        final ZipInputStream in = new ZipInputStream(new java.io.FileInputStream(zip));
        try {
            ZipEntry entry;
            while ((entry = in.getNextEntry()) != null) {
                if (!entry.getName().toLowerCase(Locale.US).endsWith(".epub")) {
                    continue;
                }
                final File out = new File(zip.getParentFile(), zip.getName() + ".epub");
                final OutputStream os = new FileOutputStream(out);
                try {
                    final byte[] buffer = new byte[64 * 1024];
                    int read;
                    while ((read = in.read(buffer)) > 0) {
                        os.write(buffer, 0, read);
                    }
                } finally {
                    os.close();
                }
                return out;
            }
        } finally {
            in.close();
        }
        return null;
    }
}
