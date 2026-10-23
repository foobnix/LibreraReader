package com.foobnix.ext;

import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.os.ParcelFileDescriptor;
import android.util.Log;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import com.foobnix.hypen.DefaultHyphenator;
import com.foobnix.hypen.HyphenPattern;
import com.foobnix.hypen.HypenUtils;
import com.foobnix.model.AppData;
import com.foobnix.model.AppProfile;
import com.foobnix.model.AppSP;
import com.foobnix.model.SimpleMeta;
import com.foobnix.pdf.info.AppsConfig;
import com.foobnix.pdf.info.model.BookCSS;
import com.foobnix.sys.ArchiveEntry;
import com.foobnix.sys.TempHolder;
import com.foobnix.sys.ZipArchiveInputStream;
import com.foobnix.sys.Zips;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Where the time of EpubExtractor.proccessHypens goes - the soft-hyphen rewrite EpubContext makes
 * of a book before an engine opens it - stage by stage, with a digest of the book it writes, so a
 * faster converter can be shown to write exactly the same book.
 *
 * The books are not in the repository: push them to {@code /data/local/tmp/foliate}, and a book
 * that is not there is skipped. Everything goes to logcat under {@code HYPHENS_BENCH}; each
 * converted book is also copied to {@code /data/local/tmp/foliate/converted} for diffing.
 * {@code -e book NAME -e lang CODE} runs just that book.
 *
 * Time it with {@code debuggable = false} on the debug build type, or it measures the wrong
 * thing: ART runs a debuggable app's loops roughly ten times slower, and the conversion of the
 * longest book here goes from 0.6 s to 7 s. The digests are the same either way.
 */
@RunWith(AndroidJUnit4.class)
public class HyphenConversionBenchmarkTest {

    private static final String TAG = "HYPHENS_BENCH";

    private static final String DIR = "/data/local/tmp/foliate/";

    private boolean hyphensBefore;
    private String langBefore;

    @Before
    public void setUp() {
        final Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        AppsConfig.IS_LOG = false;
        CacheZipUtils.init(context);
        CacheZipUtils.createAllCacheDirs();
        AppProfile.init(context);
        TempHolder.get().loadingCancelled.set(false);
        hyphensBefore = BookCSS.get().isAutoHypens;
        langBefore = AppSP.get().hypenLang;
        BookCSS.get().isAutoHypens = true;
        shell("mkdir -p " + DIR + "converted");
    }

    @After
    public void tearDown() {
        BookCSS.get().isAutoHypens = hyphensBefore;
        AppSP.get().hypenLang = langBefore;
    }

    @Test
    public void books() throws Exception {
        // -e book <name> -e lang <code>: just that one, say for a profiler to watch
        final String only = InstrumentationRegistry.getArguments().getString("book");
        if (only != null) {
            final String lang = InstrumentationRegistry.getArguments().getString("lang");
            bench(only, lang == null ? "en" : lang);
            return;
        }
        bench("user/quiet-metrics.epub", "en");
        bench("user/churchill.epub", "ru");
        bench("montecristo.epub", "en");
        bench("mobydick.epub", "en");
        bench("user/liberi-tutti.epub", "it");
        bench("notes-98409.epub", "en");
    }

    private void bench(final String name, final String lang) throws Exception {
        final Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        final File book = copy(context, name);
        if (book == null) {
            Log.i(TAG, name + " skipped: not pushed to " + DIR);
            return;
        }
        AppSP.get().hypenLang = lang;
        HypenUtils.applyLanguage(lang);
        // What the first book in a language pays once per process: the patterns made into a trie
        long started = System.currentTimeMillis();
        new DefaultHyphenator(HyphenPattern.valueOf(lang));
        final long patterns = System.currentTimeMillis() - started;
        final List<SimpleMeta> replacements = AppData.get().getAllTextReplaces();

        // ---- read: the zip and the lines of every page the converter rewrites, nothing else
        started = System.currentTimeMillis();
        final List<List<String>> pages = new ArrayList<List<String>>();
        long chars = 0;
        final ZipArchiveInputStream zip = Zips.buildZipArchiveInputStream(book.getPath());
        ArchiveEntry entry;
        final byte[] buffer = new byte[64 * 1024];
        while ((entry = zip.getNextEntry()) != null) {
            if (isRewritten(entry.getName())) {
                final BufferedReader in = new BufferedReader(new InputStreamReader(zip));
                final List<String> lines = new ArrayList<String>();
                String line;
                while ((line = in.readLine()) != null) {
                    lines.add(line);
                    chars += line.length();
                }
                pages.add(lines);
            } else {
                while (zip.read(buffer) > 0) {
                    // drained, as the converter copies it
                }
            }
        }
        zip.close();
        final long read = System.currentTimeMillis() - started;

        // ---- tokenize: the character scan every line goes through
        final HypenUtils.TokensListener nothing = new HypenUtils.TokensListener() {
            @Override
            public void findText(final String text) {
            }

            @Override
            public void findOther(final char ch) {
            }
        };
        started = System.currentTimeMillis();
        for (List<String> lines : pages) {
            HypenUtils.resetTokenizer();
            for (String line : lines) {
                HypenUtils.tokenize(line, nothing);
            }
        }
        final long tokenize = System.currentTimeMillis() - started;

        // ---- hyphenate: every line, with no word known yet and then with all of them known
        HypenUtils.cache.clear();
        started = System.currentTimeMillis();
        for (List<String> lines : pages) {
            HypenUtils.resetTokenizer();
            for (String line : lines) {
                HypenUtils.applyHypnes(line, replacements);
            }
        }
        final long cold = System.currentTimeMillis() - started;
        final int words = HypenUtils.cache.size();
        started = System.currentTimeMillis();
        for (List<String> lines : pages) {
            HypenUtils.resetTokenizer();
            for (String line : lines) {
                HypenUtils.applyHypnes(line, replacements);
            }
        }
        final long warm = System.currentTimeMillis() - started;

        // ---- the conversion itself, as on a first open and then with every word known
        final File out = new File(CacheZipUtils.CACHE_TEMP, "hyphens-" + book.getName());
        HypenUtils.cache.clear();
        started = System.currentTimeMillis();
        EpubExtractor.proccessHypens(book.getPath(), out.getPath(), null);
        final long convert = System.currentTimeMillis() - started;
        started = System.currentTimeMillis();
        EpubExtractor.proccessHypens(book.getPath(), out.getPath(), null);
        final long convertWarm = System.currentTimeMillis() - started;
        assertTrue("converted " + name, out.isFile());

        final String digest = digest(out);
        shell("cp " + out.getPath() + " " + DIR + "converted/" + book.getName());

        Log.i(TAG, String.format(Locale.US, "%s %s: convert %d ms (%d with every word known), %d pages,"
                        + " %d KB of text, %d words; patterns %d, read %d, tokenize %d, hyphenate %d cold / %d warm;"
                        + " %d KB -> %d KB, digest %s",
                name, lang, convert, convertWarm, pages.size(), chars / 1024, words, patterns, read, tokenize,
                cold, warm, book.length() / 1024, out.length() / 1024, digest));
        out.delete();
        book.delete();
    }

    /** The entries proccessHypensApache passes through generateHyphenFileEpub. */
    private static boolean isRewritten(final String name) {
        final String low = name.toLowerCase(Locale.US);
        if (low.contains("encryption.xml") || low.contains("container.xml") || low.contains("nav")
                || low.contains("toc")) {
            return false;
        }
        return !low.endsWith(".css") && (low.endsWith("html") || low.endsWith("htm") || low.endsWith("xml"));
    }

    /** The digest of every entry's name and bytes, in the order they were written. */
    private static String digest(final File book) throws Exception {
        final MessageDigest all = MessageDigest.getInstance("SHA-256");
        final ZipInputStream in = new ZipInputStream(new java.io.FileInputStream(book));
        try {
            final byte[] buffer = new byte[64 * 1024];
            ZipEntry entry;
            while ((entry = in.getNextEntry()) != null) {
                all.update(entry.getName().getBytes("UTF-8"));
                int read;
                while ((read = in.read(buffer)) > 0) {
                    all.update(buffer, 0, read);
                }
            }
        } finally {
            in.close();
        }
        final StringBuilder hex = new StringBuilder();
        for (byte b : all.digest()) {
            hex.append(String.format("%02x", b));
        }
        return hex.substring(0, 16);
    }

    private static void shell(final String command) {
        try {
            final ParcelFileDescriptor pfd = InstrumentationRegistry.getInstrumentation().getUiAutomation()
                    .executeShellCommand(command);
            final InputStream in = new ParcelFileDescriptor.AutoCloseInputStream(pfd);
            while (in.read() >= 0) {
                // waits for the command to finish
            }
            in.close();
        } catch (Exception e) {
            Log.w(TAG, command, e);
        }
    }

    /** The book in the app's own cache, so nothing depends on reading /data/local/tmp directly. */
    private static File copy(final Context context, final String name) throws Exception {
        final File out = new File(context.getCacheDir(), "hyphens-" + name.replace('/', '-'));
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
}
