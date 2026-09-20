package com.foobnix.work;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Revisions of metadata and cover files, independent of the book's modification time. */
final class SidecarRevision {
    static String file(String name, Long size, Long modified) {
        // Providers without usable timestamps cannot establish an unchanged revision.
        String version = size == null || modified == null || modified <= 0
                ? "unknown:" + java.util.UUID.randomUUID() : size + ":" + modified;
        return name.length() + ":" + name + ":" + version;
    }

    static String combine(List<String> files) {
        List<String> sorted = new ArrayList<>(files);
        Collections.sort(sorted);
        StringBuilder result = new StringBuilder("sidecars:");
        for (String file : sorted) result.append(file.length()).append(':').append(file);
        return result.toString();
    }
}
