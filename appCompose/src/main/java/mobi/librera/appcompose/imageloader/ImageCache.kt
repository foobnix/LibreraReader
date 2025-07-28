package mobi.librera.appcompose.imageloader

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.LruCache
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.asImageBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import mobi.librera.mupdf.fz.lib.openDocument
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import java.security.MessageDigest

interface ImageCache {
    fun get(key: String): ImageBitmap?
    fun put(key: String, bitmap: ImageBitmap)
    fun clear()
}

class MemoryCache(maxSize: Int) : ImageCache {
    private val lruCache = object : LruCache<String, ImageBitmap>(maxSize) {}

    override fun get(key: String): ImageBitmap? {
        return lruCache.get(key)
    }

    override fun put(key: String, bitmap: ImageBitmap) {
        lruCache.put(key, bitmap)
    }

    override fun clear() {
        lruCache.evictAll()
    }
}

class DiskCache(private val context: Context) : ImageCache {
    private val cacheDir: File by lazy {
        File(context.cacheDir, "image_cache3").apply { mkdirs() }
    }

    override fun get(key: String): ImageBitmap? {
        val file = getFileForKey(key)
        if (file.exists()) {
            return try {
                BitmapFactory.decodeFile(file.absolutePath)?.asImageBitmap()
            } catch (e: Exception) {
                e.printStackTrace()
                file.delete() // Delete corrupted file
                null
            }
        }
        return null
    }

    override fun put(key: String, bitmap: ImageBitmap) {
        val file = getFileForKey(key)
        var fos: OutputStream? = null
        try {
            fos = BufferedOutputStream(FileOutputStream(file))
            bitmap.asAndroidBitmap().compress(Bitmap.CompressFormat.PNG, 100, fos)
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            fos?.close()
        }
    }

    override fun clear() {
        cacheDir.deleteRecursively()
        cacheDir.mkdirs()
    }

    private fun getFileForKey(key: String): File {
        val file = File(cacheDir, md5(key))
        println("getFileForKey $file")
        return file
    }
}

private fun md5(input: String): String {
    val md = MessageDigest.getInstance("MD5")
    val digest = md.digest(input.toByteArray())
    return digest.joinToString("") { "%02x".format(it) }
}

object ImageDecoder {

    suspend fun decodeFromFile(filePath: String): ImageBitmap? = withContext(Dispatchers.IO) {
        try {

            val muDoc = openDocument(
                filePath,
                byteArrayOf(0),
                128 * 4,
                180 * 4,
                24
            )
            val res = muDoc.renderPage(0, 400).asImageBitmap()
            muDoc.close()
            res
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}

val mutex = Mutex()

class ImageLoader(private val memoryCache: MemoryCache, private val diskCache: DiskCache) {

    fun getFromMemory(url: String): ImageBitmap? {
        val cacheKey = url

        memoryCache.get(cacheKey)?.let {
            println("ImageLoader: Loaded from Memory Cache: $cacheKey")
            return it
        }

        diskCache.get(cacheKey)?.let {
            println("ImageLoader: Loaded from Disk Cache: $cacheKey")
            memoryCache.put(cacheKey, it)
            return it
        }
        return null
    }

    suspend fun loadImage(
        imageUrl: String
    ): ImageBitmap? {

        val cacheKey = imageUrl

        memoryCache.get(cacheKey)?.let {
            println("ImageLoader: Loaded from Memory Cache: $cacheKey")
            return it
        }

        diskCache.get(cacheKey)?.let {
            println("ImageLoader: Loaded from Disk Cache: $cacheKey")
            memoryCache.put(cacheKey, it)
            return it
        }

        mutex.withLock {


            val decodedBitmap = ImageDecoder.decodeFromFile(imageUrl)

            decodedBitmap?.let {
                memoryCache.put(cacheKey, it)
                diskCache.put(cacheKey, it)
                return it
            }
        }

        return null
    }
}

object AppImageLoader {
    private lateinit var imageLoader: ImageLoader
    private var isInitialized = false

    fun initialize(context: Context) {
        if (!isInitialized) {
            val memoryCache = MemoryCache(64 * 1024 * 1024)
            val diskCache = DiskCache(context.applicationContext)
            imageLoader = ImageLoader(memoryCache, diskCache)
            isInitialized = true
        }
    }

    fun get(): ImageLoader {
        if (!isInitialized) {
            error("AppImageLoader must be initialized with a context before use. Call AppImageLoader.initialize(context) first.")
        }
        return imageLoader
    }
}
