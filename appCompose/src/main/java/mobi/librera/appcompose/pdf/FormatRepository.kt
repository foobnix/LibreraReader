package mobi.librera.appcompose.pdf

import androidx.compose.ui.graphics.ImageBitmap

interface FormatRepository {

    suspend fun openDocument(bookPath: String, width: Int, height: Int, fontSize: Int)
    suspend fun renderPage(number: Int, pageWidth: Int): ImageBitmap
    suspend fun closeDocument()
    fun pagesCount(): Int

}