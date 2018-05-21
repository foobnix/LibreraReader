package com.foobnix.pdf.info.view;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import com.foobnix.android.utils.LOG;
import com.foobnix.dao2.FileMeta;
import com.foobnix.ext.Fb2Extractor;
import com.foobnix.pdf.info.Clouds;
import com.foobnix.pdf.info.ExtUtils;
import com.foobnix.pdf.info.R;
import com.foobnix.pdf.info.wrapper.AppState;
import com.foobnix.pdf.search.view.AsyncProgressTask;

import android.app.Activity;
import android.content.Context;

public class Downloader {

    public static void openOrDownload(final Activity a, final FileMeta meta, final Runnable onFinish) {
        String displayName = ExtUtils.getFileName(meta.getPath());
        final String path = Clouds.getPath(meta.getPath());

        File downloadDir = new File(AppState.get().downlodsPath);
        if (!downloadDir.exists()) {
            downloadDir.mkdirs();
        }

        final File file = new File(downloadDir, displayName);
        if (file.isFile() && file.length() > 0) {
            ExtUtils.openFile(a, new FileMeta(file.getPath()));
            return;
        }

        AlertDialogs.showDialog(a, a.getString(R.string.do_you_want_to_download_the_file_) + "\n\"" + displayName + "\"", a.getString(R.string.download), new Runnable() {

            @Override
            public void run() {
                new AsyncProgressTask<String>() {

                    @Override
                    public Context getContext() {
                        return a;
                    }

                    @Override
                    protected String doInBackground(Object... params) {
                        try {
                            InputStream download = Clouds.get().cloud(meta.getPath()).download(path);
                            FileOutputStream out = new FileOutputStream(file);
                            Fb2Extractor.zipCopy(download, out);
                            out.close();
                        } catch (IOException e) {
                            LOG.e(e);
                        }

                        return null;
                    }

                    @Override
                    protected void onPostExecute(String result) {
                        super.onPostExecute(result);
                        onFinish.run();
                        if (file.isFile() && file.length() > 0) {
                            ExtUtils.openFile(a, new FileMeta(file.getPath()));
                            return;
                        }
                    };

                }.execute();

            }
        });

    }

}
