package com.foobnix.ui2.fragment;

import static com.foobnix.drive.GFile.MIME_FOLDER;
import static com.foobnix.ui2.adapter.FileMetaAdapter.DISPLAY_TYPE_DIRECTORY;
import static com.foobnix.ui2.adapter.FileMetaAdapter.DISPLAY_TYPE_FILE;

import android.os.Bundle;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.TextView;

import androidx.core.util.Pair;
import androidx.recyclerview.widget.RecyclerView;

import com.foobnix.android.utils.LOG;
import com.foobnix.android.utils.ResultResponse;
import com.foobnix.android.utils.TxtUtils;
import com.foobnix.dao2.FileMeta;
import com.foobnix.drive.GFile;
import com.foobnix.model.AppSP;
import com.foobnix.model.AppState;
import com.foobnix.pdf.info.R;
import com.foobnix.pdf.info.TintUtil;
import com.foobnix.pdf.info.model.BookCSS;
import com.foobnix.pdf.info.view.AlertDialogs;
import com.foobnix.pdf.search.activity.msg.GDriveSycnEvent;
import com.foobnix.pdf.search.activity.msg.MessageSync;
import com.foobnix.ui2.adapter.FileMetaAdapter;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.api.services.drive.model.File;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

public class GoogleDriveFragment2 extends UIFragment<FileMeta> {
    public static final Pair<Integer, Integer> PAIR = new Pair<Integer, Integer>(R.string.clouds, R.drawable.glyphicons_544_cloud);

    @Override
    public Pair<Integer, Integer> getNameAndIconRes() {
        return PAIR;
    }

    View topPanel;
    TextView singIn, syncInfo, syncInfo2, syncHeader;

    CheckBox isEnableSync;
    FileMetaAdapter recentAdapter;

    String currentId;


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View inflate = inflater.inflate(R.layout.fragment_google_drive, container, false);


        topPanel = inflate.findViewById(R.id.topPanel);


        singIn = inflate.findViewById(R.id.signIn);
        syncInfo = inflate.findViewById(R.id.syncInfo);
        syncInfo2 = inflate.findViewById(R.id.syncInfo2);
        syncHeader = inflate.findViewById(R.id.syncHeader);

        recyclerView = (RecyclerView) inflate.findViewById(R.id.recyclerView);


        recentAdapter = new FileMetaAdapter();
        recentAdapter.tempValue = FileMetaAdapter.TEMP_VALUE_FOLDER_PATH;
        bindAdapter(recentAdapter);
        bindAuthorsSeriesAdapter(recentAdapter);


        isEnableSync = inflate.findViewById(R.id.isEnableSync);
        isEnableSync.setChecked(AppSP.get().isEnableSync);
        isEnableSync.setOnCheckedChangeListener((buttonView, isChecked) -> {
            AppSP.get().isEnableSync = isChecked;
            if (isChecked && getActivity() != null) {
                if (GoogleSignIn.getLastSignedInAccount(getActivity()) == null) {
                    GFile.init(getActivity());
                } else {
                    GFile.runSyncService(getActivity());
                }
            }
        });

        inflate.findViewById(R.id.isEnableSyncSettings).setOnClickListener(v -> {
            final CheckBox isSyncPullToRefresh = new CheckBox(getActivity());
            isSyncPullToRefresh.setText(R.string.pull_to_start_sync);
            isSyncPullToRefresh.setChecked(BookCSS.get().isSyncPullToRefresh);
            isSyncPullToRefresh.setOnCheckedChangeListener((buttonView, isChecked) -> BookCSS.get().isSyncPullToRefresh = isChecked);

            final CheckBox isSyncWifiOnly = new CheckBox(getActivity());
            isSyncWifiOnly.setText(R.string.wifi_sync_only);
            isSyncWifiOnly.setChecked(BookCSS.get().isSyncWifiOnly);
            isSyncWifiOnly.setOnCheckedChangeListener((buttonView, isChecked) -> BookCSS.get().isSyncWifiOnly = isChecked);

            final CheckBox isShowSyncWheel = new CheckBox(getActivity());
            isShowSyncWheel.setText(getString(R.string.animate_sync_progress));
            isShowSyncWheel.setChecked(BookCSS.get().isSyncAnimation);
            isShowSyncWheel.setOnCheckedChangeListener((buttonView, isChecked) -> BookCSS.get().isSyncAnimation = isChecked);

            AlertDialogs.showViewDialog(getActivity(), null, isSyncPullToRefresh, isSyncWifiOnly, isShowSyncWheel);
        });


        updateSyncInfo(null);

        recentAdapter = new FileMetaAdapter();
        recentAdapter.setOnItemClickListener(new ResultResponse<FileMeta>() {
            @Override
            public boolean onResultRecive(FileMeta result) {
                if (result.getCusType() == DISPLAY_TYPE_DIRECTORY) {
                    currentId = result.getPath();
                    populate();
                }
                return false;
            }
        });

        currentId = AppSP.get().syncRootID;

        onGridList(AppState.get().recentMode, null, recentAdapter, null);
        populate();

        TintUtil.setBackgroundFillColor(topPanel, TintUtil.color);
        TintUtil.setTintImageWithAlpha(inflate.findViewById(R.id.isEnableSyncSettings), TintUtil.color);
        return inflate;
    }

    @Subscribe
    public void updateSyncInfo(GDriveSycnEvent event) {
        String gdriveInfo = GFile.getDisplayInfo(getActivity());

        if (TxtUtils.isEmpty(gdriveInfo)) {
            AppSP.get().isEnableSync = false;
            syncInfo.setVisibility(View.GONE);
            singIn.setText(R.string.sign_in);
            TxtUtils.underlineTextView(singIn);
            singIn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    GFile.init(getActivity());
                    updateSyncInfo(null);
                }
            });
        } else {
            syncInfo.setVisibility(View.VISIBLE);
            syncInfo.setText(gdriveInfo);
            singIn.setText(R.string.sign_out);
            TxtUtils.underlineTextView(singIn);

            singIn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    AppSP.get().isEnableSync = false;
                    AppSP.get().syncRootID = "";
                    AppSP.get().syncTime = 0;
                    GFile.logout(getActivity());
                    updateSyncInfo(null);
                }
            });
        }

        isEnableSync.setChecked(AppSP.get().isEnableSync);

        onSync(null);


    }


    @Override
    public void populateDataInUI(List<FileMeta> items) {
        LOG.d("GDrive", items);
        if (recentAdapter != null) {
            recentAdapter.getItemsList().clear();
            recentAdapter.getItemsList().addAll(items);
            recentAdapter.notifyDataSetChanged();
        }

    }

    @Override
    public List<FileMeta> prepareDataInBackground() {
        try {
            if (TxtUtils.isEmpty(AppSP.get().syncRootID)) {
                return Collections.emptyList();
            }

            List<File> filesAll = GFile.getFiles(currentId);

            List<FileMeta> items = new ArrayList<FileMeta>();

            {
                FileMeta m = new FileMeta(AppSP.get().syncRootID);
                m.setCusType(DISPLAY_TYPE_DIRECTORY);
                m.setPathTxt("Home");
                items.add(m);
            }

            for (File f : filesAll) {
                if (f.getMimeType().equals(MIME_FOLDER)) {
                    FileMeta m = new FileMeta(f.getId());
                    m.setCusType(DISPLAY_TYPE_DIRECTORY);
                    m.setPathTxt(f.getName());
                    items.add(m);
                } else {

                    FileMeta m = new FileMeta(f.getId());
                    m.setCusType(DISPLAY_TYPE_FILE);
                    m.setTitle(f.getName());
                    m.setPathTxt(f.getName());
                    items.add(m);
                }

            }
            return items;

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onSync(MessageSync msg) {

        populate();

        if (AppSP.get().syncTime > 0) {

            final Date date = new Date(AppSP.get().syncTime);
            String format = "";
            if (DateUtils.isToday(AppSP.get().syncTime)) {
                format = getString(R.string.today) + " " + DateFormat.getTimeInstance().format(date);
            } else {
                format = DateFormat.getDateTimeInstance().format(date);
            }

            String status = AppSP.get().syncTimeStatus == MessageSync.STATE_SUCCESS ? getString(R.string.success) : getString(R.string.fail);
            if (AppSP.get().syncTimeStatus == MessageSync.STATE_VISIBLE) {
                status = "...";
            }

            syncInfo2.setText(format + " - " + status);
            syncInfo2.setVisibility(View.VISIBLE);
        } else {
            syncInfo2.setText("");
            syncInfo2.setVisibility(View.GONE);
            syncHeader.setText(R.string.clouds);

        }

    }


    @Override
    public void onTintChanged() {
        TintUtil.setBackgroundFillColor(topPanel, TintUtil.color);

    }

    @Override
    public void notifyFragment() {
        //populate();

    }

    @Override
    public void resetFragment() {
        populate();
    }

}
