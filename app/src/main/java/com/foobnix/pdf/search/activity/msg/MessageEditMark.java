package com.foobnix.pdf.search.activity.msg;

import com.foobnix.model.AppAnnotation;

/** A mark was tapped in the book, for the reader showing it to offer changing it. */
public class MessageEditMark {

    public final AppAnnotation mark;
    /** Draws the page again, with the mark as it is now. */
    public final Runnable changed;
    /** Takes the mark off the page, once it has been removed from the store. */
    public final Runnable removed;

    public MessageEditMark(final AppAnnotation mark, final Runnable changed, final Runnable removed) {
        this.mark = mark;
        this.changed = changed;
        this.removed = removed;
    }
}
