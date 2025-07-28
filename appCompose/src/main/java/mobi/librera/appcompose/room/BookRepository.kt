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

    fun getAllBookState(): List<BookState> = bookDao.getAllBookState()


    fun getAllRecent(): Flow<List<Book>> = bookDao.getAllRecent().map {
        it.map { e -> e.toBook() }
    }

    fun insertAllBookState(bookStates: List<BookState>) =
        bookDao.insertAllBookState(bookStates)


    fun insertBookState(bookState: BookState) =
        bookDao.insertBookState(bookState)

    fun insertAll(books: List<Book>) {
        bookDao.insertAll(books.map { it.toBookItem() })
    }

    fun updateBook(book: Book): BookState {
        val bookToUpdate = book.toBookState().copy(time = System.currentTimeMillis())
        bookDao.insertBookState(bookToUpdate)
        return bookToUpdate
    }

    fun deleteAllBooks() = bookDao.deleteAllBooks()

    fun getBookByPath(bookPath: String): Book? {
        return bookDao.getBookByPath(bookPath)?.toBook()
    }

}