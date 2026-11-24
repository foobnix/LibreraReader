package com.foobnix.ui2.fragment;

import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.RippleDrawable;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.format.DateUtils;
import android.text.style.UnderlineSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.LinearInterpolator;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.core.util.Pair;
import androidx.recyclerview.widget.RecyclerView;

import com.foobnix.android.utils.Dips;
import com.foobnix.android.utils.TxtUtils;
import com.foobnix.dao2.FileMeta;
import com.foobnix.drive.GFile;
import com.foobnix.drive.GoogleAuth;
import com.foobnix.model.AppData;
import com.foobnix.model.AppProfile;
import com.foobnix.model.AppSP;
import com.foobnix.model.AppState;
import com.foobnix.pdf.info.FileMetaComparators;
import com.foobnix.pdf.info.R;
import com.foobnix.pdf.info.TintUtil;
import com.foobnix.pdf.info.model.BookCSS;
import com.foobnix.pdf.info.view.Dialogs;
import com.foobnix.pdf.info.view.MyPopupMenu;
import com.foobnix.pdf.search.activity.msg.GDriveSycnEvent;
import com.foobnix.pdf.search.activity.msg.MessageSync;
import com.foobnix.ui2.MainTabs2;
import com.foobnix.ui2.adapter.FileMetaAdapter;
import org.ebookdroid.common.settings.books.SharedBooks;
import com.foobnix.work.SynctornizatoinWorker;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.text.DateFormat;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

/**
 * The Sync tab: the books in the synced folder, with where the sync stands, when it last ran and
 * a button to run it now. The band holds the order, the grid or list, and the sync's settings.
 */
public class GoogleDriveFragment2 extends UIFragment<FileMeta> {
    public static final Pair<Integer, Integer> PAIR = new Pair<Integer, Integer>(R.string.sync,
                                                                                 R.drawable.ic_cloud_filled);

    // The status stands on the band's colour, so the light shades read on it
    private static final int COLOR_SYNCED = 0xFF8BE08F;
    private static final int COLOR_FAILED = 0xFFFF9E9E;
    private static final int COLOR_SYNCED_INK = 0xFF1B5E20;
    private static final int COLOR_FAILED_INK = 0xFFB71C1C;

    @Override public Pair<Integer, Integer> getNameAndIconRes() {
        return PAIR;
    }

    View topPanel, syncNow;
    TextView syncHeader, syncInfo, syncInfo2, syncNowText, syncEmpty;
    ImageView onListGrid, onSort, onMenu, syncNowIcon;
    FileMetaAdapter syncAdapter;
    ObjectAnimator spin;
    int count = 0;

    @Override public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View inflate = inflater.inflate(R.layout.fragment_google_drive, container, false);

        topPanel = inflate.findViewById(R.id.topPanel);
        syncHeader = inflate.findViewById(R.id.syncHeader);
        syncInfo = inflate.findViewById(R.id.syncInfo);
        syncInfo2 = inflate.findViewById(R.id.syncInfo2);
        syncNow = inflate.findViewById(R.id.syncNow);
        syncNowIcon = inflate.findViewById(R.id.syncNowIcon);
        syncNowText = inflate.findViewById(R.id.syncNowText);
        syncEmpty = inflate.findViewById(R.id.syncEmpty);
        onListGrid = inflate.findViewById(R.id.onListGrid);
        onSort = inflate.findViewById(R.id.onSort);
        onMenu = inflate.findViewById(R.id.onMenu);

        recyclerView = (RecyclerView) inflate.findViewById(R.id.recyclerView);

        syncAdapter = new FileMetaAdapter();
        syncAdapter.tempValue = FileMetaAdapter.TEMP_VALUE_FOLDER_PATH;
        bindAdapter(syncAdapter);
        bindAuthorsSeriesAdapter(syncAdapter);

        onListGrid.setOnClickListener(v -> showViewMenu());
        onSort.setOnClickListener(v -> showSortMenu());
        onMenu.setOnClickListener(v -> showSettingsMenu());
        // The profile in the header: tapped, the profiles to switch to
        syncHeader.setOnClickListener(v -> PrefFragment2.showProfileMenu(getActivity(), v));
        GradientDrawable pressMask = new GradientDrawable();
        pressMask.setColor(Color.WHITE);
        pressMask.setCornerRadius(Dips.dpToPx(18));
        syncHeader.setBackground(new RippleDrawable(ColorStateList.valueOf(0x3CFFFFFF), null, pressMask));

        // The status opens the sync log, where a running sync shows how far it has got
        inflate.findViewById(R.id.syncStatus).setOnClickListener(v -> Dialogs.showSyncLOGDialog(getActivity()));

        syncNow.setOnClickListener(v -> onSyncNow());
        syncNowIcon.setImageTintList(ColorStateList.valueOf(Color.WHITE));
        spin = ObjectAnimator.ofFloat(syncNowIcon, View.ROTATION, 0f, 360f);
        spin.setDuration(1000);
        spin.setRepeatCount(ValueAnimator.INFINITE);
        spin.setInterpolator(new LinearInterpolator());

        onTintChanged();
        onGridList();
        updateSyncInfo(null);
        return inflate;
    }

    private void onSyncNow() {
        if (getActivity() == null) {
            return;
        }
        if (!isSignedIn()) {
            GFile.init(getActivity());
            return;
        }
        if (!AppSP.get().isEnableSync) {
            AppSP.get().isEnableSync = true;
            AppSP.get().save();
        }
        GFile.runSyncService(getActivity(), true);
        updatePullToRefresh();
    }

    /** The pull that starts a sync follows the settings at once, not at the next start. */
    private void updatePullToRefresh() {
        if (getActivity() instanceof MainTabs2) {
            ((MainTabs2) getActivity()).updatePullToRefresh();
        }
    }

    private boolean isSignedIn() {
        return getActivity() != null && TxtUtils.isNotEmpty(GFile.getDisplayInfo(getActivity()));
    }

    public void onGridList() {
        onGridList(AppState.get().syncMode, onListGrid, syncAdapter, null);
    }

    private void showViewMenu() {
        MyPopupMenu p = new MyPopupMenu(getActivity(), onListGrid);
        final List<Integer> names = Arrays.asList(R.string.list, R.string.compact, R.string.grid, R.string.cover);
        final List<Integer> icons = Arrays.asList(R.drawable.my_glyphicons_114_paragraph_justify,
                                                  R.drawable.my_glyphicons_114_justify_compact,
                                                  R.drawable.glyphicons_157_thumbnails,
                                                  R.drawable.glyphicons_158_thumbnails_small);
        final List<Integer> modes = Arrays.asList(AppState.MODE_LIST, AppState.MODE_LIST_COMPACT,
                                                  AppState.MODE_GRID, AppState.MODE_COVERS);
        for (int i = 0; i < names.size(); i++) {
            final int mode = modes.get(i);
            p.getMenu().add(names.get(i)).setIcon(icons.get(i)).setOnMenuItemClickListener(item -> {
                AppState.get().syncMode = mode;
                onGridList();
                return false;
            });
        }
        p.show();
    }

    private void showSortMenu() {
        MyPopupMenu p = new MyPopupMenu(getActivity(), onSort);
        final List<Integer> names = Arrays.asList(R.string.by_sync_date, R.string.by_title, R.string.by_author,
                                                  R.string.reading_progress);
        final List<Integer> sorts = Arrays.asList(AppState.SYNC_SORT_DATE, AppState.SYNC_SORT_TITLE,
                                                  AppState.SYNC_SORT_AUTHOR, AppState.SYNC_SORT_PROGRESS);
        for (int i = 0; i < names.size(); i++) {
            final int sort = sorts.get(i);
            final String name = (AppState.get().syncSortBy == sort ? "✓ " : "") + getString(names.get(i));
            p.getMenu().add(name).setOnMenuItemClickListener(item -> {
                AppState.get().syncSortBy = sort;
                populate();
                return false;
            });
        }
        p.getMenu().addCheckbox(getString(R.string.ascending), AppState.get().isSyncSortAsc, (b, isChecked) -> {
            AppState.get().isSyncSortAsc = isChecked;
            populate();
        });
        p.show();
    }

    private void showSettingsMenu() {
        MyPopupMenu p = new MyPopupMenu(getActivity(), onMenu);
        final boolean signedIn = isSignedIn();
        if (signedIn) {
            p.getMenu().addCheckbox(getString(R.string.enable), AppSP.get().isEnableSync, (b, isChecked) -> {
                AppSP.get().isEnableSync = isChecked;
                AppSP.get().save();
                if (isChecked && getActivity() != null) {
                    GFile.runSyncService(getActivity());
                }
                updatePullToRefresh();
                updateSyncInfo(null);
            });
        }
        p.getMenu().addCheckbox(getString(R.string.pull_to_start_sync), BookCSS.get().isSyncPullToRefresh,
                                (b, isChecked) -> {
                                    BookCSS.get().isSyncPullToRefresh = isChecked;
                                    updatePullToRefresh();
                                });
        p.getMenu().addCheckbox(getString(R.string.wifi_sync_only), BookCSS.get().isSyncWifiOnly,
                                (b, isChecked) -> BookCSS.get().isSyncWifiOnly = isChecked);
        if (signedIn) {
            p.getMenu().add(R.string.sign_out).setIcon(R.drawable.ic_cloud_filled).setOnMenuItemClickListener(item -> {
                AppSP.get().isEnableSync = false;
                AppSP.get().syncRootID = "";
                AppSP.get().syncTime = 0;
                GFile.logout(getActivity());
                updatePullToRefresh();
                updateSyncInfo(null);
                return false;
            });
        } else {
            p.getMenu().add(R.string.sign_in).setIcon(R.drawable.ic_cloud_filled).setOnMenuItemClickListener(item -> {
                GFile.init(getActivity());
                return false;
            });
        }
        p.show();
    }

    @Subscribe(threadMode = ThreadMode.MAIN) public void updateSyncInfo(GDriveSycnEvent event) {
        if (getActivity() == null || syncInfo == null) {
            return;
        }
        // The account is not shown: the status alone, on its two lines
        syncInfo.setVisibility(View.GONE);
        if (!isSignedIn()) {
            AppSP.get().isEnableSync = false;
        }
        onSync(null);
    }

    @Override public List<FileMeta> prepareDataInBackground() {
        final List<FileMeta> books = AppData.get().getAllSyncBooks();
        Comparator<FileMeta> by;
        switch (AppState.get().syncSortBy) {
            case AppState.SYNC_SORT_TITLE:
                by = FileMetaComparators.BR_BY_TITLE;
                break;
            case AppState.SYNC_SORT_AUTHOR:
                by = FileMetaComparators.BR_BY_AUTHOR;
                break;
            case AppState.SYNC_SORT_PROGRESS:
                by = (a, b) -> Float.compare(progress(a), progress(b));
                break;
            default:
                // Keys worked out once: the comparator runs many times per book
                final java.util.Map<String, Long> when = new java.util.HashMap<>();
                for (FileMeta m : books) {
                    when.put(m.getPath(), syncDate(m.getPath()));
                }
                by = (a, b) -> Long.compare(when.get(a.getPath()), when.get(b.getPath()));
        }
        try {
            Collections.sort(books, by);
        } catch (Exception e) {
            // A comparator that is not consistent leaves the order as the folder gave it
        }
        if (!AppState.get().isSyncSortAsc) {
            Collections.reverse(books);
        }
        return books;
    }

    /**
     * When the book last changed through the sync: read on any device (the newest of their
     * progress records), or the file itself put into the synced folder or changed there.
     */
    private static long syncDate(String path) {
        long time = 0;
        try {
            time = GFile.getLastModified(new java.io.File(path));
        } catch (Exception e) {
            // Drive's times not loaded yet: the file's own
            time = new java.io.File(path).lastModified();
        }
        try {
            final long read = SharedBooks.load(path).t;
            // A device whose clock runs far ahead would hold its book on top for good
            if (read <= System.currentTimeMillis() + SharedBooks.CLOCK_TOLERANCE) {
                time = Math.max(time, read);
            }
        } catch (Exception e) {
            // no progress record
        }
        return time;
    }

    private static float progress(FileMeta m) {
        final Float p = m.getIsRecentProgress();
        return p == null ? 0f : p;
    }

    @Override public void populateDataInUI(List<FileMeta> items) {
        if (syncAdapter != null) {
            syncAdapter.getItemsList().clear();
            syncAdapter.getItemsList().addAll(items);
            syncAdapter.notifyDataSetChanged();
        }
        count = items.size();
        if (syncHeader != null) {
            // The profile too: progress is kept per profile, so two devices only share it on
            // the same one
            final String profile = AppProfile.getCurrent();
            final String head = getString(R.string.sync) + "  " + count;
            if (TxtUtils.isEmpty(profile)) {
                syncHeader.setText(head);
            } else {
                // Underlined: it opens the profiles to switch to
                final SpannableString text = new SpannableString(head + " (" + profile + ")");
                text.setSpan(new UnderlineSpan(), head.length() + 2, head.length() + 2 + profile.length(), 0);
                syncHeader.setText(text);
            }
        }
        if (syncEmpty != null) {
            syncEmpty.setVisibility(items.isEmpty() ? View.VISIBLE : View.GONE);
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN) public void onSync(MessageSync msg) {
        if (getActivity() == null || syncInfo2 == null) {
            return;
        }
        final boolean signedIn = isSignedIn();
        final boolean running = signedIn && (msg != null ? msg.state == MessageSync.STATE_VISIBLE :
                                             SynctornizatoinWorker.isRunning);

        syncNowText.setText(signedIn ? R.string.sync : R.string.sign_in);
        syncNowIcon.setVisibility(signedIn ? View.VISIBLE : View.GONE);
        syncNow.setAlpha(running ? 0.6f : 1f);
        if (running) {
            if (!spin.isStarted()) {
                spin.start();
            }
        } else {
            spin.cancel();
            syncNowIcon.setRotation(0);
        }

        syncInfo2.setTextColor(onChrome());

        if (!signedIn) {
            syncInfo2.setText(R.string.sign_in);
            syncInfo2.setVisibility(View.GONE);
        } else if (running) {
            syncInfo2.setVisibility(View.VISIBLE);
            syncInfo2.setText(R.string.sync_status_syncing);
        } else if (AppSP.get().syncTime <= 0) {
            syncInfo2.setVisibility(View.VISIBLE);
            syncInfo2.setText(R.string.sync_status_never);
        } else if (AppSP.get().syncTimeStatus == MessageSync.STATE_VISIBLE) {
            // Saved mid-sync and never finished (the app was closed): only when it last ran
            syncInfo2.setVisibility(View.VISIBLE);
            syncInfo2.setText(syncTimeText(AppSP.get().syncTime));
        } else {
            syncInfo2.setVisibility(View.VISIBLE);
            final boolean ok = AppSP.get().syncTimeStatus == MessageSync.STATE_SUCCESS;
            final int color = isInk() ? (ok ? COLOR_SYNCED_INK : COLOR_FAILED_INK) :
                              (ok ? COLOR_SYNCED : COLOR_FAILED);
            syncInfo2.setTextColor(color);
            syncInfo2.setText(getString(ok ? R.string.sync_status_synced : R.string.sync_error) + "\n" +
                              syncTimeText(AppSP.get().syncTime));
        }

        if (msg == null || msg.state != MessageSync.STATE_VISIBLE) {
            populate();
        }
    }

    private String syncTimeText(long time) {
        final Date date = new Date(time);
        if (DateUtils.isToday(time)) {
            return getString(R.string.today) + " " + DateFormat.getTimeInstance(DateFormat.SHORT).format(date);
        }
        return DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(date);
    }

    private static boolean isInk() {
        return AppState.get().appTheme == AppState.THEME_INK;
    }

    /** White on the band's colour; on Ink the band is paper, and the words are ink. */
    private static int onChrome() {
        return isInk() ? Color.BLACK : Color.WHITE;
    }

    /**
     * The Sync button as LibreraX draws it: a pill a shade lighter than the band it stands on,
     * with white on it; on Ink a black pill on the white band.
     */
    private void paintSyncButton() {
        if (syncNow == null) {
            return;
        }
        GradientDrawable pill = new GradientDrawable();
        pill.setColor(isInk() ? Color.BLACK : 0x38FFFFFF);
        pill.setCornerRadius(Dips.dpToPx(50));
        GradientDrawable mask = new GradientDrawable();
        mask.setColor(Color.WHITE);
        mask.setCornerRadius(Dips.dpToPx(50));
        syncNow.setBackground(new RippleDrawable(ColorStateList.valueOf(0x3CFFFFFF), pill, mask));
    }

    @Override public void onTintChanged() {
        TintUtil.setBackgroundFillColor(topPanel, TintUtil.color);
        paintSyncButton();
        if (syncInfo != null) {
            syncInfo.setTextColor(onChrome());
            syncInfo.setAlpha(0.8f);
        }
        if (syncEmpty != null) {
            syncEmpty.setTextColor(onChrome());
        }
    }

    /**
     * A sync that started or ended while the tab was not listening (the library reopens after
     * a sign-in) is picked up when the tab is back.
     */
    @Override public void onResume() {
        super.onResume();
        onSync(null);
    }

    @Override public void onDestroyView() {
        if (spin != null) {
            spin.cancel();
        }
        super.onDestroyView();
    }

    @Override public void notifyFragment() {
        populate();
    }

    @Override public void resetFragment() {
        onGridList();
        populate();
    }
}
