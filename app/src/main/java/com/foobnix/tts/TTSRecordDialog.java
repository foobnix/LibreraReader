package com.foobnix.tts;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.res.ColorStateList;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.fragment.app.FragmentActivity;

import com.foobnix.android.utils.ResultResponse2;
import com.foobnix.pdf.info.R;
import com.foobnix.pdf.info.TintUtil;
import com.foobnix.pdf.info.model.BookCSS;
import com.foobnix.pdf.info.widget.ChooserDialogFragment;
import com.foobnix.pdf.info.wrapper.DocumentController;
import com.foobnix.sys.TempHolder;

import java.io.File;

/**
 * The window that records the book read aloud into MP3 files: the pages, with the whole book and
 * from-this-page a tap away; the folder the files go to and the recordings already in it; and
 * one button that starts the recording and stops it, over a bar of its progress.
 */
public final class TTSRecordDialog {

    private final DocumentController controller;
    private final Context context;
    private final String bookName;
    private final int pageCount;
    private final Handler main = new Handler(Looper.getMainLooper());

    private AlertDialog dialog;
    private EditText from;
    private EditText to;
    private TextView wholeBook;
    private TextView fromThisPage;
    private TextView path;
    private TextView filesCount;
    private TextView delete;
    private View filesRow;
    private View progressBox;
    private ProgressBar progressBar;
    private TextView progressText;
    private TextView progressHint;
    private TextView start;

    private TTSRecordDialog(DocumentController controller) {
        this.controller = controller;
        this.context = controller.getActivity();
        this.bookName = controller.getCurrentBook().getName();
        this.pageCount = Math.max(1, controller.getPageCount());
    }

    public static void show(DocumentController controller) {
        new TTSRecordDialog(controller).show();
    }

    private void show() {
        final View view = LayoutInflater.from(context).inflate(R.layout.dialog_tts_wav, null, false);
        from = view.findViewById(R.id.from);
        to = view.findViewById(R.id.to);
        wholeBook = view.findViewById(R.id.wholeBook);
        fromThisPage = view.findViewById(R.id.fromThisPage);
        path = view.findViewById(R.id.ttsSpeakPath);
        filesRow = view.findViewById(R.id.filesRow);
        filesCount = view.findViewById(R.id.filesCount);
        delete = view.findViewById(R.id.delete);
        progressBox = view.findViewById(R.id.progressBox);
        progressBar = view.findViewById(R.id.progressBar);
        progressText = view.findViewById(R.id.progressText);
        progressHint = view.findViewById(R.id.progressHint);
        start = view.findViewById(R.id.start);

        TintUtil.asLinkButton(wholeBook, 0);
        TintUtil.asLinkButton(fromThisPage, 0);
        TintUtil.asLinkButton(delete);
        TintUtil.asLinkButton(start, 0);
        // in the colour of the buttons round it
        final ColorStateList link = ColorStateList.valueOf(start.getCurrentTextColor());
        progressBar.setProgressTintList(link);
        progressBar.setIndeterminateTintList(link);

        setRange(1, pageCount);
        wholeBook.setOnClickListener(v -> setRange(1, pageCount));
        fromThisPage.setOnClickListener(v -> setRange(Math.max(1, controller.getCurentPageFirst1()), pageCount));
        path.setOnClickListener(v -> chooseFolder());
        delete.setOnClickListener(v -> askDelete());
        start.setOnClickListener(v -> {
            if (TempHolder.isRecordTTS) {
                TempHolder.isRecordTTS = false;
                start.setEnabled(false); // until the page being recorded ends
            } else {
                startRecording();
            }
        });

        dialog = new AlertDialog.Builder(context)
                .setTitle(R.string.speak_into_file_wav)
                .setView(view)
                .setNegativeButton(R.string.close, null)
                .create();
        // a recording belongs to its window: closed, it stops
        dialog.setOnDismissListener(d -> TempHolder.isRecordTTS = false);
        dialog.show();

        showFolder();
        showRecording(false);
    }

    private void setRange(int first, int last) {
        from.setText(String.valueOf(first));
        to.setText(String.valueOf(last));
        from.setError(null);
        to.setError(null);
    }

    private void showFolder() {
        String text = TTSRecorder.folder(bookName).getPath();
        for (String root : new String[]{"/storage/emulated/0/", "/sdcard/"}) {
            if (text.startsWith(root)) {
                text = text.substring(root.length());
            }
        }
        path.setText(text);
        final int count = TTSRecorder.recordings(bookName).length;
        filesRow.setVisibility(count > 0 ? View.VISIBLE : View.GONE);
        filesCount.setText(context.getString(R.string.record_files_count, count));
    }

    private void chooseFolder() {
        if (TempHolder.isRecordTTS) {
            return;
        }
        ChooserDialogFragment.chooseFolder((FragmentActivity) context, BookCSS.get().ttsSpeakPath)
                             .setOnSelectListener(new ResultResponse2<String, Dialog>() {
                                 @Override public boolean onResultRecive(String folder, Dialog chooser) {
                                     BookCSS.get().ttsSpeakPath = folder;
                                     chooser.dismiss();
                                     showFolder();
                                     return false;
                                 }
                             });
    }

    private void askDelete() {
        if (TempHolder.isRecordTTS) {
            return;
        }
        new AlertDialog.Builder(context)
                .setMessage(R.string.record_delete_confirm)
                .setPositiveButton(R.string.delete, (d, which) -> {
                    for (File file : TTSRecorder.recordings(bookName)) {
                        file.delete();
                    }
                    progressBox.setVisibility(View.GONE);
                    showFolder();
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private Integer page(EditText field) {
        try {
            final int value = Integer.parseInt(field.getText().toString().trim());
            if (value >= 1 && value <= pageCount) {
                return value;
            }
        } catch (NumberFormatException ignored) {
        }
        field.setError(context.getString(R.string.incorrect_value));
        field.requestFocus();
        return null;
    }

    private void startRecording() {
        final Integer first = page(from);
        if (first == null) {
            return;
        }
        final Integer last = page(to);
        if (last == null) {
            return;
        }
        if (first > last) {
            to.setError(context.getString(R.string.incorrect_value));
            to.requestFocus();
            return;
        }
        TempHolder.isRecordTTS = true;
        showRecording(true);
        progressBar.setIndeterminate(true);
        progressText.setText("…");
        progressHint.setVisibility(View.VISIBLE);

        TTSRecorder.record(controller, new TTSRecorder.Listener() {
            @Override public void onPreparing() {
            }

            @Override public void onPage(int page, int firstPage, int lastPage) {
                main.post(() -> {
                    progressBar.setIndeterminate(false);
                    progressBar.setMax(lastPage - firstPage + 1);
                    progressBar.setProgress(page - firstPage);
                    progressText.setText(context.getString(R.string.record_progress, page, lastPage));
                });
            }

            @Override public void onFinished(int saved, boolean stopped) {
                main.post(() -> {
                    progressBar.setIndeterminate(false);
                    if (!stopped) {
                        progressBar.setProgress(progressBar.getMax());
                    }
                    progressText.setText(context.getString(stopped ? R.string.record_stopped : R.string.record_done, saved));
                    ended();
                });
            }

            @Override public void onFailed(String message) {
                main.post(() -> {
                    progressBar.setIndeterminate(false);
                    progressText.setText(message);
                    ended();
                });
            }
        }, first, last);
    }

    private void ended() {
        progressHint.setVisibility(View.GONE);
        showRecording(false);
        showFolder();
    }

    /** The button, the fields and the screen as they are while recording, or not. */
    private void showRecording(boolean recording) {
        if (recording) {
            progressBox.setVisibility(View.VISIBLE);
        }
        start.setEnabled(true);
        start.setText(recording ? R.string.to_stop : R.string.start_recording);
        start.setCompoundDrawablesRelativeWithIntrinsicBounds(
                recording ? R.drawable.ic_stop : R.drawable.ic_mic, 0, 0, 0);
        start.setCompoundDrawableTintList(ColorStateList.valueOf(start.getCurrentTextColor()));
        for (View field : new View[]{from, to, wholeBook, fromThisPage, path, delete}) {
            field.setEnabled(!recording);
            field.setAlpha(recording ? 0.5f : 1f);
        }
        if (dialog != null && dialog.getWindow() != null) {
            // the WebView book stops answering with the screen off, and the recording with it
            if (recording) {
                dialog.getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
            } else {
                dialog.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
            }
        }
    }
}
