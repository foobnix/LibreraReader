package com.foobnix.pdf.info.presentation;

import java.util.List;

import com.foobnix.android.utils.Dips;
import com.foobnix.android.utils.TxtUtils;
import com.foobnix.pdf.info.R;
import com.foobnix.pdf.info.TintUtil;
import com.foobnix.pdf.info.model.OutlineLinkWrapper;

import android.content.Context;
import android.graphics.Color;
import android.util.SparseIntArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

public class OutlineAdapter extends BaseAdapter {

    private final ItemListener itemListener = new ItemListener();
    private final CollapseListener collapseListener = new CollapseListener();

    private final Context context;
    private final OutlineLinkWrapper[] objects;
    private final OutlineItemState[] states;
    private final SparseIntArray mapping = new SparseIntArray();
    private int currentId;
    private int pages;

    public OutlineAdapter(final Context context, final List<OutlineLinkWrapper> objects, final OutlineLinkWrapper current, int pages) {
        this.context = context;
        this.pages = pages;
        this.objects = objects.toArray(new OutlineLinkWrapper[objects.size()]);
        this.states = new OutlineItemState[this.objects.length];

        boolean treeFound = false;
        for (int i = 0; i < this.objects.length; i++) {
            mapping.put(i, i);
            final int next = i + 1;
            if (next < this.objects.length && this.objects[i].level < this.objects[next].level) {
                states[i] = OutlineItemState.COLLAPSED;
                treeFound = true;
            } else {
                states[i] = OutlineItemState.LEAF;
            }
        }

        currentId = current != null ? objects.indexOf(current) : 1;

        if (treeFound) {
            for (int parent = getParentId(currentId); parent != -1; parent = getParentId(parent)) {
                states[parent] = OutlineItemState.EXPANDED;
            }
            rebuild();
            if (getCount() == 1 && states[0] == OutlineItemState.COLLAPSED) {
                states[0] = OutlineItemState.EXPANDED;
                rebuild();
            }
        }

    }

    public int getParentId(final int id) {
        final int level = objects[id].level;
        for (int i = id - 1; i >= 0; i--) {
            if (objects[i].level < level) {
                return i;
            }
        }
        return -1;
    }

    protected void rebuild() {
        mapping.clear();
        int pos = 0;
        int level = Integer.MAX_VALUE;
        for (int cid = 0; cid < objects.length; cid++) {
            if (objects[cid].level <= level) {
                mapping.put(pos++, cid);
                if (states[cid] == OutlineItemState.COLLAPSED) {
                    level = objects[cid].level;
                } else {
                    level = Integer.MAX_VALUE;
                }
            }
        }
    }

    @Override
    public boolean hasStableIds() {
        return false;
    }

    @Override
    public int getCount() {
        return mapping.size();
    }

    @Override
    public OutlineLinkWrapper getItem(final int position) {
        final int id = mapping.get(position, -1);
        return id >= 0 && id < objects.length ? objects[id] : null;
    }

    @Override
    public long getItemId(final int position) {
        return mapping.get(position, -1);
    }

    public int getItemPosition(final OutlineLinkWrapper item) {
        for (int i = 0, n = getCount(); i < n; i++) {
            if (item == getItem(i)) {
                return i;
            }
        }
        return -1;
    }

    public int getItemId(final OutlineLinkWrapper item) {
        for (int i = 0, n = objects.length; i < n; i++) {
            if (item == objects[i]) {
                return i;
            }
        }
        return -1;
    }

    @Override
    public View getView(final int position, final View convertView, final ViewGroup parent) {
        final int id = (int) getItemId(position);
        View container = null;
        if (convertView == null) {
            container = LayoutInflater.from(context).inflate(R.layout.outline_item, parent, false);
        } else {
            container = convertView;
        }
        final TextView view = (TextView) container.findViewById(R.id.outline_title);
        final TextView num = (TextView) container.findViewById(R.id.pageNumber);
        final ImageView btn = (ImageView) container.findViewById(R.id.outline_collapse);

        final OutlineLinkWrapper item = getItem(position);
        view.setText(item.getTitleAsString().trim());
        num.setText(TxtUtils.deltaPage(item.targetPage));

        if (item.targetPage <= 0) {
            num.setVisibility(View.INVISIBLE);
        } else {
            num.setVisibility(View.VISIBLE);
        }

        if (currentId == id) {
            container.setBackgroundResource(R.color.tint_blue_alpha);
        } else {
            container.setBackgroundColor(Color.TRANSPARENT);
        }

        container.setTag(position);
        view.setTag(position);
        btn.setTag(position);

        ((LinearLayout.LayoutParams) btn.getLayoutParams()).leftMargin = item.level * Dips.dpToPx(20);

        container.setOnClickListener(itemListener);

        if (states[id] == OutlineItemState.LEAF) {
            // btn.setBackgroundColor(Color.TRANSPARENT);
            btn.setImageDrawable(null);
            btn.setOnClickListener(itemListener);
            btn.setBackgroundColor(Color.TRANSPARENT);
        } else {
            btn.setBackgroundResource(R.drawable.bg_clickable);
            btn.setOnClickListener(collapseListener);
            btn.setImageResource(states[id] == OutlineItemState.EXPANDED ? R.drawable.screen_zoom_out_dark : R.drawable.screen_zoom_in_dark);
            TintUtil.setTintImageWithAlpha(btn, view.getCurrentTextColor());

        }

        return container;
    }

    public int getCurrentId() {
        return currentId;
    }

    private static enum OutlineItemState {
        LEAF, EXPANDED, COLLAPSED;
    }

    private final class CollapseListener implements OnClickListener {

        @Override
        public void onClick(final View v) {
            {
                currentId = -1;
                final int position = ((Integer) v.getTag()).intValue();
                final int id = (int) getItemId(position);
                final OutlineItemState newState = states[id] == OutlineItemState.EXPANDED ? OutlineItemState.COLLAPSED : OutlineItemState.EXPANDED;
                states[id] = newState;
            }
            rebuild();

            v.post(new Runnable() {

                @Override
                public void run() {
                    notifyDataSetChanged();
                }
            });
        }
    }

    class ItemListener implements OnClickListener {

        @Override
        public void onClick(final View v) {
            for (ViewParent p = v.getParent(); p != null; p = p.getParent()) {
                if (p instanceof ListView) {
                    final ListView list = (ListView) p;
                    final OnItemClickListener l = list.getOnItemClickListener();
                    if (l != null) {
                        int position = ((Integer) v.getTag()).intValue();
                        l.onItemClick(list, v, position, 0);
                        currentId = (int) getItemId(position);
                        ((OutlineAdapter) list.getAdapter()).notifyDataSetChanged();
                    }
                    return;
                }
            }

        }
    }

}
