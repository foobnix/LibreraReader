package com.foobnix.pdf.info.view;

import java.io.File;
import java.io.FileOutputStream;
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
import android.widget.Toast;

public class Downloader {

    public static void openOrDownload(final Activity a, final FileMeta meta, final Runnable onFinish) {
        String displayName = ExtUtils.getFileName(meta.getPath());
        final String path = Clouds.getPath(meta.getPath());

        final File fileCache = Clouds.getCacheFile(meta.getPath());
        if (fileCache.isFile()) {
            ExtUtils.openFile(a, new FileMeta(fileCache.getPath()));
            return;
        }


        AlertDialogs.showDialog(a, a.getString(R.string.do_you_want_to_download_the_file_) + "\n\"" + displayName + "\"", a.getString(R.string.download), new Runnable() {

            @Override
            public void run() {
                new AsyncProgressTask<Boolean>() {

                    @Override
                    public Context getContext() {
                        return a;
                    }

                    @Override
                    protected Boolean doInBackground(Object... params) {
                        try {
                            LOG.d("Download file", meta.getPath(), path);
                            InputStream download = Clouds.get().cloud(meta.getPath()).download(path);
                            FileOutputStream out = new FileOutputStream(fileCache);
                            Fb2Extractor.zipCopy(download, out);
                            out.close();
                            return true;
                        } catch (Exception e) {
                            LOG.e(e);
                        }

                        return null;
                    }

                    @Override
                    protected void onPostExecute(Boolean result) {
                        super.onPostExecute(result);
                        if (result == null) {
                            Toast.makeText(getContext(), R.string.msg_unexpected_error, Toast.LENGTH_SHORT).show();
                        } else {
                            onFinish.run();
                            if (fileCache.isFile() && fileCache.length() > 0) {
                                ExtUtils.openFile(a, new FileMeta(fileCache.getPath()));
                            }
                        }
                    };

                }.execute();

            }
        });

    }

}
