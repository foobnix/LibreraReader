package com.foobnix.pdf.info.view;

import android.app.AlertDialog;
import android.content.Context;
import android.graphics.Color;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;

import com.foobnix.android.utils.LOG;
import com.foobnix.model.AppState;
import com.foobnix.pdf.info.R;
import com.foobnix.pdf.info.TintUtil;


public class MyProgressDialog {

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final AlertDialog dialog;
    private final TextView text;

    private MyProgressDialog(Context c, String subtitile) {
        View view = LayoutInflater.from(c).inflate(R.layout.dialog_loading_book, null, false);

        int color = AppState.get().isDayNotInvert ? TintUtil.color : Color.WHITE;

        text = view.findViewById(R.id.text1);
        text.setText(subtitile);
        TintUtil.setTintText(text, color);

        view.findViewById(R.id.onCancel).setVisibility(View.GONE);

        MyProgressBar pr = view.findViewById(R.id.MyProgressBarLoading);
        pr.setSaveEnabled(false);
        pr.setSaveFromParentEnabled(false);
        TintUtil.setDrawableTint(pr.getIndeterminateDrawable().getCurrent(), color);

        dialog = new AlertDialog.Builder(c).setView(view).show();
        dialog.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);

        handler.postDelayed(this::dismiss, 30 * 1000);
        dialog.setOnDismissListener(dialog1 -> handler.removeCallbacksAndMessages(null));
    }

    public static MyProgressDialog show(Context c, String subtitile) {
        return new MyProgressDialog(c, subtitile);
    }

    public void dismiss() {
        try {
            dialog.dismiss();
        } catch (Exception e) {
            LOG.e(e);
        }
    }

    public void show() {
        dialog.show();
    }

    public void setCancelable(boolean cancelable) {
        dialog.setCancelable(cancelable);
    }

    public void setMessage(CharSequence message) {
        text.setText(message);
    }

    public boolean isShowing() {
        return dialog.isShowing();
    }
}
