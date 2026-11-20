package org.ebookdroid.droids;

import com.foobnix.android.utils.LOG;
import com.foobnix.ext.FontSizes;
import com.foobnix.ext.FooterNote;
import com.foobnix.ext.LitExtract;

import java.io.File;

/**
 * Microsoft Reader (.lit) books, opened as the EPUB {@link LitExtract} converts them to - cached,
 * hyphenated and footnoted as MOBI books are.
 */
public class LitContext extends MobiContext {

    @Override
    protected FooterNote convert(String fileName, String outDir, String outName) throws Exception {
        final FooterNote result = LitExtract.extract(fileName, outDir, outName);
        // Their pages are mostly Word's HTML, every line fixed at 12.0pt, which foliate would draw
        // at that size whatever the reader's font size is
        try {
            if (result.path != null && new File(result.path).isFile()) {
                LOG.d("LitContext font sizes, body text px", FontSizes.normalizeEpub(new File(result.path)));
            }
        } catch (Exception e) {
            // The book still opens, at the sizes it was written with
            LOG.e(e, fileName);
        }
        return result;
    }
}
