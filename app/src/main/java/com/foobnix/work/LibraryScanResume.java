package com.foobnix.work;

import android.content.Context;

import com.foobnix.android.utils.LOG;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Where a library scan got to, so that a scan stopped before it finished - the app killed, or the
 * system stopping the work - carries on with the books it had not read yet instead of reading all
 * of them again.
 * <p>
 * WorkManager runs a stopped scan again under the same work id; the app cancels the scan when it
 * starts, and the library screen then starts one that carries on ({@code SearchAllBooksWorker.resume}).
 * Any other scan - one the user asks for - starts from the beginning.
 * <p>
 * The books being read are written before they are opened (several at once: the scan reads in
 * more than one thread). A book the scan was stopped on is read again once; one it was stopped on
 * twice - a native crash in a broken file - is left with its file name only.
 * <p>
 * The file: the work id, the profile, then a line per book, "c path" being read, "r path" read
 * again once already.
 */
final class LibraryScanResume {

    private static final String FILE_NAME = "library-scan-resume.txt";

    private final File file;
    String workId;
    String profile;
    /** The books being read; when read from the file, the ones the scan stopped on. */
    final Set<String> current = new LinkedHashSet<>();
    /** The books the scan was already restarted on once. */
    final Set<String> retried = new LinkedHashSet<>();

    private LibraryScanResume(Context c) {
        file = new File(c.getFilesDir(), FILE_NAME);
    }

    static LibraryScanResume read(Context c) {
        final LibraryScanResume r = new LibraryScanResume(c);
        if (!r.file.isFile()) {
            return r;
        }
        try (BufferedReader in = new BufferedReader(new FileReader(r.file))) {
            r.workId = emptyToNull(in.readLine());
            r.profile = emptyToNull(in.readLine());
            String line;
            while ((line = in.readLine()) != null) {
                if (line.startsWith("c ")) {
                    r.current.add(line.substring(2));
                } else if (line.startsWith("r ")) {
                    r.retried.add(line.substring(2));
                }
            }
        } catch (Exception e) {
            LOG.e(e);
        }
        return r;
    }

    /** A scan of this profile stopped before it finished, and can carry on. */
    boolean isFor(String profile) {
        return workId != null && profile != null && profile.equals(this.profile);
    }

    synchronized void start(String workId, String profile) {
        this.workId = workId;
        this.profile = profile;
        current.clear();
        retried.clear();
        save();
    }

    /**
     * For a scan that carries on: the books it was stopped on are read once more, and the ones it
     * was stopped on a second time are returned, not to be read.
     */
    synchronized Set<String> carryOn(String workId) {
        this.workId = workId;
        final Set<String> skip = new LinkedHashSet<>();
        for (String path : current) {
            if (!retried.add(path)) {
                skip.add(path);
            }
        }
        current.clear();
        save();
        return skip;
    }

    /** Written before the book is opened: a plain write, cheap enough for every book. */
    synchronized void begin(String path) {
        current.add(path);
        save();
    }

    synchronized void end(String path) {
        if (current.remove(path)) {
            save();
        }
    }

    synchronized void save() {
        final StringBuilder text = new StringBuilder();
        text.append(nullToEmpty(workId)).append('\n').append(nullToEmpty(profile)).append('\n');
        for (String path : current) {
            text.append("c ").append(path).append('\n');
        }
        for (String path : retried) {
            text.append("r ").append(path).append('\n');
        }
        try (FileOutputStream out = new FileOutputStream(file)) {
            out.write(text.toString().getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            LOG.e(e);
        }
    }

    void delete() {
        file.delete();
    }

    private static String emptyToNull(String s) {
        return s == null || s.isEmpty() ? null : s;
    }

    private static String nullToEmpty(String s) {
        return s == null ? "" : s;
    }
}
