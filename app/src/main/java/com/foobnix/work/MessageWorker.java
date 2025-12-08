package com.foobnix.work;

import android.content.Context;
import android.content.Intent;

import androidx.annotation.NonNull;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.foobnix.android.utils.LOG;
import com.foobnix.pdf.search.activity.msg.MessageSyncFinish;
import com.foobnix.ui2.BooksService;

import org.greenrobot.eventbus.EventBus;

import java.io.IOException;
import java.util.Collection;

abstract class MessageWorker extends Worker {
    public MessageWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }


    public static void sendFinishMessage(Context c) {
        Intent intent = new Intent(BooksService.INTENT_NAME).putExtra(Intent.EXTRA_TEXT, BooksService.RESULT_SEARCH_FINISH);
        LocalBroadcastManager.getInstance(c).sendBroadcast(intent);
    }

    @NonNull
    @Override
    public Result doWork() {
        try {
            BooksService.isRunning = true;
            LOG.d("MessageWorker-Status", "Start", BooksService.isRunning, this.getClass());
            doWorkInner();
            return Result.success();
        } catch (Exception e) {
            LOG.e(e);
            return Result.failure();
        } finally {
            sendFinishMessage();
            BooksService.isRunning = false;
            LOG.d("MessageWorker-Status", "Finish", BooksService.isRunning, this.getClass());
        }

    }

    abstract void doWorkInner() throws IOException;

    protected void sendFinishMessage() {
        try {
            //AppDB.get().getDao().detachAll();
        } catch (Exception e) {
            LOG.e(e);
        }

        sendFinishMessage(getApplicationContext());
        EventBus.getDefault().post(new MessageSyncFinish());
    }

    protected void sendTextMessage(String text) {
        Intent itent = new Intent(BooksService.INTENT_NAME).putExtra(Intent.EXTRA_TEXT, BooksService.RESULT_SEARCH_MESSAGE_TXT).putExtra("TEXT", text);
        LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(itent);
    }

    protected void sendNotifyAll() {
        Intent itent = new Intent(BooksService.INTENT_NAME).putExtra(Intent.EXTRA_TEXT, BooksService.RESULT_NOTIFY_ALL);
        LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(itent);
    }

    protected void sendProggressMessage(Collection<?> itemsMeta) {
        Intent itent = new Intent(BooksService.INTENT_NAME).putExtra(Intent.EXTRA_TEXT, BooksService.RESULT_SEARCH_COUNT).putExtra("android.intent.extra.INDEX", itemsMeta.size());
        LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(itent);
    }

    protected void sendBuildingLibrary() {
        Intent itent = new Intent(BooksService.INTENT_NAME).putExtra(Intent.EXTRA_TEXT, BooksService.RESULT_BUILD_LIBRARY);
        LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(itent);
    }

}
