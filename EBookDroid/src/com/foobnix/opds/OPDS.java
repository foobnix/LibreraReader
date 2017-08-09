package com.foobnix.opds;

import java.io.IOException;
import java.io.StringReader;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class OPDS {

    public static String getHttpUrl(String url) throws IOException {
        OkHttpClient client = new OkHttpClient();

        Request request = new Request.Builder().url(url).build();
        Response response = client.newCall(request).execute();
        String string = response.body().string();
        return string;
    }

    public static Feed getFeed(String url) {
        try {
            return getFeedByXml(getHttpUrl(url));
        } catch (Exception e) {
        }
        return null;

    }

    public static Feed getFeedByXml(String xmlString) throws Exception {

        XmlPullParser xpp = buildPullParser();
        xpp.setInput(new StringReader(xmlString));

        int eventType = xpp.getEventType();

        Feed feed = new Feed();

        Entry entry = null;

        boolean isEntry = false;
        while (eventType != XmlPullParser.END_DOCUMENT) {
            if (eventType == XmlPullParser.START_TAG) {
                if (!isEntry) {
                    if ("title".equals(xpp.getName())) {
                        feed.title = xpp.nextText();
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

                if (isEntry) {
                    if ("updated".equals(xpp.getName())) {
                        entry.updated = xpp.nextText();
                    }
                    if ("id".equals(xpp.getName())) {
                        entry.id = xpp.nextText();
                    }
                    if ("title".equals(xpp.getName())) {
                        entry.title = xpp.nextText();
                    }
                    if ("content".equals(xpp.getName())) {
                        entry.content = xpp.nextText();
                    }
                    if ("link".equals(xpp.getName())) {
                        entry.links.add(new Link(xpp));
                    }
                }

            }
            if (eventType == XmlPullParser.END_TAG) {
                if ("entry".equals(xpp.getName())) {
                    isEntry = false;
                    feed.entries.add(entry);
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
