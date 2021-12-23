package com.foobnix.sys;

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
import android.graphics.pdf.PdfRenderer;
import android.net.Uri;
import android.os.Build;
import android.os.ParcelFileDescriptor;
import android.util.Base64;
import android.util.Pair;

import androidx.annotation.RequiresApi;

import com.BaseExtractor;
import com.foobnix.android.utils.Dips;
import com.foobnix.android.utils.LOG;
import com.foobnix.android.utils.Safe;
import com.foobnix.dao2.FileMeta;
import com.foobnix.ext.CacheZipUtils.CacheDir;
import com.foobnix.ext.CalirbeExtractor;
import com.foobnix.ext.CbzCbrExtractor;
import com.foobnix.ext.EbookMeta;
import com.foobnix.ext.EpubExtractor;
import com.foobnix.ext.Fb2Extractor;
import com.foobnix.ext.MobiExtract;
import com.foobnix.ext.OdtExtractor;
import com.foobnix.ext.RtfExtract;
import com.foobnix.model.AppSP;
import com.foobnix.model.AppState;
import com.foobnix.opds.OPDS;
import com.foobnix.pdf.info.Clouds;
import com.foobnix.pdf.info.ExtUtils;
import com.foobnix.pdf.info.IMG;
import com.foobnix.pdf.info.PageUrl;
import com.foobnix.pdf.info.R;
import com.foobnix.pdf.info.model.BookCSS;
import com.foobnix.pdf.info.wrapper.MagicHelper;
import com.foobnix.pdf.search.activity.HorizontalViewActivity;
import com.foobnix.pdf.search.activity.PageImageState;
import com.foobnix.ui2.AppDB;
import com.foobnix.ui2.FileMetaCore;

import org.ebookdroid.BookType;
import org.ebookdroid.LibreraApp;
import org.ebookdroid.common.bitmaps.BitmapRef;
import org.ebookdroid.common.bitmaps.RawBitmap;
import org.ebookdroid.core.codec.CodecContext;
import org.ebookdroid.core.codec.CodecDocument;
import org.ebookdroid.core.codec.CodecPage;
import org.ebookdroid.core.codec.CodecPageInfo;
import org.ebookdroid.core.crop.PageCropper;
import org.ebookdroid.droids.FolderContext;
import org.ebookdroid.droids.MdContext;
import org.ebookdroid.droids.mupdf.codec.TextWord;
import org.ebookdroid.droids.mupdf.codec.exceptions.MuPdfPasswordException;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Locale;

import mobi.librera.smartreflow.AndroidPlatformImage;
import mobi.librera.smartreflow.SmartReflow1;
import okhttp3.Request;

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
    }

    public static Bitmap cropBitmap(Bitmap bitmap, Bitmap sample) {
        final Rect rootRect = new Rect(0, 0, sample.getWidth(), sample.getHeight());
        RectF rectCrop = PageCropper.getCropBounds(sample, rootRect, new RectF(0, 0, 1f, 1f));
        int x = (int) (bitmap.getWidth() * rectCrop.left);
        int y = (int) (bitmap.getHeight() * rectCrop.top);
        int w = (int) (bitmap.getWidth() * rectCrop.width());
        int h = (int) (bitmap.getHeight() * rectCrop.height());
        Bitmap bitmap1 = Bitmap.createBitmap(bitmap, x, y, w, h);
        bitmap.recycle();
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

    public static synchronized CodecDocument singleCodecContext(final String path, String
            passw, int w, int h) {
        try {
            CodecContext codecContex = BookType.getCodecContextByPath(path);

            LOG.d("CodecContext", codecContex);

            if (codecContex == null) {
                return null;
            }

            TempHolder.get().loadingCancelled = false;
            return codecContex.openDocument(path, passw);
        } catch (RuntimeException e) {
            LOG.e(e);
            return null;
        }
    }

    public static synchronized CodecDocument getNewCodecContext(final String path, String
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

        LOG.d("CodecContext", codecContex);

        if (codecContex == null) {
            return null;
        }


        TempHolder.get().loadingCancelled = false;
        codeCache = codecContex.openDocument(path, passw);
        if (codeCache == null) {
            LOG.d("[Open doc is null 1", path);
            return null;
        }

        LOG.d("CodecContext-fontSizeSp", w, h, BookCSS.get().fontSizeSp);
        pageCount = codeCache.getPageCount(w, h, BookCSS.get().fontSizeSp);
        pathCache = path;
        whCache = h + w;
        return codeCache;

    }

    public static Bitmap messageFileBitmap(String msg, String name) {
        return BaseExtractor.getBookCoverWithTitle(msg, name, true);
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public Bitmap coverPDFNative(PageUrl pageUrl) {
        try {
            LOG.d("Cover-PDF-navite");
            PdfRenderer renderer = new PdfRenderer(ParcelFileDescriptor.open(new File(pageUrl.getPath()), ParcelFileDescriptor.MODE_READ_ONLY));
            PdfRenderer.Page page = renderer.openPage(0);

            final float k = (float) page.getHeight() / page.getWidth();
            int width = pageUrl.getWidth();
            int height = (int) (width * k);


            Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
            page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY);

            page.close();
            renderer.close();
            return bitmap;
        } catch (Exception e) {
            LOG.e(e);
            return null;
        }

    }

    public Bitmap proccessCoverPage(PageUrl pageUrl) {
        String path = pageUrl.getPath();

        if (pageUrl.getHeight() == 0) {
            pageUrl.setHeight((int) (pageUrl.getWidth() * 1.5));
        }

        FileMeta fileMeta = AppDB.get().getOrCreate(path);

        LOG.d("FileMeta-State", fileMeta.getState());

        LOG.d("proccessCoverPage fileMeta", fileMeta, pageUrl);

        EbookMeta ebookMeta = FileMetaCore.get().getEbookMeta(path, CacheDir.ZipApp, CalirbeExtractor.isCalibre(fileMeta.getPath()) || fileMeta.getState() != FileMetaCore.STATE_FULL);
        String unZipPath = ebookMeta.getUnzipPath();

        if (fileMeta.getState() != FileMetaCore.STATE_FULL) {

            FileMetaCore.get().upadteBasicMeta(fileMeta, new File(unZipPath));
            FileMetaCore.get().udpateFullMeta(fileMeta, ebookMeta);

            AppDB.get().updateOrSave(fileMeta);
        }

        pageUrl.setPath(unZipPath);
        LOG.d("proccessCoverPage unZipPath", unZipPath);


        Bitmap cover = null;

        if (ebookMeta.coverImage != null) {
            cover = BaseExtractor.arrayToBitmap(ebookMeta.coverImage, pageUrl.getWidth());
            LOG.d("Calibre-image", pageUrl);
        } else if (BookType.EPUB.is(unZipPath)) {
            cover = BaseExtractor.arrayToBitmap(EpubExtractor.get().getBookCover(unZipPath), pageUrl.getWidth());
        } else if (ExtUtils.isLibreFile(unZipPath) || BookType.ODT.is(unZipPath) || (unZipPath != null && unZipPath.endsWith(".docx"))) {
            cover = BaseExtractor.arrayToBitmap(OdtExtractor.get().getBookCover(unZipPath), pageUrl.getWidth());
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
            LOG.d("SUMMARY_MD",unZipPath);
        }

        if (cover == null) {
            cover = BaseExtractor.getBookCoverWithTitle(fileMeta.getAuthor(), fileMeta.getTitle(), true);
            pageUrl.tempWithWatermakr = true;
        }

        LOG.d("udpateFullMeta ImageExtractor", fileMeta.getAuthor());

        return cover;
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

        CodecDocument codeCache = null;
        if (isNeedDisableMagicInPDFDjvu) {
            codeCache = singleCodecContext(path, "", pageUrl.getWidth(), pageUrl.getHeight());
        } else {
            codeCache = getNewCodecContext(path, "", pageUrl.getWidth(), pageUrl.getHeight());
        }

        if (codeCache == null) {
            LOG.d("TEST", "codecDocument == null" + path);
            return null;
        }

        final CodecPageInfo pageInfo = codeCache.getPageInfo(page);

        Bitmap bitmap = null;

        RectF rectF = new RectF(0, 0, 1f, 1f);
        final float k = (float) pageInfo.height / pageInfo.width;
        final float kScreen = (float) pageUrl.getHeight() / pageUrl.getWidth();
        //final float kScreen = Dips.screenHeight()/Dips.screenWidth();

        int width = pageUrl.getWidth();
        int height = (int) (width * k);

        LOG.d("Bitmap", width, height);
        LOG.d("Bitmap pageInfo.height", pageInfo.width, pageInfo.height);

        BitmapRef bitmapRef = null;
        CodecPage pageCodec = codeCache.getPage(page);

        if (pageUrl.getNumber() == 0) {
            rectF = new RectF(0, 0, 1f, 1f);

            if (isNeedDisableMagicInPDFDjvu) {
                bitmapRef = pageCodec.renderBitmapSimple(width, height, rectF);
            } else {
                bitmapRef = pageCodec.renderBitmap(width, height, rectF, false);
            }

            bitmap = bitmapRef.getBitmap();

            if (pageUrl.isCrop()) {
                if (BookType.DJVU.is(pageUrl.getPath())) {
                    Bitmap sample = pageCodec.renderBitmapSimple(PageCropper.MAX_WIDTH, PageCropper.MAX_HEIGHT, rectF).getBitmap();
                    bitmap = cropBitmap(bitmap, sample);
                    sample.recycle();
                    sample = null;
                } else {
                    bitmap = cropBitmap(bitmap, bitmap);
                }
            }

        } else if (pageUrl.getNumber() == 1) {
            float right = (float) pageUrl.getCutp() / 100;
            rectF = new RectF(0, 0, right, 1f);
            bitmapRef = pageCodec.renderBitmap((int) (width * right), height, rectF, false);
            bitmap = bitmapRef.getBitmap();

            if (pageUrl.isCrop()) {
                bitmap = cropBitmap(bitmap, bitmap);
            }

        } else if (pageUrl.getNumber() == 2) {
            float right = (float) pageUrl.getCutp() / 100;
            rectF = new RectF(right, 0, 1f, 1f);
            bitmapRef = pageCodec.renderBitmap((int) (width * (1 - right)), height, rectF, false);
            bitmap = bitmapRef.getBitmap();

            if (pageUrl.isCrop()) {
                bitmap = cropBitmap(bitmap, bitmap);
            }
        }

        if (AppSP.get().isSmartReflow) {
            try {
                final AndroidPlatformImage input = new AndroidPlatformImage(bitmap);

                SmartReflow1 sm = new SmartReflow1();
                sm.process(input);

                final int rWidth = (int) (bitmap.getWidth() * 0.6);
                final int rHeight = (int) (rWidth * kScreen);
                LOG.d("SmartReflow", rWidth, rHeight, k, kScreen);
                final AndroidPlatformImage output = new AndroidPlatformImage(rWidth, rHeight);
                sm.reflow(output);
                bitmap.recycle();
                bitmap = output.getImage();
            } catch (Exception e) {
                LOG.e(e);
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

        LOG.d("pageUrl", pageUrl.isDoText(), pageCodec.isRecycled(), codeCache.isRecycled(), AppSP.get().lastClosedActivity);
        if (pageUrl.isDoText() && !pageCodec.isRecycled() && !codeCache.isRecycled()) {
            LOG.d("pageUrl run", AppSP.get().lastClosedActivity);
            if (HorizontalViewActivity.class.getSimpleName().equals(AppSP.get().lastClosedActivity)) {
                TextWord[][] text = pageCodec.getText();
                PageImageState.get().pagesText.put(pageUrl.getPage(), text);
                PageImageState.get().pagesLinks.put(pageUrl.getPage(), pageCodec.getPageLinks());
                LOG.d("pageUrl Load-page-text", text != null ? text.length : 0);
            }
        }


        if (!pageCodec.isRecycled()) {
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
        final String hash = "" + imageUri.hashCode();
        try {
            final InputStream streamInner;
            try {
                if (sp.contains(hash)) {
                    LOG.d("Error-crash", imageUri, hash);
                    return messageFile("#crash", "");
                }

                sp.edit().putBoolean(hash, true).commit();
                streamInner = getStreamInner(imageUri, hash);
            } finally {
                sp.edit().remove(hash).commit();
            }
            return streamInner;
        } finally {

        }
    }

    public InputStream getStreamInner(final String imageUri, String hash) throws IOException {
        LOG.d("TEST", "url: " + imageUri);

        if (imageUri.startsWith(Safe.TXT_SAFE_RUN)) {
            LOG.d("MUPDF!", Safe.TXT_SAFE_RUN, "begin", imageUri);
            return LibreraApp.context.getResources().getAssets().open("opds/web.png");

        }
        if (imageUri.startsWith("http")) {

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
        if (imageUri.startsWith("assets:")) {
            return LibreraApp.context.getResources().getAssets().open(imageUri.replace("assets://", ""));
        }

        final PageUrl pageUrl = PageUrl.fromString(imageUri);
        String path = pageUrl.getPath();

        try {


            if (ExtUtils.isExteralSD(path)) {
                if (ExtUtils.isImagePath(path)) {
                    return c.getContentResolver().openInputStream(Uri.parse(path));
                }
                String display = ExtUtils.getFileName(Uri.decode(path));
                return messageFile("", display);
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
            return messageFile("#password", ExtUtils.getFileName(path));
        } catch (final Exception e) {
            LOG.e(e);
            return messageFile("#error", "");
        } catch (OutOfMemoryError e2) {
            IMG.clearMemoryCache();
            return messageFile("#error", "");
        } finally {

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
