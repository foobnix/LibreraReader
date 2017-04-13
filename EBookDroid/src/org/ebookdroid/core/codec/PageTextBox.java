package org.ebookdroid.core.codec;

import android.graphics.RectF;

public class PageTextBox extends RectF {

    public String text;

    @Override
    public String toString() {
        return "PageTextBox(" + left + ", " + top + ", " + right + ", " + bottom + ": " + text + ")";
    }

}
