package com.foobnix.pdf.info;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import android.system.ErrnoException;
import android.system.Os;

/** Coordinates publication, reader handoff and eviction of book cache files. */
public final class BookCacheLeases {
    private static final Map<String, Integer> leases = new HashMap<>();
    static final ExecutorService CLEANUP = Executors.newSingleThreadExecutor(work ->
            new Thread(() -> {
                android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_BACKGROUND);
                work.run();
            }, "book-cache-cleanup"));

    /** Cleanup must never queue ahead of document loading, page work, or settings saves. */
    public static void scheduleCleanup(Runnable work) { CLEANUP.execute(work); }

    private static final Set<String> initializedFolders = new HashSet<>();

    /** Crash leftovers only: ordinary cache outputs and active leases remain untouched. */
    public static void sweepAbandoned(File directory) {
        File[] files = directory.listFiles();
        if (files == null) return;
        for (File file : files) {
            String name = file.getName();
            if (name.startsWith(".evicted-")) {
                evictTree(file);
            }
        }
    }

    public static synchronized File temporary(File directory, String prefix) throws IOException {
        if (initializedFolders.add(key(directory))) {
            // This runs before any operation in this process creates a temporary file here.
            File[] abandoned = directory.listFiles(file -> file.isFile() && file.getName().endsWith(".part") || file.getName().startsWith(".evicted-"));
            if (abandoned != null) for (File file : abandoned) retireTree(file);
        }
        return File.createTempFile(prefix, ".part", directory);
    }

    private static String key(File file) { return file.getAbsolutePath(); }
    private static void add(Map<String, Integer> counts, File file) {
        counts.merge(key(file), 1, Integer::sum);
    }
    private static void remove(Map<String, Integer> counts, File file) {
        counts.computeIfPresent(key(file), (path, count) -> count == 1 ? null : count - 1);
    }




    public static synchronized AutoCloseable acquire(File file) {
        add(leases, file);
        return new AutoCloseable() {
            private boolean closed;
            @Override public void close() {
                synchronized (BookCacheLeases.class) {
                    if (!closed) { closed = true; remove(leases, file); }
                }
            }
        };
    }

    public static synchronized void readerOpened(File file) { add(leases, file); }
    public static synchronized void readerClosed(File file) { remove(leases, file); }

    private static boolean overlaps(String path, String protectedPath) {
        return path.equals(protectedPath) || path.startsWith(protectedPath + File.separator)
                || protectedPath.startsWith(path + File.separator);
    }

    private static boolean protectedBy(Map<String, Integer> counts, File file) {
        String path = key(file);
        for (String held : counts.keySet()) {
            if (overlaps(path, held)) return true;
        }
        return false;
    }

    /** A held directory protects descendants; a held child protects its ancestors. */
    public static synchronized boolean isProtected(File file) {
        return protectedBy(leases, file);
    }

    public static synchronized boolean evict(File file) {
        if (isProtected(file)) return false;
        return file.delete();
    }

    /** Do not partly remove a directory while a reader uses anything inside it. */
    public static boolean evictTree(File file) {
        File tombstone = detachTree(file);
        return tombstone != null && deleteDetachedTree(tombstone);
    }

    public static synchronized File detachTree(File file) {
        if (isProtected(file) || !file.exists()) return null;
        File tombstone = new File(file.getParentFile(), ".evicted-" + UUID.randomUUID());
        try { Os.rename(file.getAbsolutePath(), tombstone.getAbsolutePath()); }
        catch (ErrnoException unavailable) { return null; }
        return tombstone;
    }

    /** Detach synchronously; recursive deletion never runs in a caller's lease monitor. */
    private static boolean retireTree(File file) {
        File tombstone = detachTree(file);
        if (tombstone == null) return false;
        scheduleCleanup(() -> deleteDetachedTree(tombstone));
        return true;
    }

    private static boolean deleteDetachedTree(File file) {
        boolean directory;
        try { directory = android.system.OsConstants.S_ISDIR(Os.lstat(file.getAbsolutePath()).st_mode); }
        catch (ErrnoException unavailable) { return false; }
        if (directory) {
            File[] children = file.listFiles();
            if (children == null) return false;
            for (File child : children) if (!deleteDetachedTree(child)) return false;
        }
        return file.delete();
    }

    /** Atomic replacement never removes the previous good file before publication succeeds. */
    public static synchronized void publish(File temporary, File destination) throws IOException {
        try {
            Os.rename(temporary.getAbsolutePath(), destination.getAbsolutePath());
        } catch (ErrnoException failure) {
            throw new IOException("Cannot publish book cache file", failure);
        }
    }
}
