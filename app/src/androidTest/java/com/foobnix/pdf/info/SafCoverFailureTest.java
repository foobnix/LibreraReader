package com.foobnix.pdf.info;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.net.Uri;
import android.util.Base64;
import androidx.core.content.FileProvider;
import androidx.test.platform.app.InstrumentationRegistry;
import com.bumptech.glide.Glide;
import com.bumptech.glide.request.FutureTarget;
import com.foobnix.dao2.FileMeta;
import com.foobnix.model.AppProfile;
import com.foobnix.ui2.AppDB;
import com.foobnix.ui2.FileMetaCore;
import org.junit.Test;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import static org.junit.Assert.*;

public class SafCoverFailureTest {
    @Test public void failedProviderOpenIsRetriedUnderTheSameCoverSignature() throws Exception {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        AppProfile.init(context);
        File source = File.createTempFile("saf-cover-retry-", ".fb2", context.getCacheDir());
        Uri uri = FileProvider.getUriForFile(context, context.getPackageName() + ".provider", source);
        FileMeta book = new FileMeta(uri.toString());
        book.setTitle("Retry cover");
        book.setSize(100L);
        book.setDate(1000L);
        book.setState(FileMetaCore.STATE_FULL);
        AppDB.get().save(book);
        try {
            assertTrue(source.delete());
            assertThrows(ExecutionException.class, () -> request(context, book));
            writeFb2WithRedCover(source);
            assertEquals(Color.RED, request(context, book));
        } finally {
            AppDB.get().deleteBy(book.getPath());
            source.delete();
        }
    }

    private static int request(Context context, FileMeta book) throws Exception {
        FutureTarget<Bitmap> target = IMG.getCoverPageWithEffect(context, book, null)
                .skipMemoryCache(true).submit();
        try {
            Bitmap bitmap = target.get(10, TimeUnit.SECONDS);
            return bitmap.getPixel(bitmap.getWidth() / 2, bitmap.getHeight() / 2);
        }
        finally { Glide.with(context).clear(target); }
    }

    private static void writeFb2WithRedCover(File file) throws Exception {
        Bitmap image = Bitmap.createBitmap(24, 36, Bitmap.Config.ARGB_8888);
        image.eraseColor(Color.RED);
        ByteArrayOutputStream png = new ByteArrayOutputStream();
        try { assertTrue(image.compress(Bitmap.CompressFormat.PNG, 100, png)); }
        finally { image.recycle(); }
        String book = "<?xml version='1.0' encoding='UTF-8'?>"
                + "<FictionBook xmlns='http://www.gribuser.ru/xml/fictionbook/2.0' "
                + "xmlns:l='http://www.w3.org/1999/xlink'><description><title-info>"
                + "<book-title>Retry cover</book-title><coverpage><image l:href='#cover.png'/>"
                + "</coverpage></title-info></description><body><section><p>Book</p></section></body>"
                + "<binary id='cover.png' content-type='image/png'>"
                + Base64.encodeToString(png.toByteArray(), Base64.NO_WRAP)
                + "</binary></FictionBook>";
        try (FileOutputStream output = new FileOutputStream(file)) {
            output.write(book.getBytes(StandardCharsets.UTF_8));
        }
    }
}
