package com.foobnix.work;

import com.foobnix.dao2.FileMeta;
import com.foobnix.model.AppState;
import com.foobnix.pdf.info.io.SearchCore;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.Function;

/** Local listing that distinguishes a complete empty folder from an unreadable one. */
final class LocalDiscovery {
    private LocalDiscovery() {}

    static void collect(File root, List<String> extensions, List<FileMeta> output,
                        BooleanSupplier stopped) throws IOException {
        collect(root, extensions, output, stopped, File::listFiles);
    }

    static void collect(File root, List<String> extensions, List<FileMeta> output,
                        BooleanSupplier stopped, Function<File, File[]> list) throws IOException {
        java.util.Set<String> incomplete = new java.util.HashSet<>();
        collectPartial(root, extensions, output, stopped, list, incomplete);
        if (!incomplete.isEmpty()) throw new IOException("Cannot list local folder: " + incomplete.iterator().next());
    }

    /** Only unreadable prefixes lose absence evidence; readable siblings still reconcile. */
    static void collectPartial(File root, List<String> extensions, List<FileMeta> output,
                               BooleanSupplier stopped, Function<File, File[]> list,
                               java.util.Set<String> incomplete) throws IOException {
        boolean alreadyIncomplete = incomplete.contains(root.getPath());
        incomplete.add(root.getPath());
        if (!root.isDirectory()) throw new IOException("Scan root unavailable: " + root);
        if ("/".equals(root.getPath())) throw new IOException("Root directory scan is disabled");
        // A failed selected-root listing must not authorize any removal below it.
        File[] children = list.apply(root);
        if (children == null) throw new IOException("Cannot list local folder: " + root);
        walk(root, children, extensions, output, stopped, new boolean[1], list, incomplete);
        if (!alreadyIncomplete) incomplete.remove(root.getPath());
    }

    private static boolean isSymbolicLink(File file) {
        try {
            return android.system.OsConstants.S_ISLNK(android.system.Os.lstat(file.getPath()).st_mode);
        } catch (android.system.ErrnoException unavailable) { return false; }
    }

    private static void walk(File folder, File[] children, List<String> extensions, List<FileMeta> output,
                             BooleanSupplier stopped, boolean[] skippedAndroidData,
                             Function<File, File[]> list, java.util.Set<String> incomplete) throws IOException {
        if (stopped.getAsBoolean()) throw new IOException("Local scan cancelled");
        for (File child : children) {
            if (stopped.getAsBoolean()) throw new IOException("Local scan cancelled");
            if (child.isHidden()) continue;
            if (child.isDirectory()) {
                if (AppState.get().isSkipFolderWithNOMEDIA
                        && new File(child, SearchCore.NOMEDIA).isFile()) continue;
                if (!skippedAndroidData[0] && child.getPath().endsWith("/Android/data")) {
                    skippedAndroidData[0] = true;
                    // Intentional exclusion, not a failed traversal.
                    continue;
                }
                try {
                    File[] listed = list.apply(child);
                    if (listed == null) throw new IOException("Cannot list local folder: " + child);
                    walk(child, listed, extensions, output, stopped, skippedAndroidData, list, incomplete);
                } catch (IOException | RuntimeException unavailable) {
                    if (stopped.getAsBoolean()) throw new IOException("Local scan cancelled", unavailable);
                    incomplete.add(child.getPath());
                }
            } else if (child.isFile()) {
                if (child.length() > 0 && SearchCore.endWith(child.getName(), extensions)) {
                    FileMeta book = new FileMeta(child.getPath());
                    book.setTitle(child.getName());
                    output.add(book);
                }
            } else {
                // A path returned by the parent but no longer readable is not an absence proof.
                if (isSymbolicLink(child)) continue;
                incomplete.add(child.getPath());
            }
        }
    }
}
