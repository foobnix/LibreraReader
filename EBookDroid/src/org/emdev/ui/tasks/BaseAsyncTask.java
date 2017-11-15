package org.emdev.ui.tasks;

import java.util.concurrent.atomic.AtomicBoolean;

import org.emdev.utils.LengthUtils;

import com.foobnix.android.utils.LOG;
import com.foobnix.pdf.CopyAsyncTask;
import com.foobnix.pdf.info.view.Dialogs;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;

public abstract class BaseAsyncTask<Params, Result> extends CopyAsyncTask<Params, String, Result> implements OnCancelListener {

    protected final Context context;
    protected final int startProgressStringId;
    protected final boolean cancellable;
    protected final AtomicBoolean continueFlag = new AtomicBoolean(true);
    protected AlertDialog progressDialog;

    public BaseAsyncTask(Context context, int startProgressStringId, boolean cancellable) {
        this.context = context;
        this.startProgressStringId = startProgressStringId;
        this.cancellable = cancellable;
    }

    @Override
    protected void onPreExecute() {
        onProgressUpdate(context.getResources().getString(startProgressStringId));
    }

    @Override
    public void onCancel(final DialogInterface dialog) {
        if (cancellable) {
            continueFlag.set(false);
            cancel(true);
        }
    }

    @Override
    protected void onPostExecute(final Result result) {
        closeProgressDialog();
    }

    protected void closeProgressDialog() {
        if (progressDialog != null) {
            try {
                progressDialog.dismiss();
            } catch (final Throwable e) {
                LOG.e(e);
            }
            progressDialog = null;
        }
    }

    @Override
    protected void onProgressUpdate(final String... values) {
        final int length = LengthUtils.length(values);
        if (length == 0 || context == null) {
            return;
        }
        try {
            final String last = values[length - 1];
            if (progressDialog == null || !progressDialog.isShowing()) {
                progressDialog = Dialogs.loadingBook(context, new Runnable() {

                    @Override
                    public void run() {
                        onCancel(null);
                    }
                }, false);
            } else {
                progressDialog.setMessage(last);
            }
        } catch (final Throwable e) {
            LOG.e(e);
        }

    }
}
