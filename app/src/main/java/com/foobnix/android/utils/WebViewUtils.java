package com.foobnix.android.utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import com.foobnix.sys.ImageExtractor;

import java.io.OutputStream;


public class WebViewUtils {

    static public WebView web;

    public static android.os.Handler handler = new Handler(Looper.getMainLooper());

    public static void init(Context activity) {
        web = new WebView(activity);
        //web.setPadding(0, 0, 0, 0);
        web.getSettings().setJavaScriptEnabled(true);
        web.getSettings().setSupportZoom(false);
        web.getSettings().setLoadWithOverviewMode(true);
        web.getSettings().setUseWideViewPort(true);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            WebView.enableSlowWholeDocumentDraw();
        }
        web.getSettings().setCacheMode(WebSettings.LOAD_DEFAULT);

        //web.setScrollBarStyle(WebView.SCROLLBARS_OUTSIDE_OVERLAY);
        //web.setScrollbarFadingEnabled(false);
        //web.setInitialScale(1);


        web.layout(0, 0, Dips.screenMinWH(), Dips.screenMinWH());
    }


    public static void renterToPng(String name, String content, OutputStream os, Object lock, int delay) {


        Runnable execute = new Runnable() {
            @Override
            public void run() {
                try {
                    LOG.d("web.getContentHeight()", web.getContentHeight());

                    Bitmap bitmap = Bitmap.createBitmap(Dips.screenMinWH(), (int) (web.getContentHeight() * 1.1), Bitmap.Config.ARGB_8888);
                    Canvas c = new Canvas(bitmap);
                    web.draw(c);

                    bitmap = ImageExtractor.cropBitmap(bitmap,bitmap);

                    Bitmap.CompressFormat format = Bitmap.CompressFormat.PNG;
                    bitmap.compress(format, 100, os);
                    bitmap.recycle();



                } finally {
                    synchronized (lock) {
                        lock.notify();
                    }
                }

            }
        };

        handler.post(() -> {
            LOG.d("loadData-content", content);
            web.loadData(content, "text/html", "utf-8");

            web.setWebViewClient(new WebViewClient() {

                @Override
                public void onPageCommitVisible(WebView view, String url) {
                    super.onPageCommitVisible(view, url);
                }

                @Override
                public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                    return super.shouldOverrideUrlLoading(view, request);
                }
                @Override
                public boolean shouldOverrideUrlLoading(WebView view, String url) {
                    view.loadUrl(url);
                    return true;
                }


                @Override
                public void onPageFinished(WebView view, String url) {
                    super.onPageFinished(view, url);
                    handler.postDelayed(execute, delay);

                }
            });

        });


    }


}