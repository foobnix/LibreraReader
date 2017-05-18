package com.foobnix.ui2.fragment;

import java.util.ArrayList;
import java.util.List;

import com.foobnix.android.utils.Dips;
import com.foobnix.android.utils.ResultResponse;
import com.foobnix.dao2.FileMeta;
import com.foobnix.pdf.info.R;
import com.foobnix.pdf.info.wrapper.AppState;
import com.foobnix.ui2.AppDB;
import com.foobnix.ui2.adapter.FileMetaAdapter;

import android.os.Bundle;
import android.support.v4.util.Pair;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

public class RecentFragment2 extends UIFragment<FileMeta> {
    FileMetaAdapter recentAdapter;
    private RecyclerView recyclerView;

    @Override
    public Pair<Integer, Integer> getNameAndIconRes() {
        return new Pair<Integer, Integer>(R.string.recent, R.drawable.glyphicons_72_book);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_recent, container, false);

        recyclerView = (RecyclerView) view.findViewById(R.id.recyclerView);

        recentAdapter = new FileMetaAdapter();
        recentAdapter.tempValue = FileMetaAdapter.TEMP_VALUE_FOLDER_PATH;
        bindAdapter(recentAdapter);
        bindAuthorsSeriesAdapter(recentAdapter);

        recentAdapter.setOnDeleteClickListener(onDeleteRecentClick);

        recentAdapter.setClearAllRecent(clearAllRecent);
        recentAdapter.setClearAllStars(clearAllStars);

        AppState.get().recentMode = AppState.MODE_LIST;
        onGridList();

        populate();

        return view;
    }

    @Override
    public void onTintChanged() {
        recentAdapter.notifyDataSetChanged();
    }

    ResultResponse<FileMeta> onDeleteRecentClick = new ResultResponse<FileMeta>() {

        @Override
        public boolean onResultRecive(FileMeta result) {
            result.setIsRecent(false);
            AppDB.get().update(result);
            populate();
            return false;
        }
    };

    Runnable clearAllRecent = new Runnable() {

        @Override
        public void run() {
            AppDB.get().clearAllRecent();
            populate();
        }
    };

    Runnable clearAllStars = new Runnable() {
        @Override
        public void run() {
            AppDB.get().clearAllStars();
            populate();
        }
    };

    public boolean onBackAction() {
        return false;
    }

    @Override
    public void onSelectFragment() {
        populate();
    }

    @Override
    public List<FileMeta> prepareDataInBackground() {
        List<FileMeta> all = new ArrayList<FileMeta>();

        all.addAll(AppDB.get().getStarsFolder());

        FileMeta stars = new FileMeta();
        stars.setCusType(FileMetaAdapter.DISPALY_TYPE_LAYOUT_STARS);
        all.add(stars);

        all.addAll(AppDB.get().getAllRecentWithProgress());

        return all;
    }

    @Override
    public void populateDataInUI(List<FileMeta> items) {
        recentAdapter.getItemsList().clear();
        recentAdapter.getItemsList().addAll(items);
        recentAdapter.notifyDataSetChanged();
    }

    public void onGridList() {

        if (AppState.get().recentMode == AppState.MODE_LIST) {
            RecyclerView.LayoutManager mLayoutManager = new LinearLayoutManager(getActivity());
            recyclerView.setLayoutManager(mLayoutManager);
            recentAdapter.setAdapterType(FileMetaAdapter.ADAPTER_LIST);
            recyclerView.setAdapter(recentAdapter);

        }

        if (AppState.get().recentMode == AppState.MODE_COVERS) {
            int num = Dips.screenWidthDP() / AppState.get().coverBigSize;
            RecyclerView.LayoutManager mGridManager = new GridLayoutManager(getActivity(), num);
            recyclerView.setLayoutManager(mGridManager);

            recentAdapter.setAdapterType(FileMetaAdapter.ADAPTER_COVERS);
            recyclerView.setAdapter(recentAdapter);
        }

        if (AppState.get().recentMode == AppState.MODE_GRID) {
            int num = Dips.screenWidthDP() / AppState.get().coverBigSize;
            RecyclerView.LayoutManager mGridManager = new GridLayoutManager(getActivity(), num);
            recyclerView.setLayoutManager(mGridManager);
            recentAdapter.setAdapterType(FileMetaAdapter.ADAPTER_GRID);
            recyclerView.setAdapter(recentAdapter);
        }

    }

    @Override
    public void notifyFragment() {
        if (recentAdapter != null) {
            recentAdapter.notifyDataSetChanged();
        }
    }

    @Override
    public void resetFragment() {

    }

}
