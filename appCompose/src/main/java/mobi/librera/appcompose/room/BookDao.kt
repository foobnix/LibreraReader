package mobi.librera.appcompose.room

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface BookDao {

    @Transaction
    @Query("SELECT * FROM book_item")
    fun getAllBookItemAndState(): Flow<List<BookItemAndState>>

    @Query("SELECT * FROM book_item")
    fun getAll(): Flow<List<BookItem>>

    //@Transaction
    //@Query("SELECT * FROM book_item")
    //fun getAllSelected(): Flow<List<BookItem>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertAll(users: List<BookItem>)

    @Transaction
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertBookState(state: BookState)


    //@Query("DELETE FROM books WHERE path = :path")
    // fun delete(path: String)

    @Query("DELETE FROM book_item")
    fun deleteAllBooks()

    //@Query("SELECT * FROM books WHERE path = :path")
    // fun getBookById(path: String): BookItem?

    //@Update
    ///fun updateBook(book: BookItem)
}