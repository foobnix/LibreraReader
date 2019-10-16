package com.foobnix.pdf.info.view.drag;

import android.annotation.TargetApi;
import android.content.Context;
import android.os.Build;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ImageView.ScaleType;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import com.foobnix.android.utils.Dips;
import com.foobnix.android.utils.TxtUtils;
import com.foobnix.pdf.info.ExtUtils;
import com.foobnix.pdf.info.IMG;
import com.foobnix.pdf.info.R;
import com.foobnix.pdf.info.view.UnderlineImageView;

import java.util.Collections;
import java.util.List;

public class PlaylistAdapter extends RecyclerView.Adapter<PlaylistAdapter.ItemViewHolder> implements ItemTouchHelperAdapter {

    private final List<String> mItems;
    private static final int SIZE = Dips.isLargeOrXLargeScreen() ? Dips.DP_80 : Dips.DP_60;

    private final OnStartDragListener mDragStartListener;
    private boolean horizontal;

    private String currentPath;

    public PlaylistAdapter(Context context, List<String> mItems, OnStartDragListener dragStartListener, boolean horizontal) {
        mDragStartListener = dragStartListener;
        this.mItems = mItems;
        this.horizontal = horizontal;
    }

    @Override
    public ItemViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = null;
        if (horizontal) {
            view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_playlist_grid, parent, false);
        } else {
            view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_playlist_line, parent, false);

        }
        ItemViewHolder itemViewHolder = new ItemViewHolder(view);
        return itemViewHolder;
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    @Override
    public void onBindViewHolder(final ItemViewHolder holder, int position) {
        final String item = mItems.get(position);
        holder.textView.setText(ExtUtils.getFileNameWithoutExt(ExtUtils.getFileName(item)));
        holder.textView.setVisibility(TxtUtils.visibleIf(!horizontal));

        holder.parent.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                mDragStartListener.onItemClick(item);
            }
        });

        int size = SIZE;
        holder.imageView.getLayoutParams().width = size;
        holder.imageView.getLayoutParams().height = (int) (size * IMG.WIDTH_DK);

        holder.imageView.setAdjustViewBounds(true);
        holder.imageView.setScaleType(ScaleType.CENTER_CROP);

        if (Build.VERSION.SDK_INT >= 16) {
            holder.imageView.setCropToPadding(true);
        }

        holder.imageView.setLeftPadding(false);
        holder.imageView.setUnderlineValue(Dips.DP_3);
        holder.imageView.underline(item.equals(currentPath));

        IMG.getCoverPage(holder.imageView, item, SIZE);

        // Start a drag whenever the handle view it touched
        if (holder.imageDrag != null) {
            holder.imageDrag.setOnTouchListener(new View.OnTouchListener() {
                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    if (event.getAction() == MotionEvent.ACTION_DOWN) {
                        mDragStartListener.onStartDrag(holder);
                    }
                    return true;
                }
            });
            holder.imageDrag.setVisibility(TxtUtils.visibleIf(!horizontal));
        }
    }

    @Override
    public void onItemDismiss(int position) {
        mDragStartListener.onRevemove();
        mItems.remove(position);
        notifyItemRemoved(position);
    }

    @Override
    public boolean onItemMove(int fromPosition, int toPosition) {
        Collections.swap(mItems, fromPosition, toPosition);
        notifyItemMoved(fromPosition, toPosition);
        return true;
    }

    @Override
    public int getItemCount() {
        return mItems.size();
    }

    public List<String> getItems() {
        return mItems;
    }

    public String getCurrentPath() {
        return currentPath;
    }

    public void setCurrentPath(String currentPath) {
        this.currentPath = currentPath;
    }

    public static class ItemViewHolder extends RecyclerView.ViewHolder implements ItemTouchHelperViewHolder {

        public final TextView textView;
        public final ImageView imageDrag;
        public final UnderlineImageView imageView;

        public final View parent;

        public ItemViewHolder(View itemView) {
            super(itemView);
            parent = itemView;
            textView = (TextView) itemView.findViewById(R.id.text1);
            imageView = (UnderlineImageView) itemView.findViewById(R.id.image1);
            imageDrag = (ImageView) itemView.findViewById(R.id.imageDrag);

            parent.setHapticFeedbackEnabled(false);
            if (imageDrag != null) {
                imageDrag.setHapticFeedbackEnabled(false);
            }
        }

        @Override
        public void onItemSelected() {
            // itemView.setBackgroundColor(Color.LTGRAY);
        }

        @Override
        public void onItemClear() {
            // itemView.setBackgroundColor(0);
        }
    }
}