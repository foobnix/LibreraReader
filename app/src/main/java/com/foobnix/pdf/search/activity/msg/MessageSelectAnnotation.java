package com.foobnix.pdf.search.activity.msg;

import org.ebookdroid.core.codec.Annotation;

/** A drawing of the PDF was tapped in paged mode: it is picked out and the drawing panel opened. */
public class MessageSelectAnnotation {

    public final Annotation annotation;

    public MessageSelectAnnotation(final Annotation annotation) {
        this.annotation = annotation;
    }
}
