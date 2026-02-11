package mobi.librera5

expect fun initPDF()
expect suspend fun coverPDF(path: String): ByteArray
 fun coverEpub(path: String): ByteArray = EpubMetadataExtractor().cover(path)

suspend fun cover(path: String): ByteArray{
    val extension = path.substringAfterLast('.', "").lowercase()
    return when(extension){
        "pdf"->coverPDF(path)
        "epub"->coverEpub(path)
        else -> ByteArray(0)
    }
}

fun println(vararg input: String) {
    kotlin.io.println(input.joinToString("|", postfix = "|"))
}


