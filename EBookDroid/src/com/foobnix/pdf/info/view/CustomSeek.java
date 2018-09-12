package com.foobnix.pdf.info.view;

import com.foobnix.android.utils.IntegerResponse;
import com.foobnix.android.utils.LOG;
import com.foobnix.android.utils.TxtUtils;
import com.foobnix.pdf.info.R;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;

public class CustomSeek extends FrameLayout {

    private TextView textCurerntValue;
    private SeekBar seek;

    public CustomSeek(Context context) {
        super(context);
        initWith("", "");
    }

    public void initWith(String name, String textColor) {

        View inflate = LayoutInflater.from(getContext()).inflate(R.layout.custom_seek, this, false);
        titleText = (TextView) inflate.findViewById(R.id.name);
        titleText.setText(name);
        if (TxtUtils.isEmpty(name)) {
            titleText.setVisibility(View.GONE);
        }

        textCurerntValue = (TextView) inflate.findViewById(R.id.value);

        if (TxtUtils.isNotEmpty(textColor)) {
            titleText.setTextColor(Color.parseColor(textColor));
            textCurerntValue.setTextColor(Color.parseColor(textColor));
        }

        seek = (SeekBar) inflate.findViewById(R.id.seek);

        plus = inflate.findViewById(R.id.plus);
        minus = inflate.findViewById(R.id.minus);

        plus.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                int i = seek.getProgress() + 1;
                if (i < distance) {
                    seek.setProgress(i);
                }
                seek.setProgress(i);
            }
        });
        minus.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                int i = seek.getProgress() - 1;
                if (i >= 0) {
                    seek.setProgress(i);
                }
            }
        });

        addView(inflate);
        fixSeekBak(seek);

    }

    public static void fixSeekBak(SeekBar seekbar) {
        try {
            seekbar.setOnTouchListener(new OnTouchListener() {
                float x, y;

                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    int action = event.getAction();
                    switch (action) {
                    case MotionEvent.ACTION_DOWN:
                        x = event.getX();
                        y = event.getY();
                        // Disallow Drawer to intercept touch events.
                        v.getParent().requestDisallowInterceptTouchEvent(true);
                        break;

                    case MotionEvent.ACTION_MOVE:
                        float dx = Math.abs(x - event.getX());
                        float dy = Math.abs(y - event.getY());
                        if (dx > dy) {
                            v.getParent().requestDisallowInterceptTouchEvent(true);
                        } else {
                            v.getParent().requestDisallowInterceptTouchEvent(false);
                        }
                        break;

                    case MotionEvent.ACTION_UP:
                        // Allow Drawer to intercept touch events.
                        v.getParent().requestDisallowInterceptTouchEvent(false);
                        break;
                    }

                    // Handle seekbar touch events.
                    v.onTouchEvent(event);
                    return true;
                }
            });
        } catch (Exception e) {
            LOG.e(e);
        }

    }

    public CustomSeek(Context context, AttributeSet attrs) {
        super(context, attrs);

        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.CustomSeek);

        String name = a.getString(R.styleable.CustomSeek_text);

        String textColor = a.getString(R.styleable.CustomSeek_textColor);

        a.recycle();

        initWith(name, textColor);

    }

    public void setTitleTextWidth(int width) {
        titleText.getLayoutParams().width = width;
    }

    public TextView getTitleText() {
        return titleText;
    }

    @Override
    public void setEnabled(boolean enabled) {
        seek.setEnabled(enabled);
        plus.setEnabled(enabled);
        minus.setEnabled(enabled);
    }

    public boolean isChanged() {
        return current != min + seek.getProgress();
    }

    int distance = 0;
    int min = 0;
    int max = 0;
    int current = 0;
    private String sufix;

    public void reset(int current) {
        init(min, max, current, sufix);
    }

    public void sendProgressChanged() {
        integerResponse.onResultRecive(valueResponse);
    }

    public void setMax(int max) {
        this.max = max;
        distance = max - min;
        seek.setMax(distance);
    }

    public void init(final int min, int max, int current) {
        init(min, max, current, "");
    }

    public void init(final int min, int max, int current, final String sufix) {
        this.sufix = sufix;
        seek.setOnSeekBarChangeListener(null);
        this.min = min;
        this.max = max;
        this.current = current;
        valueResponse = current;
        distance = max - min;
        textCurerntValue.setText("" + current + sufix);
        seek.setMax(distance);
        seek.setProgress(current - min);

        seek.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                valueResponse = min + progress;
                textCurerntValue.setText("" + valueResponse + sufix);
                if (integerResponse != null) {
                    integerResponse.onResultRecive(valueResponse);
                }

            }
        });

    }

    int valueResponse;

    public int getCurrentValue() {
        return valueResponse;
    }

    IntegerResponse integerResponse;
    private View plus;
    private View minus;
    private TextView titleText;

    public void setValueText(String text) {
        textCurerntValue.setText(text + sufix);
    }

    public void setOnSeekChanged(IntegerResponse integerResponse) {
        this.integerResponse = integerResponse;
    }

    public void addMyPopupMenu(final MyPopupMenu menu) {
        titleText.setTextAppearance(getContext(), R.style.textLink);

        TxtUtils.underlineTextView(titleText);
        titleText.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                menu.show();
            }
        });
    }

}
