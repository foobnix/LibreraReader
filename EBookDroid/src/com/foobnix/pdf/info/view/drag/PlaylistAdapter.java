package com.foobnix.pdf.info.view.drag;

import java.util.Collections;
import java.util.List;

import com.foobnix.android.utils.Dips;
import com.foobnix.pdf.info.ExtUtils;
import com.foobnix.pdf.info.IMG;
import com.foobnix.pdf.info.R;

import android.content.Context;
import android.support.v4.view.MotionEventCompat;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

public class PlaylistAdapter extends RecyclerView.Adapter<PlaylistAdapter.ItemViewHolder> implements ItemTouchHelperAdapter {

    private final List<String> mItems;
    private static final int SIZE = Dips.isLargeOrXLargeScreen() ? Dips.DP_80 : Dips.DP_60;


    private final OnStartDragListener mDragStartListener;

    public PlaylistAdapter(Context context, List<String> mItems, OnStartDragListener dragStartListener) {
        mDragStartListener = dragStartListener;
        this.mItems = mItems;
    }

    @Override
    public ItemViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_tab_line, parent, false);
        ItemViewHolder itemViewHolder = new ItemViewHolder(view);
        return itemViewHolder;
    }

    @Override
    public void onBindViewHolder(final ItemViewHolder holder, int position) {
        final String item = mItems.get(position);
        holder.textView.setText(ExtUtils.getFileName(item));
        holder.isVisible.setVisibility(View.GONE);
        
        holder.parent.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                mDragStartListener.onItemClick(item);
            }
        });

        int size = SIZE;
        holder.imageView.getLayoutParams().width = size;
        holder.imageView.getLayoutParams().height = (int) (size * IMG.WIDTH_DK);

        IMG.getCoverPageWithEffect(holder.imageView, item, SIZE, null);

        // Start a drag whenever the handle view it touched
        holder.imageDrag.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (MotionEventCompat.getActionMasked(event) == MotionEvent.ACTION_DOWN) {
                    mDragStartListener.onStartDrag(holder);
                }
                return false;
            }
        });
    }

    @Override
    public void onItemDismiss(int position) {
        mItems.remove(position);
        notifyItemRemoved(position);
        mDragStartListener.onRevemove();
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


    public static class ItemViewHolder extends RecyclerView.ViewHolder implements ItemTouchHelperViewHolder {

        public final TextView textView;
        public final ImageView imageView, imageDrag;
        public final View isVisible;
        public final View parent;

        public ItemViewHolder(View itemView) {
            super(itemView);
            parent = itemView;
            textView = (TextView) itemView.findViewById(R.id.text1);
            imageView = (ImageView) itemView.findViewById(R.id.image1);
            imageDrag = (ImageView) itemView.findViewById(R.id.imageDrag);
            isVisible = itemView.findViewById(R.id.isVisible);
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