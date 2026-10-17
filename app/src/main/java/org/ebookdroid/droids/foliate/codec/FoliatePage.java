package org.ebookdroid.droids.foliate.codec;

import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.graphics.PointF;
import android.graphics.RectF;

import com.foobnix.android.utils.LOG;
import com.foobnix.model.AppState;
import com.foobnix.pdf.info.model.AnnotationType;
import com.foobnix.pdf.info.wrapper.MagicHelper;

import org.ebookdroid.common.bitmaps.BitmapRef;
import org.ebookdroid.core.codec.AbstractCodecPage;
import org.ebookdroid.core.codec.Annotation;
import org.ebookdroid.core.codec.PageLink;
import org.ebookdroid.droids.mupdf.codec.TextWord;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * One page of a {@link FoliateDocument}: the bitmap drawn out of the WebView, and the words and
 * links read out of it.
 *
 * The page is positioned once and then everything about it is read from that one layout - the
 * bitmap is cached whole and sliced in Java, so rendering it in tiles, and then asking for its
 * words and its links, costs one round trip to the WebView rather than four. Librera's own colour
 * settings are applied to the pixels afterwards, the way
 * {@link org.ebookdroid.droids.mupdf.codec.MuPdfPage} applies them, so nothing downstream can tell
 * which engine drew the page.
 */
public class FoliatePage extends AbstractCodecPage {

    private final FoliateDocument document;

    /** 0-based, as the page model counts pages. */
    private final int pageNumber;

    private volatile boolean recycled;

    /** The last full page drawn, kept so the tiles of one page share one layout and one capture. */
    private volatile Bitmap captured;

    private volatile int capturedWidth;

    private volatile int capturedHeight;

    FoliatePage(final FoliateDocument document, final int pageNumber) {
        super(document.getPath());
        this.document = document;
        this.pageNumber = pageNumber;
    }

    @Override
    public int getWidth() {
        return document.getW();
    }

    @Override
    public int getHeight() {
        return document.getH();
    }

    // ---------------------------------------------------------------- rendering

    @Override
    public BitmapRef renderBitmap(final int width, final int height, final RectF pageSliceBounds,
                                  final boolean cache) {
        return render(width, height, pageSliceBounds, true);
    }

    @Override
    public BitmapRef renderBitmapSimple(final int width, final int height, final RectF pageSliceBounds) {
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
        final BitmapRef ref = renderBitmap(width, (int) (width * k), new RectF(0, 0, 1f, 1f), false);
        return ref == null ? null : ref.getBitmap();
    }

    /**
     * Draws a slice of the page at the given size.
     *
     * The page is always laid out at the size it was <i>indexed</i> at, never at the size asked
     * for, and the captured bitmap is scaled instead. That matters: the page count and every page
     * number come from a pagination done at one particular size, so laying the book out at any
     * other size would move the text and page N would no longer hold what it was numbered for.
     * Librera does ask for other sizes - it splits a zoomed page into tiles and asks for each at
     * the zoomed scale, and a thumbnail comes smaller - so the slice is cut from the page at its
     * own size and then scaled. A zoomed page is therefore upscaled rather than re-rendered, and
     * looks softer the further it is zoomed; the text it shows is the right text.
     */
    private BitmapRef render(final int width, final int height, final RectF pageSliceBounds,
                             final boolean withColors) {
        if (width <= 0 || height <= 0 || recycled) {
            return null;
        }
        final int pageWidth = Math.max(1, document.getW());
        final int pageHeight = Math.max(1, document.getH());

        final Bitmap page = fullPage(pageWidth, pageHeight);
        if (page == null) {
            return null;
        }

        Bitmap slice;
        try {
            // The slice, in the page's own pixels
            final int left = Math.max(0, Math.min(pageWidth - 1,
                    Math.round(pageSliceBounds.left * pageWidth)));
            final int top = Math.max(0, Math.min(pageHeight - 1,
                    Math.round(pageSliceBounds.top * pageHeight)));
            final int w = Math.max(1, Math.min(pageWidth - left,
                    Math.round(Math.max(pageSliceBounds.width(), 0.001f) * pageWidth)));
            final int h = Math.max(1, Math.min(pageHeight - top,
                    Math.round(Math.max(pageSliceBounds.height(), 0.001f) * pageHeight)));
            slice = Bitmap.createBitmap(page, left, top, w, h);
            if (slice.getWidth() != width || slice.getHeight() != height) {
                final Bitmap scaled = Bitmap.createScaledBitmap(slice, width, height, true);
                if (scaled != slice) {
                    slice.recycle();
                }
                slice = scaled;
            }
        } catch (Throwable e) {
            LOG.e(e);
            return null;
        }

        if (withColors) {
            applyColors(slice);
        }

        final BitmapRef ref = new BitmapRef(slice, 0L);
        if (withColors && AppState.get().isMirrorImage) {
            final Matrix m = new Matrix();
            m.preScale(-1, 1);
            final Bitmap mirrored = Bitmap.createBitmap(slice, 0, 0, slice.getWidth(), slice.getHeight(), m, false);
            slice.recycle();
            ref.setBitmap(mirrored);
        }
        return ref;
    }

    /**
     * The whole page at the size the book is paginated at, drawn if it is not already the one in
     * hand. The cache is what makes a zoomed page cheap: Librera asks for the same page once per
     * tile, and all of those are cut from this one capture.
     */
    private synchronized Bitmap fullPage(final int width, final int height) {
        if (captured != null && !captured.isRecycled()
                && capturedWidth == width && capturedHeight == height) {
            return captured;
        }
        final FoliateRenderer renderer = document.getRenderer();
        if (renderer == null || !renderer.isUsable()) {
            return null;
        }
        final Bitmap bitmap;
        // Positioning and drawing are one operation. There is one book in one WebView, so
        // whatever moved it last is what gets drawn - and other threads do move it: TTS reads a
        // page's text, the search walks the pages, the reader asks for the next page's links. A
        // gap between the two is a page drawn somewhere else in the book, or part way through
        // being laid out, which is a blank page. Everything else that positions the book holds
        // this same monitor.
        synchronized (document) {
            if (!renderer.resize(width, height)) {
                return null;
            }
            if (!document.position(pageNumber)) {
                return null;
            }
            Bitmap drawn = renderer.capture(width, height);
            // A page that draws as nothing at all was drawn before the WebView had finished
            // laying it out - the view is parked out of sight, where frames are throttled, so
            // what "finished" means is not always what it says. One more go.
            //
            // The second go used to be kept for a page the engine said had text on it, so that
            // the end of a chapter was not redrawn for nothing. That reads the count as evidence
            // the count cannot give: the throttling that makes a capture early is the same
            // throttling that leaves the count reading 0, so the pages that most needed redrawing
            // were exactly the ones that never got it - a whole grid of thumbnails came back
            // blank. A genuinely blank page now pays for one extra draw and is still blank.
            if (drawn != null && isBlank(drawn)) {
                LOG.d("FOLIATE! page drew blank, once more", pageNumber,
                        document.getPositionedChars());
                final Bitmap again = renderer.redraw(width, height);
                if (again != null) {
                    drawn.recycle();
                    drawn = again;
                }
            }
            bitmap = drawn;
        }
        if (bitmap == null) {
            return null;
        }
        releaseCapture();
        captured = bitmap;
        capturedWidth = width;
        capturedHeight = height;
        return bitmap;
    }

    /**
     * Whether every pixel sampled is the same colour - an empty page. Sampled on a grid rather
     * than read in full: this runs for every page drawn, and one wrong pixel is enough to say no.
     */
    private static boolean isBlank(final Bitmap bitmap) {
        final int step = Math.max(1, Math.min(bitmap.getWidth(), bitmap.getHeight()) / 64);
        final int first = bitmap.getPixel(0, 0);
        for (int y = 0; y < bitmap.getHeight(); y += step) {
            for (int x = 0; x < bitmap.getWidth(); x += step) {
                if (bitmap.getPixel(x, y) != first) {
                    return false;
                }
            }
        }
        return true;
    }

    private synchronized void releaseCapture() {
        final Bitmap old = captured;
        captured = null;
        capturedWidth = 0;
        capturedHeight = 0;
        if (old != null && !old.isRecycled()) {
            old.recycle();
        }
    }

    /**
     * Librera's own colour settings, applied to the drawn pixels exactly as MuPdfPage applies
     * them. The page background itself is already the reader's, set in the WebView.
     */
    private static void applyColors(final Bitmap bitmap) {
        final boolean magic = MagicHelper.isNeedMagic() && AppState.get().isCustomizeBgAndColors;
        final boolean contrast = MagicHelper.isNeedBC;
        if (!magic && !contrast) {
            return;
        }
        try {
            final int w = bitmap.getWidth();
            final int h = bitmap.getHeight();
            final int[] pixels = new int[w * h];
            bitmap.getPixels(pixels, 0, w, 0, 0, w, h);
            if (magic) {
                MagicHelper.udpateColorsMagic(pixels);
            }
            if (contrast) {
                MagicHelper.applyQuickContrastAndBrightness(pixels, w, h);
            }
            bitmap.setPixels(pixels, 0, w, 0, 0, w, h);
        } catch (Throwable e) {
            LOG.e(e);
        }
    }

    // ---------------------------------------------------------------- text and links

    @Override
    public TextWord[][] getTextImpl() {
        final FoliateRenderer renderer = document.getRenderer();
        if (recycled || renderer == null || !renderer.isUsable()) {
            return new TextWord[0][0];
        }
        final String json;
        synchronized (document) {
            if (!document.position(pageNumber)) {
                return new TextWord[0][0];
            }
            json = renderer.words();
        }
        if (json == null) {
            return new TextWord[0][0];
        }
        try {
            final JSONArray words = new JSONObject(json).getJSONArray("words");
            final List<TextWord> line = new ArrayList<TextWord>(words.length());
            for (int i = 0; i < words.length(); i++) {
                final JSONObject word = words.getJSONObject(i);
                final String text = word.optString("w");
                final RectF rect = new RectF(
                        (float) word.optDouble("l", 0), (float) word.optDouble("t", 0),
                        (float) word.optDouble("r", 0), (float) word.optDouble("b", 0));
                // Already normalized to the page in the bridge, where the geometry is known
                if (rect.left < 0 || rect.top < 0) {
                    continue;
                }
                final TextWord tw = new TextWord(text, rect);
                tw.setOriginal(rect);
                line.add(tw);
            }
            if (line.isEmpty()) {
                return new TextWord[0][0];
            }
            final TextWord[][] result = new TextWord[1][];
            result[0] = line.toArray(new TextWord[line.size()]);
            return result;
        } catch (Throwable e) {
            LOG.e(e);
            return new TextWord[0][0];
        }
    }

    @Override
    public List<RectF> getLocationRects(final String location) {
        final List<RectF> result = new ArrayList<RectF>();
        final FoliateRenderer renderer = document.getRenderer();
        if (recycled || renderer == null || !renderer.isUsable()
                || location == null || location.isEmpty()) {
            return result;
        }
        final String json;
        synchronized (document) {
            if (!document.position(pageNumber)) {
                return result;
            }
            json = renderer.rects(location);
        }
        if (json == null) {
            return result;
        }
        try {
            final JSONArray rects = new JSONObject(json).getJSONArray("rects");
            for (int i = 0; i < rects.length(); i++) {
                final JSONObject rect = rects.getJSONObject(i);
                result.add(new RectF(
                        (float) rect.optDouble("l", 0), (float) rect.optDouble("t", 0),
                        (float) rect.optDouble("r", 0), (float) rect.optDouble("b", 0)));
            }
        } catch (Throwable e) {
            LOG.e(e);
        }
        return result;
    }

    @Override
    public List<PageLink> getPageLinks() {
        final List<PageLink> result = new ArrayList<PageLink>();
        final FoliateRenderer renderer = document.getRenderer();
        if (recycled || renderer == null || !renderer.isUsable()) {
            return result;
        }
        final String json;
        synchronized (document) {
            if (!document.position(pageNumber)) {
                return result;
            }
            json = renderer.links();
        }
        if (json == null) {
            return result;
        }
        try {
            final JSONArray links = new JSONObject(json).getJSONArray("links");
            for (int i = 0; i < links.length(); i++) {
                final JSONObject link = links.getJSONObject(i);
                final PageLink page = new PageLink();
                page.sourceRect = new RectF(
                        (float) link.optDouble("l", 0), (float) link.optDouble("t", 0),
                        (float) link.optDouble("r", 0), (float) link.optDouble("b", 0));
                if (link.has("index")) {
                    page.targetPage = document.pageOf(link.optInt("index"), link.optDouble("fraction", 0));
                } else {
                    page.url = link.optString("url");
                    if (page.url == null || page.url.isEmpty()) {
                        continue;
                    }
                }
                result.add(page);
            }
        } catch (Throwable e) {
            LOG.e(e);
        }
        return result;
    }

    @Override
    public String getPageHTML() {
        final FoliateRenderer renderer = document.getRenderer();
        if (recycled || renderer == null || !renderer.isUsable()) {
            return "";
        }
        final String json;
        synchronized (document) {
            if (!document.position(pageNumber)) {
                return "";
            }
            json = renderer.text();
        }
        if (json == null) {
            return "";
        }
        try {
            return new JSONObject(json).optString("html", "");
        } catch (Throwable e) {
            LOG.e(e);
            return "";
        }
    }

    @Override
    public String getPageHTMLWithImages() {
        // Only the "reflow a PDF to text" export asks for this, and that is PDF-only
        return getPageHTML();
    }

    @Override
    public int getCharCount() {
        return 0;
    }

    // ---------------------------------------------------------------- annotations

    @Override
    public List<Annotation> getAnnotationsImpl() {
        // A reflowable book has none: Librera only asks for annotations when the type is PDF
        return new ArrayList<Annotation>();
    }

    @Override
    public void addAnnotation(final float[] color, final PointF[][] points, final float width,
                              final float alpha) {
    }

    @Override
    public void addMarkupAnnotation(final PointF[] quadPoints, final AnnotationType type,
                                    final float[] color) {
    }

    @Override
    public long getPageHandle() {
        return 0;
    }

    // ---------------------------------------------------------------- lifecycle

    @Override
    public void recycle() {
        recycled = true;
        releaseCapture();
    }

    @Override
    public boolean isRecycled() {
        return recycled || document.isRecycled();
    }

    @Override
    public String toString() {
        return "FoliatePage[" + pageNumber + "]";
    }

}
