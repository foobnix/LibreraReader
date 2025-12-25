package com.foobnix.test;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.LinearLayout;

import androidx.annotation.Nullable;

public class ActivityA extends Activity {

    @Override
    protected void onCreate(
            @Nullable
            Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        if (!isTaskRoot() && getIntent().hasCategory(Intent.CATEGORY_LAUNCHER) && Intent.ACTION_MAIN.equals(getIntent().getAction())) {
            finish();
            return;
        }
        Button button = new Button(this);
        button.setText("Activity A");

        button.setOnClickListener(v -> {
            Intent i = new Intent(ActivityA.this, ActivityB.class);
            startActivity(i);
        });

        LinearLayout l = new LinearLayout(this);
        l.setPadding(100, 1000, 0, 0);
        l.addView(button);
        setContentView(l);
    }
}
