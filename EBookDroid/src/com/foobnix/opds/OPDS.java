package com.foobnix.opds;

import java.io.IOException;
import java.io.StringReader;
import java.util.Random;
import java.util.concurrent.TimeUnit;

import org.xmlpull.v1.XmlPullParser;

import com.foobnix.android.utils.LOG;
import com.foobnix.android.utils.TxtUtils;
import com.foobnix.ext.CacheZipUtils;
import com.foobnix.ext.XmlParser;
import com.foobnix.sys.TempHolder;

import okhttp3.Cache;
import okhttp3.CacheControl;
import okhttp3.Credentials;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class OPDS {
    public static final String CODE_401 = "401";
    static Cache cache = new Cache(CacheZipUtils.CACHE_WEB, 5 * 1024 * 1024);
    public static OkHttpClient client = new OkHttpClient.Builder()//
            .cookieJar(new WebviewCookieHandler())//
            .connectTimeout(15, TimeUnit.SECONDS)//
            .writeTimeout(15, TimeUnit.SECONDS)//
            .readTimeout(15, TimeUnit.SECONDS)//
            .cache(cache).build();//

    public static int random = new Random().nextInt();
    public static final String USER_AGENT = "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/57.0.2987." + random + " Safari/537.36";

    public static String getHttpResponse(String url) throws IOException {

        Request request = new Request.Builder()//
                .header("User-Agent", USER_AGENT)

                .cacheControl(new CacheControl.Builder()//
                        .maxAge(10, TimeUnit.MINUTES)//
                        .build())//
                .url(url)//
                .build();//

        Response response = client//
                .newCall(request)//
                .execute();

        LOG.d("Header: >>", url);
        LOG.d("Header: Status code:", response.code());

        for (int i = 0; i < response.headers().size(); i++) {
            String name = response.headers().name(i);
            String value = response.headers().value(i);
            LOG.d("Header: ", name, value);
        }

        if (response.code() == 401 && TxtUtils.isEmpty(TempHolder.get().login)) {
            return CODE_401;
        } else {
            LOG.d("Header:", "try to login");
            String credential = Credentials.basic(TempHolder.get().login, TempHolder.get().password);
            request = response.request().newBuilder().header("Authorization", credential).build();
            response = client.newCall(request).execute();
            if (response.code() == 401) {
                return CODE_401;
            }
        }

        String string = response.body().string();
        return string;
    }

    public static Feed getFeed(String url) {
        try {
            if (url.endsWith(SamlibOPDS.LIBRERA_MOBI)) {
                return null;
            }
            String res = getHttpResponse(url);
            if (res.equals(CODE_401)) {
                Feed feed = new Feed();
                feed.isNeedLoginPassword = true;
                return feed;
            }
            return getFeedByXml(res);
        } catch (Exception e) {
            LOG.e(e);
        }
        return null;

    }

    public static Feed getFeedByXml(String xmlString) throws Exception {
        LOG.d(xmlString);

        XmlPullParser xpp = XmlParser.buildPullParser();
        xpp.setInput(new StringReader(xmlString));

        int eventType = xpp.getEventType();

        Feed feed = new Feed();

        Entry entry = null;

        boolean isEntry = false;
        boolean isAuthor = false;
        boolean isContent = false, isTitle = false;
        while (eventType != XmlPullParser.END_DOCUMENT) {
            if (eventType == XmlPullParser.START_TAG) {
                if (!isEntry) {
                    if ("title".equals(xpp.getName())) {
                        feed.title = xpp.nextText();
                    }
                    if ("subtitle".equals(xpp.getName())) {
                        feed.subtitle = xpp.nextText();
                    }
                    if ("updated".equals(xpp.getName())) {
                        feed.updated = xpp.nextText();
                    }
                    if ("icon".equals(xpp.getName())) {
                        feed.icon = xpp.nextText();
                    }
                    if ("link".equals(xpp.getName())) {
                        feed.links.add(new Link(xpp));
                    }
                }

                if ("entry".equals(xpp.getName())) {
                    entry = new Entry();
                    isEntry = true;
                }
                if ("author".equals(xpp.getName())) {
                    isAuthor = true;
                }
                if (isEntry && "content".equals(xpp.getName())) {
                    isContent = true;
                }
                if (isEntry && "title".equals(xpp.getName())) {
                    isTitle = true;
                }

                if (isEntry) {
                    if ("summary".equals(xpp.getName())) {
                        entry.summary = xpp.nextText();
                    }
                    if ("dc:issued".equals(xpp.getName())) {
                        entry.year = xpp.nextText();
                    }
                    if ("updated".equals(xpp.getName())) {
                        entry.updated = xpp.nextText();
                    }
                    if ("id".equals(xpp.getName())) {
                        entry.id = xpp.nextText();
                    }
                    if (isAuthor && "name".equals(xpp.getName())) {
                        entry.author = xpp.nextText();
                    }
                    if (isAuthor && "uri".equals(xpp.getName())) {
                        entry.authorUrl = xpp.nextText();
                    }
                    if ("category".equals(xpp.getName())) {
                        String label = xpp.getAttributeValue(null, "label");
                        String term = xpp.getAttributeValue(null, "term");
                        if (TxtUtils.isNotEmpty(label)) {
                            entry.category += ", " + label;
                        } else if (TxtUtils.isNotEmpty(term)) {
                            entry.category += ", " + term;
                        }
                    }

                    if ("link".equals(xpp.getName())) {
                        entry.links.add(new Link(xpp));
                    }
                }

            }
            if (eventType == XmlPullParser.TEXT) {
                if (isContent) {
                    String text = xpp.getText();
                    if (TxtUtils.isNotEmpty(text) && !text.equals("\n") && !text.equals("\r")) {
                        text = text.replace(":\n", ":").trim();
                        entry.content += text + "<br/>";
                    }
                }
                if (isTitle) {
                    entry.title += " " + xpp.getText();
                }
            }
            if (eventType == XmlPullParser.END_TAG) {
                if ("entry".equals(xpp.getName())) {
                    isEntry = false;
                    entry.category = TxtUtils.replaceFirst(entry.category, ", ", "");
                    feed.entries.add(entry);
                }
                if ("author".equals(xpp.getName())) {
                    isAuthor = false;
                }
                if ("content".equals(xpp.getName())) {
                    isContent = false;
                }
                if ("title".equals(xpp.getName())) {
                    isTitle = false;
                }
            }

            eventType = xpp.next();
        }
        return feed;

    }

}
