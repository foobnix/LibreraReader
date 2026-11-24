package com.foobnix.pdf.info.wrapper;

import com.foobnix.android.utils.LOG;
import com.foobnix.android.utils.TxtUtils;

import org.ebookdroid.core.codec.CodecDocument;
import org.ebookdroid.core.codec.CodecPage;
import org.ebookdroid.droids.foliate.codec.FoliateDocument;
import org.ebookdroid.droids.mupdf.codec.TextWord;

import java.util.ArrayList;
import java.util.List;

/**
 * Where a reader is, as words: what every engine, layout and device can find again.
 * <p>
 * The page text a position is saved with ({@code AppBook.pt}) used to be the start of the page.
 * In the scroll mode the reader is usually part way down a page, so another device - whose pages
 * are another size, or another engine whose pages break elsewhere - opened at the start of that
 * page: before a chapter heading the reader had already scrolled past. The text is now taken
 * from the top of what is on screen, and in the scroll mode the book is moved back to it, with
 * those words at the top.
 * <p>
 * The words are compared without spaces, hyphens and soft hyphens: MuPDF joins a word cut at a
 * line end, foliate never cuts one, and a word cut on one layout is whole on another.
 */
public final class ReadingPlace {

    /**
     * As much of the text as is saved: short, so that on another layout it is not split by a page
     * break, which neither MuPDF's search nor foliate's would then find on either page.
     */
    private static final int TEXT_SIZE = 48;

    /** Letters of the saved text looked for on a page: few enough to fit on it near its foot. */
    private static final int NEEDLE_LETTERS = 24;

    /**
     * Pages either side of the percentage a place is looked for in, off the main thread, when the
     * usual ten do not have it: another device's layout or engine can put it that far away.
     */
    public static final int WIDE_RANGE = 50;

    /** Above this the reader is part way down the page; at the top, the page start is the place. */
    public static final float INSIDE_PAGE = 0.02f;

    private ReadingPlace() {
    }

    /** The words of a 0-based page, laid out as the reader lays it out; null when there are none. */
    public static TextWord[][] words(CodecDocument doc, int page0) {
        if (doc == null || page0 < 0) {
            return null;
        }
        CodecPage page = null;
        try {
            // foliate's pages are views of the one WebView, kept by the document: not recycled.
            // A MuPDF page is opened for this alone, and not the one being drawn.
            final boolean own = !(doc instanceof FoliateDocument);
            page = own ? doc.getPageInner(page0) : doc.getPage(page0);
            if (page == null || page.isRecycled()) {
                return null;
            }
            final TextWord[][] words = page.getText();
            if (own) {
                page.recycle();
            }
            return words == null || words.length == 0 ? null : words;
        } catch (Throwable e) {
            LOG.e(e);
            return null;
        }
    }

    /**
     * The text from a point down a page: the first line whose middle is below {@code y} (a
     * fraction of the page's height) and what follows it. Null when the page has no words there.
     */
    public static String textFrom(TextWord[][] words, float y) {
        if (words == null) {
            return null;
        }
        final StringBuilder text = new StringBuilder();
        TextWord previous = null;
        for (TextWord[] line : words) {
            if (line == null) {
                continue;
            }
            for (TextWord word : line) {
                if (word == null || TxtUtils.isEmpty(word.w) || word.w.trim().isEmpty()) {
                    continue;
                }
                if (middle(word) < y) {
                    continue;
                }
                String w = word.w.replace("­", "");
                if (previous != null) {
                    final boolean newLine = word.top > middle(previous);
                    final boolean cut = newLine && text.length() > 0 && text.charAt(text.length() - 1) == '-';
                    if (cut) {
                        // A word cut at the line end is put back together
                        text.setLength(text.length() - 1);
                    } else {
                        text.append(' ');
                    }
                }
                text.append(w);
                previous = word;
                if (text.length() >= TEXT_SIZE) {
                    return cutAtWord(text.toString());
                }
            }
        }
        return text.length() == 0 ? null : text.toString();
    }

    /** A page's text cut to the size saved, at a word. */
    public static String shorter(String text) {
        if (text == null) {
            return null;
        }
        text = text.trim();
        return text.length() <= TEXT_SIZE ? text : cutAtWord(text);
    }

    private static String cutAtWord(String text) {
        final int space = text.lastIndexOf(' ', TEXT_SIZE);
        return space > TEXT_SIZE / 2 ? text.substring(0, space) : text.substring(0, Math.min(TEXT_SIZE, text.length()));
    }

    /**
     * How far down the page the saved text starts, as a fraction of the page's height, just above
     * its first line; -1 when the page does not have it.
     */
    public static float topOf(TextWord[][] words, String saved) {
        final String needle = squeezed(saved, NEEDLE_LETTERS);
        if (words == null || needle == null) {
            return -1;
        }
        final StringBuilder letters = new StringBuilder();
        final List<TextWord> owner = new ArrayList<>();
        for (TextWord[] line : words) {
            if (line == null) {
                continue;
            }
            for (TextWord word : line) {
                if (word == null || word.w == null) {
                    continue;
                }
                for (int i = 0; i < word.w.length(); i++) {
                    final char c = word.w.charAt(i);
                    if (isLetter(c)) {
                        letters.append(Character.toLowerCase(c));
                        owner.add(word);
                    }
                }
            }
        }
        final int at = letters.indexOf(needle);
        if (at < 0) {
            return -1;
        }
        final TextWord first = owner.get(at);
        return Math.max(0f, first.top - (first.bottom - first.top) * 0.3f);
    }

    private static float middle(TextWord word) {
        return (word.top + word.bottom) / 2f;
    }

    /** The letters of the text, no spaces or hyphens, lower case, at most {@code max} of them. */
    static String squeezed(String text, int max) {
        if (TxtUtils.isEmpty(text)) {
            return null;
        }
        final StringBuilder out = new StringBuilder();
        for (int i = 0; i < text.length() && out.length() < max; i++) {
            final char c = text.charAt(i);
            if (isLetter(c)) {
                out.append(Character.toLowerCase(c));
            }
        }
        // Too short to tell one place from another
        return out.length() < 8 ? null : out.toString();
    }

    private static boolean isLetter(char c) {
        return !Character.isWhitespace(c) && c != '-' && c != '­' && c != '‐' && c != '‑'
                && !Character.isSpaceChar(c);
    }
}
