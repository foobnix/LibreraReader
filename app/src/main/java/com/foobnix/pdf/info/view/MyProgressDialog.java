package com.foobnix.pdf.info.view;

import android.app.ProgressDialog;
import android.content.Context;
import android.graphics.Color;
import android.os.Handler;
import android.os.Looper;

import com.foobnix.android.utils.LOG;
import com.foobnix.android.utils.Objects;
import com.foobnix.model.AppState;
import com.foobnix.pdf.info.TintUtil;


public class MyProgressDialog {

    static Handler handler = new Handler(Looper.getMainLooper());

    public static ProgressDialog show(Context c, String subtitile) {
        ProgressDialog dialog = android.app.ProgressDialog.show(c, "", subtitile);

        try {
            android.widget.ProgressBar pr = (android.widget.ProgressBar) Objects.getInstanceValue(dialog, "mProgress");
            pr.setSaveEnabled(false);
            TintUtil.setDrawableTint(pr.getIndeterminateDrawable().getCurrent(), AppState.get().isDayNotInvert ? TintUtil.color : Color.WHITE);
        } catch (Exception e) {
            LOG.e(e);
        }

        handler.postDelayed(() -> {
            try {
                dialog.dismiss();
            } catch (Exception e) {
                LOG.e(e);
            }
        }, 30 * 1000);

        dialog.setOnDismissListener(dialog1 -> handler.removeCallbacksAndMessages(null));


        return dialog;
    }
}
