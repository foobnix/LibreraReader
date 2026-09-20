package com.foobnix.work;

import android.content.Context;
import android.graphics.Bitmap;
import androidx.annotation.NonNull;
import androidx.test.platform.app.InstrumentationRegistry;
import com.bumptech.glide.Glide;
import com.bumptech.glide.Priority;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.data.DataFetcher;
import com.bumptech.glide.load.model.ModelLoader;
import com.bumptech.glide.load.model.ModelLoaderFactory;
import com.bumptech.glide.load.model.MultiModelLoaderFactory;
import com.bumptech.glide.request.FutureTarget;
import com.bumptech.glide.signature.ObjectKey;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.junit.Test;
import static org.junit.Assert.*;

/** A slow warmup request must leave capacity for a cover requested by the UI. */
public class CoverExecutorConcurrencyTest {
    private static final class Cover {
        final String key;
        final CountDownLatch started, release;
        final byte[] png;
        Cover(String key, CountDownLatch started, CountDownLatch release, byte[] png) {
            this.key = key; this.started = started; this.release = release; this.png = png;
        }
    }

    @Test public void visibleCoverCanLoadWhileWarmupSourceIsBlocked() throws Exception {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        Bitmap image = Bitmap.createBitmap(4, 4, Bitmap.Config.ARGB_8888);
        ByteArrayOutputStream encoded = new ByteArrayOutputStream();
        try { assertTrue(image.compress(Bitmap.CompressFormat.PNG, 100, encoded)); }
        finally { image.recycle(); }
        byte[] png = encoded.toByteArray();
        Glide.get(context).getRegistry().prepend(Cover.class, InputStream.class,
                new ModelLoaderFactory<Cover, InputStream>() {
                    @NonNull @Override public ModelLoader<Cover, InputStream> build(
                            @NonNull MultiModelLoaderFactory ignored) {
                        return new ModelLoader<Cover, InputStream>() {
                            @Override public boolean handles(@NonNull Cover model) { return true; }
                            @Override public LoadData<InputStream> buildLoadData(@NonNull Cover model,
                                    int width, int height, @NonNull com.bumptech.glide.load.Options options) {
                                return new LoadData<>(new ObjectKey(model.key), new DataFetcher<InputStream>() {
                                    @Override public void loadData(@NonNull Priority priority,
                                            @NonNull DataCallback<? super InputStream> callback) {
                                        if (model.started != null) model.started.countDown();
                                        try {
                                            if (model.release != null && !model.release.await(8, TimeUnit.SECONDS))
                                                throw new IllegalStateException("Warmup was never released");
                                            callback.onDataReady(new ByteArrayInputStream(model.png));
                                        } catch (Exception failure) { callback.onLoadFailed(failure); }
                                    }
                                    @Override public void cleanup() {}
                                    @Override public void cancel() {}
                                    @NonNull @Override public Class<InputStream> getDataClass() { return InputStream.class; }
                                    @NonNull @Override public DataSource getDataSource() { return DataSource.LOCAL; }
                                });
                            }
                        };
                    }
                    @Override public void teardown() {}
                });
        CountDownLatch started = new CountDownLatch(1), release = new CountDownLatch(1);
        FutureTarget<Bitmap> warmup = Glide.with(context).asBitmap()
                .load(new Cover("blocked-warmup", started, release, png))
                .priority(Priority.LOW).skipMemoryCache(true).diskCacheStrategy(DiskCacheStrategy.NONE).submit();
        FutureTarget<Bitmap> visible = null;
        try {
            assertTrue("Warmup did not enter the source executor", started.await(5, TimeUnit.SECONDS));
            visible = Glide.with(context).asBitmap()
                    .load(new Cover("visible-cover", null, null, png))
                    .priority(Priority.HIGH).skipMemoryCache(true).diskCacheStrategy(DiskCacheStrategy.NONE).submit();
            assertNotNull("Visible cover waited behind blocked warmup", visible.get(5, TimeUnit.SECONDS));
        } finally {
            release.countDown();
            Glide.with(context).clear(warmup);
            if (visible != null) Glide.with(context).clear(visible);
        }
    }
}
