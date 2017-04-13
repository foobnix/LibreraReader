package com.foobnix.pdf.info.fragment;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.Set;

import com.foobnix.android.utils.Dips;
import com.foobnix.android.utils.LOG;
import com.foobnix.android.utils.ResultResponse;
import com.foobnix.android.utils.TxtUtils;
import com.foobnix.dao2.FileMeta;
import com.foobnix.pdf.CopyAsyncTask;
import com.foobnix.pdf.info.ADS;
import com.foobnix.pdf.info.Analytics;
import com.foobnix.pdf.info.ExtUtils;
import com.foobnix.pdf.info.IMG;
import com.foobnix.pdf.info.R;
import com.foobnix.pdf.info.TintUtil;
import com.foobnix.pdf.info.Urls;
import com.foobnix.pdf.info.io.SearchCore;
import com.foobnix.pdf.info.presentation.AuthorsAdapter;
import com.foobnix.pdf.info.presentation.SearchAdapter;
import com.foobnix.pdf.info.widget.FileInformationDialog;
import com.foobnix.pdf.info.widget.ShareDialog;
import com.foobnix.pdf.info.wrapper.AppState;
import com.foobnix.pdf.info.wrapper.PopupHelper;
import com.foobnix.sys.TempHolder;
import com.foobnix.ui2.AppDB;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.content.ContextCompat;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.MenuItem;
import android.view.MenuItem.OnMenuItemClickListener;
import android.view.View;
import android.view.View.OnKeyListener;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

public class SearchFragment extends BaseListFragment {
    private static final String AUTO_COMLETION = "AutoComletion";
    private static final int MAX_ITEMS = 100;
    private static final String PREFIX_SERIES = "@series";
    private static final String PREFIX_GENRE = "@genre";
    private static final String PREFIX_AUTHOR = "@author";
    private static final String PREFIX_PATH = "@path";
    private SearchAdapter searchAdapter;

    Handler handler;

    List<String> listMetaItems = new ArrayList<String>();
    List<String> authorsMetaItems = new ArrayList<String>();
    List<String> genreMetaItems = new ArrayList<String>();
    List<String> seriesMetaItems = new ArrayList<String>();

    AuthorsAdapter authorsAdapter;
    public int prevLibMode = AppState.MODE_GRID;

    final Set<String> autocompleteSuggestoins = new HashSet<String>();

    private List<FileMeta> allFilesMeta = new ArrayList<FileMeta>();

    @Override
    public void onAttach(final Activity activity) {
        super.onAttach(activity);
        if (searchAdapter == null) {
            searchAdapter = new SearchAdapter(activity, allFilesMeta);
        }
        searchAdapter.setOnMenuPressed(onMenuPressed);
        handler = new Handler();

        authorsAdapter = new AuthorsAdapter(activity, listMetaItems);

    }

    public void loadAuthoSuggestoins() {
        try {
            SharedPreferences sp = getActivity().getSharedPreferences(AUTO_COMLETION, Context.MODE_PRIVATE);
            Set<String> stringSet = sp.getStringSet(AUTO_COMLETION, null);
            if (stringSet != null) {
                autocompleteSuggestoins.clear();
                autocompleteSuggestoins.addAll(stringSet);
            }
            updateFilterListAdapter();
        } catch (Exception e) {
            LOG.e(e);
        }
    }

    public void cleanAuthoSuggestoins() {
        try {
            autocompleteSuggestoins.clear();
            autocompleteSuggestoins.add(PREFIX_SERIES + " ");
            autocompleteSuggestoins.add(PREFIX_GENRE + " ");
            autocompleteSuggestoins.add(PREFIX_AUTHOR + " ");
            autocompleteSuggestoins.add(PREFIX_PATH + " ");

            saveAuthoSuggestoins();
            updateFilterListAdapter();
        } catch (Exception e) {
            LOG.e(e);
        }
    }

    public void saveAuthoSuggestoins() {
        try {
            SharedPreferences sp = getActivity().getSharedPreferences(AUTO_COMLETION, Context.MODE_PRIVATE);
            Editor edit = sp.edit();
            edit.putStringSet(AUTO_COMLETION, autocompleteSuggestoins);
            edit.commit();
        } catch (Exception e) {
            LOG.e(e);
        }
    }

    ResultResponse<File> onMenuPressed = new ResultResponse<File>() {


        @Override
        public boolean onResultRecive(final File file) {
            Runnable onDeleteAction = new Runnable() {

                @Override
                public void run() {
                    allFilesMeta.remove(file);
                    file.delete();
                    sortAndFilter();
                }
            };

            if (ExtUtils.isNotSupportedFile(file)) {
                ShareDialog.showArchive(getActivity(), file, onDeleteAction);
            } else {
                ShareDialog.show(getActivity(), file, onDeleteAction, -1, null);
            }
            return false;
        }
    };

    @Override
    public void onActivityCreated(final Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        // searchAdapter = new SearchBrowserAdapter(getActivity());
        filterLayout.setVisibility(View.VISIBLE);

        sortBy.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(final View v) {
                final CharSequence[] items = { getString(R.string.by_path), getString(R.string.by_file_name), getString(R.string.by_size), getString(R.string.by_date) };

                PopupMenu popup = new PopupMenu(getContext(), sortBy);
                for (int i = 0; i < items.length; i++) {
                    final int j = i;
                    popup.getMenu().add(items[i]).setOnMenuItemClickListener(new OnMenuItemClickListener() {

                        @Override
                        public boolean onMenuItemClick(MenuItem item) {
                            AppState.getInstance().sortBy = j;
                            sortAndFilter();
                            return false;
                        }
                    });
                }
                popup.show();
            }
        });

        sortOrderImage.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(final View v) {
                AppState.getInstance().isSortAsc = !AppState.getInstance().isSortAsc;
                sortAndFilter();
            }
        });

        onRefresh.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(final View v) {
                metaThreadCanWork = false;
                allFilesMeta.clear();
                scan(getActivity());

                // if (!AppsConfig.checkIsProInstalled(getActivity())) {
                // Urls.openPdfPro(getActivity());
                // }

            }
        });

        cleanFilter.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(final View v) {
                filterLine.setText("");
                filterLine.clearFocus();
                // searchAdapter.setUris(recurciveResults);
                sortAndFilter("");
            }
        });

        final long start = System.currentTimeMillis();
        new AsyncTask() {

            @Override
            protected void onPreExecute() {
                showLoadingText(R.string.msg_loading);
            };

            @Override
            protected Object doInBackground(Object... params) {
                allFilesMeta.clear();
                // recurciveResults.addAll(MetaCache.get().loadFiles(getActivity()));
                loadAllThread();
                return null;
            }

            @Override
            protected void onPostExecute(Object result) {
                if (!isAdded()) {
                    return;
                }
                hideLoadingText();

                // super.onPostExecute(result);
                metaThreadCanWork = false;
                onGlidList();
                if (allFilesMeta.isEmpty()) {
                    scan(getActivity());
                } else {
                    sortAndFilter();
                }
                long res = System.currentTimeMillis() - start;
                Toast.makeText(getContext(), "TIME: " + res / 1000, Toast.LENGTH_LONG).show();
                LOG.d("LOADING TIME" + res / 1000);
            }
        }.execute();

        filterLine.addTextChangedListener(filterTextWatcher);
        filterLine.setOnEditorActionListener(onActionListener);
        filterLine.setOnKeyListener(onKeyListener);

        loadAuthoSuggestoins();

        gridList.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(final View v) {
                PopupMenu p = new PopupMenu(getActivity(), gridList);
                p.getMenu().add(R.string.grid).setIcon(R.drawable.glyphicons_156_show_big_thumbnails).setOnMenuItemClickListener(new OnMenuItemClickListener() {

                    @Override
                    public boolean onMenuItemClick(MenuItem item) {
                        AppState.getInstance().libraryMode = AppState.MODE_GRID;
                        onGlidList();
                        sortAndFilter();
                        AppState.getInstance().save(getActivity());

                        return false;
                    }
                });

                p.getMenu().add(R.string.list).setIcon(R.drawable.glyphicons_114_justify).setOnMenuItemClickListener(new OnMenuItemClickListener() {

                    @Override
                    public boolean onMenuItemClick(MenuItem item) {
                        AppState.getInstance().libraryMode = AppState.MODE_LIST;
                        onGlidList();
                        sortAndFilter();
                        AppState.getInstance().save(getActivity());
                        return false;
                    }
                });

                p.getMenu().add(R.string.cover).setIcon(R.drawable.glyphicons_157_show_thumbnails).setOnMenuItemClickListener(new OnMenuItemClickListener() {

                    @Override
                    public boolean onMenuItemClick(MenuItem item) {
                        AppState.getInstance().libraryMode = AppState.MODE_COVERS;

                        onGlidList();
                        sortAndFilter();
                        AppState.getInstance().save(getActivity());
                        return false;
                    }
                });

                p.getMenu().add(R.string.author).setIcon(R.drawable.glyphicons_4_user).setOnMenuItemClickListener(new OnMenuItemClickListener() {

                    @Override
                    public boolean onMenuItemClick(MenuItem item) {
                        AppState.getInstance().libraryMode = AppState.MODE_AUTHORS;
                        filterLine.setText("");
                        onGlidList();
                        sortAndFilter();
                        AppState.getInstance().save(getActivity());

                        return false;
                    }
                });

                p.getMenu().add(R.string.genre).setIcon(R.drawable.glyphicons_66_tag).setOnMenuItemClickListener(new OnMenuItemClickListener() {

                    @Override
                    public boolean onMenuItemClick(MenuItem item) {
                        AppState.getInstance().libraryMode = AppState.MODE_GENRE;
                        filterLine.setText("");
                        onGlidList();
                        sortAndFilter();
                        AppState.getInstance().save(getActivity());

                        return false;
                    }
                });

                p.getMenu().add(R.string.series).setIcon(R.drawable.glyphicons_710_list_numbered).setOnMenuItemClickListener(new OnMenuItemClickListener() {

                    @Override
                    public boolean onMenuItemClick(MenuItem item) {
                        AppState.getInstance().libraryMode = AppState.MODE_SERIES;
                        filterLine.setText("");
                        onGlidList();
                        sortAndFilter();
                        AppState.getInstance().save(getActivity());

                        return false;
                    }
                });

                p.show();

                PopupHelper.initIcons(p, TintUtil.color);

            }
        });
    }

    public void updateFilterListAdapter() {
        try {
            ArrayList<String> list = new ArrayList<String>(autocompleteSuggestoins);
            Collections.sort(list);
            ArrayAdapter<String> adapter = new ArrayAdapter<String>(getActivity(), android.R.layout.simple_spinner_dropdown_item, list);
            filterLine.setAdapter(adapter);
            filterLine.setThreshold(1);
        } catch (Exception e) {
            LOG.e(e);
        }

    }

    int listHash = 0;

    @Override
    public void onSelected() {
        super.onSelected();
        LOG.d("SEARCH SELECTED");

        if (listHash != TempHolder.listHash) {
            listHash = TempHolder.listHash;
            onGlidList();
        }
        if (filterLine != null) {
            filterLine.clearFocus();
        }
    }

    public void onGlidList() {
        LOG.d("onGlidListRun", new Random().nextInt());

        if (getActivity() == null) {
            return;
        }

        filterLine.setEnabled(true);
        filterLine.setHint(R.string.search);

        if (AppState.get().libraryMode == AppState.MODE_AUTHORS || AppState.get().libraryMode == AppState.MODE_GENRE || AppState.get().libraryMode == AppState.MODE_SERIES) {
            filterLine.setEnabled(false);
            listMetaItems.clear();

            if (AppState.get().libraryMode == AppState.MODE_AUTHORS) {
                gridList.setImageResource(R.drawable.glyphicons_4_user);
                filterLine.setHint(R.string.author);
                filterLine.setTag(AppState.MODE_AUTHORS);

                if (authorsMetaItems.isEmpty()) {
                    LOG.d("authorsMetaItems is not empty");
                    for (FileMeta meta : AppDB.get().getRecent()) {
                        if (meta != null && meta.getAuthor() != null) {
                            if (TxtUtils.isNotEmpty(meta.getAuthor()) && !meta.getAuthor().contains(ExtUtils.REFLOW_FB2) && !authorsMetaItems.contains(meta.getAuthor())) {
                                authorsMetaItems.add(meta.getAuthor());
                            }
                        }
                    }
                    Collections.sort(authorsMetaItems, new StringInsensetiveByName());
                }

                listMetaItems.addAll(authorsMetaItems);

            }

            if (AppState.get().libraryMode == AppState.MODE_GENRE) {
                gridList.setImageResource(R.drawable.glyphicons_66_tag);
                filterLine.setHint(R.string.genre);
                filterLine.setTag(AppState.MODE_GENRE);

                if (genreMetaItems.isEmpty()) {
                    LOG.d("genreMetaItems is not empty");

                    for (FileMeta meta : AppDB.get().getRecent()) {
                        if (meta != null && TxtUtils.isNotEmpty(meta.getGenre())) {
                            for (String genre : meta.getGenre().split(",")) {
                                genre = TxtUtils.firstUppercase(genre.trim());
                                if (TxtUtils.isNotEmpty(genre) && !genreMetaItems.contains(genre)) {
                                    genreMetaItems.add(genre);
                                }
                            }
                        }
                    }
                    Collections.sort(genreMetaItems, new StringInsensetiveByName());
                }
                listMetaItems.addAll(genreMetaItems);
            }

            if (AppState.get().libraryMode == AppState.MODE_SERIES) {
                gridList.setImageResource(R.drawable.glyphicons_710_list_numbered);
                filterLine.setHint(R.string.series);
                filterLine.setTag(AppState.MODE_SERIES);

                if (seriesMetaItems.isEmpty()) {
                    LOG.d("seriesMetaItems is not empty");

                    for (FileMeta meta : AppDB.get().getRecent()) {
                        if (meta != null && TxtUtils.isNotEmpty(meta.getSequence())) {
                            for (String sequence : meta.getSequence().split(",")) {
                                sequence = sequence.trim();
                                if (TxtUtils.isNotEmpty(sequence) && !seriesMetaItems.contains(sequence)) {
                                    seriesMetaItems.add(sequence);
                                }
                            }
                        }
                    }
                    Collections.sort(seriesMetaItems, new StringInsensetiveByName());
                }
                listMetaItems.addAll(seriesMetaItems);
            }

            listView.setNumColumns(1);
            listView.setColumnWidth(-1);

            listView.setAdapter(authorsAdapter);
            // authorsAdapter.notifyDataSetChanged();

            listView.postDelayed(new Runnable() {

                @Override
                public void run() {
                    listView.setSelection(rememberPos + 1);
                }
            }, 50);

        } else {
            prevLibMode = AppState.get().libraryMode;
            listView.setAdapter(searchAdapter);

            if (AppState.get().libraryMode == AppState.MODE_LIST) {
                gridList.setImageResource(R.drawable.glyphicons_114_justify);

                int[] col = ADS.getNumberOfColumsAndWidth();
                listView.setNumColumns(col[0]);
                listView.setColumnWidth(col[1]);
            } else {
                if (AppState.get().libraryMode == AppState.MODE_GRID) {
                    gridList.setImageResource(R.drawable.glyphicons_156_show_big_thumbnails);
                } else {
                    gridList.setImageResource(R.drawable.glyphicons_157_show_thumbnails);
                }
                listView.setNumColumns(-1);
                listView.setColumnWidth(Dips.dpToPx(AppState.get().coverBigSize));
            }
            listView.setStretchMode(GridView.STRETCH_COLUMN_WIDTH);
        }

        countBooks.setText("" + listView.getAdapter().getCount());

    }

    @Override
    public void onConfigurationChanged(final android.content.res.Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        onGlidList();
    };

    View.OnClickListener onPro = new View.OnClickListener() {
        @Override
        public void onClick(final View v) {
            Urls.openPdfPro(getActivity());
        }
    };

    public void sortAndFilterIntent(String text) {
        if (TxtUtils.isNotEmpty(text)) {
            if (filterLine != null) {
                filterLine.setText(text);
            }
            sortAndFilter(text);
        }
    }

    public void sortAndFilter(String text) {
        if (!isAdded()) {
            return;
        }

        handler.removeCallbacks(sortAndSeach);

        final List<FileMeta> filtered = new ArrayList<FileMeta>();
        if (TxtUtils.isEmpty(text) || text.equals("@")) {
            cleanFilter.setVisibility(View.GONE);
            filtered.addAll(allFilesMeta);
        } else {
            cleanFilter.setVisibility(View.VISIBLE);
            text = text.trim().toLowerCase(Locale.US);

            boolean byAuthor = text.startsWith(PREFIX_AUTHOR);
            boolean byGenre = text.startsWith(PREFIX_GENRE);
            boolean bySeries = text.startsWith(PREFIX_SERIES);
            boolean byPath = text.startsWith(PREFIX_PATH);

            text = text.replace(PREFIX_AUTHOR, "").trim();
            text = text.replace(PREFIX_GENRE, "").trim();
            text = text.replace(PREFIX_SERIES, "").trim();
            text = text.replace(PREFIX_PATH, "").trim();

            for (FileMeta meta : AppDB.get().getRecent()) {
                if (meta == null) {
                    continue;
                }
                File file = new File(meta.getPath());
                if (byAuthor) {
                    if (meta.getAuthor() != null && meta.getAuthor().toLowerCase(Locale.US).contains(text)) {
                        // filtered.add(file);
                    }
                } else if (byGenre) {
                    if (meta.getGenre() != null && meta.getGenre().toLowerCase(Locale.US).contains(text)) {
                        // filtered.add(file);
                    }
                } else if (bySeries) {
                    if (meta.getSequence() != null && meta.getSequence().toLowerCase(Locale.US).contains(text)) {
                        // filtered.add(file);
                    }
                } else if (byPath) {
                    if (file.getPath().toLowerCase(Locale.US).contains(text)) {
                        // filtered.add(file);
                    }
                } else {
                    if (file.getName().toLowerCase(Locale.US).contains(text)) {
                        // filtered.add(file);
                    } else {
                        if (meta != null && (meta.getAuthor().toLowerCase(Locale.US).contains(text) || meta.getTitle().toLowerCase(Locale.US).contains(text))) {
                            // filtered.add(file);
                        }
                    }
                }
            }

        }

        if (AppState.getInstance().sortBy == AppState.SORT_BY_NAME) {
            // Collections.sort(filtered, Filer_By_Name);
        } else if (AppState.getInstance().sortBy == AppState.SORT_BY_PATH) {
            // Collections.sort(filtered, new FilerByPath());
        } else if (AppState.getInstance().sortBy == AppState.SORT_BY_SIZE) {
            // Collections.sort(filtered, new FilerBySize());
        } else if (AppState.getInstance().sortBy == AppState.SORT_BY_DATE) {
            // Collections.sort(filtered, new FilerByDate());
        }

        if (!AppState.getInstance().isSortAsc) {
            Collections.reverse(filtered);
        }

        initSearchByText();
        initSortOrderIcon();

        listView.post(new Runnable() {
            @Override
            public void run() {
                listView.setSelection(0);
            }
        });
        if (filtered.isEmpty()) {

            if (pathView != null) {
                pathView.setText("\"" + text + "\"" + " " + getString(R.string.books_not_found));
            }
            if (pathContainer != null) {
                pathContainer.setVisibility(View.VISIBLE);
            }
        } else {
            if (pathContainer != null) {
                pathContainer.setVisibility(View.GONE);
            }
        }

        showBookCount();
    }

    public void showBookCount() {
        try {
            countBooks.setText("" + listView.getAdapter().getCount());
        } catch (Exception e) {
        }
    }

    public void sortAndFilter() {
        sortAndFilter(filterLine.getText().toString());
    }

    private static final FilerByName Filer_By_Name = new FilerByName();

    static class FilerByName implements Comparator<File> {
        @Override
        public int compare(final File lhs, final File rhs) {
            return String.CASE_INSENSITIVE_ORDER.compare(lhs.getName(), rhs.getName());
        }
    }

    static class StringInsensetiveByName implements Comparator<String> {
        @Override
        public int compare(final String lhs, final String rhs) {
            return String.CASE_INSENSITIVE_ORDER.compare(lhs, rhs);
        }
    }

    static class FilerByPath implements Comparator<File> {
        @Override
        public int compare(final File lhs, final File rhs) {
            return String.CASE_INSENSITIVE_ORDER.compare(lhs.getPath(), rhs.getPath());
        }
    }

    static class FilerBySize implements Comparator<File> {
        @Override
        public int compare(final File lhs, final File rhs) {
            return new Long(lhs.length()).compareTo(new Long(rhs.length()));
        }
    }

    public static class FilerByDate implements Comparator<File> {
        @Override
        public int compare(final File lhs, final File rhs) {
            return new Long(lhs.lastModified()).compareTo(new Long(rhs.lastModified()));
        }
    }

    public void initSortOrderIcon() {
        if (AppState.getInstance().isSortAsc) {
            sortOrderImage.setImageResource(R.drawable.glyphicons_601_chevron_up);
        } else {
            sortOrderImage.setImageResource(R.drawable.glyphicons_602_chevron_down);
        }
    }

    public void initSearchByText() {
        if (AppState.getInstance().sortBy == AppState.SORT_BY_NAME) {
            sortBy.setText(R.string.by_file_name);
        } else if (AppState.getInstance().sortBy == AppState.SORT_BY_PATH) {
            sortBy.setText(R.string.by_path);
        } else if (AppState.getInstance().sortBy == AppState.SORT_BY_SIZE) {
            sortBy.setText(R.string.by_size);
        } else if (AppState.getInstance().sortBy == AppState.SORT_BY_DATE) {
            sortBy.setText(R.string.by_date);
        }
    }

    OnKeyListener onKeyListener = new OnKeyListener() {

        @Override
        public boolean onKey(View v, int keyCode, KeyEvent event) {
            if ((event.getAction() == KeyEvent.ACTION_DOWN) && (keyCode == KeyEvent.KEYCODE_ENTER)) {
                if (listView != null && listView.getAdapter() instanceof SearchAdapter) {
                    onStartFiltering();
                }
                return true;
            }
            return false;
        }
    };

    EditText.OnEditorActionListener onActionListener = new EditText.OnEditorActionListener() {

        @Override
        public boolean onEditorAction(final TextView v, final int actionId, final KeyEvent event) {
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                if (listView != null && listView.getAdapter() instanceof SearchAdapter) {
                    onStartFiltering();
                }
                return true;
            }
            return false;
        }
    };

    public void onStartFiltering() {
        final InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(filterLine.getWindowToken(), 0);
        if (searchAdapter.getCount() == 1) {
            filterLine.clearFocus();
            final File file = new File(searchAdapter.getItem(0).getPath());
            ExtUtils.showDocument(getActivity(), file);
        }
    }

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
                handler.postDelayed(sortAndSeach, 1000);

                handler.removeCallbacks(addTextToSuggestoins);
                handler.postDelayed(addTextToSuggestoins, 10 * 1000);
            }
        }

    };

    Runnable sortAndSeach = new Runnable() {

        @Override
        public void run() {
            sortAndFilter();
        }
    };

    Runnable addTextToSuggestoins = new Runnable() {

        @Override
        public void run() {
            try {
                String text = filterLine.getText().toString().trim();
                if (TxtUtils.isNotEmpty(text) && text.length() >= 3) {
                    if (!autocompleteSuggestoins.contains(text)) {
                        autocompleteSuggestoins.add(text);
                        saveAuthoSuggestoins();
                        updateFilterListAdapter();
                    }
                }
            } catch (Exception e) {
                LOG.e(e);
            }

        }
    };

    @Override
    public boolean onLongClickItem(int pos) {
        if (listView == null) {
            return false;
        }
        if (listView.getAdapter() instanceof SearchAdapter) {
            File item = new File(searchAdapter.getItem(pos).getPath());
            FileInformationDialog.showFileInfoDialog(getActivity(), item, null);
            return true;
        }
        return false;

    }

    int rememberPos = 0;

    @Override
    public void onItemClick(final int pos) {
        if (listView == null) {
            return;
        }

        if (AppState.get().libraryMode == AppState.MODE_AUTHORS || AppState.get().libraryMode == AppState.MODE_GENRE || AppState.get().libraryMode == AppState.MODE_SERIES) {
            rememberPos = listView.getFirstVisiblePosition();

            String name = (String) authorsAdapter.getItem(pos);

            if (AppState.get().libraryMode == AppState.MODE_AUTHORS) {
                name = PREFIX_AUTHOR + " " + name;
            }
            if (AppState.get().libraryMode == AppState.MODE_GENRE) {
                name = PREFIX_GENRE + " " + name;
            }
            if (AppState.get().libraryMode == AppState.MODE_SERIES) {
                name = PREFIX_SERIES + " " + name;
            }

            AppState.get().libraryMode = prevLibMode;

            filterLine.setText(name);
            filterLine.clearFocus();

            onGlidList();
            sortAndFilter(name);

        } else {
            File item = new File(searchAdapter.getItem(pos).getPath());
            ExtUtils.openFile(getActivity(), item);
        }
    }

    public boolean isBackProccesed() {
        if (listView == null) {
            return false;
        }
        if (listView.getAdapter() instanceof SearchAdapter) {
            if (TxtUtils.isEmpty(filterLine.getText().toString())) {
                return false;
            }

            Object tag = filterLine.getTag();
            if (tag != null) {
                AppState.get().libraryMode = (Integer) tag;
            }
            filterLine.setText("");
            onGlidList();
            sortAndFilter();
            return true;
        }
        if (listView.getAdapter() instanceof AuthorsAdapter) {
            AppState.get().libraryMode = prevLibMode;
            onGlidList();
            return true;
        }
        return false;
    }

    @Override
    public BaseAdapter getListAdapter() {
        return searchAdapter;
    }

    private static CopyAsyncTask async;

    SearchListener serchListener = new SearchListener() {

        @Override
        public void onUpdate() {
            notifyAdapter();
        }

        @Override
        public void onSearchFinish() {

        };
    };
    Runnable notifyRunnable = new Runnable() {

        @Override
        public void run() {
            // searchAdapter.notifyDataSetChanged();
            authorsAdapter.notifyDataSetChanged();
            if (allFilesMeta == null || allFilesMeta.isEmpty()) {
                return;
            }
        }
    };

    public void scan(final Activity a) {
        if (a == null) {
            return;
        }

        if (Build.VERSION.SDK_INT >= 23 && ContextCompat.checkSelfPermission(a, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        if (async != null && async.getStatus() == CopyAsyncTask.Status.RUNNING) {
            return;
        }

        IMG.clearDiscCache();
        IMG.clearMemoryCache();
        // MetaCache.get().clear();

        seriesMetaItems.clear();
        genreMetaItems.clear();
        authorsMetaItems.clear();
        cleanAuthoSuggestoins();

        async = new CopyAsyncTask<Object, Object, Object>() {

            @Override
            protected Object doInBackground(final Object... params) {
                try {
                    metaThreadCanWork = false;
                    allFilesMeta.clear();
                    for (final String path : AppState.getInstance().searchPaths.split(",")) {
                        if (path != null && path.trim().length() > 0) {
                            final File root = new File(path);
                            if (root.isDirectory()) {
                                SearchCore.search(allFilesMeta, root, ExtUtils.seachExts);
                            }
                        }
                    }
                    // MetaCache.get().fastDaoSave(recurciveResults);
                    metaThreadCanWork = true;
                    // startThread();

                } catch (final Exception e) {
                    Analytics.sendException(e, false);
                    e.printStackTrace();
                }

                return null;
            }

            @Override
            protected void onPreExecute() {
                if (getActivity() == null) {
                    return;
                }
                showLoadingText(R.string.searching_please_wait_);
                handler.post(timer);
            }

            @Override
            protected void onPostExecute(final Object result) {
                handler.removeCallbacks(timer);
                hideLoadingText();
                // sortAndFilter();
                showBookCount();
                searchAdapter.notifyDataSetChanged();
            };
        };
        async.execute();
    }

    Runnable timer = new Runnable() {

        @Override
        public void run() {
            showBookCount();
            handler.postDelayed(timer, 250);
        }
    };

    public void showLoadingText(int text) {
        try {
            pathView.setText(text);
            progressBar.setVisibility(View.VISIBLE);
            pathContainer.setVisibility(View.VISIBLE);

            getActivity().findViewById(R.id.onHome).setVisibility(View.GONE);
            getActivity().findViewById(R.id.onListGrid).setVisibility(View.GONE);
        } catch (Exception e) {
            LOG.e(e);
        }
    }

    public void hideLoadingText() {
        try {
            progressBar.setVisibility(View.GONE);
            pathContainer.setVisibility(View.GONE);

            getActivity().findViewById(R.id.onHome).setVisibility(View.GONE);
            getActivity().findViewById(R.id.onListGrid).setVisibility(View.GONE);
        } catch (Exception e) {
            LOG.e(e);
        }
    }

    private volatile boolean metaThreadCanWork = true;

    public void loadAllThread() {
        Thread thread = new Thread() {
            @Override
            public void run() {
                // MetaCache.get().loadAll(getActivity());
                handler.post(new Runnable() {

                    @Override
                    public void run() {
                        authorsMetaItems.clear();
                        genreMetaItems.clear();
                        seriesMetaItems.clear();
                    }
                });
            }
        };
        thread.start();
        thread.setPriority(Thread.MIN_PRIORITY);
    }

    public void startThread() {
        Thread thread = new Thread() {
            @Override
            public void run() {

                handler.post(new Runnable() {

                    @Override
                    public void run() {
                        authorsMetaItems.clear();
                        genreMetaItems.clear();
                        seriesMetaItems.clear();
                    }
                });

                // while (metaThreadCanWork && iterator.hasNext()) {
                // if (!metaThreadCanWork) {
                // break;
                // }
                // File next = iterator.next();
                // MetaCache.get().deleteCreateByPath(next.getPath());
                // }

                handler.post(new Runnable() {

                    @Override
                    public void run() {
                        authorsMetaItems.clear();
                        genreMetaItems.clear();
                        seriesMetaItems.clear();
                        hideLoadingText();
                        sortAndFilter();

                    }
                });

            }
        };
        thread.start();
        thread.setPriority(Thread.MIN_PRIORITY);
    }

    Handler h1 = new Handler();

    public void notifyAdapter() {
        h1.post(new Runnable() {

            @Override
            public void run() {
                searchAdapter.notifyDataSetChanged();
                showBookCount();
            }
        });
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        metaThreadCanWork = false;
        emptyRefs();
    }

    public void emptyRefs() {
        try {
            allFilesMeta.clear();
            // searchAdapter.setUris(null);
            searchAdapter = null;
            authorsAdapter = null;

            authorsMetaItems.clear();
            genreMetaItems.clear();
            seriesMetaItems.clear();

        } catch (Exception e) {
            LOG.e(e);
        }
    }

}
