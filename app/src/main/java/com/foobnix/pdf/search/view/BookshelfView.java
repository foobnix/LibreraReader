package com.foobnix.pdf.search.view;

import android.content.Context;
import android.graphics.Bitmap;
import android.util.AttributeSet;
import android.widget.GridView;

public class BookshelfView extends GridView {

    private Bitmap background;

    public BookshelfView(Context context) {
        super(context);
    }

    public BookshelfView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public BookshelfView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }
}