package com.foobnix.pdf.info.view.confline;

import android.content.Context;
import android.widget.TextView;

import androidx.annotation.StringRes;

import java.util.function.BooleanSupplier;

public class ConfAction {
    public String name;
    @StringRes public int nameResId;
    public int actionInt;
    /** A mark shown before the name, in the list and on the button once picked - ① for the vertical mode, as the open-with menus show it. */
    public String mark;
    /** Whether the choice can be made now - a reader that is not installed cannot be - and what to do instead when not. */
    public BooleanSupplier available;
    public Runnable otherwise;

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
        textView.setText(menuText(textView.getContext()));
    }

    /**
     * Picked while {@code available} says no, the setting is left as it was and {@code otherwise}
     * runs instead - offering to install what is missing, say.
     */
    public ConfAction requires(BooleanSupplier available, Runnable otherwise) {
        this.available = available;
        this.otherwise = otherwise;
        return this;
    }

    public boolean isAvailable() {
        return available == null || available.getAsBoolean();
    }

    public ConfAction mark(String mark) {
        this.mark = mark;
        return this;
    }

    /** The name with its mark before it, when it has one. */
    public String menuText(Context c) {
        String text = nameResId != 0 ? c.getString(nameResId) : name;
        return mark != null ? mark + " " + text : text;
    }

    public static ConfAction of(@StringRes int resId, int actionId) {
        return new ConfAction(resId, actionId);
    }

    public static ConfAction of(String name, int actionId) {
        return new ConfAction(name, actionId);
    }

}



