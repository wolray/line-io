package com.github.wolray.line.io

/**
 * @author wolray
 */
abstract class ValuesReader<S, V, T>(val converter: ValuesConverter<V, T>) :
    LineReader<S, V, T>(converter) {
    protected var limit = 0

    protected abstract fun splitHeader(v: V): List<String>
    protected abstract fun errorColMsg(col: String, v: V): String

    inner class ValuesSession(source: S) : Session(source) {

        override fun preprocess(iterator: Iterator<V>) {
            cols.ifNotEmpty {
                slots = headerToSlots(iterator.next(), it)
            }
            limit = limit.coerceAtLeast(converter.attrs.size)
            slots.ifNotEmpty {
                converter.resetOrder(it)
                limit = limit.coerceAtLeast(it.max() + 1)
            }
        }

        private fun headerToSlots(v: V, header: Array<String>): IntArray {
            val split = splitHeader(v)
            return header.map {
                split.indexOf(it).apply {
                    if (this < 0) throw NoSuchElementException(errorColMsg(it, v))
                }
            }.toIntArray()
        }
    }
}