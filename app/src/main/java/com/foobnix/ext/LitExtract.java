package com.foobnix.ext;

import com.foobnix.android.utils.LOG;
import com.foobnix.android.utils.TxtUtils;
import com.foobnix.ext.lit.LitEpub;

import java.io.File;
import java.io.IOException;

/**
 * Microsoft Reader (.lit) books, read by converting them to EPUB: see {@link LitEpub}.
 */
public class LitExtract {

    public static FooterNote extract(String inputPath, final String outputDir, String hashCode) throws IOException {
        final File result = new File(outputDir, hashCode + ".epub");
        try {
            final long start = System.currentTimeMillis();
            final LitEpub.Meta meta = LitEpub.convert(new File(inputPath), result);
            LOG.d("LitExtract", "converted", inputPath, System.currentTimeMillis() - start, "ms", "damage", meta.damage);
            return new FooterNote(result.getPath(), null);
        } catch (Exception e) {
            LOG.e(e, inputPath);
        } catch (OutOfMemoryError e) {
            LOG.e(e, inputPath);
        }
        return new FooterNote("", null);
    }

    public static EbookMeta getBookMetaInformation(String path, boolean onlyTitle) {
        try {
            final LitEpub.Meta meta = LitEpub.meta(new File(path), !onlyTitle);
            String title = meta.title;
            if (TxtUtils.isEmpty(title)) {
                title = new File(path).getName();
            }
            final EbookMeta ebookMeta = new EbookMeta(title, TxtUtils.joinList(", ", meta.authors), meta.cover);
            ebookMeta.setLang(meta.language);
            ebookMeta.setPublisher(meta.publisher);
            ebookMeta.setGenre(meta.subject);
            ebookMeta.setYear(meta.date);
            return ebookMeta;
        } catch (Throwable e) {
            LOG.e(e, path);
            return EbookMeta.Empty();
        }
    }

    public static String getBookOverview(String path) {
        try {
            final String description = LitEpub.meta(new File(path), false).description;
            return description == null ? "" : description;
        } catch (Throwable e) {
            LOG.e(e, path);
        }
        return "";
    }

    public static byte[] getBookCover(String path) {
        try {
            return LitEpub.meta(new File(path), true).cover;
        } catch (Throwable e) {
            LOG.e(e, path);
        }
        return null;
    }
}
