package com.foobnix.pdf.info.presentation;

import java.io.File;
import java.util.List;

import com.foobnix.android.utils.ResultResponse;
import com.foobnix.android.utils.TxtUtils;
import com.foobnix.dao2.FileMeta;
import com.foobnix.pdf.info.IMG;
import com.foobnix.pdf.info.R;
import com.foobnix.pdf.info.TintUtil;
import com.foobnix.pdf.info.wrapper.AppState;
import com.nostra13.universalimageloader.core.assist.FailReason;
import com.nostra13.universalimageloader.core.listener.ImageLoadingListener;

import android.app.Activity;
import android.graphics.Bitmap;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ImageView.ScaleType;
import android.widget.TextView;

public class SearchAdapter extends BaseAdapter {
    private final List<FileMeta> items;
    private Activity c;
    private ResultResponse<File> onMenuPressed;

    int adsPosition = 0;

    public SearchAdapter(final Activity c, List<FileMeta> items) {
        this.c = c;
        this.items = items;
    }

    @Override
    public int getCount() {
        return items.size();
    }

    @Override
    public FileMeta getItem(int i) {
        return items.get(i);
    }

    @Override
    public long getItemId(int i) {
        return i;
    }

    static class Holder {
        public ImageView imageView;
        public ImageView starIcon;
        public TextView title1;
        public TextView title2;
        public TextView textPath;
        public TextView textSize;
        public TextView textDate;
        public TextView textExt;
        public int libMode;
        public View layoutBootom;
    }

    @Override
    public View getView(final int i, View convertView, ViewGroup viewGroup) {
        View browserItem = convertView;

        if (browserItem != null && ((Holder) browserItem.getTag()).libMode != AppState.get().libraryMode) {
            browserItem = null;
        }

        if (browserItem == null) {
            if (AppState.get().libraryMode == AppState.MODE_GRID || AppState.get().libraryMode == AppState.MODE_COVERS) {
                browserItem = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.browse_item_grid, viewGroup, false);
            } else {
                browserItem = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.browse_item_list, viewGroup, false);
            }

            Holder holder = new Holder();

            holder.imageView = (ImageView) browserItem.findViewById(R.id.browserItemIcon);
            holder.starIcon = (ImageView) browserItem.findViewById(R.id.starIcon);

            holder.title1 = (TextView) browserItem.findViewById(R.id.title1);
            holder.title2 = (TextView) browserItem.findViewById(R.id.title2);

            holder.textPath = (TextView) browserItem.findViewById(R.id.browserPath);
            holder.textSize = (TextView) browserItem.findViewById(R.id.browserSize);
            holder.textDate = (TextView) browserItem.findViewById(R.id.browseDate);
            holder.textExt = (TextView) browserItem.findViewById(R.id.browserExt);
            holder.layoutBootom = browserItem.findViewById(R.id.layoutBootom);
            holder.libMode = AppState.get().libraryMode;

            View progresLayout = browserItem.findViewById(R.id.progresLayout);
            if (progresLayout != null) {
                progresLayout.setVisibility(View.GONE);
            }
            View delete = browserItem.findViewById(R.id.delete);
            if (delete != null) {
                delete.setVisibility(View.GONE);
            }

            browserItem.setTag(holder);
        }
        final Holder holder = (Holder) browserItem.getTag();

        final FileMeta fileMeta = getItem(i);

        ImageView menuIcon = (ImageView) browserItem.findViewById(R.id.itemMenu);
        menuIcon.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                if (onMenuPressed != null) {
                    onMenuPressed.onResultRecive(new File(fileMeta.getPath()));
                }
            }
        });

        if (AppState.get().isCropBookCovers) {
            holder.imageView.setScaleType(ScaleType.CENTER_CROP);
        } else {
            holder.imageView.setScaleType(ScaleType.FIT_CENTER);
        }

        holder.title1.setText(fileMeta.getTitle());
        holder.title2.setText(fileMeta.getAuthor());
        holder.textPath.setText(fileMeta.getPath());
        holder.textSize.setText("" + fileMeta.getSize());
        holder.textDate.setText("" + fileMeta.getDate());
        holder.textExt.setText(fileMeta.getExt());

        if (AppState.get().libraryMode == AppState.MODE_GRID || AppState.get().libraryMode == AppState.MODE_COVERS) {
            holder.textPath.setVisibility(View.GONE);
            holder.textSize.setVisibility(View.GONE);

            IMG.updateImageSizeBig(holder.imageView);
            IMG.updateImageSizeBig((View) holder.imageView.getParent());

            if (AppState.get().libraryMode == AppState.MODE_COVERS) {
                holder.layoutBootom.setVisibility(View.GONE);

            }

        } else {
            holder.textPath.setVisibility(View.VISIBLE);

            IMG.updateImageSizeSmall(holder.imageView);
            IMG.updateImageSizeSmall((View) holder.imageView.getParent());
        }

        if (AppState.get().libraryMode == AppState.MODE_LIST && AppState.get().coverSmallSize >= IMG.TWO_LINE_COVER_SIZE) {
            holder.title1.setSingleLine(false);
            holder.title1.setLines(2);
        } else {
            holder.title1.setSingleLine(true);
            holder.title1.setLines(1);
        }

        if (AppState.get().libraryMode == AppState.MODE_GRID && AppState.get().coverBigSize <= IMG.TWO_LINE_COVER_SIZE) {
            holder.textDate.setVisibility(View.GONE);
        } else {
            holder.textDate.setVisibility(View.VISIBLE);
        }

        TintUtil.setTintImageWithAlpha(menuIcon);

        // int size = AppState.get().libraryMode == AppState.MODE_LIST ?
        // AppState.get().coverSmallSize : AppState.get().coverBigSize;

        // StarsWrapper.addStars(holder.starIcon, info);
        IMG.getCoverPageWithEffect(holder.imageView, fileMeta.getPath(), IMG.getImageSize(), new ImageLoadingListener() {

            @Override
            public void onLoadingStarted(String arg0, View arg1) {
                // TODO Auto-generated method stub

            }

            @Override
            public void onLoadingFailed(String arg0, View arg1, FailReason arg2) {
                // TODO Auto-generated method stub

            }

            @Override
            public void onLoadingComplete(String arg0, View arg1, Bitmap arg2) {
                FileMeta fileMeta = getItem(i);

                holder.title1.setText(fileMeta.getTitle());
                holder.title2.setText(fileMeta.getAuthor());
                holder.textPath.setText(fileMeta.getPath());
                holder.textSize.setText("" + fileMeta.getSize());
                holder.textDate.setText("" + fileMeta.getDate());
                holder.textExt.setText(fileMeta.getExt());

                // StarsWrapper.addStars(holder.starIcon, info);

                if (TxtUtils.isEmpty(fileMeta.getAuthor()) && AppState.get().libraryMode == AppState.MODE_LIST) {
                    holder.title2.setVisibility(View.GONE);
                } else {
                    holder.title2.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void onLoadingCancelled(String arg0, View arg1) {
                // TODO Auto-generated method stub

            }
        });

        return browserItem;
    }

    public void setOnMenuPressed(ResultResponse<File> onMenuPressed) {
        this.onMenuPressed = onMenuPressed;
    }
}
