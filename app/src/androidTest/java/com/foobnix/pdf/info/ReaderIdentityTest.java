package com.foobnix.pdf.info;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import androidx.test.platform.app.InstrumentationRegistry;

import com.foobnix.dao2.FileMeta;
import com.foobnix.pdf.search.activity.HorizontalModeController;
import com.foobnix.ui2.AppDB;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.UUID;

import static org.junit.Assert.*;

/** Uses isolated DB rows; does not open books, change progress, or access a SAF provider. */
public class ReaderIdentityTest {
    private String original;
    private String staged;
    private Intent intent;
    private FileMeta book;

    @Before public void setUp() {
        com.foobnix.model.AppProfile.init(
                InstrumentationRegistry.getInstrumentation().getTargetContext());
        String id = UUID.randomUUID().toString();
        original = "content://reader-test/document/" + id;
        staged = "/reader-test/source-" + id + ".pdf";
        intent = new Intent().setData(Uri.fromFile(new java.io.File(staged)));
        SafReaderLaunch.attach(InstrumentationRegistry.getInstrumentation().getTargetContext(), intent, original);
        book = AppDB.get().getOrCreate(original);
        book.setTitle("Fluent Python");
        book.setPathTxt("Fluent Python - Luciano Ramalho.pdf");
        AppDB.get().save(book);
    }

    @After public void tearDown() {
        org.ebookdroid.common.settings.books.SharedBooks.cache.remove(ExtUtils.getFileName(original));
        org.ebookdroid.common.settings.books.SharedBooks.cache.remove(ExtUtils.getFileName(staged));
        AppDB.get().deleteBy(original);
        AppDB.get().deleteBy(staged);
    }

    private String initialReaderTitle() {
        java.util.concurrent.atomic.AtomicReference<String> title = new java.util.concurrent.atomic.AtomicReference<>();
        InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
            Activity activity = new Activity();
            activity.setIntent(intent);
            title.set(HorizontalModeController.getTempTitle(activity));
        });
        return title.get();
    }

    @Test public void stagedPdfUsesLibraryTitleAndOriginalMetadata() {
        assertEquals("Fluent Python", ExtUtils.safReaderTitle(intent, staged));
        assertEquals(original, ExtUtils.readerBookMeta(intent, staged).getPath());
        assertNull("Display lookup must not create a staged book row", AppDB.get().load(staged));
        assertEquals("Fluent Python", initialReaderTitle());
        assertEquals("Fluent Python", HorizontalModeController.getTitle(original));
    }

    @Test public void missingTitleUsesOriginalFilename() {
        book.setTitle("");
        AppDB.get().save(book);
        assertEquals(book.getPathTxt(), ExtUtils.safReaderTitle(intent, staged));
        assertEquals(book.getPathTxt(), HorizontalModeController.getTitle(original));
    }

    @Test public void stagedEpubUsesTheSameOriginalIdentity() {
        staged = staged.replace(".pdf", ".epub");
        intent.setData(Uri.fromFile(new java.io.File(staged)));
        SafReaderLaunch.attach(InstrumentationRegistry.getInstrumentation().getTargetContext(), intent, original);
        book.setPathTxt("Fluent Python.epub");
        AppDB.get().save(book);
        assertEquals("Fluent Python", initialReaderTitle());
        assertEquals(original, ExtUtils.recentPathFromIntent(intent, staged));
    }

    @Test public void localFilesKeepTheirIdentityAndPdfFilenameBehavior() {
        intent.removeExtra("SAF_ORIGINAL_URI");
        assertEquals(staged, ExtUtils.recentPathFromIntent(intent, staged));
        assertEquals(staged, ExtUtils.recentPathFromIntent(null, staged));
        assertNull(ExtUtils.safReaderTitle(intent, staged));
        assertEquals(new java.io.File(staged).getName(), HorizontalModeController.getTitle(staged));
        intent.putExtra("SAF_ORIGINAL_URI", "");
        assertEquals(staged, ExtUtils.recentPathFromIntent(intent, staged));
    }

    @Test public void differentStagedVersionsRestoreTheSameReadingProgress() {
        com.foobnix.model.AppBook previous = org.ebookdroid.common.settings.SettingsManager.getBookSettings();
        com.foobnix.model.AppBook saved = new com.foobnix.model.AppBook(original);
        saved.p = 0.75f;
        org.ebookdroid.common.settings.books.SharedBooks.cache.put(ExtUtils.getFileName(original), saved);
        try {
            assertSame(saved, org.ebookdroid.common.settings.SettingsManager.getBookSettings(intent, staged));
            assertSame(saved, org.ebookdroid.common.settings.SettingsManager.getBookSettings(intent, staged + "-new-version"));
            assertEquals(0.75f, org.ebookdroid.common.settings.SettingsManager.getBookSettings().p, 0.001f);
        } finally {
            org.ebookdroid.common.settings.books.SharedBooks.cache.remove(ExtUtils.getFileName(original));
            if (previous != null) org.ebookdroid.common.settings.SettingsManager.getBookSettings(previous.path);
        }
    }

    @Test public void readerStartupDoesNotExtractStagedFilenameMetadata() throws Exception {
        java.io.File source = java.io.File.createTempFile("reader-metadata-", ".pdf",
                InstrumentationRegistry.getInstrumentation().getTargetContext().getCacheDir());
        try {
            intent.setData(Uri.fromFile(source));
            SafReaderLaunch.attach(InstrumentationRegistry.getInstrumentation().getTargetContext(), intent, original);
            InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
                Activity activity = new Activity(); activity.setIntent(intent);
                com.foobnix.ui2.FileMetaCore.checkOrCreateMetaInfo(activity);
            });
            assertNull(AppDB.get().load(source.getPath()));
            assertEquals("Fluent Python", AppDB.get().load(original).getTitle());
        } finally { AppDB.get().deleteBy(source.getPath()); source.delete(); }
    }

    private void checkLegacyProgress(float canonicalProgress, long canonicalTime, float expected) {
        var manager = org.ebookdroid.common.settings.SettingsManager.getBookSettings();
        var canonical = new com.foobnix.model.AppBook(original);
        canonical.p = canonicalProgress; canonical.t = canonicalTime;
        var legacy = new com.foobnix.model.AppBook(staged);
        legacy.p = 0.6f; legacy.t = 100; legacy.cp = true; legacy.z = 150;
        org.ebookdroid.common.settings.books.SharedBooks.cache.put(ExtUtils.getFileName(original), canonical);
        org.ebookdroid.common.settings.books.SharedBooks.cache.put(ExtUtils.getFileName(staged), legacy);
        try {
            var restored = org.ebookdroid.common.settings.SettingsManager.getBookSettings(intent, staged);
            assertEquals(original, restored.path); assertEquals(expected, restored.p, 0.001f);
            if (canonicalTime == 0 && canonicalProgress == 0) {
                assertTrue(restored.cp); assertEquals(150, restored.z); assertEquals(100, restored.t);
            }
            assertEquals(staged, legacy.path); assertEquals(0.6f, legacy.p, 0.001f);
        } finally {
            if (manager != null) org.ebookdroid.common.settings.SettingsManager.getBookSettings(manager.path);
        }
    }

    @Test public void legacyStagedReadingStateSeedsAnUnusedOriginalEntry() { checkLegacyProgress(0, 0, 0.6f); }
    @Test public void canonicalSavedProgressIsNotOverwrittenByLegacyStagedState() { checkLegacyProgress(0.2f, 200, 0.2f); }
    @Test public void explicitlySavedZeroProgressIsNotOverwrittenByLegacyState() { checkLegacyProgress(0, 200, 0); }
    @Test public void exportedReaderRejectsForgedOrChangedIdentityExtras() {
        Intent forged = new Intent().setData(intent.getData()).putExtra("SAF_ORIGINAL_URI", original);
        assertEquals(staged, ExtUtils.recentPathFromIntent(forged, staged));
        Intent changedIdentity = new Intent(intent).putExtra("SAF_ORIGINAL_URI", "content://fixture/document/other");
        assertEquals(staged, ExtUtils.recentPathFromIntent(changedIdentity, staged));
        Intent changedFile = new Intent(intent).setData(Uri.fromFile(new java.io.File("/other.pdf")));
        assertNull(SafReaderLaunch.original(changedFile));
        assertEquals(original, ExtUtils.recentPathFromIntent(new Intent(intent), staged));
    }
}
