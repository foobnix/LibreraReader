package com.foobnix.ext;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.CRC32;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

/**
 * Makes a converted book's absolute font sizes follow the reader's font size.
 * <p>
 * Books that were written for a screen rather than for a reader - help files, Word's HTML in
 * Microsoft Reader books - fix their text in points, pixels and keywords: 11px, 12.0pt,
 * x-small. A browser draws those at that size whatever the reader asks for: foliate sets the
 * reader's font size on the root element, and only relative sizes follow it. So the size the
 * book's body text is set in becomes 1rem, the reader's own size, and every other absolute size
 * the same proportion of it: a heading twice the body text stays twice the reader's size. rem
 * rather than em, so that sizes set on nested elements do not multiply. Librera's MuPDF reads
 * rem as a scale too, and scales px and pt itself.
 * <p>
 * The body text is found by weight: each size counts for as much text as the elements it is set
 * on hold, so a navigation bar used on every page does not outweigh the text. Pages are weighed
 * before stylesheets, whose rules are weighed by the text of the classes and tags they select.
 */
public class FontSizes {

    private static final Charset UTF8 = Charset.forName("UTF-8");

    /**
     * How much text the elements of each class and each tag hold directly; for the tags also
     * the text of the ones with no class, which is what a size on the body sets.
     */
    private final Map<String, Integer> classText = new HashMap<String, Integer>();
    private final Map<String, Integer> tagText = new HashMap<String, Integer>();
    private final Map<String, Integer> plainTagText = new HashMap<String, Integer>();
    /** Each absolute font size of the book, in CSS pixels, by how much text it is likely to set. */
    private final Map<Float, Double> sizeWeight = new HashMap<Float, Double>();
    /** The size the book's body text is set in, which becomes the reader's font size. */
    private float basePx = 16;

    /** Whether the book has any absolute size at all; without one there is nothing to change. */
    public boolean hasSizes() {
        return !sizeWeight.isEmpty();
    }

    private static final Pattern FONT_SIZE = Pattern.compile("(?i)(font-size\\s*:\\s*)([^;}\"'!]+)");
    private static final Pattern FONT_SHORTHAND = Pattern.compile("(?i)((?<![\\w-])font\\s*:\\s*)([^;}\"'!]+)");
    private static final Pattern SHORTHAND_SIZE = Pattern.compile(
            "(?i)(?<![\\w.-])(\\d*\\.?\\d+(?:px|pt|pc|in|cm|mm)|xx-small|x-small|small|medium|large|x-large|xx-large)"
                    + "(?:\\s*/\\s*(\\d*\\.?\\d+)(px|pt)?)?(?![\\w-])");
    private static final Pattern LINE_HEIGHT = Pattern.compile("(?i)(line-height\\s*:\\s*)(\\d*\\.?\\d+)\\s*(px|pt)");
    private static final Pattern LENGTH = Pattern.compile("(?i)^(\\d*\\.?\\d+)\\s*(px|pt|pc|in|cm|mm)$");
    private static final Pattern CSS_COMMENT = Pattern.compile("(?s)/\\*.*?\\*/");
    private static final Pattern CSS_RULE = Pattern.compile("([^{}]+)\\{([^{}]*)\\}");
    private static final Pattern STYLE_BLOCK = Pattern.compile("(?is)<style[^>]*>(.*?)</style>");
    private static final Pattern STYLE_ATTR = Pattern.compile("(?i)\\sstyle\\s*=\\s*(?:\"([^\"]*)\"|'([^']*)')");
    private static final Pattern FONT_TAG = Pattern.compile("(?i)<font\\b[^>]*?\\ssize\\s*=\\s*[\"']?\\s*([+-]?\\d)");
    private static final Pattern CLASS_ATTR = Pattern.compile("(?i)\\sclass\\s*=\\s*(?:\"([^\"]*)\"|'([^']*)'|([^\\s>]+))");
    /** An opening tag, its attributes and the text right after it. */
    private static final Pattern OPEN_TAG = Pattern.compile("<([A-Za-z][A-Za-z0-9]*)(\\s[^>]*)?>([^<]*)");

    /** CSS pixels of an absolute size - a length or a keyword - or -1 for a relative one. */
    public static float absolutePx(String value) {
        value = value.trim().toLowerCase(Locale.US);
        if (value.equals("xx-small")) {
            return 9;
        } else if (value.equals("x-small")) {
            return 10;
        } else if (value.equals("small")) {
            return 13;
        } else if (value.equals("medium")) {
            return 16;
        } else if (value.equals("large")) {
            return 18;
        } else if (value.equals("x-large")) {
            return 24;
        } else if (value.equals("xx-large")) {
            return 32;
        }
        final Matcher m = LENGTH.matcher(value);
        if (!m.matches()) {
            return -1;
        }
        final float x;
        try {
            x = Float.parseFloat(m.group(1));
        } catch (NumberFormatException e) {
            return -1;
        }
        final String unit = m.group(2).toLowerCase(Locale.US);
        if (unit.equals("px")) {
            return x;
        } else if (unit.equals("pt")) {
            return x * 4 / 3;
        } else if (unit.equals("pc")) {
            return x * 16;
        } else if (unit.equals("in")) {
            return x * 96;
        } else if (unit.equals("cm")) {
            return x * 96 / 2.54f;
        } else {
            return x * 96 / 25.4f;
        }
    }

    /** {@code <font size="2">}, or {@code +1} from the default 3, in CSS pixels. */
    public static float fontTagPx(String size) {
        final float[] px = {10, 13, 16, 18, 24, 32, 48};
        size = size.trim();
        try {
            int n;
            if (size.startsWith("+")) {
                n = 3 + Integer.parseInt(size.substring(1));
            } else if (size.startsWith("-")) {
                n = 3 - Integer.parseInt(size.substring(1));
            } else {
                n = Integer.parseInt(size);
            }
            return px[Math.max(1, Math.min(7, n)) - 1];
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    /** A size in CSS pixels as rem of the body text; within half and four times it. */
    public String rem(final float px) {
        final float r = Math.max(0.5f, Math.min(4f, px / basePx));
        return number(r) + "rem";
    }

    private static String number(final float x) {
        String s = String.format(Locale.US, "%.3f", x);
        s = s.replaceFirst("0+$", "");
        return s.endsWith(".") ? s.substring(0, s.length() - 1) : s;
    }

    /** The absolute sizes of a stylesheet - or of a style attribute - made relative to the body text. */
    public String css(final String css) {
        StringBuffer out = new StringBuffer();
        Matcher m = FONT_SIZE.matcher(css);
        while (m.find()) {
            final float px = absolutePx(m.group(2));
            m.appendReplacement(out, Matcher.quoteReplacement(px > 0 ? m.group(1) + rem(px) : m.group()));
        }
        m.appendTail(out);

        final String sized = out.toString();
        out = new StringBuffer();
        m = FONT_SHORTHAND.matcher(sized);
        while (m.find()) {
            final Matcher size = SHORTHAND_SIZE.matcher(m.group(2));
            String value = m.group(2);
            if (size.find()) {
                final float px = absolutePx(size.group(1));
                if (px > 0) {
                    String replacement = rem(px);
                    if (size.group(2) != null) {
                        float lh = Float.parseFloat(size.group(2));
                        if (size.group(3) != null) {
                            lh = absolutePx(size.group(2) + size.group(3)) / px;
                        }
                        replacement += "/" + number(Math.max(1f, lh));
                    }
                    value = value.substring(0, size.start()) + replacement + value.substring(size.end());
                }
            }
            m.appendReplacement(out, Matcher.quoteReplacement(m.group(1) + value));
        }
        m.appendTail(out);

        final String shorthands = out.toString();
        out = new StringBuffer();
        m = LINE_HEIGHT.matcher(shorthands);
        while (m.find()) {
            // A line height in pixels would not grow with the text, and lines would overlap
            final float px = absolutePx(m.group(2) + m.group(3));
            m.appendReplacement(out, Matcher.quoteReplacement(m.group(1) + number(Math.max(1f, Math.min(3f, px / basePx)))));
        }
        m.appendTail(out);
        return out.toString();
    }

    /**
     * What a page tells about the sizes: how much text each class and tag holds, and the page's
     * own sizes. Text, not elements, is what is counted: a navigation bar's class may be on more
     * elements than the body text's, but it does not set the book.
     */
    public void weighPage(final String text) {
        Matcher m = OPEN_TAG.matcher(text);
        while (m.find()) {
            final int chars = textLength(m.group(3));
            final String tag = m.group(1).toLowerCase(Locale.US);
            add(tagText, tag, chars);
            final String attrs = m.group(2);
            final Matcher c = attrs == null ? null : CLASS_ATTR.matcher(attrs);
            if (c != null && c.find()) {
                final String value = c.group(1) != null ? c.group(1) : c.group(2) != null ? c.group(2) : c.group(3);
                for (String name : value.trim().split("\\s+")) {
                    if (name.length() > 0) {
                        add(classText, name.toLowerCase(Locale.US), chars);
                    }
                }
            } else {
                add(plainTagText, tag, chars);
            }
            // A size on the element itself sets its text
            final Matcher style = attrs == null ? null : STYLE_ATTR.matcher(attrs);
            if (style != null && style.find()) {
                final float px = declaredPx(style.group(1) != null ? style.group(1) : style.group(2));
                if (px > 0) {
                    weigh(px, chars);
                }
            }
            if (tag.equals("font") && attrs != null) {
                final Matcher size = FONT_TAG.matcher("<font" + attrs + ">");
                if (size.find()) {
                    final float px = fontTagPx(size.group(1));
                    if (px > 0) {
                        weigh(px, chars);
                    }
                }
            }
        }
        m = STYLE_BLOCK.matcher(text);
        while (m.find()) {
            weighStylesheet(m.group(1));
        }
    }

    /** Characters of text, with entities as one and runs of white space as one. */
    private static int textLength(final String raw) {
        final String t = raw.replaceAll("&#?\\w+;", "x").replaceAll("\\s+", " ").trim();
        return t.length();
    }

    /** Each rule's size counts for as much text as its selectors match in the pages. */
    public void weighStylesheet(final String css) {
        final Matcher m = CSS_RULE.matcher(CSS_COMMENT.matcher(css).replaceAll(""));
        while (m.find()) {
            final float px = declaredPx(m.group(2));
            if (px <= 0) {
                continue;
            }
            double weight = 0;
            for (String selector : m.group(1).split(",")) {
                weight += selectorUse(selector.trim());
            }
            weigh(px, Math.max(weight, 0.5));
        }
    }

    /** The absolute font size a block of declarations sets, or -1. */
    private static float declaredPx(final String declarations) {
        float px = -1;
        Matcher m = FONT_SIZE.matcher(declarations);
        while (m.find()) {
            px = absolutePx(m.group(2));
        }
        if (px > 0) {
            return px;
        }
        m = FONT_SHORTHAND.matcher(declarations);
        while (m.find()) {
            final Matcher size = SHORTHAND_SIZE.matcher(m.group(2));
            if (size.find()) {
                px = absolutePx(size.group(1));
            }
        }
        return px;
    }

    /** How much text of the pages a selector is likely to match, judged by its last part. */
    private double selectorUse(final String selector) {
        final String[] parts = selector.toLowerCase(Locale.US).split("[\\s>+~]+");
        final String last = parts[parts.length - 1].replaceAll(":.*$", "");
        final Matcher classes = Pattern.compile("\\.([\\w-]+)").matcher(last);
        int use = Integer.MAX_VALUE;
        boolean hasClass = false;
        while (classes.find()) {
            hasClass = true;
            final Integer n = classText.get(classes.group(1));
            use = Math.min(use, n == null ? 0 : n);
        }
        if (hasClass) {
            return use;
        }
        final String tag = last.replaceAll("[#\\[].*$", "");
        if (tag.equals("body") || tag.equals("html")) {
            // The body's size is the size of the text nothing else sets
            int sum = 0;
            for (String t : new String[]{"body", "p", "td", "th", "div", "li", "dd", "dt", "span", "font", "a", "b", "i"}) {
                final Integer n = plainTagText.get(t);
                sum += n == null ? 0 : n;
            }
            return sum;
        }
        final Integer n = tagText.get(tag);
        return n == null ? 0 : n;
    }

    private void weigh(final float px, final double weight) {
        final float key = Math.round(px * 2) / 2f;
        final Double w = sizeWeight.get(key);
        sizeWeight.put(key, (w == null ? 0 : w) + weight);
    }

    private static void add(final Map<String, Integer> map, final String key, final int n) {
        if (n <= 0) {
            return;
        }
        final Integer was = map.get(key);
        map.put(key, was == null ? n : was + n);
    }

    /**
     * Settles the body text's size, once every page and stylesheet has been weighed: the heaviest
     * size that could be body text, or 16px, the browser's own, when there is none.
     *
     * @return the size in CSS pixels
     */
    public float pickBase() {
        float best = 16;
        double bestWeight = 0;
        for (Map.Entry<Float, Double> e : sizeWeight.entrySet()) {
            if (e.getKey() >= 8 && e.getKey() <= 24 && e.getValue() > bestWeight) {
                best = e.getKey();
                bestWeight = e.getValue();
            }
        }
        basePx = best;
        return best;
    }

    // ------------------------------------------------------------------ an EPUB

    private static final Pattern FONT_ELEMENT = Pattern.compile("(?i)<font\\b([^>]*)>");
    private static final Pattern SIZE_ATTR = Pattern.compile("(?i)\\ssize\\s*=\\s*(?:\"([^\"]*)\"|'([^']*)'|([^\\s>/]+))");

    /**
     * Rewrites the absolute font sizes of an EPUB in place: its stylesheets, its pages' style
     * blocks and style attributes, and {@code <font size>}. A book with no absolute size is left
     * as it is.
     *
     * @return the body text's size in CSS pixels, or 0 when nothing was changed
     */
    public static float normalizeEpub(final File epub) throws IOException {
        final Map<String, byte[]> entries = new LinkedHashMap<String, byte[]>();
        final ZipFile zip = new ZipFile(epub);
        try {
            final Enumeration<? extends ZipEntry> e = zip.entries();
            while (e.hasMoreElements()) {
                final ZipEntry entry = e.nextElement();
                if (!entry.isDirectory()) {
                    entries.put(entry.getName(), read(zip.getInputStream(entry)));
                }
            }
        } finally {
            zip.close();
        }

        final FontSizes sizes = new FontSizes();
        final List<String> pages = new ArrayList<String>();
        final List<String> stylesheets = new ArrayList<String>();
        for (String name : entries.keySet()) {
            final String lower = name.toLowerCase(Locale.US);
            if (lower.endsWith(".xhtml") || lower.endsWith(".html") || lower.endsWith(".htm")) {
                pages.add(name);
            } else if (lower.endsWith(".css")) {
                stylesheets.add(name);
            }
        }
        for (String page : pages) {
            sizes.weighPage(new String(entries.get(page), UTF8));
        }
        for (String css : stylesheets) {
            sizes.weighStylesheet(new String(entries.get(css), UTF8));
        }
        if (!sizes.hasSizes()) {
            return 0;
        }
        final float base = sizes.pickBase();
        for (String css : stylesheets) {
            entries.put(css, sizes.css(new String(entries.get(css), UTF8)).getBytes(UTF8));
        }
        for (String page : pages) {
            entries.put(page, sizes.page(new String(entries.get(page), UTF8)).getBytes(UTF8));
        }

        final File temp = new File(epub.getPath() + ".sizes");
        final ZipOutputStream out = new ZipOutputStream(new FileOutputStream(temp));
        try {
            // "mimetype" goes first, whatever the order it was found in
            final List<Map.Entry<String, byte[]>> ordered = new ArrayList<Map.Entry<String, byte[]>>(entries.entrySet());
            for (int i = 0; i < ordered.size(); i++) {
                if (ordered.get(i).getKey().equals("mimetype")) {
                    ordered.add(0, ordered.remove(i));
                    break;
                }
            }
            for (Map.Entry<String, byte[]> entry : ordered) {
                final ZipEntry z = new ZipEntry(entry.getKey());
                if (entry.getKey().equals("mimetype")) {
                    // First and uncompressed, as the format asks
                    final CRC32 crc = new CRC32();
                    crc.update(entry.getValue());
                    z.setMethod(ZipEntry.STORED);
                    z.setSize(entry.getValue().length);
                    z.setCompressedSize(entry.getValue().length);
                    z.setCrc(crc.getValue());
                }
                out.putNextEntry(z);
                out.write(entry.getValue());
                out.closeEntry();
            }
        } catch (IOException ex) {
            out.close();
            temp.delete();
            throw ex;
        }
        out.close();
        if (!epub.delete() || !temp.renameTo(epub)) {
            temp.delete();
            throw new IOException("cannot replace " + epub);
        }
        return base;
    }

    /** A page's style blocks, style attributes and {@code <font size>}, as text. */
    String page(final String html) {
        StringBuffer out = new StringBuffer();
        Matcher m = STYLE_BLOCK.matcher(html);
        while (m.find()) {
            final String block = m.group();
            final int open = block.indexOf('>') + 1;
            final int close = block.toLowerCase(Locale.US).lastIndexOf("</style");
            final String replaced = block.substring(0, open) + css(block.substring(open, close)) + block.substring(close);
            m.appendReplacement(out, Matcher.quoteReplacement(replaced));
        }
        m.appendTail(out);

        String text = out.toString();
        out = new StringBuffer();
        m = STYLE_ATTR.matcher(text);
        while (m.find()) {
            final boolean single = m.group(1) == null;
            final String value = single ? m.group(2) : m.group(1);
            final String q = single ? "'" : "\"";
            m.appendReplacement(out, Matcher.quoteReplacement(" style=" + q + css(value) + q));
        }
        m.appendTail(out);

        text = out.toString();
        out = new StringBuffer();
        m = FONT_ELEMENT.matcher(text);
        while (m.find()) {
            String attrs = m.group(1);
            final Matcher size = SIZE_ATTR.matcher(attrs);
            if (size.find()) {
                final String value = size.group(1) != null ? size.group(1) : size.group(2) != null ? size.group(2) : size.group(3);
                final float px = fontTagPx(value);
                attrs = attrs.substring(0, size.start()) + attrs.substring(size.end());
                if (px > 0) {
                    final Matcher style = STYLE_ATTR.matcher(attrs);
                    if (style.find()) {
                        final boolean single = style.group(1) == null;
                        final String q = single ? "'" : "\"";
                        final String old = single ? style.group(2) : style.group(1);
                        attrs = attrs.substring(0, style.start()) + " style=" + q + "font-size:" + rem(px) + ";" + old + q
                                + attrs.substring(style.end());
                    } else {
                        attrs = " style=\"font-size:" + rem(px) + "\"" + attrs;
                    }
                }
            }
            m.appendReplacement(out, Matcher.quoteReplacement("<font" + attrs + ">"));
        }
        m.appendTail(out);
        return out.toString();
    }

    private static byte[] read(final InputStream in) throws IOException {
        try {
            final ByteArrayOutputStream out = new ByteArrayOutputStream();
            final byte[] buffer = new byte[16 * 1024];
            int n;
            while ((n = in.read(buffer)) != -1) {
                out.write(buffer, 0, n);
            }
            return out.toByteArray();
        } finally {
            in.close();
        }
    }
}
