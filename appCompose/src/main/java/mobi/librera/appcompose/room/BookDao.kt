package mobi.librera.appcompose.room

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface BookDao {
    @Query("SELECT * FROM books")
    fun getAll(): Flow<List<Book>>

    @Query("SELECT * FROM books where isSelected = 1 ORDER BY time DESC")
    fun getAllSelected(): Flow<List<Book>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertAll(users: List<Book>)

    @Query("UPDATE books SET isSelected = :isSelected, time = :time WHERE path = :path ")
    fun updateStar(path: String, isSelected: Boolean, time: Long)

    @Query("DELETE FROM books WHERE path = :path")
    fun delete(path: String)

    @Query("DELETE FROM books")
    fun deleteAllBooks()

    @Query("SELECT * FROM books WHERE path = :path")
    fun getBookById(path: String): Book?

    @Update
    fun updateBook(book: Book)
}