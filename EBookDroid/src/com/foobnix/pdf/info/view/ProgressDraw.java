package com.foobnix.pdf.info.view;

import java.util.ArrayList;
import java.util.List;

import com.foobnix.pdf.info.TintUtil;
import com.foobnix.pdf.info.model.OutlineLinkWrapper;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.util.AttributeSet;
import android.view.View;

public class ProgressDraw extends View {

    Paint paint = new Paint();
    {
        paint.setColor(Color.DKGRAY);
        paint.setStyle(Style.FILL);
    }

    Paint paintRadius = new Paint();
    {
        paintRadius.setColor(Color.parseColor("#8b0000"));
        paintRadius.setStyle(Style.FILL);
    }

    List<OutlineLinkWrapper> dividers = new ArrayList<OutlineLinkWrapper>();
    int pageCount;
    int progress;

    public ProgressDraw(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public ProgressDraw(Context context) {
        super(context);
    }

    public void updateDivs(List<OutlineLinkWrapper> dividers) {
        this.dividers = dividers;
        invalidate();
    }

    public void updateProgress(int progress) {
        this.progress = progress + 1;
        invalidate();
    }

    public void updatePageCount(int pageCount) {
        this.pageCount = pageCount;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        canvas.save();

        paint.setColor(TintUtil.color);

        float k = (float) getWidth() / pageCount;
        int h = getHeight();
        int currentChapter = 0;
        for (OutlineLinkWrapper item : dividers) {
            int pos = item.targetPage;
            if (pos < 0) {
                continue;
            }
            canvas.drawLine(pos * k, 0, pos * k, h, paint);
            if (pos <= progress) {
                currentChapter = pos;
            }
        }

        canvas.drawLine(0, h / 2, progress * k, h / 2, paint);
        canvas.drawCircle(currentChapter * k, h / 2, h / 2, paintRadius);

        canvas.restore();
    }

}
