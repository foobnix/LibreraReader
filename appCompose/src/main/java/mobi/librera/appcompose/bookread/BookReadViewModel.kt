package mobi.librera.appcompose.bookread

import androidx.compose.ui.graphics.ImageBitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import mobi.librera.appcompose.pdf.FormatRepository
import mobi.librera.appcompose.room.Book
import mobi.librera.appcompose.room.BookRepository


class BookReadViewModel(
    private val source: FormatRepository, private val bookRepository: BookRepository
) : ViewModel() {


    sealed class DocumentState {
        data object Idle : DocumentState()
        data object Loading : DocumentState()
        data object Success : DocumentState()
        data class Error(val message: String) : DocumentState()
    }

    private val _documentState = MutableStateFlow<DocumentState>(DocumentState.Idle)
    val documentState: StateFlow<DocumentState> = _documentState

    private val _selectedBook = MutableStateFlow<Book?>(null)
    val selectedBook = _selectedBook.asStateFlow()

    fun loadBook(bookPath: String, screenWidth: Int, screenHeight: Int) {
        viewModelScope.launch {
            try {
                _documentState.value = DocumentState.Loading
                _selectedBook.value = null

                var book = withContext(Dispatchers.IO) {
                    bookRepository.getBookByPath(bookPath)
                } ?: Book(bookPath, fontSize = 30)


                withContext(Dispatchers.IO) {
                    source.openDocument(bookPath, screenWidth, screenHeight, book.fontSize)
                }
                book = book.copy(pageCount = source.pagesCount())

                _selectedBook.value = book
                _documentState.value = DocumentState.Success

            } catch (e: Exception) {
                _documentState.value = DocumentState.Error(e.message.orEmpty())
            }
        }

    }

    fun closeBook() {
        pageCache.clear()
        _documentState.value = DocumentState.Idle
        _selectedBook.value = null
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