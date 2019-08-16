package com.foobnix.pdf.info;

import android.content.Context;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Build;

import com.foobnix.android.utils.Dips;

public class AppsConfig {

    public static int MUPDF_1_11 = 111;
    public static int MUPDF_1_12 = 112;
    public static int MUPDF_VERSION = MUPDF_1_11;

    public static final String PRO_LIBRERA_READER = "com.foobnix.pro.pdf.reader";
    public static final String LIBRERA_READER = "com.foobnix.pdf.reader";


    public static boolean isDOCXSupported = Build.VERSION.SDK_INT >= 26;
    public static boolean isCloudsEnable = false;


    public static boolean checkIsProInstalled(final Context a) {
        if (a == null) {
            return false;
        }
        if (Build.VERSION.SDK_INT <= 16 || Dips.isEInk(a)) {
            //no ads for old android and eink
            return true;
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
