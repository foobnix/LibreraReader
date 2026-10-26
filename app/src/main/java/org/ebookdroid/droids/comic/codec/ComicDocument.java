package org.ebookdroid.droids.comic.codec;

import android.graphics.BitmapFactory;
import android.graphics.RectF;

import com.foobnix.android.utils.LOG;
import com.foobnix.ext.NativeArchive;

import org.ebookdroid.BookType;
import org.ebookdroid.core.codec.AbstractCodecDocument;
import org.ebookdroid.core.codec.CodecPage;
import org.ebookdroid.core.codec.CodecPageInfo;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicLong;

/**
 * A comic archive - a .cbz or a .cbr - the images inside it, in reading order, one per page. Both
 * are read with libarchive in place ({@link NativeArchive}), so neither is extracted or repacked
 * first. See {@link ComicContext}.
 */
public class ComicDocument extends AbstractCodecDocument {

    /**
     * There is no native document behind this one, and Librera only uses the handle to tell
     * documents apart, so a counter stands in for it. It must not be 0: that means "recycled".
     */
    private static final AtomicLong HANDLES = new AtomicLong(1);

    /** What Android's BitmapFactory can decode, which is everything a comic is packed with. */
    private static final List<String> IMAGES =
            BookType.list(".jpg", ".jpeg", ".png", ".webp", ".gif", ".bmp", ".jfif", ".avif", ".heic", ".heif");

    private final String fname;
    private final NativeArchive archive;
    /** The entries of the archive that are pages, by their index in the archive, in page order. */
    private final List<Integer> pages;

    /** Page sizes, read off each image's header the first time that page is asked about. */
    private final CodecPageInfo[] pageInfo;

    private ComicDocument(final ComicContext context, final String fname, final NativeArchive archive,
                          final List<Integer> pages) {
        super(context, HANDLES.incrementAndGet());
        this.fname = fname;
        this.archive = archive;
        this.pages = pages;
        this.pageInfo = new CodecPageInfo[pages.size()];
    }

    /**
     * Opens a .cbz or a .cbr - whatever the name says, a zip or a RAR of images.
     *
     * @return the document, or <code>null</code> when the archive cannot be read or holds no
     *         image at all - which is what tells the reader to report the file as broken
     */
    static ComicDocument open(final ComicContext context, final String fname) {
        try {
            final NativeArchive archive = NativeArchive.open(fname);
            if (archive == null) {
                return null;
            }
            final List<Integer> pages = readPages(archive);
            if (pages.isEmpty()) {
                LOG.d("COMIC! no images in", fname);
                archive.close();
                return null;
            }
            LOG.d("COMIC! >>> open [document]", fname, pages.size(), "pages");
            return new ComicDocument(context, fname, archive, pages);
        } catch (Throwable e) {
            LOG.e(e, fname);
            return null;
        }
    }

    /**
     * The archive's images in the order they are to be read, which for a comic is the order of
     * their names - the only ordering a comic archive carries.
     */
    private static List<Integer> readPages(final NativeArchive archive) {
        final List<String> names = archive.getNames();
        final List<Integer> pages = new ArrayList<Integer>();
        for (int i = 0; i < names.size(); i++) {
            // only a file has a name here
            if (isImage(names.get(i))) {
                pages.add(i);
            }
        }
        Collections.sort(pages, new Comparator<Integer>() {
            @Override
            public int compare(final Integer a, final Integer b) {
                return naturalCompare(names.get(a), names.get(b));
            }
        });
        return pages;
    }

    public static boolean isImage(final String name) {
        if (name == null) {
            return false;
        }
        final String lower = name.toLowerCase(Locale.US);
        // The resource forks a Mac puts in a zip are copies of the images, under the same names
        for (final String part : lower.split("/")) {
            if (part.startsWith("._") || "__macosx".equals(part)) {
                return false;
            }
        }
        for (final String ext : IMAGES) {
            if (lower.endsWith(ext)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Compares names the way a reader expects pages to follow one another: digits as numbers, so
     * {@code page9} comes before {@code page10}, which a plain string sort gets wrong and most
     * comics are numbered without leading zeros.
     */
    public static int naturalCompare(final String a, final String b) {
        int i = 0;
        int j = 0;
        while (i < a.length() && j < b.length()) {
            final char ca = a.charAt(i);
            final char cb = b.charAt(j);
            if (Character.isDigit(ca) && Character.isDigit(cb)) {
                // Compare the whole runs of digits as one number each, leading zeros ignored
                final int startA = i;
                final int startB = j;
                while (i < a.length() && Character.isDigit(a.charAt(i))) {
                    i++;
                }
                while (j < b.length() && Character.isDigit(b.charAt(j))) {
                    j++;
                }
                final String numA = trimZeros(a.substring(startA, i));
                final String numB = trimZeros(b.substring(startB, j));
                if (numA.length() != numB.length()) {
                    return numA.length() - numB.length();
                }
                final int diff = numA.compareTo(numB);
                if (diff != 0) {
                    return diff;
                }
            } else {
                final char la = Character.toLowerCase(ca);
                final char lb = Character.toLowerCase(cb);
                if (la != lb) {
                    return la - lb;
                }
                i++;
                j++;
            }
        }
        return (a.length() - i) - (b.length() - j);
    }

    private static String trimZeros(final String digits) {
        int k = 0;
        while (k < digits.length() - 1 && digits.charAt(k) == '0') {
            k++;
        }
        return digits.substring(k);
    }

    // ---------------------------------------------------------------- pages

    @Override
    public int getPageCount() {
        return pages.size();
    }

    @Override
    public int getPageCount(final int w, final int h, final int fsize) {
        // Comics do not reflow: the page count is the number of images, whatever the screen is
        return getPageCount();
    }

    @Override
    public CodecPage getPageInner(final int pageNumber) {
        if (pageNumber < 0 || pageNumber >= pages.size()) {
            return null;
        }
        return new ComicPage(this, fname, pageNumber);
    }

    @Override
    public CodecPageInfo getUnifiedPageInfo() {
        // The pages of a comic are whatever size each image is; there is no one size for all
        return null;
    }

    @Override
    public CodecPageInfo getPageInfo(final int pageNumber) {
        if (pageNumber < 0 || pageNumber >= pages.size()) {
            return null;
        }
        CodecPageInfo info = pageInfo[pageNumber];
        if (info != null) {
            return info;
        }
        info = readPageInfo(pageNumber);
        pageInfo[pageNumber] = info;
        return info;
    }

    /**
     * The size of a page, read from the image's own header: inJustDecodeBounds means the pixels
     * are never decoded, so this costs one read of the entry and no memory.
     */
    private CodecPageInfo readPageInfo(final int pageNumber) {
        final byte[] bytes = read(pageNumber);
        final CodecPageInfo info = new CodecPageInfo();
        if (bytes == null) {
            return info;
        }
        final BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeByteArray(bytes, 0, bytes.length, options);
        info.width = Math.max(0, options.outWidth);
        info.height = Math.max(0, options.outHeight);
        return info;
    }

    /**
     * Reads one image out of the archive.
     *
     * The whole entry is read into memory, which is what an archive of compressed images gives
     * anyway - there is no seeking inside a compressed entry - and a comic page is a few hundred
     * kilobytes. Synchronized because one archive is read one entry at a time.
     *
     * @return the image's bytes, or <code>null</code> when the entry cannot be read
     */
    synchronized byte[] read(final int pageNumber) {
        if (isRecycled() || pageNumber < 0 || pageNumber >= pages.size()) {
            return null;
        }
        final byte[] bytes = archive.read(pages.get(pageNumber));
        if (bytes == null) {
            LOG.d("COMIC! cannot read page", fname, pageNumber);
        }
        return bytes;
    }

    // ---------------------------------------------------------------- the rest of the contract

    @Override
    public List<? extends RectF> searchText(final int pageNumber, final String pattern) throws DocSearchNotSupported {
        // There is no text in a comic to search, which is what MuPDF answers for a .cbz as well
        throw new DocSearchNotSupported();
    }

    @Override
    public BookType getBookType() {
        return BookType.CBZ;
    }

    @Override
    public String documentToHtml() {
        return "";
    }

    @Override
    public String getMeta(final String option) {
        return "";
    }

    @Override
    public void setMeta(final String key, final String value) {
    }

    @Override
    public boolean hasChanges() {
        return false;
    }

    @Override
    public void deleteAnnotation(final long pageNumber, final int index) {
    }

    @Override
    public void saveAnnotations(final String path) {
    }

    @Override
    protected void freeDocument() {
        archive.close();
    }
}
