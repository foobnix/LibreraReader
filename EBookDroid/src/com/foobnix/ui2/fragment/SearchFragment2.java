package com.foobnix.ui2.fragment;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.foobnix.android.utils.Dips;
import com.foobnix.android.utils.LOG;
import com.foobnix.android.utils.ResultResponse;
import com.foobnix.android.utils.TxtUtils;
import com.foobnix.dao2.FileMeta;
import com.foobnix.pdf.info.AppsConfig;
import com.foobnix.pdf.info.R;
import com.foobnix.pdf.info.TintUtil;
import com.foobnix.pdf.info.fragment.KeyCodeDialog;
import com.foobnix.pdf.info.widget.PrefDialogs;
import com.foobnix.pdf.info.wrapper.AppState;
import com.foobnix.pdf.info.wrapper.DocumentController;
import com.foobnix.pdf.info.wrapper.PopupHelper;
import com.foobnix.ui2.AppDB;
import com.foobnix.ui2.AppDB.SEARCH_IN;
import com.foobnix.ui2.AppDB.SORT_BY;
import com.foobnix.ui2.BooksService;
import com.foobnix.ui2.adapter.AuthorsAdapter2;
import com.foobnix.ui2.adapter.FileMetaAdapter;
import com.foobnix.ui2.fast.FastScrollRecyclerView;
import com.foobnix.ui2.fast.FastScrollStateChangeListener;
import com.nostra13.universalimageloader.core.ImageLoader;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.util.Pair;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.MenuItem.OnMenuItemClickListener;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

public class SearchFragment2 extends UIFragment<FileMeta> {

    private static final String CMD_LONG_TAP_OFF = "@cmd_long_tap_off";
    private static final String CMD_LONG_TAP_ON = "@cmd_long_tap_on";
    private static final String CMD_FULLSCREEN_ON = "@cmd_fullscreen_on";
    private static final String CMD_KEYCODE = "@cmd_keycode_config";

    public static int NONE = -1;

    FastScrollRecyclerView recyclerView;

    FileMetaAdapter searchAdapter;
    AuthorsAdapter2 authorsAdapter;

    TextView countBooks, sortBy;
    Handler handler;
    ImageView sortOrder;
    View onRefresh, cleanFilter, secondTopPanel;
    AutoCompleteTextView searchEditText;

    public static List<FileMeta> itemsMeta = new ArrayList<FileMeta>();
    final Set<String> autocomplitions = new HashSet<String>();
    public int prevLibModeFileMeta = AppState.MODE_GRID;
    public int prevLibModeAuthors = NONE;
    public int rememberPos = 0;

    @Override
    public Pair<Integer, Integer> getNameAndIconRes() {
        int resID = (AppsConfig.PDF_READER_LIRBI.equals(AppsConfig.APP_PACKAGE) || AppsConfig.PRO_PDF_READER.equals(AppsConfig.APP_PACKAGE)) ? R.drawable.lirbi_text : R.drawable.glyphicons_2_book_open;
        return new Pair<Integer, Integer>(R.string.library, resID);
    }

    @Override
    public void onTintChanged() {
        TintUtil.setBackgroundFillColor(secondTopPanel, TintUtil.color);
        TintUtil.setStrokeColor(searchEditText, TintUtil.color);
    }

    public void onGridList() {
        PopupHelper.updateGridOrListIcon(onGridlList, AppState.get().libraryMode);

        if (AppState.get().libraryMode == AppState.MODE_LIST) {
            RecyclerView.LayoutManager mLayoutManager = new LinearLayoutManager(getActivity());
            recyclerView.setLayoutManager(mLayoutManager);
            searchAdapter.setAdapterType(FileMetaAdapter.ADAPTER_LIST);
            recyclerView.setAdapter(searchAdapter);

        } else if (AppState.get().libraryMode == AppState.MODE_COVERS || AppState.get().libraryMode == AppState.MODE_GRID) {
            int num = Math.max(1, Dips.screenWidthDP() / AppState.get().coverBigSize);
            RecyclerView.LayoutManager mGridManager = new GridLayoutManager(getActivity(), num);
            recyclerView.setLayoutManager(mGridManager);

            searchAdapter.setAdapterType(AppState.get().libraryMode == AppState.MODE_COVERS ? FileMetaAdapter.ADAPTER_COVERS : FileMetaAdapter.ADAPTER_GRID);
            recyclerView.setAdapter(searchAdapter);

        } else if (Arrays.asList(AppState.MODE_AUTHORS, AppState.MODE_SERIES, AppState.MODE_GENRE).contains(AppState.get().libraryMode)) {
            RecyclerView.LayoutManager mLayoutManager = new LinearLayoutManager(getActivity());
            recyclerView.setLayoutManager(mLayoutManager);
            recyclerView.setAdapter(authorsAdapter);
        }

        recyclerView.myConfiguration();
    }

    public void initAutocomplition() {
        autocomplitions.clear();
        for (SEARCH_IN search : AppDB.SEARCH_IN.values()) {
            autocomplitions.add(search.getDotPrefix() + " ");
        }

        autocomplitions.add(CMD_FULLSCREEN_ON);
        autocomplitions.add(CMD_LONG_TAP_OFF);
        autocomplitions.add(CMD_LONG_TAP_ON);
        autocomplitions.add(CMD_KEYCODE);

        updateFilterListAdapter();
    }

    public void updateFilterListAdapter() {
        try {
            ArrayList<String> list = new ArrayList<String>(autocomplitions);
            Collections.sort(list);
            ArrayAdapter<String> adapter = new ArrayAdapter<String>(getActivity(), android.R.layout.simple_spinner_dropdown_item, list);
            searchEditText.setAdapter(adapter);
            searchEditText.setThreshold(1);
        } catch (Exception e) {
            LOG.e(e);
        }

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_search2, container, false);

        handler = new Handler();

        secondTopPanel = view.findViewById(R.id.secondTopPanel);
        countBooks = (TextView) view.findViewById(R.id.countBooks);
        onRefresh = view.findViewById(R.id.onRefresh);
        onRefresh.setActivated(true);
        cleanFilter = view.findViewById(R.id.cleanFilter);
        sortBy = (TextView) view.findViewById(R.id.sortBy);
        sortOrder = (ImageView) view.findViewById(R.id.sortOrder);
        searchEditText = (AutoCompleteTextView) view.findViewById(R.id.filterLine);
        recyclerView = (FastScrollRecyclerView) view.findViewById(R.id.recyclerView);

        recyclerView.setFastScrollStateChangeListener(new FastScrollStateChangeListener() {

            @Override
            public void onFastScrollStop() {
                ImageLoader.getInstance().resume();
                LOG.d("ImageLoader resume");
            }

            @Override
            public void onFastScrollStart() {
                LOG.d("ImageLoader pause");
                ImageLoader.getInstance().pause();
            }
        });

        searchEditText.addTextChangedListener(filterTextWatcher);
        searchAdapter = new FileMetaAdapter();
        authorsAdapter = new AuthorsAdapter2();

        recyclerView.setItemAnimator(new DefaultItemAnimator());
        recyclerView.setHasFixedSize(true);

        onGridlList = (ImageView) view.findViewById(R.id.onGridList);
        onGridlList.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                popupMenu(onGridlList);
            }
        });

        onRefresh.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                if (!onRefresh.isActivated()) {
                    Toast.makeText(getActivity(), R.string.extracting_information_from_books, Toast.LENGTH_LONG).show();
                    return;
                }
                PrefDialogs.chooseFolderDialog(getActivity(), new Runnable() {

                    @Override
                    public void run() {
                        AppState.getInstance().searchPaths = AppState.getInstance().searchPaths.replace("//", "/");
                    }
                }, new Runnable() {

                    @Override
                    public void run() {
                        recyclerView.scrollToPosition(0);
                        seachAll();
                    }
                });
            }
        });

        cleanFilter.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                searchEditText.setText("");
                recyclerView.scrollToPosition(0);
                searchAndOrderAsync();
            }
        });

        sortBy.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                sortByPopup(v);
            }
        });

        sortOrder.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                AppState.get().isSortAsc = !AppState.get().isSortAsc;
                searchAndOrderAsync();

            }
        });

        bindAdapter(searchAdapter);

        searchAdapter.setOnAuthorClickListener(onAuthorClick);
        searchAdapter.setOnSeriesClickListener(onSeriesClick);

        authorsAdapter.setOnItemClickListener(onAuthorSeriesClick);

        onGridList();

        if (AppDB.get().getCount() == 0) {
            seachAll();
        } else {
            searchAndOrderAsync();
        }

        initAutocomplition();
        onTintChanged();

        return view;

    }

    @Override
    public void onResume() {
        super.onResume();
        LocalBroadcastManager.getInstance(getActivity()).registerReceiver(broadcastReceiver, new IntentFilter(BooksService.INTENT_NAME));
    }


    @Override
    public void onPause() {
        super.onPause();
        LocalBroadcastManager.getInstance(getActivity()).unregisterReceiver(broadcastReceiver);
    }

    BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {

            if (BooksService.RESULT_SEARCH_FINISH.equals(intent.getStringExtra(Intent.EXTRA_TEXT))) {
                searchAndOrderAsync();
                searchEditText.setHint(R.string.search);
                onRefresh.setActivated(true);
            } else if (BooksService.RESULT_SEARCH_COUNT.equals(intent.getStringExtra(Intent.EXTRA_TEXT))) {
                int count = intent.getIntExtra(Intent.EXTRA_INDEX, 0);
                countBooks.setText("" + count);
                searchEditText.setHint(R.string.searching_please_wait_);
                onRefresh.setActivated(false);
            } else if (BooksService.RESULT_BUILD_LIBRARY.equals(intent.getStringExtra(Intent.EXTRA_TEXT))) {
                onRefresh.setActivated(false);
                searchEditText.setHint(R.string.extracting_information_from_books);
            }
        }

    };

    private void onMetaInfoClick(SEARCH_IN mode, String result) {
        searchEditText.setText(mode.getDotPrefix() + " " + result);
        AppState.get().libraryMode = prevLibModeFileMeta;
        onGridList();
        searchAndOrderAsync();
    }

    ResultResponse<String> onAuthorSeriesClick = new ResultResponse<String>() {

        @Override
        public boolean onResultRecive(String result) {
            rememberPos = ((LinearLayoutManager) recyclerView.getLayoutManager()).findLastVisibleItemPosition();
            onMetaInfoClick(SEARCH_IN.getByMode(AppState.get().libraryMode), result);
            return false;
        }
    };

    ResultResponse<String> onAuthorClick = new ResultResponse<String>() {

        @Override
        public boolean onResultRecive(String result) {
            onMetaInfoClick(SEARCH_IN.AUTHOR, result);
            return false;
        }
    };

    ResultResponse<String> onSeriesClick = new ResultResponse<String>() {

        @Override
        public boolean onResultRecive(String result) {
            onMetaInfoClick(SEARCH_IN.SERIES, result);
            return false;
        }
    };

    private void seachAll() {
        searchAdapter.clearItems();
        searchAdapter.notifyDataSetChanged();
        getActivity().startService(new Intent(getActivity(), BooksService.class).setAction(BooksService.ACTION_SEARCH_ALL));
    }

    @Override
    public void onTextRecive(String txt) {
        searchAndOrderExteral(txt);
    }

    public void searchAndOrderExteral(String text) {
        if (searchEditText != null) {
            searchEditText.setText(text);
            searchAndOrderSync(null);
        }

    }

    public void searchAndOrderAsync() {
        searchEditText.setHint(R.string.msg_loading);
        sortBy.setText(AppDB.SORT_BY.getByID(AppState.get().sortBy).getResName());
        sortOrder.setImageResource(AppState.getInstance().isSortAsc ? R.drawable.glyphicons_601_chevron_up : R.drawable.glyphicons_602_chevron_down);
        populate();
    }

    @Override
    public List<FileMeta> prepareDataInBackground() {
        String txt = searchEditText.getText().toString().trim();
        if (Arrays.asList(AppState.MODE_GRID, AppState.MODE_COVERS, AppState.MODE_LIST).contains(AppState.get().libraryMode)) {
            return AppDB.get().searchBy(txt, SORT_BY.getByID(AppState.get().sortBy), AppState.getInstance().isSortAsc);
        } else {
            return null;
        }
    }

    @Override
    public void populateDataInUI(List<FileMeta> items) {
        searchAndOrderSync(items);
    }

    public void searchAndOrderSync(List<FileMeta> loadingResults) {
        handler.removeCallbacks(sortAndSeach);

        String txt = searchEditText.getText().toString().trim();
        searchEditText.setHint(R.string.search);

        if (CMD_FULLSCREEN_ON.equals(txt)) {
            DocumentController.chooseFullScreen(getActivity(), true);
            Toast.makeText(getContext(), CMD_FULLSCREEN_ON, Toast.LENGTH_SHORT);
            searchEditText.setText("");
        }
        if (CMD_LONG_TAP_ON.equals(txt)) {
            AppState.get().longTapEnable = true;
            Toast.makeText(getContext(), CMD_LONG_TAP_ON, Toast.LENGTH_SHORT);
            searchEditText.setText("");
        }
        if (CMD_LONG_TAP_OFF.equals(txt)) {
            AppState.get().longTapEnable = false;
            Toast.makeText(getContext(), CMD_LONG_TAP_OFF, Toast.LENGTH_SHORT);
            searchEditText.setText("");
        }

        if (CMD_KEYCODE.equals(txt)) {
            Toast.makeText(getContext(), CMD_KEYCODE, Toast.LENGTH_SHORT);
            new KeyCodeDialog(getActivity(), null);
            searchEditText.setText("");
        }

        if (TxtUtils.isEmpty(txt)) {
            cleanFilter.setVisibility(View.GONE);
        } else {
            cleanFilter.setVisibility(View.VISIBLE);
        }

        if (Arrays.asList(AppState.MODE_GRID, AppState.MODE_COVERS, AppState.MODE_LIST).contains(AppState.get().libraryMode)) {
            prevLibModeFileMeta = AppState.get().libraryMode;
            searchEditText.setEnabled(true);
            sortBy.setEnabled(true);
            sortOrder.setEnabled(true);
            sortOrder.setVisibility(View.VISIBLE);

            searchAdapter.clearItems();
            if (loadingResults != null) {
                searchAdapter.getItemsList().addAll(loadingResults);
            } else {
                List<FileMeta> allSearchBy = AppDB.get().searchBy(txt, SORT_BY.getByID(AppState.get().sortBy), AppState.getInstance().isSortAsc);
                searchAdapter.getItemsList().addAll(allSearchBy);

            }
            searchAdapter.notifyDataSetChanged();
            handler.postDelayed(new Runnable() {

                @Override
                public void run() {
                    searchAdapter.notifyDataSetChanged();

                }
            }, 1000);

            // recyclerView.scrollToPosition(0);

        } else {
            prevLibModeAuthors = AppState.get().libraryMode;
            searchEditText.setEnabled(false);
            sortBy.setEnabled(false);
            sortBy.setText("");
            sortOrder.setEnabled(false);
            sortOrder.setVisibility(View.INVISIBLE);
            if (AppState.get().libraryMode == AppState.MODE_AUTHORS) {
                searchEditText.setHint(R.string.author);
            } else if (AppState.get().libraryMode == AppState.MODE_SERIES) {
                searchEditText.setHint(R.string.series);
            } else if (AppState.get().libraryMode == AppState.MODE_GENRE) {
                searchEditText.setHint(R.string.genre);
            }

            authorsAdapter.clearItems();
            List<String> list = AppDB.get().getAll(SEARCH_IN.getByMode(AppState.get().libraryMode));
            authorsAdapter.getItemsList().addAll(list);
            authorsAdapter.notifyDataSetChanged();

            recyclerView.scrollToPosition(rememberPos);

        }

        showBookCount();

    }

    Runnable sortAndSeach = new Runnable() {

        @Override
        public void run() {
            recyclerView.scrollToPosition(0);
            searchAndOrderAsync();
        }
    };

    private final TextWatcher filterTextWatcher = new TextWatcher() {

        @Override
        public void afterTextChanged(final Editable s) {
        }

        @Override
        public void beforeTextChanged(final CharSequence s, final int start, final int count, final int after) {
        }

        @Override
        public void onTextChanged(final CharSequence s, final int start, final int before, final int count) {
            if (//
            AppState.get().libraryMode == AppState.MODE_GRID || //
            AppState.get().libraryMode == AppState.MODE_LIST || //
            AppState.get().libraryMode == AppState.MODE_COVERS//
            ) {
                handler.removeCallbacks(sortAndSeach);
                if (s.toString().trim().length() == 0) {
                    handler.postDelayed(sortAndSeach, 250);
                } else {
                    handler.postDelayed(sortAndSeach, 1000);
                }
            }
        }

    };

    private ImageView onGridlList;

    private void sortByPopup(final View view) {

        PopupMenu popup = new PopupMenu(getActivity(), view);
        for (final SORT_BY sortBy : SORT_BY.values()) {
            popup.getMenu().add(sortBy.getResName()).setOnMenuItemClickListener(new OnMenuItemClickListener() {

                @Override
                public boolean onMenuItemClick(MenuItem item) {
                    AppState.getInstance().sortBy = sortBy.getIndex();
                    searchAndOrderAsync();
                    return false;
                }
            });
        }
        popup.show();
    }

    public void popupMenuTest() {
        popupMenu(onGridlList);
    }

    private void popupMenu(final ImageView onGridList) {
        PopupMenu p = new PopupMenu(getActivity(), onGridList);
        List<Integer> names = Arrays.asList(R.string.list, R.string.grid, R.string.cover, R.string.author, R.string.genre, R.string.series);
        final List<Integer> icons = Arrays.asList(R.drawable.glyphicons_114_justify, R.drawable.glyphicons_156_show_big_thumbnails, R.drawable.glyphicons_157_show_thumbnails, R.drawable.glyphicons_4_user, R.drawable.glyphicons_66_tag,
                R.drawable.glyphicons_710_list_numbered);
        final List<Integer> actions = Arrays.asList(AppState.MODE_LIST, AppState.MODE_GRID, AppState.MODE_COVERS, AppState.MODE_AUTHORS, AppState.MODE_GENRE, AppState.MODE_SERIES);

        for (int i = 0; i < names.size(); i++) {
            final int index = i;
            p.getMenu().add(names.get(i)).setIcon(icons.get(i)).setOnMenuItemClickListener(new OnMenuItemClickListener() {

                @Override
                public boolean onMenuItemClick(MenuItem item) {
                    AppState.getInstance().libraryMode = actions.get(index);
                    onGridList.setImageResource(icons.get(index));

                    if (Arrays.asList(AppState.MODE_AUTHORS, AppState.MODE_SERIES, AppState.MODE_GENRE).contains(AppState.get().libraryMode)) {
                        searchEditText.setText("");
                    }

                    onGridList();
                    searchAndOrderAsync();
                    return false;
                }
            });
        }

        p.show();

        PopupHelper.initIcons(p, TintUtil.color);
    }

    public void showBookCount() {
        countBooks.setText("" + recyclerView.getAdapter().getItemCount());
    }

    @Override
    public boolean isBackPressed() {
        if (recyclerView == null) {
            return false;
        }
        if (recyclerView.getAdapter() instanceof FileMetaAdapter) {
            String searchText = searchEditText.getText().toString();
            if (TxtUtils.isEmpty(searchText)) {
                return false;
            }

            searchEditText.setText("");
            if (prevLibModeAuthors == NONE) {
                prevLibModeAuthors = prevLibModeFileMeta;
            }
            AppState.get().libraryMode = prevLibModeAuthors;
            onGridList();
            searchAndOrderAsync();
            return true;

        } else {
            AppState.get().libraryMode = prevLibModeFileMeta;
            onGridList();
            searchAndOrderAsync();
            return true;
        }
    }

    @Override
    public void notifyFragment() {
        if (searchAdapter != null) {
            searchAdapter.notifyDataSetChanged();
        }
    }

    @Override
    public void resetFragment() {
        onGridList();
    }

}
