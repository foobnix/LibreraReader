package com.foobnix.pdf.search.view;

import java.util.ArrayList;
import java.util.List;

import com.foobnix.android.utils.Apps;
import com.foobnix.pdf.info.R;
import com.foobnix.pdf.info.view.MyPopupMenu;
import com.foobnix.pdf.info.wrapper.AppState;
import com.foobnix.pdf.info.wrapper.DocumentController;
import com.foobnix.pdf.info.wrapper.UITab;
import com.foobnix.tts.TTSEngine;
import com.foobnix.tts.TTSNotification;
import com.foobnix.ui2.MainTabs2;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.view.KeyEvent;
import android.view.MenuItem;
import android.view.MenuItem.OnMenuItemClickListener;
import android.view.View;

public class CloseAppDialog {

    public static boolean checkLongPress(Context c, KeyEvent event) {
        int keyCode = event.getKeyCode();
        if (keyCode == 0) {
            keyCode = event.getScanCode();
        }
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            return true;
        }
        return false;
    }

    public static void show(final Context c, final Runnable action) {
        final Runnable closeApp = new Runnable() {

            @Override
            public void run() {
                action.run();

            }
        };
        if (!AppState.get().isShowCloseAppDialog) {
            closeApp.run();
            return;
        }

        AlertDialog.Builder dialog = new AlertDialog.Builder(c);
        dialog.setTitle(R.string.close_application_);

        dialog.setPositiveButton(R.string.cancel, new OnClickListener() {

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

    public static void showOnLongClickDialog(final Activity a, View v, final DocumentController c) {

        List<String> items = new ArrayList<String>();
        items.add(c.getString(R.string.close_book)); //
        items.add(c.getString(R.string.go_to_the_library)); //
        items.add(c.getString(R.string.hide_app)); //
        items.add(c.getString(R.string.close_application)); //

        final DialogInterface.OnClickListener listener = new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {
                TTSNotification.hideNotification();
                TTSEngine.get().shutdown();
                int i = 0;
                if (which == i++) {

                    c.onCloseActivityAdnShowInterstial();

                } else if (which == i++) {
                    c.onCloseActivityFinal(new Runnable() {

                        @Override
                        public void run() {
                            MainTabs2.startActivity(a, UITab.getCurrentTabIndex(UITab.SearchFragment));
                        }
                    });
                } else if (which == i++) {
                    Apps.showDesctop(a);

                } else if (which == i++) {
                    c.onCloseActivityFinal(new Runnable() {

                        @Override
                        public void run() {
                            MainTabs2.closeApp(a);
                        }
                    });

                }
            }

        };

        if (v == null) {
            AlertDialog.Builder dialog = new AlertDialog.Builder(a);
            dialog.setItems(items.toArray(new String[items.size()]), listener);

            // dialog.setNegativeButton(R.string.don_t_show_this_dialog, new
            // OnClickListener() {
            //
            // @Override
            // public void onClick(DialogInterface dialog, int which) {
            // AppState.get().isShowLongBackDialog = false;
            // c.onCloseActivity();
            // dialog.dismiss();
            // }
            // });
            dialog.setPositiveButton(R.string.cancel, new OnClickListener() {

                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialog.dismiss();
                }
            });
            dialog.show();
        } else {
            final MyPopupMenu popupMenu = new MyPopupMenu(a, v);
            for (int i = 0; i < items.size(); i++) {
                final int j = i;
                popupMenu.getMenu().add(items.get(i)).setOnMenuItemClickListener(new OnMenuItemClickListener() {

                    @Override
                    public boolean onMenuItemClick(MenuItem item) {
                        listener.onClick(null, j);
                        return false;
                    }
                });
            }
            popupMenu.show();
        }

    }

}
