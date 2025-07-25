package mobi.librera.appcompose.room

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class BookRepository(private val bookDao: BookDao) {

    fun getAllBooks(): Flow<List<Book>> = bookDao.getAllBookItemAndState()
        .map {
            it.map { e -> e.toBook() }
        }

    fun getAllSelected(): Flow<List<Book>> = bookDao.getAllSelected().map {
        it.map { e -> e.toBook() }
    }

    fun insertAll(books: List<Book>) {
        bookDao.insertAll(books.map { it.toBookItem() })
    }

    fun updateBook(book: Book) =
        bookDao.insertBookState(book.toBookState().copy(time = System.currentTimeMillis()))

    fun deleteAllBooks() = bookDao.deleteAllBooks()

    fun getBookByPath(bookPath: String): Book? {
        return bookDao.getBookByPath(bookPath)?.toBook()
    }

}