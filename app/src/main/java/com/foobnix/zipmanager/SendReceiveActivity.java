package com.foobnix.zipmanager;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.OpenableColumns;

import androidx.core.content.FileProvider;

import com.foobnix.OpenerActivity;
import com.foobnix.android.utils.LOG;
import com.foobnix.android.utils.TxtUtils;
import com.foobnix.model.AppState;
import com.foobnix.opds.OPDS;
import com.foobnix.pdf.info.IMG;
import com.foobnix.pdf.info.model.BookCSS;
import com.foobnix.pdf.search.view.AsyncProgressTask;
import com.foobnix.ui2.MyContextWrapper;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.safety.Safelist;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.concurrent.TimeUnit;

import okhttp3.CacheControl;
import okhttp3.Request;
import okhttp3.Response;

public class SendReceiveActivity extends Activity {
    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        try {
            new AsyncProgressTask<Object>() {
                @Override public Context getContext() {
                    return SendReceiveActivity.this;
                }

                @Override protected Object doInBackground(Object... objects) {
                    try {
                        updateIntent();
                    } catch (Exception e) {
                        LOG.e(e);
                    }
                    return null;
                }

                @Override protected void onPostExecute(Object result) {
                    super.onPostExecute(result);
                    startShareIntent();
                }
            }.execute();
        } catch (Exception e) {
            LOG.e(e);
            startShareIntent();
        }

    }

    @Override protected void attachBaseContext(Context context) {
        super.attachBaseContext(MyContextWrapper.wrap(context));
    }

    private void startShareIntent() {
        try {
            IMG.clearMemoryCache();
            getIntent().setAction(Intent.ACTION_VIEW);
            getIntent().addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            getIntent().setData(getIntent().getData());
            getIntent().setClass(this, OpenerActivity.class);
            startActivity(getIntent());
            finish();
        } catch (Exception e) {
            LOG.e(e);
            finish();
        }
    }

    private void updateIntent() throws Exception {
        Intent intent = getIntent();
        Bundle extras = getIntent().getExtras();
        LOG.d("SendReceiveActivity", "updateIntent()-", getIntent());
        LOG.d("SendReceiveActivity", "updateIntent()-getExtras", getIntent().getExtras());
        LOG.d("SendReceiveActivity", "updateIntent()-getType", getIntent().getType());
        LOG.d("SendReceiveActivity", "updateIntent()-getScheme", getIntent().getScheme());
        LOG.d("SendReceiveActivity", "updateIntent()-getClipData", getIntent().getClipData()
                                                                              .getDescription());
        LOG.d("SendReceiveActivity", "updateIntent()-getClipData", getIntent().getClipData()
                                                                              .getItemCount());

        new File(BookCSS.get().downlodsPath).mkdirs();

        Uri fileUri = intent.getParcelableExtra(Intent.EXTRA_STREAM);
        LOG.d("SendReceiveActivity", "fileUri 1", fileUri);
        if (fileUri == null && intent.getClipData() != null) {
            fileUri = intent.getClipData().getItemAt(0).getUri();
        }
        LOG.d("SendReceiveActivity", "fileUri 2", fileUri);


        String fileName = "downloaded_file.pdf";
        if (fileUri != null) {
            Cursor cursor = getContentResolver().query(fileUri, null, null, null, null);
            if (cursor != null && cursor.moveToFirst()) {
                int nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                fileName = cursor.getString(nameIndex);
                cursor.close();
            }
        }
        LOG.d("SendReceiveActivity", "fileName", fileName);

        if (fileUri != null) {
            try {
                // Create a local file in your app's internal "files" directory
                File localFile = new File(BookCSS.get().downlodsPath, fileName);

                InputStream inputStream = getContentResolver().openInputStream(fileUri);
                OutputStream outputStream = new FileOutputStream(localFile);

                byte[] buffer = new byte[4096];
                int length;
                while ((length = inputStream.read(buffer)) > 0) {
                    outputStream.write(buffer, 0, length);
                }

                outputStream.flush();
                outputStream.close();
                inputStream.close();

                getIntent().setData(Uri.fromFile(localFile));

                return;

                // Success! The file is now at localFile.getAbsolutePath()
            } catch (IOException e) {
                e.printStackTrace();
            }
        }


        if (extras != null && getIntent().getData() == null) {
            Object res = Build.VERSION.SDK_INT >= 23 ? extras.get(Intent.EXTRA_PROCESS_TEXT) : null;
            final Object text = res != null ? res : extras.get(Intent.EXTRA_TEXT);

            LOG.d("SendReceiveActivity", "updateIntent res", res);
            LOG.d("SendReceiveActivity", "updateIntent()-text", text);

            if (text == null) {
                return;
            }

            Uri uri = Uri.parse((String) text);

            if (uri != null) {
                Request request = new Request.Builder().cacheControl(
                                                               new CacheControl.Builder().maxAge(10, TimeUnit.MINUTES)
                                                                                         .build())
                                                       .url(text.toString())
                                                       .build();//

                Response response = OPDS.client//
                                               .newCall(request)//
                                               .execute();
                LOG.d("SendReceiveActivity", "Headers", response.headers());
                //String contentType = response.header("Content-Type");
                String contentDisposition = response.header("content-disposition");
                if (contentDisposition != null && contentDisposition.contains("filename=")) {
                    String filename = contentDisposition.split("filename=")[1].replaceAll("\"", "");

                    File file = new File(BookCSS.get().downlodsPath, filename);
                    file.delete();

                    FileOutputStream fileOutputStream = new FileOutputStream(file);

                    fileOutputStream.write(response.body()
                                                   .bytes());
                    fileOutputStream.flush();
                    fileOutputStream.close();
                    getIntent().setData(Uri.fromFile(file));

                    return;

                }

            }

            if (uri != null && ((String) text).endsWith(".pdf")) {
                LOG.d("SendReceiveActivity", "updateIntent is Uri", uri);

                File file = new File(BookCSS.get().downlodsPath, uri.getLastPathSegment());
                file.delete();

                Request request = new Request.Builder().cacheControl(
                                                               new CacheControl.Builder().maxAge(10, TimeUnit.MINUTES)
                                                                                         .build())
                                                       .url(text.toString())
                                                       .build();//

                Response response = OPDS.client//
                                               .newCall(request)//
                                               .execute();

                FileOutputStream fileOutputStream = new FileOutputStream(file);

                fileOutputStream.write(response.body()
                                               .bytes());
                fileOutputStream.flush();
                fileOutputStream.close();
                getIntent().setData(Uri.fromFile(file));

                return;
            }

            LOG.d("SendReceiveActivity", "updateIntent uri", uri);

            if (text instanceof String && uri != null && uri.getScheme() != null && (uri.getScheme()
                                                                                        .equalsIgnoreCase(
                                                                                                "http") || uri.getScheme()
                                                                                                              .equalsIgnoreCase(
                                                                                                                      "https"))) {

                LOG.d("updateIntent uri getScheme", uri.getScheme());

                final String httpResponse = OPDS.getHttpResponse((String) text, "", "");

                boolean isText = false;
                if (TxtUtils.isNotEmpty(httpResponse) && !httpResponse.contains("<html")) {
                    isText = true;
                }
                LOG.d("updateIntent()-is text", isText);
                //Document document = Jsoup.connect((String) text).userAgent("Mozilla/5.0 (jsoup)").timeout(30000).get();
                if (isText) {
                    new File(BookCSS.get().downlodsPath).mkdirs();
                    String title = TxtUtils.fixFileName(TxtUtils.substringSmart(httpResponse, 30));
                    File file = new File(BookCSS.get().downlodsPath, title + ".txt");
                    file.delete();
                    LOG.d("outerHtml-file", file);
                    FileOutputStream fileOutputStream = new FileOutputStream(file);
                    LOG.d("outerHtml-text", httpResponse);
                    fileOutputStream.write(httpResponse.getBytes());
                    fileOutputStream.flush();
                    fileOutputStream.close();
                    getIntent().setData(Uri.fromFile(file));

                    LOG.d("updateIntent save ready", file, file.getAbsolutePath());
                } else {
                    Document document = Jsoup.parse(httpResponse);

                    String title = (document.title() + text).replaceAll("[^\\w]+", " ");
                    if (title.length() > 31) {
                        title = TxtUtils.fixFileName(TxtUtils.substringSmart(title, 30));
                    }
                    new File(BookCSS.get().downlodsPath).mkdirs();
                    File file = new File(BookCSS.get().downlodsPath, title + ".html");
                    file.delete();

                    FileOutputStream fileOutputStream = new FileOutputStream(file);
                    String outerHtml = Jsoup.clean(document.html(), Safelist.basic());
                    LOG.d("outerHtml-html", outerHtml);
                    fileOutputStream.write("<html><head></head><body style='text-align:justify;'>".getBytes());
                    fileOutputStream.write(outerHtml.getBytes());
                    fileOutputStream.write("</body></html>".getBytes());
                    fileOutputStream.flush();
                    fileOutputStream.close();
                    getIntent().setData(Uri.fromFile(file));

                    LOG.d("updateIntent save ready", file, file.getAbsolutePath());

                }

            } else {

                File root = new File(BookCSS.get().downlodsPath, "temp");
                root.mkdirs();
                try {
                    File file = new File(root, "temp-" + text.hashCode() + ".txt");
                    file.getParentFile()
                        .mkdirs();
                    FileOutputStream fileOutputStream = new FileOutputStream(file);
                    fileOutputStream.write(text.toString()
                                               .getBytes());
                    fileOutputStream.flush();
                    fileOutputStream.close();
                    getIntent().setData(Uri.fromFile(file));
                    LOG.d(" updateIntent setData temp.txt", file);
                } catch (Exception e) {
                    LOG.e(e);
                }

            }
        }

    }

}
