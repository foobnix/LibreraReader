package com.foobnix.ui2.fragment;

import java.util.Arrays;
import java.util.List;

import com.foobnix.android.utils.Dips;
import com.foobnix.android.utils.ResultResponse;
import com.foobnix.android.utils.TxtUtils;
import com.foobnix.dao2.FileMeta;
import com.foobnix.pdf.info.R;
import com.foobnix.pdf.info.TintUtil;
import com.foobnix.pdf.info.view.AlertDialogs;
import com.foobnix.pdf.info.wrapper.AppState;
import com.foobnix.pdf.info.wrapper.PopupHelper;
import com.foobnix.ui2.AppDB;
import com.foobnix.ui2.adapter.FileMetaAdapter;

import android.os.Bundle;
import android.support.v4.util.Pair;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.MenuItem.OnMenuItemClickListener;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.TextView;

public class RecentFragment2 extends UIFragment<FileMeta> {
    public static final Pair<Integer, Integer> PAIR = new Pair<Integer, Integer>(R.string.recent, R.drawable.glyphicons_72_book);
    FileMetaAdapter recentAdapter;
    private RecyclerView recyclerView;
    ImageView onListGrid;

    @Override
    public Pair<Integer, Integer> getNameAndIconRes() {
        return PAIR;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_recent, container, false);

        recyclerView = (RecyclerView) view.findViewById(R.id.recyclerView);

        onListGrid = (ImageView) view.findViewById(R.id.onListGrid);
        onListGrid.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                popupMenu(onListGrid);
            }
        });

        TxtUtils.underlineTextView((TextView) view.findViewById(R.id.clearAllRecent)).setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                AlertDialogs.showDialog(getActivity(), getString(R.string.clear_all_recent), getString(R.string.ok), new Runnable() {

                    @Override
                    public void run() {
                        clearAllRecent.run();
                    }
                });

            }
        });

        recentAdapter = new FileMetaAdapter();
        recentAdapter.tempValue = FileMetaAdapter.TEMP_VALUE_FOLDER_PATH;
        bindAdapter(recentAdapter);
        bindAuthorsSeriesAdapter(recentAdapter);

        recentAdapter.setOnDeleteClickListener(onDeleteRecentClick);


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


    public boolean onBackAction() {
        return false;
    }

    @Override
    public void onSelectFragment() {
        populate();
    }

    @Override
    public List<FileMeta> prepareDataInBackground() {
        return AppDB.get().getAllRecentWithProgress();
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
            int num = Math.max(1, Dips.screenWidthDP() / AppState.get().coverBigSize);
            RecyclerView.LayoutManager mGridManager = new GridLayoutManager(getActivity(), num);
            recyclerView.setLayoutManager(mGridManager);

            recentAdapter.setAdapterType(FileMetaAdapter.ADAPTER_COVERS);
            recyclerView.setAdapter(recentAdapter);
        }

        if (AppState.get().recentMode == AppState.MODE_GRID) {
            int num = Math.max(1, Dips.screenWidthDP() / AppState.get().coverBigSize);
            RecyclerView.LayoutManager mGridManager = new GridLayoutManager(getActivity(), num);
            recyclerView.setLayoutManager(mGridManager);
            recentAdapter.setAdapterType(FileMetaAdapter.ADAPTER_GRID);
            recyclerView.setAdapter(recentAdapter);
        }


    }

    private void popupMenu(final ImageView onGridList) {
        PopupMenu p = new PopupMenu(getActivity(), onGridList);
        List<Integer> names = Arrays.asList(R.string.list, R.string.grid, R.string.cover);
        final List<Integer> icons = Arrays.asList(R.drawable.glyphicons_114_justify, R.drawable.glyphicons_156_show_big_thumbnails, R.drawable.glyphicons_157_show_thumbnails);
        final List<Integer> actions = Arrays.asList(AppState.MODE_LIST, AppState.MODE_GRID, AppState.MODE_COVERS);

        for (int i = 0; i < names.size(); i++) {
            final int index = i;
            p.getMenu().add(names.get(i)).setIcon(icons.get(i)).setOnMenuItemClickListener(new OnMenuItemClickListener() {

                @Override
                public boolean onMenuItemClick(MenuItem item) {
                    AppState.getInstance().recentMode = actions.get(index);
                    onGridList.setImageResource(icons.get(index));
                    onGridList();
                    return false;
                }
            });
        }

        p.show();

        PopupHelper.initIcons(p, TintUtil.color);
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
