package com.foobnix.pdf.info.view.confline;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.foobnix.android.utils.TxtUtils;
import com.foobnix.pdf.info.R;
import com.foobnix.pdf.info.view.MyPopupMenu;

import java.util.Arrays;
import java.util.List;

public class ConfLineView extends FrameLayout {
    TextView textView, valueView;

    public ConfLineView(@NonNull Context context) {
        super(context, null);
    }

    public ConfLineView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);

        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.CustomSeek);
        String text = a.getString(R.styleable.CustomSeek_text);
        String value = a.getString(R.styleable.CustomSeek_value);
        a.recycle();

        View inflate = LayoutInflater.from(context)
                                     .inflate(R.layout.line_configuration, this, false);

        textView = inflate.findViewById(R.id.text);
        textView.setText(text);

        valueView = inflate.findViewById(R.id.value);

        if (TxtUtils.isNotEmpty(value)) {
            valueView.setText(value);
        }

        addView(inflate);
    }

    public void init(int defaultAction, ConfResponse onChange, ConfAction... actions) {
        if (actions == null || actions.length == 0) return;

        MyPopupMenu popup = new MyPopupMenu(getContext(), valueView);
        for (ConfAction it : actions) {
            popup.getMenu()
                 .add(it.nameResId)
                 .setOnMenuItemClickListener(item -> {
                     onChange.onResult(it.actionInt);
                     valueView.setText(it.nameResId);
                     TxtUtils.underlineTextView(valueView);
                     return true;
                 });
        }
        ConfAction current = Arrays.stream(actions)
                                   .filter(a -> a.actionInt == defaultAction)
                                   .findFirst()
                                   .orElse(actions[0]);

        valueView.setText(current.nameResId);
        TxtUtils.underlineTextView(valueView);
        valueView.setOnClickListener(v -> popup.show());

    }

}



