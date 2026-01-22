package com.foobnix.ui2.fragment;

import static com.foobnix.pdf.info.AppsConfig.SEARCH_FRAGMENT_WORKER_NAME;
import static com.foobnix.pdf.info.AppsConfig.WORKER_POLICY;

import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnDismissListener;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.MenuItem.OnMenuItemClickListener;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.graphics.ColorUtils;
import androidx.core.util.Pair;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.RecyclerView.OnScrollListener;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import androidx.work.ExistingWorkPolicy;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

import com.foobnix.LibreraApp;
import com.foobnix.android.utils.Apps;
import com.foobnix.android.utils.BaseItemLayoutAdapter;
import com.foobnix.android.utils.Dips;
import com.foobnix.android.utils.Keyboards;
import com.foobnix.android.utils.LOG;
import com.foobnix.android.utils.ResultResponse;
import com.foobnix.android.utils.StringDB;
import com.foobnix.android.utils.TxtUtils;
import com.foobnix.dao2.FileMeta;
import com.foobnix.model.AppData;
import com.foobnix.model.AppProfile;
import com.foobnix.model.AppState;
import com.foobnix.pdf.info.AppsConfig;
import com.foobnix.pdf.info.ExtUtils;
import com.foobnix.pdf.info.IMG;
import com.foobnix.pdf.info.Prefs;
import com.foobnix.pdf.info.R;
import com.foobnix.pdf.info.TintUtil;
import com.foobnix.pdf.info.view.AlertDialogs;
import com.foobnix.pdf.info.view.EditTextHelper;
import com.foobnix.pdf.info.view.KeyCodeDialog;
import com.foobnix.pdf.info.view.MyPopupMenu;
import com.foobnix.pdf.info.widget.DialogTranslateFromTo;
import com.foobnix.pdf.info.widget.PrefDialogs;
import com.foobnix.pdf.info.wrapper.PopupHelper;
import com.foobnix.pdf.search.activity.msg.NotifyAllFragments;
import com.foobnix.pdf.search.activity.msg.OpenTagMessage;
import com.foobnix.sys.TempHolder;
import com.foobnix.ui2.AdsFragmentActivity;
import com.foobnix.ui2.AppDB;
import com.foobnix.ui2.AppDB.SEARCH_IN;
import com.foobnix.ui2.AppDB.SORT_BY;
import com.foobnix.ui2.BooksService;
import com.foobnix.ui2.adapter.AuthorsAdapter2;
import com.foobnix.ui2.adapter.FileMetaAdapter;
import com.foobnix.work.CheckDeletedBooksWorker;
import com.foobnix.work.SearchAllBooksWorker;
import com.foobnix.work.SelfTestWorker;

import org.greenrobot.eventbus.EventBus;
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
    public static final Pair<Integer, Integer> PAIR =
            new Pair<Integer, Integer>(R.string.library, R.drawable.glyphicons_589_book_open);
    private static final String CMD_KEYCODE = "@@keycode_config";
    private static final String CMD_EDIT_AUTO_COMPLETE = "@@edit_autocomple";
    private static final String CMD_MARGIN = "@@keycode_margin";

    public static int NONE = -1;
    public static List<FileMeta> cacheItems;
    final Set<String> autocomplitions = new HashSet<String>();
    public int prevLibModeAuthors = NONE;
    public int rememberPos = 0;
    FileMetaAdapter searchAdapter;
    AuthorsAdapter2 authorsAdapter;
    TextView countBooks, sortBy, layoutErrorOnRestart;
    Handler handler;
    ImageView sortOrder, myAutoCompleteImage, cleanFilter, menu2;
    View onRefresh, secondTopPanel, layoutError;
    AutoCompleteTextView searchEditText;
    int countTitles = 0;
    Runnable hideKeyboard = new Runnable() {

        @Override public void run() {
            Keyboards.close(searchEditText);
            Keyboards.hideNavigation(getActivity());
        }
    };
    Runnable saveAutoComplete = new Runnable() {

        @Override public void run() {
            String txt = searchEditText.getText()
                                       .toString()
                                       .trim();
            if (TxtUtils.isNotEmpty(txt) && !txt.startsWith("@@") && !StringDB.contains(AppState.get().myAutoCompleteDb,
                    txt)) {
                if (!searchAdapter.getItemsList()
                                  .isEmpty()) {
                    StringDB.add(AppState.get().myAutoCompleteDb, txt, (db) -> AppState.get().myAutoCompleteDb = db);
                    autocomplitions.add(txt);
                    updateFilterListAdapter();
                    myAutoCompleteImage.setVisibility(View.VISIBLE);
                }

            }

        }
    };
    BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {

        @Override public void onReceive(Context context, Intent intent) {

            if (BooksService.RESULT_SEARCH_FINISH.equals(intent.getStringExtra(Intent.EXTRA_TEXT))) {
                searchAndOrderAsync();
                searchEditText.setHint(R.string.search);
                onRefresh.setActivated(true);

                if (AppsConfig.IS_LOG) {
                    searchEditText.setHint(Apps.getApplicationName(getContext()));
                }

            } else if (BooksService.RESULT_SEARCH_COUNT.equals(intent.getStringExtra(Intent.EXTRA_TEXT))) {
                int count = intent.getIntExtra("android.intent.extra.INDEX", 0);
                if (count > 0) {
                    countBooks.setText("" + count);
                }
                searchEditText.setHint(R.string.searching_please_wait_);
                onRefresh.setActivated(false);
            } else if (BooksService.RESULT_BUILD_LIBRARY.equals(intent.getStringExtra(Intent.EXTRA_TEXT))) {
                onRefresh.setActivated(false);
                searchEditText.setHint(R.string.extracting_information_from_books);
            } else if (BooksService.RESULT_SEARCH_MESSAGE_TXT.equals(intent.getStringExtra(Intent.EXTRA_TEXT))) {
                searchEditText.setHint(intent.getStringExtra("TEXT"));
            } else if (BooksService.RESULT_NOTIFY_ALL.equals(intent.getStringExtra(Intent.EXTRA_TEXT))) {
                TempHolder.listHash++;
                EventBus.getDefault()
                        .post(new NotifyAllFragments());
            }
        }

    };
    Runnable sortAndSeach = new Runnable() {

        @Override public void run() {
            recyclerView.scrollToPosition(0);
            searchAndOrderAsync();
        }
    };
    private final TextWatcher filterTextWatcher = new TextWatcher() {

        @Override public void afterTextChanged(final Editable s) {
            AppState.get().searchQuery = s.toString();
        }

        @Override
        public void beforeTextChanged(final CharSequence s, final int start, final int count, final int after) {
        }

        @Override public void onTextChanged(final CharSequence s, final int start, final int before, final int count) {
            if (//
                    AppState.get().libraryMode == AppState.MODE_GRID || //
                            AppState.get().libraryMode == AppState.MODE_LIST || //
                            AppState.get().libraryMode == AppState.MODE_LIST_COMPACT || //
                            AppState.get().libraryMode == AppState.MODE_COVERS//
            ) {
                handler.removeCallbacks(sortAndSeach);
                handler.removeCallbacks(hideKeyboard);
                if (s.toString()
                     .trim()
                     .length() == 0) {
                    handler.postDelayed(sortAndSeach, 250);
                    handler.postDelayed(hideKeyboard, 2000);
                } else {
                    handler.postDelayed(sortAndSeach, 1000);
                }
            }
            myAutoCompleteImage.setVisibility(View.GONE);
            handler.removeCallbacks(saveAutoComplete);

            if (StringDB.contains(AppState.get().myAutoCompleteDb, s.toString()
                                                                    .trim())) {
                myAutoCompleteImage.setVisibility(View.VISIBLE);
            } else {
                handler.postDelayed(saveAutoComplete, 10000);
            }

        }

    };
    boolean isOnTop = false;
    private String NO_SERIES = ":no-series";
    private Stack<String> prevText = new Stack<String>();
    private ImageView onGridlList;
    ResultResponse<String> onAuthorSeriesClick = new ResultResponse<String>() {

        @Override public boolean onResultRecive(String result) {
            rememberPos = ((LinearLayoutManager) recyclerView.getLayoutManager()).findLastVisibleItemPosition();
            onMetaInfoClick(SEARCH_IN.getByMode(AppState.get().libraryMode), result);
            return false;
        }
    };
    ResultResponse<String> onAuthorClick = new ResultResponse<String>() {

        @Override public boolean onResultRecive(String result) {
            onMetaInfoClick(SEARCH_IN.AUTHOR, result);
            return false;
        }
    };
    ResultResponse<String> onSeriesClick = new ResultResponse<String>() {

        @Override public boolean onResultRecive(String result) {
            if (result.contains(NO_SERIES)) {
                onMetaInfoClick(SEARCH_IN.getByPrefix(searchEditText.getText()
                                                                    .toString()), result);
            } else {
                onMetaInfoClick(SEARCH_IN.SERIES, result);
            }
            return false;
        }
    };

    @Override public Pair<Integer, Integer> getNameAndIconRes() {
        return PAIR;
    }

    @Override public void onTintChanged() {
        TintUtil.setBackgroundFillColor(secondTopPanel, TintUtil.color);

        int color =
                (AppState.get().appTheme == AppState.THEME_DARK_OLED || AppState.get().appTheme == AppState.THEME_DARK) ?
                        Color.WHITE : TintUtil.color;
        TintUtil.setTintImageNoAlpha(menu2, color);

        int colorTheme = TintUtil.getColorInDayNighth();
        colorTheme = ColorUtils.setAlphaComponent(colorTheme, 230);

        TintUtil.setUITextColor(countBooks, colorTheme);

        TintUtil.setTintImageNoAlpha(cleanFilter, colorTheme);
        TintUtil.setTintImageNoAlpha(myAutoCompleteImage, colorTheme);

        if (AppState.get().appTheme == AppState.THEME_DARK_OLED || (AppState.get().appTheme == AppState.THEME_DARK && TintUtil.color == Color.BLACK)) {
            searchEditText.setBackgroundResource(R.drawable.bg_search_edit_night);
            menu2.setBackgroundResource(R.drawable.bg_search_edit_night);

        } else {
            searchEditText.setBackgroundResource(R.drawable.bg_search_edit);
            menu2.setBackgroundResource(R.drawable.bg_search_edit);
        }
        TintUtil.setStrokeColor(searchEditText, TintUtil.color);
        TintUtil.setStrokeColor(menu2, TintUtil.color);

        TintUtil.setUITextColor(searchEditText, colorTheme);

        if (AppState.get().appTheme == AppState.THEME_INK) {
            searchEditText.setBackgroundResource(R.drawable.bg_search_edit);
            TintUtil.setStrokeColor(searchEditText, Color.BLACK);
            TintUtil.setStrokeColor(menu2, Color.BLACK);
            TintUtil.setUITextColor(searchEditText, Color.BLACK);
            countBooks.setTextColor(Color.BLACK);

        }

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
        autocomplitions.add(CMD_MARGIN);
        autocomplitions.add(CMD_EDIT_AUTO_COMPLETE);

        autocomplitions.addAll(StringDB.asList(AppState.get().myAutoCompleteDb));

        updateFilterListAdapter();
    }

    public void updateFilterListAdapter() {
        try {
            ArrayList<String> list = new ArrayList<String>(autocomplitions);
            Collections.sort(list);
            ArrayAdapter<String> adapter =
                    new ArrayAdapter<String>(getActivity(), android.R.layout.simple_spinner_dropdown_item, list);
            searchEditText.setAdapter(adapter);
            searchEditText.setThreshold(1);
        } catch (Exception e) {
            LOG.e(e);
        }

    }

    @Override public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_search2, container, false);

        LOG.d("Context-SF-getContext", getContext());
        LOG.d("Context-SF-getApplicationContext", getActivity().getApplicationContext());
        LOG.d("Context-SF-getBaseContext", getActivity().getBaseContext());
        LOG.d("Context-SF-LibreraApp.context", LibreraApp.context);

        LOG.d("SearchFragment2 onCreateView");

        NO_SERIES = " (" + getString(R.string.without_series) + ")";

        handler = new Handler(Looper.getMainLooper());

        secondTopPanel = view.findViewById(R.id.secondTopPanel);
        countBooks = (TextView) view.findViewById(R.id.countBooks);
        onRefresh = view.findViewById(R.id.onRefresh);
        onRefresh.setActivated(true);
        cleanFilter = (ImageView) view.findViewById(R.id.cleanFilter);
        sortBy = (TextView) view.findViewById(R.id.sortBy);
        sortOrder = (ImageView) view.findViewById(R.id.sortOrder);
        menu2 = (ImageView) view.findViewById(R.id.menu2);
        myAutoCompleteImage = (ImageView) view.findViewById(R.id.myAutoCompleteImage);
        searchEditText = (AutoCompleteTextView) view.findViewById(R.id.filterLine);
        recyclerView = (RecyclerView) view.findViewById(R.id.recyclerView);

        layoutError = view.findViewById(R.id.layoutError);
        layoutErrorOnRestart = view.findViewById(R.id.layoutErrorOnRestart);
        TxtUtils.underlineTextView(layoutErrorOnRestart);
        layoutError.setVisibility(View.GONE);

        layoutError.setOnClickListener(new OnClickListener() {
            @Override public void onClick(View v) {
                onRefresh.performClick();
            }
        });

        onRefresh.setOnLongClickListener(new View.OnLongClickListener() {
            @Override public boolean onLongClick(View v) {
                if (BooksService.isRunning) {
                    Toast.makeText(getActivity(), R.string.please_wait_books_are_being_processed_, Toast.LENGTH_SHORT)
                         .show();
                    return true;
                }
                if (AppState.get().isShowTestBooks) {
                    AlertDialogs.showDialog(getActivity(), "Run the self-test? " + AppData.getTestFileName()
                                                                                          .getName(),
                            getString(R.string.ok), new Runnable() {

                                @Override public void run() {
                                    // BooksService.startForeground(getActivity(), BooksService.ACTION_RUN_SELF_TEST);
                                    getActivity().getWindow()
                                                 .addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                                    OneTimeWorkRequest workRequest =
                                            new OneTimeWorkRequest.Builder(SelfTestWorker.class).build();

                                    //WorkManager.getInstance(getContext()).enqueue(workRequest);
                                    WorkManager.getInstance(getContext())

                                               .enqueueUniqueWork(SEARCH_FRAGMENT_WORKER_NAME, WORKER_POLICY,
                                                       workRequest);

                                }
                            }, null);
                }
                return true;
            }
        });

        if (AppState.get().appTheme == AppState.THEME_DARK_OLED || (AppState.get().appTheme == AppState.THEME_DARK && TintUtil.color == Color.BLACK)) {
            searchEditText.setBackgroundResource(R.drawable.bg_search_edit_night);
        }

        myAutoCompleteImage.setVisibility(View.GONE);

        searchEditText.addTextChangedListener(filterTextWatcher);
        searchEditText.setImeOptions(EditorInfo.IME_ACTION_SEARCH);
        EditTextHelper.enableKeyboardSearch(searchEditText, new Runnable() {

            @Override public void run() {
                Keyboards.close(searchEditText);
                Keyboards.hideNavigation(getActivity());
            }
        });

        searchAdapter = new FileMetaAdapter();

        authorsAdapter = new AuthorsAdapter2();

        onGridlList = (ImageView) view.findViewById(R.id.onGridList);
        onGridlList.setOnClickListener(new OnClickListener() {

            @Override public void onClick(View v) {
                popupMenu(onGridlList);
            }
        });

        onRefresh.setOnClickListener(new OnClickListener() {

            @Override public void onClick(View v) {

                if (!onRefresh.isActivated()) {
                    Toast.makeText(getActivity(), R.string.extracting_information_from_books, Toast.LENGTH_LONG)
                         .show();
                    return;
                }
                PrefDialogs.chooseFolderDialog(getActivity(), new Runnable() {

                    @Override public void run() {
                        //BookCSS.get().searchPaths = BookCSS.get().searchPaths.replace("//", "/");
                    }
                }, new Runnable() {

                    @Override public void run() {
                        Prefs.get()
                             .remove(AppProfile.getCurrent(), 0);
                        layoutError.setVisibility(View.GONE);
                        recyclerView.scrollToPosition(0);
                        seachAll();
                        ((AdsFragmentActivity) SearchFragment2.this.getActivity()).showInterstitialNoFinish();
                    }
                });
            }
        });

        cleanFilter.setOnClickListener(new OnClickListener() {

            @Override public void onClick(View v) {
                searchEditText.setText("");
                recyclerView.scrollToPosition(0);
                searchAndOrderAsync();
            }
        });

        sortBy.setOnClickListener(new OnClickListener() {

            @Override public void onClick(View v) {
                sortByPopup(v);
            }
        });

        sortOrder.setOnClickListener(new OnClickListener() {

            @Override public void onClick(View v) {
                AppState.get().isSortAsc = !AppState.get().isSortAsc;
                searchAndOrderAsync();
            }
        });
        sortOrder.setOnLongClickListener(new OnLongClickListener() {

            @Override public boolean onLongClick(View v) {
                AppState.get().isVisibleSorting = !AppState.get().isVisibleSorting;
                sortOrder.setVisibility(TxtUtils.visibleIf(AppState.get().isVisibleSorting));
                return true;
            }
        });

        sortBy.setOnLongClickListener(new OnLongClickListener() {

            @Override public boolean onLongClick(View v) {
                AppState.get().isVisibleSorting = !AppState.get().isVisibleSorting;
                sortOrder.setVisibility(TxtUtils.visibleIf(AppState.get().isVisibleSorting));
                return true;
            }
        });
        sortOrder.setVisibility(TxtUtils.visibleIf(AppState.get().isVisibleSorting));

        bindAdapter(searchAdapter);

        searchAdapter.setOnAuthorClickListener(onAuthorClick);
        searchAdapter.setOnSeriesClickListener(onSeriesClick);

        authorsAdapter.setOnItemClickListener(onAuthorSeriesClick);

        if (AppState.get().isRestoreSearchQuery && !TxtUtils.isEmpty(AppState.get().searchQuery)) {
            searchEditText.setText(AppState.get().searchQuery);
        }

        onGridList();

        boolean errorExist = Prefs.get()
                                  .isErrorExist(AppProfile.getCurrent(), 0);

        layoutError.setVisibility(errorExist ? View.VISIBLE : View.GONE);
        if (BooksService.isRunning) {
            layoutError.setVisibility(View.GONE);
        }

        if (errorExist) {
            searchAndOrderAsync();
        } else {
            long count = AppProfile.bookCount;
            LOG.d("worker-starts", "cound-db", count);
            if (count == 0) {
                seachAll();
            } else {
                checkForDeleteBooks();
                searchAndOrderAsync();
            }
        }

        initAutocomplition();
        onTintChanged();

        recyclerView.addOnScrollListener(new OnScrollListener() {
            @Override public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
            }
        });

        myAutoCompleteImage.setOnClickListener(new OnClickListener() {

            @Override public void onClick(View v) {
                showAutoCompleteDialog();
            }
        });

        menu2.setOnClickListener(new OnClickListener() {
            @Override public void onClick(View view) {
                if (view.getRootView()
                        .findViewById(R.id.imageMenu1) != null) {
                    view.getRootView()
                        .findViewById(R.id.imageMenu1)
                        .performClick();
                }
            }
        });

        LocalBroadcastManager.getInstance(getActivity())
                             .registerReceiver(broadcastReceiver, new IntentFilter(BooksService.INTENT_NAME));

        return view;

    }

    public void showAutoCompleteDialog() {

        final AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle(R.string.search_history_and_autocomplete);

        final ListView list = new ListView(getActivity());

        final List<String> items = new ArrayList<String>(StringDB.asList(AppState.get().myAutoCompleteDb));
        Collections.sort(items);
        BaseItemLayoutAdapter<String> adapter =
                new BaseItemLayoutAdapter<String>(getActivity(), R.layout.path_item, items) {
                    @Override public void populateView(View layout, int position, final String item) {
                        TextView text = layout.findViewById(R.id.browserPath);
                        ImageView delete = layout.findViewById(R.id.delete);

                        text.setText(item);

                        delete.setOnClickListener(new OnClickListener() {

                            @Override public void onClick(View v) {
                                autocomplitions.remove(item);
                                items.remove(item);

                                StringDB.delete(AppState.get().myAutoCompleteDb, item,
                                        (db) -> AppState.get().myAutoCompleteDb = db);
                                notifyDataSetChanged();
                            }
                        });
                        layout.setOnClickListener(new OnClickListener() {

                            @Override public void onClick(View v) {
                                searchEditText.setText(item);
                                searchAndOrderAsync();
                            }
                        });

                    }
                };

        list.setAdapter(adapter);

        builder.setView(list);

        builder.setPositiveButton(R.string.close, new AlertDialog.OnClickListener() {

            @Override public void onClick(DialogInterface dialog, int which) {
            }
        });

        AlertDialog create = builder.create();
        create.setOnDismissListener(new OnDismissListener() {

            @Override public void onDismiss(DialogInterface dialog) {
                Keyboards.hideNavigation(getActivity());
            }
        });
        create.show();

        Keyboards.close(getActivity());

    }

    @Override public void onResume() {
        super.onResume();
        menu2.setVisibility(getActivity().findViewById(R.id.imageMenu1) == null ? View.GONE : View.VISIBLE);
    }

    @Override public void onStop() {
        super.onStop();
    }

    private void onMetaInfoClick(SEARCH_IN mode, String result) {
        if (mode == SEARCH_IN.SERIES && !result.startsWith(EMPTY_ID)) {
            result = StringDB.EXACTMATCHCHAR + result + StringDB.EXACTMATCHCHAR;
        }

        searchEditText.setText(mode.getDotPrefix() + " " + result);
        AppState.get().libraryMode = AppState.get().previousLibraryMode;
        onGridList();
        searchAndOrderAsync();
    }

    private void seachAll() {
        try {
            LOG.d("SeachFragment2", "seachAll");
            searchAdapter.clearItems();
            searchAdapter.notifyDataSetChanged();
            IMG.clearMemoryCache();
            IMG.clearDiscCache();
            //BooksService.startForeground(getActivity(), BooksService.ACTION_SEARCH_ALL);

            SearchAllBooksWorker.run(getActivity());

        } catch (Exception e) {
            LOG.e(e);
        }
    }

    public void checkForDeleteBooks() {
        try {
            //BooksService.startForeground(getActivity(), BooksService.ACTION_REMOVE_DELETED);
            OneTimeWorkRequest workRequest = new OneTimeWorkRequest.Builder(CheckDeletedBooksWorker.class).build();
            WorkManager.getInstance(getContext())
                       .enqueueUniqueWork(SEARCH_FRAGMENT_WORKER_NAME, WORKER_POLICY, workRequest);
            //LOG.d("MessageWorker-Status checkForDeleteBooks");
        } catch (Exception e) {
            LOG.e(e);
        }
    }

    @Override public void onTextRecive(String txt) {
        searchAndOrderExteral(txt);
    }

    public void searchAndOrderExteral(String text) {
        if (searchEditText != null) {
            searchEditText.setText(text);
            searchAndOrderAsync();
        }

    }

    public void searchAndOrderAsync() {
        LOG.d("SeachFragment2", "searchAndOrderAsync");

        if (Apps.isDestroyedActivity(getActivity())) {
            return;
        }
        searchEditText.setHint(R.string.msg_loading);
        sortBy.setText(AppDB.SORT_BY.getByID(AppState.get().sortBy)
                                    .getResName());

        sortOrder.setImageResource(AppState.get().isSortAsc ? R.drawable.glyphicons_221_chevron_down :
                R.drawable.glyphicons_222_chevron_up);

        String order = getString(AppState.get().isSortAsc ? R.string.ascending : R.string.descending);
        sortBy.setContentDescription(getString(R.string.cd_sort_results) + " " + sortBy.getText());
        sortOrder.setContentDescription(order);

        populate();

    }

    @Subscribe public void onShowTag(OpenTagMessage msg) {
        if (searchEditText != null) {
            searchEditText.setText("@tags " + msg.getTagName());
        }
        searchAndOrderAsync();

    }

    @Override public List<FileMeta> prepareDataInBackground() {
        LOG.d("SeachFragment2", "prepareDataInBackground");
        String txt = searchEditText.getText()
                                   .toString()
                                   .trim();
        countTitles = 0;
        if (Arrays.asList(AppState.MODE_GRID, AppState.MODE_COVERS, AppState.MODE_LIST, AppState.MODE_LIST_COMPACT)
                  .contains(AppState.get().libraryMode)) {

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

            List<FileMeta> searchBy = AppDB.get()
                                           .searchBy(txt, SORT_BY.getByID(AppState.get().sortBy),
                                                   AppState.get().isSortAsc);

            ExtUtils.removeReadBooks(searchBy);
            ExtUtils.removeNotFound(searchBy);

            List<String> result = new ArrayList<String>();
            boolean byGenre = txt.startsWith(SEARCH_IN.GENRE.getDotPrefix());
            boolean byAuthor = txt.startsWith(SEARCH_IN.AUTHOR.getDotPrefix());
            if (!txt.contains("::") && (byGenre || byAuthor)) {
                if (isSearchOnlyEmpy) {
                    Iterator<FileMeta> iterator = searchBy.iterator();
                    while (iterator.hasNext()) {
                        if (TxtUtils.isNotEmpty(iterator.next()
                                                        .getSequence())) {
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
                            AppState.get().sortBy == SORT_BY.PUBLICATION_YEAR.getIndex() || AppState.get().sortBy == SORT_BY.SERIES.getIndex() || AppState.get().sortBy == SORT_BY.PUBLISHER.getIndex()) {//

                List<FileMeta> res = new ArrayList<FileMeta>();
                String last = null;

                String extDir = Environment.getExternalStorageDirectory()
                                           .getPath();

                int count = 0;
                FileMeta fm = null;

                ///

                for (FileMeta it : searchBy) {
                    String parentName = "";
                    if (AppState.get().sortBy == SORT_BY.PUBLISHER.getIndex()) {
                        parentName = "" + it.getPublisher();
                    } else if (AppState.get().sortBy == SORT_BY.SERIES.getIndex()) {
                        parentName = "" + TxtUtils.nullToEmpty(it.getSequence());
                        parentName = parentName.replace(",", "");
                        if (parentName.isEmpty()) {
                            parentName = "---";
                        }
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
                    count++;
                    if (parentName != null && !parentName.equals(last)) {
                        if (fm != null) {
                            fm.setTitle(fm.getTitle() + " (" + count + ")");
                        }
                        fm = new FileMeta();
                        fm.setCusType(FileMetaAdapter.DISPALY_TYPE_LAYOUT_TITLE_DIVIDER);
                        fm.setTitle(parentName);
                        count = 0;
                        last = parentName;
                        res.add(fm);
                        countTitles++;
                    }
                    res.add(it);
                }
                if (fm != null) {
                    fm.setTitle(fm.getTitle() + " (" + (count + 1) + ")");
                }
                searchBy = res;
            }

            return searchBy;
        } else {
            return null;
        }
    }

    public void toastState(String command, boolean state) {
        Toast.makeText(getContext(), command + " [" + (state ? "ON" : "OFF") + "]", Toast.LENGTH_LONG)
             .show();
    }

    @Override public void populateDataInUI(List<FileMeta> items) {
        cacheItems = items;
        handler.removeCallbacks(sortAndSeach);

        String txt = searchEditText.getText()
                                   .toString()
                                   .trim();
        searchEditText.setHint(R.string.search);

        //if(AppsConfig.IS_LOG){
        //searchEditText.setHint(Apps.getApplicationName(getContext()));
        //}

        if (CMD_KEYCODE.equals(txt)) {
            new KeyCodeDialog(getActivity(), null);
            searchEditText.setText("");
        }
        if (CMD_MARGIN.equals(txt)) {
            SwipeRefreshLayout layout = getActivity().findViewById(R.id.swipeRefreshLayout);
            final FrameLayout.LayoutParams layoutParams = (FrameLayout.LayoutParams) layout.getLayoutParams();
            layoutParams.rightMargin = Dips.screenWidth() / 4;
            layout.setLayoutParams(layoutParams);
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

        if (Arrays.asList(AppState.MODE_GRID, AppState.MODE_COVERS, AppState.MODE_LIST, AppState.MODE_LIST_COMPACT)
                  .contains(AppState.get().libraryMode)) {
            AppState.get().previousLibraryMode = AppState.get().libraryMode;
            searchEditText.setEnabled(true);
            sortBy.setEnabled(true);
            sortOrder.setEnabled(true);
            if (AppState.get().isVisibleSorting) {
                sortOrder.setVisibility(View.VISIBLE);
            }

            searchAdapter.clearItems();
            searchAdapter.getItemsList()
                         .addAll(items);
            searchAdapter.notifyDataSetChanged();
            handler.postDelayed(new Runnable() {

                @Override public void run() {
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
            } else if (AppState.get().libraryMode == AppState.MODE_LANGUAGES) {
                searchEditText.setHint(R.string.language);
                empty = EMPTY_ID + getActivity().getString(R.string.no_language);
            } else if (AppState.get().libraryMode == AppState.MODE_PUBLICATION_DATE) {
                searchEditText.setHint(R.string.publication_date);
                empty = EMPTY_ID + getActivity().getString(R.string.empy);
            } else if (AppState.get().libraryMode == AppState.MODE_PUBLISHER) {
                searchEditText.setHint(R.string.publisher);
                empty = EMPTY_ID + getActivity().getString(R.string.empy);
            }

            authorsAdapter.clearItems();
            List<String> list = AppDB.get()
                                     .getAll(SEARCH_IN.getByMode(AppState.get().libraryMode));
            if (AppState.get().libraryMode == AppState.MODE_LANGUAGES) {
                List<String> res = new ArrayList<String>();
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
            authorsAdapter.getItemsList()
                          .addAll(list);
            authorsAdapter.notifyDataSetChanged();

            recyclerView.scrollToPosition(rememberPos);

        }

        showBookCount();

    }

    private void sortByPopup(final View view) {

        MyPopupMenu popup = new MyPopupMenu(getActivity(), view);
        for (final SORT_BY sortBy : SORT_BY.values()) {
            popup.getMenu()
                 .add(sortBy.getResName())
                 .setOnMenuItemClickListener(new OnMenuItemClickListener() {

                     @Override public boolean onMenuItemClick(MenuItem item) {
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
                R.string.language, //
                R.string.my_tags, R.string.publisher, R.string.publication_date//
                                           );

        final List<Integer> icons = Arrays.asList(R.drawable.my_glyphicons_114_paragraph_justify, //
                R.drawable.my_glyphicons_114_justify_compact, //
                R.drawable.glyphicons_157_thumbnails, //
                R.drawable.glyphicons_158_thumbnails_small, //
                R.drawable.glyphicons_4_user, //
                R.drawable.glyphicons_66_tag, //
                R.drawable.glyphicons_115_list, //
                R.drawable.glyphicons_67_tags, //
                R.drawable.glyphicons_417_globe, //
                R.drawable.glyphicons_67_tags, R.drawable.glyphicons_451_hand_like,
                R.drawable.glyphicons_589_book_open);
        final List<Integer> actions = Arrays.asList(AppState.MODE_LIST, AppState.MODE_LIST_COMPACT, //
                AppState.MODE_GRID, //
                AppState.MODE_COVERS, //
                AppState.MODE_AUTHORS, //
                AppState.MODE_GENRE, //
                AppState.MODE_SERIES, //
                AppState.MODE_KEYWORDS, //
                AppState.MODE_LANGUAGES, //
                AppState.MODE_USER_TAGS, AppState.MODE_PUBLISHER, AppState.MODE_PUBLICATION_DATE); //

        for (int i = 0; i < names.size(); i++) {
            final int index = i;
            p.getMenu()
             .add(names.get(i))
             .setIcon(icons.get(i))
             .setOnMenuItemClickListener(new OnMenuItemClickListener() {

                 @Override public boolean onMenuItemClick(MenuItem item) {
                     AppState.get().libraryMode = actions.get(index);
                     onGridList.setImageResource(icons.get(index));
                     onGridList.setContentDescription(
                             getString(R.string.cd_view_menu) + " " + getString(names.get(index)));

                     if (Arrays.asList(AppState.MODE_PUBLICATION_DATE, AppState.MODE_PUBLISHER, AppState.MODE_AUTHORS,
                                       AppState.MODE_SERIES, AppState.MODE_GENRE, AppState.MODE_USER_TAGS, AppState.MODE_KEYWORDS,
                                       AppState.MODE_LANGUAGES)
                               .contains(AppState.get().libraryMode)) {
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

    @Override public void onDoubleClick() {
        if (isOnTop) {
            recyclerView.scrollToPosition(rememberPos);
        } else {
            rememberPos =
                    ((LinearLayoutManager) recyclerView.getLayoutManager()).findLastCompletelyVisibleItemPosition();
            recyclerView.scrollToPosition(0);
        }
        isOnTop = !isOnTop;
    }

    public void showBookCount() {
        countBooks.setText("" + (recyclerView.getAdapter()
                                             .getItemCount() - countTitles));
    }

    @Override public boolean isBackPressed() {
        if (recyclerView == null) {
            return false;
        }
        if (recyclerView.getAdapter() instanceof FileMetaAdapter) {
            String searchText = searchEditText.getText()
                                              .toString();
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
                prevLibModeAuthors = AppState.get().previousLibraryMode;
            }
            AppState.get().libraryMode = prevLibModeAuthors;
            onGridList();
            searchAndOrderAsync();
            return true;

        } else {
            AppState.get().libraryMode = AppState.get().previousLibraryMode;
            onGridList();
            searchAndOrderAsync();
            return true;
        }
    }

    @Override public void notifyFragment() {
        LOG.d("SeachFragment2", "notifyFragment");
        if (searchAdapter != null) {
            searchAdapter.notifyDataSetChanged();
            sortOrder.setVisibility(TxtUtils.visibleIf(AppState.get().isVisibleSorting));
        }
        if (!BooksService.isRunning) {
            onRefresh.setActivated(!BooksService.isRunning);
            searchEditText.setHint(R.string.search);

//            if(AppsConfig.IS_LOG){
//                searchEditText.setHint(Apps.getApplicationName(getContext()));
//            }

        }
    }

    @Override public void resetFragment() {
        LOG.d("SeachFragment2", "resetFragment");
        if (handler != null) {
            handler.postDelayed(new Runnable() {
                @Override public void run() {
                    onGridList();
                    searchAndOrderAsync();
                }
            }, 150);
        }
    }

    @Override public void onDestroy() {
        super.onDestroy();
        LOG.d("SearchFragment2 onDestroy");
        LocalBroadcastManager.getInstance(getActivity())
                             .unregisterReceiver(broadcastReceiver);
        cacheItems = null;

    }
}
