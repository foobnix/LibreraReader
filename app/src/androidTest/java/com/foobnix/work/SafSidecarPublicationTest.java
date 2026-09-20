package com.foobnix.work;

import android.content.Context;
import android.net.Uri;
import androidx.test.platform.app.InstrumentationRegistry;
import com.foobnix.dao2.FileMeta;
import com.foobnix.model.AppProfile;
import com.foobnix.pdf.info.SafOpfRegistry;
import com.foobnix.pdf.info.SafStateStore;
import com.foobnix.ui2.AppDB;
import org.junit.Test;
import java.util.*;
import static org.junit.Assert.*;

public class SafSidecarPublicationTest {
    @Test public void pickerBooksCacheCoversButUnknownScanMembersRemainUnknown() {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        AppProfile.init(context);
        String picker = "content://picker-cover/document/" + UUID.randomUUID();
        String scanned = "content://scanned-cover/document/" + UUID.randomUUID();
        try {
            assertEquals("", AppDB.get().getOrCreate(picker).getSafSidecarRevision());
            FileMeta row = new FileMeta(scanned); row.setIsSearchBook(true);
            AppDB.get().reconcileCompletedScan(Collections.singletonList(row), Collections.singleton("unknown-root"),
                    Collections.singletonMap("unknown-root", Collections.singleton(scanned)));
            assertNull(AppDB.get().getOrCreate(scanned).getSafSidecarRevision());
            SearchAllBooksWorker.updateCompletedSidecars(context, Collections.singletonList(row), Collections.emptyMap());
            AppDB.get().reconcileCompletedScan(Collections.singletonList(row), Collections.singleton("unknown-root"),
                    Collections.singletonMap("unknown-root", Collections.singleton(scanned)));
            assertEquals("", AppDB.get().load(scanned).getSafSidecarRevision());
        } finally { AppDB.get().deleteBy(picker); AppDB.get().deleteBy(scanned); }
    }

    @Test public void unchangedSidecarsAndAbsentEntriesDoNotRewriteState() throws Exception {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        AppProfile.init(context); SafOpfRegistry.restore(context);
        String path = "content://sidecar-batch/document/" + UUID.randomUUID();
        String absent = "content://sidecar-batch/document/" + UUID.randomUUID();
        SafOpfRegistry.Entry entry = new SafOpfRegistry.Entry(Uri.parse(path + "/metadata.opf"), Collections.emptyMap(), "one");
        Map<String, SafOpfRegistry.Entry> batch = new HashMap<>(); batch.put(path, entry); batch.put(absent, null);
        String name = "saf-state-" + Uri.encode(AppProfile.getCurrent() == null ? "default" : AppProfile.getCurrent()) + ".db";
        try {
            SafOpfRegistry.updateAll(batch);
            String original = SafStateStore.get(context, "SafSidecars").getString(path, null);
            assertNotNull(original);
            try (android.database.sqlite.SQLiteDatabase db = android.database.sqlite.SQLiteDatabase.openDatabase(
                    context.getDatabasePath(name).getPath(), null, android.database.sqlite.SQLiteDatabase.OPEN_READONLY)) {
                long before = android.database.DatabaseUtils.longForQuery(db, "SELECT UPDATED FROM STATE WHERE NAMESPACE='SafSidecars' AND KEY=?", new String[]{path});
                android.os.SystemClock.sleep(20);
                SafOpfRegistry.updateAll(batch);
                long after = android.database.DatabaseUtils.longForQuery(db, "SELECT UPDATED FROM STATE WHERE NAMESPACE='SafSidecars' AND KEY=?", new String[]{path});
                assertEquals(before, after);
            }
            assertFalse(SafStateStore.get(context, "SafSidecars").contains(absent));
            assertEquals("one", SafOpfRegistry.get(path).revision);
            batch.put(path, new SafOpfRegistry.Entry(entry.opfUri, Collections.emptyMap(), "two"));
            SafOpfRegistry.updateAll(batch);
            assertEquals("two", SafOpfRegistry.get(path).revision);
            // Store pruning can remove an entry still present in the registry's small LRU.
            SafStateStore.get(context, "SafSidecars").edit().remove(path).commit();
            SafOpfRegistry.unregister(path);
            assertNull(SafOpfRegistry.get(path));
        } finally { SafOpfRegistry.unregister(path); SafOpfRegistry.unregister(absent); }
    }
}
