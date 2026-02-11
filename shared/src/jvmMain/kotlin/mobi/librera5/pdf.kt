package mobi.librera5

import com.dshatz.pdfmp.ConsumerBuffer
import com.dshatz.pdfmp.ConsumerBufferUtil
import com.dshatz.pdfmp.PdfRenderer
import com.dshatz.pdfmp.PdfRendererFactory
import com.dshatz.pdfmp.model.BufferDimensions
import com.dshatz.pdfmp.model.BufferInfo
import com.dshatz.pdfmp.model.PageTransform
import com.dshatz.pdfmp.model.RenderRequest
import com.dshatz.pdfmp.model.bytes
import com.dshatz.pdfmp.source.PdfSource
import kotlinx.io.files.Path
import org.jetbrains.skia.Bitmap
import org.jetbrains.skia.EncodedImageFormat
import org.jetbrains.skia.Image
import kotlin.math.roundToInt

actual suspend fun coverPDF(path: String): ByteArray {
    println("Path:", path)
    val source = PdfSource.PdfPath(Path(path))
    var renderer: PdfRenderer? = null;
    try {
        renderer = PdfRendererFactory.createFromSource(source).getOrThrow()
    } catch (e: Exception) {
        return ByteArray(0)
    }

    val width = 200;
    val height = (width * 1.25).roundToInt()
    val stride = width * 4
    val dimensions = BufferDimensions(width, height, stride)

    val buffer: ConsumerBuffer =
        ConsumerBufferUtil.allocate((dimensions.width * dimensions.height * 4).bytes,
            dimensions.width,
            dimensions.height)


    buffer.withAddress { address ->

        val req =
            RenderRequest(listOf(PageTransform(0, 0, 0, 0, 0, width, height, 0, 1f)),
                0,
                0,
                BufferInfo(BufferDimensions(width, height, stride), address))

        renderer.render(req)

        renderer.close()
    }
    return buffer.skiaBitmap.toByteArray()!!
}

fun Bitmap.toByteArray(): ByteArray? {
    val image = Image.makeFromBitmap(this)
    val data = image.encodeToData(EncodedImageFormat.PNG, 100)
    return data?.bytes
}

