package com.github.wolray.line.io

import java.io.InputStream

/**
 * @author wolray
 */
class CsvReader<T> internal constructor(
    private val converter: ValuesConverter.Text<T>,
    private val sep: String
) : LineReader.Text<T>(converter.toParser(sep)) {

    override fun reorder(slots: IntArray) = converter.resetOrder(slots)

    override fun read(input: InputStream) = read(input, 0)

    override fun read(input: InputStream, skipLines: Int) = Session(input, skipLines)

    private fun setHeader(s: String, header: Array<String>) {
        val list = s.split(sep.toRegex())
        val slots = header
            .map { list.indexOf(it) }
            .toIntArray()
        for (i in slots.indices) {
            if (slots[i] < 0) {
                throw NoSuchElementException(header[i])
            }
        }
        reorder(slots)
    }

    inner class Session internal constructor(input: InputStream, skipLines: Int) :
        LineReader<InputStream, String, T>.Session(input, skipLines) {
        private var cols: Array<String>? = null

        fun csvHeader(vararg cols: String): Session {
            this.cols = arrayOf(*cols)
            return this
        }

        override fun preprocess(iterator: Iterator<String>) {
            if (cols.isNullOrEmpty().not()) {
                setHeader(iterator.next(), cols!!)
            } else {
                super.preprocess(iterator)
            }
        }
    }
}