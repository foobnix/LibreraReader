package com.foobnix.ui2.fragment;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.ImageView;

import androidx.core.util.Pair;
import androidx.fragment.app.Fragment;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.GridLayoutManager.SpanSizeLookup;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.bumptech.glide.Glide;
import com.foobnix.android.utils.Apps;
import com.foobnix.android.utils.Dips;
import com.foobnix.android.utils.LOG;
import com.foobnix.android.utils.TxtUtils;
import com.foobnix.model.AppState;
import com.foobnix.pdf.info.AppsConfig;
import com.foobnix.pdf.info.IMG;
import com.foobnix.pdf.info.R;
import com.foobnix.pdf.info.view.MyProgressBar;
import com.foobnix.pdf.info.wrapper.DocumentController;
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
import com.foobnix.ui2.fast.FastScrollStateChangeListener;

import org.ebookdroid.LibreraApp;
import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.Arrays;
import java.util.List;

public abstract class UIFragment<T> extends Fragment {
    public static String INTENT_TINT_CHANGE = "INTENT_TINT_CHANGE";
    protected volatile MyProgressBar MyProgressBar;
    protected RecyclerView recyclerView;
    Handler handler;
    View adFrame;
    SwipeRefreshLayout swipeRefreshLayout;
    int listHash = 0;
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
    AsyncTask<Object, Object, List<T>> execute;
    volatile boolean inProgress = false;

    public abstract Pair<Integer, Integer> getNameAndIconRes();

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        handler = new Handler();
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);


        TxtUtils.updateAllLinks(view);
        if (AppState.get().appTheme == AppState.THEME_INK) {
            TxtUtils.setInkTextView(view);
        }

        if (recyclerView instanceof FastScrollRecyclerView) {
            swipeRefreshLayout = getActivity().findViewById(R.id.swipeRefreshLayout);

            ((FastScrollRecyclerView) recyclerView).setFastScrollStateChangeListener(new FastScrollStateChangeListener() {

                @Override
                public void onFastScrollStop() {
                    IMG.resumeRequests(getContext());
                    if (MainTabs2.isPullToRefreshEnable(getActivity(), swipeRefreshLayout)) {
                        if (swipeRefreshLayout != null) {
                            swipeRefreshLayout.setEnabled(true);
                        }
                    }
                }

                @Override
                public void onFastScrollStart() {
                    IMG.pauseRequests(getContext());
                    if (swipeRefreshLayout != null) {
                        swipeRefreshLayout.setEnabled(false);
                    }

                }
            });
        }

        if (recyclerView != null) {
            recyclerView.setAccessibilityDelegate(new View.AccessibilityDelegate());
        }
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

    private List<T> prepareDataInBackgroundSync() {
        return prepareDataInBackground();
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
        DocumentController.setNavBarTintColor(getActivity());
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
        //Safe.clearAll();
        try {
            Glide.with(LibreraApp.context).resumeRequests();
        } catch (Exception e) {
            LOG.e(e);
        }
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

    public void onTextRecive(String txt) {

    }

    public boolean isInProgress() {
        return MyProgressBar != null && MyProgressBar.getVisibility() == View.VISIBLE;
    }

    public void populate() {
        if (inProgress) {
            LOG.d("IN_PROGRESS");
            return;
        }

        final Runnable target = () -> {

            if (getActivity() == null) {
                return;
            }

            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (MyProgressBar != null) {
                        handler.postDelayed(new Runnable() {

                            @Override
                            public void run() {
                                MyProgressBar.setVisibility(View.VISIBLE);
                            }
                        }, 100);
                    }
                }
            });


            final List<T> result;
            try {
                inProgress = true;
                result = prepareDataInBackgroundSync();
            } finally {
                inProgress = false;

            }
            if (isDetached() || Apps.isDestroyed(getActivity())) {
                return;
            }

            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (isAdded()) {
                        if (MyProgressBar != null) {
                            handler.removeCallbacksAndMessages(null);
                            MyProgressBar.setVisibility(View.GONE);
                        }
                        try {
                            populateDataInUI(result);
                        } catch (Exception e) {
                            LOG.e(e);
                        }
                    }

                }
            });
        };
        AppsConfig.executorService.submit(target);
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
                    if (type == FileMetaAdapter.DISPLAY_TYPE_DIRECTORY || type == FileMetaAdapter.DISPLAY_TYPE_PLAYLIST) {
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

        } else if (Arrays.asList(AppState.MODE_PUBLICATION_DATE, AppState.MODE_PUBLISHER, AppState.MODE_AUTHORS, AppState.MODE_SERIES, AppState.MODE_GENRE, AppState.MODE_USER_TAGS, AppState.MODE_KEYWORDS, AppState.MODE_LANGUAGES).contains(mode)) {
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
