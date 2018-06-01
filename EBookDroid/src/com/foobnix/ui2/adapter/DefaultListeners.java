package com.foobnix.ui2.adapter;

import java.io.File;

import org.greenrobot.eventbus.EventBus;

import com.foobnix.android.utils.LOG;
import com.foobnix.android.utils.ResultResponse;
import com.foobnix.android.utils.ResultResponse2;
import com.foobnix.dao2.FileMeta;
import com.foobnix.pdf.info.ADS;
import com.foobnix.pdf.info.Clouds;
import com.foobnix.pdf.info.ExtUtils;
import com.foobnix.pdf.info.IMG;
import com.foobnix.pdf.info.R;
import com.foobnix.pdf.info.view.Downloader;
import com.foobnix.pdf.info.widget.FileInformationDialog;
import com.foobnix.pdf.info.widget.RecentUpates;
import com.foobnix.pdf.info.widget.ShareDialog;
import com.foobnix.pdf.info.wrapper.DocumentController;
import com.foobnix.pdf.info.wrapper.UITab;
import com.foobnix.pdf.search.activity.msg.OpenDirMessage;
import com.foobnix.pdf.search.activity.msg.OpenTagMessage;
import com.foobnix.pdf.search.view.AsyncProgressTask;
import com.foobnix.sys.TempHolder;
import com.foobnix.ui2.AppDB;
import com.foobnix.ui2.MainTabs2;
import com.foobnix.ui2.fragment.UIFragment;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
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
            public boolean onResultRecive(final FileMeta result) {
                if (isTagCicked(a, result)) {
                    return true;
                }
                boolean isFolder = AppDB.get().isFolder(result);

                if (!isFolder && result.getPath().startsWith(Clouds.PREFIX_CLOUD)) {

                    Downloader.openOrDownload(a, result, new Runnable() {

                        @Override
                        public void run() {
                            IMG.clearCache(result.getPath());

                            Intent intent = new Intent(UIFragment.INTENT_TINT_CHANGE)//
                                    .putExtra(MainTabs2.EXTRA_NOTIFY_REFRESH, true)//
                                    .putExtra(MainTabs2.EXTRA_PAGE_NUMBER, -2);//
                            LocalBroadcastManager.getInstance(a).sendBroadcast(intent);
                        }
                    });

                    return false;
                }

                // final File item = new File(result.getPath());
                if (isFolder) {
                    Intent intent = new Intent(UIFragment.INTENT_TINT_CHANGE)//

                            .putExtra(MainTabs2.EXTRA_PAGE_NUMBER, UITab.getCurrentTabIndex(UITab.BrowseFragment));//
                    LocalBroadcastManager.getInstance(a).sendBroadcast(intent);

                    EventBus.getDefault().post(new OpenDirMessage(result.getPath()));

                } else {
                    ExtUtils.openFile(a, result);
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
                if (ExtUtils.isExteralSD(result.getPath())) {
                    return false;
                }

                if (isTagCicked(a, result)) {
                    return true;
                }

                File file = new File(result.getPath());

                if (Clouds.isCloud(file.getPath()) && Clouds.isCacheFileExist(file.getPath())) {
                    file = Clouds.getCacheFile(file.getPath());
                }

                if (file.isDirectory()) {
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
                if (ExtUtils.doifFileExists(a, file)) {
                    FileInformationDialog.showFileInfoDialog(a, file, onDeleteAction);
                }
                return true;
            }
        };
    };

    @SuppressLint("NewApi")
    private static void deleteFile(final Activity a, final FileMetaAdapter searchAdapter, final FileMeta result) {
        boolean delete = false;
        if (ExtUtils.isExteralSD(result.getPath())) {
            DocumentFile doc = DocumentFile.fromSingleUri(a, Uri.parse(result.getPath()));
            delete = doc.delete();
        } else if (Clouds.isCloud(result.getPath())) {
            removeCloudFile(a, searchAdapter, result);
        } else {
            final File file = new File(result.getPath());
            delete = file.delete();
        }

        LOG.d("Delete-file", result.getPath(), delete);

        if (delete) {
            TempHolder.listHash++;
            AppDB.get().delete(result);
            searchAdapter.getItemsList().remove(result);
            searchAdapter.notifyDataSetChanged();

        } else {
            if (!Clouds.isCloud(result.getPath())) {
                Toast.makeText(a, R.string.can_t_delete_file, Toast.LENGTH_LONG).show();
            }
        }
    }

    private static void removeSyncFile(final Activity a, final FileMetaAdapter searchAdapter, final FileMeta result) {
        new AsyncProgressTask<Boolean>() {

            @Override
            public Context getContext() {
                return a;
            }

            @Override
            protected Boolean doInBackground(Object... params) {
                try {
                    String cloudFile = Clouds.LIBRERA_SYNC_ONLINE_FOLDER + "/" + ExtUtils.getFileName(result.getPath());
                    LOG.d("delete dropbox", cloudFile);
                    if (!Clouds.get().dropbox.exists(cloudFile)) {
                        return true;
                    }
                    Clouds.get().dropbox.delete(cloudFile);
                } catch (Exception e) {
                    LOG.e(e);
                    return false;
                }
                return true;
            }

            @Override
            protected void onPostExecute(Boolean status) {
                super.onPostExecute(status);
                LOG.d("Delete status", status);
                if (status != null && status == true) {

                    TempHolder.listHash++;
                    AppDB.get().delete(result);
                    searchAdapter.getItemsList().remove(result);
                    searchAdapter.notifyDataSetChanged();

                    new File(result.getPath()).delete();

                    File cacheFile = Clouds.getCacheFile(result.getPath());
                    if (cacheFile != null && cacheFile.delete()) {

                        FileMeta cacheMeta = new FileMeta(cacheFile.getPath());
                        AppDB.get().delete(cacheMeta);
                        searchAdapter.getItemsList().remove(cacheMeta);
                    }

                } else {
                    Toast.makeText(a, R.string.can_t_delete_file, Toast.LENGTH_LONG).show();
                }
            };

        }.execute();
    }

    private static void removeCloudFile(final Activity a, final FileMetaAdapter searchAdapter, final FileMeta result) {
        new AsyncProgressTask<Boolean>() {

            @Override
            public Context getContext() {
                return a;
            }

            @Override
            protected Boolean doInBackground(Object... params) {
                try {
                    String path = Clouds.getPath(result.getPath());
                    LOG.d("Delete file", result.getPath(), path);
                    Clouds.get().cloud(result.getPath()).delete(path);
                } catch (Exception e) {
                    LOG.e(e);
                    return false;
                }
                return true;
            }

            @Override
            protected void onPostExecute(Boolean status) {
                super.onPostExecute(status);
                LOG.d("Delete status", status);
                if (status != null && status == true) {

                    TempHolder.listHash++;
                    AppDB.get().delete(result);
                    searchAdapter.getItemsList().remove(result);
                    searchAdapter.notifyDataSetChanged();

                    File cacheFile = Clouds.getCacheFile(result.getPath());


                    if (cacheFile != null && cacheFile.delete()) {

                        FileMeta cacheMeta = new FileMeta(cacheFile.getPath());
                        AppDB.get().delete(cacheMeta);
                        searchAdapter.getItemsList().remove(cacheMeta);
                    }

                    Clouds.runSync(a);
                } else {
                    Toast.makeText(a, R.string.can_t_delete_file, Toast.LENGTH_LONG).show();
                }
            };

        }.execute();
    }

    public static ResultResponse<FileMeta> getOnMenuClick(final Activity a, final FileMetaAdapter searchAdapter) {
        return new ResultResponse<FileMeta>() {

            @Override
            public boolean onResultRecive(final FileMeta result) {
                ADS.hideAdsTemp(a);

                File file = new File(result.getPath());

                if (Clouds.isCloud(file.getPath()) && Clouds.isCacheFileExist(file.getPath())) {
                    file = Clouds.getCacheFile(file.getPath());
                }
               

                Runnable onDeleteAction = new Runnable() {

                    @Override
                    public void run() {
                        deleteFile(a, searchAdapter, result);
                    }

                };

                if (ExtUtils.isExteralSD(result.getPath())) {
                    ShareDialog.show(a, file, onDeleteAction, -1, null, null);
                } else {

                    if (ExtUtils.doifFileExists(a, result.getPath())) {

                        if (ExtUtils.isNotSupportedFile(file)) {
                            ShareDialog.showArchive(a, file, onDeleteAction);
                        } else {
                            ShareDialog.show(a, file, onDeleteAction, -1, null, null);
                        }
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
