package com.foobnix.pdf.info;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnDismissListener;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.MenuItem.OnMenuItemClickListener;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.foobnix.android.utils.IntegerResponse;
import com.foobnix.android.utils.LOG;
import com.foobnix.android.utils.TxtUtils;
import com.foobnix.model.AppState;
import com.foobnix.pdf.info.model.BookCSS;
import com.foobnix.pdf.info.view.CustomSeek;
import com.foobnix.pdf.info.view.MyPopupMenu;
import com.foobnix.pdf.info.wrapper.DocumentController;
import com.foobnix.sys.TempHolder;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class DialogSpeedRead {

    private volatile static int currentWord = 0;
    volatile static String[] words = new String[]{""};
    static String[] punctuations = {".", ";", ":", "?", "!"};

    public static void show(final Context a, final DocumentController dc) {
        final AlertDialog.Builder builder = new AlertDialog.Builder(a);

        View layout = LayoutInflater.from(a).inflate(R.layout.dialog_speed_reading, null, false);

        final TextView textWord = (TextView) layout.findViewById(R.id.textWord);
        final TextView onReset = (TextView) layout.findViewById(R.id.onReset);

        final ImageView onNext = (ImageView) layout.findViewById(R.id.onNext);
        final ImageView onPrev = (ImageView) layout.findViewById(R.id.onPrev);

        onNext.setVisibility(View.VISIBLE);
        onPrev.setVisibility(View.VISIBLE);

        TintUtil.setTintImageWithAlpha(onNext, TintUtil.color, 100);
        TintUtil.setTintImageWithAlpha(onPrev, TintUtil.color, 100);

        final CustomSeek seekBarSpeed = (CustomSeek) layout.findViewById(R.id.fastReadSpeed);
        seekBarSpeed.init(10, 900, AppState.get().fastReadSpeed);
        seekBarSpeed.setStep(10);
        seekBarSpeed.setOnSeekChanged(new IntegerResponse() {

            @Override
            public boolean onResultRecive(int result) {
                AppState.get().fastReadSpeed = result;
                return false;
            }
        });

        final CustomSeek fastManyWords = (CustomSeek) layout.findViewById(R.id.fastManyWords);
        fastManyWords.init(0, 30, AppState.get().fastManyWords);
        fastManyWords.setOnSeekChanged(new IntegerResponse() {

            @Override
            public boolean onResultRecive(int result) {
                AppState.get().fastManyWords = result;
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

        textWord.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                MyPopupMenu menu = new MyPopupMenu(textWord);
                menu.getMenu().add(R.string.share).setIcon(R.drawable.glyphicons_basic_578_share).setOnMenuItemClickListener((it) -> {
                    final Intent intent = new Intent(Intent.ACTION_SEND);
                    intent.setType("text/plain");
                    intent.putExtra(Intent.EXTRA_TEXT, textWord.getText().toString().trim());
                    a.startActivity(Intent.createChooser(intent, a.getString(R.string.share)));
                    return true;
                });
                menu.getMenu().add(R.string.copy).setIcon(R.drawable.glyphicons_basic_614_copy).setOnMenuItemClickListener((it) -> {

                    if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.HONEYCOMB) {
                        android.text.ClipboardManager clipboard = (android.text.ClipboardManager) a.getSystemService(Context.CLIPBOARD_SERVICE);
                        clipboard.setText(textWord.getText().toString().trim());
                    } else {
                        android.content.ClipboardManager clipboard = (android.content.ClipboardManager) a.getSystemService(Context.CLIPBOARD_SERVICE);
                        android.content.ClipData clip = android.content.ClipData.newPlainText("Copied Text", textWord.getText().toString().trim());
                        clipboard.setPrimaryClip(clip);
                    }
                    Toast.makeText(a, R.string.copy_text, Toast.LENGTH_SHORT).show();
                    return true;
                });
                menu.show();

                return true;
            }
        });

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

        currentWord = 0;

        final Runnable task = new Runnable() {

            @Override
            public void run() {
                final int page = dc.getCurentPageFirst1();
                final int pageCount = dc.getPageCount() + 1;
                int counter = 0;

                for (int currentPage = page; currentPage < pageCount; currentPage++) {

                    if (!TempHolder.isActiveSpeedRead.get()) {
                        return;
                    }

                    final int currentPage1 = currentPage;

                    dc.getActivity().runOnUiThread(new Runnable() {

                        @Override
                        public void run() {
                            if (TempHolder.isActiveSpeedRead.get()) {
                                dc.onGoToPage(currentPage1);
                            }
                        }
                    });

                    String textForPage = dc.getTextForPage(currentPage - 1);
                    LOG.d("textForPage",textForPage);
                    if (TxtUtils.isEmpty(textForPage)) {
                        counter++;
                    }
                    if (counter > 3) {
                        LOG.d("3 Empty Page");
                        break;
                    }

                    List<String> tempList = Arrays.asList(textForPage.split("\\s"));
                    List<String> res = new ArrayList<String>();
                    for (String item : tempList) {
                        //currentWord = 0;
                        if (item.contains("-") && item.length() >= 10) {
                            String[] it = item.split("-");
                            if (it.length >= 2) {
                                res.add(it[0] + "-");
                                res.add(it[1]);
                            }
                        } else {
                            res.add(item);
                        }

                    }
                    words = res.toArray(new String[res.size()]);
                    //currentWord = 0;

                    LOG.d("currentWord", currentWord);
                    for (int i = currentWord; i < words.length; i++) {
                        if (!TempHolder.isActiveSpeedRead.get()) {
                            return;
                        }

                        String word = words[i];

                        if (AppState.get().fastManyWords != 0) {

                            while (i + 1 < words.length) {

                                String lastWord = TxtUtils.lastWord(word);
                                boolean isLastWorld = lastWord.endsWith(".") && lastWord.length() > 3;
                                LOG.d("isLastWorld", word, lastWord, isLastWorld);
                                if (isLastWorld) {
                                    break;
                                }

                                String temp = word + " " + words[i + 1];

                                if (word.length() >= 3 && temp.replace(" ", "").length() > AppState.get().fastManyWords) {
                                    break;
                                }

                                word = temp;
                                i++;

                            }
                        }

                        final String wordFinal = word.trim();
                        currentWord = i;
                        dc.getActivity().runOnUiThread(new Runnable() {

                            @Override
                            public void run() {
                                textWord.setText(wordFinal);
                                LOG.d("RSPV-show text", wordFinal);
                            }
                        });

                        int k = 0;
                        for (String c : punctuations) {
                            if (wordFinal.endsWith(c)) {
                                k = 100;
                                LOG.d("RSPV-punctuation");
                                break;
                            }
                        }

                        float wps = (float) AppState.get().fastReadSpeed / 60;
                        try {
                            int delta = (int) (1000 / wps) + k;
                            final int wLen = wordFinal.length();
                            delta = delta + wLen * wLen / 2;
                            LOG.d("RSPV-Sleep", wordFinal, delta);
                            Thread.sleep(delta);
                        } catch (Exception e) {
                            LOG.e(e);
                        }
                    }
                    currentWord = 0;
                }
            }
        };

        textWord.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                onReset.setVisibility(View.VISIBLE);
                TempHolder.isActiveSpeedRead.set(!TempHolder.isActiveSpeedRead.get());
                if (TempHolder.isActiveSpeedRead.get()) {
                    new Thread(task,"@T isActiveSpeedRead").start();

                    onNext.setVisibility(View.GONE);
                    onPrev.setVisibility(View.GONE);
                } else {
                    onNext.setVisibility(View.VISIBLE);
                    onPrev.setVisibility(View.VISIBLE);
                }

            }
        });

        onPrev.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                if (currentWord > 0) {
                    currentWord--;
                    textWord.setText(words[currentWord]);
                }

            }
        });

        onNext.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                if (currentWord < words.length - 1) {
                    currentWord++;
                    textWord.setText(words[currentWord]);
                }

            }
        });

        onReset.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                textWord.setText(R.string.start);
                TxtUtils.underlineTextView(textWord);
                TempHolder.isActiveSpeedRead.set(false);
                currentWord = 0;
            }
        });
        onReset.setVisibility(View.INVISIBLE);

        builder.setView(layout);

        builder.setNegativeButton(R.string.close, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int id) {
                TempHolder.isActiveSpeedRead.set(false);
                reset();
                dialog.dismiss();
            }
        });

        AlertDialog create = builder.create();

        create.setOnDismissListener(new OnDismissListener() {

            @Override
            public void onDismiss(DialogInterface dialog) {
                TempHolder.isActiveSpeedRead.set(false);
                reset();
            }

        });
        reset();
        create.show();
    }

    private static void reset() {
        currentWord = 0;
        words = new String[]{""};
        LOG.d("currentWord reset", 0);
    }

}
