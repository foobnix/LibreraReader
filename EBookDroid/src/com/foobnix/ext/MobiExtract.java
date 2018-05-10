package com.foobnix.ext;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import org.apache.commons.compress.utils.IOUtils;

import com.foobnix.android.utils.LOG;
import com.foobnix.android.utils.TxtUtils;
import com.foobnix.libmobi.LibMobi;
import com.foobnix.mobi.parser.MobiParser;

public class MobiExtract {

    public static FooterNote extract(String inputPath, final String outputDir, String hashCode) throws IOException {
        try {
            LibMobi.convertToEpub(inputPath, new File(outputDir, hashCode + "").getPath());
            File result = new File(outputDir, hashCode + hashCode + ".epub");
            return new FooterNote(result.getPath(), null);
        } catch (Exception e) {
            LOG.e(e);
        }
        return new FooterNote("", null);
    }

    public static EbookMeta getBookMetaInformation(String path, boolean onlyTitle) throws IOException {
        File file = new File(path);
        try {
            byte[] raw = IOUtils.toByteArray(new FileInputStream(file));

            MobiParser parse = new MobiParser(raw);
            String title = parse.getTitle();
            String author = parse.getAuthor();
            String subject = parse.getSubject();
            String lang = parse.getLanguage();
            String year = parse.getPublishDate();
            String publisher = parse.getPublisher();
            String ibsn = parse.getIsbn();


            if (TxtUtils.isEmpty(title)) {
                title = file.getName();
            }
            byte[] decode = null;
            if (!onlyTitle) {
                decode = parse.getCoverOrThumb();
            }

            EbookMeta ebookMeta = new EbookMeta(title, author, decode);
            ebookMeta.setGenre(subject);
            ebookMeta.setLang(lang);
            ebookMeta.setYear(year);
            ebookMeta.setPublisher(publisher);
            ebookMeta.setIsbn(ibsn);
            // ebookMeta.setPagesCount(parse.getBookSize() / 1024);
            return ebookMeta;

        } catch (Throwable e) {
            LOG.e(e);
            return EbookMeta.Empty();
        }
    }

    public static String getBookOverview(String path) {
        String info = "";
        try {
            File file = new File(path);
            byte[] raw = IOUtils.toByteArray(new FileInputStream(file));

            MobiParser parse = new MobiParser(raw);
            info = parse.getDescription();

        } catch (Throwable e) {
            LOG.e(e);
        }
        return info;
    }

    public static byte[] getBookCover(String path) {
        try {
            File file = new File(path);
            FileInputStream fileInputStream = new FileInputStream(file);
            try {
                byte[] raw = IOUtils.toByteArray(fileInputStream);
                MobiParser parse = new MobiParser(raw);
                byte[] coverOrThumb = parse.getCoverOrThumb();
                parse = null;
                raw = null;
                return coverOrThumb;
            } finally {
                fileInputStream.close();
            }
        } catch (Throwable e) {
            LOG.e(e);
        }
        return null;
    }

}
