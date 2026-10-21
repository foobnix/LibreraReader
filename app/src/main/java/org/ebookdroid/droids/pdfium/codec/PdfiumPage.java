package org.ebookdroid.droids.pdfium.codec;

import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.PointF;
import android.graphics.RectF;

import com.foobnix.android.utils.LOG;
import com.foobnix.model.AppState;
import com.foobnix.pdf.info.AppsConfig;
import com.foobnix.pdf.info.model.AnnotationType;
import com.foobnix.pdf.info.wrapper.MagicHelper;
import com.foobnix.sys.TempHolder;

import org.ebookdroid.common.bitmaps.BitmapRef;
import org.ebookdroid.core.codec.AbstractCodecPage;
import org.ebookdroid.core.codec.Annotation;
import org.ebookdroid.core.codec.CodecDocument;
import org.ebookdroid.core.codec.CodecPage;
import org.ebookdroid.core.codec.PageLink;
import org.ebookdroid.droids.mupdf.codec.TextWord;

import java.util.ArrayList;
import java.util.List;

import io.legere.pdfiumandroid.PdfPage;
import io.legere.pdfiumandroid.PdfTextPage;

/**
 * One page of a {@link PdfiumDocument}: the bitmap, the text layer and the links come from PDFium,
 * the annotations from the MuPDF page behind it (see {@link PdfiumDocument#annotations()}).
 *
 * The bitmap is produced the way {@link org.ebookdroid.droids.mupdf.codec.MuPdfPage} produces it -
 * the same slice of the page at the same size, with Librera's own background colour, inversion,
 * contrast and mirroring applied to the pixels afterwards - so nothing downstream has to know
 * which engine drew it.
 */
public class PdfiumPage extends AbstractCodecPage {

    /** White, which is what MuPDF renders a page on when no background colour is asked for. */
    private static final int WHITE = 0xFFFFFFFF;

    private final PdfiumDocument document;

    /** 1-based, as MuPdfPage counts pages. */
    private final int pageNumber;

    private final int actualWidth;
    private final int actualHeight;

    private volatile PdfPage page;
    private volatile PdfTextPage textPage;
    private volatile PdfiumTransform transform;

    /** The same page opened by MuPDF, created only when an annotation is asked for or written. */
    private volatile CodecPage annotPage;

    private PdfiumPage(final PdfiumDocument document, final int pageNumber, final PdfPage page,
                       final int width, final int height) {
        super(document.getPath());
        this.document = document;
        this.pageNumber = pageNumber;
        this.page = page;
        this.actualWidth = width;
        this.actualHeight = height;
    }

    /**
     * @param pageno 1-based, as {@link org.ebookdroid.droids.mupdf.codec.MuPdfPage#createPage} takes it
     * @return the open page, or <code>null</code> when it cannot be opened
     */
    static PdfiumPage createPage(final PdfiumDocument document, final int pageno) {
        TempHolder.lock.lock();
        try {
            if (document.isRecycled()) {
                LOG.d("PDFIUM! +create page isRecycled");
                return null;
            }
            final PdfPage page = document.openPage(pageno - 1);
            if (page == null) {
                LOG.d("PDFIUM! +create page failed", pageno);
                return null;
            }
            final int w = Math.max(1, page.getPageWidthPoint());
            final int h = Math.max(1, page.getPageHeightPoint());
            LOG.d("PDFIUM! +create page", pageno, w, h);
            return new PdfiumPage(document, pageno, page, w, h);
        } catch (Throwable e) {
            LOG.e(e);
            return null;
        } finally {
            TempHolder.lock.unlock();
        }
    }

    @Override
    public int getWidth() {
        return actualWidth;
    }

    @Override
    public int getHeight() {
        return actualHeight;
    }

    /** The PDF space -> page space transform, read off the page once. */
    private PdfiumTransform transform() {
        PdfiumTransform local = transform;
        if (local == null) {
            local = PdfiumTransform.of(page, actualWidth, actualHeight);
            transform = local;
        }
        return local;
    }

    /** The page's text layer, opened on first use and closed with the page. */
    private PdfTextPage textPage() {
        PdfTextPage local = textPage;
        if (local == null && !isRecycled()) {
            try {
                local = page.openTextPage();
                textPage = local;
            } catch (Throwable e) {
                LOG.e(e);
            }
        }
        return local;
    }

    // ---------------------------------------------------------------- rendering

    @Override
    public BitmapRef renderBitmap(final int width, final int height, final RectF pageSliceBounds, boolean cache) {
        final CodecPage edited = annotatedPage();
        if (edited != null) {
            // The annotations the user has just drawn live in the MuPDF document, which PDFium
            // cannot see: that document draws the page until the book is reopened.
            return edited.renderBitmap(width, height, pageSliceBounds, cache);
        }
        return render(width, height, pageSliceBounds, true);
    }

    @Override
    public BitmapRef renderBitmapSimple(final int width, final int height, final RectF pageSliceBounds) {
        final CodecPage edited = annotatedPage();
        if (edited != null) {
            return edited.renderBitmapSimple(width, height, pageSliceBounds);
        }
        return render(width, height, pageSliceBounds, false);
    }

    @Override
    public Bitmap renderThumbnail(final int width) {
        return renderThumbnail(width, getWidth(), getHeight());
    }

    @Override
    public Bitmap renderThumbnail(final int width, int originW, int originH) {
        if (originW <= 0 || originH <= 0) {
            originW = getWidth();
            originH = getHeight();
        }
        final float k = (float) originH / originW;
        final BitmapRef bitmap = renderBitmap(width, (int) (width * k), new RectF(0, 0, 1f, 1f), false);
        return bitmap == null ? null : bitmap.getBitmap();
    }

    /**
     * Renders a slice of the page at the given size.
     *
     * PDFium places a page on a bitmap by the size the whole page would have and the offset of the
     * bitmap inside it, which is the same thing MuPdfPage's matrix says: scale the page to
     * width/height, then blow it up by 1/slice and shift the slice into view.
     */
    private BitmapRef render(final int width, final int height, final RectF pageSliceBounds, final boolean withColors) {
        if (width <= 0 || height <= 0) {
            return null;
        }
        final float sliceW = pageSliceBounds.width() > 0 ? pageSliceBounds.width() : 1f;
        final float sliceH = pageSliceBounds.height() > 0 ? pageSliceBounds.height() : 1f;

        final int drawSizeX = Math.max(1, Math.round(width / sliceW));
        final int drawSizeY = Math.max(1, Math.round(height / sliceH));
        final int startX = -Math.round(pageSliceBounds.left * drawSizeX);
        final int startY = -Math.round(pageSliceBounds.top * drawSizeY);

        // The colour the page is drawn on. MuPDF is handed the background colour when the text is
        // recoloured without the "customize" option, and white otherwise.
        int background = WHITE;
        boolean magic = false;
        if (withColors) {
            if (MagicHelper.isNeedMagic()) {
                if (AppState.get().isCustomizeBgAndColors) {
                    magic = true;
                } else {
                    int color = MagicHelper.getBgColor();
                    if (!AppState.get().isDayNotInvert) {
                        color = ~color;
                    }
                    background = opaque(color);
                }
            }
        }

        Bitmap bitmap = null;
        TempHolder.lock.lock();
        try {
            if (isRecycled()) {
                LOG.d("PDFIUM! render skip isRecycled", pageNumber);
                return null;
            }
            bitmap = Bitmap.createBitmap(width, height, AppsConfig.CURRENT_BITMAP_ARGB);
            page.renderPageBitmap(bitmap, startX, startY, drawSizeX, drawSizeY, //
                    true, /* render the annotations the document already carries */
                    false, /* textMask */
                    background, background);
        } catch (Throwable e) {
            LOG.e(e);
            if (bitmap != null) {
                bitmap.recycle();
            }
            return null;
        } finally {
            TempHolder.lock.unlock();
        }

        final boolean contrast = withColors && MagicHelper.isNeedBC;
        if (magic || contrast) {
            final int[] pixels = new int[width * height];
            bitmap.getPixels(pixels, 0, width, 0, 0, width, height);
            if (magic) {
                MagicHelper.udpateColorsMagic(pixels);
            }
            if (contrast) {
                MagicHelper.applyQuickContrastAndBrightness(pixels, width, height);
            }
            bitmap.setPixels(pixels, 0, width, 0, 0, width, height);
        }

        final BitmapRef ref = new BitmapRef(bitmap, 0L);
        if (withColors && AppState.get().isMirrorImage) {
            final Matrix m = new Matrix();
            m.preScale(-1, 1);
            final Bitmap mirrored = Bitmap.createBitmap(bitmap, 0, 0, width, height, m, false);
            bitmap.recycle();
            ref.setBitmap(mirrored);
        }
        return ref;
    }

    private static int opaque(final int color) {
        return Color.argb(255, Color.red(color), Color.green(color), Color.blue(color));
    }

    // ---------------------------------------------------------------- text, links

    @Override
    public TextWord[][] getTextImpl() {
        if (isRecycled()) {
            LOG.d("PDFIUM! getText skip isRecycled");
            return new TextWord[0][0];
        }
        if (!AppsConfig.IS_ENABLE_1_PAGE_SEARCH && pageNumber == 1) {
            LOG.d("skip text for 1 page");
            return new TextWord[0][0];
        }
        TempHolder.lock.lock();
        try {
            final PdfTextPage text = textPage();
            if (text == null) {
                return new TextWord[0][0];
            }
            return PdfiumText.words(text, transform());
        } catch (OutOfMemoryError e) {
            LOG.e(e);
            System.gc();
            return new TextWord[0][0];
        } catch (Throwable e) {
            LOG.e(e);
            return new TextWord[0][0];
        } finally {
            TempHolder.lock.unlock();
        }
    }

    @Override
    public List<PageLink> getPageLinks() {
        if (!AppsConfig.IS_ENABLE_1_PAGE_SEARCH && pageNumber == 1) {
            LOG.d("skip links for 1 page");
            return new ArrayList<PageLink>();
        }
        TempHolder.lock.lock();
        try {
            if (isRecycled()) {
                return new ArrayList<PageLink>();
            }
            return PdfiumLinks.get(page, textPage(), transform());
        } catch (Throwable e) {
            LOG.e(e);
            return new ArrayList<PageLink>();
        } finally {
            TempHolder.lock.unlock();
        }
    }

    @Override
    public int getCharCount() {
        TempHolder.lock.lock();
        try {
            if (isRecycled()) {
                return 0;
            }
            final PdfTextPage text = textPage();
            return text == null ? 0 : Math.max(0, text.textPageCountChars());
        } catch (Throwable e) {
            LOG.e(e);
            return 0;
        } finally {
            TempHolder.lock.unlock();
        }
    }

    @Override
    public String getPageHTML() {
        TempHolder.lock.lock();
        try {
            if (isRecycled()) {
                return "";
            }
            final PdfTextPage text = textPage();
            return text == null ? "" : PdfiumText.html(text);
        } catch (Throwable e) {
            LOG.e(e);
            return "";
        } finally {
            TempHolder.lock.unlock();
        }
    }

    @Override
    public String getPageHTMLWithImages() {
        // PDFium has no way to hand out a page's images, so the export carries the text only.
        return getPageHTML();
    }

    // ---------------------------------------------------------------- annotations

    /**
     * The MuPDF page behind this one, opened on first use. MuPDF owns the annotations: PDFium can
     * draw the ones a document already carries but can neither list nor write them.
     *
     * @return the page, or <code>null</code> when MuPDF could not open the document
     */
    private CodecPage annotations() {
        CodecPage local = annotPage;
        if (local != null) {
            return local;
        }
        synchronized (this) {
            if (annotPage == null) {
                final CodecDocument doc = document.annotations();
                if (doc == null) {
                    return null;
                }
                try {
                    // getPageInner, not getPage: the document's one-page cache is left alone so
                    // two live pages of the same document cannot evict each other.
                    annotPage = doc.getPageInner(pageNumber - 1);
                } catch (Throwable e) {
                    LOG.e(e);
                }
            }
            return annotPage;
        }
    }

    /**
     * @return the MuPDF page when it holds annotation changes PDFium cannot see, else null
     */
    private CodecPage annotatedPage() {
        return document.hasAnnotationChanges() ? annotations() : null;
    }

    @Override
    public List<Annotation> getAnnotationsImpl() {
        final CodecPage annot = annotations();
        if (annot == null) {
            return new ArrayList<Annotation>();
        }
        return annot.getAnnotationsImpl();
    }

    @Override
    public void addAnnotation(final float[] color, final PointF[][] points, final float width, final float alpha) {
        final CodecPage annot = annotations();
        if (annot == null) {
            LOG.d("PDFIUM! addAnnotation skipped, MuPDF could not open the document");
            return;
        }
        annot.addAnnotation(color, points, width, alpha);
        document.onAnnotationAdded();
    }

    @Override
    public void addMarkupAnnotation(final PointF[] quadPoints, final AnnotationType type, final float[] color) {
        final CodecPage annot = annotations();
        if (annot == null) {
            LOG.d("PDFIUM! addMarkupAnnotation skipped, MuPDF could not open the document");
            return;
        }
        annot.addMarkupAnnotation(quadPoints, type, color);
        document.onAnnotationAdded();
    }

    @Override
    public long getPageHandle() {
        // The handle identifies the page an annotation belongs to, and the annotations are
        // MuPDF's, so it is MuPDF's handle that has to be reported.
        final CodecPage annot = annotPage;
        return annot == null ? 0 : annot.getPageHandle();
    }

    // ---------------------------------------------------------------- lifecycle

    @Override
    public void recycle() {
        TempHolder.lock.lock();
        try {
            final PdfTextPage text = textPage;
            textPage = null;
            if (text != null) {
                // PDFium requires a text page to be closed before the page it was loaded from
                text.close();
            }
            final PdfPage local = page;
            page = null;
            if (local != null) {
                LOG.d("PDFIUM! -recycle page", pageNumber);
                local.close();
            }
            final CodecPage annot = annotPage;
            annotPage = null;
            if (annot != null && !annot.isRecycled()) {
                annot.recycle();
            }
        } catch (Throwable e) {
            LOG.e(e);
        } finally {
            TempHolder.lock.unlock();
        }
    }

    @Override
    public boolean isRecycled() {
        return page == null || document.isRecycled();
    }

    @Override
    public String toString() {
        return "PdfiumPage[" + pageNumber + " " + actualWidth + "x" + actualHeight + "]";
    }
}
