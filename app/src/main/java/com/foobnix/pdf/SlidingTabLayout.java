package com.foobnix.pdf;

import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.RippleDrawable;
import android.graphics.drawable.ScaleDrawable;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.fragment.app.Fragment;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import androidx.viewpager.widget.ViewPager;

import com.foobnix.android.utils.Dips;
import com.foobnix.android.utils.IntegerResponse;
import com.foobnix.android.utils.LOG;
import com.foobnix.model.AppSP;
import com.foobnix.model.AppState;
import com.foobnix.pdf.info.R;
import com.foobnix.pdf.info.TintUtil;
import com.foobnix.sys.DoubleClickListener;
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

    private static final int TITLE_OFFSET_DIPS = 24;
    private static final int TAB_VIEW_TEXT_SIZE_SP = 12;
    private static int POS_HORIZONTAL = 0;
    private static int POS_VERTICAL = 1;
    private static int myPOS = POS_VERTICAL;
    private static int TAB_VIEW_PADDING_DIPS = myPOS == POS_HORIZONTAL ? 10 : 7;

    /**
     * The space a page keeps clear at its foot, so the floating bar never comes to rest on
     * the last book: the pill's own height, the gap it is lifted by, and a little air. A
     * bar of icons alone is the shorter of the two and asks for less.
     */
    private static final int FLOATING_SPACE_DIPS = 76;
    private static final int FLOATING_SPACE_ALONE_DIPS = 50;
    /**
     * A full stadium rounds to half the bar's height, the way LibreraX draws it; the pill
     * stops this far short of that, so the ends are round without closing into circles.
     */
    private static final int FLOATING_RADIUS_INSET_DIPS = 4;
    /**
     * How much of the tint colour the floating chrome keeps. Two things bound it: the page
     * has to be seen moving under the bars at all, and the white the labels are set in has
     * to hold against the brightest thing that can pass beneath them - a white cover, which
     * lightens the bar by whatever is let through.
     */
    public static final int FLOATING_ALPHA = 230;
    /** The patch the chosen tab keeps behind its icon and name. */
    private static final int PATCH_ALPHA = 46;
    /** The ripple a tap leaves on a tab, over that same patch. */
    private static final int RIPPLE_ALPHA = 60;
    /** The margin the pill keeps between its own edge and the tabs inside it. */
    private static final int FLOATING_INSET_DIPS = 5;
    private static final int FLOATING_INSET_ALONE_DIPS = 3;
    private static final int FLOATING_ELEVATION_DIPS = 6;
    /** The icon in a tab, and the smaller one it draws where it stands without a name. */
    private static final int TAB_ICON_DIPS = 28;
    private static final int TAB_ICON_ALONE_DIPS = 24;

    private final SlidingTabStrip mTabStrip;
    /** The pill itself, kept so its corners can follow the height it ends up with. */
    private GradientDrawable floatingPill;
    SwipeRefreshLayout swipeRefreshLayout;
    IntegerResponse onDoubleClickAction;
    private int mTitleOffset;

    private int mTabViewLayoutId;
    private int mTabViewTextViewId;

    private ViewPager mViewPager;
    private ViewPager.OnPageChangeListener mViewPagerPageChangeListener;

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

    /**
     * True while the tabs sit at the foot of the library, where they float over the pages
     * rather than standing on a strip of their own.
     */
    public static boolean isFloating() {
        return !AppState.get().tapPositionTop;
    }

    /** The floating bar carrying no names: each tab is an icon and nothing else. */
    private static boolean isIconsAlone() {
        return isFloating() && !AppState.get().tabWithNames;
    }

    /** What a page has to keep clear at its foot for the bar floating over it. */
    public static int floatingSpace() {
        if (!isFloating()) {
            return 0;
        }
        return Dips.dpToPx(isIconsAlone() ? FLOATING_SPACE_ALONE_DIPS : FLOATING_SPACE_DIPS);
    }

    /**
     * At the top the bar is a plain strip in the tint colour. At the foot it floats over
     * the pages instead: a rounded pill, translucent enough to read the page through, and
     * lifted off it by a shadow. On e-ink the same shape stays flat and opaque - a shadow
     * only greys the screen, and a page showing through would smear behind the names.
     */
    /** The tint the floating chrome is painted in: the colour, with the page kept showing. */
    public static int floatingTint(int color) {
        return setColorAlpha(color, FLOATING_ALPHA);
    }

    public void setTabsBackground(int color) {
        if (!isFloating()) {
            // The strip the tabs stand on is already painted in the chrome's colour, and the
            // page shows through it. Painting the tabs again over that would lay the same
            // tint down twice and come out darker than every other panel.
            setBackgroundColor(Color.TRANSPARENT);
            return;
        }
        boolean ink = AppState.get().appTheme == AppState.THEME_INK;
        floatingPill = new GradientDrawable();
        floatingPill.setColor(ink ? pageColor() : setColorAlpha(color, FLOATING_ALPHA));
        if (ink) {
            floatingPill.setStroke(Dips.DP_1, TintUtil.color);
        }
        setBackground(floatingPill);
        setClipToOutline(true);
        setElevation(ink ? 0 : Dips.dpToPx(FLOATING_ELEVATION_DIPS));

        int inset = Dips.dpToPx(isIconsAlone() ? FLOATING_INSET_ALONE_DIPS : FLOATING_INSET_DIPS);
        setPadding(inset, inset, inset, inset);

        applyFloatingRadius(getHeight());
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        applyFloatingRadius(h);
    }

    /** The corners are cut from the height, so the pill keeps its shape at any text size. */
    private void applyFloatingRadius(int height) {
        if (floatingPill != null && height > 0) {
            floatingPill.setCornerRadius(Math.max(0, height / 2f - Dips.dpToPx(FLOATING_RADIUS_INSET_DIPS)));
        }
    }

    /** What the page behind the bar is painted with, for the themes that must hide it. */
    private int pageColor() {
        TypedValue outValue = new TypedValue();
        getContext().getTheme().resolveAttribute(android.R.attr.colorBackground, outValue, true);
        return outValue.data;
    }

    private static int setColorAlpha(int color, int alpha) {
        return Color.argb(alpha, Color.red(color), Color.green(color), Color.blue(color));
    }

    /**
     * A tab-shaped patch. The radius is past any height a tab can reach, and a round rect
     * is drawn no rounder than half its own side, so the ends come out fully rounded.
     */
    private static GradientDrawable roundedPatch(int color) {
        GradientDrawable patch = new GradientDrawable();
        patch.setCornerRadius(Dips.DP_50);
        patch.setColor(color);
        return patch;
    }

    /**
     * The chosen tab keeps a rounded patch behind its icon and name, the way LibreraX marks
     * it. The patch is the tab's own background, with the tap ripple laid over it.
     */
    private void setTabPatch(View tab, boolean isSelected) {
        if (!isFloating()) {
            return;
        }
        try {
            Drawable patch = ((RippleDrawable) tab.getBackground()).getDrawable(0);
            boolean ink = AppState.get().appTheme == AppState.THEME_INK;
            int color = setColorAlpha(ink ? TintUtil.color : Color.WHITE, PATCH_ALPHA);
            ((GradientDrawable) patch).setColor(isSelected ? color : Color.TRANSPARENT);
        } catch (Exception e) {
            LOG.e(e);
        }
    }

    public void addSwipeRefreshLayout(SwipeRefreshLayout swipeRefreshLayout) {
        this.swipeRefreshLayout = swipeRefreshLayout;
    }

    public void setOnDoubleClickAction(IntegerResponse onDoubleClickAction) {
        this.onDoubleClickAction = onDoubleClickAction;
    }

    public void init() {
        myPOS = AppState.get().tapPositionTop ? POS_HORIZONTAL : POS_VERTICAL;
        TAB_VIEW_PADDING_DIPS = myPOS == POS_HORIZONTAL ? 10 : 7;
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

        if (isFloating()) {
            // Rounded all the way round, as in LibreraX: only the pill's own corners are
            // held back. The mask keeps the ripple of a tap to the same shape.
            boolean ink = AppState.get().appTheme == AppState.THEME_INK;
            int ripple = setColorAlpha(ink ? TintUtil.color : Color.WHITE, RIPPLE_ALPHA);
            textView.setBackground(new RippleDrawable(ColorStateList.valueOf(ripple),
                                                      roundedPatch(Color.TRANSPARENT),
                                                      roundedPatch(Color.WHITE)));
        } else {
            // At the top the chosen tab is told apart by the weight of its own colour and
            // keeps no patch behind it. A tap still answers, but in the same round the rest
            // of the chrome is drawn in rather than a rectangle cut around the words.
            boolean ink = AppState.get().appTheme == AppState.THEME_INK;
            int ripple = setColorAlpha(ink ? TintUtil.color : Color.WHITE, RIPPLE_ALPHA);
            textView.setBackground(new RippleDrawable(ColorStateList.valueOf(ripple),
                                                      null,
                                                      roundedPatch(Color.WHITE)));
        }

        if (myPOS == POS_HORIZONTAL) {
            textView.setAllCaps(true);
        } else {
            textView.setSingleLine();
            textView.setEllipsize(TextUtils.TruncateAt.END);
        }

        int padding = (int) (TAB_VIEW_PADDING_DIPS * getResources().getDisplayMetrics().density);
        if (isFloating()) {
            // Inside the pill the tab is drawn tight: the name in bold right under the icon,
            // and only as much air above and below as keeps the patch off the pill's edge.
            // The patch wraps the whole tab, so that air has to be even at both ends.
            textView.setTypeface(Typeface.DEFAULT_BOLD);
            int air = isIconsAlone() ? Dips.DP_3 : Dips.DP_4;
            textView.setPadding(padding, air, padding, air);
            if (isIconsAlone()) {
                // There is no name to draw, but an empty line still holds a line's height
                // under the icon: the bar would carry a band of nothing at its foot and the
                // patch would sit low around the icon rather than about it.
                textView.setTextSize(TypedValue.COMPLEX_UNIT_PX, 0);
            }
        } else if (AppState.get().tabWithNames) {
            // The patch wraps the tab, so what is padding here is the room around the mark
            // on the chosen one: enough to clear the words, not enough to swell the strip.
            textView.setTypeface(Typeface.DEFAULT_BOLD);
            textView.setPadding(padding, Dips.DP_6, padding, Dips.DP_6);
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
            final int j = i;
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
                if (AppState.get().appTheme == AppState.THEME_INK && !isIconsAlone()) {
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
                Drawable drawable = null;
                try {
                    drawable = getContext().getResources().getDrawable(adapter.getIconResId(i));
                    //drawable = new ScaleDrawable(drawable.getCurrent(),0,Dips.DP_10,Dips.DP_10);
                    int size = Dips.dpToPx(isIconsAlone() ? TAB_ICON_ALONE_DIPS : TAB_ICON_DIPS);
                    drawable.setBounds(0,0,size,size);

                    if (myPOS == POS_VERTICAL) {
                        tabTitleView.setCompoundDrawables(null, drawable, null, null);
                    } else {
                        tabTitleView.setCompoundDrawables(drawable, null, null, null);
                    }
                } catch (Exception e) {
                    LOG.e(e);
                }


                tabTitleView.setCompoundDrawablePadding(
                        isIconsAlone() ? 0 : isFloating() ? Dips.DP_1 : Dips.dpToPx(5));

                if (AppState.get().appTheme == AppState.THEME_INK) {
                    // TintUtil.setDrawableTint(drawable, Color.BLACK);
                    tabTitleView.setTextColor(TintUtil.color);
                } else {
                    if(drawable!=null) {
                        TintUtil.setDrawableTint(drawable, Color.WHITE);
                    }
                    tabTitleView.setTextColor(Color.WHITE);
                }

                //tabView.setOnClickListener(tabClickListener);
                tabView.setOnClickListener(new DoubleClickListener() {
                    @Override
                    public void onSingleClick(View v) {
                        tabClickListener.onClick(v);
                    }

                    @Override
                    public void onDoubleClick(View v) {
                        if (onDoubleClickAction != null) {
                            onDoubleClickAction.onResultRecive(j);
                        }
                    }
                });


                LinearLayout.LayoutParams
                        params =
                        new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT, 1);
                if (isFloating()) {
                    // Every tab the same width, so the patch behind the chosen one comes out
                    // as a pill across it rather than a circle in the middle of it.
                    params.width = 0;
                    params.leftMargin = params.rightMargin = Dips.DP_2;
                }
                getmTabStrip().addView(tabView, params);

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

            setTabPatch(childAt, i == position);
        }
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