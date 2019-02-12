package com.foobnix.pdf.info.view;

import com.foobnix.pdf.info.R;
import com.foobnix.pdf.info.wrapper.AppState;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnKeyListener;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

public class KeyCodeDialog {

    public KeyCodeDialog(final Activity a, final Runnable onClose) {
        AlertDialog.Builder builder = new AlertDialog.Builder(a);
        builder.setTitle(R.string.press_any_key);
        View layout = LayoutInflater.from(a).inflate(R.layout.key_dialog, null);
        final EditText keyNext = (EditText) layout.findViewById(R.id.keyNext);
        final EditText keyPrev = (EditText) layout.findViewById(R.id.keyPrev);
        final TextView textKey = (TextView) layout.findViewById(R.id.textKey);
        final Button onDef = (Button) layout.findViewById(R.id.onDefaults);

        init(keyNext, keyPrev);

        builder.setView(layout);

        builder.setPositiveButton(R.string.apply, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int id) {
                AppState.get().nextKeys = AppState.stringToKyes(keyNext.getText().toString());
                AppState.get().prevKeys = AppState.stringToKyes(keyPrev.getText().toString());
                AppState.get().save(a);
                if (onClose != null) {
                    onClose.run();
                }
            }
        });

        builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int id) {
                dialog.dismiss();
            }
        });

        builder.setOnKeyListener(new OnKeyListener() {

            @Override
            public boolean onKey(DialogInterface dialog, int keyCode, KeyEvent event) {
                if (keyCode == KeyEvent.KEYCODE_BACK || keyCode == KeyEvent.KEYCODE_DEL || keyCode == KeyEvent.KEYCODE_BACKSLASH || keyCode == KeyEvent.KEYCODE_ENTER) {
                    return false;
                }
                if (keyCode != KeyEvent.KEYCODE_UNKNOWN) {
                    textKey.setText("" + keyCode);
                } else if (event != null) {
                    textKey.setText("" + event.getScanCode());
                } else {
                    textKey.setText("--");
                }
                return false;
            }
        });

        onDef.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                AppState.get().nextKeys = AppState.NEXT_KEYS;
                AppState.get().prevKeys = AppState.PREV_KEYS;
                init(keyNext, keyPrev);

            }

        });

        builder.show();
    }

    private void init(final EditText keyNext, final EditText keyPrev) {
        keyNext.setText(AppState.keyToString(AppState.get().nextKeys));
        keyPrev.setText(AppState.keyToString(AppState.get().prevKeys));

        keyNext.setSelection(keyNext.getText().length());
        keyPrev.setSelection(keyPrev.getText().length());
    }
}
