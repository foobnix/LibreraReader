package com.foobnix.ui2.adapter;

import java.io.File;
import java.io.InputStream;

import org.greenrobot.eventbus.EventBus;

import com.foobnix.android.utils.LOG;
import com.foobnix.android.utils.ResultResponse;
import com.foobnix.android.utils.ResultResponse2;
import com.foobnix.dao2.FileMeta;
import com.foobnix.ext.CacheZipUtils;
import com.foobnix.pdf.info.ExtUtils;
import com.foobnix.pdf.info.R;
import com.foobnix.pdf.info.widget.FileInformationDialog;
import com.foobnix.pdf.info.widget.RecentUpates;
import com.foobnix.pdf.info.widget.ShareDialog;
import com.foobnix.pdf.info.wrapper.DocumentController;
import com.foobnix.pdf.info.wrapper.UITab;
import com.foobnix.pdf.search.activity.msg.OpenDirMessage;
import com.foobnix.pdf.search.activity.msg.OpenTagMessage;
import com.foobnix.sys.TempHolder;
import com.foobnix.ui2.AppDB;
import com.foobnix.ui2.MainTabs2;
import com.foobnix.ui2.fragment.UIFragment;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.provider.DocumentFile;
import android.widget.Toast;

public class DefaultListeners {

    public static void bindAdapter(final Activity a, final FileMetaAdapter searchAdapter, final DocumentController dc, final Runnable onClick) {
        searchAdapter.setOnItemClickListener(new ResultResponse<FileMeta>() {

            @Override
            public boolean onResultRecive(final FileMeta result) {
                onClick.run();
                dc.onCloseActivityFinal(new Runnable() {

                    @Override
                    public void run() {
                        ExtUtils.showDocumentWithoutDialog(a, new File(result.getPath()), -1);

                    }
                });
                return false;
            }

        });
        searchAdapter.setOnItemLongClickListener(getOnItemLongClickListener(a, searchAdapter));
        searchAdapter.setOnMenuClickListener(getOnMenuClick(a, searchAdapter));
        searchAdapter.setOnStarClickListener(getOnStarClick(a));
    }

    public static void bindAdapter(final Activity a, final FileMetaAdapter searchAdapter) {
        searchAdapter.setOnItemClickListener(getOnItemClickListener(a));
        searchAdapter.setOnItemLongClickListener(getOnItemLongClickListener(a, searchAdapter));
        searchAdapter.setOnMenuClickListener(getOnMenuClick(a, searchAdapter));
        searchAdapter.setOnStarClickListener(getOnStarClick(a));
        searchAdapter.setOnTagClickListner(new ResultResponse<String>() {

            @Override
            public boolean onResultRecive(String result) {
                showBooksByTag(a, result);
                return false;
            }

        });
    }

    private static void showBooksByTag(final Activity a, String result) {
        Intent intent = new Intent(UIFragment.INTENT_TINT_CHANGE)//
                .putExtra(MainTabs2.EXTRA_PAGE_NUMBER, UITab.getCurrentTabIndex(UITab.SearchFragment));//
        LocalBroadcastManager.getInstance(a).sendBroadcast(intent);

        EventBus.getDefault().post(new OpenTagMessage(result));
    }

    public static void bindAdapterAuthorSerias(Activity a, final FileMetaAdapter searchAdapter) {
        searchAdapter.setOnAuthorClickListener(getOnAuthorClickListener(a));
        searchAdapter.setOnSeriesClickListener(getOnSeriesClickListener(a));

    }

    public static ResultResponse<FileMeta> getOnItemClickListener(final Activity a) {
        return new ResultResponse<FileMeta>() {

            @Override
            public boolean onResultRecive(FileMeta result) {
                if (isTagCicked(a, result)) {
                    return true;
                }

                String path = result.getPath();
                if (ExtUtils.isExteralSD(path)) {
                    try {
                        Uri uri = Uri.parse(path);

                        File cacheFile = new File(CacheZipUtils.ATTACHMENTS_CACHE_DIR, result.getTitle());
                        if (cacheFile.exists()) {
                            ExtUtils.openFile(a, cacheFile);
                            return true;
                        }

                        LOG.d("Copy-file", path);
                        InputStream inputStream = a.getContentResolver().openInputStream(uri);
                        if (inputStream == null) {
                            Toast.makeText(a, R.string.incorrect_value, Toast.LENGTH_SHORT).show();
                            return false;
                        }
                        CacheZipUtils.copyFile(inputStream, cacheFile);
                        LOG.d("Create-file", cacheFile.getPath(), cacheFile.length());

                        ExtUtils.openFile(a, cacheFile);
                    } catch (Exception e) {
                        LOG.e(e);
                    }

                    return true;
                }

                final File item = new File(path);
                if (item.isDirectory()) {
                    Intent intent = new Intent(UIFragment.INTENT_TINT_CHANGE)//
                            .putExtra(MainTabs2.EXTRA_PAGE_NUMBER, UITab.getCurrentTabIndex(UITab.BrowseFragment));//
                    LocalBroadcastManager.getInstance(a).sendBroadcast(intent);

                    EventBus.getDefault().post(new OpenDirMessage(result.getPath()));

                } else {
                    ExtUtils.openFile(a, item);
                }
                return false;
            }

        };
    };

    private static boolean isTagCicked(final Activity a, FileMeta result) {
        if (result.getCusType() != null && result.getCusType() == FileMetaAdapter.DISPALY_TYPE_LAYOUT_TAG) {
            showBooksByTag(a, result.getPathTxt());
            return true;
        }
        return false;
    }

    public static ResultResponse<FileMeta> getOnItemLongClickListener(final Activity a, final FileMetaAdapter searchAdapter) {
        return new ResultResponse<FileMeta>() {

            @Override
            public boolean onResultRecive(final FileMeta result) {
                if (isTagCicked(a, result)) {
                    return true;
                }

                File item = new File(result.getPath());

                if (item.isDirectory()) {
                    Intent intent = new Intent(UIFragment.INTENT_TINT_CHANGE)//
                            .putExtra(MainTabs2.EXTRA_PAGE_NUMBER, UITab.getCurrentTabIndex(UITab.BrowseFragment));//
                    LocalBroadcastManager.getInstance(a).sendBroadcast(intent);

                    EventBus.getDefault().post(new OpenDirMessage(result.getPath()));
                    return true;
                }

                Runnable onDeleteAction = new Runnable() {

                    @Override
                    public void run() {
                        deleteFile(a, searchAdapter, result);
                    }

                };
                if (ExtUtils.doifFileExists(a, item)) {
                    FileInformationDialog.showFileInfoDialog(a, item, onDeleteAction);
                }
                return true;
            }
        };
    };

    @SuppressLint("NewApi")
    private static void deleteFile(Activity a, final FileMetaAdapter searchAdapter, final FileMeta result) {

        if (ExtUtils.isExteralSD(result.getPath())) {
            DocumentFile doc = DocumentFile.fromSingleUri(a, Uri.parse(result.getPath()));
            boolean delete = doc.delete();
            if (!delete) {
                Toast.makeText(a, R.string.can_t_delete_file, Toast.LENGTH_LONG).show();
            }
            return;
        }

        final File file = new File(result.getPath());

        boolean delete = file.delete();

        LOG.d("Delete-file", file.getPath(), delete, ExtUtils.getUriProvider(a, file));

        if (delete) {
            TempHolder.listHash++;
            AppDB.get().delete(result);
            searchAdapter.getItemsList().remove(result);
            searchAdapter.notifyDataSetChanged();

        } else {
            Toast.makeText(a, R.string.can_t_delete_file, Toast.LENGTH_LONG).show();
        }
    }

    public static ResultResponse<FileMeta> getOnMenuClick(final Activity a, final FileMetaAdapter searchAdapter) {
        return new ResultResponse<FileMeta>() {

            @Override
            public boolean onResultRecive(final FileMeta result) {

                final File file = new File(result.getPath());


                if (ExtUtils.isExteralSD(result.getPath()) || ExtUtils.doifFileExists(a, file)) {

                    Runnable onDeleteAction = new Runnable() {

                        @Override
                        public void run() {
                            deleteFile(a, searchAdapter, result);
                        }

                    };

                    if (ExtUtils.isNotSupportedFile(file)) {
                        ShareDialog.showArchive(a, file, onDeleteAction);
                    } else {
                        ShareDialog.show(a, file, onDeleteAction, -1, null, null);
                    }
                }

                return false;
            }
        };
    };

    public static ResultResponse<String> getOnAuthorClickListener(final Activity a) {
        return new ResultResponse<String>() {

            @Override
            public boolean onResultRecive(String result) {

                result = AppDB.SEARCH_IN.AUTHOR.getDotPrefix() + " " + result;

                Intent intent = new Intent(UIFragment.INTENT_TINT_CHANGE)//
                        .putExtra(MainTabs2.EXTRA_SEACH_TEXT, result)//
                        .putExtra(MainTabs2.EXTRA_PAGE_NUMBER, UITab.getCurrentTabIndex(UITab.SearchFragment));//

                LocalBroadcastManager.getInstance(a).sendBroadcast(intent);
                return false;
            }
        };
    }

    public static ResultResponse<String> getOnSeriesClickListener(final Activity a) {
        return new ResultResponse<String>() {

            @Override
            public boolean onResultRecive(String result) {

                result = AppDB.SEARCH_IN.SERIES.getDotPrefix() + " " + result;

                Intent intent = new Intent(UIFragment.INTENT_TINT_CHANGE)//
                        .putExtra(MainTabs2.EXTRA_SEACH_TEXT, result)//
                        .putExtra(MainTabs2.EXTRA_PAGE_NUMBER, UITab.getCurrentTabIndex(UITab.SearchFragment));//

                LocalBroadcastManager.getInstance(a).sendBroadcast(intent);
                return false;
            }
        };
    }

    public static ResultResponse2<FileMeta, FileMetaAdapter> getOnStarClick(final Activity a) {
        return new ResultResponse2<FileMeta, FileMetaAdapter>() {

            @Override
            public boolean onResultRecive(FileMeta fileMeta, FileMetaAdapter adapter) {
                Boolean isStar = fileMeta.getIsStar();
                if (isStar == null) {
                    isStar = false;
                }
                fileMeta.setIsStar(!isStar);
                fileMeta.setIsStarTime(System.currentTimeMillis());
                AppDB.get().updateOrSave(fileMeta);

                if (adapter != null) {
                    adapter.notifyDataSetChanged();
                }
                TempHolder.listHash++;

                RecentUpates.updateAll(a);
                return false;
            }
        };
    };

}
