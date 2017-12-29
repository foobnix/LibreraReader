package com.foobnix.pdf.info.view;

import org.greenrobot.eventbus.EventBus;

import com.foobnix.android.utils.Dips;
import com.foobnix.pdf.info.wrapper.AppState;
import com.foobnix.pdf.search.activity.msg.MessegeBrightness;

import android.view.MotionEvent;
import android.widget.Toast;

public class BrightnessHelper {

    public static final int BRIGHTNESS_WIDTH = Dips.dpToPx(50);
    Toast toast;

    int lastPercent = 0;
    int currentPercent = 0;

    int MAX = Dips.screenHeight();
    private float x;
    private float y;
    boolean isMovementStart;

    public BrightnessHelper() {
        if (AppState.get().isEnableBlueFilter) {
            currentPercent = AppState.get().blueLightAlpha * -1;
        } else {
            currentPercent = (int) (AppState.get().brightness * 100);
        }
    }

    public void onActoinDown(float x, float y) {
        this.x = x;
        this.y = y;
        isMovementStart = false;
    }

    public void onActionUp() {
        currentPercent = getMinMaxValue(currentPercent + lastPercent);
        isMovementStart = false;
    }

    public int getMinMaxValue(int value) {
        if (value > 100) {
            return 100;
        }
        if (value < -100) {
            return -100;
        }
        return value;
    }

    public boolean isBrignressWidth() {
        return x < BRIGHTNESS_WIDTH;
    }

    public boolean onActionMove(final MotionEvent event) {
        float yDiff = y - event.getY();
        float dy = Math.abs(yDiff);
        float dx = Math.abs(x - event.getX());

        if (dy > dx * 2 && x < BRIGHTNESS_WIDTH) {
            isMovementStart = true;
            lastPercent = (int) (yDiff * 100 / MAX);
            int plus = getMinMaxValue(lastPercent + currentPercent);
            EventBus.getDefault().post(new MessegeBrightness(plus));
        }
        return isMovementStart;

    }

}
