package com.foobnix.work;

import static com.foobnix.pdf.info.AppsConfig.SEARCH_FRAGMENT_WORKER_NAME;
import static com.foobnix.pdf.info.AppsConfig.WORKER_POLICY;

import android.content.ContentResolver;
import android.content.Context;
import android.net.Uri;
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
import com.foobnix.ext.CalirbeExtractor;
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
import com.foobnix.pdf.info.SafStateStore;
import com.foobnix.pdf.info.IMG;
import com.foobnix.pdf.info.Prefs;
import com.foobnix.pdf.info.SafFileLink;
import com.foobnix.pdf.info.SafDocumentIdentity;
import com.foobnix.pdf.info.SafOpfRegistry;
import com.foobnix.pdf.info.io.SearchCore;
import com.foobnix.pdf.info.model.BookCSS;
import com.foobnix.sys.ImageExtractor;
import com.foobnix.ui2.AppDB;
import com.foobnix.ui2.FileMetaCore;

import org.ebookdroid.common.settings.books.SharedBooks;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;


public class SearchAllBooksWorker extends MessageWorker {

    Handler handler;
    List<FileMeta> itemsMeta;
    private long scanGeneration;

    private static final class MetadataTask {
        final FileMeta found, baseline;
        final SafOpfRegistry.Entry sidecar;
        final String revision, revisionKey;
        MetadataTask(FileMeta found, FileMeta baseline, SafOpfRegistry.Entry sidecar,
                     String revision, String revisionKey) {
            this.found = found;
            this.baseline = baseline;
            this.sidecar = sidecar;
            this.revision = revision;
            this.revisionKey = revisionKey;
        }
    }

    private static final class MetadataResult {
        final MetadataTask task;
        final FileMeta extracted;
        final boolean extractionSucceeded;
        MetadataResult(MetadataTask task, FileMeta extracted, boolean extractionSucceeded) {
            this.task = task;
            this.extracted = extracted;
            this.extractionSucceeded = extractionSucceeded;
        }
    }

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

    static Set<String> selectedRoots() {
        Set<String> roots = new HashSet<>();
        for (String path : JsonDB.get(BookCSS.get().searchPathsJson)) {
            if (path == null || path.trim().isEmpty()) continue;
            roots.add(ExtUtils.isExteralSD(path) ? path : new File(path).getPath());
        }
        return roots;
    }

    /** Explicit removal also clears membership from older local scans that never recorded it. */
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
        });
    }

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

    public boolean doWorkInner() throws IOException, InterruptedException {
        scanGeneration = ScanOwnership.adopt(
                getInputData().getLong(ScanOwnership.GENERATION, 0));
        if (!reconcileSelection(scanGeneration, this::isStopped)) return false;
        String errorID = AppProfile.getCurrent();
        Prefs.get().put(errorID, 0);
        try {
            Tags2.migration();
            AppProfile.init(getApplicationContext());
            if (!ScanOwnership.write(scanGeneration, this::isStopped,
                    AppDB.get()::migrateAllSafRows)) return false;
            String metadataSettings = MetadataRefreshPolicy.settingsKey();
            SafStateStore metadataPreferences = SafStateStore.get(getApplicationContext(), "ScanMetadataRevisions");
            SafStateStore failures = SafStateStore.get(getApplicationContext(), "ScanMetadataFailures");
            String profileKey = AppProfile.getCurrent() + "|";
            ImageExtractor.clearErrors();
            itemsMeta = java.util.Collections.synchronizedList(new LinkedList<>());
            SafOpfRegistry.restore(getApplicationContext());
            Map<String, SafOpfRegistry.Entry> sidecars = new HashMap<>();
            Map<String, FileMeta> before = new HashMap<>();
            for (FileMeta row : AppDB.get().scanSnapshot()) before.put(row.getPath(), row);
            List<SimpleMeta> excluded = AppData.get().getAllExcluded();
            List<FileMeta> synced = AppData.get().getAllSyncBooks();
            Set<String> completedRoots = new HashSet<>();
            Set<String> incompleteLocal = new HashSet<>();
            Map<String, Set<String>> safMembership = new HashMap<>();
            handler.post(timer);
            for (String path : JsonDB.get(BookCSS.get().searchPathsJson)) {
                if (path == null || path.trim().isEmpty()) continue;
                try {
                if (ExtUtils.isExteralSD(path)) {
                    List<FileMeta> fromRoot = new ArrayList<>();
                    Map<String, SafOpfRegistry.Entry> rootSidecars = new HashMap<>();
                    try {
                        scanSafRoot(path, fromRoot, rootSidecars, excluded, synced);
                    } finally {
                        // Confirmed partial discoveries still deserve metadata, but never certify removal.
                        sidecars.putAll(rootSidecars);
                        itemsMeta.addAll(fromRoot);
                        ScanOwnership.write(scanGeneration, () -> false,
                                () -> SafOpfRegistry.save(getApplicationContext()));
                    }
                    Set<String> paths = new HashSet<>();
                    for (FileMeta row : fromRoot) paths.add(row.getPath());
                    safMembership.put(path, paths);
                    completedRoots.add(path);
                } else {
                    File root = new File(path);
                    List<FileMeta> fromRoot = new ArrayList<>();
                    try {
                        LocalDiscovery.collectPartial(root, ExtUtils.seachExts, fromRoot,
                                () -> !ScanOwnership.isCurrent(scanGeneration, this::isStopped), File::listFiles, incompleteLocal);
                    } catch (IOException incomplete) {
                        itemsMeta.addAll(fromRoot);
                        throw incomplete;
                    }
                    itemsMeta.addAll(fromRoot);
                    Set<String> paths = new HashSet<>();
                    for (FileMeta row : fromRoot) paths.add(row.getPath());
                    safMembership.put(root.getPath(), paths);
                    completedRoots.add(root.getPath());
                }
                } catch (IOException | RuntimeException unavailable) {
                    if (!ScanOwnership.isCurrent(scanGeneration, this::isStopped)) return false;
                    LOG.e(unavailable);
                    // No completed-root entry: preserve its previous membership.
                }
            }
            if (!ScanOwnership.isCurrent(scanGeneration, this::isStopped)) return false;
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
            Map<String, FileMeta> unique = new java.util.LinkedHashMap<>();
            for (FileMeta row : itemsMeta) unique.putIfAbsent(row.getPath(), row);
            itemsMeta.clear();
            itemsMeta.addAll(unique.values());
            if (!ScanMembership.apply(itemsMeta, excluded, synced, this::isStopped)) return false;
            if (!ScanOwnership.write(scanGeneration, this::isStopped, () -> {
                updateCompletedSidecars(getApplicationContext(), itemsMeta, sidecars);
                AppDB.get().reconcileCompletedScan(itemsMeta, completedRoots, safMembership, incompleteLocal);
            })) return false;
            if (!ScanOwnership.write(scanGeneration, this::isStopped, () -> {
                Set<String> retained = new HashSet<>();
                Set<String> retainedRevisions = new HashSet<>();
                for (FileMeta row : AppDB.get().scanSnapshot()) {
                    retained.add(row.getPath());
                    retainedRevisions.add(profileKey + row.getPath());
                }
                SafStateStore.get(getApplicationContext(), "SafSidecars").retain(retained);
                SafStateStore.get(getApplicationContext(), "ScanMetadataRevisions").retain(retainedRevisions);
                SafStateStore.get(getApplicationContext(), "ScanMetadataFailures").retain(retainedRevisions);
            })) return false;
            handler.removeCallbacks(timer);
            if (!ScanOwnership.isCurrent(scanGeneration, this::isStopped)) return false;
            handler.post(refreshTimer);
            List<MetadataTask> metadataWork = new ArrayList<>();
            Set<String> queuedMetadataPaths = new HashSet<>();
            for (FileMeta found : itemsMeta) {
                if (!ScanOwnership.isCurrent(scanGeneration, this::isStopped)) return false;
                if (!queuedMetadataPaths.add(found.getPath())) continue;
                FileMeta baseline = before.get(found.getPath());
                if (baseline == null) {
                    baseline = new FileMeta(found.getPath());
                    baseline.setTitle(found.getTitle());
                    if (ExtUtils.isExteralSD(found.getPath()))
                        baseline.setState(FileMetaCore.STATE_BASIC);
                    com.foobnix.model.AppBook progress = SharedBooks.load(found.getPath());
                    if (!ScanOwnership.write(scanGeneration, this::isStopped,
                            () -> AppDB.get().initializeReadingProgress(found.getPath(), progress.p, progress.t)))
                        return false;
                }
                if (ExtUtils.isExteralSD(found.getPath()) && baseline.getState() == null)
                    baseline.setState(FileMetaCore.STATE_BASIC);
                String revision = MetadataRefreshPolicy.revision(found,
                        sidecars.get(found.getPath()), metadataSettings);
                String revisionKey = profileKey + found.getPath();
                if (metadataSettings.equals(MetadataRefreshPolicy.settingsKey())
                        && !MetadataRefreshPolicy.needsExtraction(baseline,
                                metadataPreferences.getString(revisionKey, null), revision)) continue;
                if (!MetadataRefreshPolicy.retryDue(failures.getString(revisionKey, null), revision,
                        System.currentTimeMillis())) continue;
                metadataWork.add(new MetadataTask(found, baseline, sidecars.get(found.getPath()),
                        revision, revisionKey));
            }
            try {
                if (!BoundedTasks.run(metadataWork,
                        com.foobnix.pdf.info.Tunables.METADATA_EXTRACTION_PARALLELISM,
                        () -> !ScanOwnership.isCurrent(scanGeneration, this::isStopped),
                        this::extractMetadata, result -> {
                            publishMetadataResult(scanGeneration, this::isStopped,
                                    result.extracted, result.task.baseline, result.extractionSucceeded,
                                    metadataPreferences, result.task.revisionKey,
                                    result.task.revision, metadataSettings);
                            ScanOwnership.write(scanGeneration, this::isStopped, () -> {
                                if (!metadataSettings.equals(MetadataRefreshPolicy.settingsKey())) return;
                                SafStateStore.Editor edit = failures.edit();
                                if (result.extractionSucceeded) edit.remove(result.task.revisionKey);
                                else edit.putString(result.task.revisionKey,
                                        MetadataRefreshPolicy.failedRevision(failures.getString(result.task.revisionKey, null),
                                                result.task.revision, System.currentTimeMillis()));
                                edit.commit();
                            });
                        })) return false;
            } catch (ExecutionException failed) {
                LOG.e(failed);
                return false;
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
        return publishLocalMetadata(found, baseline, owner, stopped, new boolean[1]);
    }

    boolean publishLocalMetadata(FileMeta found, FileMeta baseline, long owner,
                                 java.util.function.BooleanSupplier stopped,
                                 boolean[] extractionSucceededResult) {
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
        boolean published = ScanOwnership.write(owner, stopped,
                () -> AppDB.get().updateScannedMetadata(extracted, baseline, completed));
        extractionSucceededResult[0] = published && completed;
        return published;
    }

    boolean publishSafMetadata(FileMeta found, FileMeta baseline, long owner,
                               java.util.function.BooleanSupplier stopped) {
        return publishSafMetadata(found, baseline, null, owner, stopped);
    }

    boolean publishSafMetadata(FileMeta found, FileMeta baseline, SafOpfRegistry.Entry sidecar,
                               long owner, java.util.function.BooleanSupplier stopped) {
        return publishSafMetadata(found, baseline, sidecar, owner, stopped, new boolean[1]);
    }

    boolean publishSafMetadata(FileMeta found, FileMeta baseline, SafOpfRegistry.Entry sidecar,
                               long owner, java.util.function.BooleanSupplier stopped,
                               boolean[] extractionSucceededResult) {
        FileMeta extracted = new FileMeta(found.getPath());
        extracted.setTitle(found.getTitle());
        extracted.setPathTxt(found.getPathTxt());
        extracted.setSize(found.getSize());
        extracted.setDate(found.getDate());
        extracted.setExt(found.getExt());
        extracted.setState(FileMetaCore.STATE_BASIC);
        boolean extractionSucceeded = false;
        try {
            EbookMeta metadata = readSafMetadataForScan(found, sidecar);
            FileMetaCore.get().udpateFullMeta(extracted, metadata);
            if (TxtUtils.isEmpty(extracted.getTitle()) || extracted.getTitle().startsWith("saf_"))
                extracted.setTitle(found.getTitle());
            extractionSucceeded = true;
        } catch (Exception failure) { LOG.e(failure); }
        boolean completed = extractionSucceeded;
        boolean published = ScanOwnership.write(owner, stopped,
                () -> AppDB.get().updateScannedMetadata(extracted, baseline, completed));
        extractionSucceededResult[0] = published && completed;
        return published;
    }

    protected EbookMeta readSafMetadataForScan(FileMeta found) throws Exception {
        try (SafFileLink link = new SafFileLink(getApplicationContext(),
                Uri.parse(found.getPath()), found.getPathTxt())) {
            return readLocalMetadata(link.file);
        }
    }

    private MetadataResult extractMetadata(MetadataTask task) {
        FileMeta found = task.found;
        FileMeta extracted = new FileMeta(found.getPath());
        boolean extractionSucceeded = false;
        if (ExtUtils.isExteralSD(found.getPath())) {
            extracted.setTitle(found.getTitle());
            extracted.setPathTxt(found.getPathTxt());
            extracted.setSize(found.getSize());
            extracted.setDate(found.getDate());
            extracted.setExt(found.getExt());
            extracted.setState(FileMetaCore.STATE_BASIC);
            try {
                EbookMeta metadata = readSafMetadataForScan(found, task.sidecar);
                FileMetaCore.get().udpateFullMeta(extracted, metadata);
                if (TxtUtils.isEmpty(extracted.getTitle()) || extracted.getTitle().startsWith("saf_"))
                    extracted.setTitle(found.getTitle());
                extractionSucceeded = true;
            } catch (Exception failure) { LOG.e(failure); }
        } else {
            FileMetaCore.get().upadteBasicMeta(extracted, new File(found.getPath()));
            try {
                EbookMeta metadata = readLocalMetadata(new File(found.getPath()));
                FileMetaCore.get().udpateFullMeta(extracted, metadata);
                extractionSucceeded = true;
            } catch (Exception failure) { LOG.e(failure); }
        }
        return new MetadataResult(task, extracted, extractionSucceeded);
    }

    static boolean publishMetadataResult(long owner, java.util.function.BooleanSupplier stopped,
                                         FileMeta extracted, FileMeta baseline, boolean extractionSucceeded,
                                         SafStateStore preferences, String key, String revision,
                                         String settings) {
        return ScanOwnership.write(owner, stopped, () -> {
            if (!settings.equals(MetadataRefreshPolicy.settingsKey())) return;
            AppDB.get().updateScannedMetadata(extracted, baseline, extractionSucceeded);
            // An unacknowledged success is harmless: the next scan extracts once more.
            if (extractionSucceeded && revision != null)
                preferences.edit().putString(key, revision).commit();
        });
    }

    private void publishSafBatch(String root, List<FileMeta> batch,
                                 Map<String, SafOpfRegistry.Entry> entries,
                                 List<SimpleMeta> excluded, List<FileMeta> synced) throws IOException {
        if (!ScanMembership.apply(batch, excluded, synced, this::isStopped))
            throw new IOException("SAF scan cancelled");
        if (!ScanOwnership.write(scanGeneration, this::isStopped, () -> {
            updateCompletedSidecars(getApplicationContext(), batch, entries);
            AppDB.get().publishDiscoveredBooks(root, batch);
            sendScanBatch();
        })) throw new IOException("SAF scan replaced");
    }

    static void updateCompletedSidecars(Context context, List<FileMeta> books,
                                        Map<String, SafOpfRegistry.Entry> sidecars) {
        SafOpfRegistry.save(context);
        Map<String, SafOpfRegistry.Entry> batch = new java.util.LinkedHashMap<>();
        for (FileMeta book : books) {
            if (!ExtUtils.isExteralSD(book.getPath())) continue;
            SafOpfRegistry.Entry entry = sidecars.get(book.getPath());
            batch.put(book.getPath(), entry);
            book.setSafSidecarRevision(entry == null ? "" : entry.revision);
        }
        SafOpfRegistry.updateAll(batch);
    }

    protected void scanSafRoot(String rootPath, List<FileMeta> output,
                             Map<String, SafOpfRegistry.Entry> sidecars,
                             List<SimpleMeta> excluded, List<FileMeta> synced)
            throws IOException, InterruptedException {
        SafDiscovery.collect(getApplicationContext(), Uri.parse(rootPath), output,
                sidecars, this::isStopped,
                (batch, entries) -> publishSafBatch(rootPath, batch, entries, excluded, synced));
    }

    protected EbookMeta readSafMetadataForScan(FileMeta found, SafOpfRegistry.Entry sidecar)
            throws Exception {

        if (AppState.get().isUseCalibreOpf
                && !AppState.get().isShowOnlyOriginalFileNames && sidecar != null) {
            try (InputStream input = SafDocumentIdentity.openInputStream(
                    getApplicationContext(), sidecar.opfUri)) {
                EbookMeta metadata = CalirbeExtractor.getBookMetaInformationFromStream(input,
                        SafOpfRegistry.coverResolver(getApplicationContext(), sidecar.siblingByLowerName));
                if (metadata == null || metadata.isExtractionFailed()
                        || TxtUtils.isEmpty(metadata.getTitle()))
                    throw new IOException("Calibre sidecar metadata unavailable: " + sidecar.opfUri);
                metadata.setUnzipPath(found.getPath());
                return metadata;
            }
        }
        return readSafMetadataForScan(found);
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
