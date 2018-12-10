package com.foobnix.pdf.info;

import java.util.concurrent.locks.ReentrantLock;

import com.foobnix.android.utils.Dips;
import com.foobnix.android.utils.LOG;

import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.os.Build;
import android.webkit.WebView;
import android.webkit.WebViewClient;

public class WebViewHepler {

    public static WebView webView;
    public static final ReentrantLock lock = new ReentrantLock();

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public static void init(Context c) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            // WebView.enableSlowWholeDocumentDraw();
        }

        webView = new WebView(c);
        webView.layout(0, 0, Dips.screenWidth(), Dips.screenHeight());

        webView.getSettings().setJavaScriptEnabled(true);
        webView.setFocusable(true);
    }

    public static Bitmap getBitmap(String path) {

        webView.setWebViewClient(new WebViewClient() {

            @Override
            public void onPageFinished(WebView view, String url) {
                LOG.d("WebView onPageFinished", url);

                // webView.scrollTo(0, Dips.screenHeight() / 2);
            }

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                LOG.d("WebView shouldOverrideUrlLoading", url);
                view.loadUrl(url);
                return true;
            }
        });
        webView.loadUrl("file://" + path);

        try {
            Thread.sleep(3000);
        } catch (InterruptedException e) {

        }

        Bitmap bitmap = Bitmap.createBitmap(Dips.screenWidth(), Dips.screenHeight() / 2, Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(bitmap);
        webView.draw(c);

        return bitmap;

    }

}
