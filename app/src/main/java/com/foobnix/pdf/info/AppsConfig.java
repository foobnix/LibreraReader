package com.foobnix.pdf.info;

import android.content.Context;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Build;

import com.foobnix.android.utils.Apps;
import com.foobnix.android.utils.Dips;
import com.foobnix.android.utils.LOG;
import com.foobnix.model.AppState;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;

import org.ebookdroid.LibreraApp;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AppsConfig {

    public static final String PRO_LIBRERA_READER = "com.foobnix.pro.pdf.reader";
    public static final String LIBRERA_READER = "com.foobnix.pdf.reader";
    public static final boolean ADS_ON_PAGE = false;
    public static int MUPDF_1_11 = 111;
    public static int MUPDF_1_16 = 116;
    public static int MUPDF_MASTER = 0;
    public static boolean isDOCXSupported = Build.VERSION.SDK_INT >= 26;
    public static boolean isCloudsEnable = false;

    public static final boolean IS_FDROID = BuildConfig.FLAVOR.equals("fdroid") || BuildConfig.FLAVOR.equals("huawei");
    public static final boolean IS_BETA = BuildConfig.FLAVOR.equals("beta");
    public static final boolean IS_LOG = BuildConfig.FLAVOR.equals("alpha") || BuildConfig.FLAVOR.equals("beta");
    public static boolean IS_NO_ADS = false;

    public final static ExecutorService executorService = Executors.newFixedThreadPool(2);


    public static boolean isPDF_DRAW_ENABLE() {
        return LibreraApp.MUPDF_VERSION == MUPDF_1_11;
    }

    public static boolean checkIsProInstalled(final Context a) {
        if (IS_NO_ADS) {
            LOG.d("no-ads error");

            return true;
        }

        if (a == null) {
            LOG.d("no-ads error context null");
            return true;
        }
        if (!isGooglePlayServicesAvailable(a)) {
            //no ads for old android and eink
            LOG.d("no-ads isGooglePlayServicesAvailable not available");
            return true;
        }
        if (Build.VERSION.SDK_INT <= 16 || Dips.isEInk()) {
            LOG.d("no-ads old device or eink");
            //no ads for old android and eink
            return true;
        }
        if (AppState.get().isEnableAccessibility) {
            return true;
        }

        boolean is_pro = isPackageExisted(a, PRO_LIBRERA_READER);
        return is_pro;
    }

    public static boolean isGooglePlayServicesAvailable(Context context) {
        GoogleApiAvailability googleApiAvailability = GoogleApiAvailability.getInstance();
        int resultCode = googleApiAvailability.isGooglePlayServicesAvailable(context);
        return resultCode == ConnectionResult.SUCCESS;
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
