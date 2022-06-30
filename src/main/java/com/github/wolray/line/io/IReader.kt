package com.github.wolray.line.io

import java.io.*
import java.util.function.Supplier

/**
 * @author wolray
 */
interface IReader<S, V, T> : (V) -> T {
    fun toIterator(source: S): Iterator<V>
    fun read(source: S): LineReader<S, V, T>.Session

    interface Is<V, T> : IReader<Text, V, T> {
        fun read(file: String) = read { FileInputStream(file) }
        fun read(file: File) = read { FileInputStream(file) }
        fun read(cls: Class<*>, resource: String) = read { cls.getResourceAsStream(resource)!! }
    }
}

typealias Text = Supplier<InputStream>

fun Text.toSequence() = BufferedReader(InputStreamReader(get())).lineSequence()
fun String.splitToList(sep: String) = split(sep)
fun Int.rangeUntil(before: Int): IntArray = (this until before).toList().toIntArray()

inline fun <T> Array<T>?.ifNotEmpty(block: (Array<T>) -> Unit) {
    this?.takeIf { it.isNotEmpty() }?.also(block)
}

inline fun IntArray?.ifNotEmpty(block: (IntArray) -> Unit) {
    this?.takeIf { it.isNotEmpty() }?.also(block)
}
