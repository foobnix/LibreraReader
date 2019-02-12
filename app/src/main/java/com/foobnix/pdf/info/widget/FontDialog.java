package com.foobnix.pdf.info.widget;

import java.util.List;

import org.ebookdroid.BookType;

import com.foobnix.StringResponse;
import com.foobnix.android.utils.Keyboards;
import com.foobnix.android.utils.LOG;
import com.foobnix.android.utils.TxtUtils;
import com.foobnix.pdf.info.ExtUtils;
import com.foobnix.pdf.info.R;
import com.foobnix.pdf.info.model.BookCSS;
import com.foobnix.pdf.info.view.CustomColorView;
import com.foobnix.pdf.info.view.MyPopupMenu;
import com.foobnix.pdf.info.wrapper.AppState;
import com.foobnix.pdf.info.wrapper.DocumentController;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Color;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.MenuItem.OnMenuItemClickListener;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.TextView;
import android.widget.CompoundButton.OnCheckedChangeListener;

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

    public static void show(final Context c, final Runnable apply, final DocumentController controller) {
        final View inflate = LayoutInflater.from(c).inflate(R.layout.dialog_fonts, null, false);

        final TextView normalFontSpinner = (TextView) inflate.findViewById(R.id.normalFontSpinner);
        final TextView boldFontSpinner = (TextView) inflate.findViewById(R.id.boldFontSpinner);
        final TextView italicFontSpinner = (TextView) inflate.findViewById(R.id.italicFontSpinner);
        final TextView boldItalicFontSpinner = (TextView) inflate.findViewById(R.id.boldItalicFontSpinner);
        final TextView headersFontSpinner = (TextView) inflate.findViewById(R.id.headersFontSpinner);
        final TextView capitalFontSpinner = (TextView) inflate.findViewById(R.id.capitalFontSpinner);
        final TextView capitalLetterSize = (TextView) inflate.findViewById(R.id.capitalLetterSize);

        initSpinner(inflate, normalFontSpinner, BookCSS.get().normalFont, R.id.normalFontUp, R.id.normalFontDown, R.id.textFontNormal);
        initSpinner(inflate, boldFontSpinner, BookCSS.get().boldFont, R.id.boldFontUp, R.id.boldFontDown, R.id.textFontBold);
        initSpinner(inflate, italicFontSpinner, BookCSS.get().italicFont, R.id.italicFontUp, R.id.italicFontDown, R.id.textFontItalic);
        initSpinner(inflate, boldItalicFontSpinner, BookCSS.get().boldItalicFont, R.id.boldItalicFontUp, R.id.boldItalicFontDown, R.id.textFontBoldItalic);
        initSpinner(inflate, headersFontSpinner, BookCSS.get().headersFont, R.id.headersFontUp, R.id.headersFontDown, R.id.textFontHeaders);
        initSpinner(inflate, capitalFontSpinner, BookCSS.get().capitalFont, R.id.capitalFontUp, R.id.capitalFontDown, R.id.textFontCapital);

        final TextView textFontCapital = (TextView) inflate.findViewById(R.id.textFontCapital);
        
        String text = textFontCapital.getText().toString();
        textFontCapital.setText(Html.fromHtml("<b>" + text.substring(0, 1) + "</b>" + text.substring(1)));

        boolean isFB2 = BookType.FB2.is(controller.getCurrentBook().getPath()) || BookType.ZIP.is(controller.getCurrentBook().getPath());
        int visibleFB2 = TxtUtils.visibleIf(isFB2);
        inflate.findViewById(R.id.isFB2).setVisibility(visibleFB2);
        
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
                BookCSS.get().capitalFont = (String) capitalFontSpinner.getTag();

                apply.run();
            }
        });

        // capital

        CheckBox isFirstLetter = (CheckBox) inflate.findViewById(R.id.isFirstLetter);
        isFirstLetter.setChecked(BookCSS.get().isCapitalLetter);
        isFirstLetter.setOnCheckedChangeListener(new OnCheckedChangeListener() {

            @Override
            public void onCheckedChanged(final CompoundButton buttonView, final boolean isChecked) {
                BookCSS.get().isCapitalLetter = isChecked;
            }
        });

        capitalLetterSize.setText(String.format("%.1f em", (float) BookCSS.get().capitalLetterSize / 10));
        TxtUtils.underlineTextView(capitalLetterSize);

        capitalLetterSize.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {

                final MyPopupMenu menu = new MyPopupMenu(capitalLetterSize.getContext(), capitalLetterSize);
                for (int i = 10; i <= 30; i += 1) {
                    final int j = i;
                    final String format = String.format("%.1f em", (float) i / 10);
                    menu.getMenu().add(format).setOnMenuItemClickListener(new OnMenuItemClickListener() {

                        @Override
                        public boolean onMenuItemClick(MenuItem item) {
                            capitalLetterSize.setText(format);
                            BookCSS.get().capitalLetterSize = j;
                            return false;
                        }
                    });
                }

                menu.show();

            }
        });

        final CustomColorView capitalLetterColor = (CustomColorView) inflate.findViewById(R.id.capitalLetterColor);
        capitalLetterColor.withDefaultColors(Color.RED, Color.parseColor("#800000"), Color.BLACK);
        capitalLetterColor.init(Color.parseColor(BookCSS.get().capitalLetterColor));
        capitalLetterColor.setOnColorChanged(new StringResponse() {

            @Override
            public boolean onResultRecive(String string) {
                BookCSS.get().capitalLetterColor = string;
                Keyboards.hideNavigation(controller.getActivity());
                return false;
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
