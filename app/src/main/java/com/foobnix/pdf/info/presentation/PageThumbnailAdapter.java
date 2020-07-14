package com.foobnix.pdf.info.presentation;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.foobnix.android.utils.LOG;
import com.foobnix.android.utils.TxtUtils;
import com.foobnix.model.AppSP;
import com.foobnix.pdf.info.IMG;
import com.foobnix.pdf.info.PageUrl;
import com.foobnix.pdf.info.R;

public class PageThumbnailAdapter extends BaseAdapter {

    private LayoutInflater inflater;
    private String path;

    private int pageCount;
    private Context c;
    private int currentPage;

    public PageThumbnailAdapter(Context c, int pageCount, int currentPage) {
        this.c = c;
        this.pageCount = pageCount;
        this.currentPage = currentPage;
        inflater = LayoutInflater.from(c);
    }

    @Override
    public int getCount() {
        return pageCount;
    }

    @Override
    public Object getItem(int position) {
        return null;
    }

    @Override
    public long getItemId(int position) {
        return 0;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View view = convertView;
        if (view == null) {
            view = inflater.inflate(R.layout.item_page, null);
        }
        if (currentPage == position) {
            view.setBackgroundResource(R.color.tint_selector);
        } else {
            view.setBackgroundColor(Color.TRANSPARENT);
        }

        ImageView img = (ImageView) view.findViewById(R.id.image1);
        LayoutParams lp = IMG.updateImageSizeSmall(img);
        if (AppSP.get().isDouble) {
            lp.width = lp.width * 2;
        }

        PageUrl pageUrl = getPageUrl(position);
        final String url = pageUrl.toString();

        LOG.d("Glide-load-into", c);
        IMG.with(c).asBitmap().diskCacheStrategy(DiskCacheStrategy.NONE).load(url).into(img);

        TextView txt = (TextView) view.findViewById(R.id.text1);
        String text = TxtUtils.deltaPage((position + 1));
        txt.setText(text);


        txt.setVisibility(View.VISIBLE);
        img.setVisibility(View.VISIBLE);

        view.setContentDescription(c.getString(R.string.page)+ " "+text);

        return view;
    }



    public PageUrl getPageUrl(int page) {
        return null;
    }

    public int getCurrentPage() {
        return currentPage;
    }

    public void setCurrentPage(int currentPage) {
        this.currentPage = currentPage;
    }

}
