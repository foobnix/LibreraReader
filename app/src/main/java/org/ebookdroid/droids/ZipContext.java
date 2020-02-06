package org.ebookdroid.droids;

import androidx.core.util.Pair;

import com.foobnix.android.utils.LOG;
import com.foobnix.ext.CacheZipUtils;
import com.foobnix.ext.CacheZipUtils.CacheDir;
import com.foobnix.pdf.info.ExtUtils;

import org.ebookdroid.BookType;
import org.ebookdroid.core.codec.CodecContext;
import org.ebookdroid.core.codec.CodecDocument;
import org.ebookdroid.droids.mupdf.codec.PdfContext;

import java.io.File;

public class ZipContext extends PdfContext {

    @Override
    public CodecDocument openDocumentInner(String fileName, String password) {
        LOG.d("ZipContext begin", fileName);

        Pair<Boolean, String> pack = CacheZipUtils.isSingleAndSupportEntry(fileName);
        if (pack.first) {
            LOG.d("ZipContext", "Singe archive entry");
            Fb2Context fb2Context = new Fb2Context();
            String etryPath = getFileNameSalt(fileName) + pack.second;
            String path = new File(CacheDir.ZipApp.getDir(), etryPath).getPath();

            File cacheFileName = fb2Context.getCacheFileName(path + getFileNameSalt(path));
            LOG.d("ZipContext", etryPath, cacheFileName.getName());
            if (cacheFileName.exists()) {
                LOG.d("ZipContext", "FB2 cache exists");
                return fb2Context.openDocumentInner(etryPath, password);
            }
        }


        String path = CacheZipUtils.extracIfNeed(fileName, CacheDir.ZipApp, getFileNameSalt(fileName)).unZipPath;
        if (ExtUtils.isZip(path)) {
            return null;
        }

        CodecContext ctx = BookType.getCodecContextByPath(path);
        LOG.d("ZipContext", "open", path);
        return ctx.openDocument(path, password);
    }

}
