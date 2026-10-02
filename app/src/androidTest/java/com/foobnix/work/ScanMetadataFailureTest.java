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
import com.foobnix.pdf.info.SafOpfRegistry;
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

public class ScanMetadataFailureTest {
    @Test public void validComicWithoutComicInfoHasNoParserFailure() throws Exception {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        File comic = File.createTempFile("comic-no-metadata-", ".cbz", context.getCacheDir());
        try {
            try (ZipOutputStream output = new ZipOutputStream(new FileOutputStream(comic))) {
                output.putNextEntry(new ZipEntry("page.jpg"));
                output.write(new byte[]{1, 2, 3});
                output.closeEntry();
            }
            assertFalse(CbzCbrExtractor.getBookMetaInformation(comic.getPath())
                    .isExtractionFailed());
        } finally { comic.delete(); }
    }

    @Test public void failedEpubMobiPdfAndComicReadsPreserveFullMetadata() throws Exception {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        AppProfile.init(context);
        for (String extension : new String[]{".epub", ".mobi", ".pdf", ".cbz"}) {
            File book = File.createTempFile("unreadable-format-", extension, context.getCacheDir());
            try {
                try (FileOutputStream out = new FileOutputStream(book)) {
                    out.write("not a supported book body".getBytes(StandardCharsets.UTF_8));
                }
                FileMeta old = new FileMeta(book.getPath());
                old.setTitle("Known " + extension);
                old.setAuthor("Known author");
                old.setState(FileMetaCore.STATE_FULL);
                AppDB.get().saveAll(Collections.singletonList(old));
                FileMeta found = new FileMeta(book.getPath());
                found.setTitle(book.getName());
                assertTrue(new SearchAllBooksWorker(context, parameters()).publishLocalMetadata(
                        found, AppDB.get().load(book.getPath()), ScanOwnership.claim(), () -> false));
                FileMeta retained = AppDB.get().load(book.getPath());
                assertEquals(extension, "Known " + extension, retained.getTitle());
                assertEquals(extension, "Known author", retained.getAuthor());
                assertEquals(extension, FileMetaCore.STATE_FULL, retained.getState().intValue());
            } finally {
                AppDB.get().deleteBy(book.getPath());
                book.delete();
            }
        }
    }

    @Test public void malformedFb2CannotReplaceKnownMetadataWithFilenameFallback() throws Exception {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        AppProfile.init(context);
        File book = File.createTempFile("malformed-metadata-", ".fb2", context.getCacheDir());
        try {
            try (FileOutputStream out = new FileOutputStream(book)) {
                out.write("<FictionBook><description><title-info><book-title>Broken"
                        .getBytes(StandardCharsets.UTF_8));
            }
            FileMeta old = new FileMeta(book.getPath());
            old.setTitle("Known title");
            old.setAuthor("Known author");
            old.setState(FileMetaCore.STATE_FULL);
            AppDB.get().saveAll(Collections.singletonList(old));
            FileMeta found = new FileMeta(book.getPath());
            found.setTitle(book.getName());
            long owner = ScanOwnership.claim();
            assertTrue(new SearchAllBooksWorker(context, parameters()).publishLocalMetadata(
                    found, AppDB.get().load(book.getPath()), owner, () -> false));
            FileMeta retained = AppDB.get().load(book.getPath());
            assertEquals("Known title", retained.getTitle());
            assertEquals("Known author", retained.getAuthor());
            assertEquals(FileMetaCore.STATE_FULL, retained.getState().intValue());
        } finally {
            AppDB.get().deleteBy(book.getPath());
            book.delete();
        }
    }

    @Test public void replacedWorkerCannotPublishAfterBlockedMetadataOpen() throws Exception {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        AppProfile.init(context);
        String path = "content://scan-blocked-" + UUID.randomUUID() + "/book";
        FileMeta old = new FileMeta(path);
        old.setTitle("Known title");
        old.setSize(10L);
        AppDB.get().saveAll(Collections.singletonList(old));
        CountDownLatch entered = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        AtomicReference<Boolean> published = new AtomicReference<>();
        SearchAllBooksWorker worker = new SearchAllBooksWorker(context, parameters()) {
            @Override protected EbookMeta readSafMetadataForScan(FileMeta found) throws Exception {
                entered.countDown();
                if (!release.await(3, TimeUnit.SECONDS)) throw new IOException("Timed out fixture");
                throw new IOException("Provider returned a failed content open");
            }
        };
        FileMeta found = new FileMeta(path);
        found.setTitle("Discovered title");
        found.setSize(20L);
        long oldOwner = ScanOwnership.claim();
        Thread scan = new Thread(() -> published.set(worker.publishSafMetadata(
                found, AppDB.get().load(path), oldOwner, () -> false)));
        try {
            scan.start();
            assertTrue(entered.await(3, TimeUnit.SECONDS));
            ScanOwnership.claim();
            release.countDown();
            scan.join(3000);
            assertFalse(scan.isAlive());
            assertEquals(Boolean.FALSE, published.get());
            assertEquals("Known title", AppDB.get().load(path).getTitle());
            assertEquals(Long.valueOf(10), AppDB.get().load(path).getSize());
        } finally {
            release.countDown();
            scan.join(3000);
            AppDB.get().deleteBy(path);
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

    private static final class ControlledWorker extends SearchAllBooksWorker {
        boolean fail = true;
        ControlledWorker(Context context) { super(context, parameters()); }
        @Override protected EbookMeta readLocalMetadata(File source) throws IOException {
            if (fail) throw new IOException("Simulated content access failure");
            return super.readLocalMetadata(source);
        }
        @Override protected EbookMeta readSafMetadataForScan(FileMeta found) throws Exception {
            if (fail) throw new IOException("Simulated provider failure");
            return super.readSafMetadataForScan(found);
        }
    }

    @Test public void safDiscoveryAndFailedExtractionRetainAnExistingFullRow() throws Exception {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        AppProfile.init(context);
        File source = File.createTempFile("scan-saf-meta-", ".fb2", context.getCacheDir());
        Uri uri = FileProvider.getUriForFile(context,
                context.getPackageName() + ".provider", source);
        String path = uri.toString();
        try {
            String fb2 = "<?xml version='1.0' encoding='UTF-8'?>"
                    + "<FictionBook xmlns='http://www.gribuser.ru/xml/fictionbook/2.0'>"
                    + "<description><title-info><book-title>Provider recovered</book-title>"
                    + "<author><first-name>New</first-name><last-name>Writer</last-name></author>"
                    + "</title-info></description><body><section><p>Text</p></section></body>"
                    + "</FictionBook>";
            try (FileOutputStream output = new FileOutputStream(source)) {
                output.write(fb2.getBytes(StandardCharsets.UTF_8));
            }
            FileMeta old = new FileMeta(path);
            old.setTitle("Known provider title"); old.setAuthor("Known provider author");
            old.setAnnotation("Known provider description"); old.setState(FileMetaCore.STATE_FULL);
            old.setIsStar(true);
            AppDB.get().saveAll(Collections.singletonList(old));
            FileMeta found = new FileMeta(path);
            found.setTitle("story.fb2"); found.setPathTxt("story.fb2");
            found.setSize(source.length()); found.setDate(source.lastModified());
            found.setExt("fb2"); found.setIsSearchBook(true);
            AppDB.get().reconcileCompletedScan(Collections.singletonList(found),
                    Collections.emptySet());
            assertEquals(FileMetaCore.STATE_FULL,
                    AppDB.get().load(path).getState().intValue());
            ControlledWorker worker = new ControlledWorker(context);
            long owner = ScanOwnership.claim();
            assertTrue(worker.publishSafMetadata(found, AppDB.get().load(path), owner, () -> false));
            FileMeta failed = AppDB.get().load(path);
            assertEquals("Known provider title", failed.getTitle());
            assertEquals("Known provider author", failed.getAuthor());
            assertEquals("Known provider description", failed.getAnnotation());
            assertEquals(FileMetaCore.STATE_FULL, failed.getState().intValue());
            assertEquals(Boolean.TRUE, failed.getIsStar());
            worker.fail = false;
            assertTrue(worker.publishSafMetadata(found, failed, owner, () -> false));
            FileMeta recovered = AppDB.get().load(path);
            assertEquals("Provider recovered", recovered.getTitle());
            assertTrue(recovered.getAuthor().contains("Writer"));
            assertEquals(Boolean.TRUE, recovered.getIsStar());
        } finally {
            AppDB.get().deleteBy(path);
            source.delete();
        }
    }

    @Test public void newlyDiscoveredSafBookRemainsBasicUntilExtractionSucceeds() throws Exception {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        AppProfile.init(context);
        File source = File.createTempFile("scan-new-saf-", ".fb2", context.getCacheDir());
        Uri uri = FileProvider.getUriForFile(context,
                context.getPackageName() + ".provider", source);
        String path = uri.toString();
        try {
            String fb2 = "<?xml version='1.0' encoding='UTF-8'?>"
                    + "<FictionBook xmlns='http://www.gribuser.ru/xml/fictionbook/2.0'>"
                    + "<description><title-info><book-title>New SAF title</book-title>"
                    + "</title-info></description><body><section><p>Text</p></section></body>"
                    + "</FictionBook>";
            try (FileOutputStream output = new FileOutputStream(source)) {
                output.write(fb2.getBytes(StandardCharsets.UTF_8));
            }
            FileMeta found = new FileMeta(path);
            found.setTitle("new.fb2"); found.setPathTxt("new.fb2");
            found.setSize(source.length()); found.setDate(source.lastModified());
            found.setIsSearchBook(true);
            AppDB.get().reconcileCompletedScan(Collections.singletonList(found),
                    Collections.emptySet());
            assertEquals(FileMetaCore.STATE_BASIC, AppDB.get().load(path).getState().intValue());
            ControlledWorker worker = new ControlledWorker(context);
            long owner = ScanOwnership.claim();
            FileMeta baseline = AppDB.get().load(path);
            assertTrue(worker.publishSafMetadata(found, baseline, owner, () -> false));
            FileMeta afterFailure = AppDB.get().load(path);
            assertEquals(FileMetaCore.STATE_BASIC, afterFailure.getState().intValue());
            worker.fail = false;
            assertTrue(worker.publishSafMetadata(found, afterFailure, owner, () -> false));
            FileMeta recovered = AppDB.get().load(path);
            assertEquals(FileMetaCore.STATE_FULL, recovered.getState().intValue());
            assertEquals("New SAF title", recovered.getTitle());
        } finally {
            AppDB.get().deleteBy(path);
            source.delete();
        }
    }

    @Test public void inaccessibleCalibreSidecarCannotReplaceKnownMetadata() throws Exception {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        AppProfile.init(context);
        boolean previousCalibre = AppState.get().isUseCalibreOpf;
        boolean previousNames = AppState.get().isShowOnlyOriginalFileNames;
        File source = File.createTempFile("scan-opf-book-", ".fb2", context.getCacheDir());
        File opf = File.createTempFile("scan-opf-meta-", ".opf", context.getCacheDir());
        Uri uri = FileProvider.getUriForFile(context,
                context.getPackageName() + ".provider", source);
        String path = uri.toString();
        try {
            AppState.get().isUseCalibreOpf = true;
            AppState.get().isShowOnlyOriginalFileNames = false;
            assertTrue(opf.delete());
            try (FileOutputStream output = new FileOutputStream(source)) {
                output.write("<FictionBook><description><title-info><book-title>Source title"
                        .concat("</book-title></title-info></description></FictionBook>")
                        .getBytes(StandardCharsets.UTF_8));
            }
            FileMeta old = new FileMeta(path);
            old.setTitle("Known Calibre title"); old.setAuthor("Known Calibre author");
            old.setState(FileMetaCore.STATE_FULL);
            AppDB.get().saveAll(Collections.singletonList(old));
            FileMeta found = new FileMeta(path);
            found.setTitle("book.fb2"); found.setPathTxt("book.fb2");
            found.setSize(source.length()); found.setDate(source.lastModified());
            SafOpfRegistry.Entry sidecar = new SafOpfRegistry.Entry(Uri.fromFile(opf),
                    Collections.emptyMap(), "revision-1");
            ControlledWorker worker = new ControlledWorker(context);
            worker.fail = false;
            long owner = ScanOwnership.claim();
            assertTrue(worker.publishSafMetadata(found, AppDB.get().load(path), sidecar,
                    owner, () -> false));
            FileMeta afterFailure = AppDB.get().load(path);
            assertEquals("Known Calibre title", afterFailure.getTitle());
            assertEquals("Known Calibre author", afterFailure.getAuthor());
            try (FileOutputStream output = new FileOutputStream(opf)) {
                output.write("<package><metadata><dc:title xmlns:dc='http://purl.org/dc/elements/1.1/'>Partial title</dc:title><broken"
                        .getBytes(StandardCharsets.UTF_8));
            }
            assertTrue(worker.publishSafMetadata(found, afterFailure, sidecar,
                    owner, () -> false));
            FileMeta afterMalformed = AppDB.get().load(path);
            assertEquals("Known Calibre title", afterMalformed.getTitle());
            assertEquals("Known Calibre author", afterMalformed.getAuthor());
            try (FileOutputStream output = new FileOutputStream(opf)) {
                output.write(("<package xmlns:dc='http://purl.org/dc/elements/1.1/'>"
                        + "<metadata><dc:title>Recovered Calibre title</dc:title>"
                        + "<dc:creator>Recovered author</dc:creator></metadata></package>")
                        .getBytes(StandardCharsets.UTF_8));
            }
            assertTrue(worker.publishSafMetadata(found, afterMalformed, sidecar,
                    owner, () -> false));
            FileMeta recovered = AppDB.get().load(path);
            assertEquals("Recovered Calibre title", recovered.getTitle());
            assertEquals("Recovered author", recovered.getAuthor());
        } finally {
            AppState.get().isUseCalibreOpf = previousCalibre;
            AppState.get().isShowOnlyOriginalFileNames = previousNames;
            AppDB.get().deleteBy(path);
            source.delete(); opf.delete();
        }
    }

    @Test public void discoveryCannotEraseGoodMetadataWhenContentReadFails() throws Exception {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        AppProfile.init(context);
        File root = Files.createTempDirectory(context.getCacheDir().toPath(), "scan-meta-").toFile();
        File book = new File(root, "story.fb2");
        try {
            String fb2 = "<?xml version='1.0' encoding='UTF-8'?>"
                    + "<FictionBook xmlns='http://www.gribuser.ru/xml/fictionbook/2.0'>"
                    + "<description><title-info><book-title>Recovered title</book-title>"
                    + "<author><first-name>New</first-name><last-name>Writer</last-name></author>"
                    + "<lang>en</lang></title-info></description><body><section><p>Text</p>"
                    + "</section></body></FictionBook>";
            try (FileOutputStream out = new FileOutputStream(book)) {
                out.write(fb2.getBytes(StandardCharsets.UTF_8));
            }
            FileMeta old = new FileMeta(book.getPath());
            old.setTitle("Known title");
            old.setAuthor("Known author");
            old.setAnnotation("Known description");
            old.setState(FileMetaCore.STATE_FULL);
            old.setIsStar(true);
            AppDB.get().saveAll(Collections.singletonList(old));
            List<FileMeta> discovered = new ArrayList<>();
            LocalDiscovery.collect(root, ExtUtils.seachExts, discovered, () -> false);
            assertEquals(1, discovered.size());
            FileMeta found = discovered.get(0);
            ControlledWorker worker = new ControlledWorker(context);
            long owner = ScanOwnership.claim();
            boolean[] acknowledged = new boolean[1];
            assertTrue(worker.publishLocalMetadata(found, AppDB.get().load(book.getPath()),
                    owner, () -> false, acknowledged));
            assertFalse("A failed extraction must remain due for retry", acknowledged[0]);
            FileMeta afterFailure = AppDB.get().load(book.getPath());
            assertEquals("Known title", afterFailure.getTitle());
            assertEquals("Known author", afterFailure.getAuthor());
            assertEquals("Known description", afterFailure.getAnnotation());
            assertEquals(FileMetaCore.STATE_FULL, afterFailure.getState().intValue());
            assertEquals(Boolean.TRUE, afterFailure.getIsStar());
            assertEquals(Long.valueOf(book.length()), afterFailure.getSize());
            worker.fail = false;
            assertTrue(worker.publishLocalMetadata(found, afterFailure,
                    owner, () -> false, acknowledged));
            assertTrue(acknowledged[0]);
            FileMeta recovered = AppDB.get().load(book.getPath());
            assertEquals("Recovered title", recovered.getTitle());
            assertTrue(recovered.getAuthor().contains("Writer"));
            assertEquals(Boolean.TRUE, recovered.getIsStar());
        } finally {
            AppDB.get().deleteBy(book.getPath());
            book.delete();
            root.delete();
        }
    }
}
