package com.example.zcontentintent;

import java.util.Locale;

import com.foobnix.pdf.info.wrapper.DocumentController;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;

public class BannerActivity extends Activity {

    private static final String NUMBER2 = "number";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        DocumentController.chooseFullScreen(this, true);
        setContentView(R.layout.activity_banner);

        ImageView img = (ImageView) findViewById(R.id.banner);
        TextView text = (TextView) findViewById(R.id.slogan);

        int num = getIntent().getIntExtra(NUMBER2, -1);
        if (num == -1) {
            img.setImageResource(R.drawable.banner2);
            text.setText("Универсальная читалка");
        } else if (num == 1) {
            img.setImageResource(R.drawable.banner1);
            text.setText(R.string.banner_slogan_EPUB_READER);
        } else if (num == 2) {
            img.setImageResource(R.drawable.banner2);
            text.setText(R.string.banner_slogan_LIRBI_READER);
        } else if (num == 3) {
            img.setImageResource(R.drawable.banner3);
            text.setText(R.string.banner_slogan_PDF_READER);
        } else if (num == 4) {
            img.setImageResource(R.drawable.banner4);
            text.setText(R.string.banner_slogan_PRO_LIRBI_READER_1);
        }
        // Toast.makeText(this, "num" + num, Toast.LENGTH_SHORT).show();

		registerReceiver(receiver, new IntentFilter("BannerActivity"));
    }

    BroadcastReceiver receiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            String[] data = intent.getStringExtra("locale").split(",");
            String lang = data[0];
            int number = Integer.valueOf(data[1]);

            Locale locale = new Locale(lang);
            Locale.setDefault(locale);
            Configuration config = new Configuration();
            config.locale = locale;
            context.getResources().updateConfiguration(config, context.getResources().getDisplayMetrics());
            finish();
            startActivity(new Intent(BannerActivity.this, BannerActivity.class).putExtra(NUMBER2, number));

        }
    };

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(receiver);
    }

}
