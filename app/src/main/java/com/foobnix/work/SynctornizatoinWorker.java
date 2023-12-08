package com.foobnix.work;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;
import androidx.work.WorkerParameters;

import com.foobnix.android.utils.LOG;
import com.foobnix.drive.GFile;
import com.foobnix.model.AppProfile;
import com.foobnix.model.AppSP;
import com.foobnix.pdf.search.activity.msg.MessageSync;
import com.foobnix.pdf.search.activity.msg.UpdateAllFragments;
import com.foobnix.sys.TempHolder;
import com.google.api.client.googleapis.extensions.android.gms.auth.UserRecoverableAuthIOException;

import org.greenrobot.eventbus.EventBus;

public class SynctornizatoinWorker extends MessageWorker{
    public SynctornizatoinWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }


    @Override
    void doWorkInner() {
        if (AppSP.get().isEnableSync) {
            AppProfile.save(getApplicationContext());
            try {
                EventBus.getDefault().post(new MessageSync(MessageSync.STATE_VISIBLE));
                AppSP.get().syncTimeStatus = MessageSync.STATE_VISIBLE;
                GFile.sycnronizeAll(getApplicationContext());

                AppSP.get().syncTime = System.currentTimeMillis();
                AppSP.get().syncTimeStatus = MessageSync.STATE_SUCCESS;
                EventBus.getDefault().post(new MessageSync(MessageSync.STATE_SUCCESS));
            } catch (UserRecoverableAuthIOException e) {
                GFile.logout(getApplicationContext());
                AppSP.get().syncTimeStatus = MessageSync.STATE_FAILE;
                EventBus.getDefault().post(new MessageSync(MessageSync.STATE_FAILE));
            } catch (Exception e) {
                AppSP.get().syncTimeStatus = MessageSync.STATE_FAILE;
                EventBus.getDefault().post(new MessageSync(MessageSync.STATE_FAILE));
                LOG.e(e);
            }
            if (GFile.isNeedUpdate) {
                LOG.d("GFILE-isNeedUpdate", GFile.isNeedUpdate);
                TempHolder.get().listHash++;
                EventBus.getDefault().post(new UpdateAllFragments());
            }

        }
    }
}
