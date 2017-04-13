package org.ebookdroid.common.touch;

import android.view.MotionEvent;

public interface IGestureDetector {

    boolean enabled();

    boolean onTouchEvent(final MotionEvent ev);
}
