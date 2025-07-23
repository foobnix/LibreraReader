package mobi.librera.appcompose.model

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.ImageBitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import mobi.librera.appcompose.pdf.FormatRepository
import mobi.librera.appcompose.room.BookRepository


class ReadBookModel(
    private val source: FormatRepository, private val bookRepository: BookRepository
) : ViewModel() {

    private val _documentState = MutableStateFlow<DocumentState>(DocumentState.Idle)
    val documentState: StateFlow<DocumentState> = _documentState

    var fontSize: Int by mutableIntStateOf(30)
    var progresss: Float by mutableFloatStateOf(0.0f)


    sealed class DocumentState {
        data object Idle : DocumentState()
        data object Loading : DocumentState()
        data class Success(
            val uri: String, val firstPage: Int, val pageCount: Int
        ) : DocumentState()

        data class Error(val message: String) : DocumentState()
    }

    fun updateProgress(bookPath: String) = viewModelScope.launch(Dispatchers.IO) {
        val book = bookRepository.getBookById(bookPath)
        val copy = book!!.copy(progress = progresss)
        bookRepository.updateBook(copy)
        println("Update progress $progresss")
    }

    fun openDocument(bookPath: String, width: Int, height: Int, fontSize: Int) {
        pageCache.clear()
        viewModelScope.launch(Dispatchers.IO) {
            _documentState.value = DocumentState.Loading
            try {
                progresss = bookRepository.getBookById(bookPath)?.progress!!
                println("Get progress $progresss")
                source.openDocument(bookPath, width, height, fontSize)
                _documentState.value = DocumentState.Success(bookPath, 0, source.pagesCount())
            } catch (e: Exception) {
                _documentState.value = DocumentState.Error(e.message ?: "Failed to open document")
            }
        }
    }

    private val pageCache = mutableMapOf<Int, ImageBitmap>()


    suspend fun renderPage(number: Int, pageWidth: Int): ImageBitmap {

        val res = pageCache[number]
        if (res != null) {
            //return res
        }

        val page = source.renderPage(number, 1200)

        pageCache[number] = page
        return page
    }

    fun closeDocument() = viewModelScope.launch(Dispatchers.IO) {
        source.closeDocument()
    }

    fun getPagesCount(): Int = source.pagesCount()

}