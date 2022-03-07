package com.github.wolray.line.io.ext

import java.util.function.Supplier

/**
 * @author wolray
 */
class MutableLazy<T>(private val supplier: Supplier<T>) {
    private var cache: T? = null

    fun get() = cache ?: supplier.get().also { set(it) }

    fun set(t: T) {
        cache = t
    }
}

fun <T> List<T>.asMutable() = this as MutableList
fun <K, V> Map<K, V>.asMutable() = this as MutableMap
fun <T> Set<T>.asMutable() = this as MutableSet

inline fun <T> T.replaceIf(condition: Boolean, block: T.() -> T): T {
    return if (condition) block() else this
}

inline fun <T, E> T.replaceWith(e: E?, block: T.(E) -> T): T {
    return if (e != null) block(e) else this
}
