package com.foobnix.pdf.info;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnDismissListener;
import android.content.Intent;
import android.net.Uri;
import android.view.View;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.EditText;
import android.widget.LinearLayout;

import androidx.core.text.TextUtilsCompat;

import com.foobnix.android.utils.LOG;
import com.foobnix.opds.OPDS;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Locale;

public class Urls {

    public static String encode(String string) {
        try {
            return URLEncoder.encode(string, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            // Unreachable: every JVM has to support UTF-8. The checked exception still has to go
            // somewhere, and handing back the string untouched beats encoding it with some other
            // charset behind the caller's back.
            LOG.e(e);
            return string;
        }
    }

    public static void open(final Context a, String url) {

        if (a == null || url == null) {
            return;
        }
        LOG.d(">>> open", url);
        url = url.replace("file://", "https://");
        try {
            Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
            browserIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            a.startActivity(browserIntent);
        } catch (Exception e) {
            LOG.e(e);
        }
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
        alert.setPositiveButton(R.string.open_in_browser, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                open(a, url);
                dialog.dismiss();
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

    public static void openWhatIsNew(Context a) {
        open(a, "https://librera.app/librera-reader/what-is-new/");
    }

    public static void openTTS(Context a) {
        if (AppsConfig.IS_FDROID) {
            // Google's engine comes from Google Play, which a phone without Google services
            // does not have - offer one that F-Droid has instead.
            open(a, "https://f-droid.org/packages/org.woheller69.ttsengine");
            return;
        }
        try {
            open(a, "market://details?id=com.google.android.tts");
        } catch (Exception e) {
            // android 1.6
            open(a, "https://play.google.com/store/apps/details?id=com.google.android.tts");
        }
    }

    public static void rateIT(Context a) {
        try {
            if (AppsConfig.IS_FDROID) {
                Urls.open(a, "https://github.com/foobnix/LibreraReader");
            } else {
                Urls.open(a, "market://details?id=" + a.getPackageName());
            }
        } catch (Exception e) {
            Urls.open(a, "https://play.google.com/store/apps/details?id=" + a.getPackageName());
            LOG.e(e);
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
            return TextUtilsCompat.getLayoutDirectionFromLocale(Locale.getDefault()) == View.LAYOUT_DIRECTION_RTL;
        } catch (Exception e) {
            return false;
        }
    }

}
