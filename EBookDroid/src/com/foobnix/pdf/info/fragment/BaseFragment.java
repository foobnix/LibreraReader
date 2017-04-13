package com.foobnix.pdf.info.fragment;

import android.support.v4.app.Fragment;

public abstract class BaseFragment extends Fragment {

    public BaseFragment() {
    }

	public abstract void onSelected();

    public abstract boolean hideBottomBanner();
}
