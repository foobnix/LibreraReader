package com.foobnix.work;

import static com.foobnix.pdf.info.AppsConfig.SEARCH_FRAGMENT_WORKER_NAME;
import static com.foobnix.pdf.info.AppsConfig.WORKER_POLICY;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.core.util.Pair;
import androidx.work.Data;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;
import androidx.work.WorkerParameters;

import com.foobnix.android.utils.JsonDB;
import com.foobnix.android.utils.LOG;
import com.foobnix.android.utils.TxtUtils;
import com.foobnix.dao2.FileMeta;
import com.foobnix.ext.CacheZipUtils;
import com.foobnix.ext.EbookMeta;
import com.foobnix.mobi.parser.IOUtils;
import com.foobnix.model.AppData;
import com.foobnix.model.AppProfile;
import com.foobnix.model.AppSP;
import com.foobnix.model.AppState;
import com.foobnix.model.SimpleMeta;
import com.foobnix.model.Tags2;
import com.foobnix.pdf.info.Clouds;
import com.foobnix.pdf.info.ExtUtils;
import com.foobnix.pdf.info.IMG;
import com.foobnix.pdf.info.Prefs;
import com.foobnix.pdf.info.io.SearchCore;
import com.foobnix.pdf.info.model.BookCSS;
import com.foobnix.sys.ImageExtractor;
import com.foobnix.sys.TempHolder;
import com.foobnix.ui2.AppDB;
import com.foobnix.ui2.FileMetaCore;

import org.ebookdroid.BookType;
import org.ebookdroid.common.settings.books.SharedBooks;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletionService;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;


public class SearchAllBooksWorker extends MessageWorker {

    Handler handler;
    List<FileMeta> itemsMeta;
    private volatile int metaDone;
    private volatile int metaTotal;

    /** Books read since the last save: saved in batches, so a killed scan keeps what it read. */
    private static final int SAVE_EVERY_BOOKS = 50;
    private static final long SAVE_EVERY_MS = 2000;
    private static final String KEY_RESUME = "resume";

    public SearchAllBooksWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
        handler = new Handler(Looper.getMainLooper());

    }

    /** A scan was started in this process: what the resume file says is about that one. */
    private static volatile boolean startedInThisProcess;

    public static void run(Context context) {
        startedInThisProcess = true;

        OneTimeWorkRequest workRequest = new OneTimeWorkRequest
                .Builder(SearchAllBooksWorker.class).build();

        WorkManager.getInstance(context)
                .enqueueUniqueWork(SEARCH_FRAGMENT_WORKER_NAME, WORKER_POLICY, workRequest);
    }


    /**
     * The last scan of this profile, started before the app was last killed, did not finish. Not
     * the "search was not complete" flag: when the system stops the work before it kills the app -
     * the app swiped away on Samsung - the scan ends as cancelled and clears that flag.
     */
    public static boolean canResume(Context context) {
        return !startedInThisProcess && LibraryScanResume.read(context).isFor(AppProfile.getCurrent());
    }

    /** Carries on with the stopped scan: the books it had read are not read again. */
    public static void resume(Context context) {
        startedInThisProcess = true;
        OneTimeWorkRequest workRequest = new OneTimeWorkRequest.Builder(SearchAllBooksWorker.class)
                .setInputData(new Data.Builder().putBoolean(KEY_RESUME, true).build())
                .build();
        WorkManager.getInstance(context).enqueueUniqueWork(SEARCH_FRAGMENT_WORKER_NAME, WORKER_POLICY, workRequest);
    }

    public boolean doWorkInner() {
        LOG.d("worker-starts","SearchAllBooksWorker");
        String errorID = AppProfile.getCurrent();
        Prefs.get().put(errorID, 0);
        final LibraryScanResume resume = LibraryScanResume.read(getApplicationContext());
        final String workId = getId().toString();
        // Run again by WorkManager after the system stopped it, or resumed by the library screen
        final boolean isResume = resume.isFor(errorID)
                && (workId.equals(resume.workId) || getInputData().getBoolean(KEY_RESUME, false));
        // The books the stopped run of this scan had already read, by path
        final Map<String, FileMeta> alreadyRead = new HashMap<>();
        Set<String> skip = Collections.emptySet();
        try {
            // First: in a process WorkManager started with no screen open the profile is not
            // read yet, and Tags2.migration() failed on its null folders
            AppProfile.init(getApplicationContext());
            Tags2.migration();
            itemsMeta = new LinkedList<FileMeta>();

            if (isResume) {
                for (FileMeta m : AppDB.get().getAllByState(FileMetaCore.STATE_FULL)) {
                    alreadyRead.put(m.getPath(), m);
                }
                LOG.d("SearchAllBooksWorker", "resume", alreadyRead.size(), "stopped on", resume.current);
                skip = resume.carryOn(workId);
                LOG.d("SearchAllBooksWorker", "resume skip", skip);
            }

            ImageExtractor.clearErrors();
            if (!isResume) {
                // Covers of the books already read stay right: kept when the scan carries on
                IMG.clearDiscCache();
            }

            handler.post(new Runnable() {
                @Override
                public void run() {
                    IMG.clearMemoryCache();
                }
            });


            AppDB.get().deleteAllData();
            if (!isResume) {
                resume.start(workId, errorID);
            }


            itemsMeta.clear();

            handler.post(timer);
            LOG.d("SearchAllBooksWorker","searchPaths-all", 3, BookCSS.get().searchPathsJson);
            for (final String path : AppProfile.libraryFolders()) {
                if (path == null) {
                    continue;
                }
                if (ExtUtils.isExteralSD(path)) {
                    LOG.d("Search in storage: " + path);
                    SearchCore.searchSaf(itemsMeta, getApplicationContext(), path, ExtUtils.seachExts);
                    if (isStopped()) {
                        return false;
                    }
                    continue;
                }
                final File root = new File(path);
                if (root.isDirectory()) {
                    LOG.d("Search in: " + root.getPath());
                    SearchCore.search(itemsMeta, root, ExtUtils.seachExts);
                    if (isStopped()) {
                        return false;
                    }
                }
            }
            if(itemsMeta.isEmpty()) {
                File downloadsDir = AppSP.get().getTempDownloadBooks(getApplicationContext());
                downloadsDir.mkdirs();

                try {
                    String[] books = getApplicationContext().getAssets().list("books");
                    for(String book:books) {
                        File outFile = new File(downloadsDir, book);
                        FileOutputStream out = new FileOutputStream(outFile);
                        IOUtils.copyClose(getApplicationContext().getAssets().open("books/"+book), out);
                        LOG.d("copyBook", book,outFile );
                    }
                }catch (Exception e){
                    LOG.e(e);
                }

                SearchCore.search(itemsMeta, downloadsDir, ExtUtils.seachExts);
            }

            for (FileMeta meta : itemsMeta) {
                meta.setIsSearchBook(true);
            }

            final List<SimpleMeta> allExcluded = AppData.get().getAllExcluded();

            if (TxtUtils.isListNotEmpty(allExcluded)) {
                for (FileMeta meta : itemsMeta) {
                    if (isStopped()) {
                        return false;
                    }
                    if (allExcluded.contains(SimpleMeta.SyncSimpleMeta(meta.getPath()))) {
                        meta.setIsSearchBook(false);
                    }
                }
            }

            final List<FileMeta> allSyncBooks = AppData.get().getAllSyncBooks();
            if (TxtUtils.isListNotEmpty(allSyncBooks)) {
                for (FileMeta meta : itemsMeta) {
                    for (FileMeta sync : allSyncBooks) {
                        if (isStopped()) {
                            return false;
                        }
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

            metaDone = 0;
            metaTotal = itemsMeta.size();
            handler.post(refreshTimer);

            for (FileMeta meta : itemsMeta) {
                if (isStopped()) {
                    return false;
                }
                // A book on added storage has no file to measure; the walk that found it filled
                // these in from the provider's own answer.
                if (ExtUtils.isExteralSD(meta.getPath())) {
                    continue;
                }
                File file = new File(meta.getPath());
                FileMetaCore.get().upadteBasicMeta(meta, file);
            }

            if (!alreadyRead.isEmpty()) {
                // A book the stopped run read, and that has not changed since, is not read again
                int kept = 0;
                for (ListIterator<FileMeta> it = itemsMeta.listIterator(); it.hasNext(); ) {
                    final FileMeta meta = it.next();
                    final FileMeta read = alreadyRead.get(meta.getPath());
                    if (read != null && meta.getSize() != null && meta.getSize().equals(read.getSize())
                            && meta.getDate() != null && meta.getDate().equals(read.getDate())) {
                        read.setIsSearchBook(meta.getIsSearchBook());
                        it.set(read);
                        kept++;
                    }
                }
                alreadyRead.clear();
                LOG.d("SearchAllBooksWorker", "resume kept", kept, "of", itemsMeta.size());
            }

            AppDB.get().updateAll(itemsMeta);
            sendFinishMessage();


            if (!readFullMeta(resume, skip)) {
                return false;
            }
            // Every book is read: the library is shown as it is while the rest finishes
            handler.removeCallbacks(refreshTimer);
            sendFinishMessage();

            SharedBooks.updateProgress(itemsMeta, true, -1);
            AppDB.get().updateAll(itemsMeta);


            itemsMeta.clear();

            handler.removeCallbacks(refreshTimer);
            sendFinishMessage();
            CacheZipUtils.CacheDir.ZipService.removeCacheContent();

            if (isStopped()) {
                return false;
            }

            Clouds.get().syncronizeGet();

            if (isStopped()) {
                return false;
            }

            //TagData.restoreTags();
            Tags2.updateTagsDB();


            List<FileMeta> allNone = AppDB.get().getAllByState(FileMetaCore.STATE_NONE);
            for (FileMeta m : allNone) {
                if (isStopped()) {
                    return false;
                }
                LOG.d("BooksService-createMetaIfNeedSafe-service", m.getTitle(), m.getPath(), m.getTitle());
                FileMetaCore.createMetaIfNeedSafe(m.getPath(), false);
            }

            if (isStopped()) {
                return false;
            }
            updateBookAnnotations();
            resume.delete();
        } finally {
            Prefs.get().remove(errorID, 0);
            handler.removeCallbacksAndMessages(null);
        }
        return true;


    }

    /** Native readers one at a time, as before the scan read in more than one thread. */
    private static final Object NATIVE_LOCK = new Object();

    /**
     * Reads every book not read yet, in more than one thread: the formats read by native code in
     * one thread of their own, the formats read in Java - EPUB, FB2, MOBI... and the archives
     * that hold them - in the others. What is read is saved as it goes.
     * <p>
     * Safe beside the reader: native books are read one at a time, the archives are unpacked into
     * a folder for each thread and not the reader's, a book that fails is left with its file name
     * and the rest go on, and while a book is open on screen only one Java thread reads.
     *
     * @return false when the work was stopped
     */
    private boolean readFullMeta(LibraryScanResume resume, Set<String> skip) {
        final int threads = scanThreads(getApplicationContext());
        final File unzipRoot = new File(CacheZipUtils.CacheDir.parent, "ZipScan");
        final ExecutorService serial = Executors.newSingleThreadExecutor(scanThreadFactory("scan-native", 0));
        final ExecutorService parallel = Executors.newFixedThreadPool(threads - 1, scanThreadFactory("scan-java", 1));
        // Both lanes hand what they read to the one queue, taken as it comes
        final BlockingQueue<Future<FileMeta>> finished = new LinkedBlockingQueue<>();
        final CompletionService<FileMeta> serialLane = new ExecutorCompletionService<>(serial, finished);
        final CompletionService<FileMeta> parallelLane = new ExecutorCompletionService<>(parallel, finished);
        LOG.d("SearchAllBooksWorker", "threads", threads);
        long start = System.currentTimeMillis();
        int submitted = 0;
        try {
            for (final FileMeta meta : itemsMeta) {
                final Integer state = meta.getState();
                if (state != null && state == FileMetaCore.STATE_FULL) {
                    metaDone++;
                    continue;
                }
                if (skip.contains(meta.getPath())) {
                    LOG.d("SearchAllBooksWorker", "skip, the scan stopped on it twice", meta.getPath());
                    metaDone++;
                    continue;
                }
                final Callable<FileMeta> read = () -> {
                    final int index = scanThreadIndex();
                    // While a book is open on screen one Java thread reads; the native one waits,
                    // as MuPDF takes turns with the pages being drawn
                    while (index != 1 && TempHolder.isReaderOnScreen && !isStopped()) {
                        Thread.sleep(300);
                    }
                    if (isStopped()) {
                        return null;
                    }
                    resume.begin(meta.getPath());
                    try {
                        readFullMeta(meta, new File(unzipRoot, Thread.currentThread().getName()));
                    } catch (Throwable e) {
                        // OutOfMemoryError included: this book keeps its file name, the rest go on
                        LOG.e(e, meta.getPath());
                    } finally {
                        resume.end(meta.getPath());
                    }
                    return meta;
                };
                (isNativeFormat(meta.getPath()) ? serialLane : parallelLane).submit(read);
                submitted++;
            }

            final List<FileMeta> unsaved = new ArrayList<>();
            long savedAt = System.currentTimeMillis();
            while (submitted > 0) {
                if (isStopped()) {
                    AppDB.get().updateAll(unsaved);
                    return false;
                }
                final Future<FileMeta> next = finished.poll(200, TimeUnit.MILLISECONDS);
                if (next == null) {
                    continue;
                }
                submitted--;
                metaDone++;
                try {
                    final FileMeta meta = next.get();
                    if (meta != null) {
                        unsaved.add(meta);
                    }
                } catch (ExecutionException e) {
                    LOG.e(e);
                }
                final long now = System.currentTimeMillis();
                if (unsaved.size() >= SAVE_EVERY_BOOKS || now - savedAt >= SAVE_EVERY_MS) {
                    AppDB.get().updateAll(unsaved);
                    unsaved.clear();
                    savedAt = now;
                }
            }
            AppDB.get().updateAll(unsaved);
            LOG.d("SearchAllBooksWorker", "read full meta", System.currentTimeMillis() - start, "ms");
            return true;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        } finally {
            serial.shutdownNow();
            parallel.shutdownNow();
            try {
                serial.awaitTermination(5, TimeUnit.SECONDS);
                parallel.awaitTermination(5, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            CacheZipUtils.deleteDir(unzipRoot);
        }
    }

    private void readFullMeta(FileMeta meta, File unzipDir) {
        final String path = meta.getPath();
        if (ExtUtils.isExteralSD(path)) {
            synchronized (NATIVE_LOCK) {
                FileMetaCore.get().udpateFullMetaSaf(getApplicationContext(), meta);
            }
            return;
        }
        final EbookMeta ebookMeta;
        if (isNativeFormat(path) || isNativeInArchive(path)) {
            synchronized (NATIVE_LOCK) {
                ebookMeta = FileMetaCore.get().getEbookMetaIn(path, unzipDir);
            }
        } else {
            ebookMeta = FileMetaCore.get().getEbookMetaIn(path, unzipDir);
        }
        FileMetaCore.get().udpateFullMeta(meta, ebookMeta);
    }

    /**
     * Read by native code - MuPDF, DjVu, TIFF, the comics - or a book on added storage, copied out
     * through its provider: one at a time, in a thread of their own. Everything else is read in
     * Java with nothing shared between books, several at a time: an archive goes with them, and
     * takes its turn with the native ones only if what it holds is one of theirs.
     */
    static boolean isNativeFormat(String path) {
        if (ExtUtils.isExteralSD(path)) {
            return true;
        }
        if (ExtUtils.isZip(path)) {
            return false;
        }
        return !isJavaFormat(path);
    }

    private static boolean isNativeInArchive(String path) {
        if (!ExtUtils.isZip(path)) {
            return false;
        }
        final Pair<Boolean, String> entry = CacheZipUtils.isSingleAndSupportEntry(path);
        return entry.first && !isJavaFormat(entry.second);
    }

    private static boolean isJavaFormat(String path) {
        return BookType.EPUB.is(path) || BookType.FB2.is(path) || BookType.MOBI.is(path) || BookType.LIT.is(path)
                || BookType.CHM.is(path) || BookType.TXT.is(path) || BookType.JSON.is(path) || BookType.HTML.is(path)
                || BookType.MHT.is(path) || BookType.DOCX.is(path) || BookType.DOC.is(path) || BookType.MD.is(path)
                || BookType.RTF.is(path) || BookType.ODT.is(path);
    }

    /** 2 to 4 in all: one for the native formats, the rest for the others. */
    static int scanThreads(Context c) {
        final android.app.ActivityManager am = (android.app.ActivityManager) c.getSystemService(Context.ACTIVITY_SERVICE);
        if (am != null && am.isLowRamDevice()) {
            return 2;
        }
        return Math.max(2, Math.min(4, Runtime.getRuntime().availableProcessors() / 2));
    }

    /** "scan-native-0", "scan-java-1", "scan-java-2"...: the number after the last dash. */
    private static int scanThreadIndex() {
        final String name = Thread.currentThread().getName();
        try {
            return Integer.parseInt(name.substring(name.lastIndexOf('-') + 1));
        } catch (Exception e) {
            return 0;
        }
    }

    private static ThreadFactory scanThreadFactory(String name, int first) {
        final java.util.concurrent.atomic.AtomicInteger next = new java.util.concurrent.atomic.AtomicInteger(first);
        return r -> {
            Thread t = new Thread(() -> {
                android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_BACKGROUND);
                r.run();
            }, name + "-" + next.getAndIncrement());
            t.setDaemon(true);
            return t;
        };
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


    Runnable refreshTimer = new Runnable() {

        @Override
        public void run() {
            LOG.d("timer2");
            final int total = metaTotal;
            sendBuildingLibrary(total > 0 ? Math.min(100, metaDone * 100 / total) : -1);
            handler.postDelayed(refreshTimer, 500);
        }
    };


}
