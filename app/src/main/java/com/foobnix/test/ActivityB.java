package com.foobnix.test;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;

public class ActivityB extends Activity {
    int counter = 0;

    @Override
    protected void onCreate(
            @Nullable
            Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        TextView textView = new TextView(this);
        textView.setText("Activity B");
        counter = 0;

        Handler h = new Handler(Looper.getMainLooper());

        Runnable task = new Runnable() {
            @Override
            public void run() {
                counter++;
                textView.setText("Activity B:" + counter);
                h.postDelayed(this, 1000);
            }
        };
        h.postDelayed(task, 1000);

        LinearLayout l = new LinearLayout(this);
        l.setPadding(100, 1000, 0, 0);
        l.addView(textView);
        setContentView(l);

    }
}
