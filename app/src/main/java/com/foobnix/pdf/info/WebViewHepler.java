package com.foobnix.pdf.info;

import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.os.Build;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import com.foobnix.android.utils.Dips;
import com.foobnix.android.utils.LOG;
import com.foobnix.android.utils.ResultResponse;

import java.util.concurrent.locks.ReentrantLock;

public class WebViewHepler {

    public static WebView webView;
    static long timeout = 0;
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
        webView.loadUrl("file://error.html");

    }

    public static void getBitmap(String path, final ResultResponse<Bitmap> res) {

        final long init = System.currentTimeMillis();
        webView.setWebViewClient(new WebViewClient() {

            @Override
            public void onPageFinished(WebView view, String url) {

                // webView.scrollTo(0, Dips.screenHeight() / 2);
                timeout = System.currentTimeMillis() - init;
                LOG.d("WebView onPageFinished", url, timeout);

                final Bitmap bitmap = Bitmap.createBitmap(Dips.screenWidth(), Dips.screenHeight() / 2, Bitmap.Config.ARGB_8888);
                final Canvas c = new Canvas(bitmap);
                // webView.scrollTo(0, Dips.screenHeight() / 2 * -1);
                webView.draw(c);
                res.onResultRecive(bitmap);

            }

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                LOG.d("WebView shouldOverrideUrlLoading", url);
                view.loadUrl(url);
                return true;
            }
        });
        webView.loadUrl("file://" + path);
        // webView.loadUrl("file:///android_asset/reader/index.html");

    }

}
