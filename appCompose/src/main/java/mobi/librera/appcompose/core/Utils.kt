package mobi.librera.appcompose.core

fun <T> Iterable<T>.firstOrDefault(default: T): T {
    return this.firstOrNull() ?: default
}

fun <T> Boolean.ifOr(valueIfTrue: T, valueIfFalse: T): T {
    return if (this) valueIfTrue else valueIfFalse
}