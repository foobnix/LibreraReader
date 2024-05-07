package org.spreadme.pdfgadgets.common

import kotlinx.coroutines.*
import mu.KotlinLogging
import java.io.Closeable
import java.util.*
import kotlin.coroutines.CoroutineContext

abstract class ViewModel {

    companion object {
        fun closeWithRumtimeException(any: Any) {
            if (any is Closeable) {
                any.close()
            }
        }
    }

    val tags: MutableMap<String, Any> = mutableMapOf()

    @Volatile
    var cleared: Boolean = false

    open fun onCleared() {

    }

    fun clear() {
        cleared = true
        if (tags.isNotEmpty()) {
            synchronized(tags) {
                tags.forEach { (_, u) ->
                    closeWithRumtimeException(u)
                }
            }
        }
        onCleared()
    }

    inline fun <reified T : Any> setTagIfAbsent(key: String, newValue: T): T {
        synchronized(tags) {
            val previous = tags[key]
            val result = if (previous == null) {
                tags[key] = newValue
                newValue
            } else {
                previous
            }
            if (cleared) {
                closeWithRumtimeException(result)
            }
            return result as T
        }
    }

    inline fun <reified T> getTag(key: String): T? {
        if (tags.isEmpty()) {
            return null
        }
        synchronized(tags) {
            return tags[key] as T
        }
    }
}

private const val JOB_KEY = "org.spreadme.pdfgadgets.common.ViewModelCoroutineScope.JOB_KEY"

val ViewModel.viewModelScope: CoroutineScope
    get() {
        val scope: CoroutineScope? = this.getTag(JOB_KEY)
        if (scope != null) {
            return scope
        }
        return setTagIfAbsent(
            JOB_KEY,
            CloseableCoroutineScope(CoroutineName(this::class.simpleName ?: UUID.randomUUID().toString()) + SupervisorJob() + Dispatchers.Default)
        )
    }

internal class CloseableCoroutineScope(context: CoroutineContext) : Closeable, CoroutineScope {

    private val logger = KotlinLogging.logger {}

    override val coroutineContext: CoroutineContext = context

    override fun close() {
        logger.debug("{} canceled!!!", coroutineContext[CoroutineName])
        coroutineContext.cancel()
    }

}