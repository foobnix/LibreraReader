package com.foobnix.pdf.info;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnDismissListener;
import android.content.Intent;
import android.hardware.biometrics.BiometricPrompt;
import android.hardware.biometrics.BiometricPrompt.AuthenticationCallback;
import android.os.CancellationSignal;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.foobnix.android.utils.Apps;
import com.foobnix.android.utils.Keyboards;
import com.foobnix.android.utils.LOG;
import com.foobnix.android.utils.TxtUtils;
import com.foobnix.model.AppState;
import com.foobnix.pdf.info.view.EditTextHelper;
import com.foobnix.pdf.info.wrapper.PasswordState;

import java.util.UUID;
import java.util.concurrent.Executors;

public class PasswordDialog {
    public static String EXTRA_APP_PASSWORD = "EXTRA_APP_PASSWORD";

    public static boolean isNeedPasswordDialog(final Activity a) {
        if (a == null || a.getIntent() == null) {
            return false;
        }

        if (!AppState.get().isAppPassword) {
            return false;
        }

        if (PasswordState.get().isFingerPrintPassword && TxtUtils.isNotEmpty(a.getIntent().getStringExtra(PasswordDialog.EXTRA_APP_PASSWORD))) {
            return false;
        }

        if (PasswordState.get().isFingerPrintPassword) {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P && !AppsConfig.IS_FDROID) {
                BiometricPrompt.Builder promptInfo = new BiometricPrompt.Builder(a);
                promptInfo.setTitle(Apps.getApplicationName(a));
                //promptInfo.setSubtitle("Subtitle goes here");
                //promptInfo.setDescription("This is the description");
                promptInfo.setNegativeButton(a.getString(R.string.exit_application), Executors.newSingleThreadExecutor(), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        a.finish();
                    }
                });
                final BiometricPrompt build = promptInfo.build();

                build.authenticate(new CancellationSignal(), a.getMainExecutor(), new AuthenticationCallback() {
                    @Override
                    public void onAuthenticationError(int errorCode, CharSequence errString) {
                        super.onAuthenticationError(errorCode, errString);
                        LOG.d("isFingerPrintPassword", "onAuthenticationError", errorCode, errString);
                        Toast.makeText(a, errString, Toast.LENGTH_SHORT).show();
                        a.finish();


                    }

                    @Override
                    public void onAuthenticationHelp(int helpCode, CharSequence helpString) {
                        super.onAuthenticationHelp(helpCode, helpString);
                    }

                    @Override
                    public void onAuthenticationSucceeded(BiometricPrompt.AuthenticationResult result) {
                        super.onAuthenticationSucceeded(result);
                        Toast.makeText(a, R.string.success, Toast.LENGTH_SHORT).show();
                        a.getIntent().putExtra(PasswordDialog.EXTRA_APP_PASSWORD, UUID.randomUUID().toString());
                        a.finish();
                        a.startActivity(a.getIntent());

                    }

                    @Override
                    public void onAuthenticationFailed() {
                        super.onAuthenticationFailed();
                        LOG.d("isFingerPrintPassword", "onAuthenticationFailed");
                        Toast.makeText(a, R.string.fail, Toast.LENGTH_SHORT).show();
                    }
                });

            }
            return true;

        }

        if (TxtUtils.isNotEmpty(PasswordState.get().appPassword)) {
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
            builder.setTitle(Apps.getApplicationName(a));
        }

        View inflate = LayoutInflater.from(a).inflate(R.layout.dialog_app_password, null, false);

        final EditText password1 = (EditText) inflate.findViewById(R.id.password);
        final EditText password2 = (EditText) inflate.findViewById(R.id.password2);
        final TextView password2Text = (TextView) inflate.findViewById(R.id.password2Logo);
        final TextView password1Text = (TextView) inflate.findViewById(R.id.password1Logo);


        final CheckBox isFingerPrintPassword = (CheckBox) inflate.findViewById(R.id.isFingerPrintPassword);


        password2.setVisibility(isSetPassord ? View.VISIBLE : View.GONE);
        password2Text.setVisibility(isSetPassord ? View.VISIBLE : View.GONE);

        isFingerPrintPassword.setOnCheckedChangeListener((buttonView, isChecked) -> {
            PasswordState.get().isFingerPrintPassword = isChecked;
            password1.setVisibility(TxtUtils.visibleIf(!isChecked));
            password2.setVisibility(TxtUtils.visibleIf(!isChecked));
            password2Text.setVisibility(TxtUtils.visibleIf(!isChecked));
            password1Text.setVisibility(TxtUtils.visibleIf(!isChecked));


        });
        isFingerPrintPassword.setChecked(PasswordState.get().isFingerPrintPassword);
        isFingerPrintPassword.setVisibility(TxtUtils.visibleIf(isSetPassord && android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P && !AppsConfig.IS_FDROID));


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
