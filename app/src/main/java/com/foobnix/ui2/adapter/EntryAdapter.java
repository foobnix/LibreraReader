package com.foobnix.ui2.adapter;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.text.Html;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.StaggeredGridLayoutManager.LayoutParams;

import com.bumptech.glide.Glide;
import com.foobnix.android.utils.Dips;
import com.foobnix.android.utils.Keyboards;
import com.foobnix.android.utils.LOG;
import com.foobnix.android.utils.ResultResponse;
import com.foobnix.android.utils.TxtUtils;
import com.foobnix.model.AppState;
import com.foobnix.opds.Entry;
import com.foobnix.opds.Link;
import com.foobnix.pdf.info.R;
import com.foobnix.pdf.info.TintUtil;
import com.foobnix.pdf.info.Urls;
import com.foobnix.pdf.info.view.EditTextHelper;
import com.foobnix.pdf.info.view.ScaledImageView;
import com.foobnix.pdf.info.widget.FileInformationDialog;
import com.foobnix.ui2.AppRecycleAdapter;

public class EntryAdapter extends AppRecycleAdapter<Entry, RecyclerView.ViewHolder> {

    private static final int PD = Dips.dpToPx(4);
    private ResultResponse<Link> onLinkClickListener;
    private ResultResponse<Entry> onRemoveLinkClickListener;

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
        final Context context = holder.parent.getContext();

        if (TxtUtils.isNotEmpty(entry.title)) {
            String trim = entry.title.trim();
            if (TxtUtils.isNotEmpty(entry.year)) {
                trim += " (" + entry.year + ")";
            }
            holder.title.setText("" + trim);
            holder.title.setVisibility(View.VISIBLE);
        } else {
            holder.title.setVisibility(View.GONE);
        }

        String body = entry.content + entry.summary;
        if (TxtUtils.isNotEmpty(body)) {
            holder.content.setVisibility(View.VISIBLE);
            String text = TxtUtils.replaceLast(body, "\n", "");
            holder.content.setText(Html.fromHtml(text));

            if (body.length() >= 200) {
                holder.expand.setVisibility(View.VISIBLE);
                holder.expand.setOnClickListener(new OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        holder.expand.setVisibility(View.GONE);
                        holder.content.setMaxLines(100);
                    }
                });
            } else {
                holder.expand.setVisibility(View.GONE);
            }


        } else {
            holder.content.setVisibility(View.GONE);
            holder.expand.setVisibility(View.GONE);
        }

        if (entry.appState != null) {
            holder.remove.setVisibility(View.VISIBLE);
            holder.remove.setOnClickListener(new OnClickListener() {

                @Override
                public void onClick(View v) {
                    onRemoveLinkClickListener.onResultRecive(entry);
                }
            });
            //if (SamlibOPDS.isSamlibUrl(entry.homeUrl)) {
            //    holder.remove.setVisibility(View.GONE);
            // }
        } else {

            holder.remove.setVisibility(View.GONE);
        }

        if (TxtUtils.isNotEmpty(entry.author)) {
            holder.author.setText(entry.author);
            holder.author.setVisibility(View.VISIBLE);
            if (TxtUtils.isNotEmpty(entry.authorUrl)) {
                holder.author.setTextColor(ContextCompat.getColor(context, R.color.tint_blue));
                if (AppState.get().isUiTextColor) {
                    TintUtil.setUITextColor(holder.author, AppState.get().uiTextColor);
                }
                holder.author.setOnClickListener(new OnClickListener() {

                    @Override
                    public void onClick(View v) {
                        onLinkClickListener.onResultRecive(new Link(entry.authorUrl));
                    }
                });
            } else {
            }

        } else {
            holder.author.setVisibility(View.GONE);
        }

        if (TxtUtils.isNotEmpty(entry.category)) {
            holder.category.setText(entry.category);
            holder.category.setVisibility(View.VISIBLE);
        } else {
            holder.category.setVisibility(View.GONE);
        }

        holder.category.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {

            }
        });

        holder.links.removeAllViews();
        holder.downloadLinks.removeAllViews();

        holder.image.setVisibility(View.GONE);

        String imgLink = "";
        for (final Link link : entry.links) {
            link.author = entry.author;
            if (link.TYPE_LOGO.equals(link.type) || link.isThumbnail()) {
                holder.image.setVisibility(View.VISIBLE);
                //ImageLoader.getInstance().displayImage(link.href, holder.image, IMG.displayOPDSOptions);
                Glide.with(context).load(link.href).into(holder.image);

            } else if (link.isSearchLink()) {
                LinearLayout l = new LinearLayout(context);
                l.setOrientation(LinearLayout.HORIZONTAL);
                l.setGravity(Gravity.CENTER_VERTICAL);

                final EditText search = new EditText(context);
                search.setTag(true);
                search.setSingleLine();
                search.setMinimumWidth(Dips.dpToPx(400));
                search.setHint(R.string.search);

                final ImageView button = new ImageView(context);
                button.setMinimumWidth(Dips.dpToPx(42));
                button.setImageResource(R.drawable.glyphicons_28_search);
                TintUtil.setTintImageWithAlpha(button);

                l.addView(search, new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT, 1.0f));
                l.addView(button, new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT, 0.0f));

                button.setOnClickListener(new OnClickListener() {

                    @Override
                    public void onClick(View v) {
                        if (TxtUtils.isNotEmpty(search.toString())) {
                            String encode = Urls.encode(search.getText().toString());
                            String replace = link.href.replace("{searchterms}", encode).replace("{searchTerms}", encode);
                            Link l = new Link(replace);
                            onLinkClickListener.onResultRecive(l);

                            Keyboards.close(search);
                        } else {
                            Toast.makeText(context, R.string.incorrect_value, Toast.LENGTH_SHORT).show();
                        }
                    }
                });

                EditTextHelper.enableKeyboardSearch(search, new Runnable() {

                    @Override
                    public void run() {
                        button.performClick();
                    }
                });

                holder.links.addView(l, new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));

            } else if (link.isImageLink()) {
                if (TxtUtils.isNotEmpty(imgLink)) {
                    if (link.rel.contains("thumbnail")) {
                        continue;
                    }
                }
                if (!link.href.equals(imgLink) && AppState.get().opdsLargeCovers) {
                    ScaledImageView img = new ScaledImageView(holder.parent.getContext());
                    img.setPadding(PD, PD, PD, PD);
                    //ImageLoader.getInstance().displayImage(link.href, img, IMG.displayCacheMemoryDisc);
                    Glide.with(context).load(link.href).into(img);

                    holder.links.addView(img, new LinearLayout.LayoutParams(Dips.screenWidth() / 2, LayoutParams.WRAP_CONTENT));
                    imgLink = link.href;

                    img.setOnClickListener(new OnClickListener() {

                        @Override
                        public void onClick(View v) {
                            FileInformationDialog.showImageHttpPath(context, link.href);

                        }
                    });
                }

            } else if (link.type == null || link.type.equals(Link.APPLICATION_ATOM_XML) || (link.type.contains(";profile") && link.title == null)) {
                continue;
            } else if (link.isWebLink() && TxtUtils.isEmpty(link.title)) {
                continue;
            } else if (link.isDisabled()) {
                continue;
            } else {
                TextView t = new TextView(holder.parent.getContext());
                t.setPadding(PD, PD, PD, PD);

                t.setOnClickListener(new OnClickListener() {

                    @Override
                    public void onClick(View v) {
                        onLinkClickListener.onResultRecive(link);

                    }
                });
                t.setOnLongClickListener(new OnLongClickListener() {

                    @Override
                    public boolean onLongClick(View v) {
                        holder.parent.performLongClick();
                        return false;
                    }
                });

                String downloadFormat = link.getDownloadDisplayFormat();
                LOG.d("downloadFormat", downloadFormat, link.isOpdsLink());

                if (downloadFormat != null && !link.isOpdsLink()) {
                    t.setText(link.title != null ? link.title : downloadFormat.replace(".zip", ""));
                    t.setGravity(Gravity.CENTER);
                    t.setBackgroundResource(R.drawable.bg_border_blue_entry);
                    t.setMinimumWidth(Dips.dpToPx(40));

                    if (link.filePath != null) {

                        Drawable d = ContextCompat.getDrawable(context, R.drawable.glyphicons_2_book_open2);
                        TintUtil.setDrawableTint(d, t.getCurrentTextColor());

                        t.setCompoundDrawablePadding(Dips.dpToPx(6));
                        t.setCompoundDrawablesWithIntrinsicBounds(d, null, null, null);

                        t.setActivated(true);
                    }

                    android.widget.LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
                    lp.setMargins(0, 0, Dips.dpToPx(4), 0);

                    holder.downloadLinks.addView(t, lp);
                } else {
                    String title = link.title != null ? link.title : link.isOpdsLink() ? "" : link.type;
                    if (TxtUtils.isNotEmpty(title)) {
                        t.setText(title);
                    } else {
                        t.setVisibility(View.GONE);
                    }
                    t.setTextColor(context.getResources().getColor(R.color.tint_blue));

                    if (AppState.get().isUiTextColor) {
                        TintUtil.setUITextColor(t, AppState.get().uiTextColor);
                    }

                    // t.setBackgroundResource(R.drawable.bg_clickable);
                    // if (link.type != null && link.type.contains(Entry.MY_CATALOG)) {
                    // t.setBackgroundColor(Color.TRANSPARENT);
                    // }


                    holder.links.addView(t);
                }

            }
        }

        bindItemClickAndLongClickListeners(holder.parent, entry);

        if (AppState.get().appTheme == AppState.THEME_DARK_OLED) {
            holder.parent.setBackgroundColor(Color.BLACK);
        }


        TxtUtils.setInkTextView(holder.title, holder.content, holder.author, holder.category, holder.expand);
    }

    @Override
    public int getItemViewType(int position) {
        return 0;
    }

    public void setOnLinkClickListener(ResultResponse<Link> onLinkClickListener) {
        this.onLinkClickListener = onLinkClickListener;
    }

    public void setOnRemoveLinkClickListener(ResultResponse<Entry> onLinkClickListener) {
        this.onRemoveLinkClickListener = onLinkClickListener;
    }

    public class EntryViewHolder extends RecyclerView.ViewHolder {
        public TextView title, content, author, category, expand;
        public View parent;
        public ImageView image, remove;
        public LinearLayout links, downloadLinks;

        public EntryViewHolder(View view) {
            super(view);
            title = (TextView) view.findViewById(R.id.title);
            author = (TextView) view.findViewById(R.id.author);
            content = (TextView) view.findViewById(R.id.content);
            category = (TextView) view.findViewById(R.id.category);
            expand = (TextView) view.findViewById(R.id.expand);
            links = (LinearLayout) view.findViewById(R.id.links);
            downloadLinks = (LinearLayout) view.findViewById(R.id.downloadLinks);
            image = (ImageView) view.findViewById(R.id.image);
            remove = (ImageView) view.findViewById(R.id.remove);

            parent = view;
        }
    }

}