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

        override fun preprocess(iterator: Iterator<V>) = with(NotEmpty) {
            cols.ifNotEmpty { restSlots(iterator.next(), this) }
            limit = limit.coerceAtLeast(converter.attrs.size)
            slots.ifNotEmpty {
                converter.resetOrder(this)
                limit = limit.coerceAtLeast(max() + 1)
            }
        }

        private fun restSlots(v: V, header: Array<String>) {
            val split = splitHeader(v)
            slots = header.map {
                split.indexOf(it).apply {
                    if (this < 0) throw NoSuchElementException(errorColMsg(it, v))
                }
            }.toIntArray()
        }
    }
}