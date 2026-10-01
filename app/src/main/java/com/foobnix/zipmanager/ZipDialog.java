package com.foobnix.zipmanager;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.util.Pair;

import com.foobnix.android.utils.Apps;
import com.foobnix.android.utils.BaseItemLayoutAdapter;
import com.foobnix.android.utils.LOG;
import com.foobnix.android.utils.Views;
import com.foobnix.ext.CacheZipUtils;
import com.foobnix.mobi.parser.IOUtils;
import com.foobnix.pdf.info.ExtUtils;
import com.foobnix.pdf.info.ArchiveMemberIdentity;
import com.foobnix.pdf.info.R;
import com.foobnix.pdf.info.BookCacheLeases;
import com.foobnix.dao2.FileMeta;
import com.foobnix.ui2.AppDB;
import com.foobnix.pdf.search.view.AsyncProgressTask;
import com.foobnix.sys.ArchiveEntry;
import com.foobnix.sys.ZipArchiveInputStream;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

public class ZipDialog {
    static AlertDialog create;

    public static void show(Activity a, File file, final Runnable onDismiss) {
        show(a, file, file.getAbsolutePath(), onDismiss);
    }

    public static void show(Activity a, File file, String archiveSource, final Runnable onDismiss) {
        final AtomicBoolean completed = new AtomicBoolean();
        final Runnable finish = () -> {
            if (onDismiss != null && completed.compareAndSet(false, true)) onDismiss.run();
        };
        if(Apps.isDestroyedActivity(a)){
            finish.run();
            return;
        }

        Pair<Boolean, String> res = CacheZipUtils.isSingleAndSupportEntry(file.getPath());
        if (res.first) {
            extractAsyncProccess(a, res.second, file, finish, true, archiveSource);
            return;
        }

        AlertDialog.Builder dialog = new AlertDialog.Builder(a);
        dialog.setPositiveButton(R.string.close, new OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });

        try {
            dialog.setView(getDialogContent(a, file, archiveSource, new Runnable() {

                @Override
                public void run() {
                    finish.run();
                    if (create != null) {
                        create.dismiss();
                    }
                    create = null;
                }
            }));
        } catch (Exception e) {
            LOG.e(e);
        }

        if(Apps.isDestroyedActivity(a)){
            finish.run();
            return;
        }

        create = dialog.create();
        create.setTitle(R.string.archive_files);
        create.setOnDismissListener(ignored -> {
            finish.run();
        });

        create.show();
    }

    public static View getDialogContent(final Activity a, final File file, final Runnable onDismiss) {
        return getDialogContent(a, file, file.getAbsolutePath(), onDismiss);
    }

    private static View getDialogContent(final Activity a, final File file,
                                        final String archiveSource, final Runnable onDismiss) {

        final List<String> items = new ArrayList<String>();

        BaseItemLayoutAdapter<String> adapter = new BaseItemLayoutAdapter<String>(a, R.layout.zip_item, items) {

            @Override
            public void populateView(View layout, int position, String item) {
                TextView text = (TextView) layout.findViewById(R.id.text1);
                text.setText(item);
            }

            ;
        };

        ListView list = new ListView(a);
        try {


            ZipArchiveInputStream zipInputStream = new ZipArchiveInputStream(file.getPath());


            ArchiveEntry nextEntry = null;
            while ((nextEntry = zipInputStream.getNextEntry()) != null) {
                String nameFull = nextEntry.getName();
                LOG.d(nameFull);
                if (!nextEntry.isDirectory()) {
                    if (!ExtUtils.isImagePath(nameFull)) {
                        items.add(nameFull);
                    }
                }
            }
            zipInputStream.close();

        } catch (Exception e) {
            LOG.e(e);
        }
        list.setAdapter(adapter);

        list.setOnItemClickListener(new OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> parent, View view, final int position, long id) {
                String name = items.get(position);
                extractAsyncProccess(a, name, file, onDismiss, false, archiveSource);
            }
        });

        adapter.notifyDataSetChanged();
        return list;
    }


    public static void extractAsyncProccess(final Activity a, final String name, final File file, final Runnable onDismiss, final boolean single) {
        extractAsyncProccess(a, name, file, onDismiss, single, file.getAbsolutePath());
    }

    public static void extractAsyncProccess(final Activity a, final String name, final File file,
                                            final Runnable onDismiss, final boolean single,
                                            final String archiveSource) {
        final AutoCloseable pendingLease = BookCacheLeases.acquire(file);
        final AtomicBoolean cancelled = new AtomicBoolean();
        final AtomicBoolean outputReleased = new AtomicBoolean();
        final AtomicReference<File> extractedOutput = new AtomicReference<>();
        AsyncProgressTask<File> task = new AsyncProgressTask<File>() {
            @Override
            public Context getContext() {
                return a;
            }

            @Override
            protected File doInBackground(Object... params) {
                try (AutoCloseable activeLease = BookCacheLeases.acquire(file)) {
                    pendingLease.close();
                    File extracted = extractFile(a, name, file, single, archiveSource);
                    if (extracted != null) {
                        extractedOutput.set(extracted);
                        if (cancelled.get() && outputReleased.compareAndSet(false, true))
                            BookCacheLeases.cancelReservation(extracted);
                    }
                    return extracted;
                } catch (Exception failure) {
                    LOG.e(failure);
                    return null;
                }
            }


            @Override
            protected void onPostExecute(File file) {
                super.onPostExecute(file);
                try { pendingLease.close(); } catch (Exception failure) { LOG.e(failure); }
                if (cancelled.get()) {
                    if (onDismiss != null) onDismiss.run();
                    return;
                }
                if (file == null) {
                    if (onDismiss != null) onDismiss.run();
                    Toast.makeText(a, R.string.msg_unexpected_error, Toast.LENGTH_LONG).show();
                    return;
                }
                String identity = ArchiveMemberIdentity.forExtractedFile(file);
                if (identity != null) {
                    try {
                        FileMeta meta = AppDB.get().getOrCreate(identity);
                        if (meta.getTitle() == null || meta.getTitle().isEmpty()) {
                            meta.setTitle(ExtUtils.getFileName(name));
                            meta.setPathTxt(ExtUtils.getFileName(name));
                            AppDB.get().update(meta);
                        }
                    } catch (Exception metadataFailure) { LOG.e(metadataFailure); }
                }
                if (onDismiss != null) {
                    onDismiss.run();
                }
                if (ExtUtils.isNotSupportedFile(file)) {
                    try {
                        openDurableCopy(a, file);
                    } finally {
                        BookCacheLeases.cancelReservation(file);
                    }
                } else {
                    try {
                        if (!ExtUtils.showDocumentWithoutDialog2(a, file))
                            BookCacheLeases.cancelReservation(file);
                    } catch (RuntimeException | Error failure) {
                        BookCacheLeases.cancelReservation(file);
                        throw failure;
                    }
                }
            }

            @Override protected void onCancelled() {
                cancelled.set(true);
                File extracted = extractedOutput.get();
                if (extracted != null && outputReleased.compareAndSet(false, true))
                    BookCacheLeases.cancelReservation(extracted);
                if (onDismiss != null) onDismiss.run();
                try { pendingLease.close(); } catch (Exception failure) { LOG.e(failure); }
                super.onCancelled();
            }

            ;
        };
        try {
            task.execute();
        } catch (RuntimeException failure) {
            try { pendingLease.close(); } catch (Exception ignored) { }
            throw failure;
        }

    }

    /** Archive members have no original URI; keep an external handoff outside cache eviction. */
    private static void openDurableCopy(Activity activity, File source) {
        File output = null;
        try {
            output = ExtUtils.durableHandoffCopy(activity, source);
            activity.startActivity(ExtUtils.createOpenFileIntent(activity, output));
        } catch (Exception failure) {
            LOG.e(failure);
            if (output != null) output.delete();
            Toast.makeText(activity, R.string.msg_unexpected_error, Toast.LENGTH_LONG).show();
        }
    }

    public static File extractFile(Activity a, String fileName, File file, boolean single) {
        return extractFile(a, fileName, file, single, file.getAbsolutePath());
    }

    public static File extractFile(Activity a, String fileName, File file, boolean single,
                                   String archiveSource) {
        File staging = null;
        AutoCloseable stagingLease = null;
        boolean published = false;
        try {
            CacheZipUtils.CACHE_RECENT.mkdirs();

            if (!CacheZipUtils.CACHE_RECENT.isDirectory()) {
                Toast.makeText(a, R.string.msg_unexpected_error, Toast.LENGTH_LONG).show();
                return null;
            }

            String outFileName = ExtUtils.getFileName(fileName);
            String extractionId = UUID.randomUUID().toString();
            staging = new File(CacheZipUtils.CACHE_RECENT, extractionId + ".part");
            File completed = new File(CacheZipUtils.CACHE_RECENT, extractionId);
            stagingLease = BookCacheLeases.acquire(staging);
            if (!staging.mkdir()) throw new IOException("Cannot stage archive member");
            File out = new File(staging, outFileName);

            // CacheZipUtils.removeFiles(CacheZipUtils.CACHE_UN_ZIP_DIR.listFiles());

            ZipArchiveInputStream zipInputStream = new ZipArchiveInputStream(file.getPath());


            boolean found = false;
            ArchiveEntry nextEntry = null;
            while ((nextEntry = zipInputStream.getNextEntry()) != null) {
                String name = nextEntry.getName();
                LOG.d("extractFile", name, fileName);
                if (name.equals(fileName)) {

                    LOG.d("File extract", out.getPath());
                    IOUtils.copyClose(zipInputStream, new FileOutputStream(out));
                    zipInputStream.close();
                    found = true;
                } else if (ExtUtils.isImagePath(name)) {
                    final File img = new File(out.getParentFile(), ExtUtils.getFileName(name));
                    LOG.d("Copy-image", name, ">>", img);
                    IOUtils.copyClose(zipInputStream, new FileOutputStream(img));
                    zipInputStream.close();

                }

            }

            zipInputStream.close();
            zipInputStream.release();
            if (!found) throw new IOException("Archive member was not found: " + fileName);
            ArchiveMemberIdentity.mark(staging,
                    ArchiveMemberIdentity.create(archiveSource, fileName));
            File result = new File(completed, outFileName);
            synchronized (BookCacheLeases.class) {
                BookCacheLeases.publish(staging, completed);
                BookCacheLeases.reserve(result);
            }
            published = true;
            return result;
        } catch (Exception e) {
            LOG.e(e);
        } finally {
            if (stagingLease != null) {
                try { stagingLease.close(); } catch (Exception failure) { LOG.e(failure); }
            }
            if (!published && staging != null) BookCacheLeases.evictTree(staging);
        }

        return null;

    }

}
