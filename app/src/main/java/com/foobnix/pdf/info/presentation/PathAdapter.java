package com.foobnix.pdf.info.presentation;

import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;
import android.widget.ImageView;
import com.foobnix.pdf.info.TintUtil;

import com.foobnix.android.utils.ResultResponse;
import com.foobnix.pdf.info.R;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class PathAdapter extends BaseAdapter {

    private List<Uri> uris = Collections.emptyList();
    private ResultResponse<Uri> onDeleClick;

    @Override
    public int getCount() {
        return uris.size();
    }

    @Override
    public Object getItem(int position) {
        return null;
    }

    @Override
    public long getItemId(int position) {
        return 0;
    }

    public void setPaths(List<String> paths) {
        List<Uri> uris = new ArrayList<Uri>();
        for (String str : paths) {
                uris.add(Uri.fromFile(new File(str)));
        }
        Collections.sort(uris, comparator);
        this.uris = uris;
        notifyDataSetChanged();
    }

    private final static Comparator<Uri> comparator = new Comparator<Uri>() {

        @Override
        public int compare(Uri lhs, Uri rhs) {
            return lhs.getPath().compareTo(rhs.getPath());
        }
    };
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View browserItem = LayoutInflater.from(parent.getContext()).inflate(R.layout.path_item, parent, false);

        TextView textPath = (TextView) browserItem.findViewById(R.id.browserPath);
        final Uri uri = uris.get(position);

        textPath.setText(uri.getPath());

        final View deleteView = browserItem.findViewById(R.id.delete);
        if (deleteView != null) {
            deleteView.setVisibility(View.VISIBLE);
            // Drawn in the colour the path beside it is, so the mark is as legible as the row
            // it belongs to whatever the dialog is set in.
            int rowColor = textPath.getCurrentTextColor();
            View mark = browserItem.findViewById(R.id.image1);
            if (mark instanceof ImageView) {
                TintUtil.setTintImageNoAlpha((ImageView) mark, rowColor);
            }
            TintUtil.setRingColor(deleteView, rowColor);
            if (deleteView instanceof ImageView) {
                TintUtil.setTintImageNoAlpha((ImageView) deleteView, rowColor);
            }
            deleteView.setOnClickListener(new OnClickListener() {

                @Override
                public void onClick(View v) {
                    if (onDeleClick != null) {
                        onDeleClick.onResultRecive(uri);
                    }
                }
            });
        }

        return browserItem;
    }

    public ResultResponse<Uri> getOnDeleClick() {
        return onDeleClick;
    }

    public void setOnDeleClick(ResultResponse<Uri> onDeleClick) {
        this.onDeleClick = onDeleClick;
    }

}
