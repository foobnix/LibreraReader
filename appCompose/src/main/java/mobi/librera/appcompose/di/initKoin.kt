package mobi.librera.appcompose.di

import androidx.room.Room
import coil3.ImageLoader
import coil3.decode.Decoder
import coil3.disk.DiskCache
import coil3.memory.MemoryCache
import coil3.request.CachePolicy
import coil3.request.crossfade
import coil3.util.DebugLogger
import mobi.librera.appcompose.core.FilesRepository
import mobi.librera.appcompose.core.NativePdfDecoder
import mobi.librera.appcompose.model.DataModel
import mobi.librera.appcompose.room.AppDatabase
import mobi.librera.appcompose.room.BookDao
import mobi.librera.appcompose.room.BookRepository
import okio.Path.Companion.toOkioPath
import org.koin.android.ext.koin.androidApplication
import org.koin.core.context.startKoin
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.KoinAppDeclaration
import org.koin.dsl.module
import java.io.File

val appModule = module {

    single<AppDatabase> {
        Room.databaseBuilder(
            androidApplication(),
            AppDatabase::class.java,
            "book_database"
        ).build()
    }

    single<BookDao> {
        get<AppDatabase>().bookDao()
    }

    single<BookRepository> {
        BookRepository(get())
    }
    single { FilesRepository() }

//    single<Decoder.Factory> { MupdfPdfDecoder.Factory() }
    single<Decoder.Factory> { NativePdfDecoder.Factory() }

    viewModelOf(::DataModel)
}

val imageLoaderModule = module {
    single<ImageLoader> {
        val context = get<android.content.Context>()
        val diskCache = File(context.externalCacheDir, "cache_pages").toOkioPath()

        ImageLoader.Builder(context)
            .components {
                add(get<Decoder.Factory>())
            }
            .logger(DebugLogger())
            .diskCachePolicy(CachePolicy.ENABLED)
            .memoryCachePolicy(CachePolicy.ENABLED)
            .memoryCache {
                MemoryCache.Builder()
                    .maxSizePercent(context, 0.50)
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(diskCache)
                    .maxSizeBytes(500L * 1024 * 1024) // 500 MB
                    .build()
            }
            .crossfade(true)
            .build()

    }
}

fun initKoin(config: KoinAppDeclaration) {
    startKoin {
        config.invoke(this)
        modules(
            appModule,
            imageLoaderModule
        )

    }
}

