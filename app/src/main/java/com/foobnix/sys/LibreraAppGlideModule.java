package com.foobnix.sys;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.bumptech.glide.Glide;
import com.bumptech.glide.GlideBuilder;
import com.bumptech.glide.Priority;
import com.bumptech.glide.Registry;
import com.bumptech.glide.annotation.GlideModule;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.DecodeFormat;
import com.bumptech.glide.load.Options;
import com.bumptech.glide.load.data.DataFetcher;
import com.bumptech.glide.load.engine.executor.GlideExecutor;
import com.bumptech.glide.load.model.ModelLoader;
import com.bumptech.glide.load.model.ModelLoaderFactory;
import com.bumptech.glide.load.model.MultiModelLoaderFactory;
import com.bumptech.glide.module.AppGlideModule;
import com.bumptech.glide.request.RequestOptions;
import com.bumptech.glide.signature.ObjectKey;
import com.foobnix.android.utils.LOG;

import org.ebookdroid.LibreraApp;

import java.io.InputStream;

import static com.bumptech.glide.load.engine.executor.GlideExecutor.newSourceBuilder;

@GlideModule
public class LibreraAppGlideModule extends AppGlideModule {

    static Bitmap cache;
    static String path;

    final static ModelLoader<String, Bitmap> modelLoader = new ModelLoader<String, Bitmap>() {
        @Nullable
        @Override
        public LoadData<Bitmap> buildLoadData(@NonNull final String s, int width, int height, @NonNull Options options) {

            final ObjectKey sourceKey = new ObjectKey(s);
            LOG.d("LibreraAppGlideModule sourceKey", s, sourceKey.hashCode());


            return new LoadData<>(sourceKey, new DataFetcher<Bitmap>() {
                volatile boolean isCanced = false;

                @Override
                public void loadData(@NonNull Priority priority, @NonNull DataCallback<? super Bitmap> callback) {
                    try {

                        if (isCanced) {
                            return;
                        }
                        if (s.equals(path)) {
                            callback.onDataReady(cache);
                            return;
                        }
                        LOG.d("LibreraAppGlideModule loadData", s);
                        final InputStream stream = ImageExtractor.getInstance(LibreraApp.context).getStream(s, null);
                        if (stream instanceof InputStreamBitmap) {
                            Bitmap bitmap = ((InputStreamBitmap) stream).getBitmap();
                            if (bitmap == null) {
                                Thread.sleep(250);
                                bitmap = ImageExtractor.getInstance(LibreraApp.context).proccessOtherPage(s);
                                if (bitmap == null) {
                                    LOG.d("Bitmap-test-1-cancel", bitmap, s);
                                    callback.onDataReady(ImageExtractor.messageFileBitmap("#error null", ""));
                                    return;
                                }
                            }

                            if (isCanced) {
                                LOG.d("Bitmap-test-1-cancel", bitmap, s);
                                bitmap.recycle();
                                return;
                            }

                            LOG.d("Bitmap-test-1", bitmap, bitmap.getWidth(), bitmap.getHeight(), bitmap.getConfig());
                            callback.onDataReady(bitmap);

                            path = s;
                            cache = bitmap;
                        } else {
                            callback.onDataReady(BitmapFactory.decodeStream(stream));
                        }
                        LOG.d("LibreraAppGlideModule onDataReady", stream);

                    } catch (Exception e) {
                        callback.onDataReady(ImageExtractor.messageFileBitmap("#error", ""));
                        LOG.e(e);
                    }

                }

                @Override
                public void cleanup() {
                    isCanced = true;
                    LOG.d("LibreraAppGlideModule cleanup", s);


                }

                @Override
                public void cancel() {
                    isCanced = true;
                    LOG.d("LibreraAppGlideModule cancel", s);

                }

                @NonNull
                @Override
                public Class<Bitmap> getDataClass() {
                    return Bitmap.class;
                }

                @NonNull
                @Override
                public DataSource getDataSource() {
                    return DataSource.LOCAL;
                }
            });
        }

        @Override
        public boolean handles(@NonNull String s) {
            LOG.d("LibreraAppGlideModule handles", s);
            return true;//s.startsWith("{");
        }
    };

    @Override
    public void applyOptions(@NonNull Context context, @NonNull GlideBuilder builder) {
        super.applyOptions(context, builder);
        builder.setDefaultRequestOptions(new RequestOptions().format(DecodeFormat.PREFER_RGB_565));
        builder.setSourceExecutor(
                newSourceBuilder()
                        .setUncaughtThrowableStrategy(GlideExecutor.UncaughtThrowableStrategy.IGNORE)
                        .setThreadCount(1)
                        //.setThreadTimeoutMillis(2000)
                        .build());


    }

    @Override
    public void registerComponents(Context context, Glide glide, Registry registry) {
        LOG.d("LibreraAppGlideModule registerComponents");
        registry.prepend(String.class, Bitmap.class, new ModelLoaderFactory<String, Bitmap>() {
            @NonNull
            @Override
            public ModelLoader<String, Bitmap> build(@NonNull MultiModelLoaderFactory multiFactory) {
                return modelLoader;
            }

            @Override
            public void teardown() {

            }
        });

    }


}