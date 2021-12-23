package com.foobnix.pdf;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.fragment.app.Fragment;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import androidx.viewpager.widget.ViewPager;

import com.foobnix.android.utils.Dips;
import com.foobnix.android.utils.LOG;
import com.foobnix.android.utils.TxtUtils;
import com.foobnix.model.AppSP;
import com.foobnix.model.AppState;
import com.foobnix.pdf.info.R;
import com.foobnix.pdf.info.TintUtil;
import com.foobnix.ui2.MainTabs2;
import com.foobnix.ui2.adapter.TabsAdapter2;

/**
 * To be used with ViewPager to provide a tab indicator component which give
 * constant feedback as to the user's scroll progress.
 * <p>
 * To use the component, simply add it to your view hierarchy. Then in your
 * {@link android.app.Activity} or {@link Fragment} call
 * {@link #setViewPager(ViewPager)} providing it the ViewPager this layout is
 * being used for.
 * <p>
 * The colors can be customized in two ways. The first and simplest is to
 * provide an array of colors via {@link #setSelectedIndicatorColors(int...)}
 * and {@link #setDividerColors(int...)}. The alternative is via the
 * {@link TabColorizer} interface which provides you complete control over which
 * color is used for any individual position.
 * <p>
 * The views used as tabs can be customized by calling
 * {@link #setCustomTabView(int, int)}, providing the layout ID of your custom
 * layout.
 */
public class SlidingTabLayout extends HorizontalScrollView {

    SwipeRefreshLayout swipeRefreshLayout;

    public void addSwipeRefreshLayout(SwipeRefreshLayout swipeRefreshLayout) {
        this.swipeRefreshLayout = swipeRefreshLayout;
    }

    /**
     * Allows complete control over the colors drawn in the tab layout. Set with
     * {@link #setCustomTabColorizer(TabColorizer)}.
     */
    public interface TabColorizer {

        /**
         * @return return the color of the indicator used when {@code position}
         * is selected.
         */
        int getIndicatorColor(int position);

        /**
         * @return return the color of the divider drawn to the right of
         * {@code position}.
         */
        int getDividerColor(int position);

    }

    private static final int TITLE_OFFSET_DIPS = 24;

    private static final int TAB_VIEW_TEXT_SIZE_SP = 12;

    private static int POS_HORIZONTAL = 0;
    private static int POS_VERTICAL = 1;

    private static int myPOS = POS_VERTICAL;

    private static int TAB_VIEW_PADDING_DIPS = myPOS == POS_HORIZONTAL ? 16 : 10;

    private int mTitleOffset;

    private int mTabViewLayoutId;
    private int mTabViewTextViewId;

    private ViewPager mViewPager;
    private ViewPager.OnPageChangeListener mViewPagerPageChangeListener;

    private final SlidingTabStrip mTabStrip;

    public SlidingTabLayout(Context context) {
        this(context, null);
    }

    public SlidingTabLayout(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public SlidingTabLayout(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);

        // Disable the Scroll Bar
        setHorizontalScrollBarEnabled(false);
        // Make sure that the Tab Strips fills this View
        setFillViewport(true);

        mTitleOffset = (int) (TITLE_OFFSET_DIPS * getResources().getDisplayMetrics().density);

        mTabStrip = new SlidingTabStrip(context);
        mTabStrip.setDividerColors(Color.TRANSPARENT);

    }

    public void init() {
        myPOS = AppState.get().tapPositionTop ? POS_HORIZONTAL : POS_VERTICAL;
        TAB_VIEW_PADDING_DIPS = myPOS == POS_HORIZONTAL ? 16 : 10;
        addView(getmTabStrip(), LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);

    }


    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        LOG.d("onTouchEvent-ev", ev);
        if (swipeRefreshLayout != null && AppSP.get().isEnableSync) {
            final int action = ev.getAction();

            switch (action & MotionEvent.ACTION_MASK) {
                case MotionEvent.ACTION_DOWN:
                case MotionEvent.ACTION_MOVE: {
                    swipeRefreshLayout.setEnabled(false);
                    break;
                }
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL: {
                    if (MainTabs2.isPullToRefreshEnable(getContext(), swipeRefreshLayout)) {
                        swipeRefreshLayout.setEnabled(true);
                    }

                    break;
                }
            }
        }
        return super.onTouchEvent(ev);
    }

    /**
     * Set the custom {@link TabColorizer} to be used.
     * <p>
     * If you only require simple custmisation then you can use
     * {@link #setSelectedIndicatorColors(int...)} and
     * {@link #setDividerColors(int...)} to achieve similar effects.
     */
    public void setCustomTabColorizer(TabColorizer tabColorizer) {
        getmTabStrip().setCustomTabColorizer(tabColorizer);
    }

    /**
     * Sets the colors to be used for indicating the selected tab. These colors
     * are treated as a circular array. Providing one color will mean that all
     * tabs are indicated with the same color.
     */
    public void setSelectedIndicatorColors(int... colors) {
        getmTabStrip().setSelectedIndicatorColors(colors);
    }

    /**
     * Sets the colors to be used for tab dividers. These colors are treated as
     * a circular array. Providing one color will mean that all tabs are
     * indicated with the same color.
     */
    public void setDividerColors(int... colors) {
        getmTabStrip().setDividerColors(colors);
    }

    /**
     * Set the {@link ViewPager.OnPageChangeListener}. When using
     * {@link SlidingTabLayout} you are required to set any
     * {@link ViewPager.OnPageChangeListener} through this method. This is so
     * that the layout can update it's scroll position correctly.
     *
     * @see ViewPager#setOnPageChangeListener(ViewPager.OnPageChangeListener)
     */
    public void setOnPageChangeListener(ViewPager.OnPageChangeListener listener) {
        mViewPagerPageChangeListener = listener;
    }

    /**
     * Set the custom layout to be inflated for the tab views.
     *
     * @param layoutResId Layout id to be inflated
     * @param textViewId  id of the {@link TextView} in the inflated view
     */
    public void setCustomTabView(int layoutResId, int textViewId) {
        mTabViewLayoutId = layoutResId;
        mTabViewTextViewId = textViewId;
    }

    /**
     * Sets the associated view pager. Note that the assumption here is that the
     * pager content (number of tabs and tab titles) does not change after this
     * call has been made.
     */
    public void setViewPager(ViewPager viewPager) {
        getmTabStrip().removeAllViews();

        mViewPager = viewPager;
        if (viewPager != null) {
            viewPager.setOnPageChangeListener(new InternalViewPagerListener());
            populateTabStrip();
        }
    }

    /**
     * Create a default view to be used for tabs. This is called if a custom tab
     * view is not set via {@link #setCustomTabView(int, int)}.
     */
    protected TextView createDefaultTabView(Context context) {
        TextView textView = new TextView(context);
        textView.setGravity(Gravity.CENTER);
        textView.setTextSize(TypedValue.COMPLEX_UNIT_SP, TAB_VIEW_TEXT_SIZE_SP);
        // textView.setTypeface(Typeface.DEFAULT_BOLD);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            TypedValue outValue = new TypedValue();
            getContext().getTheme().resolveAttribute(android.R.attr.selectableItemBackground, outValue, true);
            textView.setBackgroundResource(outValue.resourceId);
        }


        if (myPOS == POS_HORIZONTAL) {
            textView.setAllCaps(true);
        } else {
            textView.setSingleLine();
            textView.setEllipsize(TextUtils.TruncateAt.END);
        }

        int padding = (int) (TAB_VIEW_PADDING_DIPS * getResources().getDisplayMetrics().density);
        if (AppState.get().tabWithNames) {
            textView.setPadding(padding, padding, padding, padding);
        } else {
            if (myPOS == POS_HORIZONTAL) {
                textView.setPadding((int) (padding * 1.6), padding, padding, padding);
            } else {
                textView.setPadding(padding, (int) (padding * 1.5), padding, 0);
            }
        }

        return textView;
    }

    private void populateTabStrip() {
        final TabsAdapter2 adapter = (TabsAdapter2) mViewPager.getAdapter();
        if (adapter == null) {
            return;
        }

        final View.OnClickListener tabClickListener = new TabClickListener();

        for (int i = 0; i < adapter.getCount(); i++) {
            View tabView = null;
            TextView tabTitleView = null;

            if (mTabViewLayoutId != 0) {
                // If there is a custom tab view layout id set, try and inflate
                // it
                tabView = LayoutInflater.from(getContext()).inflate(mTabViewLayoutId, getmTabStrip(), false);
                tabTitleView = (TextView) tabView.findViewById(mTabViewTextViewId);
            }

            if (tabView == null) {
                tabView = createDefaultTabView(getContext());
                if (AppState.get().appTheme == AppState.THEME_INK) {
                    ((TextView) tabView).setTextSize(16);
                }
            }

            if (tabTitleView == null && TextView.class.isInstance(tabView)) {
                tabTitleView = (TextView) tabView;
            }

            if (tabTitleView != null) {
                CharSequence pageTitle = adapter.getPageTitle(i);
                if (AppState.get().tabWithNames) {
                    tabTitleView.setText(pageTitle);
                } else {
                    tabTitleView.setText("");
                }
                tabTitleView.setContentDescription(pageTitle + " " + getContext().getString(R.string.tab));
                // TintUtil.addTextView(tabTitleView);

                Drawable drawable = getContext().getResources().getDrawable(adapter.getIconResId(i));

                if (myPOS == POS_VERTICAL) {
                    tabTitleView.setCompoundDrawablesWithIntrinsicBounds(null, drawable, null, null);
                } else {
                    tabTitleView.setCompoundDrawablesWithIntrinsicBounds(drawable, null, null, null);
                }


                tabTitleView.setCompoundDrawablePadding(Dips.dpToPx(5));


                if (AppState.get().appTheme == AppState.THEME_INK) {
                    // TintUtil.setDrawableTint(drawable, Color.BLACK);
                    tabTitleView.setTextColor(TintUtil.color);
                } else {
                    TintUtil.setDrawableTint(drawable, Color.WHITE);
                    tabTitleView.setTextColor(Color.WHITE);
                }

                tabView.setOnClickListener(tabClickListener);


                getmTabStrip().addView(tabView, new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT, 1));

            }
        }
        updateIcons(0);

    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();

        if (mViewPager != null) {
            scrollToTab(mViewPager.getCurrentItem(), 0);
        }
    }

    private void scrollToTab(int tabIndex, int positionOffset) {
        final int tabStripChildCount = getmTabStrip().getChildCount();
        if (tabStripChildCount == 0 || tabIndex < 0 || tabIndex >= tabStripChildCount) {
            return;
        }

        TextView selectedChild = (TextView) getmTabStrip().getChildAt(tabIndex);

        if (selectedChild != null) {
            int targetScrollX = selectedChild.getLeft() + positionOffset;

            if (tabIndex > 0 || positionOffset > 0) {
                // If we're not at the first child and are mid-scroll, make sure
                // we obey the offset
                targetScrollX -= mTitleOffset;

            }

            scrollTo(targetScrollX, 0);
        }

    }

    public SlidingTabStrip getmTabStrip() {
        return mTabStrip;
    }

    private class InternalViewPagerListener implements ViewPager.OnPageChangeListener {
        private int mScrollState;

        @Override
        public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
            int tabStripChildCount = getmTabStrip().getChildCount();
            if ((tabStripChildCount == 0) || (position < 0) || (position >= tabStripChildCount)) {
                return;
            }

            getmTabStrip().onViewPagerPageChanged(position, positionOffset);
            //LOG.d("positionOffset", positionOffset);
            if (positionOffset > 0.6) {
                updateIcons(position + 1);
            } else {
                updateIcons(position);
            }

            View selectedTitle = getmTabStrip().getChildAt(position);
            int extraOffset = (selectedTitle != null) ? (int) (positionOffset * selectedTitle.getWidth()) : 0;
            scrollToTab(position, extraOffset);

            if (mViewPagerPageChangeListener != null) {
                mViewPagerPageChangeListener.onPageScrolled(position, positionOffset, positionOffsetPixels);
            }
        }

        @Override
        public void onPageScrollStateChanged(int state) {
            mScrollState = state;

            if (mViewPagerPageChangeListener != null) {
                mViewPagerPageChangeListener.onPageScrollStateChanged(state);
            }
        }

        @Override
        public void onPageSelected(int position) {
            if (mScrollState == ViewPager.SCROLL_STATE_IDLE) {

                getmTabStrip().onViewPagerPageChanged(position, 0f);
                scrollToTab(position, 0);
            }

            if (mViewPagerPageChangeListener != null) {
                mViewPagerPageChangeListener.onPageSelected(position);
            }
        }

    }

    public void updateIcons(int position) {
        for (int i = 0; i < getmTabStrip().getChildCount(); i++) {
            TextView childAt = (TextView) getmTabStrip().getChildAt(i);
            int myColor = i == position ? Color.WHITE : TintUtil.colorSecondTab;


            Drawable drawable;
            if (myPOS == POS_VERTICAL) {
                drawable = childAt.getCompoundDrawables()[1];
            } else {
                drawable = childAt.getCompoundDrawables()[0];
            }

            if (AppState.get().appTheme == AppState.THEME_INK) {
                TintUtil.setDrawableTint(drawable, TintUtil.color);
                childAt.setTextColor(TintUtil.color);
            } else {
                childAt.setTextColor(myColor);
                TintUtil.setDrawableTint(drawable, myColor);
            }
        }
    }

    private class TabClickListener implements View.OnClickListener {
        @Override
        public void onClick(View v) {
            for (int i = 0; i < getmTabStrip().getChildCount(); i++) {
                if (v == getmTabStrip().getChildAt(i)) {
                    mViewPager.setCurrentItem(i, AppState.get().appTheme != AppState.THEME_INK);
                    return;
                }
            }
        }
    }

}