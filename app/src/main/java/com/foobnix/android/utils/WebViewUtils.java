package com.foobnix.android.utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import com.foobnix.ext.Fb2Extractor;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.zip.ZipOutputStream;


public class WebViewUtils {

    static public WebView web;

    public static android.os.Handler handler = new Handler(Looper.getMainLooper());

    public static void init(Context activity) {
        web = new WebView(activity);
        //web.setPadding(0, 0, 0, 0);
        web.getSettings().setJavaScriptEnabled(false);
        web.getSettings().setSupportZoom(false);
        web.getSettings().setLoadWithOverviewMode(true);
        web.getSettings().setUseWideViewPort(true);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            WebView.enableSlowWholeDocumentDraw();
        }
        //web.getSettings().setCacheMode(WebSettings.LOAD_NO_CACHE);

        //web.setScrollBarStyle(WebView.SCROLLBARS_OUTSIDE_OVERLAY);
        //web.setScrollbarFadingEnabled(false);
        //web.setInitialScale(1);


        web.layout(0, 0, Dips.screenMinWH(), Dips.screenMinWH());
    }


    public static void renterToZip(String name, String content, ZipOutputStream zos, Object lock) {


        Runnable execute = new Runnable() {
            @Override
            public void run() {
                try {
                    int wh = Dips.screenMinWH();
                    LOG.d("web.getContentHeight()", web.getContentHeight());

                    Bitmap bitmap = Bitmap.createBitmap(wh, (int) (web.getContentHeight() * 1.1), Bitmap.Config.ARGB_8888);
                    Canvas c = new Canvas(bitmap);
                    web.draw(c);


                    ByteArrayOutputStream os = new ByteArrayOutputStream();

                    Bitmap.CompressFormat format = Bitmap.CompressFormat.PNG;
                    bitmap.compress(format, 100, os);
                    bitmap.recycle();
                    try {
                        LOG.d("SVG: writeToZipNoClose", name);
                        Fb2Extractor.writeToZipNoClose(zos, name, new ByteArrayInputStream(os.toByteArray()));

                    } catch (IOException e) {
                        LOG.e(e);
                    }
                } finally {
                    synchronized (lock) {
                        lock.notify();
                    }
                }

            }
        };

        handler.post(() -> {
            web.loadData(content, "text/html", "utf-8");

            web.setWebViewClient(new WebViewClient() {

                @Override
                public void onPageCommitVisible(WebView view, String url) {
                    super.onPageCommitVisible(view, url);
                }

                @Override
                public void onPageFinished(WebView view, String url) {
                    super.onPageFinished(view, url);
                    handler.postDelayed(execute, 50);

                }
            });

        });



    }


}