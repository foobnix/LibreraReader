package com.foobnix.ui2.adapter;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;

import java.util.Locale;

/**
 * A catalog with no picture of its own: the first letter of its name, white on a round of the
 * app's colour, as LibreraX shows them.
 */
public final class LetterDrawable extends Drawable {

    private final String letter;
    private final int size;
    private final Paint round = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint text = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Rect bounds = new Rect();

    public LetterDrawable(String name, int color, int size) {
        this.letter = firstLetter(name);
        this.size = size;
        round.setColor(color);
        text.setColor(Color.WHITE);
        text.setTypeface(Typeface.DEFAULT_BOLD);
        text.setTextAlign(Paint.Align.CENTER);
    }

    /** The first letter or digit of the name, in capitals; "#" when it has none. */
    static String firstLetter(String name) {
        if (name != null) {
            final String trimmed = name.trim();
            for (int i = 0; i < trimmed.length(); ) {
                final int cp = trimmed.codePointAt(i);
                if (Character.isLetterOrDigit(cp)) {
                    return new String(Character.toChars(cp)).toUpperCase(Locale.getDefault());
                }
                i += Character.charCount(cp);
            }
        }
        return "#";
    }

    @Override public void draw(Canvas canvas) {
        final Rect b = getBounds();
        final float radius = Math.min(b.width(), b.height()) / 2f;
        canvas.drawCircle(b.exactCenterX(), b.exactCenterY(), radius, round);
        text.setTextSize(radius);
        text.getTextBounds(letter, 0, letter.length(), bounds);
        // Centred on the letter's own height, not on the line's
        canvas.drawText(letter, b.exactCenterX(), b.exactCenterY() + bounds.height() / 2f, text);
    }

    @Override public int getIntrinsicWidth() {
        return size;
    }

    @Override public int getIntrinsicHeight() {
        return size;
    }

    @Override public void setAlpha(int alpha) {
        round.setAlpha(alpha);
        text.setAlpha(alpha);
    }

    @Override public void setColorFilter(ColorFilter colorFilter) {
        round.setColorFilter(colorFilter);
        text.setColorFilter(colorFilter);
    }

    @Override public int getOpacity() {
        return PixelFormat.TRANSLUCENT;
    }
}
