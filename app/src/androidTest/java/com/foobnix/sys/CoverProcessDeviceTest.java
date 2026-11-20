package com.foobnix.sys;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.os.Process;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import com.foobnix.ext.CacheZipUtils;
import com.foobnix.model.AppProfile;
import com.foobnix.model.AppState;
import com.foobnix.pdf.info.IMG;
import com.foobnix.pdf.info.PageUrl;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

/**
 * PDF covers are drawn in the ":covers" process, and one that takes too long has that process
 * killed under it: the book is then remembered and not tried again, and the next cover is drawn
 * by a new process. A PDF whose first page took minutes held the engine lock all that time, so no
 * other cover was drawn and no book opened.
 */
@RunWith(AndroidJUnit4.class)
public class CoverProcessDeviceTest {

    private Context context;
    private File pdf;
    private long limitBefore;

    @Before
    public void setUp() throws Exception {
        context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        CacheZipUtils.init(context);
        CacheZipUtils.createAllCacheDirs();
        AppProfile.init(context);
        TempHolder.get().loadingCancelled.set(false);
        limitBefore = CoverProcess.timeLimitMs;
        CoverProcess.forgetFailed(context);

        pdf = new File(context.getCacheDir(), "cover-process-test.pdf");
        final OutputStream out = new FileOutputStream(pdf);
        try {
            out.write(onePagePdf());
        } finally {
            out.close();
        }
    }

    @After
    public void tearDown() {
        CoverProcess.timeLimitMs = limitBefore;
        CoverProcess.forgetFailed(context);
        pdf.delete();
    }

    private PageUrl cover() {
        return PageUrl.fromString(IMG.toUrl(pdf.getPath(), ImageExtractor.COVER_PAGE, 200, false));
    }

    @Test
    public void bothEnginesDrawTheCoverInTheCoverProcess() {
        for (int engine : new int[]{AppState.PDF_ENGINE_MUPDF, AppState.PDF_ENGINE_PDFIUM}) {
            final int before = AppState.get().pdfEngine;
            AppState.get().pdfEngine = engine;
            try {
                final CoverProcess.Result result = CoverProcess.render(context, cover());
                assertFalse("engine " + engine + " drew it", result.failed);
                assertNotNull("engine " + engine + " drew a page", result.bitmap);
                assertEquals(200, result.bitmap.getWidth());
                assertNotEquals("drawn in another process", Process.myPid(), CoverProcess.pid);
            } finally {
                AppState.get().pdfEngine = before;
            }
        }
    }

    @Test
    public void aLateCoverKillsTheProcessAndIsNotTriedAgain() throws Exception {
        assertNotNull("the process is up", CoverProcess.render(context, cover()).bitmap);
        final int first = CoverProcess.pid;

        // No page is drawn in a millisecond, over a process boundary
        CoverProcess.timeLimitMs = 1;
        final CoverProcess.Result late = CoverProcess.render(context, cover());
        assertTrue("the late cover failed", late.failed);
        assertNull(late.bitmap);
        for (int i = 0; i < 50 && new File("/proc/" + first).exists(); i++) {
            Thread.sleep(100);
        }
        assertFalse("the process was killed", new File("/proc/" + first).exists());

        // Remembered: given up on at once, even with time to spare
        CoverProcess.timeLimitMs = limitBefore;
        final long start = System.currentTimeMillis();
        assertTrue("not tried again", CoverProcess.render(context, cover()).failed);
        assertTrue("given up on at once", System.currentTimeMillis() - start < 500);

        // A book that is not remembered is drawn by a new process
        CoverProcess.forgetFailed(context);
        final CoverProcess.Result again = CoverProcess.render(context, cover());
        assertFalse(again.failed);
        assertNotNull("a new process drew it", again.bitmap);
        assertNotEquals("in a new process", first, CoverProcess.pid);
    }

    /**
     * A real book whose cover never finishes, given as <code>-e slowPdf /sdcard/...pdf</code>
     * (skipped without it): the cover is given up on at the time limit, and meanwhile the engine
     * lock of the app's own process is free, so books open and other covers are drawn.
     */
    @Test
    public void aSlowBookIsGivenUpOnWithoutHoldingTheEngine() throws Exception {
        final String slow = InstrumentationRegistry.getArguments().getString("slowPdf");
        org.junit.Assume.assumeTrue("no -e slowPdf given", slow != null);
        CoverProcess.timeLimitMs = 5000;
        final PageUrl url = PageUrl.fromString(IMG.toUrl(slow, ImageExtractor.COVER_PAGE, 336, false));
        final CoverProcess.Result[] result = new CoverProcess.Result[1];
        final Thread cover = new Thread(() -> result[0] = CoverProcess.render(context, url));
        final long start = System.currentTimeMillis();
        cover.start();
        Thread.sleep(2000);
        assertTrue("the engine lock is free while the cover is drawn", TempHolder.lock.tryLock());
        TempHolder.lock.unlock();
        cover.join(30000);
        final long took = System.currentTimeMillis() - start;
        assertNotNull("the cover came back", result[0]);
        assertTrue("given up on", result[0].failed);
        assertTrue("at the time limit: " + took, took < 5000 + 3000);
    }

    private static byte[] onePagePdf() {
        final String content = "BT /F1 24 Tf 20 100 Td (Cover) Tj ET\n";
        final String[] objects = {
                "<< /Type /Catalog /Pages 2 0 R >>",
                "<< /Type /Pages /Kids [3 0 R] /Count 1 >>",
                "<< /Type /Page /Parent 2 0 R /MediaBox [0 0 200 300]"
                        + " /Resources << /Font << /F1 5 0 R >> >> /Contents 4 0 R >>",
                "<< /Length " + content.length() + " >>\nstream\n" + content + "endstream",
                "<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica >>"};
        final StringBuilder out = new StringBuilder("%PDF-1.4\n");
        final int[] offsets = new int[objects.length];
        for (int i = 0; i < objects.length; i++) {
            offsets[i] = out.length();
            out.append(i + 1).append(" 0 obj\n").append(objects[i]).append("\nendobj\n");
        }
        final int xref = out.length();
        out.append("xref\n0 ").append(objects.length + 1).append("\n0000000000 65535 f \n");
        for (int offset : offsets) {
            out.append(String.format("%010d 00000 n \n", offset));
        }
        out.append("trailer\n<< /Size ").append(objects.length + 1).append(" /Root 1 0 R >>\n");
        out.append("startxref\n").append(xref).append("\n%%EOF\n");
        return out.toString().getBytes(StandardCharsets.ISO_8859_1);
    }
}
