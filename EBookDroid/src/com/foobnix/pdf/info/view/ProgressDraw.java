package com.foobnix.pdf.info.view;

import java.util.ArrayList;
import java.util.List;

import com.foobnix.android.utils.Dips;
import com.foobnix.android.utils.LOG;
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

    int level0count, level1count = 0;

    public void updateDivs(List<OutlineLinkWrapper> dividers) {
        this.dividers = dividers;
        level0count = 0;
        level1count = 0;
        if (dividers == null) {
            return;
        }
        for (OutlineLinkWrapper link : dividers) {
            if (link.level == 0) {
                level0count++;
            }
            if (link.level == 1) {
                level1count++;
            }
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

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        canvas.save();

        int bgColor = MagicHelper.darkerColor(MagicHelper.getBgColor());
        canvas.drawColor(bgColor);

        paint.setColor(color);
        paint1.setColor(bgColor);

        float k = (float) getWidth() / pageCount;
        int h = getHeight();
        int w = getWidth();
        int currentChapter = 0;
        if (AppState.get().isShowChaptersOnProgress && dividers != null && !dividers.isEmpty()) {
            int deep = (level0count == 1 ? level1count >= 10 ? 2 : 3 : 2);
            LOG.d("Deep count", deep);
            for (OutlineLinkWrapper item : dividers) {
                int pos = item.targetPage - 1;
                if (pos < 0 || item.level >= deep || item.getTitleRaw().endsWith(Fb2Extractor.FOOTER_AFTRER_BOODY)) {
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
        canvas.drawLine(currentChapter * k, 0, currentChapter * k, h, paint1);

        canvas.restore();
    }

}
