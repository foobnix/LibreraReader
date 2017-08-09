package com.foobnix.ui2.adapter;

import org.jsoup.Jsoup;
import org.jsoup.safety.Whitelist;

import com.foobnix.android.utils.Dips;
import com.foobnix.android.utils.ResultResponse;
import com.foobnix.android.utils.TxtUtils;
import com.foobnix.opds.Entry;
import com.foobnix.opds.Link;
import com.foobnix.pdf.info.IMG;
import com.foobnix.pdf.info.R;
import com.foobnix.pdf.info.TintUtil;
import com.foobnix.ui2.AppRecycleAdapter;
import com.nostra13.universalimageloader.core.ImageLoader;

import android.support.v7.widget.RecyclerView;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

public class EntryAdapter extends AppRecycleAdapter<Entry, RecyclerView.ViewHolder> {

    private static final int PD = Dips.dpToPx(2);

    public class EntryViewHolder extends RecyclerView.ViewHolder {
        public TextView title, content, author, category;
        public View parent;
        public ImageView image;
        public LinearLayout links;

        public EntryViewHolder(View view) {
            super(view);
            title = (TextView) view.findViewById(R.id.title);
            author = (TextView) view.findViewById(R.id.author);
            category = (TextView) view.findViewById(R.id.category);
            content = (TextView) view.findViewById(R.id.content);
            links = (LinearLayout) view.findViewById(R.id.links);
            image = (ImageView) view.findViewById(R.id.image);

            parent = view;
        }
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View itemView;
        itemView = LayoutInflater.from(parent.getContext()).inflate(R.layout.browse_entry, parent, false);
        return new EntryViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(final RecyclerView.ViewHolder holderAll, final int position) {
        final Entry entry = getItem(position);
        final EntryViewHolder holder = (EntryViewHolder) holderAll;
        holder.title.setText("" + entry.title);
        if (entry.content != null) {
            holder.content.setVisibility(View.VISIBLE);
            String text = Jsoup.clean(entry.content, Whitelist.simpleText());
            holder.content.setText("(" + Html.fromHtml(text) + ")");
        } else {
            holder.content.setVisibility(View.GONE);
        }

        if (entry.author != null) {
            holder.author.setText(entry.author);
            holder.author.setVisibility(View.VISIBLE);
        } else {
            holder.author.setVisibility(View.GONE);
        }

        if (TxtUtils.isNotEmpty(entry.category)) {
            holder.category.setText(entry.category);
            holder.category.setVisibility(View.VISIBLE);
        } else {
            holder.category.setVisibility(View.GONE);
        }

        holder.author.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {

            }
        });
        holder.category.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {

            }
        });

        holder.links.removeAllViews();

        TintUtil.setTintImage(holder.image);
        holder.image.setVisibility(entry.links.size() <= 2 ? View.VISIBLE : View.GONE);

        String imgLink = "";
        for (final Link link : entry.links) {
            if (link.isImageLink()) {
                if (!link.href.equals(imgLink)) {
                    ImageView img = new ImageView(holder.parent.getContext());
                    img.setPadding(PD, PD, PD, PD);
                    ImageLoader.getInstance().displayImage(link.href, img, IMG.displayImageOptions);
                    holder.links.addView(img);
                    imgLink = link.href;
                }
            } else if (!link.isOpdsLink()) {
                TextView t = new TextView(holder.parent.getContext());
                t.setTextAppearance(t.getContext(), R.style.textLinkStyle);
                t.setPadding(PD, PD, PD, PD);
                if (link.title != null) {
                    t.setText(link.title);
                } else {
                    t.setText(link.type);
                }
                t.setOnClickListener(new OnClickListener() {

                    @Override
                    public void onClick(View v) {
                        if (onLinkClickListener != null) {
                            onLinkClickListener.onResultRecive(link);
                        }

                    }
                });
                holder.links.addView(t);
            }
        }

        bindItemClickAndLongClickListeners(holder.parent, entry);
    }

    @Override
    public int getItemViewType(int position) {
        return 0;
    }

    public void setOnLinkClickListener(ResultResponse<Link> onLinkClickListener) {
        this.onLinkClickListener = onLinkClickListener;
    }

    private ResultResponse<Link> onLinkClickListener;

}