package com.foobnix.work;

import androidx.test.platform.app.InstrumentationRegistry;
import com.foobnix.dao2.FileMeta;
import com.foobnix.pdf.info.model.BookCSS;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.junit.Test;
import static org.junit.Assert.*;

public class LocalDiscoveryTest {
    @Test public void selectedButUnavailableFolderKeepsItsSelection() {
        String missing = "/unavailable-selected-" + System.nanoTime();
        assertEquals(Collections.singletonList(missing),
                BookCSS.filtered(Collections.singletonList(missing)));
        assertTrue(BookCSS.filtered(Collections.emptyList()).isEmpty());
    }

    @Test public void unreadableChildDoesNotProduceACompleteListing() throws Exception {
        File root = new File(InstrumentationRegistry.getInstrumentation().getTargetContext()
                .getCacheDir(), "scan-test-" + System.nanoTime());
        File child = new File(root, "books");
        assertTrue(child.mkdirs());
        File healthy = new File(root, "healthy.epub");
        java.nio.file.Files.write(healthy.toPath(), new byte[]{1});
        try {
            List<FileMeta> found = new ArrayList<>();
            try {
                LocalDiscovery.collect(root, Collections.singletonList(".epub"), found,
                        () -> false, folder -> folder.equals(child) ? null : folder.listFiles());
                fail("Unreadable child must invalidate removal evidence");
            } catch (IOException expected) {
                assertTrue(expected.getMessage().contains("Cannot list"));
                assertEquals(1, found.size());
                assertEquals(healthy.getPath(), found.get(0).getPath());
            }
        } finally {
            child.delete();
            healthy.delete();
            root.delete();
        }
    }

    @Test public void unavailableSelectedRootIsNotAnEmptyLibrary() throws Exception {
        File absent = new File(InstrumentationRegistry.getInstrumentation().getTargetContext()
                .getCacheDir(), "missing-scan-" + System.nanoTime());
        try {
            LocalDiscovery.collect(absent, Collections.singletonList(".epub"),
                    new ArrayList<>(), () -> false);
            fail("Unavailable root must not authorize removals");
        } catch (IOException expected) {
            assertTrue(expected.getMessage().contains("unavailable"));
        }
    }
    @Test public void unreadableSubtreeDoesNotProtectDeletedBooksInReadableSiblings() throws Exception {
        var context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        com.foobnix.model.AppProfile.init(context);
        File root = new File(context.getCacheDir(), "partial-local-" + System.nanoTime());
        File offline = new File(root, "offline");
        assertTrue(offline.mkdirs());
        String missing = new File(root, "deleted.epub").getPath();
        String retained = new File(offline, "unavailable.epub").getPath();
        FileMeta gone = new FileMeta(missing); gone.setIsSearchBook(true);
        FileMeta hidden = new FileMeta(retained); hidden.setIsSearchBook(true);
        com.foobnix.ui2.AppDB.get().saveAll(java.util.Arrays.asList(gone, hidden));
        try {
            List<FileMeta> found = new ArrayList<>();
            java.util.Set<String> incomplete = new java.util.HashSet<>();
            LocalDiscovery.collectPartial(root, Collections.singletonList(".epub"), found,
                    () -> false, folder -> folder.equals(offline) ? null : folder.listFiles(), incomplete);
            assertEquals(Collections.singleton(offline.getPath()), incomplete);
            com.foobnix.ui2.AppDB.get().reconcileCompletedScan(found, Collections.singleton(root.getPath()),
                    Collections.emptyMap(), incomplete);
            assertFalse(com.foobnix.ui2.AppDB.get().load(missing).getIsSearchBook());
            assertTrue(com.foobnix.ui2.AppDB.get().load(retained).getIsSearchBook());
        } finally {
            com.foobnix.ui2.AppDB.get().deleteBy(missing);
            com.foobnix.ui2.AppDB.get().deleteBy(retained);
            offline.delete(); root.delete();
        }
    }

    @Test public void unavailableSelectedRootStaysProtectedWithinAnOverlappingReadableRoot() throws Exception {
        var context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        com.foobnix.model.AppProfile.init(context);
        File parent = new File(context.getCacheDir(), "overlap-local-" + System.nanoTime());
        assertTrue(parent.mkdir());
        File unavailable = new File(parent, "unmounted");
        String retained = new File(unavailable, "Book.epub").getPath();
        String missing = new File(parent, "deleted.epub").getPath();
        FileMeta offline = new FileMeta(retained); offline.setIsSearchBook(true);
        FileMeta gone = new FileMeta(missing); gone.setIsSearchBook(true);
        com.foobnix.ui2.AppDB.get().saveAll(java.util.Arrays.asList(offline, gone));
        try {
            java.util.Set<String> incomplete = new java.util.HashSet<>();
            List<FileMeta> found = new ArrayList<>();
            assertThrows(IOException.class, () -> LocalDiscovery.collectPartial(unavailable,
                    Collections.singletonList(".epub"), found, () -> false, File::listFiles, incomplete));
            LocalDiscovery.collectPartial(parent, Collections.singletonList(".epub"), found,
                    () -> false, File::listFiles, incomplete);
            com.foobnix.ui2.AppDB.get().reconcileCompletedScan(found, Collections.singleton(parent.getPath()),
                    Collections.emptyMap(), incomplete);
            assertTrue(com.foobnix.ui2.AppDB.get().load(retained).getIsSearchBook());
            assertFalse(com.foobnix.ui2.AppDB.get().load(missing).getIsSearchBook());
        } finally {
            com.foobnix.ui2.AppDB.get().deleteBy(retained); com.foobnix.ui2.AppDB.get().deleteBy(missing);
            parent.delete();
        }
    }
    @Test public void policyExcludedAndroidDataDoesNotMakeAnEmptyScanIncomplete() throws Exception {
        var context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        File root = new File(context.getCacheDir(), "policy-skip-" + System.nanoTime());
        File excluded = new File(root, "Android/data");
        assertTrue(excluded.mkdirs());
        try {
            List<FileMeta> found = new ArrayList<>();
            java.util.Set<String> incomplete = new java.util.HashSet<>();
            LocalDiscovery.collectPartial(root, Collections.singletonList(".epub"), found,
                    () -> false, folder -> folder.equals(excluded) ? null : folder.listFiles(), incomplete);
            assertTrue(found.isEmpty());
            assertTrue("Policy exclusions must allow empty-library sample seeding", incomplete.isEmpty());
        } finally { excluded.delete(); excluded.getParentFile().delete(); root.delete(); }
    }

}
