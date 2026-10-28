package com.foobnix.ext;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
 * Out of the document, an XHTML reader no longer finds an image by the FB2 {@code l:href="#id"}
 * that points into it: MuPDF looks those up only in a FictionBook root, and foliate not at all.
 * So each {@code binary} goes to {@link Images} to be stored as a file of its own, and each
 * {@code <image>} names the file twice: as {@code xlink:href}, which Librera's MuPDF loads from the
 * archive (it never looks inside an {@code image}, and an {@code l:href} it would load twice), and
 * as an {@code <img>} inside for foliate. The {@code image} itself stays for the stylesheet to place.
 *
 * It works a line at a time, keeping what it is in the middle of between calls, so it can be fed
 * the same stream {@link Fb2Extractor#generateFb2File} already reads.
 */
public class Fb2Xhtml {

    /** The xlink prefix FB2 uses for its hrefs, declared so the markup still parses as XML. */
    private static final String HEAD = "<html xmlns=\"http://www.w3.org/1999/xhtml\""
            + " xmlns:l=\"http://www.w3.org/1999/xlink\" xmlns:xlink=\"http://www.w3.org/1999/xlink\">"
            + "<head><meta http-equiv=\"Content-Type\" content=\"text/html; charset=utf-8\"/>"
            + "<title>FB2</title></head><body>";

    private static final String TAIL = "</body></html>";

    /** The elements thrown away whole, with everything between their tags. */
    private static final String[] DROPPED = {"description", "binary", "stylesheet"};

    private static final Pattern HREF = Pattern.compile("\\s[\\w-]*:?href\\s*=\\s*([\"'])(.*?)\\1");
    private static final Pattern ID = Pattern.compile("\\sid\\s*=\\s*([\"'])(.*?)\\1");
    private static final Pattern TYPE = Pattern.compile("\\scontent-type\\s*=\\s*([\"'])(.*?)\\1");

    /** Where the images go, each with the file name its {@code <img>} uses. */
    public interface Images {
        void add(String file, String contentType, String base64);
    }

    private final Images images;

    /** The element being skipped over, or null when the text is being kept. */
    private String dropping;

    /** The image being read out of a {@code binary}, with its base64 so far. */
    private String binaryFile, binaryType;
    private StringBuilder binary;

    public Fb2Xhtml() {
        this(null);
    }

    public Fb2Xhtml(final Images images) {
        this.images = images;
    }

    /** The image's file in the book, next to the text, made from the FB2 id a link names. */
    public static String fileOf(final String id) {
        final StringBuilder file = new StringBuilder("images/");
        for (int i = 0; i < id.length(); i++) {
            final char c = id.charAt(i);
            file.append(c < 128 && (Character.isLetterOrDigit(c) || c == '.' || c == '-' || c == '_') ? c : '_');
        }
        return file.toString();
    }

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
                } else if (binary != null) {
                    binary.append(line, at, line.length());
                }
                break;
            }
            if (dropping == null) {
                out.append(line, at, open);
            } else if (binary != null) {
                binary.append(line, at, open);
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
                    if (binary != null) {
                        images.add(binaryFile, binaryType, binary.toString());
                        binary = null;
                    }
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
                        startBinary(name, tag);
                    }
                    return "";
                }
            }
            if ("image".equals(name)) {
                return image(tag);
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

    private void startBinary(final String name, final String tag) {
        final String id = value(ID, tag);
        if (images != null && "binary".equals(name) && id != null && !id.isEmpty()) {
            binaryFile = fileOf(id);
            final String type = value(TYPE, tag);
            binaryType = type == null || type.isEmpty() ? "image/jpeg" : type;
            binary = new StringBuilder();
        }
    }

    private static String image(final String tag) {
        final Matcher m = HREF.matcher(tag);
        if (!m.find() || m.group(2).isEmpty()) {
            return tag;
        }
        final String href = m.group(2);
        final String src = (href.startsWith("#") ? fileOf(href.substring(1)) : href).replace("\"", "&quot;");
        final boolean empty = tag.endsWith("/>");
        String open = tag.substring(0, m.start()) + " xlink:href=\"" + src + "\"" + tag.substring(m.end());
        if (empty) {
            open = open.substring(0, open.length() - 2).trim() + ">";
        }
        return open + "<img src=\"" + src + "\" alt=\"\"/>" + (empty ? "</image>" : "");
    }

    private static String value(final Pattern pattern, final String tag) {
        final Matcher m = pattern.matcher(tag);
        return m.find() ? m.group(2) : null;
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
        return tag.substring(from, to).toLowerCase(Locale.US);
    }

    private static boolean isClose(final String tag) {
        return tag.length() > 1 && tag.charAt(1) == '/';
    }

    private static boolean isClosing(final String tag, final String name) {
        return isClose(tag) && name.equals(nameOf(tag));
    }
}
