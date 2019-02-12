package com.foobnix.pdf.info.view;

import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnKeyListener;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;

public class EditTextHelper {

    public static void enableKeyboardSearch(final EditText searchEdit, final Runnable action) {
        searchEdit.setImeOptions(EditorInfo.IME_ACTION_DONE);
        searchEdit.setOnEditorActionListener(new OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    action.run();
                }
                return true;
            }
        });

        searchEdit.setOnKeyListener(new OnKeyListener() {

            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                if ((event.getAction() == KeyEvent.ACTION_DOWN && keyCode == KeyEvent.KEYCODE_ENTER)) {
                    action.run();
                    return true;

                }
                return false;
            }
        });

    }
}
