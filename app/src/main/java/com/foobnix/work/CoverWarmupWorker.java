package com.foobnix.work;

import android.content.Context;
import android.graphics.Bitmap;
import android.util.Log;
import android.os.SystemClock;

import androidx.annotation.NonNull;
import androidx.work.ExistingWorkPolicy;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.bumptech.glide.Glide;
import com.bumptech.glide.Priority;
import com.bumptech.glide.request.FutureTarget;
import com.foobnix.dao2.FileMeta;
import com.foobnix.model.AppProfile;
import com.foobnix.model.AppState;
import com.foobnix.pdf.info.IMG;
import com.foobnix.ui2.AppDB;
import com.foobnix.ui2.FileMetaCore;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/** Warms Glide's normal cover cache independently of which rows have been visible. */
public class CoverWarmupWorker extends Worker {
    private static final String NAME = "library-cover-warmup";

    public CoverWarmupWorker(@NonNull Context context, @NonNull WorkerParameters parameters) {
        super(context, parameters);
    }

    public static void run(Context context) {
        // A completed rescan may add books while an earlier warmup is still running.
        // Replace it with a fresh snapshot rather than dropping that request.
        enqueue(WorkManager.getInstance(context), NAME);
    }

    static androidx.work.Operation enqueue(WorkManager manager, String name) {
        return manager.enqueueUniqueWork(name, ExistingWorkPolicy.REPLACE,
                new OneTimeWorkRequest.Builder(CoverWarmupWorker.class).build());
    }

    protected List<FileMeta> loadBooks() {
        return booksToWarm(AppDB.get().getAll(), AppState.get().isShowImages,
                path -> false);
    }

    protected void warmBook(Context context, FileMeta book) {
        warmBook(context, book, this::isStopped);
    }

    static boolean warmBook(Context context, FileMeta book,
                            java.util.function.BooleanSupplier stopped) {
        FutureTarget<Bitmap> target = IMG.getCoverPageWithEffect(context, book.getPath(), null)
                .priority(Priority.LOW).submit();
        try {
            while (!stopped.getAsBoolean()) {
                try {
                    return target.get(250, TimeUnit.MILLISECONDS) != null;
                } catch (TimeoutException pending) { /* Check cancellation while waiting. */ }
            }
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
        } catch (Exception failed) {
            Log.d("CoverWarmup", "Could not warm " + book.getPath(), failed);
        } finally {
            Glide.with(context).clear(target);
        }
        return false;
    }

    @NonNull @Override public Result doWork() {
        long start = SystemClock.elapsedRealtime();
        Context context = getApplicationContext();
        AppProfile.init(context);
        if (!AppState.get().isShowImages) return Result.success();
        List<FileMeta> paths = loadBooks();
        try {
            // Leave a Glide source worker available for foreground cover requests.
            boolean finished = BoundedTasks.run(paths, 1, this::isStopped, book -> {
                warmBook(context, book);
                return book.getPath();
            }, path -> {});
            Log.i("CoverWarmup", "finished=" + finished + " books=" + paths.size()
                    + " ms=" + (SystemClock.elapsedRealtime() - start));
            return finished ? Result.success() : Result.failure();
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            return Result.failure();
        } catch (Exception failed) {
            Log.w("CoverWarmup", "Cover warmup failed", failed);
            return Result.failure();
        }
    }

    static List<FileMeta> booksToWarm(Iterable<FileMeta> books, boolean images,
                                      java.util.function.Predicate<String> hasSidecar) {
        List<FileMeta> paths = new ArrayList<>();
        if (!images) return paths;
        for (FileMeta book : books) {
            if (!Boolean.TRUE.equals(book.getIsSearchBook()) || book.getPath() == null) continue;
            // Generic SAF libraries also warm covers after their first metadata extraction.
            if (book.getPath().startsWith("content://") && !hasSidecar.test(book.getPath())
                    && !Integer.valueOf(FileMetaCore.STATE_FULL).equals(book.getState())) continue;
            // Only request identity and revision are needed. Do not retain mutable DAO rows
            // while metadata extraction continues on another thread.
            FileMeta request = new FileMeta(book.getPath());
            request.setSize(book.getSize()); request.setDate(book.getDate());
            paths.add(request);
        }
        return paths;
    }
}
