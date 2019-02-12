package com.foobnix.opds;

import com.foobnix.android.utils.TxtUtils;

public class Hrefs {

    private static final String LITRES = "";// "lfrom=325404530";

    public static void fixHref(Link l, String homeUrl) {
        if (l.href.startsWith("data:image")) {
            return;
        }

        if (l.href.startsWith("//")) {
            l.href = "http:" + l.href;
        }

        if (!l.href.startsWith("http")) {
            if (l.href.startsWith("/")) {
                l.href = TxtUtils.getHostUrl(homeUrl) + l.href;
            } else {
                l.href = TxtUtils.getHostLongUrl(homeUrl) + "/" + l.href;
            }
        }

        if (l.href.contains("litres.ru")) {
            l.href = l.href + (l.href.contains("?") ? "&" : "?") + LITRES;

        }
    }

    public static String fixHref(String url, String homeUrl) {
        if (url.startsWith("data:image")) {
            return url;
        }


        if (url.startsWith("//")) {
            url = "http:" + url;
        }

        if (!url.startsWith("http")) {
            if (url.startsWith("/")) {
                url = TxtUtils.getHostUrl(homeUrl) + url;
            } else {
                url = TxtUtils.getHostLongUrl(homeUrl) + "/" + url;
            }
        }

        if (url.contains("litres.ru")) {
            url = url + (url.contains("?") ? "&" : "?") + LITRES;

        }

        return url;
    }

}
