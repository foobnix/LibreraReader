package org.ebookdroid.droids;

import com.foobnix.android.utils.LOG;
import com.foobnix.ext.CacheZipUtils;
import com.foobnix.ext.EpubExtractor;
import com.foobnix.ext.FooterNote;
import com.foobnix.ext.MobiExtract;
import com.foobnix.model.AppSP;
import com.foobnix.pdf.info.AppsConfig;
import com.foobnix.pdf.info.JsonHelper;
import com.foobnix.pdf.info.model.BookCSS;
import com.foobnix.sys.TempHolder;

import org.ebookdroid.core.codec.CodecDocument;
import org.ebookdroid.core.codec.AbstractCodecDocument;
import org.ebookdroid.droids.mupdf.codec.MuPdfDocument;
import org.ebookdroid.droids.mupdf.codec.PdfContext;

import java.io.File;
import java.util.Map;

public class MobiContext extends PdfContext {

    String fileNameEpub = null;

    public int originalHashCode;
    File cacheFile;

    @Override
    public File getCacheFileName(String fileName) {
        originalHashCode = (fileName + BookCSS.get().isAutoHypens + AppSP.get().hypenLang + BookCSS.get().isEnableBBCode).hashCode();
        cacheFile = new File(CacheZipUtils.CACHE_BOOK_DIR, originalHashCode + "" + originalHashCode + ".epub");
        return cacheFile;
    }

    /**
     * Writes the book as {@code outName}.epub in {@code outDir}.
     */
    protected FooterNote convert(String fileName, String outDir, String outName) throws Exception {
        return MobiExtract.extract(fileName, outDir, outName);
    }

    @Override
    public CodecDocument openDocumentInner(String fileName, String password) {

        LOG.d("Context", "MobiContext", fileName);

        if (!AppsConfig.IS_LOG && cacheFile.isFile()) {
            fileNameEpub = cacheFile.getPath();
            LOG.d("Context", "MobiContext cache", fileNameEpub);

        } else {
            try {
                // Without hyphens the converted book is the cached one, so it gets the cache file's
                // name: under any other the next open neither finds it nor keeps it
                String outName = BookCSS.get().isAutoHypens ? "temp".hashCode() + "" : originalHashCode + "" + originalHashCode;

                FooterNote extract = convert(fileName, CacheZipUtils.CACHE_BOOK_DIR.getPath(), outName);
                fileNameEpub = extract.path;
                LOG.d("Context", "MobiContext outName", outName, extract.path);
                if (BookCSS.get().isAutoHypens) {

                    EpubExtractor.proccessHypens(fileNameEpub, cacheFile.getPath(), null);
                    fileNameEpub = cacheFile.getPath();
                }
                LOG.d("Context", "MobiContext extract", fileNameEpub);

            } catch (Exception e) {
                LOG.e(e);
            }
        }

        // A book the conversion gave nothing for cannot be opened; its null path reached MuPDF's
        // native open and killed the app (a .chm in the self-test)
        if (fileNameEpub == null || !new File(fileNameEpub).isFile()) {
            LOG.d("Context", "MobiContext nothing converted", fileName, fileNameEpub);
            return null;
        }

        final AbstractCodecDocument muPdfDocument = openReflowable(fileNameEpub, password);
        if (muPdfDocument == null) {
            // No engine could open it; openReflowable has logged why
            return null;
        }

        final File jsonFile = new File(cacheFile + ".json");
        if (jsonFile.isFile()) {
            muPdfDocument.setFootNotes(JsonHelper.fileToMap(jsonFile));
            LOG.d("Load notes from file", jsonFile);
        } else {

            new Thread("@T mobi set footernotes") {
                @Override
                public void run() {
                    Map<String, String> notes = null;
                    try {
                        notes = EpubExtractor.get().getFooterNotes(fileNameEpub);
                        LOG.d("new file name", fileNameEpub);
                        muPdfDocument.setFootNotes(notes);

                        // a cancelled extraction is empty or partial, it must not stay in the cache
                        if (!TempHolder.get().loadingCancelled.get()) {
                            JsonHelper.mapToFile(jsonFile, notes);
                            LOG.d("save notes to file", jsonFile);
                        }

                        removeTempFilesIfCancel();

                    } catch (OutOfMemoryError e) {
                        System.gc();
                        notes = null;
                        LOG.e(e);
                    } catch (Exception e) {
                        notes = null;
                        LOG.e(e);
                    }
                }

                ;
            }.start();
        }


        return muPdfDocument;
    }

}
