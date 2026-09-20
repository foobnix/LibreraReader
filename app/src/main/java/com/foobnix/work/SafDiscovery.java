package com.foobnix.work;

import android.content.Context;
import android.net.Uri;
import com.foobnix.dao2.FileMeta;
import com.foobnix.pdf.info.ExtUtils;
import com.foobnix.pdf.info.SafDocumentIdentity;
import com.foobnix.pdf.info.io.SearchCore;
import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.BooleanSupplier;

/** Sequential selected-folder traversal; any incomplete listing invalidates removals. */
final class SafDiscovery {
    private SafDiscovery() {}

    interface DirectoryListing {
        List<SafDocuments.Document> list(Uri folder, BooleanSupplier stopped)
                throws IOException, InterruptedException;
    }

    static void collect(Context context, Uri root, List<FileMeta> output,
                        BooleanSupplier stopped) throws IOException, InterruptedException {
        collect(root, output, stopped,
                (folder, cancelled) -> SafDocuments.list(context, folder, cancelled));
    }

    static void collect(Uri root, List<FileMeta> output, BooleanSupplier stopped,
                        DirectoryListing listing) throws IOException, InterruptedException {
        walk(root, output, stopped, new HashSet<>(), listing);
    }

    private static void walk(Uri folder, List<FileMeta> output,
                             BooleanSupplier stopped, Set<Uri> visited, DirectoryListing listing)
            throws IOException, InterruptedException {
        if (stopped.getAsBoolean()) throw new IOException("SAF scan cancelled");
        // Shared children form a DAG; visiting once also bounds actual cycles.
        if (!visited.add(SafDocumentIdentity.canonical(folder))) return;
        for (SafDocuments.Document child : listing.list(folder, stopped)) {
            if (stopped.getAsBoolean()) throw new IOException("SAF scan cancelled");
            if (child.directory) {
                walk(child.uri, output, stopped, visited, listing);
            } else if (child.name != null && SearchCore.endWith(child.name, ExtUtils.seachExts)) {
                output.add(child.book());
            }
        }
    }
}
