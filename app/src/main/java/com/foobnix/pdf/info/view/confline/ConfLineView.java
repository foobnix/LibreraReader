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

import com.foobnix.android.utils.LOG;
import com.foobnix.android.utils.ResultResponse;
import com.foobnix.android.utils.TxtUtils;
import com.foobnix.pdf.info.R;
import com.foobnix.pdf.info.view.MyPopupMenu;

import java.util.Arrays;

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



    public void update(){
        int defaultAction = init.readValue();
        ConfAction current = Arrays.stream(actions)
                                   .filter(a -> a.actionInt == defaultAction)
                                   .findFirst()
                                   .orElse(actions[0]);


        current.setTextTo(valueView);
        TxtUtils.underlineTextView(valueView);
    }


    ConfAction[] actions;
    ReadInit init;

    public void init(ReadInit init, ConfResponse onChange, ConfAction... actions) {
        LOG.d("CONF-init",1);
        if (actions == null || actions.length == 0) return;

        int defaultAction = init.readValue();
        this.actions = actions;
        this.init = init;


        MyPopupMenu popup = new MyPopupMenu(getContext(), valueView);
        for (ConfAction it : actions) {
            popup.getMenu()
                 .add(it.nameResId)
                    .add(it.name)
                 .setOnMenuItemClickListener(item -> {
                     onChange.onResult(it.actionInt);
                     it.setTextTo(valueView);
                     TxtUtils.underlineTextView(valueView);
                     return true;
                 });
        }
        update();
        valueView.setOnClickListener(v -> popup.show());

    }

}



