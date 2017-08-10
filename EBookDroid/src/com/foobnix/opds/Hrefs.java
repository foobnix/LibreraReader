package com.foobnix.opds;

import com.foobnix.android.utils.TxtUtils;

public class Hrefs {

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
        return url;
    }

}
