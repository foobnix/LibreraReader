package com.foobnix.ui2;

import java.util.Locale;

import com.foobnix.android.utils.LOG;
import com.foobnix.pdf.info.wrapper.AppState;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.res.Configuration;
import android.os.Build;

public class MyContextWrapper extends ContextWrapper {

    public MyContextWrapper(Context base) {
        super(base);
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
    @SuppressWarnings("deprecation")
    public static ContextWrapper wrap(Context context) {
        AppState.get().load(context);
        if (AppState.MY_SYSTEM_LANG.equals(AppState.get().appLang)) {
            LOG.d("MyContextWrapper skip");
            return new MyContextWrapper(context);
        }

        final String language = AppState.get().appLang;
        final float scale = AppState.get().appFontScale;

        Configuration config = context.getResources().getConfiguration();
        Locale locale = new Locale(language);

        LOG.d("MyContextWrapper changed");

        config.setLocale(locale);
        config.fontScale = scale;

        Locale.setDefault(locale);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            setSystemLocale(config, locale);
        } else {
            setSystemLocaleLegacy(config, locale);
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            context = context.createConfigurationContext(config);
        } else {
            context.getResources().updateConfiguration(config, context.getResources().getDisplayMetrics());
        }
        return new MyContextWrapper(context);
    }

    @SuppressWarnings("deprecation")
    public static Locale getSystemLocaleLegacy(Configuration config) {
        return config.locale;
    }

    @TargetApi(Build.VERSION_CODES.N)
    public static Locale getSystemLocale(Configuration config) {
        return config.getLocales().get(0);
    }

    @SuppressWarnings("deprecation")
    public static void setSystemLocaleLegacy(Configuration config, Locale locale) {
        config.locale = locale;
    }

    @TargetApi(Build.VERSION_CODES.N)
    public static void setSystemLocale(Configuration config, Locale locale) {
        config.setLocale(locale);
    }
}