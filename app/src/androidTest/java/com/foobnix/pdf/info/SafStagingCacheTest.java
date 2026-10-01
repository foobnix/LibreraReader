package com.foobnix.pdf.info;

import android.content.ContentProvider;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.pm.ProviderInfo;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.provider.DocumentsContract;
import android.provider.OpenableColumns;
import androidx.test.filters.SdkSuppress;
import androidx.test.platform.app.InstrumentationRegistry;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import java.io.File;
import java.io.FileNotFoundException;
import java.nio.file.Files;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import static org.junit.Assert.*;

@SdkSuppress(minSdkVersion = 29)
public class SafStagingCacheTest {
    private Context context;
    private File directory;
    private Uri uri;
    private Provider provider;
    private static class Provider extends ContentProvider {
        File remote;
        File remoteB;
        Uri otherUri;
        String displayName = "Book.pdf";
        Long modified = 1000L;
        long size = 3;
        volatile Uri denied;
        final AtomicInteger opened = new AtomicInteger();
        volatile java.util.concurrent.CountDownLatch openBarrier;
        @Override public boolean onCreate() { return true; }
        @Override public Cursor query(Uri uri, String[] columns, String selection, String[] args, String sort) {
            MatrixCursor result = new MatrixCursor(columns);
            Object[] row = new Object[columns.length];
            for (int i=0; i<columns.length; i++) {
                if (OpenableColumns.DISPLAY_NAME.equals(columns[i])) row[i] = displayName;
                if (OpenableColumns.SIZE.equals(columns[i])) row[i] = size;
                if (DocumentsContract.Document.COLUMN_LAST_MODIFIED.equals(columns[i])) row[i] = modified;
            }
            result.addRow(row); return result;
        }
        @Override public ParcelFileDescriptor openFile(Uri uri, String mode) throws FileNotFoundException {
            if (uri.equals(denied)) throw new FileNotFoundException("Grant revoked by fixture: " + uri);
            opened.incrementAndGet();
            java.util.concurrent.CountDownLatch barrier = openBarrier;
            if (barrier != null) {
                barrier.countDown();
                try {
                    if (!barrier.await(5,java.util.concurrent.TimeUnit.SECONDS)) throw new FileNotFoundException("Concurrent download did not reach provider");
                } catch (InterruptedException stop) { Thread.currentThread().interrupt(); throw new FileNotFoundException("Interrupted fixture"); }
            }
            return ParcelFileDescriptor.open(uri.equals(otherUri) ? remoteB : remote,
                    ParcelFileDescriptor.MODE_READ_ONLY);
        }
        @Override public String getType(Uri uri) { return "application/pdf"; }
        @Override public Uri insert(Uri uri, ContentValues values) { throw new UnsupportedOperationException(); }
        @Override public int delete(Uri uri, String selection, String[] args) { throw new UnsupportedOperationException(); }
        @Override public int update(Uri uri, ContentValues values, String selection, String[] args) { throw new UnsupportedOperationException(); }
    }
    @Before public void setUp() throws Exception {
        Context target = InstrumentationRegistry.getInstrumentation().getTargetContext();
        directory = Files.createTempDirectory(target.getCacheDir().toPath(), "staging-fixture-").toFile();
        provider = new Provider(); provider.remote = new File(directory,"remote.pdf");
        Files.write(provider.remote.toPath(), new byte[]{1,2,3});
        ProviderInfo info = new ProviderInfo(); info.authority = "staging.fixture";
        provider.attachInfo(target, info);
        ContentResolver resolver = ContentResolver.wrap(provider);
        context = new ContextWrapper(target) {
            @Override public File getCacheDir() { return directory; }
            @Override public ContentResolver getContentResolver() { return resolver; }
        };
        uri = Uri.parse("content://staging.fixture/document/" + UUID.randomUUID());
    }
    @After public void tearDown() { delete(directory); }
    private void delete(File file) {
        File[] children = file.listFiles(); if (children != null) for (File child : children) delete(child);
        file.delete();
    }
    @Test public void unchangedRevisionReusesCompleteDownloadedBook() throws Exception {
        File first = ExtUtils.stageSafFile(context, uri, "fallback.pdf");
        File second = ExtUtils.stageSafFile(context, uri, "fallback.pdf");
        assertEquals(first, second); assertEquals(1, provider.opened.get());
        assertArrayEquals(new byte[]{1,2,3}, Files.readAllBytes(second.toPath()));
    }
    @Test public void localFileNamedLikeStagingIsNotTreatedAsImmutable() throws Exception {
        File named = new File(directory, "saf-open/source-ordinary.txt");
        assertTrue(named.getParentFile().mkdirs());
        Files.write(named.toPath(), new byte[]{1, 2, 3});
        String first = org.ebookdroid.core.codec.AbstractCodecContext.sourceRevisionKey(named.getPath());
        Files.write(named.toPath(), new byte[]{1, 2, 3, 4});
        assertNotEquals(first, org.ebookdroid.core.codec.AbstractCodecContext.sourceRevisionKey(named.getPath()));
        assertFalse(BookCacheLeases.isImmutableRevisionNamedSource(named));
    }
    @Test public void differentLengthAndTimeWithEqualSumHaveDifferentRevisionKeys() throws Exception {
        File source = new File(directory, "revision.txt");
        Files.write(source.toPath(), new byte[4]);
        assertTrue(source.setLastModified(2000));
        String first = org.ebookdroid.core.codec.AbstractCodecContext.sourceRevisionKey(source.getPath());
        Files.write(source.toPath(), new byte[5]);
        assertTrue(source.setLastModified(1999));
        assertNotEquals(first, org.ebookdroid.core.codec.AbstractCodecContext.sourceRevisionKey(source.getPath()));
    }
    @Test public void processedEpubReusesTheSameOutputAcrossStagedBookSwitches() throws Exception {
        byte[] book;
        try (java.io.InputStream input = InstrumentationRegistry.getInstrumentation()
                .getContext().getAssets().open("reader-fixtures/book.epub")) {
            book = com.BaseExtractor.getEntryAsByte(input);
        }
        Files.write(provider.remote.toPath(), book);
        provider.otherUri = Uri.parse("content://staging.fixture/document/" + UUID.randomUUID());
        provider.remoteB = new File(directory, "other.epub");
        Files.write(provider.remoteB.toPath(), book);
        provider.size = book.length;
        provider.displayName = "Book.epub";
        com.foobnix.ext.CacheZipUtils.init(InstrumentationRegistry.getInstrumentation().getTargetContext());
        boolean replacement = com.foobnix.model.AppState.get().isEnableTextReplacement;
        boolean reference = com.foobnix.model.AppState.get().isReferenceMode;
        String css = com.foobnix.pdf.info.model.BookCSS.get().customCSS2;
        boolean cancelled = com.foobnix.sys.TempHolder.get().loadingCancelled.getAndSet(false);
        com.foobnix.model.AppState.get().isEnableTextReplacement = true;
        com.foobnix.model.AppState.get().isReferenceMode = false;
        com.foobnix.pdf.info.model.BookCSS.get().customCSS2 = "";
        org.ebookdroid.core.codec.CodecDocument reader = null;
        File output = null;
        File stageA = null;
        try {
            stageA = ExtUtils.stageSafFile(context, uri, "Book.epub");
            org.ebookdroid.droids.EpubContext first = new org.ebookdroid.droids.EpubContext();
            reader = first.openDocument(stageA.getPath(), "");
            assertNotNull(reader);
            java.lang.reflect.Field cacheFile = first.getClass().getDeclaredField("cacheFile");
            cacheFile.setAccessible(true);
            output = (File) cacheFile.get(first);
            assertTrue(output.isFile());
            long inode = android.system.Os.stat(output.getPath()).st_ino;
            reader.recycle(); reader = null;

            // Staging updates access time for its LRU. That timestamp is not
            // a provider revision and must not change the conversion key.
            assertTrue(stageA.setLastModified(stageA.lastModified() - 10_000));

            File stageB = ExtUtils.stageSafFile(context, provider.otherUri, "Book.epub");
            reader = new org.ebookdroid.droids.EpubContext().openDocument(stageB.getPath(), "");
            assertNotNull(reader);
            reader.recycle(); reader = null;
            assertTrue(output.isFile());

            File reopenedStage = ExtUtils.stageSafFile(context, uri, "Book.epub");
            assertEquals(stageA, reopenedStage);
            BookCacheLeases.unregisterImmutableRevisionNamedSource(reopenedStage);
            org.ebookdroid.droids.EpubContext negativeControl = new org.ebookdroid.droids.EpubContext();
            reader = negativeControl.openDocument(reopenedStage.getPath(), "");
            assertNotNull(reader);
            File wrongOutput = (File) cacheFile.get(negativeControl);
            assertNotEquals("Negative control must expose the unstable cache key", output, wrongOutput);
            reader.recycle(); reader = null;
            BookCacheLeases.evict(wrongOutput);

            BookCacheLeases.registerImmutableRevisionNamedSource(reopenedStage);
            org.ebookdroid.droids.EpubContext reopened = new org.ebookdroid.droids.EpubContext();
            reader = reopened.openDocument(reopenedStage.getPath(), "");
            assertNotNull(reader);
            assertEquals("Reopened reader selected a different processed output",
                    output, cacheFile.get(reopened));
            assertEquals(inode, android.system.Os.stat(output.getPath()).st_ino);
            assertEquals(2, provider.opened.get());
        } finally {
            if (reader != null) reader.recycle();
            com.foobnix.model.AppState.get().isEnableTextReplacement = replacement;
            com.foobnix.model.AppState.get().isReferenceMode = reference;
            com.foobnix.pdf.info.model.BookCSS.get().customCSS2 = css;
            com.foobnix.sys.TempHolder.get().loadingCancelled.set(cancelled);
            if (stageA != null) BookCacheLeases.registerImmutableRevisionNamedSource(stageA);
            if (output != null) BookCacheLeases.evict(output);
        }
    }
    @Test public void canonicalBookReopensThroughEitherAvailableTreeGrant() throws Exception {
        String id = UUID.randomUUID().toString();
        Uri first = DocumentsContract.buildDocumentUriUsingTree(
                DocumentsContract.buildTreeDocumentUri("staging.fixture", "parent"), id);
        Uri second = DocumentsContract.buildDocumentUriUsingTree(
                DocumentsContract.buildTreeDocumentUri("staging.fixture", "child"), id);
        Uri identity = SafDocumentIdentity.canonical(first);
        provider.denied = second;
        File fromFirst = ExtUtils.stageSafFile(context, identity, "Book.pdf");
        assertArrayEquals(new byte[]{1,2,3}, Files.readAllBytes(fromFirst.toPath()));
        BookCacheLeases.cancelReservation(fromFirst);
        provider.modified = 2000L;
        provider.denied = first;
        File fromSecond = ExtUtils.stageSafFile(context, identity, "Book.pdf");
        assertNotEquals(fromFirst, fromSecond);
        assertArrayEquals(new byte[]{1,2,3}, Files.readAllBytes(fromSecond.toPath()));
        BookCacheLeases.cancelReservation(fromSecond);
        assertEquals(2, provider.opened.get());
    }
    @Test public void evictedStageRestagesFromRememberedGrantInNewContext() throws Exception {
        Uri access = DocumentsContract.buildDocumentUriUsingTree(
                DocumentsContract.buildTreeDocumentUri("staging.fixture", "selected"),
                UUID.randomUUID().toString());
        Uri identity = SafDocumentIdentity.canonical(access);
        File first = ExtUtils.stageSafFile(context, identity, "Book.pdf");
        BookCacheLeases.cancelReservation(first);
        assertTrue(BookCacheLeases.evict(first));
        Context restartedContext = new ContextWrapper(context);
        File reopened = ExtUtils.stageSafFile(restartedContext, identity, "Book.pdf");
        assertArrayEquals(new byte[]{1,2,3}, Files.readAllBytes(reopened.toPath()));
        assertEquals(2, provider.opened.get());
        BookCacheLeases.cancelReservation(reopened);
    }
    @Test public void updatedProviderRevisionDownloadsWithoutDeletingAPendingReadersVersion() throws Exception {
        File first = ExtUtils.stageSafFile(context, uri, "fallback.pdf");
        provider.modified = 2000L; provider.size = 4; Files.write(provider.remote.toPath(), new byte[]{4,5,6,7});
        File second = ExtUtils.stageSafFile(context, uri, "fallback.pdf");
        assertNotEquals(first, second); assertTrue(first.exists()); assertEquals(2, provider.opened.get());
        assertArrayEquals(new byte[]{1,2,3}, Files.readAllBytes(first.toPath()));
        assertArrayEquals(new byte[]{4,5,6,7}, Files.readAllBytes(second.toPath()));
    }
    @Test public void missingModificationDateCannotReusePotentiallyStaleDownload() throws Exception {
        provider.modified = null;
        File first = ExtUtils.stageSafFile(context, uri, "fallback.pdf");
        Files.write(provider.remote.toPath(), new byte[]{4,5,6});
        File second = ExtUtils.stageSafFile(context, uri, "fallback.pdf");
        assertNotEquals(first, second); assertEquals(2, provider.opened.get());
        assertArrayEquals(new byte[]{4,5,6}, Files.readAllBytes(second.toPath()));
    }
    @Test public void incompleteDownloadDoesNotPublishPartialFileOrDeleteLastGoodVersion() throws Exception {
        File first = ExtUtils.stageSafFile(context, uri, "fallback.pdf");
        provider.modified = 2000L; provider.size = 100;
        assertThrows(java.io.IOException.class, () -> ExtUtils.stageSafFile(context, uri, "fallback.pdf"));
        assertArrayEquals(new byte[]{1,2,3}, Files.readAllBytes(first.toPath()));
        assertEquals(0, first.getParentFile().listFiles((dir,name) -> name.endsWith(".part")).length);
        assertEquals(1, first.getParentFile().listFiles().length);
    }
    @Test public void concurrentOpensUseIndependentTemporaryFiles() throws Exception {
        var executor = java.util.concurrent.Executors.newFixedThreadPool(2);
        try {
            for (int iteration=0; iteration<10; iteration++) {
                provider.modified = 2000L + iteration;
                provider.openBarrier = new java.util.concurrent.CountDownLatch(2);
                var first = executor.submit(() -> ExtUtils.stageSafFile(context, uri, "Book.pdf"));
                var second = executor.submit(() -> ExtUtils.stageSafFile(context, uri, "Book.pdf"));
                assertArrayEquals(new byte[]{1,2,3}, Files.readAllBytes(first.get(5,java.util.concurrent.TimeUnit.SECONDS).toPath()));
                assertArrayEquals(new byte[]{1,2,3}, Files.readAllBytes(second.get(5,java.util.concurrent.TimeUnit.SECONDS).toPath()));
            }
        } finally { executor.shutdownNow(); }
    }
}
