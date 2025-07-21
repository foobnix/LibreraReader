package mobi.librera.appcompose.model

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import mobi.librera.appcompose.core.searchBooks
import mobi.librera.appcompose.room.Book
import mobi.librera.appcompose.room.BookRepository

class DataModel(private val bookRepository: BookRepository) : ViewModel() {

    var currentBookPath by mutableStateOf("")

    private val _currentSearchQuery = MutableStateFlow("")
    val currentSearchQuery: StateFlow<String> get() = _currentSearchQuery.asStateFlow()

    fun updateSearchQuery(query: String) {
        _currentSearchQuery.value = query
    }

    init {
        loadInitialBooks()
    }

    private fun loadInitialBooks() {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                val currentBooks = bookRepository.getAllBooks().first()

                if (currentBooks.isEmpty()) {
                    val booksToInsert = searchBooks().map { Book(it) }
                    bookRepository.insertAll(booksToInsert)
                }
            }
        }
    }

    fun getAllSelected() = bookRepository.getAllSelected().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = emptyList()
    )

    fun updateStar(book: Book, isSelected: Boolean) = viewModelScope.launch {
        withContext(Dispatchers.IO) {
            bookRepository.updateStar(book.path, isSelected = isSelected)
        }
    }

    val getAllBooks: StateFlow<List<Book>> = bookRepository.getAllBooks()
        .combine(currentSearchQuery) { books, query ->
            books.filter { it.path.contains(query, ignoreCase = true) }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )

}