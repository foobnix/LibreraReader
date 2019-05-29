package com.foobnix.android.utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.os.Handler;
import android.os.Looper;
import android.webkit.WebView;

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
        //web.setScrollBarStyle(WebView.SCROLLBARS_OUTSIDE_OVERLAY);
        //web.setScrollbarFadingEnabled(false);
        //web.setInitialScale(1);


        web.layout(0, 0, Dips.screenMinWH(), Dips.screenMinWH());
    }

    public interface WebViewResponse {

        void onResult(String id, Bitmap bitmap);

    }


    public static void renterToZip(String name, String content, ZipOutputStream zos, Object lock) {

        handler.post(() -> web.loadData(content, "text/html", "utf-8"));

        handler.postDelayed(() -> {
            int wh = Dips.screenMinWH();

            Bitmap bitmap = Bitmap.createBitmap(wh, Math.min(wh, web.getContentHeight() + Dips.DP_50), Bitmap.Config.ARGB_8888);
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
            synchronized (lock) {
                lock.notify();
            }

        }, 500);

    }


}