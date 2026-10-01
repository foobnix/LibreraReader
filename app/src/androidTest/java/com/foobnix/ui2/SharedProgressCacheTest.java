package com.foobnix.ui2;

import androidx.test.platform.app.InstrumentationRegistry;
import com.foobnix.model.AppBook;
import com.foobnix.model.AppProfile;
import org.ebookdroid.common.settings.books.SharedBooks;
import org.junit.Test;
import java.io.File;
import java.util.UUID;
import static org.junit.Assert.*;

public class SharedProgressCacheTest {
    @Test public void savingSafProgressReplacesTheEntryUsedByLibraryLoads() throws Exception {
        String path = "content://progress-fixture/document/" + UUID.randomUUID();
        File original = AppProfile.syncProgress;
        File fixture = File.createTempFile("progress-fixture-", ".json",
                InstrumentationRegistry.getInstrumentation().getTargetContext().getCacheDir());
        try {
            AppProfile.syncProgress = fixture;
            AppBook stale = new AppBook(path); stale.p = 0.001f;
            SharedBooks.cache.put(path, stale);
            AppBook updated = new AppBook(path); updated.p = 0.796f; updated.t = 1234;
            SharedBooks.save(updated);
            assertSame(updated, SharedBooks.load(path));
            assertEquals(0.796f, SharedBooks.load(path).p, 0.0001f);
        } finally {
            AppProfile.syncProgress = original;
            SharedBooks.cache.remove(path);
            fixture.delete();
        }
    }
}
