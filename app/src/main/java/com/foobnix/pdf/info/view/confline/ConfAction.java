package com.foobnix.pdf.info.view.confline;

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

    public static ConfAction of(@StringRes int resId, int actionId) {
        return new ConfAction(resId, actionId);
    }

}



