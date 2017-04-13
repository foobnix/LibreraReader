package com.foobnix.android.utils;

import java.util.List;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

public abstract class BaseItemLayoutAdapter<T> extends BaseItemAdapter<T> {

    private Context context;
    private int layoutResId;

    public BaseItemLayoutAdapter(Context context, int layoutResId, List<T> items) {
        this.context = context;
        this.layoutResId = layoutResId;
        setItems(items);
    }

    public BaseItemLayoutAdapter(Context context, int layoutResId) {
        this.context = context;
        this.layoutResId = layoutResId;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent, T item) {
        if (convertView == null) {
            convertView = LayoutInflater.from(context).inflate(layoutResId, parent, false);
        }
        populateView(convertView, position, item);
        return convertView;
    }

    public abstract void populateView(View layout, int position, T item);

}
