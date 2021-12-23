package com.foobnix.pdf.info;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.os.AsyncTask;
import android.view.View;
import android.widget.Toast;

import com.foobnix.android.utils.LOG;
import com.foobnix.ext.CacheZipUtils;
import com.foobnix.ext.EpubExtractor;
import com.foobnix.mobi.parser.IOUtils;
import com.foobnix.model.AppProfile;
import com.foobnix.model.AppState;
import com.foobnix.opds.OPDS;
import com.foobnix.pdf.info.model.BookCSS;
import com.foobnix.pdf.info.view.AlertDialogs;
import com.foobnix.pdf.info.view.MyProgressDialog;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import okhttp3.CacheControl;
import okhttp3.Response;
import okio.BufferedSink;
import okio.BufferedSource;
import okio.Okio;

public class FontExtractor {

    public static String FONT_HTTP_ZIP1 = "https://raw.github.com/foobnix/LirbiReader/master/Builder/fonts/fonts.zip";
    public static String FONT_HTTP_ZIP2 = "https://www.dropbox.com/s/c8v7d05vskmjt28/fonts.zip?raw=1";


    final static Object lock = new Object();

    public static void extractFonts(final Context c) {
        if (c == null) {
            return;
        }
        new Thread("@T extractFonts") {
            @Override
            public void run() {
                // extractInside(c, "fonts", BookCSS.FONTS_DIR);
                try {
                    synchronized (lock) {
                        if (hasZipFonts()) {
                            long lastModified = AppProfile.FONT_LOCAL_ZIP.lastModified();

                            File fontDir = new File(BookCSS.get().fontFolder);
                            File index = new File(fontDir, "index");
                            LOG.d("extractFonts index exist", index.exists());
                            if (lastModified != AppState.get().fontExtractTime || !index.exists()) {
                                copyFontsFromZip();
                                boolean indexRes = index.createNewFile();
                                AppState.get().fontExtractTime = lastModified;
                                LOG.d("extractFonts YES", AppState.get().fontExtractTime, lastModified);
                                LOG.d("extractFonts index create", indexRes);
                            } else {
                                LOG.d("extractFonts NO");
                            }
                        } else if (AppsConfig.IS_FDROID) {
                            IOUtils.copyClose(c.getAssets().open("fonts.zip"), new FileOutputStream(AppProfile.FONT_LOCAL_ZIP));
                            copyFontsFromZip();
                            LOG.d("copy fonts for IS_FDROID");
                        }
                    }
                } catch (Exception e) {
                    LOG.e(e);
                }

            }

            ;
        }.start();

    }

    private static void extractInside(final Context c, String from, String to) {
        try {
            File fontsDir = getFontsDir(c, to);
            if (fontsDir.exists()) {
                LOG.d("FontExtractor Dir exists", fontsDir);
            } else {
                fontsDir.mkdirs();
            }
            String[] list = c.getAssets().list(from);
            for (String fontName : list) {
                File fontFile = new File(fontsDir, fontName);
                if (!fontFile.exists()) {
                    LOG.d("FontExtractor Copy file" + fontName, "to", fontFile);
                    InputStream open = c.getAssets().open(from + "/" + fontName);
                    EpubExtractor.writeToStream(open, new FileOutputStream(fontFile));
                    open.close();
                }
            }

        } catch (Exception e) {
            LOG.e(e);
        }
    }

    public static File getFontsDir(final Context c, String to) {
        return new File(c.getExternalCacheDir(), to);
    }


    public static void copyFontsFromZip() {
        try {
            File fontDir = new File(BookCSS.get().fontFolder);
            fontDir.mkdirs();

            AppProfile.FONT_LOCAL_ZIP.getParentFile().mkdirs();
            CacheZipUtils.extractArchive(AppProfile.FONT_LOCAL_ZIP, fontDir);
        } catch (Exception e) {
            LOG.e(e);
        }

    }

    public static boolean hasZipFonts() {
        try {
            return AppProfile.FONT_LOCAL_ZIP.isFile() && AppProfile.FONT_LOCAL_ZIP.length() > 5 * 1024 * 1024;
        } catch (Exception e) {
            return false;
        }
    }

    public static void showDownloadFontsDialog(final Activity a, final View label1, final View label2) {
        AlertDialogs.showDialog(a, a.getString(R.string.do_you_want_to_download_more_book_fonts_10mb_), a.getString(R.string.download), new Runnable() {

            @Override
            public void run() {
                new AsyncTask() {
                    ProgressDialog progressDialog;

                    @Override
                    protected void onPreExecute() {
                        progressDialog = MyProgressDialog.show(a, a.getString(R.string.please_wait));

                    }



                    @Override
                    protected Object doInBackground(Object... params) {
                        try {
                            downloadFontFile(FONT_HTTP_ZIP1);
                        } catch (Exception e) {
                            LOG.e(e);
                        }

                        if (!hasZipFonts()) {
                            try {
                                downloadFontFile(FONT_HTTP_ZIP2);
                            } catch (Exception e) {
                                LOG.e(e);
                            }
                        }
                        if (hasZipFonts()) {
                            copyFontsFromZip();
                            return true;
                        }
                        return null;

                    }

                    private void downloadFontFile(String url) throws IOException, FileNotFoundException {
                        LOG.d("Download from", url);
                        LOG.d("Download to  ", AppProfile.FONT_LOCAL_ZIP);
                        AppProfile.FONT_LOCAL_ZIP.delete();

                        okhttp3.Request request = new okhttp3.Request.Builder()//
                                .cacheControl(new CacheControl.Builder().noCache().build()).url(url)//
                                .build();//

                        Response response = OPDS.client//
                                .newCall(request)//
                                .execute();

                        BufferedSource source = response.body().source();
                        FileOutputStream out = new FileOutputStream(AppProfile.FONT_LOCAL_ZIP);
                        BufferedSink sink = Okio.buffer(Okio.sink(out));
                        sink.writeAll(response.body().source());
                        sink.close();
                        out.close();
                    }

                    @Override
                    protected void onPostExecute(Object result) {
                        if (progressDialog != null) {
                            try {
                                progressDialog.dismiss();
                            } catch (Exception e) {
                                LOG.e(e);
                            }
                        }
                        if (result == null) {
                            Toast.makeText(a, R.string.msg_unexpected_error, Toast.LENGTH_LONG).show();
                        }
                        label1.setVisibility(FontExtractor.hasZipFonts() ? View.GONE : View.VISIBLE);
                        if (hasZipFonts() && label2 != null) {
                            label2.performClick();
                        }
                    }

                    ;

                }.execute();
            }
        });
    }
}
