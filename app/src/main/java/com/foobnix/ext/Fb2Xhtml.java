package com.foobnix.ext;

/**
 * Turns the FictionBook markup Librera packs into its cached {@code .epub} into a document a
 * browser will accept.
 *
 * The book itself is left as it is: the FB2 element names - {@code section}, {@code title},
 * {@code subtitle}, {@code poem}, {@code empty-line} and the rest - stay, because the reading
 * stylesheet already lays them out (see {@code app-Librera.css}), and MuPDF goes on rendering
 * the same tags it always has. What is added is the skeleton around them.
 *
 * Without it the section's root element is {@code <FictionBook>}, so the document has no
 * {@code body}, and foliate's paginator asks the window for the computed style of a null:
 * <pre>Uncaught TypeError: Failed to execute 'getComputedStyle' on 'Window'</pre>
 * The engine throws, and the book quietly falls back to MuPDF.
 *
 * Three parts of the file are dropped rather than wrapped: {@code description} is metadata and
 * would be read out as the first page of text, {@code binary} holds the images base64-encoded and
 * would print as a wall of characters, and {@code stylesheet} is the book's own CSS.
 *
 * It works a line at a time, keeping what it is in the middle of between calls, so it can be fed
 * the same stream {@link Fb2Extractor#generateFb2File} already reads.
 */
public class Fb2Xhtml {

    /** The xlink prefix FB2 uses for its hrefs, declared so the markup still parses as XML. */
    private static final String HEAD = "<html xmlns=\"http://www.w3.org/1999/xhtml\""
            + " xmlns:l=\"http://www.w3.org/1999/xlink\">"
            + "<head><meta http-equiv=\"Content-Type\" content=\"text/html; charset=utf-8\"/>"
            + "<title>FB2</title></head><body>";

    private static final String TAIL = "</body></html>";

    /** The elements thrown away whole, with everything between their tags. */
    private static final String[] DROPPED = {"description", "binary", "stylesheet"};

    /** The element being skipped over, or null when the text is being kept. */
    private String dropping;

    public String apply(final String line) {
        if (line == null) {
            return null;
        }
        final StringBuilder out = new StringBuilder(line.length() + HEAD.length());
        int at = 0;
        while (at < line.length()) {
            final int open = line.indexOf('<', at);
            if (open < 0) {
                if (dropping == null) {
                    out.append(line, at, line.length());
                }
                break;
            }
            if (dropping == null) {
                out.append(line, at, open);
            }
            final int close = line.indexOf('>', open);
            if (close < 0) {
                // A tag split across the line break: the stream is read line by line, so the rest
                // of it arrives next time and the text in hand is passed on as it is.
                if (dropping == null) {
                    out.append(line, open, line.length());
                }
                break;
            }
            final String tag = line.substring(open, close + 1);
            if (dropping != null) {
                if (isClosing(tag, dropping)) {
                    dropping = null;
                }
            } else {
                out.append(translate(tag));
            }
            at = close + 1;
        }
        return out.toString();
    }

    private String translate(final String tag) {
        final String name = nameOf(tag);
        if (name == null) {
            // a comment, a doctype or the xml declaration: nothing to rewrite
            return tag;
        }
        if (!isClose(tag)) {
            for (final String dropped : DROPPED) {
                if (dropped.equals(name)) {
                    // <binary .../> closes itself, and nothing follows it to skip to
                    if (!tag.endsWith("/>")) {
                        dropping = dropped;
                    }
                    return "";
                }
            }
        }
        if ("fictionbook".equals(name)) {
            return isClose(tag) ? TAIL : HEAD;
        }
        if ("body".equals(name)) {
            // XHTML carries one body and FB2 brings several - the text, then the notes
            return isClose(tag) ? "</div>" : "<div class=\"fb2-body\">";
        }
        return tag;
    }

    /** The element's name in lower case, or null when the tag names no element. */
    private static String nameOf(final String tag) {
        int from = 1;
        if (from < tag.length() && tag.charAt(from) == '/') {
            from++;
        }
        if (from >= tag.length() || !Character.isLetter(tag.charAt(from))) {
            return null;
        }
        int to = from;
        while (to < tag.length() && (Character.isLetterOrDigit(tag.charAt(to))
                || tag.charAt(to) == '-' || tag.charAt(to) == '_' || tag.charAt(to) == ':')) {
            to++;
        }
        return tag.substring(from, to).toLowerCase(java.util.Locale.US);
    }

    private static boolean isClose(final String tag) {
        return tag.length() > 1 && tag.charAt(1) == '/';
    }

    private static boolean isClosing(final String tag, final String name) {
        return isClose(tag) && name.equals(nameOf(tag));
    }
}
