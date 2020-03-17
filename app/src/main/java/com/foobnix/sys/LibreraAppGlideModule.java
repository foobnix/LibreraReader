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
    final static ModelLoader<String, Bitmap> modelLoader = new ModelLoader<String, Bitmap>() {
        @Nullable
        @Override
        public LoadData<Bitmap> buildLoadData(@NonNull final String s, int width, int height, @NonNull Options options) {
            LOG.d("LibreraAppGlideModule buildLoadData", s);

            return new LoadData<>(new ObjectKey(s), new DataFetcher<Bitmap>() {
                @Override
                public void loadData(@NonNull Priority priority, @NonNull DataCallback<? super Bitmap> callback) {
                    try {
                       // LOG.d("LibreraAppGlideModule loadData", priority, s);
                        final InputStream stream = ImageExtractor.getInstance(LibreraApp.context).getStream(s, null);
                        if (stream instanceof InputStreamBitmap) {
                            Bitmap bitmap = ((InputStreamBitmap) stream).getBitmap();
                            callback.onDataReady(bitmap);
                            //LOG.d("Bitmap-test-1",bitmap, bitmap.getWidth(), bitmap.getHeight(), bitmap.getConfig());
                        }else {

                            callback.onDataReady( BitmapFactory.decodeStream(stream));
                        }
                        LOG.d("LibreraAppGlideModule onDataReady", stream);

                    } catch (Exception e) {
                        callback.onDataReady( ImageExtractor.messageFileBitmap("#error", ""));
                        LOG.e(e);
                    }

                }

                @Override
                public void cleanup() {

                }

                @Override
                public void cancel() {

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