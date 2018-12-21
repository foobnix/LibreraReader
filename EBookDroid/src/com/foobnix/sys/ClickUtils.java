package com.foobnix.sys;

import com.foobnix.android.utils.Dips;
import com.foobnix.pdf.info.wrapper.AppState;

import android.view.MotionEvent;

public class ClickUtils {
    private int WIDHT_X, HEIGHT_Y;
    private float border_side = 0;
    private float border_top = 0;
    private static int DP_IGNORE = Dips.DP_50;

    public ClickUtils() {
        init();
    }

    public void init() {
        WIDHT_X = Dips.screenWidth();
        HEIGHT_Y = Dips.screenHeight();

        border_side = Dips.screenWidth() / 100 * AppState.get().tapzoneSize;
        border_top = Dips.screenHeight() / 100 * AppState.get().tapzoneSize;
    }

    public boolean isIgoreZoom(final MotionEvent event) {
        if (event.getY() < DP_IGNORE || event.getY(1) < DP_IGNORE) {
            return true;
        }
        if (event.getY() > (HEIGHT_Y - DP_IGNORE) || event.getY(1) > (HEIGHT_Y - DP_IGNORE)) {
            return true;
        }
        return false;
    }

    public void initMusician() {
        WIDHT_X = Dips.screenWidth();
        HEIGHT_Y = Dips.screenHeight();
        border_side = AppState.get().isMusicianMode ? 0 : Dips.screenWidth() / 100 * AppState.get().tapzoneSize;
        border_top = AppState.get().isMusicianMode ? 0 : Dips.screenHeight() / 100 * AppState.get().tapzoneSize;
    }

    public boolean isClickCenter(float x, float y) {
        boolean isX = border_side < x && x < (WIDHT_X - border_side);
        boolean isY = border_top < y && y < (HEIGHT_Y - border_top);
        return isX && isY;

    }

    public boolean isClickTop(float x, float y) {
        return y < border_top;
    }

    public boolean isClickBottom(float x, float y) {
        return y > (HEIGHT_Y - border_top);
    }

    public boolean isClickLeft(float x, float y) {
        return x < border_side;
    }

    public boolean isClickRight(float x, float y) {
        return x > (WIDHT_X - border_side);
    }

}
