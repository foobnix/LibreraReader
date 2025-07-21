package mobi.librera.appcompose.pdf

import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.graphics.createBitmap
import java.io.File

class NativePDFRepository : FormatRepository {
    private var renderer: PdfRenderer? = null
    private var fileDescriptor: ParcelFileDescriptor? = null

    override suspend fun openDocument(bookPath: String, width: Int, height: Int, fontSize: Int) {
        closeDocument()
        fileDescriptor =
            ParcelFileDescriptor.open(File(bookPath), ParcelFileDescriptor.MODE_READ_ONLY)
        renderer = PdfRenderer(fileDescriptor!!)
    }

    override suspend fun renderPage(number: Int, pageWidth: Int): ImageBitmap {
        if (renderer == null) {
            return ImageBitmap(0, 0);
        }
        val page = renderer!!.openPage(number)
        //val k: Float = page.height.toFloat() / page.width.toFloat()
        ///val bitmap = createBitmap(pageWidth, (pageWidth * k).toInt())
        val bitmap = createBitmap(page.width, page.height)
        page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
        page.close()
        return bitmap.asImageBitmap()

    }

    override suspend fun closeDocument() {
        renderer?.close()
        fileDescriptor?.close()
        renderer = null
        fileDescriptor = null
    }

    override fun pagesCount(): Int = renderer?.pageCount ?: 0

}