package com.foobnix.zipmanager;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;

import com.foobnix.android.utils.LOG;

import org.ebookdroid.ui.viewer.VerticalViewActivity;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.io.File;
import java.io.FileOutputStream;

public class SendReceiveActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        final ProgressDialog progressDialog = new ProgressDialog(this);
        progressDialog.show();
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
                        progressDialog.hide();
                        startShareIntent();
                    }
                });
            }
        }.start();
    }

    private void startShareIntent() {
        getIntent().setAction(Intent.ACTION_VIEW);
        getIntent().setData(getIntent().getData());
        getIntent().setClass(this, VerticalViewActivity.class);
        startActivity(getIntent());
        finish();
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
                                    Document document = Jsoup.connect((String) text).userAgent("Mozilla/5.0 (jsoup)").timeout(30000).get();
                                    String title = (document.title() + text).replaceAll("[^\\w]+", " ");
                                    if (title.length() > 51) {
                                        title = title.substring(0, 50) + System.currentTimeMillis();
                                    }
                                    File file = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), title + ".html");
                                    FileOutputStream fileOutputStream = new FileOutputStream(file);
                                    String outerHtml = document.outerHtml();
                                    fileOutputStream.write(outerHtml.getBytes());
                                    fileOutputStream.flush();
                                    fileOutputStream.close();
                                    getIntent().setData(Uri.fromFile(file));
                                    synchronized (waiter) {
                                        LOG.d("save notify", file, file.getAbsolutePath());
                                        waiter.notify();
                                    }
                                    LOG.d("save ready", file, file.getAbsolutePath());
                                } catch (Throwable throwable) {
                                    throwable.printStackTrace();
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

                    File file = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "temp.txt");
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
