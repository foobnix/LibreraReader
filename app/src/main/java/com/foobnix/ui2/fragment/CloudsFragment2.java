package com.foobnix.ui2.fragment;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.MenuItem.OnMenuItemClickListener;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.core.util.Pair;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.recyclerview.widget.RecyclerView;

import com.foobnix.android.utils.LOG;
import com.foobnix.android.utils.TxtUtils;
import com.foobnix.dao2.FileMeta;
import com.foobnix.model.AppState;
import com.foobnix.pdf.info.Clouds;
import com.foobnix.pdf.info.FileMetaComparators;
import com.foobnix.pdf.info.R;
import com.foobnix.pdf.info.TintUtil;
import com.foobnix.pdf.info.model.BookCSS;
import com.foobnix.pdf.info.view.MyPopupMenu;
import com.foobnix.pdf.info.wrapper.PopupHelper;
import com.foobnix.pdf.info.wrapper.UITab;
import com.foobnix.pdf.search.activity.msg.MessageSyncFinish;
import com.foobnix.pdf.search.activity.msg.MessageSyncUpdateList;
import com.foobnix.pdf.search.activity.msg.OpenDirMessage;
import com.foobnix.ui2.BooksService;
import com.foobnix.ui2.MainTabs2;
import com.foobnix.ui2.adapter.FileMetaAdapter;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class CloudsFragment2 extends UIFragment<FileMeta> {
    public static final Pair<Integer, Integer> PAIR = new Pair<Integer, Integer>(R.string.clouds, R.drawable.glyphicons_2_cloud);
    FileMetaAdapter metaAdapter;
    ImageView onListGrid;
    View panelRecent;
    private ImageView imageDropbox;
    private ImageView imageGDrive;
    private ImageView imageOneDrive;
    private ImageView onRefresh;
    private ImageView isShowCloudsLine;
    private View cloudsLayout;

    @Override
    public Pair<Integer, Integer> getNameAndIconRes() {
        return PAIR;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_clouds, container, false);

        recyclerView = (RecyclerView) view.findViewById(R.id.recyclerView);
        panelRecent = view.findViewById(R.id.panelRecent);

        onListGrid = (ImageView) view.findViewById(R.id.onListGrid);
        onListGrid.setOnClickListener(v -> popupMenu(onListGrid));

        metaAdapter = new FileMetaAdapter();
        metaAdapter.tempValue = FileMetaAdapter.TEMP_VALUE_FOLDER_PATH;
        bindAdapter(metaAdapter);
        bindAuthorsSeriesAdapter(metaAdapter);

        onGridList();
        populate();

        MyProgressBar = view.findViewById(R.id.MyProgressBarClouds);
        MyProgressBar.setVisibility(View.GONE);
        TintUtil.setDrawableTint(MyProgressBar.getIndeterminateDrawable().getCurrent(), Color.WHITE);

        onRefresh = view.findViewById(R.id.onRefreshDropbox);
        onRefresh.setOnClickListener(v -> {
            MyProgressBar.setVisibility(View.VISIBLE);
            BooksService.startForeground(getActivity(), BooksService.ACTION_SYNC_DROPBOX);

        });
        cloudsLayout = view.findViewById(R.id.cloudsLayout);
        isShowCloudsLine = view.findViewById(R.id.isShowCloudsLine);
        isShowCloudsLine.setImageResource(AppState.get().isShowCloudsLine ? R.drawable.glyphicons_602_chevron_down : R.drawable.glyphicons_601_chevron_up);
        cloudsLayout.setVisibility(TxtUtils.visibleIf(AppState.get().isShowCloudsLine));
        isShowCloudsLine.setOnClickListener(v -> {
            AppState.get().isShowCloudsLine = !AppState.get().isShowCloudsLine;
            isShowCloudsLine.setImageResource(AppState.get().isShowCloudsLine ? R.drawable.glyphicons_602_chevron_down : R.drawable.glyphicons_601_chevron_up);
            cloudsLayout.setVisibility(TxtUtils.visibleIf(AppState.get().isShowCloudsLine));
        });

        TintUtil.setBackgroundFillColor(panelRecent, TintUtil.color);

        imageDropbox = view.findViewById(R.id.imageDropbox);
        imageGDrive = view.findViewById(R.id.imageGDrive);
        imageOneDrive = view.findViewById(R.id.imageOneDrive);

        View dropbox = view.findViewById(R.id.dropbox);


        dropbox.setOnClickListener(v -> {
            final boolean isDropbox = Clouds.get().isDropbox();
            Clouds.get().loginToDropbox(getActivity(), () -> {
                if (isDropbox) {
                    Intent intent = new Intent(UIFragment.INTENT_TINT_CHANGE)//
                            .putExtra(MainTabs2.EXTRA_PAGE_NUMBER, UITab.getCurrentTabIndex(UITab.BrowseFragment));//
                    LocalBroadcastManager.getInstance(requireContext()).sendBroadcast(intent);

                    EventBus.getDefault().post(new OpenDirMessage(Clouds.PREFIX_CLOUD_DROPBOX + "/"));
                } else {
                    Toast.makeText(getActivity(), R.string.success, Toast.LENGTH_SHORT).show();
                    BooksService.startForeground(getActivity(), BooksService.ACTION_SYNC_DROPBOX);

                    MyProgressBar.setVisibility(View.VISIBLE);
                }
                updateImages();
            });
        });

        final View gdrive = view.findViewById(R.id.gdrive);
        gdrive.setOnClickListener(v -> {
            final boolean isDrive = Clouds.get().isGoogleDrive();
            Clouds.get().loginToGoogleDrive(getActivity(), () -> {
                if (getActivity() == null) {
                    return;
                }
                if (isDrive) {
                    Intent intent = new Intent(UIFragment.INTENT_TINT_CHANGE)//
                            .putExtra(MainTabs2.EXTRA_PAGE_NUMBER, UITab.getCurrentTabIndex(UITab.BrowseFragment));//
                    LocalBroadcastManager.getInstance(getActivity()).sendBroadcast(intent);

                    EventBus.getDefault().post(new OpenDirMessage(Clouds.PREFIX_CLOUD_GDRIVE + "/"));

                } else {
                    MyProgressBar.setVisibility(View.VISIBLE);
                    Toast.makeText(getActivity(), R.string.success, Toast.LENGTH_SHORT).show();
                }
                updateImages();
            });
        });

        final View onedrive = view.findViewById(R.id.oneDrive);
        onedrive.setOnClickListener(v -> {
            final boolean isDrive = Clouds.get().isOneDrive();

            Clouds.get().loginToOneDrive(getActivity(), () -> {
                if (isDrive) {
                    Intent intent = new Intent(UIFragment.INTENT_TINT_CHANGE)//
                            .putExtra(MainTabs2.EXTRA_PAGE_NUMBER, UITab.getCurrentTabIndex(UITab.BrowseFragment));//
                    LocalBroadcastManager.getInstance(getActivity()).sendBroadcast(intent);

                    EventBus.getDefault().post(new OpenDirMessage(Clouds.PREFIX_CLOUD_ONEDRIVE + "/"));
                } else {
                    MyProgressBar.setVisibility(View.VISIBLE);
                    Toast.makeText(getActivity(), R.string.success, Toast.LENGTH_SHORT).show();
                }
                updateImages();
            });
        });

        if (AppState.get().appTheme == AppState.THEME_DARK_OLED) {
            dropbox.setBackgroundResource(R.drawable.bg_border_ltgray_oled);
            gdrive.setBackgroundResource(R.drawable.bg_border_ltgray_oled);
            onedrive.setBackgroundResource(R.drawable.bg_border_ltgray_oled);
        }

        return view;
    }

    public void updateImages() {
        if (Clouds.get().isDropbox()) {
            TintUtil.setNoTintImage(imageDropbox);
        } else {
            TintUtil.setTintImageNoAlpha(imageDropbox, Color.LTGRAY);
        }

        if (Clouds.get().isGoogleDrive()) {
            TintUtil.setNoTintImage(imageGDrive);
        } else {
            TintUtil.setTintImageNoAlpha(imageGDrive, Color.LTGRAY);
        }

        if (Clouds.get().isOneDrive()) {
            TintUtil.setNoTintImage(imageOneDrive);
        } else {
            TintUtil.setTintImageNoAlpha(imageOneDrive, Color.LTGRAY);
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void notifyUpdateFragment(MessageSyncFinish event) {
        MyProgressBar.setVisibility(View.GONE);
        populate();
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void notifyUpdateList(MessageSyncUpdateList event) {
        populate();
        LOG.d("notifyUpdateList populate MessageSyncUpdateList");
    }

    @Override
    public void onTintChanged() {
        TintUtil.setBackgroundFillColor(panelRecent, TintUtil.color);
    }

    public boolean onBackAction() {
        return false;
    }

    public static List<FileMeta> getCloudFiles(String path, String prefix) {

        List<FileMeta> res = new ArrayList<FileMeta>();

        File root = new File(path);
        File[] listFiles = root.listFiles();
        if (listFiles == null) {
            return new ArrayList<FileMeta>();
        }

        for (File file : listFiles) {
            if (file.isDirectory()) {
                continue;
            }
            //FileMeta meta = FileMetaCore.createMetaIfNeed(file.getPath(), true);
            FileMeta meta = new FileMeta(file.getPath());
            //meta.setPath(prefix + "/" + file.getName());
            res.add(meta);
        }
        Collections.sort(res, FileMetaComparators.BY_DATE);
        Collections.reverse(res);

        return res;

    }

    @Override
    public List<FileMeta> prepareDataInBackground() {
        List<FileMeta> res = new ArrayList<FileMeta>();

        if (Clouds.get().isDropbox()) {
            String title = getActivity().getString(R.string.dropbox) + " (" + Clouds.get().dropboxSpace + ")";
            res.add(metaTitle(title));
            res.addAll(getCloudFiles(BookCSS.get().syncDropboxPath, Clouds.PREFIX_CLOUD_DROPBOX + Clouds.LIBRERA_SYNC_ONLINE_FOLDER));
        }

        if (Clouds.get().isGoogleDrive()) {
            String title = getActivity().getString(R.string.google_drive) + " (" + Clouds.get().googleSpace + ")";
            res.add(metaTitle(title));
            res.addAll(getCloudFiles(BookCSS.get().syncGdrivePath, Clouds.PREFIX_CLOUD_GDRIVE + Clouds.LIBRERA_SYNC_ONLINE_FOLDER));
        }

        if (Clouds.get().isOneDrive()) {
            String title = getActivity().getString(R.string.one_drive) + " (" + Clouds.get().oneDriveSpace + ")";
            res.add(metaTitle(title));
            res.addAll(getCloudFiles(BookCSS.get().syncOneDrivePath, Clouds.PREFIX_CLOUD_ONEDRIVE + Clouds.LIBRERA_SYNC_ONLINE_FOLDER));
        }

        LOG.d("prepareDataInBackground");

        return res;
    }

    private FileMeta metaTitle(String name) {
        FileMeta meta = new FileMeta();
        meta.setCusType(FileMetaAdapter.DISPALY_TYPE_LAYOUT_TITLE_DIVIDER);
        meta.setTitle(name);
        return meta;
    }

    @Override
    public void populateDataInUI(List<FileMeta> items) {
        if (metaAdapter != null) {
            metaAdapter.getItemsList().clear();
            metaAdapter.getItemsList().addAll(items);
            metaAdapter.notifyDataSetChanged();
        }
        updateImages();
    }

    public void onGridList() {
        LOG.d("onGridList");
        onGridList(AppState.get().cloudMode, onListGrid, metaAdapter, null);
    }

    private void popupMenu(final ImageView onGridList) {
        MyPopupMenu p = new MyPopupMenu(getActivity(), onGridList);
        PopupHelper.addPROIcon(p, getActivity());

        List<Integer> names = Arrays.asList(R.string.list, R.string.compact, R.string.grid, R.string.cover);
        final List<Integer> icons = Arrays.asList(R.drawable.glyphicons_114_justify, R.drawable.glyphicons_114_justify_compact, R.drawable.glyphicons_156_show_big_thumbnails, R.drawable.glyphicons_157_show_thumbnails);
        final List<Integer> actions = Arrays.asList(AppState.MODE_LIST, AppState.MODE_LIST_COMPACT, AppState.MODE_GRID, AppState.MODE_COVERS);

        for (int i = 0; i < names.size(); i++) {
            final int index = i;
            p.getMenu().add(names.get(i)).setIcon(icons.get(i)).setOnMenuItemClickListener(item -> {
                AppState.get().cloudMode = actions.get(index);
                onGridList.setImageResource(icons.get(index));
                onGridList();
                return false;
            });
        }

        p.show();
    }

    @Override
    public void notifyFragment() {
        populate();
    }

    @Override
    public void resetFragment() {
        onGridList();
        populate();
    }

}
