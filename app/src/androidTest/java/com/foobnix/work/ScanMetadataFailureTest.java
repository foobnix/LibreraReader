package com.foobnix.work;

import android.content.Context;
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
            assertTrue(worker.publishLocalMetadata(found, AppDB.get().load(book.getPath()),
                    owner, () -> false));
            FileMeta afterFailure = AppDB.get().load(book.getPath());
            assertEquals("Known title", afterFailure.getTitle());
            assertEquals("Known author", afterFailure.getAuthor());
            assertEquals("Known description", afterFailure.getAnnotation());
            assertEquals(FileMetaCore.STATE_FULL, afterFailure.getState().intValue());
            assertEquals(Boolean.TRUE, afterFailure.getIsStar());
            assertEquals(Long.valueOf(book.length()), afterFailure.getSize());
            worker.fail = false;
            assertTrue(worker.publishLocalMetadata(found, afterFailure, owner, () -> false));
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
