package mobi.librera.lib.model

data class BookModel(
    val fileName: String,
    val percent: Float,
    val percentTime: Long,
    val starIs: Boolean,
    val starTime: Long,
    val recentIs: Boolean,
    val customBookCss: String,
    val bookmarks: List<BookBookmark>,
    val tags: List<String>,
    val collections: List<String>,
    val downloadPath: String,
    val downloadThumbnail: String,
    val meta: BookMeta,
    val devices: List<BookDevice>
)


data class BookBookmark(
    val percent: Float,
    val time: Long,
    val text: String
)

data class BookDevice(
    val filePath: Float,
    val zoom: Float,
    val offsetX: Float,
    val offsetY: Float,
    val splitPages: Boolean,
    val cropPages: Boolean,
    val speed: Int,
)

data class BookMeta(
    val title: String,
    val author: String,
    val sequence: String,
    val genre: String,
    val index: Int,
    val size: Long,
    val data: Long,
    val language: String,
    val publisher: String,
    val isbn: String
)