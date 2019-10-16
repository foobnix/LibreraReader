package org.ebookdroid.common.settings.types;

import android.graphics.RectF;

public enum PageType {

    /**
     *
     */
    LEFT_PAGE(0, 2),
    /**
     *
     */
    RIGHT_PAGE(1, 2),
    /**
     *
     */
    FULL_PAGE(0, 1);

    private final RectF initialRect;
    private final float leftPos;
    private final float widthScale;

    private PageType(float leftPos, float widthScale) {
        this.initialRect = new RectF(leftPos / widthScale, 0, (leftPos + 1) / widthScale, 1);
        this.leftPos = leftPos;
        this.widthScale = widthScale;
    }

    public RectF getInitialRect() {
        return initialRect;
    }

    public float getLeftPos() {
        return leftPos;
    }

    public float getWidthScale() {
        return widthScale;
    }
}
