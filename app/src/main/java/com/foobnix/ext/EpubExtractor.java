package com.foobnix.ext;

import com.BaseExtractor;
import com.foobnix.android.utils.LOG;
import com.foobnix.android.utils.TxtUtils;
import com.foobnix.android.utils.WebViewUtils;
import com.foobnix.hypen.HypenUtils;
import com.foobnix.model.AppData;
import com.foobnix.model.AppSP;
import com.foobnix.model.AppState;
import com.foobnix.model.SimpleMeta;
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
import org.librera.JSONException;
import org.librera.LinkedJSONObject;
import org.xmlpull.v1.XmlPullParser;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.URLDecoder;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
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

        Fb2Extractor.epub3Pages.clear();

        LOG.d("proccessHypens2", input, output);

        ZipArchiveInputStream zipInputStream = Zips.buildZipArchiveInputStream(input);

        ArchiveEntry nextEntry = null;

        File outputFile = new File(output);

        try {
            Objects.requireNonNull(outputFile.getParentFile())
                   .mkdirs();
        }catch (Exception ignored){

        }

        ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(outputFile));
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

        List<SimpleMeta> replacements = AppData.get().getAllTextReplaces();

        while ((nextEntry = zipInputStream.getNextEntry()) != null) {
            if (TempHolder.get().loadingCancelled.get()) {
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

            if (nameLow.endsWith(".css")) {
                InputStreamReader inputStreamReader = new InputStreamReader(zipInputStream);
                BufferedReader in = new BufferedReader(inputStreamReader);
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                PrintWriter writer = new PrintWriter(out);
                String line;
                boolean skipInvalidCss = false;

                while ((line = in.readLine()) != null) {
                    if (line.indexOf('.', 200) > 0) {
                        skipInvalidCss = true;
                        break;
                    }
                    writer.println(line);
                }
                writer.close();

                LOG.d("Skip skipInvalidCss", name, skipInvalidCss);
                if (!skipInvalidCss) {
                    Fb2Extractor.writeToZipNoClose(zos, name, new ByteArrayInputStream(out.toByteArray()));
                }
            } else if (nameLow.endsWith("html") || nameLow.endsWith("htm") || nameLow.endsWith("xml")) {

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

                Fb2Extractor.generateHyphenFileEpub(new InputStreamReader(zipInputStream), notes, hStream, name, svgs, count, replacements);


                Fb2Extractor.writeToZipNoClose(zos, name, new ByteArrayInputStream(hStream.toByteArray()));
            } else {
                LOG.d("nextEntry cancell", TempHolder.get().loadingCancelled.get(), name);
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

//                if (LibreraBuildConfig.LOG) {
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
                if (TempHolder.get().loadingCancelled.get()) {
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
                if (TempHolder.get().loadingCancelled.get()) {
                    return new ArrayList<>();
                }
                String name = nextEntry.getName();
                LOG.d("getAttachments", name,"finished",TempHolder.get().loadingCancelled.get());
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
            String calibreTimestamp = null;
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
                                } else if ("calibre:timestamp".equals(nameAttr)) {
                                    calibreTimestamp = value;
                                } else if ("calibre:user_metadata:#genre".equals(nameAttr)) {
                                    LOG.d("userGenre", value);
                                    try {
                                        LinkedJSONObject obj = new LinkedJSONObject(value);
                                        try {
                                            genre = obj.getString("#value#");
                                        } catch (JSONException e) {
                                            JSONArray jsonArray = obj.getJSONArray("#value#");
                                            String res = "";
                                            for (int i = 0; i < jsonArray.length(); i++) {
                                                res = res + "," + jsonArray.getString(i);
                                            }
                                            genre = TxtUtils.replaceFirst(res, ",", "");
                                            LOG.d("userGenre-list", genre);
                                        }
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
            if (date == null) {
                ebookMeta.setYear(calibreTimestamp);
            } else {
                ebookMeta.setYear(date);
            }

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
            LOG.d("Covers-", "Book:", path);
            LOG.d("Covers-", "coverName:", coverName);
            if (coverName != null) {
                zipInputStream.close();

                zipInputStream = Zips.buildZipArchiveInputStream(path);
                while ((nextEntry = zipInputStream.getNextEntry()) != null) {
                    String name = nextEntry.getName();
                    if (name.equals(coverName) || name.equals("OEBPS/" + coverName)) { //EQUALS
                        LOG.d("Covers-", "EQ:", name);
                        cover = BaseExtractor.getEntryAsByte(zipInputStream);
                        break;
                    }
                }
                if (cover == null) {
                    zipInputStream.close();

                    zipInputStream = Zips.buildZipArchiveInputStream(path);
                    while ((nextEntry = zipInputStream.getNextEntry()) != null) {
                        String name = nextEntry.getName();
                        if (name.endsWith(coverName)) { // CONTAIN
                            LOG.d("Covers-", "END:", name, coverName);
                            cover = BaseExtractor.getEntryAsByte(zipInputStream);
                            break;
                        }
                    }
                }
            }

            if (cover == null) {
                LOG.d("Covers-", "OTHER:");
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
        ZipArchiveInputStream zipInputStream = null;
        try {
            // pass 1: footnote links like <a href="notes.xhtml#n1">[1]</a> in OEBPS/ch1.xhtml,
            // kept as "OEBPS/notes.xhtml" -> "n1" -> ["[1]#OEBPS/ch1.xhtml"]
            Map<String, Map<String, List<String>>> links = new HashMap<String, Map<String, List<String>>>();
            List<String> documents = new ArrayList<String>();

            zipInputStream = Zips.buildZipArchiveInputStream(inputPath);
            ArchiveEntry nextEntry;
            while ((nextEntry = zipInputStream.getNextEntry()) != null) {
                if (TempHolder.get().loadingCancelled.get()) {
                    return new HashMap<String, String>();
                }
                String name = nextEntry.getName();
                if (!isDocument(name)) {
                    continue;
                }
                documents.add(name);
                try {
                    findFooterLinks(readText(zipInputStream), name, links);
                } catch (Exception e) {
                    LOG.e(e, name);
                }
            }
            zipInputStream.release();
            zipInputStream = null;

            // pass 2: text of the linked elements, parsing only the files that have them
            Map<String, Map<String, List<String>>> targets = resolveTargets(links, documents);
            if (targets.isEmpty()) {
                return notes;
            }
            zipInputStream = Zips.buildZipArchiveInputStream(inputPath);
            while ((nextEntry = zipInputStream.getNextEntry()) != null) {
                if (TempHolder.get().loadingCancelled.get()) {
                    return new HashMap<String, String>();
                }
                Map<String, List<String>> ids = targets.get(nextEntry.getName());
                if (ids == null) {
                    continue;
                }
                try {
                    collectNotes(Jsoup.parse(zipInputStream, null, "", Parser.xmlParser()), ids, notes);
                } catch (Exception e) {
                    LOG.e(e, nextEntry.getName());
                }
            }
            return notes;
        } catch (Throwable e) {
            LOG.e(e);
            return notes;
        } finally {
            if (zipInputStream != null) {
                zipInputStream.release();
            }
        }
    }

    // an id used more than once, like a list <div id="n1"> around <a id="n1">[1]</a>, gives the element
    // whose text is the link text, else the first one
    private static void collectNotes(Document parse, Map<String, List<String>> ids, Map<String, String> notes) {
        Map<Element, String> parentText = new IdentityHashMap<Element, String>();
        Map<String, Integer> matches = new HashMap<String, Integer>();
        for (Element item : parse.select("[id]")) {
            String id = item.attr("id");
            List<String> textKeys = ids.get(id);
            if (textKeys == null) {
                continue;
            }
            String text = item.text();
            for (String textKey : textKeys) {
                int match = match(text, textKey.substring(0, textKey.lastIndexOf('#')));
                String pair = id + "#" + textKey;
                Integer best = matches.get(pair);
                if (best == null || match > best) {
                    matches.put(pair, match);
                    notes.put(textKey, noteValue(item, text, parentText));
                }
            }
        }
    }

    private static int match(String text, String label) {
        text = text.replace(TxtUtils.NON_BREAKE_SPACE, " ").trim();
        if (text.equals(label)) {
            return 2;
        }
        return text.startsWith(label) ? 1 : 0;
    }

    private static boolean isDocument(String name) {
        String nameLow = name.toLowerCase(Locale.US);
        return nameLow.endsWith("html") || nameLow.endsWith("htm") || nameLow.endsWith("xml");
    }

    // text of the linked element; a short one like "[1]" gets the text after it or of its paragraph
    private static String noteValue(Element item, String text, Map<Element, String> parentText) {
        int min = 20;
        String value = text;
        Element sibling = item.nextElementSibling();
        if (value.trim().length() < min) {
            value = value + " " + (sibling == null ? "" : sibling.text());
        }
        if (value.trim().length() < min) {
            Element next = sibling == null ? null : sibling.nextElementSibling();
            value = value + " " + (next == null ? "" : next.text());
        }
        Element parent = item.parent();
        if (value.trim().length() < min && parent != null && !(parent instanceof Document)) {
            String parentValue = parentText.get(parent);
            if (parentValue == null) {
                parentValue = parent.text();
                parentText.put(parent, parentValue);
            }
            value = value + " " + parentValue;
        }
        return value.trim();
    }

    // <a href="...#id">text</a> with a footnote text like [1], found without building a DOM
    private static void findFooterLinks(String html, String name, Map<String, Map<String, List<String>>> links) {
        int length = html.length();
        int comment = html.indexOf("<!--");
        int hash = -1;
        int i = 0;
        while ((i = html.indexOf("<a", i)) >= 0) {
            if (comment >= 0 && comment < i) {
                int commentEnd = html.indexOf("-->", comment + 4);
                if (commentEnd < 0) {
                    return;
                }
                comment = html.indexOf("<!--", commentEnd + 3);
                if (i < commentEnd + 3) {
                    i = commentEnd + 3;
                }
                continue;
            }
            int start = i;
            i += 2;
            if (i >= length || !isSpace(html.charAt(i))) {
                continue; // <abbr>, <aside>, <a> without attributes
            }
            int tagEnd = tagEnd(html, i);
            if (tagEnd < 0) {
                return;
            }
            i = tagEnd + 1;
            if (html.charAt(tagEnd - 1) == '/') {
                continue; // <a id="x"/>
            }
            if (hash < start) {
                hash = html.indexOf('#', start);
                if (hash < 0) {
                    return; // no more links to an #id
                }
            }
            if (hash > tagEnd) {
                continue;
            }
            int close = closingTag(html, i);
            if (close < 0) {
                return;
            }
            int closeEnd = html.indexOf('>', close);
            if (closeEnd < 0) {
                return;
            }
            if (!hasFooterNoteChars(html, i, close)) {
                continue;
            }

            String text = null;
            String href = null;
            if (isPlainText(html, i, close)) {
                href = plainHref(html, start + 2, tagEnd);
                text = html.substring(i, close);
            }
            if (href == null) {
                Element a = Jsoup.parse(html.substring(start, closeEnd + 1), "", Parser.xmlParser()).selectFirst("a[href]");
                if (a == null) {
                    continue;
                }
                text = a.text();
                href = a.attr("href");
            }
            i = closeEnd + 1;
            addFooterLink(links, name, href, text);
        }
    }

    private static void addFooterLink(Map<String, Map<String, List<String>>> links, String name, String href, String text) {
        int sharp = href.indexOf('#');
        if (sharp < 0 || !TxtUtils.isFooterNote(text)) {
            return;
        }
        String file = href.substring(0, sharp);
        String target = file.isEmpty() ? name : resolvePath(name, file);
        if (target == null) {
            return;
        }
        Map<String, List<String>> ids = links.get(target);
        if (ids == null) {
            ids = new HashMap<String, List<String>>();
            links.put(target, ids);
        }
        String id = href.substring(sharp + 1);
        List<String> textKeys = ids.get(id);
        if (textKeys == null) {
            textKeys = new ArrayList<String>(1);
            ids.put(id, textKeys);
        }
        String textKey = text + "#" + name;
        if (!textKeys.contains(textKey)) {
            textKeys.add(textKey);
        }
    }

    // the zip entry a link points to, relative to the linking file; null for external links
    static String resolvePath(String base, String href) {
        if (href.indexOf(':') >= 0) {
            return null;
        }
        int query = href.indexOf('?');
        if (query >= 0) {
            href = href.substring(0, query);
        }
        if (href.indexOf('%') >= 0) {
            try {
                href = URLDecoder.decode(href.replace("+", "%2B"), "UTF-8");
            } catch (Exception e) {
                // keep it as it is
            }
        }
        String path;
        if (href.startsWith("/")) {
            path = href.substring(1);
        } else {
            int slash = base.lastIndexOf('/');
            path = slash < 0 ? href : base.substring(0, slash + 1) + href;
        }
        if (!path.contains("./") && !path.contains("//")) {
            return path;
        }
        List<String> parts = new ArrayList<String>();
        for (String part : path.split("/")) {
            if (part.equals("..")) {
                if (!parts.isEmpty()) {
                    parts.remove(parts.size() - 1);
                }
            } else if (!part.isEmpty() && !part.equals(".")) {
                parts.add(part);
            }
        }
        StringBuilder out = new StringBuilder(path.length());
        for (String part : parts) {
            if (out.length() > 0) {
                out.append('/');
            }
            out.append(part);
        }
        return out.toString();
    }

    // a link to a missing path falls back to a document with the same file name
    private static Map<String, Map<String, List<String>>> resolveTargets(Map<String, Map<String, List<String>>> links, List<String> documents) {
        Set<String> names = new HashSet<String>(documents);
        Map<String, String> byFileName = new HashMap<String, String>();
        for (String document : documents) {
            String fileName = document.substring(document.lastIndexOf('/') + 1);
            if (!byFileName.containsKey(fileName)) {
                byFileName.put(fileName, document);
            }
        }
        Map<String, Map<String, List<String>>> targets = new HashMap<String, Map<String, List<String>>>();
        for (Map.Entry<String, Map<String, List<String>>> link : links.entrySet()) {
            String path = link.getKey();
            String document = names.contains(path) ? path : byFileName.get(path.substring(path.lastIndexOf('/') + 1));
            if (document == null) {
                continue;
            }
            Map<String, List<String>> ids = targets.get(document);
            if (ids == null) {
                targets.put(document, link.getValue());
                continue;
            }
            for (Map.Entry<String, List<String>> id : link.getValue().entrySet()) {
                List<String> textKeys = ids.get(id.getKey());
                if (textKeys == null) {
                    ids.put(id.getKey(), id.getValue());
                } else {
                    textKeys.addAll(id.getValue());
                }
            }
        }
        return targets;
    }

    private static boolean isSpace(char c) {
        return c == ' ' || c == '\t' || c == '\n' || c == '\r' || c == '\f';
    }

    // the '>' that ends a tag, skipping quoted attribute values
    private static int tagEnd(String html, int from) {
        char quote = 0;
        for (int i = from; i < html.length(); i++) {
            char c = html.charAt(i);
            if (quote != 0) {
                if (c == quote) {
                    quote = 0;
                }
            } else if (c == '"' || c == '\'') {
                quote = c;
            } else if (c == '>') {
                return i;
            }
        }
        return -1;
    }

    private static int closingTag(String html, int from) {
        int i = from;
        while ((i = html.indexOf("</a", i)) >= 0) {
            int next = i + 3;
            if (next < html.length() && (html.charAt(next) == '>' || isSpace(html.charAt(next)))) {
                return i;
            }
            i = next;
        }
        return -1;
    }

    // a footnote text has a bracket, maybe written as an entity
    private static boolean hasFooterNoteChars(String html, int from, int to) {
        for (int i = from; i < to; i++) {
            char c = html.charAt(i);
            if (c == '[' || c == '{' || c == '&') {
                return true;
            }
        }
        return false;
    }

    // text that jsoup would return unchanged: no tags, entities or whitespace
    private static boolean isPlainText(String html, int from, int to) {
        for (int i = from; i < to; i++) {
            char c = html.charAt(i);
            if (c == '<' || c == '&' || c == ' ' || Character.isWhitespace(c)) {
                return false;
            }
        }
        return to > from;
    }

    // quoted href value without entities, else null to let jsoup parse the tag
    private static String plainHref(String html, int from, int to) {
        int i = from;
        while ((i = html.indexOf("href", i)) >= 0 && i < to) {
            int j = i + 4;
            if (!isSpace(html.charAt(i - 1))) {
                i = j; // data-href, xlink:href
                continue;
            }
            while (j < to && isSpace(html.charAt(j))) {
                j++;
            }
            if (j >= to || html.charAt(j) != '=') {
                return null;
            }
            j++;
            while (j < to && isSpace(html.charAt(j))) {
                j++;
            }
            if (j >= to || html.charAt(j) != '"' && html.charAt(j) != '\'') {
                return null;
            }
            int end = html.indexOf(html.charAt(j), j + 1);
            if (end < 0 || end > to) {
                return null;
            }
            String value = html.substring(j + 1, end);
            return value.indexOf('&') < 0 ? value : null;
        }
        return null;
    }

    private static String readText(InputStream in) throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream(64 * 1024);
        byte[] buffer = new byte[16 * 1024];
        int n;
        while ((n = in.read(buffer)) != -1) {
            bytes.write(buffer, 0, n);
        }
        return decode(bytes.toByteArray());
    }

    // BOM, then encoding="..." of the XML declaration or charset= of a meta tag, else UTF-8
    private static String decode(byte[] data) {
        int n = data.length;
        if (n >= 2 && (data[0] & 0xFF) == 0xFE && (data[1] & 0xFF) == 0xFF) {
            return new String(data, 2, n - 2, StandardCharsets.UTF_16BE);
        }
        if (n >= 2 && (data[0] & 0xFF) == 0xFF && (data[1] & 0xFF) == 0xFE) {
            return new String(data, 2, n - 2, StandardCharsets.UTF_16LE);
        }
        if (n >= 3 && (data[0] & 0xFF) == 0xEF && (data[1] & 0xFF) == 0xBB && (data[2] & 0xFF) == 0xBF) {
            return new String(data, 3, n - 3, StandardCharsets.UTF_8);
        }
        String head = new String(data, 0, Math.min(n, 1024), StandardCharsets.ISO_8859_1);
        int i = head.indexOf("encoding=");
        int from = i + "encoding=".length();
        if (i < 0) {
            i = head.indexOf("charset=");
            from = i + "charset=".length();
        }
        if (i >= 0) {
            while (from < head.length() && (head.charAt(from) == '"' || head.charAt(from) == '\'')) {
                from++;
            }
            int to = from;
            while (to < head.length() && (Character.isLetterOrDigit(head.charAt(to)) || "-_.:".indexOf(head.charAt(to)) >= 0)) {
                to++;
            }
            try {
                return new String(data, Charset.forName(head.substring(from, to)));
            } catch (Exception e) {
                // unknown charset
            }
        }
        return new String(data, StandardCharsets.UTF_8);
    }

}
