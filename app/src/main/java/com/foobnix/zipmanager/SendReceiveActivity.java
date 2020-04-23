package com.foobnix.zipmanager;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;

import com.foobnix.OpenerActivity;
import com.foobnix.android.utils.LOG;
import com.foobnix.android.utils.TxtUtils;
import com.foobnix.opds.OPDS;
import com.foobnix.pdf.info.IMG;
import com.foobnix.pdf.info.model.BookCSS;
import com.foobnix.pdf.search.view.AsyncProgressTask;
import com.foobnix.ui2.MyContextWrapper;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.safety.Whitelist;

import java.io.File;
import java.io.FileOutputStream;

public class SendReceiveActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        new AsyncProgressTask<Object>() {
            @Override
            public Context getContext() {
                return SendReceiveActivity.this;
            }

            @Override
            protected Object doInBackground(Object... objects) {
                try {
                    updateIntent();
                } catch (Exception e) {
                    LOG.e(e);
                }
                return null;
            }

            @Override
            protected void onPostExecute(Object result) {
                super.onPostExecute(result);
                startShareIntent();
            }
        }.execute();

    }

    @Override
    protected void attachBaseContext(Context context) {
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
        Bundle extras = getIntent().getExtras();
        LOG.d("updateIntent()-", getIntent());
        LOG.d("updateIntent()-getExtras", getIntent().getExtras());
        LOG.d("updateIntent()-getScheme", getIntent().getScheme());

        if (extras != null && getIntent().getData() == null) {
            Object res = Build.VERSION.SDK_INT >= 23 ? extras.get(Intent.EXTRA_PROCESS_TEXT) : null;
            final Object text = res != null ? res : extras.get(Intent.EXTRA_TEXT);

            LOG.d("updateIntent()-text", text);
            if (text instanceof Uri) {
                getIntent().setData((Uri) text);
            }
            Uri uri = Uri.parse((String) text);
            LOG.d("updateIntent uri", uri);
            if (text instanceof String && uri != null && uri.getScheme() != null && (uri.getScheme().equalsIgnoreCase("http") || uri.getScheme().equalsIgnoreCase("https"))) {

                LOG.d("updateIntent uri getScheme", uri.getScheme());


                final String httpResponse = OPDS.getHttpResponse((String) text, "", "");

                boolean isText = false;
                if (TxtUtils.isNotEmpty(httpResponse) && !httpResponse.contains("<html")) {
                    isText = true;
                }
                LOG.d("updateIntent()-is text", isText);
                //Document document = Jsoup.connect((String) text).userAgent("Mozilla/5.0 (jsoup)").timeout(30000).get();
                if (isText) {
                    String
                            title = TxtUtils.fixFileName(TxtUtils.substringSmart(httpResponse, 30));
                    File file = new File(BookCSS.get().downlodsPath, title + ".txt");
                    file.delete();
                    file.getParentFile().mkdirs();
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

                    File file = new File(BookCSS.get().downlodsPath, title + ".html");
                    file.delete();
                    file.getParentFile().mkdirs();

                    FileOutputStream fileOutputStream = new FileOutputStream(file);
                    String outerHtml = Jsoup.clean(document.html(), Whitelist.basic());
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

                File file = new File(BookCSS.get().downlodsPath, "temp.txt");
                file.delete();
                try {
                    file.getParentFile().mkdirs();
                    FileOutputStream fileOutputStream = new FileOutputStream(file);
                    fileOutputStream.write(text.toString().getBytes());
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
