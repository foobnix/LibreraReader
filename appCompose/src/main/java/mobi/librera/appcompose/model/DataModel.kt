package mobi.librera.appcompose.model

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import mobi.librera.appcompose.room.Book
import mobi.librera.appcompose.room.BookRepository

class DataModel(private val bookRepository: BookRepository) : ViewModel() {

//    private val _text = MutableStateFlow("")
//    val currentBookPath: StateFlow<String> = _text

    var currentBookPath by mutableStateOf("")


    fun insertAll(books: List<Book>) = viewModelScope.launch {
        withContext(Dispatchers.IO) {
            bookRepository.insertAll(books)
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

    val allBooks: StateFlow<List<Book>> = bookRepository.getAllBooks().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = emptyList()
    )
}