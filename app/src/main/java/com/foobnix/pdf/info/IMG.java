package com.foobnix.pdf.info;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.widget.ImageView;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.bumptech.glide.Glide;
import com.bumptech.glide.RequestBuilder;
import com.bumptech.glide.RequestManager;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.EncodeStrategy;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;
import com.bumptech.glide.signature.ObjectKey;
import com.foobnix.LibreraApp;
import com.foobnix.android.utils.Dips;
import com.foobnix.android.utils.LOG;
import com.foobnix.model.AppState;
import com.foobnix.pdf.info.wrapper.MagicHelper;
import com.foobnix.pdf.search.activity.HorizontalViewActivity;
import com.foobnix.sys.ImageExtractor;
import com.foobnix.ui2.MainTabs2;

import org.ebookdroid.ui.viewer.VerticalViewActivity;

import java.util.regex.Pattern;

public class IMG {

    public static final float WIDTH_DK = 1.4f;
    public static final int TWO_LINE_COVER_SIZE = 74;
    public static Drawable bookBGWithMark;
    public static Drawable bookBGNoMark;
    public static Context context;

    public static void init(Context context) {

        IMG.context = context;

        bookBGWithMark = ContextCompat.getDrawable(context, R.drawable.bookeffect2);
        bookBGNoMark = ContextCompat.getDrawable(context, R.drawable.bookeffect1);

    }

    public static int getImageSize() {
        return Dips.dpToPx(Math.max(AppState.get().coverSmallSize, AppState.get().coverBigSize));
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

    public static LayoutParams updateImageSizeSmallDir(View imageView) {
        if (imageView == null || imageView.getLayoutParams() == null) {
            return null;
        }
        LayoutParams lp = imageView.getLayoutParams();
        lp.width = (int) (Dips.dpToPx(AppState.get().coverSmallSize) / 1.5);
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
        if (LibreraApp.context != null) {
            LOG.d("clearMemoryCache", "Bitmap-test-2 clearMemoryCache");
            Glide.get(LibreraApp.context)
                 .clearMemory();
        }
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
        try {
            LOG.d("Glide-clear", image.getContext());
            Activity activity = ((Activity) image.getContext());
            if (Build.VERSION.SDK_INT < 17 || !activity.isDestroyed()) {
                with(image.getContext()).clear(image);
            }
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
        LOG.d("clearDiscCache", "Bitmap-test-2 clearDiscCache");
        AppsConfig.executorService.execute(new Runnable() {
            @Override public void run() {
                try {
                    if (LibreraApp.context != null) {
                        Glide.get(LibreraApp.context)
                             .clearDiskCache();
                    }
                } catch (Exception e) {
                    LOG.e(e);
                }
            }
        });

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


    public static interface ResourceReady {
        void onResourceReady(Bitmap bitmap);
    }


    private static String getCoverUrl(String path){
        return toUrl(path, ImageExtractor.COVER_PAGE, IMG.getImageSize());
    }
    public static String getCoverUrl1(String path){
        return toUrl(path, ImageExtractor.COVER_PAGE, IMG.getImageSize());
    }
    public static final DiskCacheStrategy AUTOMATIC1 =
            new DiskCacheStrategy() {
                @Override
                public boolean isDataCacheable(DataSource dataSource) {
                    return dataSource == DataSource.LOCAL;
                }

                @Override
                public boolean isResourceCacheable(
                        boolean isFromAlternateCacheKey, DataSource dataSource, EncodeStrategy encodeStrategy) {
                    return dataSource == DataSource.LOCAL;
                }

                @Override
                public boolean decodeCachedResource() {
                    return true;
                }

                @Override
                public boolean decodeCachedData() {
                    return true;
                }
            };


    public static RequestBuilder<Bitmap> getCoverPageWithEffect(Context context, String path, ResourceReady run) {
        int imageSize = IMG.getImageSize();
        String url = toUrl(path, ImageExtractor.COVER_PAGE, imageSize);
        return IMG.with(context)
           .asBitmap()
           .load(url)
           .override(Target.SIZE_ORIGINAL)
                .onlyRetrieveFromCache(false)
           .diskCacheStrategy(DiskCacheStrategy.RESOURCE)
           //.override(imageSize)
              .signature(new ObjectKey(url.hashCode()))
           .listener(new RequestListener<>() {
               @Override public boolean onLoadFailed(@Nullable GlideException e, Object model, Target<Bitmap> target,
                                                     boolean isFirstResource) {
                   return false;
               }

               @Override
               public boolean onResourceReady(Bitmap bitmap, Object model, Target<Bitmap> target, DataSource dataSource,
                                              boolean isFirstResource) {
                   target.onResourceReady(bitmap, null);
                   LOG.d("Bitmap-test-2", bitmap.getWidth(), bitmap.getHeight(), bitmap.getConfig(),
                           dataSource,isFirstResource,model);

                   if (run != null) {
                       run.onResourceReady(null);
                   }
                   return true;
               }
           });

    }


    private static String toUrl(final String path, final int page, final int width) {
        PageUrl pdfUrl = new PageUrl(path, page, width, 0, false, false, 0);
        pdfUrl.setUnic(0);
        pdfUrl.hash =
                ("" + AppState.get().isBookCoverEffect + TintUtil.getColorInDayNighth() + AppState.get().sortByBrowse).hashCode() + MagicHelper.hash();
        return pdfUrl.toString();
    }


    public static String toUrlWithContext(final String path, final int page, final int width) {
        PageUrl pdfUrl = new PageUrl(path, page, width, 0, false, false, 0);
        return pdfUrl.toString();
    }

}
