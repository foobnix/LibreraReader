package mobi.librera.appcompose

import android.app.Application
import android.content.Context
import coil3.ImageLoader
import coil3.SingletonImageLoader
import mobi.librera.appcompose.di.initKoin
import org.koin.android.ext.koin.androidContext
import org.koin.core.component.KoinComponent
import org.koin.core.component.get

class CustomApplication : Application(), KoinComponent, SingletonImageLoader.Factory {
    override fun onCreate() {
        super.onCreate()
        initKoin {
            androidContext(this@CustomApplication)
        }
    }

    override fun newImageLoader(context: Context): ImageLoader {
        return get<ImageLoader>()
    }
}