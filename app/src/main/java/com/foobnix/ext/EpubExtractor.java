package com.foobnix.ext;

import com.BaseExtractor;
import com.foobnix.android.utils.LOG;
import com.foobnix.android.utils.TxtUtils;
import com.foobnix.android.utils.WebViewUtils;
import com.foobnix.hypen.HypenUtils;
import com.foobnix.model.AppSP;
import com.foobnix.model.AppState;
import com.foobnix.pdf.info.ExtUtils;
import com.foobnix.sys.ArchiveEntry;
import com.foobnix.sys.TempHolder;
import com.foobnix.sys.ZipArchiveInputStream;
import com.foobnix.sys.Zips;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.parser.Parser;
import org.jsoup.select.Elements;
import org.librera.JSONArray;
import org.librera.LinkedJSONObject;
import org.xmlpull.v1.XmlPullParser;

import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

public class EpubExtractor extends BaseExtractor {

    final static EpubExtractor inst = new EpubExtractor();

    private EpubExtractor() {

    }

    public static EpubExtractor get() {
        return inst;
    }

    public static void proccessHypens(String input, String output, Map<String, String> notes) {
        try {
            // proccessHypensDefault(input, output);
            LOG.d("proccessHypens begin");
            proccessHypensApache(input, output, notes);
            LOG.d("proccessHypens end");
        } catch (Exception e) {
            LOG.e(e);
            try {

            } catch (Exception e1) {
                LOG.e(e1);
            }
        }

    }

    public static void proccessHypensApache(String input, String output, final Map<String, String> notes) throws Exception {

        LOG.d("proccessHypens2", input, output);

        ZipArchiveInputStream zipInputStream = Zips.buildZipArchiveInputStream(input);
        ArchiveEntry nextEntry = null;

        ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(new File(output)));
        zos.setLevel(0);

        HypenUtils.applyLanguage(AppSP.get().hypenLang);

        Map<String, String> svgs = new HashMap<>();

        List<String> spine = new ArrayList<>();
        Map<String, String> manifest = new HashMap<>();
        if (AppState.get().isReferenceMode) {
            while ((nextEntry = zipInputStream.getNextEntry()) != null) {
                String name = nextEntry.getName().toLowerCase(Locale.US);
                if (name.endsWith(".opf")) {

                    XmlPullParser xpp = XmlParser.buildPullParser();
                    xpp.setInput(zipInputStream, "utf-8");

                    int eventType = xpp.getEventType();

                    while (eventType != XmlPullParser.END_DOCUMENT) {
                        if (eventType == XmlPullParser.START_TAG) {
                            if ("item".equals(xpp.getName())) {
                                String id = xpp.getAttributeValue(null, "id");
                                String href = xpp.getAttributeValue(null, "href");
                                String nav = xpp.getAttributeValue(null, "properties");

                                manifest.put(href, id);
                                LOG.d("isReferenceMode-manifest", id, href);

                            } else if ("itemref".equals(xpp.getName())) {
                                final String idref = xpp.getAttributeValue(null, "idref");
                                final String linear = xpp.getAttributeValue(null, "linear");
                                if ("no".equals(linear)) {
                                    LOG.d("isReferenceMode-itemref skip", idref);
                                } else {
                                    spine.add(idref);
                                }
                                LOG.d("isReferenceMode-itemref", idref);
                            }
                        }
                        eventType = xpp.next();
                    }
                }
            }
            zipInputStream.close();
            zipInputStream = Zips.buildZipArchiveInputStream(input);
        }

        while ((nextEntry = zipInputStream.getNextEntry()) != null) {
            if (TempHolder.get().loadingCancelled) {
                break;
            }
            String name = nextEntry.getName();
            String nameLow = name.toLowerCase(Locale.US);

            if (nameLow.contains("encryption.xml") || //
                    nameLow.contains("container.xml") || //
                    nameLow.contains("nav") || //
                    nameLow.contains("toc")//
            ) {
                LOG.d("nextEntry HTML skip", name);
                Fb2Extractor.writeToZipNoClose(zos, name, zipInputStream);
                continue;
            }

            if (nameLow.endsWith("html") || nameLow.endsWith("htm") || nameLow.endsWith("xml")) {

                int count = 0;
                if (AppState.get().isReferenceMode) {
                    String ch = "";
                    for (String key : manifest.keySet()) {
                        if (name.contains(key)) {
                            ch = manifest.get(key);
                            break;
                        }
                    }

                    count = spine.indexOf(ch) + 1;
                    LOG.d("isReferenceMode ok", name, ch, count);
                }

                ByteArrayOutputStream hStream = new ByteArrayOutputStream();

                Fb2Extractor.generateHyphenFileEpub(new InputStreamReader(zipInputStream), notes, hStream, name, svgs, count);


                Fb2Extractor.writeToZipNoClose(zos, name, new ByteArrayInputStream(hStream.toByteArray()));
            } else {
                LOG.d("nextEntry cancell", TempHolder.get().loadingCancelled, name);
                Fb2Extractor.writeToZipNoClose(zos, name, zipInputStream);
            }

        }

        if (AppState.get().isExperimental) {

            Object lock = new Object();

            for (String key : svgs.keySet()) {


                ByteArrayOutputStream out = new ByteArrayOutputStream();
                WebViewUtils.renterToPng(key, svgs.get(key), out, lock);

                synchronized (lock) {
                    lock.wait(2000);
                }

//                if (BuildConfig.LOG) {
//                    final File file = new File(CacheZipUtils.CACHE_BOOK_DIR, key + ".svg");
//                    IO.writeString(file, svgs.get(key));
//                }

                Fb2Extractor.writeToZipNoClose(zos, key, new ByteArrayInputStream(out.toByteArray()));


            }
        }

        zipInputStream.close();

        zos.close();

    }

    public static File extractAttachment(File bookPath, String attachmentName) {
        LOG.d("Begin extractAttachment", bookPath.getPath(), attachmentName);
        try {

            InputStream in = new FileInputStream(bookPath);
            ZipInputStream zipInputStream = new ZipInputStream(in);

            ZipEntry nextEntry = null;
            while ((nextEntry = zipInputStream.getNextEntry()) != null) {
                if (TempHolder.get().loadingCancelled) {
                    break;
                }
                if (nextEntry.getName().equals(attachmentName)) {
                    if (attachmentName.contains("/")) {
                        attachmentName = attachmentName.substring(attachmentName.lastIndexOf("/") + 1);
                    }
                    File extractMedia = new File(CacheZipUtils.ATTACHMENTS_CACHE_DIR, attachmentName);

                    LOG.d("Begin extractAttachment extract", extractMedia.getPath());

                    FileOutputStream fileOutputStream = new FileOutputStream(extractMedia);
                    OutputStream out = new BufferedOutputStream(fileOutputStream);
                    writeToStream(zipInputStream, out);
                    return extractMedia;
                }
                // zipInputStream.closeEntry();
            }

            return null;
        } catch (Exception e) {
            LOG.e(e);
            return null;
        }
    }

    public static void writeToStream(InputStream zipInputStream, OutputStream out) throws IOException {

        byte[] bytesIn = new byte[BUFFER_SIZE];
        int read = 0;
        while ((read = zipInputStream.read(bytesIn)) != -1) {
            out.write(bytesIn, 0, read);
        }
        out.close();
    }

    public static List<String> getAttachments(String inputPath) throws IOException {
        List<String> attachments = new ArrayList<String>();
        try {
            ArchiveEntry nextEntry = null;
            ZipArchiveInputStream zipInputStream = Zips.buildZipArchiveInputStream(inputPath);
            while ((nextEntry = zipInputStream.getNextEntry()) != null) {
                if (TempHolder.get().loadingCancelled) {
                    break;
                }
                String name = nextEntry.getName();
                LOG.d("getAttachments", name);
                if (ExtUtils.isMediaContent(name)) {
                    if (nextEntry.getSize() > 0) {
                        name = name + "," + nextEntry.getSize();
                    } else if (nextEntry.getCompressedSize() > 0) {
                        name = name + "," + nextEntry.getCompressedSize();
                    } else {
                        name = name + "," + 0;
                    }
                    attachments.add(name);
                }
            }
            zipInputStream.close();
        } catch (Exception e) {
            LOG.e(e);
        }
        return attachments;
    }

    @Deprecated
    private void proccessHypensDefault(String input, String output) throws Exception {
        LOG.d("proccessHypens1", input, output);

        FileInputStream inputStream = new FileInputStream(new File(input));
        ZipInputStream zipInputStream = new ZipInputStream(inputStream);
        ZipEntry nextEntry = null;

        ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(new File(output)));
        zos.setLevel(0);

        while ((nextEntry = zipInputStream.getNextEntry()) != null) {
            if (TempHolder.get().loadingCancelled) {
                break;
            }
            String name = nextEntry.getName();
            String nameLow = name.toLowerCase(Locale.US);

            if (!name.endsWith("container.xml") && (nameLow.endsWith("html") || nameLow.endsWith("htm") || nameLow.endsWith("xml"))) {
                LOG.d("nextEntry HTML cancell", TempHolder.get().loadingCancelled, name);

                ByteArrayOutputStream hStream = new ByteArrayOutputStream();
                Fb2Extractor.generateHyphenFileEpub(new InputStreamReader(zipInputStream), null, hStream, null, null, 0);
                Fb2Extractor.writeToZipNoClose(zos, name, new ByteArrayInputStream(hStream.toByteArray()));
            } else {
                LOG.d("nextEntry cancell", TempHolder.get().loadingCancelled, name);
                Fb2Extractor.writeToZipNoClose(zos, name, zipInputStream);

            }

        }
        zipInputStream.close();
        inputStream.close();

        zos.close();

    }

    @Override
    public String getBookOverview(String path) {
        String info = "";
        try {
            ZipArchiveInputStream zipInputStream = Zips.buildZipArchiveInputStream(path);

            ArchiveEntry nextEntry = null;

            while ((nextEntry = zipInputStream.getNextEntry()) != null) {
                String name = nextEntry.getName().toLowerCase(Locale.US);
                if (name.endsWith(".opf")) {

                    XmlPullParser xpp = XmlParser.buildPullParser();
                    xpp.setInput(zipInputStream, "utf-8");

                    int eventType = xpp.getEventType();

                    while (eventType != XmlPullParser.END_DOCUMENT) {
                        if (eventType == XmlPullParser.START_TAG) {
                            if ("dc:description".equals(xpp.getName()) || "dcns:description".equals(xpp.getName())) {
                                info = xpp.nextText();
                                break;
                            }
                        }
                        if (eventType == XmlPullParser.END_TAG) {
                            if ("metadata".equals(xpp.getName())) {
                                break;
                            }
                        }
                        eventType = xpp.next();
                    }
                }
            }
            zipInputStream.close();
        } catch (Exception e) {
            LOG.e(e);
        }
        return info;
    }

    @Override
    public EbookMeta getBookMetaInformation(String path) {
        try {
            LOG.d("getBookMetaInformation path", path);
            ZipArchiveInputStream zipInputStream = Zips.buildZipArchiveInputStream(path);

            ArchiveEntry nextEntry = null;

            String title = null;
            String author = "";
            String subject = "";
            String series = null;
            String number = null;
            String lang = null;
            String genre = "";
            String date = null;
            String publisher = "";
            String ibsn = "";

            while ((nextEntry = zipInputStream.getNextEntry()) != null) {
                String name = nextEntry.getName().toLowerCase(Locale.US);

                if (name.endsWith(".opf") || name.endsWith("meta.xml")) {

                    XmlPullParser xpp = XmlParser.buildPullParser();
                    xpp.setInput(zipInputStream, "utf-8");

                    int eventType = xpp.getEventType();

                    while (eventType != XmlPullParser.END_DOCUMENT) {
                        if (eventType == XmlPullParser.START_TAG) {
                            if ("dc:title".equals(xpp.getName()) || "dcns:title".equals(xpp.getName())) {
                                if (title == null) {
                                    title = xpp.nextText();
                                } else {
                                    title = title + " - " + xpp.nextText();
                                }
                            }

                            if ("dc:creator".equals(xpp.getName()) || "dcns:creator".equals(xpp.getName())) {
                                author = author + ", " + xpp.nextText();
                            }

                            if ("dc:date".equals(xpp.getName()) || "dcns:date".equals(xpp.getName())) {
                                if (date != null && xpp.getAttributeCount() == 0) {
                                    date = xpp.nextText();
                                } else if (date == null) {
                                    date = xpp.nextText();
                                }
                            }

                            if ("dc:subject".equals(xpp.getName()) || "dcns:subject".equals(xpp.getName())) {
                                subject = xpp.nextText() + "," + subject;
                            }

                            if ("dc:publisher".equals(xpp.getName()) || "dcns:publisher".equals(xpp.getName())) {
                                publisher = xpp.nextText();
                            }

                            if ("dc:identifier".equals(xpp.getName()) || "dcns:identifier".equals(xpp.getName())) {
                                ibsn = xpp.nextText() + "," + ibsn;
                            }

                            if (lang == null && ("dc:language".equals(xpp.getName()) || "dcns:language".equals(xpp.getName()))) {
                                lang = xpp.nextText();
                            }

                            if ("meta".equals(xpp.getName()) || "opf:meta".equals(xpp.getName())) {

                                String nameAttr = TxtUtils.nullToEmpty(xpp.getAttributeValue(null, "name"));
                                String propertyAttr = TxtUtils.nullToEmpty(xpp.getAttributeValue(null, "property"));
                                String value = TxtUtils.nullToEmpty(xpp.getAttributeValue(null, "content"));

                                if (propertyAttr.equals("belongs-to-collection")) {
                                    series = xpp.nextText();
                                    LOG.d("belongs-to-collection series", series);
                                } else if (propertyAttr.equals("group-position")) {
                                    number = xpp.nextText();
                                    LOG.d("belongs-to-collection group-position number", number);
                                } else if (nameAttr.endsWith(":series")) {
                                    series = value;
                                } else if (nameAttr.endsWith(":series_index")) {
                                    number = value;
                                } else if ("calibre:user_metadata:#genre".equals(nameAttr)) {
                                    LOG.d("userGenre", value);
                                    try {
                                        LinkedJSONObject obj = new LinkedJSONObject(value);
                                        JSONArray jsonArray = obj.getJSONArray("#value#");
                                        String res = "";
                                        for (int i = 0; i < jsonArray.length(); i++) {
                                            res = res + "," + jsonArray.getString(i);
                                        }
                                        genre = TxtUtils.replaceFirst(res, ",", "");
                                        LOG.d("userGenre-list", genre);
                                    } catch (Exception e) {
                                        LOG.e(e);
                                    }
                                }
                                if ("librera:user_metadata:#genre".equals(nameAttr)) {
                                    LOG.d("librera-userGenre", value);
                                    try {
                                        genre = value;
                                    } catch (Exception e) {
                                        LOG.e(e);
                                    }
                                }

                            }
                        }
                        if (eventType == XmlPullParser.END_TAG) {
                            if ("metadata".equals(xpp.getName())) {
                                break;
                            }
                        }
                        eventType = xpp.next();
                    }
                }
            }
            zipInputStream.close();

            author = TxtUtils.replaceFirst(author, ", ", "");
            ibsn = TxtUtils.replaceLast(ibsn, ",", "");

            String allGenres = subject + "," + genre;

            EbookMeta ebookMeta = new EbookMeta(title, author, series, allGenres.replaceAll(",$", ""));
            try {
                if (number != null) {
                    //number = number.replace(".0", "");
                    if (number.contains(".")) {
                        ebookMeta.setsIndex((int) Float.parseFloat(number));
                    } else {
                        ebookMeta.setsIndex(Integer.parseInt(number));
                    }
                    LOG.d("epub3", series, ebookMeta.getsIndex());

                }
            } catch (Exception e) {
                title = title + " [" + number + "]";
                ebookMeta.setTitle(title);
                LOG.d(e);
            }
            ebookMeta.setLang(lang);
            ebookMeta.setYear(date);
            ebookMeta.setPublisher(publisher);
            ebookMeta.setIsbn(ibsn);
            // ebookMeta.setPagesCount((int) size / 1024);
            return ebookMeta;
        } catch (Exception e) {
            LOG.e(e);
            return EbookMeta.Empty();
        }
    }

    @Override
    public byte[] getBookCover(String path) {
        byte[] cover = null;
        try {
            ZipArchiveInputStream zipInputStream = Zips.buildZipArchiveInputStream(path);

            ArchiveEntry nextEntry = null;

            String coverName = null;
            String coverResource = null;

            while (coverName == null && (nextEntry = zipInputStream.getNextEntry()) != null) {
                String name = nextEntry.getName().toLowerCase(Locale.US);
                if (name.endsWith(".opf")) {
                    XmlPullParser xpp = XmlParser.buildPullParser();
                    xpp.setInput(zipInputStream, "utf-8");

                    int eventType = xpp.getEventType();

                    while (eventType != XmlPullParser.END_DOCUMENT) {
                        if (eventType == XmlPullParser.START_TAG) {
                            if ("meta".equals(xpp.getName()) && "cover".equals(xpp.getAttributeValue(null, "name"))) {
                                coverResource = xpp.getAttributeValue(null, "content");
                            }
                            if ("item".equals(xpp.getName()) && "cover-image".equals(xpp.getAttributeValue(null, "properties"))) {
                                coverName = xpp.getAttributeValue(null, "href");
                                if (coverName != null && coverName.endsWith(".svg")) {
                                    coverName = null;
                                }
                                break;
                            }

                            if (coverResource != null && "item".equals(xpp.getName()) && coverResource.equals(xpp.getAttributeValue(null, "id"))) {
                                coverName = xpp.getAttributeValue(null, "href");
                                if (coverName != null && coverName.endsWith(".svg")) {
                                    coverName = null;
                                }
                                break;
                            }
                        }
                        eventType = xpp.next();
                    }
                }
            }

            if (coverName != null) {
                zipInputStream.close();

                zipInputStream = Zips.buildZipArchiveInputStream(path);
                while ((nextEntry = zipInputStream.getNextEntry()) != null) {
                    String name = nextEntry.getName();
                    if (name.contains(coverName)) {
                        cover = BaseExtractor.getEntryAsByte(zipInputStream);
                        break;
                    }
                }
            }

            if (cover == null) {
                zipInputStream.close();

                zipInputStream = Zips.buildZipArchiveInputStream(path);
                while ((nextEntry = zipInputStream.getNextEntry()) != null) {
                    String name = nextEntry.getName().toLowerCase(Locale.US);
                    if (name.endsWith(".jpeg") || name.endsWith(".jpg") || name.endsWith(".png")) {
                        if (name.contains("cover")) {
                            cover = BaseExtractor.getEntryAsByte(zipInputStream);
                            break;
                        }

                    }
                }
            }

            if (cover == null) {
                zipInputStream.close();

                zipInputStream = Zips.buildZipArchiveInputStream(path);
                while ((nextEntry = zipInputStream.getNextEntry()) != null) {
                    String name = nextEntry.getName().toLowerCase(Locale.US);
                    if (name.endsWith(".jpeg") || name.endsWith(".jpg") || name.endsWith(".png")) {
                        cover = BaseExtractor.getEntryAsByte(zipInputStream);
                        break;
                    }
                }
            }

            zipInputStream.close();

        } catch (Exception e) {
            LOG.e(e);
        }
        return cover;
    }

    @Override
    public Map<String, String> getFooterNotes(String inputPath) {

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
                    if (TempHolder.get().loadingCancelled) {
                        break;
                    }
                    String name = nextEntry.getName();
                    String nameLow = name.toLowerCase(Locale.US);
                    if (nameLow.endsWith("html") || nameLow.endsWith("htm") || nameLow.endsWith("xml")) {
                        // System.out.println("- " + nameLow + " -");
                        Document parse = Jsoup.parse(zipInputStream, null, "", Parser.xmlParser());
                        Elements select = parse.select("a[href]");

                        for (int i = 0; i < select.size(); i++) {
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
                                LOG.d("link-item-text", attr, text);
                                if (!TxtUtils.isFooterNote(text)) {
                                    LOG.d("Skip text", text);
                                    continue;
                                }

                                textLink.put(attr, text);
                                LOG.d("put links >>", attr, text);

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
                    if (TempHolder.get().loadingCancelled) {
                        break;
                    }
                    String name = nextEntry.getName();
                    for (String fileName : files) {

                        LOG.d("PARSE FILE NAME begin", name);
                        if (ExtUtils.getFileName(name).endsWith(ExtUtils.getFileName(fileName))) {
                            LOG.d("PARSE FILE NAME", name);
                            // System.out.println("file: " + name);
                            Parser xmlParser = Parser.xmlParser();
                            Document parse = Jsoup.parse(zipInputStream, null, "", xmlParser);

                            Elements ids = parse.select("[id]");
                            for (int i = 0; i < ids.size(); i++) {
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


                                LOG.d("put text >>", textKey, value);
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

}
