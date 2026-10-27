package com.foobnix.ext;

import com.foobnix.android.utils.LOG;
import com.foobnix.sys.TempHolder;

import org.junit.Assume;
import org.junit.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * Opt-in benchmark: runs master's footnote code ("old") and the rewrite with zip4j and with java.util.zip
 * over real books, records the time and a dump of every notes map.
 * Skipped unless FOOTNOTES_BOOKS (file with one book path per line) and FOOTNOTES_OUT (output dir) are set.
 * <p>
 * FOOTNOTES_BOOKS=books.txt FOOTNOTES_OUT=out ./gradlew :app:testFdroidDebugUnitTest
 * --tests com.foobnix.ext.FooterNotesBenchmarkTest --rerun
 * <p>
 * out/summary.tsv has one line per book and variant: variant, status, ms, notes, hash of the notes, path, exception
 */
public class FooterNotesBenchmarkTest {

    private static final long TIMEOUT_SEC = 300;

    interface Extractor {
        Map<String, String> notes(String path, boolean fb2);
    }

    @Test
    public void extractNotes() throws Exception {
        String list = System.getenv("FOOTNOTES_BOOKS");
        String outDir = System.getenv("FOOTNOTES_OUT");
        Assume.assumeTrue(list != null && outDir != null);

        // not a constant in android.jar, so it is null in JVM tests and kxml rejects it
        android.util.Xml.FEATURE_RELAXED = "http://xmlpull.org/v1/doc/features.html#relaxed";

        Map<String, Extractor> variants = new LinkedHashMap<>();
        variants.put("old", (path, fb2) -> fb2 ? LegacyFooterNotes.fb2(path) : LegacyFooterNotes.epub(path));
        variants.put("zip4j", (path, fb2) -> fb2 ? Fb2Extractor.get().getFooterNotes(path) : EpubExtractor.get().getFooterNotes(path, false));
        variants.put("java.util.zip", (path, fb2) -> fb2 ? null : EpubExtractor.get().getFooterNotes(path, true));
        for (String variant : variants.keySet()) {
            new File(outDir, "maps/" + variant).mkdirs();
        }

        ScheduledExecutorService watchdog = Executors.newSingleThreadScheduledExecutor();
        // extractors swallow exceptions into LOG.e, keep the first one per run
        Throwable[] logged = new Throwable[1];
        // stubOnly: a normal mock records every LOG call with its arguments and runs out of memory;
        // mockStatic is thread-local, so extraction runs on this thread and the watchdog only cancels it
        try (MockedStatic<LOG> ignored = Mockito.mockStatic(LOG.class, Mockito.withSettings().stubOnly().defaultAnswer(invocation -> {
            if (invocation.getMethod().getName().equals("e") && logged[0] == null
                    && invocation.getArguments().length > 0 && invocation.getArgument(0) instanceof Throwable) {
                logged[0] = invocation.getArgument(0);
            }
            return null;
        }));
             PrintWriter summary = new PrintWriter(new OutputStreamWriter(
                     new FileOutputStream(new File(outDir, "summary.tsv")), StandardCharsets.UTF_8))) {
            for (String path : Files.readAllLines(new File(list).toPath(), StandardCharsets.UTF_8)) {
                File file = new File(path);
                if (!file.isFile()) {
                    continue;
                }
                warmUp(file);
                boolean fb2 = path.toLowerCase(Locale.US).endsWith(".fb2");

                for (Map.Entry<String, Extractor> variant : variants.entrySet()) {
                    if (fb2 && variant.getKey().equals("java.util.zip")) {
                        continue;
                    }
                    TempHolder.get().loadingCancelled.set(false);
                    ScheduledFuture<?> timeout = watchdog.schedule(
                            () -> TempHolder.get().loadingCancelled.set(true), TIMEOUT_SEC, TimeUnit.SECONDS);

                    String status = "OK";
                    Map<String, String> notes = null;
                    logged[0] = null;
                    long start = System.nanoTime();
                    try {
                        notes = variant.getValue().notes(path, fb2);
                    } catch (Throwable e) {
                        status = "ERROR " + e.getClass().getSimpleName();
                    }
                    long ms = (System.nanoTime() - start) / 1_000_000;

                    timeout.cancel(false);
                    if (TempHolder.get().loadingCancelled.getAndSet(false)) {
                        status = "TIMEOUT";
                    }

                    String dump = dump(notes);
                    Files.write(new File(outDir, "maps/" + variant.getKey() + "/" + sha1(path) + ".txt").toPath(),
                            (path + "\n" + dump).getBytes(StandardCharsets.UTF_8));
                    String error = logged[0] == null ? "" : escape(describe(logged[0]));
                    summary.println(variant.getKey() + "\t" + status + "\t" + ms + "\t" + (notes == null ? -1 : notes.size())
                            + "\t" + sha1(dump) + "\t" + path + "\t" + error);
                }
                summary.flush();
            }
        } finally {
            watchdog.shutdownNow();
        }
    }

    // reads the file once so disk and cloud-drive latency is not counted in the timing
    private static void warmUp(File file) throws Exception {
        byte[] buffer = new byte[1 << 16];
        try (InputStream in = new FileInputStream(file)) {
            while (in.read(buffer) > 0) {
                // discard
            }
        }
    }

    // root cause plus where it was thrown, e.g. to see why a static initializer failed
    private static String describe(Throwable e) {
        Throwable root = e;
        while (root.getCause() != null && root.getCause() != root) {
            root = root.getCause();
        }
        StringBuilder out = new StringBuilder(e.toString());
        if (root != e) {
            out.append(" <- ").append(root);
        }
        StackTraceElement[] trace = root.getStackTrace();
        for (int i = 0; i < Math.min(4, trace.length); i++) {
            out.append(" @ ").append(trace[i]);
        }
        return out.toString();
    }

    private static String dump(Map<String, String> notes) {
        if (notes == null) {
            return "null\n";
        }
        List<String> lines = new ArrayList<>();
        for (Map.Entry<String, String> e : notes.entrySet()) {
            lines.add(escape(String.valueOf(e.getKey())) + "\t" + escape(String.valueOf(e.getValue())));
        }
        lines.sort(null);
        StringBuilder out = new StringBuilder();
        for (String line : lines) {
            out.append(line).append('\n');
        }
        return out.toString();
    }

    private static String escape(String s) {
        return s.replace("\\", "\\\\").replace("\n", "\\n").replace("\r", "\\r").replace("\t", "\\t");
    }

    private static String sha1(String s) throws Exception {
        byte[] digest = MessageDigest.getInstance("SHA-1").digest(s.getBytes(StandardCharsets.UTF_8));
        StringBuilder hex = new StringBuilder();
        for (byte b : digest) {
            hex.append(String.format("%02x", b));
        }
        return hex.toString();
    }
}
