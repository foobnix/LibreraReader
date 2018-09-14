package org.ebookdroid;

import java.io.PrintWriter;
import java.io.StringWriter;

import org.ebookdroid.common.bitmaps.BitmapManager;
import org.ebookdroid.common.cache.CacheManager;
import org.ebookdroid.common.settings.SettingsManager;

import com.artifex.mupdf.fitz.StructuredText;
import com.foobnix.android.utils.Apps;
import com.foobnix.android.utils.Dips;
import com.foobnix.android.utils.LOG;
import com.foobnix.ext.CacheZipUtils;
import com.foobnix.pdf.info.ADS;
import com.foobnix.pdf.info.AppSharedPreferences;
import com.foobnix.pdf.info.AppsConfig;
import com.foobnix.pdf.info.Clouds;
import com.foobnix.pdf.info.ExtUtils;
import com.foobnix.pdf.info.IMG;
import com.foobnix.pdf.info.R;
import com.foobnix.pdf.info.TintUtil;
import com.foobnix.pdf.info.wrapper.AppState;
import com.foobnix.tts.TTSNotification;
import com.foobnix.ui2.AppDB;
import com.google.android.gms.ads.AdRequest;

import android.app.Application;
import android.content.Context;
import android.os.Build;
import android.os.Environment;

public class LibreraApp extends Application {

    static {
        System.loadLibrary("mypdf");
        System.loadLibrary("mobi");
    }

    public static Context context;

    @Override
    public void onCreate() {
        super.onCreate();

        context = getApplicationContext();

        LOG.isEnable = getResources().getBoolean(R.bool.is_log_enable);

        AppsConfig.init(this);

        if (AppsConfig.MUPDF_VERSION == AppsConfig.MUPDF_1_12) {
            int initNative = StructuredText.initNative();
            LOG.d("initNative", initNative);
        }

        TTSNotification.initChannels(this);
        Dips.init(this);
        AppDB.get().open(this);
        AppState.get().load(this);
        AppSharedPreferences.get().init(this);
        CacheManager.init(this);
        CacheZipUtils.init(this);
        ExtUtils.init(this);
        IMG.init(this);

        TintUtil.init();

        SettingsManager.init(this);

        Clouds.get().init(this);

        LOG.d("Build", "Build.MANUFACTURER", Build.MANUFACTURER);
        LOG.d("Build", "Build.PRODUCT", Build.PRODUCT);
        LOG.d("Build", "Build.DEVICE", Build.DEVICE);
        LOG.d("Build", "Build.BRAND", Build.BRAND);
        LOG.d("Build", "Build.MODEL", Build.MODEL);

        LOG.d("Build.Context", "Context.getFilesDir()", getFilesDir());
        LOG.d("Build.Context", "Context.getCacheDir()", getCacheDir());
        LOG.d("Build.Context", "Context.getExternalCacheDir", getExternalCacheDir());
        LOG.d("Build.Context", "Context.getExternalFilesDir(null)", getExternalFilesDir(null));
        LOG.d("Build.Context", "Environment.getExternalStorageDirectory()", Environment.getExternalStorageDirectory());
        LOG.d("Build.Height", Dips.screenHeight());

        try {
            if (LOG.isEnable) {
                String myID = ADS.getByTestID(this);
                ADS.adRequest = new AdRequest.Builder()//
                        .addTestDevice(AdRequest.DEVICE_ID_EMULATOR)//
                        // .addTestDevice(myID)//
                        .build();//
            }
        } catch (Exception e) {
            LOG.e(e);
        }


        if (AppsConfig.IS_BETA && !LOG.isEnable) {
            Thread.setDefaultUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
                @Override
                public void uncaughtException(Thread thread, final Throwable e) {
                    LOG.e(e);
                    e.printStackTrace();
                    if (e instanceof android.database.sqlite.SQLiteException) {
                        LOG.d("Drop-databases1");
                        AppDB.get().dropCreateTables(LibreraApp.this);
                        LOG.d("Drop-databases2");
                    }
                    try {

                        StringWriter errors = new StringWriter();
                        e.printStackTrace(new PrintWriter(errors));
                        String log = errors.toString();
                        log = log + "/n";
                        log = log + Build.MANUFACTURER + "/n";
                        log = log + Build.PRODUCT + "/n";
                        log = log + Build.DEVICE + "/n";
                        log = log + Build.BRAND + "/n";
                        log = log + Build.BRAND + "/n";
                        log = log + Build.MODEL + "/n";
                        log = log + Build.VERSION.SDK_INT + "/n";
                        Apps.onCrashEmail(context, log, AppsConfig.TXT_APP_NAME + " " + context.getString(R.string.application_error_please_send_this_report_by_emial));

                        System.exit(1);

                    } catch (Exception e1) {
                        LOG.e(e1);
                    }
                }
            });
        }

    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        LOG.d("AppState save onLowMemory");
        IMG.clearMemoryCache();
        BitmapManager.clear("on Low Memory: ");
        TintUtil.clean();
    }

}
