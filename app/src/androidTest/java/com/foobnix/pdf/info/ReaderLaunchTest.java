package com.foobnix.pdf.info;

import android.content.Context;
import android.content.ContextWrapper;
import android.content.ContentProvider;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.pm.ProviderInfo;
import android.database.Cursor;
import android.os.ParcelFileDescriptor;
import android.content.Intent;
import android.net.Uri;
import androidx.test.platform.app.InstrumentationRegistry;
import com.foobnix.model.AppSP;
import com.foobnix.model.AppState;
import com.foobnix.ext.CacheZipUtils;
import com.foobnix.zipmanager.ZipDialog;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import static org.junit.Assert.*;

/** Records real reader dispatch intents without opening readers or modifying progress. */
public class ReaderLaunchTest {
    private File source;
    private Uri uri;
    private int priorMode;
    private RecordingContext context;
    private static class RecordingContext extends ContextWrapper {
        final List<Intent> intents = Collections.synchronizedList(new ArrayList<>());
        ContentResolver resolver;
        RecordingContext(Context context) { super(context); }
        @Override public void startActivity(Intent intent) { intents.add(intent); }
        @Override public ContentResolver getContentResolver() {
            return resolver == null ? super.getContentResolver() : resolver;
        }
    }
    @Before public void setUp() throws Exception {
        context = new RecordingContext(InstrumentationRegistry.getInstrumentation().getTargetContext());
        source = File.createTempFile("reader-dispatch-", ".pdf", context.getCacheDir());
        uri = Uri.fromFile(source); priorMode = AppSP.get().readingMode;
    }
    @After public void tearDown() { source.delete(); AppSP.get().readingMode = priorMode; }
    private void checkMode(int mode, String activity) {
        AppSP.get().readingMode = mode;
        ExtUtils.showDocumentInner(context, uri, 0.25f, null, "content://fixture/book-a");
        ExtUtils.showDocumentInner(context, uri, 0, null, "content://fixture/book-b");
        ExtUtils.showDocumentInner(context, uri, 0, null);
        assertEquals(3, context.intents.size());
        assertEquals("content://fixture/book-a", context.intents.get(0).getStringExtra("SAF_ORIGINAL_URI"));
        assertEquals("content://fixture/book-b", context.intents.get(1).getStringExtra("SAF_ORIGINAL_URI"));
        assertFalse(context.intents.get(2).hasExtra("SAF_ORIGINAL_URI"));
        for (Intent intent : context.intents) { assertEquals(uri, intent.getData()); assertTrue(intent.getComponent().getClassName().endsWith(activity)); }
    }
    @Test public void scrollReaderDispatchKeepsPhysicalFileAndPerBookIdentity() { checkMode(AppState.READING_MODE_SCROLL, "VerticalViewActivity"); }
    @Test public void bookReaderDispatchKeepsPhysicalFileAndPerBookIdentity() { checkMode(AppState.READING_MODE_BOOK, "HorizontalViewActivity"); }
    @Test public void concurrentDispatchCannotStealAnotherBooksIdentity() throws Exception {
        AppSP.get().readingMode = AppState.READING_MODE_BOOK;
        var executor = Executors.newFixedThreadPool(3);
        try {
            for (int i=0; i<100; i++) {
                final String book = "content://fixture/book-" + i;
                executor.submit(() -> ExtUtils.showDocumentInner(context, uri, 0, null, book));
            }
            executor.shutdown(); assertTrue(executor.awaitTermination(5, TimeUnit.SECONDS));
            assertEquals(100, context.intents.size());
            assertEquals(100, context.intents.stream().map(intent -> intent.getStringExtra("SAF_ORIGINAL_URI")).distinct().count());
            for (Intent intent : context.intents) assertEquals(uri, intent.getData());
        } finally { executor.shutdownNow(); }
    }

    @Test public void externalReaderReceivesOriginalSafUriWithReadGrant() {
        Uri original = Uri.parse("content://fixture/document/book.epub");
        ExtUtils.openWithSafUri(context, original, "book.epub");
        assertEquals(1, context.intents.size());
        Intent intent = context.intents.get(0);
        assertEquals(Intent.ACTION_VIEW, intent.getAction());
        assertEquals(original, intent.getData());
        assertTrue((intent.getFlags() & Intent.FLAG_GRANT_READ_URI_PERMISSION) != 0);
    }

    @Test public void extractedArchiveReaderUsesMemberIdentityAcrossHandoff() throws Exception {
        File directory = java.nio.file.Files.createTempDirectory(context.getCacheDir().toPath(),
                "reader-archive-").toFile();
        File previous = CacheZipUtils.CACHE_RECENT;
        CacheZipUtils.CACHE_RECENT = new File(directory, "recent");
        File archive = new File(directory, "books.zip");
        try {
            try (java.util.zip.ZipOutputStream output = new java.util.zip.ZipOutputStream(
                    new java.io.FileOutputStream(archive))) {
                output.putNextEntry(new java.util.zip.ZipEntry("inside/book.txt"));
                output.write("archive book".getBytes(java.nio.charset.StandardCharsets.UTF_8));
                output.closeEntry();
            }
            File extracted = ZipDialog.extractFile(null, "inside/book.txt", archive, false);
            assertNotNull(extracted);
            AppSP.get().readingMode = AppState.READING_MODE_BOOK;
            ExtUtils.showDocumentInner(context, Uri.fromFile(extracted), 0, null);
            assertEquals(1, context.intents.size());
            String identity = ArchiveMemberIdentity.create(archive.getPath(), "inside/book.txt");
            assertEquals(identity, ExtUtils.recentPathFromIntent(context.intents.get(0), extracted.getPath()));
            context.intents.clear();
            AppSP.get().readingMode = AppState.READING_MODE_LIBRERAX;
            ExtUtils.showDocumentInner(context, Uri.fromFile(extracted), 0, null);
            assertEquals(1, context.intents.size());
            assertEquals(identity, context.intents.get(0).getStringExtra(
                    com.foobnix.librerax.LibreraX.EXTRA_BOOK_IDENTITY));
            new File(context.intents.get(0).getData().getPath()).delete();
            BookCacheLeases.cancelReservation(extracted);
        } finally {
            CacheZipUtils.CACHE_RECENT = previous;
            BookCacheLeases.evictTree(directory);
        }
    }

    @Test public void libreraXReceivesOriginalUriAndReleasesUnusedStage() {
        AppSP.get().readingMode = AppState.READING_MODE_LIBRERAX;
        BookCacheLeases.reserve(source);
        Uri original = Uri.parse("content://fixture/document/book.epub");
        ContentProvider provider = new ContentProvider() {
            @Override public boolean onCreate() { return true; }
            @Override public String getType(Uri uri) { return "application/pdf"; }
            @Override public Cursor query(Uri uri, String[] projection, String selection,
                                          String[] selectionArgs, String sortOrder) { return null; }
            @Override public Uri insert(Uri uri, ContentValues values) { return null; }
            @Override public int delete(Uri uri, String selection, String[] selectionArgs) { return 0; }
            @Override public int update(Uri uri, ContentValues values, String selection,
                                        String[] selectionArgs) { return 0; }
            @Override public ParcelFileDescriptor openFile(Uri uri, String mode)
                    throws java.io.FileNotFoundException {
                return ParcelFileDescriptor.open(source, ParcelFileDescriptor.MODE_READ_ONLY);
            }
        };
        ProviderInfo info = new ProviderInfo(); info.authority = "fixture";
        provider.attachInfo(context, info);
        context.resolver = ContentResolver.wrap(provider);
        ExtUtils.showDocumentInner(context, uri, 0, null, original.toString());
        assertEquals(1, context.intents.size());
        Intent intent = context.intents.get(0);
        assertEquals(original, intent.getData());
        assertEquals(original.toString(), intent.getStringExtra(com.foobnix.librerax.LibreraX.EXTRA_BOOK_IDENTITY));
        assertTrue("External handoff left its staged copy pinned", BookCacheLeases.evict(source));
    }
}
