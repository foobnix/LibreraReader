package com.foobnix.ui2.fragment;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

import org.ebookdroid.common.settings.SettingsManager;
import org.ebookdroid.common.settings.books.BookSettings;

import com.foobnix.android.utils.Keyboards;
import com.foobnix.android.utils.ResultResponse;
import com.foobnix.android.utils.TxtUtils;
import com.foobnix.pdf.info.AppSharedPreferences;
import com.foobnix.pdf.info.ExtUtils;
import com.foobnix.pdf.info.R;
import com.foobnix.pdf.info.TintUtil;
import com.foobnix.pdf.info.view.MyPopupMenu;
import com.foobnix.pdf.info.widget.FileInformationDialog;
import com.foobnix.pdf.info.wrapper.AppBookmark;
import com.foobnix.pdf.info.wrapper.AppState;
import com.foobnix.pdf.info.wrapper.PopupHelper;
import com.foobnix.ui2.adapter.BookmarksAdapter2;

import android.os.Bundle;
import android.support.v4.util.Pair;
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
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.TextView;

public class BookmarksFragment2 extends UIFragment<AppBookmark> {
    public static final Pair<Integer, Integer> PAIR = new Pair<Integer, Integer>(R.string.bookmarks, R.drawable.glyphicons_73_bookmark);
    private static final String BOOK_PREFIX = "@book";

    BookmarksAdapter2 bookmarksAdapter;
    View bookmarksSearchContainer, bookmarksClearFilter, topPanel;
    TextView exportBookmarks, importBookmarks, search, allBookmarks;
    EditText bookmarksEditSearch;
    ImageView onListGrid;

    @Override
    public Pair<Integer, Integer> getNameAndIconRes() {
        return PAIR;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_bookmarks2, container, false);
        recyclerView = (RecyclerView) view.findViewById(R.id.recyclerView);

        topPanel = view.findViewById(R.id.topPanel);
        bookmarksSearchContainer = view.findViewById(R.id.bookmarksSearchContainer);
        bookmarksClearFilter = view.findViewById(R.id.bookmarksClearFilter);
        bookmarksEditSearch = (EditText) view.findViewById(R.id.bookmarksEditSearch);
        bookmarksEditSearch.addTextChangedListener(filterTextWatcher);

        onListGrid = (ImageView) view.findViewById(R.id.onListGrid);
        exportBookmarks = (TextView) view.findViewById(R.id.exportBookmarks);
        importBookmarks = (TextView) view.findViewById(R.id.importBookmarks);
        search = (TextView) view.findViewById(R.id.search);
        allBookmarks = (TextView) view.findViewById(R.id.allBookmarks);
        TxtUtils.underlineTextView(allBookmarks).setOnClickListener(onCleanSearch);

        TxtUtils.underlineTextView(exportBookmarks).setOnClickListener(exportBookmarksClickListener);
        TxtUtils.underlineTextView(importBookmarks).setOnClickListener(importBookmarksClickListener);
        TxtUtils.underlineTextView(search).setOnClickListener(searchBookmarks);
        bookmarksSearchContainer.setVisibility(View.GONE);

        bookmarksClearFilter.setOnClickListener(onCleanSearch);

        bookmarksAdapter = new BookmarksAdapter2();

        RecyclerView.LayoutManager mLayoutManager = new LinearLayoutManager(getActivity());
        recyclerView.setLayoutManager(mLayoutManager);
        recyclerView.setAdapter(bookmarksAdapter);
        bookmarksAdapter.setOnDeleteClickListener(onDeleteResponse);

        bookmarksAdapter.setOnItemClickListener(onItemClickListener);
        bookmarksAdapter.setOnItemLongClickListener(new ResultResponse<AppBookmark>() {

            @Override
            public boolean onResultRecive(AppBookmark result) {
                FileInformationDialog.showFileInfoDialog(getActivity(), new File(result.getPath()), null);
                return true;
            }
        });

        onListGrid.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                popupMenu(onListGrid);
            }
        });

        populate();
        onTintChanged();
        return view;
    }


    private void popupMenu(final ImageView onGridList) {
        MyPopupMenu p = new MyPopupMenu(getActivity(), onGridList);
        PopupHelper.addPROIcon(p, getActivity());

        List<Integer> names = Arrays.asList(R.string.bookmark_by_date, R.string.bookmark_by_book);
        final List<Integer> icons = Arrays.asList(R.drawable.glyphicons_114_justify, R.drawable.glyphicons_157_1_show_thumbnails);
        final List<Integer> actions = Arrays.asList(AppState.BOOKMARK_MODE_BY_DATE, AppState.BOOKMARK_MODE_BY_BOOK);

        for (int i = 0; i < names.size(); i++) {
            final int index = i;
            p.getMenu().add(names.get(i)).setIcon(icons.get(i)).setOnMenuItemClickListener(new OnMenuItemClickListener() {

                @Override
                public boolean onMenuItemClick(MenuItem item) {
                    AppState.get().bookmarksMode = actions.get(index);
                    onGridList.setImageResource(icons.get(index));
                    bookmarksEditSearch.setText("");
                    bookmarksSearchContainer.setVisibility(View.GONE);
                    populate();
                    return false;
                }
            });
        }
        p.show();
    }

    @Override
    public void onTintChanged() {
        TintUtil.setBackgroundFillColor(topPanel, TintUtil.color);
        TintUtil.setStrokeColor(bookmarksEditSearch, TintUtil.color);
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
            handler.removeCallbacks(timer);
            handler.postDelayed(timer, 500);
        }
    };
    Runnable timer = new Runnable() {

        @Override
        public void run() {
            populate();
        }
    };

    OnClickListener exportBookmarksClickListener = new OnClickListener() {

        @Override
        public void onClick(View v) {
            final PopupMenu popupMenu = new PopupMenu(getActivity(), exportBookmarks);

            final MenuItem toEmail = popupMenu.getMenu().add(R.string.email);
            toEmail.setOnMenuItemClickListener(new OnMenuItemClickListener() {

                @Override
                public boolean onMenuItemClick(final MenuItem item) {
                    ExtUtils.exportAllBookmarksToGmail(getActivity());
                    return false;
                }
            });

            final MenuItem toFile = popupMenu.getMenu().add(R.string.file);
            toFile.setOnMenuItemClickListener(new OnMenuItemClickListener() {

                @Override
                public boolean onMenuItemClick(final MenuItem item) {
                    ExtUtils.exportAllBookmarksToFile(getActivity());
                    return false;
                }
            });

            final MenuItem toJson = popupMenu.getMenu().add("JSON");
            toJson.setOnMenuItemClickListener(new OnMenuItemClickListener() {

                @Override
                public boolean onMenuItemClick(final MenuItem item) {

                    ExtUtils.exportAllBookmarksToJson(getActivity());
                    return false;
                }
            });
            popupMenu.show();

        }
    };

    OnClickListener onCleanSearch = new OnClickListener() {

        @Override
        public void onClick(View v) {
            bookmarksEditSearch.setText("");
            populate();
        }
    };

    OnClickListener importBookmarksClickListener = new OnClickListener() {

        @Override
        public void onClick(View v) {
            final PopupMenu popupMenu = new PopupMenu(importBookmarks.getContext(), importBookmarks);

            final MenuItem toJson = popupMenu.getMenu().add("JSON");
            toJson.setOnMenuItemClickListener(new OnMenuItemClickListener() {

                @Override
                public boolean onMenuItemClick(final MenuItem item) {

                    ExtUtils.importAllBookmarksFromJson(getActivity(), new Runnable() {

                        @Override
                        public void run() {
                            populate();
                        }
                    });

                    return false;
                }
            });
            popupMenu.show();

        }
    };

    OnClickListener searchBookmarks = new OnClickListener() {

        @Override
        public void onClick(View v) {
            boolean isVisible = bookmarksSearchContainer.getVisibility() == View.VISIBLE;
            if (isVisible) {
                bookmarksSearchContainer.setVisibility(View.GONE);
                Keyboards.close(bookmarksEditSearch);
            } else {
                bookmarksSearchContainer.setVisibility(View.VISIBLE);
            }
            if (TxtUtils.isNotEmpty(bookmarksEditSearch.getText().toString())) {
                bookmarksEditSearch.setText("");
                // filterByText();
            }

        }
    };

    @Override
    public boolean isBackPressed() {
        if (bookmarksEditSearch != null && TxtUtils.isNotEmpty(bookmarksEditSearch.getText().toString())) {
            bookmarksEditSearch.setText("");
            populate();
            return true;
        }
        return false;
    }

    ResultResponse<AppBookmark> onTitleClickListener = new ResultResponse<AppBookmark>() {

        @Override
        public boolean onResultRecive(AppBookmark result) {
            // bookmarksSearchContainer.setVisibility(View.VISIBLE);
            bookmarksEditSearch.setText(BOOK_PREFIX + " " + result.getTitle());
            populate();
            return false;
        }
    };

    ResultResponse<AppBookmark> onItemClickListener = new ResultResponse<AppBookmark>() {

        @Override
        public boolean onResultRecive(AppBookmark result) {
            String text = bookmarksEditSearch.getText().toString().toLowerCase(Locale.US).trim();
            if (TxtUtils.isNotEmpty(text) || AppState.get().bookmarksMode == AppState.BOOKMARK_MODE_BY_DATE) {
                if (ExtUtils.doifFileExists(getContext(), result.getPath())) {
                    final File file = new File(result.getPath());
                    BookSettings bs = SettingsManager.getBookSettings(result.getPath());
                    if (bs.splitPages) {
                        ExtUtils.showDocument(getActivity(), file, result.getPage() * 2);
                    } else {
                        ExtUtils.showDocument(getActivity(), file, result.getPage());
                    }
                }
            } else {
                bookmarksEditSearch.setText(BOOK_PREFIX + " " + result.getTitle());
                populate();
            }
            return false;
        }
    };

    ResultResponse<AppBookmark> onDeleteResponse = new ResultResponse<AppBookmark>() {

        @Override
        public boolean onResultRecive(AppBookmark result) {
            AppSharedPreferences.get().removeBookmark(result);
            populate();
            return false;
        }
    };

    @Override
    public List<AppBookmark> prepareDataInBackground() {
        handler.removeCallbacks(timer);

        String text = bookmarksEditSearch.getText().toString().toLowerCase(Locale.US).trim();
        if (TxtUtils.isEmpty(text)) {
            List<AppBookmark> bookmarks = AppSharedPreferences.get().getBookmarks();

            if (AppState.get().bookmarksMode == AppState.BOOKMARK_MODE_BY_BOOK) {
                List<AppBookmark> filtered = new ArrayList<AppBookmark>();
                List<String> unic = new ArrayList<String>();
                for (AppBookmark bookmark : bookmarks) {
                    if (!unic.contains(bookmark.getPath())) {
                        unic.add(bookmark.getPath());
                        filtered.add(bookmark);
                    }
                }
                return filtered;
            } else {
                return bookmarks;
            }
        } else {
            List<AppBookmark> filtered = new ArrayList<AppBookmark>();
            List<AppBookmark> bookmarks = AppSharedPreferences.get().getBookmarks();

            if (text.startsWith(BOOK_PREFIX)) {
                text = text.replace(BOOK_PREFIX, "").trim();
                for (final AppBookmark bookmark : bookmarks) {
                    if (bookmark.getTitle().toLowerCase(Locale.US).contains(text.toLowerCase(Locale.US))) {
                        filtered.add(bookmark);
                    }
                }

                Collections.sort(filtered, new Comparator<AppBookmark>() {

                    @Override
                    public int compare(AppBookmark o1, AppBookmark o2) {
                        return new Integer(o1.getPage()).compareTo(new Integer(o2.getPage()));
                    }
                });

            } else {
                for (AppBookmark bookmark : bookmarks) {
                    if (bookmark.getText().toLowerCase(Locale.US).contains(text)) {
                        filtered.add(bookmark);
                    }
                }
            }

            return filtered;
        }

    }

    public boolean isPrefixText() {
        String text = bookmarksEditSearch.getText().toString().toLowerCase(Locale.US).trim();
        return text.startsWith(BOOK_PREFIX);
    }

    @Override
    public void populateDataInUI(List<AppBookmark> items) {
        if (AppState.get().bookmarksMode == AppState.BOOKMARK_MODE_BY_DATE) {
            onListGrid.setImageResource(R.drawable.glyphicons_114_justify);
            bookmarksAdapter.withPageNumber = true;
        } else if (AppState.get().bookmarksMode == AppState.BOOKMARK_MODE_BY_BOOK) {
            bookmarksAdapter.withPageNumber = false;
            onListGrid.setImageResource(R.drawable.glyphicons_157_1_show_thumbnails);
        }
        if (TxtUtils.isNotEmpty(bookmarksEditSearch.getText().toString().toLowerCase(Locale.US).trim())) {
            bookmarksAdapter.withPageNumber = true;
        }

        bookmarksAdapter.getItemsList().clear();
        if (items != null) {
            bookmarksAdapter.getItemsList().addAll(items);
        }
        bookmarksAdapter.notifyDataSetChanged();

        if (isPrefixText()) {
            allBookmarks.setVisibility(View.VISIBLE);
        } else {
            allBookmarks.setVisibility(View.GONE);
        }

    }

    @Override
    public void notifyFragment() {

    }

    @Override
    public void resetFragment() {
        populate();
    }

}
