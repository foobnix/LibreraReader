package com.foobnix.pdf.search.activity;

import android.view.GestureDetector.SimpleOnGestureListener;
import android.view.MotionEvent;

public abstract class SimpleTouchOnGestureListener extends SimpleOnGestureListener {

    public abstract boolean onTouchEvent(MotionEvent e);
}
