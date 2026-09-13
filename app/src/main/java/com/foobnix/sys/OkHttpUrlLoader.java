package com.foobnix.sys;

import androidx.annotation.NonNull;

import com.bumptech.glide.Priority;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.HttpException;
import com.bumptech.glide.load.Options;
import com.bumptech.glide.load.data.DataFetcher;
import com.bumptech.glide.load.model.GlideUrl;
import com.bumptech.glide.load.model.ModelLoader;
import com.bumptech.glide.load.model.ModelLoaderFactory;
import com.bumptech.glide.load.model.MultiModelLoaderFactory;
import com.bumptech.glide.util.ContentLengthInputStream;
import com.foobnix.android.utils.LOG;
import com.foobnix.model.AppState;
import com.foobnix.opds.OPDS;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

/**
 * Pictures off the network - catalogue covers and icons, pictures a book links to - are fetched
 * with the catalogues' own client, so they go through the same proxy, log-in and cache as the
 * catalogue does, and are handed to Glide still encoded: Glide decodes them at the size they are
 * shown at, into its own pool, rather than at whatever size the server sent.
 */
public class OkHttpUrlLoader implements ModelLoader<GlideUrl, InputStream> {

    @Override
    public LoadData<InputStream> buildLoadData(@NonNull GlideUrl url, int width, int height, @NonNull Options options) {
        return new LoadData<>(url, new Fetcher(url));
    }

    @Override
    public boolean handles(@NonNull GlideUrl url) {
        return true;
    }

    public static class Factory implements ModelLoaderFactory<GlideUrl, InputStream> {

        @NonNull
        @Override
        public ModelLoader<GlideUrl, InputStream> build(@NonNull MultiModelLoaderFactory multiFactory) {
            return new OkHttpUrlLoader();
        }

        @Override
        public void teardown() {
        }
    }

    static class Fetcher implements DataFetcher<InputStream>, Callback {

        private final GlideUrl url;
        private volatile Call call;
        private volatile DataCallback<? super InputStream> callback;
        private ResponseBody body;
        private InputStream stream;

        Fetcher(GlideUrl url) {
            this.url = url;
        }

        @Override
        public void loadData(@NonNull Priority priority, @NonNull DataCallback<? super InputStream> callback) {
            Request.Builder request = new Request.Builder().url(url.toStringUrl());
            for (Map.Entry<String, String> header : url.getHeaders().entrySet()) {
                request.addHeader(header.getKey(), header.getValue());
            }
            request.header("User-Agent", OPDS.USER_AGENT);
            request.header("Accept-Language", AppState.get().getAppLang());

            this.callback = callback;
            call = OPDS.client.newCall(request.build());
            call.enqueue(this);
        }

        @Override
        public void onFailure(@NonNull Call call, @NonNull IOException e) {
            LOG.d("OkHttpUrlLoader failed", url, e);
            DataCallback<? super InputStream> waiting = callback;
            if (waiting != null) {
                waiting.onLoadFailed(e);
            }
        }

        @Override
        public void onResponse(@NonNull Call call, @NonNull Response response) {
            body = response.body();
            DataCallback<? super InputStream> waiting = callback;
            if (waiting == null) {
                response.close();
                return;
            }
            if (response.isSuccessful() && body != null) {
                stream = ContentLengthInputStream.obtain(body.byteStream(), body.contentLength());
                waiting.onDataReady(stream);
            } else {
                waiting.onLoadFailed(new HttpException(response.message(), response.code()));
            }
        }

        @Override
        public void cleanup() {
            try {
                if (stream != null) {
                    stream.close();
                }
            } catch (IOException ignored) {
            }
            if (body != null) {
                body.close();
            }
            callback = null;
        }

        @Override
        public void cancel() {
            Call started = call;
            if (started != null) {
                started.cancel();
            }
        }

        @NonNull
        @Override
        public Class<InputStream> getDataClass() {
            return InputStream.class;
        }

        @NonNull
        @Override
        public DataSource getDataSource() {
            return DataSource.REMOTE;
        }
    }
}
