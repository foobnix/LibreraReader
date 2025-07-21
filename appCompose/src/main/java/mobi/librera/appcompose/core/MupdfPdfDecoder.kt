package mobi.librera.appcompose.core

import coil3.ImageLoader
import coil3.annotation.InternalCoilApi
import coil3.asImage
import coil3.decode.DecodeResult
import coil3.decode.Decoder
import coil3.decode.ImageSource
import coil3.fetch.SourceFetchResult
import coil3.request.Options
import mobi.librera.mupdf.fz.lib.openDocument


class MupdfPdfDecoder(
    private val source: ImageSource,
    private val options: Options,
) : Decoder {
    @OptIn(InternalCoilApi::class)
    override suspend fun decode(): DecodeResult {
        println("Decode book begin ${source.file()}  ${Thread.currentThread()}")

        val muDoc = openDocument(
            source.file().toString(),
            byteArrayOf(0),
            128 * 4,
            180 * 4,
            24
        )
        println("Decode renderPage begin ")
        val imageBitmap = muDoc.renderPage(0, 128 * 4)
        println("Decode renderPage end ")
        muDoc.close();
        println("Decode renderPage close ")




        println("Decode book end ${source.file()}  ${Thread.currentThread()}")


        return DecodeResult(
            image = imageBitmap.asImage(shareable = true),
            isSampled = false,
        )

    }


    class Factory : Decoder.Factory {
        override fun create(
            result: SourceFetchResult,
            options: Options,
            imageLoader: ImageLoader
        ): Decoder {
            return MupdfPdfDecoder(result.source, options)
        }
    }

}