package mobi.librera.appcompose.model

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import mobi.librera.appcompose.room.Book
import mobi.librera.appcompose.room.BookDao
import mobi.librera.appcompose.room.BookRepository

class DataModel(private val bookRepository: BookRepository, private val bookDao: BookDao) :
    ViewModel() {

    fun insertAll(books: List<Book>) = viewModelScope.launch {
        withContext(Dispatchers.IO) {
            bookRepository.insertAll(books)
        }
    }

    fun getAllSelected() = bookDao.getAllSelected().stateIn(
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