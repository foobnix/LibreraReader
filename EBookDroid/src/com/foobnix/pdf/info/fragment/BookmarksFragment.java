package com.foobnix.pdf.info.fragment;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import com.foobnix.android.utils.TxtUtils;
import com.foobnix.pdf.info.AppSharedPreferences;
import com.foobnix.pdf.info.ExtUtils;
import com.foobnix.pdf.info.R;
import com.foobnix.pdf.info.presentation.BookmarksAdapter;
import com.foobnix.pdf.info.wrapper.AppBookmark;
import com.foobnix.sys.TempHolder;

import android.annotation.SuppressLint;
import android.os.Build;
import android.os.Bundle;
import android.text.Editable;
import android.text.Html;
import android.text.TextWatcher;
import android.view.MenuItem;
import android.view.MenuItem.OnMenuItemClickListener;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

public class BookmarksFragment extends BaseListFragment {
    private final List<AppBookmark> objects = new ArrayList<AppBookmark>();
    private BookmarksAdapter adapter;
    private EditText bookmarksEditSearch;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        adapter = new BookmarksAdapter(getActivity(), objects, false);
        adapter.notifyDataSetChanged();
        withBigAds = true;

    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        exportContainer.setVisibility(View.VISIBLE);

        final TextView export = (TextView) exportContainer.findViewById(R.id.exportBookmarks);
        export.setText(Html.fromHtml("<u>" + getString(R.string.export_bookmarks) + "</u>"));
        export.setOnClickListener(new OnClickListener() {

            @Override
            @SuppressLint("NewApi")
            public void onClick(View v) {
                if (Build.VERSION.SDK_INT <= 10) {
                    Toast.makeText(getActivity(), R.string.this_function_will_works_in_modern_android, Toast.LENGTH_SHORT).show();
                    return;
                }

                final PopupMenu popupMenu = new PopupMenu(export.getContext(), export);

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

        });

        final TextView importBookmarks = (TextView) exportContainer.findViewById(R.id.importBookmarks);
        importBookmarks.setText(Html.fromHtml("<u>" + getString(R.string.import_bookmarks) + "</u>"));
        importBookmarks.setOnClickListener(new OnClickListener() {

            @Override
            @SuppressLint("NewApi")
            public void onClick(View v) {
                if (Build.VERSION.SDK_INT <= 10) {
                    Toast.makeText(getActivity(), R.string.this_function_will_works_in_modern_android, Toast.LENGTH_SHORT).show();
                    return;
                }

                final PopupMenu popupMenu = new PopupMenu(importBookmarks.getContext(), importBookmarks);

                final MenuItem toJson = popupMenu.getMenu().add("JSON");
                toJson.setOnMenuItemClickListener(new OnMenuItemClickListener() {

                    @Override
                    public boolean onMenuItemClick(final MenuItem item) {

                        ExtUtils.importAllBookmarksFromJson(getActivity(), new Runnable() {

                            @Override
                            public void run() {
                                onSelected();
                            }
                        });

                        return false;
                    }
                });
                popupMenu.show();

            }

        });

        TextView search = TxtUtils.underlineTextView((TextView) exportContainer.findViewById(R.id.searchBookmarks));
        search.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                boolean isVisible = bookmarksSearchContainer.getVisibility() == View.VISIBLE;
                if (isVisible) {
                    bookmarksSearchContainer.setVisibility(View.GONE);
                } else {
                    bookmarksSearchContainer.setVisibility(View.VISIBLE);
                }
                if (TxtUtils.isNotEmpty(bookmarksEditSearch.getText().toString())) {
                    bookmarksEditSearch.setText("");
                    filterByText();
                }
            }
        });
        bookmarksEditSearch = (EditText) bookmarksSearchContainer.findViewById(R.id.bookmarksEditSearch);
        bookmarksEditSearch.addTextChangedListener(filterTextWatcher);

        ImageView clearImage = (ImageView) bookmarksSearchContainer.findViewById(R.id.bookmarksClearFilter);
        clearImage.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                bookmarksEditSearch.setText("");
                filterByText();
            }
        });
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
            filterByText();
        }

    };

    int listHash = 0;

    public void filterByText() {
        if (bookmarksEditSearch == null) {
            return;
        }

        if (listHash == TempHolder.listHash) {
            // return;
        }
        listHash = TempHolder.listHash;

        String text = bookmarksEditSearch.getText().toString().toLowerCase().trim();
        adapter.setHiglightText(null);
        if (TxtUtils.isEmpty(text)) {
            adapter.setMuxnumberOfLines(3);
            onSelected();
        } else {
            List<AppBookmark> bookmarks = AppSharedPreferences.get().getBookmarks();
            List<AppBookmark> filtered = new ArrayList<AppBookmark>();
            for (AppBookmark bookmark : bookmarks) {
                if (bookmark.getText().toLowerCase().contains(text)) {
                    filtered.add(bookmark);
                }
            }

            if (text.length() >= 2) {
                adapter.setHiglightText(text);

                adapter.setMuxnumberOfLines(Integer.MAX_VALUE);
                objects.clear();
                objects.addAll(filtered);
                adapter.notifyDataSetChanged();
            }
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        filterByText();
    }

    public void onCheckAds() {
        super.onSelected();
    }

    @Override
    public void onSelected() {
        objects.clear();
        List<AppBookmark> bookmarks = AppSharedPreferences.get().getBookmarks();
        objects.addAll(bookmarks);
        adapter.notifyDataSetChanged();
    }

    @Override
    public BaseAdapter getListAdapter() {
        return adapter;
    }

    @Override
    public void onItemClick(int pos) {
        AppBookmark appBookmark = objects.get(pos);
        if (ExtUtils.doifFileExists(getContext(), appBookmark.getPath())) {
            final File file = new File(appBookmark.getPath());
            ExtUtils.showDocument(getActivity(), file, appBookmark.getPage());
        }

    }

}