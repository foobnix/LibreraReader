package com.foobnix.pdf.info;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Locale;

import com.foobnix.android.utils.LOG;
import com.foobnix.opds.OPDS;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnDismissListener;
import android.content.Intent;
import android.net.Uri;
import android.support.v4.text.TextUtilsCompat;
import android.support.v4.view.ViewCompat;
import android.view.View;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.EditText;
import android.widget.LinearLayout;

public class Urls {

    public static String encode(String string) {
        try {
            return URLEncoder.encode(string, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            LOG.e(e);
            return URLEncoder.encode(string);
        }
    }

    public static void open(final Context a, String url) {
        if (a == null || url == null) {
            return;
        }
        LOG.d(">>> open", url);
        Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
        browserIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        a.startActivity(browserIntent);
    }

    public static void openWevView(final Context a, String url, final Runnable onClose) {
        LOG.d(">>> open WevView", url);

        AlertDialog.Builder alert = new AlertDialog.Builder(a);
        alert.setTitle(url);

        LinearLayout wrapper = new LinearLayout(a);
        EditText keyboardHack = new EditText(a);
        keyboardHack.setVisibility(View.GONE);

        final WebView wv = new WebView(a);
        wv.getSettings().setUserAgentString(OPDS.USER_AGENT);
        wv.getSettings().setJavaScriptEnabled(true);
        wv.loadUrl(url);
        wv.setFocusable(true);
        wv.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                view.loadUrl(url);
                return true;
            }

        });

        wrapper.setOrientation(LinearLayout.VERTICAL);
        wrapper.addView(wv, LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT);
        wrapper.addView(keyboardHack, LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        alert.setView(wrapper);
        alert.setNegativeButton(R.string.close, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int id) {
                dialog.dismiss();
                if (onClose != null) {
                    onClose.run();
                }
            }
        });
        AlertDialog create = alert.create();
        create.setOnDismissListener(new OnDismissListener() {

            @Override
            public void onDismiss(DialogInterface dialog) {
                if (onClose != null) {
                    onClose.run();
                }
            }
        });
        alert.show();

    }

    public static void openPdfPro(Context a) {
        try {
            open(a, "market://details?id=com.foobnix.pro.pdf.reader");
        } catch (Exception e) {
            // android 1.6
            open(a, "https://play.google.com/store/apps/details?id=com.foobnix.pro.pdf.reader");
        }
    }

    public static void openTTS(Context a) {
        try {
            open(a, "market://details?id=com.google.android.tts");
        } catch (Exception e) {
            // android 1.6
            open(a, "https://play.google.com/store/apps/details?id=com.google.android.tts");
        }
    }

    public static String getLangCode() {
        try {
            return Locale.getDefault().getLanguage();
        } catch (Exception e) {
            return "";
        }
    }

    public static boolean isRtl() {
        try {
            return TextUtilsCompat.getLayoutDirectionFromLocale(Locale.getDefault()) == ViewCompat.LAYOUT_DIRECTION_RTL;
        } catch (Exception e) {
            return false;
        }
    }

}
