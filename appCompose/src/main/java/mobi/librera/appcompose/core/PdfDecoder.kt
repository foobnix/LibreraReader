package mobi.librera.appcompose.core

import android.graphics.Bitmap
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

class PdfDecoder(
    private val source: ImageSource,
    private val options: Options,
) : Decoder {
    @OptIn(InternalCoilApi::class)
    override suspend fun decode(): DecodeResult {
        var imageBitmap: Bitmap = mutex.withLock {
            println("Decode book begin ${source.file()}  ${Thread.currentThread()}")

            val muDoc = openDocument(
                source.file().toString(),
                byteArrayOf(0),
                128 * 4,
                180 * 4,
                24
            )
            println("Decode renderPage begin ")
            val res = muDoc.renderPage(0, 128 * 4)
            println("Decode renderPage end ")
            muDoc.close();
            println("Decode renderPage close ")
            res
        }




        println("Decode book end ${source.file()}  ${Thread.currentThread()}")


        return DecodeResult(
            image = imageBitmap.asImage(shareable = false),
            isSampled = false
        )

    }


    class Factory : Decoder.Factory {
        override fun create(
            result: SourceFetchResult,
            options: Options,
            imageLoader: ImageLoader
        ): Decoder {
            return PdfDecoder(result.source, options)
        }
    }

}