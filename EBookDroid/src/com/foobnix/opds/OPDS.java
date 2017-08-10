package com.foobnix.opds;

import java.io.IOException;
import java.io.StringReader;
import java.util.concurrent.TimeUnit;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import com.foobnix.android.utils.LOG;
import com.foobnix.ext.CacheZipUtils;

import okhttp3.Cache;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class OPDS {
    static Cache cache = new Cache(CacheZipUtils.CACHE_WEB, 10 * 1024 * 1024);
    public static OkHttpClient client = new OkHttpClient.Builder()//
            .connectTimeout(15, TimeUnit.SECONDS)//
            .writeTimeout(15, TimeUnit.SECONDS)//
            .readTimeout(30, TimeUnit.SECONDS)//
            .cache(cache).build();//

    public static String getHttpUrl(String url) throws IOException {
        Request request = new Request.Builder()//
                .url(url)//
                .build();//

        Response response = client//
                .newCall(request)//
                .execute();
        String string = response.body().string();
        return string;
    }

    public static Feed getFeed(String url) {
        try {
            return getFeedByXml(getHttpUrl(url));
        } catch (Exception e) {
            LOG.e(e);
        }
        return null;

    }

    public static Feed getFeedByXml(String xmlString) throws Exception {
        LOG.d(xmlString);

        XmlPullParser xpp = buildPullParser();
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
                    if ("updated".equals(xpp.getName())) {
                        entry.updated = xpp.nextText();
                    }
                    if ("id".equals(xpp.getName())) {
                        entry.id = xpp.nextText();
                    }
                    if (isAuthor && "name".equals(xpp.getName())) {
                        entry.author = xpp.nextText();
                    }
                    if ("category".equals(xpp.getName())) {
                        entry.category = entry.category + " " + xpp.getAttributeValue(null, "term");
                    }

                    if ("link".equals(xpp.getName())) {
                        entry.links.add(new Link(xpp));
                    }
                }

            }
            if (eventType == XmlPullParser.TEXT) {
                if (isContent) {
                    entry.content += xpp.getText();
                }
                if (isTitle) {
                    entry.title += xpp.getText();
                }
            }
            if (eventType == XmlPullParser.END_TAG) {
                if ("entry".equals(xpp.getName())) {
                    isEntry = false;
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

    public static XmlPullParser buildPullParser() throws XmlPullParserException {
        XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
        factory.setValidating(false);
        return factory.newPullParser();
    }

}
