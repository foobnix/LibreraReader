package com.foobnix.work;

import static com.foobnix.pdf.info.AppsConfig.SEARCH_FRAGMENT_WORKER_NAME;
import static com.foobnix.pdf.info.AppsConfig.WORKER_POLICY;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.work.Data;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;
import androidx.work.WorkerParameters;

import com.foobnix.android.utils.JsonDB;
import com.foobnix.android.utils.IO;
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
import com.foobnix.ui2.AppDB;
import com.foobnix.ui2.FileMetaCore;

import org.ebookdroid.common.settings.books.SharedBooks;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;


public class SearchAllBooksWorker extends MessageWorker {

    Handler handler;
    List<FileMeta> itemsMeta;
    private long scanGeneration;

    public SearchAllBooksWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
        handler = new Handler(Looper.getMainLooper());

    }

    public static void run(Context context) {
        Context app = context.getApplicationContext();
        ScanOwnership.request(() -> enqueue(app));
    }

    private static void enqueue(Context context) {


        long generation = ScanOwnership.claim();
        OneTimeWorkRequest workRequest = new OneTimeWorkRequest.Builder(SearchAllBooksWorker.class)
                .setInputData(new Data.Builder().putLong(ScanOwnership.GENERATION, generation).build())
                .build();

        WorkManager.getInstance(context)
                .enqueueUniqueWork(SEARCH_FRAGMENT_WORKER_NAME, WORKER_POLICY, workRequest);
    }

    private static Set<String> selectedRoots() {
        Set<String> roots = new HashSet<>();
        for (String path : JsonDB.get(BookCSS.get().searchPathsJson)) {
            if (path != null && !path.trim().isEmpty()) roots.add(new File(path).getPath());
        }
        return roots;
    }

    public static void deselectRoot(Context context, String removedRoot) {
        Context app = context.getApplicationContext();
        Set<String> selected = selectedRoots();
        String root = ExtUtils.isExteralSD(removedRoot)
                ? removedRoot : new File(removedRoot).getPath();
        ScanOwnership.request(() -> {
            IO.writeObjSync(AppProfile.syncCSS, BookCSS.get());
            long owner = ScanOwnership.claim();
            ScanOwnership.write(owner, () -> false, () -> AppDB.get().reconcileDeselectedRoots(
                    selected, java.util.Collections.singleton(root)));
            enqueue(app);
        });    }

    static boolean reconcileSelection(long owner, java.util.function.BooleanSupplier stopped) {
        return ScanOwnership.write(owner, stopped, () -> AppDB.get().reconcileDeselectedRoots(
                selectedRoots(), java.util.Collections.emptySet()));
    }


    @Override protected boolean publishCompletion(Runnable action) {
        return ScanOwnership.write(scanGeneration, this::isStopped, action);
    }

    @Override protected boolean reportsOwnCompletion() { return true; }

    @Override protected boolean publishFailure(Runnable action) {
        return ScanOwnership.write(scanGeneration, () -> false, action);
    }

    public boolean doWorkInner() throws IOException {
        scanGeneration = ScanOwnership.adopt(
                getInputData().getLong(ScanOwnership.GENERATION, 0));
        if (!reconcileSelection(scanGeneration, this::isStopped)) return false;
        String errorID = AppProfile.getCurrent();
        Prefs.get().put(errorID, 0);
        try {
            Tags2.migration();
            AppProfile.init(getApplicationContext());
            ImageExtractor.clearErrors();
            itemsMeta = java.util.Collections.synchronizedList(new LinkedList<>());
            Map<String, FileMeta> before = new HashMap<>();
            for (FileMeta row : AppDB.get().scanSnapshot()) before.put(row.getPath(), row);
            Set<String> completedRoots = new HashSet<>();
            Set<String> incompleteLocal = new HashSet<>();
            Map<String, Set<String>> rootMembership = new HashMap<>();
            handler.post(timer);
            for (String path : JsonDB.get(BookCSS.get().searchPathsJson)) {
                if (path == null || path.trim().isEmpty()) continue;
                File root = new File(path);
                List<FileMeta> fromRoot = new ArrayList<>();
                try {
                    LocalDiscovery.collectPartial(root, ExtUtils.seachExts, fromRoot,
                            () -> !ScanOwnership.isCurrent(scanGeneration, this::isStopped), File::listFiles, incompleteLocal);
                } catch (IOException | RuntimeException unavailable) {
                    if (!ScanOwnership.isCurrent(scanGeneration, this::isStopped)) return false;
                    itemsMeta.addAll(fromRoot);
                    LOG.e(unavailable);
                    continue;
                }
                itemsMeta.addAll(fromRoot);
                Set<String> paths = new HashSet<>();
                for (FileMeta row : fromRoot) paths.add(row.getPath());
                rootMembership.put(root.getPath(), paths);
                completedRoots.add(root.getPath());
            }
            if (isStopped()) return false;
            if (!completedRoots.isEmpty() && incompleteLocal.isEmpty()
                    && itemsMeta.isEmpty() && !selectedRoots().isEmpty()) {
                File downloadsDir = AppSP.get().getTempDownloadBooks(getApplicationContext());
                if (!downloadsDir.isDirectory() && !downloadsDir.mkdirs()) {
                    throw new IOException("Cannot create sample-book directory");
                }
                try {
                    String[] books = getApplicationContext().getAssets().list("books");
                    if (books != null) for (String book : books) {
                        File outFile = new File(downloadsDir, book);
                        try (FileOutputStream out = new FileOutputStream(outFile)) {
                            IOUtils.copyClose(getApplicationContext().getAssets().open("books/" + book), out);
                        }
                    }
                } catch (Exception failure) { LOG.e(failure); }
                LocalDiscovery.collect(downloadsDir, ExtUtils.seachExts, itemsMeta, this::isStopped);
            }
            List<SimpleMeta> excluded = AppData.get().getAllExcluded();
            List<FileMeta> synced = AppData.get().getAllSyncBooks();
            if (!ScanMembership.apply(itemsMeta, excluded, synced, this::isStopped)) return false;
            if (!ScanOwnership.write(scanGeneration, this::isStopped,
                    () -> AppDB.get().reconcileCompletedScan(
                            itemsMeta, completedRoots, rootMembership, incompleteLocal))) return false;
            handler.removeCallbacks(timer);
            if (!ScanOwnership.isCurrent(scanGeneration, this::isStopped)) return false;
            handler.post(refreshTimer);
            for (FileMeta found : itemsMeta) {
                if (isStopped()) return false;
                FileMeta baseline = before.get(found.getPath());
                if (baseline == null) {
                    baseline = new FileMeta(found.getPath());
                    baseline.setTitle(found.getTitle());
                    com.foobnix.model.AppBook progress = SharedBooks.load(found.getPath());
                    if (!ScanOwnership.write(scanGeneration, this::isStopped,
                            () -> AppDB.get().initializeReadingProgress(found.getPath(), progress.p, progress.t)))
                        return false;
                }
                if (!publishLocalMetadata(found, baseline, scanGeneration, this::isStopped)) return false;
            }
            itemsMeta.clear();
            handler.removeCallbacks(refreshTimer);
            CacheZipUtils.CacheDir.ZipService.removeCacheContent();
            if (isStopped()) return false;
            Clouds.get().syncronizeGet();
            if (isStopped()) return false;
            Tags2.updateTagsDB();
            if (isStopped()) return false;
            updateBookAnnotations();
            return ScanOwnership.isCurrent(scanGeneration, this::isStopped);
        } finally {
            Prefs.get().remove(errorID, 0);
            handler.removeCallbacksAndMessages(null);
        }
    }

    boolean publishLocalMetadata(FileMeta found, FileMeta baseline, long owner,
                                 java.util.function.BooleanSupplier stopped) {
        FileMeta extracted = new FileMeta(found.getPath());
        File file = new File(found.getPath());
        FileMetaCore.get().upadteBasicMeta(extracted, file);
        boolean extractionSucceeded = false;
        try {
            EbookMeta metadata = readLocalMetadata(file);
            FileMetaCore.get().udpateFullMeta(extracted, metadata);
            extractionSucceeded = true;
        } catch (Exception failure) { LOG.e(failure); }
        boolean completed = extractionSucceeded;
        return ScanOwnership.write(owner, stopped,
                () -> AppDB.get().updateScannedMetadata(extracted, baseline, completed));
    }

    /** A missing or unreadable discovery cannot certify an empty metadata result. */
    protected EbookMeta readLocalMetadata(File source) throws IOException {
        try (java.io.FileInputStream input = new java.io.FileInputStream(source)) {
            if (input.read() == -1) throw new IOException("Empty book: " + source);
        }
        EbookMeta metadata = FileMetaCore.get().getEbookMetaForScan(
                source.getPath(), CacheZipUtils.CacheDir.ZipService);
        if (!source.isFile() || !source.canRead() || metadata == null
                || TxtUtils.isEmpty(metadata.getTitle())) {
            throw new IOException("Book metadata unavailable: " + source);
        }
        return metadata;
    }

    public void updateBookAnnotations() {
        if (!AppState.get().isDisplayAnnotation) return;
        for (FileMeta row : AppDB.get().scanSnapshot()) {
            if (isStopped()) return;
            if (TxtUtils.isEmpty(row.getAnnotation())) {
                String overview = FileMetaCore.getBookOverview(row.getPath());
                if (!ScanOwnership.write(scanGeneration, this::isStopped,
                        () -> AppDB.get().updateAnnotationIfMissing(row.getPath(), overview))) return;
            }
        }
    }

    Runnable timer = new Runnable() {

        @Override
        public void run() {
            if (!ScanOwnership.isCurrent(scanGeneration, SearchAllBooksWorker.this::isStopped)) return;
            ScanOwnership.tryProgress(scanGeneration, SearchAllBooksWorker.this::isStopped,
                    () -> sendProggressMessage(itemsMeta));
            handler.postDelayed(timer, 250);
        }
    };


    Runnable refreshTimer = new Runnable() {

        @Override
        public void run() {
            if (!ScanOwnership.isCurrent(scanGeneration, SearchAllBooksWorker.this::isStopped)) return;
            ScanOwnership.tryProgress(scanGeneration, SearchAllBooksWorker.this::isStopped,
                    SearchAllBooksWorker.this::sendBuildingLibrary);
            handler.postDelayed(refreshTimer, 500);
        }
    };


}
