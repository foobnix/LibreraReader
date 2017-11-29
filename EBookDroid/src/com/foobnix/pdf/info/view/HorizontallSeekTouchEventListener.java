package com.foobnix.pdf.info.view;

import com.foobnix.android.utils.Dips;
import com.foobnix.android.utils.LOG;
import com.foobnix.pdf.info.wrapper.AppState;

import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.widget.SeekBar;

public class HorizontallSeekTouchEventListener implements OnTouchListener {

    SeekBar.OnSeekBarChangeListener onSeekBarChangeListener;
    int pages;
    boolean isProccessClick;
    int distance = Dips.dpToPx(5);
    int delta = Dips.dpToPx(30);
    volatile boolean isMyAction;

    public HorizontallSeekTouchEventListener(SeekBar.OnSeekBarChangeListener onSeekBarChangeListener, int pages, boolean isProccessClick) {
        this.onSeekBarChangeListener = onSeekBarChangeListener;
        this.pages = pages;
        this.isProccessClick = isProccessClick;
    }

    float x, y;

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        if (!AppState.get().isRewindEnable) {
            return false;
        }

        final int action = event.getAction() & MotionEvent.ACTION_MASK;

        if (action == MotionEvent.ACTION_DOWN) {

            isMyAction = false;
            x = event.getX();
            y = event.getY();
            if (onSeekBarChangeListener != null && isProccessClick) {
                int progress = (int) (event.getX() * pages / Dips.screenWidth());
                onSeekBarChangeListener.onProgressChanged(null, progress, true);
                isMyAction = true;
                return true;
            }

            LOG.d("isMyAction", "ACTION_DOWN", isMyAction, Thread.currentThread().getId());
        }
        if (action == MotionEvent.ACTION_MOVE) {
            if (onSeekBarChangeListener != null) {
                float dx = Math.abs(x - event.getX());
                float dy = Math.abs(y - event.getY());
                if (dx >= distance && /* dy <= delta && */ dx > dy * 5) {
                    int progress = (int) (event.getX() * pages / Dips.screenWidth());
                    onSeekBarChangeListener.onProgressChanged(null, progress, true);
                    isMyAction = true;
                    return true;
                }
            }
            LOG.d("isMyAction", "ACTION_MOVE", isMyAction, Thread.currentThread().getId());
        }
        if (action == MotionEvent.ACTION_UP) {
            LOG.d("isMyAction", "ACTION_UP", isMyAction, Thread.currentThread().getId());
            return isMyAction;
        }

        return false;
    }

}
