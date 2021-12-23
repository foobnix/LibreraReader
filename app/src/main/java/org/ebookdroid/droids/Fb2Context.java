package org.ebookdroid.droids;

import com.foobnix.android.utils.LOG;
import com.foobnix.ext.CacheZipUtils;
import com.foobnix.ext.Fb2Extractor;
import com.foobnix.model.AppSP;
import com.foobnix.model.AppState;
import com.foobnix.pdf.info.JsonHelper;
import com.foobnix.pdf.info.model.BookCSS;

import org.ebookdroid.core.codec.CodecDocument;
import org.ebookdroid.droids.mupdf.codec.MuPdfDocument;
import org.ebookdroid.droids.mupdf.codec.PdfContext;

import java.io.File;
import java.util.Map;

public class Fb2Context extends PdfContext {

    File cacheFile;

    @Override
    public File getCacheFileName(String fileNameOriginal) {
        fileNameOriginal = fileNameOriginal + AppState.get().isShowFooterNotesInText + BookCSS.get().isAutoHypens + AppSP.get().hypenLang + AppSP.get().isDouble + AppState.get().isAccurateFontSize + BookCSS.get().isCapitalLetter;
        cacheFile = new File(CacheZipUtils.CACHE_BOOK_DIR, fileNameOriginal.hashCode() + ".epub");
        return cacheFile;
    }

    MuPdfDocument muPdfDocument;

    @Override
    public CodecDocument openDocumentInner(final String fileName, String password) {
        String outName = null;

        Map<String, String> notes = null;
        if (AppState.get().isShowFooterNotesInText) {
            notes = getNotes(fileName);

        }

        if (cacheFile.isFile()) {
            outName = cacheFile.getPath();
        } else

        if (outName == null) {
            outName = cacheFile.getPath();
            Fb2Extractor.get().convert(fileName, outName, false, notes);
            LOG.d("Fb2Context create", fileName, "to", outName);
        }

        LOG.d("Fb2Context open", outName);

        try {
            muPdfDocument = new MuPdfDocument(this, MuPdfDocument.FORMAT_PDF, outName, password);
            muPdfDocument.getPageCount();
        } catch (Exception e) {
            LOG.e(e);
            LOG.d("Fb2Context Fix XML true");
            if (cacheFile.isFile()) {
                cacheFile.delete();
            }
            Fb2Extractor.get().convert(fileName, outName, true, notes);
            LOG.d("Fb2Context create 2", outName);
            muPdfDocument = new MuPdfDocument(this, MuPdfDocument.FORMAT_PDF, outName, password);
        }

        if (notes != null) {
            muPdfDocument.setFootNotes(notes);
        } else {
            new Thread("@T fb2 set footnotes") {
                @Override
                public void run() {
                    muPdfDocument.setFootNotes(getNotes(fileName));
                    removeTempFiles();
                };
            }.start();
        }

        return muPdfDocument;
    }

    public Map<String, String> getNotes(String fileName) {
        Map<String, String> notes = null;
        final File jsonFile = new File(cacheFile + ".json");
        if (jsonFile.isFile()) {
            notes = JsonHelper.fileToMap(jsonFile);
        } else {
            notes = Fb2Extractor.get().getFooterNotes(fileName);
            JsonHelper.mapToFile(jsonFile, notes);
            LOG.d("save notes to file", jsonFile);
        }
        return notes;
    }

}
