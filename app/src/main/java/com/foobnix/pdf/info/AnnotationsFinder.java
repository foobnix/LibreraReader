package com.foobnix.pdf.info;

import android.graphics.RectF;

import com.foobnix.android.utils.LOG;
import com.foobnix.android.utils.TxtUtils;
import com.foobnix.model.AppAnnotation;
import com.foobnix.model.AppSP;

import org.ebookdroid.core.codec.CodecDocument;
import org.ebookdroid.core.codec.CodecPage;
import org.ebookdroid.droids.mupdf.codec.TextWord;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Turns the marks stored for a book into the rectangles to draw on a given page.
 *
 * A mark is stored with whichever anchor the engine that made it could promise, and each of them
 * is found again differently - see {@link AppAnnotation}. This is where the three meet, so that
 * everything drawing a page asks one question and gets rectangles back, whatever kind of book it
 * is reading.
 *
 * One of these belongs to one open book. The page a mark is on is worked out once and kept, since
 * for a reflowable book under MuPDF finding it means searching the text of several pages, and the
 * page is being drawn while that happens.
 */
public class AnnotationsFinder {

    /** A mark and where it is on the page being drawn. */
    public static class Drawn {
        public final AppAnnotation annotation;
        public final List<RectF> rects;

        public Drawn(final AppAnnotation annotation, final List<RectF> rects) {
            this.annotation = annotation;
            this.rects = rects;
        }
    }

    /** The book being read. Both reading modes have one open at a time, as the engines do. */
    private static volatile AnnotationsFinder current;

    private final String bookPath;

    private volatile List<AppAnnotation> all = Collections.emptyList();

    /** Which page each mark turned out to be on, 0-based; -1 once it is known to be nowhere. */
    private final Map<Long, Integer> resolved = new HashMap<Long, Integer>();

    /** The finder for a book, kept while that book is the one being read. */
    public static synchronized AnnotationsFinder forBook(final String bookPath) {
        AnnotationsFinder finder = current;
        if (finder == null || !finder.bookPath.equals(bookPath)) {
            finder = new AnnotationsFinder(bookPath);
            current = finder;
        }
        return finder;
    }

    /**
     * The finder for the book being read.
     *
     * Keyed on the book the reader opened, not on the file an engine happens to be rendering: a
     * converted format is rendered from an epub in the cache, and a mark on it belongs to the
     * fb2 or mobi in the library that the reader will open again.
     */
    public static AnnotationsFinder forCurrentBook() {
        return forBook(AppSP.get().lastBookPath);
    }

    /** After the reader has added or removed a mark, so the pages drawn next show it. */
    public static void invalidate() {
        final AnnotationsFinder finder = current;
        if (finder != null) {
            finder.reload();
        }
    }

    public AnnotationsFinder(final String bookPath) {
        this.bookPath = bookPath;
        reload();
    }

    /** Re-reads the store, after the reader has added or removed one. */
    public synchronized void reload() {
        all = AnnotationsData.get().byBook(bookPath);
        resolved.clear();
        LOG.d("AnnotationsFinder", bookPath, all.size());
    }

    public boolean isEmpty() {
        return all.isEmpty();
    }

    public List<AppAnnotation> getAll() {
        return all;
    }

    /**
     * The marks on one page, with their rectangles.
     *
     * @param page      the page, already decoded - its text is used to find a mark that has only
     *                  its words to go by
     * @param pageIndex the page's number, 0-based
     */
    public List<Drawn> forPage(final CodecDocument document, final CodecPage page, final int pageIndex) {
        // Always a list of its own, even when there is nothing on the page: a mark made next is
        // added to the list the page is holding, and a shared immutable one would refuse it.
        final List<Drawn> result = new ArrayList<Drawn>();
        final List<AppAnnotation> marks = all;
        if (marks.isEmpty() || page == null) {
            return result;
        }
        for (AppAnnotation annotation : marks) {
            final List<RectF> rects = rectsOn(document, page, pageIndex, annotation);
            if (!rects.isEmpty()) {
                result.add(new Drawn(annotation, rects));
            }
        }
        return result;
    }

    private List<RectF> rectsOn(final CodecDocument document, final CodecPage page,
                                final int pageIndex, final AppAnnotation annotation) {
        try {
            // A fixed-layout book keeps its own rectangles: the page never reflows, so they are
            // still exactly where they were put.
            if (TxtUtils.isNotEmpty(annotation.rects)) {
                return annotation.page == pageIndex ? parse(annotation.rects) : Collections.<RectF>emptyList();
            }
            // A CFI names the characters themselves; the engine says where they are now, and
            // answers with nothing when they are not on this page.
            if (TxtUtils.isNotEmpty(annotation.cfi)) {
                return page.getLocationRects(annotation.cfi);
            }
            // Nothing finer than the words. Find which page they are on, once, then look for
            // them among that page's own words.
            if (TxtUtils.isNotEmpty(annotation.text)) {
                if (pageOf(document, annotation) != pageIndex) {
                    return Collections.emptyList();
                }
                return findText(page.getText(), annotation.text);
            }
        } catch (Throwable e) {
            LOG.e(e);
        }
        return Collections.emptyList();
    }

    /** Which page a words-only mark is on, worked out once per open book. */
    private int pageOf(final CodecDocument document, final AppAnnotation annotation) {
        final Integer known;
        synchronized (this) {
            known = resolved.get(annotation.t);
        }
        if (known != null) {
            return known;
        }
        int page = annotation.page;
        try {
            if (document != null) {
                final int count = document.getPageCount();
                final int from = count > 0 ? Math.max(1, Math.min(count, annotation.page + 1)) : annotation.page + 1;
                // The same call bookmarks restore with: the page text, not the marked words -
                // it is what the engines' search is tuned for, and a page of text pins the page
                // far better than a phrase that may repeat.
                final String pageText = TxtUtils.isNotEmpty(annotation.pt) ? annotation.pt : annotation.text;
                final int found = document.findBookmarkPage(from, null, pageText);
                if (found > 0) {
                    page = found - 1;
                }
            }
        } catch (Throwable e) {
            LOG.e(e);
        }
        synchronized (this) {
            resolved.put(annotation.t, page);
        }
        return page;
    }

    // ---------------------------------------------------------------- from a selection

    /**
     * The selected words as one rectangle per line.
     *
     * A mark has to follow the text: drawn as one box around everything from the first word to
     * the last, a selection of three lines covers the margins too.
     */
    public static List<RectF> mergeLines(final List<? extends RectF> boxes) {
        final List<RectF> lines = new ArrayList<RectF>();
        RectF line = null;
        for (RectF box : boxes) {
            if (box == null || box.width() <= 0 && box.height() <= 0) {
                continue;
            }
            if (line != null && Math.abs(line.top - box.top) < 0.001f) {
                line.union(box);
            } else {
                line = new RectF(box);
                lines.add(line);
            }
        }
        return lines;
    }

    /** The selected words as the text they spell, for finding them again and for the list. */
    public static String textOf(final List<? extends TextWord> words) {
        final StringBuilder out = new StringBuilder();
        for (TextWord word : words) {
            if (word == null || word.w == null) {
                continue;
            }
            final String w = word.w.trim();
            if (w.length() == 0) {
                continue;
            }
            if (out.length() > 0) {
                out.append(' ');
            }
            out.append(w);
        }
        return out.toString();
    }

    // ---------------------------------------------------------------- rectangles as text

    /** "l,t,r,b;l,t,r,b" - compact, and readable in the JSON when something has to be looked at. */
    public static String write(final List<RectF> rects) {
        final StringBuilder out = new StringBuilder();
        for (RectF rect : rects) {
            if (out.length() > 0) {
                out.append(';');
            }
            out.append(round(rect.left)).append(',').append(round(rect.top)).append(',')
                    .append(round(rect.right)).append(',').append(round(rect.bottom));
        }
        return out.toString();
    }

    /** Four decimals is a fifth of a pixel on a page of any size anyone reads on. */
    private static String round(final float value) {
        return String.valueOf(Math.round(value * 10000f) / 10000f);
    }

    public static List<RectF> parse(final String text) {
        final List<RectF> rects = new ArrayList<RectF>();
        if (TxtUtils.isEmpty(text)) {
            return rects;
        }
        for (String part : text.split(";")) {
            final String[] n = part.split(",");
            if (n.length != 4) {
                continue;
            }
            try {
                rects.add(new RectF(Float.parseFloat(n[0]), Float.parseFloat(n[1]),
                        Float.parseFloat(n[2]), Float.parseFloat(n[3])));
            } catch (NumberFormatException e) {
                LOG.e(e, part);
            }
        }
        return rects;
    }

    // ---------------------------------------------------------------- finding words again

    /**
     * The rectangles of a run of text among a page's words, or empty when it is not there.
     *
     * Matched on the words rather than on the characters: the page's text and the stored text
     * were produced by the same word splitting, so comparing them word by word avoids having to
     * care about the spaces between them, which differ by engine.
     */
    public static List<RectF> findText(final TextWord[][] lines, final String text) {
        final List<RectF> result = new ArrayList<RectF>();
        if (lines == null || TxtUtils.isEmpty(text)) {
            return result;
        }
        final List<TextWord> words = new ArrayList<TextWord>();
        for (TextWord[] line : lines) {
            for (TextWord word : line) {
                if (word != null && word.w != null && word.w.trim().length() > 0) {
                    words.add(word);
                }
            }
        }
        final String[] wanted = text.trim().toLowerCase().split("\\s+");
        if (wanted.length == 0 || words.isEmpty()) {
            return result;
        }
        for (int start = 0; start + wanted.length <= words.size(); start++) {
            boolean match = true;
            for (int i = 0; i < wanted.length; i++) {
                if (!words.get(start + i).w.trim().toLowerCase().equals(wanted[i])) {
                    match = false;
                    break;
                }
            }
            if (!match) {
                continue;
            }
            // One rectangle per line of the run, so a highlight follows the text rather than
            // boxing everything between its first word and its last
            RectF line = null;
            for (int i = 0; i < wanted.length; i++) {
                final TextWord word = words.get(start + i);
                if (line != null && Math.abs(line.top - word.top) < 0.001f) {
                    line.union(word);
                } else {
                    line = new RectF(word);
                    result.add(line);
                }
            }
            return result;
        }
        return result;
    }
}
