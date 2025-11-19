package com.foobnix.pdf.search.view;

import android.app.ProgressDialog;
import android.content.Context;
import android.graphics.Color;
import android.widget.Toast;

import com.foobnix.android.utils.LOG;
import com.foobnix.android.utils.Objects;
import com.foobnix.android.utils.ResultResponse;
import com.foobnix.model.AppState;
import com.foobnix.pdf.info.R;
import com.foobnix.pdf.info.TintUtil;

public abstract class AsyncProgressResultToastTask extends ProgressTask<Boolean> {

    ProgressDialog dialog;
    Context c;
    ResultResponse<Boolean> onResult;

    public AsyncProgressResultToastTask(Context c, ResultResponse<Boolean> onResult) {
        this.c = c;
        this.onResult = onResult;
    }

    @Override
    public Context getContext() {
        return c;
    }

    public AsyncProgressResultToastTask(Context c) {
        this.c = c;
    }

    @Override
    protected void onPreExecute() {

        try {
            android.widget.ProgressBar pr = (android.widget.ProgressBar) Objects.getInstanceValue(dialog, "mProgress");
            pr.setSaveEnabled(false);
            TintUtil.setDrawableTint(pr.getIndeterminateDrawable().getCurrent(), AppState.get().isDayNotInvert ? TintUtil.color : Color.WHITE);
        } catch (Exception e) {
            LOG.e(e);
        }


    }


    @Override
    protected void onPostExecute(Boolean result) {
        try {
            dialog.dismiss();
        } catch (Exception e) {
            LOG.d(e);
        }
        if (onResult != null) {
            onResult.onResultRecive(result);
        }

        if (result) {
            Toast.makeText(c, R.string.success, Toast.LENGTH_LONG).show();
        } else {
            Toast.makeText(c, R.string.fail, Toast.LENGTH_LONG).show();
        }
    }

}
