package com.foobnix.ui2;

import java.io.File;
import java.io.IOException;
import java.util.Locale;

import org.ebookdroid.BookType;
import org.ebookdroid.common.cache.CacheManager;
import org.jsoup.Jsoup;
import org.jsoup.safety.Whitelist;

import com.foobnix.android.utils.LOG;
import com.foobnix.android.utils.TxtUtils;
import com.foobnix.dao2.FileMeta;
import com.foobnix.ext.CacheZipUtils;
import com.foobnix.ext.CacheZipUtils.CacheDir;
import com.foobnix.ext.CacheZipUtils.UnZipRes;
import com.foobnix.ext.CalirbeExtractor;
import com.foobnix.ext.EbookMeta;
import com.foobnix.ext.EpubExtractor;
import com.foobnix.ext.Fb2Extractor;
import com.foobnix.ext.MobiExtract;
import com.foobnix.ext.PdfExtract;
import com.foobnix.pdf.info.ExtUtils;

import android.app.Activity;
import android.support.v4.util.Pair;

public class FileMetaCore {
    private static FileMetaCore in = new FileMetaCore();

    public static FileMetaCore get() {
        return in;
    }

    public static void checkOrCreateMetaInfo(Activity a) {
        try {

            String path = CacheManager.getFilePathFromAttachmentIfNeed(a);
            if (!BookType.isSupportedExtByPath(path)) {
                path = a.getIntent().getData().getPath();
            }

            LOG.d("checkOrCreateMetaInfo", path);
            if (new File(path).isFile()) {
                FileMeta fileMeta = AppDB.get().getOrCreate(path);
                if (TxtUtils.isEmpty(fileMeta.getTitle())) {

                    EbookMeta ebookMeta = FileMetaCore.get().getEbookMeta(path, CacheDir.ZipApp, false);

                    FileMetaCore.get().upadteBasicMeta(fileMeta, new File(path));
                    FileMetaCore.get().udpateFullMeta(fileMeta, ebookMeta);

                    AppDB.get().update(fileMeta);
                    LOG.d("checkOrCreateMetaInfo", "UPDATE", path);
                } else {
                    LOG.d("checkOrCreateMetaInfo", "LOAD", path);
                }
            }
        } catch (Exception e) {
            LOG.e(e);
        }
    }

    public EbookMeta getEbookMeta(String path, CacheDir folder, boolean withPDF) {
        EbookMeta ebookMeta = EbookMeta.Empty();
        try {
            if (path.toLowerCase(Locale.US).endsWith(".zip")) {
                CacheZipUtils.cacheLock.lock();
                try {
                    UnZipRes res = CacheZipUtils.extracIfNeed(path, folder);
                    ebookMeta = getEbookMeta(path, res.unZipPath, res.entryName, withPDF);
                    ebookMeta.setUnzipPath(res.unZipPath);
                } finally {
                    CacheZipUtils.cacheLock.unlock();
                }
            } else {
                ebookMeta = getEbookMeta(path, path, null, withPDF);
                ebookMeta.setUnzipPath(path);
            }

        } catch (Exception e) {
            LOG.e(e);
        }
        return ebookMeta;

    }

    public static boolean isNeedToExtractPDFMeta(String path) {
        return !path.contains(" - ") && BookType.PDF.is(path);
    }

    private EbookMeta getEbookMeta(String path, String unZipPath, String child, boolean withDPF) throws IOException {
        EbookMeta ebookMeta = EbookMeta.Empty();
        String fileName = ExtUtils.getFileName(unZipPath);
        String fileNameOriginal = ExtUtils.getFileName(path);
        if (BookType.FB2.is(unZipPath) || BookType.FB2.is(unZipPath)) {
            fileNameOriginal = TxtUtils.encode1251(fileNameOriginal);
            fileName = TxtUtils.encode1251(fileNameOriginal);
        }

        if (CalirbeExtractor.isCalibre(unZipPath)) {
            ebookMeta = CalirbeExtractor.getBookMetaInformation(unZipPath);
            LOG.d("isCalibre find", unZipPath);
        } else if (BookType.EPUB.is(unZipPath)) {
            ebookMeta = EpubExtractor.get().getBookMetaInformation(unZipPath);
        } else if (BookType.FB2.is(unZipPath)) {
            ebookMeta = Fb2Extractor.get().getBookMetaInformation(unZipPath);
        } else if (BookType.MOBI.is(unZipPath)) {
            ebookMeta = MobiExtract.getBookMetaInformation(unZipPath, true);
        } else if (withDPF && isNeedToExtractPDFMeta(unZipPath)) {
            ebookMeta = PdfExtract.getBookMetaInformation(unZipPath);
        }

        if (TxtUtils.isEmpty(ebookMeta.getTitle())) {
            Pair<String, String> pair = TxtUtils.getTitleAuthorByPath(fileName);
            ebookMeta = new EbookMeta(pair.first, TxtUtils.isNotEmpty(ebookMeta.getAuthor()) ? ebookMeta.getAuthor() : pair.second);
        }

        if (ebookMeta.getsIndex() == null && (path.contains("_") || path.contains(")"))) {
            for (int i = 20; i >= 1; i--) {
                if (path.contains("_" + i + "_") || path.contains(" " + i + ")") || path.contains(" 0" + i + ")")) {
                    ebookMeta.setsIndex(i);
                    break;
                }
            }
        }

        if (ebookMeta.getsIndex() != null) {
            ebookMeta.setTitle(ebookMeta.getTitle() + " [" + ebookMeta.getsIndex() + "]");
        }

        if (path.endsWith(".zip") && !path.endsWith("fb2.zip")) {
            ebookMeta.setTitle("{" + fileNameOriginal + "} " + ebookMeta.getTitle());
        }

        return ebookMeta;
    }

    public static String getBookOverview(String path) {
        String info = "";
        try {

            if (CalirbeExtractor.isCalibre(path)) {
                return CalirbeExtractor.getBookOverview(path);
            }

            path = CacheZipUtils.extracIfNeed(path, CacheDir.ZipApp).unZipPath;

            if (BookType.EPUB.is(path)) {
                info = EpubExtractor.get().getBookOverview(path);
            } else if (BookType.FB2.is(path)) {
                info = Fb2Extractor.get().getBookOverview(path);
            } else if (BookType.MOBI.is(path)) {
                info = MobiExtract.getBookOverview(path);
            }
            if (TxtUtils.isEmpty(info)) {
                return "";
            }
            info = Jsoup.clean(info, Whitelist.none());
            info = info.replace("&nbsp;", " ");
        } catch (Exception e) {
            LOG.e(e);
        }
        return info;
    }

    public void udpateFullMeta(FileMeta fileMeta, EbookMeta meta) {
        fileMeta.setAuthor(meta.getAuthor());
        fileMeta.setTitle(meta.getTitle());
        fileMeta.setSequence(TxtUtils.firstUppercase(meta.getSequence()));
        fileMeta.setGenre(meta.getGenre());
        fileMeta.setAnnotation(meta.getAnnotation());
        fileMeta.setSIndex(meta.getsIndex());
        fileMeta.setChild(ExtUtils.getFileExtension(meta.getUnzipPath()));
        fileMeta.setLang(TxtUtils.toLowerCase(meta.getLang()));

    }

    public void upadteBasicMeta(FileMeta fileMeta, File file) {
        fileMeta.setTitle(file.getName());// temp

        fileMeta.setSize(file.length());
        fileMeta.setDate(file.lastModified());

        fileMeta.setExt(ExtUtils.getFileExtension(file));
        fileMeta.setSizeTxt(ExtUtils.readableFileSize(file.length()));
        fileMeta.setDateTxt(ExtUtils.getDateFormat(file));

        if (BookType.FB2.is(file.getName())) {
            fileMeta.setPathTxt(TxtUtils.encode1251(file.getName()));
        } else {
            fileMeta.setPathTxt(file.getName());

        }
    }

}
