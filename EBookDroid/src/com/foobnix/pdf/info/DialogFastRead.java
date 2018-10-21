package com.foobnix.pdf.info;

import java.util.Arrays;
import java.util.List;

import com.foobnix.android.utils.IntegerResponse;
import com.foobnix.android.utils.TxtUtils;
import com.foobnix.pdf.info.model.BookCSS;
import com.foobnix.pdf.info.view.CustomSeek;
import com.foobnix.pdf.info.view.MyPopupMenu;
import com.foobnix.pdf.info.wrapper.AppState;
import com.foobnix.pdf.info.wrapper.DocumentController;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnDismissListener;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.MenuItem.OnMenuItemClickListener;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.TextView;

public class DialogFastRead {

    volatile static boolean isActive = false;

    volatile static int currentWord = 0;

    public static void show(final Context a, final DocumentController dc) {
        final AlertDialog.Builder builder = new AlertDialog.Builder(a);

        View layout = LayoutInflater.from(a).inflate(R.layout.dialog_fast_reading, null, false);

        final TextView textWord = (TextView) layout.findViewById(R.id.textWord);
        final TextView onReset = (TextView) layout.findViewById(R.id.onReset);

        final CustomSeek seekBarSpeed = (CustomSeek) layout.findViewById(R.id.fastReadSpeed);
        seekBarSpeed.init(0, 900, AppState.get().fastReadSpeed);
        seekBarSpeed.setStep(10);
        seekBarSpeed.setOnSeekChanged(new IntegerResponse() {

            @Override
            public boolean onResultRecive(int result) {
                AppState.get().fastReadSpeed = result;
                return false;
            }
        });

        final CustomSeek fastReadFontSize = (CustomSeek) layout.findViewById(R.id.fastReadFontSize);
        fastReadFontSize.init(10, 100, AppState.get().fastReadFontSize);
        fastReadFontSize.setOnSeekChanged(new IntegerResponse() {

            @Override
            public boolean onResultRecive(int result) {
                AppState.get().fastReadFontSize = result;
                textWord.setTextSize(AppState.get().fastReadFontSize);
                return false;
            }
        });
        textWord.setTextSize(AppState.get().fastReadFontSize);
        textWord.setTypeface(BookCSS.getTypeFaceForFont(BookCSS.get().normalFont));
        TxtUtils.underlineTextView(textWord);

        final MyPopupMenu menu = new MyPopupMenu(seekBarSpeed.getContext(), seekBarSpeed);
        List<Integer> values = Arrays.asList(100, 150, 200, 250, 300, 350, 400, 450, 500, 550, 600, 700);
        for (final int i : values) {
            menu.getMenu().add(String.format("%s wpm", i)).setOnMenuItemClickListener(new OnMenuItemClickListener() {

                @Override
                public boolean onMenuItemClick(MenuItem item) {
                    seekBarSpeed.reset(i);
                    seekBarSpeed.sendProgressChanged();
                    return false;
                }
            });
        }

        seekBarSpeed.addMyPopupMenu(menu);
        TxtUtils.setLinkTextColor(seekBarSpeed.getTitleText());

        final Runnable task = new Runnable() {

            @Override
            public void run() {
                final int page = dc.getCurentPageFirst1();
                final int pageCount = dc.getPageCount() + 1;

                for (int currentPage = page; currentPage < pageCount; currentPage++) {
                    final int currentPage1 = currentPage;
                    String textForPage = dc.getTextForPage(currentPage - 1);
                    final String[] words = textForPage.split(" ");

                    dc.getActivity().runOnUiThread(new Runnable() {

                        @Override
                        public void run() {
                            dc.onGoToPage(currentPage1);
                        }
                    });

                    for (int i = currentWord; i < words.length; i++) {
                        final String word = words[i];
                        if (!isActive) {
                            currentWord = i;
                            return;
                        }

                        dc.getActivity().runOnUiThread(new Runnable() {

                            @Override
                            public void run() {
                                textWord.setText(word);
                            }
                        });
                        try {
                            float wps = (float) AppState.get().fastReadSpeed / 60;
                            Thread.sleep((int) (1000 / wps));
                        } catch (Exception e) {
                        }
                    }
                }
            }
        };

        textWord.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                isActive = !isActive;
                if (isActive) {
                    new Thread(task).start();
                }

            }
        });

        onReset.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                textWord.setText(R.string.start);
                TxtUtils.underlineTextView(textWord);
                isActive = false;
                currentWord = 0;
            }
        });

        builder.setView(layout);

        builder.setNegativeButton(R.string.close, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int id) {
                isActive = false;
                currentWord = 0;
                dialog.dismiss();
            }
        });

        AlertDialog create = builder.create();
        create.setOnDismissListener(new OnDismissListener() {

            @Override
            public void onDismiss(DialogInterface dialog) {
                isActive = false;
                currentWord = 0;
            }
        });

        create.show();
    }

}
