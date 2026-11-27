package com.foobnix.pdf.search.view;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.animation.LinearInterpolator;
import android.widget.Scroller;

import androidx.viewpager.widget.PagerAdapter;
import androidx.viewpager.widget.ViewPager;

import com.foobnix.android.utils.Dips;
import com.foobnix.android.utils.LOG;
import com.foobnix.model.AppState;

import java.lang.reflect.Field;

public class VerticalViewPager extends CustomViewPager {

    private MyScroller value;

    private PageCurlView curl;
    private final int curlSlop;
    private float curlSteep = 1f;
    private float curlDownX, curlDownY;
    /** The finger may still start a page turn; it is turning one. */
    private boolean curlTracking, curlDragging;
    private VelocityTracker velocity;

    public VerticalViewPager(Context context, AttributeSet attrs) {
        super(context, attrs);
        curlSlop = ViewConfiguration.get(context).getScaledPagingTouchSlop();
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

    /** Pages are turned as in a paper book, by this view over the pager. */
    public void setPageCurl(PageCurlView curl) {
        this.curl = curl;
    }

    private boolean isCurl() {
        return curl != null && PageCurlView.isOn();
    }

    /**
     * Swipes turn the page as in a paper book, not the pager: a move across the page starts the
     * turn as the pager would start a scroll. Returns whether the move is a turn.
     */
    private boolean curlTouch(MotionEvent ev) {
        final int action = ev.getActionMasked();
        final float x = ev.getX();
        final float y = ev.getY();
        if (action == MotionEvent.ACTION_DOWN) {
            curl.finishNow();
            curlDownX = x;
            curlDownY = y;
            curlDragging = false;
            curlTracking = true;
            curlSteep = curl.steepAt(x, y);
            if (velocity != null) {
                velocity.recycle();
            }
            velocity = VelocityTracker.obtain();
            velocity.addMovement(ev);
            return false;
        }
        if (velocity != null) {
            velocity.addMovement(ev);
        }
        if (action == MotionEvent.ACTION_MOVE) {
            if (!curlDragging && curlTracking) {
                if (ev.getPointerCount() != 1 || AppState.get().selectedText != null) {
                    LOG.d("PageCurl", "not started", ev.getPointerCount() != 1 ? "two fingers" : "text selected");
                    curlTracking = false;
                    return false;
                }
                // Across the page as it turns: down the screen for a calendar
                final boolean calendar = PageCurlView.isVertical();
                final float dx = calendar ? y - curlDownY : x - curlDownX;
                final float dy = calendar ? x - curlDownX : y - curlDownY;
                // A page is pulled by its corner on a slant: any drag more across than down turns
                // it, and from a corner one twice as much down as across still does. The page
                // itself takes the steeper ones, to move it
                final boolean across = Math.abs(dx) >= Math.abs(dy) * curlSteep;
                if (Math.abs(dx) > curlSlop && across) {
                    if (!calendar && canScroll(this, false, (int) dx, (int) x, (int) y)) {
                        LOG.d("PageCurl", "not started", "the page scrolls across");
                        curlTracking = false;
                        return false;
                    }
                    // Across the book as it is read: right to left in a right-to-left book; up
                    // for a calendar
                    final boolean forward = !calendar && isRtl() ? dx > 0 : dx < 0;
                    curlTracking = false;
                    final boolean started = curl.startDrag(forward ? 1 : -1, curlDownX, curlDownY);
                    LOG.d("PageCurl", started ? "started" : "not started, no page", forward ? "next" : "prev",
                          (int) curlDownX, (int) curlDownY, "by", (int) dx, (int) dy, "steep", curlSteep);
                    if (started) {
                        curlDragging = true;
                        if (getParent() != null) {
                            getParent().requestDisallowInterceptTouchEvent(true);
                        }
                    }
                } else if (Math.abs(dy) > curlSlop && !across) {
                    LOG.d("PageCurl", "not started, down not across", (int) curlDownX, (int) curlDownY,
                          "by", (int) dx, (int) dy, "steep", curlSteep);
                    curlTracking = false;
                }
            }
            if (curlDragging) {
                curl.drag(x, y);
            }
            return curlDragging;
        }
        if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) {
            final boolean was = curlDragging;
            if (curlDragging) {
                if (action == MotionEvent.ACTION_UP && velocity != null) {
                    velocity.computeCurrentVelocity(1000);
                    curl.release(velocity.getXVelocity(), velocity.getYVelocity());
                } else {
                    curl.cancelDrag();
                }
            }
            curlDragging = false;
            curlTracking = false;
            if (velocity != null) {
                velocity.recycle();
                velocity = null;
            }
            return was;
        }
        return curlDragging;
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        if (!AppState.get().isEnableHorizontalSwipe) {
            return false;
        }
        if(AppState.get().isSelectTexByTouch){
            return false;
        }

        if (isCurl()) {
            return curlTouch(ev);
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
        if(AppState.get().isSelectTexByTouch){
            return false;
        }

        if (isCurl()) {
            curlTouch(ev);
            return true;
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