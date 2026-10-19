package com.foobnix.pdf.info.view;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.TextView;

import com.foobnix.android.utils.LOG;

/**
 * A TextView whose text can be selected, and that survives a long press on its own selection.
 * <p>
 * A long press inside a selection makes the framework drag the selected text, with a shadow
 * drawn from it. When that text measures nothing - a selection of line breaks alone - the shadow
 * has no size and {@code View.startDragAndDrop} throws "Drag shadow dimensions must be positive"
 * (Android 12, seen by the Play pre-launch crawler on the book information sheet). The drag is
 * not needed here, so the throw is swallowed and the selection simply stays.
 */
public class SelectableTextView extends TextView {

    public SelectableTextView(Context context) {
        super(context);
    }

    public SelectableTextView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public SelectableTextView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    public boolean performLongClick() {
        try {
            return super.performLongClick();
        } catch (IllegalStateException e) {
            LOG.e(e);
            return true;
        }
    }
}
