package com.foobnix.work;

import android.content.Context;
import android.content.Intent;

import androidx.annotation.NonNull;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.work.ExistingWorkPolicy;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.foobnix.android.utils.LOG;
import com.foobnix.pdf.info.Prefs;
import com.foobnix.pdf.search.activity.msg.MessageSyncFinish;
import com.foobnix.ui2.BooksService;

import org.greenrobot.eventbus.EventBus;

import java.io.IOException;
import java.util.Collection;

abstract class MessageWorker extends Worker {
    private static int activeWorkers;

    private static synchronized void started() {
        activeWorkers++;
        BooksService.isRunning = true;
    }

    private static synchronized boolean finished() {
        BooksService.isRunning = --activeWorkers > 0;
        return !BooksService.isRunning;
    }

    public MessageWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    public static void sendFinishMessage(Context c) {
        Intent intent =
                new Intent(BooksService.INTENT_NAME).putExtra(Intent.EXTRA_TEXT, BooksService.RESULT_SEARCH_FINISH);
        LocalBroadcastManager.getInstance(c)
                             .sendBroadcast(intent);
    }

    @NonNull @Override public Result doWork() {
        boolean notifyResult = false;
        started();
        try {
            Prefs.get().init(getApplicationContext());
            LOG.d("MessageWorker-Status", "Status: #1 Started", this.getClass(), Thread.currentThread());
            notifyResult = doWorkInner();
            return notifyResult ? Result.success() : Result.failure();
        } catch (Exception e) {
            LOG.e(e);
            return Result.failure();
        } catch (OutOfMemoryError e) {
            System.gc();
            return Result.failure();
        } catch (Throwable e) {
            return Result.failure();
        } finally {
            boolean lastWorker = finished();
            if (reportsOwnCompletion()) {
                if (!notifyResult || !publishCompletion(this::sendFinishMessage))
                    publishFailure(() -> sendFinishMessage(getApplicationContext()));
            } else if (lastWorker) {
                if (notifyResult && publishCompletion(this::sendFinishMessage)) {
                    // Success was published by the current worker.
                } else {
                    // Failure and cancellation must also release the library's busy UI.
                    // Do not publish the success-only synchronization event.
                    sendFinishMessage(getApplicationContext());
                }
            }
            LOG.d("MessageWorker-Status", "Status: #2 Finished", this.getClass(), Thread.currentThread());
        }

    }

    abstract boolean doWorkInner() throws IOException, InterruptedException;

    protected boolean publishCompletion(Runnable action) {
        action.run();
        return true;
    }

    /** Scans publish their own terminal event even while an obsolete worker unwinds. */
    protected boolean reportsOwnCompletion() { return false; }

    /** The current cancelled scan still releases its busy UI; a replaced scan is silent. */
    protected boolean publishFailure(Runnable action) { return publishCompletion(action); }

    protected void sendFinishMessage() {
        try {
            //AppDB.get().getDao().detachAll();
        } catch (Exception e) {
            LOG.e(e);
        }

        sendFinishMessage(getApplicationContext());
        EventBus.getDefault()
                .post(new MessageSyncFinish());
    }

    protected void sendTextMessage(String text) {
        Intent itent =
                new Intent(BooksService.INTENT_NAME).putExtra(Intent.EXTRA_TEXT, BooksService.RESULT_SEARCH_MESSAGE_TXT)
                                                    .putExtra("TEXT", text);
        LocalBroadcastManager.getInstance(getApplicationContext())
                             .sendBroadcast(itent);
    }

    protected void sendNotifyAll() {
        Intent itent = new Intent(BooksService.INTENT_NAME).putExtra(Intent.EXTRA_TEXT, BooksService.RESULT_NOTIFY_ALL);
        LocalBroadcastManager.getInstance(getApplicationContext())
                             .sendBroadcast(itent);
    }

    protected void sendScanBatch() {
        Intent intent = new Intent(BooksService.INTENT_NAME)
                .putExtra(Intent.EXTRA_TEXT, BooksService.RESULT_SEARCH_BATCH);
        LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(intent);
    }

    protected void sendProggressMessage(Collection<?> itemsMeta) {
        Intent itent =
                new Intent(BooksService.INTENT_NAME).putExtra(Intent.EXTRA_TEXT, BooksService.RESULT_SEARCH_COUNT)
                                                    .putExtra("android.intent.extra.INDEX", itemsMeta.size());
        LocalBroadcastManager.getInstance(getApplicationContext())
                             .sendBroadcast(itent);
    }

    protected void sendBuildingLibrary() {
        Intent itent =
                new Intent(BooksService.INTENT_NAME).putExtra(Intent.EXTRA_TEXT, BooksService.RESULT_BUILD_LIBRARY);
        LocalBroadcastManager.getInstance(getApplicationContext())
                             .sendBroadcast(itent);
    }

}
