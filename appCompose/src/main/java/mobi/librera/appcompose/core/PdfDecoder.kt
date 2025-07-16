package mobi.librera.appcompose.core

import coil3.ImageLoader
import coil3.annotation.InternalCoilApi
import coil3.asImage
import coil3.decode.DecodeResult
import coil3.decode.Decoder
import coil3.decode.ImageSource
import coil3.fetch.SourceFetchResult
import coil3.request.Options
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.runInterruptible
import mobi.librera.mupdf.fz.lib.openDocument

class PdfDecoder(
    private val source: ImageSource,
    private val options: Options,
) : Decoder {
    @OptIn(InternalCoilApi::class)
    override suspend fun decode(): DecodeResult = runInterruptible {

        val muDoc = openDocument(
            source.file().toString(),
            byteArrayOf(0),
            128 * 4,
            180 * 4,
            24
        )
        val imageBitmap = runBlocking {
            muDoc.renderPage(0, 128 * 4)
        }
        println("Decode book ${source.file()}")


        DecodeResult(
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