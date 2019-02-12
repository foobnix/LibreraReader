package com.foobnix.pdf.info.presentation;

import java.util.List;

import com.foobnix.android.utils.TxtUtils;
import com.foobnix.pdf.info.R;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

public class AuthorsAdapter extends BaseAdapter {

    private List<String> authors;
    private Context c;

    public AuthorsAdapter(Context c, List<String> authors) {
        this.c = c;
        this.authors = authors;
    }

    @Override
    public int getCount() {
        return authors.size();
    }

    @Override
    public Object getItem(int position) {
        return authors.get(position);
    }

    @Override
    public long getItemId(int position) {
        return 0;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        if (convertView == null) {
            convertView = LayoutInflater.from(c).inflate(R.layout.browse_author, parent, false);
        }
        TextView letter = (TextView) convertView.findViewById(R.id.image1);
        TextView text = (TextView) convertView.findViewById(R.id.text1);


        try {
            String name = authors.get(position);

            if (TxtUtils.isNotEmpty(name)) {
                letter.setText(String.valueOf(name.charAt(0)));
            } else {
                letter.setText("");
            }

            text.setText(name);
        } catch (Exception e) {
            text.setText("");
        }

        return convertView;
    }

    @Override
    public void notifyDataSetChanged() {
        super.notifyDataSetChanged();
    }

}
