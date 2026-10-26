package org.ebookdroid.droids.pdfium;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.junit.Assume.assumeTrue;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.RectF;
import android.util.Log;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import com.foobnix.ext.CacheZipUtils;
import com.foobnix.model.AppProfile;
import com.foobnix.model.AppState;
import com.foobnix.pdf.info.AppsConfig;
import com.foobnix.pdf.info.Engines;
import com.foobnix.sys.TempHolder;

import org.ebookdroid.BookType;
import org.ebookdroid.common.bitmaps.BitmapRef;
import org.ebookdroid.core.codec.CodecContext;
import org.ebookdroid.core.codec.CodecDocument;
import org.ebookdroid.core.codec.CodecPage;
import org.ebookdroid.droids.mupdf.codec.exceptions.MuPdfPasswordRequiredException;
import org.ebookdroid.droids.mupdf.codec.exceptions.MuPdfWrongPasswordEnteredException;
import org.ebookdroid.droids.pdfium.codec.PdfiumContext;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;
import java.util.Locale;

@RunWith(AndroidJUnit4.class)
public class PdfiumPasswordDeviceTest {

    private static final String TAG = "PdfiumPasswordTest";
    private static final String PASSWORD = "1234";
    private static final int PAGE_WIDTH = 1080;
    private static final int PAGE_HEIGHT = 1500;

    private File file;
    private int engineBefore;

    @Before
    public void setUp() {
        final Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        AppsConfig.IS_LOG = true;
        CacheZipUtils.init(context);
        CacheZipUtils.createAllCacheDirs();
        AppProfile.init(context);
        TempHolder.get().loadingCancelled.set(false);
        engineBefore = AppState.get().pdfEngine;
        file = new File(new File(context.getExternalFilesDir(null), "pdf-test"), "HotEnglish65Password.pdf");
        assumeTrue("push the PDF into " + file, file.isFile());
        assumeTrue("PDFium is on this device", PdfiumContext.isAvailable());
    }

    @After
    public void tearDown() {
        AppState.get().pdfEngine = engineBefore;
    }

    @Test
    public void withoutAPasswordPdfiumAsksForOne() {
        try {
            new PdfiumContext().openDocumentInner(file.getPath(), "");
            fail("opened without a password");
        } catch (MuPdfPasswordRequiredException e) {
            Log.i(TAG, "no password: asks for one");
        }
    }

    @Test
    public void aWrongPasswordIsRejected() {
        try {
            new PdfiumContext().openDocumentInner(file.getPath(), "0000");
            fail("opened with a wrong password");
        } catch (MuPdfWrongPasswordEnteredException e) {
            Log.i(TAG, "wrong password: rejected");
        }
    }

    @Test
    public void thePasswordOpensItInPdfium() {
        final long t = System.nanoTime();
        final CodecDocument document = new PdfiumContext().openDocumentInner(file.getPath(), PASSWORD);
        assertNotNull("PDFium opened it with " + PASSWORD, document);
        readAndRender(document, "PdfiumContext", t);
    }

    @Test
    public void theReaderOpensItWithPdfiumAndThePassword() {
        assumeTrue("this build reads PDFs with PDFium", !Engines.IS_MUPDF || Engines.IS_ENGINE_CHOICE);
        AppState.get().pdfEngine = AppState.PDF_ENGINE_PDFIUM;
        final CodecContext context = BookType.getCodecContextByPath(file.getPath());
        assertTrue("the reader picks PDFium: " + context, context instanceof PdfiumContext);
        final long t = System.nanoTime();
        final CodecDocument document = context.openDocument(file.getPath(), PASSWORD);
        assertNotNull("the reader opened it with " + PASSWORD, document);
        readAndRender(document, "BookType", t);
    }

    private static void readAndRender(final CodecDocument document, final String how, final long since) {
        try {
            final int pages = document.getPageCount();
            final long openMs = (System.nanoTime() - since) / 1000000;
            assertTrue("it has pages", pages > 0);
            final long t = System.nanoTime();
            for (final int index : new int[]{0, pages / 2, pages - 1}) {
                final CodecPage page = document.getPage(index);
                assertNotNull(page);
                final BitmapRef ref = page.renderBitmap(PAGE_WIDTH, PAGE_HEIGHT, new RectF(0, 0, 1, 1), false);
                assertNotNull("page " + index + " rendered", ref);
                assertTrue("page " + index + " shows something", hasContent(ref.getBitmap()));
                page.recycle();
            }
            Log.i(TAG, String.format(Locale.US, "%s: %d pages, open %d ms, 3 pages rendered in %d ms",
                    how, pages, openMs, (System.nanoTime() - t) / 1000000));
        } finally {
            document.recycle();
        }
    }

    private static boolean hasContent(final Bitmap bitmap) {
        final int first = bitmap.getPixel(0, 0);
        for (int y = 0; y < 10; y++) {
            for (int x = 0; x < 10; x++) {
                if (bitmap.getPixel(bitmap.getWidth() * x / 10, bitmap.getHeight() * y / 10) != first) {
                    return true;
                }
            }
        }
        return false;
    }
}
