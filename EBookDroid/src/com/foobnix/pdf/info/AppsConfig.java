package com.foobnix.pdf.info;

import java.util.Random;

import com.foobnix.android.utils.LOG;

import android.content.Context;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;

public class AppsConfig {

    public static final String PRO_LIBRERA_READER = "com.foobnix.pro.pdf.reader";

    public static final String LIBRERA_READER = "com.foobnix.pdf.reader";

    public static final String CLASSIC_PDF_READER = "classic.pdf.reader.viewer.djvu.epub.fb2.txt.mobi.book.reader.lirbi.libri";

    public static final String EBOOKA_READER = "droid.reader.book.epub.mobi.pdf.djvu.fb2.txt.azw.azw3";

    public static final String LIBRERA_INK_EDITION = "mobi.librera.book.reader";

    public static String ADMOB_BANNER;
    public static String ADMOB_FULLSCREEN;
    public static String ADMOB_NATIVE_BANNER;

    public static String EP_BANNER_NATIVE;
    public static String EP_INTERSTITIAL;

    public static String ANALYTICS_ID;


    public static String APP_NAME;
    public static String APP_PACKAGE;
    public static boolean IS_BETA, IS_CLASSIC, IS_INK, IS_EP;

    public static boolean IS_TEST_EP = false;

    static Random random = new Random();

    public static void init(final Context a) {
        final String packageName = a.getPackageName();
        LOG.d("init packageName", packageName);

        APP_NAME = a.getString(R.string.app_name);
        APP_PACKAGE = packageName;


        IS_EP = false;

        if (PRO_LIBRERA_READER.equals(packageName)) {
            ADMOB_BANNER = null;
            ANALYTICS_ID = null;
            IS_EP = false;
            return;
        }

        if (LIBRERA_READER.equals(packageName)) {
            ANALYTICS_ID = "UA-36581296-2";

            ADMOB_BANNER/*     */ = "ca-app-pub-8347903083053959/4166397275";
            ADMOB_FULLSCREEN/*  */ = "ca-app-pub-8347903083053959/2769081274";
            ADMOB_NATIVE_BANNER/**/ = "ca-app-pub-8347903083053959/5722547677";
            
            EP_BANNER_NATIVE = "9cf064256b16a112cc1fd3fb42487dbd";
            EP_INTERSTITIAL = "cd6563264b30c32814df5f0e1048079b";

            IS_EP = false;
        }

        if (CLASSIC_PDF_READER.equals(packageName)) {
            IS_CLASSIC = true;
            ANALYTICS_ID = "UA-36581296-6";

            ADMOB_BANNER/*       */ = "ca-app-pub-8347903083053959/5364245672";
            ADMOB_FULLSCREEN/*   */ = "ca-app-pub-8347903083053959/7763820878";
            ADMOB_NATIVE_BANNER/**/ = "ca-app-pub-8347903083053959/8572902871";

            EP_BANNER_NATIVE = "45cb427bedf4118fd6983475ce7cfb3e";
            EP_INTERSTITIAL = "c6d71b0cf97d5ca37764b26e8f365cf1";

            IS_EP = false;
        }

        if (EBOOKA_READER.equals(packageName)) {
            ANALYTICS_ID = "UA-36581296-8";
            ADMOB_BANNER/*     */ = "ca-app-pub-8347903083053959/6159730856";
            ADMOB_FULLSCREEN/* */ = "ca-app-pub-8347903083053959/2346153515";
            ADMOB_NATIVE_BANNER/**/ = null;

            EP_BANNER_NATIVE = "ec263d6b75792d1e566a53f78b297cfc";
            EP_INTERSTITIAL = "ff99040c9b6a825dbc8dfb56a8834225";

            // IS_EP = random.nextBoolean();
            IS_EP = false;
        }
        if (LIBRERA_INK_EDITION.equals(packageName)) {
            IS_INK = true;
            ANALYTICS_ID = "UA-36581296-8";

            ADMOB_BANNER/*     */ = "ca-app-pub-8347903083053959/5364245672";
            ADMOB_FULLSCREEN/*  */ = null;
            ADMOB_NATIVE_BANNER/**/ = "ca-app-pub-8347903083053959/8572902871";

            IS_EP = false;
        }

        IS_BETA = APP_NAME.contains("Beta");

        IS_BETA = false;

        if (IS_BETA) {
            IS_EP = false;
            ANALYTICS_ID = "UA-36581296-9";
            ADMOB_BANNER = ADMOB_FULLSCREEN = ADMOB_NATIVE_BANNER = null;
            LOG.isEnable = true;
        }
        if (IS_TEST_EP) {
            EP_INTERSTITIAL = "0928de1630a1452b64eaab1813d3af64";
            EP_BANNER_NATIVE = "ec5086312cf4959dcc54fe8a8ad15401";
        }


    }

    public static boolean isDroidReaderPkg(Context c) {
        final String packageName = c.getPackageName();
        return EBOOKA_READER.equals(packageName);

    }

    public static boolean checkIsProInstalled(final Context a) {
        if (a == null) {
            return false;
        }
        boolean is_pro = isPackageExisted(a, PRO_LIBRERA_READER);
        if (is_pro) {
            ADMOB_BANNER = null;
            ADMOB_FULLSCREEN = null;
            ADMOB_NATIVE_BANNER = null;
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
