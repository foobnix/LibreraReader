package com.foobnix.pdf.info.model;

/**
 * MuPDF's annotation types, in MuPDF's order: the native code hands the position over, so an
 * entry missing here shifts every one after it. Highlight, underline and strike-out are also
 * stored by position, so new ones go after them.
 */
public enum AnnotationType {
    TEXT,
 LINK,
 FREETEXT,
 LINE,
 SQUARE,
 CIRCLE,
 POLYGON,
 POLYLINE,
 HIGHLIGHT,
 UNDERLINE,
 SQUIGGLY,
 STRIKEOUT,
 REDACT,
 STAMP,
 CARET,
 INK,
 POPUP,
 FILEATTACHMENT,
 SOUND,
 MOVIE,
 RICH_MEDIA,
 WIDGET,
 SCREEN,
 PRINTERMARK,
 TRAPNET,
 WATERMARK,
 A3D,
 PROJECTION,
 UNKNOWN;

    /**
     * A drawing on the page - the pen's own ink, or a shape another app drew - which the drawing
     * panel picks out to move, restyle or erase. Links, notes and form fields are not.
     */
    public boolean isDrawing() {
        return this == INK || this == LINE || this == SQUARE || this == CIRCLE || this == POLYGON || this == POLYLINE;
    }
}
