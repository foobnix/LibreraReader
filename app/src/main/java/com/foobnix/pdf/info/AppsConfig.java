package com.foobnix.pdf.info;

import android.content.Context;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Build;

import com.foobnix.android.utils.Apps;
import com.foobnix.android.utils.LOG;

import org.ebookdroid.droids.mupdf.codec.MuPdfDocument;

import java.util.Random;

public class AppsConfig {

    public static int MUPDF_1_11 = 111;
    public static int MUPDF_1_12 = 112;
    public static int MUPDF_VERSION = -1;

    public static final String PRO_LIBRERA_READER = "com.foobnix.pro.pdf.reader";
    public static final String LIBRERA_READER = "com.foobnix.pdf.reader";


    public static boolean IS_BETA;


    public static String TXT_APP_NAME;

    public static boolean isDOCXSupported = Build.VERSION.SDK_INT >= 26;

    static Random random = new Random();

    public static boolean isCloudsEnable = true;


    public static void init(final Context a) {


        MUPDF_VERSION = MuPdfDocument.getMupdfVersion();
        TXT_APP_NAME = Apps.getApplicationName(a);
        IS_BETA = TXT_APP_NAME.toLowerCase().contains("beta");
        LOG.d("IS_BETA", IS_BETA);
    }


    public static boolean checkIsProInstalled(final Context a) {
        if (a == null) {
            return false;
        }
        boolean is_pro = isPackageExisted(a, PRO_LIBRERA_READER);
        return is_pro;
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
