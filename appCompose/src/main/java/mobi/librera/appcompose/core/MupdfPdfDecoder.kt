package mobi.librera.appcompose.core

import android.graphics.Bitmap
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import coil3.ImageLoader
import coil3.annotation.InternalCoilApi
import coil3.asImage
import coil3.decode.DecodeResult
import coil3.decode.Decoder
import coil3.decode.ImageSource
import coil3.fetch.SourceFetchResult
import coil3.request.Options
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import mobi.librera.mupdf.fz.lib.openDocument

val mutex = Mutex()

class MupdfPdfDecoder(
    private val source: ImageSource,
    private val options: Options,
) : Decoder {


    @OptIn(InternalCoilApi::class)
    override suspend fun decode(): DecodeResult {
        if (!source.file().toFile().isFile) {
            return DecodeResult(
                image = ImageBitmap(1, 1).asAndroidBitmap().asImage(shareable = true),
                isSampled = true,
            )
        }
        mutex.withLock {
            println("Decode book begin ${source.file()}  ${Thread.currentThread()}")


            val imageBitmap: Bitmap


            val muDoc = openDocument(
                source.file().toString(),
                byteArrayOf(0),
                128 * 4,
                180 * 4,
                24
            )

            println("Decode pageCount ${muDoc.pageCount}")
            if (muDoc.pageCount == 0) {
                println("Decode ERROR")
                imageBitmap = ImageBitmap(1, 1).asAndroidBitmap()
            } else {
                println("Decode renderPage begin ")
                imageBitmap = muDoc.renderPage(0, 128 * 4)
                println("Decode renderPage end ")
                muDoc.close();
                println("Decode renderPage close ")
            }



            println("Decode book end ${source.file()}  ${Thread.currentThread()}")


            return DecodeResult(
                image = imageBitmap.asImage(shareable = true),
                isSampled = false,
            )
        }

    }


    class Factory : Decoder.Factory {
        override fun create(
            result: SourceFetchResult,
            options: Options,
            imageLoader: ImageLoader
        ): Decoder? {
            println("Decoder.Factory mime: |${result.mimeType}|")
            return if (true || result.mimeType == "application/pdf" || result.mimeType == "application/epub+zip") {
                MupdfPdfDecoder(result.source, options)
            } else {
                null
            }
        }
    }

}