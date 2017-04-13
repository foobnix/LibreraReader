package com.foobnix.pdf;

import java.util.ArrayList;
import java.util.List;

import com.foobnix.android.utils.Dips;
import com.foobnix.android.utils.LOG;
import com.foobnix.pdf.info.AppsConfig;
import com.foobnix.pdf.info.R;
import com.foobnix.pdf.info.view.AnchorHelper;
import com.foobnix.pdf.info.widget.DraggbleTouchListener;
import com.foobnix.pdf.info.wrapper.AppState;

import android.app.Activity;
import android.content.Context;
import android.os.Vibrator;
import android.view.Gravity;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageView;
import android.widget.Toast;

public class DroidActivate {

    private final Activity a;
    int num = 0;

    public static boolean isShowDroidReader(Context c) {
        return AppsConfig.isDroidReaderPkg(c) && AppState.get().isShowDroid;
    }

    public DroidActivate(final Activity a) {
        this.a = a;
        final ImageView droid = (ImageView) a.findViewById(R.id.droid);
        if (!isShowDroidReader(a)) {
            droid.setVisibility(View.GONE);
            return;
        }
        OnClickListener onClickListener = new OnClickListener() {

            @Override
            public void onClick(View v) {
                if (AnchorHelper.getX(v) > Dips.screenWidth() / 2) {
                    droid.setImageResource(R.drawable.icon_pdf_droid);
                } else {
                    droid.setImageResource(R.drawable.icon_pdf_droid);
                }
                vibrate();
                try {
                    List<String> list = new ArrayList<String>();
                    list.add(a.getString(R.string.say_hello_my_name_is_droid_reader1));
                    list.add(a.getString(R.string.let_read_a_book));

                    say("> " + list.get(num));

                    num++;
                    if (num == list.size()) {
                        num = 0;
                    }
                } catch (Exception e) {
                    LOG.e(e);
                }

            }
        };
        droid.setOnTouchListener(new DraggbleTouchListener(droid, null, onClickListener));
    }

    public void say(String str) {
        Toast t = Toast.makeText(a, str, Toast.LENGTH_LONG);
        t.setGravity(Gravity.CENTER, Dips.dpToPx(10), 0);
        t.show();
    }

    public void vibrate() {
        if (AppState.get().isVibration) {
            Vibrator v = (Vibrator) a.getSystemService(Context.VIBRATOR_SERVICE);
            v.vibrate(500);
        }
    }

}
