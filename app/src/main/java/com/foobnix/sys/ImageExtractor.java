package com.foobnix.sys;

import static com.foobnix.pdf.info.io.SearchCore.SUPPORTED_EXT_FILES_ONLY;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.util.Base64;
import android.util.Pair;


import com.BaseExtractor;
import com.foobnix.LibreraApp;
import com.foobnix.android.utils.Dips;
import com.foobnix.android.utils.LOG;
import com.foobnix.android.utils.Safe;
import com.foobnix.dao2.FileMeta;
import com.foobnix.ext.CacheZipUtils;
import com.foobnix.ext.CacheZipUtils.CacheDir;
import com.foobnix.ext.CalirbeExtractor;
import com.foobnix.ext.CbzCbrExtractor;
import com.foobnix.ext.EbookMeta;
import com.foobnix.ext.EpubExtractor;
import com.foobnix.ext.Fb2Extractor;
import com.foobnix.ext.LitExtract;
import com.foobnix.ext.MobiExtract;
import com.foobnix.ext.OdtExtractor;
import com.foobnix.ext.office.RtfToEpub;
import com.foobnix.model.AppSP;
import com.foobnix.model.AppState;
import com.foobnix.pdf.info.AnnotationsFinder;
import com.foobnix.pdf.info.AppsConfig;
import com.foobnix.pdf.info.Clouds;
import com.foobnix.pdf.info.Android6;
import com.foobnix.pdf.info.ExtUtils;
import com.foobnix.pdf.info.IMG;
import com.foobnix.pdf.info.PageUrl;
import com.foobnix.pdf.info.R;
import com.foobnix.pdf.info.TintUtil;
import com.foobnix.pdf.info.model.BookCSS;
import com.foobnix.librerax.BookOpenGuard;
import com.foobnix.pdf.info.wrapper.MagicHelper;
import com.foobnix.pdf.search.activity.PageImageState;
import com.foobnix.ui2.AppDB;
import com.foobnix.ui2.FileMetaCore;
import com.foobnix.ui2.fragment.BrowseFragment2;
import com.google.common.collect.Lists;

import org.ebookdroid.BookType;
import org.ebookdroid.common.bitmaps.BitmapRef;
import org.ebookdroid.common.bitmaps.RawBitmap;
import org.ebookdroid.core.codec.CodecContext;
import org.ebookdroid.core.codec.CodecDocument;
import org.ebookdroid.core.codec.CodecPage;
import org.ebookdroid.core.codec.CodecPageInfo;
import org.ebookdroid.core.crop.PageCropper;
import org.ebookdroid.droids.FolderContext;
import org.ebookdroid.droids.MdContext;
import org.ebookdroid.droids.mupdf.codec.PdfContext;
import org.ebookdroid.droids.mupdf.codec.TextWord;
import org.ebookdroid.droids.mupdf.codec.exceptions.MuPdfPasswordException;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.ebookdroid.core.codec.Annotation;
import org.ebookdroid.core.codec.PageLink;
import org.greenrobot.eventbus.EventBus;
import com.foobnix.pdf.search.activity.msg.InvalidateMessage;
import java.util.Locale;


public class ImageExtractor {

    public static final int COVER_PAGE_WITH_EFFECT = -3;
    public static final int COVER_PAGE_NO_EFFECT = -2;
    public static final int COVER_PAGE = -1;
    public static SharedPreferences sp;
    public static volatile CodecDocument codeCache;
    public static volatile CodecContext codecContex;
    public static String pathCache;
    static int pageCount = 0;
    static int whCache;
    private static ImageExtractor instance;
    private final Context c;

    private ImageExtractor(final Context c) {
        this.c = c;
    }

    public static synchronized ImageExtractor getInstance(final Context c) {
        if (instance == null) {
            instance = new ImageExtractor(c);
        }
        sp = c.getSharedPreferences("Errors", Context.MODE_PRIVATE);
        return instance;
    }

    public static void clearErrors() {
        if (sp != null) {
            sp.edit().clear().commit();
        }
        if (instance != null) {
            final File[] marks = instance.markDir().listFiles();
            if (marks != null) {
                for (File mark : marks) {
                    mark.delete();
                }
            }
        }
    }

    public static Bitmap cropBitmap(Bitmap bitmap, Bitmap sample) {
        return cropBitmap(bitmap, sample, null);
    }

    /**
     * @param appliedCrop optional out-parameter; receives the normalized (0..1) rectangle of the
     *                    source bitmap that was actually kept. Text and link coordinates are
     *                    reported by the codec in full-page space, so they have to be mapped
     *                    through this rectangle to line up with the cropped bitmap.
     */
    public static Bitmap cropBitmap(Bitmap bitmap, Bitmap sample, RectF appliedCrop) {
        final int srcW = bitmap.getWidth();
        final int srcH = bitmap.getHeight();

        final Rect rootRect = new Rect(0, 0, sample.getWidth(), sample.getHeight());
        RectF rectCrop = PageCropper.getCropBounds(sample, rootRect, new RectF(0, 0, 1f, 1f));
        int x = (int) (srcW * rectCrop.left);
        int y = (int) (srcH * rectCrop.top);
        int w = (int) (srcW * rectCrop.width());
        int h = (int) (srcH * rectCrop.height());
        Bitmap bitmap1 = Bitmap.createBitmap(bitmap, x, y, w, h);
        bitmap.recycle();

        if (appliedCrop != null) {
            // Report the pixel-quantized rectangle that was really taken, not the raw
            // detector output, so the mapping matches the bitmap exactly.
            appliedCrop.set((float) x / srcW, (float) y / srcH, (float) (x + w) / srcW, (float) (y + h) / srcH);
        }

        return bitmap1;
    }

    public static ByteArrayInputStream bitmapToStream(Bitmap bitmap) {
        try {
            ByteArrayOutputStream os = new ByteArrayOutputStream();

            CompressFormat format = CompressFormat.JPEG;
            bitmap.compress(format, 80, os);

            byte[] byteArray = os.toByteArray();
            final ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(byteArray);
            bitmap.recycle();
            bitmap = null;

            os.close();
            os = null;
            byteArray = null;

            return byteArrayInputStream;
        } catch (Exception e) {
            return null;
        }
    }

    private static final ExecutorService closer = Executors.newSingleThreadExecutor(r -> new Thread(r, "@T DocClose"));
    private static final AtomicInteger closing = new AtomicInteger();

    public static void closeInBackground(final Runnable close) {
        closing.incrementAndGet();
        closer.execute(() -> {
            try {
                close.run();
            } catch (Throwable e) {
                LOG.e(e);
            } finally {
                closing.decrementAndGet();
            }
        });
    }

    public static void awaitClosed() {
        if (closing.get() == 0 || Looper.myLooper() == Looper.getMainLooper()) {
            return;
        }
        try {
            closer.submit(() -> {
            }).get();
        } catch (Exception e) {
            LOG.e(e);
        }
    }

    public static synchronized void clearCodeDocument() {
        if (codeCache != null) {
            codeCache.recycle();
            codeCache = null;
            pathCache = null;
            LOG.d("getNewCodecContext codeCache recycle");
        }
        if (codecContex != null) {
            codecContex.recycle();
            codecContex = null;
            LOG.d("getNewCodecContext codecContex recycle");
        }

    }

    public static void init(CodecDocument codec, String path) {
        clearCodeDocument();
        codeCache = codec;
        pathCache = path;
    }

    public static CodecDocument singleCodecContext(final String path, String passw) {
        awaitClosed();
        return singleCodecContextLocked(path, passw);
    }

    private static synchronized CodecDocument singleCodecContextLocked(final String path, String
            passw) {
        try {
            CodecContext codecContex = BookType.getCodecContextByPath(path);


            LOG.d("CodecContext", codecContex);

            if (codecContex == null) {
                return null;
            }
            LOG.d("CodecContext", "path12",path,passw);
            CodecDocument codecDocument = codecContex.openDocument(path, passw);
            LOG.d("CodecContext", "codecDocument",codecDocument);
            return  codecDocument;
        } catch (RuntimeException e) {
            LOG.e(e);
            return null;
        }
    }

    public static CodecDocument getNewCodecContext(final String path, String passw, int w, int h) {
        awaitClosed();
        return getNewCodecContextLocked(path, passw, w, h);
    }

    private static synchronized CodecDocument getNewCodecContextLocked(final String path, String
            passw, int w, int h) {

        if (path.equals(pathCache) /* && whCache == h + w */ && codeCache != null && !codeCache.isRecycled()) {
            LOG.d("getNewCodecContext cache", path, w, h);
            return codeCache;
        }
        LOG.d("getNewCodecContext new", path, w, h);

        clearCodeDocument();

        pageCount = 0;
        pathCache = null;
        codeCache = null;
        whCache = -1;

        if (w <= 0 || h <= 0) {
            w = Dips.screenWidth();
            h = Dips.screenHeight();
        }
        LOG.d("getNewCodecContext after", w, h);

        codecContex = BookType.getCodecContextByPath(path);

        LOG.d(" getNewCodecContext CodecContext", codecContex);

        if (codecContex == null) {
            return null;
        }

        codeCache = codecContex.openDocument(path, passw);
        if (codeCache == null) {
            LOG.d("getNewCodecContext [Open doc is null 1", path);
            return null;
        }

        LOG.d("getNewCodecContext CodecContext-fontSizeSp", w, h, BookCSS.get().fontSizeSp);
        pageCount = codeCache.getPageCount(w, h, BookCSS.get().fontSizeSp);
        pathCache = path;
        whCache = h + w;
        return codeCache;

    }

    public static Bitmap messageFileBitmap(String msg, String name) {
        return BaseExtractor.getBookCoverWithTitle(msg, name, true);
    }

    public Bitmap proccessCoverPage(PageUrl pageUrl) {
        String path = pageUrl.getPath();

        if (pageUrl.getHeight() == 0) {
            pageUrl.setHeight((int) (pageUrl.getWidth() * 1.5));
        }

        if (AppState.get().isFolderPreview) {
            File root = new File(path);
            if (root.isDirectory()) {
                File[] listFiles = root.listFiles(SUPPORTED_EXT_FILES_ONLY);
                if (listFiles == null || listFiles.length == 0) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                        Drawable drawable = LibreraApp.context.getDrawable(R.drawable.glyphicons_145_folder_open);
                        drawable.setTint(TintUtil.getColorInDayNighth());
                        Bitmap bitmap = Bitmap.createBitmap(drawable.getIntrinsicWidth(),
                                drawable.getIntrinsicHeight(), AppsConfig.CURRENT_BITMAP_ARGB);
                        Canvas canvas = new Canvas(bitmap);
                        drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
                        drawable.draw(canvas);
                        return bitmap;
                    } else {
                        return BitmapFactory.decodeResource(LibreraApp.context.getResources(), R.drawable.gitbook);
                    }
                }
                List<FileMeta> list = new ArrayList<>();
                for (File file : listFiles) {
                    list.add(new FileMeta(file.getPath()));
                }
                BrowseFragment2.sortItems(list);
                path = list.get(0).getPath();

                FileMeta fileMeta = AppDB.get().getOrCreate(root.getPath());
                fileMeta.setFilesCount(list.size());

                if(AppState.get().isHideReadBook) {
                    fileMeta.setReadCount(ExtUtils.countReadBooks(listFiles));
                }

                AppDB.get().save(fileMeta);


                //LOG.d("Directory-Count", root.getName(), list.size(), ExtUtils.countReadBooks(listFiles));

            }
        }

        // The copy of a storage book lives in the cache and is not a book of the user's, so it
        // gets a record of its own instead of one in the library.
        final boolean isSafCopy = CacheZipUtils.isSafBookCopy(path);
        FileMeta fileMeta;
        if (isSafCopy) {
            fileMeta = new FileMeta(path);
            fileMeta.setState(FileMetaCore.STATE_NONE);
        } else {
            fileMeta = AppDB.get().getOrCreate(path);
        }

        LOG.d("FileMeta-State", fileMeta.getState());

        LOG.d("proccessCoverPage fileMeta", fileMeta, pageUrl);

        EbookMeta ebookMeta = FileMetaCore.get().getEbookMeta(path, CacheDir.ZipApp, CalirbeExtractor.isCalibre(fileMeta.getPath()) || fileMeta.getState() != FileMetaCore.STATE_FULL);
        String unZipPath = ebookMeta.getUnzipPath();

        if (fileMeta.getState() != FileMetaCore.STATE_FULL) {

            FileMetaCore.get().upadteBasicMeta(fileMeta, new File(unZipPath));
            FileMetaCore.get().udpateFullMeta(fileMeta, ebookMeta);

            if (!isSafCopy) {
                AppDB.get().save(fileMeta);
            }
        }

        pageUrl.setPath(unZipPath);
        LOG.d("proccessCoverPage unZipPath", unZipPath);

        Bitmap cover = null;

        if (ebookMeta.coverImage != null) {
            cover = BaseExtractor.arrayToBitmap(ebookMeta.coverImage, pageUrl.getWidth());
            LOG.d("Calibre-image", pageUrl);
        } else if (BookType.EPUB.is(unZipPath)) {
            LOG.d("getBookCover-Epub","EPUB",pageUrl.getWidth(),pageUrl.getPath());
            cover = BaseExtractor.arrayToBitmap(EpubExtractor.get().getBookCover(unZipPath), pageUrl.getWidth());
        } else if (ExtUtils.isLibreFile(unZipPath) || BookType.ODT.is(unZipPath) || (unZipPath != null && unZipPath.endsWith(".docx"))) {
            cover = BaseExtractor.arrayToBitmap(OdtExtractor.get().getBookCover(unZipPath), pageUrl.getWidth());
        } else if (BookType.FB2.is(unZipPath)) {
            cover = BaseExtractor.arrayToBitmap(Fb2Extractor.get().getBookCover(unZipPath), pageUrl.getWidth());
        } else if (BookType.MOBI.is(unZipPath)) {
            cover = BaseExtractor.arrayToBitmap(MobiExtract.getBookCover(unZipPath), pageUrl.getWidth());
        } else if (BookType.LIT.is(unZipPath)) {
            cover = BaseExtractor.arrayToBitmap(LitExtract.getBookCover(unZipPath), pageUrl.getWidth());
        } else if (BookType.RTF.is(unZipPath)) {
            cover = BaseExtractor.arrayToBitmap(RtfToEpub.coverImage(unZipPath), pageUrl.getWidth());
        } else if (BookType.PDF.is(unZipPath)) {
            // With the engine the book is read with, which IMG.toUrl keeps in the cover's url
            cover = drawnCover(pageUrl);
        } else if (BookType.DJVU.is(unZipPath)) {
            cover = drawnCover(pageUrl);
            LOG.d("BookType.DJVU", cover, pageUrl);
        } else if (BookType.TIFF.is(unZipPath)) {
            try {
                cover = drawnCover(pageUrl);
            } catch (Exception e) {
                LOG.e(e, unZipPath);
            }
            if (cover == null && !CoverProcess.isFailed(LibreraApp.context, unZipPath)) {
                // A TIFF libtiff does not read - a kind of image it does not draw, or a damaged
                // file - says so, under its name, rather than #error
                cover = BaseExtractor.getBookCoverWithTitle(LibreraApp.context.getString(R.string.not_supported),
                        ExtUtils.getFileName(unZipPath), true);
                pageUrl.tempWithWatermakr = true;
            }
        } else if (BookType.CBZ.is(unZipPath) || BookType.CBR.is(unZipPath)) {
            cover = BaseExtractor.arrayToBitmap(CbzCbrExtractor.getBookCover(unZipPath), pageUrl.getWidth());
        } else if (BookType.FOLDER.is(unZipPath)) {
            cover = BaseExtractor.arrayToBitmap(FolderContext.getBookCover(unZipPath), pageUrl.getWidth());
        } else if (ExtUtils.isFileArchive(unZipPath)) {
            String ext = ExtUtils.getFileExtension(unZipPath);
            cover = BaseExtractor.arrayToBitmap(CbzCbrExtractor.getBookCover(unZipPath), pageUrl.getWidth());
            if (cover == null) {
                cover = BaseExtractor.getBookCoverWithTitle("...", "  [" + ext.toUpperCase(Locale.US) + "]", true);
            }
            pageUrl.tempWithWatermakr = true;
        } else if (ExtUtils.isFontFile(unZipPath)) {
            cover = BaseExtractor.getBookCoverWithTitle("font", "", true);
            pageUrl.tempWithWatermakr = true;
        } else if (unZipPath.endsWith(MdContext.SUMMARY_MD)) {
            cover = BitmapFactory.decodeResource(LibreraApp.context.getResources(), R.drawable.gitbook);
            LOG.d("SUMMARY_MD", unZipPath);
        }

        if (cover == null) {
            cover = BaseExtractor.getBookCoverWithTitle(fileMeta.getAuthor(), fileMeta.getTitle(), true);
            pageUrl.tempWithWatermakr = true;
        }

        LOG.d("udpateFullMeta ImageExtractor", fileMeta.getAuthor());

        return cover;
    }

    /**
     * A cover the engine draws from the book's first page, drawn in the cover process: a page
     * that takes minutes would otherwise hold the engine lock all that time, and no other cover
     * would be drawn and no book opened meanwhile. A page that took too long gets the cover with
     * the book's title, see {@link CoverProcess}.
     */
    private Bitmap drawnCover(PageUrl pageUrl) {
        return CoverProcess.render(LibreraApp.context, pageUrl).bitmap;
    }

    public Bitmap generalCoverWithEffect(PageUrl pageUrl, Bitmap cover) {
        LOG.d("generalCoverWithEffect", pageUrl.getWidth(), cover.getWidth(), " --- ", pageUrl.getHeight(), cover.getHeight());
        Bitmap res;
        if (AppState.get().isBookCoverEffect || pageUrl.getPage() == COVER_PAGE_WITH_EFFECT) {
            res = MagicHelper.scaleCenterCrop(cover, pageUrl.getHeight(), pageUrl.getWidth(), !pageUrl.tempWithWatermakr);
        } else {
            res = cover;
        }
        return res;
    }

    public Bitmap proccessOtherPage(String pageUrl) {
        try {
            return proccessOtherPage(PageUrl.fromString(pageUrl));
        } catch (Exception e) {
            return BaseExtractor.getBookCoverWithTitle("error", "", true);
        }

    }

    public Bitmap proccessOtherPage(PageUrl pageUrl) {
        int page = pageUrl.getPage();
        String path = pageUrl.getPath();

        boolean isNeedDisableMagicInPDFDjvu = false;
        LOG.d("Page-Number OTHER", pageUrl.getPage());
        if (pageUrl.getPage() == COVER_PAGE || pageUrl.getPage() == COVER_PAGE_NO_EFFECT || pageUrl.getPage() == COVER_PAGE_WITH_EFFECT) {
            isNeedDisableMagicInPDFDjvu = true;
        }
        if (page < 0) {
            page = 0;
        }
        if (pageUrl.isCrop()) {
            // isNeedDisableMagicInPDFDjvu = true;
        }

        CodecDocument codeCache = null;
        if (isNeedDisableMagicInPDFDjvu) {
            codeCache = singleCodecContext(path, "");
        } else {
            codeCache = getNewCodecContext(path, "", pageUrl.getWidth(), pageUrl.getHeight());
        }

        if (codeCache == null) {
            LOG.d("TEST", "codecDocument == null" + path);
            return null;
        }

        final CodecPageInfo pageInfo = codeCache.getPageInfo(page);
        if (pageInfo == null || pageInfo.width <= 0 || pageInfo.height <= 0) {
            LOG.d("TEST", "pageInfo is null" + path);
            return null;
        }

        Bitmap bitmap = null;

        RectF rectF = new RectF(0, 0, 1f, 1f);
        final float k = (float) pageInfo.height / pageInfo.width;

        int width = pageUrl.getWidth();
        int height = (int) (width * k);

        LOG.d("Bitmap", width, height);
        LOG.d("Bitmap pageInfo.height", pageInfo.width, pageInfo.height);

        BitmapRef bitmapRef = null;
        CodecPage pageCodec = codeCache.getPage(page);

        rectF = new RectF(0, 0, 1f, 1f);

        if (isNeedDisableMagicInPDFDjvu) {
            //bitmapRef = pageCodec.renderBitmapSimple(width, height, rectF);
            //bitmapRef = pageCodec.renderBitmap(width, height, rectF, false);
        } else {

        }
        bitmapRef = pageCodec.renderBitmap(width, height, rectF, false);
        if (bitmapRef == null || bitmapRef.getBitmap() == null) {
            // the page could not be drawn: the caller falls back to a cover of its own
            LOG.d("TEST", "page is not drawn" + path);
            return null;
        }

        bitmap = bitmapRef.getBitmap();

        if (pageUrl.getRotate() > 0) {
            final Matrix matrix = new Matrix();
            matrix.postRotate(pageUrl.getRotate());
            final Bitmap bitmap1 = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
            bitmap.recycle();
            bitmap = bitmap1;
        }

        if (pageUrl.getNumber() == 0) {
            //do nothing
        } else if (pageUrl.getNumber() == 1) {
            final Bitmap bitmap1 = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth() / 2, bitmap.getHeight());
            bitmap.recycle();
            bitmap = bitmap1;
        } else if (pageUrl.getNumber() == 2) {
            final Bitmap bitmap1 = Bitmap.createBitmap(bitmap, bitmap.getWidth() / 2, 0, bitmap.getWidth() / 2, bitmap.getHeight());
            bitmap.recycle();
            bitmap = bitmap1;
        }

        if (pageUrl.isCrop()) {
            // Only a plain, unrotated single-page render has a crop rectangle that can be
            // mapped back onto full-page text coordinates. For split/double pages the crop is
            // measured against a half bitmap and for rotated pages the axes no longer match,
            // so no rectangle is published and text selection stays disabled for those.
            // Restricted to isDoText() renders (the reading page) so a thumbnail or cover
            // render, which measures the crop on a much smaller bitmap, cannot replace the
            // rectangle belonging to the page text stored below.
            final boolean mappable = pageUrl.isDoText() && pageUrl.getNumber() == 0 && pageUrl.getRotate() == 0;
            final RectF appliedCrop = mappable ? new RectF() : null;

            if (BookType.DJVU.is(pageUrl.getPath())) {
                Bitmap sample = pageCodec.renderBitmapSimple(PageCropper.MAX_WIDTH, PageCropper.MAX_HEIGHT, rectF).getBitmap();
                bitmap = cropBitmap(bitmap, sample, appliedCrop);
                sample.recycle();
                sample = null;
            } else {
                bitmap = cropBitmap(bitmap, bitmap, appliedCrop);
            }

            if (mappable) {
                PageImageState.get().putPageCrop(pageUrl.getPage(), appliedCrop);
            }
        } else if (pageUrl.isDoText()) {
            // Drop any rectangle left over from an earlier cropped render of this page.
            PageImageState.get().putPageCrop(pageUrl.getPage(), null);
        }

        if (pageUrl.isInvert()) {
            final RawBitmap bmp = new RawBitmap(bitmap, new Rect(0, 0, bitmap.getWidth(), bitmap.getHeight()));
            bmp.invert();
            bitmap.recycle();
            bitmap = bmp.toBitmap().getBitmap();

        }

        LOG.d("pageUrl", pageUrl.isDoText(), pageCodec.isRecycled(), codeCache.isRecycled());
        final List<Runnable> later = TEXT_LATER.get();
        if (pageUrl.isDoText() && later != null && !isNeedDisableMagicInPDFDjvu) {
            // The page is shown first and its text read after: 7 to 44 ms of each page turned,
            // 100 on the first page of a book. The page is let go of once it is read.
            final CodecPage textPage = pageCodec;
            final CodecDocument textDoc = codeCache;
            final int textPageNumber = pageUrl.getPage();
            final String textPath = path;
            final int codecPageNumber = page;
            later.add(() -> readPageText(textDoc, textPage, codecPageNumber, textPageNumber, textPath, true));
        } else if (pageUrl.isDoText()) {
            readPageText(codeCache, pageCodec, page, pageUrl.getPage(), path, false);
        } else if (!pageCodec.isRecycled()) {
            pageCodec.recycle();
        }
        if (isNeedDisableMagicInPDFDjvu) {
            codeCache.recycle();
        }

        if (!isNeedDisableMagicInPDFDjvu && MagicHelper.isNeedBookBackgroundImage()) {
            bitmap = MagicHelper.updateWithBackground(bitmap);
        }

        return bitmap;
    }

    /**
     * Set by the reader's page loader: the text of each page it draws is read only after the page
     * is handed over to be shown - the tasks are left here, and the loader runs them then.
     */
    public static final ThreadLocal<List<Runnable>> TEXT_LATER = new ThreadLocal<>();

    /**
     * Reads the text, the links, the reader's marks and the PDF's annotations of a page drawn
     * for reading, then lets go of the page.
     *
     * @param onMain whether the page is shown already: what was read is then put in place on the
     *               main thread, which draws from it, and the pages are drawn again with it
     */
    private static void readPageText(final CodecDocument doc, final CodecPage drawn, final int page,
                                     final int key, final String path, final boolean onMain) {
        CodecPage pageCodec = drawn;
        try {
            if (pageCodec.isRecycled() && !doc.isRecycled()) {
                // The document hands the same page object to every thread that asks for this
                // page, and the sharp part of a zoomed page (PageImaveView.renderDetail) closes
                // it when it is drawn. Opened zoomed, the page lost that race, kept no text and
                // could not be selected or searched on screen: the page is opened again for it.
                pageCodec = doc.getPage(page);
            }
            if (pageCodec.isRecycled() || doc.isRecycled()) {
                return;
            }
            final TextWord[][] text = pageCodec.getText();
            final List<PageLink> links = pageCodec.getPageLinks();
            // The reader's own marks, resolved here for the same reason the text is: finding one
            // in a reflowable book means searching
            final List<AnnotationsFinder.Drawn> marks =
                    AnnotationsFinder.forCurrentBook().forPage(doc, pageCodec, key);
            final List<Annotation> annotations = BookType.PDF.is(path) ? pageCodec.getAnnotations() : null;
            LOG.d("pageUrl Load-page-text", key, text != null ? text.length : 0);
            final Runnable put = () -> {
                final PageImageState state = PageImageState.get();
                state.pagesText.put(key, text);
                state.pagesLinks.put(key, links);
                state.pagesMarks.put(key, marks);
                if (annotations != null) {
                    state.pagesAnnotation.put(key, annotations);
                }
            };
            if (!onMain) {
                put.run();
                return;
            }
            new Handler(Looper.getMainLooper()).post(() -> {
                // Another book opened meanwhile: its pages are not this one's
                if (doc != codeCache || doc.isRecycled()) {
                    return;
                }
                put.run();
                EventBus.getDefault().post(new InvalidateMessage());
            });
        } catch (Exception e) {
            LOG.e(e);
        } finally {
            if (!pageCodec.isRecycled()) {
                pageCodec.recycle();
            }
            if (pageCodec != drawn && !drawn.isRecycled()) {
                drawn.recycle();
            }
        }
    }

    @Deprecated
    public Pair<Bitmap, RectF> getCroppedPage(CodecDocument codecDocumentLocal, int page, Bitmap
            bitmap) {
        RectF rectF = new RectF(0, 0, 1f, 1f);
        final Rect rootRect = new Rect(0, 0, bitmap.getWidth(), bitmap.getHeight());

        rectF = PageCropper.getCropBounds(bitmap, rootRect, rectF);

        float nWidth = bitmap.getWidth() - bitmap.getWidth() * (rectF.left + (1 - rectF.right));
        float nHeiht = bitmap.getHeight() - bitmap.getHeight() * (rectF.top + (1 - rectF.bottom));

        bitmap.recycle();
        codecDocumentLocal.getPage(page).recycle();
        Bitmap result = codecDocumentLocal.getPage(page).renderBitmap((int) nWidth, (int) nHeiht, rectF, false).getBitmap();
        return new Pair<Bitmap, RectF>(result, rectF);

    }

    public InputStream getStream(String imageUri, final Object extra) throws
            IOException {
        if (imageUri == null) {
            imageUri = "";
        }
        // The crash record is two synchronous preference writes, and it is only worth them where
        // a native reader runs: a cover that is merely drawn or decoded by the platform cannot
        // take the process down with it, and paying for it there doubles the cost of the cover.
        if (!isCrashGuarded(imageUri)) {
            return getStreamInner(imageUri, "");
        }
        final String hash = "" + imageUri.hashCode();
        // The mark names the run of the app that made it. One of this run is a drawing of the
        // same page still under way, not a crash; one of an earlier run is a crash only if that
        // run ended in one - an update, or the app stopped by the user or the system while the
        // page was being drawn, left marks that kept good pages out for good.
        final Object mark = readMark(hash);
        if (mark != null) {
            final boolean thisRun = mark instanceof String && mark.equals(BookOpenGuard.RUN);
            if (!thisRun && !BookOpenGuard.lastRunEndedNormally(c)) {
                LOG.d("Error-crash", imageUri, hash);
                return messageFile("#crash", "");
            }
            if (thisRun) {
                return getStreamInner(imageUri, hash);
            }
        }
        try {
            writeMark(hash);
            return getStreamInner(imageUri, hash);
        } finally {
            clearMark(hash);
        }
    }

    /** Whether drawing the page the url names took the app down in an earlier run. */
    public boolean crashedBefore(final String imageUri) {
        final Object mark = readMark("" + imageUri.hashCode());
        return mark != null && !BookOpenGuard.RUN.equals(mark) && !BookOpenGuard.lastRunEndedNormally(c);
    }

    /**
     * Draws a part of a page under a mark of its own, as a page is drawn (see getStream): a
     * part that took the app down is not drawn again, and its page is still shown.
     *
     * @return null, not drawn, if drawing it took the app down before
     */
    public <T> T drawGuarded(final String key, final java.util.concurrent.Callable<T> draw) throws Exception {
        final String hash = "" + key.hashCode();
        final Object mark = readMark(hash);
        if (mark != null && !BookOpenGuard.RUN.equals(mark)) {
            if (!BookOpenGuard.lastRunEndedNormally(c)) {
                LOG.d("Error-crash", key, hash);
                return null;
            }
        } else if (mark != null) {
            return draw.call();
        }
        try {
            writeMark(hash);
            return draw.call();
        } finally {
            clearMark(hash);
        }
    }

    private Object readMark(final String hash) {
        // Marks were kept in preferences before: one left by a crash then still counts
        if (sp != null && sp.contains(hash)) {
            return sp.getAll().get(hash);
        }
        return readMark(new File(markDir(), hash));
    }

    private void writeMark(final String hash) {
        writeMark(new File(markDir(), hash));
    }

    private void clearMark(final String hash) {
        new File(markDir(), hash).delete();
        if (sp != null && sp.contains(hash)) {
            sp.edit().remove(hash).commit();
        }
    }

    /**
     * Each page being drawn is marked with a small file of its own. A crash takes only the
     * process down, and a file written survives that; the marks were two synchronous preference
     * commits, each writing the whole file out and waiting for the disk - 3 to 30 ms of each
     * page turned, before it was shown.
     */
    private File markDir() {
        if (markDir == null) {
            final File dir = new File(c.getFilesDir(), "page-marks");
            dir.mkdirs();
            markDir = dir;
        }
        return markDir;
    }

    private volatile File markDir;

    private static String readMark(final File file) {
        if (!file.isFile()) {
            return null;
        }
        try (java.io.FileInputStream in = new java.io.FileInputStream(file)) {
            final byte[] bytes = new byte[(int) Math.min(file.length(), 256)];
            int read = 0;
            while (read < bytes.length) {
                final int n = in.read(bytes, read, bytes.length - read);
                if (n < 0) {
                    break;
                }
                read += n;
            }
            return new String(bytes, 0, read, "UTF-8");
        } catch (Exception e) {
            LOG.e(e);
            return "";
        }
    }

    private static void writeMark(final File file) {
        try (java.io.FileOutputStream out = new java.io.FileOutputStream(file)) {
            out.write(BookOpenGuard.RUN.getBytes("UTF-8"));
        } catch (Exception e) {
            LOG.e(e);
        }
    }

    private boolean isCrashGuarded(String imageUri) {
        if (imageUri.startsWith(Safe.TXT_SAFE_RUN) || imageUri.startsWith("data:") || imageUri.startsWith("assets:")) {
            return false;
        }
        try {
            String path = PageUrl.fromString(imageUri).getPath();
            if (ExtUtils.isExteralSD(path)) {
                // Nothing but the name is drawn, unless the book is copied out and read.
                return AppState.get().isSafBookCovers && !ExtUtils.isImagePath(path);
            }
        } catch (Exception e) {
            LOG.e(e);
        }
        return true;
    }

    public InputStream getStreamInner(final String imageUri, String hash) throws IOException {
        LOG.d("TEST", "url: " + imageUri);

        if (imageUri.startsWith(Safe.TXT_SAFE_RUN)) {
            LOG.d("MUPDF!", Safe.TXT_SAFE_RUN, "begin", imageUri);
            //return LibreraApp.context.getResources().getAssets().open("opds/web.png");
            return messageFile("safe run", "stub");

        }
        // Pictures off the network never come here: Glide fetches and decodes those itself, at
        // the size they are shown at (see OkHttpUrlLoader).
        if (imageUri.startsWith("data:")) {
            String uri = imageUri;
            // uri = uri.replace("data:image/png;base64,", "");
            // uri = uri.replace("data:image/jpeg;base64,", "");
            // uri = uri.replace("data:image/jpg;base64,", "");
            // uri = uri.replace("data:image/gif;base64,", "");
            uri = uri.substring(uri.indexOf(",") + 1);
            LOG.d("Load image data ", uri);
            return new ByteArrayInputStream(Base64.decode(uri, Base64.DEFAULT));
        }
        if (imageUri.startsWith("assets:")) {
            return LibreraApp.context.getResources().getAssets().open(imageUri.replace("assets://", ""));
        }

        final PageUrl pageUrl = PageUrl.fromString(imageUri);
        String path = pageUrl.getPath();
        File coverCopy = null;

        try {

            // A book from the history, known by a path the app may no longer read, is read
            // through a folder the user handed over instead, as ExtUtils.openFile opens it
            if (!ExtUtils.isExteralSD(path) && path.startsWith("/") && !Android6.canWrite(c)
                    && !new File(path).canRead()) {
                String saf = ExtUtils.getSafUriForPath(c, path);
                if (saf != null) {
                    path = saf;
                }
            }

            if (ExtUtils.isExteralSD(path)) {
                if (ExtUtils.isImagePath(path)) {
                    return c.getContentResolver().openInputStream(Uri.parse(path));
                }
                String display = ExtUtils.getFileName(Uri.decode(path));

                // A comic's cover is read out of the archive in place, through the provider's
                // descriptor: the book is not copied for it
                if (AppState.get().isSafBookCovers && (BookType.CBZ.is(display) || BookType.CBR.is(display)) && Arrays.asList(COVER_PAGE, COVER_PAGE_NO_EFFECT, COVER_PAGE_WITH_EFFECT)
                                                                                    .contains(pageUrl.getPage())) {
                    if (pageUrl.getHeight() == 0) {
                        pageUrl.setHeight((int) (pageUrl.getWidth() * 1.5));
                    }
                    Bitmap cover = BaseExtractor.arrayToBitmap(CbzCbrExtractor.getBookCover(path), pageUrl.getWidth());
                    if (cover != null) {
                        if (pageUrl.getPage() == COVER_PAGE_NO_EFFECT) {
                            return bitmapToStreamRAW(cover);
                        }
                        try {
                            MagicHelper.isNeedBC = false;
                            return bitmapToStreamRAW(generalCoverWithEffect(pageUrl, cover));
                        } finally {
                            MagicHelper.isNeedBC = true;
                        }
                    }
                }

                // A book the readers can open only as a file: with covers for added storage
                // asked for, the cover is read from a copy, thrown away again below unless it is
                // the book being read.
                File copy = AppState.get().isSafBookCovers ?
                        CacheZipUtils.getSafCoverCopy(c, path, display) : null;
                if (copy == null) {
                    return messageFile("", display);
                }
                coverCopy = copy;
                path = copy.getPath();
                pageUrl.setPath(path);
            }

            if (path.startsWith(Clouds.PREFIX_CLOUD)) {
                if (!Clouds.isCacheFileExist(path)) {
                    String display = ExtUtils.getFileName(path);
                    return messageFile("", display);
                }
            }

            // File file = new File(path);

            if (ExtUtils.isImagePath(path)) {
                FileMeta fileMeta = AppDB.get().getOrCreate(path);

                File f = Clouds.getCacheFile(path);
                if (f != null) {
                    path = f.getPath();
                }

                FileMetaCore.get().upadteBasicMeta(fileMeta, new File(path));
                AppDB.get().update(fileMeta);

                return BaseExtractor.decodeImage(path, IMG.getImageSize());
            }

//            if (path.endsWith("json")) {
//                FileMeta fileMeta = AppDB.get().getOrCreate(path);
//                FileMetaCore.get().upadteBasicMeta(fileMeta, new File(path));
//                AppDB.get().update(fileMeta);
//                return messageFile("#json", "");
//            }

            // if (!file.isFile()) {
            // return messageFile("#no file", "");
            // }

            int page = pageUrl.getPage();

            if (pageUrl.getHeight() == 0) {
                pageUrl.setHeight((int) (pageUrl.getWidth() * 1.5));
            }

            if (page == COVER_PAGE || page == COVER_PAGE_WITH_EFFECT) {
                try {
                    MagicHelper.isNeedBC = false;
                    Bitmap proccessCoverPage = proccessCoverPage(pageUrl);
                    return bitmapToStreamRAW(generalCoverWithEffect(pageUrl, proccessCoverPage));
                } finally {
                    MagicHelper.isNeedBC = true;
                }
            } else if (page == COVER_PAGE_NO_EFFECT) {
                //ByteArrayInputStream bitmapToStream = bitmapToStream(proccessCoverPage(pageUrl));
                return bitmapToStreamRAW(proccessCoverPage(pageUrl));
            } else {
                if (pageUrl.isDouble()) {
                    LOG.d("isDouble", pageUrl.getHeight(), pageUrl.getWidth());
                    if (AppSP.get().isDoubleCoverAlone) {
                        pageUrl.setPage(pageUrl.getPage() - 1);
                    }

                    Bitmap bitmap1 = proccessOtherPage(pageUrl);
                    pageUrl.setPage(pageUrl.getPage() + 1);

                    Bitmap bitmap2 = null;
                    if (pageUrl.getPage() < pageCount) {
                        bitmap2 = proccessOtherPage(pageUrl);
                    } else {
                        bitmap2 = Bitmap.createBitmap(bitmap1);
                        Canvas canvas = new Canvas(bitmap2);
                        canvas.drawColor(Color.WHITE);
                    }

                    int maxH = Math.max(bitmap1.getHeight(), bitmap2.getHeight());
                    Bitmap bitmap = Bitmap.createBitmap(bitmap1.getWidth() + bitmap2.getWidth(), maxH, AppsConfig.CURRENT_BITMAP_ARGB);
                    Canvas canvas = new Canvas(bitmap);
                    canvas.drawColor(MagicHelper.getBgColor());

                    if (AppState.get().isRTLByDefault) {
                        canvas.drawBitmap(bitmap2, 0, (maxH - bitmap2.getHeight()) / 2, null);
                        canvas.drawBitmap(bitmap1, bitmap2.getWidth(), (maxH - bitmap1.getHeight()) / 2, null);
                    } else {
                        canvas.drawBitmap(bitmap1, 0, (maxH - bitmap1.getHeight()) / 2, null);
                        canvas.drawBitmap(bitmap2, bitmap1.getWidth(), (maxH - bitmap2.getHeight()) / 2, null);
                    }

                    bitmap1.recycle();
                    bitmap2.recycle();
                    return bitmapToStreamRAW(bitmap);

                }

                return bitmapToStreamRAW(proccessOtherPage(pageUrl));
            }

        } catch (MuPdfPasswordException e) {
            return messageFile("#password", ExtUtils.getFileName(path));
        } catch (final Exception e) {
            LOG.w(e);
            return messageFile("#error", "");
        } catch (OutOfMemoryError e2) {
            IMG.clearMemoryCache();
            return messageFile("#error", "");
        } finally {
            CacheZipUtils.removeSafCoverCopy(coverCopy);
        }
    }

    private InputStream bitmapToStreamRAW(Bitmap bitmap) {
        try {
            LOG.d("Rerurn bitmapToStreamRAW");
            return new InputStreamBitmap(bitmap);
            // return bitmapToStream(bitmap);
        } catch (Exception e) {
            return null;
        }
    }

    private InputStream messageFile(String msg, String name) {
        return bitmapToStream(BaseExtractor.getBookCoverWithTitle(msg, name, true));
    }

}
