package org.ebookdroid.droids;

import com.foobnix.android.utils.LOG;
import com.foobnix.ext.CacheZipUtils;
import com.foobnix.ext.EpubExtractor;
import com.foobnix.ext.office.DocToEpub;
import com.foobnix.ext.office.DocxToEpub;
import com.foobnix.ext.office.RtfToEpub;
import com.foobnix.model.AppSP;
import com.foobnix.pdf.info.JsonHelper;
import com.foobnix.pdf.info.model.BookCSS;
import com.foobnix.sys.TempHolder;

import org.ebookdroid.core.codec.AbstractCodecDocument;
import org.ebookdroid.core.codec.CodecDocument;
import org.ebookdroid.droids.mupdf.codec.PdfContext;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

/**
 * The office formats - {@code .docx}, {@code .doc}, {@code .rtf} - converted to an EPUB by
 * Librera's own converters ({@link DocxToEpub}, {@link DocToEpub}, {@link RtfToEpub}) and then
 * opened as any EPUB is, by MuPDF or foliate, with the book's hyphenation and its footnotes.
 *
 * Which converter is used depends on what the file is, not on its name: a Word document saved
 * as RTF under a .doc name, or a .docx renamed .doc, opens all the same.
 */
public class OfficeContext extends PdfContext {

    /** Raised when the converters change, so books converted before are converted again. */
    private static final int VERSION = 1;

    protected File cacheFile;

    @Override
    public File getCacheFileName(String fileName) {
        final File source = new File(fileName);
        final int hash = (fileName + source.length() + source.lastModified() + VERSION
                + BookCSS.get().isAutoHypens + AppSP.get().hypenLang).hashCode();
        cacheFile = new File(CacheZipUtils.CACHE_BOOK_DIR, hash + ".office.epub");
        return cacheFile;
    }

    /** What the file is when it is none of the office formats; null to give up on it. */
    protected CodecDocument openOther(String fileName, String password) {
        return null;
    }

    @Override
    public CodecDocument openDocumentInner(String fileName, String password) {
        if (cacheFile == null) {
            getCacheFileName(fileName);
        }
        if (!cacheFile.isFile()) {
            final File temp = new File(cacheFile.getPath() + ".tmp.epub");
            temp.delete();
            final int kind = kind(fileName);
            boolean converted;
            switch (kind) {
                case RTF:
                    converted = RtfToEpub.convert(fileName, temp);
                    break;
                case DOC:
                    converted = DocToEpub.convert(fileName, temp);
                    break;
                case DOCX:
                    converted = DocxToEpub.convert(fileName, temp);
                    break;
                default:
                    return openOther(fileName, password);
            }
            if (!converted || TempHolder.get().loadingCancelled.get()) {
                LOG.d("OfficeContext", "not converted", fileName);
                temp.delete();
                return null;
            }
            if (BookCSS.get().isAutoHypens) {
                EpubExtractor.proccessHypens(temp.getPath(), cacheFile.getPath(), null);
            }
            if (cacheFile.isFile()) {
                temp.delete();
            } else if (TempHolder.get().loadingCancelled.get() || !temp.renameTo(cacheFile)) {
                // without the hyphens, if putting them in failed, rather than not at all
                temp.delete();
                LOG.d("OfficeContext", "cannot keep", cacheFile);
                return null;
            }
            if (!cacheFile.isFile()) {
                return null;
            }
        }

        final AbstractCodecDocument document = openReflowable(cacheFile.getPath(), password);
        if (document == null) {
            return null;
        }
        footNotes(document);
        return document;
    }

    private void footNotes(final AbstractCodecDocument document) {
        final File jsonFile = new File(cacheFile + ".json");
        if (jsonFile.isFile()) {
            document.setFootNotes(JsonHelper.fileToMap(jsonFile));
            return;
        }
        final String epub = cacheFile.getPath();
        new Thread("@T office footnotes") {
            @Override
            public void run() {
                try {
                    final Map<String, String> notes = EpubExtractor.get().getFooterNotes(epub);
                    document.setFootNotes(notes);
                    // a cancelled extraction is empty or partial, it must not stay in the cache
                    if (!TempHolder.get().loadingCancelled.get()) {
                        JsonHelper.mapToFile(jsonFile, notes);
                    }
                    removeTempFilesIfCancel();
                } catch (OutOfMemoryError e) {
                    System.gc();
                    LOG.e(e);
                } catch (Exception e) {
                    LOG.e(e);
                }
            }
        }.start();
    }

    static final int OTHER = 0;
    static final int RTF = 1;
    static final int DOC = 2;
    static final int DOCX = 3;

    /** What the file is, from its first bytes. */
    static int kind(String fileName) {
        final byte[] header = new byte[8];
        int read = 0;
        try {
            final InputStream in = new FileInputStream(fileName);
            try {
                read = in.read(header);
            } finally {
                in.close();
            }
        } catch (IOException e) {
            LOG.e(e);
        }
        if (read >= 5 && RtfToEpub.isRtf(header)) {
            return RTF;
        }
        if (read == 8 && (header[0] & 0xFF) == 0xD0 && (header[1] & 0xFF) == 0xCF
                && (header[2] & 0xFF) == 0x11 && (header[3] & 0xFF) == 0xE0) {
            return DOC;
        }
        if (read >= 4 && header[0] == 'P' && header[1] == 'K' && header[2] == 3 && header[3] == 4) {
            return DOCX;
        }
        return OTHER;
    }
}
