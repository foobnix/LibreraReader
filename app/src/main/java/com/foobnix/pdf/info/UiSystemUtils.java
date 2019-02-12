package com.foobnix.pdf.info;

import java.sql.Date;

import android.app.Activity;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;

public class UiSystemUtils {

    public static String getSystemTime(Activity a) {
        final java.text.DateFormat dateFormat = android.text.format.DateFormat.getTimeFormat(a);
        return dateFormat.format(new Date(System.currentTimeMillis()));
    }

    public static int getPowerLevel(Activity a) {
        try {
            final Intent batteryIntent = a.registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
            final int level = batteryIntent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
            final int scale = batteryIntent.getIntExtra(BatteryManager.EXTRA_SCALE, -1);

            return level * 100 / scale;
        } catch (final Exception e) {
            return -1;
        }
    }
}
