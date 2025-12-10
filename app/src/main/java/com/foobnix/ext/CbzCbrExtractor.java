package com.foobnix.ext;

import com.foobnix.android.utils.LOG;
import com.foobnix.android.utils.TxtUtils;
import com.foobnix.mobi.parser.IOUtils;
import com.foobnix.pdf.info.ExtUtils;
import com.foobnix.sys.ArchiveEntry;
import com.foobnix.sys.ZipArchiveInputStream;
import com.foobnix.sys.Zips;
import com.github.junrar.Archive;
import com.github.junrar.rarfile.FileHeader;

import org.ebookdroid.BookType;
import org.xmlpull.v1.XmlPullParser;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public class CbzCbrExtractor {
    public static boolean isZip(String path) {
        try {
            byte[] buffer = new byte[2];
            FileInputStream is = new FileInputStream(path);
            is.read(buffer);
            is.close();
            if (buffer[0] == 'P' && buffer[1] == 'K') {
                return true;
            }
        } catch (Exception e) {
            LOG.e(e);
        }
        return false;
    }

    public static boolean isDoc(String path) {
        try {
            byte[] buffer = new byte[2];
            FileInputStream is = new FileInputStream(path);
            is.read(buffer);
            is.close();
            LOG.d("isDOC buffer", buffer[0], buffer[1]);
            if (buffer[0] == -48 && buffer[1] == -49) {
                return true;
            }
        } catch (Exception e) {
            LOG.e(e);
        }
        return false;
    }

    public static int getPageCount(String path) {
        int count = 0;
        try {
            if (BookType.CBZ.is(path) || isZip(path)) {

                ZipArchiveInputStream zipInputStream = Zips.buildZipArchiveInputStream(path);
                ArchiveEntry nextEntry = null;
                while ((nextEntry = zipInputStream.getNextEntry()) != null) {
                    if (nextEntry.isDirectory()) {
                        continue;
                    }

                    String name = nextEntry.getName().toLowerCase(Locale.US);
                    if ("comicinfo.xml".equals(name)) {
                        continue;
                    }

                    count++;
                }
                zipInputStream.close();
            } else if (BookType.CBR.is(path)) {
                Archive archive = new Archive(new File(path));

                for (FileHeader it : archive.getFileHeaders()) {
                    if (it.isDirectory()) {
                        continue;
                    }

                    String name = it.getFileNameString().toLowerCase(Locale.US);
                    if ("comicinfo.xml".equals(name)) {
                        continue;
                    }

                    count++;
                }

                archive.close();
            }
        } catch (Exception e) {
            LOG.e(e);
        }
        return count;
    }

    public static String getBookOverview(String path) {
        String info = "";
        try {
            ZipArchiveInputStream zipInputStream = null;
            ByteArrayOutputStream byteArrayStream = null;
            ByteArrayInputStream inputStream = null;

            if (BookType.CBZ.is(path) || isZip(path)) {
                zipInputStream = Zips.buildZipArchiveInputStream(path);
                ArchiveEntry nextEntry = null;

                while ((nextEntry = zipInputStream.getNextEntry()) != null) {
                    String name = nextEntry.getName().toLowerCase(Locale.US);
                    if ("comicinfo.xml".equals(name)) {
                        break;
                    }
                }

                if (nextEntry == null) {
                    zipInputStream.close();
                    return "";
                }
            } else if (BookType.CBR.is(path)) {
                Archive archive = new Archive(new File(path));
                byteArrayStream = new ByteArrayOutputStream();

                for (FileHeader it : archive.getFileHeaders()) {
                    if (it.isDirectory()) {
                        continue;
                    }

                    String name = it.getFileNameString().toLowerCase(Locale.US);
                    if ("comicinfo.xml".equals(name)) {
                        archive.extractFile(it, byteArrayStream);
                        break;
                    }
                }
                archive.close();

                if (byteArrayStream.size() == 0) {
                    return "";
                }
            }

            XmlPullParser xpp = XmlParser.buildPullParser();
            if (zipInputStream != null) {
                xpp.setInput(zipInputStream, "utf-8");
            } else if (byteArrayStream != null) {
                inputStream = new ByteArrayInputStream(byteArrayStream.toByteArray());
                xpp.setInput(inputStream, "utf-8");
            }

            int eventType = xpp.getEventType();

            while (eventType != XmlPullParser.END_DOCUMENT) {
                if (eventType == XmlPullParser.START_TAG) {
                    if ("Summary".equals(xpp.getName())) {
                        info = xpp.nextText();
                        break;
                    }
                }
                eventType = xpp.next();
            }

            if (zipInputStream != null) {
                zipInputStream.close();
            } else if (byteArrayStream != null) {
                byteArrayStream.close();
                inputStream.close();
            }
        } catch (Exception e) {
            LOG.e(e);
        }
        return info;
    }

    public static EbookMeta getBookMetaInformation(String path) {
        try {
            ZipArchiveInputStream zipInputStream = null;
            ByteArrayOutputStream byteArrayStream = null;
            ByteArrayInputStream inputStream = null;

            String title = null;
            String author = "";
            String series = null;
            String number = null;
            String genre = "";
            String lang = null;
            String date = null;
            String publisher = "";
            String pageCount = null;
            String isbn = "";

            if (BookType.CBZ.is(path) || isZip(path)) {
                zipInputStream = Zips.buildZipArchiveInputStream(path);
                ArchiveEntry nextEntry = null;

                while ((nextEntry = zipInputStream.getNextEntry()) != null) {
                    String name = nextEntry.getName().toLowerCase(Locale.US);

                    if ("comicinfo.xml".equals(name)) {
                        break;
                    }
                }

                if (nextEntry == null) {
                    zipInputStream.close();
                    return EbookMeta.Empty();
                }
            } else if (BookType.CBR.is(path)) {
                Archive archive = new Archive(new File(path));
                byteArrayStream = new ByteArrayOutputStream();

                for (FileHeader it : archive.getFileHeaders()) {
                    if (it.isDirectory()) {
                        continue;
                    }

                    String name = it.getFileNameString().toLowerCase(Locale.US);
                    if ("comicinfo.xml".equals(name)) {
                        archive.extractFile(it, byteArrayStream);
                        break;
                    }
                }
                archive.close();

                if (byteArrayStream.size() == 0) {
                    return EbookMeta.Empty();
                }
            }

            XmlPullParser xpp = XmlParser.buildPullParser();
            if (zipInputStream != null) {
                xpp.setInput(zipInputStream, "utf-8");
            } else if (byteArrayStream != null) {
                inputStream = new ByteArrayInputStream(byteArrayStream.toByteArray());
                xpp.setInput(inputStream, "utf-8");
            }

            int eventType = xpp.getEventType();

            while (eventType != XmlPullParser.END_DOCUMENT) {
                if (eventType == XmlPullParser.START_TAG) {
                    if (title == null && "Title".equals(xpp.getName())) {
                        title = xpp.nextText();
                    }

                    if (series == null && "Series".equals(xpp.getName())) {
                        series = xpp.nextText();
                    }

                    if (number == null && "Number".equals(xpp.getName())) {
                        number = xpp.nextText();
                    }

                    if ("".equals(genre) && "Genre".equals(xpp.getName())) {
                        genre = xpp.nextText();
                    }

                    if ("Writer".equals(xpp.getName()) || "Penciller".equals(xpp.getName()) ||
                        "Inker".equals(xpp.getName()) || "Colorist".equals(xpp.getName()) ||
                        "Letterer".equals(xpp.getName()) || "CoverArtist".equals(xpp.getName()) ||
                        "Editor".equals(xpp.getName())) {
                        author = author + ", " + xpp.nextText();
                    }

                    if (date == null && "Year".equals(xpp.getName())) {
                        date = xpp.nextText();
                    }

                    if ("".equals(publisher) && "Publisher".equals(xpp.getName())) {
                        publisher = xpp.nextText();
                    }

                    if (pageCount == null && "PageCount".equals(xpp.getName())) {
                        pageCount = xpp.nextText();
                    }

                    if (lang == null && "LanguageISO".equals(xpp.getName())) {
                        lang = xpp.nextText();
                    }

                    if ("".equals(isbn) && "GTIN".equals(xpp.getName())) {
                        isbn = xpp.nextText();
                    }
                }
                if (eventType == XmlPullParser.END_TAG) {
                    if ("ComicInfo".equals(xpp.getName())) {
                        break;
                    }
                }
                eventType = xpp.next();
            }

            if (zipInputStream != null) {
                zipInputStream.close();
            } else if (byteArrayStream != null) {
                byteArrayStream.close();
                inputStream.close();
            }

            author = TxtUtils.replaceFirst(author, ", ", "");

            EbookMeta ebookMeta = new EbookMeta(title, author, series, genre.replaceAll(",$", ""));
            try {
                if (number != null) {
                    if (number.contains(".")) {
                        ebookMeta.setsIndex((int) Float.parseFloat(number));
                    } else {
                        ebookMeta.setsIndex(Integer.parseInt(number));
                    }
                    LOG.d("cbz/cbr", series, ebookMeta.getsIndex());
                }
            } catch (Exception e) {
                title = title + " [" + number + "]";
                ebookMeta.setTitle(title);
                LOG.d(e);
            }
            ebookMeta.setLang(lang);
            ebookMeta.setYear(date);
            ebookMeta.setPublisher(publisher);
            ebookMeta.setIsbn(isbn);
            if (pageCount != null) {
                try {
                    if (pageCount.contains(".")) {
                        ebookMeta.setPagesCount((int) Float.parseFloat(pageCount));
                    } else {
                        ebookMeta.setPagesCount(Integer.parseInt(pageCount));
                    }
                } catch (Exception e) {
                    ebookMeta.setPagesCount(getPageCount(path));
                    LOG.d(e);
                }
            } else {
                ebookMeta.setPagesCount(getPageCount(path));
            }
            return ebookMeta;
        } catch (Exception e) {
            LOG.e(e);
            return EbookMeta.Empty();
        }
    }

    public static byte[] getBookCover(String path) {

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try {
            if (BookType.CBZ.is(path) || isZip(path)) {

                ZipArchiveInputStream zipInputStream = new ZipArchiveInputStream(path);
                ArchiveEntry nextEntry = null;

                List<String> names = new ArrayList<String>();
                String firstTemp = null;
                while ((nextEntry = zipInputStream.getNextEntry()) != null) {

                    String name = nextEntry.getName();
                    String fileName = ExtUtils.getFileName(name);
                    if (firstTemp == null) {
                        firstTemp = fileName;
                    }
                    if (fileName.startsWith(".")) {
                        continue;
                    }

                    if (ExtUtils.isImagePath(name)) {
                        names.add(name);
                    }
                }
                zipInputStream.close();
                try {
                    Collections.sort(names);
                } catch (Exception e) {
                    LOG.e(e);
                }

                zipInputStream = new ZipArchiveInputStream(path);
                nextEntry = null;

                if (names.isEmpty() && firstTemp != null) {
                    names.add(firstTemp);
                }

                String first = names.get(0);
                LOG.d("cbz-Name-first", first);
                while ((nextEntry = zipInputStream.getNextEntry()) != null) {
                    if (nextEntry.getName().equals(first)) {
                        LOG.d("cbz-Name-first-eq", nextEntry.getName());
                        IOUtils.copyClose(zipInputStream, out);
                        break;
                    }
                }
                zipInputStream.close();
            } else if (BookType.CBR.is(path)) {
                Archive archive = new Archive(new File(path));

                List<FileHeader> fileHeaders = archive.getFileHeaders();
                Collections.sort(fileHeaders, new Comparator<FileHeader>() {
                    @Override public int compare(FileHeader o1, FileHeader o2) {
                        return o1.getFileNameString().compareTo(o2.getFileNameString());
                    }
                });

                FileHeader fileHeader = fileHeaders.get(0);

                if (fileHeader.isDirectory()) {
                    fileHeader = fileHeaders.get(1);
                }

                for (FileHeader it : fileHeaders) {
                    if (it.isDirectory()) {
                        continue;
                    }
                    String lowerCase = it.getFileNameString().toLowerCase(Locale.US);
                    if (lowerCase.contains("\\")) {
                        lowerCase = lowerCase.substring(lowerCase.indexOf("\\") + 1);
                    }

                    if (lowerCase.contains("cover")) {
                        fileHeader = it;
                        break;
                    }
                }

                LOG.d("fileHeader", fileHeader.getFileNameString());

                LOG.d("EXtract CBR", fileHeader.getFileNameString());
                archive.extractFile(fileHeader, out);
                archive.close();
            }
        } catch (Exception e) {
            LOG.e(e);
        }
        return out.toByteArray();
    }
}
