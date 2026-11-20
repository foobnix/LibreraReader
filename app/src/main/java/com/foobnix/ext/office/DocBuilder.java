package com.foobnix.ext.office;

/**
 * Puts the paragraphs and tables of a converted document into an {@link EpubBook}: headings as
 * headings, the items of a list gathered into one {@code <ul>} or {@code <ol>} per depth, and the
 * empty paragraphs documents use for spacing kept down to one in a row.
 */
public final class DocBuilder {

    private final EpubBook book;

    /** The lists open now, the outermost first: "ul" or "ol". */
    private final java.util.ArrayList<String> lists = new java.util.ArrayList<String>();
    private final StringBuilder list = new StringBuilder();

    private boolean lastBlank = true;
    /** A blank line seen, put in only when something follows it. */
    private boolean pendingBlank;

    public DocBuilder(EpubBook book) {
        this.book = book;
    }

    public EpubBook book() {
        return book;
    }

    public void paragraph(Paragraph p) {
        if (p.isBlank()) {
            if (!lastBlank) {
                pendingBlank = true;
                lastBlank = true;
            }
            return;
        }
        lastBlank = false;
        blank(p.heading > 0 || p.listLevel > 0);
        if (p.heading > 0) {
            closeLists();
            book.heading(p.heading, p.inline(), p.plainText());
            return;
        }
        if (p.listLevel > 0) {
            item(p);
            return;
        }
        closeLists();
        book.block(p.toXhtml());
    }

    /** A table or any other block that is already XHTML. */
    public void block(String xhtml) {
        closeLists();
        blank(false);
        book.block(xhtml);
        lastBlank = false;
    }

    /** Closes what is still open. */
    public void finish() {
        closeLists();
    }

    /** The blank line before what comes now; none before a heading or inside a list. */
    private void blank(boolean skip) {
        if (pendingBlank && !skip) {
            closeLists();
            book.block("<p>\u00A0</p>");
        }
        pendingBlank = false;
    }

    private void item(Paragraph p) {
        final int depth = Math.min(p.listLevel, 9);
        final String kind = p.ordered ? "ol" : "ul";
        while (lists.size() > depth) {
            close();
        }
        if (lists.size() == depth && !lists.get(depth - 1).equals(kind)) {
            close();
        }
        while (lists.size() < depth) {
            if (lists.size() > 0) {
                // a deeper list goes inside the item before it, which is left open for it - or
                // inside an empty one, when the list starts deeper than its first level
                if (endsWith(list, "</li>")) {
                    list.setLength(list.length() - "</li>".length());
                } else {
                    list.append("<li>");
                }
            }
            list.append('<').append(kind).append('>');
            lists.add(kind);
        }
        list.append("<li").append(p.alignClass()).append('>').append(p.inline()).append("</li>");
    }

    private static boolean endsWith(StringBuilder text, String end) {
        return text.length() >= end.length() && text.substring(text.length() - end.length()).equals(end);
    }

    private void close() {
        final String kind = lists.remove(lists.size() - 1);
        list.append("</").append(kind).append('>');
        if (lists.size() > 0) {
            list.append("</li>");
        }
    }

    private void closeLists() {
        if (lists.isEmpty()) {
            return;
        }
        while (!lists.isEmpty()) {
            close();
        }
        book.block(list.toString());
        list.setLength(0);
    }
}
