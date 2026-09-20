package com.foobnix.ui2;

import androidx.test.platform.app.InstrumentationRegistry;
import com.foobnix.dao2.FileMeta;
import com.foobnix.model.AppProfile;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

public class ScanReconciliationTest {
    @Before public void setUp() {
        AppProfile.init(InstrumentationRegistry.getInstrumentation().getTargetContext());
    }

    @Test public void completedScanAndMetadataDoNotOverwriteUserChanges() {
        String root = "/scan-fixture-" + UUID.randomUUID();
        String path = root + "/book.epub";
        try {
            FileMeta old = new FileMeta(path);
            old.setTitle("Original");
            old.setIsSearchBook(true);
            old.setIsRecentProgress(0.2f);
            AppDB.get().saveAll(Collections.singletonList(old));
            FileMeta snapshot = AppDB.get().scanSnapshot().stream()
                    .filter(row -> path.equals(row.getPath())).findFirst().get();

            FileMeta user = AppDB.get().load(path);
            user.setTitle("My title");
            user.setIsStar(true);
            user.setIsRecent(true);
            AppDB.get().save(user);
            AppDB.get().updateReadingProgress(path, 0.8f);

            FileMeta found = new FileMeta(path);
            found.setTitle("book.epub");
            found.setIsSearchBook(true);
            AppDB.get().reconcileCompletedScan(Collections.singletonList(found),
                    Collections.singleton(root));
            FileMeta extracted = new FileMeta(path);
            extracted.setTitle("Extracted title");
            extracted.setAuthor("Extracted author");
            AppDB.get().updateScannedMetadata(extracted, snapshot);

            FileMeta result = AppDB.get().scanSnapshot().stream()
                    .filter(row -> path.equals(row.getPath())).findFirst().get();
            assertEquals("My title", result.getTitle());
            assertEquals("Extracted author", result.getAuthor());
            assertEquals(0.8f, result.getIsRecentProgress(), 0.0001f);
            assertEquals(Boolean.TRUE, result.getIsStar());
            assertEquals(Boolean.TRUE, result.getIsRecent());
            assertEquals(Boolean.TRUE, result.getIsSearchBook());
        } finally {
            AppDB.get().deleteBy(path);
        }
    }
    @Test public void explicitLocalDeselectionPreservesUserStateAndOverlappingRoot() {
        String parent = "/scan-fixture-" + UUID.randomUUID();
        String nested = parent + "/nested";
        String path = nested + "/book.epub";
        try {
            FileMeta row = new FileMeta(path);
            row.setTitle("Book");
            row.setIsSearchBook(true);
            row.setIsStar(true);
            row.setIsRecentProgress(0.4f);
            AppDB.get().saveAll(Collections.singletonList(row));
            AppDB.get().reconcileDeselectedRoots(Collections.singleton(nested),
                    Collections.singleton(parent));
            assertEquals(Boolean.TRUE, AppDB.get().load(path).getIsSearchBook());
            AppDB.get().reconcileDeselectedRoots(Collections.emptySet(),
                    Collections.singleton(nested));
            FileMeta removed = AppDB.get().load(path);
            assertEquals(Boolean.FALSE, removed.getIsSearchBook());
            assertEquals(Boolean.TRUE, removed.getIsStar());
            assertEquals(0.4f, removed.getIsRecentProgress(), 0.0001f);
        } finally {
            AppDB.get().deleteBy(path);
        }
    }
    @Test public void recordedLocalMembershipSurvivesInaccessibleSelectionUntilDeselected() {
        String root = "/scan-fixture-" + UUID.randomUUID();
        String path = root + "/book.epub";
        try {
            FileMeta row = new FileMeta(path);
            row.setTitle("Book");
            row.setIsSearchBook(true);
            AppDB.get().saveAll(Collections.singletonList(row));
            Map<String, Set<String>> membership = new HashMap<>();
            membership.put(root, Collections.singleton(path));
            AppDB.get().reconcileCompletedScan(Collections.singletonList(row),
                    Collections.singleton(root), membership);
            // Failure to traverse a still-selected root is not evidence of removal.
            AppDB.get().reconcileDeselectedRoots(Collections.singleton(root),
                    Collections.emptySet());
            assertEquals(Boolean.TRUE, AppDB.get().load(path).getIsSearchBook());
            AppDB.get().reconcileDeselectedRoots(Collections.emptySet(),
                    Collections.emptySet());
            assertEquals(Boolean.FALSE, AppDB.get().load(path).getIsSearchBook());
        } finally {
            AppDB.get().deleteBy(path);
        }
    }
}
