package com.foobnix.android.utils;

import android.app.Activity;
import android.widget.LinearLayout;
import android.widget.TextView;

public class UI {

    public static LinearLayout verticalLayout(Activity a) {
        LinearLayout l = new LinearLayout(a);
        l.setOrientation(LinearLayout.VERTICAL);
        return l;
    }

    public static LinearLayout horizontalLayout(Activity a) {
        LinearLayout l = new LinearLayout(a);
        l.setOrientation(LinearLayout.VERTICAL);
        return l;
    }

    public static TextView text(Activity a, String text) {
        TextView l = new TextView(a);
        l.setText(text);
        return l;
    }

    public static TextView uText(Activity a, String text) {
        TextView l = new TextView(a);
        l.setText(text);
        TxtUtils.underlineTextView(l);
        return l;
    }
}
