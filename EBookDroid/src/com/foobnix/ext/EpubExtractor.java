package com.foobnix.ext;

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

import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipArchiveInputStream;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.parser.Parser;
import org.jsoup.select.Elements;
import org.xmlpull.v1.XmlPullParser;

import com.BaseExtractor;
import com.foobnix.android.utils.LOG;
import com.foobnix.android.utils.TxtUtils;
import com.foobnix.pdf.info.ExtUtils;
import com.foobnix.sys.TempHolder;

public class EpubExtractor extends BaseExtractor {

    final static EpubExtractor inst = new EpubExtractor();

    private EpubExtractor() {

    }

    public static EpubExtractor get() {
        return inst;
    }

    public static void proccessHypens(String input, String output) {
        try {
            // proccessHypensDefault(input, output);
            proccessHypensApache(input, output);
        } catch (Exception e) {
            LOG.e(e);
            try {

            } catch (Exception e1) {
                LOG.e(e1);
            }
        }

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
                ByteArrayOutputStream hStream = Fb2Extractor.generateHyphenFileEpub(new InputStreamReader(zipInputStream));
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

    public static void proccessHypensApache(String input, String output) throws Exception {

        LOG.d("proccessHypens2", input, output);

        FileInputStream inputStream = new FileInputStream(new File(input));
        ZipArchiveInputStream zipInputStream = new ZipArchiveInputStream(inputStream);
        ArchiveEntry nextEntry = null;

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

                ByteArrayOutputStream hStream = Fb2Extractor.generateHyphenFileEpub(new InputStreamReader(zipInputStream));

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
            final File file = new File(path);
            InputStream inputStream = new FileInputStream(file);
            ZipArchiveInputStream zipInputStream = new ZipArchiveInputStream(inputStream);

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
            inputStream.close();
        } catch (Exception e) {
            LOG.e(e);
        }
        return info;
    }

    @Override
    public EbookMeta getBookMetaInformation(String path) {
        final File file = new File(path);
        try {
            InputStream inputStream = new FileInputStream(file);
            ZipArchiveInputStream zipInputStream = new ZipArchiveInputStream(inputStream);

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

                if (name.endsWith(".opf")) {

                    XmlPullParser xpp = XmlParser.buildPullParser();
                    xpp.setInput(zipInputStream, "utf-8");

                    int eventType = xpp.getEventType();

                    while (eventType != XmlPullParser.END_DOCUMENT) {
                        if (eventType == XmlPullParser.START_TAG) {
                            if ("dc:title".equals(xpp.getName()) || "dcns:title".equals(xpp.getName())) {
                                title = xpp.nextText();
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

                            if ("meta".equals(xpp.getName())) {
                                String nameAttr = xpp.getAttributeValue(null, "name");
                                String value = xpp.getAttributeValue(null, "content");
                                if ("calibre:series".equals(nameAttr)) {
                                    series = value;
                                } else if ("calibre:series_index".equals(nameAttr)) {
                                    number = value;
                                    if (number != null) {
                                        number = number.replace(".0", "");
                                    }
                                } else

                                if ("calibre:user_metadata:#genre".equals(nameAttr)) {
                                    LOG.d("userGenre", value);
                                    try {
                                        JSONObject obj = new JSONObject(value);
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
            inputStream.close();

            author = TxtUtils.replaceFirst(author, ", ", "");
            ibsn = TxtUtils.replaceLast(ibsn, ",", "");

            String allGenres = subject + "," + genre;

            EbookMeta ebookMeta = new EbookMeta(title, author, series, allGenres.replaceAll(",$", ""));
            try {
                if (number != null) {
                    ebookMeta.setsIndex(Integer.parseInt(number));
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
        } catch (

        Exception e) {
            return EbookMeta.Empty();
        }
    }

    @Override
    public byte[] getBookCover(String path) {
        byte[] cover = null;
        try {
            InputStream inputStream = new FileInputStream(new File(path));
            ZipArchiveInputStream zipInputStream = new ZipArchiveInputStream(inputStream);

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
                inputStream.close();

                inputStream = new FileInputStream(new File(path));
                zipInputStream = new ZipArchiveInputStream(inputStream);
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
                inputStream.close();

                inputStream = new FileInputStream(new File(path));
                zipInputStream = new ZipArchiveInputStream(inputStream);
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
                inputStream.close();

                inputStream = new FileInputStream(new File(path));
                zipInputStream = new ZipArchiveInputStream(inputStream);
                while ((nextEntry = zipInputStream.getNextEntry()) != null) {
                    String name = nextEntry.getName().toLowerCase(Locale.US);
                    if (name.endsWith(".jpeg") || name.endsWith(".jpg") || name.endsWith(".png")) {
                        cover = BaseExtractor.getEntryAsByte(zipInputStream);
                        break;
                    }
                }
            }

            zipInputStream.close();
            inputStream.close();

        } catch (Exception e) {
            LOG.e(e);
        }
        return cover;
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
            InputStream in = new FileInputStream(new File(inputPath));
            ZipEntry nextEntry = null;
            ZipInputStream zipInputStream = new ZipInputStream(in);
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

    @Override
    public Map<String, String> getFooterNotes(String inputPath) {
        Map<String, String> notes = new HashMap<String, String>();
        try {
            InputStream in = new FileInputStream(new File(inputPath));
            ZipInputStream zipInputStream = new ZipInputStream(in);

            ZipEntry nextEntry = null;
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
                                if (!TxtUtils.isFooterNote(text)) {
                                    LOG.d("Skip text", text);
                                    continue;
                                }

                                textLink.put(attr, text);

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
                    zipInputStream.closeEntry();
                }

                in = new FileInputStream(new File(inputPath));
                zipInputStream = new ZipInputStream(in);

                while ((nextEntry = zipInputStream.getNextEntry()) != null) {
                    if (TempHolder.get().loadingCancelled) {
                        break;
                    }
                    String name = nextEntry.getName();
                    for (String fileName : files) {

                        if (name.endsWith(fileName)) {
                            LOG.d("PARSE FILE NAME", name);
                            // System.out.println("file: " + name);
                            Parser xmlParser = Parser.xmlParser();
                            Document parse = Jsoup.parse(zipInputStream, null, "", xmlParser);
                            Elements ids = parse.select("[id]");
                            for (int i = 0; i < ids.size(); i++) {
                                Element item = ids.get(i);
                                String id = item.attr("id");
                                String value = item.text();

                                if (value.trim().length() < 4) {
                                    value = value + " " + parse.select("[id=" + id + "]+*").text();
                                }
                                if (value.trim().length() < 4) {
                                    value = value + " " + parse.select("[id=" + id + "]+*+*").text();
                                }
                                try {
                                    if (value.trim().length() < 4) {
                                        value = value + " " + parse.select("[id=" + id + "]").parents().get(0).text();
                                    }
                                } catch (Exception e) {
                                    LOG.e(e);
                                }

                                // System.out.println("id:" + id + " value:"
                                // +
                                // value);
                                String fileKey = fileName + "#" + id;

                                String textKey = textLink.get(fileKey);

                                LOG.d(textKey + " " + value);
                                notes.put(textKey, value);

                            }
                        }

                    }
                    zipInputStream.closeEntry();
                }
                zipInputStream.close();
                in.close();

            } catch (Exception e) {
                LOG.e(e);
            }

            return notes;
        } catch (Throwable e) {
            LOG.e(e);
            return notes;
        }
    }

    @Override
    public boolean convert(String path, String to) {
        return false;
    }

}
