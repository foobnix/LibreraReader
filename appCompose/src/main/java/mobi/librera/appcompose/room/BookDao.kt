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

    @Transaction
    @Query(
        """
        SELECT *
        FROM book_item AS a
        INNER JOIN book_state AS b ON a.fileName = b.fileName 
        WHERE b.is_selected = 1
    """
    )
    fun getAllSelected(): Flow<List<BookItemAndState>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertAll(users: List<BookItem>)

    @Transaction
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertBookState(state: BookState)

    @Query("DELETE FROM book_item")
    fun deleteAllBooks()


    @Query("SELECT * FROM book_item WHERE path=:path")
    fun getBookByPath(path: String): BookItemAndState?


//    @Transaction
//    @Query("SELECT * FROM book_item WHERE path = :bookPath")
//    fun getBookWithTags(bookPath: String): BookWithTags?

//    @Transaction
//    @Query("SELECT * FROM book_tag WHERE name = :tagName")
//    suspend fun getTagWithBooks(tagName: String): TagWithBooks?

}