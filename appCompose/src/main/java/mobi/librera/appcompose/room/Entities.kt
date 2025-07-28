package mobi.librera.appcompose.room

import androidx.annotation.Keep
import androidx.room.ColumnInfo
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.ForeignKey.Companion.CASCADE
import androidx.room.Index
import androidx.room.Junction
import androidx.room.PrimaryKey
import androidx.room.Relation
import mobi.librera.appcompose.App
import mobi.librera.appcompose.core.toFile


data class Book(
    val path: String,
    val fileName: String = "",
    val isSelected: Boolean = false,
    val isRecent: Boolean = false,
    val progress: Float = 0.0f,
    val time: Long = 0,
    val fontSize: Int = DEFAULT_FONT_SIZE,
    val pageCount: Int = 0,
) {
    companion object {
        const val DEFAULT_FONT_SIZE = 24
        val Empty = Book("")
    }
}


data class BookItemWithDetails(
    val bookItem: BookItem, val state: BookState, val meta: BookMeta
)

fun Book.toBookDetails(): BookItemWithDetails {
    val fileName = this.fileName.toFile().name
    return BookItemWithDetails(
        BookItem(this.path, fileName),
        BookState(fileName = fileName, bookPaths = mapOf(App.DEVICE_ID to this.path)),
        BookMeta(this.path)
    )
}

fun Book.toBookItem() = BookItem(this.path, this.path.substringAfterLast("/"))
fun Book.toBookState() = BookState(
    fileName = this.fileName,
    bookPaths = mapOf(App.DEVICE_ID to this.path),
    progress = this.progress,
    time = this.time,
    recent = this.isRecent,
    selected = this.isSelected,
    fontSize = this.fontSize
)

fun BookItem.toBook() = Book(
    path = this.path, fileName = this.fileName
)

fun BookItemAndState.toBook() = Book(
    this.bookItem.path,
    this.bookItem.fileName,
    this.bookState?.selected ?: false,
    this.bookState?.recent ?: false,
    this.bookState?.progress ?: 0f,
    this.bookState?.time ?: 0,
)

fun BookState.toBook() = Book(
    this.bookPaths[App.DEVICE_ID] ?: "",
    this.fileName,
    this.selected,
    this.recent,
    this.progress,
    this.time,
)

data class BookItemAndState(
    @Embedded val bookItem: BookItem,
    @Relation(
        parentColumn = "fileName",
        entityColumn = "fileName",
    ) val bookState: BookState?,
)

data class BookWithTags(
    @Embedded val bookItem: BookItem, @Relation(
        parentColumn = "fileName", entityColumn = "name", associateBy = Junction(
            BookItemTags::class, parentColumn = "fileName", entityColumn = "tagName"
        )
    ) val tags: List<BookTag>
)

data class TagWithBooks(
    @Embedded val bookTag: BookTag, @Relation(
        parentColumn = "name", entityColumn = "fileName", associateBy = Junction(
            BookItemTags::class, parentColumn = "tagName", entityColumn = "bookPath"
        )
    ) val books: List<BookItem>
)

@Entity(
    tableName = "book_item_tags", primaryKeys = ["fileName", "tagName"], foreignKeys = [ForeignKey(
        entity = BookItem::class,
        parentColumns = ["fileName"],
        childColumns = ["fileName"],
        onDelete = CASCADE
    ), ForeignKey(
        entity = BookTag::class,
        parentColumns = ["tagName"],
        childColumns = ["name"],
        onDelete = CASCADE
    )], indices = [Index(value = ["bookPath"]), Index(value = ["tagName"])]
)
data class BookItemTags(
    @ColumnInfo val fileName: String,
    @ColumnInfo val tagName: String,
)


@Entity(tableName = "book_item")
data class BookItem(
    @PrimaryKey val path: String,
    @ColumnInfo val fileName: String,
)

@Entity(tableName = "book_state")
@Keep
data class BookState(
    @PrimaryKey val fileName: String = "",
    @ColumnInfo(name = "book_paths") var bookPaths: Map<String, String> = mapOf(),
    @ColumnInfo(name = "progress") val progress: Float = 0.0f,
    @ColumnInfo(name = "time") val time: Long = 0,
    @ColumnInfo(name = "is_recent") val recent: Boolean = false,
    @ColumnInfo(name = "is_selected") val selected: Boolean = false,
    @ColumnInfo(name = "font_size") val fontSize: Int = 30,
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