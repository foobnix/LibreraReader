package com.foobnix.ui2.fragment;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.foobnix.android.utils.Dips;
import com.foobnix.android.utils.LOG;
import com.foobnix.android.utils.ResultResponse;
import com.foobnix.dao2.FileMeta;
import com.foobnix.opds.Entry;
import com.foobnix.opds.Feed;
import com.foobnix.opds.OPDS;
import com.foobnix.pdf.info.R;
import com.foobnix.pdf.info.TintUtil;
import com.foobnix.pdf.info.wrapper.AppState;
import com.foobnix.pdf.info.wrapper.PopupHelper;
import com.foobnix.ui2.adapter.FileMetaAdapter;
import com.foobnix.ui2.fast.FastScrollRecyclerView;

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

public class OpdsFragment2 extends UIFragment<FileMeta> {

    private static final String FLIBUSTA = "http://flibusta.is/opds";
    FileMetaAdapter searchAdapter;
    private FastScrollRecyclerView recyclerView;
    ImageView onListGrid;
    TextView titleView, contentView;

    String url = FLIBUSTA;
    String title;

    public OpdsFragment2() {
        super();
    }

    public static OpdsFragment2 newInstance(Bundle bundle) {
        OpdsFragment2 br = new OpdsFragment2();
        br.setArguments(bundle);
        return br;
    }

    @Override
    public Pair<Integer, Integer> getNameAndIconRes() {
        return new Pair<Integer, Integer>(R.string.catalogs, R.drawable.glyphicons_145_folder_open);
    }

    @Override
    public void onTintChanged() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_opds2, container, false);

        recyclerView = (FastScrollRecyclerView) view.findViewById(R.id.recyclerView);

        titleView = (TextView) view.findViewById(R.id.titleView);
        contentView = (TextView) view.findViewById(R.id.contentView);

        onListGrid = (ImageView) view.findViewById(R.id.onListGrid);
        onListGrid.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                popupMenu(onListGrid);
            }
        });

        searchAdapter = new FileMetaAdapter();
        bindAdapter(searchAdapter);
        bindAuthorsSeriesAdapter(searchAdapter);

        onGridList();

        searchAdapter.setOnItemClickListener(new ResultResponse<FileMeta>() {

            @Override
            public boolean onResultRecive(FileMeta result) {
                LOG.d("Click", result.getPathTxt(), result.getPath());
                url = result.getPath();
                populate();
                return false;
            }
        });

        searchAdapter.setOnItemLongClickListener(new ResultResponse<FileMeta>() {
            @Override
            public boolean onResultRecive(FileMeta result) {
                return false;
            }
        });

        view.findViewById(R.id.onBack).setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                populate();
            }
        });

        view.findViewById(R.id.onHome).setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                url = FLIBUSTA;
                populate();
            }
        });

        populate();
        onTintChanged();

        return view;
    }

    public List<FileMeta> convert(List<Entry> list) {
        List<FileMeta> res = new ArrayList<FileMeta>();
        for (Entry e : list) {
            res.add(convert(e));
        }
        return res;
    }

    public FileMeta convert(Entry feed) {
        FileMeta meta = new FileMeta();
        meta.setPathTxt(feed.title + " " + feed.content);
        meta.setPath("http://flibusta.is/" + feed.links.get(0).href);
        meta.setCusType(FileMetaAdapter.DISPLAY_TYPE_DIRECTORY);
        LOG.d(meta.getPath());
        return meta;
    }

    @Override
    public List<FileMeta> prepareDataInBackground() {
        Feed feed = OPDS.getFeed(url);
        title = feed.title;
        return convert(feed.entries);
    }

    @Override
    public void populateDataInUI(List<FileMeta> items) {
        searchAdapter.clearItems();
        searchAdapter.getItemsList().addAll(items);
        recyclerView.setAdapter(searchAdapter);

        titleView.setText("" + title);
    }

    public boolean onBackAction() {
        return false;
    }

    public void onGridList() {
        if (searchAdapter == null) {
            return;
        }
        PopupHelper.updateGridOrListIcon(onListGrid, AppState.get().broseMode);

        if (AppState.get().broseMode == AppState.MODE_LIST) {
            RecyclerView.LayoutManager mLayoutManager = new LinearLayoutManager(getActivity());
            recyclerView.setLayoutManager(mLayoutManager);
            searchAdapter.setAdapterType(FileMetaAdapter.ADAPTER_LIST);
            recyclerView.setAdapter(searchAdapter);

        }

        if (AppState.get().broseMode == AppState.MODE_COVERS) {
            int num = Math.max(1, Dips.screenWidthDP() / AppState.get().coverBigSize);
            RecyclerView.LayoutManager mGridManager = new GridLayoutManager(getActivity(), num);
            recyclerView.setLayoutManager(mGridManager);

            searchAdapter.setAdapterType(FileMetaAdapter.ADAPTER_COVERS);
            recyclerView.setAdapter(searchAdapter);
        }

        if (AppState.get().broseMode == AppState.MODE_GRID) {
            int num = Math.max(1, Dips.screenWidthDP() / AppState.get().coverBigSize);
            RecyclerView.LayoutManager mGridManager = new GridLayoutManager(getActivity(), num);
            recyclerView.setLayoutManager(mGridManager);

            searchAdapter.setAdapterType(FileMetaAdapter.ADAPTER_GRID);
            recyclerView.setAdapter(searchAdapter);
        }

        recyclerView.myConfiguration();
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
                    AppState.getInstance().broseMode = actions.get(index);
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
    public boolean isBackPressed() {
        return onBackAction();
    }

    @Override
    public void notifyFragment() {
        if (searchAdapter != null) {
            searchAdapter.notifyDataSetChanged();
            populate();
        }
    }

    @Override
    public void resetFragment() {
        onGridList();
    }

}
