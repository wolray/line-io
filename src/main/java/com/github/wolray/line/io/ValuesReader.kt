package com.github.wolray.line.io

/**
 * @author wolray
 */
abstract class ValuesReader<S, V, T>(val converter: ValuesConverter<V, T>) :
    LineReader<S, V, T>(converter) {
    protected var limit = 0

    override fun read(source: S): Session = ValuesSession(source)

    protected abstract fun splitHeader(iterator: Iterator<V>): List<String>
    protected abstract fun errorColMsg(col: String, header: List<String>): String

    open inner class ValuesSession(source: S) : Session(source) {

        override fun preprocess(iterator: Iterator<V>) = with(NotEmpty) {
            cols.ifNotEmpty { restSlots(iterator, this) }
            limit = limit.coerceAtLeast(converter.attrs.size)
            slots.ifNotEmpty {
                converter.resetOrder(this)
                limit = limit.coerceAtLeast(max() + 1)
            }
        }

        private fun restSlots(iterator: Iterator<V>, cols: Array<String>) {
            val split = splitHeader(iterator)
            slots = cols.map {
                split.indexOf(it).apply {
                    if (this < 0) throw NoSuchElementException(errorColMsg(it, split))
                }
            }.toIntArray()
        }
    }
}
