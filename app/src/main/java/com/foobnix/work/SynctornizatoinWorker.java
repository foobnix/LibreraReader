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
import com.foobnix.pdf.info.AnnotationsFinder;
import com.foobnix.pdf.search.activity.msg.MessageSync;
import com.foobnix.pdf.search.activity.msg.UpdateAllFragments;
import com.foobnix.sys.TempHolder;
import com.google.api.client.googleapis.extensions.android.gms.auth.UserRecoverableAuthIOException;

import org.greenrobot.eventbus.EventBus;

public class SynctornizatoinWorker extends MessageWorker{

    /**
     * True for as long as a sync runs in this process. The saved status cannot say so: a
     * screen opened mid-sync (the library reopens after a sign-in) reads it from a profile
     * saved before the sync began.
     */
    public static volatile boolean isRunning;
    public SynctornizatoinWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }


    @Override
    boolean doWorkInner() {
        if (AppSP.get().isEnableSync) {
            AppProfile.save(getApplicationContext());
            isRunning = true;
            try {
                AppSP.get().syncTimeStatus = MessageSync.STATE_VISIBLE;
                EventBus.getDefault().post(new MessageSync(MessageSync.STATE_VISIBLE));
                GFile.sycnronizeAll(getApplicationContext());

                AppSP.get().syncTime = System.currentTimeMillis();
                AppSP.get().syncTimeStatus = MessageSync.STATE_SUCCESS;
                isRunning = false;
                EventBus.getDefault().post(new MessageSync(MessageSync.STATE_SUCCESS));
            } catch (UserRecoverableAuthIOException e) {
                isRunning = false;
                GFile.logout(getApplicationContext());
                AppSP.get().syncTimeStatus = MessageSync.STATE_FAILE;
                EventBus.getDefault().post(new MessageSync(MessageSync.STATE_FAILE));
            } catch (Exception e) {
                isRunning = false;
                AppSP.get().syncTimeStatus = MessageSync.STATE_FAILE;
                EventBus.getDefault().post(new MessageSync(MessageSync.STATE_FAILE));
                LOG.e(e);
            } finally {
                isRunning = false;
            }
            if (GFile.isNeedUpdate) {
                LOG.d("GFILE-isNeedUpdate", GFile.isNeedUpdate);
                TempHolder.get().listHash++;
                // Marks made on another device: the finder stays for as long as the app runs
                AnnotationsFinder.invalidate();
                EventBus.getDefault().post(new UpdateAllFragments());
            }

        }
        return true;
    }
}
