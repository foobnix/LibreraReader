package com.foobnix;

import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Environment;
import android.os.StrictMode;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.multidex.MultiDexApplication;
import androidx.work.Configuration;
import androidx.work.WorkManager;

import com.foobnix.android.utils.Dips;
import com.foobnix.android.utils.LOG;
import com.foobnix.android.utils.TxtUtils;
import com.foobnix.ext.CacheZipUtils;
import com.foobnix.hypen.HypenUtils;
import com.foobnix.pdf.info.ADS;
import com.foobnix.pdf.info.AppsConfig;
import com.foobnix.pdf.info.IMG;
import com.foobnix.pdf.info.Prefs;
import com.foobnix.pdf.info.TintUtil;
import com.foobnix.tts.TTSNotification;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.RequestConfiguration;
import com.google.android.gms.ads.initialization.InitializationStatus;
import com.google.android.gms.ads.initialization.OnInitializationCompleteListener;


public class LibreraApp extends MultiDexApplication {


    public static Context context;


    @Override
    public void onCreate() {
        if (false) {
            StrictMode.setThreadPolicy(new StrictMode.ThreadPolicy.Builder().detectDiskReads().detectDiskWrites().detectNetwork().penaltyLog().build());
            StrictMode.setVmPolicy(new StrictMode.VmPolicy.Builder().detectLeakedSqlLiteObjects().detectLeakedClosableObjects().penaltyLog().penaltyDeath().build());
        }
        super.onCreate();

        //AppsConfig.loadEngine(this);


        context = getApplicationContext();
        if (!WorkManager.isInitialized()) {
            WorkManager.initialize(this, new Configuration.Builder().setMinimumLoggingLevel(Log.DEBUG).build());
        }


//        FirebaseAnalytics analytics = FirebaseAnalytics.getInstance(this);
//        analytics.setUserProperty("APP_NAME", Apps.getApplicationName(this));
//        analytics.setUserProperty("APP_VERSION", Apps.getVersionName(this));


        AppsConfig.init(this);
        Dips.init(this);
        Prefs.get().init(this);

        try {
            if (!AppsConfig.checkIsProInstalled(this)) {
                MobileAds.initialize(this, new OnInitializationCompleteListener() {
                    @Override
                    public void onInitializationComplete(@NonNull InitializationStatus initializationStatus) {
                        LOG.d("ads-complete");

                    }
                });
            }
        } catch (Exception e) {
            LOG.e(e);
        }

        LOG.d("AppsConfig.IS_TEST_DEVICE", AppsConfig.IS_TEST_DEVICE);
        if (AppsConfig.IS_TEST_DEVICE) {
            RequestConfiguration configuration = new RequestConfiguration.Builder().setTestDeviceIds(AppsConfig.testDevices).build();
            MobileAds.setRequestConfiguration(configuration);
        }


        Log.d("Build", "Build.TestDeviceID :" + ADS.getByTestID(this));
        Log.d("Build", "Build.MODEL :" + Build.MODEL);
        Log.d("Build", "Build.DEVICE:" + Build.DEVICE);

        TTSNotification.initChannels(this);


        CacheZipUtils.init(this);

        IMG.init(this);

        LOG.d("Build", "Build.MANUFACTURER", Build.MANUFACTURER);
        LOG.d("Build", "Build.PRODUCT", Build.PRODUCT);
        LOG.d("Build", "Build.DEVICE", Build.DEVICE);
        LOG.d("Build", "Build.BRAND", Build.BRAND);
        LOG.d("Build", "Build.MODEL", Build.MODEL);
        LOG.d("Build", "Build.VERSION.SDK_INT", Build.VERSION.SDK_INT);

        LOG.d("Build", "Build.screenWidth", Dips.screenWidthDP(), Dips.screenWidth());

        LOG.d("Build.Context", "Context.getFilesDir()", getFilesDir());
        LOG.d("Build.Context", "Context.getCacheDir()", getCacheDir());
        LOG.d("Build.Context", "Context.getExternalCacheDir", getExternalCacheDir());
        LOG.d("Build.Context", "Context.getExternalFilesDir(null)", getExternalFilesDir(null));
        LOG.d("Build.Context", "Environment.getExternalStorageDirectory()", Environment.getExternalStorageDirectory());
        LOG.d("Build.Height", Dips.screenHeight());


        if (TxtUtils.isEmpty(AppsConfig.FLAVOR)) {
            throw new RuntimeException("Application not configured correctly!");
        }

        if (AppsConfig.IS_WRITE_LOGS) {
            LOG.writeCrashTofile = true;
            Thread.setDefaultUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
                @Override
                public void uncaughtException(Thread thread, final Throwable e) {
                    LOG.uncaughtException(e);

                    Intent intent = new Intent(Intent.ACTION_MAIN);
                    intent.addCategory(Intent.CATEGORY_HOME);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);

                    System.exit(0);

                }
            });
        }


    }


    @Override
    public void onLowMemory() {
        super.onLowMemory();
        LOG.d("AppState save onLowMemory");
        IMG.clearMemoryCache();
        TintUtil.clean();
        HypenUtils.cache.clear();
    }

    @Override
    public void onTrimMemory(int level) {
        super.onTrimMemory(level);
        LOG.d("onTrimMemory", level);
    }
}
