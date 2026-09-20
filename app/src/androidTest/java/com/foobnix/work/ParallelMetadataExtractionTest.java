package com.foobnix.work;

import static org.junit.Assert.*;

import androidx.test.platform.app.InstrumentationRegistry;
import com.foobnix.ext.CacheZipUtils;
import com.foobnix.ext.EbookMeta;
import com.foobnix.ext.Fb2Extractor;
import com.foobnix.dao2.FileMeta;
import com.foobnix.model.AppProfile;
import com.foobnix.ui2.AppDB;
import com.foobnix.ui2.FileMetaCore;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.List;
import java.util.Collections;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.Test;

public class ParallelMetadataExtractionTest {
    @Test public void concurrentFb2GenreReadsSeeTheCompleteLookup() throws Exception {
        var extractor = Fb2Extractor.get();
        var field = Fb2Extractor.class.getDeclaredField("genresRus");
        field.setAccessible(true);
        field.set(extractor, Collections.emptyMap());
        CountDownLatch start = new CountDownLatch(1);
        AtomicReference<Throwable> failure = new AtomicReference<>();
        Thread[] workers = new Thread[8];
        for (int i = 0; i < workers.length; i++) {
            workers[i] = new Thread(() -> {
                try {
                    assertTrue(start.await(5, TimeUnit.SECONDS));
                    extractor.loadGenres();
                    @SuppressWarnings("unchecked") java.util.Map<String, String> genres =
                            (java.util.Map<String, String>) field.get(extractor);
                    assertEquals("Детективная фантастика", genres.get("sf_detective"));
                } catch (Throwable error) { failure.compareAndSet(null, error); }
            });
            workers[i].start();
        }
        start.countDown();
        for (Thread worker : workers) {
            worker.join(10_000);
            assertFalse(worker.isAlive());
        }
        if (failure.get() != null) throw new AssertionError(failure.get());
    }

    @Test public void epubAndFb2MetadataCanBeReadConcurrently() throws Exception {
        var context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        File epub = File.createTempFile("parallel-meta-", ".epub", context.getCacheDir());
        File fb2 = File.createTempFile("parallel-meta-", ".fb2", context.getCacheDir());
        try {
            try (InputStream input = InstrumentationRegistry.getInstrumentation().getContext()
                    .getAssets().open("reader-fixtures/book.epub");
                 FileOutputStream output = new FileOutputStream(epub)) {
                byte[] bytes = new byte[8192];
                for (int count; (count = input.read(bytes)) != -1; ) output.write(bytes, 0, count);
            }
            try (FileOutputStream output = new FileOutputStream(fb2)) {
                output.write(("<?xml version='1.0' encoding='utf-8'?>"
                        + "<FictionBook xmlns='http://www.gribuser.ru/xml/fictionbook/2.0'>"
                        + "<description><title-info><book-title>Parallel FB2</book-title>"
                        + "<author><first-name>A</first-name><last-name>Reader</last-name></author>"
                        + "</title-info></description><body><section><p>Text</p></section></body>"
                        + "</FictionBook>").getBytes(StandardCharsets.UTF_8));
            }
            CountDownLatch bothStarted = new CountDownLatch(2);
            List<EbookMeta> results = new ArrayList<>();
            assertTrue(BoundedTasks.run(Arrays.asList(epub, fb2), 2, () -> false, path -> {
                bothStarted.countDown();
                try { assertTrue(bothStarted.await(5, TimeUnit.SECONDS)); }
                catch (InterruptedException interrupted) { throw new RuntimeException(interrupted); }
                return FileMetaCore.get().getEbookMeta(path.getPath(),
                        CacheZipUtils.CacheDir.ZipService, true);
            }, results::add));
            assertEquals(2, results.size());
            assertTrue(results.stream().anyMatch(meta -> "Parallel FB2".equals(meta.getTitle())));
            assertTrue(results.stream().allMatch(meta -> meta.getTitle() != null));
        } finally {
            epub.delete();
            fb2.delete();
        }
    }


    @Test public void extractionCompletionCannotReplaceProgressWrittenWhileItRuns() throws Exception {
        var context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        AppProfile.init(context);
        String path = new File(context.getCacheDir(),
                "metadata-race-" + java.util.UUID.randomUUID() + ".epub").getPath();
        CountDownLatch extracting = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        AtomicReference<Throwable> failure = new AtomicReference<>();
        try {
            FileMeta initial = new FileMeta(path);
            initial.setTitle("Before");
            initial.setIsSearchBook(true);
            AppDB.get().saveAll(java.util.Collections.singletonList(initial));
            FileMeta baseline = AppDB.get().scanSnapshot().stream()
                    .filter(row -> path.equals(row.getPath())).findFirst().get();
            Thread extraction = new Thread(() -> {
                try {
                    BoundedTasks.run(java.util.Collections.singletonList(path), 2, () -> false,
                            item -> {
                                extracting.countDown();
                                try { assertTrue(release.await(5, TimeUnit.SECONDS)); }
                                catch (InterruptedException interrupted) { throw new RuntimeException(interrupted); }
                                FileMeta metadata = new FileMeta(item);
                                metadata.setTitle("Extracted");
                                return metadata;
                            }, metadata -> AppDB.get().updateScannedMetadata(metadata, baseline));
                } catch (Throwable error) { failure.set(error); }
            });
            extraction.start();
            assertTrue(extracting.await(5, TimeUnit.SECONDS));
            FileMeta reader = AppDB.get().load(path);
            reader.setIsStar(true);
            AppDB.get().save(reader);
            AppDB.get().updateReadingProgress(path, 0.75f);
            release.countDown();
            extraction.join(5000);
            assertFalse(extraction.isAlive());
            if (failure.get() != null) throw new AssertionError(failure.get());
            FileMeta result = AppDB.get().load(path);
            assertEquals("Extracted", result.getTitle());
            assertEquals(Boolean.TRUE, result.getIsStar());
            assertEquals(0.75f, result.getIsRecentProgress(), 0.0001f);
        } finally {
            release.countDown();
            AppDB.get().deleteBy(path);
        }
    }
}
