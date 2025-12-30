package com.foobnix.work;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.work.ExistingWorkPolicy;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;
import androidx.work.WorkerParameters;

import com.foobnix.pdf.info.Clouds;

public class SyncDropboxWorker extends MessageWorker{
    public SyncDropboxWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    public static void run(Context context) {
        OneTimeWorkRequest workRequest = new OneTimeWorkRequest.Builder(SyncDropboxWorker.class).build();
        //WorkManager.getInstance(context).enqueue(workRequest);
        WorkManager.getInstance(context).enqueueUniqueWork("search", ExistingWorkPolicy.KEEP, workRequest);
    }


    @Override
    boolean doWorkInner() {

        Clouds.get().syncronizeGet();
        return true;
    }
}
