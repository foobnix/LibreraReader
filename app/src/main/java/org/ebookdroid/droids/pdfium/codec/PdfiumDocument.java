package org.ebookdroid.droids.pdfium.codec;

import android.graphics.RectF;
import android.os.ParcelFileDescriptor;

import com.foobnix.android.utils.LOG;
import com.foobnix.android.utils.TxtUtils;
import com.foobnix.sys.TempHolder;

import org.ebookdroid.BookType;
import org.ebookdroid.core.codec.AbstractCodecDocument;
import org.ebookdroid.core.codec.CodecDocument;
import org.ebookdroid.core.codec.CodecPage;
import org.ebookdroid.core.codec.CodecPageInfo;
import org.ebookdroid.core.codec.OutlineLink;
import org.ebookdroid.droids.mupdf.codec.MuPdfDocument;
import org.ebookdroid.droids.mupdf.codec.PdfContext;
import org.ebookdroid.droids.mupdf.codec.exceptions.MuPdfPasswordRequiredException;
import org.ebookdroid.droids.mupdf.codec.exceptions.MuPdfWrongPasswordEnteredException;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import io.legere.pdfiumandroid.PdfDocument;
import io.legere.pdfiumandroid.PdfPage;
import io.legere.pdfiumandroid.PdfTextPage;
import io.legere.pdfiumandroid.PdfiumCore;
import io.legere.pdfiumandroid.api.Meta;
import io.legere.pdfiumandroid.api.PdfPasswordException;
import io.legere.pdfiumandroid.api.Size;

/**
 * A PDF opened by PDFium.
 *
 * It answers the same {@link CodecDocument} contract as
 * {@link org.ebookdroid.droids.mupdf.codec.MuPdfDocument}: the page count, the page sizes, the
 * pages, the outline and the metadata, so the reader, the library's covers and the converters work
 * unchanged.
 *
 * Annotations are the exception. PDFium draws the ones a document already carries, but this
 * binding can neither list them nor write one, so a MuPDF document for the same file is opened
 * beside it - lazily, the first time an annotation is asked for - and owns them: listing, drawing
 * new ones and saving. While it holds changes PDFium has not seen, it also draws the pages, so a
 * freshly drawn annotation shows up at once (see {@link PdfiumPage#renderBitmap}).
 */
public class PdfiumDocument extends AbstractCodecDocument {

    /**
     * PDFium hands out no document handle of its own, and Librera only uses the handle to tell
     * documents apart, so a counter stands in for it. It must not be 0: that is "recycled".
     */
    private static final AtomicLong HANDLES = new AtomicLong(1);

    /** Points per inch: the unit MuPDF reports page sizes in, so PDFium is asked for the same. */
    private static final int POINTS_DPI = 72;

    private final String fname;
    private final PdfDocument document;

    private volatile Meta meta;
    private volatile int pageCount = -1;

    /** The MuPDF document that owns this file's annotations; opened on demand. */
    private volatile CodecDocument annotDocument;
    private volatile boolean annotDocumentTried;

    /** Set once an annotation has been drawn, so the pages render through MuPDF from then on. */
    private volatile boolean annotationsEdited;

    private PdfiumDocument(final PdfiumContext context, final String fname, final PdfDocument document) {
        super(context, HANDLES.incrementAndGet());
        this.fname = fname;
        this.document = document;
    }

    /**
     * Opens a PDF.
     *
     * @return the document, or <code>null</code> when PDFium cannot open the file
     * @throws org.ebookdroid.droids.mupdf.codec.exceptions.MuPdfPasswordException when the file
     *         needs a password - the exception Librera's password prompt is driven by, so an
     *         encrypted PDF asks for its password with either engine
     */
    static PdfiumDocument open(final PdfiumContext context, final String fname, final String password) {
        final PdfiumCore core = PdfiumContext.core();
        if (core == null) {
            return null;
        }
        TempHolder.lock.lock();
        ParcelFileDescriptor fd = null;
        try {
            fd = ParcelFileDescriptor.open(new File(fname), ParcelFileDescriptor.MODE_READ_ONLY);
            // PDFium closes the descriptor itself when the document is closed
            final PdfDocument document = core.newDocument(fd, TxtUtils.isEmpty(password) ? null : password);
            LOG.d("PDFIUM! >>> open [document]", fname);
            return new PdfiumDocument(context, fname, document);
        } catch (PdfPasswordException e) {
            close(fd);
            throw TxtUtils.isEmpty(password) ? new MuPdfPasswordRequiredException()
                    : new MuPdfWrongPasswordEnteredException();
        } catch (Throwable e) {
            close(fd);
            LOG.e(e, fname);
            return null;
        } finally {
            TempHolder.lock.unlock();
        }
    }

    private static void close(final ParcelFileDescriptor fd) {
        if (fd != null) {
            try {
                fd.close();
            } catch (Throwable e) {
                LOG.e(e);
            }
        }
    }

    public String getPath() {
        return fname;
    }

    // ---------------------------------------------------------------- pages

    /** Opens a page by its 0-based index; for {@link PdfiumPage} only. */
    PdfPage openPage(final int index) {
        return document.openPage(index);
    }

    @Override
    public CodecPage getPageInner(final int pageNumber) {
        return PdfiumPage.createPage(this, pageNumber + 1);
    }

    @Override
    public int getPageCount() {
        if (pageCount >= 0) {
            return pageCount;
        }
        TempHolder.lock.lock();
        try {
            if (isRecycled()) {
                return 0;
            }
            pageCount = Math.max(0, document.getPageCount());
            return pageCount;
        } catch (Throwable e) {
            LOG.e(e);
            return 0;
        } finally {
            TempHolder.lock.unlock();
        }
    }

    @Override
    public int getPageCount(final int w, final int h, final int fsize) {
        // A PDF has fixed pages: the size the text would be laid out at does not change them.
        return getPageCount();
    }

    @Override
    public CodecPageInfo getUnifiedPageInfo() {
        // The pages have their own sizes, as with MuPDF's PDFs.
        return null;
    }

    @Override
    public CodecPageInfo getPageInfo(final int pageNumber) {
        TempHolder.lock.lock();
        try {
            if (isRecycled()) {
                return null;
            }
            // The size is read off the document, without opening the page
            final Size size = document.getPageSize(pageNumber, POINTS_DPI);
            if (size == null || size.getWidth() <= 0 || size.getHeight() <= 0) {
                return null;
            }
            final CodecPageInfo info = new CodecPageInfo(size.getWidth(), size.getHeight());
            // The sizes are already the ones the page is shown at, /Rotate applied, which is what
            // MuPDF's getPageInfo reports too - so there is no rotation left to pass on.
            info.rotation = 0;
            return info;
        } catch (Throwable e) {
            LOG.e(e);
            return null;
        } finally {
            TempHolder.lock.unlock();
        }
    }

    @Override
    public BookType getBookType() {
        return BookType.PDF;
    }

    // ---------------------------------------------------------------- outline, text

    @Override
    public synchronized List<OutlineLink> getOutline() {
        TempHolder.lock.lock();
        try {
            if (isRecycled()) {
                LOG.d("PDFIUM! getOutline doc isRecycled");
                return Collections.emptyList();
            }
            return PdfiumOutline.get(document, getDocumentHandle());
        } finally {
            TempHolder.lock.unlock();
        }
    }

    @Override
    public String documentToHtml() {
        final StringBuilder out = new StringBuilder();
        final int pages = getPageCount();
        for (int i = 0; i < pages; i++) {
            // Pages of their own, each released again: a whole document's worth of open pages
            // would be a whole document's worth of PDFium page and text-layer memory
            final CodecPage page = getPageInner(i);
            if (page == null) {
                continue;
            }
            try {
                out.append(page.getPageHTML());
            } finally {
                page.recycle();
            }
        }
        return out.toString();
    }

    @Override
    public List<RectF> searchText(final int pageNuber, final String pattern) throws DocSearchNotSupported {
        // Librera searches the pages' text itself, as it does with MuPDF
        throw new DocSearchNotSupported();
    }

    @Override
    public Map<String, String> getFootNotes() {
        return null;
    }

    @Override
    public List<String> getMediaAttachments() {
        return null;
    }

    // ---------------------------------------------------------------- metadata

    private Meta meta() {
        Meta local = meta;
        if (local == null) {
            TempHolder.lock.lock();
            try {
                local = isRecycled() ? new Meta() : document.getDocumentMeta();
            } catch (Throwable e) {
                LOG.e(e);
                local = new Meta();
            } finally {
                TempHolder.lock.unlock();
            }
            meta = local;
        }
        return local;
    }

    /**
     * The keys MuPDF answers, with the same <code>info:</code> prefix. PDFium reads only the
     * standard entries of the document information dictionary, so anything else comes back empty.
     */
    @Override
    public String getMeta(final String option) {
        if (option == null) {
            return "";
        }
        final Meta m = meta();
        final String value;
        if (MuPdfDocument.META_INFO_TITLE.equals(option)) {
            value = m.getTitle();
        } else if (MuPdfDocument.META_INFO_AUTHOR.equals(option)) {
            value = m.getAuthor();
        } else if (MuPdfDocument.META_INFO_SUBJECT.equals(option)) {
            value = m.getSubject();
        } else if (MuPdfDocument.META_INFO_KEYWORDS.equals(option)) {
            value = m.getKeywords();
        } else if (MuPdfDocument.META_INFO_CREATOR.equals(option)) {
            value = m.getCreator();
        } else if (MuPdfDocument.META_INFO_PRODUCER.equals(option)) {
            value = m.getProducer();
        } else if (MuPdfDocument.META_INFO_CREATIONDATE.equals(option)) {
            value = m.getCreationDate();
        } else if (MuPdfDocument.META_INFO_MODIFICATIONDATE.equals(option)) {
            value = m.getModDate();
        } else {
            value = "";
        }
        return value == null ? "" : value;
    }

    @Override
    public String getBookTitle() {
        return getMeta(MuPdfDocument.META_INFO_TITLE);
    }

    @Override
    public String getBookAuthor() {
        return getMeta(MuPdfDocument.META_INFO_AUTHOR);
    }

    @Override
    public void setMeta(final String key, final String value) {
        // PDFium has no way to write the information dictionary
        LOG.d("PDFIUM! setMeta is not supported", key);
    }

    // ---------------------------------------------------------------- bookmarks

    /** The page text saved with a bookmark, as MuPDF's own bookmark text is sized. */
    private static final int BOOKMARK_TEXT_SIZE = 128;

    /** The part of it that is searched; short enough to still fit on one page after re-layout. */
    private static final int BOOKMARK_NEEDLE_SIZE = 64;

    private static final int BOOKMARK_NEEDLE_MIN = 8;

    @Override
    public String getBookmarkText(final int page) {
        TempHolder.lock.lock();
        try {
            if (isRecycled()) {
                return null;
            }
            return collapse(pageText(page), BOOKMARK_TEXT_SIZE);
        } catch (Throwable e) {
            LOG.e(e);
            return null;
        } finally {
            TempHolder.lock.unlock();
        }
    }

    /**
     * The page a bookmark was made on, looked for around the page its percentage points at:
     * <code>page</code>, <code>page + 1</code>, <code>page - 1</code> and so on. The bookmark's
     * own text wins; the page text is only used while that is not found - the same order MuPDF's
     * native search uses.
     *
     * @return the 1-based page, or -1
     */
    @Override
    public int findBookmarkPage(final int page, final String text, final String pageText) {
        final String needle1 = needle(text);
        final String needle2 = needle(pageText);
        if (needle1 == null && needle2 == null) {
            return -1;
        }
        final int range = 10;
        final int count = getPageCount();
        int found2 = -1;

        TempHolder.lock.lock();
        try {
            for (int i = 0; i <= 2 * range && (needle1 != null || found2 == -1); i++) {
                if (isRecycled()) {
                    return -1;
                }
                final int candidate = i % 2 != 0 ? page + (i + 1) / 2 : page - i / 2;
                if (candidate < 1 || candidate > count) {
                    continue;
                }
                final String haystack = collapse(pageText(candidate), 0);
                if (haystack == null) {
                    continue;
                }
                if (needle1 != null && haystack.contains(needle1)) {
                    return candidate;
                }
                if (needle2 != null && found2 == -1 && haystack.contains(needle2)) {
                    found2 = candidate;
                }
            }
        } catch (Throwable e) {
            LOG.e(e);
        } finally {
            TempHolder.lock.unlock();
        }
        return found2;
    }

    /** The text of a 1-based page, read straight from PDFium's text layer. */
    private String pageText(final int page) {
        if (page < 1) {
            return null;
        }
        PdfPage pdfPage = null;
        PdfTextPage textPage = null;
        try {
            pdfPage = document.openPage(page - 1);
            if (pdfPage == null) {
                return null;
            }
            textPage = pdfPage.openTextPage();
            return textPage == null ? null : PdfiumText.text(textPage);
        } catch (Throwable e) {
            LOG.e(e);
            return null;
        } finally {
            if (textPage != null) {
                try {
                    textPage.close();
                } catch (Throwable e) {
                    LOG.e(e);
                }
            }
            if (pdfPage != null) {
                try {
                    pdfPage.close();
                } catch (Throwable e) {
                    LOG.e(e);
                }
            }
        }
    }

    /** The searched part of a bookmark's text, or null when it is too short to tell pages apart. */
    private static String needle(final String text) {
        final String needle = collapse(text, BOOKMARK_NEEDLE_SIZE);
        return needle != null && needle.length() >= BOOKMARK_NEEDLE_MIN ? needle : null;
    }

    /**
     * Collapses runs of whitespace to one space, as MuPDF's <code>bookmark_text</code> does, so
     * the bookmarks of a document read by either engine match each other.
     *
     * @param limit the longest result, in characters; a word cut by it is dropped. 0 is no limit.
     */
    private static String collapse(final String src, final int limit) {
        if (src == null) {
            return null;
        }
        final StringBuilder out = new StringBuilder(limit > 0 ? limit : Math.min(src.length(), 4096));
        boolean space = false;
        int lastSpace = -1;
        for (int i = 0; i < src.length(); i++) {
            final char c = src.charAt(i);
            if (c == ' ' || c == '\t' || c == '\n' || c == '\r' || c == 0) {
                space = out.length() > 0;
                continue;
            }
            if (limit > 0 && out.length() + (space ? 1 : 0) + 1 >= limit) {
                if (!space && lastSpace > 0) {
                    out.setLength(lastSpace);
                }
                break;
            }
            if (space) {
                lastSpace = out.length();
                out.append(' ');
                space = false;
            }
            out.append(c);
        }
        return out.toString();
    }

    // ---------------------------------------------------------------- annotations

    /**
     * The MuPDF document that owns this file's annotations, opened on first use.
     *
     * @return the document, or <code>null</code> when MuPDF cannot open the file either
     */
    CodecDocument annotations() {
        CodecDocument local = annotDocument;
        if (local != null) {
            return local;
        }
        synchronized (this) {
            if (annotDocument == null && !annotDocumentTried) {
                annotDocumentTried = true;
                try {
                    annotDocument = new PdfContext().openDocument(fname, "");
                    LOG.d("PDFIUM! annotations opened through MuPDF", fname, annotDocument != null);
                } catch (Throwable e) {
                    LOG.e(e, fname);
                }
            }
            return annotDocument;
        }
    }

    /** Called by {@link PdfiumPage} after it has written an annotation through MuPDF. */
    void onAnnotationAdded() {
        annotationsEdited = true;
    }

    /**
     * @return <code>true</code> while the MuPDF document holds annotations PDFium cannot see
     */
    boolean hasAnnotationChanges() {
        return annotationsEdited && annotDocument != null && !annotDocument.isRecycled();
    }

    @Override
    public boolean hasChanges() {
        final CodecDocument annot = annotDocument;
        return annot != null && !annot.isRecycled() && annot.hasChanges();
    }

    @Override
    public void saveAnnotations(final String path) {
        final CodecDocument annot = annotDocument;
        if (annot == null || annot.isRecycled()) {
            LOG.d("PDFIUM! saveAnnotations skipped, nothing was changed");
            return;
        }
        annot.saveAnnotations(path);
    }

    @Override
    public void deleteAnnotation(final long pageHandle, final int index) {
        final CodecDocument annot = annotDocument;
        if (annot == null || annot.isRecycled()) {
            return;
        }
        annot.deleteAnnotation(pageHandle, index);
        onAnnotationAdded();
    }

    @Override
    public List<String> getMetaKeys() {
        return new ArrayList<String>();
    }

    // ---------------------------------------------------------------- lifecycle

    @Override
    protected void freeDocument() {
        TempHolder.lock.lock();
        try {
            final CodecDocument annot = annotDocument;
            annotDocument = null;
            if (annot != null && !annot.isRecycled()) {
                annot.recycle();
            }
            // Closes every page PDFium still holds, and the file descriptor with them
            document.close();
        } catch (Throwable e) {
            LOG.e(e);
        } finally {
            TempHolder.lock.unlock();
        }
        LOG.d("PDFIUM! <<< recycle [document]", fname);
    }
}
