package com.foobnix.ext;

import com.BaseExtractor;
import com.foobnix.android.utils.LOG;
import com.foobnix.android.utils.TxtUtils;
import com.foobnix.pdf.info.ExtUtils;

import org.jsoup.Jsoup;
import org.jsoup.safety.Safelist;
import org.librera.JSONArray;
import org.librera.JSONException;
import org.librera.LinkedJSONObject;
import org.xmlpull.v1.XmlPullParser;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class CalirbeExtractor {

    public interface CoverResolver {
        byte[] resolve(String href);
    }

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
        return getBookOverviewFromOpf(getCalibreOPF(path));
    }

    public static String getBookOverviewFromOpf(File metadata) {
        if (metadata == null || !metadata.isFile()) {
            return null;
        }
        try (FileInputStream is = new FileInputStream(metadata)) {
            return getBookOverviewFromStream(is);
        } catch (Exception e) {
            LOG.e(e);
            return null;
        }
    }

    public static String getBookOverviewFromStream(InputStream stream) {
        try {
            XmlPullParser xpp = XmlParser.buildPullParser();
            xpp.setInput(stream, "UTF-8");

            int eventType = xpp.getEventType();

            while (eventType != XmlPullParser.END_DOCUMENT) {
                if (eventType == XmlPullParser.START_TAG) {
                    if ("dc:description".equals(xpp.getName()) || "dcns:description".equals(xpp.getName())) {
                        return Jsoup.clean(xpp.nextText(), Safelist.simpleText());
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
        if(TxtUtils.isNotEmpty(value) && value.contains(div+value2)){
            return value;
        }
        return TxtUtils.isEmpty(value) ? value2 : value + div + value2;
    }

    public static EbookMeta getBookMetaInformation(String path) {
        return getBookMetaInformationFromOpf(getCalibreOPF(path));
    }

    public static EbookMeta getBookMetaInformationFromOpf(File metadata) {
        if (metadata == null || !metadata.isFile()) {
            return null;
        }
        final File rootFolder = metadata.getParentFile();
        CoverResolver coverResolver = href -> {
            try (FileInputStream fs = new FileInputStream(new File(rootFolder, href))) {
                return BaseExtractor.getEntryAsByte(fs);
            } catch (Exception e) {
                LOG.e(e);
                return null;
            }
        };
        try (FileInputStream is = new FileInputStream(metadata)) {
            return getBookMetaInformationFromStream(is, coverResolver);
        } catch (Exception e) {
            LOG.e(e);
            return null;
        }
    }

    public static EbookMeta getBookMetaInformationFromStream(InputStream stream, CoverResolver coverResolver) {
        EbookMeta meta = EbookMeta.Empty();
        try {
            XmlPullParser xpp = XmlParser.buildPullParser();
            xpp.setInput(stream, "UTF-8");

            List<String[]> dates = new ArrayList<>();
            int eventType = xpp.getEventType();
            String rootName = null;
            boolean closedRoot = false;

            while (eventType != XmlPullParser.END_DOCUMENT) {
                if (eventType == XmlPullParser.START_TAG) {
                    if (rootName == null) rootName = xpp.getName();
                    if ("dc:title".equals(xpp.getName()) || "dcns:title".equals(xpp.getName())) {
                        meta.setTitle(add(meta.getTitle(), " - ", xpp.nextText()));
                    }

                    if ("dc:creator".equals(xpp.getName()) || "dcns:creator".equals(xpp.getName())) {
                        String author =  xpp.nextText();
                        meta.setAuthor(add(meta.getAuthor(), ", ",author));
                    }

                    if ("dc:date".equals(xpp.getName()) || "dcns:date".equals(xpp.getName())) {
                        String event = PubDate.event(xpp);
                        dates.add(new String[]{event, xpp.nextText()});
                    }

                    if ("dc:subject".equals(xpp.getName()) || "dcns:subject".equals(xpp.getName())) {
                        meta.setGenre(add(meta.getGenre(), ", ", xpp.nextText()));

                    }

                    if ("dc:publisher".equals(xpp.getName()) || "dcns:publisher".equals(xpp.getName())) {
                        meta.setPublisher(xpp.nextText());
                    }

                    if ("dc:identifier".equals(xpp.getName()) || "dcns:identifier".equals(xpp.getName())) {
                        String id = xpp.getAttributeValue(null,"opf:scheme");
                        if(TxtUtils.isNotEmpty(id)){
                            meta.setIsbn(add(meta.getIsbn(), ", ",id +": " +xpp.nextText()));
                        }else {
                            meta.setIsbn(add(meta.getIsbn(), ", ",xpp.nextText()));
                        }

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
                                try {
                                    String ge = obj.getString("#value#");

                                    meta.setGenre(add(meta.getGenre(), ", ", ge));

                                } catch (JSONException e) {
                                    JSONArray jsonArray = obj.getJSONArray("#value#");
                                    String res = "";
                                    for (int i = 0; i < jsonArray.length(); i++) {
                                        res = res + "," + jsonArray.getString(i);
                                    }
                                    String ge1 = TxtUtils.replaceFirst(res, ",", "");

                                    meta.setGenre(add(meta.getGenre(), ", ", ge1));
                                }
                            } catch (Exception e) {
                                LOG.e(e);
                            }
                        }
                        if ("calibre:user_metadata:#keywords".equals(attrName) || "calibre:user_metadata:#keyword".equals(attrName)) {
                            LOG.d("user keywords", attrContent);
                            try {
                                LinkedJSONObject obj = new LinkedJSONObject(attrContent);
                                try {
                                    String ge = obj.getString("#value#");

                                    meta.setKeywords(add(meta.getKeywords(), ", ", ge));

                                } catch (JSONException e) {
                                    JSONArray jsonArray = obj.getJSONArray("#value#");
                                    String res = "";
                                    for (int i = 0; i < jsonArray.length(); i++) {
                                        res = res + "," + jsonArray.getString(i);
                                    }
                                    String ge1 = TxtUtils.replaceFirst(res, ",", "");

                                    meta.setKeywords(add(meta.getKeywords(), ", ", ge1));
                                }
                            } catch (Exception e) {
                                LOG.e(e);
                            }
                        }

                        if ("librera:user_metadata:#genre".equals(attrName)) {
                            LOG.d("librera-userGenre", attrContent);
                            try {

                                meta.setGenre(add(meta.getGenre(), ", ", attrContent));
                            } catch (Exception e) {
                                LOG.e(e);
                            }
                        }

                    }

                    if ("reference".equals(xpp.getName()) && "cover".equals(xpp.getAttributeValue(null, "type"))) {
                        if (coverResolver != null) {
                            try {
                                String imgName = xpp.getAttributeValue(null, "href");
                                meta.coverImage = coverResolver.resolve(imgName);
                                LOG.d("reference-img", imgName, meta.coverImage != null);
                            } catch (Exception e) {
                                LOG.e(e);
                            }
                        }
                    }

                }
                if (eventType == XmlPullParser.END_TAG && xpp.getDepth() == 1
                        && xpp.getName().equals(rootName)) closedRoot = true;
                eventType = xpp.next();
            }
            if (!closedRoot) meta.markExtractionFailed();
            meta.setYear(PubDate.published(dates));

        } catch (Exception e) {
            LOG.e(e);
            meta.markExtractionFailed();
        }

        return meta;

    }
}
