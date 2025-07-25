package mobi.librera.appcompose.bookgrid

import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import mobi.librera.appcompose.core.DEFAULT_SEARCH_DIR
import mobi.librera.appcompose.core.FilesRepository
import mobi.librera.appcompose.datastore.UserPreferencesRepository
import mobi.librera.appcompose.room.Book
import mobi.librera.appcompose.room.BookRepository

class BookGridViewModel(
    private val bookRepository: BookRepository,
    private val filesRepository: FilesRepository,
    private val preferenecesRepository: UserPreferencesRepository,
) : ViewModel() {

    private val _selectedBook = MutableStateFlow<String>("")
    val selectedBook = _selectedBook.asStateFlow()

    fun onSelectBook(book: String) {
        _selectedBook.value = book
    }

    val searchPath: StateFlow<String> = preferenecesRepository.userName.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = DEFAULT_SEARCH_DIR.path
    )

    val isDarkModeEnabled: StateFlow<Boolean> = preferenecesRepository.isDarkModeEnabled.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = false
    )

    fun updateDarkMode(value: Boolean) = viewModelScope.launch {
        preferenecesRepository.setDarkModeEnabled(value)
    }

    fun updateSearchPath(name: String) = viewModelScope.launch {
        preferenecesRepository.saveUserName(name)
    }

    var listGridStates = LazyGridState(0, 0)

    var initialFirstVisibleItemIndex: Int by mutableIntStateOf(0)
    var initialFirstVisibleItemScrollOffset: Int by mutableIntStateOf(0)


    var isSearchPDF by mutableStateOf(true)
    var isSearchEPUB by mutableStateOf(true)

    private val _currentSearchQuery = MutableStateFlow("")
    val currentSearchQuery: StateFlow<String>
        get() = _currentSearchQuery.asStateFlow()

    fun updateSearchQuery(query: String) {
        _currentSearchQuery.value = query
    }

    init {
        loadInitialBooks()
    }

    public fun loadInitialBooks() = viewModelScope.launch {
        val currentBooks = bookRepository.getAllBooks().first()

        if (currentBooks.isEmpty()) {
            searchBooks()
        }
    }

    fun updateStar(book: Book, isSelected: Boolean) = viewModelScope.launch(Dispatchers.IO) {
        bookRepository.updateStar(
            book.path, isSelected = isSelected, time = System.currentTimeMillis()
        )
    }

    fun searchBooks() = viewModelScope.launch(Dispatchers.IO) {
        bookRepository.deleteAllBooks()
        val booksToInsert = filesRepository.getAllBooks(
            isPDF = isSearchPDF, isEPUB = isSearchEPUB, searchPath.first()
        ).map { Book(it) }
        bookRepository.insertAll(booksToInsert)
    }

    val getAllSelected = bookRepository.getAllSelected().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = emptyList()
    )


    val getAllBooks = bookRepository.getAllBooks().combine(currentSearchQuery) { books, query ->
        if (query.isNotEmpty()) {
            books.filter { it.path.contains(query, ignoreCase = true) }
        } else {
            books
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = emptyList()
    )

}