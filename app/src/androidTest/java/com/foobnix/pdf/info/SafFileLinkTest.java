package com.foobnix.pdf.info;

import androidx.test.platform.app.InstrumentationRegistry;
import android.net.Uri;
import org.junit.Test;
import java.io.File;
import java.nio.file.Files;
import static org.junit.Assert.*;

public class SafFileLinkTest {
    @Test public void nativeExtractionPathRetainsOriginalNameAndCleansUp() throws Exception {
        var context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        File source = File.createTempFile("link-source-", ".pdf", context.getCacheDir());
        Files.write(source.toPath(), new byte[]{1,2,3});
        File linked, directory;
        try {
            try (SafFileLink link = new SafFileLink(context, Uri.fromFile(source), "Fluent Python.pdf")) {
                linked = link.file; directory = linked.getParentFile();
                assertEquals("Fluent Python.pdf", linked.getName());
                assertArrayEquals(new byte[]{1,2,3}, Files.readAllBytes(linked.toPath()));
            }
            assertFalse(linked.exists()); assertFalse(directory.exists()); assertTrue(source.exists());
        } finally { source.delete(); }
    }
    @Test public void identicalDisplayNamesHaveIndependentDescriptorsAndDirectories() throws Exception {
        var context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        File first = File.createTempFile("link-first-", ".epub", context.getCacheDir());
        File second = File.createTempFile("link-second-", ".epub", context.getCacheDir());
        Files.write(first.toPath(), new byte[]{1}); Files.write(second.toPath(), new byte[]{2});
        try (SafFileLink a = new SafFileLink(context, Uri.fromFile(first), "Book.epub");
             SafFileLink b = new SafFileLink(context, Uri.fromFile(second), "Book.epub")) {
            assertNotEquals(a.file.getParentFile(), b.file.getParentFile());
            assertArrayEquals(new byte[]{1}, Files.readAllBytes(a.file.toPath()));
            assertArrayEquals(new byte[]{2}, Files.readAllBytes(b.file.toPath()));
        } finally { first.delete(); second.delete(); }
    }
    @Test public void pipeProviderIsStagedWithoutLosingFirstByte() throws Exception {
        android.os.ParcelFileDescriptor[] pipe = android.os.ParcelFileDescriptor.createPipe();
        byte[] body = new byte[]{80, 75, 3, 4, 5, 6};
        try (java.io.OutputStream output = new android.os.ParcelFileDescriptor.AutoCloseOutputStream(pipe[1])) {
            output.write(body);
        }
        assertFalse(SafFileLink.seekableRegularFile(pipe[0]));
        File staged;
        try (SafFileLink link = new SafFileLink(
                InstrumentationRegistry.getInstrumentation().getTargetContext(), pipe[0], "Book.epub")) {
            staged = link.file;
            assertArrayEquals(body, Files.readAllBytes(staged.toPath()));
            assertArrayEquals("The staged file can be opened again", body, Files.readAllBytes(staged.toPath()));
        }
        assertFalse(staged.exists());
    }
    @Test public void startupSweepCannotRemoveAnOpenStagedBook() throws Exception {
        var context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        File source = File.createTempFile("sweep-source-", ".epub", context.getCacheDir());
        Files.write(source.toPath(), new byte[]{1, 2});
        try (SafFileLink link = new SafFileLink(context, Uri.fromFile(source), "Book.epub")) {
            BookCacheLeases.sweepAbandoned(context.getCacheDir());
            assertArrayEquals(new byte[]{1, 2}, Files.readAllBytes(link.file.toPath()));
        } finally { source.delete(); }
    }

}
