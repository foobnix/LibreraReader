package org.ebookdroid.droids.pdfium.codec;

import android.graphics.RectF;

import com.foobnix.android.utils.Dips;
import com.foobnix.android.utils.LOG;
import com.foobnix.android.utils.TxtUtils;
import com.foobnix.model.AppState;
import com.foobnix.pdf.info.model.BookCSS;

import org.ebookdroid.droids.mupdf.codec.TextWord;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.legere.pdfiumandroid.PdfTextPage;
import io.legere.pdfiumandroid.api.WordRangeRect;

/**
 * The text layer of a PDFium page: the words Librera selects, searches and speaks, and the
 * pseudo-HTML its TTS and its "export as text" read.
 *
 * PDFium gives a page's text as one string with the characters' boxes beside it, where MuPDF gives
 * blocks, lines and words. The words are rebuilt here the same way
 * {@link org.ebookdroid.droids.mupdf.codec.MuPdfPage#getText_116()} rebuilds them from MuPDF's
 * characters, so the rest of the app cannot tell the two engines apart.
 */
class PdfiumText {

    /** Characters that end a line in PDFium's text; they have no box worth selecting. */
    private static boolean isLineBreak(final char c) {
        return c == '\r' || c == '\n';
    }

    /** The rectangles of the character ranges of one page, in PDF space. */
    private interface RangeRects {
        RectF rect(int start, int length);
    }

    /**
     * All the words of the page, as one line, in reading order - the shape
     * {@link org.ebookdroid.droids.mupdf.codec.MuPdfPage} produces for MuPDF 1.16 and later.
     * Spaces come through as words of their own, which is what selecting by letters needs.
     *
     * A word is as wide as PDFium boxes its characters and as tall as the whole line it is on.
     * PDFium boxes a character tight around the glyph, so on its own a word with no ascender or
     * descender comes out shorter than its neighbours and a selection drawn over it looks ragged;
     * MuPDF's lines are boxed by the font's ascent and descent and are even. Taking the line's
     * extent gets that evenness back, and a tap anywhere on a line's height still finds its word.
     */
    static TextWord[][] words(final PdfTextPage textPage, final PdfiumTransform transform) {
        final String text = text(textPage);
        if (TxtUtils.isEmpty(text)) {
            return new TextWord[0][0];
        }

        final boolean byLetters = AppState.get().selectingByLetters;

        // The ranges whose rectangles are wanted: one per character when selecting by letters,
        // else one per word plus one per space. Every range remembers the line it is on.
        final List<int[]> ranges = new ArrayList<int[]>();
        final List<String> labels = new ArrayList<String>();
        final List<Integer> lines = new ArrayList<Integer>();
        int line = 0;
        int wordStart = -1;
        for (int i = 0; i < text.length(); i++) {
            final char c = text.charAt(i);
            final boolean isSpace = c == ' ' || c == TxtUtils.NON_BREAKE_SPACE_CHAR || c == '\t';

            if (byLetters) {
                if (isLineBreak(c)) {
                    line++;
                } else if (c != 0) {
                    ranges.add(new int[]{i, 1});
                    labels.add(String.valueOf(isSpace ? ' ' : c));
                    lines.add(line);
                }
                continue;
            }

            if (isSpace || isLineBreak(c) || c == 0) {
                if (wordStart >= 0) {
                    ranges.add(new int[]{wordStart, i - wordStart});
                    labels.add(text.substring(wordStart, i));
                    lines.add(line);
                    wordStart = -1;
                }
                if (isSpace) {
                    ranges.add(new int[]{i, 1});
                    labels.add(" ");
                    lines.add(line);
                } else if (isLineBreak(c)) {
                    line++;
                }
            } else if (wordStart < 0) {
                wordStart = i;
            }
        }
        if (wordStart >= 0) {
            ranges.add(new int[]{wordStart, text.length() - wordStart});
            labels.add(text.substring(wordStart));
            lines.add(line);
        }

        if (ranges.isEmpty()) {
            return new TextWord[0][0];
        }

        // Every range in page space, and with it the vertical extent of each line
        final RangeRects rects = rangeRects(textPage, ranges);
        final RectF[] boxes = new RectF[ranges.size()];
        final Map<Integer, float[]> lineExtent = new HashMap<Integer, float[]>();
        for (int i = 0; i < ranges.size(); i++) {
            final int[] range = ranges.get(i);
            final RectF pdf = rects.rect(range[0], range[1]);
            if (pdf == null) {
                continue;
            }
            final RectF box = transform.toPageSpace(pdf);
            boxes[i] = box;
            if (box.top == box.bottom) {
                // A space, or a character PDFium gives no height: it takes the line's
                continue;
            }
            final float[] extent = lineExtent.get(lines.get(i));
            if (extent == null) {
                lineExtent.put(lines.get(i), new float[]{box.top, box.bottom});
            } else {
                extent[0] = Math.min(extent[0], box.top);
                extent[1] = Math.max(extent[1], box.bottom);
            }
        }

        final List<TextWord> words = new ArrayList<TextWord>(ranges.size());
        for (int i = 0; i < boxes.length; i++) {
            if (boxes[i] == null) {
                continue;
            }
            final float[] extent = lineExtent.get(lines.get(i));
            final RectF box = boxes[i];
            if (extent != null) {
                box.top = extent[0];
                box.bottom = extent[1];
            }
            final TextWord word = new TextWord(labels.get(i), box);
            // The original, in page space, is what a markup annotation's quad points are built
            // from; only the copy the app hit-tests against is scaled to the page's 0..1.
            word.setOriginal(box);
            transform.normalize(word);
            words.add(word);
        }

        if (words.isEmpty()) {
            return new TextWord[0][0];
        }

        final TextWord[][] res = new TextWord[1][];
        res[0] = words.toArray(new TextWord[words.size()]);
        return res;
    }

    /**
     * Asks PDFium for every range's rectangle in one call, and falls back to the characters' own
     * boxes when that call is not answered.
     */
    private static RangeRects rangeRects(final PdfTextPage textPage, final List<int[]> ranges) {
        try {
            final int[] flat = new int[ranges.size() * 2];
            for (int i = 0; i < ranges.size(); i++) {
                flat[i * 2] = ranges.get(i)[0];
                flat[i * 2 + 1] = ranges.get(i)[1];
            }
            final List<WordRangeRect> answer = textPage.textPageGetRectsForRanges(flat);
            if (answer != null && !answer.isEmpty()) {
                final Map<Integer, RectF> byStart = new HashMap<Integer, RectF>(answer.size() * 2);
                for (WordRangeRect r : answer) {
                    if (r != null && r.getRect() != null) {
                        byStart.put(r.getRangeStart(), r.getRect());
                    }
                }
                if (!byStart.isEmpty()) {
                    return new RangeRects() {
                        @Override
                        public RectF rect(final int start, final int length) {
                            return byStart.get(start);
                        }
                    };
                }
            }
        } catch (Throwable e) {
            LOG.e(e);
        }

        return new RangeRects() {
            @Override
            public RectF rect(final int start, final int length) {
                return charBoxUnion(textPage, start, length);
            }
        };
    }

    /** The union of the boxes of <code>length</code> characters from <code>start</code>. */
    private static RectF charBoxUnion(final PdfTextPage textPage, final int start, final int length) {
        RectF union = null;
        for (int i = start; i < start + length; i++) {
            final RectF box = textPage.textPageGetCharBox(i);
            if (box == null) {
                continue;
            }
            if (union == null) {
                union = new RectF(box);
            } else {
                // The boxes are in PDF space, where "top" is the larger y, so RectF.union would
                // read them upside down: take the extremes by value instead.
                union.left = Math.min(union.left, Math.min(box.left, box.right));
                union.right = Math.max(union.right, Math.max(box.left, box.right));
                union.top = Math.max(union.top, Math.max(box.top, box.bottom));
                union.bottom = Math.min(union.bottom, Math.min(box.top, box.bottom));
            }
        }
        return union;
    }

    /** The whole page's text, with the trailing padding PDFium leaves behind removed. */
    static String text(final PdfTextPage textPage) {
        try {
            final int count = textPage.textPageCountChars();
            if (count <= 0) {
                return "";
            }
            String text = textPage.textPageGetText(0, count);
            if (text == null) {
                return "";
            }
            if (text.length() > count) {
                text = text.substring(0, count);
            }
            int end = text.length();
            while (end > 0 && text.charAt(end - 1) == 0) {
                end--;
            }
            return text.substring(0, end);
        } catch (Throwable e) {
            LOG.e(e);
            return "";
        }
    }

    /**
     * The page as the pseudo-HTML Librera's TTS, its text export and its document search read:
     * a <code>&lt;p&gt;</code> per paragraph with the lines joined by a space, a
     * <code>&lt;pause&gt;</code> around a paragraph set in a larger font than the body text -
     * what MuPDF's own <code>fz_print_stext_page_as_html_my</code> writes.
     *
     * PDFium has no notion of a block, so a paragraph ends where a blank line is: the text of a
     * page comes out the same, only its division into paragraphs is coarser than MuPDF's.
     */
    static String html(final PdfTextPage textPage) {
        final String text = text(textPage);
        if (TxtUtils.isEmpty(text)) {
            return "";
        }

        // The size the app lays reflowable text out at; MuPDF compares a block's font to it to
        // decide whether the block is a heading.
        int bodySize = 0;
        try {
            bodySize = Dips.spToPx(BookCSS.get().fontSizeSp);
        } catch (Throwable e) {
            LOG.e(e);
        }

        final StringBuilder out = new StringBuilder(text.length() + 64);
        final List<String> paragraph = new ArrayList<String>();
        int paragraphStart = -1;

        int i = 0;
        int lineStart = 0;
        while (i <= text.length()) {
            final boolean end = i == text.length();
            if (!end && !isLineBreak(text.charAt(i))) {
                i++;
                continue;
            }
            final String line = text.substring(lineStart, i).trim();
            if (line.length() > 0) {
                if (paragraph.isEmpty()) {
                    paragraphStart = lineStart;
                }
                paragraph.add(line);
            } else if (!paragraph.isEmpty()) {
                writeParagraph(out, paragraph, fontSize(textPage, paragraphStart), bodySize);
                paragraph.clear();
            }
            if (end) {
                break;
            }
            // \r\n counts as one break
            if (text.charAt(i) == '\r' && i + 1 < text.length() && text.charAt(i + 1) == '\n') {
                i++;
            }
            i++;
            lineStart = i;
        }
        if (!paragraph.isEmpty()) {
            writeParagraph(out, paragraph, fontSize(textPage, paragraphStart), bodySize);
        }

        return out.toString();
    }

    private static void writeParagraph(final StringBuilder out, final List<String> lines, final double size,
                                       final int bodySize) {
        final boolean heading = bodySize > 0 && size > bodySize;
        if (heading) {
            out.append("<pause>");
        }
        out.append("<p>");
        for (String line : lines) {
            escape(out, line);
            out.append(" ");
        }
        out.append("</p>\n");
        if (heading) {
            out.append("<pause>\n");
        }
    }

    private static double fontSize(final PdfTextPage textPage, final int charIndex) {
        if (charIndex < 0) {
            return 0;
        }
        try {
            return textPage.getFontSize(charIndex);
        } catch (Throwable e) {
            return 0;
        }
    }

    private static void escape(final StringBuilder out, final String text) {
        for (int i = 0; i < text.length(); i++) {
            final char c = text.charAt(i);
            switch (c) {
                case '<':
                    out.append("&lt;");
                    break;
                case '>':
                    out.append("&gt;");
                    break;
                case '&':
                    out.append("&amp;");
                    break;
                case '"':
                    out.append("&quot;");
                    break;
                default:
                    out.append(c);
                    break;
            }
        }
    }
}
