package org.ebookdroid.common.settings.types;

import static android.view.Gravity.BOTTOM;
import static android.view.Gravity.CENTER;
import static android.view.Gravity.LEFT;
import static android.view.Gravity.RIGHT;
import static android.view.Gravity.TOP;

public enum ToastPosition {

    /**
     * 
     */
    Invisible(0),
    /**
     * 
     */
    LeftTop(LEFT | TOP),
    /**
     * 
     */
    RightTop(RIGHT | TOP),
    /**
     * 
     */
    LeftBottom(LEFT | BOTTOM),
    /**
     * 
     */
    Bottom(CENTER | BOTTOM),
    /**
     * 
     */
    RightBottom(RIGHT | BOTTOM);

    public final int position;

    private ToastPosition(int position) {
        this.position = position;
    }

}
