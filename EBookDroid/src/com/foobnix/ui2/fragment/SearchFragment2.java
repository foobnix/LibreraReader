package com.foobnix.ui2.fragment;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EmptyStackException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.Stack;

import org.greenrobot.eventbus.Subscribe;

import com.foobnix.android.utils.BaseItemLayoutAdapter;
import com.foobnix.android.utils.Dips;
import com.foobnix.android.utils.Keyboards;
import com.foobnix.android.utils.LOG;
import com.foobnix.android.utils.ResultResponse;
import com.foobnix.android.utils.TxtUtils;
import com.foobnix.dao2.FileMeta;
import com.foobnix.pdf.info.ADS;
import com.foobnix.pdf.info.R;
import com.foobnix.pdf.info.TintUtil;
import com.foobnix.pdf.info.view.EditTextHelper;
import com.foobnix.pdf.info.view.KeyCodeDialog;
import com.foobnix.pdf.info.view.MyPopupMenu;
import com.foobnix.pdf.info.widget.PrefDialogs;
import com.foobnix.pdf.info.wrapper.AppState;
import com.foobnix.pdf.info.wrapper.PopupHelper;
import com.foobnix.pdf.search.activity.msg.OpenTagMessage;
import com.foobnix.ui2.AppDB;
import com.foobnix.ui2.AppDB.SEARCH_IN;
import com.foobnix.ui2.AppDB.SORT_BY;
import com.foobnix.ui2.BooksService;
import com.foobnix.ui2.adapter.AuthorsAdapter2;
import com.foobnix.ui2.adapter.FileMetaAdapter;
import com.foobnix.ui2.fast.FastScrollRecyclerView;
import com.foobnix.ui2.fast.FastScrollStateChangeListener;
import com.nostra13.universalimageloader.core.ImageLoader;

import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnDismissListener;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.graphics.ColorUtils;
import android.support.v4.util.Pair;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.RecyclerView.OnScrollListener;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.MenuItem.OnMenuItemClickListener;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

public class SearchFragment2 extends UIFragment<FileMeta> {

    public static final String EMPTY_ID = "\u00A0";
    private String NO_SERIES = ":no-series";
    public static final Pair<Integer, Integer> PAIR = new Pair<Integer, Integer>(R.string.library, R.drawable.glyphicons_2_book_open);
    private static final String CMD_KEYCODE = "@@keycode_config";
    private static final String CMD_EDIT_AUTO_COMPLETE = "@@edit_autocomple";

    public static int NONE = -1;

    FileMetaAdapter searchAdapter;
    AuthorsAdapter2 authorsAdapter;

    TextView countBooks, sortBy;
    Handler handler;
    ImageView sortOrder, myAutoCompleteImage, cleanFilter;
    View onRefresh, secondTopPanel;
    AutoCompleteTextView searchEditText;

    final Set<String> autocomplitions = new HashSet<String>();
    public int prevLibModeFileMeta = AppState.MODE_GRID;
    public int prevLibModeAuthors = NONE;
    public int rememberPos = 0;

    private Stack<String> prevText = new Stack<String>();

    @Override
    public Pair<Integer, Integer> getNameAndIconRes() {
        return PAIR;
    }

    @Override
    public void onTintChanged() {
        TintUtil.setBackgroundFillColor(secondTopPanel, TintUtil.color);

        int colorTheme = TintUtil.getColorInDayNighth();
        colorTheme = ColorUtils.setAlphaComponent(colorTheme, 230);

        TintUtil.setStrokeColor(searchEditText, colorTheme);
        TintUtil.setUITextColor(countBooks, colorTheme);
        TintUtil.setUITextColor(searchEditText, colorTheme);

        TintUtil.setTintImageNoAlpha(cleanFilter, colorTheme);
        TintUtil.setTintImageNoAlpha(myAutoCompleteImage, colorTheme);

    }

    public void onGridList() {
        onGridList(AppState.get().libraryMode, onGridlList, searchAdapter, authorsAdapter);
    }

    public void initAutocomplition() {
        autocomplitions.clear();
        for (SEARCH_IN search : AppDB.SEARCH_IN.values()) {
            if (search == SEARCH_IN.SERIES) {
                autocomplitions.add(search.getDotPrefix() + " *");
            } else {
                autocomplitions.add(search.getDotPrefix());
            }
        }

        autocomplitions.add(CMD_KEYCODE);
        autocomplitions.add(CMD_EDIT_AUTO_COMPLETE);

        autocomplitions.addAll(AppState.get().myAutoComplete);

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

        NO_SERIES = " (" + getString(R.string.without_series) + ")";

        handler = new Handler();

        secondTopPanel = view.findViewById(R.id.secondTopPanel);
        countBooks = (TextView) view.findViewById(R.id.countBooks);
        onRefresh = view.findViewById(R.id.onRefresh);
        onRefresh.setActivated(true);
        cleanFilter = (ImageView) view.findViewById(R.id.cleanFilter);
        sortBy = (TextView) view.findViewById(R.id.sortBy);
        sortOrder = (ImageView) view.findViewById(R.id.sortOrder);
        myAutoCompleteImage = (ImageView) view.findViewById(R.id.myAutoCompleteImage);
        searchEditText = (AutoCompleteTextView) view.findViewById(R.id.filterLine);
        recyclerView = (RecyclerView) view.findViewById(R.id.recyclerView);

        myAutoCompleteImage.setVisibility(View.GONE);

        ((FastScrollRecyclerView) recyclerView).setFastScrollStateChangeListener(new FastScrollStateChangeListener() {

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
        searchEditText.setImeOptions(EditorInfo.IME_ACTION_SEARCH);
        EditTextHelper.enableKeyboardSearch(searchEditText, new Runnable() {

            @Override
            public void run() {
                Keyboards.close(searchEditText);
                Keyboards.hideNavigation(getActivity());
            }
        });

        searchAdapter = new FileMetaAdapter();
        authorsAdapter = new AuthorsAdapter2();

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
                if (true) {
                    // test();
                    // return;
                }

                if (!onRefresh.isActivated()) {
                    Toast.makeText(getActivity(), R.string.extracting_information_from_books, Toast.LENGTH_LONG).show();
                    return;
                }
                PrefDialogs.chooseFolderDialog(getActivity(), new Runnable() {

                    @Override
                    public void run() {
                        AppState.get().searchPaths = AppState.get().searchPaths.replace("//", "/");
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
            checkForDeleteBooks();
            searchAndOrderAsync();
        }

        initAutocomplition();
        onTintChanged();

        recyclerView.addOnScrollListener(new OnScrollListener() {
            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
            }
        });

        myAutoCompleteImage.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                showAutoCompleteDialog();
            }
        });

        return view;

    }

    public void showAutoCompleteDialog() {

        final AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle(R.string.search_history_and_autocomplete);

        final ListView list = new ListView(getActivity());

        final List<String> items = new ArrayList<String>(AppState.get().myAutoComplete);
        Collections.sort(items);
        BaseItemLayoutAdapter<String> adapter = new BaseItemLayoutAdapter<String>(getActivity(), R.layout.path_item, items) {
            @Override
            public void populateView(View layout, int position, final String item) {
                TextView text = layout.findViewById(R.id.browserPath);
                ImageView delete = layout.findViewById(R.id.delete);

                text.setText(item);

                delete.setOnClickListener(new OnClickListener() {

                    @Override
                    public void onClick(View v) {
                        autocomplitions.remove(item);
                        items.remove(item);
                        AppState.get().myAutoComplete.remove(item);
                        notifyDataSetChanged();
                    }
                });
                layout.setOnClickListener(new OnClickListener() {

                    @Override
                    public void onClick(View v) {
                        searchEditText.setText(item);
                        searchAndOrderAsync();
                    }
                });

            }
        };

        list.setAdapter(adapter);

        builder.setView(list);

        builder.setPositiveButton(R.string.close, new AlertDialog.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {
            }
        });

        AlertDialog create = builder.create();
        create.setOnDismissListener(new OnDismissListener() {

            @Override
            public void onDismiss(DialogInterface dialog) {
                Keyboards.hideNavigation(getActivity());
            }
        });
        create.show();

        Keyboards.close(getActivity());

    }

    @Override
    public void onResume() {
        super.onResume();
        LocalBroadcastManager.getInstance(getActivity()).registerReceiver(broadcastReceiver, new IntentFilter(BooksService.INTENT_NAME));

    }

    @Override
    public void onStop() {
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
            if (result.contains(NO_SERIES)) {
                onMetaInfoClick(SEARCH_IN.getByPrefix(searchEditText.getText().toString()), result);
            } else {
                onMetaInfoClick(SEARCH_IN.SERIES, result);
            }
            return false;
        }
    };

    private void seachAll() {
        searchAdapter.clearItems();
        searchAdapter.notifyDataSetChanged();
        getActivity().startService(new Intent(getActivity(), BooksService.class).setAction(BooksService.ACTION_SEARCH_ALL));
    }

    public void checkForDeleteBooks() {
        getActivity().startService(new Intent(getActivity(), BooksService.class).setAction(BooksService.ACTION_REMOVE_DELETED));
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
        sortOrder.setImageResource(AppState.get().isSortAsc ? R.drawable.glyphicons_601_chevron_up : R.drawable.glyphicons_602_chevron_down);
        populate();
    }

    @Subscribe
    public void onShowTag(OpenTagMessage msg) {
        searchAndOrderExteral("@tags " + msg.getTagName());

    }

    @Override
    public List<FileMeta> prepareDataInBackground() {
        String txt = searchEditText.getText().toString().trim();
        if (Arrays.asList(AppState.MODE_GRID, AppState.MODE_COVERS, AppState.MODE_LIST, AppState.MODE_LIST_COMPACT).contains(AppState.get().libraryMode)) {

            if (!prevText.contains(txt)) {
                prevText.push(txt);
            }
            if (TxtUtils.isEmpty(txt)) {
                prevText.clear();
            }

            boolean isSearchOnlyEmpy = txt.contains(NO_SERIES);
            if (isSearchOnlyEmpy) {
                txt = txt.replace(NO_SERIES, "");
            }

            List<FileMeta> searchBy = AppDB.get().searchBy(txt, SORT_BY.getByID(AppState.get().sortBy), AppState.get().isSortAsc);

            List<String> result = new ArrayList<String>();
            boolean byGenre = txt.startsWith(SEARCH_IN.GENRE.getDotPrefix());
            boolean byAuthor = txt.startsWith(SEARCH_IN.AUTHOR.getDotPrefix());
            if (!txt.contains("::") && (byGenre || byAuthor)) {
                if (isSearchOnlyEmpy) {
                    Iterator<FileMeta> iterator = searchBy.iterator();
                    while (iterator.hasNext()) {
                        if (TxtUtils.isNotEmpty(iterator.next().getSequence())) {
                            iterator.remove();
                        }
                    }
                    return searchBy;
                }

                boolean hasEmpySeries = false;
                for (FileMeta it : searchBy) {
                    String sequence = it.getSequence();
                    TxtUtils.addFilteredGenreSeries(sequence, result, true);
                    if (!hasEmpySeries && TxtUtils.isEmpty(sequence)) {
                        hasEmpySeries = true;
                    }
                }
                Collections.sort(result, String.CASE_INSENSITIVE_ORDER);
                Collections.reverse(result);
                String genreName = txt.replace(byGenre ? "@genre " : "@author ", "");
                for (String it : result) {
                    FileMeta fm = new FileMeta();
                    fm.setCusType(FileMetaAdapter.DISPALY_TYPE_SERIES);
                    fm.setSequence(it);
                    searchBy.add(0, fm);
                }
                if (hasEmpySeries && !result.isEmpty()) {
                    FileMeta fm = new FileMeta();
                    fm.setCusType(FileMetaAdapter.DISPALY_TYPE_SERIES);
                    fm.setSequence(genreName + NO_SERIES);
                    searchBy.add(result.size(), fm);
                }
            }

            return searchBy;
        } else {
            return null;
        }
    }

    @Override
    public void populateDataInUI(List<FileMeta> items) {
        searchAndOrderSync(items);
    }

    public void toastState(String command, boolean state) {
        Toast.makeText(getContext(), command + " [" + (state ? "ON" : "OFF") + "]", Toast.LENGTH_LONG).show();
    }

    public void searchAndOrderSync(List<FileMeta> loadingResults) {
        handler.removeCallbacks(sortAndSeach);

        String txt = searchEditText.getText().toString().trim();
        searchEditText.setHint(R.string.search);

        if (CMD_KEYCODE.equals(txt)) {
            new KeyCodeDialog(getActivity(), null);
            searchEditText.setText("");
        }

        if (CMD_EDIT_AUTO_COMPLETE.equals(txt)) {
            searchEditText.setText("");
            showAutoCompleteDialog();
        }

        if (TxtUtils.isEmpty(txt)) {
            cleanFilter.setVisibility(View.GONE);
        } else {
            cleanFilter.setVisibility(View.VISIBLE);
        }

        if (Arrays.asList(AppState.MODE_GRID, AppState.MODE_COVERS, AppState.MODE_LIST, AppState.MODE_LIST_COMPACT).contains(AppState.get().libraryMode)) {
            prevLibModeFileMeta = AppState.get().libraryMode;
            searchEditText.setEnabled(true);
            sortBy.setEnabled(true);
            sortOrder.setEnabled(true);
            sortOrder.setVisibility(View.VISIBLE);

            searchAdapter.clearItems();
            if (loadingResults != null) {
                searchAdapter.getItemsList().addAll(loadingResults);
            } else {
                List<FileMeta> allSearchBy = AppDB.get().searchBy(txt, SORT_BY.getByID(AppState.get().sortBy), AppState.get().isSortAsc);
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

            String empty = "";
            if (AppState.get().libraryMode == AppState.MODE_AUTHORS) {
                searchEditText.setHint(R.string.author);
                empty = EMPTY_ID + getString(R.string.no_author);
            } else if (AppState.get().libraryMode == AppState.MODE_SERIES) {
                searchEditText.setHint(R.string.serie);
                empty = EMPTY_ID + getString(R.string.no_serie);
            } else if (AppState.get().libraryMode == AppState.MODE_GENRE) {
                searchEditText.setHint(R.string.genre);
                empty = EMPTY_ID + getString(R.string.no_genre);
            } else if (AppState.get().libraryMode == AppState.MODE_KEYWORDS) {
                searchEditText.setHint(R.string.keywords);
                empty = EMPTY_ID + getString(R.string.no_keywords);
            } else if (AppState.get().libraryMode == AppState.MODE_USER_TAGS) {
                searchEditText.setHint(R.string.my_tags);
                empty = EMPTY_ID + getActivity().getString(R.string.no_tag);
            }

            authorsAdapter.clearItems();
            List<String> list = AppDB.get().getAll(SEARCH_IN.getByMode(AppState.get().libraryMode));
            list.add(0, empty);
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
    Runnable hideKeyboard = new Runnable() {

        @Override
        public void run() {
            Keyboards.close(searchEditText);
            Keyboards.hideNavigation(getActivity());
        }
    };

    Runnable saveAutoComplete = new Runnable() {

        @Override
        public void run() {
            String txt = searchEditText.getText().toString().trim();
            if (TxtUtils.isNotEmpty(txt) && !txt.startsWith("@@") && !AppState.get().myAutoComplete.contains(txt)) {
                if (!searchAdapter.getItemsList().isEmpty()) {
                    AppState.get().myAutoComplete.add(txt);
                    autocomplitions.add(txt);
                    updateFilterListAdapter();
                    myAutoCompleteImage.setVisibility(View.VISIBLE);
                }

            }

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
            AppState.get().libraryMode == AppState.MODE_LIST_COMPACT || //
            AppState.get().libraryMode == AppState.MODE_COVERS//
            ) {
                handler.removeCallbacks(sortAndSeach);
                handler.removeCallbacks(hideKeyboard);
                if (s.toString().trim().length() == 0) {
                    handler.postDelayed(sortAndSeach, 250);
                    handler.postDelayed(hideKeyboard, 2000);
                } else {
                    handler.postDelayed(sortAndSeach, 1000);
                }
            }
            myAutoCompleteImage.setVisibility(View.GONE);
            handler.removeCallbacks(saveAutoComplete);

            if (AppState.get().myAutoComplete.contains(s.toString().trim())) {
                myAutoCompleteImage.setVisibility(View.VISIBLE);
            } else {
                handler.postDelayed(saveAutoComplete, 10000);
            }

        }

    };

    private ImageView onGridlList;

    private void sortByPopup(final View view) {


        MyPopupMenu popup = new MyPopupMenu(getActivity(), view);
        for (final SORT_BY sortBy : SORT_BY.values()) {
            popup.getMenu().add(sortBy.getResName()).setOnMenuItemClickListener(new OnMenuItemClickListener() {

                @Override
                public boolean onMenuItemClick(MenuItem item) {
                    AppState.get().sortBy = sortBy.getIndex();
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
        MyPopupMenu p = new MyPopupMenu(getActivity(), onGridList);
        PopupHelper.addPROIcon(p, getActivity());
        List<Integer> names = Arrays.asList(R.string.list, //
                R.string.compact, //
                R.string.grid, //
                R.string.cover, //
                R.string.author, //
                R.string.genre, //
                R.string.serie, //
                R.string.keywords, //
                R.string.my_tags //
        );

        final List<Integer> icons = Arrays.asList(R.drawable.glyphicons_114_justify, //
                R.drawable.glyphicons_114_justify_compact, //
                R.drawable.glyphicons_156_show_big_thumbnails, //
                R.drawable.glyphicons_157_show_thumbnails, //
                R.drawable.glyphicons_4_user, //
                R.drawable.glyphicons_66_tag, //
                R.drawable.glyphicons_710_list_numbered, //
                R.drawable.glyphicons_67_keywords, //
                R.drawable.glyphicons_67_tags);
        final List<Integer> actions = Arrays.asList(AppState.MODE_LIST, AppState.MODE_LIST_COMPACT, //
                AppState.MODE_GRID, //
                AppState.MODE_COVERS, //
                AppState.MODE_AUTHORS, //
                AppState.MODE_GENRE, //
                AppState.MODE_SERIES, //
                AppState.MODE_KEYWORDS, //
                AppState.MODE_USER_TAGS); //

        for (int i = 0; i < names.size(); i++) {
            final int index = i;
            p.getMenu().add(names.get(i)).setIcon(icons.get(i)).setOnMenuItemClickListener(new OnMenuItemClickListener() {

                @Override
                public boolean onMenuItemClick(MenuItem item) {
                    AppState.get().libraryMode = actions.get(index);
                    onGridList.setImageResource(icons.get(index));

                    if (Arrays.asList(AppState.MODE_AUTHORS, AppState.MODE_SERIES, AppState.MODE_GENRE, AppState.MODE_USER_TAGS, AppState.MODE_KEYWORDS).contains(AppState.get().libraryMode)) {
                        searchEditText.setText("");
                    }

                    onGridList();
                    searchAndOrderAsync();
                    return false;
                }
            });
        }

        p.show();
        return;

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

            if (searchText.startsWith("@")) {
                try {
                    prevText.pop();
                    String pop = prevText.pop();
                    LOG.d("pop", pop);
                    if (TxtUtils.isNotEmpty(pop)) {
                        searchEditText.setText(pop);
                        searchAndOrderAsync();
                        return true;
                    }
                } catch (EmptyStackException e) {
                    LOG.e(e);
                }
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
        searchAndOrderAsync();
    }

}
