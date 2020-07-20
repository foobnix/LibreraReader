package com.foobnix.ui2.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.core.util.Pair;

import com.foobnix.android.utils.LOG;
import com.foobnix.android.utils.ResultResponse;
import com.foobnix.android.utils.TxtUtils;
import com.foobnix.dao2.FileMeta;
import com.foobnix.ext.CacheZipUtils;
import com.foobnix.model.AppData;
import com.foobnix.model.AppState;
import com.foobnix.pdf.info.R;
import com.foobnix.pdf.info.TintUtil;
import com.foobnix.pdf.info.databinding.FragmentRecentBinding;
import com.foobnix.pdf.info.view.AlertDialogs;
import com.foobnix.pdf.info.view.MyPopupMenu;
import com.foobnix.pdf.info.wrapper.PopupHelper;
import com.foobnix.ui2.AppDB;
import com.foobnix.ui2.adapter.FileMetaAdapter;

import java.io.File;
import java.util.Arrays;
import java.util.List;

public class RecentFragment2 extends UIFragment<FileMeta> {
    public static final Pair<Integer, Integer> PAIR = new Pair<>(R.string.recent, R.drawable.glyphicons_72_book);
    private FragmentRecentBinding binding;
    FileMetaAdapter recentAdapter;

    @Override
    public Pair<Integer, Integer> getNameAndIconRes() {
        return PAIR;
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentRecentBinding.inflate(inflater, container, false);

        recyclerView = binding.recyclerView;

        binding.onListGrid.setOnClickListener(v -> popupMenu());

        TxtUtils.underlineTextView(binding.clearAllRecent).setOnClickListener(v ->
                AlertDialogs.showDialog(getActivity(),
                        getString(R.string.do_you_want_to_clear_everything_),
                        getString(R.string.ok), () -> clearAllRecent.run()));

        recentAdapter = new FileMetaAdapter();
        recentAdapter.tempValue = FileMetaAdapter.TEMP_VALUE_FOLDER_PATH;
        bindAdapter(recentAdapter);
        bindAuthorsSeriesAdapter(recentAdapter);

        recentAdapter.setOnDeleteClickListener(onDeleteRecentClick);

        onGridList();
        populate();

        TintUtil.setBackgroundFillColor(binding.panelRecent, TintUtil.color);

        return binding.getRoot();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    @Override
    public void onTintChanged() {
        TintUtil.setBackgroundFillColor(binding.panelRecent, TintUtil.color);
    }

    ResultResponse<FileMeta> onDeleteRecentClick = result -> {
        result.setIsRecent(false);
        AppDB.get().update(result);

        if(result.getPath().startsWith(CacheZipUtils.CACHE_RECENT.getPath())){
            new File(result.getPath()).delete();
            LOG.d("Delete cache recent file", result.getPath());
        }

        AppData.get().removeRecent(result);

        populate();

        return false;
    };

    Runnable clearAllRecent = () -> {
        AppDB.get().clearAllRecent();
        CacheZipUtils.removeFiles(CacheZipUtils.CACHE_RECENT.listFiles());
        AppData.get().clearRecents();
        populate();
    };

    @Override
    public List<FileMeta> prepareDataInBackground() {
        return AppData.get().getAllRecent(true);
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
        onGridList(AppState.get().recentMode, binding.onListGrid, recentAdapter, null);
    }

    private void popupMenu() {
        MyPopupMenu p = new MyPopupMenu(getActivity(), binding.onListGrid);
        PopupHelper.addPROIcon(p, getActivity());

        List<Integer> names = Arrays.asList(R.string.list, R.string.compact, R.string.grid, R.string.cover);
        final List<Integer> icons = Arrays.asList(R.drawable.glyphicons_114_justify, R.drawable.glyphicons_114_justify_compact, R.drawable.glyphicons_156_show_big_thumbnails, R.drawable.glyphicons_157_show_thumbnails);
        final List<Integer> actions = Arrays.asList(AppState.MODE_LIST, AppState.MODE_LIST_COMPACT, AppState.MODE_GRID, AppState.MODE_COVERS);

        for (int i = 0; i < names.size(); i++) {
            final int index = i;
            p.getMenu().add(names.get(i)).setIcon(icons.get(i)).setOnMenuItemClickListener(item -> {
                AppState.get().recentMode = actions.get(index);
                binding.onListGrid.setImageResource(icons.get(index));
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
