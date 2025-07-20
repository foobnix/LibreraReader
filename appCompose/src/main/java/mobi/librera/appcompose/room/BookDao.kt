package mobi.librera.appcompose.room

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface BookDao {
    @Query("SELECT * FROM books")
    fun getAll(): Flow<List<Book>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertAll(users: List<Book>)

    @Query("DELETE FROM books WHERE path = :path")
    fun delete(path: String)

    @Query("DELETE FROM books")
    fun deleteAllBooks()
}