package mobi.librera.appcompose

import android.app.Application
import android.content.Context
import coil3.ImageLoader
import coil3.SingletonImageLoader
import coil3.disk.DiskCache
import coil3.memory.MemoryCache
import coil3.request.CachePolicy
import coil3.request.crossfade
import coil3.util.DebugLogger
import mobi.librera.appcompose.core.PdfDecoder
import okio.Path.Companion.toPath
import java.io.File

class CustomApplication : Application(), SingletonImageLoader.Factory {
    override fun onCreate() {
        super.onCreate()


    }

    override fun newImageLoader(context: Context): ImageLoader {
        val diskCache = File(context.externalCacheDir, "cache_pages")
        diskCache.listFiles()?.forEach { println("Cache file: $it") }

        diskCache.mkdirs()
        println("Disk-Cache: $diskCache ${diskCache.isDirectory}")
        return ImageLoader.Builder(context).components {
            add(PdfDecoder.Factory())
        }.logger(DebugLogger()).diskCachePolicy(CachePolicy.ENABLED)
            .memoryCachePolicy(CachePolicy.ENABLED).memoryCache {
                MemoryCache.Builder().maxSizePercent(context, 0.50).build()
            }

            .diskCache {
                DiskCache.Builder().directory(
                    diskCache.toString().toPath()
                ).maxSizeBytes(500L * 1024 * 1024) // 500 MB
                    .build()
            }.crossfade(true).build()
    }
}