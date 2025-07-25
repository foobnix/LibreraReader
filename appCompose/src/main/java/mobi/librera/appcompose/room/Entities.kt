package mobi.librera.appcompose.room

import androidx.room.ColumnInfo
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Relation
import mobi.librera.appcompose.core.toFile

data class Book(
    val path: String,
    val fileName: String = "",
    val isSelected: Boolean = false,
    val isRecent: Boolean = false,
    val progress: Float = 0.0f,
    val time: Long = 0,
    val fontSize: Int = 0,
)


data class BookItemWithDetails(
    val bookItem: BookItem,
    val state: BookState,
    val meta: BookMeta
)

fun Book.toBookDetails(): BookItemWithDetails {
    val fileName = this.fileName.toFile().name
    return BookItemWithDetails(
        BookItem(this.path, fileName),
        BookState(fileName = fileName),
        BookMeta(this.path)
    )
}

fun Book.toBookItem() = BookItem(this.path, this.path.substringAfterLast("/"))
fun Book.toBookState() =
    BookState(
        fileName = this.fileName,
        progress = this.progress,
        time = this.time,
        isRecent = this.isRecent,
        isSelected = this.isSelected,
        fontSize = this.fontSize
    )

fun BookItem.toBook() = Book(
    path = this.path,
    fileName = this.fileName
)

fun BookItemAndState.toBook() = Book(
    this.bookItem.path,
    this.bookItem.fileName,
    this.bookState?.isSelected ?: false,
    this.bookState?.isRecent ?: false,
    this.bookState?.progress ?: 0f,
    this.bookState?.time ?: 0,
)

data class BookItemAndState(
    @Embedded val bookItem: BookItem,
    @Relation(
        parentColumn = "fileName",
        entityColumn = "fileName",
    )
    val bookState: BookState?
)

@Entity(tableName = "book_item")
data class BookItem(
    @PrimaryKey val path: String,
    @ColumnInfo val fileName: String,
)

@Entity(tableName = "book_state")
data class BookState(
    @PrimaryKey val fileName: String,
    @ColumnInfo(name = "progress") val progress: Float = 0.0f,
    @ColumnInfo(name = "time") val time: Long = 0,
    @ColumnInfo(name = "is_recent") val isRecent: Boolean = false,
    @ColumnInfo(name = "is_selected") val isSelected: Boolean = false,
    @ColumnInfo(name = "font_size") val fontSize: Int = 30
)

@Entity(tableName = "book_meta")
data class BookMeta(
    @PrimaryKey val path: String,
    @ColumnInfo(name = "author") val author: String = "",
    @ColumnInfo(name = "title") val title: String = "",
    @ColumnInfo(name = "description") val description: String = "",
    @ColumnInfo(name = "series") val series: String = "",
    @ColumnInfo(name = "series_index") val seriesIndex: String = "",
)

@Entity(tableName = "book_tag")
data class BookTag(
    @PrimaryKey val name: String,
    @ColumnInfo val order: Int = 0,
    @ColumnInfo val color: Int = 0,
)