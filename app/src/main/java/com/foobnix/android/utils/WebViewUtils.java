package com.foobnix.android.utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
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

    static WebView web;

    public static android.os.Handler handler = new Handler(Looper.getMainLooper());

    public static void init(Context activity) {
        web = new WebView(activity);
        web.getSettings().setJavaScriptEnabled(false);
        web.getSettings().setSupportZoom(false);
        web.layout(0, 0, Dips.screenWidth(), Dips.screenHeight());
    }

    public interface WebViewResponse {

        void onResult(String id, Bitmap bitmap);

    }


    public static void renterToZip(String name, String text, ZipOutputStream zos) {

        handler.post(new Runnable() {
            @Override
            public void run() {


                web.loadData(text, "text/html", "utf-8");


                web.setWebViewClient(new WebViewClient() {

                    @Override
                    public void onPageCommitVisible(WebView view, String url) {
                        super.onPageCommitVisible(view, url);


                        int wh = Dips.screenMinWH();

                        Bitmap bitmap = Bitmap.createBitmap(wh, Math.max(wh, view.getContentHeight() + Dips.DP_50), Bitmap.Config.ARGB_8888);
                        Canvas c = new Canvas(bitmap);
                        view.draw(c);


                        ByteArrayOutputStream os = new ByteArrayOutputStream();

                        Bitmap.CompressFormat format = Bitmap.CompressFormat.PNG;
                        bitmap.compress(format, 80, os);
                        bitmap.recycle();
                        try {
                            LOG.d("SVG: writeToZipNoClose", name);
                            Fb2Extractor.writeToZipNoClose(zos, name, new ByteArrayInputStream(os.toByteArray()));

                        } catch (IOException e) {
                            LOG.e(e);
                        }

                    }
                });

            }
        });
    }


}