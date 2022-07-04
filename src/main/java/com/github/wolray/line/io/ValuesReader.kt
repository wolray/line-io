package com.github.wolray.line.io

import com.github.wolray.line.io.EmptyScope.ifNotEmpty

/**
 * @author wolray
 */
abstract class ValuesReader<S, V, T>(val converter: ValuesConverter<V, *, T>) :
    LineReader<S, V, T>(converter) {
    protected var limit = 0

    override fun read(source: S): Session = ValuesSession(source)

    protected abstract fun splitHeader(iterator: Iterator<V>): List<String>

    open inner class ValuesSession(source: S) : Session(source) {

        override fun preprocess(iterator: Iterator<V>) {
            cols.ifNotEmpty { restSlots(iterator, this) }
            limit = limit.coerceAtLeast(converter.typeValues.size)
            slots.ifNotEmpty {
                converter.resetOrder(this)
                limit = limit.coerceAtLeast(max() + 1)
            }
        }

        private fun restSlots(iterator: Iterator<V>, cols: Array<String>) {
            val split = splitHeader(iterator)
            slots = cols.map {
                split.indexOf(it).apply {
                    if (this < 0) throw NoSuchElementException("$it not in $split")
                }
            }.toIntArray()
        }
    }
}