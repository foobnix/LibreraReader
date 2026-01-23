package com.foobnix.pdf.info.view.confline;

import android.widget.TextView;

import androidx.annotation.StringRes;

public class ConfAction {
    public String name;
    @StringRes public int nameResId;
    public int actionInt;

    public ConfAction(String name, int action) {
        this.name = name;
        this.actionInt = action;
        this.nameResId = 0;
    }

    public ConfAction(@StringRes int nameResId, int action) {
        this.name = "";
        this.nameResId = nameResId;
        this.actionInt = action;
    }

    public void setTextTo(TextView textView) {
        if (nameResId != 0) {
            textView.setText(nameResId);
        } else {
            textView.setText(name);
        }
    }

    public static ConfAction of(@StringRes int resId, int actionId) {
        return new ConfAction(resId, actionId);
    }

    public static ConfAction of(String name, int actionId) {
        return new ConfAction(name, actionId);
    }

}



