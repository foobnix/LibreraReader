package com.foobnix.ui2;

import androidx.test.platform.app.InstrumentationRegistry;

import com.foobnix.ext.CacheZipUtils;
import com.foobnix.ext.EbookMeta;

import org.junit.Test;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.junit.Assert.*;

public class FileMetaArchiveConcurrencyTest {
    @Test public void failedArchiveReadsReleaseTheSharedLock() throws Exception {
        File missing = new File(InstrumentationRegistry.getInstrumentation()
                .getTargetContext().getCacheDir(), "missing-archive.zip");
        missing.delete();
        FileMetaCore.getBookOverview(missing.getPath());
        FileMetaCore.get().getEbookMeta(missing.getPath(),
                CacheZipUtils.CacheDir.ZipApp, true);
        AtomicReference<Boolean> acquired = new AtomicReference<>(false);
        Thread nextReader = new Thread(() -> {
            try {
                acquired.set(CacheZipUtils.cacheLock.tryLock(5, TimeUnit.SECONDS));
                if (acquired.get()) CacheZipUtils.cacheLock.unlock();
            } catch (InterruptedException error) {
                Thread.currentThread().interrupt();
            }
        });
        nextReader.start();
        nextReader.join(6_000);
        assertFalse(nextReader.isAlive());
        assertTrue("Failure path retained ZipApp lock", acquired.get());
    }

    @Test public void overviewAndMetadataHoldTheSharedArchiveCacheUntilReadingFinishes()
            throws Exception {
        File root = Files.createTempDirectory(InstrumentationRegistry.getInstrumentation()
                .getTargetContext().getCacheDir().toPath(), "archive-metadata-").toFile();
        File alpha = new File(root, "alpha.zip");
        File beta = new File(root, "beta.zip");
        zipFb2(alpha, "Alpha", "Alpha synopsis");
        zipFb2(beta, "Beta", "Beta synopsis");
        CacheZipUtils.init(InstrumentationRegistry.getInstrumentation().getTargetContext());
        CountDownLatch started = new CountDownLatch(2);
        AtomicReference<String> overview = new AtomicReference<>();
        AtomicReference<EbookMeta> metadata = new AtomicReference<>();
        AtomicReference<Throwable> failure = new AtomicReference<>();
        Thread overviewWorker = new Thread(() -> {
            started.countDown();
            try { overview.set(FileMetaCore.getBookOverview(alpha.getPath())); }
            catch (Throwable error) { failure.compareAndSet(null, error); }
        });
        Thread metadataWorker = new Thread(() -> {
            started.countDown();
            try { metadata.set(FileMetaCore.get().getEbookMeta(
                    beta.getPath(), CacheZipUtils.CacheDir.ZipApp, true)); }
            catch (Throwable error) { failure.compareAndSet(null, error); }
        });
        CacheZipUtils.cacheLock.lock();
        try {
            overviewWorker.start();
            metadataWorker.start();
            assertTrue(started.await(5, TimeUnit.SECONDS));
            awaitLock(overviewWorker);
            awaitLock(metadataWorker);
            assertNull("Overview read escaped the shared extraction lock", overview.get());
            assertNull("Metadata read escaped the shared extraction lock", metadata.get());
        } finally {
            CacheZipUtils.cacheLock.unlock();
        }
        overviewWorker.join(10_000);
        metadataWorker.join(10_000);
        assertFalse(overviewWorker.isAlive());
        assertFalse(metadataWorker.isAlive());
        if (failure.get() != null) throw new AssertionError(failure.get());
        assertTrue(overview.get(), overview.get().contains("Alpha synopsis"));
        assertNotNull(metadata.get());
        assertEquals("Beta", metadata.get().getTitle());
        alpha.delete(); beta.delete(); root.delete();
    }

    private static void awaitLock(Thread worker) throws Exception {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
        while (System.nanoTime() < deadline) {
            for (StackTraceElement frame : worker.getStackTrace()) {
                if (frame.getClassName().equals("java.util.concurrent.locks.ReentrantLock")
                        && frame.getMethodName().equals("lock")) return;
            }
            if (!worker.isAlive()) break;
            Thread.sleep(10);
        }
        fail("Archive worker did not wait for the shared extraction lock: " + worker.getName());
    }

    private static void zipFb2(File output, String title, String synopsis) throws Exception {
        String fb2 = "<?xml version='1.0' encoding='UTF-8'?>"
                + "<FictionBook xmlns='http://www.gribuser.ru/xml/fictionbook/2.0'>"
                + "<description><title-info><genre>fiction</genre><author><first-name>A</first-name>"
                + "<last-name>Writer</last-name></author><book-title>" + title + "</book-title>"
                + "<annotation><p>" + synopsis + "</p></annotation><lang>en</lang>"
                + "</title-info></description><body><section><p>Chapter</p></section></body>"
                + "</FictionBook>";
        try (ZipOutputStream zip = new ZipOutputStream(new FileOutputStream(output))) {
            zip.putNextEntry(new ZipEntry("book.fb2"));
            zip.write(fb2.getBytes(StandardCharsets.UTF_8));
            zip.closeEntry();
        }
    }
}
