package mobi.librera.appcompose.room

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Entity(tableName = "books")
@Serializable
data class Book(
    @PrimaryKey val path: String,
    @ColumnInfo(name = "fileName") val fileName: String = "",
    @ColumnInfo(name = "author") val author: String = "",
    @ColumnInfo(name = "title") val title: String = "",
    @ColumnInfo(name = "progress") val progress: Float = 0.0f,
    @ColumnInfo(name = "time") val time: Long = 0,
    @ColumnInfo(name = "isRecent") val isRecent: Boolean = false,
    @ColumnInfo(name = "isSelected") val isSelected: Boolean = false
)