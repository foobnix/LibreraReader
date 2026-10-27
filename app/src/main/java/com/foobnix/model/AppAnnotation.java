package com.foobnix.model;

import com.foobnix.pdf.info.model.AnnotationType;

import java.io.File;

/**
 * A mark the reader put on a book's text - a highlight, an underline or a strikethrough - kept by
 * Librera rather than by the document.
 *
 * A PDF carries its own annotations and keeps doing so; these are for every other format, which
 * has nowhere inside the file to put them. They live beside the bookmarks in the device profile
 * and travel with it, so the same highlight is on the book on every device the folder reaches.
 *
 * <h3>Finding it again</h3>
 *
 * The hard part is not storing a mark but finding the words it was put on after the book has been
 * laid out again - a different font size, a different screen, a different device. Which of the
 * three anchors below is filled depends on what the engine that made the mark could promise:
 *
 * <ul>
 * <li><b>{@link #rects}</b> - a fixed-layout book (PDF, DjVu, TIFF). The page never reflows, so
 *     the page number and the rectangles on it are exact for ever.</li>
 * <li><b>{@link #cfi}</b> - a reflowable book laid out by foliate-js. A CFI names the characters
 *     themselves, so the mark is on the same words at any size. This is the only exact anchor a
 *     reflowable book can have.</li>
 * <li><b>{@link #pt}</b> - a reflowable book laid out by MuPDF, which can name nothing finer.
 *     The page text finds the page the way a bookmark does, and {@link #text} is then looked for
 *     among that page's words. It finds the sentence; after a large font change it may sit a line
 *     out.</li>
 * </ul>
 */
public class AppAnnotation implements MyPath.RelativePath {

    /** The book, relative, so the same folder works on another device. */
    public String path;

    /** Created at; also the key in the JSON and the identity of the mark. */
    public long t;

    /** {@link AnnotationType#ordinal()} - one of HIGHLIGHT, UNDERLINE, STRIKEOUT. */
    public int type = AnnotationType.HIGHLIGHT.ordinal();

    /** "#RRGGBB", as the reading settings store a colour. */
    public String color = "#FFFF00";

    /** The words that were marked. Shown in the list, and used to find them again. */
    public String text = "";

    /** Where it was made, 0-based. Exact for a fixed-layout book, a starting guess otherwise. */
    public int page;

    /** How far through the book, for when the page number cannot be trusted. */
    public float p;

    /** A fixed-layout book's rectangles: "l,t,r,b;l,t,r,b", each 0..1 of the page. */
    public String rects = "";

    /** A reflowable book's exact place, when foliate-js laid it out. */
    public String cfi = "";

    /** A reflowable book's page text, when MuPDF did. */
    public String pt = "";

    /** Which device's file this was read from, so it can be removed from the right one. */
    transient public File file;

    public AppAnnotation() {
    }

    public AppAnnotation(final String path) {
        this.path = MyPath.toRelative(path);
        this.t = System.currentTimeMillis();
    }

    @Override
    public String getPath() {
        return MyPath.toAbsolute(path);
    }

    @Override
    public void setPath(final String path) {
        this.path = MyPath.toRelative(path);
    }

    public AnnotationType getType() {
        final AnnotationType[] all = AnnotationType.values();
        return type >= 0 && type < all.length ? all[type] : AnnotationType.HIGHLIGHT;
    }

    public void setType(final AnnotationType type) {
        this.type = type.ordinal();
    }

    /** Whether this mark knows exactly which characters it is on. */
    public boolean isExact() {
        return rects != null && rects.length() > 0 || cfi != null && cfi.length() > 0;
    }

    @Override
    public int hashCode() {
        return (int) t;
    }

    @Override
    public boolean equals(final Object obj) {
        return obj instanceof AppAnnotation && ((AppAnnotation) obj).t == t;
    }

    @Override
    public String toString() {
        return "AppAnnotation[" + getType() + " " + color + " p" + page + " " + text + "]";
    }
}
