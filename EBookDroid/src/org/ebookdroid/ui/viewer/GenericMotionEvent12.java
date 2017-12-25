package org.ebookdroid.ui.viewer;

import android.view.InputDevice;
import android.view.MotionEvent;

public class GenericMotionEvent12 {

    public static boolean onGenericMotionEvent(MotionEvent event, VerticalViewActivity va) {
        if (0 != (event.getSource() & InputDevice.SOURCE_CLASS_POINTER)) {
            if (event.getAction() == MotionEvent.ACTION_SCROLL) {
                if (event.getAxisValue(MotionEvent.AXIS_VSCROLL) < 0.0f) {
                    va.getController().getListener().onScrollDown();
                } else {
                    va.getController().getListener().onScrollUp();
                }
                return true;
            }
        }
        return false;
    }

}
