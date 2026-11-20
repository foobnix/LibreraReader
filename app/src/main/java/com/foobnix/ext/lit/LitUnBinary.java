package com.foobnix.ext.lit;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Deque;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Turns Microsoft Reader's binary HTML (and binary OPF) back into text, after calibre's UnBinary
 * (ebooks/lit/reader.py). The output is well-formed XML, as foliate parses every section as XML:
 * text and values are escaped here, elements that may not be empty in HTML are written with an
 * end tag, and names an XML parser would refuse become span or are dropped.
 */
final class LitUnBinary {

    private static final Charset UTF8 = Charset.forName("UTF-8");

    private static final int FLAG_OPENING = 1;
    private static final int FLAG_CLOSING = 1 << 1;
    private static final int FLAG_ATOM = 1 << 4;

    private static final Set<String> VOID = new HashSet<String>(Arrays.asList(
            "area", "base", "basefont", "br", "col", "embed", "frame", "hr", "img", "input", "link", "meta", "param", "wbr", "bgsound", "nextid"));
    private static final Pattern XML_NAME = Pattern.compile("[A-Za-z_][A-Za-z0-9._-]*");

    private enum State {
        TEXT, GET_FLAGS, GET_TAG, GET_ATTR, GET_VALUE_LENGTH, GET_VALUE, GET_CUSTOM_LENGTH, GET_CUSTOM,
        GET_ATTR_LENGTH, GET_CUSTOM_ATTR, GET_HREF_LENGTH, GET_HREF, CLOSE_TAG
    }

    private static final class Frame {
        final int depth;
        String tagName;
        Map<Integer, String> currentMap;
        boolean isGoingDown;
        State state;
        int flags;

        Frame(int depth, String tagName, Map<Integer, String> currentMap, State state, int flags) {
            this.depth = depth;
            this.tagName = tagName;
            this.currentMap = currentMap;
            this.state = state;
            this.flags = flags;
        }
    }

    /**
     * Maps an internal manifest id to the path of the file, relative to the one being decoded.
     */
    interface Paths {
        String relative(String internalId);
    }

    private final byte[] bin;
    private final int end;
    private final LitMaps.Map3 map;
    private final boolean html;
    private final Map<Integer, String> tagAtoms;
    private final Map<Integer, String> attrAtoms;
    private final Paths paths;
    private final StringBuilder out = new StringBuilder();
    private int cpos;

    // the start tag being written: its attributes so far, and whether the current one is dropped
    private final Set<String> attrsSeen = new HashSet<String>();
    private boolean skipValue;

    private LitUnBinary(byte[] bin, int off, int len, LitMaps.Map3 map, Map<Integer, String> tagAtoms, Map<Integer, String> attrAtoms, Paths paths) {
        this.bin = bin;
        this.cpos = off;
        this.end = off + len;
        this.map = map;
        this.html = map == LitMaps.HTML;
        this.tagAtoms = tagAtoms;
        this.attrAtoms = attrAtoms;
        this.paths = paths;
    }

    static String decode(byte[] bin, LitMaps.Map3 map, Map<Integer, String> tagAtoms, Map<Integer, String> attrAtoms, Paths paths) throws IOException {
        final LitUnBinary u = new LitUnBinary(bin, 0, bin.length, map, tagAtoms, attrAtoms, paths);
        final Deque<Frame> stack = new ArrayDeque<Frame>();
        stack.push(new Frame(0, null, null, State.TEXT, 0));
        while (!stack.isEmpty()) {
            u.inner(stack);
        }
        return u.out.toString();
    }

    private int readChar() throws IOException {
        int c = bin[cpos] & 0xFF;
        int mask = 0x80;
        int size;
        if ((c & mask) != 0) {
            size = 0;
            while ((c & mask) != 0) {
                mask >>= 1;
                size++;
            }
            if (mask <= 1 || mask == 0x40) {
                throw new IOException("Invalid UTF8 character at " + cpos);
            }
        } else {
            size = 1;
        }
        if (size > 1) {
            if (size + cpos > end) {
                throw new IOException("Invalid UTF8 character at " + cpos);
            }
            c &= mask - 1;
            for (int i = 1; i < size; i++) {
                final int b = bin[cpos + i] & 0xFF;
                if ((b & 0xC0) != 0x80) {
                    throw new IOException("Invalid UTF8 character at " + cpos);
                }
                c = (c << 6) | (b & 0x3F);
            }
        }
        cpos += size;
        return c;
    }

    private void inner(Deque<Frame> stack) throws IOException {
        final Frame f = stack.pop();
        int count = 0;
        StringBuilder custom = null;

        if (f.state == State.CLOSE_TAG) {
            if (f.tagName == null) {
                throw new IOException("Tag ends before it begins");
            }
            out.append("</").append(f.tagName).append('>');
            f.tagName = null;
            f.state = State.TEXT;
        }

        while (cpos < end) {
            final int oc = readChar();

            switch (f.state) {
                case TEXT:
                    if (oc == 0) {
                        f.state = State.GET_FLAGS;
                        break;
                    }
                    text(oc == 0x0B ? '\n' : oc, false);
                    break;

                case GET_FLAGS:
                    if (oc == 0) {
                        f.state = State.TEXT;
                        break;
                    }
                    f.flags = oc;
                    f.state = State.GET_TAG;
                    break;

                case GET_TAG:
                    f.state = oc == 0 ? State.TEXT : State.GET_ATTR;
                    if ((f.flags & FLAG_OPENING) != 0) {
                        out.append('<');
                        attrsSeen.clear();
                        if ((f.flags & FLAG_CLOSING) == 0) {
                            f.isGoingDown = true;
                        }
                        if (oc == 0x8000) {
                            f.state = State.GET_CUSTOM_LENGTH;
                            break;
                        }
                        String name;
                        if ((f.flags & FLAG_ATOM) != 0) {
                            name = tagAtoms == null ? null : tagAtoms.get(oc);
                            if (name == null) {
                                throw new IOException("Atom tag " + oc + " not in the atom tag list");
                            }
                            f.currentMap = attrAtoms;
                        } else if (oc < map.tags.length) {
                            name = map.tags[oc];
                            f.currentMap = oc < map.tagAttrs.length ? map.tagAttrs[oc] : null;
                        } else {
                            name = null;
                            f.currentMap = null;
                        }
                        f.tagName = tagName(name);
                        out.append(f.tagName);
                    } else if ((f.flags & FLAG_CLOSING) != 0) {
                        if (f.depth == 0) {
                            throw new IOException("Extra closing tag at " + cpos);
                        }
                        return;
                    }
                    break;

                case GET_ATTR:
                    skipValue = false;
                    if (oc == 0) {
                        f.state = State.TEXT;
                        if (!f.isGoingDown) {
                            if (VOID.contains(f.tagName) || !html) {
                                out.append(" />");
                            } else {
                                out.append("></").append(f.tagName).append('>');
                            }
                            f.tagName = null;
                        } else {
                            out.append('>');
                            final Frame parent = new Frame(f.depth, f.tagName, f.currentMap, State.CLOSE_TAG, f.flags);
                            stack.push(parent);
                            stack.push(new Frame(f.depth + 1, null, null, State.TEXT, 0));
                            return;
                        }
                        break;
                    }
                    if (oc == 0x8000) {
                        f.state = State.GET_ATTR_LENGTH;
                        break;
                    }
                    String attr = null;
                    if (f.currentMap != null && f.currentMap.get(oc) != null) {
                        attr = f.currentMap.get(oc);
                    } else if (map.attrs.containsKey(oc)) {
                        attr = map.attrs.get(oc);
                    }
                    if (attr == null || attr.startsWith("%")) {
                        // Reader's own, or unknown: its value is read and dropped
                        skipValue = true;
                        f.state = State.GET_VALUE_LENGTH;
                        break;
                    }
                    startAttr(attr);
                    f.state = !skipValue && ("href".equals(attr) || "src".equals(attr)) ? State.GET_HREF_LENGTH : State.GET_VALUE_LENGTH;
                    break;

                case GET_VALUE_LENGTH:
                    if (!skipValue) {
                        out.append('"');
                    }
                    count = oc - 1;
                    if (count == 0) {
                        if (!skipValue) {
                            out.append('"');
                        }
                        skipValue = false;
                        f.state = State.GET_ATTR;
                        break;
                    }
                    f.state = State.GET_VALUE;
                    if (oc == 0xFFFF) {
                        break;
                    }
                    if (count < 0 || count > end - cpos) {
                        throw new IOException("Invalid character count " + count);
                    }
                    break;

                case GET_VALUE:
                    if (count == 0xFFFE) {
                        if (!skipValue) {
                            out.append(oc - 1).append('"');
                        }
                        skipValue = false;
                        f.state = State.GET_ATTR;
                        break;
                    }
                    if (count > 0) {
                        if (!skipValue) {
                            text(oc, true);
                        }
                        count--;
                    }
                    if (count == 0) {
                        if (!skipValue) {
                            out.append('"');
                        }
                        skipValue = false;
                        f.state = State.GET_ATTR;
                    }
                    break;

                case GET_CUSTOM_LENGTH:
                    count = oc - 1;
                    if (count <= 0 || count > end - cpos) {
                        throw new IOException("Invalid character count " + count);
                    }
                    custom = new StringBuilder();
                    f.state = State.GET_CUSTOM;
                    break;

                case GET_CUSTOM:
                    custom.appendCodePoint(oc);
                    if (--count == 0) {
                        f.tagName = tagName(custom.toString());
                        f.currentMap = null;
                        out.append(f.tagName);
                        f.state = State.GET_ATTR;
                    }
                    break;

                case GET_ATTR_LENGTH:
                    count = oc - 1;
                    if (count <= 0 || count > end - cpos) {
                        throw new IOException("Invalid character count " + count);
                    }
                    custom = new StringBuilder();
                    f.state = State.GET_CUSTOM_ATTR;
                    break;

                case GET_CUSTOM_ATTR:
                    custom.appendCodePoint(oc);
                    if (--count == 0) {
                        startAttr(custom.toString());
                        f.state = State.GET_VALUE_LENGTH;
                    }
                    break;

                case GET_HREF_LENGTH:
                    count = oc - 1;
                    if (count <= 0 || count > end - cpos) {
                        throw new IOException("Invalid character count " + count);
                    }
                    custom = new StringBuilder();
                    f.state = State.GET_HREF;
                    break;

                case GET_HREF:
                    custom.appendCodePoint(oc);
                    if (--count == 0) {
                        out.append('"');
                        String href = custom.substring(1);
                        String frag = null;
                        final int hash = href.indexOf('#');
                        if (hash >= 0) {
                            frag = href.substring(hash + 1);
                            href = href.substring(0, hash);
                        }
                        String path = href.isEmpty() || paths == null ? href : paths.relative(href);
                        if (frag != null && !frag.isEmpty()) {
                            path = path + "#" + frag;
                        }
                        final String quoted = LitFile.quoteHref(path);
                        for (int i = 0; i < quoted.length(); i++) {
                            text(quoted.charAt(i), true);
                        }
                        out.append('"');
                        f.state = State.GET_ATTR;
                    }
                    break;

                default:
                    throw new IOException("Bad state " + f.state);
            }
        }
    }

    /**
     * Writes the name and = of an attribute, or marks its value to be dropped: a name an XML
     * parser would not take, a namespace no element declares, or one the tag already has.
     */
    private void startAttr(String attr) {
        String name = attr;
        if (html) {
            if ("xml:lang".equals(name)) {
                name = "xml:lang";
            } else if (name.indexOf(':') >= 0 || !XML_NAME.matcher(name).matches()) {
                skipValue = true;
                return;
            } else {
                name = name.toLowerCase(java.util.Locale.US);
            }
        }
        if (!attrsSeen.add(name)) {
            skipValue = true;
            return;
        }
        out.append(' ').append(name).append('=');
    }

    private String tagName(String name) {
        if (!html) {
            return name == null ? "unknown" : name;
        }
        if (name == null || !XML_NAME.matcher(name).matches()) {
            return "span";
        }
        name = name.toLowerCase(java.util.Locale.US);
        if ("form".equals(name)) {
            return "div";
        }
        return name;
    }

    private void text(int c, boolean attribute) {
        switch (c) {
            case '&':
                out.append("&amp;");
                return;
            case '<':
                out.append("&lt;");
                return;
            case '>':
                out.append("&gt;");
                return;
            case '"':
                out.append(attribute ? "&quot;" : "\"");
                return;
            default:
        }
        // characters XML does not allow
        if (c < 0x20 && c != '\t' && c != '\n' && c != '\r') {
            return;
        }
        if ((c >= 0xD800 && c <= 0xDFFF) || c == 0xFFFE || c == 0xFFFF || c > 0x10FFFF) {
            return;
        }
        if (attribute && (c == '\n' || c == '\r' || c == '\t')) {
            out.append(' ');
            return;
        }
        out.appendCodePoint(c);
    }

    static byte[] utf8(String s) {
        return s.getBytes(UTF8);
    }
}
