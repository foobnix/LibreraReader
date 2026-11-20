package com.foobnix.ext.office;

/**
 * One paragraph of a document being converted: its runs of text with their formatting, and what
 * the paragraph is - a heading, an item of a list, aligned to a side. The converters fill it in
 * and hand it to a {@link DocBuilder}.
 *
 * Every run is wrapped in its own tags, so the XHTML is well formed however the runs' formatting
 * changes from one to the next.
 */
public final class Paragraph {

    public static final int ALIGN_LEFT = 0;
    public static final int ALIGN_CENTER = 1;
    public static final int ALIGN_RIGHT = 2;
    public static final int ALIGN_JUSTIFY = 3;

    public static final int VERT_NONE = 0;
    public static final int VERT_SUPER = 1;
    public static final int VERT_SUB = 2;

    /** The formatting of a run of text. */
    public static final class Style {
        public boolean bold;
        public boolean italic;
        public boolean underline;
        public boolean strike;
        public int vert;
        /** The run is a link to this address. */
        public String href;

        public Style copy() {
            final Style style = new Style();
            style.bold = bold;
            style.italic = italic;
            style.underline = underline;
            style.strike = strike;
            style.vert = vert;
            style.href = href;
            return style;
        }

        boolean same(Style other) {
            if (other == null) {
                return false;
            }
            return bold == other.bold && italic == other.italic && underline == other.underline
                    && strike == other.strike && vert == other.vert
                    && (href == null ? other.href == null : href.equals(other.href));
        }

        public void reset() {
            bold = italic = underline = strike = false;
            vert = VERT_NONE;
            href = null;
        }
    }

    final StringBuilder inline = new StringBuilder();
    final StringBuilder plain = new StringBuilder();

    // text not yet wrapped: the converters of RTF and .doc hand it over a character at a time,
    // and a run of the same formatting is one element, not one a character
    private final StringBuilder pending = new StringBuilder();
    private Style pendingStyle;

    /** 1..6 for a heading, 0 for a paragraph. */
    public int heading;
    public int align = ALIGN_LEFT;
    /** 0 for a paragraph, else the depth of the list it is an item of, from 1. */
    public int listLevel;
    public boolean ordered;

    /** Text with the formatting it has. */
    public void text(String text, Style style) {
        if (text == null || text.length() == 0) {
            return;
        }
        plain.append(text);
        if (pending.length() > 0 && !(style == null ? pendingStyle == null : style.same(pendingStyle))) {
            flush();
        }
        if (pending.length() == 0) {
            pendingStyle = style == null ? null : style.copy();
        }
        EpubBook.appendEscaped(pending, text);
    }

    private void flush() {
        if (pending.length() > 0) {
            wrap(pending, pendingStyle);
            pending.setLength(0);
        }
    }

    /** XHTML that is already well formed: a link to a footnote, say. */
    public void markup(String xhtml) {
        flush();
        inline.append(xhtml);
    }

    public void lineBreak() {
        plain.append(' ');
        flush();
        inline.append("<br/>");
    }

    public void tab() {
        text(" ", null);
    }

    public void image(String href) {
        if (href != null) {
            flush();
            inline.append("<img src=\"").append(EpubBook.escape(href)).append("\" alt=\"\"/>");
        }
    }

    /** Whether the paragraph shows nothing: no text, no picture. */
    public boolean isBlank() {
        flush();
        if (inline.indexOf("<img") >= 0) {
            return false;
        }
        for (int i = 0; i < plain.length(); i++) {
            final char c = plain.charAt(i);
            if (!Character.isWhitespace(c) && c != ' ' && c != ' ') {
                return false;
            }
        }
        return true;
    }

    public String plainText() {
        return plain.toString();
    }

    String inline() {
        flush();
        return inline.toString();
    }

    /** The paragraph as a block of its own: a {@code <p>} with its alignment. */
    public String toXhtml() {
        return "<p" + alignClass() + ">" + inline() + "</p>";
    }

    String alignClass() {
        switch (align) {
            case ALIGN_CENTER:
                return " class=\"center\"";
            case ALIGN_RIGHT:
                return " class=\"right\"";
            case ALIGN_JUSTIFY:
                return " class=\"justify\"";
            default:
                return "";
        }
    }

    private void wrap(CharSequence escaped, Style style) {
        if (style == null) {
            inline.append(escaped);
            return;
        }
        final StringBuilder open = new StringBuilder();
        final StringBuilder close = new StringBuilder();
        if (style.href != null) {
            tag(open, close, "a href=\"" + EpubBook.escape(style.href) + "\"", "a");
        }
        if (style.bold) {
            tag(open, close, "b", "b");
        }
        if (style.italic) {
            tag(open, close, "i", "i");
        }
        if (style.underline) {
            tag(open, close, "u", "u");
        }
        if (style.strike) {
            tag(open, close, "s", "s");
        }
        if (style.vert == VERT_SUPER) {
            tag(open, close, "sup", "sup");
        } else if (style.vert == VERT_SUB) {
            tag(open, close, "sub", "sub");
        }
        inline.append(open).append(escaped).append(close);
    }

    private static void tag(StringBuilder open, StringBuilder close, String start, String name) {
        open.append('<').append(start).append('>');
        close.insert(0, "</" + name + ">");
    }
}
