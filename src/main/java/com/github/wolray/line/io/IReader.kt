package com.github.wolray.line.io

import java.io.File
import java.io.FileInputStream

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
