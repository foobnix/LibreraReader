package com.foobnix.pdf.search.view;

import android.app.ProgressDialog;
import android.content.Context;

import com.foobnix.android.utils.LOG;

public abstract class AsyncProgressTask<T> extends ProgressTask<T> {

    ProgressDialog dialog;

    public abstract Context getContext();

    @Override
    protected void onPostExecute(T result) {
        try {
            dialog.dismiss();
        } catch (Exception e) {
            LOG.d(e);
        }

    }

}
