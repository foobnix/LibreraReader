package com.foobnix.pdf.search.activity;

import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.target.SimpleTarget;
import com.bumptech.glide.request.transition.Transition;
import com.foobnix.android.utils.LOG;
import com.foobnix.pdf.info.IMG;
import com.foobnix.pdf.info.R;
import com.foobnix.pdf.info.databinding.PageNBinding;
import com.foobnix.pdf.info.wrapper.MagicHelper;


public class ImagePageFragment extends Fragment {
    public static final String POS = "pos";
    public static final String PAGE_PATH = "pagePath";
    public static final String IS_TEXTFORMAT = "isTEXT";
    public static volatile int count = 0;
    int page;
    Handler handler;
    long lifeTime = 0;
    SimpleTarget<Bitmap> target = null;
    private PageNBinding binding;
    Runnable callback = () -> {
        if (!isDetached()) {
            loadImageGlide();
        } else {
            LOG.d("Image page is detached");
        }
    };

    public String getPath() {
        assert getArguments() != null;
        return getArguments().getString(PAGE_PATH);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = PageNBinding.inflate(inflater, container, false);

        assert getArguments() != null;
        page = getArguments().getInt(POS);

        binding.myImage1.setPageNumber(page);
        binding.text1.setText(getString(R.string.page) + " " + (page + 1));

        binding.text1.setTextColor(MagicHelper.getTextColor());

        handler = new Handler();
        handler.postDelayed(() -> {
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
        }, 150);
        lifeTime = System.currentTimeMillis();

        return binding.getRoot();
    }

    public void loadImageGlide() {
        if (binding.myImage1.getWidth() == 0) {
            return;
        }
        target = new SimpleTarget<Bitmap>() {
            @Override
            public void onResourceReady(@NonNull Bitmap resource, @Nullable Transition<? super Bitmap> transition) {
                binding.text1.setVisibility(View.GONE);
                binding.myImage1.addBitmap(resource);
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
        binding.myImage1.clickUtils.init();

        float[] values = new float[10];
        binding.myImage1.imageMatrix().getValues(values);
        if (values[Matrix.MSCALE_X] == 0) {
            PageImageState.get().isAutoFit = true;
            binding.myImage1.autoFit();
            LOG.d("fonResume-autofit", page);
        }
    }

    public int getPriority() {
        return Math.min(Math.abs(PageImageState.currentPage - page), 10);
    }


    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
        LOG.d("loadImageGlide-onDestroyView");
//        if (submit != null) {
//            submit.cancel(true);
//
//        }
//        if (image != null) {
//            image.recycle();
//        }
//        LoaderManager.getInstance(getActivity()).destroyLoader(getPath().hashCode());

        if (Build.VERSION.SDK_INT >= 17 && !requireActivity().isDestroyed()) {
            IMG.clear(getActivity(), target);
        } else if (!requireActivity().isFinishing()) {
            IMG.clear(getActivity(), target);
        }

        LOG.d("ImagePageFragment1 onDetach ", page, "Lifi Time: ", System.currentTimeMillis() - lifeTime);
        if (handler != null) {
            handler.removeCallbacksAndMessages(null);
        }
    }
}
