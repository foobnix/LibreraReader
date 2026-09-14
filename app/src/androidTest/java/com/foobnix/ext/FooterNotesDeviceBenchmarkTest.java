package com.foobnix.ext;

import static org.junit.Assert.assertEquals;

import android.app.UiAutomation;
import android.os.Build;
import android.os.ParcelFileDescriptor;
import android.util.Log;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import com.foobnix.pdf.info.AppsConfig;
import com.foobnix.sys.TempHolder;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Footnotes of real books on a device: master's code ("old") and the rewrite with zip4j and with java.util.zip.
 * Books are read from /data/local/tmp/footnotes (adb push), results go to logcat with the tag FootnotesBench:
 * adb logcat -d -s FootnotesBench
 */
@RunWith(AndroidJUnit4.class)
public class FooterNotesDeviceBenchmarkTest {

    private static final String TAG = "FootnotesBench";
    private static final String BOOKS = "/data/local/tmp/footnotes";
    private static final int ROUNDS = 3;

    interface Extractor {
        Map<String, String> notes(String path);
    }

    @Test
    public void benchmark() throws Exception {
        AppsConfig.IS_LOG = false; // the emulator turns logging on, it would slow the old code most
        TempHolder.get().loadingCancelled.set(false);
        File dir = new File(InstrumentationRegistry.getInstrumentation().getTargetContext().getCacheDir(), "footnotes");
        dir.mkdirs();

        Log.i(TAG, "device " + Build.MODEL + " Android " + Build.VERSION.RELEASE + " API " + Build.VERSION.SDK_INT);
        Log.i(TAG, String.format("%-8s %5s %8s %8s %8s  %s", "book", "notes", "old", "zip4j", "javazip", "zip4j==javazip, old keys kept"));
        // am instrument -e books .fb2 runs only the books whose name has that text ("filter" belongs to the runner)
        String filter = InstrumentationRegistry.getArguments().getString("books", "");
        long[] total = new long[3];
        for (String name : shell("ls " + BOOKS).split("\n")) {
            name = name.trim();
            if (name.isEmpty() || !name.contains(filter)) {
                continue;
            }
            File book = copy(BOOKS + "/" + name, new File(dir, name));
            String path = book.getPath();
            boolean fb2 = name.endsWith(".fb2");

            Extractor old = p -> fb2 ? LegacyFooterNotes.fb2(p) : LegacyFooterNotes.epub(p);
            Extractor zip4j = p -> fb2 ? Fb2Extractor.get().getFooterNotes(p) : EpubExtractor.get().getFooterNotes(p, false);
            Extractor javaZip = p -> EpubExtractor.get().getFooterNotes(p, true);

            List<Map<String, String>> results = new ArrayList<>();
            long oldMs = best(old, path, results);
            long zip4jMs = best(zip4j, path, results);
            long javaZipMs = fb2 ? -1 : best(javaZip, path, results);

            Map<String, String> oldNotes = results.get(0);
            Map<String, String> newNotes = results.get(1);
            int kept = 0;
            for (String key : oldNotes.keySet()) {
                if (key != null && !key.startsWith("null#") && newNotes.containsKey(key)) {
                    kept++;
                }
            }
            int oldKeys = 0;
            for (String key : oldNotes.keySet()) {
                if (key != null && !key.startsWith("null#")) {
                    oldKeys++;
                }
            }
            String same = fb2 ? "-" : String.valueOf(results.get(2).equals(newNotes));
            Log.i(TAG, String.format("%-8s %5d %8d %8d %8s  %s, %d/%d", name, newNotes.size(), oldMs, zip4jMs,
                    fb2 ? "-" : String.valueOf(javaZipMs), same, kept, oldKeys));
            if (!fb2) {
                assertEquals(name + ": java.util.zip and zip4j give other notes", newNotes, results.get(2));
                total[2] += javaZipMs;
            }
            total[0] += oldMs;
            total[1] += zip4jMs;
            book.delete();
        }
        Log.i(TAG, String.format("total ms: old %d, zip4j %d, java.util.zip %d (EPUB only)", total[0], total[1], total[2]));
    }

    // best of a few runs, the notes of the first one are kept; the app cancels loading on low memory
    // (LibreraApp.onTrimMemory), a cancelled run is reported and not counted
    private static long best(Extractor extractor, String path, List<Map<String, String>> results) {
        long best = Long.MAX_VALUE;
        Map<String, String> notes = null;
        for (int i = 0; i < ROUNDS; i++) {
            TempHolder.get().loadingCancelled.set(false);
            long start = System.nanoTime();
            Map<String, String> result = extractor.notes(path);
            long time = System.nanoTime() - start;
            if (TempHolder.get().loadingCancelled.getAndSet(false)) {
                Log.w(TAG, "cancelled by the app while reading " + path);
                continue;
            }
            best = Math.min(best, time);
            if (notes == null) {
                notes = result;
            }
        }
        results.add(notes == null ? new java.util.HashMap<String, String>() : notes);
        return best == Long.MAX_VALUE ? -1 : best / 1_000_000;
    }

    // the shell user can read /data/local/tmp, the app cannot
    private static File copy(String from, File to) throws Exception {
        UiAutomation automation = InstrumentationRegistry.getInstrumentation().getUiAutomation();
        ParcelFileDescriptor pfd = automation.executeShellCommand("cat " + from);
        try (InputStream in = new FileInputStream(pfd.getFileDescriptor()); OutputStream out = new FileOutputStream(to)) {
            byte[] buffer = new byte[64 * 1024];
            int n;
            while ((n = in.read(buffer)) != -1) {
                out.write(buffer, 0, n);
            }
        } finally {
            pfd.close();
        }
        return to;
    }

    private static String shell(String command) throws Exception {
        ParcelFileDescriptor pfd = InstrumentationRegistry.getInstrumentation().getUiAutomation().executeShellCommand(command);
        StringBuilder out = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(pfd.getFileDescriptor())))) {
            String line;
            while ((line = reader.readLine()) != null) {
                out.append(line).append('\n');
            }
        } finally {
            pfd.close();
        }
        return out.toString();
    }
}
