package com.foobnix.pdf.info.presentation;

import android.net.Uri;
import android.provider.DocumentsContract;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;
import android.widget.ImageView;
import com.foobnix.pdf.info.TintUtil;

import com.foobnix.android.utils.LOG;
import com.foobnix.android.utils.ResultResponse;
import com.foobnix.pdf.info.R;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class PathAdapter extends BaseAdapter {

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

    public void setPaths(List<String> paths) {
        List<String> copy = new ArrayList<>(paths);
        Collections.sort(copy, comparator);
        this.paths = copy;
        notifyDataSetChanged();
    }

    private static String displayNameFor(String path) {
        if (path == null) return "";
        if (path.startsWith("content://")) {
            try {
                return DocumentsContract.getTreeDocumentId(Uri.parse(path));
            } catch (Exception e) {
                LOG.e(e);
                return path;
            }
        }
        return path;
    }

    private static final Comparator<String> comparator = new Comparator<String>() {
        @Override
        public int compare(String lhs, String rhs) {
            return displayNameFor(lhs).compareTo(displayNameFor(rhs));
        }
    };

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View browserItem = LayoutInflater.from(parent.getContext()).inflate(R.layout.path_item, parent, false);

        TextView textPath = (TextView) browserItem.findViewById(R.id.browserPath);
        final String path = paths.get(position);

        textPath.setText(displayNameFor(path));

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
