package mobi.librera.appcompose.model

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import mobi.librera.mupdf.fz.lib.MupdfDocument

class ReadBookModel : ViewModel() {

    private var doc: MupdfDocument = MupdfDocument.EmptyDoc

    fun openDocument(bookPath: String, width: Int, height: Int, fontSize: Int) =
        viewModelScope.launch(Dispatchers.IO) {
            doc = mobi.librera.mupdf.fz.lib.openDocument(bookPath, ByteArray(0), 1200, 1000, 44)
        }

    suspend fun renderPage(number: Int, pageWidth: Int): ImageBitmap {
        return doc.renderPage(number, 1200).asImageBitmap()
    }

    fun close() {
        viewModelScope.launch(Dispatchers.IO) {
            doc.close()
        }
    }

    fun getPagesCount(): Int = doc.pageCount

}