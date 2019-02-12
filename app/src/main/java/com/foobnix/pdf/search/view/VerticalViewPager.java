package com.foobnix.pdf.search.view;

import java.lang.reflect.Field;

import com.foobnix.android.utils.Dips;
import com.foobnix.android.utils.LOG;
import com.foobnix.pdf.info.wrapper.AppState;

import android.annotation.TargetApi;
import android.content.Context;
import android.os.Build;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.LinearInterpolator;
import android.widget.Scroller;

public class VerticalViewPager extends CustomViewPager {

    private MyScroller value;

    public VerticalViewPager(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
        setMyScroller();
    }

    private void init() {
        if (AppState.get().rotateViewPager == 90) {
            setPageTransformer(true, new VerticalPageTransformer());
        }
    }

    private void setMyScroller() {
        try {
            Class<?> viewpager = ViewPager.class;
            Field scroller = viewpager.getDeclaredField("mScroller");
            scroller.setAccessible(true);
            value = new MyScroller(getContext());
            scroller.set(this, value);

            Field mFlingDistance = viewpager.getDeclaredField("mFlingDistance");
            mFlingDistance.setAccessible(true);
            mFlingDistance.set(this, Dips.DP_10);

            Field mMinimumVelocity = viewpager.getDeclaredField("mMinimumVelocity");
            mMinimumVelocity.setAccessible(true);
            mMinimumVelocity.set(this, 0);

        } catch (Exception e) {
            LOG.e(e);
        }

    }

    public class MyScroller extends Scroller {
        public MyScroller(Context context) {
            super(context, new LinearInterpolator());
        }

        @Override
        public void startScroll(int startX, int startY, int dx, int dy, int duration) {
            super.startScroll(startX, startY, dx, dy, 175);
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
        if (!AppState.get().isEnableHorizontalSwipe) {
            return false;
        }
        if (value != null && !value.isFinished()) {
            return false;
        }

        boolean intercepted = super.onInterceptTouchEvent(swapXY(ev));
        swapXY(ev);
        return intercepted;
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        if (!AppState.get().isEnableHorizontalSwipe) {
            return false;
        }

        return super.onTouchEvent(swapXY(ev));
    }

    @Override
    public void setAdapter(PagerAdapter adapter) {
        try {
            super.setAdapter(adapter);
        } catch (Exception e) {
            LOG.e(e);
        }
    }

}