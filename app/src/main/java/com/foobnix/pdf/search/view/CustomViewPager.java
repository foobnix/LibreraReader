package com.foobnix.pdf.search.view;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;

import com.foobnix.model.AppState;

public class CustomViewPager extends RtlViewPager {

    public CustomViewPager(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getPointerCount() == 1 && AppState.get().selectedText == null) {
            try {
                return super.onTouchEvent(event);
            } catch (Exception e) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent event) {
        if (event.getPointerCount() == 1 && AppState.get().selectedText == null) {
            try {
                return super.onInterceptTouchEvent(event);
            } catch (Exception e) {
                return true;
            }
        }
        return false;
    }

    @Override
    protected boolean isRtl() {
        return AppState.get().isRTL;
    }

}