package com.foobnix.zipmanager;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import com.foobnix.OpenerActivity;
import com.foobnix.android.utils.LOG;
import com.foobnix.android.utils.TxtUtils;
import com.foobnix.opds.OPDS;
import com.foobnix.pdf.info.R;
import com.foobnix.pdf.info.model.BookCSS;
import com.foobnix.pdf.info.view.MyProgressDialog;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.safety.Whitelist;

import java.io.File;
import java.io.FileOutputStream;

public class SendReceiveActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        final ProgressDialog progressDialog = MyProgressDialog.show(this,  getString(R.string.please_wait));
        new Thread() {
            @Override
            public void run() {
                try {
                    updateIntent();
                } catch (Exception e) {
                    LOG.e(e);
                }
                new Handler(Looper.getMainLooper()).post(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            progressDialog.dismiss();
                        } catch (Exception e) {
                            LOG.e(e);
                        }
                        startShareIntent();
                    }
                });
            }
        }.start();
    }

    private void startShareIntent() {
        try {
            getIntent().setAction(Intent.ACTION_VIEW);
            getIntent().setData(getIntent().getData());
            getIntent().setClass(this, OpenerActivity.class);
            startActivity(getIntent());
            finish();
        } catch (Exception e) {
            LOG.e(e);
            finish();
        }
    }

    private void updateIntent() {
        Bundle extras = getIntent().getExtras();
        LOG.d("updateIntent()-", getIntent());
        LOG.d("updateIntent()-getExtras", getIntent().getExtras());
        LOG.d("updateIntent()-getScheme", getIntent().getScheme());

        if (extras != null && getIntent().getData() == null) {
            final Object text = extras.get(Intent.EXTRA_TEXT);
            LOG.d("updateIntent()-text", text);
            if (text instanceof Uri) {
                getIntent().setData((Uri) text);
            }
            if (text instanceof String) {
                Uri uri = Uri.parse((String) text);
                if (uri != null && uri.getScheme() != null && (uri.getScheme().equalsIgnoreCase("http") || uri.getScheme().equalsIgnoreCase("https"))) {
                    try {

                        final Object waiter = new Object();
                        Thread thread = new Thread() {
                            @Override
                            public void run() {
                                try {

                                    final String httpResponse = OPDS.getHttpResponse((String) text, "", "");
                                    boolean isText = false;
                                    if (TxtUtils.isNotEmpty(httpResponse) && !httpResponse.contains("<html")) {
                                        isText = true;
                                    }
                                    //Document document = Jsoup.connect((String) text).userAgent("Mozilla/5.0 (jsoup)").timeout(30000).get();
                                    if (isText) {
                                        String
                                                title = TxtUtils.fixFileName(TxtUtils.substringSmart(httpResponse, 30));
                                        File file = new File(BookCSS.get().downlodsPath, title + ".txt");
                                        file.getParentFile().mkdirs();
                                        LOG.d("outerHtml-file", file);
                                        FileOutputStream fileOutputStream = new FileOutputStream(file);
                                        LOG.d("outerHtml-text", httpResponse);
                                        fileOutputStream.write(httpResponse.getBytes());
                                        fileOutputStream.flush();
                                        fileOutputStream.close();
                                        getIntent().setData(Uri.fromFile(file));

                                        LOG.d("save ready", file, file.getAbsolutePath());
                                    } else {
                                        Document document = Jsoup.parse(httpResponse);


                                        String title = (document.title() + text).replaceAll("[^\\w]+", " ");
                                        if (title.length() > 31) {
                                            title = TxtUtils.fixFileName(TxtUtils.substringSmart(title, 30));
                                        }

                                        File file = new File(BookCSS.get().downlodsPath, title + ".html");
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

                                        LOG.d("save ready", file, file.getAbsolutePath());

                                    }
                                } catch (Throwable throwable) {
                                    throwable.printStackTrace();
                                } finally {
                                    synchronized (waiter) {
                                        LOG.d("save notify");
                                        waiter.notify();
                                    }
                                }
                            }
                        };
                        thread.start();
                        synchronized (waiter) {
                            waiter.wait(30000);
                        }
                        LOG.d("wait end", getIntent().getData());
                    } catch (Throwable throwable) {
                        throwable.printStackTrace();
                    }
                } else {

                    File file = new File(BookCSS.get().downlodsPath, "temp.txt");
                    try {
                        FileOutputStream fileOutputStream = new FileOutputStream(file);
                        fileOutputStream.write(text.toString().getBytes());
                        fileOutputStream.flush();
                        fileOutputStream.close();
                        getIntent().setData(Uri.fromFile(file));
                    } catch (Exception e) {
                        LOG.e(e);
                    }

                }
                // getIntent().setData(Uri.parse((String)text));
            }
            for (String s : extras.keySet()) {
                Object o = extras.get(s);
                LOG.d(s, o);
            }
        }

        LOG.d(">>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>");

        if (getIntent() != null && getIntent().getData() == null && getIntent().getExtras() != null && getIntent().getExtras().get(Intent.EXTRA_STREAM) instanceof Uri) {
            getIntent().setData((Uri) getIntent().getExtras().get(Intent.EXTRA_STREAM));
        }
    }

}
