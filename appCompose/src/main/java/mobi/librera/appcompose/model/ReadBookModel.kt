package mobi.librera.appcompose.model

import androidx.compose.ui.graphics.ImageBitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import mobi.librera.appcompose.pdf.FormatRepository
import java.lang.ref.WeakReference

data class PageItem(val page: Int, val image: ImageBitmap)

class ReadBookModel(val source: FormatRepository) : ViewModel() {

    private var documentLoaded: Boolean = false
    fun openDocument(bookPath: String, width: Int, height: Int, fontSize: Int) {
        if (!documentLoaded) {
            pageCache.clear()
            viewModelScope.launch(Dispatchers.IO) {
                source.openDocument(bookPath, 1800, 1200, 44)
                documentLoaded = true
            }
        }
    }

    private val pageCache = mutableMapOf<Int, WeakReference<ImageBitmap>>()


    suspend fun renderPage(number: Int, pageWidth: Int): ImageBitmap {
        if (!documentLoaded) {
            return ImageBitmap(1, 1)
        }

        val res = pageCache[number]
        if (res?.get() != null) {
            return res.get()!!
        }

        val page = source.renderPage(number, 1200)

        pageCache[number] = WeakReference(page)
        return page
    }

    fun closeDocument() = viewModelScope.launch(Dispatchers.IO) {
        source.closeDocument()
    }

    fun getPagesCount(): Int = source.pagesCount()

}