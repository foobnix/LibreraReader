package com.foobnix.pdf.info.view;

import com.foobnix.android.utils.IntegerResponse;
import com.foobnix.android.utils.TxtUtils;
import com.foobnix.pdf.info.R;
import com.foobnix.pdf.info.wrapper.AppState;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.view.View;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.LinearLayout;
import android.widget.TextView;

public class Dialogs {

    public static void showContrastDialogByUrl(final Context c, final Runnable action) {
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

    public static LinearLayout getBCView(final Context c, final Runnable action) {
        LinearLayout l = new LinearLayout(c);
        l.setOrientation(LinearLayout.VERTICAL);


        final CustomSeek contrastSeek = new CustomSeek(c);
        contrastSeek.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT, 0));
        contrastSeek.initWith("", "");
        contrastSeek.init(0, 200, AppState.get().contrastImage);
        contrastSeek.setOnSeekChanged(new IntegerResponse() {

            @Override
            public boolean onResultRecive(int result) {
                AppState.get().contrastImage = result;
                action.run();
                return false;
            }
        });

        final CustomSeek brightnesSeek = new CustomSeek(c);
        brightnesSeek.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT, 0));
        brightnesSeek.initWith("", "");
        brightnesSeek.init(-100, 100, AppState.get().brigtnessImage);
        brightnesSeek.setOnSeekChanged(new IntegerResponse() {

            @Override
            public boolean onResultRecive(int result) {
                AppState.get().brigtnessImage = result;
                action.run();
                return false;
            }
        });

        final CheckBox bolderText = new CheckBox(c);
        bolderText.setText(R.string.make_text_bold);
        bolderText.setChecked(AppState.getInstance().bolderTextOnImage);
        bolderText.setOnCheckedChangeListener(new OnCheckedChangeListener() {

            @Override
            public void onCheckedChanged(final CompoundButton buttonView, final boolean isChecked) {
                if (contrastSeek.getCurrentValue() == 0) {
                    // contrastSeek.reset(25);
                }
                AppState.getInstance().bolderTextOnImage = isChecked;
                action.run();
            }
        });

        TextView contrastText = new TextView(c);
        contrastText.setText(R.string.contrast);

        TextView brightnessText = new TextView(c);
        brightnessText.setText(R.string.brightness);

        l.addView(contrastText);
        l.addView(contrastSeek);
        l.addView(brightnessText);
        l.addView(brightnesSeek);
        l.addView(bolderText);

        TextView defaults = new TextView(c);
        defaults.setTextAppearance(c, R.style.textLinkStyle);
        defaults.setText(R.string.restore_defaults);
        TxtUtils.underlineTextView(defaults);
        defaults.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                AppState.get().brigtnessImage = 0;
                AppState.get().contrastImage = 0;
                AppState.getInstance().bolderTextOnImage = false;
                bolderText.setChecked(AppState.getInstance().bolderTextOnImage);

                brightnesSeek.reset(AppState.get().brigtnessImage);
                contrastSeek.reset(AppState.get().contrastImage);

                action.run();

            }
        });

        l.addView(defaults);
        return l;
    }

}
