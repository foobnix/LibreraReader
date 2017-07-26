package com.foobnix.zipmanager;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipArchiveInputStream;

import com.foobnix.android.utils.BaseItemLayoutAdapter;
import com.foobnix.android.utils.LOG;
import com.foobnix.ext.CacheZipUtils;
import com.foobnix.pdf.info.ExtUtils;
import com.foobnix.pdf.info.R;
import com.foobnix.pdf.search.view.AsyncProgressTask;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.net.Uri;
import android.support.v4.util.Pair;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

public class ZipDialog {
    public static String CP1251 = "CP866";

    static {
        try {
            java.nio.charset.Charset.forName(CP1251);
        } catch (Exception e) {
            CP1251 = "cp1251";
        }
    }

    static AlertDialog create;

    public static Executor EXECUTOR = Executors.newSingleThreadExecutor(new ThreadFactory() {

        @Override
        public Thread newThread(Runnable r) {
            Thread t = new Thread();
            t.setPriority(Thread.MAX_PRIORITY);
            return t;
        }
    });

    public static void show(Activity a, Uri uri, final Runnable onDismiss) {

        Pair<Boolean, String> res = CacheZipUtils.isSingleAndSupportEntry(getStream(a, uri));
        if (res.first) {
            extractAsyncProccess(a, res.second, uri, onDismiss, true);
            return;
        }

        AlertDialog.Builder dialog = new AlertDialog.Builder(a);
        dialog.setPositiveButton(R.string.close, new OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {
                if (onDismiss != null) {
                    onDismiss.run();
                }
            }
        });

        try {
            dialog.setView(getDialogContent(a, uri, new Runnable() {

                @Override
                public void run() {
                    if (onDismiss != null) {
                        onDismiss.run();
                    }
                    if (create != null) {
                        create.dismiss();
                    }
                    create = null;
                }
            }));
        } catch (Exception e) {
            LOG.e(e);
        }

        create = dialog.create();
        create.setTitle(R.string.archive_files);

        create.show();
    }

    public static View getDialogContent(final Activity a, final Uri uri, final Runnable onDismiss) {

        final List<String> items = new ArrayList<String>();

        BaseItemLayoutAdapter<String> adapter = new BaseItemLayoutAdapter<String>(a, R.layout.zip_item, items) {

            @Override
            public void populateView(View layout, int position, String item) {
                TextView text = (TextView) layout.findViewById(R.id.text1);
                text.setText(item);
            };
        };

        ListView list = new ListView(a);
        try {

            InputStream openInputStream = getStream(a, uri);
            ZipArchiveInputStream zipInputStream = new ZipArchiveInputStream(openInputStream, CP1251, true);
            ZipArchiveEntry nextEntry = null;
            while ((nextEntry = zipInputStream.getNextZipEntry()) != null) {
                String nameFull = nextEntry.getName();
                LOG.d(nameFull);
                if (!nextEntry.isDirectory()) {
                    items.add(nameFull);
                }
            }
            zipInputStream.close();
            openInputStream.close();

        } catch (Exception e) {
            LOG.e(e);
        }
        list.setAdapter(adapter);

        list.setOnItemClickListener(new OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> parent, View view, final int position, long id) {
                String name = items.get(position);
                extractAsyncProccess(a, name, uri, onDismiss, false);
            }
        });

        adapter.notifyDataSetChanged();
        return list;
    }

    private static InputStream getStream(final Activity a, final Uri uri) {
        try {
            LOG.d("getStream", uri);
            File file = new File(uri.getPath());
            if (file.isFile()) {
                return new FileInputStream(file);
            }
            return a.getContentResolver().openInputStream(uri);
        } catch (Exception e) {
            return null;
        }
    }

    public static void extractAsyncProccess(final Activity a, final String name, final Uri uri, final Runnable onDismiss, final boolean single) {
        new AsyncProgressTask<File>() {
            @Override
            public Context getContext() {
                return a;
            }

            @Override
            protected File doInBackground(Object... params) {
                return extractFile(a, name, uri, single);
            };

            @Override
            protected void onPostExecute(File file) {
                super.onPostExecute(file);
                if (file == null) {
                    Toast.makeText(a, R.string.msg_unexpected_error, Toast.LENGTH_LONG).show();
                    return;
                }
                if (onDismiss != null) {
                    onDismiss.run();
                }
                if (ExtUtils.isNotSupportedFile(file)) {
                    ExtUtils.openWith(a, file);
                } else {
                    ExtUtils.showDocument(a, file);
                }
            };
        }.execute();

    }

    public static File extractFile(Activity a, String fileName, Uri uri, boolean single) {
        try {
            CacheZipUtils.CACHE_UN_ZIP_DIR.mkdirs();

            if (!CacheZipUtils.CACHE_UN_ZIP_DIR.isDirectory()) {
                Toast.makeText(a, R.string.msg_unexpected_error, Toast.LENGTH_LONG).show();
                return null;
            }

            String outFileName = ExtUtils.getFileName(fileName);
            File out = new File(CacheZipUtils.CACHE_UN_ZIP_DIR, outFileName);
            if (out.isFile()) {
                return out;
            }

            // CacheZipUtils.removeFiles(CacheZipUtils.CACHE_UN_ZIP_DIR.listFiles());

            InputStream openInputStream = getStream(a, uri);
            ZipArchiveInputStream zipInputStream = new ZipArchiveInputStream(openInputStream, CP1251, true);
            ZipArchiveEntry nextEntry = null;
            while ((nextEntry = zipInputStream.getNextZipEntry()) != null) {
                String name = nextEntry.getName();
                LOG.d("extractFile", name, fileName);
                if (name.equals(fileName) || single) {

                    LOG.d("File extract", out.getPath());
                    CacheZipUtils.writeToStream(zipInputStream, new FileOutputStream(out));
                    zipInputStream.close();
                    openInputStream.close();
                    return out;
                }
            }
            zipInputStream.close();
            openInputStream.close();
        } catch (Exception e) {
            LOG.e(e);
        }

        return null;

    }

}
