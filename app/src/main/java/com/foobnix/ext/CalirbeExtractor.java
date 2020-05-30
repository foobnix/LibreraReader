package com.foobnix.ext;

import com.BaseExtractor;
import com.foobnix.android.utils.LOG;
import com.foobnix.android.utils.TxtUtils;
import com.foobnix.pdf.info.ExtUtils;

import org.jsoup.Jsoup;
import org.jsoup.safety.Whitelist;
import org.librera.JSONArray;
import org.librera.LinkedJSONObject;
import org.xmlpull.v1.XmlPullParser;

import java.io.File;
import java.io.FileInputStream;

public class CalirbeExtractor {

    public static boolean isCalibre(String path) {
        return getCalibreOPF(path) != null;
    }

    public static File getCalibreOPF(String path) {
        File rootFolder = new File(path).getParentFile();
        File metadata = new File(rootFolder, "metadata.opf");
        if (!metadata.isFile()) {
            metadata = new File(rootFolder, ExtUtils.getFileName(ExtUtils.getFileNameWithoutExt(path)) + ".opf");
        }
        return metadata.isFile() ? metadata : null;

    }

    public static String getBookOverview(String path) {
        try {

            File metadata = getCalibreOPF(path);
            if (metadata==null || !metadata.isFile()) {
                return null;
            }

            XmlPullParser xpp = XmlParser.buildPullParser();
            xpp.setInput(new FileInputStream(metadata), "UTF-8");

            int eventType = xpp.getEventType();

            while (eventType != XmlPullParser.END_DOCUMENT) {
                if (eventType == XmlPullParser.START_TAG) {
                    if ("dc:description".equals(xpp.getName()) || "dcns:description".equals(xpp.getName())) {
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

    public static String add(String value, String div, String value2) {
        return TxtUtils.isEmpty(value) ? value2 : value + div + value2;
    }

    public static EbookMeta getBookMetaInformation(String path) {
        EbookMeta meta = EbookMeta.Empty();
        try {


            File metadata = getCalibreOPF(path);

            if (metadata ==null || !metadata.isFile()) {
                return null;
            }

            XmlPullParser xpp = XmlParser.buildPullParser();
            final FileInputStream inputStream = new FileInputStream(metadata);
            xpp.setInput(inputStream, "UTF-8");

            int eventType = xpp.getEventType();

            while (eventType != XmlPullParser.END_DOCUMENT) {
                if (eventType == XmlPullParser.START_TAG) {
                    if ("dc:title".equals(xpp.getName()) || "dcns:title".equals(xpp.getName())) {
                        meta.setTitle(add(meta.getTitle(), " - ", xpp.nextText()));
                    }

                    if ("dc:creator".equals(xpp.getName()) || "dcns:creator".equals(xpp.getName())) {
                        meta.setAuthor(add(meta.getAuthor(), ", ", xpp.nextText()));
                    }

                    if ("dc:date".equals(xpp.getName()) || "dcns:date".equals(xpp.getName())) {
                        if (meta.getYear() != null && xpp.getAttributeCount() == 0) {
                            meta.setYear(xpp.nextText());
                        } else if (meta.getYear() == null) {
                            meta.setYear(xpp.nextText());
                        }
                    }

                    if ("dc:subject".equals(xpp.getName()) || "dcns:subject".equals(xpp.getName())) {
                        meta.setGenre(add(meta.getGenre(), ", ", xpp.nextText()));

                    }

                    if ("dc:publisher".equals(xpp.getName()) || "dcns:publisher".equals(xpp.getName())) {
                        meta.setPublisher(xpp.nextText());
                    }

                    if ("dc:identifier".equals(xpp.getName()) || "dcns:identifier".equals(xpp.getName())) {
                        meta.setIsbn(add(meta.getIsbn(), ", ", xpp.nextText()));

                    }

                    if (meta.getLang() == null && ("dc:language".equals(xpp.getName()) || "dcns:language".equals(xpp.getName()))) {
                        meta.setLang(xpp.nextText());
                    }

                    if ("meta".equals(xpp.getName())) {
                        String attrName = xpp.getAttributeValue(null, "name");
                        String attrContent = xpp.getAttributeValue(null, "content");

                        if(TxtUtils.isNotEmpty(attrContent)) {
                            if ("calibre:series".equals(attrName)) {
                                meta.setSequence(attrContent.replace(",", ""));
                            }

                            if ("calibre:series_index".equals(attrName)) {
                                try {
                                    if (attrContent.contains(".")) {
                                        meta.setsIndex((int) Float.parseFloat(attrContent));
                                    } else {
                                        meta.setsIndex(Integer.parseInt(attrContent));
                                    }
                                } catch (Exception e) {
                                    LOG.e(e);
                                }
                            }
                        }

                        if ("calibre:user_metadata:#genre".equals(attrName)) {
                            LOG.d("userGenre", attrContent);
                            try {
                                LinkedJSONObject obj = new LinkedJSONObject(attrContent);
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
                        if ("librera:user_metadata:#genre".equals(attrName)) {
                            LOG.d("librera-userGenre", attrContent);
                            try {
                                meta.setGenre(attrContent);
                            } catch (Exception e) {
                                LOG.e(e);
                            }
                        }

                    }

                    if ("reference".equals(xpp.getName()) && "cover".equals(xpp.getAttributeValue(null, "type"))) {
                        try {
                            String imgName = xpp.getAttributeValue(null, "href");
                            File rootFolder = new File(path).getParentFile();
                            final File img = new File(rootFolder, imgName);
                            FileInputStream fileStream = new FileInputStream(img);
                            meta.coverImage = BaseExtractor.getEntryAsByte(fileStream);
                            LOG.d("reference-img", img.getPath(), img.isFile());
                            fileStream.close();
                        } catch (Exception e) {
                            LOG.e(e);
                        }

                    }

                }
                eventType = xpp.next();
            }
            inputStream.close();
        } catch (Exception e) {
            LOG.e(e);
        }

        return meta;

    }
}
