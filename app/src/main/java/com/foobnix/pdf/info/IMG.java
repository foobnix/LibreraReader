package com.foobnix.pdf.info;

import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.provider.MediaStore;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.ImageView;

import androidx.annotation.Nullable;

import com.bumptech.glide.Glide;
import com.bumptech.glide.RequestManager;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;
import com.foobnix.android.utils.Dips;
import com.foobnix.android.utils.LOG;
import com.foobnix.model.AppState;
import com.foobnix.pdf.search.activity.HorizontalViewActivity;
import com.foobnix.sys.ImageExtractor;
import com.foobnix.ui2.MainTabs2;

import org.ebookdroid.LibreraApp;
import org.ebookdroid.ui.viewer.VerticalViewActivity;

import java.util.regex.Pattern;

public class IMG {

    public static final float WIDTH_DK = 1.4f;
    public static final int DP5 = -Dips.dpToPx(40);
    public static final Config BMP_CFG = Config.RGB_565;
    public static final int TWO_LINE_COVER_SIZE = 74;
    private static final ColorDrawable COLOR_DRAWABLE = new ColorDrawable(Color.LTGRAY);
    public static boolean RESET_VIEW_BEFORE_LOADING = true;
    public static Drawable bookBGWithMark;
    public static Drawable bookBGNoMark;
    public static Context context;
    private static String pattern = Pattern.quote("||");

    public static void init(Context context) {

        IMG.context = context;

        bookBGWithMark = context.getResources().getDrawable(R.drawable.bookeffect2);
        bookBGNoMark = context.getResources().getDrawable(R.drawable.bookeffect1);

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

    public static void clearMemoryCache() {
        Glide.get(LibreraApp.context).clearMemory();
    }

    public static RequestManager with(Context a) {
        if (a instanceof HorizontalViewActivity) {
            return Glide.with((HorizontalViewActivity) a);
        } else if (a instanceof VerticalViewActivity) {
            return Glide.with((VerticalViewActivity) a);
        } else if (a instanceof MainTabs2) {
            return Glide.with((MainTabs2) a);
        } else {
            return Glide.with(LibreraApp.context);
        }
    }

    public static void pauseRequests(Context a) {
        LOG.d("Glide-pause", a);
        //with(a).pauseRequests();
    }

    public static void resumeRequests(Context a) {
        LOG.d("Glide-resume", a);
        //with(a).resumeRequests();
    }

    public static void clear(ImageView image) {
        LOG.d("Glide-clear", image.getContext());
        try {
            with(image.getContext()).clear(image);
        } catch (Exception e) {
            LOG.e(e);
        }
    }

    public static void clear(Context c, Target t) {
        LOG.d("Glide-clear", c);
        try {
            with(c).clear(t);
        } catch (Exception e) {
            LOG.e(e);
        }

    }

    public static void clearDiscCache() {
        try {
            new Thread() {
                @Override
                public void run() {
                    Glide.get(LibreraApp.context).clearDiskCache();
                }
            }.start();
        } catch (Exception e) {
            LOG.e(e);
        }
    }

    public static void clearCache(String path) {
        try {


            String url = IMG.toUrl(path, ImageExtractor.COVER_PAGE, IMG.getImageSize());
            //Glide.get(LibreraApp.context).clearMemory();
            //Glide.get(LibreraApp.context).getRegistry()

        } catch (Exception e) {
            LOG.e(e);
        }
    }

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
        final String url = IMG.toUrl(path, ImageExtractor.COVER_PAGE, width);
        Glide.with(LibreraApp.context).asBitmap().load(url).into(img);
    }

    public static void getCoverPageWithEffect(ImageView img, String path, int width, Runnable run) {
        String url = IMG.toUrl(path, ImageExtractor.COVER_PAGE, width);
        LOG.d("Bitmap-test-load", path);
        IMG.with(img.getContext())
                .asBitmap()
                .load(url)
                .override(Target.SIZE_ORIGINAL, Target.SIZE_ORIGINAL)
                .diskCacheStrategy(DiskCacheStrategy.RESOURCE)
                .listener(new RequestListener<Bitmap>() {
                    @Override
                    public boolean onLoadFailed(@Nullable GlideException e, Object model, Target<Bitmap> target, boolean isFirstResource) {
                        LOG.d("Bitmap-test-2", "failed");

                        return false;
                    }

                    @Override
                    public boolean onResourceReady(Bitmap bitmap, Object model, Target<Bitmap> target, DataSource dataSource, boolean isFirstResource) {
                        target.onResourceReady(bitmap, null);
                        LOG.d("Bitmap-test-2", bitmap, bitmap.getWidth(), bitmap.getHeight(), bitmap.getConfig());

                        if (run != null) {
                            run.run();
                        }
                        return true;
                    }
                })
                .into(img);
    }


    public static void getCoverPageWithEffectPos(ImageView img, String path, int width, int pos) {
        final String url = IMG.toUrlPos(path, ImageExtractor.COVER_PAGE, width, pos);
        try {
            Glide.with(LibreraApp.context).asBitmap().load(url).into(img);
        }catch (Exception e){
            LOG.e(e);
        }
    }

    public static String toUrl(final String path, final int page, final int width) {
        PageUrl pdfUrl = new PageUrl(path, page, width, 0, false, false, 0);
        pdfUrl.setUnic(0);
        pdfUrl.hash = (""+AppState.get().isBookCoverEffect).hashCode();
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
