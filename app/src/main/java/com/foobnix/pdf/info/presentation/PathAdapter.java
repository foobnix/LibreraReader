package com.foobnix.pdf.info.presentation;

import android.content.Context;
import com.foobnix.pdf.info.SafPathLabels;
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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class PathAdapter extends BaseAdapter {

    private final Context context;

    public PathAdapter(Context context) {
        this.context = context;
    }

    private List<String> paths = Collections.emptyList();
    private ResultResponse<String> onDeleClick;

    @Override
    public int getCount() {
        return paths.size();
    }

    @Override
    public Object getItem(int position) {
        return null;
    }

    @Override
    public long getItemId(int position) {
        return 0;
    }

    private Object sorting;

    public void setPaths(List<String> paths) {
        Object request = new Object();
        sorting = request;
        List<String> copy = new ArrayList<>(paths);
        java.util.Map<String, String> labels = new java.util.HashMap<>();
        for (String path : copy) labels.put(path, SafPathLabels.displayName(context, path));
        Comparator<String> comparator = (left, right) -> {
            int order = labels.get(left).compareToIgnoreCase(labels.get(right));
            return order != 0 ? order : left.compareTo(right);
        };
        copy.sort(comparator);
        this.paths = copy;
        notifyDataSetChanged();
        for (String path : copy) SafPathLabels.refresh(context, path, label -> {
            if (sorting != request) return;
            labels.put(path, label);
            // Each comparison uses one snapshot; async cache fills cannot change it mid-sort.
            List<String> reordered = new ArrayList<>(this.paths);
            reordered.sort(comparator);
            this.paths = reordered;
            notifyDataSetChanged();
        });
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View browserItem = LayoutInflater.from(parent.getContext()).inflate(R.layout.path_item, parent, false);

        TextView textPath = (TextView) browserItem.findViewById(R.id.browserPath);
        final String path = paths.get(position);

        SafPathLabels.bind(textPath, path);

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
            if (deleteView instanceof ImageView) {
                TintUtil.setTintImageNoAlpha((ImageView) deleteView, rowColor);
            }
            deleteView.setOnClickListener(new OnClickListener() {

                @Override
                public void onClick(View v) {
                    if (onDeleClick != null) {
                        onDeleClick.onResultRecive(path);
                    }
                }
            });
        }

        return browserItem;
    }

    public ResultResponse<String> getOnDeleClick() {
        return onDeleClick;
    }

    public void setOnDeleClick(ResultResponse<String> onDeleClick) {
        this.onDeleClick = onDeleClick;
    }

}
