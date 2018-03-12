package org.emdev.ui.tasks;

import com.foobnix.android.utils.LOG;
import com.foobnix.pdf.CopyAsyncTask;
import com.foobnix.pdf.info.view.Dialogs;

import android.app.AlertDialog;
import android.content.Context;

public abstract class BaseAsyncTask<Params, Result> extends CopyAsyncTask<Params, String, Result> {

    protected final Context context;
    protected AlertDialog progressDialog;

    public BaseAsyncTask(Context context) {
        this.context = context;
    }

    public void onBookCancel() {
        try {
            progressDialog.dismiss();
        } catch (Exception e) {
            LOG.e(e);
        }

    }

    @Override
    protected void onPreExecute() {
        progressDialog = Dialogs.loadingBook(context, new Runnable() {

            @Override
            public void run() {
                onBookCancel();
            }
        });
    }

    @Override
    protected void onPostExecute(Result result) {
        super.onPostExecute(result);
        try {
            if (progressDialog != null) {
                progressDialog.dismiss();
            }
        } catch (Exception e) {
        }
    }

}
