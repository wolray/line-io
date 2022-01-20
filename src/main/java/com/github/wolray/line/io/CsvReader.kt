package com.github.wolray.line.io

import java.io.InputStream

/**
 * @author wolray
 */
class CsvReader<T> internal constructor(
    private val converter: ValuesConverter.Text<T>,
    private val sep: String
) : LineReader.Text<T>(converter.toParser(sep)) {

    override fun read(source: InputStream): Session {
        return read(source, 0)
    }

    override fun read(source: InputStream, skipLines: Int): Session {
        return Session(source, skipLines)
    }

    override fun reorder(slots: IntArray) {
        converter.resetOrder(slots)
    }

    private fun setHeader(firstRow: String, header: Array<String>) {
        val list = firstRow.split(sep.toRegex())
        val slots = header.map(list::indexOf).toIntArray()
        for (i in slots.indices) {
            if (slots[i] < 0) {
                throw NoSuchElementException(header[i])
            }
        }
        reorder(slots)
    }

    inner class Session(source: InputStream, skipLines: Int) :
        LineReader<InputStream, String, T>.Session(source, skipLines) {
        private var cols: Array<String>? = null

        fun csvHeader(vararg cols: String): Session {
            this.cols = arrayOf(*cols)
            return this
        }

        override fun prepare(iterator: Iterator<String>) {
            if (cols.isNullOrEmpty().not()) {
                setHeader(iterator.next(), cols!!)
            } else super.prepare(iterator)
        }
    }
}
