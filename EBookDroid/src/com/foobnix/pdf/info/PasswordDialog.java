package com.foobnix.pdf.info;

import com.foobnix.android.utils.Keyboards;
import com.foobnix.android.utils.TxtUtils;
import com.foobnix.pdf.info.view.EditTextHelper;
import com.foobnix.pdf.info.wrapper.PasswordState;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnDismissListener;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

public class PasswordDialog {
    public static String EXTRA_APP_PASSWORD = "EXTRA_APP_PASSWORD";

    public static boolean isNeedPasswordDialog(final Activity a) {
        if (a == null || a.getIntent() == null) {
            return false;
        }

        if (PasswordState.get().isAppPassword) {
            String password = a.getIntent().getStringExtra(PasswordDialog.EXTRA_APP_PASSWORD);
            if (TxtUtils.isNotEmpty(PasswordState.get().appPassword) && !PasswordState.get().appPassword.equals(password)) {
                PasswordDialog.showDialog(a, false, null);
                return true;
            }
        }
        return false;

    }

    public static AlertDialog showDialog(final Activity a, final boolean isSetPassord, final Runnable onDismiss) {
        final AlertDialog.Builder builder = new AlertDialog.Builder(a);
        builder.setCancelable(false);
        if (isSetPassord) {
            builder.setTitle(R.string.set_an_application_password);
        } else {
            builder.setTitle(AppsConfig.TXT_APP_NAME);
        }

        View inflate = LayoutInflater.from(a).inflate(R.layout.dialog_app_password, null, false);

        final EditText password1 = (EditText) inflate.findViewById(R.id.password);
        final EditText password2 = (EditText) inflate.findViewById(R.id.password2);
        final TextView password2Text = (TextView) inflate.findViewById(R.id.password2Logo);

        password2.setVisibility(isSetPassord ? View.VISIBLE : View.GONE);
        password2Text.setVisibility(isSetPassord ? View.VISIBLE : View.GONE);

        if (isSetPassord) {
            password1.setText(TxtUtils.nullToEmpty(PasswordState.get().appPassword));
            password2.setText(TxtUtils.nullToEmpty(PasswordState.get().appPassword));
        }

        builder.setView(inflate);

        builder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {

            @Override
            public void onClick(final DialogInterface dialog, final int id) {
                Keyboards.close(password1);
            }
        });
        builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {

            @Override
            public void onClick(final DialogInterface dialog, final int id) {
                if (!isSetPassord) {
                    a.finish();
                }
                if (onDismiss != null) {
                    onDismiss.run();
                }
                Keyboards.close(password1);
            }
        });
        final AlertDialog dialog = builder.create();

        dialog.setOnDismissListener(new OnDismissListener() {

            @Override
            public void onDismiss(DialogInterface dialog) {
                Keyboards.hideNavigation(a);
            }
        });

        dialog.show();

        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                String pass1 = password1.getText().toString().trim();
                if (isSetPassord) {
                    String pass2 = password2.getText().toString().trim();

                    if (TxtUtils.isEmpty(pass1) && TxtUtils.isEmpty(pass2)) {
                        PasswordState.get().appPassword = null;
                        dialog.dismiss();
                        if (onDismiss != null)
                            onDismiss.run();
                        Keyboards.close(password1);
                        return;
                    }

                    if (TxtUtils.isEmpty(pass1)) {
                        password1.requestFocus();
                        Toast.makeText(a, R.string.incorrect_value, Toast.LENGTH_SHORT).show();
                        return;
                    }
                    if (TxtUtils.isEmpty(pass2)) {
                        password2.requestFocus();
                        Toast.makeText(a, R.string.incorrect_value, Toast.LENGTH_SHORT).show();
                        return;
                    }
                    if (!pass1.equals(pass2)) {
                        Toast.makeText(a, R.string.passwords_do_not_match, Toast.LENGTH_SHORT).show();
                        return;
                    }
                    PasswordState.get().appPassword = pass1;
                    PasswordState.get().save(a);

                    Toast.makeText(a, R.string.success, Toast.LENGTH_LONG).show();
                    if (onDismiss != null)
                        onDismiss.run();

                    a.getIntent().putExtra(PasswordDialog.EXTRA_APP_PASSWORD, PasswordState.get().appPassword);
                    Keyboards.close(password1);
                    dialog.dismiss();
                } else {

                    if (TxtUtils.isEmpty(pass1) || !pass1.equals(PasswordState.get().appPassword)) {
                        Toast.makeText(a, R.string.incorrect_password, Toast.LENGTH_SHORT).show();
                        password1.setText("");
                        return;
                    }
                    a.finish();
                    Intent intent = a.getIntent();
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    intent.putExtra(EXTRA_APP_PASSWORD, PasswordState.get().appPassword);
                    a.startActivity(intent);
                }
                Keyboards.close(password1);
            }
        });

        EditTextHelper.enableKeyboardSearch(password1, new Runnable() {

            @Override
            public void run() {
                dialog.getButton(AlertDialog.BUTTON_POSITIVE).performClick();
                Keyboards.close(password1);
            }
        });

        return dialog;

    }

}
