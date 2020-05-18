package com.foobnix.ui2.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.MenuItem.OnMenuItemClickListener;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.core.util.Pair;
import androidx.recyclerview.widget.RecyclerView;

import com.foobnix.android.utils.LOG;
import com.foobnix.android.utils.ResultResponse;
import com.foobnix.android.utils.TxtUtils;
import com.foobnix.dao2.FileMeta;
import com.foobnix.ext.CacheZipUtils;
import com.foobnix.model.AppData;
import com.foobnix.model.AppState;
import com.foobnix.pdf.info.R;
import com.foobnix.pdf.info.TintUtil;
import com.foobnix.pdf.info.view.AlertDialogs;
import com.foobnix.pdf.info.view.MyPopupMenu;
import com.foobnix.pdf.info.wrapper.PopupHelper;
import com.foobnix.ui2.AppDB;
import com.foobnix.ui2.adapter.FileMetaAdapter;

import java.io.File;
import java.util.Arrays;
import java.util.List;

public class RecentFragment2 extends UIFragment<FileMeta> {
    public static final Pair<Integer, Integer> PAIR = new Pair<Integer, Integer>(R.string.recent, R.drawable.glyphicons_72_book);
    FileMetaAdapter recentAdapter;
    ImageView onListGrid;
    View panelRecent;

    @Override
    public Pair<Integer, Integer> getNameAndIconRes() {
        return PAIR;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_recent, container, false);

        recyclerView = (RecyclerView) view.findViewById(R.id.recyclerView);
        panelRecent = view.findViewById(R.id.panelRecent);

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
                AlertDialogs.showDialog(getActivity(), getString(R.string.do_you_want_to_clear_everything_), getString(R.string.ok), new Runnable() {

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

        onGridList();
        populate();

        TintUtil.setBackgroundFillColor(panelRecent, TintUtil.color);

        return view;
    }

    @Override
    public void onTintChanged() {
        TintUtil.setBackgroundFillColor(panelRecent, TintUtil.color);
    }

    ResultResponse<FileMeta> onDeleteRecentClick = new ResultResponse<FileMeta>() {

        @Override
        public boolean onResultRecive(FileMeta result) {
            result.setIsRecent(false);
            AppDB.get().update(result);

            if(result.getPath().startsWith(CacheZipUtils.CACHE_RECENT.getPath())){
                new File(result.getPath()).delete();
                LOG.d("Delete cache recent file", result.getPath());
            }



            AppData.get().removeRecent(result);

            populate();



            return false;
        }
    };

    Runnable clearAllRecent = new Runnable() {

        @Override
        public void run() {
            AppDB.get().clearAllRecent();


            CacheZipUtils.removeFiles(CacheZipUtils.CACHE_RECENT.listFiles());

            AppData.get().clearRecents();

            populate();
        }
    };

    public boolean onBackAction() {
        return false;
    }

    @Override
    public List<FileMeta> prepareDataInBackground() {
        List<FileMeta> allRecent = AppData.get().getAllRecent(true);
        return allRecent;
    }

    @Override
    public void populateDataInUI(List<FileMeta> items) {
        if (recentAdapter != null) {
            recentAdapter.getItemsList().clear();
            recentAdapter.getItemsList().addAll(items);
            recentAdapter.notifyDataSetChanged();
        }
    }

    public void onGridList() {
        LOG.d("onGridList");
        onGridList(AppState.get().recentMode, onListGrid, recentAdapter, null);
    }

    private void popupMenu(final ImageView onGridList) {
        MyPopupMenu p = new MyPopupMenu(getActivity(), onGridList);
        PopupHelper.addPROIcon(p, getActivity());

        List<Integer> names = Arrays.asList(R.string.list, R.string.compact, R.string.grid, R.string.cover);
        final List<Integer> icons = Arrays.asList(R.drawable.glyphicons_114_justify, R.drawable.glyphicons_114_justify_compact, R.drawable.glyphicons_156_show_big_thumbnails, R.drawable.glyphicons_157_show_thumbnails);
        final List<Integer> actions = Arrays.asList(AppState.MODE_LIST, AppState.MODE_LIST_COMPACT, AppState.MODE_GRID, AppState.MODE_COVERS);

        for (int i = 0; i < names.size(); i++) {
            final int index = i;
            p.getMenu().add(names.get(i)).setIcon(icons.get(i)).setOnMenuItemClickListener(new OnMenuItemClickListener() {

                @Override
                public boolean onMenuItemClick(MenuItem item) {
                    AppState.get().recentMode = actions.get(index);
                    onGridList.setImageResource(icons.get(index));
                    onGridList();
                    return false;
                }
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
