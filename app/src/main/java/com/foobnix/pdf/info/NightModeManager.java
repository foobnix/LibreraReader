package com.foobnix.pdf.info;

import android.content.Context;
import android.content.SharedPreferences;

public class NightModeManager {

    private static final String PREFS_NAME = "NightModePrefs";
    private static final String NIGHT_MODE_ENABLED = "NightModeEnabled";
    private static final String FONT_SIZE = "FontSize";
    private static final String BACKGROUND_COLOR = "BackgroundColor";

    private SharedPreferences sharedPreferences;

    public NightModeManager(Context context) {
        sharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    public void enableNightMode(boolean enable) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean(NIGHT_MODE_ENABLED, enable);
        editor.apply();
    }

    public boolean isNightModeEnabled() {
        return sharedPreferences.getBoolean(NIGHT_MODE_ENABLED, false);
    }

    public void setFontSize(int size) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putInt(FONT_SIZE, size);
        editor.apply();
    }

    public int getFontSize() {
        return sharedPreferences.getInt(FONT_SIZE, 16);
    }

    public void setBackgroundColor(int color) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putInt(BACKGROUND_COLOR, color);
        editor.apply();
    }

    public int getBackgroundColor() {
        return sharedPreferences.getInt(BACKGROUND_COLOR, 0xFFFFFF);
    }
}
