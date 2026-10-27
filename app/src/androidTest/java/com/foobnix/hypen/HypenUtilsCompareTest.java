package com.foobnix.hypen;

import static org.junit.Assert.assertEquals;

import android.content.Context;
import android.os.ParcelFileDescriptor;
import android.util.Log;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import com.foobnix.ext.CacheZipUtils;
import com.foobnix.model.AppData;
import com.foobnix.model.AppProfile;
import com.foobnix.model.SimpleMeta;
import com.foobnix.pdf.info.AppsConfig;
import com.foobnix.pdf.info.model.BookCSS;
import com.foobnix.sys.TempHolder;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * HypenUtils.applyHypnes against its previous version (LegacyHypenUtils) on real books: the same
 * output line for line, and how long each takes over a whole book.
 *
 * The books come from {@code /data/local/tmp/foliate/userbooks}, listed in its {@code list.txt}
 * as {@code file|size|original path}; the test is skipped when there is none. Each book is
 * hyphenated in its own language, read from its package. Results go to logcat under
 * {@code HYPHENS_COMPARE}. For times that mean anything, build with {@code debuggable = false}:
 * ART runs a debuggable app's loops about ten times slower.
 */
@RunWith(AndroidJUnit4.class)
public class HypenUtilsCompareTest {

    private static final String TAG = "HYPHENS_COMPARE";

    private static final String DIR = "/data/local/tmp/foliate/userbooks/";

    private static final Pattern LANGUAGE = Pattern.compile("<dc:language[^>]*>\\s*([A-Za-z]{2,3})");

    private boolean hyphensBefore;

    @Before
    public void setUp() {
        final Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        AppsConfig.IS_LOG = false;
        CacheZipUtils.init(context);
        AppProfile.init(context);
        TempHolder.get().loadingCancelled.set(false);
        hyphensBefore = BookCSS.get().isAutoHypens;
        BookCSS.get().isAutoHypens = true;
    }

    @After
    public void tearDown() {
        BookCSS.get().isAutoHypens = hyphensBefore;
    }

    @Test
    public void sameOutputOnRealBooks() throws Exception {
        final String list = new String(shell("cat " + DIR + "list.txt"), StandardCharsets.UTF_8);
        final List<SimpleMeta> replacements = AppData.get().getAllTextReplaces();
        int books = 0;
        int different = 0;
        long oldTotal = 0;
        long newTotal = 0;
        for (String row : list.split("\n")) {
            if (row.trim().isEmpty()) {
                continue;
            }
            final String[] parts = row.split("\\|", 3);
            final File epub = copy(parts[0]);
            if (epub == null) {
                continue;
            }
            final Book book = read(epub);
            epub.delete();
            final String lang = book.lang == null ? "en" : book.lang;
            HypenUtils.applyLanguage(lang);
            LegacyHypenUtils.instance = new DefaultHyphenator(patternFor(lang));

            // Best of two each, every word hyphenated afresh as for a first book, alternating so
            // neither side is always the one the JIT has just warmed up
            long oldMs = Long.MAX_VALUE;
            long newMs = Long.MAX_VALUE;
            String oldDigest = null;
            String newDigest = null;
            for (int run = 0; run < 2; run++) {
                LegacyHypenUtils.cache.clear();
                final MessageDigest oldSum = MessageDigest.getInstance("SHA-256");
                long started = System.nanoTime();
                for (List<String> lines : book.pages) {
                    LegacyHypenUtils.resetTokenizer();
                    for (String line : lines) {
                        oldSum.update(LegacyHypenUtils.applyHypnes(line, replacements).getBytes(StandardCharsets.UTF_8));
                    }
                }
                oldMs = Math.min(oldMs, (System.nanoTime() - started) / 1_000_000);
                oldDigest = hex(oldSum.digest());

                HypenUtils.cache.clear();
                final MessageDigest newSum = MessageDigest.getInstance("SHA-256");
                started = System.nanoTime();
                for (List<String> lines : book.pages) {
                    HypenUtils.resetTokenizer();
                    for (String line : lines) {
                        newSum.update(HypenUtils.applyHypnes(line, replacements).getBytes(StandardCharsets.UTF_8));
                    }
                }
                newMs = Math.min(newMs, (System.nanoTime() - started) / 1_000_000);
                newDigest = hex(newSum.digest());
            }

            // Line by line too, so a difference can be shown rather than just counted
            int lineDiffs = 0;
            String firstDiff = "";
            for (List<String> lines : book.pages) {
                LegacyHypenUtils.resetTokenizer();
                HypenUtils.resetTokenizer();
                for (String line : lines) {
                    final String a = LegacyHypenUtils.applyHypnes(line, replacements);
                    final String b = HypenUtils.applyHypnes(line, replacements);
                    if (!a.equals(b)) {
                        if (lineDiffs++ == 0) {
                            firstDiff = " first: " + a.substring(0, Math.min(80, a.length())) + " | "
                                    + b.substring(0, Math.min(80, b.length()));
                        }
                    }
                }
            }
            final boolean same = lineDiffs == 0 && oldDigest.equals(newDigest);
            if (!same) {
                different++;
            }
            books++;
            oldTotal += oldMs;
            newTotal += newMs;
            final String name = parts.length > 2 ? new File(parts[2]).getName() : parts[0];
            Log.i(TAG, String.format(Locale.US, "%s\t%s\t%d KB\t%d lines\told %d ms\tnew %d ms\t%s\t%s%s",
                    parts[0], lang, book.chars / 1024, book.lines, oldMs, newMs,
                    same ? "same " + newDigest.substring(0, 12) : "DIFFERENT (" + lineDiffs + " lines)", name,
                    firstDiff));
        }
        Log.i(TAG, String.format(Locale.US, "TOTAL %d books: old %d ms, new %d ms, %d different",
                books, oldTotal, newTotal, different));
        assertEquals("books whose hyphenated text differs", 0, different);
    }

    /** The lines of the pages EpubExtractor hyphenates, and the book's language. */
    private static final class Book {
        final List<List<String>> pages = new ArrayList<List<String>>();
        String lang;
        long chars;
        int lines;
    }

    private static Book read(final File epub) throws Exception {
        final Book book = new Book();
        final ZipInputStream zip = new ZipInputStream(new java.io.BufferedInputStream(new java.io.FileInputStream(epub)));
        ZipEntry entry;
        while ((entry = zip.getNextEntry()) != null) {
            final String low = entry.getName().toLowerCase(Locale.US);
            if (low.endsWith(".opf")) {
                final Matcher m = LANGUAGE.matcher(new String(readAll(zip), StandardCharsets.UTF_8));
                if (m.find()) {
                    book.lang = m.group(1).toLowerCase(Locale.US);
                }
                continue;
            }
            if (low.contains("encryption.xml") || low.contains("container.xml") || low.contains("nav")
                    || low.contains("toc") || low.endsWith(".css")
                    || !(low.endsWith("html") || low.endsWith("htm") || low.endsWith("xml"))) {
                continue;
            }
            final BufferedReader in = new BufferedReader(new InputStreamReader(
                    new java.io.ByteArrayInputStream(readAll(zip))));
            final List<String> lines = new ArrayList<String>();
            String line;
            while ((line = in.readLine()) != null) {
                lines.add(line);
                book.chars += line.length();
            }
            book.lines += lines.size();
            book.pages.add(lines);
        }
        zip.close();
        return book;
    }

    /** The pattern HypenUtils.applyLanguage picks for a language. */
    private static HyphenPattern patternFor(String lang) {
        if (lang.length() > 2) {
            lang = lang.substring(0, 2).toLowerCase(Locale.US);
        }
        if ("sp".equals(lang)) {
            lang = "es";
        }
        HyphenPattern pattern = HyphenPattern.error;
        for (HyphenPattern p : HyphenPattern.values()) {
            if (p.lang.equals(lang)) {
                pattern = p;
            }
        }
        return pattern;
    }

    /** The book in the app's own cache, streamed rather than held: some are a hundred MB of images. */
    private static File copy(final String name) throws Exception {
        final Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        final File out = new File(context.getCacheDir(), "compare-" + name);
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

    private static byte[] readAll(final InputStream in) throws Exception {
        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        final byte[] buffer = new byte[64 * 1024];
        int read;
        while ((read = in.read(buffer)) > 0) {
            out.write(buffer, 0, read);
        }
        return out.toByteArray();
    }

    private static byte[] shell(final String command) throws Exception {
        final ParcelFileDescriptor pfd = InstrumentationRegistry.getInstrumentation().getUiAutomation()
                .executeShellCommand(command);
        final InputStream in = new ParcelFileDescriptor.AutoCloseInputStream(pfd);
        try {
            return readAll(in);
        } finally {
            in.close();
        }
    }

    private static String hex(final byte[] bytes) {
        final StringBuilder out = new StringBuilder();
        for (byte b : bytes) {
            out.append(String.format("%02x", b));
        }
        return out.toString();
    }
}
