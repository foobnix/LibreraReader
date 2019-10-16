package com.foobnix.ext;

import com.foobnix.android.utils.LOG;
import com.foobnix.android.utils.TxtUtils;
import com.foobnix.sys.ArchiveEntry;
import com.foobnix.sys.ZipArchiveInputStream;
import com.foobnix.sys.Zips;

import org.xmlpull.v1.XmlPullParser;

import java.util.Locale;

public class DocxExtractor {


    public static String getLang(String path) {
        try {
            LOG.d("docx-path", path);
            ZipArchiveInputStream zipInputStream = Zips.buildZipArchiveInputStream(path);

            ArchiveEntry nextEntry = null;

            if(false) {
                while ((nextEntry = zipInputStream.getNextEntry()) != null) {
                    String name = nextEntry.getName().toLowerCase(Locale.US);

                    if (name.endsWith("document.xml")) {

                        XmlPullParser xpp = XmlParser.buildPullParser();
                        xpp.setInput(zipInputStream, "utf-8");

                        int eventType = xpp.getEventType();

                        while (eventType != XmlPullParser.END_DOCUMENT) {
                            if (eventType == XmlPullParser.START_TAG) {
                                //LOG.d("docx-tag", xpp.getName());
                                if ("w:lang".equals(xpp.getName())) {

                                    String lang = xpp.getAttributeValue(null, "w:eastAsia");
                                    if (TxtUtils.isEmpty(lang)) {
                                        lang = xpp.getAttributeValue(null, "w:val");
                                    }
                                    LOG.d("docx-lang", lang);
                                    zipInputStream.close();
                                    return lang;
                                }
                            }
                            eventType = xpp.next();
                        }
                    }
                }

                zipInputStream.close();
                zipInputStream = Zips.buildZipArchiveInputStream(path);
            }

            while ((nextEntry = zipInputStream.getNextEntry()) != null) {
                String name = nextEntry.getName().toLowerCase(Locale.US);

                if (name.endsWith("styles.xml")) {

                    XmlPullParser xpp = XmlParser.buildPullParser();
                    xpp.setInput(zipInputStream, "utf-8");

                    int eventType = xpp.getEventType();

                    boolean isFind = false;

                    while (eventType != XmlPullParser.END_DOCUMENT) {
                        if (eventType == XmlPullParser.START_TAG) {
                            //LOG.d("docx-tag", xpp.getName());
                            if("w:style".equals(xpp.getName())){
                                isFind = true;
                                eventType = xpp.next();
                                continue;

                            }

                            if (isFind && "w:lang".equals(xpp.getName())) {



                                String lang = xpp.getAttributeValue(null, "w:val");
                                if (TxtUtils.isEmpty(lang)) {
                                    lang = xpp.getAttributeValue(null, "w:eastAsia");
                                }

                                LOG.d("docx-lang", lang);
                                zipInputStream.close();
                                return lang;
                            }
                        }
                        eventType = xpp.next();
                    }
                }
            }

            zipInputStream.close();

        } catch (Exception e) {
            LOG.e(e);
            return "";
        }
        return "";
    }
}



