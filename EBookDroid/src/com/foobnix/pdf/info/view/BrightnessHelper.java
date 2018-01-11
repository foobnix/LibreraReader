package com.foobnix.pdf.info.view;

import org.greenrobot.eventbus.EventBus;

import com.foobnix.android.utils.Dips;
import com.foobnix.android.utils.IntegerResponse;
import com.foobnix.android.utils.LOG;
import com.foobnix.pdf.info.R;
import com.foobnix.pdf.info.wrapper.AppState;
import com.foobnix.pdf.search.activity.msg.MessegeBrightness;

import android.app.Activity;
import android.provider.Settings.SettingNotFoundException;
import android.support.v4.graphics.ColorUtils;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.WindowManager;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.TextView;
import android.widget.Toast;

public class BrightnessHelper {

    public static final int BRIGHTNESS_WIDTH = Dips.dpToPx(50);
    Toast toast;

    float lastPercent = 0;
    static float currentPercent = 0;

    int MAX = Dips.dpToPx(1000);
    private float x;
    private float y;
    boolean isMovementStart;

    public BrightnessHelper() {
        updateCurrentValue();
    }

    public void updateCurrentValue() {
        if (AppState.get().isEnableBlueFilter) {
            currentPercent = AppState.get().blueLightAlpha * -1;
        } else {
            currentPercent = AppState.get().appBrightness == AppState.AUTO_BRIGTNESS ? 0 : AppState.get().appBrightness;
        }
    }

    public void onActoinDown(float x, float y) {
        this.x = x;
        this.y = y;
        isMovementStart = false;
        currentPercent = getMinMaxValue(currentPercent + lastPercent);
    }

    public void onActionUp() {
        // isMovementStart = false;
    }

    public float getMinMaxValue(float value) {
        if (value > 100) {
            return 100;
        }
        if (value < -100) {
            return -100;
        }
        return value;
    }


    public boolean onActionMove(final MotionEvent event) {
        if (!AppState.get().isBrighrnessEnable) {
            return false;
        }

        float yDiff = y - event.getY();
        float dy = Math.abs(yDiff);
        float dx = Math.abs(x - event.getX());

        if (dy > dx * 2 && x < BRIGHTNESS_WIDTH && event.getPointerCount() == 1 && dy > Dips.dpToPx(5)) {
            isMovementStart = true;
            lastPercent = (int) (yDiff * 100 / MAX);
            float plus = getMinMaxValue(lastPercent + currentPercent);
            EventBus.getDefault().post(new MessegeBrightness((int) plus));
        }
        return isMovementStart;

    }

    public static void controlsWrapper(View inflate, Activity a) {
        controlsWrapper(inflate, a, null);
    }

    public static void controlsWrapper(View inflate, Activity a, final Runnable onAuto) {
        final CheckBox autoSettings = (CheckBox) inflate.findViewById(R.id.autoSettings);

        final int systemBrigtnessInt = getSystemBrigtnessInt(a);

        final CustomSeek customBrightness = (CustomSeek) inflate.findViewById(R.id.customBrightness);

        int value = 0;
        if (AppState.get().appBrightness == AppState.AUTO_BRIGTNESS) {
            value = systemBrigtnessInt;
        } else {
            value = AppState.get().isEnableBlueFilter ? AppState.get().blueLightAlpha * -1 : AppState.get().appBrightness;
        }

        customBrightness.init(-100, 100, value);

        customBrightness.setOnSeekChanged(new IntegerResponse() {

            @Override
            public boolean onResultRecive(int result) {
                customBrightness.setValueText("" + result);
                EventBus.getDefault().post(new MessegeBrightness(result));
                return false;
            }
        });

        autoSettings.setOnCheckedChangeListener(new OnCheckedChangeListener() {

            @Override
            public void onCheckedChanged(final CompoundButton buttonView, final boolean isChecked) {
                if (!buttonView.isPressed()) {
                    return;
                }
                if (onAuto != null) {
                    onAuto.run();
                }
                if (isChecked) {// auto
                    customBrightness.setEnabled(false);
                    customBrightness.reset(systemBrigtnessInt);
                    EventBus.getDefault().post(new MessegeBrightness(AppState.AUTO_BRIGTNESS));
                } else {
                    customBrightness.setEnabled(true);
                    EventBus.getDefault().post(new MessegeBrightness(systemBrigtnessInt));
                }

            }
        });
        autoSettings.setChecked(AppState.get().appBrightness == AppState.AUTO_BRIGTNESS);
        customBrightness.setEnabled(AppState.get().appBrightness != AppState.AUTO_BRIGTNESS);

    }

    public static void applyBrigtness(final Activity a) {
        try {
            int appBrightness = AppState.get().appBrightness;

            final WindowManager.LayoutParams lp = a.getWindow().getAttributes();

            if (appBrightness == AppState.AUTO_BRIGTNESS) {
                lp.screenBrightness = -1;
            } else if (appBrightness == 0) {
                lp.screenBrightness = 0;
            } else {
                lp.screenBrightness = (float) appBrightness / 100;
            }

            LOG.d("applyBrigtness", lp.screenBrightness);

            a.getWindow().setAttributes(lp);
        } catch (

        Exception e) {
            LOG.e(e);
        }
    }

    public static int getSystemBrigtnessInt(final Activity a) {
        try {
            final int brightInt = android.provider.Settings.System.getInt(a.getContentResolver(), android.provider.Settings.System.SCREEN_BRIGHTNESS);
            int res = brightInt * 100 / 255;
            LOG.d("getSystemBrigtnessInt", brightInt, res);
            return res;
        } catch (final SettingNotFoundException e) {
            e.printStackTrace();
        }
        return 50;
    }

    public static void onMessegeBrightness(MessegeBrightness msg, final TextView textView, final View overlay) {
        int value = msg.getValue();
        textView.setVisibility(View.VISIBLE);
        textView.getHandler().removeCallbacksAndMessages(null);
        textView.getHandler().postDelayed(new Runnable() {

            @Override
            public void run() {
                textView.setVisibility(View.GONE);
            }
        }, 1000);

        textView.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                onMessegeBrightness(new MessegeBrightness(AppState.AUTO_BRIGTNESS), textView, overlay);
            }
        });

        if (value == AppState.AUTO_BRIGTNESS) {
            AppState.get().isEnableBlueFilter = false;
            AppState.get().blueLightAlpha = 0;
            AppState.get().appBrightness = AppState.AUTO_BRIGTNESS;
            textView.setText(textView.getContext().getString(R.string.automatic));

        } else if (value < 0) {
            AppState.get().isEnableBlueFilter = true;
            AppState.get().blueLightAlpha = Math.abs(value);
            AppState.get().appBrightness = 0;

            textView.setText(textView.getContext().getString(R.string.brightness) + " " + value + "%");
        } else {
            AppState.get().isEnableBlueFilter = false;
            AppState.get().blueLightAlpha = 0;
            AppState.get().appBrightness = value;

            textView.setText(textView.getContext().getString(R.string.brightness) + " " + value + "%");
        }

        BrightnessHelper.applyBrigtness((Activity) textView.getContext());
        updateOverlay(overlay);

    }

    public static void updateOverlay(final View overlay) {
        if (AppState.get().isEnableBlueFilter) {
            overlay.setVisibility(View.VISIBLE);
            overlay.setBackgroundColor(ColorUtils.setAlphaComponent(AppState.get().blueLightColor, 220 * AppState.get().blueLightAlpha / 100));
        } else {
            overlay.setVisibility(View.GONE);
        }

    }

}
