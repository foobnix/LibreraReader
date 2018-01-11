package org.ebookdroid;

import org.ebookdroid.common.bitmaps.BitmapManager;
import org.ebookdroid.common.cache.CacheManager;
import org.ebookdroid.common.settings.SettingsManager;

import com.artifex.mupdf.fitz.StructuredText;
import com.foobnix.android.utils.Dips;
import com.foobnix.android.utils.LOG;
import com.foobnix.ext.CacheZipUtils;
import com.foobnix.pdf.info.AppSharedPreferences;
import com.foobnix.pdf.info.AppsConfig;
import com.foobnix.pdf.info.ExtUtils;
import com.foobnix.pdf.info.IMG;
import com.foobnix.pdf.info.R;
import com.foobnix.pdf.info.TintUtil;
import com.foobnix.pdf.info.wrapper.AppState;
import com.foobnix.ui2.AppDB;

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

        int initNative = StructuredText.initNative();
        LOG.d("initNative", initNative);

        context = getApplicationContext();
        AppsConfig.init(getApplicationContext());
        Dips.init(context);
        AppDB.get().open(this);
        AppState.get().load(this);
        AppSharedPreferences.get().init(this);
        CacheZipUtils.init(context);
        ExtUtils.init(getApplicationContext());
        IMG.init(getApplicationContext());

        LOG.isEnable = getResources().getBoolean(R.bool.is_log_enable);

        TintUtil.init();

        SettingsManager.init(this);
        CacheManager.init(this);

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
