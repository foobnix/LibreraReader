package com.foobnix.pdf.search.view;

import android.app.ProgressDialog;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import com.foobnix.android.utils.LOG;
import com.foobnix.pdf.info.AppsConfig;
import com.foobnix.pdf.info.R;
import com.foobnix.pdf.info.view.MyProgressDialog;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public abstract class ProgressTask<T> {
    private ProgressDialog dialog;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private volatile boolean cancelled = false;

    public abstract Context getContext();

    protected abstract T doInBackground(Object... params) throws Exception;

    protected void onPostExecute(T result) {
    }

    private boolean withDialog = true;

    public ProgressTask() {

    }

    public ProgressTask(Boolean withDialog) {
        this.withDialog = withDialog;
    }

    protected void onPreExecute() {

    }

    protected void onCancelled() {
    }

    public final void execute(final Object... params) {
        LOG.d("ProgressTask", "ProgressTask run");
        //   onPreExecute();
        Context context = getContext();
        if (withDialog) {
            dialog = MyProgressDialog.show(context, context.getString(R.string.please_wait));
        }

        AppsConfig.executorService.execute(() -> {
            try {
                final T result = doInBackground(params);

                if (!cancelled) {
                    mainHandler.post(() -> {
                        if (dialog != null) {
                            dialog.dismiss();
                        }
                        onPostExecute(result);
                    });
                }
            } catch (Exception e) {
                LOG.d("ProgressTask", e.toString());
                mainHandler.post(() -> {
                    if (dialog != null) {
                        dialog.dismiss();
                    }
                });
            }
        });
    }

    public final void cancel() {
        cancelled = true;
        if (dialog != null) {
            dialog.dismiss();
        }
        onCancelled();
    }
}
