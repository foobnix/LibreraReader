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

import com.foobnix.android.utils.BaseItemLayoutAdapter;
import com.foobnix.android.utils.LOG;
import com.foobnix.android.utils.Views;
import com.foobnix.ext.CacheZipUtils;
import com.foobnix.mobi.parser.IOUtils;
import com.foobnix.pdf.info.ExtUtils;
import com.foobnix.pdf.info.R;
import com.foobnix.pdf.search.view.AsyncProgressTask;
import com.foobnix.sys.ArchiveEntry;
import com.foobnix.sys.ZipArchiveInputStream;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.List;

public class ZipDialog {
    static AlertDialog create;

    public static void show(Activity a, File file, final Runnable onDismiss) {
        if(Views.isDestroyedActivity(a)){
            return;
        }

        Pair<Boolean, String> res = CacheZipUtils.isSingleAndSupportEntry(file.getPath());
        if (res.first) {
            extractAsyncProccess(a, res.second, file, onDismiss, true);
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
            dialog.setView(getDialogContent(a, file, new Runnable() {

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

        if(Views.isDestroyedActivity(a)){
            return;
        }

        create = dialog.create();
        create.setTitle(R.string.archive_files);

        create.show();
    }

    public static View getDialogContent(final Activity a, final File file, final Runnable onDismiss) {

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
                extractAsyncProccess(a, name, file, onDismiss, false);
            }
        });

        adapter.notifyDataSetChanged();
        return list;
    }


    public static void extractAsyncProccess(final Activity a, final String name, final File file, final Runnable onDismiss, final boolean single) {
        new AsyncProgressTask<File>() {
            @Override
            public Context getContext() {
                return a;
            }

            @Override
            protected File doInBackground(Object... params) {
                return extractFile(a, name, file, single);
            }


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
                    ExtUtils.showDocumentWithoutDialog2(a, file);
                }
            }

            ;
        }.execute();

    }

    public static File extractFile(Activity a, String fileName, File file, boolean single) {
        try {
            CacheZipUtils.CACHE_RECENT.mkdirs();

            if (!CacheZipUtils.CACHE_RECENT.isDirectory()) {
                Toast.makeText(a, R.string.msg_unexpected_error, Toast.LENGTH_LONG).show();
                return null;
            }

            String outFileName = ExtUtils.getFileName(fileName);
            File out = new File(CacheZipUtils.CACHE_RECENT, outFileName);
            if (out.isFile()) {
                return out;
            }

            // CacheZipUtils.removeFiles(CacheZipUtils.CACHE_UN_ZIP_DIR.listFiles());

            ZipArchiveInputStream zipInputStream = new ZipArchiveInputStream(file.getPath());


            ArchiveEntry nextEntry = null;
            while ((nextEntry = zipInputStream.getNextEntry()) != null) {
                String name = nextEntry.getName();
                LOG.d("extractFile", name, fileName);
                if (name.equals(fileName) || single) {

                    LOG.d("File extract", out.getPath());
                    IOUtils.copyClose(zipInputStream, new FileOutputStream(out));
                    zipInputStream.close();
                } else if (ExtUtils.isImagePath(name)) {
                    final File img = new File(out.getParentFile(), ExtUtils.getFileName(name));
                    LOG.d("Copy-image", name, ">>", img);
                    IOUtils.copyClose(zipInputStream, new FileOutputStream(img));
                    zipInputStream.close();

                }

            }

            zipInputStream.close();
            zipInputStream.release();
            return out;
        } catch (Exception e) {
            LOG.e(e);
        }

        return null;

    }

}
