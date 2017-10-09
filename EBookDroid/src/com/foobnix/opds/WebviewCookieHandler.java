package com.foobnix.opds;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.foobnix.pdf.info.IMG;

import android.os.Build;
import android.webkit.CookieManager;
import android.webkit.CookieSyncManager;
import okhttp3.Cookie;
import okhttp3.CookieJar;
import okhttp3.HttpUrl;

public final class WebviewCookieHandler implements CookieJar {

    private CookieManager webviewCookieManager;

    public WebviewCookieHandler() {
        if (Build.VERSION.SDK_INT <= 19) {
            CookieSyncManager.createInstance(IMG.context);
        }
        webviewCookieManager = CookieManager.getInstance();
    }

    @Override
    public void saveFromResponse(HttpUrl url, List<Cookie> cookies) {
        String urlString = url.toString();

        for (Cookie cookie : cookies) {
            webviewCookieManager.setCookie(urlString, cookie.toString());
        }
    }

    @Override
    public List<Cookie> loadForRequest(HttpUrl url) {
        String urlString = url.toString();
        String cookiesString = webviewCookieManager.getCookie(urlString);

        if (cookiesString != null && !cookiesString.isEmpty()) {
            // We can split on the ';' char as the cookie manager only returns
            // cookies
            // that match the url and haven't expired, so the cookie attributes
            // aren't included
            String[] cookieHeaders = cookiesString.split(";");
            List<Cookie> cookies = new ArrayList<Cookie>(cookieHeaders.length);

            for (String header : cookieHeaders) {
                cookies.add(Cookie.parse(url, header));
            }

            return cookies;
        }

        return Collections.emptyList();
    }
}