package com.foobnix.pdf.info.view;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Color;
import android.os.Handler;
import android.text.InputType;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.view.WindowManager;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.fragment.app.FragmentActivity;

import com.buzzingandroid.ui.HSVColorPickerDialog;
import com.foobnix.android.utils.BaseItemLayoutAdapter;
import com.foobnix.android.utils.Dips;
import com.foobnix.android.utils.IO;
import com.foobnix.android.utils.IntegerResponse;
import com.foobnix.android.utils.Keyboards;
import com.foobnix.android.utils.LOG;
import com.foobnix.android.utils.ResultResponse;
import com.foobnix.android.utils.StringDB;
import com.foobnix.android.utils.TxtUtils;
import com.foobnix.android.utils.UI;
import com.foobnix.android.utils.Vibro;
import com.foobnix.android.utils.Views;
import com.foobnix.android.utils.WebViewUtils;
import com.foobnix.dao2.FileMeta;
import com.foobnix.drive.GFile;
import com.foobnix.model.AppProfile;
import com.foobnix.model.AppState;
import com.foobnix.model.TagData;
import com.foobnix.pdf.info.ExtUtils;
import com.foobnix.pdf.info.R;
import com.foobnix.pdf.info.TintUtil;
import com.foobnix.pdf.info.WebViewHepler;
import com.foobnix.pdf.info.databinding.DialogLinksColorBinding;
import com.foobnix.pdf.info.databinding.DialogLoadingBookBinding;
import com.foobnix.pdf.info.databinding.DialogTagsBinding;
import com.foobnix.pdf.info.databinding.ItemColorBinding;
import com.foobnix.pdf.info.model.BookCSS;
import com.foobnix.pdf.info.widget.ChooserDialogFragment;
import com.foobnix.pdf.info.wrapper.DocumentController;
import com.foobnix.pdf.info.wrapper.MagicHelper;
import com.foobnix.sys.TempHolder;
import com.foobnix.ui2.AppDB;
import com.foobnix.ui2.AppDB.SEARCH_IN;
import com.jmedeisis.draglinearlayout.DragLinearLayout;

import org.librera.JSONException;
import org.librera.LinkedJSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Pattern;

public class Dialogs {
    static AlertDialog create;

    public static void replaceTTSDialog(Activity activity) {
        final DragLinearLayout root = new DragLinearLayout(activity);
        root.setOrientation(LinearLayout.VERTICAL);

        LinearLayout dicts = UI.verticalLayout(activity);

        if (TxtUtils.isNotEmpty(BookCSS.get().dictPath)) {
            final List<String> strings = StringDB.asList(BookCSS.get().dictPath);
            for (String s : strings) {
                final TextView text = UI.text(activity, s);
                text.setSingleLine();
                text.setEllipsize(TextUtils.TruncateAt.START);
                text.setPadding(Dips.DP_2, Dips.DP_2, Dips.DP_2, Dips.DP_2);

                text.setOnClickListener(a -> StringDB.delete(BookCSS.get().dictPath, s, result -> {
                    BookCSS.get().dictPath = result;
                    text.setVisibility(View.GONE);
                }));
                dicts.addView(text);
            }
        }
        root.addView(dicts);

        try {
            LinkedJSONObject linkedJsonObjectRoot = new LinkedJSONObject(AppState.get().lineTTSReplacements3);
            LOG.d("TTS-load", AppState.get().lineTTSReplacements3);
            //new LinkedJSONObject(AppState.get().lineTTSReplacements3);

            final Iterator<String> rootKeys = linkedJsonObjectRoot.keys();

            while (rootKeys.hasNext()) {
                String key = rootKeys.next();
                String value = linkedJsonObjectRoot.getString(key);

                LOG.d("TTS-load-key", key, value);

                LinearLayout h = new LinearLayout(activity);
                root.setPadding(Dips.DP_5, Dips.DP_5, Dips.DP_5, Dips.DP_5);
                h.setWeightSum(2);
                h.setOrientation(LinearLayout.HORIZONTAL);
                h.setGravity(Gravity.CENTER_VERTICAL);

                EditText from = new EditText(activity);
                from.setWidth(Dips.DP_150);
                from.setText(key);
                from.setSingleLine();

                TextView text = new TextView(activity);
                text.setText(" -> ");
                root.setPadding(Dips.DP_10, Dips.DP_0, Dips.DP_10, Dips.DP_0);

                EditText to = new EditText(activity);
                to.setWidth(Dips.DP_120);
                to.setText(value);
                to.setSingleLine();
                to.setHint("_");

                ImageView img = new ImageView(activity);
                img.setPadding(Dips.DP_10, Dips.DP_10, Dips.DP_10, Dips.DP_10);
                img.setMaxWidth(Dips.DP_25);
                img.setMaxHeight(Dips.DP_25);

                img.setImageResource(R.drawable.glyphicons_208_remove_2);
                TintUtil.setTintImageWithAlpha(img);

                img.setOnClickListener(v -> root.removeView(h));

                ImageView move = new ImageView(activity);
                move.setPadding(Dips.DP_10, Dips.DP_10, Dips.DP_10, Dips.DP_10);
                move.setMaxWidth(Dips.DP_25);
                move.setMaxHeight(Dips.DP_25);

                move.setImageResource(R.drawable.glyphicons_517_menu_hamburger);
                TintUtil.setTintImageWithAlpha(move);

                from.setLayoutParams(new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT, 1.0f));
                text.setLayoutParams(new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT, 0f));
                to.setLayoutParams(new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT, 1.0f));
                img.setLayoutParams(new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT, 0f));
                move.setLayoutParams(new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT, 0f));

                h.addView(from);
                h.addView(text);
                h.addView(to);
                h.addView(img);
                h.addView(move);

                root.addView(h);
                root.setViewDraggable(h, move);
            }
        } catch (Exception e) {
            LOG.e(e);
        }

        TextView add = new TextView(activity, null, R.style.textLink);
        add.setText(activity.getString(R.string.add_rule));
        add.setPadding(Dips.DP_2, Dips.DP_2, Dips.DP_2, Dips.DP_2);

        TxtUtils.underlineTextView(add);
        add.setOnClickListener(v -> {
            LinearLayout h = new LinearLayout(activity);
            h.setOrientation(LinearLayout.HORIZONTAL);

            EditText from = new EditText(activity);
            from.setWidth(Dips.DP_200);
            from.setSingleLine();
            from.requestFocus();

            TextView text = new TextView(activity);
            text.setText(" -> ");
            root.setPadding(Dips.DP_10, Dips.DP_0, Dips.DP_10, Dips.DP_0);

            EditText to = new EditText(activity);
            to.setWidth(Dips.DP_120);
            to.setSingleLine();
            to.setHint("_");

            h.addView(from);
            h.addView(text);
            h.addView(to);
            root.addView(h, root.getChildCount() - 3);
        });

        RunnableBoolean save = () -> {
            LinkedJSONObject res = new LinkedJSONObject();
            //AppState.get().lineTTSAccents = lineTTSAccents.getText().toString();
            boolean hasErrors = false;
            for (int i = 0; i < root.getChildCount(); i++) {
                final View childAt = root.getChildAt(i);
                if (childAt instanceof LinearLayout) {
                    final LinearLayout line = (LinearLayout) childAt;
                    if (line.getOrientation() == LinearLayout.VERTICAL) {
                        continue;
                    }
                    EditText childFrom = (EditText) line.getChildAt(0);
                    String from = childFrom.getText().toString();
                    String to = ((EditText) line.getChildAt(2)).getText().toString();

                    LOG.d("TTS-add", from, to);

                    try {
                        if (from.startsWith("*")) {
                            try {
                                Pattern.compile(from.substring(1));
                                res.put(from, to);
                            } catch (Exception e) {
                                LOG.d("TTS-incorrect value", from, to);
                                hasErrors = true;
                                childFrom.requestFocus();
                                Toast.makeText(activity, R.string.incorrect_value, Toast.LENGTH_SHORT).show();
                            }
                        } else if (TxtUtils.isNotEmpty(from)) {
                            res.put(from, to);
                        }
                    } catch (Exception e) {
                        LOG.e(e);
                    }
                }
            }
            if (!hasErrors) {
                AppState.get().lineTTSReplacements3 = res.toString();
                LOG.d("TTS-save", AppState.get().lineTTSReplacements3);
            }
            return hasErrors;
        };

        TextView export = new TextView(activity, null, R.style.textLink);
        export.setText(activity.getString(R.string.export_));
        export.setPadding(Dips.DP_2, Dips.DP_2, Dips.DP_2, Dips.DP_2);
        TxtUtils.underlineTextView(export);
        export.setOnClickListener(v -> {
            if (save.run()) {

            }
            ChooserDialogFragment.createFile((FragmentActivity) activity, "TTS-RegEx.txt").setOnSelectListener((result1, result2) -> {

                //errors
                try {
                    LinkedJSONObject linkedJsonObject = new LinkedJSONObject(AppState.get().lineTTSReplacements3);

                    final Iterator<String> keys = linkedJsonObject.keys();
                    StringBuilder res = new StringBuilder();
                    while (keys.hasNext()) {
                        String key = keys.next();
                        String value = linkedJsonObject.getString(key);
                        res.append(String.format("\"%s\" \"%s\"\n", key, value));
                    }
                    IO.writeString(new File(result1), res.toString());
                } catch (Exception e) {
                    Toast.makeText(activity, R.string.msg_unexpected_error, Toast.LENGTH_LONG).show();
                    LOG.e(e);
                }
                Toast.makeText(activity, R.string.success, Toast.LENGTH_LONG).show();
                Vibro.vibrate();
                result2.dismiss();

                return false;
            });
        });

        TextView importFile = new TextView(activity, null, R.style.textLink);
        importFile.setText(activity.getString(R.string.import_));
        importFile.setPadding(Dips.DP_2, Dips.DP_2, Dips.DP_2, Dips.DP_2);
        TxtUtils.underlineTextView(importFile);
        importFile.setOnClickListener(v -> ChooserDialogFragment.chooseFile((FragmentActivity) activity, "TTS-RegEx.txt")
                .setOnSelectListener((result1, result2) -> {
                    try {
                        LinkedJSONObject linkedJsonObject = new LinkedJSONObject();

                        TxtUtils.processDict(new FileInputStream(result1), new TxtUtils.ReplaceRule() {
                            @Override
                            public void replace(String from, String to) {
                                try {
                                    linkedJsonObject.put(from, to);
                                } catch (JSONException e) {
                                    LOG.e(e);
                                }
                            }

                            @Override
                            public void replaceAll(String from, String to) {
                                try {
                                    linkedJsonObject.put(from, to);
                                } catch (JSONException e) {
                                    LOG.e(e);
                                }
                            }
                        });

                        AppState.get().lineTTSReplacements3 = linkedJsonObject.toString();
                        Toast.makeText(activity, R.string.success, Toast.LENGTH_LONG).show();

                        Vibro.vibrate();
                        result2.dismiss();

                        create.dismiss();
                        //Views.handler.postDelayed(()->replaceTTSDialog(activity), 150);
                        replaceTTSDialog(activity);
                    } catch (Exception e) {
                        LOG.e(e);
                        Toast.makeText(activity, R.string.msg_unexpected_error, Toast.LENGTH_LONG).show();
                        Vibro.vibrate();
                        result2.dismiss();
                    }
                    return false;
                }));

        LinearLayout line = new LinearLayout(activity);
        line.setOrientation(LinearLayout.HORIZONTAL);

        add.setSingleLine();
        export.setSingleLine();
        importFile.setSingleLine();

        add.setEllipsize(TextUtils.TruncateAt.END);
        export.setEllipsize(TextUtils.TruncateAt.END);
        importFile.setEllipsize(TextUtils.TruncateAt.END);

        line.addView(add);
        line.addView(Views.newText(activity, " ("));
        line.addView(export);
        line.addView(Views.newText(activity, " | "));
        line.addView(importFile);
        line.addView(Views.newText(activity, " )"));

        root.addView(Views.newFrameLayout(activity, line));

        TextView addDict = new TextView(activity, null, R.style.textLink);
        addDict.setText(activity.getString(R.string.add_dictionary) + " (.txt RegEx @Voice)");
        addDict.setPadding(Dips.DP_2, Dips.DP_2, Dips.DP_2, Dips.DP_2);
        TxtUtils.underlineTextView(addDict);
        addDict.setOnClickListener(v -> {
            ChooserDialogFragment.chooseFile((FragmentActivity) activity, ".txt").setOnSelectListener((result1, result2) -> {
                if (!StringDB.contains(BookCSS.get().dictPath, result1)) {
                    StringDB.add(BookCSS.get().dictPath, result1, result ->
                            BookCSS.get().dictPath = result);

                    final TextView text = UI.text(activity, result1);
                    text.setSingleLine();
                    text.setPadding(Dips.DP_2, Dips.DP_2, Dips.DP_2, Dips.DP_2);
                    text.setEllipsize(TextUtils.TruncateAt.START);
                    text.setOnClickListener(a -> StringDB.delete(BookCSS.get().dictPath, result1, result3 -> {
                        BookCSS.get().dictPath = result3;
                        text.setVisibility(View.GONE);
                    }));
                    dicts.addView(text);

                    Vibro.vibrate();
                }
                result2.dismiss();
                return false;
            });
        });

        addDict.setSingleLine();
        addDict.setEllipsize(TextUtils.TruncateAt.END);
        root.addView(addDict);

        TextView restore = new TextView(activity, null, R.style.textLink);
        restore.setPadding(Dips.DP_2, Dips.DP_2, Dips.DP_2, Dips.DP_2);

        restore.setSingleLine();
        restore.setEllipsize(TextUtils.TruncateAt.END);

        restore.setText(R.string.restore_defaults_short);
        TxtUtils.underlineTextView(restore);
        root.addView(restore);

        ScrollView scroll = new ScrollView(activity);
        scroll.setOverScrollMode(ScrollView.OVER_SCROLL_IF_CONTENT_SCROLLS);
        scroll.setVerticalScrollBarEnabled(true);
        scroll.addView(root);

        final AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setTitle(R.string.replacements);
        builder.setCancelable(true);
        builder.setView(scroll);
        builder.setNegativeButton(R.string.close, (dialog, id) -> { });
        builder.setPositiveButton(R.string.apply, (dialog, id) -> { });
        create = builder.create();
        create.show();

        create.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {

            LOG.d("lineTTSReplacements3", AppState.get().lineTTSReplacements3);
            if (!save.run()) {
                create.dismiss();
            }

        });

        restore.setOnClickListener((a) -> {
            AppState.get().lineTTSReplacements3 = AppState.TTS_REPLACEMENTS;
            //AppState.get().lineTTSAccents = AppState.TTS_ACCENTS;
            BookCSS.get().dictPath = "";

            create.dismiss();
            replaceTTSDialog(activity);
        });

        create.setOnDismissListener(dialog -> Keyboards.hideNavigation(activity));
    }

    public static void showSyncLOGDialog(Activity a) {
        TextView result = new TextView(a);

        final AtomicBoolean flag = new AtomicBoolean(true);

        new Thread(() -> {
            while (flag.get()) {
                a.runOnUiThread(() -> result.setText(GFile.debugOut));
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {

                }
            }
        }).start();

        result.setText(GFile.debugOut);

        result.setTextSize(12);
        result.setText(GFile.debugOut);
        result.setMinWidth(Dips.dpToPx(1000));
        result.setMinHeight(Dips.dpToPx(1000));

        TextView t = UI.uText(a, a.getString(R.string.clear_log));
        t.setTextSize(16);
        t.setOnClickListener(v -> GFile.debugOut = "");

        AlertDialogs.showViewDialog(a, () -> flag.set(false), t, result);
    }

    public static void testWebView(final Activity a, final String path) {
        if (WebViewHepler.webView == null) {
            WebViewHepler.init(a);
        }

        final ImageView imageView = new ImageView(a);

        WebViewHepler.getBitmap(path, result -> {
            imageView.setImageBitmap(result);
            return false;
        });

        AlertDialog.Builder builder = new AlertDialog.Builder(a);
        builder.setView(imageView);

        builder.setPositiveButton(R.string.close, (dialog, id) -> { });
        builder.show();
    }

    public static void customValueDialog(final Context a, final int initValue,
                                         final IntegerResponse response) {
        final AlertDialog.Builder builder = new AlertDialog.Builder(a);
        builder.setTitle(R.string.custom_value);

        final CustomSeek myValue = new CustomSeek(a);
        myValue.init(1, 100, initValue);
        myValue.setOnSeekChanged(result -> false);
        myValue.setValueText(initValue + "%");

        builder.setView(myValue);

        builder.setPositiveButton(R.string.apply, (dialog, which) -> response.onResultRecive(myValue.getCurrentValue()));
        builder.setNegativeButton(R.string.cancel, (dialog, which) -> { });

        builder.show();
    }

    public static void showLinksColorDialog(final Activity a, final Runnable action) {
        final AlertDialog.Builder builder = new AlertDialog.Builder(a);
        builder.setCancelable(true);
        builder.setTitle(R.string.link_color);

        LayoutInflater inflater = LayoutInflater.from(a);
        final DialogLinksColorBinding dialogLinksColorBinding =
                DialogLinksColorBinding.inflate(LayoutInflater.from(a), null, false);

        dialogLinksColorBinding.isUiTextColor.setOnCheckedChangeListener((buttonView, isChecked) -> {
            AppState.get().isUiTextColor = isChecked;
            if (action != null) {
                action.run();
            }
        });

        dialogLinksColorBinding.colorsLine1.removeAllViews();

        for (String color : AppState.STYLE_COLORS) {
            final ItemColorBinding itemColorBinding = ItemColorBinding.inflate(inflater,
                    dialogLinksColorBinding.getRoot(), false);
            itemColorBinding.getRoot().setBackgroundColor(Color.TRANSPARENT);
            final int intColor = Color.parseColor(color);
            itemColorBinding.itColor.setBackgroundColor(intColor);

            dialogLinksColorBinding.colorsLine1.addView(itemColorBinding.getRoot(),
                    new LayoutParams(Dips.dpToPx(30), Dips.dpToPx(30)));

            itemColorBinding.getRoot().setOnClickListener(v -> {
                dialogLinksColorBinding.isUiTextColor.setChecked(true);
                AppState.get().isUiTextColor = true;
                AppState.get().uiTextColor = intColor;
                if (action != null) {
                    action.run();
                }
            });
        }

        final ItemColorBinding itemColorBinding = ItemColorBinding.inflate(inflater,
                dialogLinksColorBinding.getRoot(), false);
        itemColorBinding.getRoot().setBackgroundColor(Color.TRANSPARENT);
        itemColorBinding.itColor.setColorFilter(a.getResources().getColor(R.color.tint_gray));
        itemColorBinding.itColor.setImageResource(R.drawable.glyphicons_433_plus);
        itemColorBinding.itColor.setBackgroundColor(AppState.get().uiTextColorUser);
        dialogLinksColorBinding.colorsLine1.addView(itemColorBinding.getRoot(),
                new LayoutParams(Dips.dpToPx(30), Dips.dpToPx(30)));

        itemColorBinding.getRoot().setOnClickListener(v -> new HSVColorPickerDialog(a, AppState.get().uiTextColorUser, color -> {
            dialogLinksColorBinding.isUiTextColor.setChecked(true);
            AppState.get().isUiTextColor = true;
            AppState.get().uiTextColor = color;
            AppState.get().uiTextColorUser = color;
            itemColorBinding.itColor.setBackgroundColor(color);

            if (action != null) {
                action.run();
            }
        }).show());

        builder.setView(dialogLinksColorBinding.getRoot());
        builder.setPositiveButton(R.string.close, (dialog, id) -> { });
        AlertDialog dialog = builder.show();
        dialog.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
    }

    public static AlertDialog loadingBook(Context c, final Runnable onCancel) {
        try {
            final AlertDialog.Builder builder = new AlertDialog.Builder(c);
            final DialogLoadingBookBinding binding =
                    DialogLoadingBookBinding.inflate(LayoutInflater.from(c), null, false);

            binding.myProgressBarLoading.setSaveEnabled(false);
            binding.myProgressBarLoading.setSaveFromParentEnabled(false);
            TintUtil.setDrawableTint(binding.myProgressBarLoading.getIndeterminateDrawable().getCurrent(), AppState.get().isDayNotInvert ? TintUtil.color : Color.WHITE);

            TintUtil.setTintImageNoAlpha(binding.onCancel, AppState.get().isDayNotInvert ? TintUtil.color : Color.WHITE);
            binding.onCancel.setOnClickListener(v -> {
                LOG.d("loadingBook Cancel");
                onCancel.run();
            });

            builder.setView(binding.getRoot());
            builder.setCancelable(false);

            AlertDialog dialog = builder.show();
            dialog.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);

            if (AppState.get().isExperimental) {
                WebViewUtils.init(c);
            }

            return dialog;
        } catch (Exception e) {
            return null;
        }
    }

    public static void showEditDialog(final Context c, String title, String init,
                                      final ResultResponse<String> onresult) {
        final AlertDialog.Builder builder = new AlertDialog.Builder(c);
        builder.setTitle(title);
        final EditText input = new EditText(c);
        input.setSingleLine();
        input.setText(init);
        builder.setView(input);
        builder.setPositiveButton(R.string.apply, (dialog, which) -> onresult.onResultRecive(input.getText().toString()));
        builder.setNeutralButton(R.string.add, (dialog, which) -> onresult.onResultRecive(input.getText().toString()));
        builder.setNegativeButton(R.string.cancel, (dialog, which) -> { });

        AlertDialog create = builder.show();

        create.setOnDismissListener(dialog -> Keyboards.close(input));

        create.getButton(AlertDialog.BUTTON_NEUTRAL).setOnClickListener(v -> new HSVColorPickerDialog(c, AppState.get().uiTextColorUser,
                color -> {
            String res = input.getText().toString();
            input.setText(res + "," + MagicHelper.colorToString(color));
        }).show());
    }

    public static void showEditDialog2(final Activity c, String title, String init,
                                       final ResultResponse<String> onresult) {
        final AlertDialog.Builder builder = new AlertDialog.Builder(c);
        builder.setTitle(title);
        final EditText input = new EditText(c);
        input.setSingleLine();

        if (init != null) {
            if (!init.endsWith("/")) {
                init += "/";
            }
            input.setText(init);
            input.setSelection(init.length());
        }
        builder.setView(input);
        builder.setPositiveButton(R.string.ok, (dialog, which) -> onresult.onResultRecive(input.getText().toString()));

        builder.setNegativeButton(R.string.cancel, (dialog, which) -> { });
        final AlertDialog alertDialog = builder.create();
        alertDialog.setOnDismissListener(dialog -> {
            Keyboards.close(input);
            Keyboards.hideNavigation(c);
        });
        alertDialog.show();
    }

    public static void showDeltaPage(final FrameLayout anchor,
                                     final DocumentController controller, final int pageNumber, final Runnable reloadUI) {
        Vibro.vibrate();
        String txt = controller.getString(R.string.set_the_current_page_number);

        final AlertDialog.Builder builder = new AlertDialog.Builder(anchor.getContext());
        builder.setMessage(txt);

        final EditText edit = new EditText(anchor.getContext());
        edit.setInputType(InputType.TYPE_CLASS_NUMBER);
        edit.setMaxWidth(Dips.dpToPx(100));
        edit.setText("" + pageNumber);

        builder.setView(edit);
        builder.setCancelable(false);
        builder.setPositiveButton(R.string.ok, (dialog, id) -> {
            try {
                TempHolder.get().pageDelta = Integer.parseInt(edit.getText().toString()) - controller.getCurentPageFirst1();
            } catch (Exception e) {
                TempHolder.get().pageDelta = 0;
            }
            reloadUI.run();
        });
        builder.setNeutralButton(R.string.restore_defaults_short, (dialog, id) ->
                AlertDialogs.showOkDialog(controller.getActivity(),
                        controller.getString(R.string.restore_defaults_full), () -> {
                            TempHolder.get().pageDelta = 0;
                            reloadUI.run();
                        }));
        builder.setNegativeButton(R.string.cancel, (dialog, id) -> { });
        AlertDialog create = builder.create();
        create.setOnDismissListener(dialog -> Keyboards.hideNavigation(controller.getActivity()));
        create.show();
    }

    public static void showContrastDialogByUrl(final Activity c, final Runnable action) {
        final AlertDialog.Builder builder = new AlertDialog.Builder(c);
        builder.setCancelable(true);
        builder.setTitle(R.string.contrast_and_brightness);

        LinearLayout l = getBCView(c, action);
        builder.setView(l);

        builder.setPositiveButton(R.string.ok, (dialog, id) -> {
            if (action != null) {
                action.run();
            }
        });
        builder.show();
    }

    public static LinearLayout getBCView(final Activity c, final Runnable action) {
        LinearLayout l = new LinearLayout(c);
        l.setPadding(Dips.dpToPx(5), Dips.dpToPx(5), Dips.dpToPx(5), Dips.dpToPx(5));
        l.setOrientation(LinearLayout.VERTICAL);

        final Handler handler = new Handler();

        final Runnable actionWrapper = () -> {
            handler.removeCallbacks(action);
            handler.postDelayed(action, 100);
        };

        final CheckBox isEnableBC = new CheckBox(c);
        isEnableBC.setText(R.string.enable_contrast_and_brightness);
        isEnableBC.setChecked(AppState.get().isEnableBC);
        isEnableBC.setOnCheckedChangeListener((buttonView, isChecked) -> {
            AppState.get().isEnableBC = isChecked;
            actionWrapper.run();
        });

        final CustomSeek contrastSeek = new CustomSeek(c);
        contrastSeek.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT, 0));
        contrastSeek.init(0, 200, AppState.get().contrastImage);
        contrastSeek.setOnSeekChanged(result -> {
            AppState.get().contrastImage = result;
            isEnableBC.setChecked(true);
            actionWrapper.run();
            return false;
        });

        final CustomSeek brightnesSeek = new CustomSeek(c);
        brightnesSeek.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT, 0));
        brightnesSeek.init(-100, 100, AppState.get().brigtnessImage);
        brightnesSeek.setOnSeekChanged(result -> {
            AppState.get().brigtnessImage = result;
            isEnableBC.setChecked(true);
            actionWrapper.run();
            return false;
        });

        final CheckBox bolderText = new CheckBox(c);
        bolderText.setText(R.string.make_text_bold);
        bolderText.setChecked(AppState.get().bolderTextOnImage);
        bolderText.setOnCheckedChangeListener((buttonView, isChecked) -> {
            AppState.get().bolderTextOnImage = isChecked;
            if (isChecked) {
                isEnableBC.setChecked(true);
            }
            actionWrapper.run();
        });

        TextView contrastText = new TextView(c);
        contrastText.setText(R.string.contrast);

        TextView brightnessText = new TextView(c);
        brightnessText.setText(R.string.brightness);

        l.addView(isEnableBC);
        l.addView(contrastText);
        l.addView(contrastSeek);
        l.addView(brightnessText);
        l.addView(brightnesSeek);
        l.addView(bolderText);

        TextView defaults = new TextView(c);
        defaults.setTextAppearance(c, R.style.textLinkStyle);
        defaults.setText(R.string.restore_defaults_short);
        TxtUtils.underlineTextView(defaults);
        defaults.setOnClickListener(v -> AlertDialogs.showOkDialog(c,
                c.getString(R.string.restore_defaults_full), () -> {
            AppState.get().brigtnessImage = 0;
            AppState.get().contrastImage = 0;
            AppState.get().bolderTextOnImage = false;
            isEnableBC.setChecked(AppState.get().bolderTextOnImage);

            brightnesSeek.reset(AppState.get().brigtnessImage);
            contrastSeek.reset(AppState.get().contrastImage);

            actionWrapper.run();
        }));

        l.addView(defaults);
        return l;
    }

    public static void addTagsDialog(final Context a, final Runnable onRefresh) {
        final AlertDialog.Builder builder = new AlertDialog.Builder(a);
        builder.setTitle(R.string.tag);

        final EditText edit = new EditText(a);
        edit.setHint("#");

        builder.setView(edit);

        builder.setNegativeButton(R.string.cancel, (dialog, which) -> Keyboards.close(edit));

        builder.setPositiveButton(R.string.add, (dialog, which) -> Keyboards.close(edit));

        final AlertDialog create = builder.create();
        create.setOnDismissListener(dialog -> { });
        create.show();

        create.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String text = edit.getText().toString().trim();

            if (TxtUtils.isEmpty(text)) {
                Toast.makeText(a, R.string.incorrect_value, Toast.LENGTH_SHORT).show();
                return;
            }

            if (!text.startsWith("#")) {
                text = "#" + text;
            }

            if (StringDB.contains(AppState.get().bookTags, text)) {
                Toast.makeText(a, R.string.incorrect_value, Toast.LENGTH_SHORT).show();
                return;
            }

            Keyboards.close(edit);
            Keyboards.hideNavigation((Activity) a);

            StringDB.add(AppState.get().bookTags, text, (db) -> AppState.get().bookTags = db);
            if (onRefresh != null) {
                onRefresh.run();
            }
            create.dismiss();
            AppProfile.save(a);
        });
    }

    public static void showTagsDialog(final Activity a, final File file,
                                      final boolean isReadBookOption, final Runnable refresh) {
        final FileMeta fileMeta = file == null ? null : AppDB.get().getOrCreate(file.getPath());
        final String tag = file == null ? "" : fileMeta.getTag();

        LOG.d("showTagsDialog book tags", tag);

        final AlertDialog.Builder builder = new AlertDialog.Builder(a);
        // builder.setTitle(R.string.tag);

        final DialogTagsBinding binding = DialogTagsBinding.inflate(LayoutInflater.from(a), null, false);
        View inflate = LayoutInflater.from(a).inflate(R.layout.dialog_tags, null, false);

        TxtUtils.underline(binding.addTag, a.getString(R.string.create_tag));

        final List<String> tags = getAllTags(tag);

        final Set<Integer> checked = new HashSet<>();

        final BaseItemLayoutAdapter<String> adapter = new BaseItemLayoutAdapter<String>(a, R.layout.tag_item, tags) {
            @Override
            public void populateView(View layout, final int position, final String tagName) {
                CheckBox text = layout.findViewById(R.id.tagName);
                text.setText(tagName);
                if (fileMeta == null) {
                    text.setEnabled(false);
                }

                text.setOnCheckedChangeListener((buttonView, isChecked) -> {
                    if (isChecked) {
                        checked.add(position);
                    } else {
                        checked.remove(position);
                    }
                });

                text.setChecked(StringDB.contains(tag, tagName));

                ImageView delete = layout.findViewById(R.id.deleteTag);
                TintUtil.setTintImageWithAlpha(delete, Color.GRAY);
                delete.setOnClickListener(v -> AlertDialogs.showOkDialog(a,
                        a.getString(R.string.do_you_want_to_delete_this_tag_from_all_books_), () -> {
                    checked.clear();

                    LOG.d("AppState.get().bookTags before", AppState.get().bookTags);
                    StringDB.delete(AppState.get().bookTags, tagName, (db) -> AppState.get().bookTags = db);

                    tags.clear();
                    tags.addAll(getAllTags(tag));

                    notifyDataSetChanged();

                    List<FileMeta> allWithTag = AppDB.get().getAllWithTag(tagName);
                    for (FileMeta meta : allWithTag) {
                        StringDB.delete(meta.getTag(), tagName, meta::setTag);
                        TagData.saveTags(meta);
                    }
                    AppDB.get().updateAll(allWithTag);

                    if (refresh != null) {
                        refresh.run();
                    }

                    LOG.d("AppState.get().bookTags after", AppState.get().bookTags);
                }));
            }
        };

        binding.addTag.setOnClickListener(v -> addTagsDialog(a, () -> {
            tags.clear();
            tags.addAll(getAllTags(tag));
            adapter.notifyDataSetChanged();
        }));

        binding.listView1.setAdapter(adapter);

        builder.setView(inflate);

        builder.setNegativeButton(R.string.close, (dialog, which) -> { });

        if (fileMeta != null) {
            builder.setPositiveButton(R.string.apply, new AlertDialog.OnClickListener() {
                String res = "";

                @Override
                public void onClick(DialogInterface dialog, int which) {
                    res = "";
                    for (int i : checked) {
                        StringDB.add(res, tags.get(i), (db) -> res = db);
                    }
                    LOG.d("showTagsDialog", res);
                    if (fileMeta != null) {
                        fileMeta.setTag(res);
                        AppDB.get().update(fileMeta);
                        TagData.saveTags(fileMeta);
                    }
                    if (refresh != null) {
                        refresh.run();
                    }
                    TempHolder.listHash++;
                }
            });
        }

        if (isReadBookOption) {
            builder.setNeutralButton(R.string.read_a_book, new AlertDialog.OnClickListener() {
                String res = "";

                @Override
                public void onClick(DialogInterface dialog, int which) {
                    res = "";
                    for (int i : checked) {
                        StringDB.add(res, tags.get(i), (db) -> res = db);
                    }
                    LOG.d("showTagsDialog", res);
                    if (fileMeta != null) {
                        fileMeta.setTag(res);
                        AppDB.get().update(fileMeta);
                        TagData.saveTags(fileMeta);
                    }

                    ExtUtils.openFile(a, new FileMeta(file.getPath()));
                }
            });
        }

        AlertDialog create = builder.create();
        create.setOnDismissListener(dialog -> {
            if (refresh != null) {
                refresh.run();
            }
            TempHolder.listHash++;
            Keyboards.close(a);
            Keyboards.hideNavigation(a);
        });
        create.show();
    }

    private static List<String> getAllTags(final String tag) {
        Collection<String> res = new LinkedHashSet<>();

        res.addAll(StringDB.asList(AppState.get().bookTags));

        res.addAll(StringDB.asList(tag));
        res.addAll(AppDB.get().getAll(SEARCH_IN.TAGS));

        Iterator<String> iterator = res.iterator();
        while (iterator.hasNext()) {
            if (TxtUtils.isEmpty(iterator.next().trim())) {
                iterator.remove();
            }
        }
        final List<String> tags = new ArrayList<>(res);
        Collections.sort(tags);
        return tags;
    }
}
