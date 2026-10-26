package com.foobnix.ext;

import com.foobnix.android.utils.LOG;
import com.foobnix.android.utils.TxtUtils;

import org.ebookdroid.droids.comic.codec.ComicDocument;
import org.xmlpull.v1.XmlPullParser;

import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/**
 * What the library shows of a comic archive - its cover, page count and ComicInfo.xml - read
 * the same way for a .cbz and a .cbr, with {@link NativeArchive}, out of the file as it is.
 */
public class CbzCbrExtractor {

    private static final String COMIC_INFO = "comicinfo.xml";

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

    /** The files in the archive, ComicInfo.xml aside: what a comic's pages are counted as. */
    public static int getPageCount(String path) {
        final NativeArchive archive = NativeArchive.open(path);
        if (archive == null) {
            return 0;
        }
        try {
            int count = 0;
            for (final String name : archive.getNames()) {
                if (name != null && !COMIC_INFO.equals(name.toLowerCase(Locale.US))) {
                    count++;
                }
            }
            return count;
        } finally {
            archive.close();
        }
    }

    /** The ComicInfo.xml at the top of the archive, or <code>null</code>. */
    private static byte[] readComicInfo(String path) {
        final NativeArchive archive = NativeArchive.open(path);
        if (archive == null) {
            return null;
        }
        try {
            final List<String> names = archive.getNames();
            for (int i = 0; i < names.size(); i++) {
                final String name = names.get(i);
                if (name != null && COMIC_INFO.equals(name.toLowerCase(Locale.US))) {
                    return archive.read(i);
                }
            }
            return null;
        } finally {
            archive.close();
        }
    }

    public static String getBookOverview(String path) {
        String info = "";
        try {
            final byte[] comicInfo = readComicInfo(path);
            if (comicInfo == null || comicInfo.length == 0) {
                return "";
            }

            XmlPullParser xpp = XmlParser.buildPullParser();
            xpp.setInput(new ByteArrayInputStream(comicInfo), "utf-8");

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
        } catch (Exception e) {
            LOG.e(e);
        }
        return info;
    }

    public static EbookMeta getBookMetaInformation(String path) {
        try {
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

            final byte[] comicInfo = readComicInfo(path);
            if (comicInfo == null || comicInfo.length == 0) {
                return EbookMeta.Empty();
            }

            XmlPullParser xpp = XmlParser.buildPullParser();
            xpp.setInput(new ByteArrayInputStream(comicInfo), "utf-8");

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

    /**
     * The cover: an image whose file name says it is the cover, or else the first page in
     * reading order - the page the comic engine opens on.
     *
     * @return the image's bytes, empty when the archive holds no image or cannot be read
     */
    public static byte[] getBookCover(String path) {
        final NativeArchive archive = NativeArchive.open(path);
        if (archive == null) {
            return new byte[0];
        }
        try {
            final List<String> names = archive.getNames();
            final List<Integer> images = new ArrayList<Integer>();
            for (int i = 0; i < names.size(); i++) {
                if (ComicDocument.isImage(names.get(i))) {
                    images.add(i);
                }
            }
            if (images.isEmpty()) {
                return new byte[0];
            }
            Collections.sort(images, new Comparator<Integer>() {
                @Override
                public int compare(final Integer a, final Integer b) {
                    return ComicDocument.naturalCompare(names.get(a), names.get(b));
                }
            });

            int cover = images.get(0);
            for (final int i : images) {
                if (fileName(names.get(i)).contains("cover")) {
                    cover = i;
                    break;
                }
            }
            LOG.d("comic cover", path, names.get(cover));

            final byte[] bytes = archive.read(cover);
            return bytes != null ? bytes : new byte[0];
        } finally {
            archive.close();
        }
    }

    /** The name without its folders, lower-cased; RAR on Windows separates them with a backslash. */
    private static String fileName(final String name) {
        final String lower = name.toLowerCase(Locale.US);
        final int slash = Math.max(lower.lastIndexOf('/'), lower.lastIndexOf('\\'));
        return lower.substring(slash + 1);
    }
}
