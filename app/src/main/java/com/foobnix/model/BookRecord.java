package com.foobnix.model;

import com.foobnix.android.utils.LOG;

import org.librera.LinkedJSONObject;

/**
 * YuriReader Unified Book Record Entity (Phase 2)
 *
 * Replaces scattered JSON persistence (SharedBooks/AppBook, BookCSS per-book, etc.)
 * with a single SQLite table per book.
 *
 * Key fields:
 * - bookKey: stable identifier (path for now, hash later)
 * - currentPageIndex: int - reliable page-level progress (replaces float percent)
 * - progressPercent: float 0..1 - for backwards compatibility / external display
 * - bookmarksJson: JSON array [{pageIndex, title, addedTime}]
 * - annotationsJson: JSON array [{pageIndex, text, note, color, addedTime}]
 * - styleSettingsJson: JSON object {font, size, lineSpacing, theme, etc.}
 * - contentFingerprint: file hash or metadata hash to detect file changes
 */
public class BookRecord {

    public static final int LOCK_NONE = AppBook.LOCK_NONE;
    public static final int LOCK_YES = AppBook.LOCK_YES;
    public static final int LOCK_NOT = AppBook.LOCK_NOT;

    /** Stable book identifier. Initially same as file path. */
    private String bookKey;

    /** Last time the user read this book (epoch millis) */
    private long lastReadTime;

    /** Reliable page index (0-based). Nullable if not yet set. */
    private int currentPageIndex;

    /** Float percent 0..1 for backwards compatibility and display */
    private float progressPercent;

    /** Total page count at last save time */
    private int totalPages;

    /** Render flags packed as JSON: crop, double, split, coverAlone, rtl, lock */
    private String renderFlagsJson;

    /** Zoom level (1.0 = 100%) */
    private float zoom = 1.0f;

    /** Page offset X */
    private float offsetX;
    /** Page offset Y */
    private float offsetY;

    /** Auto scroll speed */
    private int scrollSpeed;

    /** Bookmarks as JSON array string */
    private String bookmarksJson;
    /** Annotations/highlights as JSON array string */
    private String annotationsJson;
    private String styleSettingsJson;
    private String paragraphConfigJson;
    /** Content fingerprint for change detection (hash of first/last N bytes + size) */
    private String contentFingerprint;

    /** When this record was first created (epoch millis) */
    private long addedTime;

    public BookRecord() {
    }

    /**
     * Convenience constructor from path.
     */
    public BookRecord(String bookKey) {
        this.bookKey = bookKey;
        this.addedTime = System.currentTimeMillis();
    }

    /**
     * Construct from legacy AppBook (for migration / transition layer).
     */
    public static BookRecord fromAppBook(AppBook appBook, String bookKey) {
        BookRecord rec = new BookRecord(bookKey);
        if (appBook == null) {
            return rec;
        }
        rec.progressPercent = appBook.p;
        rec.lastReadTime = appBook.t;
        rec.zoom = appBook.getZoom();
        rec.offsetX = appBook.x;
        rec.offsetY = appBook.y;
        rec.scrollSpeed = appBook.s;

        LinkedJSONObject flags = new LinkedJSONObject();
        try {
            flags.put("splitPages", appBook.sp);
            flags.put("cropPages", appBook.cp);
            flags.put("doublePages", appBook.dp);
            flags.put("doubleCoverAlone", appBook.dc);
            flags.put("rtl", appBook.rtl);
            flags.put("lock", appBook.lk);
        } catch (Exception e) {
            LOG.e(e);
        }
        rec.renderFlagsJson = flags.toString();
        return rec;
    }

    /**
     * Convert this BookRecord back to legacy AppBook (for transition layer).
     */
    public AppBook toAppBook() {
        AppBook book = new AppBook(bookKey);
        book.p = progressPercent;
        book.t = lastReadTime;
        book.setZoom(zoom);
        book.x = offsetX;
        book.y = offsetY;
        book.s = scrollSpeed;

        if (renderFlagsJson != null) {
            try {
                LinkedJSONObject jo = new LinkedJSONObject(renderFlagsJson);
                book.sp = jo.optBoolean("splitPages", false);
                book.cp = jo.optBoolean("cropPages", false);
                book.dp = jo.optBoolean("doublePages", false);
                book.dc = jo.optBoolean("doubleCoverAlone", false);
                book.rtl = jo.optBoolean("rtl", false);
                book.lk = jo.optInt("lock", LOCK_NONE);
            } catch (Exception e) {
                LOG.e(e);
            }
        }
        return book;
    }

    // --- Getters / Setters ---

    public String getBookKey() { return bookKey; }
    public void setBookKey(String bookKey) { this.bookKey = bookKey; }

    public long getLastReadTime() { return lastReadTime; }
    public void setLastReadTime(long lastReadTime) { this.lastReadTime = lastReadTime; }

    public int getCurrentPageIndex() { return currentPageIndex; }
    public void setCurrentPageIndex(int currentPageIndex) { this.currentPageIndex = currentPageIndex; }

    public float getProgressPercent() { return progressPercent; }
    public void setProgressPercent(float progressPercent) { this.progressPercent = progressPercent; }

    public int getTotalPages() { return totalPages; }
    public void setTotalPages(int totalPages) { this.totalPages = totalPages; }

    public String getRenderFlagsJson() { return renderFlagsJson; }
    public void setRenderFlagsJson(String renderFlagsJson) { this.renderFlagsJson = renderFlagsJson; }

    public float getZoom() { return zoom; }
    public void setZoom(float zoom) { this.zoom = zoom; }

    public float getOffsetX() { return offsetX; }
    public void setOffsetX(float offsetX) { this.offsetX = offsetX; }

    public float getOffsetY() { return offsetY; }
    public void setOffsetY(float offsetY) { this.offsetY = offsetY; }

    public int getScrollSpeed() { return scrollSpeed; }
    public void setScrollSpeed(int scrollSpeed) { this.scrollSpeed = scrollSpeed; }

    public String getBookmarksJson() { return bookmarksJson; }
    public void setBookmarksJson(String bookmarksJson) { this.bookmarksJson = bookmarksJson; }

    public String getAnnotationsJson() { return annotationsJson; }
    public void setAnnotationsJson(String annotationsJson) { this.annotationsJson = annotationsJson; }

    public String getStyleSettingsJson() { return styleSettingsJson; }
    public void setStyleSettingsJson(String styleSettingsJson) { this.styleSettingsJson = styleSettingsJson; }

    public String getParagraphConfigJson() { return paragraphConfigJson; }
    public void setParagraphConfigJson(String paragraphConfigJson) { this.paragraphConfigJson = paragraphConfigJson; }

    public String getContentFingerprint() { return contentFingerprint; }
    public void setContentFingerprint(String contentFingerprint) { this.contentFingerprint = contentFingerprint; }

    public long getAddedTime() { return addedTime; }
    public void setAddedTime(long addedTime) { this.addedTime = addedTime; }
}