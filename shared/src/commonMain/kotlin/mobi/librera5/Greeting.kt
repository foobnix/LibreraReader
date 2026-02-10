package mobi.librera5

class Greeting {
    private val platform = getPlatform()

    fun greet(): String {
        return "Hello, ${platform.name}!"
    }

    fun cover(path: String): ByteArray = EpubMetadataExtractor().cover(path)

}