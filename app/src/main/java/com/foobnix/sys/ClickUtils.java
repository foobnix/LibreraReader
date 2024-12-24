package com.foobnix.sys;

import com.foobnix.android.utils.Dips;
import com.foobnix.model.AppSP;
import com.foobnix.model.AppState;

public class ClickUtils {
    private int WIDHT_X, HEIGHT_Y;
    private float border_side = 0;
    private float border_top = 0;

    public ClickUtils() {
        init();
    }

    public void init() {
        WIDHT_X = Dips.screenWidth();
        HEIGHT_Y = Dips.screenHeight();

        border_side = Dips.screenWidth() * AppState.get().tapzoneSize / 100;
        border_top = Dips.screenHeight() * AppState.get().tapzoneSize / 100;
    }


    public void initMusician() {
        WIDHT_X = Dips.screenWidth();
        HEIGHT_Y = Dips.screenHeight();
        border_side = AppSP.get().readingMode == AppState.READING_MODE_MUSICIAN ? 0 : Dips.screenWidth() * AppState.get().tapzoneSize / 100;
        border_top = AppSP.get().readingMode == AppState.READING_MODE_MUSICIAN ? 0 : Dips.screenHeight() * AppState.get().tapzoneSize / 100;
    }

    public boolean isClickCenter(float x, float y) {
        boolean isX = border_side < x && x < (WIDHT_X - border_side);
        boolean isY = border_top < y && y < (HEIGHT_Y - border_top);
        return isX && isY;

    }

    public boolean isClickLeftTop(float x, float y) {
        boolean isX = x <= WIDHT_X / 3;
        boolean isY = y <= HEIGHT_Y / 3;
        return isX && isY;
    }

    public boolean isClickRightTop(float x, float y) {
        boolean isX = (WIDHT_X - WIDHT_X / 3) <= x;
        boolean isY = y <= HEIGHT_Y / 3;
        return isX && isY;
    }

    public boolean isClickLeftBottom(float x, float y) {
        boolean isX = x <= WIDHT_X / 3;
        boolean isY = (HEIGHT_Y - HEIGHT_Y / 3) <= y;
        return isX && isY;
    }

    public boolean isClickRightBottom(float x, float y) {
        boolean isX = (WIDHT_X - WIDHT_X / 3) <= x;
        boolean isY = (HEIGHT_Y - HEIGHT_Y / 3) <= y;
        return isX && isY;
    }

    public boolean isClickTop(float x, float y) {
        return y <= border_top;
    }

    public boolean isClickBottom(float x, float y) {
        return y >= (HEIGHT_Y - border_top);
    }

    public boolean isClickLeft(float x, float y) {
        boolean isX = x <= border_side;
        boolean isY = HEIGHT_Y / 3 < y && y < (HEIGHT_Y - HEIGHT_Y / 3);
        return isX && isY;
    }

    public boolean isClickRight(float x, float y) {
        boolean isX = x >= (WIDHT_X - border_side);
        boolean isY = HEIGHT_Y / 3 < y && y < (HEIGHT_Y - HEIGHT_Y / 3);
        return isX && isY;
    }

}
