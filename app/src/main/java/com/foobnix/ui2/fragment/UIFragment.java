package com.foobnix.ui2.fragment;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver.OnGlobalLayoutListener;
import android.widget.ImageView;
import android.widget.LinearLayout;

import androidx.core.util.Pair;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.GridLayoutManager.SpanSizeLookup;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.bumptech.glide.Glide;
import com.foobnix.LibreraApp;
import com.foobnix.android.utils.Apps;
import com.foobnix.android.utils.Dips;
import com.foobnix.android.utils.LOG;
import com.foobnix.android.utils.TxtUtils;
import com.foobnix.model.AppState;
import com.foobnix.pdf.SlidingTabLayout;
import com.foobnix.pdf.info.AppsConfig;
import com.foobnix.pdf.info.IMG;
import com.foobnix.pdf.info.R;
import com.foobnix.pdf.info.TintUtil;
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

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.Arrays;
import java.util.List;

public abstract class UIFragment<T> extends Fragment {
    public static String INTENT_TINT_CHANGE = "INTENT_TINT_CHANGE";
    protected volatile MyProgressBar MyProgressBar;
    protected RecyclerView recyclerView;
    /** The header floated over this tab's page, and the room it stood in before it did. */
    private View floatingHeader;
    private int headerHeight;
    private int headerPaddingTop;
    private boolean headerSquared;
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
                // The tint is painted back on with the corners it was drawn with; the
                // header has to be squared off again.
                headerSquared = false;
                if (floatingHeader != null) {
                    floatingHeader.requestLayout();
                }
            }
        }
    };

    volatile boolean inProgress = false;

    public abstract Pair<Integer, Integer> getNameAndIconRes();

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        handler = new Handler(Looper.getMainLooper());
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        //TxtUtils.updateAllLinks(view);
        if (AppState.get().appTheme == AppState.THEME_INK) {
            TxtUtils.setInkTextView(view);
        }

        floatChromeOverPage(view);

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

    }

    /**
     * The library's own filter line is the screen rather than a title over it, so it keeps
     * the room it stands in; every other tab lets its header float.
     */
    public boolean hasFloatingHeader() {
        return true;
    }

    /**
     * With the tabs floating at the foot of the library, the tab's own header floats at the
     * top: the page runs the whole height of the screen and the chrome is drawn over it,
     * the header carrying up behind the status bar so the two read as one surface.
     * <p>
     * What that costs the page is the space it keeps clear at each end, and that is worked
     * out again on every layout pass: the status bar's height is only known once the view
     * is on screen, and what a tab stands above and below its list comes and goes.
     */
    private void floatChromeOverPage(final View root) {
        if (!(getActivity() instanceof MainTabs2)) {
            return;
        }
        // Only the tabs themselves: the settings are also the drawer, and a tab's screen is
        // borrowed by dialogs, and the floating chrome reaches over neither.
        if (getId() != R.id.pager || !(root instanceof LinearLayout)) {
            return;
        }
        final ViewGroup column = (ViewGroup) root;
        if (column.getChildCount() < 2) {
            return;
        }
        // The window runs the whole height of the screen now, so every header takes the
        // full width and carries the status bar itself rather than sitting under a strip.
        floatingHeader = column.getChildAt(0);
        headerHeight = floatingHeader.getLayoutParams().height;
        headerPaddingTop = floatingHeader.getPaddingTop();
        ViewGroup.MarginLayoutParams lp = (ViewGroup.MarginLayoutParams) floatingHeader.getLayoutParams();
        lp.leftMargin = lp.topMargin = lp.rightMargin = lp.bottomMargin = 0;
        floatingHeader.setLayoutParams(lp);
        // A header carrying the status bar has to be painted, or the bar's own clock and
        // icons are left over whatever the page happens to be showing.
        if (floatingHeader.getBackground() == null) {
            floatingHeader.setBackgroundColor(TintUtil.color);
        }
        // The same tint the tabs float in at the foot, kept back by the same amount, so the
        // two bars are one colour. The colour itself stays the tab's own to set and change.
        floatingHeader.getBackground().setAlpha(SlidingTabLayout.FLOATING_ALPHA);
        column.getViewTreeObserver().addOnGlobalLayoutListener(new OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                layOutAroundChrome(column);
            }
        });
    }

    /**
     * Each step writes only what has changed and then leaves, so the layout it asks for
     * settles instead of running on.
     */
    private void layOutAroundChrome(ViewGroup column) {
        if (getActivity() == null || floatingHeader == null || !liftHeaderOverStatusBar(column)) {
            return;
        }
        squareHeaderCorners();
        View scroller = recyclerView != null ? recyclerView : column.findViewById(R.id.scroll);
        View holder = childHolding(column, scroller);
        if (holder == null || scroller == null) {
            return;
        }
        // Everything the tab stands above its list floats over it: the list is pulled up to
        // the top of the page and keeps that much as padding it can scroll through.
        // At the foot the page runs on under the chrome. At the head it begins below it: the
        // bar is still drawn over the page, but nothing is read through it.
        ViewGroup.MarginLayoutParams lp = (ViewGroup.MarginLayoutParams) holder.getLayoutParams();
        int above = hasFloatingHeader() && SlidingTabLayout.isFloating()
                ? holder.getTop() - lp.topMargin : 0;
        if (lp.topMargin != -above) {
            lp.topMargin = -above;
            holder.setLayoutParams(lp);
            return;
        }
        // A row of views draws in the order it holds them, so the list - the later child -
        // would be drawn over the chrome it has just been pulled up behind. Lifting the
        // chrome puts it back on top, and gives it the same shadow the tabs cast below.
        for (int i = 0; above > 0 && i < column.indexOfChild(holder); i++) {
            View chrome = column.getChildAt(i);
            if (chrome.getElevation() < Dips.DP_4) {
                chrome.setElevation(Dips.DP_4);
            }
        }
        // At the foot the same again for the tabs, unless the tab stands something of its
        // own down there - then the space comes off the tab's edge, or the bar would sit on
        // top of it.
        boolean listReachesFoot = isLastShown(column, holder);
        int space = SlidingTabLayout.floatingSpace();
        int under = listReachesFoot ? space : 0;
        if (column.getPaddingBottom() != space - under) {
            column.setPadding(column.getPaddingLeft(),
                              column.getPaddingTop(),
                              column.getPaddingRight(),
                              space - under);
            return;
        }
        if (scroller.getPaddingTop() != above || scroller.getPaddingBottom() != under) {
            ((ViewGroup) scroller).setClipToPadding(false);
            scroller.setPadding(scroller.getPaddingLeft(), above, scroller.getPaddingRight(), under);
        }
    }

    /**
     * The header runs the full width of the screen and meets the strips above and below it.
     * Rounded, its corners let the page through in four small notches along those seams.
     */
    private void squareHeaderCorners() {
        if (headerSquared) {
            return;
        }
        try {
            Drawable face = floatingHeader.getBackground().getCurrent();
            if (face instanceof GradientDrawable) {
                ((GradientDrawable) face).setCornerRadius(0);
            }
            headerSquared = true;
        } catch (Exception e) {
            LOG.e(e);
        }
    }

    /**
     * The header runs up under whatever the library floats above the page. At the foot that
     * is the status bar alone, and the header carries its height; at the head the tabs are
     * up there too, already carrying it, and the header clears the strip they stand on.
     */
    private boolean liftHeaderOverStatusBar(View attached) {
        int statusBar;
        if (SlidingTabLayout.isFloating()) {
            WindowInsetsCompat insets = ViewCompat.getRootWindowInsets(attached);
            statusBar = insets == null ? 0 : insets.getInsets(WindowInsetsCompat.Type.statusBars()).top;
        } else {
            View tabs = getActivity().findViewById(R.id.imageParent1);
            statusBar = tabs == null ? 0 : tabs.getHeight();
        }
        if (floatingHeader.getPaddingTop() == headerPaddingTop + statusBar) {
            return true;
        }
        if (headerHeight > 0) {
            ViewGroup.LayoutParams lp = floatingHeader.getLayoutParams();
            lp.height = headerHeight + statusBar;
            floatingHeader.setLayoutParams(lp);
        }
        floatingHeader.setPadding(floatingHeader.getPaddingLeft(),
                                  headerPaddingTop + statusBar,
                                  floatingHeader.getPaddingRight(),
                                  floatingHeader.getPaddingBottom());
        return false;
    }

    /** The child of the column the list sits in, which may well be the list itself. */
    private static View childHolding(ViewGroup column, View view) {
        while (view != null && view.getParent() != column) {
            view = view.getParent() instanceof View ? (View) view.getParent() : null;
        }
        return view;
    }

    /** True when nothing a reader can see stands between the child and the foot of the tab. */
    private static boolean isLastShown(ViewGroup column, View child) {
        for (int i = column.indexOfChild(child) + 1; i < column.getChildCount(); i++) {
            if (column.getChildAt(i).getVisibility() != View.GONE) {
                return false;
            }
        }
        return true;
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

    public void onDoubleClick() {

    }

    public final void onSelectFragment() {
        if (getActivity() == null) {
            return;
        }
        if (listHash != TempHolder.listHash) {
            LOG.d("TempHolder.listHash", listHash, TempHolder.listHash);
            resetFragment();
            listHash = TempHolder.listHash;
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
        LOG.d("UI-Fragment","prepareDataInBackground",getString(getNameAndIconRes().first));
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
        if (getActivity() != null) {
            Intent intent = new Intent(INTENT_TINT_CHANGE);
            LocalBroadcastManager.getInstance(getActivity()).sendBroadcast(intent);
            DocumentController.setNavBarTintColor(getActivity());
        }
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        TempHolder.listHash++;
        onSelectFragment();
    }

    boolean isSecondTime = false;

    @Override
    public void onResume() {
        super.onResume();
        LOG.d("isSecondTime",isSecondTime);
        if (isSecondTime) {
            try {
                Glide.with(LibreraApp.context).resumeRequests();
            } catch (Exception e) {
                LOG.e(e);
            }

            notifyFragment();
        }

        if (getActivity() != null) {
            LocalBroadcastManager.getInstance(getActivity())
                                 .registerReceiver(broadcastReceiver, new IntentFilter(INTENT_TINT_CHANGE));
            EventBus.getDefault().register(this);
        }

        isSecondTime = true;

    }

    @Override
    public void onPause() {
        super.onPause();
        if (getActivity() != null) {
            LocalBroadcastManager.getInstance(getActivity()).unregisterReceiver(broadcastReceiver);
            EventBus.getDefault().unregister(this);
        }
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
        floatingHeader = null;
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
            if (isDetached() || Apps.isDestroyedActivity(getActivity())) {
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

    public void onGridList(int mode,
                           ImageView onGridlList,
                           final FileMetaAdapter searchAdapter,
                           AuthorsAdapter2 authorsAdapter) {
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
                    if (pos < 0 || pos >= searchAdapter.getItemCount()) {
                        return 1;
                    }

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

            searchAdapter.setAdapterType(mode == AppState.MODE_COVERS ? FileMetaAdapter.ADAPTER_COVERS : FileMetaAdapter.ADAPTER_GRID);
            recyclerView.setLayoutManager(mGridManager);
            recyclerView.setAdapter(searchAdapter);

        } else if (Arrays.asList(AppState.MODE_PUBLICATION_DATE, AppState.MODE_PUBLISHER, AppState.MODE_AUTHORS, AppState.MODE_SERIES, AppState.MODE_GENRE, AppState.MODE_USER_TAGS, AppState.MODE_KEYWORDS, AppState.MODE_LANGUAGES)
                         .contains(mode)) {
            RecyclerView.LayoutManager mLayoutManager = new LinearLayoutManager(getActivity());
            recyclerView.setLayoutManager(mLayoutManager);
            recyclerView.setAdapter(authorsAdapter);
        } else if (mode == AppState.MODE_LIST_COMPACT) {
            final int num = Math.max(2, Dips.screenWidthDP() / Dips.dpToPx(300));
            GridLayoutManager mGridManager = new GridLayoutManager(getActivity(), num);

            mGridManager.setSpanSizeLookup(new SpanSizeLookup() {

                @Override
                public int getSpanSize(int pos) {

                    if (pos < 0 || pos >= searchAdapter.getItemCount()) {
                        return 1;
                    }

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

            searchAdapter.setAdapterType(FileMetaAdapter.ADAPTER_LIST_COMPACT);
            recyclerView.setLayoutManager(mGridManager);
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
