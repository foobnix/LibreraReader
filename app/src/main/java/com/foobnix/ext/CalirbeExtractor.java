package com.foobnix.ext;

import java.io.File;
import java.io.FileInputStream;

import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.safety.Whitelist;
import org.xmlpull.v1.XmlPullParser;

import com.BaseExtractor;
import com.foobnix.android.utils.LOG;
import com.foobnix.android.utils.TxtUtils;

public class CalirbeExtractor {

    public static boolean isCalibre(String path) {
        File rootFolder = new File(path).getParentFile();
        File metadata = new File(rootFolder, "metadata.opf");
        return metadata.isFile();
    }

    public static String getBookOverview(String path) {
        try {

            File rootFolder = new File(path).getParentFile();
            File metadata = new File(rootFolder, "metadata.opf");
            if (!metadata.isFile()) {
                return null;
            }

            XmlPullParser xpp = XmlParser.buildPullParser();
            xpp.setInput(new FileInputStream(metadata), "UTF-8");

            int eventType = xpp.getEventType();

            while (eventType != XmlPullParser.END_DOCUMENT) {
                if (eventType == XmlPullParser.START_TAG) {
                    if ("dc:description".equals(xpp.getName())) {
                        return Jsoup.clean(xpp.nextText(), Whitelist.simpleText());
                    }
                }
                eventType = xpp.next();
            }
        } catch (Exception e) {
            LOG.e(e);
        }
        return "";
    }

    public static EbookMeta getBookMetaInformation(String path) {
        EbookMeta meta = EbookMeta.Empty();
        try {

            File rootFolder = new File(path).getParentFile();
            File metadata = new File(rootFolder, "metadata.opf");
            if (!metadata.isFile()) {
                return null;
            }

            XmlPullParser xpp = XmlParser.buildPullParser();
            xpp.setInput(new FileInputStream(metadata), "UTF-8");

            int eventType = xpp.getEventType();

            while (eventType != XmlPullParser.END_DOCUMENT) {
                if (eventType == XmlPullParser.START_TAG) {
                    if ("dc:title".equals(xpp.getName())) {
                        meta.setTitle(xpp.nextText());
                    }

                    if ("dc:creator".equals(xpp.getName())) {
                        String author = xpp.nextText();
                        if (TxtUtils.isNotEmpty(meta.getAuthor())) {
                            meta.setAuthor(meta.getAuthor() + ", " + author);
                        } else {
                            meta.setAuthor(author);
                        }
                    }

                    if ("dc:description".equals(xpp.getName())) {
                        meta.setAnnotation(xpp.nextText());
                    }

                    if ("meta".equals(xpp.getName())) {
                        String attrName = xpp.getAttributeValue(null, "name");
                        String attrContent = xpp.getAttributeValue(null, "content");

                        if ("calibre:series".equals(attrName)) {
                            meta.setSequence(attrContent.replace(",", ""));
                        }

                        if ("calibre:series_index".equals(attrName)) {
                            meta.setsIndex(Integer.parseInt(attrContent));
                        }

                        if ("calibre:user_metadata:#genre".equals(attrName)) {
                            LOG.d("userGenre", attrContent);
                            try {
                                JSONObject obj = new JSONObject(attrContent);
                                JSONArray jsonArray = obj.getJSONArray("#value#");
                                String res = "";
                                for (int i = 0; i < jsonArray.length(); i++) {
                                    res = res + "," + jsonArray.getString(i);
                                }
                                res = TxtUtils.replaceFirst(res, ",", "");
                                meta.setGenre(res);
                                LOG.d("userGenre-list", res);
                            } catch (Exception e) {
                                LOG.e(e);
                            }
                        }

                    }

                    if ("reference".equals(xpp.getName()) && "cover".equals(xpp.getAttributeValue(null, "type"))) {
                        try {
                            String imgName = xpp.getAttributeValue(null, "href");
                            FileInputStream fileStream = new FileInputStream(new File(rootFolder, imgName));
                            meta.coverImage = BaseExtractor.getEntryAsByte(fileStream);
                            fileStream.close();
                        } catch (Exception e) {
                            LOG.e(e);
                        }

                    }

                }
                eventType = xpp.next();
            }
        } catch (Exception e) {
            LOG.e(e);
        }

        return meta;

    }
}
