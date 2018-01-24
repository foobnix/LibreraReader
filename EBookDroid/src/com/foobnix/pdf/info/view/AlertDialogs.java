package com.foobnix.pdf.info.view;

import com.foobnix.android.utils.Keyboards;
import com.foobnix.pdf.info.R;
import com.foobnix.pdf.info.Urls;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnDismissListener;

public class AlertDialogs {

    public static void openUrl(final Activity c, final String url) {
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
        AlertDialog create = builder.create();
        create.setOnDismissListener(new OnDismissListener() {

            @Override
            public void onDismiss(DialogInterface dialog) {
                Keyboards.hideNavigation(c);
            }
        });

        create.show();
    }

    public static void showOkDialog(final Activity c, final String message, final Runnable action) {
        final AlertDialog.Builder builder = new AlertDialog.Builder(c);
        builder.setMessage(message);
        builder.setCancelable(true);
        builder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {

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
        AlertDialog create = builder.create();
        create.setOnDismissListener(new OnDismissListener() {

            @Override
            public void onDismiss(DialogInterface dialog) {
                Keyboards.hideNavigation(c);
            }
        });

        create.show();
    }

    public static void showDialog(final Activity c, final String message, String okButton, final Runnable onAction) {
        showDialog(c, message, okButton, onAction, null);
    }

    public static void showDialog(final Activity c, final String message, String okButton, final Runnable onAction, final Runnable onCancel) {
        final AlertDialog.Builder builder = new AlertDialog.Builder(c);
        builder.setMessage(message);
        builder.setCancelable(true);
        builder.setPositiveButton(okButton, new DialogInterface.OnClickListener() {

            @Override
            public void onClick(final DialogInterface dialog, final int id) {
                if (onAction != null) {
                    onAction.run();
                }

            }
        });
        builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {

            @Override
            public void onClick(final DialogInterface dialog, final int id) {
                if (onCancel != null) {
                    onCancel.run();
                }
            }
        });
        AlertDialog create = builder.create();
        create.setOnDismissListener(new OnDismissListener() {

            @Override
            public void onDismiss(DialogInterface dialog) {
                Keyboards.hideNavigation(c);
            }
        });

        create.show();
    }

}
