package com.foobnix.ui2.fragment;

import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.PopupMenu;

import androidx.annotation.NonNull;
import androidx.core.util.Pair;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.foobnix.android.utils.Keyboards;
import com.foobnix.android.utils.LOG;
import com.foobnix.android.utils.ResultResponse;
import com.foobnix.android.utils.TxtUtils;
import com.foobnix.model.AppBookmark;
import com.foobnix.model.AppState;
import com.foobnix.pdf.info.BookmarksData;
import com.foobnix.pdf.info.ExtUtils;
import com.foobnix.pdf.info.R;
import com.foobnix.pdf.info.TintUtil;
import com.foobnix.pdf.info.databinding.FragmentBookmarks2Binding;
import com.foobnix.pdf.info.view.MyPopupMenu;
import com.foobnix.pdf.info.widget.FileInformationDialog;
import com.foobnix.pdf.info.wrapper.PopupHelper;
import com.foobnix.ui2.adapter.BookmarksAdapter2;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public class BookmarksFragment2 extends UIFragment<AppBookmark> {
    public static final Pair<Integer, Integer> PAIR = new Pair<>(R.string.bookmarks, R.drawable.glyphicons_73_bookmark);
    private static final String BOOK_PREFIX = "@book";

    BookmarksAdapter2 bookmarksAdapter;
    private FragmentBookmarks2Binding binding;

    @Override
    public Pair<Integer, Integer> getNameAndIconRes() {
        return PAIR;
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentBookmarks2Binding.inflate(inflater, container, false);
        recyclerView = binding.recyclerView;

        binding.bookmarksEditSearch.addTextChangedListener(filterTextWatcher);

        TxtUtils.underlineTextView(binding.allBookmarks).setOnClickListener(onCleanSearch);

        TxtUtils.underlineTextView(binding.exportBookmarks).setOnClickListener(exportBookmarksClickListener);
        TxtUtils.underlineTextView(binding.importBookmarks).setOnClickListener(importBookmarksClickListener);
        binding.search.setOnClickListener(searchBookmarks);
        binding.bookmarksSearchContainer.setVisibility(View.GONE);

        binding.bookmarksClearFilter.setOnClickListener(onCleanSearch);

        bookmarksAdapter = new BookmarksAdapter2();

        RecyclerView.LayoutManager mLayoutManager = new LinearLayoutManager(getActivity());
        recyclerView.setLayoutManager(mLayoutManager);
        recyclerView.setAdapter(bookmarksAdapter);
        bookmarksAdapter.setOnDeleteClickListener(onDeleteResponse);

        bookmarksAdapter.setOnItemClickListener(onItemClickListener);
        bookmarksAdapter.setOnItemLongClickListener(result -> {
            FileInformationDialog.showFileInfoDialog(getActivity(), new File(result.getPath()), null);
            return true;
        });

        binding.onListGrid.setOnClickListener(v -> popupMenu());

        binding.onSettings.setOnClickListener(v -> {
            MyPopupMenu menu = new MyPopupMenu(v);
            menu.getMenu().addCheckbox(getString(R.string.show_quick_bookmarks), AppState.get().isShowFastBookmarks, (a, is) -> {
                AppState.get().isShowFastBookmarks = is;
                populate();
                LOG.d("show--show_quick_bookmarks");
            });
            menu.getMenu().addCheckbox(getString(R.string.show_only_available_books), AppState.get().isShowOnlyAvailabeBooks, (a, is) -> {
                AppState.get().isShowOnlyAvailabeBooks = is;
                populate();
                LOG.d("show--_only_available_books");
            });

            menu.getMenu(R.drawable.glyphicons_basic_578_share, R.string.share,
                    () -> ExtUtils.sendAllBookmarksTo(getActivity()));

            menu.show();
        });

        populate();
        onTintChanged();
        return binding.getRoot();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    private void popupMenu() {
        MyPopupMenu p = new MyPopupMenu(getActivity(), binding.onListGrid);
        PopupHelper.addPROIcon(p, getActivity());

        List<Integer> names = Arrays.asList(R.string.bookmark_by_date, R.string.bookmark_by_book);
        final List<Integer> icons = Arrays.asList(R.drawable.glyphicons_114_justify, R.drawable.glyphicons_157_1_show_thumbnails);
        final List<Integer> actions = Arrays.asList(AppState.BOOKMARK_MODE_BY_DATE, AppState.BOOKMARK_MODE_BY_BOOK);

        for (int i = 0; i < names.size(); i++) {
            final int index = i;
            p.getMenu().add(names.get(i)).setIcon(icons.get(i)).setOnMenuItemClickListener(item -> {
                AppState.get().bookmarksMode = actions.get(index);
                binding.onListGrid.setImageResource(icons.get(index));
                binding.bookmarksEditSearch.setText("");
                binding.bookmarksSearchContainer.setVisibility(View.GONE);
                populate();
                return false;
            });
        }
        p.show();
    }

    @Override
    public void onTintChanged() {
        TintUtil.setBackgroundFillColor(binding.topPanel, TintUtil.color);
        TintUtil.setStrokeColor(binding.bookmarksEditSearch, TintUtil.color);
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
    Runnable timer = this::populate;

    OnClickListener exportBookmarksClickListener = new OnClickListener() {
        @Override
        public void onClick(View v) {
            final PopupMenu popupMenu = new PopupMenu(getActivity(), binding.exportBookmarks);

            final MenuItem toEmail = popupMenu.getMenu().add(R.string.email);
            toEmail.setOnMenuItemClickListener(item -> {
                ExtUtils.exportAllBookmarksToGmail(getActivity());
                return false;
            });

            final MenuItem toFile = popupMenu.getMenu().add(R.string.file);
            toFile.setOnMenuItemClickListener(item -> {
                ExtUtils.exportAllBookmarksToFile(getActivity());
                return false;
            });

            final MenuItem toJson = popupMenu.getMenu().add("JSON");
            toJson.setOnMenuItemClickListener(item -> {
                ExtUtils.exportAllBookmarksToJson(getActivity(), null);
                return false;
            });
            popupMenu.show();
        }
    };

    OnClickListener onCleanSearch = new OnClickListener() {
        @Override
        public void onClick(View v) {
            binding.bookmarksEditSearch.setText("");
            populate();
        }
    };

    OnClickListener importBookmarksClickListener = new OnClickListener() {

        @Override
        public void onClick(View v) {
            final PopupMenu popupMenu = new PopupMenu(binding.importBookmarks.getContext(), binding.importBookmarks);

            final MenuItem toJson = popupMenu.getMenu().add("JSON");
            toJson.setOnMenuItemClickListener(item -> {
                ExtUtils.importAllBookmarksFromJson(getActivity(), BookmarksFragment2.this::populate);

                return false;
            });
            popupMenu.show();
        }
    };

    OnClickListener searchBookmarks = new OnClickListener() {

        @Override
        public void onClick(View v) {
            boolean isVisible = binding.bookmarksSearchContainer.getVisibility() == View.VISIBLE;
            if (isVisible) {
                binding.bookmarksSearchContainer.setVisibility(View.GONE);
                Keyboards.close(binding.bookmarksEditSearch);
            } else {
                binding.bookmarksSearchContainer.setVisibility(View.VISIBLE);
            }
            if (TxtUtils.isNotEmpty(binding.bookmarksEditSearch.getText().toString())) {
                binding.bookmarksEditSearch.setText("");
                // filterByText();
            }
        }
    };

    @Override
    public boolean isBackPressed() {
        if (TxtUtils.isNotEmpty(binding.bookmarksEditSearch.getText().toString())) {
            binding.bookmarksEditSearch.setText("");
            populate();
            return true;
        }
        return false;
    }

    ResultResponse<AppBookmark> onItemClickListener = new ResultResponse<AppBookmark>() {
        @Override
        public boolean onResultRecive(AppBookmark result) {
            String text = binding.bookmarksEditSearch.getText().toString().toLowerCase(Locale.US).trim();
            if (TxtUtils.isNotEmpty(text) || AppState.get().bookmarksMode == AppState.BOOKMARK_MODE_BY_DATE) {
                if (ExtUtils.doifFileExists(getContext(), result.getPath())) {
                    final File file = new File(result.getPath());
                    ExtUtils.showDocumentWithoutDialog2(getActivity(), Uri.fromFile(file), result.getPercent(), null);
                }
            } else {
                binding.bookmarksEditSearch.setText(BOOK_PREFIX + " " + result.getPath());
                populate();
            }
            return false;
        }
    };

    ResultResponse<AppBookmark> onDeleteResponse = new ResultResponse<AppBookmark>() {

        @Override
        public boolean onResultRecive(AppBookmark result) {
            if (bookmarksAdapter.withPageNumber) {
                BookmarksData.get().remove(result);
                populate();
            } else {
                ExtUtils.sendBookmarksTo(getActivity(), new File(result.getPath()));
            }
            return false;
        }
    };

    @Override
    public List<AppBookmark> prepareDataInBackground() {
        handler.removeCallbacks(timer);

        String text = binding.bookmarksEditSearch.getText().toString().toLowerCase(Locale.US).trim();
        if (TxtUtils.isEmpty(text)) {
            List<AppBookmark> bookmarks = BookmarksData.get().getAll(requireActivity());

            if (AppState.get().bookmarksMode == AppState.BOOKMARK_MODE_BY_BOOK) {
                List<AppBookmark> filtered = new ArrayList<>();
                List<String> unic = new ArrayList<>();
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
            List<AppBookmark> filtered = new ArrayList<>();
            List<AppBookmark> bookmarks = BookmarksData.get().getAll(requireActivity());

            if (text.startsWith(BOOK_PREFIX)) {
                text = text.replace(BOOK_PREFIX, "").trim();
                for (final AppBookmark bookmark : bookmarks) {
                    if (bookmark.getPath().toLowerCase(Locale.US).contains(text.toLowerCase(Locale.US))) {
                        filtered.add(bookmark);
                    }
                }


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
        String text = binding.bookmarksEditSearch.getText().toString().toLowerCase(Locale.US).trim();
        return text.startsWith(BOOK_PREFIX);
    }

    @Override
    public void populateDataInUI(List<AppBookmark> items) {
        if (AppState.get().bookmarksMode == AppState.BOOKMARK_MODE_BY_DATE) {
            binding.onListGrid.setImageResource(R.drawable.glyphicons_114_justify);
            bookmarksAdapter.withPageNumber = true;
        } else if (AppState.get().bookmarksMode == AppState.BOOKMARK_MODE_BY_BOOK) {
            bookmarksAdapter.withPageNumber = false;
            binding.onListGrid.setImageResource(R.drawable.glyphicons_157_1_show_thumbnails);
        }
        if (TxtUtils.isNotEmpty(binding.bookmarksEditSearch.getText().toString().toLowerCase(Locale.US).trim())) {
            bookmarksAdapter.withPageNumber = true;
        }

        bookmarksAdapter.getItemsList().clear();
        if (items != null) {
            bookmarksAdapter.getItemsList().addAll(items);
        }
        bookmarksAdapter.notifyDataSetChanged();

        if (isPrefixText()) {
            binding.allBookmarks.setVisibility(View.VISIBLE);
        } else {
            binding.allBookmarks.setVisibility(View.GONE);
        }
    }

    @Override
    public void notifyFragment() {
        populate();
    }

    @Override
    public void resetFragment() {
        populate();
    }

}
