package org.ebookdroid.droids;

import java.io.File;

import org.ebookdroid.BookType;
import org.ebookdroid.core.codec.CodecContext;
import org.ebookdroid.core.codec.CodecDocument;
import org.ebookdroid.droids.mupdf.codec.PdfContext;

import com.foobnix.android.utils.LOG;
import com.foobnix.ext.CacheZipUtils;

public class ZipContext extends PdfContext {


    @Override
    public CodecDocument openDocumentInner(String fileName, String password) {
        LOG.d("ZipContext", fileName);

        Fb2Context fb2Context = new Fb2Context();
        File cacheFileName = fb2Context.getCacheFileName(fileName);
        if (cacheFileName.exists()) {
            LOG.d("ZipContext", "FB2 cache exists");
            return fb2Context.openDocumentInner(fileName, password);
        }

        String path = CacheZipUtils.extracIfNeed(fileName).unZipPath;
        if (path.endsWith("zip")) {
            return null;
        }
        CodecContext ctx = BookType.getCodecContextByPath(path);
        return ctx.openDocument(path, password);
    }

}
