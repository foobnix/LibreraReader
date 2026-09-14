package com.foobnix.ext;

import com.foobnix.android.utils.LOG;
import com.foobnix.android.utils.StreamUtils;
import com.foobnix.android.utils.TxtUtils;
import com.foobnix.sys.ArchiveEntry;
import com.foobnix.sys.TempHolder;
import com.foobnix.sys.ZipArchiveInputStream;
import com.foobnix.sys.Zips;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.parser.Parser;
import org.jsoup.select.Elements;
import org.xmlpull.v1.XmlPullParser;

import java.io.FileInputStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * The footnote code of master c891f9867 before the rewrite, copied as it was to compare results and speed.
 * Only ExtUtils.getFileName is replaced by baseName, ExtUtils cannot be loaded in JVM tests.
 */
final class LegacyFooterNotes {

    private LegacyFooterNotes() {
    }

    static Map<String, String> epub(String inputPath) {

        LOG.d("getNotes getFooterNotes", inputPath);

        Map<String, String> notes = new HashMap<String, String>();
        try {
            ZipArchiveInputStream zipInputStream = Zips.buildZipArchiveInputStream(inputPath);

            ArchiveEntry nextEntry = null;
            Map<String, String> textLink = new HashMap<String, String>();
            Set<String> files = new HashSet<String>();

            try {
                // CacheZipUtils.removeFiles(CacheZipUtils.ATTACHMENTS_CACHE_DIR.listFiles());

                while ((nextEntry = zipInputStream.getNextEntry()) != null) {
                    if (TempHolder.get().loadingCancelled.get()) {
                        return new HashMap<String, String>();
                    }
                    String name = nextEntry.getName();
                    String nameLow = name.toLowerCase(Locale.US);
                    if (nameLow.endsWith("html") || nameLow.endsWith("htm") || nameLow.endsWith("xml")) {
                        // System.out.println("- " + nameLow + " -");
                        Document parse = Jsoup.parse(zipInputStream, null, "", Parser.xmlParser());
                        Elements select = parse.select("a[href]");

                        for (int i = 0; i < select.size(); i++) {
                            if (TempHolder.get().loadingCancelled.get()) {

                                return new HashMap<String, String>();
                            }
                            Element item = select.get(i);
                            String text = item.text();
                            if (item.attr("href").contains("#")) {
                                String attr = item.attr("href");
                                String file = attr.substring(0, attr.indexOf("#"));
                                // System.out.println(text + " -> " + attr + "
                                // [" +
                                // file);
                                if (attr.startsWith("#")) {
                                    attr = name + attr;
                                }
                                LOG.d("link-item-text", attr, text,"finished",TempHolder.get().loadingCancelled.get());
                                if (!TxtUtils.isFooterNote(text)) {
                                    LOG.d("Skip text", text);
                                    continue;
                                }

                                textLink.put(attr, text + "#" + name);
                                LOG.d("put links >>", attr, text + "#" + name);

                                LOG.d("Extract file", file);
                                if (TxtUtils.isEmpty(file)) {
                                    file = name;
                                }

                                if (file.endsWith("html") || file.endsWith("htm") || nameLow.endsWith("xml")) {
                                    files.add(file);
                                }
                            }
                        }


                    }
                }

                zipInputStream.release();
                zipInputStream = Zips.buildZipArchiveInputStream(inputPath);

                while ((nextEntry = zipInputStream.getNextEntry()) != null) {
                    if (TempHolder.get().loadingCancelled.get()) {
                        return new HashMap<String, String>();
                    }
                    String name = nextEntry.getName();
                    for (String fileName : files) {
                        if (TempHolder.get().loadingCancelled.get()) {
                            return new HashMap<String, String>();
                        }
                        LOG.d("PARSE FILE NAME begin", name);
                        if (baseName(name).endsWith(baseName(fileName))) {
                            LOG.d("PARSE FILE NAME", name);
                            // System.out.println("file: " + name);
                            Parser xmlParser = Parser.xmlParser();
                            Document parse = Jsoup.parse(zipInputStream, null, "", xmlParser);

                            Elements ids = parse.select("[id]");
                            for (int i = 0; i < ids.size(); i++) {
                                if (TempHolder.get().loadingCancelled.get()) {
                                    return new HashMap<String, String>();
                                }
                                Element item = ids.get(i);
                                String id = item.attr("id");

                                String fileKey = fileName + "#" + id;

                                String textKey = textLink.get(fileKey);
                                if (textKey == null) {
                                    LOG.d("skip #id", fileKey);
                                    continue;
                                }

                                String value = item.text();

                                int min = 20;
                                if (value.trim().length() < min) {
                                    value = value + " " + parse.select("[id=" + id + "]+*").text();
                                }
                                if (value.trim().length() < min) {
                                    value = value + " " + parse.select("[id=" + id + "]+*+*").text();
                                }
                                try {
                                    if (value.trim().length() < min) {
                                        value = value + " " + parse.select("[id=" + id + "]").parents().get(0).text();
                                    }
                                } catch (Exception e) {
                                    LOG.e(e);
                                }


                                LOG.d("put text >>", TempHolder.get().loadingCancelled.get(), textKey, value);
                                notes.put(textKey, value.trim());


                            }

                        }

                    }
                }

                zipInputStream.release();
            } catch (Exception e) {
                LOG.e(e);
            }

            return notes;
        } catch (Throwable e) {
            LOG.e(e);
            return notes;
        }
    }

    static Map<String, String> fb2(String inputFile) {
        Map<String, String> map = new HashMap<String, String>();
        try {

            XmlPullParser xpp = XmlParser.buildPullParser();
            final FileInputStream inputStream = new FileInputStream(inputFile);
            xpp.setInput(inputStream, Fb2Extractor.findHeaderEncoding(inputFile));
            int eventType = xpp.getEventType();

            String sectionId = null;
            StringBuilder text = null;
            boolean isLink = false;
            String link = null;
            String key = "";

            while (eventType != XmlPullParser.END_DOCUMENT) {
                if (TempHolder.get().loadingCancelled.get()) {
                    break;
                }
                if (eventType == XmlPullParser.START_TAG) {
                    if (xpp.getName().equals("a")) {
                        // String type = xpp.getAttributeValue(null, "type");
                        // if ("note".equals(type)) {
                        isLink = true;

                        link = xpp.getAttributeValue(null, "l:href");
                        if (link == null) {
                            link = xpp.getAttributeValue(null, "xlink:href");
                        }

                        // }
                    } else if (xpp.getName().equals("section")) {
                        sectionId = xpp.getAttributeValue(null, "id");
                        text = new StringBuilder();
                    }
                } else if (eventType == XmlPullParser.TEXT) {
                    if (sectionId != null) {
                        String trim = xpp.getText().trim();
                        if (trim.length() > 0) {
                            text.append(trim + " ");
                        }
                    }
                    if (isLink) {
                        key = key + " " + xpp.getText();
                        LOG.d("key", key);
                    }
                } else if (eventType == XmlPullParser.END_TAG) {
                    if (sectionId != null && xpp.getName().equals("section")) {
                        String keyEnd = StreamUtils.getKeyByValue(map, sectionId);

                        map.put(keyEnd, text.toString().trim());//1
                        keyEnd = keyEnd + "#OEBPS/fb2.fb2";
                        map.put(keyEnd, text.toString().trim());//2

                        LOG.d("getFooterNotes-section", sectionId, keyEnd, ">", text.toString());
                        LOG.d("getFooterNotesFb2-section", keyEnd, text.toString().trim());
                        sectionId = null;
                        text = null;
                    } else if (xpp.getName().equals("a")) {

                        if (isLink && link != null) {
                            key = key.trim();
                            if (!TxtUtils.isFooterNote(key)) {
                                key = "[" + link + "]";
                            }
                            link = link.replace("#", "");
                            map.put(key, link.trim());
                            LOG.d("getFooterNotes-link", key, ">", link);
                            LOG.d("getFooterNotesFb2-link", key, link);


                            key = "";
                        }
                        if (isLink) {
                            isLink = false;
                        }
                    }
                }

                eventType = xpp.next();
            }
            inputStream.close();
        } catch (Exception e) {
            LOG.e(e);
        }
        return map;
    }

    static String includeFooterNotes(String line, Map<String, String> notes, String name) {
        if (notes == null) {
            return line;
        }

        int beginIndex = -1;
        int endIndex = -1;
        StringBuffer out = new StringBuffer();

        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c == '[' || c == '{') {
                beginIndex = i;
            }
            if (c == ']' || c == '}') {
                endIndex = i;
            }
            out.append(c);

            if (beginIndex > 0 && endIndex > beginIndex && endIndex - beginIndex < 6) {
                String number = line.substring(beginIndex, endIndex + 1);
                beginIndex = -1;
                endIndex = -1;

                int end = line.indexOf('>', i);
                int k = end - i;
                if (end > i && k < 8) {
                    out.append(line.substring(i + 1, end + 1));
                    i += k;
                }

                LOG.d("includeFooterNotes", number, number + "#" + name);

                String value = notes.get(number + "#" + name);
                if (value != null) {
                    value = value.replace(TxtUtils.NON_BREAKE_SPACE, " ").trim();
                    value = value.replaceAll("^[\\[{][0-9]+[\\]}]", "").trim();
                    value = value.replaceAll("^[\\[{][0-9]+[\\]}]", "").trim();// two times!
                    value = value.replaceAll("^[0-9]+", "").trim();

                    out.append(" <t>[");
                    out.append(TxtUtils.escapeHtml(value));
                    out.append("]</t>");
                }
            }

        }
        return out.toString();
    }

    private static String baseName(String name) {
        if (TxtUtils.isEmpty(name)) {
            return "";
        }
        if (!name.contains("/")) {
            return name;
        }
        return name.substring(name.lastIndexOf("/") + 1);
    }
}
