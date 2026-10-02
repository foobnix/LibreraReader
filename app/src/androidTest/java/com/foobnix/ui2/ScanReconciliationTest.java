package com.foobnix.ui2;

import androidx.test.platform.app.InstrumentationRegistry;
import com.foobnix.dao2.FileMeta;
import com.foobnix.model.AppProfile;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
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

    @Test public void opaqueSafDocumentIdsUseCompletedRootMembershipForRemovals() {
        String root = "content://scan-fixture/tree/" + UUID.randomUUID();
        String path = "content://scan-fixture/document/opaque-" + UUID.randomUUID();
        try {
            FileMeta book = new FileMeta(path);
            book.setTitle("Book");
            book.setIsSearchBook(true);
            book.setIsStar(true);
            book.setIsRecent(true);
            AppDB.get().saveAll(Collections.singletonList(book));
            Map<String, Set<String>> membership = new HashMap<>();
            membership.put(root, new HashSet<>(Collections.singleton(path)));
            AppDB.get().reconcileCompletedScan(Collections.singletonList(book),
                    Collections.singleton(root), membership);
            assertEquals(Boolean.TRUE, AppDB.get().load(path).getIsSearchBook());

            // Only a second complete listing can confirm absence from this root.
            membership.put(root, Collections.emptySet());
            AppDB.get().reconcileCompletedScan(Collections.emptyList(),
                    Collections.singleton(root), membership);
            FileMeta retained = AppDB.get().load(path);
            assertEquals(Boolean.FALSE, retained.getIsSearchBook());
            assertEquals(Boolean.TRUE, retained.getIsStar());
            assertEquals(Boolean.TRUE, retained.getIsRecent());
        } finally {
            AppDB.get().deleteBy(path);
        }
    }

    @Test public void deselectedSafRootClearsOnlyExclusiveMembership() {
        String first = "content://scan-fixture/tree/" + UUID.randomUUID();
        String second = "content://scan-fixture/tree/" + UUID.randomUUID();
        String shared = "content://scan-fixture/document/" + UUID.randomUUID();
        String exclusive = "content://scan-fixture/document/" + UUID.randomUUID();
        try {
            for (String path : new String[]{shared, exclusive}) {
                FileMeta row = new FileMeta(path);
                row.setTitle("Book");
                row.setIsSearchBook(true);
                row.setIsStar(true);
                row.setIsRecent(true);
                row.setIsRecentProgress(0.7f);
                AppDB.get().saveAll(Collections.singletonList(row));
            }
            Map<String, Set<String>> membership = new HashMap<>();
            membership.put(first, new HashSet<>(java.util.Arrays.asList(shared, exclusive)));
            membership.put(second, Collections.singleton(shared));
            AppDB.get().reconcileCompletedScan(java.util.Arrays.asList(
                    AppDB.get().load(shared), AppDB.get().load(exclusive)),
                    new HashSet<>(membership.keySet()), membership);

            AppDB.get().reconcileDeselectedRoots(Collections.singleton(second),
                    Collections.singleton(first));
            assertEquals(Boolean.TRUE, AppDB.get().load(shared).getIsSearchBook());
            FileMeta removed = AppDB.get().load(exclusive);
            assertEquals(Boolean.FALSE, removed.getIsSearchBook());
            assertEquals(Boolean.TRUE, removed.getIsStar());
            assertEquals(Boolean.TRUE, removed.getIsRecent());
            assertEquals(0.7f, removed.getIsRecentProgress(), 0.0001f);
            // A stale membership row must not keep the shared book after its final root goes.
            AppDB.get().reconcileDeselectedRoots(Collections.emptySet(),
                    Collections.singleton(second));
            assertEquals(Boolean.FALSE, AppDB.get().load(shared).getIsSearchBook());
        } finally {
            AppDB.get().deleteBy(shared);
            AppDB.get().deleteBy(exclusive);
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

    @Test public void durablePartialDiscoveryPreservesUserEditsAndNeedsCompleteRemovalProof() {
        String root = "content://scan-fixture/tree/" + UUID.randomUUID();
        String path = "content://scan-fixture/document/" + UUID.randomUUID();
        try {
            FileMeta found = new FileMeta(path);
            found.setTitle("Book");
            found.setIsSearchBook(true);
            AppDB.get().publishDiscoveredBooks(root, Collections.singletonList(found));
            FileMeta reader = AppDB.get().load(path);
            reader.setIsStar(true);
            reader.setIsRecent(true);
            AppDB.get().save(reader);
            AppDB.get().updateReadingProgress(path, 0.6f);

            // A failed scan does not call completed reconciliation. Repeated discovery
            // must leave progress, favorites, and Recents with their current owners.
            AppDB.get().publishDiscoveredBooks(root, Collections.singletonList(found));
            FileMeta durable = AppDB.get().load(path);
            assertEquals(Boolean.TRUE, durable.getIsSearchBook());
            assertEquals(Boolean.TRUE, durable.getIsStar());
            assertEquals(Boolean.TRUE, durable.getIsRecent());
            assertEquals(0.6f, durable.getIsRecentProgress(), 0.0001f);

            Map<String, Set<String>> absent = new HashMap<>();
            absent.put(root, Collections.emptySet());
            AppDB.get().reconcileCompletedScan(Collections.emptyList(),
                    Collections.singleton(root), absent);
            FileMeta removed = AppDB.get().load(path);
            assertEquals(Boolean.FALSE, removed.getIsSearchBook());
            assertEquals(Boolean.TRUE, removed.getIsStar());
            assertEquals(0.6f, removed.getIsRecentProgress(), 0.0001f);
        } finally {
            AppDB.get().deleteBy(path);
        }
    }
}
