package org.emdev.utils;

import android.graphics.Rect;
import android.graphics.RectF;

public class MathUtils {

    public static int adjust(final int value, final int min, final int max) {
        return Math.min(Math.max(min, value), max);
    }

    public static float adjust(final float value, final float min, final float max) {
        return Math.min(Math.max(min, value), max);
    }

    public static Rect rect(final RectF rect) {
        return new Rect((int) rect.left, (int) rect.top, (int) rect.right, (int) rect.bottom);
    }

    public static Rect rect(final float left, final float top, final float right, final float bottom) {
        return new Rect((int) left, (int) top, (int) right, (int) bottom);
    }

    public static RectF zoom(final RectF rect, final float zoom) {
        return new RectF(zoom * rect.left, zoom * rect.top, zoom * rect.right, zoom * rect.bottom);
    }

    public static RectF zoom(final Rect rect, final float zoom) {
        return new RectF(zoom * rect.left, zoom * rect.top, zoom * rect.right, zoom * rect.bottom);
    }

    public static Rect zoom(final float left, final float top, final float right, final float bottom, final float zoom) {
        return new Rect((int) (zoom * left), (int) (zoom * top), (int) (zoom * right), (int) (zoom * bottom));
    }

    public static int min(final int... values) {
        int min = Integer.MAX_VALUE;
        for (final int v : values) {
            min = Math.min(v, min);
        }
        return min;
    }

    public static int max(final int... values) {
        int max = Integer.MIN_VALUE;
        for (final int v : values) {
            max = Math.max(v, max);
        }
        return max;
    }

    public static float fmin(final float... values) {
        float min = Float.MAX_VALUE;
        for (final float v : values) {
            min = Math.min(v, min);
        }
        return min;
    }

    public static float fmax(final float... values) {
        float max = Float.MIN_VALUE;
        for (final float v : values) {
            max = Math.max(v, max);
        }
        return max;
    }

    public static float round(final float value, final float share) {
        return  (float)Math.floor(value * share) / share;
    }

    public static RectF round(final RectF rect, final float share) {
        rect.left =  (float)Math.floor(rect.left * share) / share;
        rect.top = (float) Math.floor(rect.top * share) / share;
        rect.right = (float) Math.floor(rect.right * share) / share;
        rect.bottom =  (float)Math.floor(rect.bottom * share) / share;
        return rect;
    }
    
    public static RectF floor(final RectF rect) {
        rect.left =  (float)Math.floor(rect.left);
        rect.top = (float) Math.floor(rect.top);
        rect.right = (float) Math.floor(rect.right);
        rect.bottom =  (float)Math.floor(rect.bottom);
        return rect;
    }
    
    public static RectF ceil(final RectF rect) {
        rect.left = (float) Math.ceil(rect.left);
        rect.top = (float) Math.ceil(rect.top);
        rect.right = (float) Math.ceil(rect.right);
        rect.bottom = (float) Math.ceil(rect.bottom);
        return rect;
    }
    
    public static RectF round(final RectF rect) {
        rect.left =  (float)Math.floor(rect.left);
        rect.top = (float) Math.floor(rect.top);
        rect.right = (float) Math.ceil(rect.right);
        rect.bottom =  (float)Math.ceil(rect.bottom);
        return rect;
    }
}
