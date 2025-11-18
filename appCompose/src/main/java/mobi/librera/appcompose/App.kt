package mobi.librera.appcompose

import android.annotation.SuppressLint
import android.app.Application
import android.content.Context
import android.os.Build
import android.provider.Settings
import coil3.ImageLoader
import coil3.SingletonImageLoader
import com.google.firebase.FirebaseApp
import mobi.librera.appcompose.di.initKoin
import mobi.librera.appcompose.imageloader.AppImageLoader
import org.koin.android.ext.koin.androidContext
import org.koin.core.component.KoinComponent
import org.koin.core.component.get

class App : Application(), KoinComponent, SingletonImageLoader.Factory {

    @SuppressLint("HardwareIds")
    override fun onCreate() {
        super.onCreate()
        val androidId = Settings.Secure.getString(
            this.contentResolver,
            Settings.Secure.ANDROID_ID
        )
        DEVICE_ID = androidId + "_" + Build.VERSION.SDK_INT
        println("ANDROID_ID: $androidId")


        AppImageLoader.initialize(this)
        FirebaseApp.initializeApp(this)
        initKoin {
            androidContext(this@App)
        }


    }

    override fun newImageLoader(context: Context): ImageLoader {
        return get<ImageLoader>()
    }

    companion object {
        lateinit var DEVICE_ID: String
    }
}