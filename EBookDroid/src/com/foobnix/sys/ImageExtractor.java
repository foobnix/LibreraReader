package com.foobnix.sys;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Locale;

import org.ebookdroid.BookType;
import org.ebookdroid.common.bitmaps.BitmapRef;
import org.ebookdroid.common.bitmaps.RawBitmap;
import org.ebookdroid.core.codec.CodecContext;
import org.ebookdroid.core.codec.CodecDocument;
import org.ebookdroid.core.codec.CodecPageInfo;
import org.ebookdroid.core.crop.PageCropper;
import org.ebookdroid.droids.mupdf.codec.exceptions.MuPdfPasswordException;

import com.BaseExtractor;
import com.foobnix.android.utils.Dips;
import com.foobnix.android.utils.LOG;
import com.foobnix.dao2.FileMeta;
import com.foobnix.ext.CacheZipUtils;
import com.foobnix.ext.CbzCbrExtractor;
import com.foobnix.ext.EbookMeta;
import com.foobnix.ext.EpubExtractor;
import com.foobnix.ext.Fb2Extractor;
import com.foobnix.ext.MobiExtract;
import com.foobnix.ext.RtfExtract;
import com.foobnix.opds.OPDS;
import com.foobnix.pdf.info.ExtUtils;
import com.foobnix.pdf.info.IMG;
import com.foobnix.pdf.info.PageUrl;
import com.foobnix.pdf.info.wrapper.AppState;
import com.foobnix.pdf.info.wrapper.MagicHelper;
import com.foobnix.ui2.AppDB;
import com.foobnix.ui2.FileMetaCore;
import com.nostra13.universalimageloader.core.download.BaseImageDownloader;
import com.nostra13.universalimageloader.core.download.ImageDownloader;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.RectF;
import android.util.Base64;
import android.util.Pair;
import okhttp3.Request;

public class ImageExtractor implements ImageDownloader {

    public static final int COVER_PAGE_WITH_EFFECT = -3;
    public static final int COVER_PAGE_NO_EFFECT = -2;
    public static final int COVER_PAGE = -1;
    private static final int FIRST_PAGE = 0;
    private static ImageExtractor instance;
    private final BaseImageDownloader baseImage;
    private final Context c;

    CodecDocument codeCache;
    String pathCache;
    public static SharedPreferences sp;

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
    }

    private ImageExtractor(final Context c) {
        this.c = c;
        baseImage = new BaseImageDownloader(c);
    }

    public Bitmap proccessCoverPage(PageUrl pageUrl) {
        String path = pageUrl.getPath();
        if (pageUrl.getHeight() == 0) {
            pageUrl.setHeight((int) (pageUrl.getWidth() * 1.5));
        }

        FileMeta fileMeta = AppDB.get().getOrCreate(path);
        EbookMeta ebookMeta = FileMetaCore.get().getEbookMeta(path);

        FileMetaCore.get().upadteBasicMeta(fileMeta, new File(path));
        FileMetaCore.get().udpateFullMeta(fileMeta, ebookMeta);

        AppDB.get().update(fileMeta);

        String unZipPath = ebookMeta.getUnzipPath();

        Bitmap cover = null;

        if (ebookMeta.coverImage != null) {
            cover = BaseExtractor.arrayToBitmap(ebookMeta.coverImage, pageUrl.getWidth());
        } else if (BookType.EPUB.is(unZipPath)) {
            cover = BaseExtractor.arrayToBitmap(EpubExtractor.get().getBookCover(unZipPath), pageUrl.getWidth());
        } else if (BookType.FB2.is(unZipPath)) {
            cover = BaseExtractor.arrayToBitmap(Fb2Extractor.get().getBookCover(unZipPath), pageUrl.getWidth());
        } else if (BookType.MOBI.is(unZipPath)) {
            cover = BaseExtractor.arrayToBitmap(MobiExtract.getBookCover(unZipPath), pageUrl.getWidth());
        } else if (BookType.RTF.is(unZipPath)) {
            cover = BaseExtractor.arrayToBitmap(RtfExtract.getImageCover(unZipPath), pageUrl.getWidth());
        } else if (BookType.PDF.is(unZipPath) || BookType.DJVU.is(unZipPath) || BookType.TIFF.is(unZipPath)) {
            cover = proccessOtherPage(pageUrl);
        } else if (BookType.CBZ.is(unZipPath) || BookType.CBR.is(unZipPath)) {
            cover = BaseExtractor.arrayToBitmap(CbzCbrExtractor.getBookCover(unZipPath), pageUrl.getWidth());
        } else if (ExtUtils.isFileArchive(unZipPath)) {
            String ext = ExtUtils.getFileExtension(unZipPath);
            cover = BaseExtractor.getBookCoverWithTitle("...", "  [" + ext.toUpperCase(Locale.US) + "]", true);
            pageUrl.tempWithWatermakr = true;
        } else if (ExtUtils.isFontFile(unZipPath)) {
            cover = BaseExtractor.getBookCoverWithTitle("font", "", true);
            pageUrl.tempWithWatermakr = true;
        }

        if (cover == null) {
            cover = BaseExtractor.getBookCoverWithTitle(ebookMeta.getAuthor(), ebookMeta.getTitle(), true);
            pageUrl.tempWithWatermakr = true;
        }

        return cover;
    }

    public InputStream generalCoverWithEffect(PageUrl pageUrl, Bitmap cover) {
        try {
            LOG.d("generalCoverWithEffect", pageUrl.getWidth(), cover.getWidth(), " --- ", pageUrl.getHeight(), cover.getHeight());
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            Bitmap res;
            if (AppState.getInstance().isBookCoverEffect || pageUrl.getPage() == COVER_PAGE_WITH_EFFECT) {
                res = MagicHelper.scaleCenterCrop(cover, pageUrl.getHeight(), pageUrl.getWidth(), !pageUrl.tempWithWatermakr);
                res.compress(CompressFormat.PNG, 90, out);
            } else {
                res = cover;
                res.compress(CompressFormat.JPEG, 90, out);
            }

            byte[] byteArray = out.toByteArray();
            final ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(byteArray);
            res.recycle();
            res = null;

            out.close();
            out = null;
            byteArray = null;
            return byteArrayInputStream;
        } catch (Exception e) {
            LOG.e(e);
            return null;
        }
    }

    public Bitmap proccessOtherPage(PageUrl pageUrl) {
        int page = pageUrl.getPage();
        String path = pageUrl.getPath();

        boolean isNeedDisableMagicInPDFDjvu = false;
        LOG.d("Page Number", pageUrl.getPage());
        if (pageUrl.getPage() == COVER_PAGE || pageUrl.getPage() == COVER_PAGE_NO_EFFECT || pageUrl.getPage() == COVER_PAGE_WITH_EFFECT) {
            isNeedDisableMagicInPDFDjvu = true;
        }
        if (page < 0) {
            page = 0;
        }
        if (pageUrl.isCrop()) {
            // isNeedDisableMagicInPDFDjvu = true;
        }

        CodecDocument codecDocumentLocal = null;

        LOG.d("PdfImage", "TempHolder.path", path);

        if (path.equals(TempHolder.get().path) && TempHolder.get().codecDocument != null) {
            codecDocumentLocal = TempHolder.get().codecDocument;
            LOG.d("PdfImage", "TempHolder.path");
        } else if (path.equals(pathCache) && codeCache != null && !codeCache.isRecycled()) {
            codecDocumentLocal = codeCache;
            LOG.d("PdfImage", "Local cache");
        } else {
            codecDocumentLocal = getCodecContext(path, "", pageUrl.getWidth(), pageUrl.getHeight());

            if (codecDocumentLocal == null) {
                LOG.d("TEST", "codecDocument == null " + path);
                return null;
            }
            codeCache = codecDocumentLocal;
            pathCache = path;
            LOG.d("PdfImage", "new codec");
        }

        final CodecPageInfo pageInfo = codecDocumentLocal.getPageInfo(page);

        Bitmap bitmap = null;

        RectF rectF = new RectF(0, 0, 1f, 1f);
        final float k = (float) pageInfo.height / pageInfo.width;
        int width = pageUrl.getWidth();
        int height = (int) (width * k);

        LOG.d("Bitmap", width, height);
        LOG.d("Bitmap pageInfo.height", pageInfo.width, pageInfo.height);

        BitmapRef bitmapRef = null;
        if (pageUrl.getNumber() == 0) {
            rectF = new RectF(0, 0, 1f, 1f);
            if (isNeedDisableMagicInPDFDjvu) {
                MagicHelper.isNeedMagic = false;
            }

            bitmapRef = codecDocumentLocal.getPage(page).renderBitmap(width, height, rectF);

            if (isNeedDisableMagicInPDFDjvu) {
                MagicHelper.isNeedMagic = true;
            }
            bitmap = bitmapRef.getBitmap();

            if (pageUrl.isCrop()) {
                // bitmap = getCroppedPage(codecDocumentLocal, page,
                // bitmap).first;
                bitmap = cropBitmap(bitmap);
            }

        } else if (pageUrl.getNumber() == 1) {
            float right = (float) pageUrl.getCutp() / 100;
            rectF = new RectF(0, 0, right, 1f);
            bitmapRef = codecDocumentLocal.getPage(page).renderBitmap((int) (width * right), height, rectF);
            bitmap = bitmapRef.getBitmap();

            if (pageUrl.isCrop()) {
                bitmap = cropBitmap(bitmap);
            }

        } else if (pageUrl.getNumber() == 2) {
            float right = (float) pageUrl.getCutp() / 100;
            rectF = new RectF(right, 0, 1f, 1f);
            bitmapRef = codecDocumentLocal.getPage(page).renderBitmap((int) (width * (1 - right)), height, rectF);
            bitmap = bitmapRef.getBitmap();

            if (pageUrl.isCrop()) {
                bitmap = cropBitmap(bitmap);
            }
        }

        if (pageUrl.isInvert()) {
            final RawBitmap bmp = new RawBitmap(bitmap, new Rect(0, 0, bitmap.getWidth(), bitmap.getHeight()));
            bmp.invert();
            bitmap.recycle();
            bitmap = bmp.toBitmap().getBitmap();

        }
        if (pageUrl.getRotate() > 0) {
            final Matrix matrix = new Matrix();
            matrix.postRotate(pageUrl.getRotate());
            final Bitmap bitmap1 = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
            bitmap.recycle();
            bitmap = bitmap1;
        }

        codecDocumentLocal.getPage(page).recycle();

        if (!isNeedDisableMagicInPDFDjvu && MagicHelper.isNeedBookBackgroundImage()) {
            bitmap = MagicHelper.updateWithBackground(bitmap);
        }

        return bitmap;
    }

    public Bitmap cropBitmap(Bitmap bitmap) {
        final Rect rootRect = new Rect(0, 0, bitmap.getWidth(), bitmap.getHeight());
        RectF rectCrop = PageCropper.getCropBounds(bitmap, rootRect, new RectF(0, 0, 1f, 1f));
        int x = (int) (bitmap.getWidth() * rectCrop.left);
        int y = (int) (bitmap.getHeight() * rectCrop.top);
        int w = (int) (bitmap.getWidth() * rectCrop.width());
        int h = (int) (bitmap.getHeight() * rectCrop.height());
        Bitmap bitmap1 = Bitmap.createBitmap(bitmap, x, y, w, h);
        bitmap.recycle();
        return bitmap1;
    }

    public Pair<Bitmap, RectF> getCroppedPage(CodecDocument codecDocumentLocal, int page, Bitmap bitmap) {
        RectF rectF = new RectF(0, 0, 1f, 1f);
        final Rect rootRect = new Rect(0, 0, bitmap.getWidth(), bitmap.getHeight());

        rectF = PageCropper.getCropBounds(bitmap, rootRect, rectF);

        float nWidth = bitmap.getWidth() - bitmap.getWidth() * (rectF.left + (1 - rectF.right));
        float nHeiht = bitmap.getHeight() - bitmap.getHeight() * (rectF.top + (1 - rectF.bottom));

        bitmap.recycle();
        codecDocumentLocal.getPage(page).recycle();
        Bitmap result = codecDocumentLocal.getPage(page).renderBitmap((int) nWidth, (int) nHeiht, rectF).getBitmap();
        return new Pair<Bitmap, RectF>(result, rectF);

    }

    @Override
    public synchronized InputStream getStream(final String imageUri, final Object extra) throws IOException {
        LOG.d("TEST", "url: " + imageUri);

        if (imageUri.startsWith("https")) {

            Request request = new Request.Builder()//
                    .header("User-Agent", OPDS.USER_AGENT).url(imageUri)//
                    .build();//

            LOG.d("https!!!", imageUri);
            return OPDS.client.newCall(request).execute().body().byteStream();
        }

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

        if (!imageUri.startsWith("{")) {
            return baseImage.getStream(imageUri, extra);
        }
        if (sp.contains("" + imageUri.hashCode())) {
            LOG.d("Error FILE", imageUri);
            return messageFile("#crash", "");
        }

        final PageUrl pageUrl = PageUrl.fromString(imageUri);
        String path = pageUrl.getPath();

        File file = new File(path);
        try {

            // if (ExtUtils.isTiffFile(file)) {
            // FileMeta fileMeta = AppDB.get().getOrCreate(path);
            // FileMetaCore.get().upadteBasicMeta(fileMeta, new File(path));
            // AppDB.get().update(fileMeta);
            // return bitmapToStream(proccessOtherPage(pageUrl));
            // }

            if (ExtUtils.isImageFile(file)) {
                FileMeta fileMeta = AppDB.get().getOrCreate(path);
                FileMetaCore.get().upadteBasicMeta(fileMeta, new File(path));
                AppDB.get().update(fileMeta);

                return BaseExtractor.decodeImage(path, IMG.getImageSize());
            }

            if (path.endsWith("json")) {
                FileMeta fileMeta = AppDB.get().getOrCreate(path);
                FileMetaCore.get().upadteBasicMeta(fileMeta, new File(path));
                AppDB.get().update(fileMeta);
                return messageFile("#json", "");
            }

            if (!file.isFile()) {
                return messageFile("#no file", "");
            }
            sp.edit().putBoolean("" + imageUri.hashCode(), true).commit();

            int page = pageUrl.getPage();

            if (pageUrl.getHeight() == 0) {
                pageUrl.setHeight((int) (pageUrl.getWidth() * 1.5));
            }

            if (page == COVER_PAGE || page == COVER_PAGE_WITH_EFFECT) {
                Bitmap proccessCoverPage = proccessCoverPage(pageUrl);
                return generalCoverWithEffect(pageUrl, proccessCoverPage);
            } else if (page == COVER_PAGE_NO_EFFECT) {
                return bitmapToStream(proccessCoverPage(pageUrl));
            } else {
                if (pageUrl.isDouble()) {
                    LOG.d("isDouble", pageUrl.getHeight(), pageUrl.getWidth());
                    if (AppState.get().isDoubleCoverAlone) {
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
                    Bitmap bitmap = Bitmap.createBitmap(bitmap1.getWidth() + bitmap2.getWidth(), maxH, Bitmap.Config.RGB_565);
                    Canvas canvas = new Canvas(bitmap);
                    canvas.drawColor(MagicHelper.getBgColor());

                    if (AppState.get().isCutRTL) {
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
            return messageFile("#password", file.getName());
        } catch (final Exception e) {
            LOG.e(e);
            return messageFile("#error", "");
        } catch (OutOfMemoryError e2) {
            AppState.outOfMemoryHack();
            return messageFile("#error", "");
        } finally {
            sp.edit().remove("" + imageUri.hashCode()).commit();
        }
    }

    private ByteArrayInputStream bitmapToStream(Bitmap bitmap) {
        try {
            ByteArrayOutputStream os = new ByteArrayOutputStream();
            boolean isJPG = AppState.get().imageFormat.equals(AppState.JPG);

            CompressFormat format = isJPG ? CompressFormat.JPEG : CompressFormat.PNG;
            int quality = isJPG ? 80 : 100;
            bitmap.compress(format, quality, os);

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

    private InputStream bitmapToStreamRAW(Bitmap bitmap) {
        try {
            LOG.d("Rerurn bitmapToStreamRAW");
            return new InputStreamBitmap(bitmap);
            // return bitmapToStream(bitmap);
        } catch (Exception e) {
            return null;
        }
    }

    static int pageCount = 0;

    public static CodecDocument getCodecContext(final String path, String passw, int w, int h) {
        if (path.equals(TempHolder.get().path) && TempHolder.get().codecDocument != null) {
            return TempHolder.get().codecDocument;
        }
        LOG.d("getCodecContext before", w, h);
        if (w <= 0 || h <= 0) {
            w = Dips.screenWidth();
            h = Dips.screenHeight();
        }
        LOG.d("getCodecContext after", w, h);

        CodecContext ctx = BookType.getCodecContextByPath(path);

        if (ctx == null) {
            return null;
        }
        CodecDocument openDocument = null;
        CacheZipUtils.cacheLock.lock();
        try {
            String zipPath = CacheZipUtils.extracIfNeed(path).unZipPath;
            LOG.d("getCodecContext", zipPath);
            openDocument = ctx.openDocument(zipPath, passw);
        } finally {
            CacheZipUtils.cacheLock.unlock();
        }

        pageCount = openDocument.getPageCount(w, h, AppState.get().fontSizeSp);

        TempHolder.get().init(openDocument, path);
        return openDocument;
    }

    private InputStream messageFile(String msg, String name) {
        return bitmapToStream(BaseExtractor.getBookCoverWithTitle(msg, name, true));
    }

}
