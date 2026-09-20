package com.foobnix.pdf.info;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.net.Uri;
import androidx.test.platform.app.InstrumentationRegistry;
import com.bumptech.glide.Glide;
import com.bumptech.glide.Priority;
import com.bumptech.glide.request.FutureTarget;
import com.foobnix.dao2.FileMeta;
import com.foobnix.model.AppProfile;
import com.foobnix.ui2.AppDB;
import com.foobnix.ui2.FileMetaCore;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import java.io.File;
import java.io.FileOutputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import static org.junit.Assert.*;

/** Runs the real SAF sidecar extractor, Glide transformations, and resource disk cache. */
public class CoverCacheTest {
    private Context context;
    private File cover;
    private FileMeta book;
    private final Map<String, Uri> siblings = new HashMap<>();
    @Before public void setUp() throws Exception {
        context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        AppProfile.init(context);
        book = AppDB.get().getOrCreate("content://cover-fixture/document/" + UUID.randomUUID());
        book.setTitle("Fluent Python"); book.setAuthor("Luciano Ramalho");
        book.setPathTxt("Fluent Python.pdf"); book.setSize(100L); book.setDate(1000L);
        book.setState(FileMetaCore.STATE_FULL); AppDB.get().save(book);
        cover = File.createTempFile("cover-fixture-", ".png", context.getCacheDir());
        writeCover(Color.RED);
        siblings.put("cover.jpg", Uri.fromFile(cover));
        register("revision-1");
    }
    @After public void tearDown() {
        SafOpfRegistry.unregister(book.getPath()); AppDB.get().deleteBy(book.getPath()); cover.delete();
    }
    private void register(String revision) {
        // Missing OPF and unreachable ebook URI ensure pixels came from the sidecar alone.
        SafOpfRegistry.restore(context);
        SafOpfRegistry.register(book.getPath(), new SafOpfRegistry.Entry(
                Uri.fromFile(new File(cover.getParentFile(), "missing-" + UUID.randomUUID() + ".opf")), siblings, revision));
        AppDB.get().updateSidecarRevision(book, revision);
    }
    private void writeCover(int color) throws Exception {
        Bitmap bitmap = Bitmap.createBitmap(24, 36, Bitmap.Config.ARGB_8888); bitmap.eraseColor(color);
        try (FileOutputStream out = new FileOutputStream(cover)) { assertTrue(bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)); }
        finally { bitmap.recycle(); }
    }
    private int request(boolean cacheOnly, Priority priority) throws Exception {
        FutureTarget<Bitmap> target = IMG.getCoverPageWithEffect(context, book, null)
                .priority(priority).skipMemoryCache(true).onlyRetrieveFromCache(cacheOnly).submit();
        try {
            Bitmap result = target.get(10, TimeUnit.SECONDS);
            return result.getPixel(result.getWidth()/2, result.getHeight()/2);
        } finally { Glide.with(context).clear(target); }
    }
    private void clearMemory() {
        InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> Glide.get(context).clearMemory());
    }
    @Test public void transientSidecarFailureRetriesWithoutARevisionChange() throws Exception {
        assertTrue(cover.delete());
        assertThrows(java.util.concurrent.ExecutionException.class,
                () -> request(false, Priority.HIGH));
        writeCover(Color.RED);
        assertEquals(Color.RED, request(false, Priority.HIGH));
        assertEquals(Long.valueOf(1000), book.getDate());
    }

    @Test public void warmingBeforeAnyRowIsVisiblePopulatesTheVisibleRequestDiskCache() throws Exception {
        assertEquals(Color.RED, request(false, Priority.LOW));
        clearMemory(); SafOpfRegistry.clear(); book = AppDB.get().load(book.getPath());
        assertTrue(cover.delete());
        // Source no longer exists, so a cache-only success proves resource disk-cache reuse.
        // Glide delivers the bitmap before its deferred resource encoding commits.
        // Memory is disabled and the source is gone: every successful request must read disk.
        long deadline = android.os.SystemClock.elapsedRealtime() + 5000;
        while (true) {
            try {
                assertEquals(Color.RED, request(true, Priority.HIGH));
                break;
            } catch (java.util.concurrent.ExecutionException pendingEncoding) {
                if (android.os.SystemClock.elapsedRealtime() >= deadline) throw pendingEncoding;
                Thread.sleep(20);
            }
        }
    }
    @Test public void sidecarRevisionInvalidatesCoverEvenWhenBookSizeAndDateDoNotChange() throws Exception {
        assertEquals(Color.RED, request(false, Priority.LOW));
        clearMemory(); writeCover(Color.BLUE); register("revision-2");
        SafOpfRegistry.clear(); book = AppDB.get().load(book.getPath());
        assertThrows(java.util.concurrent.ExecutionException.class, () -> request(true, Priority.HIGH));
        assertEquals(Color.BLUE, request(false, Priority.HIGH));
        assertEquals(Long.valueOf(100), book.getSize()); assertEquals(Long.valueOf(1000), book.getDate());
    }
    @Test public void bookRevisionInvalidatesPreviouslyCachedCover() throws Exception {
        assertEquals(Color.RED, request(false, Priority.LOW));
        clearMemory(); writeCover(Color.BLUE); book.setDate(2000L);
        assertThrows(java.util.concurrent.ExecutionException.class, () -> request(true, Priority.HIGH));
        assertEquals(Color.BLUE, request(false, Priority.HIGH));
    }
    @Test public void extractingCoverDoesNotOverwriteAuthoritativeLibraryMetadata() throws Exception {
        assertEquals(Color.RED, request(false, Priority.HIGH));
        FileMeta stored = AppDB.get().scanSnapshot().stream()
                .filter(row -> book.getPath().equals(row.getPath())).findFirst().orElseThrow(AssertionError::new);
        assertEquals("Fluent Python", stored.getTitle()); assertEquals("Luciano Ramalho", stored.getAuthor());
        assertEquals(Integer.valueOf(FileMetaCore.STATE_FULL), stored.getState());
        assertNull(stored.getSIndex());
    }
    @Test public void registryEntryTakesImmutableSiblingSnapshot() {
        SafOpfRegistry.Entry entry = SafOpfRegistry.get(book.getPath()); siblings.clear();
        assertEquals(Uri.fromFile(cover), entry.siblingByLowerName.get("cover.jpg"));
        assertThrows(UnsupportedOperationException.class, () -> entry.siblingByLowerName.clear());
    }
}
