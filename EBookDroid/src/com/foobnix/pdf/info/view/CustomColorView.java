package com.foobnix.pdf.info.view;

import com.buzzingandroid.ui.HSVColorPickerDialog;
import com.buzzingandroid.ui.HSVColorPickerDialog.OnColorSelectedListener;
import com.foobnix.StringResponse;
import com.foobnix.pdf.info.R;
import com.foobnix.pdf.info.wrapper.MagicHelper;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.TextView;

public class CustomColorView extends FrameLayout {

    private TextView text2;
    StringResponse stringResponse;
    private int initColor;
    private TextView text1;

    public CustomColorView(Context context, AttributeSet attrs) {
        super(context, attrs);

        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.CustomSeek);
        String name = a.getString(R.styleable.CustomSeek_text);
        a.recycle();

        View inflate = LayoutInflater.from(context).inflate(R.layout.custom_color_view, this, false);
        text1 = (TextView) inflate.findViewById(R.id.text1);
        text2 = (TextView) inflate.findViewById(R.id.text2);

        text1.setText(name);

        text2.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                new HSVColorPickerDialog(getContext(), initColor, new OnColorSelectedListener() {

                    @Override
                    public void colorSelected(Integer color) {
                        init(color);
                        stringResponse.onResultRecive(MagicHelper.colorToString(color));

                    }
                }).show();

            }
        });

        addView(inflate);
    }

    public TextView getText1() {
        return text1;
    }

    public void init(int initColor) {
        this.initColor = initColor;
        text2.setBackgroundColor(initColor);
        text2.setText(MagicHelper.colorToString(initColor));
    }

    public void setOnColorChanged(StringResponse response) {
        stringResponse = response;
    }

}
