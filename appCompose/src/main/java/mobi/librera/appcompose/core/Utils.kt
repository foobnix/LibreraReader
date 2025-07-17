package mobi.librera.appcompose.core

fun <T> Iterable<T>.firstOrDefault(default: T): T {
    return this.firstOrNull() ?: default
}