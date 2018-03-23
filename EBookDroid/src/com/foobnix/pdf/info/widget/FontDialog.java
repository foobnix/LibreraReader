package com.foobnix.pdf.info.widget;

import java.util.List;

import com.foobnix.android.utils.LOG;
import com.foobnix.pdf.info.ExtUtils;
import com.foobnix.pdf.info.R;
import com.foobnix.pdf.info.model.BookCSS;
import com.foobnix.pdf.info.view.MyPopupMenu;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.MenuItem.OnMenuItemClickListener;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.TextView;

public class FontDialog {

    public static void initSpinner(final View inflate, final TextView spinner, String path, int plusRes, int minusRes, int sampleRes) {
        final List<String> allFonts = BookCSS.get().getAllFonts();

        updateTextTag(spinner, path);

        spinner.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                MyPopupMenu menu = new MyPopupMenu(v.getContext(), v);
                for (final String fontPath : allFonts) {
                    final String name = ExtUtils.getFileName(fontPath);
                    menu.getMenu().add(name, fontPath).setOnMenuItemClickListener(new OnMenuItemClickListener() {

                        @Override
                        public boolean onMenuItemClick(MenuItem item) {
                            updateTextTag(spinner, fontPath);
                            return false;
                        }
                    });
                }
                menu.show(getItemPos(spinner));

            }

        });

        inflate.findViewById(minusRes).setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                int index = getItemPos(spinner);
                if (index >= allFonts.size() - 1) {
                    return;
                }
                String nextFont = allFonts.get(index + 1);

                updateTextTag(spinner, nextFont);

            }
        });

        inflate.findViewById(plusRes).setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                int index = getItemPos(spinner);
                if (index < 1) {
                    return;
                }
                String nextFont = allFonts.get(index - 1);

                updateTextTag(spinner, nextFont);

            }
        });

    }

    public static int getItemPos(TextView spinner) {
        return getItemPos((String) spinner.getTag());

    }

    public static int getItemPos(String path) {
        try {
            return BookCSS.get().getAllFonts().indexOf(path);
        } catch (Exception e) {
            LOG.e(e);
            return 0;
        }

    }

    private static void updateTextTag(final TextView spinner, String path) {
        spinner.setTag(path);
        spinner.setText(ExtUtils.getFileName(path));
        spinner.setTypeface(BookCSS.getTypeFaceForFont(path));
    }

    public static void show(final Context c, final Runnable apply) {
        final View inflate = LayoutInflater.from(c).inflate(R.layout.dialog_fonts, null, false);

        final TextView normalFontSpinner = (TextView) inflate.findViewById(R.id.normalFontSpinner);
        final TextView boldFontSpinner = (TextView) inflate.findViewById(R.id.boldFontSpinner);
        final TextView italicFontSpinner = (TextView) inflate.findViewById(R.id.italicFontSpinner);
        final TextView boldItalicFontSpinner = (TextView) inflate.findViewById(R.id.boldItalicFontSpinner);
        final TextView headersFontSpinner = (TextView) inflate.findViewById(R.id.headersFontSpinner);

        initSpinner(inflate, normalFontSpinner, BookCSS.get().normalFont, R.id.normalFontUp, R.id.normalFontDown, R.id.textFontNormal);
        initSpinner(inflate, boldFontSpinner, BookCSS.get().boldFont, R.id.boldFontUp, R.id.boldFontDown, R.id.textFontBold);
        initSpinner(inflate, italicFontSpinner, BookCSS.get().italicFont, R.id.italicFontUp, R.id.italicFontDown, R.id.textFontItalic);
        initSpinner(inflate, boldItalicFontSpinner, BookCSS.get().boldItalicFont, R.id.boldItalicFontUp, R.id.boldItalicFontDown, R.id.textFontBoldItalic);
        initSpinner(inflate, headersFontSpinner, BookCSS.get().headersFont, R.id.headersFontUp, R.id.headersFontDown, R.id.textFontHeaders);

        final AlertDialog.Builder builder = new AlertDialog.Builder(c);
        builder.setTitle(R.string.customize_fonts);
        builder.setView(inflate);

        builder.setPositiveButton(R.string.apply, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                BookCSS.get().normalFont = (String) normalFontSpinner.getTag();
                BookCSS.get().boldFont = (String) boldFontSpinner.getTag();
                BookCSS.get().italicFont = (String) italicFontSpinner.getTag();
                BookCSS.get().boldItalicFont = (String) boldItalicFontSpinner.getTag();
                BookCSS.get().headersFont = (String) headersFontSpinner.getTag();

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
