package com.foobnix.ext;

import com.foobnix.android.utils.LOG;
import com.foobnix.android.utils.TxtUtils;
import com.foobnix.libmobi.LibMobi;
import com.foobnix.mobi.parser.MobiParserIS;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

public class MobiExtract {

    public static FooterNote extract(String inputPath, final String outputDir, String hashCode) throws IOException {
        try {
            LibMobi.convertToEpub(inputPath, new File(outputDir, hashCode + "").getPath());
            File result = new File(outputDir, hashCode + ".epub");
            return new FooterNote(result.getPath(), null);
        } catch (Exception e) {
            LOG.e(e);
        }
        return new FooterNote("", null);
    }

    public static EbookMeta getBookMetaInformation(String path, boolean onlyTitle) throws IOException {
        File file = new File(path);
        try {

            final FileInputStream is = new FileInputStream(file);
            MobiParserIS parse = new MobiParserIS(is);
            try {
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
            } finally {
                is.close();
                parse.close();
            }

        } catch (Throwable e) {
            LOG.e(e);
            return EbookMeta.Empty();
        }
    }

    public static String getBookOverview(String path) {
        String info = "";
        try {
            File file = new File(path);
            MobiParserIS parse = new MobiParserIS(new FileInputStream(file));
            try {
                info = parse.getDescription();
            } finally {
                parse.close();
            }
        } catch (Throwable e) {
            LOG.e(e);
        }
        return info;
    }

    public static byte[] getBookCover(String path) {
        LOG.d("getBookCover", path);
        try {
            File file = new File(path);
            MobiParserIS parse = new MobiParserIS(new FileInputStream(file));
            try {
                byte[] coverOrThumb = parse.getCoverOrThumb();
                return coverOrThumb;
            } finally {
                parse.close();
            }
        } catch (Throwable e) {
            LOG.e(e, path);
            LOG.e(e);
        }
        return null;
    }

}
