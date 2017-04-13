package com.foobnix.pdf.info.widget;

import com.foobnix.android.utils.Dips;
import com.foobnix.android.utils.ResultResponse;
import com.foobnix.android.utils.ResultResponse2;
import com.foobnix.pdf.info.R;
import com.foobnix.ui2.fragment.BrowseFragment2;

import android.app.Dialog;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.FrameLayout.LayoutParams;

public class ChooserDialogFragment extends DialogFragment {

    public ChooserDialogFragment() {
    }

    public static ChooserDialogFragment chooseFolder(FragmentActivity a, String initPath) {
        ChooserDialogFragment ch = new ChooserDialogFragment();
        Bundle bundle = new Bundle();
        bundle.putInt(BrowseFragment2.EXTRA_TYPE, BrowseFragment2.TYPE_SELECT_FOLDER);
        bundle.putString(BrowseFragment2.EXTRA_INIT_PATH, initPath);
        ch.setArguments(bundle);
        ch.show(a.getSupportFragmentManager(), "da");
        return ch;
    }

    public static ChooserDialogFragment chooseFile(FragmentActivity a, String text) {
        ChooserDialogFragment ch = new ChooserDialogFragment();
        Bundle bundle = new Bundle();
        bundle.putInt(BrowseFragment2.EXTRA_TYPE, BrowseFragment2.TYPE_SELECT_FILE);
        bundle.putString(BrowseFragment2.EXTRA_TEXT, text);
        ch.setArguments(bundle);
        ch.show(a.getSupportFragmentManager(), "da");
        return ch;
    }

    public static ChooserDialogFragment createFile(FragmentActivity a, String text) {
        ChooserDialogFragment ch = new ChooserDialogFragment();
        Bundle bundle = new Bundle();
        bundle.putInt(BrowseFragment2.EXTRA_TYPE, BrowseFragment2.TYPE_CREATE_FILE);
        bundle.putString(BrowseFragment2.EXTRA_TEXT, text);
        ch.setArguments(bundle);
        ch.show(a.getSupportFragmentManager(), "da");
        return ch;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, android.view.ViewGroup container, android.os.Bundle savedInstanceState) {
        setRetainInstance(true);
        FrameLayout frame = new FrameLayout(getContext());
        frame.setLayoutParams(new FrameLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
        frame.setMinimumHeight(Dips.dpToPx(1000));
        frame.setMinimumWidth(Dips.dpToPx(1000));
        frame.setId(0x13);

        final BrowseFragment2 fr = BrowseFragment2.newInstance(getArguments());

        getChildFragmentManager().beginTransaction().replace(0x13, fr, "fr").commit();

        fr.setOnCloseAction(new ResultResponse<String>() {

            @Override
            public boolean onResultRecive(String result) {
                getDialog().dismiss();
                return false;
            }
        });
        fr.setOnPositiveAction(new ResultResponse<String>() {
            @Override
            public boolean onResultRecive(String result) {
                onSelectListener.onResultRecive(result, getDialog());
                return false;
            }
        });

        getDialog().setTitle(R.string.choose_);

        return frame;
    };


    public void setOnSelectListener(ResultResponse2<String, Dialog> onSelectListener) {
        this.onSelectListener = onSelectListener;
    }

    private ResultResponse2<String, Dialog> onSelectListener;
};