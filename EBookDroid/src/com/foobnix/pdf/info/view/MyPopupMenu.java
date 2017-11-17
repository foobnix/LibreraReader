package com.foobnix.pdf.info.view;

import java.util.ArrayList;
import java.util.List;

import com.foobnix.android.utils.BaseItemLayoutAdapter;
import com.foobnix.android.utils.Dips;
import com.foobnix.android.utils.LOG;
import com.foobnix.pdf.info.R;
import com.foobnix.pdf.info.TintUtil;

import android.content.Context;
import android.view.MenuItem.OnMenuItemClickListener;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageView;
import android.widget.ListPopupWindow;
import android.widget.PopupWindow.OnDismissListener;
import android.widget.TextView;

public class MyPopupMenu {
    Context c;
    View anchor;
    List<Menu> list = new ArrayList<Menu>();

    public MyPopupMenu(Context c, View anchor) {
        this.c = c;
        this.anchor = anchor;
    }

    public class Menu {
        String stringRes;
        int iconRes;
        OnMenuItemClickListener click;

        public Menu add(int res) {
            this.stringRes = c.getString(res);
            return this;
        }

        public Menu add(String name) {
            this.stringRes = name;
            return this;
        }

        public Menu setIcon(int res) {
            this.iconRes = res;
            return this;
        }

        public Menu setOnMenuItemClickListener(OnMenuItemClickListener onclick) {
            this.click = onclick;
            return this;
        }

    }

    public void show() {

        final ListPopupWindow p1 = new ListPopupWindow(c);
        p1.setModal(true);
        p1.setOnDismissListener(new OnDismissListener() {

            @Override
            public void onDismiss() {
                p1.dismiss();

            }
        });

        BaseItemLayoutAdapter<Menu> a = new BaseItemLayoutAdapter<Menu>(c, R.layout.item_dict_line, list) {
            @Override
            public void populateView(View layout, int position, final Menu item) {
                ((TextView) layout.findViewById(R.id.text1)).setText(item.stringRes);
                ImageView imageView = (ImageView) layout.findViewById(R.id.image1);
                if (item.iconRes != 0) {
                    imageView.setImageResource(item.iconRes);
                    TintUtil.setTintImage(imageView);
                } else {
                    imageView.setVisibility(View.GONE);
                }
                layout.setOnClickListener(new OnClickListener() {

                    @Override
                    public void onClick(View v) {
                        item.click.onMenuItemClick(null);
                        try {
                            p1.dismiss();
                        } catch (Exception e) {
                            LOG.e(e);
                        }
                    }
                });
            }
        };

        int size = 0;
        for (Menu m : list) {
            TextView t = new TextView(c);
            t.setText(m.stringRes);
            t.setTextSize(14);
            t.measure(0, 0);
            if (t.getMeasuredWidth() > size) {
                size = t.getMeasuredWidth();
            }
            LOG.d("getMeasuredWidth", m.stringRes, size);
        }

        p1.setAnchorView(anchor);
        p1.setAdapter(a);
        p1.setWidth(size + Dips.dpToPx(80));
        p1.show();

    }

    public Menu getMenu() {
        Menu m = new Menu();
        list.add(m);
        return m;
    }

}
