package mobi.librera.appcompose.room

import kotlinx.coroutines.flow.Flow

class BookRepository(private val bookDao: BookDao) {

    fun getAllBooks(): Flow<List<Book>> = bookDao.getAll()

    fun getAllSelected(): Flow<List<Book>> = bookDao.getAllSelected()

    suspend fun insertAll(books: List<Book>) = bookDao.insertAll(books)

    suspend fun updateStar(path: String, isSelected: Boolean) = bookDao.updateStar(path, isSelected)


}