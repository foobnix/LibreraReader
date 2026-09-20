package com.foobnix.work;

import android.net.Uri;
import androidx.test.platform.app.InstrumentationRegistry;
import com.foobnix.dao2.FileMeta;
import com.foobnix.model.AppProfile;
import com.foobnix.pdf.info.SafOpfRegistry;
import com.foobnix.ui2.AppDB;
import com.foobnix.ui2.FileMetaCore;
import java.io.File;
import java.io.FileOutputStream;
import java.util.Collections;
import java.util.UUID;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

public class MetadataRefreshPolicyTest {
    @Before public void setUp() {
        AppProfile.init(InstrumentationRegistry.getInstrumentation().getTargetContext());
    }

    @Test public void unchangedLocalBookSkipsButSourceAndSidecarRevisionsDoNot() throws Exception {
        File folder = new File(InstrumentationRegistry.getInstrumentation().getTargetContext()
                .getCacheDir(), "incremental-" + UUID.randomUUID());
        assertTrue(folder.mkdir());
        File book = new File(folder, "book.epub");
        File opf = new File(folder, "metadata.opf");
        try {
            try (FileOutputStream output = new FileOutputStream(book)) { output.write(new byte[]{1, 2, 3}); }
            FileMeta found = new FileMeta(book.getPath());
            FileMeta before = new FileMeta(book.getPath());
            before.setState(FileMetaCore.STATE_FULL);
            before.setSize(book.length());
            before.setDate(book.lastModified());
            String original = MetadataRefreshPolicy.revision(found, null, "settings-a");
            assertFalse(MetadataRefreshPolicy.needsExtraction(before, original, original));
            assertTrue(MetadataRefreshPolicy.needsExtraction(before, original,
                    MetadataRefreshPolicy.revision(found, null, "settings-b")));
            try (FileOutputStream output = new FileOutputStream(opf)) { output.write(1); }
            String withOpf = MetadataRefreshPolicy.revision(found, null, "settings-a");
            assertTrue(MetadataRefreshPolicy.needsExtraction(before, original, withOpf));
            assertTrue(MetadataRefreshPolicy.needsExtraction(before, withOpf, original));
            assertTrue(opf.delete());
            File namedOpf = new File(folder, "book.opf");
            try (FileOutputStream output = new FileOutputStream(namedOpf)) { output.write(1); }
            assertTrue(MetadataRefreshPolicy.needsExtraction(before, original,
                    MetadataRefreshPolicy.revision(found, null, "settings-a")));
            assertTrue(namedOpf.delete());
            try (FileOutputStream output = new FileOutputStream(book, true)) { output.write(4); }
            assertTrue(MetadataRefreshPolicy.needsExtraction(before, original,
                    MetadataRefreshPolicy.revision(found, null, "settings-a")));
        } finally {
            opf.delete(); book.delete(); folder.delete();
        }
    }

    @Test public void safRevisionAndSidecarControlExtraction() {
        String path = "content://scan-incremental/document/" + UUID.randomUUID();
        FileMeta before = new FileMeta(path);
        before.setState(FileMetaCore.STATE_FULL);
        before.setSize(100L);
        before.setDate(123L);
        FileMeta found = new FileMeta(path);
        found.setSize(100L);
        found.setDate(123L);
        String original = MetadataRefreshPolicy.revision(found, null, "settings");
        assertFalse(MetadataRefreshPolicy.needsExtraction(before, original, original));
        SafOpfRegistry.Entry sidecar = new SafOpfRegistry.Entry(Uri.parse(path + "/metadata.opf"),
                Collections.emptyMap(), "sidecars:known-revision");
        String withOpf = MetadataRefreshPolicy.revision(found, sidecar, "settings");
        assertTrue(MetadataRefreshPolicy.needsExtraction(before, original, withOpf));
        assertFalse(MetadataRefreshPolicy.needsExtraction(before, withOpf, withOpf));
        SafOpfRegistry.Entry revised = new SafOpfRegistry.Entry(sidecar.opfUri,
                Collections.emptyMap(), "sidecars:next-revision");
        assertTrue(MetadataRefreshPolicy.needsExtraction(before, withOpf,
                MetadataRefreshPolicy.revision(found, revised, "settings")));
        assertTrue(MetadataRefreshPolicy.needsExtraction(before, withOpf, original));
        SafOpfRegistry.Entry unknown = new SafOpfRegistry.Entry(sidecar.opfUri,
                Collections.emptyMap(), "sidecars:unknown:revision");
        assertNull(MetadataRefreshPolicy.revision(found, unknown, "settings"));
        found.setDate(null);
        assertNull(MetadataRefreshPolicy.revision(found, null, "settings"));
        found.setDate(124L);
        assertTrue(MetadataRefreshPolicy.needsExtraction(before, original,
                MetadataRefreshPolicy.revision(found, null, "settings")));
    }

    @Test public void completedSafDiscoveryPreservesUserStateAndRevisionTrackingControlsRefresh() {
        String root = "content://scan-incremental/tree/" + UUID.randomUUID();
        String path = root + "/document/book";
        try {
            FileMeta before = new FileMeta(path);
            before.setState(FileMetaCore.STATE_FULL);
            before.setSize(100L);
            before.setDate(123L);
            before.setIsRecentProgress(0.75f);
            AppDB.get().saveAll(Collections.singletonList(before));
            FileMeta found = new FileMeta(path);
            found.setIsSearchBook(true);
            found.setSize(100L);
            found.setDate(123L);
            AppDB.get().reconcileCompletedScan(Collections.singletonList(found),
                    Collections.singleton(root),
                    Collections.singletonMap(root, Collections.singleton(path)));
            FileMeta stable = AppDB.get().scanSnapshot().stream()
                    .filter(row -> path.equals(row.getPath())).findFirst().get();
            assertEquals(FileMetaCore.STATE_FULL, stable.getState().intValue());
            assertEquals(0.75f, stable.getIsRecentProgress(), 0.0001f);

            found.setDate(124L);
            AppDB.get().reconcileCompletedScan(Collections.singletonList(found),
                    Collections.singleton(root),
                    Collections.singletonMap(root, Collections.singleton(path)));
            FileMeta changed = AppDB.get().scanSnapshot().stream()
                    .filter(row -> path.equals(row.getPath())).findFirst().get();
            assertEquals(FileMetaCore.STATE_FULL, changed.getState().intValue());
            FileMeta previouslyDiscovered = new FileMeta(path);
            previouslyDiscovered.setSize(100L);
            previouslyDiscovered.setDate(123L);
            String oldRevision = MetadataRefreshPolicy.revision(previouslyDiscovered, null, "settings");
            assertTrue(MetadataRefreshPolicy.needsExtraction(changed, oldRevision,
                    MetadataRefreshPolicy.revision(found, null, "settings")));
            assertEquals(0.75f, changed.getIsRecentProgress(), 0.0001f);
        } finally {
            AppDB.get().deleteBy(path);
        }
    }
}
