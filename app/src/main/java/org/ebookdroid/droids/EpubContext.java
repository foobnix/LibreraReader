package org.ebookdroid.droids;

import com.foobnix.android.utils.LOG;
import com.foobnix.ext.CacheZipUtils;
import com.foobnix.ext.EpubExtractor;
import com.foobnix.model.AppSP;
import com.foobnix.model.AppState;
import com.foobnix.pdf.info.AppsConfig;
import com.foobnix.pdf.info.JsonHelper;
import com.foobnix.pdf.info.model.BookCSS;

import net.lingala.zip4j.ZipFile;
import net.lingala.zip4j.exception.ZipException;

import org.ebookdroid.core.codec.CodecDocument;
import org.ebookdroid.core.codec.AbstractCodecDocument;
import org.ebookdroid.droids.mupdf.codec.MuPdfDocument;
import org.ebookdroid.droids.mupdf.codec.PdfContext;

import java.io.File;
import java.util.Map;

public class
EpubContext extends PdfContext {

    private static final String TAG = "EpubContext";
    File cacheFile;

    /**
     * The converted book, named after the settings the conversion reads. A setting the conversion
     * does not read must not be in the name: every style change restarts the reader, and a new
     * name converts the whole book again. Full screen and image scaling were here and did that;
     * neither changes a byte of the conversion, and MuPDF checks its saved layout against the page
     * size itself (MuPdfDocument folds image scaling into that file's name, as MuPDF cannot).
     */
    @Override
    public File getCacheFileName(String fileNameOriginal) {
        LOG.d(TAG, "getCacheFileName", fileNameOriginal, AppSP.get().hypenLang);
        cacheFile = new File(CacheZipUtils.CACHE_BOOK_DIR, (fileNameOriginal +
                AppState.get().isReferenceMode +
                AppState.get().isShowPageNumbers +
                AppState.get().isShowFooterNotesInText +
                //AppState.get().isAccurateFontSize +
                BookCSS.get().documentStyle +
                BookCSS.get().isAutoHypens +
                AppState.get().isBionicMode +
                AppSP.get().hypenLang +
                AppState.get().textReplacementHash +
                BookCSS.get().isEnableBBCode +
                AppState.get().isExperimental)
                .hashCode() + ".epub");
        return cacheFile;
    }

    @Override
    public CodecDocument openDocumentInner(final String fileName, String password) {
        LOG.d(TAG, fileName);

        if (cacheFile == null) {
            cacheFile = getCacheFileName(fileName);
        }

        final boolean process = /** LibreraBuildConfig.DEBUG || **/AppState.get().isEnableTextReplacement || BookCSS.get().isAutoHypens ||
                AppState.get().isReferenceMode || AppState.get().isShowFooterNotesInText || BookCSS.get().isEnableBBCode;
        // A copy cut short by the app dying while converting - before proccessHypens wrote it
        // aside first - would be reused for good, and the book never open again
        if (cacheFile.isFile() && !new ZipFile(cacheFile).isValidZipFile()) {
            LOG.d(TAG, "converted copy is not a whole zip, converting again", cacheFile);
            cacheFile.delete();
        }
        final boolean cacheExists = cacheFile.isFile();
        final boolean rewritten = EpubOpenPlan.isRewritten(process, cacheExists);

        final File notesFile = FooterNotesCache.fileFor(cacheFile);
        final boolean notesCached = notesFile.isFile();

        Map<String, String> notes = null;
        if (EpubOpenPlan.needsNotesBeforeOpen(AppState.get().isShowFooterNotesInText, process, cacheExists)) {
            notes = loadNotes(fileName);
            LOG.d("footer-notes-extracted");
        }

        if (process && !rewritten) {
            EpubExtractor.proccessHypens(fileName, cacheFile.getPath(), notes);
        }

        String bookPath = EpubOpenPlan.bookPath(process, cacheFile.isFile(), cacheFile.getPath(), fileName);

        if (AppsConfig.IS_LOG) {//accelerate open books
            File out = new File(cacheFile.getPath() + "-source");
            try {
                if (!out.isDirectory()) {
                    out.mkdirs();
                    new ZipFile(bookPath).extractAll(out.getPath());
                    LOG.d("EpubContext unzip all", out.getPath());

                }
                //bookPath = out.getPath() + "/META-INF/container.xml";
                LOG.d("EpubContext open", bookPath);
            } catch (ZipException e) {
                LOG.e(e);
            }

        }

        final AbstractCodecDocument muPdfDocument = openReflowable(bookPath, password);
        if (muPdfDocument == null) {
            // No engine could open it; openReflowable has logged why
            return null;
        }
        muPdfDocument.cacheFilename = bookPath;

        if (notes != null) {
            muPdfDocument.setFootNotes(notes);
            if (!notesCached) {
                // the map was just extracted: cache it, but let the book open first
                FooterNotesCache.saveAsync(notesFile, notes);
            }
        }

        Thread t = new Thread("@T openDocument") {
            @Override
            public void run() {
                try {

                    if (muPdfDocument.getFootNotes() == null) {
                        // a rewritten book carries the notes in its text; the map is read here,
                        // for the note popups, and cached when it had to be extracted
                        Map<String, String> background = loadNotes(fileName);
                        muPdfDocument.setFootNotes(background);
                        if (!notesCached) {
                            FooterNotesCache.save(notesFile, background);
                        }
                    }
                    muPdfDocument.setMediaAttachment(EpubExtractor.getAttachments(fileName));

                    removeTempFilesIfCancel();
                } catch (Throwable e) {
                    LOG.e(e);
                }
            }

        };
        t.setPriority(Thread.MIN_PRIORITY);
        t.start();

        return muPdfDocument;
    }

    /** The cached map when there is one, a fresh extraction when there is not. Writes nothing. */
    public Map<String, String> loadNotes(String fileName) {
        final File jsonFile = FooterNotesCache.fileFor(cacheFile);
        if (/** !LibreraBuildConfig.DEBUG && **/jsonFile.isFile()) {
            LOG.d("getNotes cache", fileName);
            return JsonHelper.fileToMap(jsonFile);
        }
        LOG.d("getNotes extract", fileName);
        return EpubExtractor.get().getFooterNotes(fileName);
    }

}
