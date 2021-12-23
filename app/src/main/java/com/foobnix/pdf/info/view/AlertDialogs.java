package com.foobnix.pdf.info.view;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnDismissListener;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.foobnix.StringResponse;
import com.foobnix.android.utils.Dips;
import com.foobnix.android.utils.IO;
import com.foobnix.android.utils.Keyboards;
import com.foobnix.android.utils.LOG;
import com.foobnix.android.utils.UI;
import com.foobnix.dao2.FileMeta;
import com.foobnix.pdf.info.R;
import com.foobnix.pdf.info.Urls;
import com.foobnix.pdf.info.wrapper.DocumentController;
import com.foobnix.ui2.FileMetaCore;

import java.io.File;

public class AlertDialogs {

    public static void showResultToasts(Context c, boolean result) {
        Toast.makeText(c, result ? R.string.success : R.string.fail, Toast.LENGTH_LONG).show();
    }

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
        showOkDialog(c, message, action, null);
    }

    public static void showOkDialog(final Activity c, final String message, final Runnable action, Runnable onDismiss) {
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
                if (onDismiss != null) {
                    onDismiss.run();
                }
            }
        });
        AlertDialog create = builder.create();
        create.setOnDismissListener(new OnDismissListener() {

            @Override
            public void onDismiss(DialogInterface dialog) {
                if (onDismiss != null) {
                    onDismiss.run();
                }
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

    public static AlertDialog showViewDialog(final Activity c, final View child) {
        return showViewDialog(c, null, child);
    }

    public static AlertDialog showViewDialog(final Activity c, Runnable ondissmiss, final View... childs) {
        final AlertDialog.Builder builder = new AlertDialog.Builder(c);
        builder.setCancelable(true);

        ScrollView scroll = new ScrollView(c);

        LinearLayout l = new LinearLayout(c);
        l.setPadding(Dips.DP_5, Dips.DP_5, Dips.DP_5, Dips.DP_5);
        l.setOrientation(LinearLayout.VERTICAL);

        for (View child : childs) {
            l.addView(child);
        }

        scroll.addView(l);
        builder.setView(scroll);

        builder.setNegativeButton(R.string.close, new DialogInterface.OnClickListener() {

            @Override
            public void onClick(final DialogInterface dialog, final int id) {
            }
        });
        AlertDialog create = builder.create();
        create.setOnDismissListener(new OnDismissListener() {

            @Override
            public void onDismiss(DialogInterface dialog) {
                if (ondissmiss != null) {
                    ondissmiss.run();
                }
                Keyboards.hideNavigation(c);
            }
        });

        create.show();
        return create;
    }

    public static void showTTSDebug(DocumentController controller) {
        TextView t = new TextView(controller.getActivity());
        t.setMinWidth(Dips.DP_800);
        t.setTextIsSelectable(true);
        //t.setMinHeight(Dips.DP_800);
        String textForPage = controller.getPageHtml();
        t.setText(textForPage);
        final AlertDialog alertDialog = AlertDialogs.showViewDialog(controller.getActivity(), t);
        t.setOnClickListener(a -> alertDialog.dismiss());
    }

    public static void showEditDialog(Activity a, String title, String hint, StringResponse onAdd) {
        final AlertDialog.Builder builder = new AlertDialog.Builder(a);
        builder.setTitle(title);

        final EditText edit = new EditText(a);
        edit.setHint(hint);

        builder.setView(edit);

        builder.setNegativeButton(R.string.cancel, new AlertDialog.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {
                Keyboards.close(edit);
            }
        });

        builder.setPositiveButton(R.string.add, new AlertDialog.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {
                Keyboards.close(edit);
            }
        });

        final AlertDialog create = builder.create();
        create.setOnDismissListener(new DialogInterface.OnDismissListener() {

            @Override
            public void onDismiss(DialogInterface dialog) {
                Keyboards.close(edit);
                Keyboards.hideNavigation(a);
            }
        });
        create.show();

        create.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                String text = edit.getText().toString().trim();
                if (onAdd.onResultRecive(text)) {
                    create.dismiss();
                    Keyboards.close(edit);
                    Keyboards.hideNavigation((Activity) a);
                }


            }
        });
    }

    public static void editFileTxt(Activity a, File file, final File outDir, StringResponse onSave) {

        final AlertDialog.Builder builder = new AlertDialog.Builder(a);


        final EditText name = new EditText(a);
        name.setHint(a.getString(R.string.name) + ".txt");
        name.setSingleLine();


        final EditText edit = new EditText(a);
        edit.setHint(R.string.paste_or_type_text_in_here);
        edit.setGravity(Gravity.TOP);
        edit.setMinHeight(Dips.screenHeight());
        edit.setMinWidth(Dips.screenWidth());

        //edit.setLines(20);
        edit.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE);

        if (file != null && file.isFile()) {
            name.setText(file.getName());
            edit.setText(IO.readString(file, true));
        }

        LinearLayout v1 = UI.verticalLayout(a, name, edit);
        //v1.addView(name);
        //v1.addView(edit);
        builder.setView(v1);

        builder.setNegativeButton(R.string.cancel, new AlertDialog.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {
                Keyboards.close(edit);
            }
        });

        builder.setPositiveButton(R.string.save, new AlertDialog.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {
                Keyboards.close(edit);
            }
        });

        final AlertDialog create = builder.create();
        create.setOnDismissListener(new DialogInterface.OnDismissListener() {

            @Override
            public void onDismiss(DialogInterface dialog) {
                Keyboards.close(edit);
                Keyboards.hideNavigation(a);
            }
        });
        create.show();

        create.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                String text = edit.getText().toString().trim();
                String title = name.getText().toString().trim();
                if (!title.endsWith(".txt")) {
                    title += ".txt";
                }

                final File out = new File(outDir , title);
                LOG.d("create file exists", out.exists());
                LOG.d("create file isFile", out.isFile());
                if (out.exists()) {
                    AlertDialogs.showOkDialog(a, "File with the same name is already present, overwrite?", new Runnable() {
                        @Override
                        public void run() {
                            boolean res = IO.writeString(out, text);
                            if (!res) {
                                name.requestFocus();
                                Toast.makeText(a, "Can't create file" + ": " + out.getPath(), Toast.LENGTH_SHORT).show();
                                return;
                            }
                            create.dismiss();
                            onSave.onResultRecive(out.getPath());

                        }
                    });
                    return;
                }
                boolean res = IO.writeString(out, text);
                if (!res) {
                    name.requestFocus();
                    Toast.makeText(a, "Can't create file" + ": " + out.getPath(), Toast.LENGTH_SHORT).show();
                    return;
                }


                final FileMeta meta = FileMetaCore.createMetaIfNeed(out.getPath(), true);


                create.dismiss();
                Keyboards.close(edit);
                Keyboards.hideNavigation(a);

                Toast.makeText(a, R.string.success, Toast.LENGTH_SHORT).show();

                onSave.onResultRecive(out.getPath());


            }
        });
    }

}
