package mobi.librera.appcompose.pdf

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import mobi.librera.mupdf.fz.lib.MupdfDocument

class MupdfRepository : FormatRepository {
    private var doc: MupdfDocument = MupdfDocument.EmptyDoc
    override suspend fun openDocument(bookPath: String, width: Int, height: Int, fontSize: Int) {
        doc =
            mobi.librera.mupdf.fz.lib.openDocument(bookPath, ByteArray(0), width, height, fontSize)
    }

    override suspend fun renderPage(number: Int, pageWidth: Int): ImageBitmap {
        return doc.renderPage(number, pageWidth).asImageBitmap()
    }

    override suspend fun closeDocument() = doc.close()

    override fun pagesCount(): Int = doc.pageCount


}