package com.foobnix.pdf.info.view;

import com.foobnix.pdf.info.R;
import com.foobnix.pdf.info.Urls;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;

public class AlertDialogs {

    public static void openUrl(final Context c, final String url) {
        final AlertDialog.Builder builder = new AlertDialog.Builder(c);
        builder.setTitle(R.string.open_web_page);
        builder.setMessage(url);
        builder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {

            @Override
            public void onClick(final DialogInterface dialog, final int id) {
                Urls.open(c, url);
            }
        }).setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {

            @Override
            public void onClick(final DialogInterface dialog, final int id) {
                if (dialog != null) {
                    dialog.dismiss();
                }
            }
        });
        builder.show();
    }

    public static void showOkDialog(final Context c, final String message, final Runnable action) {
        final AlertDialog.Builder builder = new AlertDialog.Builder(c);
        builder.setMessage(message);
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

    public static void showDialog(final Context c, final String message, String okButton, final Runnable action) {
        final AlertDialog.Builder builder = new AlertDialog.Builder(c);
        builder.setMessage(message);
        builder.setPositiveButton(okButton, new DialogInterface.OnClickListener() {

            @Override
            public void onClick(final DialogInterface dialog, final int id) {
                if (action != null) {
                    action.run();
                }

            }
        });
        builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {

            @Override
            public void onClick(final DialogInterface dialog, final int id) {

            }
        });
        builder.show();
    }

}
