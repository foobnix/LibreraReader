package mobi.librera.appcompose.di

import android.util.Log
import androidx.room.Room
import androidx.room.RoomDatabase
import coil3.ImageLoader
import coil3.decode.Decoder
import coil3.disk.DiskCache
import coil3.memory.MemoryCache
import coil3.request.CachePolicy
import coil3.request.crossfade
import coil3.util.DebugLogger
import mobi.librera.appcompose.core.FilesRepository
import mobi.librera.appcompose.core.MupdfPdfDecoder
import mobi.librera.appcompose.datastore.UserPreferencesRepository
import mobi.librera.appcompose.model.DataModel
import mobi.librera.appcompose.model.ReadBookModel
import mobi.librera.appcompose.pdf.FormatRepository
import mobi.librera.appcompose.pdf.MupdfRepository
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
import java.util.concurrent.Executors

val appModule = module {

    single<AppDatabase> {
        val db = Room.databaseBuilder(
            androidApplication(),
            AppDatabase::class.java,
            "book_database3"
        )
        db.setQueryCallback(
            object : RoomDatabase.QueryCallback {
                override fun onQuery(sqlQuery: String, bindArgs: List<Any?>) {
                    Log.d("DB", "SQL Query: $sqlQuery, Args: $bindArgs")
                }
            },
            Executors.newSingleThreadExecutor() // Use a background thread for logging
        )
        db.build()
    }

    single<BookDao> {
        get<AppDatabase>().bookDao()
    }

    single<BookRepository> {
        BookRepository(get())
    }
    single { FilesRepository() }
    single { UserPreferencesRepository(get()) }


    //single<Decoder.Factory> { NativePdfDecoder.Factory() }
    //single<FormatRepository> { NativePDFRepository() }

    single<FormatRepository> { MupdfRepository() }
    single<Decoder.Factory> { MupdfPdfDecoder.Factory() }


    viewModelOf(::DataModel)
    viewModelOf(::ReadBookModel)
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

