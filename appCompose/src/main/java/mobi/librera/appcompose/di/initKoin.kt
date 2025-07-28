package mobi.librera.appcompose.di

import android.util.LruCache
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.unit.dp
import coil3.Image
import coil3.ImageLoader
import coil3.asImage
import coil3.decode.DataSource
import coil3.decode.Decoder
import coil3.disk.DiskCache
import coil3.intercept.Interceptor
import coil3.memory.MemoryCache
import coil3.request.CachePolicy
import coil3.request.ImageResult
import coil3.request.SuccessResult
import coil3.request.crossfade
import coil3.size.Precision
import coil3.util.DebugLogger
import mobi.librera.appcompose.bookgrid.BookGridViewModel
import mobi.librera.appcompose.bookread.BookReadViewModel
import mobi.librera.appcompose.booksync.GoogleSingInViewModel
import mobi.librera.appcompose.core.FilesRepository
import mobi.librera.appcompose.core.MupdfPdfDecoder
import mobi.librera.appcompose.datastore.UserPreferencesRepository
import mobi.librera.appcompose.pdf.FormatRepository
import mobi.librera.appcompose.pdf.MupdfRepository
import mobi.librera.appcompose.room.AppDatabase
import mobi.librera.appcompose.room.BookDao
import mobi.librera.appcompose.room.BookRepository
import mobi.librera.appcompose.room.buildDatabase
import okio.Path.Companion.toOkioPath
import org.koin.android.ext.koin.androidApplication
import org.koin.core.context.startKoin
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.KoinAppDeclaration
import org.koin.dsl.module
import java.io.File

val appModule = module {

    single<AppDatabase> { buildDatabase(androidApplication()) }

    single<BookDao> {
        get<AppDatabase>().bookDao()
    }

    single<BookRepository> {
        BookRepository(get())
    }
    single { FilesRepository() }
    single { GoogleSingInViewModel() }
    single { UserPreferencesRepository(get()) }


    //single<Decoder.Factory> { NativePdfDecoder.Factory() }
    //single<FormatRepository> { NativePDFRepository() }

    single<FormatRepository> { MupdfRepository() }
    single<Decoder.Factory> { MupdfPdfDecoder.Factory() }


    viewModelOf(::BookGridViewModel)
    viewModelOf(::BookReadViewModel)
}

val imageLoaderModule = module {
    single<ImageLoader> {
        val context = get<android.content.Context>()
        val diskCache = File(context.cacheDir, "cache_pages").toOkioPath()
        diskCache.toFile().mkdirs()
        println("imageLoaderModule $diskCache ${diskCache.toFile().isDirectory}")

        ImageLoader.Builder(context)
            .components {
                //add(CustomCacheInterceptor())
                add(get<Decoder.Factory>())
            }
            .logger(DebugLogger())
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
            .placeholder(
                ImageBitmap(42.dp.value.toInt(), 42.dp.value.toInt()).asAndroidBitmap()
                    .asImage(true)
            )
            .precision(Precision.EXACT)
            .networkCachePolicy(CachePolicy.ENABLED)
            .diskCachePolicy(CachePolicy.ENABLED)
            .memoryCachePolicy(CachePolicy.ENABLED)
            .crossfade(true)
            .build()

    }
}

class CustomCacheInterceptor(

) : Interceptor {

    private val cache: LruCache<String, Image> = LruCache<String, Image>(30)

    override suspend fun intercept(chain: Interceptor.Chain): ImageResult {
        println("intercept-chain ${chain.request.data}")


        val value = cache.get(chain.request.data.toString())
        if (value != null) {
            return SuccessResult(
                image = value,
                request = chain.request,
                dataSource = DataSource.MEMORY_CACHE,
            )
        }
        return chain.proceed()
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

