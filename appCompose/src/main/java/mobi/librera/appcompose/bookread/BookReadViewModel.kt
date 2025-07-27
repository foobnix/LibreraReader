package mobi.librera.appcompose.bookread

import android.content.Context
import androidx.compose.ui.graphics.ImageBitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import mobi.librera.appcompose.core.spToPx
import mobi.librera.appcompose.pdf.FormatRepository
import mobi.librera.appcompose.room.Book
import mobi.librera.appcompose.room.BookRepository
import mobi.librera.appcompose.room.DEFAULT_FONT_SIZE


interface ModelActions {
    fun onPrintHello(string: String)
    fun getPagesCount(): Int
    fun getCurrentPage(): Int
    suspend fun renderPage(page: Int, width: Int): ImageBitmap
    fun onPageChanged(page: Int)
    fun saveBookState()
}

class BookReadViewModel(
    private val source: FormatRepository, private val bookRepository: BookRepository
) : ViewModel(), ModelActions {

    override fun onPrintHello(string: String) {
        println("onPrintHello")
    }

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


    fun loadBook(context: Context, bookPath: String, screenWidth: Int, screenHeight: Int) {
        viewModelScope.launch {
            try {
                _documentState.value = DocumentState.Loading


                _selectedBook.value = withContext(Dispatchers.IO) {
                    val bookByPath = bookRepository.getBookByPath(bookPath)
                    println("loadBook bookByPath ${bookByPath} ${bookPath}")
                    val currentBook = _selectedBook.value
                        ?: bookByPath
                        ?: Book(bookPath, fontSize = DEFAULT_FONT_SIZE)



                    source.openDocument(
                        bookPath,
                        screenWidth,
                        screenHeight,
                        currentBook.fontSize.spToPx(context)
                    )
                    currentBook.copy(pageCount = source.pagesCount())
                }

                _documentState.value = DocumentState.Success

            } catch (e: Exception) {
                _documentState.value = DocumentState.Error(e.message.orEmpty())
            }
        }
    }

    override fun onPageChanged(page: Int) {
        println("onPageChanged $page")
        _selectedBook.update {
            it?.copy(progress = page.toFloat() / it.pageCount)
        }
    }

    override fun saveBookState() {
        viewModelScope.launch(Dispatchers.IO) {
            println("saveBookState ${selectedBook.value}")
            selectedBook.value?.let {
                bookRepository.updateBook(it)
            }
        }
    }


    fun pageSizeInc() {
        _selectedBook.update {
            it?.copy(fontSize = it.fontSize + 1)
        }
    }

    fun pageSizeDec() {
        _selectedBook.update {
            it?.copy(fontSize = it.fontSize - 1)
        }

    }

    fun closeBook() {
        pageCache.clear()
        _documentState.value = DocumentState.Idle
        _selectedBook.value = null
    }


    private val pageCache = mutableMapOf<Int, ImageBitmap>()


    override suspend fun renderPage(number: Int, pageWidth: Int): ImageBitmap {

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

    override fun getPagesCount(): Int = source.pagesCount()
    override fun getCurrentPage(): Int =
        selectedBook.value?.let { (it.pageCount * it.progress).toInt() } ?: 0
}