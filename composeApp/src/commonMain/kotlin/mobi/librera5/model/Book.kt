package mobi.librera5.model

data class Book(
    val path: String,
    val title: String,
    val type: BookType
)

enum class BookType {
    PDF,
    EPUB
}
