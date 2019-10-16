package org.ebookdroid.core.codec;

public class CodecPageInfo {

    public int width; /* page width (in pixels) */
    public int height; /* page height (in pixels) */
    public int dpi; /* page resolution (in dots per inch) */
    public int rotation; /* initial page orientation */
    public int version; /* page version */

    public CodecPageInfo() {
    }

    public CodecPageInfo(final int width, final int height) {
        this.width = width;
        this.height = height;
    }

    @Override
    public String toString() {
        return new StringBuilder("[").append(width).append(", ").append(height).append(", ").append(rotation).append("]").toString();
    }
}
