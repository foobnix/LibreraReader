package mobi.librera.appcompose.bookread

import androidx.compose.ui.graphics.ImageBitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import mobi.librera.appcompose.pdf.FormatRepository
import mobi.librera.appcompose.room.Book
import mobi.librera.appcompose.room.BookRepository


class BookReadViewModel(
    private val source: FormatRepository, private val bookRepository: BookRepository
) : ViewModel() {

    private val _documentState = MutableStateFlow<DocumentState>(DocumentState.Idle)
    val documentState: StateFlow<DocumentState> = _documentState

    private val _selectedBook = MutableStateFlow<Book?>(null)
    val selectedBook = _selectedBook.asStateFlow()

    fun loadBook(bookPath: String) = viewModelScope.launch(Dispatchers.IO) {
        val book = bookRepository.getBookByPath(bookPath)
        _selectedBook.value = book
    }

    fun updateBook(book: Book?) {
        _selectedBook.value = book
    }

    sealed class DocumentState {
        data object Idle : DocumentState()
        data object Loading : DocumentState()
        data class Success(
            val uri: String, val firstPage: Int, val pageCount: Int
        ) : DocumentState()

        data class Error(val message: String) : DocumentState()
    }

    fun updateProgress(book: Book) = viewModelScope.launch(Dispatchers.IO) {
        bookRepository.updateBook(book)
    }

    fun openDocument(bookPath: String, width: Int, height: Int, fontSize: Int) {
        pageCache.clear()
        viewModelScope.launch(Dispatchers.IO) {
            _documentState.value = DocumentState.Loading
            try {
                loadBook(bookPath)
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