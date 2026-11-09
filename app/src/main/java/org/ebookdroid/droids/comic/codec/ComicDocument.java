package org.ebookdroid.droids.comic.codec;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import com.foobnix.android.utils.LOG;
import com.foobnix.ext.NativeArchive;
import com.foobnix.ext.NativeTiff;
import com.foobnix.pdf.info.AppsConfig;

import org.ebookdroid.BookType;
import org.ebookdroid.core.codec.CodecPageInfo;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/**
 * A comic archive - a .cbz or a .cbr - the images inside it, in reading order, one per page. Both
 * are read with libarchive in place ({@link NativeArchive}), so neither is extracted or repacked
 * first. See {@link ComicContext}.
 */
public class ComicDocument extends ImageDocument {

    /**
     * The images a comic is packed with: what Android's BitmapFactory decodes, and TIFF, which
     * libtiff does ({@link NativeTiff}).
     */
    private static final List<String> IMAGES =
            BookType.list(".jpg", ".jpeg", ".png", ".webp", ".gif", ".bmp", ".jfif", ".avif", ".heic", ".heif",
                    ".tif", ".tiff");

    private final NativeArchive archive;
    /** The entries of the archive that are pages, by their index in the archive, in page order. */
    private final List<Integer> pages;
    private ComicDocument(final ComicContext context, final String fname, final NativeArchive archive,
                          final List<Integer> pages) {
        super(context, fname);
        this.archive = archive;
        this.pages = pages;
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

    /**
     * The size of a page, read from the image's own header: inJustDecodeBounds means the pixels
     * are never decoded, so this costs one read of the entry and no memory.
     */
    @Override
    protected CodecPageInfo readPageInfo(final int pageNumber) {
        final byte[] bytes = read(pageNumber);
        final CodecPageInfo info = new CodecPageInfo();
        if (bytes == null) {
            return info;
        }
        if (NativeTiff.isTiff(bytes)) {
            final int[] size = NativeTiff.size(bytes);
            if (size != null) {
                info.width = size[0];
                info.height = size[1];
            }
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

    /**
     * Decodes the image no larger than it needs to be: BitmapFactory's inSampleSize halves it as
     * many times as it can while still covering the size asked for, so reading a page of a comic
     * at a page-fitting zoom costs a fraction of the full-resolution decode - and a 4000px scan
     * never has to be held in memory whole. A TIFF page, which BitmapFactory cannot read, is
     * read by libtiff, shrunk the same way.
     */
    @Override
    protected Bitmap decodePage(final int pageNumber, final int sample) {
        final byte[] bytes = read(pageNumber);
        if (bytes == null) {
            return null;
        }
        if (NativeTiff.isTiff(bytes)) {
            return NativeTiff.decode(bytes, sample);
        }
        final BitmapFactory.Options options = new BitmapFactory.Options();
        options.inPreferredConfig = AppsConfig.CURRENT_BITMAP_ARGB;
        options.inSampleSize = sample;
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.length, options);
    }

    @Override
    public BookType getBookType() {
        return BookType.CBZ;
    }

    @Override
    protected void freeDocument() {
        archive.close();
    }
}
