package com.foobnix.pdf.info.widget;

import android.app.Dialog;
import android.content.DialogInterface;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.FrameLayout.LayoutParams;
import android.widget.TextView;

import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.FragmentActivity;

import com.foobnix.android.utils.Keyboards;
import com.foobnix.android.utils.LOG;
import com.foobnix.android.utils.ResultResponse;
import com.foobnix.android.utils.ResultResponse2;
import com.foobnix.pdf.info.R;
import com.foobnix.pdf.info.TintUtil;
import com.foobnix.ui2.fragment.BrowseFragment2;

public class ChooserDialogFragment extends DialogFragment {

    public ChooserDialogFragment() {

    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // The card draws its own head, so the frame around it is asked for none.
        setStyle(DialogFragment.STYLE_NO_TITLE, 0);
    }

    @Override
    public void onDismiss(DialogInterface dialog) {
        super.onDismiss(dialog);
        Keyboards.hideNavigation(getActivity());
        Keyboards.close(getActivity());
    }

    public static ChooserDialogFragment chooseFolder(FragmentActivity a, String initPath) {
        ChooserDialogFragment ch = new ChooserDialogFragment();
        Bundle bundle = new Bundle();
        bundle.putInt(BrowseFragment2.EXTRA_TYPE, BrowseFragment2.TYPE_SELECT_FOLDER);
        bundle.putString(BrowseFragment2.EXTRA_INIT_PATH, initPath);
        ch.setArguments(bundle);
        try {
            ch.show(a.getSupportFragmentManager(), "da");
        }catch (Exception e){
            LOG.e(e);
        }
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

    public static ChooserDialogFragment chooseFileorFolder(FragmentActivity a, String text) {
        ChooserDialogFragment ch = new ChooserDialogFragment();
        Bundle bundle = new Bundle();
        bundle.putInt(BrowseFragment2.EXTRA_TYPE, BrowseFragment2.TYPE_SELECT_FILE_OR_FOLDER);
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
        View frame = inflater.inflate(R.layout.dialog_chooser, container, false);

        // The head is cut from the same colour, and to the same weight, as a draging popup's.
        TintUtil.setTintBgSimple(frame.findViewById(R.id.topLayout), 230);

        ((TextView) frame.findViewById(R.id.dialogTitle)).setText(R.string.choose_);

        frame.findViewById(R.id.closePopup).setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                dismissAllowingStateLoss();
            }
        });

        final BrowseFragment2 fr = BrowseFragment2.newInstance(getArguments());

        getChildFragmentManager().beginTransaction().replace(R.id.metaGenreID, fr, "fr").commit();

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
                if (onSelectListener != null && result != null && getDialog() != null) {
                    onSelectListener.onResultRecive(result, getDialog());
                }
                return false;
            }
        });

        return frame;
    };

    @Override
    public void onResume() {
        final Window window = getDialog().getWindow();
        ViewGroup.LayoutParams params = window.getAttributes();
        params.width = LayoutParams.MATCH_PARENT;
        params.height = LayoutParams.MATCH_PARENT;
        window.setAttributes((android.view.WindowManager.LayoutParams) params);
        // Nothing square is left behind the card, or its round corners would be filled in.
        window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));

        super.onResume();
    }

    public void setOnSelectListener(ResultResponse2<String, Dialog> onSelectListener) {
        this.onSelectListener = onSelectListener;
    }

    private ResultResponse2<String, Dialog> onSelectListener;
};