package com.foobnix.pdf.info;

import java.util.Random;

import org.ebookdroid.droids.mupdf.codec.MuPdfDocument;

import com.foobnix.android.utils.LOG;

import android.content.Context;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;

public class AppsConfig {

    public static int MUPDF_1_11 = 111;
    public static int MUPDF_1_12 = 112;
    public static int MUPDF_VERSION = -1;

    public static final String PRO_LIBRERA_READER = "com.foobnix.pro.pdf.reader";

    public static final String LIBRERA_READER = "com.foobnix.pdf.reader";

    public static final String CLASSIC_PDF_READER = "classic.pdf.reader.viewer.djvu.epub.fb2.txt.mobi.book.reader.lirbi.libri";

    public static final String EBOOKA_READER = "droid.reader.book.epub.mobi.pdf.djvu.fb2.txt.azw.azw3";

    public static final String LIBRERA_INK_EDITION = "mobi.librera.book.reader";

    public static String ADMOB_BANNER;
    public static String ADMOB_FULLSCREEN;
    public static String ADMOB_NATIVE_BANNER;

    public static String ANALYTICS_ID;

    public static String GOOGLE_DRIVE_KEY;

    public static String APP_PACKAGE;
    public static boolean IS_BETA, IS_CLASSIC, IS_INK;


    public static String TXT_APP_NAME;

    static Random random = new Random();

    public static boolean isCloudsEnable = true;


    public static void init(final Context a) {


        MUPDF_VERSION = MuPdfDocument.getMupdfVersion();
        final String packageName = a.getPackageName();
        LOG.d("init packageName", packageName);

        TXT_APP_NAME = a.getString(R.string.app_name);

        APP_PACKAGE = packageName;

        if (PRO_LIBRERA_READER.equals(packageName)) {
            ADMOB_BANNER = null;
            ANALYTICS_ID = null;
            GOOGLE_DRIVE_KEY = "961762082517-ej9mdc7bgp7jkd6hvbfimvda6vpi5p4t.apps.googleusercontent.com";
            return;
        }

        if (LIBRERA_READER.equals(packageName)) {
            ANALYTICS_ID = "UA-36581296-2";

            ADMOB_BANNER/*     */ = "ca-app-pub-8347903083053959/4166397275";
            ADMOB_FULLSCREEN/*  */ = "ca-app-pub-8347903083053959/2769081274";
            ADMOB_NATIVE_BANNER/**/ = "ca-app-pub-8347903083053959/5722547677";

            GOOGLE_DRIVE_KEY = "961762082517-dgsen03hb73s6oh59ovivcansatu16lc.apps.googleusercontent.com";

        }

        if (CLASSIC_PDF_READER.equals(packageName)) {
            IS_CLASSIC = true;
            ANALYTICS_ID = "UA-36581296-6";

            ADMOB_BANNER/*       */ = "ca-app-pub-8347903083053959/5364245672";
            ADMOB_FULLSCREEN/*   */ = "ca-app-pub-8347903083053959/7763820878";
            ADMOB_NATIVE_BANNER/**/ = "ca-app-pub-8347903083053959/8572902871";


            GOOGLE_DRIVE_KEY = "961762082517-5825mr1t8duo7tlnkdr6itersmlreejq.apps.googleusercontent.com";

        }

        if (EBOOKA_READER.equals(packageName)) {
            ANALYTICS_ID = "UA-36581296-8";
            ADMOB_BANNER/*     */ = "ca-app-pub-8347903083053959/6159730856";
            ADMOB_FULLSCREEN/* */ = "ca-app-pub-8347903083053959/2346153515";
            ADMOB_NATIVE_BANNER/**/ = null;

            GOOGLE_DRIVE_KEY = "961762082517-3ud6u3hmjlbbpqdsqtk9md55o5jgkg41.apps.googleusercontent.com";

        }
        if (LIBRERA_INK_EDITION.equals(packageName)) {
            IS_INK = true;
            ANALYTICS_ID = "UA-36581296-8";

            ADMOB_BANNER/*     */ = "ca-app-pub-8347903083053959/5364245672";
            ADMOB_FULLSCREEN/*  */ = null;
            ADMOB_NATIVE_BANNER/**/ = "ca-app-pub-8347903083053959/8572902871";

            GOOGLE_DRIVE_KEY = "961762082517-4heqqjfki9n6og2mrltb260l1q8pfmdq.apps.googleusercontent.com";

        }

        IS_BETA = TXT_APP_NAME.contains("Beta");

        // IS_BETA = false;

        if (IS_BETA) {
            ANALYTICS_ID = "UA-36581296-9";
            ADMOB_BANNER = ADMOB_FULLSCREEN = ADMOB_NATIVE_BANNER = null;
            // LOG.isEnable = true;
        }
        if (LOG.isEnable) {
            GOOGLE_DRIVE_KEY = "961762082517-d2jro57s97ck73grvknbfnio8cg5fca3.apps.googleusercontent.com";
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
