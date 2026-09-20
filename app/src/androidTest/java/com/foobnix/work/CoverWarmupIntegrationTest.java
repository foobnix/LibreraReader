package com.foobnix.work;

import static org.junit.Assert.assertTrue;

import android.content.Context;
import androidx.test.platform.app.InstrumentationRegistry;
import com.foobnix.dao2.FileMeta;
import com.foobnix.ext.CacheZipUtils;
import com.foobnix.model.AppProfile;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.UUID;
import org.junit.Test;

public class CoverWarmupIntegrationTest {
    @Test public void warmsARealDisposableEpubThroughNormalGlideRequest() throws Exception {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        AppProfile.init(context);
        CacheZipUtils.init(context);
        File source = new File(context.getCacheDir(), "warmup-" + UUID.randomUUID() + ".epub");
        try {
            try (InputStream input = InstrumentationRegistry.getInstrumentation().getContext()
                    .getAssets().open("reader-fixtures/book.epub");
                 FileOutputStream output = new FileOutputStream(source)) {
                byte[] buffer = new byte[8192];
                int count;
                while ((count = input.read(buffer)) != -1) output.write(buffer, 0, count);
            }
            FileMeta book = new FileMeta(source.getPath());
            book.setSize(source.length());
            book.setDate(source.lastModified());
            assertTrue(CoverWarmupWorker.warmBook(context, book, () -> false));
        } finally {
            source.delete();
        }
    }
}
