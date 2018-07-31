package com.foobnix.pdf.info.view;

import java.util.ArrayList;
import java.util.List;

import com.foobnix.android.utils.Dips;
import com.foobnix.android.utils.LOG;
import com.foobnix.android.utils.TxtUtils;
import com.foobnix.ext.Fb2Extractor;
import com.foobnix.pdf.info.model.OutlineLinkWrapper;
import com.foobnix.pdf.info.wrapper.AppState;
import com.foobnix.pdf.info.wrapper.MagicHelper;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.support.v4.graphics.ColorUtils;
import android.util.AttributeSet;
import android.view.View;

public class ProgressDraw extends View {

    private static final int max_count = 5;
    private static final int ALPHA = 200;
    Paint paint = new Paint();
    {
        paint.setColor(Color.DKGRAY);
        paint.setStyle(Style.FILL);
        paint.setStrokeWidth(Dips.dpToPx(1));
        paint.setAntiAlias(true);
        paint.setDither(true);
    }

    Paint paint1 = new Paint();
    {
        paint1.setColor(Color.DKGRAY);
        paint1.setStyle(Style.FILL);
        paint1.setStrokeWidth(Dips.dpToPx(1));
        paint1.setAntiAlias(true);
        paint1.setDither(true);
    }

    List<OutlineLinkWrapper> dividers = new ArrayList<OutlineLinkWrapper>();
    int pageCount;
    int progress;
    int color = Color.BLACK;

    public ProgressDraw(Context context, AttributeSet attrs) {
        super(context, attrs);
        setClickable(true);
    }

    public void updateDivs(List<OutlineLinkWrapper> dividers) {
        this.dividers = dividers;
        if (dividers == null || dividers.isEmpty()) {
            return;
        }

        invalidate();
    }

    public void updateProgress(int progress) {
        LOG.d("updateProgress", progress);
        this.progress = progress;
        invalidate();
    }

    public void updatePageCount(int pageCount) {
        LOG.d("updatePageCount", pageCount);
        this.pageCount = pageCount - 1;
        invalidate();
    }

    public void updateColor(int color) {
        this.color = ColorUtils.setAlphaComponent(color, ALPHA);
        invalidate();
    }

    public int getLeftPages(int currentPage, int maxPages) {
        if (TxtUtils.isListEmpty(dividers)) {
            return maxPages;
        }

        int first = dividers.get(0).level;
        for (int i = 0; i < dividers.size(); i++) {
            OutlineLinkWrapper item = dividers.get(i);
            int nextTarget = item.targetPage;
            if (nextTarget > currentPage && item.level <= (AppState.get().isShowSubChaptersOnProgress ? 2 : 0) + first) {
                return nextTarget - currentPage;
            }
        }

        return maxPages - currentPage;

    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        canvas.save();
        try {
            int titleColor = AppState.get().isDayNotInvert ? MagicHelper.otherColor(AppState.get().colorDayBg, -0.05f) : MagicHelper.otherColor(AppState.get().colorNigthBg, 0.05f);
            // int titleColor = AppState.get().isInvert ? Color.WHITE :
            // Color.BLACK;
            if (AppState.get().isAlwaysOpenAsMagazine) {

            } else {
                canvas.drawColor(titleColor);
            }

            paint.setColor(color);
            paint1.setColor(titleColor);

            float k = (float) getWidth() / pageCount;
            int h = getHeight();
            int currentChapter = 0;

            if (AppState.get().isShowChaptersOnProgress && dividers != null && !dividers.isEmpty()) {
                int first = dividers.get(0).level;
                for (OutlineLinkWrapper item : dividers) {
                    int pos = item.targetPage - 1;
                    if (pos < 0 || item.level >= (AppState.get().isShowSubChaptersOnProgress ? 3 : 1) + first || item.getTitleRaw().endsWith(Fb2Extractor.FOOTER_AFTRER_BOODY)) {
                        continue;
                    }

                    if (pos <= progress) {
                        currentChapter = pos;
                    } else {
                        canvas.drawLine(pos * k, 0, pos * k, h, paint);
                    }
                }
            }

            canvas.drawRect(0, 0, progress * k, h, paint);
            if (currentChapter != 0) {
                canvas.drawLine(currentChapter * k, 0, currentChapter * k, h, paint1);
            }

        } catch (Exception e) {
            LOG.e(e);
        }
        canvas.restore();
    }

}
