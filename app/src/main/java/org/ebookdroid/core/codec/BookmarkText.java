package org.ebookdroid.core.codec;

/**
 * The text a bookmark keeps of the page it was made on, and the needle that finds that page again.
 *
 * Librera has no CFI: a reading position is a percentage plus a short piece of the page's text
 * ({@link com.foobnix.model.AppBook#p} and {@code pt}). After a re-layout - a font size change, a
 * rotation, another device, another engine - the percentage gives an approximate page and this text
 * is searched around it to correct the guess. So two engines only agree on a position if they agree
 * on how that text is cut, which is why this lives in one place rather than in each of them.
 *
 * The rules match MuPDF's native {@code bookmark_text}: whitespace runs collapse to one space, the
 * result is capped, and a word the cap would cut in half is dropped instead of being left partial.
 */
public final class BookmarkText {

    /** The page text saved with a bookmark. */
    public static final int TEXT_SIZE = 128;

    /** The part of it that is searched - short enough to still fit on one page after a re-layout. */
    public static final int NEEDLE_SIZE = 64;

    /** Below this a needle matches too much to tell one page from another. */
    public static final int NEEDLE_MIN = 8;

    /** How far either side of the estimated page to look. */
    public static final int RANGE = 10;

    private static final char SOFT_HYPHEN = '\u00AD';

    private BookmarkText() {
    }

    /**
     * Collapses whitespace runs to one space and caps the result.
     *
     * @param limit the longest result in characters, or 0 for no limit; a word the limit would cut
     *              is dropped
     */
    public static String collapse(final String src, final int limit) {
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
            // A soft hyphen is invisible, and the conversion puts one inside every long word.
            // MuPDF reads its own text with FZ_STEXT_DEHYPHENATE and so never sees them; dropping
            // them here is what lets an anchor written by one engine be found by the other.
            if (c == SOFT_HYPHEN) {
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

    /** The text to save with a bookmark for the page whose text this is. */
    public static String of(final String pageText) {
        return collapse(pageText, TEXT_SIZE);
    }

    /**
     * @return the part of a saved text to search for, or <code>null</code> when it is too short to
     *         identify a page
     */
    public static String needle(final String text) {
        final String needle = collapse(text, NEEDLE_SIZE);
        return needle != null && needle.length() >= NEEDLE_MIN ? needle : null;
    }

    /**
     * The order pages are searched in around an estimate: the estimate itself, then one after, one
     * before, two after, two before, and so on - so the nearest match wins.
     *
     * @param step 0 .. 2 * {@link #RANGE}
     * @return the 1-based page to try at that step
     */
    public static int candidate(final int page, final int step) {
        return step % 2 != 0 ? page + (step + 1) / 2 : page - step / 2;
    }

    /** A page's text, ready to be matched against a needle. */
    public static String haystack(final String pageText) {
        return collapse(pageText, 0);
    }
}
