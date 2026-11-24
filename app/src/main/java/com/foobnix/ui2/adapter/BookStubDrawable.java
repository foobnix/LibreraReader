package com.foobnix.ui2.adapter;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;

import java.util.Locale;

/**
 * A book in a catalog with no picture, or one that does not load (Gallica's are behind a bot
 * check): a green book with its format on it, "EPUB", lighter at the spine.
 */
public final class BookStubDrawable extends Drawable {

    private static final int TOP = 0xFF5DA05F;
    private static final int BOTTOM = 0xFF2E6B33;

    private final String format;
    private final int width;
    private final int height;
    private final Paint fill = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint spine = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint text = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF rect = new RectF();
    private final Rect textBounds = new Rect();

    public BookStubDrawable(String format, int width) {
        this.format = format == null ? "" : format.replace(".zip", "").toUpperCase(Locale.US);
        this.width = width;
        this.height = Math.round(width * 1.45f);
        spine.setColor(Color.WHITE);
        spine.setAlpha(40);
        text.setColor(Color.WHITE);
        text.setTypeface(Typeface.DEFAULT_BOLD);
        text.setTextAlign(Paint.Align.CENTER);
    }

    @Override public void draw(Canvas canvas) {
        final Rect b = getBounds();
        rect.set(b);
        final float radius = b.width() * 0.08f;
        fill.setShader(new LinearGradient(0, b.top, 0, b.bottom, TOP, BOTTOM, Shader.TileMode.CLAMP));
        canvas.drawRoundRect(rect, radius, radius, fill);
        // The spine: a lighter band down the left edge
        canvas.drawRect(b.left + b.width() * 0.06f, b.top, b.left + b.width() * 0.11f, b.bottom, spine);
        if (!format.isEmpty()) {
            // As large as fits across the cover, at most a fifth of its width
            float size = b.width() * 0.2f;
            text.setTextSize(size);
            final float room = b.width() * 0.78f;
            final float measured = text.measureText(format);
            if (measured > room) {
                text.setTextSize(size * room / measured);
            }
            text.getTextBounds(format, 0, format.length(), textBounds);
            canvas.drawText(format, b.exactCenterX() + b.width() * 0.03f, b.exactCenterY() + textBounds.height() / 2f, text);
        }
    }

    @Override public int getIntrinsicWidth() {
        return width;
    }

    @Override public int getIntrinsicHeight() {
        return height;
    }

    @Override public void setAlpha(int alpha) {
        fill.setAlpha(alpha);
        text.setAlpha(alpha);
    }

    @Override public void setColorFilter(ColorFilter colorFilter) {
        fill.setColorFilter(colorFilter);
        text.setColorFilter(colorFilter);
    }

    @Override public int getOpacity() {
        return PixelFormat.TRANSLUCENT;
    }
}
