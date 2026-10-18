package com.foobnix.pdf.info;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;

import com.foobnix.android.utils.Dips;
import com.foobnix.android.utils.TxtUtils;
import com.foobnix.model.AppAnnotation;

/**
 * Draws a mark the way its kind is read: a highlight behind the words, an underline under them,
 * a strikethrough across them.
 *
 * Both reading modes draw their own overlay - scroll mode in {@code EventDraw}, paged mode in
 * {@code PageImaveView} - and a mark has to look the same in either, so the drawing itself lives
 * here and each mode only supplies the rectangle in its own coordinates.
 */
public class AnnotationsPainter {

    /** A highlight has to leave the words readable through it. */
    private static final int HIGHLIGHT_ALPHA = 90;

    private static final Paint paint = new Paint();

    static {
        paint.setAntiAlias(true);
    }

    public static synchronized void draw(final Canvas canvas, final RectF rect, final AppAnnotation annotation) {
        if (canvas == null || rect == null || annotation == null) {
            return;
        }
        final int color = parseColor(annotation.color);
        switch (annotation.getType()) {
            case UNDERLINE:
                paint.setStyle(Paint.Style.STROKE);
                paint.setStrokeWidth(Dips.dpToPx(2));
                paint.setColor(color);
                paint.setAlpha(255);
                canvas.drawLine(rect.left, rect.bottom, rect.right, rect.bottom, paint);
                break;
            case STRIKEOUT:
                paint.setStyle(Paint.Style.STROKE);
                paint.setStrokeWidth(Dips.dpToPx(2));
                paint.setColor(color);
                paint.setAlpha(255);
                canvas.drawLine(rect.left, rect.centerY(), rect.right, rect.centerY(), paint);
                break;
            case HIGHLIGHT:
            default:
                paint.setStyle(Paint.Style.FILL);
                paint.setColor(color);
                paint.setAlpha(HIGHLIGHT_ALPHA);
                canvas.drawRect(rect, paint);
                break;
        }
    }

    /** The form the reading settings keep a colour in, so a mark's is readable in the JSON. */
    public static String toHex(final int color) {
        return String.format("#%06X", 0xFFFFFF & color);
    }

    public static int parseColor(final String color) {
        if (TxtUtils.isNotEmpty(color)) {
            try {
                return Color.parseColor(color);
            } catch (IllegalArgumentException e) {
                // a colour from a file someone edited by hand - draw it yellow rather than not
            }
        }
        return Color.YELLOW;
    }
}
