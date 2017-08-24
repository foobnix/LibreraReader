package com.foobnix.pdf.info.presentation;

import com.foobnix.android.utils.Dips;
import com.foobnix.pdf.info.IMG;
import com.foobnix.pdf.info.R;
import com.foobnix.pdf.info.wrapper.AppState;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

public class PageThumbnailAdapter extends BaseAdapter {

    private LayoutInflater inflater;
    private String path;

    private int pageCount;
    private Context c;
    private int currentPage;

    public PageThumbnailAdapter(Context c, String path, int pageCount, int currentPage) {
        this.c = c;
        this.path = path;
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
        IMG.updateImageSizeSmall(img);

        if (AppState.get().isCut || AppState.get().isDouble) {
            IMG.displayPageUrl(c, getPageUrl(position), img, null);
        } else {
            IMG.display(c, path, img, Dips.dpToPx(AppState.get().coverSmallSize), position, null, IMG.displayImageOptionsNoDiscCache);
        }

        TextView txt = (TextView) view.findViewById(R.id.text1);
        txt.setText("" + (position + 1));

        txt.setVisibility(View.VISIBLE);
        img.setVisibility(View.VISIBLE);

        return view;
    }

    public String getPageUrl(int page) {
        return path;
    }

    public int getCurrentPage() {
        return currentPage;
    }

    public void setCurrentPage(int currentPage) {
        this.currentPage = currentPage;
    }

}
