package com.foobnix.ui2.fragment;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.ebookdroid.BookType;

import com.cloudrail.si.interfaces.CloudStorage;
import com.cloudrail.si.types.CloudMetaData;
import com.foobnix.android.utils.LOG;
import com.foobnix.android.utils.ResultResponse;
import com.foobnix.android.utils.StringDB;
import com.foobnix.android.utils.TxtUtils;
import com.foobnix.dao2.FileMeta;
import com.foobnix.pdf.info.AppsConfig;
import com.foobnix.pdf.info.Clouds;
import com.foobnix.pdf.info.ExtUtils;
import com.foobnix.pdf.info.FileMetaComparators;
import com.foobnix.pdf.info.R;
import com.foobnix.pdf.info.TintUtil;
import com.foobnix.pdf.info.io.SearchCore;
import com.foobnix.pdf.info.view.MyPopupMenu;
import com.foobnix.pdf.info.wrapper.AppState;
import com.foobnix.pdf.info.wrapper.PopupHelper;
import com.foobnix.pdf.search.view.AsyncProgressTask;
import com.foobnix.ui2.AppDB;
import com.foobnix.ui2.FileMetaCore;
import com.foobnix.ui2.MainTabs2;
import com.foobnix.ui2.adapter.DefaultListeners;
import com.foobnix.ui2.adapter.FileMetaAdapter;
import com.foobnix.ui2.fast.FastScrollRecyclerView;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.DocumentsContract;
import android.provider.DocumentsContract.Document;
import android.support.v4.util.Pair;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.MenuItem.OnMenuItemClickListener;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.EditText;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

@SuppressLint("NewApi")
public class BrowseFragment2 extends UIFragment<FileMeta> {

    public static final Pair<Integer, Integer> PAIR = new Pair<Integer, Integer>(R.string.folders, R.drawable.glyphicons_145_folder_open);
    public static final String EXTRA_INIT_PATH = "EXTRA_PATH";
    public static final String EXTRA_TYPE = "EXTRA_TYPE";
    public static final String EXTRA_TEXT = "EXTRA_TEXT";
    public static int TYPE_DEFAULT = 0;
    public static int TYPE_SELECT_FOLDER = 1;
    public static int TYPE_SELECT_FILE = 2;
    public static int TYPE_CREATE_FILE = 3;

    FileMetaAdapter searchAdapter;

    private LinearLayout paths;
    HorizontalScrollView scroller;
    private TextView stub;
    private ImageView onListGrid, starIcon, onSort;
    private EditText editPath;
    private View pathContainer, onClose, onAction;

    private int fragmentType = TYPE_DEFAULT;
    private String fragmentText = "";
    Map<String, Integer> rememberPos = new HashMap<String, Integer>();

    public BrowseFragment2() {
        super();
    }

    public static BrowseFragment2 newInstance(Bundle bundle) {
        BrowseFragment2 br = new BrowseFragment2();
        br.setArguments(bundle);
        return br;
    }

    @Override
    public Pair<Integer, Integer> getNameAndIconRes() {
        return PAIR;
    }

    private ResultResponse<String> onPositiveAction;
    private ResultResponse<String> onCloseAction;

    @Override
    public void onTintChanged() {
        TintUtil.setBackgroundFillColor(pathContainer, TintUtil.color);
        TintUtil.setBackgroundFillColor(onClose, TintUtil.color);
        TintUtil.setBackgroundFillColor(onAction, TintUtil.color);
    }

    public CloudStorage cloudStorage;

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_browse2, container, false);

        Bundle arguments = getArguments();

        pathContainer = view.findViewById(R.id.pathContainer);
        View onCloseActionPaner = view.findViewById(R.id.onCloseActionPaner);
        onClose = view.findViewById(R.id.onClose);

        starIcon = (ImageView) view.findViewById(R.id.starIcon);
        onSort = (ImageView) view.findViewById(R.id.onSort);

        onSort.setOnLongClickListener(new OnLongClickListener() {

            @Override
            public boolean onLongClick(View v) {
                AppState.get().sortByReverse = !AppState.get().sortByReverse;
                onSort.setImageResource(AppState.get().sortByReverse ? R.drawable.glyphicons_410_sort_by_attributes_alt : R.drawable.glyphicons_409_sort_by_attributes);
                populate();
                return true;
            }
        });

        onSort.setImageResource(AppState.get().sortByReverse ? R.drawable.glyphicons_410_sort_by_attributes_alt : R.drawable.glyphicons_409_sort_by_attributes);

        onAction = view.findViewById(R.id.onAction);
        editPath = (EditText) view.findViewById(R.id.editPath);

        fragmentType = TYPE_DEFAULT;
        if (arguments != null) {
            fragmentType = arguments.getInt(EXTRA_TYPE, TYPE_DEFAULT);
            fragmentText = arguments.getString(EXTRA_TEXT);
            editPath.setText(fragmentText);
        }

        onClose.setOnClickListener(onCloseButtonActoin);
        onAction.setOnClickListener(onSelectAction);

        if (TYPE_DEFAULT == fragmentType) {
            editPath.setVisibility(View.GONE);
            onCloseActionPaner.setVisibility(View.GONE);
        }
        if (TYPE_SELECT_FOLDER == fragmentType) {
            editPath.setVisibility(View.VISIBLE);
            editPath.setEnabled(false);
            onCloseActionPaner.setVisibility(View.VISIBLE);
        }
        if (TYPE_SELECT_FILE == fragmentType) {
            editPath.setVisibility(View.VISIBLE);
            editPath.setEnabled(false);
            onCloseActionPaner.setVisibility(View.VISIBLE);
        }
        if (TYPE_CREATE_FILE == fragmentType) {
            editPath.setVisibility(View.VISIBLE);
            editPath.setEnabled(true);
            onCloseActionPaner.setVisibility(View.VISIBLE);
        }

        View onBack = view.findViewById(R.id.onBack);
        recyclerView = (FastScrollRecyclerView) view.findViewById(R.id.recyclerView);

        paths = (LinearLayout) view.findViewById(R.id.paths);
        scroller = (HorizontalScrollView) view.findViewById(R.id.scroller);
        final View onHome = view.findViewById(R.id.onHome);
        onListGrid = (ImageView) view.findViewById(R.id.onListGrid);
        onListGrid.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                popupMenu(onListGrid);
            }
        });

        searchAdapter = new FileMetaAdapter();
        bindAdapter(searchAdapter);
        bindAuthorsSeriesAdapter(searchAdapter);

        onGridList();

        onHome.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                List<String> extFolders = new ArrayList<String>();

                extFolders = ExtUtils.getExternalStorageDirectories(getActivity());
                String sdPath = ExtUtils.getSDPath();
                if (TxtUtils.isNotEmpty(sdPath) && !extFolders.contains(sdPath)) {
                    extFolders.add(sdPath);
                }

                MyPopupMenu menu = new MyPopupMenu(getActivity(), onHome);

                menu.getMenu().add(R.string.internal_storage).setOnMenuItemClickListener(new OnMenuItemClickListener() {

                    @Override
                    public boolean onMenuItemClick(MenuItem item) {
                        displayAnyPath(Environment.getExternalStorageDirectory().getPath());
                        return false;
                    }
                }).setIcon(R.drawable.glyphicons_146_folder_sd1);

                for (final String info : extFolders) {

                    String name;

                    if (ExtUtils.isExteralSD(info)) {
                        name = ExtUtils.getExtSDDisplayName(getContext(), info);
                    } else {
                        name = new File(info).getName();
                    }

                    menu.getMenu().add(name).setOnMenuItemClickListener(new OnMenuItemClickListener() {

                        @Override
                        public boolean onMenuItemClick(MenuItem item) {
                            displayAnyPath(info);
                            return false;
                        }
                    }).setIcon(R.drawable.glyphicons_146_folder_sd1);

                }

                menu.getMenu().add("Librera").setOnMenuItemClickListener(new OnMenuItemClickListener() {

                    @Override
                    public boolean onMenuItemClick(MenuItem item) {
                        displayAnyPath(new File(AppState.DOWNLOADS_DIR, "Librera").getPath());
                        return false;
                    }
                }).setIcon(R.drawable.glyphicons_591_folder_heart);

                // resources

                if (Build.VERSION.SDK_INT >= 21 && getActivity() instanceof MainTabs2) {
                    List<String> safs = StringDB.asList(AppState.get().pathSAF);

                    for (final String saf : safs) {
                        String fileName = DocumentsContract.getTreeDocumentId(Uri.parse(saf));
                        menu.getMenu().add(fileName).setOnMenuItemClickListener(new OnMenuItemClickListener() {

                            @Override
                            public boolean onMenuItemClick(MenuItem item) {
                                displayAnyPath(saf);
                                return false;
                            }
                        }).setOnMenuItemLongClickListener(new OnMenuItemClickListener() {

                            @Override
                            public boolean onMenuItemClick(MenuItem item) {
                                AppState.get().pathSAF = StringDB.delete(AppState.get().pathSAF, saf);
                                return false;
                            }
                        }).setIcon(R.drawable.glyphicons_146_folder_plus);
                    }
                }

                // stars
                List<FileMeta> starFolders = AppDB.get().getStarsFolder();
                List<String> names = new ArrayList<String>();
                for (FileMeta f : starFolders) {
                    names.add(f.getPath());
                }

                Collections.sort(names, String.CASE_INSENSITIVE_ORDER);

                for (final String info : names) {

                    String name;

                    if (ExtUtils.isExteralSD(info)) {
                        name = ExtUtils.getExtSDDisplayName(getContext(), info);
                    } else {
                        name = new File(info).getName();
                    }

                    menu.getMenu().add(name).setOnMenuItemClickListener(new OnMenuItemClickListener() {

                        @Override
                        public boolean onMenuItemClick(MenuItem item) {
                            displayAnyPath(info);
                            return false;
                        }
                    }).setIcon(R.drawable.glyphicons_591_folder_star);

                }

                if (Build.VERSION.SDK_INT >= 21 && getActivity() instanceof MainTabs2) {
                    menu.getMenu().add(R.string.add_resource).setOnMenuItemClickListener(new OnMenuItemClickListener() {

                        @Override
                        public boolean onMenuItemClick(MenuItem item) {
                            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);

                            intent.addFlags(Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION//
                                    | Intent.FLAG_GRANT_PREFIX_URI_PERMISSION//
                                    | Intent.FLAG_GRANT_READ_URI_PERMISSION//
                                    | Intent.FLAG_GRANT_WRITE_URI_PERMISSION//
                            );

                            getActivity().startActivityForResult(intent, MainTabs2.REQUEST_CODE_ADD_RESOURCE);
                            return true;
                        }
                    }).setIcon(R.drawable.glyphicons_146_add_folder_plus);

                }

                if (AppsConfig.isCloudsEnable) {

                    menu.getMenu().add(R.string.dropbox).active(Clouds.get().isDropbox()).setOnMenuItemClickListener(new OnMenuItemClickListener() {

                        @Override
                        public boolean onMenuItemClick(MenuItem item) {

                            Clouds.get().loginToDropbox(getActivity(), new Runnable() {

                                @Override
                                public void run() {
                                    displayAnyPath(Clouds.PREFIX_CLOUD_DROPBOX + "/");
                                }
                            });

                            return true;
                        }
                    }).setIcon(R.drawable.dropbox);

                    menu.getMenu().add(R.string.google_drive).active(Clouds.get().isGoogleDrive()).setOnMenuItemClickListener(new OnMenuItemClickListener() {

                        @Override
                        public boolean onMenuItemClick(MenuItem item) {
                            Clouds.get().loginToGoogleDrive(getActivity(), new Runnable() {

                                @Override
                                public void run() {
                                    displayAnyPath(Clouds.PREFIX_CLOUD_GDRIVE + "/");
                                }
                            });

                            return true;
                        }
                    }).setIcon(R.drawable.gdrive);

                    menu.getMenu().add(R.string.one_drive).active(Clouds.get().isOneDrive()).setOnMenuItemClickListener(new OnMenuItemClickListener() {

                        @Override
                        public boolean onMenuItemClick(MenuItem item) {
                            Clouds.get().loginToOneDrive(getActivity(), new Runnable() {

                                @Override
                                public void run() {
                                    displayAnyPath(Clouds.PREFIX_CLOUD_ONEDRIVE + "/");
                                }
                            });

                            return true;
                        }
                    }).setIcon(R.drawable.onedrive);
                }

                menu.show();

            }
        });
        onBack.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                onBackAction();
            }
        });

        searchAdapter.setOnItemClickListener(new ResultResponse<FileMeta>() {

            @Override
            public boolean onResultRecive(FileMeta result) {
                if (result.getCusType() != null && result.getCusType() == FileMetaAdapter.DISPLAY_TYPE_DIRECTORY) {
                    displayAnyPath(result.getPath());
                    if (fragmentType == TYPE_SELECT_FOLDER) {
                        editPath.setText(fragmentText);
                    }
                } else {
                    if (fragmentType == TYPE_DEFAULT) {
                        DefaultListeners.getOnItemClickListener(getActivity()).onResultRecive(result);
                    } else if (fragmentType == TYPE_SELECT_FILE) {
                        editPath.setText(ExtUtils.getFileName(result.getPath()));
                    }

                }
                return false;
            }
        });

        searchAdapter.setOnItemLongClickListener(new ResultResponse<FileMeta>() {
            @Override
            public boolean onResultRecive(FileMeta result) {
                if (result.getCusType() != null && result.getCusType() == FileMetaAdapter.DISPLAY_TYPE_DIRECTORY) {
                    // displayAnyPath(result.getPath());
                } else {
                    DefaultListeners.getOnItemLongClickListener(getActivity(), searchAdapter).onResultRecive(result);
                }
                return false;
            }
        });

        onSort.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {

                List<String> names = Arrays.asList(//
                        getActivity().getString(R.string.by_file_name), //
                        getActivity().getString(R.string.by_date), //
                        getActivity().getString(R.string.by_size), //
                        getActivity().getString(R.string.by_title), //
                        getActivity().getString(R.string.by_number_in_serie), //
                        getActivity().getString(R.string.by_number_of_pages), //
                        getActivity().getString(R.string.by_extension) //
                );//

                final List<Integer> ids = Arrays.asList(//
                        AppState.BR_SORT_BY_PATH, //
                        AppState.BR_SORT_BY_DATE, //
                        AppState.BR_SORT_BY_SIZE, //
                        AppState.BR_SORT_BY_TITLE, //
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
                            AppState.get().sortByBrowse = ids.get(j);
                            populate();
                            return false;
                        }
                    });
                }
                menu.show();
            }
        });

        displayAnyPath(getInitPath());
        onTintChanged();

        progressBar = (ProgressBar) view.findViewById(R.id.progressBarBrowse);
        progressBar.setVisibility(View.GONE);
        TintUtil.setDrawableTint(progressBar.getIndeterminateDrawable().getCurrent(), Color.WHITE);

        return view;
    }

    @Override
    public void onReviceOpenDir(String path) {
        displayAnyPath(path);
    }

    OnClickListener onCloseButtonActoin = new OnClickListener() {

        @Override
        public void onClick(View v) {
            if (onCloseAction != null) {
                onCloseAction.onResultRecive("");
            }
        }
    };

    OnClickListener onSelectAction = new OnClickListener() {

        @Override
        public void onClick(View v) {
            if (fragmentType == TYPE_SELECT_FOLDER) {

                if (ExtUtils.isExteralSD(AppState.get().dirLastPath) || new File(AppState.get().dirLastPath).canRead()) {
                    onPositiveAction.onResultRecive(AppState.get().dirLastPath);
                } else {
                    Toast.makeText(getContext(), R.string.incorrect_value, Toast.LENGTH_SHORT).show();
                }
            } else if (fragmentType == TYPE_SELECT_FILE) {
                onPositiveAction.onResultRecive(AppState.get().dirLastPath + "/" + editPath.getText());
            } else if (fragmentType == TYPE_CREATE_FILE) {
                onPositiveAction.onResultRecive(AppState.get().dirLastPath + "/" + editPath.getText());
            }

        }
    };

    public String getInitPath() {
        try {
            String pathArgument = getArguments() != null ? getArguments().getString(EXTRA_INIT_PATH, null) : "";
            if (TxtUtils.isNotEmpty(pathArgument)) {
                return pathArgument;
            }
        } catch (Exception e) {
            LOG.e(e);
        }
        String path = AppState.get().dirLastPath == null ? Environment.getExternalStorageDirectory().getPath() : AppState.get().dirLastPath;
        if (ExtUtils.isExteralSD(path)) {
            return path;
        }
        if (new File(path).isDirectory()) {
            return path;
        }
        return Environment.getExternalStorageDirectory().getPath();
    }

    String displayPath;

    @Override
    public List<FileMeta> prepareDataInBackground() {

        if (displayPath.startsWith(Clouds.PREFIX_CLOUD)) {
            cloudStorage = Clouds.get().cloud(displayPath);
            String cloudPath = Clouds.getPath(displayPath);
            if (TxtUtils.isEmpty(cloudPath)) {
                cloudPath = "/";
            }
            LOG.d("Open clound path", cloudPath);
            List<CloudMetaData> items = cloudStorage.getChildren(cloudPath);

            List<FileMeta> result = new ArrayList<FileMeta>();

            for (CloudMetaData cl : items) {
                String path = cl.getPath();
                String name = cl.getName();
                Long modifiedAt = cl.getModifiedAt();
                long size = cl.getSize();

                LOG.d("CloudMetaData", path, name, modifiedAt, size, cl.getImageMetaData());

                FileMeta meta = new FileMeta(Clouds.getPrefix(displayPath) + path);

                if (cl.getFolder()) {
                    meta.setCusType(FileMetaAdapter.DISPLAY_TYPE_DIRECTORY);
                }

                meta.setTitle(name);
                meta.setPathTxt(name);

                meta.setSize(size);
                meta.setSizeTxt(ExtUtils.readableFileSize(size));

                if (modifiedAt != null) {
                    meta.setDate(modifiedAt);
                    meta.setDateTxt(ExtUtils.getDateFormat(modifiedAt));
                }
                meta.setState(FileMetaCore.STATE_FULL);

                result.add(meta);
            }

            return result;
        }

        if (ExtUtils.isExteralSD(getInitPath())) {

            List<FileMeta> items = new ArrayList<FileMeta>();

            Uri uri = Uri.parse(displayPath);

            ContentResolver contentResolver = getActivity().getContentResolver();
            Uri childrenUri = null;

            childrenUri = ExtUtils.getChildUri(getContext(), uri);

            if (childrenUri != null) {

                LOG.d("newNode uri >> ", uri);
                LOG.d("newNode childrenUri >> ", childrenUri);

                Cursor childCursor = contentResolver.query(childrenUri, new String[] { //
                        Document.COLUMN_DISPLAY_NAME, //
                        Document.COLUMN_DOCUMENT_ID, //
                        Document.COLUMN_ICON, //
                        Document.COLUMN_LAST_MODIFIED, //
                        Document.COLUMN_MIME_TYPE, //
                        Document.COLUMN_SIZE, //
                        Document.COLUMN_SUMMARY, //
                }, //
                        null, null, null); //
                try {
                    while (childCursor.moveToNext()) {
                        String COLUMN_DISPLAY_NAME = childCursor.getString(0);
                        String COLUMN_DOCUMENT_ID = childCursor.getString(1);
                        String COLUMN_ICON = childCursor.getString(2);
                        String COLUMN_LAST_MODIFIED = childCursor.getString(3);
                        String COLUMN_MIME_TYPE = childCursor.getString(4);
                        String COLUMN_SIZE = childCursor.getString(5);
                        String COLUMN_SUMMARY = childCursor.getString(6);

                        LOG.d("found- child 2=", COLUMN_DISPLAY_NAME, COLUMN_DOCUMENT_ID, COLUMN_ICON);

                        FileMeta meta = new FileMeta();
                        meta.setAuthor(SearchFragment2.EMPTY_ID);

                        final Uri newNode = DocumentsContract.buildDocumentUriUsingTree(uri, COLUMN_DOCUMENT_ID);
                        meta.setPath(newNode.toString());
                        LOG.d("newNode", newNode);

                        if (Document.MIME_TYPE_DIR.equals(COLUMN_MIME_TYPE)) {
                            meta.setCusType(FileMetaAdapter.DISPLAY_TYPE_DIRECTORY);
                            meta.setPathTxt(COLUMN_DISPLAY_NAME);
                            meta.setTitle(COLUMN_DISPLAY_NAME);

                        } else {
                            try {
                                if (COLUMN_SIZE != null) {
                                    long size = Long.parseLong(COLUMN_SIZE);
                                    meta.setSize(size);
                                    meta.setSizeTxt(ExtUtils.readableFileSize(size));
                                }
                                if (COLUMN_LAST_MODIFIED != null) {
                                    meta.setDateTxt(ExtUtils.getDateFormat(Long.parseLong(COLUMN_LAST_MODIFIED)));
                                }
                            } catch (Exception e) {
                                LOG.e(e);
                            }
                            meta.setExt(ExtUtils.getFileExtension(COLUMN_DISPLAY_NAME));

                            if (BookType.FB2.is(COLUMN_DISPLAY_NAME)) {
                                meta.setTitle(TxtUtils.encode1251(COLUMN_DISPLAY_NAME));
                            } else {
                                meta.setTitle(COLUMN_DISPLAY_NAME);
                            }

                        }
                        items.add(meta);

                    }
                } finally {
                    closeQuietly(childCursor);
                }
            }
            return items;

        } else {
            return SearchCore.getFilesAndDirs(displayPath, fragmentType == TYPE_DEFAULT);
        }
    }

    @Override
    public void populateDataInUI(List<FileMeta> items) {
        displayItems(items);
        showPathHeader();

    }

    public boolean onBackAction() {

        if (ExtUtils.isExteralSD(AppState.get().dirLastPath)) {
            String path = AppState.get().dirLastPath;
            LOG.d("pathBack before", path);
            if (path.contains("%2F")) {
                path = path.substring(0, path.lastIndexOf("%2F"));
            } else {
                path = path.substring(0, path.lastIndexOf("%3A") + 3);
            }
            LOG.d("pathBack after", path);

            if (path.endsWith("%3A")) {
                displayAnyPath(path);
                return false;
            } else {
                displayAnyPath(path);
                return true;
            }

        } else {
            File file = new File(displayPath);
            String path = file.getParent();

            if (TxtUtils.isEmpty(path)) {
                return false;
            }

            if (TxtUtils.isEmpty(path) || !path.contains("/")) {
                path = Clouds.getPrefix(displayPath) + "/";
            }

            LOG.d("parent", path);

            int pos = rememberPos.get(path) == null ? 0 : rememberPos.get(path);
            displayAnyPath(path);
            recyclerView.scrollToPosition(pos);
            return true;
        }
    }

    public void displayAnyPath(String path) {
        LOG.d("Display-path", path);

        displayPath = path;
        AppState.get().dirLastPath = path;

        populate();
    }

    String prevPath;

    @TargetApi(Build.VERSION_CODES.KITKAT)
    public void closeQuietly(AutoCloseable closeable) {
        if (closeable != null) {
            try {
                closeable.close();
            } catch (RuntimeException rethrown) {
                throw rethrown;
            } catch (Exception ignored) {
            }
        }
    }

    public void displayItems(List<FileMeta> items) {
        if (searchAdapter == null) {
            return;
        }

        // if (!path.equals(prevPath)) {
        // int pos = ((LinearLayoutManager)
        // recyclerView.getLayoutManager()).findFirstVisibleItemPosition();
        // rememberPos.put(prevPath, pos);
        // LOG.d("rememberPos", path, pos);
        // }
        // prevPath = path;

        searchAdapter.clearItems();

        try {
            if (AppState.get().sortByBrowse == AppState.BR_SORT_BY_PATH) {
                Collections.sort(items, FileMetaComparators.BY_PATH);
            } else if (AppState.get().sortByBrowse == AppState.BR_SORT_BY_DATE) {
                Collections.sort(items, FileMetaComparators.BY_DATE);
            } else if (AppState.get().sortByBrowse == AppState.BR_SORT_BY_SIZE) {
                Collections.sort(items, FileMetaComparators.BY_SIZE);
            } else if (AppState.get().sortByBrowse == AppState.BR_SORT_BY_NUMBER) {
                Collections.sort(items, FileMetaComparators.BR_BY_NUMBER1);
            } else if (AppState.get().sortByBrowse == AppState.BR_SORT_BY_PAGES) {
                Collections.sort(items, FileMetaComparators.BR_BY_PAGES);
            } else if (AppState.get().sortByBrowse == AppState.BR_SORT_BY_TITLE) {
                Collections.sort(items, FileMetaComparators.BR_BY_TITLE);
            } else if (AppState.get().sortByBrowse == AppState.BR_SORT_BY_EXT) {
                Collections.sort(items, FileMetaComparators.BR_BY_EXT);
            }
            if (AppState.get().sortByReverse) {
                Collections.reverse(items);
            }
            Collections.sort(items, FileMetaComparators.DIRS);

        } catch (Exception e) {
            LOG.e(e);
        }

        for (int i = 0; i < items.size(); i++) {
            FileMeta m = items.get(i);
            if (m.getCusType() == null) {// directory
                LOG.d("DISPALY_TYPE_LAYOUT_TITLE_FOLDERS", i);
                FileMeta it = new FileMeta();
                it.setCusType(FileMetaAdapter.DISPALY_TYPE_LAYOUT_TITLE_NONE);
                items.add(i, it);
                break;
            }
        }

        searchAdapter.getItemsList().addAll(items);
        recyclerView.setAdapter(searchAdapter);

        scroller.postDelayed(new Runnable() {

            @Override
            public void run() {
                scroller.fullScroll(HorizontalScrollView.FOCUS_RIGHT);
            }

        }, 100);

    }

    public void showPathHeader() {
        paths.removeAllViews();

        if (ExtUtils.isExteralSD(displayPath)) {
            String id = ExtUtils.getExtSDDisplayName(getContext(), displayPath);

            TextView slash = new TextView(getActivity());
            slash.setText(id);
            slash.setTextColor(getResources().getColor(R.color.white));
            paths.addView(slash);
        } else {

            final String prefix = Clouds.getPrefix(displayPath);
            final String path = Clouds.getPath(displayPath);
            final String name = Clouds.getPrefixName(displayPath);

            final String[] split = path.split("/");

            TextView nameView = new TextView(getActivity());
            if (split.length == 0) {
                nameView.setText(name + ": " + Clouds.get().getUserLogin(displayPath) + " ");
            } else {
                nameView.setText(name + ":");
            }
            nameView.setTextColor(getResources().getColor(R.color.white));
            nameView.setOnClickListener(new OnClickListener() {

                @Override
                public void onClick(View v) {
                    displayAnyPath(prefix + "/");
                }
            });
            paths.addView(nameView);

            if (split.length == 0 && Clouds.isCloud(displayPath)) {
                TextView logout = new TextView(getActivity());
                logout.setText(TxtUtils.underline(getActivity().getString(R.string.logout)));
                logout.setTextColor(getResources().getColor(R.color.white));
                logout.setOnClickListener(new OnClickListener() {

                    @Override
                    public void onClick(View v) {
                        new AsyncProgressTask<Boolean>() {

                            @Override
                            public Context getContext() {
                                return getActivity();
                            }

                            @Override
                            protected Boolean doInBackground(Object... params) {
                                try {
                                    Clouds.get().logout(displayPath);
                                } catch (Exception e) {
                                    return false;
                                }
                                return true;
                            }

                            @Override
                            protected void onPostExecute(Boolean result) {
                                super.onPostExecute(result);
                                displayAnyPath(Environment.getExternalStorageDirectory().getPath());
                            }

                        }.execute();
                    }
                });
                paths.addView(logout);
            }

            for (int i = 0; i < split.length; i++) {
                final int index = i;
                String part = split[i];
                if (TxtUtils.isEmpty(part)) {
                    continue;
                }
                TextView slash = new TextView(getActivity());
                slash.setText(" / ");
                slash.setTextColor(getResources().getColor(R.color.white));

                TextView item = new TextView(getActivity());
                item.setText(part);
                item.setGravity(Gravity.CENTER);
                item.setTextColor(getResources().getColor(R.color.white));
                item.setSingleLine();
                TypedValue outValue = new TypedValue();
                getContext().getTheme().resolveAttribute(android.R.attr.selectableItemBackground, outValue, true);
                item.setBackgroundResource(outValue.resourceId);

                if (i == split.length - 1) {
                    item.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
                }

                item.setOnClickListener(new OnClickListener() {

                    @Override
                    public void onClick(View v) {
                        StringBuilder builder = new StringBuilder();
                        for (int j = 0; j <= index; j++) {
                            builder.append("/");
                            builder.append(split[j]);
                        }
                        String itemPath = builder.toString();
                        String pathFull = prefix + itemPath;
                        pathFull = pathFull.replace("://", ":/");
                        displayAnyPath(pathFull);
                    }
                });

                paths.addView(slash);
                paths.addView(item, new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.MATCH_PARENT));
            }

            TextView stub = new TextView(getActivity());
            stub.setText("    ");
            paths.addView(stub);

        }

        if (AppDB.get().isStarFolder(displayPath)) {
            starIcon.setImageResource(R.drawable.star_1);
        } else {
            starIcon.setImageResource(R.drawable.star_2);
        }
        TintUtil.setTintImageWithAlpha(starIcon, Color.WHITE);

        starIcon.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                FileMeta fileMeta = AppDB.get().getOrCreate(displayPath);
                fileMeta.setCusType(FileMetaAdapter.DISPLAY_TYPE_DIRECTORY);
                fileMeta.setPathTxt(ExtUtils.getFileName(displayPath));
                DefaultListeners.getOnStarClick(getActivity()).onResultRecive(fileMeta, null);
                if (AppDB.get().isStarFolder(displayPath)) {
                    starIcon.setImageResource(R.drawable.star_1);
                } else {
                    starIcon.setImageResource(R.drawable.star_2);
                }
            }
        });

    }

    public void onGridList() {
        onGridList(AppState.get().broseMode, onListGrid, searchAdapter, null);

    }

    private void popupMenu(final ImageView onGridList) {
        MyPopupMenu p = new MyPopupMenu(getActivity(), onGridList);
        PopupHelper.addPROIcon(p, getActivity());

        List<Integer> names = Arrays.asList(R.string.list, R.string.compact, R.string.grid, R.string.cover);
        final List<Integer> icons = Arrays.asList(R.drawable.glyphicons_114_justify, R.drawable.glyphicons_114_justify_compact, R.drawable.glyphicons_156_show_big_thumbnails, R.drawable.glyphicons_157_show_thumbnails);
        final List<Integer> actions = Arrays.asList(AppState.MODE_LIST, AppState.MODE_LIST_COMPACT, AppState.MODE_GRID, AppState.MODE_COVERS);

        for (int i = 0; i < names.size(); i++) {
            final int index = i;
            p.getMenu().add(names.get(i)).setIcon(icons.get(i)).setOnMenuItemClickListener(new OnMenuItemClickListener() {

                @Override
                public boolean onMenuItemClick(MenuItem item) {
                    AppState.get().broseMode = actions.get(index);
                    onGridList.setImageResource(icons.get(index));
                    onGridList();
                    return false;
                }
            });
        }

        p.show();

    }

    @Override
    public boolean isBackPressed() {
        return onBackAction();
    }

    public void setOnPositiveAction(ResultResponse<String> onPositiveAction) {
        this.onPositiveAction = onPositiveAction;
    }

    public void setOnCloseAction(ResultResponse<String> onCloseAction) {
        this.onCloseAction = onCloseAction;
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
        populate();
    }

}
