package org.ebookdroid.droids.foliate.codec;

import android.graphics.RectF;
import com.foobnix.android.utils.Dips;
import com.foobnix.android.utils.LOG;
import com.foobnix.android.utils.TxtUtils;
import com.foobnix.LibreraApp;

import org.ebookdroid.BookType;
import org.ebookdroid.core.codec.AbstractCodecDocument;
import org.ebookdroid.core.codec.BookmarkText;
import org.ebookdroid.core.codec.CodecContext;
import org.ebookdroid.core.codec.CodecPage;
import org.ebookdroid.core.codec.CodecPageInfo;
import org.ebookdroid.core.codec.OutlineLink;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

/**
 * A reflowable book laid out by foliate-js in a WebView.
 *
 * It answers the same contract as the MuPDF document for a text format: a page count that depends
 * on the viewport and the font size, one page size for every page, an outline, the page text, and
 * the bookmark anchor that restores a reading position after a re-layout. The book it is handed is
 * whatever the format context prepared - the converted, hyphenated, footnote-inlined {@code .epub}
 * from {@link org.ebookdroid.droids.EpubContext} and friends - so none of that pipeline changes.
 *
 * Everything that talks to the WebView goes through {@link FoliateRenderer}, which refuses to block
 * a caller that is already on the main thread. Methods here degrade to a miss in that case rather
 * than hanging; see that class for why.
 */
public class FoliateDocument extends AbstractCodecDocument {

    /** foliate hands out no handle of its own; Librera only uses one to tell documents apart. */
    private static final AtomicLong HANDLES = new AtomicLong(1);

    private final String bookPath;

    private final BookType bookType;

    private volatile FoliateRenderer renderer;

    private volatile int sections = -1;

    /** The page size the index was built at, and the index itself. */
    private volatile int width;

    private volatile int height;

    private volatile FoliateIndex index;

    /** Which page the WebView is currently showing, so a repeat render costs nothing. */
    private volatile int positioned = -1;

    /** How much text the page it is on holds, so a blank drawing can be told from a blank page. */
    private volatile int positionedChars;

    private volatile List<OutlineLink> outline;

    public FoliateDocument(final CodecContext context, final String bookPath) {
        super(context, HANDLES.incrementAndGet());
        this.bookPath = bookPath;
        this.bookType = BookType.getByUri(bookPath);
        this.cacheFilename = bookPath;
    }

    public String getPath() {
        return bookPath;
    }

    /**
     * Creates the WebView, loads the shell and opens the book - everything except the page index,
     * which waits for the viewport.
     *
     * @return <code>false</code> when foliate could not open the book, so the caller can fall back
     *         to MuPDF while the format context's own state is still intact
     */
    public boolean openEngine() {
        if (!FoliateRenderer.canWait()) {
            LOG.d("FOLIATE! refusing to open on the main thread");
            return false;
        }
        final FoliateRenderer local = new FoliateRenderer(LibreraApp.context, bookPath);
        final int count = local.open(Dips.screenWidth(), Dips.screenHeight());
        if (count <= 0) {
            LOG.d("FOLIATE! could not open", bookPath);
            local.destroy();
            return false;
        }
        renderer = local;
        sections = count;
        width = Dips.screenWidth();
        height = Dips.screenHeight();
        LOG.d("FOLIATE! >>> open [document]", bookPath, count, "sections");
        return true;
    }

    /** Throws the WebView away without recycling the context, for the fall-back above. */
    public void disposeEngine() {
        final FoliateRenderer local = renderer;
        renderer = null;
        if (local != null) {
            local.destroy();
        }
    }

    // ---------------------------------------------------------------- pages

    /**
     * The page index for a viewport, from the cache when it is there and by walking the book's
     * sections when it is not.
     */
    private synchronized FoliateIndex index(final int w, final int h) {
        final FoliateRenderer local = renderer;
        if (local == null || !local.isUsable()) {
            return null;
        }
        final int pageW = w > 0 ? w : Dips.screenWidth();
        final int pageH = h > 0 ? h : Dips.screenHeight();
        if (index != null && pageW == width && pageH == height) {
            return index;
        }

        final String key = FoliateIndex.key(bookPath, pageW, pageH, FoliateCss.rootFontSizePx(),
                FoliateCss.after(bookPath));
        FoliateIndex loaded = FoliateIndex.load(key);
        if (loaded == null) {
            if (!local.resize(pageW, pageH)) {
                return index;
            }
            final long started = System.currentTimeMillis();
            loaded = FoliateIndex.of(local.index());
            if (loaded == null) {
                LOG.d("FOLIATE! could not index", bookPath);
                return index;
            }
            LOG.d("FOLIATE! indexed", loaded.getSectionCount(), "sections",
                    loaded.getTotal(), "pages in", (System.currentTimeMillis() - started) + "ms");
            loaded.save(key);
        } else if (!local.resize(pageW, pageH)) {
            return index;
        }

        width = pageW;
        height = pageH;
        index = loaded;
        // A re-columnised book is no longer on the page it was
        invalidatePosition();
        return loaded;
    }

    @Override
    public int getPageCount(final int w, final int h, final int fsize) {
        final FoliateIndex local = index(w, h);
        return local == null ? 0 : local.getTotal();
    }

    @Override
    public int getPageCount() {
        // As MuPdfDocument.getW()/getH() do: before a viewport is known, the screen stands in.
        // Fb2Context asks this of a fresh document as a "does this parse?" probe.
        return getPageCount(width > 0 ? width : Dips.screenWidth(),
                height > 0 ? height : Dips.screenHeight(), 0);
    }

    @Override
    public CodecPageInfo getUnifiedPageInfo() {
        // Every page of a reflowable book is the viewport, which is what makes it reflowable
        return new CodecPageInfo(getW(), getH());
    }

    @Override
    public CodecPageInfo getPageInfo(final int pageNumber) {
        // Not null: ImageExtractor derives a page bitmap's height from this one's aspect ratio
        return new CodecPageInfo(getW(), getH());
    }

    int getW() {
        return width > 0 ? width : Dips.screenWidth();
    }

    int getH() {
        return height > 0 ? height : Dips.screenHeight();
    }

    @Override
    public CodecPage getPageInner(final int pageNumber) {
        final FoliateIndex local = index(width, height);
        if (local == null) {
            return null;
        }
        return new FoliatePage(this, pageNumber);
    }

    @Override
    public BookType getBookType() {
        return bookType;
    }

    // ---------------------------------------------------------------- positioning

    /**
     * Puts the book on an absolute 0-based page, ready to be drawn or read from.
     *
     * The page the WebView already shows is remembered, so rendering a page in tiles, then reading
     * its words and its links, costs one layout rather than four.
     */
    synchronized boolean position(final int page) {
        final FoliateRenderer local = renderer;
        final FoliateIndex local$index = index;
        if (local == null || local$index == null) {
            return false;
        }
        if (positioned == page) {
            return true;
        }
        final int section = local$index.sectionOf(page);
        final float fraction = local$index.fractionOf(page);
        final int chars = local.goTo(section, fraction);
        if (chars < 0) {
            positioned = -1;
            positionedChars = 0;
            return false;
        }
        positioned = page;
        positionedChars = chars;
        return true;
    }

    /** How much text the page the book is on holds; 0 when it is genuinely blank. */
    int getPositionedChars() {
        return positionedChars;
    }

    /** Forgets where the book is, after anything that could have moved or re-laid it out. */
    synchronized void invalidatePosition() {
        positioned = -1;
    }

    FoliateRenderer getRenderer() {
        return renderer;
    }

    /** The absolute 0-based page a section and fraction land on; -1 while there is no index. */
    int pageOf(final int section, final double fraction) {
        final FoliateIndex local = index;
        return local == null ? -1 : local.pageOf(section, fraction);
    }

    // ---------------------------------------------------------------- outline

    @Override
    public synchronized List<OutlineLink> getOutline() {
        if (outline != null) {
            return outline;
        }
        final FoliateIndex local$index = index(width, height);
        final FoliateRenderer local = renderer;
        if (local == null || local$index == null) {
            return Collections.emptyList();
        }
        final String json = local.toc();
        if (json == null) {
            return Collections.emptyList();
        }
        final List<OutlineLink> result = new ArrayList<OutlineLink>();
        try {
            final JSONArray entries = new JSONObject(json).getJSONArray("toc");
            for (int i = 0; i < entries.length(); i++) {
                final JSONObject entry = entries.getJSONObject(i);
                final String title = entry.optString("title");
                if (TxtUtils.isEmpty(title)) {
                    continue;
                }
                final int page = local$index.pageOf(entry.optInt("index", 0),
                        entry.optDouble("fraction", 0)) + 1;
                // The link must be "#<page>" and must not be "#0": anything else sends the TOC
                // panel to MuPdfLinks.getLinkPageWrapper, a MuPDF native, with this document's
                // handle - which is not a MuPDF document.
                // linkUri must carry the href: OutlineHelper.getCurrentChapterFile reads it to
                // name the current chapter, and the footnote lookup keys on that name.
                result.add(new OutlineLink(title, "#" + Math.max(1, page), entry.optInt("level", 0),
                        getDocumentHandle(), entry.optString("href")));
            }
        } catch (Throwable e) {
            LOG.e(e);
        }
        LOG.d("FOLIATE! outline", result.size());
        outline = result;
        return result;
    }

    // ---------------------------------------------------------------- bookmarks

    /**
     * The CFI of the page the reader is on.
     *
     * Positioning the book to read it would be wrong - the reader is already there, and moving
     * the one WebView would move what everything else is about to draw - so this reports where
     * the book actually is and says nothing when that is not the page being asked about.
     */
    @Override
    public String getLocation(final int page) {
        if (!FoliateRenderer.canWait()) {
            // The periodic save asks for this on the main thread; a miss costs the exact place
            // for this save, and the percentage beside it is unaffected
            LOG.d("FOLIATE! getLocation skipped on the main thread");
            return null;
        }
        final FoliateRenderer local = renderer;
        if (local == null || isRecycled()) {
            return null;
        }
        synchronized (this) {
            if (index(width, height) == null || !position(page - 1)) {
                return null;
            }
            return local.location();
        }
    }

    /**
     * A CFI for the characters under a selection on one page.
     *
     * The page is put back on screen first, as every other read of a page does, because the
     * browser can only be asked what is at a point on the page it is actually showing.
     */
    @Override
    public String getLocation(final int page, final List<RectF> rects) {
        if (rects == null || rects.isEmpty()) {
            return null;
        }
        if (!FoliateRenderer.canWait()) {
            LOG.d("FOLIATE! getLocation(rects) skipped on the main thread");
            return null;
        }
        final FoliateRenderer local = renderer;
        if (local == null || isRecycled()) {
            return null;
        }
        synchronized (this) {
            if (index(width, height) == null || !position(page - 1)) {
                return null;
            }
            return local.cfiAt(rects);
        }
    }

    /**
     * The page a CFI points at.
     *
     * @return the 1-based page, or -1 when the CFI is empty, is not a CFI, or no longer resolves
     *         in this book
     */
    @Override
    public int findLocationPage(final String location) {
        if (TxtUtils.isEmpty(location)) {
            return -1;
        }
        if (!FoliateRenderer.canWait()) {
            // AbstractViewController.show() asks on the UI thread; the percentage covers it
            LOG.d("FOLIATE! findLocationPage skipped on the main thread");
            return -1;
        }
        final FoliateRenderer local = renderer;
        if (local == null || isRecycled()) {
            return -1;
        }
        synchronized (this) {
            final FoliateIndex local$index = index(width, height);
            if (local$index == null) {
                return -1;
            }
            final double[] target = local.resolve(location);
            if (target == null) {
                LOG.d("FOLIATE! the location means nothing in this book", location);
                return -1;
            }
            // The book has been moved to it, so the cached position has to say so
            final int page = local$index.pageOf((int) target[0], target[1]);
            positioned = page;
            positionedChars = 0;
            return page + 1;
        }
    }

    @Override
    public String getBookmarkText(final int page) {
        if (!FoliateRenderer.canWait()) {
            // The periodic save asks for this on the main thread; a miss only costs the hint
            LOG.d("FOLIATE! getBookmarkText skipped on the main thread");
            return null;
        }
        return BookmarkText.of(pageText(page));
    }

    /**
     * The page a bookmark was made on, looked for around the page its percentage points at, the
     * bookmark's own text winning over the page text.
     *
     * This is what carries a reading position across a re-layout - and across a switch between
     * this engine and MuPDF, which paginate differently.
     *
     * @return the 1-based page, or -1
     */
    @Override
    public int findBookmarkPage(final int page, final String text, final String pageText) {
        if (!FoliateRenderer.canWait()) {
            // AbstractViewController.show() asks for this on the UI thread
            LOG.d("FOLIATE! findBookmarkPage skipped on the main thread");
            return -1;
        }
        final String needle1 = BookmarkText.needle(text);
        final String needle2 = BookmarkText.needle(pageText);
        if (needle1 == null && needle2 == null) {
            return -1;
        }
        final int count = getPageCount();
        int found2 = -1;
        for (int i = 0; i <= 2 * BookmarkText.RANGE && (needle1 != null || found2 == -1); i++) {
            if (isRecycled()) {
                return -1;
            }
            final int candidate = BookmarkText.candidate(page, i);
            if (candidate < 1 || candidate > count) {
                continue;
            }
            final String haystack = BookmarkText.haystack(pageText(candidate));
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
        return found2;
    }

    /** The plain text of a 1-based page. */
    private String pageText(final int page) {
        if (page < 1 || isRecycled()) {
            return null;
        }
        final FoliateRenderer local = renderer;
        if (local == null) {
            return null;
        }
        synchronized (this) {
            if (index(width, height) == null || !position(page - 1)) {
                return null;
            }
            final String json = local.text();
            if (json == null) {
                return null;
            }
            try {
                return new JSONObject(json).optString("text");
            } catch (Throwable e) {
                LOG.e(e);
                return null;
            }
        }
    }

    // ---------------------------------------------------------------- not reached for a book

    @Override
    public void setMeta(final String key, final String value) {
        LOG.d("FOLIATE! setMeta is not supported", key);
    }

    @Override
    public String getMeta(final String option) {
        // Librera reads a reflowable book's metadata out of the archive, not the engine
        return "";
    }

    @Override
    public String documentToHtml() {
        return "";
    }

    @Override
    public List<android.graphics.RectF> searchText(final int pageNuber, final String pattern)
            throws DocSearchNotSupported {
        // Librera searches a page's text itself, as it does with MuPDF
        throw new DocSearchNotSupported();
    }

    @Override
    public boolean hasChanges() {
        return false;
    }

    @Override
    public void saveAnnotations(final String path) {
    }

    @Override
    public void deleteAnnotation(final long pageHandle, final int index) {
    }

    // ---------------------------------------------------------------- lifecycle

    @Override
    protected void freeDocument() {
        disposeEngine();
        index = null;
        outline = null;
        LOG.d("FOLIATE! <<< recycle [document]", bookPath);
    }
}
