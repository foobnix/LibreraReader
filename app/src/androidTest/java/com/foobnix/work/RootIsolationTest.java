package com.foobnix.work;

import android.content.Context;
import android.net.Uri;
import androidx.core.content.FileProvider;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.work.Data;
import androidx.work.ListenableWorker;
import androidx.work.WorkerFactory;
import androidx.work.WorkerParameters;
import androidx.work.impl.utils.taskexecutor.WorkManagerTaskExecutor;
import com.foobnix.dao2.FileMeta;
import com.foobnix.ext.EbookMeta;
import com.foobnix.ext.CbzCbrExtractor;
import com.foobnix.model.AppProfile;
import com.foobnix.model.AppState;
import com.foobnix.pdf.info.ExtUtils;
import com.foobnix.ui2.AppDB;
import com.foobnix.ui2.FileMetaCore;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import kotlin.coroutines.EmptyCoroutineContext;
import org.junit.Test;
import static org.junit.Assert.*;

public class RootIsolationTest {
    @Test public void failedProviderDoesNotBlockHealthyLocalReconciliation() throws Exception {
        check(false);
    }
    @Test public void failedProviderDoesNotBlockHealthyLocalMetadataExtraction() throws Exception {
        check(true);
    }
    private void check(boolean extract) throws Exception {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        AppProfile.init(context);
        String previous = com.foobnix.pdf.info.model.BookCSS.get().searchPathsJson;
        File folder = new File(context.getCacheDir(), "root-isolation-" + UUID.randomUUID());
        assertTrue(folder.mkdir());
        File book = new File(folder, "Book.fb2");
        Files.write(book.toPath(), ("<?xml version=\"1.0\"?><FictionBook xmlns=\"http://www.gribuser.ru/xml/fictionbook/2.0\"><description><title-info><book-title>Healthy title</book-title></title-info></description><body><section><p>Text</p></section></body></FictionBook>").getBytes(StandardCharsets.UTF_8));
        String root = "content://unavailable.fixture/tree/" + UUID.randomUUID();
        String retained = "content://unavailable.fixture/document/" + UUID.randomUUID();
        String vanished = new File(folder, "gone.fb2").getPath();
        FileMeta remote = new FileMeta(retained); remote.setIsSearchBook(true); remote.setTitle("Offline book");
        FileMeta removed = new FileMeta(vanished); removed.setIsSearchBook(true);
        AppDB.get().saveAll(java.util.Arrays.asList(remote, removed));
        AppDB.get().reconcileCompletedScan(Collections.singletonList(remote), Collections.singleton(root),
                Collections.singletonMap(root, Collections.singleton(retained)));
        boolean added = !ExtUtils.seachExts.contains(".fb2");
        if (added) ExtUtils.seachExts.add(".fb2");
        try {
            com.foobnix.pdf.info.model.BookCSS.get().searchPathsJson = com.foobnix.android.utils.JsonDB.set(
                    java.util.Arrays.asList(folder.getPath(), new File(folder, "missing").getPath(), root));
            boolean success = extract ? new SearchAllBooksWorker(context, parameters()).doWorkInner()
                    : new CheckDeletedBooksWorker(context, parameters()).doWorkInner();
            assertTrue(success);
            assertEquals(Boolean.TRUE, AppDB.get().load(book.getPath()).getIsSearchBook());
            assertEquals(Boolean.TRUE, AppDB.get().load(retained).getIsSearchBook());
            assertEquals(Boolean.FALSE, AppDB.get().load(vanished).getIsSearchBook());
            if (extract) assertEquals("Healthy title", AppDB.get().load(book.getPath()).getTitle());
        } finally {
            com.foobnix.pdf.info.model.BookCSS.get().searchPathsJson = previous;
            AppDB.get().reconcileDeselectedRoots(SearchAllBooksWorker.selectedRoots(), Collections.singleton(root));
            AppDB.get().deleteBy(book.getPath()); AppDB.get().deleteBy(retained); AppDB.get().deleteBy(vanished);
            if (added) ExtUtils.seachExts.remove(".fb2");
            book.delete(); folder.delete();
        }
    }
    @Test public void partialSafRootStillExtractsConfirmedMetadataAfterProviderRuntimeFailure() throws Exception {
        Context target = InstrumentationRegistry.getInstrumentation().getTargetContext();
        AppProfile.init(target);
        String id = "partial-" + UUID.randomUUID();
        Uri root = android.provider.DocumentsContract.buildTreeDocumentUri("partial.fixture", id);
        String book = android.provider.DocumentsContract.buildDocumentUri("partial.fixture", id + "-book").toString();
        String retained = android.provider.DocumentsContract.buildDocumentUri("partial.fixture", id + "-offline-book").toString();
        Context context = target;
        String previous = com.foobnix.pdf.info.model.BookCSS.get().searchPathsJson;
        FileMeta offline = new FileMeta(retained); offline.setIsSearchBook(true);
        AppDB.get().save(offline);
        AppDB.get().reconcileCompletedScan(Collections.singletonList(offline), Collections.singleton(root.toString()),
                Collections.singletonMap(root.toString(), Collections.singleton(retained)));
        boolean added = !ExtUtils.seachExts.contains(".epub");
        if (added) ExtUtils.seachExts.add(".epub");
        try {
            com.foobnix.pdf.info.model.BookCSS.get().searchPathsJson = com.foobnix.android.utils.JsonDB.set(Collections.singletonList(root.toString()));
            SearchAllBooksWorker worker = new SearchAllBooksWorker(context, parameters()) {
                @Override protected void scanSafRoot(String rootPath, List<FileMeta> output,
                        java.util.Map<String, com.foobnix.pdf.info.SafOpfRegistry.Entry> sidecars,
                        List<com.foobnix.model.SimpleMeta> excluded, List<FileMeta> synced) {
                    FileMeta discovered = new FileMeta(book);
                    discovered.setTitle("Book.epub"); discovered.setSize(100L); discovered.setDate(1000L);
                    output.add(discovered);
                    throw new IllegalStateException("Provider failed after a confirmed discovery");
                }
                @Override protected EbookMeta readSafMetadataForScan(FileMeta found) {
                    return new EbookMeta("Confirmed title", "Fixture author");
                }
            };
            assertTrue(worker.doWorkInner());
            assertEquals("Confirmed title", AppDB.get().load(book).getTitle());
            assertTrue(AppDB.get().load(retained).getIsSearchBook());
        } finally {
            com.foobnix.pdf.info.model.BookCSS.get().searchPathsJson = previous;
            AppDB.get().reconcileDeselectedRoots(SearchAllBooksWorker.selectedRoots(), Collections.singleton(root.toString()));
            AppDB.get().deleteBy(book); AppDB.get().deleteBy(retained);
            if (added) ExtUtils.seachExts.remove(".epub");
        }
    }

    private static WorkerParameters parameters() {
        return new WorkerParameters(UUID.randomUUID(), Data.EMPTY, Collections.emptyList(),
                new WorkerParameters.RuntimeExtras(), 0, 0, Runnable::run,
                EmptyCoroutineContext.INSTANCE, new WorkManagerTaskExecutor(Runnable::run),
                new WorkerFactory() {
                    @Override public ListenableWorker createWorker(Context context, String name,
                                                                    WorkerParameters parameters) {
                        throw new AssertionError("Unexpected worker creation");
                    }
                }, (context, id, data) -> { throw new AssertionError("Unexpected progress API"); },
                (context, id, info) -> { throw new AssertionError("Unexpected foreground API"); });
    }

}
