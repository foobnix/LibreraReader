package com.foobnix.ui2.adapter;

import java.util.List;

import com.foobnix.ui2.fragment.UIFragment;

import android.os.Parcelable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentStatePagerAdapter;

public class TabsAdapter2 extends FragmentStatePagerAdapter {

    private List<UIFragment> tabFragments;
    FragmentActivity a;

    public TabsAdapter2(FragmentActivity a, List<UIFragment> tabFragments) {
        super(a.getSupportFragmentManager());
        this.tabFragments = tabFragments;
        this.a = a;

    }

    @Override
    public Fragment getItem(int index) {
        return tabFragments.get(index);
    }

    @Override
    public int getCount() {
        return tabFragments.size();
    }

    @Override
    public CharSequence getPageTitle(int position) {
        CharSequence text = a.getText((Integer) tabFragments.get(position).getNameAndIconRes().first);
        if (position == 0) {
            // text = "Lirbi " + text;
        }
        return text;
    }

    public int getIconResId(final int position) {
        int second = (Integer) tabFragments.get(position).getNameAndIconRes().second;
        return second;
    }

    @Override
    public Parcelable saveState() {
        return null;
    }

    @Override
    public void restoreState(Parcelable arg0, ClassLoader arg1) {
    }

}
