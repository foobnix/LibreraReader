package com.foobnix.pdf.search.view;

import android.app.ProgressDialog;
import android.content.Context;
import android.os.AsyncTask;

import com.foobnix.android.utils.LOG;
import com.foobnix.pdf.info.R;
import com.foobnix.pdf.info.view.MyProgressDialog;

public abstract class AsyncProgressTask<T> extends AsyncTask<Object, Object, T> {

    ProgressDialog dialog;

    public abstract Context getContext();


    @Override
    protected void onPreExecute() {
        dialog = MyProgressDialog.show(getContext(),  getContext().getString(R.string.please_wait));
    }

    @Override
    protected void onPostExecute(T result) {
        try {
            dialog.dismiss();
        } catch (Exception e) {
            LOG.d(e);
        }

    }

}
