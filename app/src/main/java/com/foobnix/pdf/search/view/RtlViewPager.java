/*
 * Copyright 2016 Konstantin Loginov
 * Copyright 2015 Diego Gómez Olvera
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
package com.foobnix.pdf.search.view;

import java.util.Locale;
import java.util.Map;

import com.foobnix.android.utils.LOG;

import android.content.Context;
import android.database.DataSetObserver;
import android.support.v4.text.TextUtilsCompat;
import android.support.v4.util.ArrayMap;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewCompat;
import android.support.v4.view.ViewPager;
import android.util.AttributeSet;
import android.view.ViewGroup;

/**
 * ViewPager that reverses the items order in RTL locales.
 */
public class RtlViewPager extends ViewPager {

    private final Map<OnPageChangeListener, ReverseOnPageChangeListener> reverseOnPageChangeListeners;

    private DataSetObserver dataSetObserver;

    private boolean suppressOnPageChangeListeners;

    public RtlViewPager(Context context) {
        super(context);
        reverseOnPageChangeListeners = new ArrayMap<OnPageChangeListener, ReverseOnPageChangeListener>(1);
    }

    public RtlViewPager(Context context, AttributeSet attrs) {
        super(context, attrs);
        reverseOnPageChangeListeners = new ArrayMap<OnPageChangeListener, ReverseOnPageChangeListener>(1);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        registerRtlDataSetObserver(super.getAdapter());
    }

    @Override
    protected void onDetachedFromWindow() {
        unregisterRtlDataSetObserver();
        super.onDetachedFromWindow();
    }

    private void registerRtlDataSetObserver(PagerAdapter adapter) {
        if (adapter instanceof ReverseAdapter && dataSetObserver == null) {
            dataSetObserver = new RevalidateIndicesOnContentChange((ReverseAdapter) adapter);
            adapter.registerDataSetObserver(dataSetObserver);
            ((ReverseAdapter) adapter).revalidateIndices();
        }
    }

    private void unregisterRtlDataSetObserver() {
        final PagerAdapter adapter = super.getAdapter();

        if (adapter instanceof ReverseAdapter && dataSetObserver != null) {
            adapter.unregisterDataSetObserver(dataSetObserver);
            dataSetObserver = null;
        }
    }

    @Override
    public void setCurrentItem(int item, boolean smoothScroll) {
        try {
            super.setCurrentItem(convert(item), smoothScroll);
        } catch (Exception e) {
            LOG.e(e);
        }
    }

    @Override
    public void setCurrentItem(int item) {
        if (item <= 0) {
            item = 0;
        }
        super.setCurrentItem(convert(item));
    }

    @Override
    public int getCurrentItem() {
        return convert(super.getCurrentItem());
    }

    private int convert(int position) {
        if (position >= 0 && isRtl()) {
            return getAdapter() == null ? 0 : getAdapter().getCount() - position - 1;
        } else {
            return position;
        }
    }

    @Override
    public PagerAdapter getAdapter() {
        final PagerAdapter adapter = super.getAdapter();
        return adapter instanceof ReverseAdapter ? ((ReverseAdapter) adapter).getInnerAdapter() : adapter;
    }

    @Override
    public void fakeDragBy(float xOffset) {
        super.fakeDragBy(isRtl() ? xOffset : -xOffset);
    }

    @Override
    public void setAdapter(PagerAdapter adapter) {
        unregisterRtlDataSetObserver();

        final boolean rtlReady = adapter != null && isRtl();
        if (rtlReady) {
            adapter = new ReverseAdapter(adapter);
            registerRtlDataSetObserver(adapter);
        }
        super.setAdapter(adapter);
        if (rtlReady) {
            setCurrentItemWithoutNotification(0);
        }
    }

    private void setCurrentItemWithoutNotification(int index) {
        suppressOnPageChangeListeners = true;
        setCurrentItem(index, false);
        suppressOnPageChangeListeners = false;
    }

    protected boolean isRtl() {
        return TextUtilsCompat.getLayoutDirectionFromLocale(Locale.getDefault()) == ViewCompat.LAYOUT_DIRECTION_RTL;
    }

    @Override
    public void addOnPageChangeListener(OnPageChangeListener listener) {
        if (isRtl()) {
            final ReverseOnPageChangeListener reverseListener = new ReverseOnPageChangeListener(listener);
            reverseOnPageChangeListeners.put(listener, reverseListener);
            listener = reverseListener;
        }
        super.addOnPageChangeListener(listener);
    }

    @Override
    public void removeOnPageChangeListener(OnPageChangeListener listener) {
        if (isRtl()) {
            listener = reverseOnPageChangeListeners.remove(listener);
        }
        super.removeOnPageChangeListener(listener);
    }

    private class ReverseAdapter extends PagerAdapterWrapper {

        private int lastCount;

        public ReverseAdapter(PagerAdapter adapter) {
            super(adapter);
            lastCount = adapter.getCount();
        }

        @Override
        public CharSequence getPageTitle(int position) {
            return super.getPageTitle(reverse(position));
        }

        @Override
        public float getPageWidth(int position) {
            return super.getPageWidth(reverse(position));
        }

        @Override
        public int getItemPosition(Object object) {
            final int itemPosition = super.getItemPosition(object);
            return itemPosition < 0 ? itemPosition : reverse(itemPosition);
        }

        @Override
        public Object instantiateItem(ViewGroup container, int position) {
            return super.instantiateItem(container, reverse(position));
        }

        @Override
        public void destroyItem(ViewGroup container, int position, Object object) {
            try {
                super.destroyItem(container, reverse(position), object);
            } catch (Exception e) {
                super.destroyItem(container, position, object);
            }
        }

        @Override
        public void setPrimaryItem(ViewGroup container, int position, Object object) {
            super.setPrimaryItem(container, lastCount - position - 1, object);
        }

        private int reverse(int position) {
            return getCount() - position - 1;
        }

        private void revalidateIndices() {
            final int newCount = getCount();
            if (newCount != lastCount) {
                setCurrentItemWithoutNotification(Math.max(0, lastCount - 1));
                lastCount = newCount;
            }
        }
    }

    private static class RevalidateIndicesOnContentChange extends DataSetObserver {

        private final ReverseAdapter adapter;

        private RevalidateIndicesOnContentChange(ReverseAdapter adapter) {
            this.adapter = adapter;
        }

        @Override
        public void onChanged() {
            super.onChanged();
            adapter.revalidateIndices();
        }
    }

    private class ReverseOnPageChangeListener implements OnPageChangeListener {

        private int pagerPosition = -1;

        private final OnPageChangeListener original;

        private ReverseOnPageChangeListener(OnPageChangeListener original) {
            this.original = original;
        }

        @Override
        public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
            if (!suppressOnPageChangeListeners) {

                if (positionOffset == 0f && positionOffsetPixels == 0) {
                    pagerPosition = reverse(position);
                } else {
                    pagerPosition = reverse(position + 1);
                }

                original.onPageScrolled(pagerPosition, positionOffset > 0 ? 1f - positionOffset : positionOffset, positionOffsetPixels);
            }
        }

        @Override
        public void onPageSelected(int position) {
            if (!suppressOnPageChangeListeners) {
                original.onPageSelected(reverse(position));
            }
        }

        @Override
        public void onPageScrollStateChanged(int state) {
            if (!suppressOnPageChangeListeners) {
                original.onPageScrollStateChanged(state);
            }
        }

        private int reverse(int position) {
            final PagerAdapter adapter = getAdapter();
            return adapter == null ? position : adapter.getCount() - position - 1;
        }
    }
}