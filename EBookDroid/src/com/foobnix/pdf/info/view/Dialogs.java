package com.foobnix.pdf.info.view;

import com.foobnix.android.utils.IntegerResponse;
import com.foobnix.pdf.info.R;
import com.foobnix.pdf.info.wrapper.AppState;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;

public class Dialogs {

    public static void showContrastDialog(final Context c, final String message, final Runnable action) {
        final AlertDialog.Builder builder = new AlertDialog.Builder(c);
        builder.setMessage(message);
        builder.setCancelable(true);
        
        
        final CustomSeek seek = new CustomSeek(c);
        seek.initWith(message, "");
        seek.init(0, 150, AppState.get().contrast);
        seek.setOnSeekChanged(new IntegerResponse() {

            @Override
            public boolean onResultRecive(int result) {
                AppState.get().contrast = result;
                return false;
            }
        });
        
        builder.setView(seek);

        builder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {

            @Override
            public void onClick(final DialogInterface dialog, final int id) {
                if (action != null) {
                    action.run();
                }

            }
        });
        builder.show();
    }

}
