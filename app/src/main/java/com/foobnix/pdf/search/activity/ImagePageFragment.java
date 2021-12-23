package com.foobnix.pdf.search.activity;

import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.loader.app.LoaderManager;
import androidx.loader.content.AsyncTaskLoader;
import androidx.loader.content.Loader;

import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;
import com.foobnix.android.utils.LOG;
import com.foobnix.pdf.info.IMG;
import com.foobnix.pdf.info.R;
import com.foobnix.pdf.info.wrapper.MagicHelper;
import com.foobnix.sys.ImageExtractor;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;


public class ImagePageFragment extends Fragment {
    public static final String POS = "pos";
    public static final String PAGE_PATH = "pagePath";
    public static final String IS_TEXTFORMAT = "isTEXT";
    final static ExecutorService executorService = Executors.newSingleThreadExecutor();
    public static volatile int count = 0;
    int page;
    Handler handler;
    long lifeTime = 0;
    int loadImageId;
    boolean fistTime = true;
    CustomTarget<Bitmap> target = null;
    Future<?> submit;
    private PageImaveView image;
    private TextView text;
    Runnable callback = new Runnable() {

        @Override
        public void run() {
            if (!isDetached()) {
                loadImageGlide();
            } else {
                LOG.d("Image page is detached");
            }
        }

    };

    public String getPath() {
        return getArguments().getString(PAGE_PATH);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.page_n, container, false);

        page = getArguments().getInt(POS);


        text = (TextView) view.findViewById(R.id.text1);
        image = (PageImaveView) view.findViewById(R.id.myImage1);

        image.setPageNumber(page);
        text.setText(getString(R.string.page) + " " + (page + 1));

        text.setTextColor(MagicHelper.getTextColor());

        handler = new Handler();
        handler.postDelayed(new Runnable() {

            @Override
            public void run() {
                LOG.d("ImagePageFragment1  run ", page, "getPriority", getPriority(), "counter", count);
                if (isVisible()) {
                    if (0 == getPriority()) {
                        handler.post(callback);
                    } else if (1 == getPriority()) {
                        handler.postDelayed(callback, 200);
                    } else {
                        handler.postDelayed(callback, getPriority() * 200);
                    }
                }

            }
        }, 150);
        lifeTime = System.currentTimeMillis();


        return view;
    }

    public void loadImageGlide21() {

        submit = executorService.submit(new Runnable() {
            @Override
            public void run() {
                if (submit.isCancelled()) {
                    LOG.d("loadImageGlide-isCancelled 1");
                    return;
                }
                Bitmap bitmap = ImageExtractor.getInstance(getContext()).proccessOtherPage(getPath());

                if (submit.isCancelled()) {
                    LOG.d("loadImageGlide-isCancelled 2");
                    return;
                }

                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        text.setVisibility(View.GONE);
                        if (bitmap != null && image != null) {
                            image.addBitmap(bitmap);
                        }
                    }
                });

            }
        });

    }

    public void loadImageGlide2() {
        final LoaderManager.LoaderCallbacks<Bitmap> callback = new LoaderManager.LoaderCallbacks<Bitmap>() {
            @NonNull
            @Override
            public Loader<Bitmap> onCreateLoader(int id, @Nullable Bundle args) {
                return new AsyncTaskLoader<Bitmap>(getContext()) {

                    @Nullable
                    @Override
                    public Bitmap loadInBackground() {
                        return ImageExtractor.getInstance(getContext()).proccessOtherPage(getPath());

                    }
                };
            }

            @Override
            public void onLoadFinished(@NonNull Loader<Bitmap> loader, Bitmap data) {
                LOG.d("loadImageGlide-onLoadFinished");

                text.setVisibility(View.GONE);
                if (data != null && image != null) {
                    image.addBitmap(data);
                }
            }

            @Override
            public void onLoaderReset(@NonNull Loader<Bitmap> loader) {
                LOG.d("loadImageGlide-onLoaderReset");
            }
        };
        LoaderManager.getInstance(getActivity()).initLoader(getPath().hashCode(), null, callback).forceLoad();
    }

    public void loadImageGlide() {
        if (image != null && image.getWidth() == 0) {
            return;
        }
        target = new CustomTarget<Bitmap>() {
            @Override
            public void onResourceReady(@NonNull Bitmap resource, @Nullable Transition<? super Bitmap> transition) {
                text.setVisibility(View.GONE);
                if (resource != null && image != null) {
                    image.addBitmap(resource);
                }
            }

            @Override
            public void onLoadCleared(Drawable placeholder) {

            }

        };
        LOG.d("Glide-load-into", getActivity());
        IMG.with(getActivity())
                .asBitmap()
                .load(getPath())
                .diskCacheStrategy(DiskCacheStrategy.NONE)
                .into(target);
    }

    @Override
    public void onResume() {
        super.onResume();
        if (image != null) {
            image.clickUtils.init();

            float[] values = new float[10];
            if (image != null) {
                image.imageMatrix().getValues(values);
                if (values[Matrix.MSCALE_X] == 0) {
                    PageImageState.get().isAutoFit = true;
                    image.autoFit();
                    LOG.d("fonResume-autofit", page);
                }
            }

        }
    }

    public int getPriority() {
        return Math.min(Math.abs(PageImageState.currentPage - page), 10);
    }


    @Override
    public void onDestroyView() {

        super.onDestroyView();
        LOG.d("loadImageGlide-onDestroyView");
//        if (submit != null) {
//            submit.cancel(true);
//
//        }
//        if (image != null) {
//            image.recycle();
//        }
//        LoaderManager.getInstance(getActivity()).destroyLoader(getPath().hashCode());

        if (Build.VERSION.SDK_INT >= 17 && !getActivity().isDestroyed()) {
            IMG.clear(getActivity(), target);
        } else if (!getActivity().isFinishing()) {
            IMG.clear(getActivity(), target);
        }


        LOG.d("ImagePageFragment1 onDetach ", page, "Lifi Time: ", System.currentTimeMillis() - lifeTime);
        if (handler != null) {
            handler.removeCallbacksAndMessages(null);
        }
        image = null;
    }

}
