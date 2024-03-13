package com.foobnix.ui2.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.MenuItem.OnMenuItemClickListener;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.ImageView;

import androidx.core.util.Pair;
import androidx.fragment.app.FragmentActivity;
import androidx.recyclerview.widget.RecyclerView;

import com.foobnix.android.utils.LOG;
import com.foobnix.android.utils.ResultResponse;
import com.foobnix.android.utils.TxtUtils;
import com.foobnix.dao2.FileMeta;
import com.foobnix.model.AppData;
import com.foobnix.model.AppState;
import com.foobnix.model.SimpleMeta;
import com.foobnix.model.TagData;
import com.foobnix.pdf.info.FileMetaComparators;
import com.foobnix.pdf.info.Playlists;
import com.foobnix.pdf.info.R;
import com.foobnix.pdf.info.TintUtil;
import com.foobnix.pdf.info.view.AlertDialogs;
import com.foobnix.pdf.info.view.Dialogs;
import com.foobnix.pdf.info.view.DialogsPlaylist;
import com.foobnix.pdf.info.view.MyPopupMenu;
import com.foobnix.pdf.info.wrapper.PopupHelper;
import com.foobnix.pdf.search.activity.msg.NotifyAllFragments;
import com.foobnix.ui2.AppDB;
import com.foobnix.ui2.adapter.FileMetaAdapter;

import org.greenrobot.eventbus.EventBus;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class FavoritesFragment2 extends UIFragment<FileMeta> {
    public static final Pair<Integer, Integer> PAIR = new Pair<Integer, Integer>(R.string.starred, R.drawable.glyphicons_49_star);

    FileMetaAdapter recentAdapter;
    ImageView onListGrid, sortOrder, onSort;
    View panelRecent;
    String syncronizedBooksTitle;

    @Override
    public Pair<Integer, Integer> getNameAndIconRes() {
        return PAIR;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_starred, container, false);


        recyclerView = (RecyclerView) view.findViewById(R.id.recyclerView);
        panelRecent = view.findViewById(R.id.panelRecent);

        recentAdapter = new FileMetaAdapter();
        recentAdapter.tempValue = FileMetaAdapter.TEMP_VALUE_FOLDER_PATH;
        bindAdapter(recentAdapter);
        bindAuthorsSeriesAdapter(recentAdapter);

        syncronizedBooksTitle = getString(R.string.synchronized_books);

        onSort = (ImageView) view.findViewById(R.id.onSort);
        sortOrder = (ImageView) view.findViewById(R.id.sortOrder);


        view.findViewById(R.id.onShowMenu).setOnClickListener(v -> {

            MyPopupMenu p = new MyPopupMenu(getActivity(), v);
            p.getMenu().addCheckbox(getString(R.string.tags), AppState.get().isShowFavTags, new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    AppState.get().isShowFavTags = isChecked;
                    populate();
                }
            });
            p.getMenu().addCheckbox(getString(R.string.playlists), AppState.get().isShowFavPlaylist, new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    AppState.get().isShowFavPlaylist = isChecked;
                    populate();
                }
            });
            p.getMenu().addCheckbox(getString(R.string.folders), AppState.get().isShowFavFolders, new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    AppState.get().isShowFavFolders = isChecked;
                    populate();
                }
            });
            p.getMenu().addCheckbox(getString(R.string.books), AppState.get().isShowFavBooks, new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    AppState.get().isShowFavBooks = isChecked;
                    populate();
                }
            });
            p.getMenu().addCheckbox(getString(R.string.synced_books), AppState.get().isShowSyncBooks, new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    AppState.get().isShowSyncBooks = isChecked;
                    populate();
                }
            });

            p.getMenu().addCheckbox(getString(R.string.discarded_books), AppState.get().isShowDiscardedBooks, new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    AppState.get().isShowDiscardedBooks = isChecked;
                    populate();
                }
            });

            p.getMenu().addCheckbox("Testing books", AppState.get().isShowTestBooks, new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    AppState.get().isShowTestBooks = isChecked;
                    populate();
                }
            });
            p.show();
        });


        sortOrder.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                AppState.get().sortByFavoriteReverse = !AppState.get().sortByFavoriteReverse;
                onSort.setImageResource(AppState.get().sortByFavoriteReverse ? R.drawable.glyphicons_477_sort_attributes_alt : R.drawable.glyphicons_476_sort_attributes);
                sortOrder.setImageResource(AppState.get().sortByFavoriteReverse ? R.drawable.glyphicons_222_chevron_up : R.drawable.glyphicons_221_chevron_down);


                populate();

            }
        });

        onSort.setImageResource(AppState.get().sortByReverse ? R.drawable.glyphicons_477_sort_attributes_alt : R.drawable.glyphicons_476_sort_attributes);
        sortOrder.setImageResource(AppState.get().sortByReverse ? R.drawable.glyphicons_222_chevron_up : R.drawable.glyphicons_221_chevron_down);

        sortOrder.setContentDescription(getString(R.string.ascending) + " " + getString(R.string.descending));
        onSort.setContentDescription(getString(R.string.cd_sort_results));

        onSort.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {

                List<String> names = Arrays.asList(//
                        getActivity().getString(R.string.by_file_name), //
                        getActivity().getString(R.string.by_date), //
                        getActivity().getString(R.string.by_size), //
                        getActivity().getString(R.string.by_title), //
                        getActivity().getString(R.string.by_author), //
                        getActivity().getString(R.string.by_number_in_serie), //
                        getActivity().getString(R.string.by_number_of_pages), //
                        getActivity().getString(R.string.by_extension) //
                );//

                final List<Integer> ids = Arrays.asList(//
                        AppState.BR_SORT_BY_PATH, //
                        AppState.BR_SORT_BY_DATE, //
                        AppState.BR_SORT_BY_SIZE, //
                        AppState.BR_SORT_BY_TITLE, //
                        AppState.BR_SORT_BY_AUTHOR, //
                        AppState.BR_SORT_BY_NUMBER, //
                        AppState.BR_SORT_BY_PAGES, //
                        AppState.BR_SORT_BY_EXT//
                );//

                MyPopupMenu menu = new MyPopupMenu(getActivity(), v);
                for (int i = 0; i < names.size(); i++) {
                    String name = names.get(i);
                    final int j = i;
                    menu.getMenu().add(name).setOnMenuItemClickListener(new OnMenuItemClickListener() {

                        @Override
                        public boolean onMenuItemClick(MenuItem item) {
                            AppState.get().sortByFavorite = ids.get(j);
                            populate();
                            return false;
                        }
                    });
                }
                menu.show();
            }
        });


        TxtUtils.underlineTextView(view.findViewById(R.id.clearAllRecent)).setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                AlertDialogs.showDialog(getActivity(), getString(R.string.do_you_want_to_clear_everything_), getString(R.string.ok), new Runnable() {

                    @Override
                    public void run() {
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
                    }
                });

            }
        });

        onListGrid = (ImageView) view.findViewById(R.id.onListGrid);
        onListGrid.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                popupMenu(onListGrid);
            }
        });

        TxtUtils.underlineTextView(view.findViewById(R.id.onPlaylists)).setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                DialogsPlaylist.showPlaylistsDialog(v.getContext(), new Runnable() {

                    @Override
                    public void run() {
                        resetFragment();
                        EventBus.getDefault().post(new NotifyAllFragments());
                    }
                }, null);

            }
        });
        TxtUtils.underlineTextView(view.findViewById(R.id.onTags)).setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                Dialogs.showTagsDialog((FragmentActivity) v.getContext(), null, false, new Runnable() {

                    @Override
                    public void run() {
                        resetFragment();
                        EventBus.getDefault().post(new NotifyAllFragments());
                    }
                });

            }
        });


        recentAdapter.setOnGridOrList(new ResultResponse<ImageView>() {

            @Override
            public boolean onResultRecive(ImageView result) {
                popupMenu(result);
                return false;
            }
        });

        onGridList();

        populate();

        TintUtil.setBackgroundFillColor(panelRecent, TintUtil.color);

        return view;
    }

    private void popupMenu(final ImageView image) {
        MyPopupMenu p = new MyPopupMenu(getActivity(), image);
        PopupHelper.addPROIcon(p, getActivity());

        List<Integer> names = Arrays.asList(R.string.list, //
                R.string.compact, //
                R.string.grid, //
                R.string.cover//
        );

        final List<Integer> icons = Arrays.asList(R.drawable.my_glyphicons_114_paragraph_justify, //
                R.drawable.my_glyphicons_114_justify_compact, //
                R.drawable.glyphicons_157_thumbnails, //
                R.drawable.glyphicons_158_thumbnails_small //
        );
        final List<Integer> actions = Arrays.asList(AppState.MODE_LIST, AppState.MODE_LIST_COMPACT, AppState.MODE_GRID, AppState.MODE_COVERS);

        for (int i = 0; i < names.size(); i++) {
            final int index = i;
            p.getMenu().add(names.get(i)).setIcon(icons.get(i)).setOnMenuItemClickListener(new OnMenuItemClickListener() {

                @Override
                public boolean onMenuItemClick(MenuItem item) {
                    AppState.get().starsMode = actions.get(index);
                    image.setImageResource(icons.get(index));

                    onGridList();

                    return false;
                }
            });
        }

        p.show();

    }

    @Override
    public void onTintChanged() {
        TintUtil.setBackgroundFillColor(panelRecent, TintUtil.color);

    }

    public boolean onBackAction() {
        return false;
    }

    @Override
    public List<FileMeta> prepareDataInBackground() {

        List<FileMeta> all = new ArrayList<FileMeta>();

        if (AppState.get().isShowFavTags) {
            List<String> tags = TagData.getAllTagsByFile();
            Collections.sort(tags, String.CASE_INSENSITIVE_ORDER);

            if (TxtUtils.isListNotEmpty(tags)) {
                for (String tag : tags) {
                    if (TxtUtils.isEmpty(tag)) {
                        continue;
                    }
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
        }

        if (AppState.get().isShowFavPlaylist) {
            all.addAll(Playlists.getAllPlaylistsMeta());
        }

        if (AppState.get().isShowFavFolders) {
            {
                FileMeta empy = new FileMeta();
                empy.setCusType(FileMetaAdapter.DISPALY_TYPE_LAYOUT_TITLE_NONE);
                all.add(empy);
            }

            all.addAll(AppData.get().getAllFavoriteFolders());
        }


        if (AppState.get().isShowFavBooks) {
            final List<FileMeta> allFavoriteFiles = AppData.get().getAllFavoriteFiles(true);

            if (TxtUtils.isListNotEmpty(allFavoriteFiles)) {

                try {
                    if (AppState.get().sortByFavorite == AppState.BR_SORT_BY_PATH) {
                        Collections.sort(allFavoriteFiles, FileMetaComparators.BY_PATH_NUMBER);
                    } else if (AppState.get().sortByFavorite == AppState.BR_SORT_BY_DATE) {
                        Collections.sort(allFavoriteFiles, FileMetaComparators.BY_DATE);
                    } else if (AppState.get().sortByFavorite == AppState.BR_SORT_BY_SIZE) {
                        Collections.sort(allFavoriteFiles, FileMetaComparators.BY_SIZE);
                    } else if (AppState.get().sortByFavorite == AppState.BR_SORT_BY_NUMBER) {
                        Collections.sort(allFavoriteFiles, FileMetaComparators.BR_BY_NUMBER1);
                    } else if (AppState.get().sortByFavorite == AppState.BR_SORT_BY_PAGES) {
                        Collections.sort(allFavoriteFiles, FileMetaComparators.BR_BY_PAGES);
                    } else if (AppState.get().sortByFavorite == AppState.BR_SORT_BY_TITLE) {
                        Collections.sort(allFavoriteFiles, FileMetaComparators.BR_BY_TITLE);
                         Comparator.naturalOrder();
                    } else if (AppState.get().sortByFavorite == AppState.BR_SORT_BY_EXT) {
                        Collections.sort(allFavoriteFiles, FileMetaComparators.BR_BY_EXT);
                    } else if (AppState.get().sortByFavorite == AppState.BR_SORT_BY_AUTHOR) {
                        Collections.sort(allFavoriteFiles, FileMetaComparators.BR_BY_AUTHOR);
                    }
                    if (AppState.get().sortByFavoriteReverse) {
                        Collections.reverse(allFavoriteFiles);
                    }

                } catch (Exception e) {
                    LOG.e(e);
                }


                FileMeta empy = new FileMeta();
                empy.setCusType(FileMetaAdapter.DISPALY_TYPE_LAYOUT_TITLE_NONE);
                all.add(empy);

                all.addAll(allFavoriteFiles);
            }
        }

        if (AppState.get().isShowSyncBooks) {
            final List<FileMeta> allSyncBooks = AppData.get().getAllSyncBooks();
            if (TxtUtils.isListNotEmpty(allSyncBooks)) {

                FileMeta empy = new FileMeta();
                empy.setCusType(FileMetaAdapter.DISPALY_TYPE_LAYOUT_TITLE_DIVIDER);
                empy.setTitle(syncronizedBooksTitle);
                all.add(empy);


                all.addAll(allSyncBooks);
            }
        }
        if (AppState.get().isShowDiscardedBooks) {
            final List<SimpleMeta> allSyncBooks = AppData.get().getAllExcluded();
            List<FileMeta> discardedBooks = new ArrayList<>();
            for (SimpleMeta s : allSyncBooks) {
                discardedBooks.add(AppDB.get().getOrCreate(s.getPath()));
            }

            if (TxtUtils.isListNotEmpty(allSyncBooks)) {

                FileMeta empty = new FileMeta();
                empty.setCusType(FileMetaAdapter.DISPALY_TYPE_LAYOUT_TITLE_DIVIDER);
                empty.setTitle(getString(R.string.discarded_books));
                all.add(empty);

                all.addAll(discardedBooks);
            }
        }
        if (AppState.get().isShowTestBooks) {
            final List<FileMeta> allSyncBooks = AppData.get().getAllTestedBooks();
            if (TxtUtils.isListNotEmpty(allSyncBooks)) {

                FileMeta empy = new FileMeta();
                empy.setCusType(FileMetaAdapter.DISPALY_TYPE_LAYOUT_TITLE_DIVIDER);
                empy.setTitle("Testing books");
                all.add(empy);


                all.addAll(allSyncBooks);
            }
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
        onGridList(AppState.get().starsMode, onListGrid, recentAdapter, null);

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
