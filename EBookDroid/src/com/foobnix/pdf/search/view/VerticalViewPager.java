package com.foobnix.pdf.search.view;

import com.foobnix.pdf.info.wrapper.AppState;

import android.annotation.TargetApi;
import android.content.Context;
import android.os.Build;
import android.support.v4.view.ViewPager;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

public class VerticalViewPager extends CustomViewPager {

    public VerticalViewPager(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        if (AppState.get().rotateViewPager == 90) {
            setPageTransformer(true, new VerticalPageTransformer());
        }
    }

    private class VerticalPageTransformer implements ViewPager.PageTransformer {

        @TargetApi(Build.VERSION_CODES.HONEYCOMB)
        @Override
        public void transformPage(View view, float position) {
            if (position < -1) {
                // view.setAlpha(0);
            } else if (position <= 1) {
                // view.setAlpha(1);

                view.setTranslationX(view.getWidth() * -position);

                float yPosition = position * view.getHeight();
                view.setTranslationY(yPosition);
            } else {
                // view.setAlpha(0);
            }
        }
    }

    private MotionEvent swapXY(MotionEvent ev) {
        if (AppState.get().rotateViewPager == 90) {
            float width = getWidth();
            float height = getHeight();

            float newX = (ev.getY() / height) * width;
            float newY = (ev.getX() / width) * height;

            ev.setLocation(newX, newY);
        }

        return ev;
    }


    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        boolean intercepted = super.onInterceptTouchEvent(swapXY(ev));
        swapXY(ev);
        return intercepted;
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        return super.onTouchEvent(swapXY(ev));
    }

}