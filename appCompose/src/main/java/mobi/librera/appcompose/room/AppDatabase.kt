package mobi.librera.appcompose.room

import android.content.Context
import android.util.Log
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import java.util.concurrent.Executors

@Database(
    entities = [
        BookItem::class,
        BookMeta::class,
        BookState::class,
        BookTag::class
    ], version = 1
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun bookDao(): BookDao
}

fun buildDatabase(context: Context): AppDatabase {
    val db = Room.databaseBuilder(
        context,
        AppDatabase::class.java,
        "book_database_3"
    )
    db.setQueryCallback(
        object : RoomDatabase.QueryCallback {
            override fun onQuery(sqlQuery: String, bindArgs: List<Any?>) {
                Log.d("DB", "SQL Query: $sqlQuery, Args: $bindArgs")
            }
        },
        Executors.newSingleThreadExecutor()
    )
    return db.build()
}