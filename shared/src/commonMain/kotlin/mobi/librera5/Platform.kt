package mobi.librera5

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform
