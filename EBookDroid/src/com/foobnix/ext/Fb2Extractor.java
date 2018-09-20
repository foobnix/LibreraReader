package com.foobnix.ext;

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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.ebookdroid.LibreraApp;
import org.ebookdroid.core.codec.OutlineLink;
import org.xmlpull.v1.XmlPullParser;

import com.BaseExtractor;
import com.foobnix.android.utils.LOG;
import com.foobnix.android.utils.StreamUtils;
import com.foobnix.android.utils.TxtUtils;
import com.foobnix.hypen.HypenUtils;
import com.foobnix.pdf.info.ExtUtils;
import com.foobnix.pdf.info.model.BookCSS;
import com.foobnix.pdf.info.model.OutlineLinkWrapper;
import com.foobnix.pdf.info.wrapper.AppState;
import com.foobnix.sys.TempHolder;

import android.util.Base64;

public class Fb2Extractor extends BaseExtractor {
    public static final String FOOTER_NOTES_SIGN = "***";
    public static final String FOOTER_AFTRER_BOODY = "[!]";

    public static final String DIVIDER = "~@~";

    static Fb2Extractor inst = new Fb2Extractor();

    private Fb2Extractor() {
    }

    public Map<String, String> genresRus = new HashMap<>();

    public void loadGenres() {
        if (!genresRus.isEmpty()) {
            return;
        }
        try {
            {
                InputStream xmlStream = LibreraApp.context.getAssets().open("union_genres_ru_1.xml");
                XmlPullParser xpp = XmlParser.buildPullParser();
                xpp.setInput(xmlStream, "UTF-8");

                int eventType = xpp.getEventType();
                while (eventType != XmlPullParser.END_DOCUMENT) {
                    if (eventType == XmlPullParser.START_TAG) {
                        if (xpp.getName().equals("genre")) {
                            String name = xpp.getAttributeValue(0);
                            String code = xpp.getAttributeValue(1);
                            genresRus.put(code, name);
                            LOG.d("loadGenres-add-1", code, name);
                        }
                    }
                    eventType = xpp.next();
                }
                xmlStream.close();
            }
            {
                InputStream xmlStream = LibreraApp.context.getAssets().open("union_genres_ru_2.xml");
                XmlPullParser xpp = XmlParser.buildPullParser();
                xpp.setInput(xmlStream, "UTF-8");

                int eventType = xpp.getEventType();
                while (eventType != XmlPullParser.END_DOCUMENT) {
                    if (eventType == XmlPullParser.START_TAG) {
                        if (xpp.getName().equals("subgenres")) {
                            String name = xpp.getAttributeValue(1);
                            String code = xpp.getAttributeValue(2);
                            if (!genresRus.containsKey(code)) {
                                genresRus.put(code, name);
                            }
                            LOG.d("loadGenres-add-2", code, name);
                        }
                    }
                    eventType = xpp.next();
                }
                xmlStream.close();
            }
        } catch (Exception e) {
            LOG.e(e);
        }
    }

    public static Fb2Extractor get() {
        return inst;
    }

    public byte[] getBookCover(InputStream inputStream, String name) {
        byte[] decode = null;
        try {
            XmlPullParser xpp = XmlParser.buildPullParser();
            xpp.setInput(inputStream, "UTF-8");

            int eventType = xpp.getEventType();
            String imageID = null;
            while (eventType != XmlPullParser.END_DOCUMENT) {
                if (eventType == XmlPullParser.START_TAG) {

                    if (imageID == null && xpp.getName().equals("image")) {
                        imageID = xpp.getAttributeValue(0);

                        if (TxtUtils.isNotEmpty(imageID)) {
                            imageID = imageID.replace("#", "");
                        }

                    }
                    if (imageID != null && xpp.getName().equals("binary") && imageID.equals(xpp.getAttributeValue(null, "id"))) {
                        decode = Base64.decode(xpp.nextText(), Base64.DEFAULT);
                        break;
                    }
                }
                eventType = xpp.next();
            }
        } catch (Exception e) {
            LOG.e(e);
        }
        return decode;
    }

    @Override
    public byte[] getBookCover(String path) {
        byte[] decode = null;
        try {
            XmlPullParser xpp = XmlParser.buildPullParser();
            xpp.setInput(new FileInputStream(path), "UTF-8");

            int eventType = xpp.getEventType();
            String imageID = null;
            String imageCover = null;
            while (eventType != XmlPullParser.END_DOCUMENT) {
                if (eventType == XmlPullParser.START_TAG) {

                    if (xpp.getName().equals("image")) {
                        if (imageID == null) {
                            imageID = xpp.getAttributeValue(0);
                            if (TxtUtils.isNotEmpty(imageID)) {
                                imageID = imageID.replace("#", "");
                            }
                        }
                        if (imageCover == null) {
                            imageCover = xpp.getAttributeValue(0);
                            if (TxtUtils.isNotEmpty(imageCover) && imageCover.toLowerCase(Locale.US).contains("cover")) {
                                imageCover = imageID = imageCover.replace("#", "");
                            } else {
                                imageCover = null;
                            }
                        }
                    }

                    if (imageID != null && xpp.getName().equals("binary") && imageID.equals(xpp.getAttributeValue(null, "id"))) {
                        decode = Base64.decode(xpp.nextText(), Base64.DEFAULT);
                        break;
                    }
                }
                eventType = xpp.next();
            }
        } catch (Exception e) {
            LOG.e(e, path);
        }
        return decode;
    }

    @Override
    public String getBookOverview(String path) {
        String info = "";
        try {
            XmlPullParser xpp = XmlParser.buildPullParser();
            xpp.setInput(new FileInputStream(path), findHeaderEncoding(path));

            int eventType = xpp.getEventType();
            boolean findAnnotation = false;
            while (eventType != XmlPullParser.END_DOCUMENT) {
                if (eventType == XmlPullParser.START_TAG) {
                    if ("annotation".equals(xpp.getName())) {
                        findAnnotation = true;
                    }
                    if ("body".equals(xpp.getName())) {
                        break;
                    }
                }
                if (eventType == XmlPullParser.TEXT) {
                    if (findAnnotation) {
                        info = info + " " + xpp.getText();
                    }

                }
                if (eventType == XmlPullParser.END_TAG) {
                    if ("annotation".equals(xpp.getName())) {
                        break;
                    }
                }

                eventType = xpp.next();
            }
        } catch (Exception e) {
            LOG.e(e);
        }

        return info;
    }

    @Override
    public EbookMeta getBookMetaInformation(String inputFile) {
        try {
            // if (inputFile.contains(ExtUtils.REFLOW_FB2)) {
            // return new EbookMeta(new
            // File(inputFile).getName().replace(ExtUtils.REFLOW_FB2, ""), "Text Reflow",
            // "", "");
            // }

            XmlPullParser xpp = XmlParser.buildPullParser();
            xpp.setInput(new FileInputStream(inputFile), findHeaderEncoding(inputFile));

            String bookTitle = null;

            String firstName = null;
            String lastName = null;

            String authors = "";

            String genre = "";
            String sequence = "";
            String lang = "";
            String number = "";
            String keywords = "";
            boolean titleInfo = false;
            boolean publishInfo = false;
            String year = "";
            String publisher = "";
            String isbn = "";

            int eventType = xpp.getEventType();
            while (eventType != XmlPullParser.END_DOCUMENT) {

                if (eventType == XmlPullParser.START_TAG) {

                    if (xpp.getName().equals("title-info")) {
                        titleInfo = true;
                    }

                    if (xpp.getName().equals("publish-info")) {
                        publishInfo = true;
                    }

                    if (publishInfo) {
                        if (xpp.getName().equals("year")) {
                            year = xpp.nextText();
                        }
                        if (xpp.getName().equals("publisher")) {
                            publisher = xpp.nextText();
                        }
                        if (xpp.getName().equals("isbn")) {
                            isbn = xpp.nextText();
                        }
                    }

                    if (titleInfo) {
                        if (xpp.getName().equals("book-title")) {
                            bookTitle = xpp.nextText();
                        } else if (xpp.getName().equals("lang")) {
                            lang = xpp.nextText();
                        } else if (firstName == null && xpp.getName().equals("first-name")) {
                            firstName = xpp.nextText();
                        } else if (lastName == null && xpp.getName().equals("last-name")) {
                            lastName = xpp.nextText();
                        } else if (xpp.getName().equals("genre")) {
                            genre = xpp.nextText() + "," + genre;
                        } else if (xpp.getName().equals("keywords")) {
                            keywords = xpp.nextText();
                        } else if (xpp.getName().equals("sequence")) {
                            sequence = xpp.getAttributeValue(null, "name");
                            String current = xpp.getAttributeValue(null, "number");
                            if (TxtUtils.isNotEmpty(current) && !("0".equals(current) || "00".equals(current))) {
                                number = current;
                            }
                        }

                    }
                }
                if (eventType == XmlPullParser.END_TAG) {
                    if (titleInfo && xpp.getName().equals("author") && firstName != null && lastName != null) {
                        firstName = TxtUtils.trim(firstName);
                        lastName = TxtUtils.trim(lastName);

                        authors = authors + ", " + firstName + " " + lastName;
                        firstName = null;
                        lastName = null;

                    }

                    if (xpp.getName().equals("description")) {
                        break;
                    }
                    if (xpp.getName().equals("title-info")) {
                        titleInfo = false;
                    }

                    if (xpp.getName().equals("publish-info")) {
                        publishInfo = false;
                    }
                }
                eventType = xpp.next();
            }

            genre = genre.replace(",,", ",") + ",";
            authors = TxtUtils.replaceFirst(authors, ", ", "");

            loadGenres();
            for (String g : genre.split(",")) {
                String value = genresRus.get(g.trim());
                if (TxtUtils.isNotEmpty(value)) {
                    genre = genre.replace(g + ",", value + ",");
                    LOG.d("loadGenres-repalce", g, value);
                } else {
                    LOG.d("loadGenres-not-found", g);
                }
            }
            genre = TxtUtils.replaceLast(genre, ",", "");

            if (TxtUtils.isNotEmpty(number)) {
                EbookMeta ebookMeta = new EbookMeta(bookTitle, authors, sequence, genre);
                try {
                    ebookMeta.setLang(lang);
                    ebookMeta.setsIndex(Integer.parseInt(number));
                    ebookMeta.setKeywords(keywords);
                    ebookMeta.setYear(year);
                    ebookMeta.setPublisher(publisher);
                    ebookMeta.setIsbn(isbn);
                    // ebookMeta.setPagesCount((int) fileSize / 512);
                } catch (Exception e) {
                    LOG.e(e);
                }
                return ebookMeta;
            } else {
                EbookMeta ebookMeta = new EbookMeta(bookTitle, authors, sequence, genre);
                ebookMeta.setLang(lang);
                ebookMeta.setKeywords(keywords);
                ebookMeta.setYear(year);
                ebookMeta.setPublisher(publisher);
                ebookMeta.setIsbn(isbn);
                // ebookMeta.setPagesCount((int) fileSize / 512);
                return ebookMeta;
            }
        } catch (Exception e) {
            LOG.e(e, "!!!!", inputFile);
        }
        return EbookMeta.Empty();
    }

    @Override
    public Map<String, String> getFooterNotes(String inputFile) {
        Map<String, String> map = new HashMap<String, String>();
        try {

            XmlPullParser xpp = XmlParser.buildPullParser();
            xpp.setInput(new FileInputStream(inputFile), findHeaderEncoding(inputFile));
            int eventType = xpp.getEventType();

            String sectionId = null;
            StringBuilder text = null;
            boolean isLink = false;
            String link = null;
            String key = "";

            while (eventType != XmlPullParser.END_DOCUMENT) {
                if (TempHolder.get().loadingCancelled) {
                    break;
                }
                if (eventType == XmlPullParser.START_TAG) {
                    if (xpp.getName().equals("a")) {
                        String type = xpp.getAttributeValue(null, "type");
                        if ("note".equals(type)) {
                            isLink = true;

                            link = xpp.getAttributeValue(null, "l:href");
                            if (link == null) {
                                link = xpp.getAttributeValue(null, "xlink:href");
                            }

                        }
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
                        map.put(keyEnd, text.toString());
                        LOG.d("getFooterNotes section", sectionId, keyEnd, ">", text.toString());
                        sectionId = null;
                        text = null;
                    } else if (xpp.getName().equals("a")) {

                        if (isLink && link != null) {
                            key = key.trim();
                            if (!TxtUtils.isFooterNote(key)) {
                                key = "[" + link + "]";
                            }
                            link = link.replace("#", "");
                            map.put(key, link);
                            LOG.d("getFooterNotes", key, ">", link);

                            isLink = false;
                            key = "";
                        }
                    }
                }

                eventType = xpp.next();
            }
        } catch (Exception e) {
            LOG.e(e);
        }
        return map;
    }

    public boolean convertFB2(String inputFile, String toName) {
        try {
            String encoding = findHeaderEncoding(inputFile);
            ByteArrayOutputStream generateFb2File = generateFb2File(inputFile, encoding, true);
            FileOutputStream out = new FileOutputStream(toName);
            out.write(generateFb2File.toByteArray());
            out.close();
        } catch (Exception e) {
            LOG.e(e);
            return false;
        }
        return true;

    }

    @Override
    public boolean convert(String inputFile, String toName) {

        try {
            ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(new File(toName)));
            zos.setLevel(0);

            writeToZip(zos, "mimetype", "application/epub+zip");
            writeToZip(zos, "META-INF/container.xml", container_xml);
            writeToZip(zos, "OEBPS/content.opf", content_opf);

            String encoding = findHeaderEncoding(inputFile);
            List<String> titles = getFb2Titles(inputFile, encoding);

            String ncx = genetateNCX(titles);
            writeToZip(zos, "OEBPS/fb2.ncx", ncx);

            ByteArrayOutputStream generateFb2File = generateFb2File(inputFile, encoding, false);
            writeToZip(zos, "OEBPS/fb2.fb2", new ByteArrayInputStream(generateFb2File.toByteArray()));
            LOG.d("Fb2Context convert true");
            zos.close();
            return true;
        } catch (Exception e) {
            LOG.d("Fb2Context convert false error");
            LOG.e(e);
        }
        LOG.d("Fb2Context convert false");
        return false;
    }

    public static boolean convertFolderToEpub(File inputFolder, File outputFile, String author, String title, List<OutlineLink> outline) {

        try {
            ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(outputFile));
            zos.setLevel(0);

            writeToZip(zos, "mimetype", "application/epub+zip");
            writeToZip(zos, "META-INF/container.xml", container_xml);

            String meta = content_opf.replace("fb2.fb2", "temp" + ExtUtils.REFLOW_HTML);
            meta = meta.replace("%title%", title);
            meta = meta.replace("%creator%", author);

            writeToZip(zos, "OEBPS/content.opf", meta);
            if (TxtUtils.isListNotEmpty(outline)) {
                writeToZip(zos, "OEBPS/fb2.ncx", genetateNCXbyOutline(outline));
            }

            for (File file : inputFolder.listFiles()) {
                writeToZip(zos, "OEBPS/" + file.getName(), new FileInputStream(file));
            }

            LOG.d("Fb2Context convert true");
            zos.close();
            return true;
        } catch (Exception e) {
            LOG.d("Fb2Context convert false error");
            LOG.e(e);
        }
        LOG.d("Fb2Context convert false");
        return false;
    }

    public ByteArrayOutputStream generateFb2File(String fb2, String encoding, boolean fixXML) throws Exception {
        BufferedReader input = new BufferedReader(new InputStreamReader(new FileInputStream(fb2), encoding));

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        PrintWriter writer = new PrintWriter(out);
        String line;

        int count = 0;

        if (BookCSS.get().isAutoHypens) {
            HypenUtils.applyLanguage(BookCSS.get().hypenLang);
        }

        boolean isFindBodyEnd = false;

        long init = System.currentTimeMillis();

        boolean firstLine = true;

        boolean ready = true;

        while ((line = input.readLine()) != null) {
            if (TempHolder.get().loadingCancelled) {
                break;
            }

            if (firstLine) {
                List<String> encodings = Arrays.asList("utf-8", "windows-1251", "Windows-1251", "windows-1252", "Windows-1252");
                for (String e : encodings) {
                    if (line.contains(e)) {
                        line = line.replace(e, "utf-8");
                        break;
                    }
                }
                firstLine = false;
                writer.println(line);
                continue;
            }

            if (fixXML) {
                line = line.replace("l:href==", "l:href=");
            }

            line = accurateLine(line);

            String subLine[] = line.split("</");

            line = null;

            for (int i = 0; i < subLine.length; i++) {
                if (i == 0) {
                    line = subLine[i];
                } else {
                    line = "</" + subLine[i];
                }

                if (!isFindBodyEnd) {

                    int indexOf = line.indexOf("</title>");
                    if (indexOf >= 0) {
                        ready = true;
                        count++;
                        line = line.substring(0, indexOf) + "<a id=\"" + count + "\"></a>" + line.substring(indexOf);
                    }

                    if (BookCSS.get().isCapitalLetter && ready) {
                        int indexP = line.indexOf("<p");
                        if (indexP >= 0) {
                            line = capitalLetter(line, indexP);
                            ready = false;
                        }
                    }
                }

                if (!isFindBodyEnd && line.contains("</body>")) {
                    isFindBodyEnd = true;
                }

                if (!isFindBodyEnd) {
                    if (BookCSS.get().isAutoHypens) {
                        line = HypenUtils.applyHypnes(line);
                    }
                }
                writer.println(line);
            }

        }

        long delta = System.currentTimeMillis() - init;
        LOG.d("generateFb2File", delta / 1000.0);
        input.close();
        writer.close();

        return out;
    }

    public String capitalLetter(String line, int indexOf) {
        String anchor = "<p";
        if (indexOf >= 0) {
            anchor = ">";
            indexOf = line.indexOf('>', indexOf);
        }
        if (indexOf >= 0 && line.length() - anchor.length() - indexOf >= 3) {
            indexOf = indexOf + anchor.length();

            if (line.charAt(indexOf) != '<') {

                if (!Character.isLetter(line.charAt(indexOf))) {
                    indexOf++;
                }

                if (!Character.isLetter(line.charAt(indexOf))) {
                    indexOf++;
                }

                if (Character.isLetter(line.charAt(indexOf))) {
                    line = line.substring(0, indexOf) + "<letter>" + line.substring(indexOf, indexOf + 1) + "</letter>" + line.substring(indexOf + 1);
                    LOG.d("check-line-new", line);
                }
            }

        }
        return line;
    }

    public static String accurateLine(String line) {
        if (AppState.get().isAccurateFontSize) {
            line = line.replace(TxtUtils.NON_BREAKE_SPACE, " ");
            line = line.replace(">" + TxtUtils.LONG_DASH1 + " ", ">" + TxtUtils.LONG_DASH1 + TxtUtils.NON_BREAKE_SPACE);
            line = line.replace(">" + TxtUtils.LONG_DASH2 + " ", ">" + TxtUtils.LONG_DASH2 + TxtUtils.NON_BREAKE_SPACE);
        }
        return line;
    }

    public static ByteArrayOutputStream generateHyphenFileEpub(InputStreamReader inputStream) throws Exception {
        BufferedReader input = new BufferedReader(inputStream);

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        PrintWriter writer = new PrintWriter(out);
        String line;

        HypenUtils.applyLanguage(BookCSS.get().hypenLang);

        while ((line = input.readLine()) != null) {
            if (TempHolder.get().loadingCancelled) {
                break;
            }
            line = accurateLine(line);
            line = HypenUtils.applyHypnes(line);
            writer.println(line);
        }
        writer.close();
        return out;
    }

    @Deprecated
    private ByteArrayOutputStream generateHyphenFileEpubOld(InputStreamReader inputStream) throws Exception {
        BufferedReader input = new BufferedReader(inputStream);

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        PrintWriter writer = new PrintWriter(out);
        String line;

        HypenUtils.applyLanguage(BookCSS.get().hypenLang);

        while ((line = input.readLine()) != null) {
            if (TempHolder.get().loadingCancelled) {
                break;
            }

            if (!line.endsWith(" ")) {
                line = line + " ";
            }

            line = accurateLine(line);

            String subLine[] = line.split("</");

            for (int i = 0; i < subLine.length; i++) {
                if (i == 0) {
                    line = subLine[i];
                } else {
                    line = "</" + subLine[i];
                }

                line = HypenUtils.applyHypnesOld2(line);
                writer.println(line);
            }
        }
        writer.close();
        return out;
    }

    public static String genetateNCX(List<String> titles) {
        StringBuilder navs = new StringBuilder();
        for (int i = 0; i < titles.size(); i++) {
            navs.append(createNavPoint(i + 1, titles.get(i)));
        }
        return NCX.replace("%nav%", navs.toString());
    }

    public static String genetateNCXbyOutline(List<OutlineLink> titles) {
        StringBuilder navs = new StringBuilder();
        for (int i = 0; i < titles.size(); i++) {
            OutlineLink link = titles.get(i);
            String titleTxt = link.getTitle();
            if (TxtUtils.isNotEmpty(titleTxt)) {
                String createNavPoint = createNavPoint(OutlineLinkWrapper.getPageNumber(link.getLink()), link.getLevel() + DIVIDER + titleTxt);
                createNavPoint = createNavPoint.replace("fb2.fb2", "temp-reflow.html");
                navs.append(createNavPoint);
            }
        }
        return NCX.replace("%nav%", navs.toString());
    }

    public static List<String> getFb2Titles(String fb2, String encoding) throws Exception {
        XmlPullParser xpp = XmlParser.buildPullParser();

        xpp.setInput(new FileInputStream(fb2), encoding);
        int eventType = xpp.getEventType();

        boolean isTitle = false;
        String title = "";
        List<String> titles = new ArrayList<String>();

        int section = 0;
        int dividerSection = -1;
        String dividerLine = null;
        while (eventType != XmlPullParser.END_DOCUMENT) {
            if (TempHolder.get().loadingCancelled) {
                break;
            }
            if (eventType == XmlPullParser.START_TAG) {
                if (xpp.getName().equals("section")) {
                    section++;
                }
                if (xpp.getName().equals("title")) {
                    isTitle = true;
                }

            } else if (eventType == XmlPullParser.END_TAG) {
                if (xpp.getName().equals("title")) {
                    isTitle = false;
                    title = "[" + xpp.getName() + "]" + title;
                    titles.add(section + DIVIDER + title);
                    title = "";
                    if (section == dividerSection) {
                        titles.remove(dividerLine);
                    }
                }
                if (xpp.getName().equals("section")) {
                    section--;
                }
                if (xpp.getName().equals("body")) {
                    break;
                }

            } else if (eventType == XmlPullParser.TEXT) {
                if (isTitle) {
                    title = title + " " + xpp.getText().trim();
                }
            }
            eventType = xpp.next();
        }
        if (!titles.isEmpty() && titles.get(titles.size() - 1).endsWith(DIVIDER)) {
            titles.remove(titles.size() - 1);
        }
        if (!titles.isEmpty() && titles.get(titles.size() - 1).endsWith(FOOTER_NOTES_SIGN)) {
            titles.remove(titles.size() - 1);
        }

        return titles;
    }

    public static String findHeaderEncoding(String fb2) {
        String encoding = "UTF-8";
        try {
            InputStream encodingCheck = new FileInputStream(fb2);
            byte[] header = new byte[80];
            encodingCheck.read(header);
            if (new String(header).toLowerCase(Locale.US).contains("windows-1251")) {
                encoding = "cp1251";
            } else if (new String(header).toLowerCase(Locale.US).contains("windows-1252")) {
                encoding = "cp1252";
            }
            encodingCheck.close();
            return encoding;
        } catch (Exception e) {
            return encoding;
        }
    }

    public static String container_xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" + //
            "<container version=\"1.0\" xmlns=\"urn:oasis:names:tc:opendocument:xmlns:container\">\n" + //
            "  <rootfiles>\n" + //
            "    <rootfile full-path=\"OEBPS/content.opf\" media-type=\"application/oebps-package+xml\"/>\n" + //
            "  </rootfiles>\n" + //
            "</container>";//

    public static String content_opf = "<?xml version=\"1.0\"?>\n" + //
            "<package version=\"2.0\" unique-identifier=\"uid\" xmlns=\"http://www.idpf.org/2007/opf\">\n" + //
            " <metadata xmlns:opf=\"http://www.idpf.org/2007/opf\" xmlns:dc=\"http://purl.org/dc/elements/1.1/\">\n" + //
            "  <dc:title>%title%</dc:title>\n" + //
            "  <dc:creator>%creator%</dc:creator>\n" + //
            "<meta name=\"cover\" content=\"cover.jpg\" />\n" + //
            " </metadata>\n" + //
            "\n" + "<manifest>\n" + //
            "  <item id=\"idBookFb2\" href=\"fb2.fb2\" media-type=\"application/xhtml+xml\"/>\n" + //
            "  <item id=\"idResourceFb2\" href=\"fb2.ncx\" media-type=\"application/x-dtbncx+xml\"/>\n" + //
            " </manifest>\n" + //
            " \n" + //
            "<spine toc=\"idResourceFb2\">\n" + //
            "  <itemref idref=\"idBookFb2\"/>\n" + //
            "</spine>\n" + //
            "</package>";//

    public static String NCX = "<?xml version=\"1.0\"?>\n" + //
            "<ncx version=\"2005-1\" xml:lang=\"en\" xmlns=\"http://www.daisy.org/z3986/2005/ncx/\">\n" + //
            " <head>\n" + //
            " </head>\n" + //
            " <docTitle>\n" + //
            "  <text>title</text>\n" + //
            " </docTitle>\n" + //
            " <navMap>\n" + //
            "  \n" + //
            "%nav% \n" + //
            "   \n" + //
            " </navMap>\n" + //
            "</ncx>";//

    public static String createNavPoint(int id, String text) {
        return "<navPoint id=\"toc-" + id + "\" playOrder=\"" + id + "\">\n" + //
                "<navLabel>\n" + //
                "<text>" + text + "</text>\n" + //
                "</navLabel>\n" + //
                "<content src=\"fb2.fb2#" + id + "\"/>\n" + //
                "</navPoint>"; //
    }

    public static void writeToZip(ZipOutputStream zos, String name, InputStream stream) throws IOException {
        zos.putNextEntry(new ZipEntry(name));
        zipCopy(stream, zos);
    }

    public static void writeToZipNoClose(ZipOutputStream zos, String name, InputStream stream) throws IOException {
        zos.putNextEntry(new ZipEntry(name));
        zipCopyNoClose(stream, zos);
    }

    public static void writeToZip(ZipOutputStream zos, String name, String content) throws IOException {
        writeToZip(zos, name, new ByteArrayInputStream(content.getBytes()));
    }

    private static final int BUFFER_SIZE = 16 * 1024;

    public static void zipCopy(InputStream inputStream, OutputStream zipStream) throws IOException {

        byte[] bytesIn = new byte[BUFFER_SIZE];
        int read = 0;
        while ((read = inputStream.read(bytesIn)) != -1) {
            zipStream.write(bytesIn, 0, read);
        }
        inputStream.close();
    }

    public static void zipCopyNoClose(InputStream inputStream, OutputStream zipStream) throws IOException {

        byte[] bytesIn = new byte[BUFFER_SIZE];
        int read = 0;
        while ((read = inputStream.read(bytesIn)) != -1) {
            zipStream.write(bytesIn, 0, read);
        }
    }

}
