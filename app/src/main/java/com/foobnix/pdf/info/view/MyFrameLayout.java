package com.foobnix.pdf.info.view;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.widget.FrameLayout;

public class MyFrameLayout extends FrameLayout {

    public MyFrameLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        try {
            if (onEventDetected != null) {
                onEventDetected.run();
            }
            return super.dispatchTouchEvent(ev);
        } catch (Exception e) {
            return false;
        }
    }

    public void setOnEventDetected(Runnable onEventDetected) {
        this.onEventDetected = onEventDetected;
    }

    private Runnable onEventDetected;

}
