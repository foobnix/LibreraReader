package com.foobnix.work;

import android.content.Context;
import android.net.Uri;
import com.foobnix.dao2.FileMeta;
import com.foobnix.pdf.info.ExtUtils;
import com.foobnix.pdf.info.SafDocumentIdentity;
import com.foobnix.pdf.info.SafOpfRegistry;
import com.foobnix.pdf.info.io.SearchCore;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
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
        collect(context, root, output, new HashMap<>(), stopped);
    }

    static void collect(Context context, Uri root, List<FileMeta> output,
                        Map<String, SafOpfRegistry.Entry> sidecars, BooleanSupplier stopped)
            throws IOException, InterruptedException {
        collect(root, output, sidecars, stopped,
                (folder, cancelled) -> SafDocuments.list(context, folder, cancelled));
    }

    static void collect(Uri root, List<FileMeta> output,
                        Map<String, SafOpfRegistry.Entry> sidecars,
                        BooleanSupplier stopped, DirectoryListing listing)
            throws IOException, InterruptedException {
        walk(root, output, sidecars, stopped, new HashSet<>(), listing);
    }

    private static void walk(Uri folder, List<FileMeta> output,
                             Map<String, SafOpfRegistry.Entry> sidecars,
                             BooleanSupplier stopped, Set<Uri> visited, DirectoryListing listing)
            throws IOException, InterruptedException {
        if (stopped.getAsBoolean()) throw new IOException("SAF scan cancelled");
        if (!visited.add(SafDocumentIdentity.canonical(folder))) return;
        List<SafDocuments.Document> children = listing.list(folder, stopped);
        recordSidecars(children, sidecars);
        for (SafDocuments.Document child : children) {
            if (stopped.getAsBoolean()) throw new IOException("SAF scan cancelled");
            if (child.directory) {
                walk(child.uri, output, sidecars, stopped, visited, listing);
            } else if (child.name != null && SearchCore.endWith(child.name, ExtUtils.seachExts)) {
                output.add(child.book());
            }
        }
    }

    static void recordSidecars(List<SafDocuments.Document> children,
                               Map<String, SafOpfRegistry.Entry> sidecars) {
        Map<String, Uri> siblings = new HashMap<>();
        List<String> revisions = new ArrayList<>();
        for (SafDocuments.Document child : children) {
            if (child.directory || child.name == null) continue;
            String name = child.name.toLowerCase(Locale.US);
            if (ExtUtils.imageExts.stream().anyMatch(name::endsWith) || name.endsWith(".opf"))
                siblings.put(name, child.uri);
            if (name.endsWith(".opf") || name.endsWith(".jpg") || name.endsWith(".jpeg")
                    || name.endsWith(".png")) {
                revisions.add(SidecarRevision.file(name, child.size, child.modified));
            }
        }
        String revision = SidecarRevision.combine(revisions);
        for (SafDocuments.Document child : children) {
            if (!child.directory && child.name != null
                    && SearchCore.endWith(child.name, ExtUtils.seachExts)) {
                String stem = ExtUtils.getFileNameWithoutExt(child.name).toLowerCase(Locale.US);
                Uri opf = siblings.get(stem + ".opf");
                if (opf == null) opf = siblings.get("metadata.opf");
                if (opf != null) sidecars.put(SafDocumentIdentity.canonical(child.uri).toString(),
                        new SafOpfRegistry.Entry(opf, siblings, revision));
            }
        }
    }
}
