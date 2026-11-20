package org.ebookdroid.droids;

import com.foobnix.android.utils.LOG;
import com.foobnix.ext.FooterNote;
import com.foobnix.ext.chm.ChmToEpub;

import java.io.File;
import java.io.IOException;

/**
 * Microsoft Compiled HTML Help (.chm) books, opened as the EPUB {@link ChmToEpub} converts them
 * to - cached, hyphenated and footnoted as MOBI books are, and read by foliate in the builds that
 * have no MuPDF.
 */
public class ChmContext extends MobiContext {

    @Override
    protected FooterNote convert(String fileName, String outDir, String outName) throws Exception {
        final File epub = new File(outDir, outName + ".epub");
        final long start = System.currentTimeMillis();
        try {
            final ChmToEpub.Result result = ChmToEpub.convert(new File(fileName), epub);
            LOG.d("ChmContext converted", fileName, "pages", result.pages, "resources",
                    result.resources, System.currentTimeMillis() - start, "ms");
        } catch (OutOfMemoryError e) {
            throw new IOException("CHM too large to convert: " + fileName, e);
        }
        return new FooterNote(epub.getPath(), null);
    }
}
