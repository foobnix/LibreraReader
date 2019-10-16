package com.foobnix.pdf.info.view.drag;

import androidx.recyclerview.widget.RecyclerView;

public interface OnStartDragListener {

    /**
     * Called when a view is requesting a start of a drag.
     *
     * @param viewHolder The holder of the view to drag.
     */
    void onStartDrag(RecyclerView.ViewHolder viewHolder);

    void onRevemove();

    void onItemClick(String result);

}