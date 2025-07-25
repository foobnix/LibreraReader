package mobi.librera.appcompose.room

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

class BookRepository(private val bookDao: BookDao) {

    fun getAllBooks2(): Flow<List<Book>> = bookDao.getAll()
        .map { it.map { e -> e.toBook() } }

    fun getAllBooks(): Flow<List<Book>> = bookDao.getAllBookItemAndState()
        .map {
            it.map { e -> e.toBook() }
        }


    fun getAllSelected(): Flow<List<Book>> =
        flowOf(emptyList<Book>()) // bookDao.getAllSelected()

    fun insertAll(books: List<Book>) {
        bookDao.insertAll(books.map { it.toBookItem() })
    }//

    fun updateBook(book: Book) =
        bookDao.insertBookState(book.toBookState().copy(time = System.currentTimeMillis()))


    fun deleteAllBooks() = bookDao.deleteAllBooks()

    suspend fun getBookById(path: String): BookItem? = null//bookDao.getBookById(path)


}