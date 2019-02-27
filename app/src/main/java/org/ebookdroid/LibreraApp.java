package org.ebookdroid;

import android.content.Context;
import android.os.Build;
import android.os.Environment;
import android.support.multidex.MultiDexApplication;

import com.artifex.mupdf.fitz.StructuredText;
import com.foobnix.android.utils.Apps;
import com.foobnix.android.utils.Dips;
import com.foobnix.android.utils.LOG;
import com.foobnix.ext.CacheZipUtils;
import com.foobnix.pdf.info.ADS;
import com.foobnix.pdf.info.AppSharedPreferences;
import com.foobnix.pdf.info.AppsConfig;
import com.foobnix.pdf.info.BuildConfig;
import com.foobnix.pdf.info.Clouds;
import com.foobnix.pdf.info.ExtUtils;
import com.foobnix.pdf.info.IMG;
import com.foobnix.pdf.info.TintUtil;
import com.foobnix.pdf.info.wrapper.AppState;
import com.foobnix.tts.TTSNotification;
import com.foobnix.ui2.AppDB;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.MobileAds;

import org.ebookdroid.common.bitmaps.BitmapManager;
import org.ebookdroid.common.settings.SettingsManager;

public class LibreraApp extends MultiDexApplication {

    static {
        System.loadLibrary("mypdf");
        System.loadLibrary("mobi");
        System.loadLibrary("antiword");
    }

    public static Context context;

    @Override
    public void onCreate() {
        super.onCreate();


        MobileAds.initialize(this, Apps.getMetaData(this,"com.google.android.gms.ads.APPLICATION_ID"));

        context = getApplicationContext();

        LOG.isEnable = BuildConfig.LOG;

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

        LOG.d("Build", "Build.screenWidth", Dips.screenWidthDP(), Dips.screenWidth());

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
                        .addTestDevice(myID)//
                        .build();//
            }
        } catch (Exception e) {
            LOG.e(e);
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
