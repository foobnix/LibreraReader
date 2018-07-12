package com.foobnix.pdf.search.activity;

import com.foobnix.android.utils.LOG;
import com.foobnix.pdf.info.IMG;
import com.foobnix.pdf.info.R;
import com.foobnix.pdf.info.wrapper.MagicHelper;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.assist.FailReason;
import com.nostra13.universalimageloader.core.listener.ImageLoadingListener;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

public class ImagePageFragment extends Fragment {
    public static final String POS = "pos";
    public static final String PAGE_PATH = "pagePath";
    public static final String IS_TEXTFORMAT = "isTEXT";

    int page;

    public static volatile int count = 0;

    Handler handler;

    long lifeTime = 0;

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

    @Override
    public void onResume() {
        super.onResume();
        image.clickUtils.init();
        LOG.d("fonResume", page);
    }

    public int getPriority() {
        return Math.min(Math.abs(PageImageState.currentPage - page), 10);
    }

    int loadImageId;

    public void loadImage() {
        if (getPriority() > 2 || isDetached() || !isVisible()) {
            LOG.d("ImagePageFragment1  skip loading page ", page, "getPriority", getPriority(), "page");
            return;
        }

        LOG.d("ImagePageFragment1 loadImage start with lifetime ", page, System.currentTimeMillis() - lifeTime);

        loadImageId = ImageLoader.getInstance().loadImage(getPath(), IMG.ligthOptions, new ImageLoadingListener() {

            @Override
            public void onLoadingStarted(String arg0, View arg1) {
                LOG.d("ImagePageFragment1 onLoadingStarted ", page, isVisible(), isDetached(), isInLayout(), isAdded());
                count++;

                if (LOG.isEnable) {
                    text.setText("onLoadingStarted");
                }
            }

            @Override
            public void onLoadingFailed(String arg0, View arg1, FailReason arg2) {
                text.setText("Failed " + arg2.getType());
                // text.setVisibility(View.GONE);
                // loadImage();
                count--;

                if (LOG.isEnable) {
                    text.setText("onLoadingFailed");
                }
            }

            @Override
            public void onLoadingComplete(String arg0, View arg1, Bitmap bitmap) {
                LOG.d("ImagePageFragment1 onLoadingComplete ", page, "isVisible", isVisible());
                if (text != null && isVisible()) {
                    text.setVisibility(View.GONE);
                    image.addBitmap(bitmap);
                } else {
                    bitmap.recycle();
                    bitmap = null;
                }
                count--;

                if (LOG.isEnable) {
                    text.setText("onLoadingComplete");
                }
            }

            @Override
            public void onLoadingCancelled(String arg0, View arg1) {
                count--;
                loadImage();

                if (LOG.isEnable) {
                    text.setText("onLoadingCancelled");
                }
            }
        });
    }

    Runnable callback = new Runnable() {

        @Override
        public void run() {
            if (!isDetached()) {
                loadImage();
            } else {
                LOG.d("Image page is detached");
            }
        }

    };
    private PageImaveView image;
    private TextView text;

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        LOG.d("ImagePageFragment1 onDestroyView ", page, "Lifi Time: ", System.currentTimeMillis() - lifeTime);
        // ImageLoader.getInstance().cancelDisplayTaskForID(loadImageId);
        handler.removeCallbacksAndMessages(null);
        image = null;
    }

    @Override
    public void onDetach() {
        super.onDetach();
        LOG.d("ImagePageFragment1 onDetach ", page, "Lifi Time: ", System.currentTimeMillis() - lifeTime);
        handler.removeCallbacksAndMessages(null);
        image = null;
    }

}
