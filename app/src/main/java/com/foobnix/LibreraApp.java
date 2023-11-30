package com.foobnix;

import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Environment;
import android.os.StrictMode;

import androidx.annotation.NonNull;
import androidx.multidex.MultiDexApplication;

import com.foobnix.android.utils.Dips;
import com.foobnix.android.utils.LOG;
import com.foobnix.ext.CacheZipUtils;
import com.foobnix.pdf.info.AppsConfig;
import com.foobnix.pdf.info.BuildConfig;
import com.foobnix.pdf.info.IMG;
import com.foobnix.pdf.info.Prefs;
import com.foobnix.pdf.info.TintUtil;
import com.foobnix.tts.TTSNotification;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.initialization.InitializationStatus;
import com.google.android.gms.ads.initialization.OnInitializationCompleteListener;


public class LibreraApp extends MultiDexApplication {


    public static Context context;


    @Override
    public void onCreate() {
        if (false) {
            StrictMode.setThreadPolicy(new StrictMode.ThreadPolicy.Builder()
                    .detectDiskReads()
                    .detectDiskWrites()
                    .detectNetwork()
                    .penaltyLog()
                    .build());
            StrictMode.setVmPolicy(new StrictMode.VmPolicy.Builder()
                    .detectLeakedSqlLiteObjects()
                    .detectLeakedClosableObjects()
                    .penaltyLog()
                    .penaltyDeath()
                    .build());
        }
        super.onCreate();

        //AppsConfig.loadEngine(this);


        context = getApplicationContext();
        Dips.init(this);
        Prefs.get().init(this);

        try {
            if (!AppsConfig.checkIsProInstalled(this)) {
                //MobileAds.initialize(this, Apps.getMetaData(this, "com.google.android.gms.ads.APPLICATION_ID"));
                MobileAds.initialize(this, new OnInitializationCompleteListener() {
                    @Override
                    public void onInitializationComplete(@NonNull InitializationStatus initializationStatus) {
                        LOG.d("ads-complete");

                    }
                });
            }
        } catch (Exception e) {
            AppsConfig.IS_NO_ADS = true;
            LOG.e(e);
        }


        LOG.isEnable = BuildConfig.DEBUG || AppsConfig.IS_LOG;

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
    }

    @Override
    public void onTrimMemory(int level) {
        super.onTrimMemory(level);
        LOG.d("onTrimMemory", level);
    }
}
