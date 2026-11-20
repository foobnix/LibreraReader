package org.ebookdroid.droids.foliate;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.RectF;

import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import com.foobnix.ext.CacheZipUtils;
import com.foobnix.ext.HtmlToEpub;
import com.foobnix.model.AppProfile;
import com.foobnix.model.AppState;
import com.foobnix.pdf.info.AppsConfig;
import com.foobnix.pdf.info.model.BookCSS;
import com.foobnix.sys.TempHolder;

import org.ebookdroid.BookType;
import org.ebookdroid.common.bitmaps.BitmapRef;
import org.ebookdroid.core.codec.CodecContext;
import org.ebookdroid.core.codec.CodecDocument;
import org.ebookdroid.droids.foliate.codec.FoliateDocument;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;

/**
 * A heading that wraps keeps a line height of its own size in foliate.
 * <p>
 * The reading settings give the text {@code line-height:1.5em}. A browser works that em out at
 * the body's font size and headings inherit the length, so the lines of a heading twice the
 * text's size were 1.5 text lines apart and ran into each other; foliate is given the factor
 * instead (see {@code FoliateCss}). The heading here is capitals only - no descender reaches into
 * the next line - so its two lines must come out as two bands of ink with paper between them.
 */
@RunWith(AndroidJUnit4.class)
public class FoliateHeadingLineHeightDeviceTest {

    private static final int PAGE_WIDTH = 1080;

    private static final int PAGE_HEIGHT = 1800;

    private ActivityScenario<FoliateHostActivity> host;

    private File dir;

    private int engineBefore;

    private int lineHeightBefore;

    @Before
    public void setUp() {
        final Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        AppsConfig.IS_LOG = true;
        CacheZipUtils.init(context);
        CacheZipUtils.createAllCacheDirs();
        AppProfile.init(context);
        TempHolder.get().loadingCancelled.set(false);

        host = ActivityScenario.launch(FoliateHostActivity.class);
        InstrumentationRegistry.getInstrumentation().waitForIdleSync();

        engineBefore = AppState.get().epubEngine;
        AppState.get().epubEngine = AppState.EPUB_ENGINE_FOLIATE;
        lineHeightBefore = BookCSS.get().lineHeight12Foliate;
        // foliate's default line spacing: 1.5em
        BookCSS.get().lineHeight12Foliate = BookCSS.FOLIATE_LINE_HEIGHT;

        dir = new File(context.getCacheDir(), "heading-line-height-test");
        dir.mkdirs();
    }

    @After
    public void tearDown() {
        AppState.get().epubEngine = engineBefore;
        BookCSS.get().lineHeight12Foliate = lineHeightBefore;
        final File[] files = dir.listFiles();
        for (int i = 0; files != null && i < files.length; i++) {
            files[i].delete();
        }
        dir.delete();
        if (host != null) {
            host.close();
        }
    }

    @Test
    public void aWrappedHeadingsLinesDoNotTouch() throws Exception {
        final File html = new File(dir, "heading.html");
        final FileOutputStream out = new FileOutputStream(html);
        try {
            out.write(("<html><head><title>Heading</title></head><body>"
                    + "<h1 style=\"font-size:3em\">HEADING ONE HEADING TWO HEADING THREE HEADING FOUR</h1>"
                    + "</body></html>").getBytes(StandardCharsets.UTF_8));
        } finally {
            out.close();
        }

        // foliate is handed archives: in a build with MuPDF a loose page would go to MuPDF
        final String epub = HtmlToEpub.wrap(html.getPath());
        assertNotNull("the page was wrapped", epub);
        final CodecContext context = BookType.getCodecContextByPath(epub);
        assertNotNull(context);
        final CodecDocument document = context.openDocument(epub, "");
        assertNotNull("the page was opened", document);
        try {
            assertTrue("foliate laid it out: " + document, document instanceof FoliateDocument);
            document.getPageCount(PAGE_WIDTH, PAGE_HEIGHT, BookCSS.get().fontSizeSp);
            final BitmapRef ref = document.getPage(0).renderBitmap(PAGE_WIDTH, PAGE_HEIGHT, new RectF(0, 0, 1, 1), false);
            assertNotNull(ref);
            final Bitmap bitmap = ref.getBitmap();

            // Bands of rows with ink in them, top to bottom
            int bands = 0;
            boolean inInk = false;
            for (int y = 0; y < bitmap.getHeight(); y++) {
                boolean ink = false;
                for (int x = 0; x < bitmap.getWidth() && !ink; x += 2) {
                    ink = Color.red(bitmap.getPixel(x, y)) < 100;
                }
                if (ink && !inInk) {
                    bands++;
                }
                inInk = ink;
            }
            assertTrue("the heading wraps into lines with space between them, bands of ink: " + bands, bands >= 2);
        } finally {
            document.recycle();
        }
    }
}
