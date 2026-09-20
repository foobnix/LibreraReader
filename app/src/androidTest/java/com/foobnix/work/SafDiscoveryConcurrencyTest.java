package com.foobnix.work;

import static org.junit.Assert.*;

import android.net.Uri;
import com.foobnix.dao2.FileMeta;
import com.foobnix.pdf.info.ExtUtils;
import com.foobnix.pdf.info.SafOpfRegistry;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.Test;

public class SafDiscoveryConcurrencyTest {
    private final Uri root = Uri.parse("content://fixture/tree/root/document/root");
    private final Uri first = Uri.parse("content://fixture/tree/root/document/first");
    private final Uri second = Uri.parse("content://fixture/tree/root/document/second");

    private SafDocuments.Document folder(String name, Uri uri) {
        return new SafDocuments.Document(name, uri, true, null, null);
    }
    private SafDocuments.Document book(String name, Uri uri) {
        return new SafDocuments.Document(name, uri, false, 10L, 20L);
    }

    @Test public void siblingFoldersListConcurrentlyAndPublishAllBooks() throws Exception {
        CountDownLatch bothStarted = new CountDownLatch(2);
        List<FileMeta> books = new ArrayList<>();
        Map<String, SafOpfRegistry.Entry> sidecars = new HashMap<>();
        boolean addedExtension = !ExtUtils.seachExts.contains(".epub");
        if (addedExtension) ExtUtils.seachExts.add(".epub");
        try {
            SafDiscovery.collect(root, books, sidecars, () -> false, (uri, stopped) -> {
                if (uri.equals(root)) return Arrays.asList(folder("first", first), folder("second", second));
                bothStarted.countDown();
                if (!bothStarted.await(2, TimeUnit.SECONDS))
                    throw new IOException("Folder listings did not overlap");
                return Collections.singletonList(book("book.epub", Uri.withAppendedPath(uri, "book")));
            });
            assertEquals(2, books.size());
        } finally {
            if (addedExtension) ExtUtils.seachExts.remove(".epub");
        }
    }

    @Test public void failedListingRetainsConfirmedBooksAndCancellationRejectsUnconfirmedBooks() throws Exception {
        List<FileMeta> books = new ArrayList<>();
        Map<String, SafOpfRegistry.Entry> sidecars = new HashMap<>();
        boolean addedExtension = !ExtUtils.seachExts.contains(".epub");
        if (addedExtension) ExtUtils.seachExts.add(".epub");
        try {
            SafDiscovery.DirectoryListing failure = (uri, stopped) -> {
                if (uri.equals(root)) return Arrays.asList(
                        book("book.epub", Uri.withAppendedPath(root, "book")), folder("first", first));
                throw new IOException("Provider offline");
            };
            assertThrows(IOException.class,
                    () -> SafDiscovery.collect(root, books, sidecars, () -> false, failure));
            assertEquals(1, books.size());
            assertTrue(sidecars.isEmpty());
            books.clear();

            AtomicBoolean cancelled = new AtomicBoolean();
            assertThrows(IOException.class, () -> SafDiscovery.collect(root, books, sidecars,
                    cancelled::get, (uri, stopped) -> {
                        if (uri.equals(root)) return Collections.singletonList(folder("second", second));
                        cancelled.set(true);
                        return Collections.singletonList(book("book.epub", Uri.withAppendedPath(uri, "book")));
                    }));
            assertTrue(books.isEmpty());
            assertTrue(sidecars.isEmpty());
        } finally {
            if (addedExtension) ExtUtils.seachExts.remove(".epub");
        }
    }

    @Test public void confirmedBatchesCanRemainAfterLaterFolderFailure() throws Exception {
        List<FileMeta> completeOutput = new ArrayList<>();
        Map<String, SafOpfRegistry.Entry> sidecars = new HashMap<>();
        List<FileMeta> published = new ArrayList<>();
        boolean addedExtension = !ExtUtils.seachExts.contains(".epub");
        if (addedExtension) ExtUtils.seachExts.add(".epub");
        try {
            assertThrows(IOException.class, () -> SafDiscovery.collect(root, completeOutput,
                    sidecars, () -> false, (uri, stopped) -> {
                        if (uri.equals(root)) return Arrays.asList(
                                book("found.epub", Uri.withAppendedPath(root, "found")),
                                folder("first", first));
                        throw new IOException("Provider offline");
                    }, (batch, entries) -> published.addAll(batch)));
            assertEquals(1, published.size());
            assertEquals(published.size(), completeOutput.size());
            assertEquals(published.get(0).getPath(), completeOutput.get(0).getPath());
        } finally {
            if (addedExtension) ExtUtils.seachExts.remove(".epub");
        }
    }
    @Test public void sharedChildAndBackEdgeAreVisitedOnlyOnce() throws Exception {
        Uri shared = Uri.parse("content://fixture/tree/root/document/shared");
        java.util.concurrent.atomic.AtomicInteger sharedListings = new java.util.concurrent.atomic.AtomicInteger();
        List<FileMeta> books = new ArrayList<>();
        boolean added = !ExtUtils.seachExts.contains(".epub");
        if (added) ExtUtils.seachExts.add(".epub");
        try {
            SafDiscovery.collect(root, books, new HashMap<>(), () -> false, (uri, stopped) -> {
                if (uri.equals(root)) return Arrays.asList(folder("first", first), folder("second", second));
                if (uri.equals(first) || uri.equals(second)) return Collections.singletonList(folder("shared", shared));
                sharedListings.incrementAndGet();
                return Arrays.asList(folder("back", root), book("Book.epub", Uri.parse("content://fixture/document/book")));
            });
            assertEquals(1, sharedListings.get());
            assertEquals(1, books.size());
        } finally { if (added) ExtUtils.seachExts.remove(".epub"); }
    }
}
