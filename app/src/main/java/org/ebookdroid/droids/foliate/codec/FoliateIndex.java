package org.ebookdroid.droids.foliate.codec;

import com.foobnix.android.utils.LOG;
import com.foobnix.ext.CacheZipUtils;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.nio.charset.Charset;

/**
 * How many pages each of a book's sections holds, and the map between an absolute page number and
 * the place in the book that page is.
 *
 * foliate's paginator only knows a section's page count once that section has been laid out, so the
 * only way to a real page count is to walk every section at the page size being read at. That walk
 * is the expensive part of opening a book with this engine, so its result is written next to the
 * book and keyed by everything that could change it - the book, the page size, the font size and
 * the stylesheet. MuPDF does the same thing for the same reason: {@code fz_layout_document} walks
 * the whole book and {@code fz_save_accelerator} caches the result in a {@code +accel} file.
 */
class FoliateIndex {

    private static final Charset UTF8 = Charset.forName("UTF-8");

    /**
     * Bumped when the bridge lays a page out differently for the same settings, which the key
     * cannot see otherwise. 2: the root font size became !important.
     */
    private static final int FORMAT = 2;

    /** Pages in each section, in spine order. At least 1 each, so no section is unreachable. */
    private final int[] sectionPages;

    /** The absolute 0-based page each section starts on. */
    private final int[] sectionStart;

    private final int total;

    private FoliateIndex(final int[] sectionPages) {
        this.sectionPages = sectionPages;
        this.sectionStart = new int[sectionPages.length];
        int running = 0;
        for (int i = 0; i < sectionPages.length; i++) {
            sectionStart[i] = running;
            running += Math.max(1, sectionPages[i]);
        }
        this.total = Math.max(1, running);
    }

    static FoliateIndex of(final int[] sectionPages) {
        if (sectionPages == null || sectionPages.length == 0) {
            return null;
        }
        return new FoliateIndex(sectionPages);
    }

    int getTotal() {
        return total;
    }

    int getSectionCount() {
        return sectionPages.length;
    }

    /** The section an absolute 0-based page falls in. */
    int sectionOf(final int page) {
        final int clamped = Math.max(0, Math.min(total - 1, page));
        // The sections are few and the starts ascending; a binary search keeps it cheap on a book
        // with hundreds of them.
        int low = 0;
        int high = sectionStart.length - 1;
        while (low < high) {
            final int mid = (low + high + 1) >>> 1;
            if (sectionStart[mid] <= clamped) {
                low = mid;
            } else {
                high = mid - 1;
            }
        }
        return low;
    }

    /**
     * Where in its section an absolute page is, as the fraction foliate's {@code goTo} anchor
     * takes: 0 is the section's first page, 1 its last.
     */
    float fractionOf(final int page) {
        final int section = sectionOf(page);
        final int local = Math.max(0, Math.min(sectionPages[section] - 1,
                Math.max(0, Math.min(total - 1, page)) - sectionStart[section]));
        final int pages = Math.max(1, sectionPages[section]);
        return pages == 1 ? 0f : (float) local / (pages - 1);
    }

    /** The absolute 0-based page a section and fraction land on; the inverse of the two above. */
    int pageOf(final int section, final double fraction) {
        if (section < 0 || section >= sectionPages.length) {
            return 0;
        }
        final int pages = Math.max(1, sectionPages[section]);
        final double clamped = Math.max(0, Math.min(1, fraction));
        return sectionStart[section] + (int) Math.round(clamped * (pages - 1));
    }

    // ---------------------------------------------------------------- the cache

    /**
     * The key everything that could change the pagination is folded into. A different key means a
     * different file, so a stale index is never read rather than being overwritten.
     *
     * A book converted into {@link CacheZipUtils#CACHE_BOOK_DIR} is keyed without its date. Its
     * name already says what its content follows from - the source book with its size and date,
     * and the conversion settings - and AbstractCodecContext keeps only the book opened last
     * there, so going back to a book converts it again and gives the copy a new date. Keyed on
     * that date, the index was thrown away with it and the whole book walked again on top of the
     * conversion; the length still tells a different conversion apart.
     */
    static String key(final String bookPath, final int width, final int height, final int fontSizePx,
                      final String css) {
        long length = 0;
        long modified = 0;
        try {
            final File book = new File(bookPath);
            length = book.length();
            modified = isConverted(book) ? 0 : book.lastModified();
        } catch (Throwable e) {
            LOG.e(e);
        }
        return FORMAT + "|" + bookPath + "|" + length + "|" + modified + "|" + width + "x" + height
                + "|" + fontSizePx + "|" + css.hashCode();
    }

    /** Whether a book is a converted copy in the book cache rather than the reader's own file. */
    static boolean isConverted(final File book) {
        final File dir = CacheZipUtils.CACHE_BOOK_DIR;
        final File parent = book.getAbsoluteFile().getParentFile();
        return dir != null && parent != null && parent.equals(dir.getAbsoluteFile());
    }

    /** Where an index with this key lives: beside MuPDF's own accelerator files. */
    static File fileFor(final String key) {
        return new File(CacheZipUtils.CACHE_TEMP, Math.abs(key.hashCode()) + ".foliate.json");
    }

    /**
     * @return the cached index for this key, or <code>null</code> when there is none or it was
     *         written for a different key
     */
    static FoliateIndex load(final String key) {
        final File file = fileFor(key);
        if (!file.isFile()) {
            return null;
        }
        RandomAccessFile raf = null;
        try {
            raf = new RandomAccessFile(file, "r");
            final byte[] bytes = new byte[(int) Math.min(raf.length(), 8 * 1024 * 1024)];
            raf.readFully(bytes);
            final JSONObject json = new JSONObject(new String(bytes, UTF8));
            if (!key.equals(json.optString("key"))) {
                // Two keys can collide in a hash; the key itself is what decides
                LOG.d("FOLIATE! index key mismatch", file);
                return null;
            }
            final JSONArray array = json.getJSONArray("pages");
            final int[] counts = new int[array.length()];
            for (int i = 0; i < counts.length; i++) {
                counts[i] = array.getInt(i);
            }
            LOG.d("FOLIATE! index from cache", file, counts.length, "sections");
            return of(counts);
        } catch (Throwable e) {
            LOG.e(e, file);
            return null;
        } finally {
            if (raf != null) {
                try {
                    raf.close();
                } catch (Throwable e) {
                    LOG.e(e);
                }
            }
        }
    }

    void save(final String key) {
        final File file = fileFor(key);
        OutputStream out = null;
        try {
            final JSONObject json = new JSONObject();
            json.put("key", key);
            final JSONArray array = new JSONArray();
            for (int pages : sectionPages) {
                array.put(pages);
            }
            json.put("pages", array);
            CacheZipUtils.createAllCacheDirs();
            out = new FileOutputStream(file);
            out.write(json.toString().getBytes(UTF8));
            LOG.d("FOLIATE! index cached", file, total, "pages");
        } catch (Throwable e) {
            LOG.e(e, file);
        } finally {
            if (out != null) {
                try {
                    out.close();
                } catch (Throwable e) {
                    LOG.e(e);
                }
            }
        }
    }
}
