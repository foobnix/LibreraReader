package mobi.librera.appcompose.room

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "books")
data class Book(
    @PrimaryKey val path: String,
    @ColumnInfo(name = "fileName") val fileName: String = "",
    @ColumnInfo(name = "author") val author: String = "",
    @ColumnInfo(name = "title") val title: String = "",
    @ColumnInfo(name = "page") val page: Int = 0,
    @ColumnInfo(name = "time") val time: Long = 0,
    @ColumnInfo(name = "isRecent") val isRecent: Boolean = false,
    @ColumnInfo(name = "isSelected") val isSelected: Boolean = false
)