package com.foobnix.pdf.search.activity;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.target.SimpleTarget;
import com.bumptech.glide.request.transition.Transition;
import com.foobnix.android.utils.LOG;
import com.foobnix.android.utils.Safe;
import com.foobnix.pdf.info.R;
import com.foobnix.pdf.info.wrapper.MagicHelper;

import org.ebookdroid.LibreraApp;


public class ImagePageFragment extends Fragment {
    public static final String POS = "pos";
    public static final String PAGE_PATH = "pagePath";
    public static final String IS_TEXTFORMAT = "isTEXT";
    public static volatile int count = 0;
    int page;
    Handler handler;

    long lifeTime = 0;
    int loadImageId;
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

    public void loadImageGlide() {
        Glide.with(LibreraApp.context)
                .asBitmap()
                .load(getPath())
                .diskCacheStrategy(DiskCacheStrategy.NONE)
                .into(Safe.target(new SimpleTarget<Bitmap>() {
                    @Override
                    public void onResourceReady(@NonNull Bitmap resource, @Nullable Transition<? super Bitmap> transition) {
                        text.setVisibility(View.GONE);
                        if (resource != null && image != null) {
                            image.addBitmap(resource);
                        }
                    }
                }));
    }

    @Override
    public void onResume() {
        super.onResume();
        if (image != null) {
            image.clickUtils.init();
        }
        LOG.d("fonResume", page);
    }

    public int getPriority() {
        return Math.min(Math.abs(PageImageState.currentPage - page), 10);
    }


    @Override
    public void onDestroyView() {
        super.onDestroyView();
        LOG.d("ImagePageFragment1 onDestroyView ", page, "Lifi Time: ", System.currentTimeMillis() - lifeTime);
        // ImageLoader.getInstance().cancelDisplayTaskForID(loadImageId);
        if (handler != null) {
            handler.removeCallbacksAndMessages(null);
        }
        image = null;

    }

    @Override
    public void onDetach() {
        super.onDetach();
        LOG.d("ImagePageFragment1 onDetach ", page, "Lifi Time: ", System.currentTimeMillis() - lifeTime);
        if (handler != null) {
            handler.removeCallbacksAndMessages(null);
        }
        image = null;
    }

}
