package com.github.wolray.line.io.ext

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

/**
 * @author wolray
 */
inline fun <T> getInMillis(block: () -> T): Pair<T, Long> {
    val start = System.currentTimeMillis()
    val t = block()
    return t to System.currentTimeMillis() - start
}

inline fun <T> Iterable<T>.parallelLaunch(crossinline block: (T) -> Unit) {
    runBlocking(Dispatchers.IO) {
        forEach {
            launch { block(it) }
        }
    }
}

fun <T> List<T>.asMutable() = this as MutableList
fun <K, V> Map<K, V>.asMutable() = this as MutableMap
fun <T> Set<T>.asMutable() = this as MutableSet
