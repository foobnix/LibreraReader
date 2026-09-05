package com.foobnix.pdf.search.view;

import android.content.Context;
import android.widget.Toast;

import com.foobnix.android.utils.ResultResponse;
import com.foobnix.pdf.info.R;

public abstract class AsyncProgressResultToastTask extends ProgressTask<Boolean> {

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
    protected void onPostExecute(Boolean result) {
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
