package com.github.wolray.line.io

import com.github.wolray.line.io.TextScope.toSequence
import java.io.File
import java.io.FileInputStream
import java.net.URL

/**
 * @author wolray
 */
interface IReader<S, V, T> : (V) -> T {
    fun toIterator(source: S): Iterator<V>
    fun read(source: S): LineReader<S, V, T>.Session

    interface Is<V, T> : IReader<Text, V, T> {
        fun read(url: URL) = read { url.openStream() }
        fun read(file: String) = read { FileInputStream(file) }
        fun read(file: File) = read { FileInputStream(file) }
        fun read(cls: Class<*>, resource: String) = read { cls.getResourceAsStream(resource)!! }
    }

    interface Csv<T> : Is<List<String>, T> {
        fun split(s: String): List<String>

        override fun toIterator(source: Text): Iterator<List<String>> {
            return source.toSequence().map(::split).iterator()
        }
    }
}
