package com.foobnix.pdf.info.widget;

import com.foobnix.android.utils.Dips;
import com.foobnix.android.utils.LOG;
import com.foobnix.pdf.info.view.AnchorHelper;
import com.foobnix.pdf.info.view.DragingPopup;

import android.graphics.PointF;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;

public class DraggbleTouchListener implements OnTouchListener {
    PointF initLatout = new PointF();
    PointF initPoint = new PointF();
    private int width;
    private int heigh;
    private int sWidth;
    private int sHeigh;
    private View anchor;
    private View root;
    private DragingPopup popup;
    private OnClickListener onClickListener;

    public DraggbleTouchListener(View anchor, DragingPopup popup, OnClickListener onClickListener) {
        this.anchor = anchor;
        this.popup = popup;
        this.onClickListener = onClickListener;
        this.root = (View) anchor.getParent();
    }

    public DraggbleTouchListener(View anchor, DragingPopup popup) {
        this.anchor = anchor;
        this.popup = popup;
        this.root = (View) anchor.getParent();
    }

    long time;

    private Runnable onEventDetected;

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        LOG.d("DraggbleTouchListener", event);
        if (onEventDetected != null) {
            onEventDetected.run();
        }
        if (anchor == null || root == null) {
            return false;
        }
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            width = anchor.getWidth();
            heigh = anchor.getHeight();

            sWidth = root.getWidth();
            sHeigh = root.getHeight();

            initLatout.x = AnchorHelper.getX(anchor);
            initLatout.y = AnchorHelper.getY(anchor);

            initPoint.x = event.getRawX();
            initPoint.y = event.getRawY();

            if (System.currentTimeMillis() - time < 250) {
                if (popup != null) {
                    popup.initState();
                }

            }
            time = System.currentTimeMillis();

        }

        if (event.getAction() == MotionEvent.ACTION_MOVE) {
            float dx = event.getRawX() - initPoint.x;
            float dy = event.getRawY() - initPoint.y;
            float x = initLatout.x + dx;

            if (x < 0) {
                x = 0;
            }
            if (x > sWidth - width) {
                x = sWidth - width;
            }

            float y = initLatout.y + dy;
            if (y < 0) {
                y = 0;
            }
            if (y > sHeigh - heigh) {
                y = sHeigh - heigh;
            }

            AnchorHelper.setXY(anchor, x, y);

            if (heigh > Dips.screenHeight() - Dips.DP_25) {
                popup.getView().getLayoutParams().height = Dips.screenHeight() - Dips.DP_25;
                popup.getView().requestLayout();
            }

            if (width > Dips.screenWidth() - Dips.DP_25) {
                popup.getView().getLayoutParams().width = Dips.screenWidth() - Dips.DP_25;
                popup.getView().requestLayout();
            }

        }
        if (event.getAction() == MotionEvent.ACTION_UP) {
            float dx = event.getRawX() - initPoint.x;
            float dy = event.getRawY() - initPoint.y;
            boolean isMove = (Math.abs(dx) + Math.abs(dy)) < Dips.dpToPx(10);
            if (onClickListener != null && isMove) {
                onClickListener.onClick(anchor);
            }
            if (onMoveFinish != null) {
                onMoveFinish.run();
            }
        }
        return true;
    }

    Runnable onMoveFinish;

    public void setOnMoveFinish(Runnable onMoveFinish) {
        this.onMoveFinish = onMoveFinish;

    }

    public Runnable getOnEventDetected() {
        return onEventDetected;
    }

    public void setOnEventDetected(Runnable onEventDetected) {
        this.onEventDetected = onEventDetected;
    }
}
