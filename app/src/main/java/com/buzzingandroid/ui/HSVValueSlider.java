package com.buzzingandroid.ui;

import com.buzzingandroid.ui.HSVColorPickerDialog.OnColorSelectedListener;
import com.foobnix.android.utils.LOG;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

public class HSVValueSlider extends View {

    public HSVValueSlider(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    public HSVValueSlider(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public HSVValueSlider(Context context) {
        super(context);
    }

    private OnColorSelectedListener listener;

    public void setListener(OnColorSelectedListener listener) {
        this.listener = listener;
    }

    float[] colorHsv = { 0f, 0f, 1f };

    public void setColor(int color, boolean keepValue) {
        float oldValue = colorHsv[2];
        Color.colorToHSV(color, colorHsv);
        if (keepValue) {
            colorHsv[2] = oldValue;
        }
        if (listener != null) {
            LOG.d("HSV", colorHsv[0], colorHsv[1], colorHsv[2]);
            listener.colorSelected(Color.HSVToColor(colorHsv));
        }

        createBitmap();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (bitmap != null) {
            canvas.drawBitmap(bitmap, srcRect, dstRect, null);
        }
    }

    private Rect srcRect;
    private Rect dstRect;
    private Bitmap bitmap;
    private int[] pixels;

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        if (w <= 0) {
            return;
        }

        srcRect = new Rect(0, 0, w, 1);
        dstRect = new Rect(0, 0, w, h);
        bitmap = Bitmap.createBitmap(w, 1, Config.ARGB_8888);
        pixels = new int[w];

        createBitmap();
    }

    private void createBitmap() {
        if (bitmap == null) {
            return;
        }
        int w = getWidth();

        float[] hsv = new float[] { colorHsv[0], colorHsv[1], 1f };

        int selectedX = (int) (colorHsv[2] * w);

        float value = 0;
        float valueStep = 1f / w;
        for (int x = 0; x < w; x++) {
            value += valueStep;
            if (x >= selectedX - 1 && x <= selectedX + 1) {
                int intVal = 0xFF - (int) (value * 0xFF);
                int color = intVal * 0x010101 + 0xFF000000;
                pixels[x] = color;
            } else {
                hsv[2] = value;
                pixels[x] = Color.HSVToColor(hsv);
            }
        }

        bitmap.setPixels(pixels, 0, w, 0, 0, w, 1);

        invalidate();
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        int action = event.getActionMasked();
        switch (action) {
        case MotionEvent.ACTION_DOWN:
        case MotionEvent.ACTION_MOVE:
            int x = Math.max(0, Math.min(bitmap.getWidth() - 1, (int) event.getX()));
            float value = x / (float) bitmap.getWidth();
            if (true || colorHsv[2] != value) {
                colorHsv[2] = value;
                if (listener != null) {
                    listener.colorSelected(Color.HSVToColor(colorHsv));
                }
                createBitmap();
                invalidate();
            }
            return true;
        }
        return super.onTouchEvent(event);
    }

}