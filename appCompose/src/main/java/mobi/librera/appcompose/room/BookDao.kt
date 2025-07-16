package mobi.librera.appcompose.room

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface BookDao {
    @Query("SELECT * FROM book")
    fun getAll(): List<Book>

    @Query("SELECT * FROM book")
    fun getAllFlow(): Flow<List<Book>>

    @Query("SELECT * FROM book WHERE path IN (:userIds)")
    fun loadAllByIds(userIds: IntArray): List<Book>

    @Query(
        "SELECT * FROM book WHERE first_name LIKE :first AND " +
                "last_name LIKE :last LIMIT 1"
    )
    fun findByName(first: String, last: String): Book

    @Insert
    suspend fun insertAll(users: List<Book>)

    @Delete
    fun delete(user: Book)
}