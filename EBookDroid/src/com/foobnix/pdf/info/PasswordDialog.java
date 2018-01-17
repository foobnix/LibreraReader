package com.foobnix.pdf.info;

import com.foobnix.pdf.info.wrapper.AppState;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

public class PasswordDialog {
    public static String EXTRA_APP_PASSWORD = "EXTRA_APP_PASSWORD";

    public static void showDialog(final Activity a) {
        final AlertDialog.Builder builder = new AlertDialog.Builder(a);
        builder.setTitle(AppsConfig.TXT_APP_NAME);

        View inflate = LayoutInflater.from(a).inflate(R.layout.dialog_app_password, null, false);

        final EditText password1 = (EditText) inflate.findViewById(R.id.password);
        final EditText password2 = (EditText) inflate.findViewById(R.id.password2);
        final TextView password2Text = (TextView) inflate.findViewById(R.id.password2Logo);

        password2.setVisibility(View.GONE);
        password2Text.setVisibility(View.GONE);

        builder.setView(inflate);

        builder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {

            @Override
            public void onClick(final DialogInterface dialog, final int id) {
            }
        });
        builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {

            @Override
            public void onClick(final DialogInterface dialog, final int id) {
                a.finish();
            }
        });
        AlertDialog dialog = builder.create();
        dialog.show();

        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                AppState.get().isOpenAppPassword = password1.getText().toString().trim();
                a.finish();
                Intent intent = new Intent(a.getIntent());
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                intent.putExtra(EXTRA_APP_PASSWORD, AppState.get().isOpenAppPassword);
                a.startActivity(intent);
            }
        });

    }

}
