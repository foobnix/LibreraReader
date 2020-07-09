package org.ebookdroid.droids.mupdf.codec;

import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.RectF;
import android.util.DisplayMetrics;

import com.foobnix.android.utils.LOG;
import com.foobnix.android.utils.TxtUtils;
import com.foobnix.model.AppState;
import com.foobnix.pdf.info.AppsConfig;
import com.foobnix.pdf.info.model.AnnotationType;
import com.foobnix.pdf.info.model.BookCSS;
import com.foobnix.pdf.info.wrapper.MagicHelper;
import com.foobnix.sys.TempHolder;

import org.ebookdroid.LibreraApp;
import org.ebookdroid.common.bitmaps.BitmapManager;
import org.ebookdroid.common.bitmaps.BitmapRef;
import org.ebookdroid.core.codec.AbstractCodecPage;
import org.ebookdroid.core.codec.Annotation;
import org.ebookdroid.core.codec.PageLink;
import org.ebookdroid.core.codec.PageTextBox;
import org.emdev.utils.LengthUtils;
import org.emdev.utils.MatrixUtils;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

public class MuPdfPage extends AbstractCodecPage {


    final RectF pageBounds;
    final int actualWidth;
    final int actualHeight;
    private final long docHandle;
    MuPdfDocument muPdfDocument;
    private volatile long pageHandle;
    private int pageNumber;

    private MuPdfPage(final long pageHandle, final MuPdfDocument muPdfDocument, int pageNumber) {
        this.pageHandle = pageHandle;
        this.muPdfDocument = muPdfDocument;
        this.docHandle = muPdfDocument.getDocumentHandle();
        this.pageNumber = pageNumber;

        this.pageBounds = getBounds();
        this.actualWidth = (int) pageBounds.width();
        this.actualHeight = (int) pageBounds.height();
    }

    static MuPdfPage createPage(final MuPdfDocument dochandle, final int pageno) {
        TempHolder.lock.lock();
        try {
            LOG.d("MUPDF! +create page", dochandle, pageno);
            final long open = open(dochandle.getDocumentHandle(), pageno);
            return new MuPdfPage(open, dochandle, pageno);
        } finally {
            TempHolder.lock.unlock();
        }
    }

    private static native void getBounds(long dochandle, long handle, float[] bounds);

    private static native int getCharCount(long dochandle, long handle);

    private static native void free(long dochandle, long handle);

    private static native long open(long dochandle, int pageno);

    private static void renderPageSafe(MuPdfDocument dochandle, long pagehandle, int[] viewboxarray, float[] matrixarray, int[] bufferarray, int r, int g, int b) {
        if (dochandle != null && dochandle.getDocumentHandle() != 0 && !dochandle.isRecycled()) {
            renderPage(dochandle.getDocumentHandle(), pagehandle, viewboxarray, matrixarray, bufferarray, r, g, b);
        }
    }

    private static native void renderPage(long dochandle, long pagehandle, int[] viewboxarray, float[] matrixarray, int[] bufferarray, int r, int g, int b);

    private native static TextChar[][][][] text(long docHandle, long pageHandle);

    private native static ArrayList<TextChar> text116(long docHandle, long pageHandle);

    @Override
    public long getPageHandle() {
        return pageHandle;
    }

    @Override
    public int getWidth() {
        return actualWidth;
    }

    @Override
    public int getHeight() {
        return actualHeight;
    }

    @Override
    public BitmapRef renderBitmap(final int width, final int height, final RectF pageSliceBounds, boolean cache) {
        final float[] matrixArray = calculateFz(width, height, pageSliceBounds);
        return render(new Rect(0, 0, width, height), matrixArray, cache);
    }

    @Override
    public BitmapRef renderBitmapSimple(final int width, final int height, final RectF pageSliceBounds) {
        final float[] matrixArray = calculateFz(width, height, pageSliceBounds);
        return renderSimple(new Rect(0, 0, width, height), matrixArray);
    }

    @Override
    public Bitmap renderThumbnail(final int width) {
        return renderThumbnail(width, getWidth(), getHeight());
    }

    @Override
    public Bitmap renderThumbnail(final int width, final int originW, final int originH) {
        final RectF rectF = new RectF(0, 0, 1f, 1f);
        final float k = (float) originH / originW;
        LOG.d("TEST", "Render" + " w" + getWidth() + " H " + getHeight() + " " + k + " " + width * k);
        final BitmapRef renderBitmap = renderBitmap(width, (int) (width * k), rectF, false);
        return renderBitmap.getBitmap();
    }

    private float[] calculateFz(final int width, final int height, final RectF pageSliceBounds) {
        final Matrix matrix = MatrixUtils.get();
        matrix.postScale(width / pageBounds.width(), height / pageBounds.height());
        matrix.postTranslate(-pageSliceBounds.left * width, -pageSliceBounds.top * height);
        matrix.postScale(1 / pageSliceBounds.width(), 1 / pageSliceBounds.height());

        final float[] matrixSource = new float[9];
        matrix.getValues(matrixSource);

        final float[] matrixArray = new float[6];

        matrixArray[0] = matrixSource[0];
        matrixArray[1] = matrixSource[3];
        matrixArray[2] = matrixSource[1];
        matrixArray[3] = matrixSource[4];
        matrixArray[4] = matrixSource[2];
        matrixArray[5] = matrixSource[5];

        return matrixArray;
    }

    @Override
    protected void finalize() throws Throwable {
        try {
            // recycle();
        } finally {
            super.finalize();
        }
    }

    @Override
    public void recycle() {
        try {
            TempHolder.lock.lock();
            if (pageHandle != 0 && muPdfDocument != null && muPdfDocument.getDocumentHandle() != 0 && !muPdfDocument.isRecycled()) {
                LOG.d("MUPDF! -recycle page", docHandle, pageNumber);
                free(docHandle, pageHandle);
            }
        } catch (final Exception e) {
            LOG.e(e);
        } finally {
            pageHandle = 0;
            TempHolder.lock.unlock();
        }
    }

    @Override
    public boolean isRecycled() {
        return pageHandle == 0;
    }

    private RectF getBounds() {
        final float[] box = new float[4];
        TempHolder.lock.lock();
        try {
            getBounds(docHandle, pageHandle, box);
        } finally {
            TempHolder.lock.unlock();
        }

        return new RectF(box[0], box[1], box[2], box[3]);
    }

    public BitmapRef renderSimple(final Rect viewbox, final float[] ctm) {
        TempHolder.lock.lock();
        try {


            if (isRecycled()) {
                throw new RuntimeException("The page has been recycled before: " + this);
            }
            final int[] mRect = new int[4];
            mRect[0] = viewbox.left;
            mRect[1] = viewbox.top;
            mRect[2] = viewbox.right;
            mRect[3] = viewbox.bottom;

            final int width = viewbox.width();
            final int height = viewbox.height();

            final int[] bufferarray = new int[width * height];

            renderPageSafe(muPdfDocument, pageHandle, mRect, ctm, bufferarray, -1, -1, -1);

            final BitmapRef b = BitmapManager.getBitmap("PDF page", width, height, Config.RGB_565);
            b.getBitmap().setPixels(bufferarray, 0, width, 0, 0, width, height);
            return b;
        } finally {
            TempHolder.lock.unlock();
        }
    }

    public BitmapRef render(final Rect viewbox, final float[] ctm, boolean cache) {
        TempHolder.lock.lock();
        try {


            if (isRecycled()) {
                throw new RuntimeException("The page has been recycled before: " + this);
            }
            final int[] mRect = new int[4];
            mRect[0] = viewbox.left;
            mRect[1] = viewbox.top;
            mRect[2] = viewbox.right;
            mRect[3] = viewbox.bottom;

            final int width = viewbox.width();
            final int height = viewbox.height();

            final int[] bufferarray = new int[width * height];

            if (BookCSS.get().isTextFormat()) {
                int color = MagicHelper.getBgColor();
                int r = Color.red(color);
                int g = Color.green(color);
                int b = Color.blue(color);
                renderPageSafe(muPdfDocument, pageHandle, mRect, ctm, bufferarray, r, g, b);

                if (AppState.get().isReplaceWhite && MagicHelper.isNeedMagicSimple()) {
                    MagicHelper.udpateColorsMagicSimple(bufferarray);
                }

            } else if (MagicHelper.isNeedMagic()) {

                if (AppState.get().isCustomizeBgAndColors) {
                    renderPageSafe(muPdfDocument, pageHandle, mRect, ctm, bufferarray, -1, -1, -1);
                    MagicHelper.udpateColorsMagic(bufferarray);
                } else {
                    int color = MagicHelper.getBgColor();
                    if (!AppState.get().isDayNotInvert) {
                        color = ~color;
                    }
                    int r = Color.red(color);
                    int g = Color.green(color);
                    int b = Color.blue(color);
                    renderPageSafe(muPdfDocument, pageHandle, mRect, ctm, bufferarray, r, g, b);

                }
            } else {
                renderPageSafe(muPdfDocument, pageHandle, mRect, ctm, bufferarray, -1, -1, -1);
            }

            if (MagicHelper.isNeedBC) {
                MagicHelper.applyQuickContrastAndBrightness(bufferarray, width, height);
            }


            BitmapRef b;
            if (false) {
                b = BitmapManager.getBitmap("PDF page", width, height, Config.RGB_565);
            } else {
                b = new BitmapRef(Bitmap.createBitmap(width, height, Config.RGB_565), 0l);
            }
            b.getBitmap().setPixels(bufferarray, 0, width, 0, 0, width, height);
            if (AppState.get().isMirrorImage) {
                Matrix m = new Matrix();
                m.preScale(-1, 1);
                Bitmap dst = Bitmap.createBitmap(b.getBitmap(), 0, 0, width, height, m, false);
                b.getBitmap().recycle();
                b.setBitmap(dst);
            }

            return b;
        } finally {
            TempHolder.lock.unlock();
        }
    }

    //private static native boolean renderPageSafeBitmap(long dochandle, long pagehandle, int[] viewboxarray, float[] matrixarray, Bitmap bitmap);

    @Override
    public List<PageLink> getPageLinks() {
        if (pageNumber == 1) {
            LOG.d("skip links for 1 page");
            return new ArrayList<PageLink>();
        }

        TempHolder.lock.lock();
        try {
            return MuPdfLinks.getPageLinks(docHandle, pageHandle, pageBounds);
        } finally {
            TempHolder.lock.unlock();
        }
    }

    @Override
    public int getCharCount() {
        TempHolder.lock.lock();
        try {
            return getCharCount(docHandle, pageHandle);
        } finally {
            TempHolder.lock.unlock();
        }
    }

    private native void addInkAnnotationInternal(long docHandle, long pageHandle, float[] color, PointF[][] arcs, int width, float alpha);

    private native Annotation[] getAnnotationsInternal(long docHandle, long pageHandle);

    private native void addMarkupAnnotationInternal(long docHandle, long pageHandle, PointF[] quadPoints, int type, float color[]);

    private native byte[] getPageAsHtml(long docHandle, long pageHandle, int opts);

    @Override
    public String getPageHTML() {
        LOG.d("getPageAsHtml");
        TempHolder.lock.lock();
        try {
            byte[] pageAsHtml = getPageAsHtml(docHandle, pageHandle, -1);
            String string = new String(pageAsHtml);
            LOG.d("getPageAsHtml", string);
            return string;
        } catch (Exception e) {
            LOG.e(e);
            return "";
        } finally {
            TempHolder.lock.unlock();
        }
    }

    @Override
    public String getPageHTMLWithImages() {
        LOG.d("getPageAsHtml");
        TempHolder.lock.lock();
        try {
            // FZ_STEXT_PRESERVE_LIGATURES = 1,
            // FZ_STEXT_PRESERVE_WHITESPACE = 2,
            // FZ_STEXT_PRESERVE_IMAGES = 4,
            byte[] pageAsHtml = getPageAsHtml(docHandle, pageHandle, 4);
            String string = new String(pageAsHtml);
            LOG.d("getPageAsHtml WithImages", string);
            return string;
        } catch (Exception e) {
            LOG.e(e);
            return "";
        } finally {
            TempHolder.lock.unlock();
        }
    }

    @Override
    public void addMarkupAnnotation(PointF[] quadPoints, AnnotationType type, float color[]) {
        if (quadPoints.length <= 0) {
            LOG.d("addMarkupAnnotation", "skip");
            return;
        }
        LOG.d("addMarkupAnnotation", quadPoints.length, type, color[0], color[1], color[2]);
        TempHolder.lock.lock();
        try {
            addMarkupAnnotationInternal(docHandle, pageHandle, quadPoints, type.ordinal(), color);
        } finally {
            TempHolder.lock.unlock();
        }

    }

    @Override
    public List<Annotation> getAnnotations() {
        TempHolder.lock.lock();
        List<Annotation> result = new ArrayList<Annotation>();
        try {
            Annotation[] list = getAnnotationsInternal(docHandle, pageHandle);

            if (list != null) {
                for (int i = 0; i < list.length; i++) {
                    Annotation a = list[i];
                    update(a);
                    a.setIndex(i);
                    a.setPage(pageNumber);
                    a.setPageHandler(pageHandle);
                    result.add(a);
                    LOG.d("getAnnotation1s", pageNumber, i, "h", pageHandle);
                }
            }
        } finally {
            TempHolder.lock.unlock();
        }
        return result;
    }

    @Override
    public void addAnnotation(float[] color, PointF[][] points, float width, float alpha) {
        LOG.d("addInkAnnotationInternal", color[0], color[1], color[2]);
        TempHolder.lock.lock();
        try {
            addInkAnnotationInternal(docHandle, pageHandle, color, points, (int) width, alpha);
        } finally {
            TempHolder.lock.unlock();
        }
    }

    public TextChar[][][][] text() {
        TempHolder.lock.lock();
        try {
            return text(docHandle, pageHandle);
        } catch (Throwable e) {
            LOG.e(e);
            return null;
        } finally {
            TempHolder.lock.unlock();
        }
    }

    @Override
    public TextWord[][] getText() {
        if (pageNumber == 1) {
            LOG.d("skip text for 1 page");
            return new TextWord[0][0];
        }


        if (LibreraApp.MUPDF_VERSION == AppsConfig.MUPDF_1_11) {
            return getText_111();
        } else {
            return getText_116();
        }

    }

    public TextWord[][] getText_116() {
        List<TextChar> chars = null;

        TempHolder.lock.lock();
        try {
            chars = text116(docHandle, pageHandle);
        } finally {
            TempHolder.lock.unlock();
        }

        LOG.d("text116 size", chars.size());

        if (TxtUtils.isListEmpty(chars)) {
            return new TextWord[0][0];
        }

        ArrayList<TextWord[]> lns = new ArrayList<TextWord[]>();

        ArrayList<TextWord> words = new ArrayList<TextWord>();
        TextWord tw = new TextWord();
        for (TextChar tc : chars) {
            if (tc.c == ' ') {
                //update(tw);
                words.add(tw);
                //LOG.d("text116 add1", tw.w);
                tw = new TextWord();
            } else {
                tw.Add(tc);
            }
        }
        if (tw.w.length() > 0) {
            words.add(tw);
            // LOG.d("text116 add2", tw.w);
        }


        if (words.size() > 0)
            lns.add(words.toArray(new TextWord[words.size()]));


        TextWord[][] res = lns.toArray(new TextWord[lns.size()][]);

        for (TextWord[] lines : res) {
            for (TextWord word : lines) {
                update(word);
            }
        }

        return res;
    }

    public TextWord[][] getText_111() {
        TextChar[][][][] chars = text();
        if (chars == null) {
            return new TextWord[0][0];
        }

        ArrayList<TextWord[]> lns = new ArrayList<TextWord[]>();

        for (TextChar[][][] bl : chars) {

            if (bl == null)
                continue;
            for (TextChar[][] ln : bl) {
                ArrayList<TextWord> words = new ArrayList<TextWord>();
                TextWord word = new TextWord();

                for (TextChar[] sp : ln) {
                    for (TextChar tc : sp) {
                        if (AppState.get().selectingByLetters) {
                            if (tc.c == TxtUtils.NON_BREAKE_SPACE_CHAR) {
                                tc.c = ' ';
                            }
                            words.add(new TextWord(tc));
                            continue;
                        }
                        if (tc.c == ' ') {
                            words.add(new TextWord(tc));
                        }
                        if (tc.c != ' ') {
                            word.Add(tc);
                        } else if (word.w.length() > 0) {
                            words.add(word);
                            word = new TextWord();
                        }
                    }
                }

                if (word.w.length() > 0)
                    words.add(word);

                if (words.size() > 0)
                    lns.add(words.toArray(new TextWord[words.size()]));
            }
        }

        TextWord[][] res = lns.toArray(new TextWord[lns.size()][]);
        for (TextWord[] lines : res) {
            for (TextWord word : lines) {
                update(word);
            }
        }

        return res;
    }


    public void update(TextWord wd) {
        wd.setOriginal(wd);
        update((RectF) wd);
    }

    public void update(RectF wd) {
        wd.left = (wd.left - pageBounds.left) / pageBounds.width();
        wd.top = (wd.top - pageBounds.top) / pageBounds.height();
        wd.right = (wd.right - pageBounds.left) / pageBounds.width();
        wd.bottom = (wd.bottom - pageBounds.top) / pageBounds.height();
    }

    private void udpateSearchResult(final List<PageTextBox> rects) {
        if (LengthUtils.isNotEmpty(rects)) {
            final Set<String> temp = new HashSet<String>();
            final Iterator<PageTextBox> iter = rects.iterator();
            while (iter.hasNext()) {
                final PageTextBox b = iter.next();
                if (temp.add(b.toString())) {
                    b.left = (b.left - pageBounds.left) / pageBounds.width();
                    b.top = (b.top - pageBounds.top) / pageBounds.height();
                    b.right = (b.right - pageBounds.left) / pageBounds.width();
                    b.bottom = (b.bottom - pageBounds.top) / pageBounds.height();
                } else {
                    iter.remove();
                }
            }
        }
    }
}
