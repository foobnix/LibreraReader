package org.ebookdroid;

import java.lang.reflect.Method;

import org.ebookdroid.common.bitmaps.BitmapManager;
import org.ebookdroid.common.cache.CacheManager;
import org.ebookdroid.common.settings.SettingsManager;

import com.foobnix.android.utils.Dips;
import com.foobnix.android.utils.LOG;
import com.foobnix.android.utils.MemoryUtils;
import com.foobnix.ext.CacheZipUtils;
import com.foobnix.pdf.info.Analytics;
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
import android.os.StrictMode;

public class EBookDroidApp extends Application {

    static {
        System.loadLibrary("mypdf");
        System.loadLibrary("mobi");
    }

    public static Context context;

    @Override
    public void onCreate() {
        super.onCreate();

        // StrictMode.VmPolicy.Builder builder = new
        // StrictMode.VmPolicy.Builder();
        // StrictMode.setVmPolicy(builder.build());

        if (Build.VERSION.SDK_INT >= 24) {
            try {
                String string = "DeathOn";
                String string2 = "Exposure";
                String string3 = "FileUri";
                Method m = StrictMode.class.getMethod("disable" + string + string3 + string2);
                m.invoke(null);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        context = getApplicationContext();
        Dips.init(context);
        AppDB.get().open(this);
        AppState.getInstance().load(this);
        AppSharedPreferences.get().init(this);
        CacheZipUtils.init(context);
        ExtUtils.init(getApplicationContext());
        IMG.init(getApplicationContext());
        AppsConfig.init(getApplicationContext());
        LOG.isEnable = getResources().getBoolean(R.bool.is_log_enable);

        TintUtil.init();

        SettingsManager.init(this);
        CacheManager.init(this);
        Analytics.setContext(this);

        LOG.d("MEMORY SIZE", "isBig:", MemoryUtils.IS_BIG_MEMORY_SIZE, "is Small: " + MemoryUtils.IS_SMALL_MEMORY_SIZE, "Size: ", MemoryUtils.RECOMENDED_MEMORY_SIZE);

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
