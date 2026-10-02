package com.foobnix.work;

import android.content.Context;
import android.net.Uri;
import com.foobnix.dao2.FileMeta;
import com.foobnix.pdf.info.ExtUtils;
import com.foobnix.pdf.info.SafDocumentIdentity;
import com.foobnix.pdf.info.SafOpfRegistry;
import com.foobnix.pdf.info.Tunables;
import com.foobnix.pdf.info.io.SearchCore;
import java.io.IOException;
import java.util.ArrayList;
import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.function.BooleanSupplier;

/** Bounded selected-folder traversal; any incomplete listing invalidates removals. */
final class SafDiscovery {
    private SafDiscovery() {}

    interface DirectoryListing {
        List<SafDocuments.Document> list(Uri folder, BooleanSupplier stopped)
                throws IOException, InterruptedException;
    }

    interface BatchListener {
        void discovered(List<FileMeta> books, Map<String, SafOpfRegistry.Entry> sidecars)
                throws IOException;
    }

    private static final class FolderResult {
        final Uri folder;
        final List<SafDocuments.Document> children;
        FolderResult(Uri folder, List<SafDocuments.Document> children) {
            this.folder = folder;
            this.children = children;
        }
    }

    static void collect(Context context, Uri root, List<FileMeta> output,
                        BooleanSupplier stopped) throws IOException, InterruptedException {
        collect(context, root, output, new HashMap<>(), stopped);
    }

    static void collect(Context context, Uri root, List<FileMeta> output,
                        Map<String, SafOpfRegistry.Entry> sidecars, BooleanSupplier stopped)
            throws IOException, InterruptedException {
        collect(context, root, output, sidecars, stopped, (books, entries) -> {});
    }

    static void collect(Context context, Uri root, List<FileMeta> output,
                        Map<String, SafOpfRegistry.Entry> sidecars, BooleanSupplier stopped,
                        BatchListener listener) throws IOException, InterruptedException {
        collect(root, output, sidecars, stopped,
                (folder, cancelled) -> SafDocuments.list(context, folder, cancelled), listener);
    }

    static void collect(Uri root, List<FileMeta> output,
                        Map<String, SafOpfRegistry.Entry> sidecars,
                        BooleanSupplier stopped, DirectoryListing listing)
            throws IOException, InterruptedException {
        collect(root, output, sidecars, stopped, listing, (books, entries) -> {});
    }

    static void collect(Uri root, List<FileMeta> output,
                        Map<String, SafOpfRegistry.Entry> sidecars,
                        BooleanSupplier stopped, DirectoryListing listing, BatchListener listener)
            throws IOException, InterruptedException {
        int parallelism = Math.max(1, Tunables.SAF_DISCOVERY_PARALLELISM);
        ExecutorService workers = Executors.newFixedThreadPool(parallelism);
        ExecutorCompletionService<FolderResult> completed = new ExecutorCompletionService<>(workers);
        ArrayDeque<Uri> queued = new ArrayDeque<>();
        Set<Uri> visited = new HashSet<>();
        List<FileMeta> found = new ArrayList<>();
        Map<String, SafOpfRegistry.Entry> foundSidecars = new HashMap<>();
        queued.add(root);
        visited.add(SafDocumentIdentity.canonical(root));
        int pending = 0;
        try {
            while (!queued.isEmpty() || pending != 0) {
                if (stopped.getAsBoolean()) throw new IOException("SAF scan cancelled");
                while (pending < parallelism && !queued.isEmpty()) {
                    Uri folder = queued.remove();
                    completed.submit(() -> new FolderResult(folder,
                            listing.list(folder, () -> stopped.getAsBoolean()
                                    || Thread.currentThread().isInterrupted())));
                    pending++;
                }
                Future<FolderResult> next = completed.poll(100, TimeUnit.MILLISECONDS);
                if (next == null) continue;
                pending--;
                FolderResult result;
                try {
                    result = next.get();
                } catch (ExecutionException failed) {
                    Throwable cause = failed.getCause();
                    if (cause instanceof InterruptedException) throw (InterruptedException) cause;
                    if (cause instanceof IOException) throw (IOException) cause;
                    throw new IOException("Cannot list SAF folder", cause);
                }
                Map<String, SafOpfRegistry.Entry> batchSidecars = new HashMap<>();
                List<FileMeta> batch = new ArrayList<>();
                recordSidecars(result.children, batchSidecars);
                for (SafDocuments.Document child : result.children) {
                    if (stopped.getAsBoolean()) throw new IOException("SAF scan cancelled");
                    if (child.directory) {
                        if (visited.add(SafDocumentIdentity.canonical(child.uri))) queued.add(child.uri);
                    } else if (child.name != null
                            && SearchCore.endWith(child.name, ExtUtils.seachExts)) {
                        batch.add(child.book());
                    }
                }
                if (!batch.isEmpty()) {
                    listener.discovered(batch, batchSidecars);
                    found.addAll(batch);
                    foundSidecars.putAll(batchSidecars);
                }
            }
        } finally {
            output.addAll(found);
            sidecars.putAll(foundSidecars);
            workers.shutdownNow();
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
