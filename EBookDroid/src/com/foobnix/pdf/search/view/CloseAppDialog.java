package com.foobnix.pdf.search.view;

import com.foobnix.pdf.info.R;
import com.foobnix.pdf.info.wrapper.AppState;
import com.foobnix.ui2.MainTabs2;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.view.KeyEvent;

public class CloseAppDialog {

    public static boolean checkLongPress(Context c, KeyEvent event, Runnable action) {
        int keyCode = event.getKeyCode();
        if (keyCode == 0) {
            keyCode = event.getScanCode();
        }
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            show(c, action);
            return true;
        }
        return false;
    }

    public static void show(final Context c, final Runnable action) {
        final Runnable closeApp = new Runnable() {

            @Override
            public void run() {
                action.run();
                if (MainTabs2.isInStack) {
                    Intent startMain = new Intent(c, MainTabs2.class);
                    startMain.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    startMain.putExtra(MainTabs2.EXTRA_EXIT, true);
                    c.startActivity(startMain);
                }

            }
        };
        if (!AppState.get().isShowCloseAppDialog) {
            closeApp.run();
            return;
        }

        AlertDialog.Builder dialog = new AlertDialog.Builder(c);
        dialog.setTitle(R.string.close_application_);

        dialog.setPositiveButton(R.string.no, new OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {

            }
        });

        dialog.setNegativeButton(R.string.yes, new OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {

                closeApp.run();

            }
        });
        dialog.show();

    }

}
