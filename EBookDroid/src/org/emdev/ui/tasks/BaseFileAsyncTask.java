package org.emdev.ui.tasks;

import android.app.AlertDialog;
import android.content.Context;
import android.widget.Toast;

import java.io.File;

import org.emdev.ui.progress.IProgressIndicator;
import org.emdev.ui.tasks.BaseFileAsyncTask.FileTaskResult;

public abstract class BaseFileAsyncTask<Params, Result extends FileTaskResult> extends BaseAsyncTask<Params, Result> implements
        IProgressIndicator {

    protected final int resultStringId;
    protected final int errorStringId;

    public BaseFileAsyncTask(final Context context, final int startProgressStringId, final int resultStringId,
            final int errorStringId, final boolean cancellable) {
        super(context, startProgressStringId, cancellable);
        this.resultStringId = resultStringId;
        this.errorStringId = errorStringId;
    }

    @Override
    public void setProgressDialogMessage(final int resourceID, final Object... args) {
        publishProgress(context.getResources().getString(resourceID, args));
    }

    @Override
    protected void onPostExecute(final Result result) {
        super.onPostExecute(result);
        if (result == null) {
            processNoResult();
            return;
        }

        if (result.target != null) {
            processTargetFile(result.target);
            return;
        }

        if (result.error != null) {
            processError(result.error);
        }
    }

    protected void processNoResult() {
    }

    protected void processTargetFile(final File target) {
        final String msg = context.getResources().getString(resultStringId, target.getAbsolutePath());
        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show();
    }

    protected void processError(final Throwable error) {
        final AlertDialog.Builder builder = new AlertDialog.Builder(context);

        builder.setTitle("Error");
        builder.setMessage(context.getResources().getString(errorStringId, error.getLocalizedMessage()));
        builder.setPositiveButton(android.R.string.ok, null);
        builder.show();
    }

    public static class FileTaskResult {

        public File target;
        public Throwable error;

        public FileTaskResult(final File target) {
            super();
            this.target = target;
        }

        public FileTaskResult(final Throwable error) {
            super();
            this.error = error;
        }

    }

}
