package com.foobnix.android.utils;

import android.app.Activity;
import android.graphics.Typeface;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

public class UI {

    public static LinearLayout verticalLayout(Activity a, View... views) {
        LinearLayout l = new LinearLayout(a);
        l.setOrientation(LinearLayout.VERTICAL);
        if(views!=null){
            for(View v: views){
                l.addView(v);
            }
        }
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
    public static TextView text(Activity a, String text, int color) {
        TextView l = new TextView(a);
        l.setText(text);
        l.setTextColor(color);
        return l;
    }
    public static TextView button(Activity a, String text) {
        Button l = new Button(a);
        l.setText(text);
        return l;
    }

    public static TextView uText(Activity a, String text) {
        TextView l = new TextView(a);
        l.setText(text);
        TxtUtils.underlineTextView(l);
        return l;
    }
    public static TextView bText(Activity a, String text) {
        TextView l = new TextView(a);
        l.setText(text);
        l.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        return l;
    }
}
