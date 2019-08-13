package com.jmedeisis.draglinearlayout;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.LayoutTransition;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.util.AttributeSet;
import android.util.Log;
import android.util.SparseArray;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewTreeObserver;
import android.view.ViewTreeObserver.OnPreDrawListener;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.core.view.MotionEventCompat;

/**
 * copy from github:https://github.com/justasm/DragLinearLayout
 * <p/>
 * Edited by @author chensuilun add support for HORIZONTAL , simplify code ,
 * some useful callback ,add OverScroll constrain , long click to drag support
 */
public class DragLinearLayout extends LinearLayout {
    private static final String TAG = DragLinearLayout.class.getSimpleName();
    private static final long NOMINAL_SWITCH_DURATION = 150;
    private static final long MIN_SWITCH_DURATION = NOMINAL_SWITCH_DURATION;
    private static final long MAX_SWITCH_DURATION = NOMINAL_SWITCH_DURATION * 2;
    private static final float NOMINAL_DISTANCE = 20;
    private final float mNominalDistanceScaled;

    /**
     * Use with
     * {@link DragLinearLayout#setOnViewSwapListener(DragLinearLayout.OnViewSwapListener)}
     * to listen for draggable mView swaps.
     */
    public interface OnViewSwapListener {
        /**
         * Invoked right before the two items are swapped due to a drag event. After the
         * swap, the firstView will be in the secondPosition, and vice versa.
         * <p/>
         * No guarantee is made as to which of the two has a lesser/greater mPosition.
         */
        void onSwap(View draggedView, int initPosition, View swappedView, int swappedPosition);

        /**
         * Invoked when swap action finish
         */
        void onSwapFinish();
    }

    private OnViewSwapListener mSwapListener;

    private LayoutTransition mLayoutTransition;

    /**
     * Mapping from child index to drag-related info container.
     * Presence of mapping implies the child can be dragged, and is considered for swaps with the
     * currently dragged item.
     */
    private final SparseArray<DraggableChild> mDraggableChildren;

    private class DraggableChild {
        /**
         * If non-null, a reference to an on-going mPosition animation.
         */
        private ValueAnimator mValueAnimator;

        public void endExistingAnimation() {
            if (null != mValueAnimator)
                mValueAnimator.end();
        }

        public void cancelExistingAnimation() {
            if (null != mValueAnimator)
                mValueAnimator.cancel();
        }
    }

    /**
     * Holds state information about the currently dragged item.
     * <p/>
     * Rough lifecycle:
     * <li>#startDetectingOnPossibleDrag - #mDetecting == true</li>
     * <li>if drag is recognised, #onDragStart - #mDragging == true</li>
     * <li>if drag ends, #onDragStop - #mDragging == false, #settling == true</li>
     * <li>if gesture ends without drag, or settling finishes, #stopDetecting -
     * #mDetecting == false</li>
     */
    private class DragItem {
        private View mView;
        private int mStartVisibility;
        private BitmapDrawable mBitmapDrawable;
        private int mPosition;

        private int mStartTop;
        private int mHeight;
        private int mTotalDragOffset;
        private int mTargetTopOffset;

        private int mStartLeft;
        private int mWidth;
        private int mTargetLeftOffset;

        private ValueAnimator mSettleAnimation;

        private boolean mDetecting;
        private boolean mDragging;

        private int mMaxLeftOffset;
        private int mMaxRightOffset;
        private int mMaxTopOffset;
        private int mMaxBottomOffset;

        public DragItem() {
            stopDetecting();
        }

        public void startDetectingOnPossibleDrag(final View view, final int position) {
            this.mView = view;
            this.mStartVisibility = view.getVisibility();
            this.mBitmapDrawable = getDragDrawable(view);
            this.mPosition = position;
            this.mStartTop = view.getTop();
            this.mHeight = view.getHeight();
            mStartLeft = view.getLeft();
            mWidth = view.getWidth();
            this.mTotalDragOffset = 0;
            this.mTargetTopOffset = 0;
            this.mTargetLeftOffset = 0;
            this.mSettleAnimation = null;

            mMaxLeftOffset = -mStartLeft;
            mMaxRightOffset = getWidth() - view.getRight();
            mMaxTopOffset = -mStartTop;
            mMaxBottomOffset = getHeight() - view.getBottom();

            this.mDetecting = true;
        }

        public void onDragStart() {
            mView.setVisibility(View.INVISIBLE);
            this.mDragging = true;
        }

        public void setTotalOffset(int offset) {
            mTotalDragOffset = offset;
            if (!mOverScrollable) {
                if (getOrientation() == HORIZONTAL) {
                    mTotalDragOffset = Math.min(Math.max(mMaxLeftOffset, mTotalDragOffset), mMaxRightOffset);
                } else {
                    mTotalDragOffset = Math.min(Math.max(mMaxTopOffset, mTotalDragOffset), mMaxBottomOffset);
                }
            }
            updateTargetLocation();
        }

        public void updateTargetLocation() {
            if (getOrientation() == VERTICAL) {
                updateTargetTop();
            } else {
                updateTargetLeft();
            }
        }

        private void updateTargetLeft() {
            mTargetLeftOffset = mStartLeft - mView.getLeft() + mTotalDragOffset;
        }

        private void updateTargetTop() {
            mTargetTopOffset = mStartTop - mView.getTop() + mTotalDragOffset;
        }

        public void onDragStop() {
            this.mDragging = false;
        }

        public boolean settling() {
            return null != mSettleAnimation;
        }

        public void stopDetecting() {
            this.mDetecting = false;
            if (null != mView)
                mView.setVisibility(mStartVisibility);
            mView = null;
            mStartVisibility = -1;
            mBitmapDrawable = null;
            mPosition = -1;
            mStartTop = -1;
            mHeight = -1;
            mStartLeft = -1;
            mWidth = -1;
            mTotalDragOffset = 0;
            mTargetTopOffset = 0;
            mTargetLeftOffset = 0;
            if (null != mSettleAnimation)
                mSettleAnimation.end();
            mSettleAnimation = null;
        }
    }

    /**
     * The currently dragged item, if {@link DragLinearLayout.DragItem#mDetecting}.
     */
    private final DragItem mDragItem;
    private final int mSlop;

    private static final int INVALID_POINTER_ID = -1;
    private int mDownY = -1;
    private int mDownX = -1;
    private int mActivePointerId = INVALID_POINTER_ID;


    public DragLinearLayout(Context context) {
        this(context, null);
    }

    public DragLinearLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        mDraggableChildren = new SparseArray<DraggableChild>();
        mDragItem = new DragItem();
        ViewConfiguration vc = ViewConfiguration.get(context);
        mSlop = vc.getScaledTouchSlop();
        final Resources resources = getResources();
        mNominalDistanceScaled = (int) (NOMINAL_DISTANCE * resources.getDisplayMetrics().density + 0.5f);
    }

    /**
     * Calls {@link #addView(android.view.View)} followed by {@link #setViewDraggable(android.view.View, android.view.View)}.
     */
    public void addDragView(View child, View dragHandle) {
        addView(child);
        setViewDraggable(child, dragHandle);
    }

    /**
     * Calls {@link #addView(android.view.View, int)} followed by
     * {@link #setViewDraggable(android.view.View, android.view.View)} and correctly updates the
     * drag-ability state of all existing views.
     */
    public void addDragView(View child, View dragHandle, int index) {
        addView(child, index);

        // update drag-able children mappings
        final int numMappings = mDraggableChildren.size();
        for (int i = numMappings - 1; i >= 0; i--) {
            final int key = mDraggableChildren.keyAt(i);
            if (key >= index) {
                mDraggableChildren.put(key + 1, mDraggableChildren.get(key));
            }
        }

        setViewDraggable(child, dragHandle);
    }

    /**
     * Makes the child a candidate for mDragging. Must be an existing child of this
     * layout.
     */
    public void setViewDraggable(View child, View dragHandle) {
        if (null == child || null == dragHandle) {
            throw new IllegalArgumentException(
                    "Draggable children and their drag handles must not be null.");
        }

        if (this == child.getParent()) {
            dragHandle.setOnTouchListener(new DragHandleOnTouchListener(child));
            dragHandle.setOnLongClickListener(mLongClickDragListener);
            mDraggableChildren.put(indexOfChild(child), new DraggableChild());
        } else {
            Log.e(TAG, child + " is not a child, cannot make draggable.");
        }
    }

    /**
     * Calls {@link #removeView(android.view.View)} and correctly updates the drag-ability state of
     * all remaining views.
     */
    @SuppressWarnings("UnusedDeclaration")
    public void removeDragView(View child) {
        if (this == child.getParent()) {
            final int index = indexOfChild(child);
            removeView(child);
            child.setOnTouchListener(null);
            child.setOnLongClickListener(null);
            // update drag-able children mappings
            final int mappings = mDraggableChildren.size();
            for (int i = 0; i < mappings; i++) {
                final int key = mDraggableChildren.keyAt(i);
                if (key >= index) {
                    DraggableChild next = mDraggableChildren.get(key + 1);
                    if (null == next) {
                        mDraggableChildren.delete(key);
                    } else {
                        mDraggableChildren.put(key, next);
                    }
                }
            }
        }
    }


    @Override
    public void removeAllViews() {
        int count = getChildCount();
        for (int i = 0; i < count; i++) {
            getChildAt(i).setOnLongClickListener(null);
            getChildAt(i).setOnTouchListener(null);
        }
        super.removeAllViews();
        mDraggableChildren.clear();
    }


    /**
     * See {@link DragLinearLayout.OnViewSwapListener}.
     */
    public void setOnViewSwapListener(OnViewSwapListener swapListener) {
        this.mSwapListener = swapListener;
    }

    /**
     * A linear relationship b/w distance and duration, bounded.
     */
    private long getTranslateAnimationDuration(float distance) {
        return Math.min(MAX_SWITCH_DURATION, Math.max(MIN_SWITCH_DURATION,
                (long) (NOMINAL_SWITCH_DURATION * Math.abs(distance) / mNominalDistanceScaled)));
    }

    /**
     * Initiates a new {@link #mDragItem} unless the current one is still
     * {@link DragLinearLayout.DragItem#mDetecting}.
     */
    public void startDetectingDrag(View child) {
        if (mDragItem.mDetecting)
            return; // existing drag in process, only one at a time is allowed

        final int position = indexOfChild(child);

        // complete any existing animations, both for the newly selected child and the previous dragged one
        mDraggableChildren.get(position).endExistingAnimation();

        mDragItem.startDetectingOnPossibleDrag(child, position);
    }

    private void startDrag() {
        // remove layout transition, it conflicts with drag animation
        // we will restore it after drag animation end, see onDragStop()
        mLayoutTransition = getLayoutTransition();
        if (mLayoutTransition != null) {
            setLayoutTransition(null);
        }

        mDragItem.onDragStart();
        requestDisallowInterceptTouchEvent(true);
    }

    /**
     * Animates the dragged item to its final resting mPosition.
     */
    private void onDragStop() {
        if (getOrientation() == VERTICAL) {
            mDragItem.mSettleAnimation = ValueAnimator.ofFloat(mDragItem.mTotalDragOffset, mDragItem.mTotalDragOffset - mDragItem.mTargetTopOffset).setDuration(getTranslateAnimationDuration(mDragItem.mTargetTopOffset));
        } else {
            mDragItem.mSettleAnimation = ValueAnimator.ofFloat(mDragItem.mTotalDragOffset, mDragItem.mTotalDragOffset - mDragItem.mTargetLeftOffset).setDuration(getTranslateAnimationDuration(mDragItem.mTargetLeftOffset));
        }

        mDragItem.mSettleAnimation.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                if (!mDragItem.mDetecting)
                    return; // already stopped

                mDragItem.setTotalOffset(((Float) animation.getAnimatedValue()).intValue());

                invalidate();
            }
        });
        mDragItem.mSettleAnimation.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationStart(Animator animation) {
                mDragItem.onDragStop();
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                if (!mDragItem.mDetecting) {
                    return; // already stopped
                }

                mDragItem.mSettleAnimation = null;
                mDragItem.stopDetecting();

                // restore layout transition
                if (mLayoutTransition != null && getLayoutTransition() == null) {
                    setLayoutTransition(mLayoutTransition);
                }
                if (mSwapListener != null) {
                    mSwapListener.onSwapFinish();
                }
            }
        });
        mDragItem.mSettleAnimation.start();
    }

    /**
     * Updates the dragged item with the given total offset from its starting
     * mPosition. Evaluates and executes draggable mView swaps.
     */
    private void onDrag(final int offset) {
        if (getOrientation() == VERTICAL) {
            mDragItem.setTotalOffset(offset);
            invalidate();

            int currentTop = mDragItem.mStartTop + mDragItem.mTotalDragOffset;

            int belowPosition = nextDraggablePosition(mDragItem.mPosition);
            int abovePosition = previousDraggablePosition(mDragItem.mPosition);

            View belowView = getChildAt(belowPosition);
            View aboveView = getChildAt(abovePosition);

            final boolean isBelow = (belowView != null) && (currentTop + mDragItem.mHeight > belowView.getTop() + belowView.getHeight() / 2);
            final boolean isAbove = (aboveView != null) && (currentTop < aboveView.getTop() + aboveView.getHeight() / 2);

            if (isBelow || isAbove) {
                final View switchView = isBelow ? belowView : aboveView;

                // swap elements
                final int originalPosition = mDragItem.mPosition;
                final int switchPosition = isBelow ? belowPosition : abovePosition;

                mDraggableChildren.get(switchPosition).cancelExistingAnimation();
                final float switchViewStartY = switchView.getY();

                if (null != mSwapListener) {
                    mSwapListener.onSwap(mDragItem.mView, mDragItem.mPosition, switchView, switchPosition);
                }

                if (isBelow) {
                    removeViewAt(originalPosition);
                    removeViewAt(switchPosition - 1);

                    addView(belowView, originalPosition);
                    addView(mDragItem.mView, switchPosition);
                } else {
                    removeViewAt(switchPosition);
                    removeViewAt(originalPosition - 1);

                    addView(mDragItem.mView, switchPosition);
                    addView(aboveView, originalPosition);
                }
                mDragItem.mPosition = switchPosition;

                final ViewTreeObserver switchViewObserver = switchView.getViewTreeObserver();
                switchViewObserver.addOnPreDrawListener(new OnPreDrawListener() {
                    @Override
                    public boolean onPreDraw() {
                        switchViewObserver.removeOnPreDrawListener(this);

                        final ObjectAnimator switchAnimator = ObjectAnimator.ofFloat(switchView, "y", switchViewStartY, switchView.getTop()).setDuration(getTranslateAnimationDuration(switchView.getTop() - switchViewStartY));
                        switchAnimator.addListener(new AnimatorListenerAdapter() {
                            @Override
                            public void onAnimationStart(Animator animation) {
                                mDraggableChildren.get(originalPosition).mValueAnimator = switchAnimator;
                            }

                            @Override
                            public void onAnimationEnd(Animator animation) {
                                mDraggableChildren.get(originalPosition).mValueAnimator = null;
                            }
                        });
                        switchAnimator.start();

                        return true;
                    }
                });

                final ViewTreeObserver observer = mDragItem.mView.getViewTreeObserver();
                observer.addOnPreDrawListener(new OnPreDrawListener() {
                    @Override
                    public boolean onPreDraw() {
                        observer.removeOnPreDrawListener(this);
                        mDragItem.updateTargetLocation();

                        // TODO test if still necessary..
                        // because mDragItem#mView#getTop() is only up-to-date NOW
                        // (and not right after the #addView() swaps above)
                        // we may need to update an ongoing settle animation
                        if (mDragItem.settling()) {
                            Log.d(TAG, "Updating settle animation");
                            mDragItem.mSettleAnimation.removeAllListeners();
                            mDragItem.mSettleAnimation.cancel();
                            onDragStop();
                        }
                        return true;
                    }
                });
            }
        } else {
            mDragItem.setTotalOffset(offset);
            invalidate();

            int currentLeft = mDragItem.mStartLeft + mDragItem.mTotalDragOffset;

            int nextPosition = nextDraggablePosition(mDragItem.mPosition);
            int prePosition = previousDraggablePosition(mDragItem.mPosition);

            View nextView = getChildAt(nextPosition);
            View preView = getChildAt(prePosition);

            final boolean isToNext = (nextView != null) && (currentLeft + mDragItem.mWidth > nextView.getLeft() + nextView.getWidth() / 2);
            final boolean isToPre = (preView != null) && (currentLeft < preView.getLeft() + preView.getWidth() / 2);

            if (isToNext || isToPre) {
                final View switchView = isToNext ? nextView : preView;

                // swap elements
                final int originalPosition = mDragItem.mPosition;
                final int switchPosition = isToNext ? nextPosition : prePosition;

                mDraggableChildren.get(switchPosition).cancelExistingAnimation();
                final float switchViewStartX = switchView.getX();

                if (null != mSwapListener) {
                    mSwapListener.onSwap(mDragItem.mView, mDragItem.mPosition, switchView, switchPosition);
                }

                if (isToNext) {
                    removeViewAt(originalPosition);
                    removeViewAt(switchPosition - 1);

                    addView(nextView, originalPosition);
                    addView(mDragItem.mView, switchPosition);
                } else {
                    removeViewAt(switchPosition);
                    removeViewAt(originalPosition - 1);

                    addView(mDragItem.mView, switchPosition);
                    addView(preView, originalPosition);
                }
                mDragItem.mPosition = switchPosition;

                final ViewTreeObserver switchViewObserver = switchView.getViewTreeObserver();
                switchViewObserver.addOnPreDrawListener(new OnPreDrawListener() {
                    @Override
                    public boolean onPreDraw() {
                        switchViewObserver.removeOnPreDrawListener(this);

                        final ObjectAnimator switchAnimator = ObjectAnimator.ofFloat(switchView, "x", switchViewStartX, switchView.getLeft()).setDuration(getTranslateAnimationDuration(switchView.getLeft() - switchViewStartX));
                        switchAnimator.addListener(new AnimatorListenerAdapter() {
                            @Override
                            public void onAnimationStart(Animator animation) {
                                mDraggableChildren.get(originalPosition).mValueAnimator = switchAnimator;
                            }

                            @Override
                            public void onAnimationEnd(Animator animation) {
                                mDraggableChildren.get(originalPosition).mValueAnimator = null;
                            }
                        });
                        switchAnimator.start();

                        return true;
                    }
                });

                final ViewTreeObserver observer = mDragItem.mView.getViewTreeObserver();
                observer.addOnPreDrawListener(new OnPreDrawListener() {
                    @Override
                    public boolean onPreDraw() {
                        observer.removeOnPreDrawListener(this);
                        mDragItem.updateTargetLocation();

                        if (mDragItem.settling()) {
                            mDragItem.mSettleAnimation.removeAllListeners();
                            mDragItem.mSettleAnimation.cancel();
                            onDragStop();
                        }
                        return true;
                    }
                });
            }
        }
    }

    private int previousDraggablePosition(int position) {
        int startIndex = mDraggableChildren.indexOfKey(position);
        if (startIndex < 1 || startIndex > mDraggableChildren.size())
            return -1;
        return mDraggableChildren.keyAt(startIndex - 1);
    }

    private int nextDraggablePosition(int position) {
        int startIndex = mDraggableChildren.indexOfKey(position);
        if (startIndex < -1 || startIndex > mDraggableChildren.size() - 2)
            return -1;
        return mDraggableChildren.keyAt(startIndex + 1);
    }

    private Runnable dragUpdater;


    /**
     * By Ken Perlin. See <a href="http://en.wikipedia.org/wiki/Smoothstep">Smoothstep - Wikipedia</a>.
     */
    private static float smootherStep(float edge1, float edge2, float val) {
        val = Math.max(0, Math.min((val - edge1) / (edge2 - edge1), 1));
        return val * val * val * (val * (val * 6 - 15) + 10);
    }

    @Override
    protected void dispatchDraw(@NonNull Canvas canvas) {
        super.dispatchDraw(canvas);

        if (mDragItem.mDetecting && (mDragItem.mDragging || mDragItem.settling())) {
            canvas.save();
            if (getOrientation() == VERTICAL) {
                canvas.translate(0, mDragItem.mTotalDragOffset);
            } else {
                canvas.translate(mDragItem.mTotalDragOffset, 0);
            }
            mDragItem.mBitmapDrawable.draw(canvas);

            canvas.restore();
        }
    }

    /*
     * Note regarding touch handling: In general, we have three cases - 1) User taps
     * outside any children. #onInterceptTouchEvent receives DOWN #onTouchEvent
     * receives DOWN mDragItem.mDetecting == false, we return false and no further
     * events are received 2) User taps on non-interactive drag handle / child, e.g.
     * TextView or ImageView. #onInterceptTouchEvent receives DOWN
     * DragHandleOnTouchListener (attached to each draggable child) #onTouch
     * receives DOWN #startDetectingDrag is called, mDragItem is now mDetecting
     * mView does not handle touch, so our #onTouchEvent receives DOWN
     * mDragItem.mDetecting == true, we #startDrag() and proceed to handle the drag
     * 3) User taps on interactive drag handle / child, e.g. Button.
     * #onInterceptTouchEvent receives DOWN DragHandleOnTouchListener (attached to
     * each draggable child) #onTouch receives DOWN #startDetectingDrag is called,
     * mDragItem is now mDetecting mView handles touch, so our #onTouchEvent is not
     * called yet #onInterceptTouchEvent receives ACTION_MOVE if dy > touch mSlop,
     * we assume user wants to drag and intercept the event #onTouchEvent receives
     * further ACTION_MOVE events, proceed to handle the drag
     *
     * For cases 2) and 3), lifting the active pointer at any point in the sequence
     * of events triggers #onTouchEnd and the mDragItem, if mDetecting, is
     * #stopDetecting.
     */

    @Override
    public boolean onInterceptTouchEvent(MotionEvent event) {
        if (!mDraggable && !mIsLongClickDraggable) {
            return super.onInterceptTouchEvent(event);
        }
        switch (MotionEventCompat.getActionMasked(event)) {
            case MotionEvent.ACTION_DOWN: {
            getParent().requestDisallowInterceptTouchEvent(true);
            if (mDragItem.mDetecting)
                return false; // an existing item is (likely) settling
            mDownY = (int) MotionEventCompat.getY(event, 0);
            mDownX = (int) MotionEventCompat.getX(event, 0);
            mActivePointerId = MotionEventCompat.getPointerId(event, 0);
                break;
            }
            case MotionEvent.ACTION_MOVE: {
            if (!mDraggable) {
                return super.onInterceptTouchEvent(event);
            }
            if (!mDragItem.mDetecting)
                return false;
            if (INVALID_POINTER_ID == mActivePointerId)
                break;
            final int pointerIndex = event.findPointerIndex(mActivePointerId);
                final float y = MotionEventCompat.getY(event, pointerIndex);
            final float x = MotionEventCompat.getX(event, pointerIndex);
            final float dy = y - mDownY;
            final float dx = x - mDownX;
            if (getOrientation() == VERTICAL) {
                if (Math.abs(dy) > mSlop) {
                    startDrag();
                    return true;
                }
            } else {
                if (Math.abs(dx) > mSlop) {
                    startDrag();
                    return true;
                }
                }
                return false;
            }
            case MotionEvent.ACTION_POINTER_UP: {
                final int pointerIndex = MotionEventCompat.getActionIndex(event);
                final int pointerId = MotionEventCompat.getPointerId(event, pointerIndex);

            if (pointerId != mActivePointerId)
                    break; // if active pointer, fall through and cancel!
            }
            case MotionEvent.ACTION_CANCEL:
            case MotionEvent.ACTION_UP: {
            getParent().requestDisallowInterceptTouchEvent(false);
                onTouchEnd();

            if (mDragItem.mDetecting)
                mDragItem.stopDetecting();
                break;
            }
        }

        return false;
    }

    @Override
    public boolean onTouchEvent(@NonNull MotionEvent event) {
        if (!mDraggable && !mIsLongClickDraggable) {
            return super.onTouchEvent(event);
        }
        switch (MotionEventCompat.getActionMasked(event)) {
            case MotionEvent.ACTION_DOWN: {
            if (!mDragItem.mDetecting || mDragItem.settling())
                return false;
                startDrag();
                return true;
            }
            case MotionEvent.ACTION_MOVE: {
            if (!mDraggable && !mIsEnterLongClick) {
                return super.onTouchEvent(event);
            }
            if (!mDragItem.mDragging)
                break;
            if (INVALID_POINTER_ID == mActivePointerId)
                break;

            int pointerIndex = event.findPointerIndex(mActivePointerId);
                int lastEventY = (int) MotionEventCompat.getY(event, pointerIndex);
            int lastEventX = (int) MotionEventCompat.getX(event, pointerIndex);
            if (getOrientation() == VERTICAL) {
                int deltaY = lastEventY - mDownY;
                onDrag(deltaY);
            } else {
                int deltaX = lastEventX - mDownX;
                onDrag(deltaX);
            }
                return true;
            }
            case MotionEvent.ACTION_POINTER_UP: {
                final int pointerIndex = MotionEventCompat.getActionIndex(event);
                final int pointerId = MotionEventCompat.getPointerId(event, pointerIndex);

            if (pointerId != mActivePointerId)
                    break; // if active pointer, fall through and cancel!
            }
            case MotionEvent.ACTION_CANCEL:
            case MotionEvent.ACTION_UP: {
                onTouchEnd();

            if (mDragItem.mDragging) {
                    onDragStop();
            } else if (mDragItem.mDetecting) {
                mDragItem.stopDetecting();
                }
                return true;
            }
        }
        return false;
    }

    private void onTouchEnd() {
        mDownY = -1;
        mDownX = -1;
        mIsEnterLongClick = false;
        mActivePointerId = INVALID_POINTER_ID;
    }

    private class DragHandleOnTouchListener implements OnTouchListener {
        private final View view;

        public DragHandleOnTouchListener(final View view) {
            this.view = view;
        }

        @Override
        public boolean onTouch(View v, MotionEvent event) {
            if (MotionEvent.ACTION_DOWN == MotionEventCompat.getActionMasked(event) && mDraggable) {
                startDetectingDrag(view);
            }
            return false;
        }
    }

    private BitmapDrawable getDragDrawable(View view) {
        int top = view.getTop();
        int left = view.getLeft();

        Bitmap bitmap = getBitmapFromView(view);

        BitmapDrawable drawable = new BitmapDrawable(getResources(), bitmap);

        drawable.setBounds(new Rect(left, top, left + view.getWidth(), top + view.getHeight()));

        return drawable;
    }

    /**
     * @return a bitmap showing a screenshot of the mView passed in.
     */
    private static Bitmap getBitmapFromView(View view) {
        Bitmap bitmap = Bitmap.createBitmap(view.getWidth(), view.getHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        view.draw(canvas);
        return bitmap;
    }

    // ------------------------------扩展---------------------------------------

    private boolean mDraggable = true;
    private boolean mOverScrollable = true;
    private boolean mIsLongClickDraggable = true;
    private ILongClickToDragListener mClickToDragListener;
    private boolean mIsEnterLongClick = false;

    public void setClickToDragListener(ILongClickToDragListener clickToDragListener) {
        mClickToDragListener = clickToDragListener;
    }

    public void setDraggable(boolean draggable) {
        if (mDraggable != draggable) {
            mDraggable = draggable;
        }
    }

    public void setOverScrollable(boolean overScrollable) {
        if (mOverScrollable != overScrollable) {
            mOverScrollable = overScrollable;
        }
    }

    /**
     * @param longClickDrag if set ,it will ignore
     *                      {@link DragLinearLayout#mDraggable}
     */
    public void setLongClickDrag(boolean longClickDrag) {
        if (mIsLongClickDraggable != longClickDrag) {
            mIsLongClickDraggable = longClickDrag;
        }
    }

    private LongClickDragListener mLongClickDragListener = new LongClickDragListener();

    /**
     * @author chensuilun 长按进入编辑
     */
    public class LongClickDragListener implements OnLongClickListener {

        @Override
        public boolean onLongClick(View v) {
            if (!mIsLongClickDraggable) {
                return false;
            }
            mIsEnterLongClick = true;
            if (mClickToDragListener != null) {
                mClickToDragListener.onLongClickToDrag(v);
            }
            startDetectingDrag(v);
            return true;
        }
    }

    /**
     * @author chensuilun
     */
    public interface ILongClickToDragListener {

        void onLongClickToDrag(View dragableView);
    }

}