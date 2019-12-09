package com.foobnix.pdf.info.view;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnDismissListener;
import android.graphics.Color;
import android.os.Handler;
import android.provider.Settings.SettingNotFoundException;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.WindowManager;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.TextView;

import androidx.core.graphics.ColorUtils;

import com.foobnix.StringResponse;
import com.foobnix.android.utils.Dips;
import com.foobnix.android.utils.IntegerResponse;
import com.foobnix.android.utils.Keyboards;
import com.foobnix.android.utils.LOG;
import com.foobnix.android.utils.TxtUtils;
import com.foobnix.model.AppState;
import com.foobnix.pdf.info.R;
import com.foobnix.pdf.search.activity.msg.MessegeBrightness;

import org.greenrobot.eventbus.EventBus;


public class BrightnessHelper {

    public static final int BRIGHTNESS_WIDTH = Dips.DP_50;
    static float currentPercent = 0;
    float lastPercent = 0;
    int MAX = Dips.dpToPx(3000);
    int MIN = Dips.dpToPx(500);
    boolean isMovementStart;
    private float x;
    private float y;

    public BrightnessHelper(Context c) {
        updateCurrentValue();
    }

    public static void applyBrigtness(final Activity a) {
        try {
            int appBrightness = appBrightness();

            final WindowManager.LayoutParams lp = a.getWindow().getAttributes();

            float myBrightness;

            if (appBrightness == AppState.AUTO_BRIGTNESS) {
                myBrightness = WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE;
            } else if (appBrightness == 0) {
                if (AppState.get().isAllowMinBrigthness) {
                    myBrightness = WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_OFF;
                } else {
                    myBrightness = 0.01f;
                }
            } else {
                float value = (float) appBrightness / 100;
                myBrightness = Math.max(0.01f, value);
            }

            if (myBrightness != lp.screenBrightness) {
                LOG.d("applyBrigtness", a.getComponentName(), lp.screenBrightness, myBrightness);
                lp.screenBrightness = myBrightness;
                a.getWindow().setAttributes(lp);
            } else {
                LOG.d("applyBrigtness not need", lp.screenBrightness);
            }
        } catch (Exception e) {
            LOG.e(e);
        }
    }

    public static int getSystemBrigtnessInt(final Activity a) {
        try {
            int brightInt = android.provider.Settings.System.getInt(a.getContentResolver(), android.provider.Settings.System.SCREEN_BRIGHTNESS);

            if (brightInt > 255) {
                brightInt = 255;
            }

            int res = (brightInt * 100) / 255;
            LOG.d("getSystemBrigtnessInt", brightInt, res);

            return res;
        } catch (final SettingNotFoundException e) {
            LOG.e(e);
        }
        return 50;
    }

    public static synchronized void onMessegeBrightness(final Handler h, MessegeBrightness msg, final TextView textView, final View overlay) {
        int value = msg.getValue();
        textView.setVisibility(View.VISIBLE);
        h.removeCallbacksAndMessages(null);
        h.postDelayed(new Runnable() {

            @Override
            public void run() {
                textView.setVisibility(View.GONE);
            }
        }, 2000);

        textView.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                onMessegeBrightness(h, new MessegeBrightness(AppState.AUTO_BRIGTNESS), textView, overlay);
                Activity inflate = (Activity) textView.getContext();
                CheckBox isEnableBlueFilter = inflate.findViewById(R.id.autoSettings);
                if (isEnableBlueFilter != null) {
                    isEnableBlueFilter.setChecked(true);
                }
                View customBrightness = inflate.findViewById(R.id.customBrightness);
                if (customBrightness != null) {
                    customBrightness.setEnabled(false);
                }

            }
        });

        if (value == AppState.AUTO_BRIGTNESS) {
            isEnableBlueFilter(false);
            // AppState.get().blueLightAlpha = 0;

            appBrightness(AppState.AUTO_BRIGTNESS);
            textView.setText(textView.getContext().getString(R.string.system_brightness));

        } else if (value < 0) {
            isEnableBlueFilter (true);
            blueLightAlpha(Math.abs(value));
            appBrightness(0);

            textView.setText(textView.getContext().getString(R.string.brightness) + " " + value + "%");
        } else {
            isEnableBlueFilter(false);
            // AppState.get().blueLightAlpha = 0;
            appBrightness(value);

            textView.setText(textView.getContext().getString(R.string.brightness) + " " + value + "%");
        }

        BrightnessHelper.applyBrigtness((Activity) textView.getContext());
        updateOverlay(overlay);

    }

    public static void updateOverlay(final View overlay) {
        if (overlay == null) {
            return;
        }
        if (isEnableBlueFilter()) {
            overlay.setVisibility(View.VISIBLE);
            int alpha;

            if (AppState.get().isAllowMinBrigthness) {
                alpha = Math.min(200, (200 * blueLightAlpha()) / 100);
            } else {
                alpha = Math.min(160, (160 * blueLightAlpha()) / 100);
            }


            LOG.d("blueLightAlpha-value", alpha);

            overlay.setBackgroundColor(ColorUtils.setAlphaComponent(AppState.get().blueLightColor, alpha));
        } else {
            overlay.setVisibility(View.GONE);
        }

    }

    public static void showBlueLigthDialogAndBrightness(final Activity a, View inflate, final Runnable onRefresh) {
        final CheckBox isEnableBlueFilter = (CheckBox) inflate.findViewById(R.id.isEnableBlueFilter);
        isEnableBlueFilter.setVisibility(Dips.isEInk() ? View.GONE : View.VISIBLE);
        isEnableBlueFilter.setOnCheckedChangeListener(new OnCheckedChangeListener() {

            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                isEnableBlueFilter(isChecked);
                if (onRefresh != null) {
                    onRefresh.run();
                }
            }
        });

        final TextView onBlueFilter = (TextView) inflate.findViewById(R.id.onBlueFilter);
        onBlueFilter.setVisibility(Dips.isEInk() ? View.GONE : View.VISIBLE);
        TxtUtils.underlineTextView(onBlueFilter);
        onBlueFilter.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                final AlertDialog.Builder builder = new AlertDialog.Builder(v.getContext());
                builder.setTitle(R.string.bluelight_filter);

                final View inflate = LayoutInflater.from(a).inflate(R.layout.dialog_bluelight, null, false);

                final CustomColorView blueLightColor = (CustomColorView) inflate.findViewById(R.id.blueLightColor);
                TxtUtils.bold(blueLightColor.getText1());
                blueLightColor.withDefaultColors(AppState.BLUE_FILTER_DEFAULT_COLOR, Color.parseColor("#FFDD00"), Color.RED, Color.GREEN);
                blueLightColor.init(AppState.get().blueLightColor);
                blueLightColor.setOnColorChanged(new StringResponse() {

                    @Override
                    public boolean onResultRecive(String string) {
                        isEnableBlueFilter(true);
                        AppState.get().blueLightColor = Color.parseColor(string);
                        if (onRefresh != null) {
                            onRefresh.run();
                        }
                        // Keyboards.hideNavigation(a);
                        return false;
                    }
                });

                final CustomSeek blueLightAlpha = (CustomSeek) inflate.findViewById(R.id.blueLightAlpha);
                blueLightAlpha.init(0, 99, blueLightAlpha());
                blueLightAlpha.setOnSeekChanged(new IntegerResponse() {

                    @Override
                    public boolean onResultRecive(int result) {
                        isEnableBlueFilter(true);
                        blueLightAlpha(result);
                        blueLightAlpha.setValueText("" + blueLightAlpha() + "%");
                        if (onRefresh != null) {
                            onRefresh.run();
                        }
                        return false;
                    }
                });
                blueLightAlpha.setValueText("" + blueLightAlpha() + "%");

                builder.setView(inflate);

                builder.setNegativeButton(R.string.close, new DialogInterface.OnClickListener() {

                    @Override
                    public void onClick(final DialogInterface dialog, final int id) {
                        isEnableBlueFilter.setChecked(isEnableBlueFilter());
                        if (dialog != null) {
                            dialog.dismiss();
                        }
                    }
                });

                AlertDialog dialog = builder.create();
                dialog.setOnDismissListener(new OnDismissListener() {

                    @Override
                    public void onDismiss(DialogInterface dialog) {
                        Keyboards.hideNavigation(a);
                    }
                });
                dialog.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
                dialog.show();
            }
        });

        // brightness
        final CheckBox autoSettings = (CheckBox) inflate.findViewById(R.id.autoSettings);

        final int systemBrigtnessInt = getSystemBrigtnessInt(a);

        final CustomSeek customBrightness = (CustomSeek) inflate.findViewById(R.id.customBrightness);

        int value = 0;
        if (appBrightness() == AppState.AUTO_BRIGTNESS) {
            value = systemBrigtnessInt;
        } else {
            value = isEnableBlueFilter() ? blueLightAlpha() * -1 : appBrightness();
        }

        customBrightness.init(-100, 100, value);

        customBrightness.setOnSeekChanged(new IntegerResponse() {

            @Override
            public boolean onResultRecive(int result) {
                customBrightness.setValueText("" + result);
                EventBus.getDefault().post(new MessegeBrightness(result));

                isEnableBlueFilter.setChecked(isEnableBlueFilter());
                return false;
            }
        });

        autoSettings.setOnCheckedChangeListener(new OnCheckedChangeListener() {

            @Override
            public void onCheckedChanged(final CompoundButton buttonView, final boolean isChecked) {
                if (!buttonView.isPressed()) {
                    return;
                }
                if (isChecked) {// auto
                    customBrightness.setEnabled(false);
                    customBrightness.reset(systemBrigtnessInt);
                    EventBus.getDefault().post(new MessegeBrightness(AppState.AUTO_BRIGTNESS));
                } else {
                    customBrightness.setEnabled(true);
                    EventBus.getDefault().post(new MessegeBrightness(systemBrigtnessInt));
                }

                isEnableBlueFilter.setChecked(isEnableBlueFilter());

            }
        });

        isEnableBlueFilter.setChecked(isEnableBlueFilter());
        autoSettings.setChecked(appBrightness() == AppState.AUTO_BRIGTNESS);
        customBrightness.setEnabled(appBrightness() != AppState.AUTO_BRIGTNESS);

    }

    public static int appBrightness() {
        return AppState.get().isDayNotInvert ? AppState.get().appBrightness : AppState.get().appBrightnessNight;
    }

    public static void appBrightness(int value) {
        if (AppState.get().isDayNotInvert) {
            AppState.get().appBrightness = value;
        } else {
            AppState.get().appBrightnessNight = value;
        }
    }

    public static boolean isEnableBlueFilter() {
        return AppState.get().isDayNotInvert ? AppState.get().isEnableBlueFilter : AppState.get().isEnableBlueFilterNight;
    }

    public static void isEnableBlueFilter(boolean value) {
        if (AppState.get().isDayNotInvert) {
            AppState.get().isEnableBlueFilter = value;
        } else {
            AppState.get().isEnableBlueFilterNight = value;
        }
    }


    public static int blueLightAlpha() {
        return AppState.get().isDayNotInvert ? AppState.get().blueLightAlpha : AppState.get().blueLightAlphaNight;
    }

    public static void blueLightAlpha(int value) {
        if (AppState.get().isDayNotInvert) {
            AppState.get().blueLightAlpha = value;
        } else {
            AppState.get().blueLightAlphaNight = value;
        }
    }


    public void updateCurrentValue() {
        if (isEnableBlueFilter()) {
            currentPercent = blueLightAlpha() * -1;
        } else {
            currentPercent = appBrightness() == AppState.AUTO_BRIGTNESS ? 0 : appBrightness();
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

        if ((isMovementStart || dy > Dips.DP_25) && event.getPointerCount() == 1 && x < BRIGHTNESS_WIDTH && dy > dx) {
            isMovementStart = true;

            if (y < Dips.screenHeight() / 3 || y > Dips.screenHeight() - Dips.screenHeight() / 3) {
                lastPercent = (int) (yDiff * 100 / MIN);
            } else {
                lastPercent = (int) (yDiff * 100 / MAX);
            }

            float plus = getMinMaxValue(lastPercent + currentPercent);
            EventBus.getDefault().post(new MessegeBrightness((int) plus));
        }
        return isMovementStart;

    }
}
