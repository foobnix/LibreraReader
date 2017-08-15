package com.foobnix.ui2.fragment;

import java.util.ArrayList;
import java.util.List;

import com.foobnix.android.utils.ResultResponse;
import com.foobnix.dao2.FileMeta;
import com.foobnix.pdf.info.R;
import com.foobnix.pdf.info.view.AlertDialogs;
import com.foobnix.ui2.AppDB;
import com.foobnix.ui2.adapter.FileMetaAdapter;

import android.os.Bundle;
import android.support.v4.util.Pair;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

public class StarsFragment2 extends UIFragment<FileMeta> {
    public static final Pair<Integer, Integer> PAIR = new Pair<Integer, Integer>(R.string.starred, R.drawable.glyphicons_50_star);

    FileMetaAdapter recentAdapter;
    private RecyclerView recyclerView;

    @Override
    public Pair<Integer, Integer> getNameAndIconRes() {
        return PAIR;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_starred, container, false);

        recyclerView = (RecyclerView) view.findViewById(R.id.recyclerView);

        recentAdapter = new FileMetaAdapter();
        recentAdapter.tempValue = FileMetaAdapter.TEMP_VALUE_FOLDER_PATH;
        bindAdapter(recentAdapter);
        bindAuthorsSeriesAdapter(recentAdapter);

        recentAdapter.setOnDeleteClickListener(onDeleteRecentClick);

        recentAdapter.setClearAllStarredFolders(new Runnable() {

            @Override
            public void run() {
                AlertDialogs.showDialog(getActivity(), getString(R.string.clear_all), getString(R.string.ok), new Runnable() {

                    @Override
                    public void run() {
                        for (FileMeta f : AppDB.get().getStarsFolder()) {
                            f.setIsStar(false);
                            AppDB.get().update(f);
                        }
                        populate();
                    }
                });

            }
        });
        recentAdapter.setClearAllStarredBooks(new Runnable() {

            @Override
            public void run() {
                AlertDialogs.showDialog(getActivity(), getString(R.string.clear_all), getString(R.string.ok), new Runnable() {

                    @Override
                    public void run() {
                        for (FileMeta f : AppDB.get().getStarsFiles()) {
                            f.setIsStar(false);
                            AppDB.get().update(f);
                        }
                        populate();
                    }
                });

            }
        });

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

        all.add(new FileMeta(FileMetaAdapter.DISPALY_TYPE_LAYOUT_TITLE_FOLDERS));
        all.addAll(AppDB.get().getStarsFolder());
        all.add(new FileMeta(FileMetaAdapter.DISPALY_TYPE_LAYOUT_TITLE_BOOKS));
        all.addAll(AppDB.get().getStarsFiles());

        return all;
    }

    @Override
    public void populateDataInUI(List<FileMeta> items) {
        recentAdapter.getItemsList().clear();
        recentAdapter.getItemsList().addAll(items);
        recentAdapter.notifyDataSetChanged();
    }

    public void onGridList() {

        RecyclerView.LayoutManager mLayoutManager = new LinearLayoutManager(getActivity());
        recyclerView.setLayoutManager(mLayoutManager);
        recentAdapter.setAdapterType(FileMetaAdapter.ADAPTER_LIST);
        recyclerView.setAdapter(recentAdapter);

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
