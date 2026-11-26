package com.foobnix.work;

import android.content.Context;

import com.foobnix.android.utils.LOG;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.nio.charset.StandardCharsets;

/**
 * Where a library scan got to, so that the same scan, started again by WorkManager after the app
 * was killed or the system stopped it, carries on with the books it had not read yet instead of
 * reading all of them again.
 * <p>
 * A new scan has a new work id, so a scan the user asks for always starts from the beginning.
 * The book being read is written before it is opened: a scan that dies on the same book twice -
 * a native crash in a broken file - leaves that book with its file name only and goes on.
 */
final class LibraryScanResume {

    private static final String FILE_NAME = "library-scan-resume.txt";

    private final File file;
    String workId;
    String profile;
    /** The book being read when the scan stopped, or null. */
    String current;
    /** The book the scan was already restarted on once, or null. */
    String retried;

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
            r.current = emptyToNull(in.readLine());
            r.retried = emptyToNull(in.readLine());
        } catch (Exception e) {
            LOG.e(e);
        }
        return r;
    }

    boolean isFor(String workId, String profile) {
        return workId != null && workId.equals(this.workId) && profile != null && profile.equals(this.profile);
    }

    void start(String workId, String profile) {
        this.workId = workId;
        this.profile = profile;
        current = null;
        retried = null;
        save();
    }

    /** Written before the book is opened; a plain write, cheap enough for every book. */
    void setCurrent(String path) {
        current = path;
        save();
    }

    void save() {
        final String text = nullToEmpty(workId) + "\n" + nullToEmpty(profile) + "\n" + nullToEmpty(current) + "\n"
                + nullToEmpty(retried) + "\n";
        try (FileOutputStream out = new FileOutputStream(file)) {
            out.write(text.getBytes(StandardCharsets.UTF_8));
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
