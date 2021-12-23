package com.foobnix.ui2.adapter;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import com.foobnix.android.utils.ResultResponse;
import com.foobnix.android.utils.TxtUtils;
import com.foobnix.dao2.FileMeta;
import com.foobnix.model.AppBookmark;
import com.foobnix.model.AppState;
import com.foobnix.model.MyPath;
import com.foobnix.pdf.info.Clouds;
import com.foobnix.pdf.info.ExtUtils;
import com.foobnix.pdf.info.IMG;
import com.foobnix.pdf.info.R;
import com.foobnix.pdf.info.TintUtil;
import com.foobnix.ui2.AppDB;
import com.foobnix.ui2.AppRecycleAdapter;
import com.foobnix.ui2.adapter.BookmarksAdapter2.BookmarksViewHolder;

public class BookmarksAdapter2 extends AppRecycleAdapter<AppBookmark, BookmarksViewHolder> {


    public boolean withTitle = true;
    public boolean withPageNumber = true;
    private ResultResponse<AppBookmark> onDeleteClickListener;

    @Override
    public BookmarksViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext()).inflate(R.layout.bookmark_item, parent, false);
        return new BookmarksViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(final BookmarksViewHolder holder, final int position) {
        final AppBookmark item = getItem(position);

        holder.page.setText(TxtUtils.percentFormatInt(item.getPercent()));
        FileMeta m = AppDB.get().load(MyPath.toAbsolute(item.path));

        if (m != null && m.getPages() != null && m.getPages() > 0) {
            holder.page.setText("" + Math.round(item.getPercent() * m.getPages()));
        }


        holder.title.setText(ExtUtils.getFileName(item.getPath()));


        holder.text.setText(item.text);
        holder.remove.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                onDeleteClickListener.onResultRecive(item);
            }
        });
        holder.remove.setImageResource(withPageNumber ? R.drawable.glyphicons_208_remove_2 : R.drawable.glyphicons_basic_578_share);
        TintUtil.setTintImageNoAlpha(holder.remove, holder.remove.getResources().getColor(R.color.lt_grey_dima));

        if (withTitle) {
            //holder.title.setVisibility(View.VISIBLE);
            //holder.title.setVisibility(View.GONE);
        } else {
            holder.title.setVisibility(View.GONE);
        }

        TintUtil.setTintBgSimple(holder.page, 240);
        holder.page.setTextColor(Color.WHITE);
        if (withPageNumber) {
            holder.page.setVisibility(View.VISIBLE);
            // holder.remove.setVisibility(View.VISIBLE);
        } else {
            holder.page.setVisibility(View.GONE);
            //holder.remove.setVisibility(View.GONE);
        }

        IMG.getCoverPageWithEffectPos(holder.image, item.getPath(), IMG.getImageSize(), position);

        Clouds.showHideCloudImage(holder.cloudImage, item.getPath());


        if (!AppState.get().isBorderAndShadow) {
            holder.parent.setBackgroundColor(Color.TRANSPARENT);
        }

        bindItemClickAndLongClickListeners(holder.parent, getItem(position));

        if (AppState.get().appTheme == AppState.THEME_DARK_OLED) {
            holder.parent.setBackgroundColor(Color.BLACK);
        }
    }

    public void setOnDeleteClickListener(ResultResponse<AppBookmark> onDeleteClickListener) {
        this.onDeleteClickListener = onDeleteClickListener;
    }

    public class BookmarksViewHolder extends RecyclerView.ViewHolder {
        public TextView page, text, title;
        public ImageView remove;
        public CardView parent;
        public ImageView image, cloudImage;

        public BookmarksViewHolder(View view) {
            super(view);
            page = (TextView) view.findViewById(R.id.page);
            title = (TextView) view.findViewById(R.id.title);
            text = (TextView) view.findViewById(R.id.text);
            image = (ImageView) view.findViewById(R.id.image);
            cloudImage = (ImageView) view.findViewById(R.id.cloudImage);
            remove = view.findViewById(R.id.remove);
            parent = (CardView) view;
        }
    }

}