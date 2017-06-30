package com.foobnix.ui2.fragment;

import java.util.ArrayList;
import java.util.List;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;

import com.foobnix.android.utils.LOG;
import com.foobnix.android.utils.TxtUtils;
import com.foobnix.pdf.search.activity.msg.OpenDirMessage;
import com.foobnix.sys.TempHolder;
import com.foobnix.ui2.MainTabs2;
import com.foobnix.ui2.adapter.DefaultListeners;
import com.foobnix.ui2.adapter.FileMetaAdapter;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.os.AsyncTask;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.util.Pair;

public abstract class UIFragment<T> extends Fragment {
    public static String INTENT_TINT_CHANGE = "INTENT_TINT_CHANGE";

    Handler handler;

    public abstract Pair<Integer, Integer> getNameAndIconRes();

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        handler = new Handler();
    }

    @Override
    public void onDetach() {
        super.onDetach();
        handler.removeCallbacksAndMessages(null);
    }

    public boolean isBackPressed() {
        return false;
    }

    public abstract void notifyFragment();

    public abstract void resetFragment();

    int listHash = 0;

    public void onSelectFragment() {
        if (getActivity() == null) {
            return;
        }
        if (listHash != TempHolder.listHash) {
            LOG.d("TempHolder.listHash", listHash, TempHolder.listHash);
            resetFragment();
            notifyFragment();
            listHash = TempHolder.listHash;
        }
    }

    public void bindAdapter(FileMetaAdapter searchAdapter) {
        DefaultListeners.bindAdapter(getActivity(), searchAdapter);
    }

    public void bindAuthorsSeriesAdapter(FileMetaAdapter searchAdapter) {
        DefaultListeners.bindAdapterAuthorSerias(getActivity(), searchAdapter);
    }

    public List<T> prepareDataInBackground() {
        return null;
    }

    public void populateDataInUI(List<T> items) {

    }

    public void onTintChanged() {

    }

    public void sendNotifyTintChanged() {
        Intent itent = new Intent(INTENT_TINT_CHANGE);
        LocalBroadcastManager.getInstance(getActivity()).sendBroadcast(itent);
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        TempHolder.listHash++;
        onSelectFragment();
    }


    @Override
    public void onResume() {
        super.onResume();
        onSelectFragment();
        LocalBroadcastManager.getInstance(getActivity()).registerReceiver(broadcastReceiver, new IntentFilter(INTENT_TINT_CHANGE));
        EventBus.getDefault().register(this);
    }

    @Override
    public void onPause() {
        super.onPause();
        LocalBroadcastManager.getInstance(getActivity()).unregisterReceiver(broadcastReceiver);
        EventBus.getDefault().unregister(this);
    }

    @Subscribe
    public void onReviceOpenDir(OpenDirMessage msg) {
        onReviceOpenDir(msg.getPath());
    }

    public void onReviceOpenDir(String path) {

    }

    BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            String txt = intent.getStringExtra(MainTabs2.EXTRA_SEACH_TEXT);
            if (TxtUtils.isNotEmpty(txt)) {
                onTextRecive(txt);
            } else {
                onTintChanged();
            }
        }
    };

    public void onTextRecive(String txt) {

    }

    AsyncTask<Object, Object, List<T>> execute;

    public void populate() {
        if (execute == null || execute.getStatus() == AsyncTask.Status.FINISHED) {
            execute = new AsyncTask<Object, Object, List<T>>() {
                @Override
                protected List<T> doInBackground(Object... params) {
                    try {
                        return prepareDataInBackground();
                    } catch (Exception e) {
                        LOG.e(e);
                        return new ArrayList<T>();
                    }
                }

                @Override
                protected void onPostExecute(List<T> result) {
                    if (getActivity() != null) {
                        try {
                            populateDataInUI(result);
                        } catch (Exception e) {
                            LOG.e(e);
                        }
                    }
                }
            }.execute(AsyncTask.THREAD_POOL_EXECUTOR);
        } else {
            LOG.d("SKIP task");
        }
    }

}
