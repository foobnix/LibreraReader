package org.ebookdroid.droids;

import java.io.File;
import java.util.Map;

import org.ebookdroid.core.codec.CodecDocument;
import org.ebookdroid.droids.mupdf.codec.MuPdfDocument;
import org.ebookdroid.droids.mupdf.codec.PdfContext;

import com.foobnix.android.utils.LOG;
import com.foobnix.ext.CacheZipUtils;
import com.foobnix.ext.EpubExtractor;
import com.foobnix.pdf.info.JsonHelper;
import com.foobnix.pdf.info.model.BookCSS;

public class EpubContext extends PdfContext {

    File cacheFile;

    @Override
    public File getCacheFileName(String fileNameOriginal) {
        cacheFile = new File(CacheZipUtils.CACHE_BOOK_DIR, (fileNameOriginal + BookCSS.get().isAutoHypens + BookCSS.get().hypenLang).hashCode() + ".epub");
        return cacheFile;
    }

    @Override
    public CodecDocument openDocumentInner(final String fileName, String password) {
        LOG.d("Context", "EpubContext", fileName);

        if (BookCSS.get().isAutoHypens && !cacheFile.isFile()) {
            EpubExtractor.proccessHypens(fileName, cacheFile.getPath());
        }

        String bookPath = BookCSS.get().isAutoHypens ? cacheFile.getPath() : fileName;
        final MuPdfDocument muPdfDocument = new MuPdfDocument(this, MuPdfDocument.FORMAT_PDF, bookPath, password);

        final File jsonFile = new File(cacheFile + ".json");
        if (jsonFile.isFile()) {
            muPdfDocument.setFootNotes(JsonHelper.fileToMap(jsonFile));
            LOG.d("Load notes from file", jsonFile);
        }

        new Thread() {
            @Override
            public void run() {
                Map<String, String> notes = null;
                try {
                    muPdfDocument.setMediaAttachment(EpubExtractor.getAttachments(fileName));
                    if (!jsonFile.isFile()) {
                        notes = EpubExtractor.get().getFooterNotes(fileName);
                        muPdfDocument.setFootNotes(notes);

                        JsonHelper.mapToFile(jsonFile, notes);
                        LOG.d("save notes to file", jsonFile);
                    }
                } catch (Exception e) {
                    LOG.e(e);
                }
            };
        }.start();

        return muPdfDocument;
    }

}
