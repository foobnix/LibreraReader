package com.foobnix.ui2;

import android.provider.DocumentsContract;
import android.app.Activity;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.DocumentsContract.Document;

import androidx.core.util.Pair;

import com.foobnix.LibreraApp;
import com.foobnix.android.utils.LOG;
import com.foobnix.android.utils.TxtUtils;
import com.foobnix.dao2.FileMeta;
import com.foobnix.ext.CacheZipUtils;
import com.foobnix.ext.CacheZipUtils.CacheDir;
import com.foobnix.ext.CacheZipUtils.UnZipRes;
import com.foobnix.ext.CalirbeExtractor;
import com.foobnix.ext.CbzCbrExtractor;
import com.foobnix.ext.DjvuExtract;
import com.foobnix.ext.DocxExtractor;
import com.foobnix.ext.EbookMeta;
import com.foobnix.ext.EpubExtractor;
import com.foobnix.ext.Fb2Extractor;
import com.foobnix.ext.MobiExtract;
import com.foobnix.ext.PdfExtract;
import com.foobnix.ext.PubDate;
import com.foobnix.model.AppState;
import com.foobnix.pdf.info.Clouds;
import com.foobnix.pdf.info.ExtUtils;

import org.ebookdroid.BookType;
import org.ebookdroid.droids.FolderContext;
import org.jsoup.Jsoup;
import org.jsoup.safety.Safelist;

import java.io.File;
import java.io.IOException;

public class FileMetaCore {

    public static int STATE_NONE = 0;
    public static int STATE_BASIC = 1;
    public static int STATE_FULL = 2;

    private static FileMetaCore in = new FileMetaCore();

    public static FileMetaCore get() {
        return in;
    }

    public static void checkOrCreateMetaInfo(Activity a) {
        try {

            String path = a.getIntent().getData().getPath();

            LOG.d("checkOrCreateMetaInfo", path);
            if (new File(path).isFile()) {
                FileMeta fileMeta = AppDB.get().getOrCreate(path);
                if (fileMeta.getState() != FileMetaCore.STATE_FULL) {
                    EbookMeta ebookMeta = FileMetaCore.get().getEbookMeta(path, CacheDir.ZipApp, true);

                    FileMetaCore.get().upadteBasicMeta(fileMeta, new File(path));
                    FileMetaCore.get().udpateFullMeta(fileMeta, ebookMeta);

                    AppDB.get().update(fileMeta);
                    LOG.d("checkOrCreateMetaInfo", "UPDATE", path);
                }
            }
        } catch (Exception e) {
            LOG.e(e);
        }
    }

    public static void createMetaIfNeedSafe(String path, final boolean isSearhcBook) {
        //if (isSafeToExtactBook(path)) {
        createMetaIfNeed(path, isSearhcBook);
        //}

    }

    public static FileMeta createMetaIfNeed(String path, final boolean isSearhcBook) {


        FileMeta fileMeta = AppDB.get().getOrCreate(path);

        LOG.d("BooksService-createMetaIfNeed", path, fileMeta.getState());

        try {
            if (FileMetaCore.STATE_FULL != fileMeta.getState()) {
                if (ExtUtils.isExteralSD(path)) {
                    // There is no file behind a document uri: what a row shows is asked of the
                    // provider, and the rest is read from a copy that is thrown away at once.
                    FileMetaCore.get().upadteBasicMetaSaf(LibreraApp.context, fileMeta);
                    FileMetaCore.get().udpateFullMetaSaf(LibreraApp.context, fileMeta);
                } else {
                    EbookMeta ebookMeta = FileMetaCore.get().getEbookMeta(path, CacheDir.ZipApp, true);

                    FileMetaCore.get().upadteBasicMeta(fileMeta, new File(path));
                    FileMetaCore.get().udpateFullMeta(fileMeta, ebookMeta);
                }


                if (isSearhcBook) {
                    fileMeta.setIsSearchBook(isSearhcBook);
                }

                AppDB.get().update(fileMeta);
                LOG.d("BooksService", isSearhcBook, "UPDATE", path);
            }
        } catch (Exception e) {
            LOG.e(e);
        }
        return fileMeta;

    }

    public static boolean isNeedToExtractPDFMeta(String path) {
        return !path.contains(" - ") && BookType.PDF.is(path);
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
            } else if (BookType.DJVU.is(path)) {
                info = DjvuExtract.getBookOverview(path);
            } else if (BookType.PDF.is(path)) {
                info = PdfExtract.getBookOverview(path);
            } else if (BookType.CBR.is(path) || BookType.CBZ.is(path)) {
                info = CbzCbrExtractor.getBookOverview(path);
            }
            if (TxtUtils.isEmpty(info)) {
                return "";
            }
            try {
                info = Jsoup.clean(info, Safelist.none());
                info = info.replace("&nbsp;", " ");
            } catch (Throwable e1) {
                LOG.e(e1);
            }
        } catch (Exception e) {
            LOG.e(e);
        }
        return info;
    }

    public static String replaceComma(String input) {
        if (TxtUtils.isEmpty(input)) {
            return input;
        }
        input = input.replace("; ", ",").replace(";", ",");

        if (!input.endsWith(",")) {
            input = input + ",";
        }

        if (!input.startsWith(",")) {
            input = "," + input;
        }

        return input;
    }

    public static void reUpdateIfNeed(FileMeta fileMeta) {
        if (fileMeta.getState() != null && fileMeta.getState() != FileMetaCore.STATE_FULL) {
            LOG.d("reupdateIfNeed 1", fileMeta.getPath(), fileMeta.getState());
            EbookMeta ebookMeta = FileMetaCore.get().getEbookMeta(fileMeta.getPath(), CacheDir.ZipApp, true);
            FileMetaCore.get().upadteBasicMeta(fileMeta, new File(fileMeta.getPath()));
            FileMetaCore.get().udpateFullMeta(fileMeta, ebookMeta);
            AppDB.get().save(fileMeta);
            LOG.d("reupdateIfNeed 2", fileMeta.getPath(), fileMeta.getState());
        }
    }

    public static int extractYear(String input) {
        return PubDate.yearOf(input);
    }

    public EbookMeta getEbookMeta(String path, CacheDir folder, boolean extract) {


        if (Clouds.isCacheFileExist(path)) {
            path = Clouds.getCacheFile(path).getPath();
        }

        EbookMeta ebookMeta = EbookMeta.Empty();
        try {
            if (ExtUtils.isZip(path)) {
                CacheZipUtils.cacheLock.lock();
                try {
                    UnZipRes res = CacheZipUtils.extracIfNeed(path, folder);
                    if (extract) {
                        ebookMeta = getEbookMeta(path, res.unZipPath, res.entryName);
                    }
                    ebookMeta.setUnzipPath(res.unZipPath);
                } finally {
                    CacheZipUtils.cacheLock.unlock();
                }
            } else {
                if (extract) {
                    ebookMeta = getEbookMeta(path, path, null);
                }
                ebookMeta.setUnzipPath(path);
            }

        } catch (Exception e) {
            LOG.w(e);
        }
        return ebookMeta;

    }

    private EbookMeta getEbookMeta(String path, String unZipPath, String child) throws IOException {
        EbookMeta ebookMeta = EbookMeta.Empty();
        String fileName = ExtUtils.getFileName(unZipPath);
        String fileNameOriginal = ExtUtils.getFileName(path);

        if (AppState.get().isShowOnlyOriginalFileNames) {
            ebookMeta.setTitle(fileName);
            return ebookMeta;
        }


        if (BookType.FB2.is(unZipPath)) {
            fileNameOriginal = TxtUtils.encode1251(fileNameOriginal);
            fileName = TxtUtils.encode1251(fileNameOriginal);
        }
        if (AppState.get().isUseCalibreOpf && CalirbeExtractor.isCalibre(unZipPath)) {
            ebookMeta = CalirbeExtractor.getBookMetaInformation(unZipPath);
            LOG.d("isCalibre find", unZipPath, ebookMeta.coverImage);
        } else if (BookType.EPUB.is(unZipPath) || BookType.ODT.is(unZipPath)) {
            ebookMeta = EpubExtractor.get().getBookMetaInformation(unZipPath);
        } else if (BookType.FB2.is(unZipPath)) {
            ebookMeta = Fb2Extractor.get().getBookMetaInformation(unZipPath);
        } else if (BookType.MOBI.is(unZipPath)) {
            ebookMeta = MobiExtract.getBookMetaInformation(unZipPath, true);
        } else if (BookType.DJVU.is(unZipPath)) {
            ebookMeta = DjvuExtract.getBookMetaInformation(unZipPath);
        } else if (BookType.DOCX.is(unZipPath)) {
            ebookMeta.setLang(DocxExtractor.getLang(unZipPath));
        } else if (BookType.JSON.is(unZipPath)) {
            ebookMeta.setLang("en");
        } else if (BookType.PDF.is(unZipPath)) {
            boolean needExtractMeta = AppState.get().isAuthorTitleFromMetaPDF ? true : isNeedToExtractPDFMeta(unZipPath);
            EbookMeta local = PdfExtract.getBookMetaInformation(unZipPath);
            if (needExtractMeta) {
                ebookMeta.setTitle(local.getTitle());
                ebookMeta.setAuthor(local.getAuthor());
            }
            ebookMeta.setKeywords(local.getKeywords());
            ebookMeta.setGenre(local.getGenre());
            ebookMeta.setSequence(local.getSequence());
            ebookMeta.setPagesCount(local.getPagesCount());
            ebookMeta.setYear(local.getYear());
            ebookMeta.setIsbn(local.getIsbn());
            ebookMeta.setPublisher(local.getPublisher());

        } else if (BookType.CBR.is(unZipPath) || BookType.CBZ.is(unZipPath)) {
            ebookMeta = CbzCbrExtractor.getBookMetaInformation(unZipPath);
        } else if (BookType.FOLDER.is(unZipPath)) {
            File parent = new File(unZipPath).getParentFile();
            ebookMeta.setTitle(parent.getName());
            ebookMeta.setPagesCount(FolderContext.getPageCount(unZipPath));
        }

        try {
            String sequence = ebookMeta.getSequence();
            if (sequence != null && sequence.contains(",")) {
                String[] split = sequence.split(",");
                String name = split[0].trim();
                String intger = split[1].trim();

                int intValue = Integer.parseInt(intger);
                ebookMeta.setSequence(name);
                ebookMeta.setsIndex(intValue);
            }
        } catch (Exception e) {
            LOG.e(e);
        }

        if (TxtUtils.isEmpty(ebookMeta.getTitle())) {
            Pair<String, String> pair = TxtUtils.getTitleAuthorByPath(fileName);
            ebookMeta.setTitle(pair.first);
            ebookMeta.setAuthor(TxtUtils.isNotEmpty(ebookMeta.getAuthor()) ? ebookMeta.getAuthor() : pair.second);
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
            //ebookMeta.setTitle("[" + ebookMeta.getsIndex() + "] " + ebookMeta.getTitle());
        }

        if (ExtUtils.isZip(path) && !unZipPath.endsWith("fb2")) {
            ebookMeta.setTitle("{" + fileNameOriginal + "} " + ebookMeta.getTitle());
        }

        if (AppState.get().isFirstSurname) {
            String before = ebookMeta.getAuthor();
            ebookMeta.setAuthor(TxtUtils.replaceLastFirstNameSplit(before));
            LOG.d("isFirstSurname1", before, "=>", ebookMeta.getAuthor());
        }

        String date = ebookMeta.getYear();
        if (date != null) {
            ebookMeta.setYear(PubDate.published(date));
        }

        return ebookMeta;
    }

    public void udpateFullMeta(FileMeta fileMeta, EbookMeta meta) {
        fileMeta.setAuthor(TxtUtils.trim(meta.getAuthor()));
        fileMeta.setTitle(meta.getTitle());
        fileMeta.setSequence(replaceComma(TxtUtils.firstUppercase(meta.getSequence())));
        fileMeta.setGenre(replaceComma(meta.getGenre()));
        fileMeta.setAnnotation(meta.getAnnotation());
        fileMeta.setSIndex(meta.getsIndex());
        fileMeta.setChild(ExtUtils.getFileExtension(meta.getUnzipPath()));
        fileMeta.setLang(TxtUtils.toLowerCase(meta.getLang()));
        fileMeta.setKeyword(replaceComma(meta.getKeywords()));
        int pagesCount = meta.getPagesCount();
        if (pagesCount != 0) {
            fileMeta.setPages(pagesCount);
        }

        // Set even when there is none, so a year read wrongly before — Calibre's "no date"
        // taken for the year 100 — goes when the book is read again.
        String pubDate = PubDate.published(meta.getYear());
        fileMeta.setPubDate(pubDate);
        int year = extractYear(pubDate);
        fileMeta.setYear(year > 0 ? year : null);
        fileMeta.setPublisher(meta.getPublisher());
        fileMeta.setIsbn(meta.getIsbn());

        fileMeta.setState(STATE_FULL);

    }

    /**
     * What {@link #upadteBasicMeta} fills in, for a book on a folder added through the system's
     * folder picker: there is no file to measure, so the figures come from the query that found
     * the document.
     */
    public void upadteBasicMetaSaf(FileMeta fileMeta, String displayName, long size, long date, String parentName) {
        fileMeta.setTitle(displayName);

        fileMeta.setSize(size);
        fileMeta.setDate(date);
        fileMeta.setParentPath(parentName);

        fileMeta.setExt(ExtUtils.getFileExtension(displayName));
        fileMeta.setSizeTxt(ExtUtils.readableFileSize(size));
        fileMeta.setDateTxt(ExtUtils.getDateFormat(date));

        if (BookType.FB2.is(displayName)) {
            fileMeta.setPathTxt(TxtUtils.encode1251(displayName));
        } else {
            fileMeta.setPathTxt(displayName);
        }
        fileMeta.setState(STATE_BASIC);
    }

    /**
     * The name, size and date of a document on added storage, read from its provider, for a book
     * known only by its uri. Saved, so it is asked once.
     */
    public void updateBasicMetaSaf(Context c, FileMeta fileMeta) {
        if (c == null || fileMeta == null) {
            return;
        }
        Uri uri = Uri.parse(fileMeta.getPath());
        String[] columns = {DocumentsContract.Document.COLUMN_DISPLAY_NAME, DocumentsContract.Document.COLUMN_SIZE,
                DocumentsContract.Document.COLUMN_LAST_MODIFIED};
        try (Cursor cursor = c.getContentResolver().query(uri, columns, null, null, null)) {
            if (cursor == null || !cursor.moveToFirst()) {
                return;
            }
            String name = cursor.getString(0);
            if (TxtUtils.isEmpty(name)) {
                return;
            }
            String parent = "";
            String id = Uri.decode(DocumentsContract.getDocumentId(uri));
            int slash = id.lastIndexOf('/');
            if (slash > 0) {
                parent = id.substring(0, slash);
            }
            String title = fileMeta.getTitle();
            upadteBasicMetaSaf(fileMeta, name, cursor.getLong(1), cursor.getLong(2), parent);
            // the title the book was read with stays: it is more than the file name
            if (TxtUtils.isNotEmpty(title) && !title.equals(name)) {
                fileMeta.setTitle(title);
            }
            AppDB.get().save(fileMeta);
        } catch (Exception e) {
            LOG.e(e, fileMeta.getPath());
        }
    }

    /**
     * Reads a book's author, series and the rest out of a document on added storage. The readers
     * want a file, so the book is copied out, read and the copy thrown away at once - a library on
     * added storage then costs one book of disk at a time instead of all of them.
     */
    public void udpateFullMetaSaf(Context c, FileMeta fileMeta) {
        if (c == null || fileMeta == null) {
            return;
        }
        if (TxtUtils.isEmpty(fileMeta.getTitle()) || fileMeta.getState() == null) {
            upadteBasicMetaSaf(c, fileMeta);
        }
        File copy = CacheZipUtils.getSafScanCopy(c, fileMeta.getPath(), fileMeta.getTitle());
        if (copy == null) {
            return;
        }
        try {
            String displayName = fileMeta.getTitle();
            EbookMeta ebookMeta = getEbookMeta(copy.getPath(), CacheDir.ZipService, true);
            udpateFullMeta(fileMeta, ebookMeta);
            // A book that says nothing about itself keeps the name it is filed under, rather than
            // the name of the copy it was read from.
            if (TxtUtils.isEmpty(fileMeta.getTitle())) {
                fileMeta.setTitle(displayName);
            }
        } catch (Exception e) {
            LOG.e(e);
        } finally {
            CacheZipUtils.removeSafScanCopy(copy);
        }
    }

    /**
     * The same as {@link #upadteBasicMetaSaf(FileMeta, String, long, long, String)} for a document
     * met on its own, with no listing to take the figures from: it asks the provider for them.
     */
    public boolean upadteBasicMetaSaf(Context c, FileMeta fileMeta) {
        Cursor cursor = null;
        try {
            cursor = c.getContentResolver()
                      .query(Uri.parse(fileMeta.getPath()), new String[]{ //
                              Document.COLUMN_DISPLAY_NAME, //
                              Document.COLUMN_LAST_MODIFIED, //
                              Document.COLUMN_SIZE, //
                      }, null, null, null);
            if (cursor != null && cursor.moveToFirst()) {
                long size = cursor.isNull(2) ? 0 : cursor.getLong(2);
                long date = cursor.isNull(1) ? 0 : cursor.getLong(1);
                upadteBasicMetaSaf(fileMeta, cursor.getString(0), size, date, fileMeta.getParentPath());
                return true;
            }
        } catch (Exception e) {
            LOG.e(e);
        } finally {
            if (cursor != null) {
                try {
                    cursor.close();
                } catch (Exception ignored) {
                }
            }
        }
        return false;
    }

    public void upadteBasicMeta(FileMeta fileMeta, File file) {
        fileMeta.setTitle(file.getName());// temp

        fileMeta.setSize(file.length());
        fileMeta.setDate(file.lastModified());
        fileMeta.setParentPath(file.getParent());

        fileMeta.setExt(ExtUtils.getFileExtension(file));
        fileMeta.setSizeTxt(ExtUtils.readableFileSize(file.length()));
        if (BookType.TXT.is(file.getName())) {
            fileMeta.setDateTxt(ExtUtils.getDateTimeFormat(file));
        } else {
            fileMeta.setDateTxt(ExtUtils.getDateFormat(file));

        }

        if (BookType.FB2.is(file.getName())) {
            fileMeta.setPathTxt(TxtUtils.encode1251(file.getName()));
        } else {
            fileMeta.setPathTxt(file.getName());

        }
        fileMeta.setState(STATE_BASIC);
    }

}
