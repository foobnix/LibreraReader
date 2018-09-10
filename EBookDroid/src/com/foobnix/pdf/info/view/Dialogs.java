package com.foobnix.pdf.info.view;

import java.io.File;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import com.buzzingandroid.ui.HSVColorPickerDialog;
import com.buzzingandroid.ui.HSVColorPickerDialog.OnColorSelectedListener;
import com.foobnix.android.utils.BaseItemLayoutAdapter;
import com.foobnix.android.utils.Dips;
import com.foobnix.android.utils.IntegerResponse;
import com.foobnix.android.utils.Keyboards;
import com.foobnix.android.utils.LOG;
import com.foobnix.android.utils.StringDB;
import com.foobnix.android.utils.TxtUtils;
import com.foobnix.android.utils.Vibro;
import com.foobnix.dao2.FileMeta;
import com.foobnix.pdf.info.R;
import com.foobnix.pdf.info.TintUtil;
import com.foobnix.pdf.info.wrapper.AppState;
import com.foobnix.pdf.info.wrapper.DocumentController;
import com.foobnix.sys.TempHolder;
import com.foobnix.ui2.AppDB;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnDismissListener;
import android.graphics.Color;
import android.os.Handler;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.view.WindowManager;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

public class Dialogs {

    public static void customValueDialog(final Context a, final int initValue, final IntegerResponse reponse) {
        final AlertDialog.Builder builder = new AlertDialog.Builder(a);
        builder.setTitle(R.string.custom_value);

        final CustomSeek myValue = new CustomSeek(a);
        myValue.init(1, 100, initValue);
        myValue.setOnSeekChanged(new IntegerResponse() {

            @Override
            public boolean onResultRecive(int result) {
                return false;
            }
        });
        myValue.setValueText(initValue + "%");

        builder.setView(myValue);

        builder.setPositiveButton(R.string.apply, new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {
                reponse.onResultRecive(myValue.getCurrentValue());
            }
        });
        builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {

            }
        });

        builder.show();
    }

    public static void showLinksColorDialog(final Activity a, final Runnable action) {
        final AlertDialog.Builder builder = new AlertDialog.Builder(a);
        builder.setCancelable(true);
        builder.setTitle(R.string.link_color);

        LayoutInflater inflater = LayoutInflater.from(a);
        View inflate = inflater.inflate(R.layout.dialog_links_color, null, false);

        final CheckBox isUiTextColor = (CheckBox) inflate.findViewById(R.id.isUiTextColor);
        isUiTextColor.setChecked(AppState.get().isUiTextColor);
        isUiTextColor.setOnCheckedChangeListener(new OnCheckedChangeListener() {

            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                AppState.get().isUiTextColor = isChecked;
                if (action != null) {
                    action.run();
                }
            }
        });

        LinearLayout colorsLine1 = inflate.findViewById(R.id.colorsLine1);
        colorsLine1.removeAllViews();

        for (String color : AppState.STYLE_COLORS) {
            View view = inflater.inflate(R.layout.item_color, (ViewGroup) inflate, false);
            view.setBackgroundColor(Color.TRANSPARENT);
            final int intColor = Color.parseColor(color);
            final View img = view.findViewById(R.id.itColor);
            img.setBackgroundColor(intColor);

            colorsLine1.addView(view, new LayoutParams(Dips.dpToPx(30), Dips.dpToPx(30)));

            view.setOnClickListener(new OnClickListener() {

                @Override
                public void onClick(View v) {
                    isUiTextColor.setChecked(true);
                    AppState.get().isUiTextColor = true;
                    AppState.get().uiTextColor = intColor;
                    if (action != null) {
                        action.run();
                    }
                }
            });
        }

        View view = inflater.inflate(R.layout.item_color, (ViewGroup) inflate, false);
        view.setBackgroundColor(Color.TRANSPARENT);
        final ImageView img = (ImageView) view.findViewById(R.id.itColor);
        img.setColorFilter(a.getResources().getColor(R.color.tint_gray));
        img.setImageResource(R.drawable.glyphicons_433_plus);
        img.setBackgroundColor(AppState.get().uiTextColorUser);
        colorsLine1.addView(view, new LayoutParams(Dips.dpToPx(30), Dips.dpToPx(30)));

        view.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                new HSVColorPickerDialog(a, AppState.get().uiTextColorUser, new OnColorSelectedListener() {

                    @Override
                    public void colorSelected(Integer color) {
                        isUiTextColor.setChecked(true);
                        AppState.get().isUiTextColor = true;
                        AppState.get().uiTextColor = color;
                        AppState.get().uiTextColorUser = color;
                        img.setBackgroundColor(color);

                        if (action != null) {
                            action.run();
                        }

                    }
                }).show();

            }
        });

        builder.setView(inflate);

        builder.setPositiveButton(R.string.close, new DialogInterface.OnClickListener() {

            @Override
            public void onClick(final DialogInterface dialog, final int id) {

            }
        });
        AlertDialog dialog = builder.show();
        dialog.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);

    }

    public static AlertDialog loadingBook(Context c, final Runnable onCancel) {
        try {
            final AlertDialog.Builder builder = new AlertDialog.Builder(c);
            View view = LayoutInflater.from(c).inflate(R.layout.dialog_loading_book, null, false);
            final TextView text = (TextView) view.findViewById(R.id.text1);

            ProgressBar pr = (ProgressBar) view.findViewById(R.id.progressBarLoading);
            TintUtil.setDrawableTint(pr.getIndeterminateDrawable().getCurrent(), AppState.get().isDayNotInvert ? TintUtil.color : Color.WHITE);

            ImageView image = (ImageView) view.findViewById(R.id.onCancel);
            TintUtil.setTintImageNoAlpha(image, AppState.get().isDayNotInvert ? TintUtil.color : Color.WHITE);
            image.setOnClickListener(new OnClickListener() {

                @Override
                public void onClick(View v) {
                    LOG.d("loadingBook Cancel");
                    onCancel.run();
                }
            });

            builder.setView(view);
            builder.setCancelable(false);

            AlertDialog dialog = builder.show();
            dialog.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
            return dialog;
        } catch (Exception e) {
            return null;
        }

    }

    public static void showDeltaPage(final FrameLayout anchor, final DocumentController controller, final int pageNumber, final Runnable reloadUI) {
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
        builder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {

            @Override
            public void onClick(final DialogInterface dialog, final int id) {
                try {
                    TempHolder.get().pageDelta = Integer.parseInt(edit.getText().toString()) - controller.getCurentPageFirst1();
                } catch (Exception e) {
                    TempHolder.get().pageDelta = 0;
                }
                reloadUI.run();
            }
        });
        builder.setNeutralButton(R.string.restore_defaults_short, new DialogInterface.OnClickListener() {

            @Override
            public void onClick(final DialogInterface dialog, final int id) {

                AlertDialogs.showOkDialog(controller.getActivity(), controller.getString(R.string.restore_defaults_full), new Runnable() {

                    @Override
                    public void run() {
                        TempHolder.get().pageDelta = 0;
                        reloadUI.run();
                    }
                });

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
                Keyboards.hideNavigation(controller.getActivity());
            }
        });
        create.show();
    }

    public static void showContrastDialogByUrl(final Activity c, final Runnable action) {
        final AlertDialog.Builder builder = new AlertDialog.Builder(c);
        builder.setCancelable(true);
        builder.setTitle(R.string.contrast_and_brightness);

        LinearLayout l = getBCView(c, action);
        builder.setView(l);

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

    public static LinearLayout getBCView(final Activity c, final Runnable action) {
        LinearLayout l = new LinearLayout(c);
        l.setPadding(Dips.dpToPx(5), Dips.dpToPx(5), Dips.dpToPx(5), Dips.dpToPx(5));
        l.setOrientation(LinearLayout.VERTICAL);

        final Handler handler = new Handler();

        final Runnable actionWrapper = new Runnable() {

            @Override
            public void run() {
                handler.removeCallbacks(action);
                handler.postDelayed(action, 100);
            }
        };

        final CheckBox isEnableBC = new CheckBox(c);
        isEnableBC.setText(R.string.enable_contrast_and_brightness);
        isEnableBC.setChecked(AppState.get().isEnableBC);
        isEnableBC.setOnCheckedChangeListener(new OnCheckedChangeListener() {

            @Override
            public void onCheckedChanged(final CompoundButton buttonView, final boolean isChecked) {
                AppState.get().isEnableBC = isChecked;
                actionWrapper.run();
            }
        });

        final CustomSeek contrastSeek = new CustomSeek(c);
        contrastSeek.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT, 0));
        contrastSeek.init(0, 200, AppState.get().contrastImage);
        contrastSeek.setOnSeekChanged(new IntegerResponse() {

            @Override
            public boolean onResultRecive(int result) {
                AppState.get().contrastImage = result;
                isEnableBC.setChecked(true);
                actionWrapper.run();
                return false;
            }
        });

        final CustomSeek brightnesSeek = new CustomSeek(c);
        brightnesSeek.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT, 0));
        brightnesSeek.init(-100, 100, AppState.get().brigtnessImage);
        brightnesSeek.setOnSeekChanged(new IntegerResponse() {

            @Override
            public boolean onResultRecive(int result) {
                AppState.get().brigtnessImage = result;
                isEnableBC.setChecked(true);
                actionWrapper.run();
                return false;
            }
        });

        final CheckBox bolderText = new CheckBox(c);
        bolderText.setText(R.string.make_text_bold);
        bolderText.setChecked(AppState.get().bolderTextOnImage);
        bolderText.setOnCheckedChangeListener(new OnCheckedChangeListener() {

            @Override
            public void onCheckedChanged(final CompoundButton buttonView, final boolean isChecked) {
                AppState.get().bolderTextOnImage = isChecked;
                if (isChecked) {
                    isEnableBC.setChecked(true);
                }
                actionWrapper.run();
            }
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
        defaults.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {

                AlertDialogs.showOkDialog(c, c.getString(R.string.restore_defaults_full), new Runnable() {

                    @Override
                    public void run() {
                        AppState.get().brigtnessImage = 0;
                        AppState.get().contrastImage = 0;
                        AppState.get().bolderTextOnImage = false;
                        isEnableBC.setChecked(AppState.get().bolderTextOnImage);

                        brightnesSeek.reset(AppState.get().brigtnessImage);
                        contrastSeek.reset(AppState.get().contrastImage);

                        actionWrapper.run();
                    }
                });

            }
        });

        l.addView(defaults);
        return l;
    }

    public static void addTagsDialog(final Context a, final Runnable onRefresh) {
        final AlertDialog.Builder builder = new AlertDialog.Builder(a);
        builder.setTitle(R.string.tag);

        final EditText edit = new EditText(a);
        edit.setHint("#");

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
        create.setOnDismissListener(new OnDismissListener() {

            @Override
            public void onDismiss(DialogInterface dialog) {
            }
        });
        create.show();

        create.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
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


                AppState.get().bookTags = StringDB.add(AppState.get().bookTags, text);
                if (onRefresh != null) {
                    onRefresh.run();
                }
                create.dismiss();
                AppState.get().save(a);

            }
        });
    }

    public static void showTagsDialog(final Context a, File file, final Runnable refresh) {
        final FileMeta fileMeta = AppDB.get().getOrCreate(file.getPath());

        LOG.d("showTagsDialog book tags", fileMeta.getTag());

        final AlertDialog.Builder builder = new AlertDialog.Builder(a);
        // builder.setTitle(R.string.tag);

        View inflate = LayoutInflater.from(a).inflate(R.layout.dialog_tags, null, false);

        final ListView list = (ListView) inflate.findViewById(R.id.listView1);
        final TextView add = (TextView) inflate.findViewById(R.id.addTag);
        TxtUtils.underline(add, "+ " + a.getString(R.string.add_tag));

        final List<String> tags = StringDB.asList(AppState.get().bookTags);
        List<String> fileTags = StringDB.asList(fileMeta.getTag());
        for (String fileTag : fileTags) {
            if (!StringDB.contains(AppState.get().bookTags, fileTag)) {
                tags.add(fileTag);
            }
        }
        Collections.sort(tags);

        Iterator<String> iterator = tags.iterator();
        while (iterator.hasNext()) {
            if (TxtUtils.isEmpty(iterator.next().trim())) {
                iterator.remove();
            }
        }

        final Set<Integer> checked = new HashSet<>();

        final BaseItemLayoutAdapter<String> adapter = new BaseItemLayoutAdapter<String>(a, R.layout.tag_item, tags) {
            @Override
            public void populateView(View layout, final int position, final String tagName) {
                CheckBox text = (CheckBox) layout.findViewById(R.id.tagName);
                text.setText(tagName);

                text.setOnCheckedChangeListener(new OnCheckedChangeListener() {

                    @Override
                    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                        if (isChecked) {
                            checked.add(position);
                        } else {
                            checked.remove(position);
                        }
                    }
                });

                text.setChecked(StringDB.contains(fileMeta.getTag(), tagName));

                layout.findViewById(R.id.deleteTag).setOnClickListener(new OnClickListener() {

                    @Override
                    public void onClick(View v) {
                        AlertDialogs.showOkDialog((Activity) a, a.getString(R.string.do_you_want_to_delete_this_tag_from_all_books_), new Runnable() {

                            @Override
                            public void run() {
                                checked.clear();

                                AppState.get().bookTags = StringDB.delete(AppState.get().bookTags, tagName);
                                tags.clear();
                                tags.addAll(StringDB.asList(AppState.get().bookTags));
                                notifyDataSetChanged();

                                List<FileMeta> allWithTag = AppDB.get().getAllWithTag(tagName);
                                for (FileMeta meta : allWithTag) {
                                    meta.setTag(StringDB.delete(meta.getTag(), tagName));
                                }
                                AppDB.get().updateAll(allWithTag);
                                if (refresh != null) {
                                    refresh.run();
                                }

                            }
                        });

                    }
                });

            }
        };

        add.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                addTagsDialog(a, new Runnable() {

                    @Override
                    public void run() {
                        tags.clear();
                        tags.addAll(StringDB.asList(AppState.get().bookTags));
                        adapter.notifyDataSetChanged();
                    }
                });
            }
        });

        list.setAdapter(adapter);

        builder.setView(inflate);

        builder.setNegativeButton(R.string.close, new AlertDialog.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {

            }
        });

        builder.setPositiveButton(R.string.apply, new AlertDialog.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {
                String res = "";
                for (int i : checked) {
                    res = StringDB.add(res, tags.get(i));
                }
                LOG.d("showTagsDialog", res);
                fileMeta.setTag(res);
                AppDB.get().update(fileMeta);
                if (refresh != null) {
                    refresh.run();
                }
                TempHolder.listHash++;
            }

        });

        AlertDialog create = builder.create();
        create.setOnDismissListener(new OnDismissListener() {

            @Override
            public void onDismiss(DialogInterface dialog) {
                Keyboards.close((Activity) a);
                Keyboards.hideNavigation((Activity) a);

            }
        });
        create.show();

    }

}
