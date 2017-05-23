package com.foobnix.pdf.info.widget;

import java.util.List;

import com.foobnix.android.utils.BaseItemLayoutAdapter;
import com.foobnix.android.utils.LOG;
import com.foobnix.pdf.info.R;
import com.foobnix.pdf.info.model.BookCSS;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Typeface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;

public class FontDialog {

    public static void initSpinner(final Spinner spinner, BaseItemLayoutAdapter adaper, int selected, int plusRes, int minusRes, int sampleRes) {
        spinner.setAdapter(adaper);
        LinearLayout parent = (LinearLayout) spinner.getParent();

        final TextView textViewPreview = (TextView) parent.findViewById(sampleRes);

        if (plusRes != -1) {
            parent.findViewById(plusRes).setOnClickListener(new OnClickListener() {

                @Override
                public void onClick(View v) {
                    int i = spinner.getSelectedItemPosition() - 1;
                    if (i <= 0) {
                        i = 0;
                    }
                    spinner.setSelection(i);
                }
            });
        }

        if (minusRes != -1) {
            parent.findViewById(minusRes).setOnClickListener(new OnClickListener() {

                @Override
                public void onClick(View v) {
                    int item = spinner.getSelectedItemPosition() + 1;
                    if (item >= spinner.getCount()) {
                        item = spinner.getCount() - 1;
                    }
                    spinner.setSelection(item);
                }
            });
        }
        spinner.setOnItemSelectedListener(new OnItemSelectedListener() {

            @Override
            public void onItemSelected(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
                TextView textView = (TextView) spinner.getChildAt(0);
                try {
                    String value = (String) spinner.getSelectedItem();
                    textView.setTypeface(BookCSS.getTypeFaceForFont(value));
                } catch (Exception e) {
                    textView.setTypeface(Typeface.DEFAULT);
                    LOG.e(e);
                }

                // textView.setTextAppearance(spinner.getContext(),
                // R.style.textLinkStyle);
            }

            @Override
            public void onNothingSelected(AdapterView<?> arg0) {

            }
        });
        spinner.setSelection(selected, false);

    }

    public static void show(final Context c, final Runnable apply) {
        final View inflate = LayoutInflater.from(c).inflate(R.layout.dialog_fonts, null, false);

        final List<String> allFonts = BookCSS.get().getAllFonts();

        final BaseItemLayoutAdapter adapter = new BaseItemLayoutAdapter<String>(c, android.R.layout.simple_spinner_dropdown_item, allFonts) {

            @Override
            public void populateView(View inflate, int arg1, String value) {
                TextView tv = (TextView) inflate.findViewById(android.R.id.text1);
                tv.setText("" + value);
                // tv.setTypeface(BookCSS.getTypeFaceForFont(value));
            }

            @Override
            public String getItem(int position) {
                return allFonts.get(position);
            }
        };

        final Spinner normalFontSpinner = (Spinner) inflate.findViewById(R.id.normalFontSpinner);
        final Spinner boldFontSpinner = (Spinner) inflate.findViewById(R.id.boldFontSpinner);
        final Spinner italicFontSpinner = (Spinner) inflate.findViewById(R.id.italicFontSpinner);
        final Spinner boldItalicFontSpinner = (Spinner) inflate.findViewById(R.id.boldItalicFontSpinner);
        final Spinner headersFontSpinner = (Spinner) inflate.findViewById(R.id.headersFontSpinner);

        initSpinner(normalFontSpinner, adapter, BookCSS.get().position(BookCSS.get().normalFont), R.id.normalFontUp, R.id.normalFontDown, R.id.textFontNormal);
        initSpinner(boldFontSpinner, adapter, BookCSS.get().position(BookCSS.get().boldFont), R.id.boldFontUp, R.id.boldFontDown, R.id.textFontBold);
        initSpinner(italicFontSpinner, adapter, BookCSS.get().position(BookCSS.get().italicFont), R.id.italicFontUp, R.id.italicFontDown, R.id.textFontItalic);
        initSpinner(boldItalicFontSpinner, adapter, BookCSS.get().position(BookCSS.get().boldItalicFont), R.id.boldItalicFontUp, R.id.boldItalicFontDown, R.id.textFontBoldItalic);
        initSpinner(headersFontSpinner, adapter, BookCSS.get().position(BookCSS.get().headersFont), R.id.headersFontUp, R.id.headersFontDown, R.id.textFontHeaders);

        final AlertDialog.Builder builder = new AlertDialog.Builder(c);
        builder.setTitle(R.string.customize_fonts);
        builder.setView(inflate);

        builder.setPositiveButton(R.string.apply, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                BookCSS.get().normalFont = (String) normalFontSpinner.getSelectedItem();
                BookCSS.get().boldFont = (String) boldFontSpinner.getSelectedItem();
                BookCSS.get().italicFont = (String) italicFontSpinner.getSelectedItem();
                BookCSS.get().boldItalicFont = (String) boldItalicFontSpinner.getSelectedItem();
                BookCSS.get().headersFont = (String) headersFontSpinner.getSelectedItem();

                apply.run();
            }
        });
        builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });
        builder.show();


    }

}
