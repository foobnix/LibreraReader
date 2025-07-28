package mobi.librera.appcompose.bookread

import android.content.Context
import android.util.LruCache
import androidx.compose.ui.graphics.ImageBitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import mobi.librera.appcompose.core.spToPx
import mobi.librera.appcompose.pdf.FormatRepository
import mobi.librera.appcompose.room.Book
import mobi.librera.appcompose.room.BookRepository


interface ModelActions {
    suspend fun renderPage(page: Int, width: Int): ImageBitmap
    fun onPageChanged(page: Int)
    fun saveBookState()
    fun onFontSizeChange(size: Int)
}

data class BookReadUiState(
    val isLoading: Boolean = true,
    val error: String? = null,
    val book: Book = Book.Empty,
    val bookPathToOpen: String = "",
    val openPage: Int = 0,
    val showControls: Boolean = true
)

class BookReadViewModel(
    private val source: FormatRepository, private val bookRepository: BookRepository
) : ViewModel(), ModelActions {

    private val _uiState = MutableStateFlow(BookReadUiState())
    val uiState = _uiState.asStateFlow()

    private val pageCache = LruCache<Int, ImageBitmap>(10)
    private var loadJob: Job? = null

    fun openBook(path: String) {
        _uiState.value.book.let { book ->
            _uiState.update { it.copy(bookPathToOpen = path) }
        }

    }

    fun loadBook(context: Context, screenWidth: Int, screenHeight: Int) {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            try {

                _uiState.update { it.copy(isLoading = true) }


                val book = if (uiState.value.book == Book.Empty) {
                    withContext(Dispatchers.IO) {
                        bookRepository.getBookByPath(_uiState.value.bookPathToOpen)
                    } ?: Book.Empty
                } else {
                    uiState.value.book
                }


                pageCache.evictAll()
                source.closeDocument()
                source.openDocument(
                    _uiState.value.bookPathToOpen,
                    screenWidth,
                    screenHeight,
                    book.fontSize.spToPx(context)
                )

                val updatedBook = book.copy(
                    pageCount = source.pagesCount()
                )

                _uiState.update {
                    it.copy(
                        isLoading = false,
                        book = updatedBook,
                        openPage = (updatedBook.pageCount * updatedBook.progress).toInt(),
                        error = null
                    )
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }

    override suspend fun renderPage(page: Int, width: Int): ImageBitmap {
        return pageCache.get(page) ?: withContext(Dispatchers.IO) {
            source.renderPage(page, width).also { pageCache.put(page, it) }
        }
    }


    override fun onPageChanged(page: Int) {
        println("onPageChanged $page")
        _uiState.value.book.let { book ->
            val newProgress = (page + 1).toFloat() / book.pageCount
            _uiState.update { it.copy(book = book.copy(progress = newProgress)) }
        }
    }

    override fun saveBookState() {
        viewModelScope.launch(Dispatchers.IO) {
            bookRepository.updateBook(_uiState.value.book)
        }
    }


    override fun onFontSizeChange(size: Int) {
        viewModelScope.launch {
            _uiState.value.book.let { book ->
                _uiState.update {
                    it.copy(book = book.copy(fontSize = size))
                }
            }
            withContext(Dispatchers.IO) {
                bookRepository.updateBook(_uiState.value.book)
            }
        }
    }


    fun closeDocument() = viewModelScope.launch(Dispatchers.IO) {
        //source.closeDocument()
        _uiState.update { it.copy(book = Book.Empty) }

    }


}