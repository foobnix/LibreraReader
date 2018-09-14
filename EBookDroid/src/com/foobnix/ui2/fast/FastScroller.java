/*
 * Copyright 2016 L4 Digital LLC. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.foobnix.ui2.fast;


import com.foobnix.pdf.info.R;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.support.v4.content.ContextCompat;
import android.support.v4.graphics.drawable.DrawableCompat;
import android.support.v4.view.GravityCompat;
import android.support.v4.view.ViewCompat;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewPropertyAnimator;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

@SuppressLint("NewApi")
public class FastScroller extends LinearLayout {

    public interface SectionIndexer {

        String getSectionText(int position);
    }

    private static final int sBubbleAnimDuration = 100;
    private static final int sScrollbarAnimDuration = 300;
    private static final int sScrollbarHideDelay = 500;
    private static final int sTrackSnapRange = 5;

    private int mBubbleColor;
    private int mHandleColor;

    private int mHeight;
    private boolean mHideScrollbar;
    private SectionIndexer mSectionIndexer;
    private ViewPropertyAnimator mScrollbarAnimator;
    private ViewPropertyAnimator mBubbleAnimator;
    private RecyclerView mRecyclerView;
    private TextView mBubbleView;
    private ImageView mHandleView;
    private ImageView mTrackView;
    private View mScrollbar;
    private Drawable mBubbleImage;
    private Drawable mHandleImage;
    private Drawable mTrackImage;

    private FastScrollStateChangeListener mFastScrollStateChangeListener;

    private Runnable mScrollbarHider = new Runnable() {

        @Override
        public void run() {
            hideScrollbar();
        }
    };

    private RecyclerView.OnScrollListener mScrollListener = new RecyclerView.OnScrollListener() {

        @Override
        public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
            if (!mHandleView.isSelected() && isEnabled()) {
                setViewPositions(getScrollProportion(recyclerView));
            }
        }

        @Override
        public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
            super.onScrollStateChanged(recyclerView, newState);

            if (isEnabled()) {
                switch (newState) {
                case RecyclerView.SCROLL_STATE_DRAGGING:
                    getHandler().removeCallbacks(mScrollbarHider);
                    cancelAnimation(mScrollbarAnimator);

                    if (!isViewVisible(mScrollbar)) {
                        showScrollbar();
                    }

                    break;

                case RecyclerView.SCROLL_STATE_IDLE:
                    if (mHideScrollbar && !mHandleView.isSelected()) {
                        getHandler().postDelayed(mScrollbarHider, sScrollbarHideDelay);
                    }

                    break;
                }
            }
        }
    };

    public FastScroller(Context context) {
        super(context);
        layout(context, null);
        setLayoutParams(new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.MATCH_PARENT));
    }

    public FastScroller(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public FastScroller(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        layout(context, attrs);
        setLayoutParams(generateLayoutParams(attrs));
    }

    @Override
    public void setLayoutParams(ViewGroup.LayoutParams params) {
        params.width = LayoutParams.WRAP_CONTENT;
        super.setLayoutParams(params);
    }

    public void setLayoutParams(ViewGroup viewGroup) {
        if (viewGroup instanceof FrameLayout) {
            FrameLayout.LayoutParams layoutParams = (FrameLayout.LayoutParams) getLayoutParams();

            layoutParams.gravity = GravityCompat.END;
            setLayoutParams(layoutParams);
        } else if (viewGroup instanceof RelativeLayout) {
            RelativeLayout.LayoutParams layoutParams = (RelativeLayout.LayoutParams) getLayoutParams();

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
                layoutParams.addRule(RelativeLayout.ALIGN_PARENT_END);
            } else {
                layoutParams.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
            }
            setLayoutParams(layoutParams);
        } else {
            throw new IllegalArgumentException("Parent ViewGroup must be a CoordinatorLayout, FrameLayout, or RelativeLayout");
        }
    }

    public void setSectionIndexer(SectionIndexer sectionIndexer) {
        mSectionIndexer = sectionIndexer;
    }

    public void attachRecyclerView(RecyclerView recyclerView) {
        mRecyclerView = recyclerView;

        if (mRecyclerView != null) {
            mRecyclerView.addOnScrollListener(mScrollListener);
        }
    }

    public void detachRecyclerView() {
        if (mRecyclerView != null) {
            mRecyclerView.removeOnScrollListener(mScrollListener);
            mRecyclerView = null;
        }
    }

    /**
     * Hide the scrollbar when not scrolling.
     *
     * @param hideScrollbar True to hide the scrollbar, false to show
     */
    public void setHideScrollbar(boolean hideScrollbar) {
        mHideScrollbar = hideScrollbar;
        mScrollbar.setVisibility(hideScrollbar ? GONE : VISIBLE);
    }

    /**
     * Display a scroll track while scrolling.
     *
     * @param visible True to show scroll track, false to hide
     */
    public void setTrackVisible(boolean visible) {
        mTrackView.setVisibility(visible ? VISIBLE : GONE);
    }

    /**
     * Set the color of the scroll track.
     *
     * @param color The color for the scroll track
     */
    public void setTrackColor(int color) {
        int trackColor = color;

        if (mTrackImage == null) {
            mTrackImage = DrawableCompat.wrap(ContextCompat.getDrawable(getContext(), R.drawable.fastscroll_track));
            mTrackImage.mutate();
        }

        DrawableCompat.setTint(mTrackImage, trackColor);
        mTrackView.setImageDrawable(mTrackImage);
    }

    /**
     * Set the color for the scroll handle.
     *
     * @param color The color for the scroll handle
     */
    public void setHandleColor(int color) {
        mHandleColor = color;

        if (mHandleImage == null) {
            mHandleImage = DrawableCompat.wrap(ContextCompat.getDrawable(getContext(), R.drawable.fastscroll_handle));
            mHandleImage.mutate();
        }

        DrawableCompat.setTint(mHandleImage, mHandleColor);
        mHandleView.setImageDrawable(mHandleImage);
    }

    /**
     * Set the background color of the index bubble.
     *
     * @param color The background color for the index bubble
     */
    public void setBubbleColor(int color) {
        mBubbleColor = color;

        if (mBubbleImage == null) {
            mBubbleImage = DrawableCompat.wrap(ContextCompat.getDrawable(getContext(), R.drawable.fastscroll_bubble));
            mBubbleImage.mutate();
        }

        DrawableCompat.setTint(mBubbleImage, mBubbleColor);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            mBubbleView.setBackground(mBubbleImage);
        } else {
            //noinspection deprecation
            mBubbleView.setBackgroundDrawable(mBubbleImage);
        }
    }

    /**
     * Set the text color of the index bubble.
     *
     * @param color The text color for the index bubble
     */
    public void setBubbleTextColor(int color) {
        mBubbleView.setTextColor(color);
    }

    /**
     * Set the fast scroll state change listener.
     *
     * @param fastScrollStateChangeListener The interface that will listen to fastscroll state change events
     */
    public void setFastScrollStateChangeListener(FastScrollStateChangeListener fastScrollStateChangeListener) {
        mFastScrollStateChangeListener = fastScrollStateChangeListener;
    }

    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        setVisibility(enabled ? VISIBLE : GONE);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getAction()) {
        case MotionEvent.ACTION_DOWN:
            if (event.getX() < mHandleView.getX() - ViewCompat.getPaddingStart(mHandleView)) {
                return false;
            }

            setHandleSelected(true);

            getHandler().removeCallbacks(mScrollbarHider);
            cancelAnimation(mScrollbarAnimator);
            cancelAnimation(mBubbleAnimator);

            if (!isViewVisible(mScrollbar)) {
                showScrollbar();
            }

            if (mSectionIndexer != null && !isViewVisible(mBubbleView)) {
                showBubble();
            }

            if (mFastScrollStateChangeListener != null) {
                mFastScrollStateChangeListener.onFastScrollStart();
            }
        case MotionEvent.ACTION_MOVE:
            final float y = event.getY();
            setViewPositions(y);
            setRecyclerViewPosition(y);
            return true;
        case MotionEvent.ACTION_UP:
        case MotionEvent.ACTION_CANCEL:
            setHandleSelected(false);

            if (mHideScrollbar) {
                getHandler().postDelayed(mScrollbarHider, sScrollbarHideDelay);
            }

            if (isViewVisible(mBubbleView)) {
                hideBubble();
            }

            if (mFastScrollStateChangeListener != null) {
                mFastScrollStateChangeListener.onFastScrollStop();
            }

            return true;
        }

        return super.onTouchEvent(event);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        mHeight = h;
    }

    private void setRecyclerViewPosition(float y) {
        if (mRecyclerView != null && mRecyclerView.getAdapter() != null) {
            int itemCount = mRecyclerView.getAdapter().getItemCount();
            float proportion;

            if (mHandleView.getY() == 0) {
                proportion = 0f;
            } else if (mHandleView.getY() + mHandleView.getHeight() >= mHeight - sTrackSnapRange) {
                proportion = 1f;
            } else {
                proportion = y / mHeight;
            }

            int targetPos = getValueInRange(0, itemCount - 1, (int) (proportion * itemCount));
            mRecyclerView.getLayoutManager().scrollToPosition(targetPos);

            if (mSectionIndexer != null) {
                mBubbleView.setText(mSectionIndexer.getSectionText(targetPos));
            }
        }
    }

    private float getScrollProportion(RecyclerView recyclerView) {
        final int verticalScrollOffset = recyclerView.computeVerticalScrollOffset();
        final int verticalScrollRange = recyclerView.computeVerticalScrollRange();
        float proportion = verticalScrollOffset / ((float) verticalScrollRange - mHeight);
        return mHeight * proportion;
    }

    private int getValueInRange(int min, int max, int value) {
        int minimum = Math.max(min, value);
        return Math.min(minimum, max);
    }

    private void setViewPositions(float y) {
        int bubbleHeight = mBubbleView.getHeight();
        int handleHeight = mHandleView.getHeight();

        mBubbleView.setY(getValueInRange(0, mHeight - bubbleHeight - handleHeight / 2, (int) (y - bubbleHeight)));
        mHandleView.setY(getValueInRange(0, mHeight - handleHeight, (int) (y - handleHeight / 2)));
    }

    private boolean isViewVisible(View view) {
        return view != null && view.getVisibility() == VISIBLE;
    }

    private void cancelAnimation(ViewPropertyAnimator animator) {
        if (animator != null) {
            animator.cancel();
        }
    }

    private void showBubble() {
        mBubbleView.setVisibility(VISIBLE);
        mBubbleAnimator = mBubbleView.animate().alpha(1f)
                .setDuration(sBubbleAnimDuration)
                .setListener(new AnimatorListenerAdapter() {
                    // adapter required for new alpha value to stick
                });
    }

    private void hideBubble() {
        mBubbleAnimator = mBubbleView.animate().alpha(0f)
                .setDuration(sBubbleAnimDuration)
                .setListener(new AnimatorListenerAdapter() {

                    @Override
                    public void onAnimationEnd(Animator animation) {
                        super.onAnimationEnd(animation);
                        mBubbleView.setVisibility(GONE);
                        mBubbleAnimator = null;
                    }

                    @Override
                    public void onAnimationCancel(Animator animation) {
                        super.onAnimationCancel(animation);
                        mBubbleView.setVisibility(GONE);
                        mBubbleAnimator = null;
                    }
                });
    }

    private void showScrollbar() {
        if (mRecyclerView.computeVerticalScrollRange() - mHeight > 0) {
            float transX = getResources().getDimensionPixelSize(R.dimen.fastscroll_scrollbar_padding);

            mScrollbar.setTranslationX(transX);
            mScrollbar.setVisibility(VISIBLE);
            mScrollbarAnimator = mScrollbar.animate().translationX(0f).alpha(1f)
                    .setDuration(sScrollbarAnimDuration)
                    .setListener(new AnimatorListenerAdapter() {
                        // adapter required for new alpha value to stick
                    });
        }
    }

    private void hideScrollbar() {
        float transX = getResources().getDimensionPixelSize(R.dimen.fastscroll_scrollbar_padding);

        mScrollbarAnimator = mScrollbar.animate().translationX(transX).alpha(0f)
                .setDuration(sScrollbarAnimDuration)
                .setListener(new AnimatorListenerAdapter() {

                    @Override
                    public void onAnimationEnd(Animator animation) {
                        super.onAnimationEnd(animation);
                        mScrollbar.setVisibility(GONE);
                        mScrollbarAnimator = null;
                    }

                    @Override
                    public void onAnimationCancel(Animator animation) {
                        super.onAnimationCancel(animation);
                        mScrollbar.setVisibility(GONE);
                        mScrollbarAnimator = null;
                    }
                });
    }

    private void setHandleSelected(boolean selected) {
        mHandleView.setSelected(selected);
        DrawableCompat.setTint(mHandleImage, selected ? mBubbleColor : mHandleColor);
    }

    private void layout(Context context, AttributeSet attrs) {
        inflate(context, R.layout.fastscroller, this);

        setClipChildren(false);
        setOrientation(HORIZONTAL);

        mBubbleView = (TextView) findViewById(R.id.fastscroll_bubble);
        mHandleView = (ImageView) findViewById(R.id.fastscroll_handle);
        mTrackView = (ImageView) findViewById(R.id.fastscroll_track);
        mScrollbar = findViewById(R.id.fastscroll_scrollbar);

        int bubbleColor = Color.GRAY;
        int handleColor = Color.DKGRAY;
        int trackColor = Color.LTGRAY;
        int textColor = Color.WHITE;

        boolean hideScrollbar = true;
        boolean showTrack = false;

        if (attrs != null) {
            TypedArray typedArray = context.obtainStyledAttributes(attrs, R.styleable.FastScrollRecyclerView, 0, 0);

            if (typedArray != null) {
                try {
                    bubbleColor = typedArray.getColor(R.styleable.FastScrollRecyclerView_bubbleColor, bubbleColor);
                    handleColor = typedArray.getColor(R.styleable.FastScrollRecyclerView_handleColor, handleColor);
                    trackColor = typedArray.getColor(R.styleable.FastScrollRecyclerView_trackColor, trackColor);
                    textColor = typedArray.getColor(R.styleable.FastScrollRecyclerView_bubbleTextColor, textColor);
                    showTrack = typedArray.getBoolean(R.styleable.FastScrollRecyclerView_showTrack, false);
                    hideScrollbar = typedArray.getBoolean(R.styleable.FastScrollRecyclerView_hideScrollbar, true);
                } finally {
                    typedArray.recycle();
                }
            }
        }

        setTrackColor(trackColor);
        setHandleColor(handleColor);
        setBubbleColor(bubbleColor);
        setBubbleTextColor(textColor);
        setHideScrollbar(hideScrollbar);
        setTrackVisible(showTrack);
    }
}