package com.foobnix.pdf.info;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Build;

import com.foobnix.android.utils.Apps;
import com.foobnix.android.utils.Dips;
import com.foobnix.android.utils.LOG;
import com.foobnix.model.AppState;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;

import org.ebookdroid.droids.mupdf.codec.MuPdfDocument;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AppsConfig {

    public static final String PRO_LIBRERA_READER = "com.foobnix.pro.pdf.reader";
    public static final String LIBRERA_READER = "com.foobnix.pdf.reader";
    public static final boolean ADS_ON_PAGE = false;
    public static final boolean IS_FDROID = BuildConfig.FLAVOR.equals("fdroid") || BuildConfig.FLAVOR.equals("huawei");
    public static final boolean IS_LOG = BuildConfig.FLAVOR.equals("alpha") || BuildConfig.FLAVOR.equals("beta");

    public static final boolean IS_PRO = BuildConfig.FLAVOR.equals("pro");
    public static final boolean IS_ENABLE_1_PAGE_SEARCH = true;
    public final static ExecutorService executorService = Executors.newFixedThreadPool(2);
    public final static String ENGINE_MuPDF_1_11 = "MuPDF_1.11";
    public final static String ENGINE_MuPDF_LATEST = "MuPDF_1.22.0";
    public static int MUPDF_VERSION = 0;
    public static int MUPDF_1_11 = 111;
    public static boolean isDOCXSupported = Build.VERSION.SDK_INT >= 26;
    public static boolean isCloudsEnable = false;
    public static boolean IS_NO_ADS = false;

    static {
        try {
            System.loadLibrary(ENGINE_MuPDF_LATEST);
        } catch (UnsatisfiedLinkError e) {
            LOG.e(e);
            try {
                System.loadLibrary(ENGINE_MuPDF_1_11);
            } catch (UnsatisfiedLinkError e1) {
                LOG.e(e1);
            }
        }
        AppsConfig.MUPDF_VERSION = MuPdfDocument.getMupdfVersion();
    }

    public static void loadEngine(Context c) {
        if (true) {
            return;
        }
        String engine = getCurrentEngine(c);
        try {
            engine = !ENGINE_MuPDF_1_11.equals(engine) ? ENGINE_MuPDF_LATEST : ENGINE_MuPDF_1_11;
            System.loadLibrary(engine);
            setEngine(c, engine);
        } catch (UnsatisfiedLinkError e) {
            try {
                System.loadLibrary(ENGINE_MuPDF_1_11);
                setEngine(c, ENGINE_MuPDF_1_11);
            } catch (UnsatisfiedLinkError e1) {
                System.loadLibrary(ENGINE_MuPDF_LATEST);
                setEngine(c, ENGINE_MuPDF_LATEST);
            }
            LOG.e(e);
        }


    }

    public static void setEngine(Context c, String engine) {
        LOG.d("setEngine", engine);
        SharedPreferences sp = c.getSharedPreferences("Engine", Context.MODE_PRIVATE);
        sp.edit().putString("version", engine).commit();
    }

    public static String getCurrentEngine(Context c) {
        return c.getSharedPreferences("Engine", Context.MODE_PRIVATE).getString("version", ENGINE_MuPDF_LATEST);
    }


    public static boolean isPDF_DRAW_ENABLE() {
        return true;
    }

    public static boolean checkIsProInstalled(final Context a) {
        if (IS_NO_ADS || IS_LOG) {
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
        if (Apps.isAccessibilityEnable(a)) {
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
