package mobi.librera.appcompose.room

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

class BookRepository(private val bookDao: BookDao) {

    fun getAllBooks(): Flow<List<Book>> = bookDao.getAll().map { it.map { e -> e.toBook() } }

    fun getAllSelected(): Flow<List<Book>> =
        flowOf(emptyList<Book>()) // bookDao.getAllSelected()

    fun insertAll(books: List<Book>) {
        bookDao.insertAll(books.map { it.toBookItem() })
    }//

    suspend fun updateStar(path: String, isSelected: Boolean, time: Long) = {}
    //bookDao.updateStar(path, isSelected, time)

    fun deleteAllBooks() = bookDao.deleteAllBooks()

    suspend fun getBookById(path: String): BookItem? = null//bookDao.getBookById(path)

    suspend fun updateBook(book: Book) = {}////bookDao.updateBook(book)


}