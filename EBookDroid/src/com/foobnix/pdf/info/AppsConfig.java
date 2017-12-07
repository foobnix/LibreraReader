package com.foobnix.pdf.info;

import com.foobnix.android.utils.LOG;

import android.content.Context;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;

public class AppsConfig {

    public static final String PRO_PDF_READER = "com.foobnix.pro.pdf.reader";

    public static final String PDF_READER_LIRBI = "com.foobnix.pdf.reader";

    public static final String CLASSIC_READER_PKG = "classic.pdf.reader.viewer.djvu.epub.fb2.txt.mobi.book.reader.lirbi.libri";

    public static final String DROID_READER_PKG = "droid.reader.book.epub.mobi.pdf.djvu.fb2.txt.azw.azw3";

    public static final String LIBRERA_INK = "mobi.librera.book.reader";

    public static String ADMOB_CLASSIC;

    public static String ADMOB_FULLSCREEN;
    public static String ADMOB_NATIVE_BIG;
    public static String ADMOB_NATIVE_SMALL;

    public static String ANALYTICS_ID;

    public static boolean IS_APP_WITH_ANALITICS = true;

    public static String APP_NAME;
    public static String APP_PACKAGE;
    public static boolean IS_BETA, IS_CLASSIC, IS_INK, IS_EP;

    public static boolean IS_TEST_KEY_EP = false;

    public static void init(final Context a) {
        final String packageName = a.getPackageName();
        LOG.d("init packageName", packageName);

        APP_NAME = a.getString(R.string.app_name);
        APP_PACKAGE = packageName;

        IS_APP_WITH_ANALITICS = true;

        IS_EP = false;

        if (PRO_PDF_READER.equals(packageName)) {
            ADMOB_CLASSIC = null;
            ANALYTICS_ID = null;
            IS_APP_WITH_ANALITICS = false;
            return;
        }

        if (PDF_READER_LIRBI.equals(packageName)) {
            ANALYTICS_ID = "UA-36581296-2";

            ADMOB_CLASSIC/*     */ = "ca-app-pub-8347903083053959/4166397275";
            ADMOB_FULLSCREEN/*  */ = "ca-app-pub-8347903083053959/2769081274";
            ADMOB_NATIVE_BIG/*  */ = "ca-app-pub-8347903083053959/4245814471";
            ADMOB_NATIVE_SMALL/**/ = "ca-app-pub-8347903083053959/5722547677";
            
            IS_EP = false;
        }

        if (CLASSIC_READER_PKG.equals(packageName)) {
            IS_CLASSIC = true;
            ANALYTICS_ID = "UA-36581296-6";

            ADMOB_CLASSIC/*     */ = "ca-app-pub-8347903083053959/5364245672";
            ADMOB_FULLSCREEN/*  */ = "ca-app-pub-8347903083053959/7763820878";
            ADMOB_NATIVE_BIG/*  */ = "ca-app-pub-8347903083053959/8961352478";
            ADMOB_NATIVE_SMALL/**/ = "ca-app-pub-8347903083053959/8572902871";
        }

        if (DROID_READER_PKG.equals(packageName)) {
            ANALYTICS_ID = "UA-36581296-8";
            ADMOB_CLASSIC/*     */ = "ca-app-pub-8347903083053959/5364245672";
            ADMOB_FULLSCREEN/*  */ = "ca-app-pub-8347903083053959/7763820878";
            ADMOB_NATIVE_BIG/*  */ = "ca-app-pub-8347903083053959/8961352478";
            ADMOB_NATIVE_SMALL/**/ = "ca-app-pub-8347903083053959/8572902871";

            IS_EP = true;
        }
        if (LIBRERA_INK.equals(packageName)) {
            IS_INK = true;
            ANALYTICS_ID = "UA-36581296-8";

            ADMOB_CLASSIC/*     */ = "ca-app-pub-8347903083053959/5364245672";
            ADMOB_FULLSCREEN/*  */ = null;
            ADMOB_NATIVE_BIG/*  */ = "ca-app-pub-8347903083053959/8961352478";
            ADMOB_NATIVE_SMALL/**/ = "ca-app-pub-8347903083053959/8572902871";

            ADMOB_CLASSIC = null;
            ANALYTICS_ID = null;
            IS_APP_WITH_ANALITICS = false;

            IS_EP = true;
        }

        IS_BETA = APP_NAME.contains("Beta");


        IS_BETA = false;

        if (IS_BETA) {
            ANALYTICS_ID = "UA-36581296-9";
            ADMOB_CLASSIC = ADMOB_FULLSCREEN = ADMOB_NATIVE_BIG = ADMOB_NATIVE_SMALL = null;
        }


    }

    public static boolean isDroidReaderPkg(Context c) {
        final String packageName = c.getPackageName();
        return DROID_READER_PKG.equals(packageName);

    }

    public static boolean checkIsProInstalled(final Context a) {
        if (a == null) {
            return false;
        }
        boolean is_pro = isPackageExisted(a, PRO_PDF_READER);
        if (is_pro) {
            ADMOB_CLASSIC = null;
            ADMOB_FULLSCREEN = null;
            ADMOB_NATIVE_BIG = null;
            ADMOB_NATIVE_SMALL = null;
        }
        return is_pro || IS_BETA;
    }

    public static boolean isPackageExisted(final Context a, final String targetPackage) {
        try {
            final PackageManager pm = a.getPackageManager();
            pm.getPackageInfo(targetPackage, PackageManager.GET_META_DATA);
        } catch (final NameNotFoundException e) {
            return false;
        }
        return true;
    }

}
