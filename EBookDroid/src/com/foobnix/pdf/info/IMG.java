package com.foobnix.pdf.info;

import java.io.File;
import java.util.regex.Pattern;

import com.foobnix.android.utils.Dips;
import com.foobnix.android.utils.LOG;
import com.foobnix.pdf.info.model.BookCSS;
import com.foobnix.pdf.info.wrapper.AppState;
import com.foobnix.sys.ImageExtractor;
import com.nostra13.universalimageloader.cache.disc.impl.UnlimitedDiskCache;
import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.DisplayImageOptions.Builder;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.ImageLoaderConfiguration;
import com.nostra13.universalimageloader.core.assist.ImageScaleType;
import com.nostra13.universalimageloader.core.listener.ImageLoadingListener;
import com.nostra13.universalimageloader.utils.MemoryCacheUtils;

import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.BitmapFactory.Options;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.provider.MediaStore;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.ImageView;

public class IMG {

    public static final float WIDTH_DK = 1.4f;
    public static final int DP5 = -Dips.dpToPx(40);
    private static final ColorDrawable COLOR_DRAWABLE = new ColorDrawable(Color.LTGRAY);

    public static final Config BMP_CFG = Config.RGB_565;
    public static final int TWO_LINE_COVER_SIZE = 74;

    public static boolean RESET_VIEW_BEFORE_LOADING = true;
    public static Drawable bookBGWithMark;
    public static Drawable bookBGNoMark;
    public static Context context;

    public static void init(Context context) {
        IMG.context = context;

        bookBGWithMark = context.getResources().getDrawable(R.drawable.bookeffect2);
        bookBGNoMark = context.getResources().getDrawable(R.drawable.bookeffect1);

        final Builder builder = new DisplayImageOptions.Builder();
        builder.cacheInMemory(true);
        builder.cacheOnDisk(true);
        builder.showImageOnLoading(COLOR_DRAWABLE);
        builder.showImageOnFail(COLOR_DRAWABLE);
        builder.showImageForEmptyUri(COLOR_DRAWABLE);
        builder.bitmapConfig(BMP_CFG);
        builder.imageScaleType(ImageScaleType.NONE);
        builder.resetViewBeforeLoading(RESET_VIEW_BEFORE_LOADING);
        builder.considerExifParams(false);
        builder.decodingOptions(new Options());

        final ImageLoaderConfiguration config = new ImageLoaderConfiguration.Builder(context)//
                .threadPoolSize(1)//
                .threadPriority(Thread.NORM_PRIORITY)//
                .defaultDisplayImageOptions(builder.build())//
                .diskCache(new UnlimitedDiskCache(new File(context.getExternalCacheDir(), "Images-1")))//
                .imageDownloader(ImageExtractor.getInstance(context))//
                .build();

        ImageLoader.getInstance().init(config);
    }

    public static int getImageSize() {
        return Dips.dpToPx(Math.max(AppState.get().coverSmallSize, AppState.get().coverBigSize));
    }

    public static void updateLayoutHeightSizeSmall(ViewGroup imageView) {
        if (imageView == null || imageView.getLayoutParams() == null) {
            return;
        }
        int widht = Dips.dpToPx(AppState.get().coverSmallSize);
        LayoutParams lp = imageView.getLayoutParams();
        lp.height = (int) (widht * WIDTH_DK);
    }

    public static void updateLayoutHeightSizeBig(ViewGroup imageView) {
        if (imageView == null || imageView.getLayoutParams() == null) {
            return;
        }
        int widht = Dips.dpToPx(AppState.get().coverBigSize);
        LayoutParams lp = imageView.getLayoutParams();
        lp.height = (int) (widht * WIDTH_DK);
    }

    public static LayoutParams updateImageSizeSmall(View imageView) {
        if (imageView == null || imageView.getLayoutParams() == null) {
            return null;
        }
        LayoutParams lp = imageView.getLayoutParams();
        lp.width = Dips.dpToPx(AppState.get().coverSmallSize);
        lp.height = (int) (lp.width * WIDTH_DK);
        return lp;
    }

    public static void updateImageSizeBig(View imageView) {
        if (imageView == null || imageView.getLayoutParams() == null) {
            return;
        }

        LayoutParams lp = imageView.getLayoutParams();
        lp.width = Dips.dpToPx(AppState.get().coverBigSize);
        lp.height = (int) (lp.width * WIDTH_DK);
    }

    public static void updateImageSizeBig(View imageView, int sizeDP) {
        if (imageView == null || imageView.getLayoutParams() == null) {
            return;
        }

        LayoutParams lp = imageView.getLayoutParams();
        lp.width = Dips.dpToPx(sizeDP);
        lp.height = (int) (lp.width * WIDTH_DK);
    }

    public static int alphaColor(int persent, String color) {
        try {
            int parseColor = Color.parseColor(color);
            int alpha = 255 * persent / 100;
            return Color.argb(alpha, Color.red(parseColor), Color.green(parseColor), Color.blue(parseColor));
        } catch (Exception e) {
            return Color.WHITE;
        }
    }

    public static void initImageProcessing(final Context c, final boolean destroy) {
        if (destroy) {
            ImageLoader.getInstance().destroy();
        }

    }

    public static DisplayImageOptions displayCacheMemoryDisc = new DisplayImageOptions.Builder() //
            .showImageOnFail(COLOR_DRAWABLE)//
            .showImageForEmptyUri(COLOR_DRAWABLE)//
            .showImageOnFail(COLOR_DRAWABLE)//
            .cacheInMemory(true)//
            .cacheOnDisk(true)//
            .considerExifParams(false)//
            .imageScaleType(ImageScaleType.NONE)//
            .resetViewBeforeLoading(RESET_VIEW_BEFORE_LOADING)//
            .bitmapConfig(BMP_CFG)//
            .build();//

    public static DisplayImageOptions displayOPDSOptions = new DisplayImageOptions.Builder() //
            .showImageOnFail(R.drawable.web)//
            .showImageForEmptyUri(R.drawable.web)//
            .showImageOnFail(R.drawable.web)//
            .cacheInMemory(true)//
            .cacheOnDisk(true)//
            .considerExifParams(false)//
            .imageScaleType(ImageScaleType.IN_SAMPLE_POWER_OF_2)//
            .resetViewBeforeLoading(RESET_VIEW_BEFORE_LOADING)//
            .bitmapConfig(BMP_CFG)//
            // .displayer(new FadeInBitmapDisplayer(200))//
            .build();//

    public static DisplayImageOptions displayImageOptionsNoDiscCache = new DisplayImageOptions.Builder() //
            .showImageOnFail(COLOR_DRAWABLE)//
            .showImageForEmptyUri(COLOR_DRAWABLE)//
            .showImageOnFail(COLOR_DRAWABLE)//
            .cacheInMemory(true)//
            .imageScaleType(ImageScaleType.NONE)//
            .considerExifParams(false)//
            .cacheOnDisk(false)//
            .resetViewBeforeLoading(RESET_VIEW_BEFORE_LOADING)//
            .bitmapConfig(BMP_CFG)//
            .build();//

    public static DisplayImageOptions ligthOptions = new DisplayImageOptions.Builder() //
            .showImageOnFail(COLOR_DRAWABLE)//
            .showImageForEmptyUri(COLOR_DRAWABLE)//
            .showImageOnFail(COLOR_DRAWABLE)//
            .cacheInMemory(true)//
            .cacheOnDisk(false)//
            .considerExifParams(false)//
            .imageScaleType(ImageScaleType.NONE)//
            .resetViewBeforeLoading(RESET_VIEW_BEFORE_LOADING)//
            .bitmapConfig(BMP_CFG)//
            .build();//

    public static DisplayImageOptions ExportOptions = new DisplayImageOptions.Builder() //
            .showImageOnFail(COLOR_DRAWABLE)//
            .showImageForEmptyUri(COLOR_DRAWABLE)//
            .showImageOnFail(COLOR_DRAWABLE)//
            .cacheInMemory(false)//
            .considerExifParams(false)//
            .cacheOnDisk(false)//
            .imageScaleType(ImageScaleType.NONE)//
            .resetViewBeforeLoading(RESET_VIEW_BEFORE_LOADING)//
            .bitmapConfig(BMP_CFG)//
            .build();//

    public static DisplayImageOptions noneOptions = new DisplayImageOptions.Builder() //
            .showImageOnFail(COLOR_DRAWABLE)//
            .showImageForEmptyUri(COLOR_DRAWABLE)//
            .showImageOnFail(COLOR_DRAWABLE)//
            .cacheInMemory(false)//
            .cacheOnDisk(false)//
            .considerExifParams(false)//
            .imageScaleType(ImageScaleType.NONE)//
            .bitmapConfig(BMP_CFG)//
            .build();//

    public static void display(final Context c, final String path, final ImageView imageView, final int sizePx, final int page, ImageLoadingListener listener) {
        display(c, path, imageView, sizePx, page, listener, IMG.displayCacheMemoryDisc);

    }

    public static void display(final Context c, final String path, final ImageView imageView, final int sizePx, final int page, ImageLoadingListener listener, DisplayImageOptions displayOptions) {
        if (path == null) {
            // imageView.setImageResource(COLOR_DRAWABLE);
            return;
        }

        PageUrl pageUrl = IMG.toPageUrl(path, page, sizePx);
        pageUrl.setUnic(BookCSS.get().toCssString().hashCode());
        pageUrl.setInvert(!AppState.get().isDayNotInvert);

        final String url = pageUrl.toString();
        if (listener != null) {
            ImageLoader.getInstance().displayImage(url, imageView, displayOptions, listener);
        } else {
            ImageLoader.getInstance().displayImage(url, imageView, displayOptions);
        }

    }

    public static void displayPageUrl(final Context c, final String pageUrl, final ImageView imageView, ImageLoadingListener listener) {
        if (pageUrl == null) {
            // imageView.setImageResource(COLOR_DRAWABLE);
            return;
        }

        if (listener != null) {
            ImageLoader.getInstance().displayImage(pageUrl, imageView, IMG.displayCacheMemoryDisc, listener);
        } else {
            ImageLoader.getInstance().displayImage(pageUrl, imageView, IMG.displayCacheMemoryDisc);
        }
    }

    public static void clearMemoryCache() {
        try {
            ImageLoader.getInstance().clearMemoryCache();
        } catch (Exception e) {
            LOG.e(e);
        }
    }

    public static void clearDiscCache() {
        try {
            ImageLoader.getInstance().clearDiskCache();
        } catch (Exception e) {
            LOG.e(e);
        }
    }

    public static void clearCache(String path) {
        try {
            String url = IMG.toUrl(path, ImageExtractor.COVER_PAGE, IMG.getImageSize());

            String key = MemoryCacheUtils.generateKeyNoTarget(url);
            ImageLoader.getInstance().getMemoryCache().remove(key);

            ImageLoader.getInstance().getDiskCache().remove(url);
        } catch (Exception e) {
            LOG.e(e);
        }
    }

    private static String pattern = Pattern.quote("||");

    public static String getRealPathFromURI(final Context c, final Uri contentURI) {
        final Cursor cursor = c.getContentResolver().query(contentURI, null, null, null, null);
        if (cursor == null) { // Source is Dropbox or other similar local file
                              // path
            return contentURI.getPath();
        } else {
            cursor.moveToFirst();
            final int idx = cursor.getColumnIndex(MediaStore.Images.ImageColumns.DATA);
            return cursor.getString(idx);
        }
    }

    public static void getCoverPage(ImageView img, String path, int width) {
        ImageLoader.getInstance().displayImage(IMG.toUrl(path, ImageExtractor.COVER_PAGE, width), img);
    }

    public static void getCoverPageWithEffect(ImageView img, String path, int width, ImageLoadingListener listener) {
        String url = IMG.toUrl(path, ImageExtractor.COVER_PAGE, width);
        ImageLoader.getInstance().displayImage(url, img, IMG.displayCacheMemoryDisc, listener);
    }

    public static Bitmap loadCoverPageWithEffect(String path, int width) {
        return ImageLoader.getInstance().loadImageSync(IMG.toUrl(path, ImageExtractor.COVER_PAGE, width), IMG.displayCacheMemoryDisc);
    }

    public static void getCoverPageWithEffectPos(ImageView img, String path, int width, int pos, ImageLoadingListener listener) {
        ImageLoader.getInstance().displayImage(IMG.toUrlPos(path, ImageExtractor.COVER_PAGE, width, pos), img, IMG.displayCacheMemoryDisc, listener);
    }

    public static String toUrl(final String path, final int page, final int width) {
        PageUrl pdfUrl = new PageUrl(path, page, width, 0, false, false, 0);
        pdfUrl.setUnic(0);
        return pdfUrl.toString();
    }

    public static String toUrlPos(final String path, final int page, final int width, int pos) {
        PageUrl pdfUrl = new PageUrl(path, page, width, 0, false, false, 0);
        return pdfUrl.toString();
    }

    public static PageUrl toPageUrl(final String path, final int page, final int width) {
        return new PageUrl(path, page, width, 0, false, false, 0);
    }

    public static String toUrlWithContext(final String path, final int page, final int width) {
        PageUrl pdfUrl = new PageUrl(path, page, width, 0, false, false, 0);
        return pdfUrl.toString();
    }

    public static String toUrl(final String path, final int page, final int width, int heigth) {
        return new PageUrl(path, page, width, 0, false, false, 0, heigth).toString();
    }

    public static String toUrl(final Uri path, final int page, final int width) {
        return toUrl(path.getPath(), page, width);
    }

    public static String toUrl(final Uri path, final int page, final int width, final int number) {
        return new PageUrl(path.getPath(), page, width, number, false, false, 0).toString();
    }

    public static String[] fromUrl(final String url) {
        return url.split(pattern);
    }
}
