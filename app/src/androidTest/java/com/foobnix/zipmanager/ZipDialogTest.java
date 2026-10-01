package com.foobnix.zipmanager;

import androidx.test.platform.app.InstrumentationRegistry;
import com.foobnix.ext.CacheZipUtils;
import com.foobnix.pdf.info.BookCacheLeases;
import com.foobnix.pdf.info.ArchiveMemberIdentity;
import com.foobnix.pdf.info.ExtUtils;
import com.foobnix.dao2.FileMeta;
import com.foobnix.ui2.AppDB;
import com.foobnix.model.AppBookmark;
import org.junit.Test;
import java.io.File;
import java.io.FileOutputStream;
import java.nio.file.Files;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import static org.junit.Assert.*;

public class ZipDialogTest {
    @Test public void selectedHtmlMemberKeepsItsImageSibling() throws Exception {
        File root = Files.createTempDirectory(InstrumentationRegistry.getInstrumentation()
                .getTargetContext().getCacheDir().toPath(), "archive-html-image-").toFile();
        File archive = new File(root, "book.zip");
        File prior = CacheZipUtils.CACHE_RECENT;
        CacheZipUtils.CACHE_RECENT = new File(root, "recent");
        File extracted = null;
        try {
            try (ZipOutputStream output = new ZipOutputStream(new FileOutputStream(archive))) {
                output.putNextEntry(new ZipEntry("book.html"));
                output.write("<html><img src='cover.png'></html>".getBytes(
                        java.nio.charset.StandardCharsets.UTF_8));
                output.closeEntry();
                output.putNextEntry(new ZipEntry("cover.png"));
                output.write(new byte[]{1, 2, 3});
                output.closeEntry();
            }
            assertTrue(CacheZipUtils.isSingleAndSupportEntryInner(archive.getPath()).first);
            extracted = ZipDialog.extractFile(null, "book.html", archive, true);
            assertNotNull(extracted);
            assertArrayEquals(new byte[]{1, 2, 3}, Files.readAllBytes(
                    new File(extracted.getParentFile(), "cover.png").toPath()));
        } finally {
            if (extracted != null) {
                BookCacheLeases.cancelReservation(extracted);
                BookCacheLeases.evictTree(extracted.getParentFile());
            }
            CacheZipUtils.CACHE_RECENT = prior;
            BookCacheLeases.evictTree(root);
        }
    }

    @Test public void overlappingSafGrantsNameTheSameArchiveMember() {
        android.net.Uri parent = android.provider.DocumentsContract.buildDocumentUriUsingTree(
                android.provider.DocumentsContract.buildTreeDocumentUri("archive.fixture", "books"),
                "books/collection.zip");
        android.net.Uri child = android.provider.DocumentsContract.buildDocumentUriUsingTree(
                android.provider.DocumentsContract.buildTreeDocumentUri("archive.fixture", "books/child"),
                "books/collection.zip");
        String one = ArchiveMemberIdentity.create(parent.toString(), "volume/book.epub");
        String two = ArchiveMemberIdentity.create(child.toString(), "volume/book.epub");
        assertEquals(one, two);
        assertEquals("volume/book.epub", ArchiveMemberIdentity.entry(one));
        assertEquals(com.foobnix.pdf.info.SafDocumentIdentity.canonical(parent).toString(),
                ArchiveMemberIdentity.source(one));
    }
    @Test public void memberIdentitySurvivesExtractionCleanupAndDistinguishesArchivesAndEntries() throws Exception {
        File root = Files.createTempDirectory(InstrumentationRegistry.getInstrumentation()
                .getTargetContext().getCacheDir().toPath(), "archive-identity-").toFile();
        File prior = CacheZipUtils.CACHE_RECENT;
        CacheZipUtils.CACHE_RECENT = new File(root, "recent");
        File firstArchive = new File(root, "one.zip");
        File secondArchive = new File(root, "two.zip");
        try {
            for (File archive : new File[]{firstArchive, secondArchive}) {
                try (ZipOutputStream output = new ZipOutputStream(new FileOutputStream(archive))) {
                    for (String entry : new String[]{"first/book.txt", "second/book.txt"}) {
                        output.putNextEntry(new ZipEntry(entry));
                        output.write(entry.getBytes(java.nio.charset.StandardCharsets.UTF_8));
                        output.closeEntry();
                    }
                }
            }
            File first = ZipDialog.extractFile(null, "first/book.txt", firstArchive, false);
            File otherEntry = ZipDialog.extractFile(null, "second/book.txt", firstArchive, false);
            File otherArchive = ZipDialog.extractFile(null, "first/book.txt", secondArchive, false);
            assertNotNull(first); assertNotNull(otherEntry); assertNotNull(otherArchive);
            String identity = ArchiveMemberIdentity.forExtractedFile(first);
            assertEquals(ArchiveMemberIdentity.create(firstArchive.getPath(), "first/book.txt"), identity);
            assertEquals(identity, ExtUtils.recentPathFromIntent(null, first.getPath()));
            com.foobnix.model.AppBook progress = new com.foobnix.model.AppBook(identity);
            progress.p = 0.43f;
            org.ebookdroid.common.settings.books.SharedBooks.cache.put(
                    ExtUtils.getFileName(identity), progress);
            assertSame(progress, org.ebookdroid.common.settings.SettingsManager
                    .getBookSettings(new android.content.Intent(), first.getPath()));
            AppBookmark bookmark = new AppBookmark();
            bookmark.setPath(identity);
            assertEquals(identity, bookmark.getPath());
            assertNotEquals(identity, ArchiveMemberIdentity.forExtractedFile(otherEntry));
            assertNotEquals(identity, ArchiveMemberIdentity.forExtractedFile(otherArchive));
            assertNotEquals(ExtUtils.getFileName(identity),
                    ExtUtils.getFileName(ArchiveMemberIdentity.forExtractedFile(otherArchive)));
            for (File file : new File[]{first, otherEntry, otherArchive}) {
                BookCacheLeases.cancelReservation(file);
                assertTrue(BookCacheLeases.evictTree(file.getParentFile()));
            }
            assertTrue(ArchiveMemberIdentity.sourceMayExist(identity));
            assertEquals(1, AppDB.removeNotExist(new java.util.ArrayList<>(
                    java.util.Collections.singletonList(new FileMeta(identity)))).size());
            assertTrue(ExtUtils.isAvailableBookSource(identity));
            File reopened = ZipDialog.extractFile(null, ArchiveMemberIdentity.entry(identity),
                    new File(ArchiveMemberIdentity.source(identity)), false);
            assertNotNull(reopened);
            assertEquals(identity, ArchiveMemberIdentity.forExtractedFile(reopened));
            assertSame(progress, org.ebookdroid.common.settings.SettingsManager
                    .getBookSettings(new android.content.Intent(), reopened.getPath()));
            BookCacheLeases.cancelReservation(reopened);
            org.ebookdroid.common.settings.books.SharedBooks.cache.remove(ExtUtils.getFileName(identity));
        } finally {
            CacheZipUtils.CACHE_RECENT = prior;
            BookCacheLeases.evictTree(root);
        }
    }
    @Test public void concurrentArchiveMembersHaveDistinctCompletePublicationAndCleanup() throws Exception {
        File root = Files.createTempDirectory(InstrumentationRegistry.getInstrumentation()
                .getTargetContext().getCacheDir().toPath(), "archive-publication-").toFile();
        File prior = CacheZipUtils.CACHE_RECENT;
        File archive = new File(root, "books.zip");
        CacheZipUtils.CACHE_RECENT = new File(root, "recent");
        try {
            try (ZipOutputStream output = new ZipOutputStream(new FileOutputStream(archive))) {
                output.putNextEntry(new ZipEntry("book.txt"));
                output.write(new byte[]{1, 2, 3});
                output.closeEntry();
            }
            File first = ZipDialog.extractFile(null, "book.txt", archive, true);
            File second = ZipDialog.extractFile(null, "book.txt", archive, true);
            assertNotNull(first); assertNotNull(second);
            assertNotEquals(first.getParent(), second.getParent());
            assertArrayEquals(new byte[]{1, 2, 3}, Files.readAllBytes(first.toPath()));
            assertArrayEquals(new byte[]{1, 2, 3}, Files.readAllBytes(second.toPath()));
            assertFalse(first.getParentFile().getName().endsWith(".part"));
            assertFalse(BookCacheLeases.evictTree(first.getParentFile()));
            BookCacheLeases.cancelReservation(first);
            BookCacheLeases.cancelReservation(second);
            assertTrue(BookCacheLeases.evictTree(first.getParentFile()));
            assertTrue(BookCacheLeases.evictTree(second.getParentFile()));
        } finally {
            CacheZipUtils.CACHE_RECENT = prior;
            BookCacheLeases.evictTree(root);
        }
    }
}
