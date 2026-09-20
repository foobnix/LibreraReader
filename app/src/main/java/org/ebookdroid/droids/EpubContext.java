package org.ebookdroid.droids;

import com.foobnix.android.utils.LOG;
import com.foobnix.ext.CacheZipUtils;
import com.foobnix.ext.ConversionCache;
import com.foobnix.ext.EpubExtractor;
import com.foobnix.ext.EpubProcessingSettings;
import com.foobnix.model.AppSP;
import com.foobnix.model.AppState;
import com.foobnix.pdf.info.AppsConfig;
import com.foobnix.pdf.info.JsonHelper;
import com.foobnix.pdf.info.BookCacheLeases;
import com.foobnix.sys.TempHolder;

import net.lingala.zip4j.ZipFile;
import net.lingala.zip4j.exception.ZipException;

import org.ebookdroid.core.codec.CodecDocument;
import org.ebookdroid.droids.mupdf.codec.MuPdfDocument;
import org.ebookdroid.droids.mupdf.codec.PdfContext;

import java.io.File;
import java.util.Locale;
import java.util.Map;

public class
EpubContext extends PdfContext {

    private static final String TAG = "EpubContext";
    private static final String PROCESSED_PREFIX = "processed-epub-";
    File cacheFile;

    public static String processingSettingsKey() { return EpubProcessingSettings.key(); }

    public static String prepareProcessingLanguage(String metadataLanguage) {
        String language = AppState.get().isDefaultHyphenLanguage
                ? AppState.get().defaultHyphenLanguageCode : metadataLanguage;
        if (language == null || language.trim().isEmpty()) {
            AppSP.get().hypenLang = null;
            return null;
        }
        String code = language.trim().toLowerCase(Locale.US).split("[-_]", 2)[0];
        if (code.length() == 3) {
            for (String iso2 : Locale.getISOLanguages()) {
                try {
                    if (code.equals(Locale.forLanguageTag(iso2).getISO3Language())) {
                        code = iso2;
                        break;
                    }
                } catch (Exception ignored) { }
            }
        }
        AppSP.get().hypenLang = code;
        return code;
    }

    @Override protected EpubProcessingSettings.Scope captureProcessingSettings(String path) {
        String language = null;
        try { language = prepareProcessingLanguage(EpubExtractor.get().getBookMetaInformation(path).getLang()); }
        catch (Exception failure) { LOG.e(failure); }
        return EpubProcessingSettings.capture(language);
    }

    @Override
    public File getCacheFileName(String fileNameOriginal) {
        LOG.d(TAG, "getCacheFileName", fileNameOriginal, AppSP.get().hypenLang);
        cacheFile = new File(CacheZipUtils.CACHE_BOOK_DIR, PROCESSED_PREFIX + ConversionCache.key(fileNameOriginal + EpubProcessingSettings.key()) + ".epub");
        return cacheFile;
    }

    @Override
    public CodecDocument openDocumentInner(final String fileName, String password) {
        LOG.d(TAG, fileName);

        if (cacheFile == null) {
            cacheFile = getCacheFileName(fileName);
        }

        ConversionCache.prepare(cacheFile);
        final boolean process = EpubProcessingSettings.enabled();
        final boolean showNotes = EpubProcessingSettings.isShowFooterNotesInText();
        final File notesFile = FooterNotesCache.fileFor(cacheFile);
        Map<String, String> notes = null;
        AutoCloseable outputLease = null;
        String bookPath = fileName;
        if (process) {
            synchronized (BookCacheLeases.class) {
                if (cacheFile.isFile()) outputLease = BookCacheLeases.acquire(cacheFile);
            }
            if (outputLease == null) {
                if (EpubOpenPlan.needsNotesBeforeOpen(showNotes, process, false)) {
                    notes = loadNotes(fileName);
                    LOG.d("footer-notes-extracted");
                }
                File temporary = null;
                try {
                    AutoCloseable writing;
                    synchronized (BookCacheLeases.class) {
                        temporary = BookCacheLeases.temporary(cacheFile.getParentFile(), "epub-process-");
                        writing = BookCacheLeases.acquire(temporary);
                    }
                    try (AutoCloseable protectedOutput = writing) {
                        EpubExtractor.proccessHypensApache(fileName, temporary.getPath(), notes);
                        if (TempHolder.get().loadingCancelled.get())
                            throw new java.io.IOException("EPUB processing cancelled");
                        synchronized (BookCacheLeases.class) {
                            if (!cacheFile.isFile()) BookCacheLeases.publish(temporary, cacheFile);
                            outputLease = BookCacheLeases.acquire(cacheFile);
                        }
                    }
                } catch (Exception failure) {
                    LOG.e(failure);
                } finally {
                    if (temporary != null) temporary.delete();
                }
            }
            if (outputLease != null) bookPath = cacheFile.getPath();
        }

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

        final MuPdfDocument muPdfDocument;
        try {
            muPdfDocument = new MuPdfDocument(this, MuPdfDocument.FORMAT_PDF, bookPath, password);
        } finally {
            if (outputLease != null) {
                try { outputLease.close(); } catch (Exception failure) { LOG.e(failure); }
            }
        }
        muPdfDocument.cacheFilename = bookPath;

        if (notes != null) {
            muPdfDocument.setFootNotes(notes);
            // the map was just extracted: cache it, but let the book open first
            FooterNotesCache.saveAsync(notesFile, notes);
        }

        BookCacheLeases.startLeasedThread("@T openDocument", Thread.MIN_PRIORITY, () -> {
                try {

                    if (muPdfDocument.getFootNotes() == null) {
                        // a rewritten book carries the notes in its text; the map is read here,
                        // for the note popups, and cached when it had to be extracted
                        Map<String, String> background = loadNotes(fileName);
                        muPdfDocument.setFootNotes(background);
                        FooterNotesCache.save(notesFile, background);
                    }
                    muPdfDocument.setMediaAttachment(EpubExtractor.getAttachments(fileName));

                    removeTempFilesIfCancel();
                } catch (Throwable e) {
                    LOG.e(e);
                }
        }, new File(fileName), cacheFile);

        return muPdfDocument;
    }

    /** The cached map when there is one, a fresh extraction when there is not. Writes nothing. */
    public Map<String, String> loadNotes(String fileName) {
        final File jsonFile = FooterNotesCache.fileFor(cacheFile);
        if (JsonHelper.isValidMapFile(jsonFile)) {
            LOG.d("getNotes cache", fileName);
            return JsonHelper.fileToMap(jsonFile);
        }
        LOG.d("getNotes extract", fileName);
        return EpubExtractor.get().getFooterNotes(fileName);
    }

}
