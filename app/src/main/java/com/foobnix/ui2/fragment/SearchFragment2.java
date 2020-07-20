package com.foobnix.ui2.fragment;

import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.ArrayAdapter;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.graphics.ColorUtils;
import androidx.core.util.Pair;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.RecyclerView.OnScrollListener;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.foobnix.android.utils.Apps;
import com.foobnix.android.utils.BaseItemLayoutAdapter;
import com.foobnix.android.utils.Dips;
import com.foobnix.android.utils.Keyboards;
import com.foobnix.android.utils.LOG;
import com.foobnix.android.utils.ResultResponse;
import com.foobnix.android.utils.StringDB;
import com.foobnix.android.utils.TxtUtils;
import com.foobnix.dao2.FileMeta;
import com.foobnix.model.AppState;
import com.foobnix.pdf.info.IMG;
import com.foobnix.pdf.info.R;
import com.foobnix.pdf.info.TintUtil;
import com.foobnix.pdf.info.databinding.FragmentSearch2Binding;
import com.foobnix.pdf.info.view.EditTextHelper;
import com.foobnix.pdf.info.view.KeyCodeDialog;
import com.foobnix.pdf.info.view.MyPopupMenu;
import com.foobnix.pdf.info.widget.DialogTranslateFromTo;
import com.foobnix.pdf.info.widget.PrefDialogs;
import com.foobnix.pdf.info.wrapper.PopupHelper;
import com.foobnix.pdf.search.activity.msg.OpenTagMessage;
import com.foobnix.ui2.AppDB;
import com.foobnix.ui2.AppDB.SEARCH_IN;
import com.foobnix.ui2.AppDB.SORT_BY;
import com.foobnix.ui2.BooksService;
import com.foobnix.ui2.adapter.AuthorsAdapter2;
import com.foobnix.ui2.adapter.FileMetaAdapter;

import org.ebookdroid.LibreraApp;
import org.greenrobot.eventbus.Subscribe;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EmptyStackException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.Stack;

public class SearchFragment2 extends UIFragment<FileMeta> {
    public static final String EMPTY_ID = "\u00A0";
    public static final Pair<Integer, Integer> PAIR = new Pair<>(R.string.library, R.drawable.glyphicons_2_book_open);
    private static final String CMD_KEYCODE = "@@keycode_config";
    private static final String CMD_EDIT_AUTO_COMPLETE = "@@edit_autocomple";
    private static final String CMD_MARGIN = "@@keycode_margin";
    public static int NONE = -1;
    final Set<String> autocomplitions = new HashSet<>();
    public int prevLibModeFileMeta = AppState.MODE_GRID;
    public int prevLibModeAuthors = NONE;
    public int rememberPos = 0;
    FileMetaAdapter searchAdapter;
    AuthorsAdapter2 authorsAdapter;
    private FragmentSearch2Binding binding;
    Handler handler;
    int countTitles = 0;
    Runnable hideKeyboard = new Runnable() {
        @Override
        public void run() {
            Keyboards.close(binding.filterLine);
            Keyboards.hideNavigation(getActivity());
        }
    };
    Runnable saveAutoComplete = new Runnable() {
        @Override
        public void run() {
            String txt = binding.filterLine.getText().toString().trim();
            if (TxtUtils.isNotEmpty(txt) && !txt.startsWith("@@") && !StringDB.contains(AppState.get().myAutoCompleteDb, txt)) {
                if (!searchAdapter.getItemsList().isEmpty()) {
                    StringDB.add(AppState.get().myAutoCompleteDb, txt, (db) -> AppState.get().myAutoCompleteDb = db);
                    autocomplitions.add(txt);
                    updateFilterListAdapter();
                    binding.myAutoCompleteImage.setVisibility(View.VISIBLE);
                }
            }
        }
    };
    BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            if (BooksService.RESULT_SEARCH_FINISH.equals(intent.getStringExtra(Intent.EXTRA_TEXT))) {
                searchAndOrderAsync();
                binding.filterLine.setHint(R.string.search);
                binding.onRefresh.setActivated(true);
            } else if (BooksService.RESULT_SEARCH_COUNT.equals(intent.getStringExtra(Intent.EXTRA_TEXT))) {
                int count = intent.getIntExtra("android.intent.extra.INDEX", 0);
                if (count > 0) {
                    binding.countBooks.setText("" + count);
                }
                binding.filterLine.setHint(R.string.searching_please_wait_);
                binding.onRefresh.setActivated(false);
            } else if (BooksService.RESULT_BUILD_LIBRARY.equals(intent.getStringExtra(Intent.EXTRA_TEXT))) {
                binding.onRefresh.setActivated(false);
                binding.filterLine.setHint(R.string.extracting_information_from_books);
            }
        }

    };
    Runnable sortAndSearch = () -> {
        recyclerView.scrollToPosition(0);
        searchAndOrderAsync();
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
                handler.removeCallbacks(sortAndSearch);
                handler.removeCallbacks(hideKeyboard);
                if (s.toString().trim().length() == 0) {
                    handler.postDelayed(sortAndSearch, 250);
                    handler.postDelayed(hideKeyboard, 2000);
                } else {
                    handler.postDelayed(sortAndSearch, 1000);
                }
            }
            binding.myAutoCompleteImage.setVisibility(View.GONE);
            handler.removeCallbacks(saveAutoComplete);

            if (StringDB.contains(AppState.get().myAutoCompleteDb, s.toString().trim())) {
                binding.myAutoCompleteImage.setVisibility(View.VISIBLE);
            } else {
                handler.postDelayed(saveAutoComplete, 10000);
            }
        }
    };
    private String NO_SERIES = ":no-series";
    private final Stack<String> prevText = new Stack<>();
    ResultResponse<String> onAuthorSeriesClick = result -> {
        rememberPos = ((LinearLayoutManager) recyclerView.getLayoutManager()).findLastVisibleItemPosition();
        onMetaInfoClick(SEARCH_IN.getByMode(AppState.get().libraryMode), result);
        return false;
    };
    ResultResponse<String> onAuthorClick = result -> {
        onMetaInfoClick(SEARCH_IN.AUTHOR, result);
        return false;
    };
    ResultResponse<String> onSeriesClick = new ResultResponse<String>() {
        @Override
        public boolean onResultRecive(String result) {
            if (result.contains(NO_SERIES)) {
                onMetaInfoClick(SEARCH_IN.getByPrefix(binding.filterLine.getText().toString()), result);
            } else {
                onMetaInfoClick(SEARCH_IN.SERIES, result);
            }
            return false;
        }
    };

    @Override
    public Pair<Integer, Integer> getNameAndIconRes() {
        return PAIR;
    }

    @Override
    public void onTintChanged() {
        TintUtil.setBackgroundFillColor(binding.secondTopPanel, TintUtil.color);

        int colorTheme = TintUtil.getColorInDayNighth();
        colorTheme = ColorUtils.setAlphaComponent(colorTheme, 230);

        TintUtil.setUITextColor(binding.countBooks, colorTheme);

        TintUtil.setTintImageNoAlpha(binding.cleanFilter, colorTheme);
        TintUtil.setTintImageNoAlpha(binding.myAutoCompleteImage, colorTheme);

        if (AppState.get().appTheme == AppState.THEME_DARK_OLED || (AppState.get().appTheme == AppState.THEME_DARK && TintUtil.color == Color.BLACK)) {
            binding.filterLine.setBackgroundResource(R.drawable.bg_search_edit_night);
        } else {
            binding.filterLine.setBackgroundResource(R.drawable.bg_search_edit);
        }
        TintUtil.setStrokeColor(binding.filterLine, colorTheme);
        TintUtil.setUITextColor(binding.filterLine, colorTheme);

        if (AppState.get().appTheme == AppState.THEME_INK) {
            binding.filterLine.setBackgroundResource(R.drawable.bg_search_edit);
            TintUtil.setStrokeColor(binding.filterLine, Color.BLACK);
            TintUtil.setUITextColor(binding.filterLine, Color.BLACK);
            binding.countBooks.setTextColor(Color.BLACK);
        }
    }

    public void onGridList() {
        onGridList(AppState.get().libraryMode, binding.onGridList, searchAdapter, authorsAdapter);
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
        autocomplitions.add(CMD_MARGIN);
        autocomplitions.add(CMD_EDIT_AUTO_COMPLETE);

        autocomplitions.addAll(StringDB.asList(AppState.get().myAutoCompleteDb));

        updateFilterListAdapter();
    }

    public void updateFilterListAdapter() {
        try {
            ArrayList<String> list = new ArrayList<>(autocomplitions);
            Collections.sort(list);
            ArrayAdapter<String> adapter = new ArrayAdapter<>(getActivity(), android.R.layout.simple_spinner_dropdown_item, list);
            binding.filterLine.setAdapter(adapter);
            binding.filterLine.setThreshold(1);
        } catch (Exception e) {
            LOG.e(e);
        }
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentSearch2Binding.inflate(inflater, container, false);

        LOG.d("Context-SF-getContext", getContext());
        LOG.d("Context-SF-getApplicationContext", getActivity().getApplicationContext());
        LOG.d("Context-SF-getBaseContext", getActivity().getBaseContext());
        LOG.d("Context-SF-LibreraApp.context", LibreraApp.context);

        LOG.d("SearchFragment2 onCreateView");

        NO_SERIES = " (" + getString(R.string.without_series) + ")";

        handler = new Handler();

        binding.onRefresh.setActivated(true);
        recyclerView = binding.recyclerView;

        if (AppState.get().appTheme == AppState.THEME_DARK_OLED || (AppState.get().appTheme == AppState.THEME_DARK && TintUtil.color == Color.BLACK)) {
            binding.filterLine.setBackgroundResource(R.drawable.bg_search_edit_night);
        }

        binding.myAutoCompleteImage.setVisibility(View.GONE);

        binding.filterLine.addTextChangedListener(filterTextWatcher);
        binding.filterLine.setImeOptions(EditorInfo.IME_ACTION_SEARCH);
        EditTextHelper.enableKeyboardSearch(binding.filterLine, () -> {
            Keyboards.close(binding.filterLine);
            Keyboards.hideNavigation(getActivity());
        });

        searchAdapter = new FileMetaAdapter();

        authorsAdapter = new AuthorsAdapter2();

        binding.onGridList.setOnClickListener(v -> popupMenu());

        binding.onRefresh.setOnClickListener(v -> {
            if (true) {
                // test();
                // return;
            }

            if (!binding.onRefresh.isActivated()) {
                Toast.makeText(getActivity(), R.string.extracting_information_from_books, Toast.LENGTH_LONG).show();
                return;
            }
            PrefDialogs.chooseFolderDialog(getActivity(), () -> {
                //BookCSS.get().searchPaths = BookCSS.get().searchPaths.replace("//", "/");
            }, () -> {
                recyclerView.scrollToPosition(0);
                searchAll();
            });
        });

        binding.cleanFilter.setOnClickListener(v -> {
            binding.filterLine.setText("");
            recyclerView.scrollToPosition(0);
            searchAndOrderAsync();
        });

        binding.sortBy.setOnClickListener(this::sortByPopup);

        binding.sortOrder.setOnClickListener(v -> {
            AppState.get().isSortAsc = !AppState.get().isSortAsc;
            searchAndOrderAsync();
        });
        binding.sortOrder.setOnLongClickListener(v -> {
            AppState.get().isVisibleSorting = !AppState.get().isVisibleSorting;
            binding.sortOrder.setVisibility(TxtUtils.visibleIf(AppState.get().isVisibleSorting));
            return true;
        });

        binding.sortBy.setOnLongClickListener(v -> {
            AppState.get().isVisibleSorting = !AppState.get().isVisibleSorting;
            binding.sortOrder.setVisibility(TxtUtils.visibleIf(AppState.get().isVisibleSorting));
            return true;
        });
        binding.sortOrder.setVisibility(TxtUtils.visibleIf(AppState.get().isVisibleSorting));

        bindAdapter(searchAdapter);

        searchAdapter.setOnAuthorClickListener(onAuthorClick);
        searchAdapter.setOnSeriesClickListener(onSeriesClick);

        authorsAdapter.setOnItemClickListener(onAuthorSeriesClick);

        onGridList();

        if (AppDB.get().getCount() == 0) {
            //detect crash here! not start second time!!!
            searchAll();
        } else {
            checkForDeleteBooks();
            searchAndOrderAsync();
        }

        initAutocomplition();
        onTintChanged();

        recyclerView.addOnScrollListener(new OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
            }
        });

        binding.myAutoCompleteImage.setOnClickListener(v -> showAutoCompleteDialog());

        return binding.getRoot();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    public void showAutoCompleteDialog() {
        final AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle(R.string.search_history_and_autocomplete);

        final ListView list = new ListView(getActivity());

        final List<String> items = new ArrayList<>(StringDB.asList(AppState.get().myAutoCompleteDb));
        Collections.sort(items);
        BaseItemLayoutAdapter<String> adapter = new BaseItemLayoutAdapter<String>(getActivity(), R.layout.path_item, items) {
            @Override
            public void populateView(View layout, int position, final String item) {
                TextView text = layout.findViewById(R.id.browserPath);
                ImageView delete = layout.findViewById(R.id.delete);

                text.setText(item);

                delete.setOnClickListener(v -> {
                    autocomplitions.remove(item);
                    items.remove(item);

                    StringDB.delete(AppState.get().myAutoCompleteDb, item, (db) -> AppState.get().myAutoCompleteDb = db);
                    notifyDataSetChanged();
                });
                layout.setOnClickListener(v -> {
                    binding.filterLine.setText(item);
                    searchAndOrderAsync();
                });
            }
        };

        list.setAdapter(adapter);

        builder.setView(list);

        builder.setPositiveButton(R.string.close, (dialog, which) -> { });

        AlertDialog create = builder.create();
        create.setOnDismissListener(dialog -> Keyboards.hideNavigation(getActivity()));
        create.show();

        Keyboards.close(getActivity());
    }

    @Override
    public void onResume() {
        super.onResume();
        LocalBroadcastManager.getInstance(requireContext()).registerReceiver(broadcastReceiver, new IntentFilter(BooksService.INTENT_NAME));
    }

    @Override
    public void onStop() {
        super.onStop();
        LocalBroadcastManager.getInstance(requireContext()).unregisterReceiver(broadcastReceiver);
    }

    private void onMetaInfoClick(SEARCH_IN mode, String result) {
        if (mode == SEARCH_IN.SERIES) {
            result = "," + result + ",";
        }

        binding.filterLine.setText(mode.getDotPrefix() + " " + result);
        AppState.get().libraryMode = prevLibModeFileMeta;
        onGridList();
        searchAndOrderAsync();
    }

    private void searchAll() {
        try {
            searchAdapter.clearItems();
            searchAdapter.notifyDataSetChanged();
            IMG.clearMemoryCache();
            IMG.clearDiscCache();
            BooksService.startForeground(getActivity(), BooksService.ACTION_SEARCH_ALL);
        } catch (Exception e) {
            LOG.e(e);
        }
    }

    public void checkForDeleteBooks() {
        try {
            BooksService.startForeground(getActivity(), BooksService.ACTION_REMOVE_DELETED);
        } catch (Exception e) {
            LOG.e(e);
        }
    }

    @Override
    public void onTextRecive(String txt) {
        searchAndOrderExternal(txt);
    }

    public void searchAndOrderExternal(String text) {
        binding.filterLine.setText(text);
        searchAndOrderSync(null);
    }

    public void searchAndOrderAsync() {
        binding.filterLine.setHint(R.string.msg_loading);
        binding.sortBy.setText(AppDB.SORT_BY.getByID(AppState.get().sortBy).getResName());

        binding.sortOrder.setImageResource(AppState.get().isSortAsc ? R.drawable.glyphicons_602_chevron_down : R.drawable.glyphicons_601_chevron_up);

        String order = getString(AppState.get().isSortAsc ? R.string.ascending : R.string.descending);
        binding.sortBy.setContentDescription(getString(R.string.cd_sort_results) + " " + binding.sortBy.getText());
        binding.sortOrder.setContentDescription(order);
        populate();

        Apps.accessibilityText(getActivity(), "" + binding.sortBy.getContentDescription());
    }

    @Subscribe
    public void onShowTag(OpenTagMessage msg) {
        searchAndOrderExternal("@tags " + msg.getTagName());
    }

    @Override
    public List<FileMeta> prepareDataInBackground() {
        String txt = binding.filterLine.getText().toString().trim();
        countTitles = 0;
        if (Arrays.asList(AppState.MODE_GRID, AppState.MODE_COVERS, AppState.MODE_LIST, AppState.MODE_LIST_COMPACT).contains(AppState.get().libraryMode)) {
            if (!prevText.contains(txt)) {
                prevText.push(txt);
            }
            if (TxtUtils.isEmpty(txt)) {
                prevText.clear();
            }

            boolean isSearchOnlyEmpty = txt.contains(NO_SERIES);
            if (isSearchOnlyEmpty) {
                txt = txt.replace(NO_SERIES, "");
            }

            List<FileMeta> searchBy = AppDB.get().searchBy(txt, SORT_BY.getByID(AppState.get().sortBy), AppState.get().isSortAsc);

            List<String> result = new ArrayList<>();
            boolean byGenre = txt.startsWith(SEARCH_IN.GENRE.getDotPrefix());
            boolean byAuthor = txt.startsWith(SEARCH_IN.AUTHOR.getDotPrefix());
            if (!txt.contains("::") && (byGenre || byAuthor)) {
                if (isSearchOnlyEmpty) {
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

            if (//
                    AppState.get().sortBy == SORT_BY.PATH.getIndex() ||//
                            AppState.get().sortBy == SORT_BY.LANGUAGE.getIndex() ||//
                            AppState.get().sortBy == SORT_BY.PUBLICATION_YEAR.getIndex() ||
                            AppState.get().sortBy == SORT_BY.PUBLISHER.getIndex()) {//

                List<FileMeta> res = new ArrayList<FileMeta>();
                String last = null;

                String extDir = Environment.getExternalStorageDirectory().getPath();

                for (FileMeta it : searchBy) {
                    String parentName = "";
                    if (AppState.get().sortBy == SORT_BY.PUBLISHER.getIndex()) {
                        parentName = "" + it.getPublisher();
                    } else if (AppState.get().sortBy == SORT_BY.PUBLICATION_YEAR.getIndex()) {
                        parentName = "" + it.getYear();
                    } else if (AppState.get().sortBy == SORT_BY.PATH.getIndex()) {
                        parentName = it.getParentPath();
                        if (parentName != null) {
                            parentName = parentName.replace(extDir, "");
                        }
                    } else if (AppState.get().sortBy == SORT_BY.LANGUAGE.getIndex()) {
                        String lang = it.getLang();
                        if (TxtUtils.isEmpty(lang)) {
                            parentName = "---";
                        } else {
                            parentName = DialogTranslateFromTo.getLanuageByCode(lang);
                        }
                    }
                    if (parentName != null && !parentName.equals(last)) {
                        FileMeta fm = new FileMeta();
                        fm.setCusType(FileMetaAdapter.DISPALY_TYPE_LAYOUT_TITLE_DIVIDER);
                        fm.setTitle(parentName);
                        last = parentName;
                        res.add(fm);
                        countTitles++;
                    }
                    res.add(it);
                }
                searchBy = res;
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

    public void searchAndOrderSync(List<FileMeta> loadingResults) {
        handler.removeCallbacks(sortAndSearch);

        String txt = binding.filterLine.getText().toString().trim();
        binding.filterLine.setHint(R.string.search);

        if (CMD_KEYCODE.equals(txt)) {
            new KeyCodeDialog(getActivity(), null);
            binding.filterLine.setText("");
        }
        if (CMD_MARGIN.equals(txt)) {
            SwipeRefreshLayout layout = getActivity().findViewById(R.id.swipeRefreshLayout);
            final FrameLayout.LayoutParams layoutParams = (FrameLayout.LayoutParams) layout.getLayoutParams();
            layoutParams.rightMargin = Dips.screenWidth() / 4;
            layout.setLayoutParams(layoutParams);
            binding.filterLine.setText("");
        }

        if (CMD_EDIT_AUTO_COMPLETE.equals(txt)) {
            binding.filterLine.setText("");
            showAutoCompleteDialog();
        }

        if (TxtUtils.isEmpty(txt)) {
            binding.cleanFilter.setVisibility(View.GONE);
        } else {
            binding.cleanFilter.setVisibility(View.VISIBLE);
        }

        if (Arrays.asList(AppState.MODE_GRID, AppState.MODE_COVERS, AppState.MODE_LIST, AppState.MODE_LIST_COMPACT).contains(AppState.get().libraryMode)) {
            prevLibModeFileMeta = AppState.get().libraryMode;
            binding.filterLine.setEnabled(true);
            binding.sortBy.setEnabled(true);
            binding.sortOrder.setEnabled(true);
            if (AppState.get().isVisibleSorting) {
                binding.sortOrder.setVisibility(View.VISIBLE);
            }

            searchAdapter.clearItems();
            if (loadingResults != null) {
                searchAdapter.getItemsList().addAll(loadingResults);
            } else {
                List<FileMeta> allSearchBy = AppDB.get().searchBy(txt, SORT_BY.getByID(AppState.get().sortBy), AppState.get().isSortAsc);
                searchAdapter.getItemsList().addAll(allSearchBy);
            }
            searchAdapter.notifyDataSetChanged();
            handler.postDelayed(() -> searchAdapter.notifyDataSetChanged(), 1000);

            // recyclerView.scrollToPosition(0);
        } else {
            prevLibModeAuthors = AppState.get().libraryMode;
            binding.filterLine.setEnabled(false);
            binding.sortBy.setEnabled(false);
            binding.sortBy.setText("");
            binding.sortOrder.setEnabled(false);
            binding.sortOrder.setVisibility(View.INVISIBLE);

            String empty = "";
            if (AppState.get().libraryMode == AppState.MODE_AUTHORS) {
                binding.filterLine.setHint(R.string.author);
                empty = EMPTY_ID + getString(R.string.no_author);
            } else if (AppState.get().libraryMode == AppState.MODE_SERIES) {
                binding.filterLine.setHint(R.string.serie);
                empty = EMPTY_ID + getString(R.string.no_serie);
            } else if (AppState.get().libraryMode == AppState.MODE_GENRE) {
                binding.filterLine.setHint(R.string.genre);
                empty = EMPTY_ID + getString(R.string.no_genre);
            } else if (AppState.get().libraryMode == AppState.MODE_KEYWORDS) {
                binding.filterLine.setHint(R.string.keywords);
                empty = EMPTY_ID + getString(R.string.no_keywords);
            } else if (AppState.get().libraryMode == AppState.MODE_USER_TAGS) {
                binding.filterLine.setHint(R.string.my_tags);
                empty = EMPTY_ID + getString(R.string.no_tag);
            } else if (AppState.get().libraryMode == AppState.MODE_LANGUAGES) {
                binding.filterLine.setHint(R.string.language);
                empty = EMPTY_ID + getString(R.string.no_language);
            } else if (AppState.get().libraryMode == AppState.MODE_PUBLICATION_DATE) {
                binding.filterLine.setHint(R.string.publication_date);
                empty = EMPTY_ID + getString(R.string.empy);
            } else if (AppState.get().libraryMode == AppState.MODE_PUBLISHER) {
                binding.filterLine.setHint(R.string.publisher);
                empty = EMPTY_ID + getString(R.string.empy);
            }

            authorsAdapter.clearItems();
            List<String> list = AppDB.get().getAll(SEARCH_IN.getByMode(AppState.get().libraryMode));
            if (AppState.get().libraryMode == AppState.MODE_LANGUAGES) {
                List<String> res = new ArrayList<>();
                String prev = null;
                for (String ln : list) {
                    if (TxtUtils.isEmpty(ln)) {
                        ln = "";
                    } else if (ln.length() > 2) {
                        ln = ln.substring(0, 2);
                    }

                    String full = DialogTranslateFromTo.getLanuageByCode(ln);
                    String lnLow = ln.toLowerCase(Locale.US);

                    if (!lnLow.equals(prev)) {
                        prev = lnLow;
                        res.add(full + " (" + lnLow + ")");
                    }
                }
                list = res;

            }
            if (AppState.get().libraryMode == AppState.MODE_PUBLICATION_DATE) {
                Collections.reverse(list);
            }

            list.add(0, empty);
            authorsAdapter.getItemsList().addAll(list);
            authorsAdapter.notifyDataSetChanged();

            recyclerView.scrollToPosition(rememberPos);
        }

        showBookCount();
    }

    private void sortByPopup(final View view) {
        MyPopupMenu popup = new MyPopupMenu(getActivity(), view);
        for (final SORT_BY sortBy : SORT_BY.values()) {
            popup.getMenu().add(sortBy.getResName()).setOnMenuItemClickListener(item -> {
                AppState.get().sortBy = sortBy.getIndex();
                searchAndOrderAsync();
                return false;
            });
        }
        popup.show();
    }

    private void popupMenu() {
        MyPopupMenu p = new MyPopupMenu(getActivity(), binding.onGridList);
        PopupHelper.addPROIcon(p, getActivity());
        List<Integer> names = Arrays.asList(R.string.list, //
                R.string.compact, //
                R.string.grid, //
                R.string.cover, //
                R.string.author, //
                R.string.genre, //
                R.string.serie, //
                R.string.keywords, //
                R.string.language, //
                R.string.my_tags,
                R.string.publisher,
                R.string.publication_date//
        );

        final List<Integer> icons = Arrays.asList(R.drawable.glyphicons_114_justify, //
                R.drawable.glyphicons_114_justify_compact, //
                R.drawable.glyphicons_156_show_big_thumbnails, //
                R.drawable.glyphicons_157_show_thumbnails, //
                R.drawable.glyphicons_4_user, //
                R.drawable.glyphicons_66_tag, //
                R.drawable.glyphicons_710_list_numbered, //
                R.drawable.glyphicons_67_keywords, //
                R.drawable.glyphicons_basic_417_globe, //
                R.drawable.glyphicons_67_tags,
                R.drawable.glyphicons_4_thumbs_up,
                R.drawable.glyphicons_2_book_open
        );
        final List<Integer> actions = Arrays.asList(AppState.MODE_LIST, AppState.MODE_LIST_COMPACT, //
                AppState.MODE_GRID, //
                AppState.MODE_COVERS, //
                AppState.MODE_AUTHORS, //
                AppState.MODE_GENRE, //
                AppState.MODE_SERIES, //
                AppState.MODE_KEYWORDS, //
                AppState.MODE_LANGUAGES, //
                AppState.MODE_USER_TAGS,
                AppState.MODE_PUBLISHER,
                AppState.MODE_PUBLICATION_DATE); //

        for (int i = 0; i < names.size(); i++) {
            final int index = i;
            p.getMenu().add(names.get(i)).setIcon(icons.get(i)).setOnMenuItemClickListener(item -> {
                AppState.get().libraryMode = actions.get(index);
                binding.onGridList.setImageResource(icons.get(index));

                if (Arrays.asList(AppState.MODE_PUBLICATION_DATE, AppState.MODE_PUBLISHER, AppState.MODE_AUTHORS, AppState.MODE_SERIES, AppState.MODE_GENRE, AppState.MODE_USER_TAGS, AppState.MODE_KEYWORDS, AppState.MODE_LANGUAGES).contains(AppState.get().libraryMode)) {
                    binding.filterLine.setText("");
                }

                onGridList();
                searchAndOrderAsync();
                return false;
            });
        }

        p.show();
    }

    public void showBookCount() {
        binding.countBooks.setText("" + (recyclerView.getAdapter().getItemCount() - countTitles));
    }

    @Override
    public boolean isBackPressed() {
        if (recyclerView == null) {
            return false;
        }
        if (recyclerView.getAdapter() instanceof FileMetaAdapter) {
            String searchText = binding.filterLine.getText().toString();
            if (TxtUtils.isEmpty(searchText)) {
                return false;
            }

            if (searchText.startsWith("@")) {
                try {
                    prevText.pop();

                    String pop = prevText.pop();
                    LOG.d("pop", pop);
                    if (TxtUtils.isNotEmpty(pop)) {
                        binding.filterLine.setText(pop);
                        searchAndOrderAsync();
                        return true;
                    }
                } catch (EmptyStackException e) {
                    LOG.e(e);
                }
            }

            binding.filterLine.setText("");
            if (prevLibModeAuthors == NONE) {
                prevLibModeAuthors = prevLibModeFileMeta;
            }
            AppState.get().libraryMode = prevLibModeAuthors;

        } else {
            AppState.get().libraryMode = prevLibModeFileMeta;
        }
        onGridList();
        searchAndOrderAsync();
        return true;
    }

    @Override
    public void notifyFragment() {
        if (searchAdapter != null) {
            searchAdapter.notifyDataSetChanged();
            binding.sortOrder.setVisibility(TxtUtils.visibleIf(AppState.get().isVisibleSorting));
        }
        if (!BooksService.isRunning) {
            binding.onRefresh.setActivated(!BooksService.isRunning);
            binding.filterLine.setHint(R.string.search);
        }
    }

    @Override
    public void resetFragment() {
        onGridList();
        searchAndOrderAsync();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        LOG.d("SearchFragment2 onDestroy");
    }
}
