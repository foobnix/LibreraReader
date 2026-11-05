package com.foobnix.pdf.search.activity.msg;

import org.ebookdroid.core.codec.Annotation;

/** An annotation of the PDF was tapped with the eraser, in paged mode, for it to be erased. */
public class MessageEraseAnnotation {

    public final Annotation annotation;

    public MessageEraseAnnotation(final Annotation annotation) {
        this.annotation = annotation;
    }
}
