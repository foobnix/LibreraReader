package com.foobnix.work;

import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.work.ExistingWorkPolicy;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;
import androidx.work.WorkerParameters;

import com.foobnix.android.utils.JsonDB;
import com.foobnix.android.utils.LOG;
import com.foobnix.android.utils.TxtUtils;
import com.foobnix.dao2.FileMeta;
import com.foobnix.ext.CacheZipUtils;
import com.foobnix.ext.EbookMeta;
import com.foobnix.model.AppData;
import com.foobnix.model.AppProfile;
import com.foobnix.model.AppState;
import com.foobnix.model.SimpleMeta;
import com.foobnix.model.TagData;
import com.foobnix.pdf.info.Clouds;
import com.foobnix.pdf.info.ExtUtils;
import com.foobnix.pdf.info.IMG;
import com.foobnix.pdf.info.Prefs;
import com.foobnix.pdf.info.io.SearchCore;
import com.foobnix.pdf.info.model.BookCSS;
import com.foobnix.sys.ImageExtractor;
import com.foobnix.ui2.AppDB;
import com.foobnix.ui2.BooksService;
import com.foobnix.ui2.FileMetaCore;

import org.ebookdroid.common.settings.books.SharedBooks;

import java.io.File;
import java.util.LinkedList;
import java.util.List;


public class SearchAllBooksWorker extends MessageWorker {
    public static final String SEARCH_ERRORS = "search_errors";
    Handler handler;
    List<FileMeta> itemsMeta;

    public SearchAllBooksWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
        handler = new Handler(Looper.getMainLooper());

    }

    public static void run(Context context) {
        OneTimeWorkRequest workRequest = new OneTimeWorkRequest
                .Builder(SearchAllBooksWorker.class).build();

        WorkManager.getInstance(context)
                .enqueueUniqueWork("search", ExistingWorkPolicy.KEEP, workRequest);
    }

    public static void sendFinishMessage(Context c) {
        Intent intent = new Intent(BooksService.INTENT_NAME).putExtra(Intent.EXTRA_TEXT, BooksService.RESULT_SEARCH_FINISH);
        LocalBroadcastManager.getInstance(c).sendBroadcast(intent);
    }

    public void doWorkInner() {


        Prefs.get().put(SEARCH_ERRORS, 0);

        itemsMeta = new LinkedList<FileMeta>();

        AppProfile.init(getApplicationContext());

        ImageExtractor.clearErrors();
        IMG.clearDiscCache();

        handler.post(new Runnable() {
            @Override
            public void run() {
                IMG.clearMemoryCache();
            }
        });


        AppDB.get().deleteAllData();


        itemsMeta.clear();

        handler.post(timer);


        for (final String path : JsonDB.get(BookCSS.get().searchPathsJson)) {
            if (path != null && path.trim().length() > 0) {
                final File root = new File(path);
                if (root.isDirectory()) {
                    LOG.d("Search in: " + root.getPath());
                    SearchCore.search(itemsMeta, root, ExtUtils.seachExts);
                }
            }
        }


        for (FileMeta meta : itemsMeta) {
            meta.setIsSearchBook(true);
        }

        final List<SimpleMeta> allExcluded = AppData.get().getAllExcluded();

        if (TxtUtils.isListNotEmpty(allExcluded)) {
            for (FileMeta meta : itemsMeta) {
                if (allExcluded.contains(SimpleMeta.SyncSimpleMeta(meta.getPath()))) {
                    meta.setIsSearchBook(false);
                }
            }
        }

        final List<FileMeta> allSyncBooks = AppData.get().getAllSyncBooks();
        if (TxtUtils.isListNotEmpty(allSyncBooks)) {
            for (FileMeta meta : itemsMeta) {
                for (FileMeta sync : allSyncBooks) {
                    if (meta.getTitle().equals(sync.getTitle()) && !meta.getPath().equals(sync.getPath())) {
                        meta.setIsSearchBook(false);
                        LOG.d("Worker", "remove-dublicate", meta.getPath());
                    }
                }

            }
        }


        itemsMeta.addAll(AppData.get().getAllFavoriteFiles(false));
        itemsMeta.addAll(AppData.get().getAllFavoriteFolders());


        AppDB.get().saveAll(itemsMeta);

        handler.removeCallbacks(timer);

        sendFinishMessage();

        handler.post(timer2);

        for (FileMeta meta : itemsMeta) {
            File file = new File(meta.getPath());
            FileMetaCore.get().upadteBasicMeta(meta, file);
        }

        AppDB.get().updateAll(itemsMeta);
        sendFinishMessage();


        for (FileMeta meta : itemsMeta) {
            //if(FileMetaCore.isSafeToExtactBook(meta.getPath())) {
            EbookMeta ebookMeta = FileMetaCore.get().getEbookMeta(meta.getPath(), CacheZipUtils.CacheDir.ZipService, true);
            FileMetaCore.get().udpateFullMeta(meta, ebookMeta);
            //}
        }

        SharedBooks.updateProgress(itemsMeta, true, -1);
        AppDB.get().updateAll(itemsMeta);


        itemsMeta.clear();

        handler.removeCallbacks(timer2);
        sendFinishMessage();
        CacheZipUtils.CacheDir.ZipService.removeCacheContent();

        Clouds.get().syncronizeGet();

        TagData.restoreTags();


        List<FileMeta> allNone = AppDB.get().getAllByState(FileMetaCore.STATE_NONE);
        for (FileMeta m : allNone) {
            LOG.d("BooksService-createMetaIfNeedSafe-service", m.getTitle(), m.getPath(), m.getTitle());
            FileMetaCore.createMetaIfNeedSafe(m.getPath(), false);
        }

        updateBookAnnotations();



        Prefs.get().remove(SEARCH_ERRORS, 0);


    }

    public void updateBookAnnotations() {

        if (AppState.get().isDisplayAnnotation) {
            sendBuildingLibrary();
            LOG.d("updateBookAnnotations begin");
            List<FileMeta> itemsMeta = AppDB.get().getAll();
            for (FileMeta meta : itemsMeta) {
                if (TxtUtils.isEmpty(meta.getAnnotation())) {
                    String bookOverview = FileMetaCore.getBookOverview(meta.getPath());
                    meta.setAnnotation(bookOverview);
                }
            }
            AppDB.get().updateAll(itemsMeta);
            sendFinishMessage();
            LOG.d("updateBookAnnotations end");
        }

    }

    Runnable timer = new Runnable() {

        @Override
        public void run() {
            LOG.d("timer 2");
            sendProggressMessage(itemsMeta);
            handler.postDelayed(timer, 250);
        }
    };


    Runnable timer2 = new Runnable() {

        @Override
        public void run() {
            LOG.d("timer2");
            sendBuildingLibrary();
            handler.postDelayed(timer2, 250);
        }
    };


}
