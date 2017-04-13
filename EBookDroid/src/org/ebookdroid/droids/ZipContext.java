package org.ebookdroid.droids;

import org.ebookdroid.BookType;
import org.ebookdroid.core.codec.CodecDocument;
import org.ebookdroid.droids.mupdf.codec.PdfContext;

import com.foobnix.android.utils.LOG;
import com.foobnix.ext.CacheZipUtils;

public class ZipContext extends PdfContext {

    @Override
    public CodecDocument openDocumentInner(String fileName, String password) {
        LOG.d("ZipContext", fileName);
        String path = CacheZipUtils.extracIfNeed(fileName).unZipPath;
        if (path.endsWith("zip")) {
            return null;
        }
        return BookType.getCodecContextByPath(path).openDocument(path, password);
    }

}
