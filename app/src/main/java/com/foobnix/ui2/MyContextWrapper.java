package com.foobnix.ui2;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Build;
import android.os.LocaleList;

import com.foobnix.android.utils.LOG;
import com.foobnix.model.AppProfile;
import com.foobnix.model.AppState;
import com.foobnix.pdf.info.Urls;
import com.foobnix.pdf.info.model.BookCSS;
import com.foobnix.pdf.info.widget.DialogTranslateFromTo;

import java.util.Locale;

public class MyContextWrapper {



    @TargetApi(24)
    public static ContextWrapper wrap(Context context) {

        AppProfile.init(context);

        if (AppState.MY_SYSTEM_LANG.equals(AppState.get().appLang) && BookCSS.get().appFontScale == 1.0f) {
            LOG.d("ContextWrapper skip");
            return new ContextWrapper(context);
        }

        String language = AppState.get().appLang;
        final float scale = BookCSS.get().appFontScale;

        Resources res = context.getResources();
        Configuration configuration = res.getConfiguration();
        configuration.fontScale = scale;

        if (AppState.MY_SYSTEM_LANG.equals(AppState.get().appLang)) {
            language = Urls.getLangCode();
        }

        LOG.d("ContextWrapper language", language);

        Locale newLocale = new Locale(language);

        if (language.equals("zh")) {
            newLocale = Locale.getDefault();
        } else if (language.equals(DialogTranslateFromTo.CHINESE_SIMPLE)) {
            newLocale = Locale.SIMPLIFIED_CHINESE;
        } else if (language.equals(DialogTranslateFromTo.CHINESE_TRADITIOANAL)) {
            newLocale = Locale.TRADITIONAL_CHINESE;
        }

        LOG.d("ContextWrapper newLocale", newLocale.getDisplayName(), newLocale.getCountry());

        if (Build.VERSION.SDK_INT >= 24) {
            configuration.setLocale(newLocale);
            LocaleList localeList = new LocaleList(newLocale);
            LocaleList.setDefault(localeList);
            configuration.setLocales(localeList);

            context = context.createConfigurationContext(configuration);

        } else if (Build.VERSION.SDK_INT >= 17) {
            configuration.setLocale(newLocale);
            context = context.createConfigurationContext(configuration);
        } else {
            configuration.locale = newLocale;
            res.updateConfiguration(configuration, res.getDisplayMetrics());
        }

        return new ContextWrapper(context);
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