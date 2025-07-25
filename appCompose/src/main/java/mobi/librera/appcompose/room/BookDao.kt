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
    fun getAllMetaAndState(): Flow<List<BooItemAndState>>

    @Query("SELECT * FROM book_item")
    fun getAll(): Flow<List<BookItem>>

    //@Transaction
    //@Query("SELECT * FROM book_item")
    //fun getAllSelected(): Flow<List<BookItem>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertAll(users: List<BookItem>)

    // @Query("UPDATE books SET isSelected = :isSelected, time = :time WHERE path = :path ")
    //  fun updateStar(path: String, isSelected: Boolean, time: Long)

    //@Query("DELETE FROM books WHERE path = :path")
    // fun delete(path: String)

    @Query("DELETE FROM book_item")
    fun deleteAllBooks()

    //@Query("SELECT * FROM books WHERE path = :path")
    // fun getBookById(path: String): BookItem?

    //@Update
    ///fun updateBook(book: BookItem)
}