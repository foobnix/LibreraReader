package com.foobnix.ui2.fragment;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import com.foobnix.android.utils.AsyncTasks;
import com.foobnix.android.utils.Dips;
import com.foobnix.android.utils.LOG;
import com.foobnix.android.utils.TxtUtils;
import com.foobnix.pdf.info.R;
import com.foobnix.pdf.info.wrapper.AppState;
import com.foobnix.pdf.info.wrapper.PopupHelper;
import com.foobnix.pdf.search.activity.msg.NotifyAllFragments;
import com.foobnix.pdf.search.activity.msg.OpenDirMessage;
import com.foobnix.pdf.search.activity.msg.UpdateAllFragments;
import com.foobnix.sys.TempHolder;
import com.foobnix.ui2.MainTabs2;
import com.foobnix.ui2.adapter.AuthorsAdapter2;
import com.foobnix.ui2.adapter.DefaultListeners;
import com.foobnix.ui2.adapter.FileMetaAdapter;
import com.foobnix.ui2.fast.FastScrollRecyclerView;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.util.Pair;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.GridLayoutManager.SpanSizeLookup;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;

public abstract class UIFragment<T> extends Fragment {
    public static String INTENT_TINT_CHANGE = "INTENT_TINT_CHANGE";

    Handler handler;
    protected volatile ProgressBar progressBar;
    protected RecyclerView recyclerView;

    public abstract Pair<Integer, Integer> getNameAndIconRes();

    View adFrame;

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        handler = new Handler();
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        TxtUtils.updateAllLinks(view);
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

    public final void onSelectFragment() {
        if (getActivity() == null) {
            return;
        }
        if (listHash != TempHolder.listHash) {
            LOG.d("TempHolder.listHash", listHash, TempHolder.listHash);
            resetFragment();
            listHash = TempHolder.listHash;
        } else {
            notifyFragment();

            try {
                if (adFrame == null) {
                    adFrame = getActivity().findViewById(R.id.adFrame);
                }

                if (adFrame != null && adFrame.getVisibility() == View.INVISIBLE) {
                    adFrame.setVisibility(View.VISIBLE);
                }
            } catch (Exception e) {
                LOG.e(e);
            }

        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void notifyUpdateFragment(UpdateAllFragments event) {
        TempHolder.listHash++;
        onSelectFragment();
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void notifyUpdateFragment(NotifyAllFragments event) {
        notifyFragment();
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
        notifyFragment();
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

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (recyclerView != null) {
            try {
                recyclerView.setAdapter(null);
            } catch (Exception e) {
                LOG.e(e);
            }
        }
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

    public boolean isInProgress() {
        return progressBar != null && progressBar.getVisibility() == View.VISIBLE;
    }

    AsyncTask<Object, Object, List<T>> execute;

    public void populate() {
        if (true) {
            // return;
        }

        // if (isInProgress()) {
        // AsyncTasks.toastPleaseWait(getActivity());
        // return;
        // }

        if (AsyncTasks.isFinished(execute)) {

            execute = new AsyncTask<Object, Object, List<T>>() {
                @Override
                protected List<T> doInBackground(Object... params) {
                    try {
                        LOG.d("UIFragment", "prepareDataInBackground");
                        return prepareDataInBackground();
                    } catch (Exception e) {
                        LOG.e(e);
                        return new ArrayList<T>();
                    }
                }

                @Override
                protected void onPreExecute() {
                    if (progressBar != null) {
                        handler.postDelayed(new Runnable() {

                            @Override
                            public void run() {
                                progressBar.setVisibility(View.VISIBLE);
                            }
                        }, 100);

                    }
                };

                @Override
                protected void onPostExecute(List<T> result) {
                    if (progressBar != null) {
                        handler.removeCallbacksAndMessages(null);
                        progressBar.setVisibility(View.GONE);
                    }
                    if (getActivity() != null) {
                        try {
                            populateDataInUI(result);
                        } catch (Exception e) {
                            LOG.e(e);
                        }
                    }
                }
            }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        } else {
            // AsyncTasks.toastPleaseWait(getActivity());
            LOG.d("SKIP task");
        }
    }

    public void onGridList(int mode, ImageView onGridlList, final FileMetaAdapter searchAdapter, AuthorsAdapter2 authorsAdapter) {
        if (searchAdapter == null) {
            return;
        }
        if (onGridlList != null) {
            PopupHelper.updateGridOrListIcon(onGridlList, mode);
        }

        if (mode == AppState.MODE_LIST) {
            RecyclerView.LayoutManager mLayoutManager = new LinearLayoutManager(getActivity());
            recyclerView.setLayoutManager(mLayoutManager);
            searchAdapter.setAdapterType(FileMetaAdapter.ADAPTER_LIST);
            recyclerView.setAdapter(searchAdapter);

        } else if (mode == AppState.MODE_COVERS || mode == AppState.MODE_GRID) {
            final int num = Math.max(1, Dips.screenWidthDP() / AppState.get().coverBigSize);

            GridLayoutManager mGridManager = new GridLayoutManager(getActivity(), num);
            mGridManager.setSpanSizeLookup(new SpanSizeLookup() {

                @Override
                public int getSpanSize(int pos) {
                    int type = searchAdapter.getItemViewType(pos);
                    if (type == FileMetaAdapter.DISPALY_TYPE_LAYOUT_TITLE_FOLDERS) {
                        return num;
                    }
                    if (type == FileMetaAdapter.DISPALY_TYPE_LAYOUT_TAG) {
                        return 1;
                    }

                    if (type == FileMetaAdapter.DISPALY_TYPE_LAYOUT_TITLE_NONE) {
                        return num;
                    }
                    if (type == FileMetaAdapter.DISPALY_TYPE_LAYOUT_TITLE_DIVIDER) {
                        return num;
                    }
                    if (type == FileMetaAdapter.DISPLAY_TYPE_DIRECTORY) {
                        if (num == 1) {
                            return 1;
                        } else if (num == 2) {
                            return 1;
                        } else if (num == 3) {
                            return 3;
                        }
                        return 2;
                    }

                    if (type == FileMetaAdapter.DISPALY_TYPE_SERIES) {
                        return num;
                    }
                    return (type == FileMetaAdapter.DISPALY_TYPE_LAYOUT_TITLE_BOOKS) ? num : 1;
                }
            });

            recyclerView.setLayoutManager(mGridManager);

            searchAdapter.setAdapterType(mode == AppState.MODE_COVERS ? FileMetaAdapter.ADAPTER_COVERS : FileMetaAdapter.ADAPTER_GRID);
            recyclerView.setAdapter(searchAdapter);

        } else if (Arrays.asList(AppState.MODE_AUTHORS, AppState.MODE_SERIES, AppState.MODE_GENRE, AppState.MODE_USER_TAGS, AppState.MODE_KEYWORDS).contains(mode)) {
            RecyclerView.LayoutManager mLayoutManager = new LinearLayoutManager(getActivity());
            recyclerView.setLayoutManager(mLayoutManager);
            recyclerView.setAdapter(authorsAdapter);
        } else if (mode == AppState.MODE_LIST_COMPACT) {
            final int num = Math.max(2, Dips.screenWidthDP() / Dips.dpToPx(300));
            GridLayoutManager mGridManager = new GridLayoutManager(getActivity(), num);
            mGridManager.setSpanSizeLookup(new SpanSizeLookup() {

                @Override
                public int getSpanSize(int pos) {
                    int type = searchAdapter.getItemViewType(pos);
                    if (type == FileMetaAdapter.DISPALY_TYPE_LAYOUT_TITLE_FOLDERS) {
                        return num;
                    }
                    if (type == FileMetaAdapter.DISPALY_TYPE_LAYOUT_TAG) {
                        return 1;
                    }

                    if (type == FileMetaAdapter.DISPALY_TYPE_LAYOUT_TITLE_NONE) {
                        return num;
                    }
                    if (type == FileMetaAdapter.DISPALY_TYPE_LAYOUT_TITLE_DIVIDER) {
                        return num;
                    }

                    return (type == FileMetaAdapter.DISPALY_TYPE_LAYOUT_TITLE_BOOKS) ? num : 1;
                }
            });

            recyclerView.setLayoutManager(mGridManager);
            searchAdapter.setAdapterType(FileMetaAdapter.ADAPTER_LIST_COMPACT);
            recyclerView.setAdapter(searchAdapter);
        }

        if (recyclerView instanceof FastScrollRecyclerView) {
            ((FastScrollRecyclerView) recyclerView).myConfiguration();
        }
    }

    public boolean onKeyDown(int keyCode) {
        if (recyclerView == null) {
            return false;
        }
        View childAt = recyclerView.getChildAt(0);
        if (childAt == null) {
            return false;
        }
        int size = childAt.getHeight() + childAt.getPaddingTop() + Dips.dpToPx(2);

        if (AppState.get().getNextKeys().contains(keyCode)) {
            recyclerView.scrollBy(0, size);
            return true;

        }
        if (AppState.get().getPrevKeys().contains(keyCode)) {
            recyclerView.scrollBy(0, size * -1);
            return true;
        }
        return false;
    }

}
