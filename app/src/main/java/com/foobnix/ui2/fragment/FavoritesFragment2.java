package com.foobnix.ui2.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.core.util.Pair;
import androidx.fragment.app.FragmentActivity;

import com.foobnix.android.utils.TxtUtils;
import com.foobnix.dao2.FileMeta;
import com.foobnix.model.AppData;
import com.foobnix.model.AppState;
import com.foobnix.pdf.info.Playlists;
import com.foobnix.pdf.info.R;
import com.foobnix.pdf.info.TintUtil;
import com.foobnix.pdf.info.databinding.FragmentStarredBinding;
import com.foobnix.pdf.info.view.AlertDialogs;
import com.foobnix.pdf.info.view.Dialogs;
import com.foobnix.pdf.info.view.DialogsPlaylist;
import com.foobnix.pdf.info.view.MyPopupMenu;
import com.foobnix.pdf.info.wrapper.PopupHelper;
import com.foobnix.pdf.search.activity.msg.NotifyAllFragments;
import com.foobnix.ui2.AppDB;
import com.foobnix.ui2.AppDB.SEARCH_IN;
import com.foobnix.ui2.adapter.FileMetaAdapter;

import org.greenrobot.eventbus.EventBus;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class FavoritesFragment2 extends UIFragment<FileMeta> {
    public static final Pair<Integer, Integer> PAIR = new Pair<>(R.string.starred, R.drawable.glyphicons_50_star);

    FileMetaAdapter recentAdapter;
    private FragmentStarredBinding binding;
    String synchronizedBooksTitle;

    @Override
    public Pair<Integer, Integer> getNameAndIconRes() {
        return PAIR;
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentStarredBinding.inflate(inflater, container, false);

        recyclerView = binding.recyclerView;

        recentAdapter = new FileMetaAdapter();
        recentAdapter.tempValue = FileMetaAdapter.TEMP_VALUE_FOLDER_PATH;
        bindAdapter(recentAdapter);
        bindAuthorsSeriesAdapter(recentAdapter);

        synchronizedBooksTitle = getString(R.string.synchronized_books);

        TxtUtils.underlineTextView(binding.clearAllRecent).setOnClickListener(v -> AlertDialogs.showDialog(getActivity(),
                getString(R.string.do_you_want_to_clear_everything_),
                getString(R.string.ok), () -> {
                    for (FileMeta f : AppDB.get().getStarsFilesDeprecated()) {
                        f.setIsStar(false);
                        AppDB.get().update(f);
                    }
                    for (FileMeta f : AppDB.get().getStarsFoldersDeprecated()) {
                        f.setIsStar(false);
                        AppDB.get().update(f);
                    }
                    AppData.get().clearFavorites();

                    populate();
                }));

        binding.onListGrid.setOnClickListener(v -> popupMenu(binding.onListGrid));

        TxtUtils.underlineTextView(binding.onPlaylists)
                .setOnClickListener(v -> DialogsPlaylist.showPlaylistsDialog(v.getContext(), () -> {
                    resetFragment();
                    EventBus.getDefault().post(new NotifyAllFragments());
                }, null));
        TxtUtils.underlineTextView(binding.onTags)
                .setOnClickListener(v -> Dialogs.showTagsDialog((FragmentActivity) v.getContext(), null, false,
                        () -> {
                            resetFragment();
                            EventBus.getDefault().post(new NotifyAllFragments());
                        }));


        recentAdapter.setOnGridOrList(result -> {
            popupMenu(result);
            return false;
        });

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

    private void popupMenu(final ImageView image) {
        MyPopupMenu p = new MyPopupMenu(getActivity(), image);
        PopupHelper.addPROIcon(p, getActivity());

        List<Integer> names = Arrays.asList(R.string.list, //
                R.string.compact, //
                R.string.grid, //
                R.string.cover//
        );

        final List<Integer> icons = Arrays.asList(R.drawable.glyphicons_114_justify, //
                R.drawable.glyphicons_114_justify_compact, //
                R.drawable.glyphicons_156_show_big_thumbnails, //
                R.drawable.glyphicons_157_show_thumbnails //
        );
        final List<Integer> actions = Arrays.asList(AppState.MODE_LIST, AppState.MODE_LIST_COMPACT, AppState.MODE_GRID, AppState.MODE_COVERS);

        for (int i = 0; i < names.size(); i++) {
            final int index = i;
            p.getMenu().add(names.get(i)).setIcon(icons.get(i)).setOnMenuItemClickListener(item -> {
                AppState.get().starsMode = actions.get(index);
                image.setImageResource(icons.get(index));

                onGridList();

                return false;
            });
        }

        p.show();
    }

    @Override
    public void onTintChanged() {
        TintUtil.setBackgroundFillColor(binding.panelRecent, TintUtil.color);
    }

    @Override
    public List<FileMeta> prepareDataInBackground() {
        List<FileMeta> all = new ArrayList<>();

        List<String> tags = AppDB.get().getAll(SEARCH_IN.TAGS);
        if (TxtUtils.isListNotEmpty(tags)) {
            for (String tag : tags) {
                FileMeta m = new FileMeta("");
                m.setCusType(FileMetaAdapter.DISPALY_TYPE_LAYOUT_TAG);
                int count = AppDB.get().getAllWithTag(tag).size();
                m.setPathTxt(tag + " (" + count + ")");
                all.add(m);
            }

            {
                FileMeta empy = new FileMeta();
                empy.setCusType(FileMetaAdapter.DISPALY_TYPE_LAYOUT_TITLE_NONE);
                all.add(empy);
            }
        }

        all.addAll(Playlists.getAllPlaylistsMeta());

        {
            FileMeta empy = new FileMeta();
            empy.setCusType(FileMetaAdapter.DISPALY_TYPE_LAYOUT_TITLE_NONE);
            all.add(empy);
        }

        all.addAll(AppData.get().getAllFavoriteFolders());


        final List<FileMeta> allFavoriteFiles = AppData.get().getAllFavoriteFiles(true);

        if (TxtUtils.isListNotEmpty(allFavoriteFiles)) {
            FileMeta empy = new FileMeta();
            empy.setCusType(FileMetaAdapter.DISPALY_TYPE_LAYOUT_TITLE_NONE);
            all.add(empy);

            all.addAll(allFavoriteFiles);
        }

        final List<FileMeta> allSyncBooks = AppData.get().getAllSyncBooks();
        if (TxtUtils.isListNotEmpty(allSyncBooks)) {

            FileMeta empy = new FileMeta();
            empy.setCusType(FileMetaAdapter.DISPALY_TYPE_LAYOUT_TITLE_DIVIDER);
            empy.setTitle(synchronizedBooksTitle);
            all.add(empy);


            all.addAll(allSyncBooks);
        }

        return all;
    }

    @Override
    public void populateDataInUI(List<FileMeta> items) {
        recentAdapter.getItemsList().clear();
        recentAdapter.getItemsList().addAll(items);
        recentAdapter.notifyDataSetChanged();
    }

    public void onGridList() {
        onGridList(AppState.get().starsMode, binding.onListGrid, recentAdapter, null);
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
