package com.foobnix.pdf.info;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import androidx.test.platform.app.InstrumentationRegistry;
import java.io.File;
import java.io.FileOutputStream;
import java.util.UUID;
import org.junit.Test;

public class ExternalHandoffRetentionTest {
    @Test public void copiedBookSurvivesCleanupUntilOldAndUnleased() throws Exception {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        File source = new File(context.getCacheDir(), "handoff-source-" + UUID.randomUUID() + ".epub");
        File output = null;
        AutoCloseable lease = null;
        try {
            try (FileOutputStream stream = new FileOutputStream(source)) { stream.write(42); }
            output = ExtUtils.durableHandoffCopy(context, source);
            long now = System.currentTimeMillis();
            assertTrue(output.isFile());
            ExtUtils.pruneDurableHandoffs(context, now);
            assertTrue(output.isFile());

            assertTrue(output.setLastModified(now - ExtUtils.HANDOFF_RETENTION_MS - 1000));
            lease = BookCacheLeases.acquire(output);
            ExtUtils.pruneDurableHandoffs(context, now);
            assertTrue(output.isFile());
            lease.close();
            lease = null;
            ExtUtils.pruneDurableHandoffs(context, now);
            assertFalse(output.exists());
        } finally {
            if (lease != null) lease.close();
            source.delete();
            if (output != null) output.delete();
        }
    }
}
