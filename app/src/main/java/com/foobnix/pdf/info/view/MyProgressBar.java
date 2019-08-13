package com.foobnix.pdf.info.view;

import android.content.Context;
import android.os.Build;
import android.support.annotation.RequiresApi;
import android.util.AttributeSet;
import android.widget.ProgressBar;

import com.foobnix.android.utils.LOG;

public class MyProgressBar extends ProgressBar {

    public MyProgressBar(Context context) {
        super(context);
        LOG.d("MyProgressBar-1", getId());
        setSaveEnabled(false);
    }

    public MyProgressBar(Context context, AttributeSet attrs) {
        super(context, attrs);
        LOG.d("MyProgressBar-2", getId());
        setSaveEnabled(false);
    }

    public MyProgressBar(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        LOG.d("MyProgressBar-3", getId());
        setSaveEnabled(false);
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public MyProgressBar(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        LOG.d("MyProgressBar-4", getId());
        setSaveEnabled(false);
    }

}
