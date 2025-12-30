package com.foobnix.pdf.info.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.util.AttributeSet;
import android.view.View;

import androidx.core.graphics.ColorUtils;

import com.foobnix.android.utils.Dips;
import com.foobnix.android.utils.LOG;
import com.foobnix.ext.Fb2Extractor;
import com.foobnix.model.AppState;
import com.foobnix.pdf.info.model.OutlineLinkWrapper;
import com.foobnix.pdf.info.wrapper.MagicHelper;

import java.util.ArrayList;
import java.util.List;

public class ProgressDraw extends View {

    private static final int ALPHA = 245;
    Paint paint = new Paint();

    {
        paint.setColor(Color.YELLOW);
        paint.setStyle(Style.FILL);
        paint.setStrokeWidth(Dips.dpToPx(1));
        paint.setAntiAlias(true);
        paint.setDither(true);
    }


    List<OutlineLinkWrapper> dividers = new ArrayList<OutlineLinkWrapper>();
    int pageCount;
    int progress;
    int color = Color.BLACK;
    int color1 = Color.BLACK;
    //int bgColor = Color.BLACK;

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
        this.color1 = MagicHelper.alpha(40, MagicHelper.getForegroundColor());

        paint.setColor(color);
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        canvas.save();
        try {
            paint.setColor(color);


            float k = (float) getWidth() / pageCount;
            int h = getHeight();
            int currentChapter = 0;

            int prevX = -1;
            if (AppState.get().isShowChaptersOnProgress && dividers != null && !dividers.isEmpty()) {
                int first = dividers.get(0).level;
                for (OutlineLinkWrapper item : dividers) {
                    int pos = item.targetPage - 1;
                    if (pos < 0 || item.level >= (AppState.get().isShowSubChaptersOnProgress ? 3 : 1) + first || item.getTitleRaw()
                                                                                                                     .endsWith(Fb2Extractor.FOOTER_AFTRER_BOODY)) {
                        continue;
                    }

                    if (pos <= progress) {
                        currentChapter = pos;
                    } else {
                        int posX = (int) (pos * k);
                        if (posX != prevX) {
                            canvas.drawLine(posX, 0, posX, h, paint);
                            LOG.d("Skip-line", posX);
                            prevX = posX;
                        }
                    }
                }
            }

            float progressLine = progress * k;
            //canvas.drawRect(0, 0, progressLine, h, paint);
            if (currentChapter != 0) {
                float chapterLine = currentChapter * k;
                //canvas.drawLine(chapterLine, 0, chapterLine, h, paint1);
                canvas.drawRect(0, 0, chapterLine - Dips.dpToPx(1), h, paint);
                canvas.drawRect(chapterLine, 0, progressLine, h, paint);
            } else {
                canvas.drawRect(0, 0, progressLine, h, paint);
            }

        } catch (Exception e) {
            LOG.e(e);
        }
        canvas.restore();
    }

}
