package com.example.zcontentintent;

import java.io.File;
import java.io.FileInputStream;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import com.foobnix.android.utils.LOG;
import com.foobnix.android.utils.TxtUtils;
import com.foobnix.pdf.info.wrapper.DocumentController;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Environment;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

public class ScreenActivity extends Activity {
    private static final String TEXT2 = "text";
    private static final String NUMBER2 = "number";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        DocumentController.chooseFullScreen(this, true);
        setContentView(R.layout.activity_screenshot_text);

        TextView text1 = (TextView) findViewById(R.id.text1);
        int num = getIntent().getIntExtra(NUMBER2, -1);

        if (TxtUtils.isNotEmpty(getIntent().getStringExtra(TEXT2))) {
            text1.setText(getIntent().getStringExtra(TEXT2));
        } else {
            Map<Integer, Integer> ids = new HashMap<Integer, Integer>();
            ids.put(-1, R.string.app_name);
            // ids.put(1, R.string.screenshot1);
            // ids.put(2, R.string.screenshot2);
            // ids.put(3, R.string.screenshot3);
            // ids.put(4, R.string.screenshot4);
            // ids.put(5, R.string.screenshot5);
            // ids.put(6, R.string.screenshot6);
            // ids.put(7, R.string.screenshot7);
            // ids.put(8, R.string.screenshot8);
            // ids.put(9, R.string.screenshot9);
            // ids.put(10, R.string.screenshot10);
            // ids.put(11, R.string.screenshot11);
            // ids.put(12, R.string.screenshot12);
            // ids.put(13, R.string.screenshot13);
            // ids.put(14, R.string.screenshot14);
            // ids.put(15, R.string.screenshot15);
            // ids.put(16, R.string.screenshot16);
            // ids.put(17, R.string.screenshot17);
            // ids.put(18, R.string.screenshot18);
            // ids.put(19, R.string.screenshot19);
            try {
                text1.setText(ids.get(num));
            } catch (Exception e) {
                text1.setText(e.getMessage());
            }
        }

        ImageView screenShot = (ImageView) findViewById(R.id.screenShot);
        File file = new File(Environment.getExternalStorageDirectory(), "screen.png");
        if (!file.exists()) {
            Toast.makeText(this, "no file", Toast.LENGTH_SHORT).show();
        }

        try {
            FileInputStream in = new FileInputStream(file);
            Bitmap decodeFile = BitmapFactory.decodeStream(in);
            screenShot.setImageBitmap(decodeFile);
            in.close();
        } catch (Exception e) {
            LOG.e(e);
        }

        registerReceiver(receiver, new IntentFilter("ScreenActivity"));

    }

    BroadcastReceiver receiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            String[] data = intent.getStringExtra("locale").split(",");
            String lang = data[0];
            int number = Integer.valueOf(data[1]);
            String text = data[2];

            Locale locale = new Locale(lang);
            Locale.setDefault(locale);
            Configuration config = new Configuration();
            config.locale = locale;
            context.getResources().updateConfiguration(config, context.getResources().getDisplayMetrics());
            finish();
            startActivity(new Intent(ScreenActivity.this, ScreenActivity.class).putExtra(NUMBER2, number).putExtra(TEXT2, text));

        }
    };

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(receiver);
    }

}
