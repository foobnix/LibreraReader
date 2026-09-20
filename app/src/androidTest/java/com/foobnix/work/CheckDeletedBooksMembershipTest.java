package com.foobnix.work;

import androidx.test.platform.app.InstrumentationRegistry;
import com.foobnix.dao2.FileMeta;
import com.foobnix.model.AppProfile;
import com.foobnix.model.SimpleMeta;
import com.foobnix.pdf.info.ExtUtils;
import com.foobnix.ui2.AppDB;
import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.Test;
import static org.junit.Assert.*;

public class CheckDeletedBooksMembershipTest {
    @Test public void backgroundLocalDiscoveryHonorsExclusionsAndSyncedDuplicates() throws Exception {
        AppProfile.init(InstrumentationRegistry.getInstrumentation().getTargetContext());
        File root = new File(InstrumentationRegistry.getInstrumentation().getTargetContext()
                .getCacheDir(), "membership-" + System.nanoTime());
        assertTrue(root.mkdirs());
        File excluded = new File(root, "excluded.epub");
        File duplicate = new File(root, "duplicate.epub");
        File fresh = new File(root, "fresh.epub");
        List<FileMeta> found = new ArrayList<>();
        try {
            assertTrue(excluded.createNewFile());
            assertTrue(duplicate.createNewFile());
            assertTrue(fresh.createNewFile());
            for (File file : Arrays.asList(excluded, duplicate, fresh)) {
                try (FileOutputStream out = new FileOutputStream(file)) { out.write(1); }
            }
            LocalDiscovery.collect(root, ExtUtils.seachExts, found, () -> false);
            assertEquals(3, found.size());
            for (FileMeta row : found) {
                if (row.getPath().equals(duplicate.getPath())) row.setTitle("Synced title");
            }
            FileMeta synced = new FileMeta("/other/synced.epub");
            synced.setTitle("Synced title");
            Map<String, Set<String>> membership = new HashMap<>();
            membership.put(root.getPath(), new HashSet<>(Arrays.asList(
                    excluded.getPath(), duplicate.getPath(), fresh.getPath())));
            long owner = ScanOwnership.claim();
            assertTrue(CheckDeletedBooksWorker.reconcileFound(owner, () -> false, found,
                    Collections.singleton(root.getPath()), membership,
                    Collections.singletonList(SimpleMeta.SyncSimpleMeta(excluded.getPath())),
                    Collections.singletonList(synced)));
            assertEquals(Boolean.FALSE, AppDB.get().load(excluded.getPath()).getIsSearchBook());
            assertEquals(Boolean.FALSE, AppDB.get().load(duplicate.getPath()).getIsSearchBook());
            assertEquals(Boolean.TRUE, AppDB.get().load(fresh.getPath()).getIsSearchBook());
        } finally {
            for (FileMeta row : found) AppDB.get().deleteBy(row.getPath());
            excluded.delete();
            duplicate.delete();
            fresh.delete();
            root.delete();
        }
    }
}
