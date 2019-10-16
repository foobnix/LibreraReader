package com.foobnix.android.utils;

import android.content.Context;
import android.os.AsyncTask;
import android.widget.Toast;

import com.foobnix.pdf.info.R;

public class AsyncTasks {

    public static boolean isRunning(AsyncTask task) {
        return !isFinished(task);
    }

    public static boolean isFinished(AsyncTask task) {
        return task == null || task.getStatus() == AsyncTask.Status.FINISHED;
    }

    public static void toastPleaseWait(Context c) {
        if (c != null) {
            Toast.makeText(c, R.string.please_wait, Toast.LENGTH_SHORT).show();
        }
    }

}
