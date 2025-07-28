package mobi.librera.appcompose

import android.app.Application
import android.content.Context
import coil3.ImageLoader
import coil3.SingletonImageLoader
import com.google.firebase.FirebaseApp
import mobi.librera.appcompose.di.initKoin
import mobi.librera.appcompose.imageloader.AppImageLoader
import org.koin.android.ext.koin.androidContext
import org.koin.core.component.KoinComponent
import org.koin.core.component.get

class App : Application(), KoinComponent, SingletonImageLoader.Factory {
    override fun onCreate() {
        super.onCreate()
        AppImageLoader.initialize(this)
        FirebaseApp.initializeApp(this)
        initKoin {
            androidContext(this@App)
        }
    }

    override fun newImageLoader(context: Context): ImageLoader {
        return get<ImageLoader>()
    }
}