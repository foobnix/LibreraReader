package mobi.librera5

interface Platform {
    val name: String

    //fun loadCoverImage(path: String): Any?
}

expect fun getPlatform(): Platform
